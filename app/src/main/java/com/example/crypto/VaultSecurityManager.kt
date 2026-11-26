package com.example.crypto

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricPrompt
import com.example.ActiveUserManager
import com.example.BuildConfig
import com.example.LegacySecurityMigration
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
import kotlin.math.ceil

enum class VaultStatus {
    NOT_CONFIGURED,
    LOCKED,
    UNLOCKED
}

class VaultCooldownException(val remainingSeconds: Int) :
    IllegalStateException("Too many wrong attempts. Try again in ${remainingSeconds}s.")

object VaultSecurityManager {
    private const val TAG = "VaultSecurityManager"

    private const val PREF_IS_CONFIGURED = "vault_is_configured"
    private const val PREF_BIOMETRIC_ENABLED = "vault_biometric_enabled"
    private const val PREF_PASSWORD_SALT = "vault_password_salt"
    private const val PREF_PASSWORD_WRAPPED_KEY = "vault_password_wrapped_key"
    private const val PREF_PASSWORD_IV = "vault_password_iv"
    private const val PREF_BIOMETRIC_WRAPPED_KEY = "vault_biometric_wrapped_key"
    private const val PREF_BIOMETRIC_IV = "vault_biometric_iv"
    private const val PREF_FAILED_ATTEMPTS = "vault_failed_attempts"
    private const val PREF_COOLDOWN_UNTIL_MS = "vault_cooldown_until_ms"

    private const val MAX_ATTEMPTS_BEFORE_COOLDOWN = 5
    private val COOLDOWN_DURATIONS_SEC = intArrayOf(30, 60, 120, 300)

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 310_000
    private const val KEK_KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"

    private val _vaultStatus = MutableStateFlow(VaultStatus.LOCKED)
    val vaultStatus: StateFlow<VaultStatus> = _vaultStatus.asStateFlow()

    @Volatile
    private var activeVaultKey: ByteArray? = null

    @Volatile
    private var activeKeyUid: String? = null

    private fun getPrefsForUid(context: Context, uid: String): SharedPreferences {
        return context.getSharedPreferences(ActiveUserManager.vaultPrefsName(uid), Context.MODE_PRIVATE)
    }

    private fun resolveUid(context: Context? = null): String? {
        return ActiveUserManager.getActiveUid(context)
    }

    fun getQaTestVaultPassword(): String? {
        return BuildConfig.QA_TEST_VAULT_PASSWORD.takeIf { BuildConfig.DEBUG && it.isNotBlank() }
    }

    fun getRemainingCooldownSeconds(context: Context): Int {
        val uid = resolveUid(context) ?: return 0
        val prefs = getPrefsForUid(context, uid)
        val untilMs = prefs.getLong(PREF_COOLDOWN_UNTIL_MS, 0L)
        if (untilMs <= 0L) return 0
        val remainingMs = untilMs - System.currentTimeMillis()
        if (remainingMs <= 0L) {
            prefs.edit().remove(PREF_COOLDOWN_UNTIL_MS).apply()
            return 0
        }
        return ceil(remainingMs / 1000.0).toInt().coerceAtLeast(1)
    }

    private fun clearCooldownCounters(prefs: SharedPreferences) {
        prefs.edit()
            .remove(PREF_FAILED_ATTEMPTS)
            .remove(PREF_COOLDOWN_UNTIL_MS)
            .apply()
    }

    private fun computeCooldownSeconds(failedAttempts: Int): Int {
        val step = (failedAttempts - MAX_ATTEMPTS_BEFORE_COOLDOWN).coerceAtLeast(0)
        return COOLDOWN_DURATIONS_SEC[step.coerceAtMost(COOLDOWN_DURATIONS_SEC.lastIndex)]
    }

    fun isVaultConfigured(context: Context): Boolean {
        val uid = resolveUid(context) ?: return false
        return getPrefsForUid(context, uid).getBoolean(PREF_IS_CONFIGURED, false)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        val uid = resolveUid(context) ?: return false
        val prefs = getPrefsForUid(context, uid)
        return prefs.getBoolean(PREF_BIOMETRIC_ENABLED, false) &&
            !prefs.getString(PREF_BIOMETRIC_WRAPPED_KEY, null).isNullOrBlank()
    }

