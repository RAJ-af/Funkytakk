package com.example

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.ByteBuffer
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class DeviceMetadata(
    val deviceId: String,
    val name: String,
    val os: String,
    val appVersion: String
)

/**
 * Generates and persists a stable per-install device identifier (random UUID) encrypted
 * at rest with Android Keystore AES-256-GCM inside dedicated SharedPreferences.
 *
 * - Never uses ANDROID_ID or Advertising ID.
 * - Strictly matches ^[A-Za-z0-9._-]{8,64}$.
 * - Survives logout and session clears; resets only on uninstall / app data wipe.
 */
object DeviceIdentity {
    private const val TAG = "DeviceIdentity"
    private const val PREFS_NAME = "funkytalk_device_identity_prefs"
    private const val KEY_ENCRYPTED_ID = "encrypted_device_uuid_v1"
    private const val KEY_FALLBACK_ID = "device_uuid_fallback_v1"
    private const val KEYSTORE_ALIAS = "funkytalk_device_identity_aes_key"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val NONCE_BYTES = 12
    private const val GCM_TAG_BITS = 128

    val DEVICE_ID_REGEX = Regex("^[A-Za-z0-9._-]{8,64}$")

    @Volatile
    private var cachedDeviceId: String? = null

    fun getDeviceId(context: Context): String {
        cachedDeviceId?.takeIf { DEVICE_ID_REGEX.matches(it) }?.let { return it }
        synchronized(this) {
            cachedDeviceId?.takeIf { DEVICE_ID_REGEX.matches(it) }?.let { return it }

            val appCtx = context.applicationContext ?: context
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val decrypted = readEncryptedId(prefs)
            if (decrypted != null && DEVICE_ID_REGEX.matches(decrypted)) {
                cachedDeviceId = decrypted
                return decrypted
            }

            val fallback = prefs.getString(KEY_FALLBACK_ID, null)?.trim()
            if (fallback != null && DEVICE_ID_REGEX.matches(fallback)) {
                // Upgrade into encrypted storage when Keystore is available
                if (writeEncryptedId(prefs, fallback)) {
                    prefs.edit().remove(KEY_FALLBACK_ID).commit()
                }
                cachedDeviceId = fallback
                return fallback
            }

            val freshId = UUID.randomUUID().toString()
            if (!writeEncryptedId(prefs, freshId)) {
                prefs.edit().putString(KEY_FALLBACK_ID, freshId).commit()
            }
            cachedDeviceId = freshId
            return freshId
        }
    }

    fun getDeviceName(): String {
        val mfg = Build.MANUFACTURER?.trim().orEmpty()
        val model = Build.MODEL?.trim().orEmpty()
        val combined = "$mfg $model".trim()
        return combined.ifBlank { "Android Device" }.take(80)
    }

    fun getOsVersion(): String {
        val release = Build.VERSION.RELEASE?.trim().orEmpty()
        return if (release.isNotBlank()) "Android $release" else "Android ${Build.VERSION.SDK_INT}"
    }

    fun getAppVersion(): String = BuildConfig.VERSION_NAME

    fun getDeviceMetadata(context: Context): DeviceMetadata {
        return DeviceMetadata(
            deviceId = getDeviceId(context),
            name = getDeviceName(),
            os = getOsVersion(),
            appVersion = getAppVersion()
        )
    }

    fun getVerifyDevicePayload(context: Context): VerifyDevicePayload {
        val meta = getDeviceMetadata(context)
        return VerifyDevicePayload(
            device_id = meta.deviceId,
            name = meta.name,
            os = meta.os,
            app_version = meta.appVersion
        )
    }

    private fun readEncryptedId(prefs: android.content.SharedPreferences): String? {
        val encoded = prefs.getString(KEY_ENCRYPTED_ID, null) ?: return null
        return try {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            if (payload.size <= NONCE_BYTES) return null
            val buffer = ByteBuffer.wrap(payload)
            val iv = ByteArray(NONCE_BYTES)
            buffer.get(iv)
            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)

            val secretKey = getKeystoreKey(createIfMissing = false) ?: return null
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8).trim()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Unable to decrypt stored device identity: ${e.javaClass.simpleName}")
            }
            null
        }
    }

    private fun writeEncryptedId(prefs: android.content.SharedPreferences, deviceId: String): Boolean {
        return try {
            val secretKey = getKeystoreKey(createIfMissing = true) ?: return false
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(deviceId.toByteArray(Charsets.UTF_8))

            val payload = ByteBuffer.allocate(iv.size + encrypted.size)
                .put(iv)
                .put(encrypted)
                .array()
            val encoded = Base64.encodeToString(payload, Base64.NO_WRAP)
            prefs.edit().putString(KEY_ENCRYPTED_ID, encoded).commit()
            true
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Keystore unavailable for device identity (${e.javaClass.simpleName})")
            }
            false
        }
    }

    private fun getKeystoreKey(createIfMissing: Boolean): SecretKey? {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }
        if (!createIfMissing) return null

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
