package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object ChatBackupPreferencesManager {
    private const val TAG = "ChatBackupPrefs"
    private const val PREFS_NAME = "chat_backup_prefs"

    private const val KEY_DRIVE_ACCOUNT_NAME = "drive_account_name"
    private const val KEY_DRIVE_ACCESS_TOKEN = "drive_access_token"
    private const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"
    private const val KEY_LAST_BACKUP_SIZE = "last_backup_size"
    private const val KEY_AUTO_BACKUP_FREQ = "auto_backup_freq" // "OFF", "DAILY", "WEEKLY"
    private const val KEY_INCLUDE_MEDIA = "include_media"
    private const val KEY_WIFI_ONLY = "wifi_only"
    private const val KEY_WRAPPED_VAULT_PASSWORD = "wrapped_vault_password"

    private const val WRAPPER_KEY_ALIAS = "vault_password_wrapper_key"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SIZE_BYTES = 12

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getOrCreateWrapperKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(WRAPPER_KEY_ALIAS)) {
            val entry = keyStore.getEntry(WRAPPER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            WRAPPER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Allows worker to read in background

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    fun getDriveAccountName(context: Context): String? {
        return getPrefs(context).getString(KEY_DRIVE_ACCOUNT_NAME, null)
    }

    fun setDriveAccountName(context: Context, name: String?) {
        getPrefs(context).edit().putString(KEY_DRIVE_ACCOUNT_NAME, name).apply()
    }

    fun getDriveAccessToken(context: Context): String? {
        return getPrefs(context).getString(KEY_DRIVE_ACCESS_TOKEN, null)
    }

    fun setDriveAccessToken(context: Context, token: String?) {
        getPrefs(context).edit().putString(KEY_DRIVE_ACCESS_TOKEN, token).apply()
    }

    fun getLastBackupTimestamp(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_BACKUP_TIMESTAMP, 0L)
    }

    fun setLastBackupTimestamp(context: Context, timestamp: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_BACKUP_TIMESTAMP, timestamp).apply()
    }

    fun getLastBackupSizeBytes(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_BACKUP_SIZE, 0L)
    }

    fun setLastBackupSizeBytes(context: Context, size: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_BACKUP_SIZE, size).apply()
    }

    fun getAutoBackupFrequency(context: Context): String {
        return getPrefs(context).getString(KEY_AUTO_BACKUP_FREQ, "OFF") ?: "OFF"
    }

    fun setAutoBackupFrequency(context: Context, freq: String) {
        getPrefs(context).edit().putString(KEY_AUTO_BACKUP_FREQ, freq).apply()
    }

    fun getIncludeMedia(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INCLUDE_MEDIA, false)
    }

    fun setIncludeMedia(context: Context, include: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_INCLUDE_MEDIA, include).apply()
    }

    fun getWifiOnly(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WIFI_ONLY, true)
    }

    fun setWifiOnly(context: Context, wifiOnly: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WIFI_ONLY, wifiOnly).apply()
    }

    /**
     * Stores the vault password encrypted with Android Keystore AES-256-GCM wrapper key.
     * Never stores plaintext passwords in SharedPreferences.
     */
    fun setCachedVaultPassword(context: Context, password: String?) {
        if (password.isNullOrBlank()) {
            getPrefs(context).edit().remove(KEY_WRAPPED_VAULT_PASSWORD).apply()
            return
        }

        try {
            val secretKey = getOrCreateWrapperKey()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(password.toByteArray(Charsets.UTF_8))

            val buffer = ByteBuffer.allocate(IV_SIZE_BYTES + encryptedBytes.size)
            buffer.put(iv)
            buffer.put(encryptedBytes)

            val base64Payload = Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
            getPrefs(context).edit().putString(KEY_WRAPPED_VAULT_PASSWORD, base64Payload).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error wrapping vault password for storage", e)
        }
    }

    /**
     * Unwraps the vault password using Android Keystore AES-256-GCM wrapper key.
     */
    fun getCachedVaultPassword(context: Context): String? {
        val base64Payload = getPrefs(context).getString(KEY_WRAPPED_VAULT_PASSWORD, null) ?: return null
        return try {
            val payloadBytes = Base64.decode(base64Payload, Base64.NO_WRAP)
            if (payloadBytes.size <= IV_SIZE_BYTES) return null

            val buffer = ByteBuffer.wrap(payloadBytes)
            val iv = ByteArray(IV_SIZE_BYTES)
            buffer.get(iv)

            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)

            val secretKey = getOrCreateWrapperKey()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error unwrapping vault password from storage", e)
            null
        }
    }

    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