    fun isUnlocked(): Boolean {
        val currentUid = ActiveUserManager.getActiveUid()
        if (activeKeyUid != null && currentUid != null && activeKeyUid != currentUid) {
            return false
        }
        return activeVaultKey != null && _vaultStatus.value == VaultStatus.UNLOCKED
    }

    fun isVaultLocked(): Boolean {
        return !isUnlocked()
    }

    internal fun getInMemoryVaultKey(): ByteArray? = activeVaultKey

    internal fun commitUnlockedVaultKey(
        context: Context,
        vaultKey: ByteArray,
        uid: String,
        markActive: Boolean = true
    ) {
        activeVaultKey = vaultKey
        activeKeyUid = uid
        _vaultStatus.value = VaultStatus.UNLOCKED
        if (markActive) {
            ChatLockManager.markUnlockedTimestamp(context, uid)
        }
    }

    /**
     * Strictly returns the unlocked in-memory vault key during foreground chat usage,
     * never silently bypassing [ChatLockManager] lock policies.
     */
    fun getActiveVaultKey(context: Context? = null): ByteArray? {
        val currentUid = resolveUid(context)
        if (activeVaultKey != null) {
            if (currentUid == null || activeKeyUid == currentUid) {
                return activeVaultKey
            }
            lock()
        }
        return null
    }

    /**
     * Used exclusively by unattended background WorkManager backup jobs (`DriveBackupEngine`)
     * so scheduled backups can run when the foreground vault is locked.
     */
    fun getVaultKeyForBackgroundBackup(context: Context): ByteArray? {
        getActiveVaultKey(context)?.let { return it }
        val uid = resolveUid(context) ?: return null
        return BackupCrypto.loadVaultKeyFromKeystore(context, uid)
    }

    fun init(context: Context) {
        val uid = resolveUid(context)
        if (uid == null) {
            lock()
            ChatLockManager.refreshState(context, null)
            return
        }
        if (activeKeyUid != null && activeKeyUid != uid) {
            lock()
        }
        if (!isVaultConfigured(context)) {
            activeVaultKey?.fill(0)
            activeVaultKey = null
            activeKeyUid = null
            _vaultStatus.value = VaultStatus.NOT_CONFIGURED
            ChatLockManager.refreshState(context, uid)
        } else if (activeVaultKey != null && activeKeyUid == uid) {
            if (ChatLockManager.is14DayPasswordExpired(context, uid)) {
                lock()
            } else {
                _vaultStatus.value = VaultStatus.UNLOCKED
            }
            ChatLockManager.refreshState(context, uid)
        } else {
            val silentKey = ChatLockManager.trySilentUnlockOnStartOrInit(context, uid)
            if (silentKey != null) {
                activeVaultKey = silentKey
                activeKeyUid = uid
                _vaultStatus.value = VaultStatus.UNLOCKED
            } else {
                _vaultStatus.value = VaultStatus.LOCKED
            }
            ChatLockManager.refreshState(context, uid)
        }
    }

    /**
     * Initializes the chat vault with a user chosen password, scoped to active UID.
     */
    suspend fun setupVaultWithPassword(context: Context, password: String): Result<ByteArray> = withContext(Dispatchers.Default) {
        try {
            val uid = ActiveUserManager.requireActiveUid(context)
            if (password.length < 4) {
                return@withContext Result.failure(IllegalArgumentException("Password must be at least 4 characters"))
            }

            val vaultKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val kek = deriveKek(password.toCharArray(), salt)

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, kek)
            val iv = cipher.iv
            val wrappedVaultKey = cipher.doFinal(vaultKey)

            getPrefsForUid(context, uid).edit()
                .putBoolean(PREF_IS_CONFIGURED, true)
                .putString(PREF_PASSWORD_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(PREF_PASSWORD_WRAPPED_KEY, Base64.encodeToString(wrappedVaultKey, Base64.NO_WRAP))
                .putString(PREF_PASSWORD_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .remove(PREF_FAILED_ATTEMPTS)
                .remove(PREF_COOLDOWN_UNTIL_MS)
                .apply()

            activeVaultKey = vaultKey
            activeKeyUid = uid
            _vaultStatus.value = VaultStatus.UNLOCKED
            BackupCrypto.saveVaultKeyToKeystore(context, vaultKey, uid)
            ChatLockManager.onPasswordVerified(context, uid, vaultKey)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Vault configured and unlocked.")
            }
            Result.success(vaultKey)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up vault", e)
            Result.failure(e)
        }
    }

