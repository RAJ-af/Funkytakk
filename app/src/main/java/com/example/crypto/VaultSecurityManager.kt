package com.example.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricPrompt
import com.example.db.EncryptedChatDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class VaultStatus {
    NOT_CONFIGURED,
    LOCKED,
    UNLOCKED
}

object VaultSecurityManager {
    private const val TAG = "VaultSecurityManager"
    private const val PREFS_FILE = "vault_envelope_prefs"

    private const val PREF_IS_CONFIGURED = "vault_is_configured"
    private const val PREF_BIOMETRIC_ENABLED = "vault_biometric_enabled"
    private const val PREF_PASSWORD_SALT = "vault_password_salt"
    private const val PREF_PASSWORD_WRAPPED_KEY = "vault_password_wrapped_key"
    private const val PREF_PASSWORD_IV = "vault_password_iv"
    private const val PREF_BIOMETRIC_WRAPPED_KEY = "vault_biometric_wrapped_key"
    private const val PREF_BIOMETRIC_IV = "vault_biometric_iv"

    private const val BIOMETRIC_KEY_ALIAS = "vault_biometric_master_key"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 310_000
    private const val KEK_KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"

    private val _vaultStatus = MutableStateFlow(VaultStatus.LOCKED)
    val vaultStatus: StateFlow<VaultStatus> = _vaultStatus.asStateFlow()

    @Volatile
    private var activeVaultKey: ByteArray? = null

    fun isVaultConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_IS_CONFIGURED, false)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_BIOMETRIC_ENABLED, false) &&
                !prefs.getString(PREF_BIOMETRIC_WRAPPED_KEY, null).isNullOrBlank()
    }

    fun isUnlocked(): Boolean {
        return activeVaultKey != null && _vaultStatus.value == VaultStatus.UNLOCKED
    }

    fun getActiveVaultKey(): ByteArray? = activeVaultKey

    fun init(context: Context) {
        if (!isVaultConfigured(context)) {
            _vaultStatus.value = VaultStatus.NOT_CONFIGURED
        } else if (activeVaultKey != null) {
            _vaultStatus.value = VaultStatus.UNLOCKED
        } else {
            _vaultStatus.value = VaultStatus.LOCKED
        }
    }

    /**
     * Initializes the chat vault with a user chosen password.
     * Generates a random 256-bit vault key and wraps it with PBKDF2-derived KEK.
     */
    suspend fun setupVaultWithPassword(context: Context, password: String): Result<ByteArray> = withContext(Dispatchers.Default) {
        try {
            if (password.length < 4) {
                return@withContext Result.failure(IllegalArgumentException("Password must be at least 4 characters"))
            }

            // 1. Generate random 256-bit (32 bytes) vault key
            val vaultKey = ByteArray(32).also { SecureRandom().nextBytes(it) }

            // 2. Generate random 16-byte salt for PBKDF2
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }

            // 3. Derive KEK with PBKDF2WithHmacSHA256 (>= 310,000 iterations)
            val kek = deriveKek(password.toCharArray(), salt)

            // 4. Wrap vault key using AES-256-GCM
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, kek)
            val iv = cipher.iv
            val wrappedVaultKey = cipher.doFinal(vaultKey)

            // 5. Store wrapped password copy in SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(PREF_IS_CONFIGURED, true)
                .putString(PREF_PASSWORD_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(PREF_PASSWORD_WRAPPED_KEY, Base64.encodeToString(wrappedVaultKey, Base64.NO_WRAP))
                .putString(PREF_PASSWORD_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()

            activeVaultKey = vaultKey
            _vaultStatus.value = VaultStatus.UNLOCKED
            Log.d(TAG, "Vault configured and unlocked successfully with password.")
            Result.success(vaultKey)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up vault", e)
            Result.failure(e)
        }
    }

    /**
     * Unwraps the vault key using the user's password.
     */
    suspend fun unlockWithPassword(context: Context, password: String): Result<ByteArray> = withContext(Dispatchers.Default) {
        try {
            val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            val saltBase64 = prefs.getString(PREF_PASSWORD_SALT, null)
                ?: return@withContext Result.failure(IllegalStateException("Vault not configured"))
            val wrappedBase64 = prefs.getString(PREF_PASSWORD_WRAPPED_KEY, null)
                ?: return@withContext Result.failure(IllegalStateException("No password vault copy found"))
            val ivBase64 = prefs.getString(PREF_PASSWORD_IV, null)
                ?: return@withContext Result.failure(IllegalStateException("Corrupted vault state"))

            val salt = Base64.decode(saltBase64, Base64.DEFAULT)
            val wrappedVaultKey = Base64.decode(wrappedBase64, Base64.DEFAULT)
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)

            val kek = deriveKek(password.toCharArray(), salt)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            val unwrappedKey = cipher.doFinal(wrappedVaultKey)
            activeVaultKey = unwrappedKey
            _vaultStatus.value = VaultStatus.UNLOCKED
            Log.d(TAG, "Vault unlocked successfully with password.")
            Result.success(unwrappedKey)
        } catch (e: javax.crypto.AEADBadTagException) {
            Log.w(TAG, "Password authentication failed (AEADBadTagException)")
            Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unlock vault with password", e)
            Result.failure(e)
        }
    }

    /**
     * Prepares Biometric Cipher for wrapping the vault key during biometric setup.
     */
    fun createBiometricEncryptCryptoObject(): Result<BiometricPrompt.CryptoObject> {
        return try {
            val secretKey = getOrCreateBiometricKeyStoreKey()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            Result.success(BiometricPrompt.CryptoObject(cipher))
        } catch (e: Exception) {
            Log.e(TAG, "Error creating biometric encrypt crypto object", e)
            Result.failure(e)
        }
    }

    /**
     * Wraps the active vault key with the authenticated Biometric Cipher.
     */
    fun saveBiometricWrappedVaultKey(context: Context, authenticatedCipher: Cipher): Result<Unit> {
        val vaultKey = activeVaultKey ?: return Result.failure(IllegalStateException("Vault must be unlocked first"))
        return try {
            val wrapped = authenticatedCipher.doFinal(vaultKey)
            val iv = authenticatedCipher.iv
            val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(PREF_BIOMETRIC_ENABLED, true)
                .putString(PREF_BIOMETRIC_WRAPPED_KEY, Base64.encodeToString(wrapped, Base64.NO_WRAP))
                .putString(PREF_BIOMETRIC_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
            Log.d(TAG, "Biometric copy of vault key saved successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving biometric wrapped key", e)
            Result.failure(e)
        }
    }

    /**
     * Prepares Biometric Cipher for unwrapping the vault key during biometric unlock.
     */
    fun createBiometricDecryptCryptoObject(context: Context): Result<BiometricPrompt.CryptoObject> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            val ivBase64 = prefs.getString(PREF_BIOMETRIC_IV, null)
                ?: return Result.failure(IllegalStateException("Biometric unlock not configured"))
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)

            val secretKey = getBiometricKeyStoreKey()
                ?: return Result.failure(IllegalStateException("Keystore biometric key not found"))

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            Result.success(BiometricPrompt.CryptoObject(cipher))
        } catch (e: Exception) {
            Log.e(TAG, "Error creating biometric decrypt crypto object", e)
            Result.failure(e)
        }
    }

    /**
     * Unwraps the vault key using the authenticated Biometric Cipher.
     */
    fun unlockWithBiometricCipher(context: Context, authenticatedCipher: Cipher): Result<ByteArray> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            val wrappedBase64 = prefs.getString(PREF_BIOMETRIC_WRAPPED_KEY, null)
                ?: return Result.failure(IllegalStateException("No biometric vault copy found"))
            val wrapped = Base64.decode(wrappedBase64, Base64.DEFAULT)

            val unwrapped = authenticatedCipher.doFinal(wrapped)
            activeVaultKey = unwrapped
            _vaultStatus.value = VaultStatus.UNLOCKED
            Log.d(TAG, "Vault unlocked successfully with biometric prompt.")
            Result.success(unwrapped)
        } catch (e: Exception) {
            Log.e(TAG, "Error unlocking with biometric cipher", e)
            Result.failure(e)
        }
    }

    fun lock() {
        activeVaultKey?.fill(0)
        activeVaultKey = null
        _vaultStatus.value = VaultStatus.LOCKED
        Log.d(TAG, "Vault locked.")
    }

    /**
     * Resets the vault if unrecoverable, clearing keys and database.
     */
    fun resetVault(context: Context) {
        lock()
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
                keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting KeyStore entry", e)
        }
        EncryptedChatDatabase.deleteDatabase(context)
        _vaultStatus.value = VaultStatus.NOT_CONFIGURED
        Log.d(TAG, "Vault reset complete.")
    }

    private fun deriveKek(password: CharArray, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEK_KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun getOrCreateBiometricKeyStoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            val entry = keyStore.getEntry(BIOMETRIC_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    private fun getBiometricKeyStoreKey(): SecretKey? {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            val entry = keyStore.getEntry(BIOMETRIC_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            return entry?.secretKey
        }
        return null
    }
}