    /**
     * Unwraps the vault key using the user's password for the active UID, enforcing
     * escalating per-account cooldown after 5 consecutive failed attempts.
     */
    suspend fun unlockWithPassword(context: Context, password: String): Result<ByteArray> = withContext(Dispatchers.Default) {
        val uid = try {
            ActiveUserManager.requireActiveUid(context)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
        val prefs = getPrefsForUid(context, uid)

        val cooldownRemaining = getRemainingCooldownSeconds(context)
        if (cooldownRemaining > 0) {
            return@withContext Result.failure(VaultCooldownException(cooldownRemaining))
        }

        try {
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
            clearCooldownCounters(prefs)
            activeVaultKey = unwrappedKey
            activeKeyUid = uid
            _vaultStatus.value = VaultStatus.UNLOCKED
            BackupCrypto.saveVaultKeyToKeystore(context, unwrappedKey, uid)
            ChatLockManager.onPasswordVerified(context, uid, unwrappedKey)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Vault unlocked successfully with password.")
            }
            Result.success(unwrappedKey)
        } catch (e: javax.crypto.AEADBadTagException) {
            val attempts = prefs.getInt(PREF_FAILED_ATTEMPTS, 0) + 1
            if (attempts >= MAX_ATTEMPTS_BEFORE_COOLDOWN) {
                val cooldownSec = computeCooldownSeconds(attempts)
                val untilMs = System.currentTimeMillis() + (cooldownSec * 1000L)
                prefs.edit()
                    .putInt(PREF_FAILED_ATTEMPTS, attempts)
                    .putLong(PREF_COOLDOWN_UNTIL_MS, untilMs)
                    .apply()
                Log.w(TAG, "Vault unlock entered ${cooldownSec}s cooldown after $attempts failed attempts")
                Result.failure(VaultCooldownException(cooldownSec))
            } else {
                prefs.edit().putInt(PREF_FAILED_ATTEMPTS, attempts).apply()
                val left = MAX_ATTEMPTS_BEFORE_COOLDOWN - attempts
                Log.w(TAG, "Password authentication failed ($attempts/$MAX_ATTEMPTS_BEFORE_COOLDOWN)")
                Result.failure(IllegalArgumentException("Incorrect password. $left ${if (left == 1) "attempt" else "attempts"} remaining before cooldown."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unlock vault with password", e)
            Result.failure(e)
        }
    }

    fun createBiometricEncryptCryptoObject(context: Context? = null): Result<BiometricPrompt.CryptoObject> {
        return try {
            val uid = ActiveUserManager.requireActiveUid(context)
            val secretKey = getOrCreateBiometricKeyStoreKey(uid)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            Result.success(BiometricPrompt.CryptoObject(cipher))
        } catch (e: Exception) {
            Log.e(TAG, "Error creating biometric encrypt crypto object", e)
            Result.failure(e)
        }
    }

    fun saveBiometricWrappedVaultKey(context: Context, authenticatedCipher: Cipher): Result<Unit> {
        val uid = ActiveUserManager.getActiveUid(context)
            ?: return Result.failure(IllegalStateException("No active account"))
        val vaultKey = getActiveVaultKey(context)
            ?: return Result.failure(IllegalStateException("Vault must be unlocked first"))
        return try {
            val wrapped = authenticatedCipher.doFinal(vaultKey)
            val iv = authenticatedCipher.iv
            getPrefsForUid(context, uid).edit()
                .putBoolean(PREF_BIOMETRIC_ENABLED, true)
                .putString(PREF_BIOMETRIC_WRAPPED_KEY, Base64.encodeToString(wrapped, Base64.NO_WRAP))
                .putString(PREF_BIOMETRIC_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Biometric copy of vault key saved.")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving biometric wrapped key", e)
            Result.failure(e)
        }
    }

    fun createBiometricDecryptCryptoObject(context: Context): Result<BiometricPrompt.CryptoObject> {
        return try {
            val uid = ActiveUserManager.requireActiveUid(context)
            val prefs = getPrefsForUid(context, uid)
            val ivBase64 = prefs.getString(PREF_BIOMETRIC_IV, null)
                ?: return Result.failure(IllegalStateException("Biometric unlock not configured"))
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)

            val secretKey = getBiometricKeyStoreKey(uid)
                ?: return Result.failure(IllegalStateException("Keystore biometric key not found"))

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            Result.success(BiometricPrompt.CryptoObject(cipher))
        } catch (e: Exception) {
            Log.e(TAG, "Error creating biometric decrypt crypto object", e)
            Result.failure(e)
        }
    }

    fun unlockWithBiometricCipher(context: Context, authenticatedCipher: Cipher): Result<ByteArray> {
        return try {
            val uid = ActiveUserManager.requireActiveUid(context)
            val prefs = getPrefsForUid(context, uid)
            val wrappedBase64 = prefs.getString(PREF_BIOMETRIC_WRAPPED_KEY, null)
                ?: return Result.failure(IllegalStateException("No biometric vault copy found"))
            val wrapped = Base64.decode(wrappedBase64, Base64.DEFAULT)

            val unwrapped = authenticatedCipher.doFinal(wrapped)
            clearCooldownCounters(prefs)
            activeVaultKey = unwrapped
            activeKeyUid = uid
            _vaultStatus.value = VaultStatus.UNLOCKED
            ChatLockManager.markUnlockedTimestamp(context, uid)
            ChatLockManager.refreshState(context, uid)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Vault unlocked with biometric prompt.")
            }
            Result.success(unwrapped)
        } catch (e: Exception) {
            Log.e(TAG, "Error unlocking with biometric cipher", e)
            Result.failure(e)
        }
    }

    fun lock() {
        activeVaultKey?.fill(0)
        activeVaultKey = null
        activeKeyUid = null
        com.example.data.SavedMomentsStore.clearMemoryCache()
        _vaultStatus.value = VaultStatus.LOCKED
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Vault locked.")
        }
    }

    /**
     * Clears only in-memory vault keys and marks Vault LOCKED on logout without deleting on-disk
     * scoped encrypted chats for that user.
     */
    fun resetForLogout() {
        lock()
    }

    /**
     * Resets the vault if unrecoverable or erased for the current/specified UID.
     */
    fun resetVault(context: Context, uid: String? = null) {
        val targetUid = uid?.takeIf { it.isNotBlank() } ?: resolveUid(context)
        lock()
        if (targetUid != null) {
            wipeForUid(context, targetUid)
        }
        _vaultStatus.value = VaultStatus.NOT_CONFIGURED
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Vault reset complete.")
        }
    }

    fun wipeForUid(context: Context, uid: String) {
        if (uid.isBlank()) return
        if (activeKeyUid == uid) {
            lock()
        }
        LegacySecurityMigration.deleteSharedPrefsCompat(context, ActiveUserManager.vaultPrefsName(uid))
        try {
            val alias = ActiveUserManager.vaultBiometricAlias(uid)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting biometric KeyStore entry", e)
        }
        EncryptedChatDatabase.deleteDatabaseForUid(context, uid)
        BackupCrypto.wipeForUid(context, uid)
        ChatLockManager.wipeForUid(context, uid)
        com.example.data.SavedMomentsStore.wipeForUid(context, uid)
        try {
            ActiveUserManager.mediaDir(context, uid).deleteRecursively()
            ActiveUserManager.restoredMediaDir(context, uid).deleteRecursively()
        } catch (_: Exception) {}
    }

    private fun deriveKek(password: CharArray, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEK_KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun getOrCreateBiometricKeyStoreKey(uid: String): SecretKey {
        val alias = ActiveUserManager.vaultBiometricAlias(uid)
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    private fun getBiometricKeyStoreKey(uid: String): SecretKey? {
        val alias = ActiveUserManager.vaultBiometricAlias(uid)
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            return entry?.secretKey
        }
        return null
    }
}
