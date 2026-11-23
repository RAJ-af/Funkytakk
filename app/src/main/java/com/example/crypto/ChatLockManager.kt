package com.example.crypto

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.ActiveUserManager
import com.example.BuildConfig
import com.example.LegacySecurityMigration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class LockAskAgainMode(val prefValue: String, val label: String, val timeoutMs: Long) {
    EVERY_TIME("every_time", "Every time I open the app", 0L),
    AFTER_5_MIN("after_5_min", "After 5 minutes away", 5L * 60L * 1000L),
    AFTER_1_HOUR("after_1_hour", "After 1 hour away", 60L * 60L * 1000L);

    companion object {
        fun fromPref(value: String?): LockAskAgainMode {
            return entries.firstOrNull { it.prefValue == value } ?: AFTER_5_MIN
        }
    }
}

enum class UnlockRequirement {
    SETUP_REQUIRED,
    ALREADY_UNLOCKED,
    LEVEL_1_BIOMETRIC_OR_DEVICE_CREDENTIAL,
    LEVEL_2_VAULT_PASSWORD
}

data class ChatLockUiState(
    val isChatLockEnabled: Boolean = true,
    val askAgainMode: LockAskAgainMode = LockAskAgainMode.AFTER_5_MIN,
    val lastPasswordCheckWallMs: Long = 0L,
    val is14DayPasswordExpired: Boolean = false,
    val hasRememberedKey: Boolean = false,
    val hasScreenLockOrBiometric: Boolean = true,
    val hasStrongBiometric: Boolean = false,
    val biometricEnrollmentChanged: Boolean = false,
    val requirement: UnlockRequirement = UnlockRequirement.LEVEL_2_VAULT_PASSWORD
)

/**
 * Coordinates Two-Level Chat Lock:
 * - Level 1 (frequent): BiometricPrompt (BIOMETRIC_STRONG / DEVICE_CREDENTIAL, with visible "Use vault password" fallback)
 * - Level 2 (rare): Vault password on first setup, new device/reinstall, every 14 days, or if biometrics changed/unavailable.
 *
 * Security contract:
 * - Never stores plaintext vault password or raw vaultKey.
 * - Remembers vaultKey ONLY wrapped by an Android Keystore AES-256-GCM key (StrongBox when available, TEE fallback).
 * - Unattended background backups continue to use [VaultSecurityManager.getVaultKeyForBackgroundBackup].
 */
object ChatLockManager {
    private const val TAG = "ChatLockManager"

    const val PASSWORD_MAX_AGE_MS: Long = 14L * 24L * 60L * 60L * 1000L

    private const val PREF_LOCK_ENABLED = "chat_lock_enabled"
    private const val PREF_ASK_AGAIN_MODE = "chat_lock_ask_again_mode"
    private const val PREF_WRAPPED_VAULT_KEY = "chat_lock_wrapped_vault_key"
    private const val PREF_LAST_PASSWORD_WALL_MS = "chat_lock_last_password_wall_ms"
    private const val PREF_LAST_ACTIVE_ELAPSED_MS = "chat_lock_last_active_elapsed_ms"
    private const val PREF_LAST_BOOT_COUNT = "chat_lock_last_boot_count"
    private const val PREF_BIO_CHANGED_FLAG = "chat_lock_bio_changed_flag"
    private const val PREF_HAD_BIO_ENROLLED = "chat_lock_had_bio_enrolled"

    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val NONCE_SIZE_BYTES = 12
    private const val GCM_TAG_BITS = 128

    private val observerAttached = AtomicBoolean(false)

    @Volatile
    private var appContextRef: Context? = null

    @Volatile
    private var isInBackground = false

    @Volatile
    private var isBiometricPromptActive = false

    private val _uiState = MutableStateFlow(ChatLockUiState())
    val uiState: StateFlow<ChatLockUiState> = _uiState.asStateFlow()

    fun prefsName(uid: String): String = "chat_lock_prefs_${ActiveUserManager.sanitizeUid(uid)}"
    fun rememberedKeyAlias(uid: String): String = "chat_lock_remembered_vk_${ActiveUserManager.sanitizeUid(uid)}"
    fun bioProbeKeyAlias(uid: String): String = "chat_lock_bio_probe_${ActiveUserManager.sanitizeUid(uid)}"

    private fun getPrefs(context: Context, uid: String): SharedPreferences {
        return context.applicationContext.getSharedPreferences(prefsName(uid), Context.MODE_PRIVATE)
    }

    fun attachLifecycleObserver(context: Context) {
        appContextRef = context.applicationContext
        if (observerAttached.compareAndSet(false, true)) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    val ctx = appContextRef ?: return
                    if (isInBackground) {
                        isInBackground = false
                        onAppForegrounded(ctx)
                    } else {
                        refreshState(ctx)
                    }
                }

                override fun onStop(owner: LifecycleOwner) {
                    val ctx = appContextRef ?: return
                    if (isBiometricPromptActive) return
                    isInBackground = true
                    recordBackgroundTimestamp(ctx)
                }
            })
        }
        refreshState(context)
    }

    fun isChatLockEnabled(context: Context, uid: String? = ActiveUserManager.getActiveUid(context)): Boolean {
        val resolvedUid = uid ?: return true
        return getPrefs(context, resolvedUid).getBoolean(PREF_LOCK_ENABLED, true)
    }

    fun setChatLockEnabled(context: Context, enabled: Boolean) {
        val uid = ActiveUserManager.getActiveUid(context) ?: return
        getPrefs(context, uid).edit().putBoolean(PREF_LOCK_ENABLED, enabled).apply()
        if (!enabled && !is14DayPasswordExpired(context, uid)) {
            val existingKey = VaultSecurityManager.getInMemoryVaultKey() ?: unwrapRememberedVaultKey(context, uid)
            if (existingKey != null) {
                VaultSecurityManager.commitUnlockedVaultKey(context, existingKey, uid, markActive = true)
            }
        }
        refreshState(context, uid)
    }

    fun getAskAgainMode(context: Context, uid: String? = ActiveUserManager.getActiveUid(context)): LockAskAgainMode {
        val resolvedUid = uid ?: return LockAskAgainMode.AFTER_5_MIN
        return LockAskAgainMode.fromPref(getPrefs(context, resolvedUid).getString(PREF_ASK_AGAIN_MODE, null))
    }

    fun setAskAgainMode(context: Context, mode: LockAskAgainMode) {
        val uid = ActiveUserManager.getActiveUid(context) ?: return
        getPrefs(context, uid).edit().putString(PREF_ASK_AGAIN_MODE, mode.prefValue).apply()
        refreshState(context, uid)
    }

    fun hasScreenLockOrBiometric(context: Context): Boolean {
        return hasBiometricEnrolled(context) || isDeviceSecure(context)
    }

    fun hasBiometricEnrolled(context: Context): Boolean {
        val bm = BiometricManager.from(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS ||
                bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        }
    }

    fun hasStrongBiometric(context: Context): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isDeviceSecure(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isDeviceSecure == true
    }

    fun is14DayPasswordExpired(context: Context, uid: String? = ActiveUserManager.getActiveUid(context)): Boolean {
        val resolvedUid = uid ?: return true
        val lastCheck = getPrefs(context, resolvedUid).getLong(PREF_LAST_PASSWORD_WALL_MS, 0L)
        if (lastCheck <= 0L) return true
        val elapsed = System.currentTimeMillis() - lastCheck
        return elapsed < 0L || elapsed >= PASSWORD_MAX_AGE_MS
    }

    fun hasRememberedVaultKey(context: Context, uid: String? = ActiveUserManager.getActiveUid(context)): Boolean {
        val resolvedUid = uid ?: return false
        return !getPrefs(context, resolvedUid).getString(PREF_WRAPPED_VAULT_KEY, null).isNullOrBlank()
    }

    /**
     * Called after password setup or password unlock succeeds.
     * Stores an Android Keystore AES-GCM wrapped vault key and resets the 14-day timer.
     */
    fun onPasswordVerified(context: Context, uid: String, vaultKey: ByteArray) {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        val bioEnrolledNow = hasBiometricEnrolled(context)

        wrapAndSaveVaultKey(context, uid, vaultKey)
        if (hasStrongBiometric(context)) {
            ensureOrRecreateBioProbeKey(uid)
        } else {
            deleteKeystoreAlias(bioProbeKeyAlias(uid))
        }

        getPrefs(context, uid).edit()
            .putLong(PREF_LAST_PASSWORD_WALL_MS, nowWall)
            .putLong(PREF_LAST_ACTIVE_ELAPSED_MS, nowElapsed)
            .putInt(PREF_LAST_BOOT_COUNT, readBootCount(context))
            .putBoolean(PREF_BIO_CHANGED_FLAG, false)
            .putBoolean(PREF_HAD_BIO_ENROLLED, bioEnrolledNow)
            .apply()

        refreshState(context, uid)
    }

    /**
     * Evaluates whether cold start or foreground resume can silently restore the remembered Keystore key
     * within the user's chosen "Ask again" window (or when Lock Chats is OFF), without weakening the 14-day rule.
     *
     * Returns unwrapped vaultKey if silent unlock is permitted; null if UI unlock is required.
     */
    fun trySilentUnlockOnStartOrInit(context: Context, uid: String): ByteArray? {
        if (!VaultSecurityManager.isVaultConfigured(context)) return null
        val prefs = getPrefs(context, uid)
        if (is14DayPasswordExpired(context, uid)) {
            refreshState(context, uid)
            return null
        }
        if (checkAndMarkBiometricChanges(context, uid)) {
            refreshState(context, uid)
            return null
        }

        val wrappedKey = unwrapRememberedVaultKey(context, uid) ?: run {
            refreshState(context, uid)
            return null
        }

        val lockEnabled = prefs.getBoolean(PREF_LOCK_ENABLED, true)
        if (!lockEnabled) {
            markUnlockedTimestamp(context, uid)
            refreshState(context, uid)
            return wrappedKey
        }

        val mode = LockAskAgainMode.fromPref(prefs.getString(PREF_ASK_AGAIN_MODE, null))
        if (mode == LockAskAgainMode.EVERY_TIME) {
            wrappedKey.fill(0)
            refreshState(context, uid)
            return null
        }

        val savedBoot = prefs.getInt(PREF_LAST_BOOT_COUNT, -1)
        val currentBoot = readBootCount(context)
        val lastActiveElapsed = prefs.getLong(PREF_LAST_ACTIVE_ELAPSED_MS, -1L)
        val nowElapsed = SystemClock.elapsedRealtime()

        val sameBoot = savedBoot >= 0 && currentBoot >= 0 && savedBoot == currentBoot
        if (sameBoot && lastActiveElapsed > 0L && nowElapsed >= lastActiveElapsed) {
            val awayMs = nowElapsed - lastActiveElapsed
            if (awayMs < mode.timeoutMs) {
                markUnlockedTimestamp(context, uid)
                refreshState(context, uid)
                return wrappedKey
            }
        }

        wrappedKey.fill(0)
        refreshState(context, uid)
        return null
    }

    fun onAppForegrounded(context: Context) {
        val uid = ActiveUserManager.getActiveUid(context) ?: return
        if (!VaultSecurityManager.isVaultConfigured(context)) {
            refreshState(context, uid)
            return
        }

        if (is14DayPasswordExpired(context, uid) || checkAndMarkBiometricChanges(context, uid)) {
            VaultSecurityManager.lock()
            refreshState(context, uid)
            return
        }

        val lockEnabled = isChatLockEnabled(context, uid)
        if (!lockEnabled) {
            if (!VaultSecurityManager.isUnlocked()) {
                val restored = unwrapRememberedVaultKey(context, uid)
                if (restored != null) {
                    VaultSecurityManager.commitUnlockedVaultKey(context, restored, uid, markActive = true)
                }
            }
            refreshState(context, uid)
            return
        }

        val prefs = getPrefs(context, uid)
        val mode = getAskAgainMode(context, uid)
        val lastElapsed = prefs.getLong(PREF_LAST_ACTIVE_ELAPSED_MS, -1L)
        val nowElapsed = SystemClock.elapsedRealtime()
        val awayMs = if (lastElapsed > 0L && nowElapsed >= lastElapsed) {
            nowElapsed - lastElapsed
        } else {
            Long.MAX_VALUE
        }

        val shouldLock = when (mode) {
            LockAskAgainMode.EVERY_TIME -> lastElapsed > 0L
            LockAskAgainMode.AFTER_5_MIN, LockAskAgainMode.AFTER_1_HOUR -> awayMs >= mode.timeoutMs
        }

        if (shouldLock && VaultSecurityManager.isUnlocked()) {
            VaultSecurityManager.lock()
        }
        refreshState(context, uid)
    }

    fun recordBackgroundTimestamp(context: Context) {
        val uid = ActiveUserManager.getActiveUid(context) ?: return
        if (!VaultSecurityManager.isUnlocked()) return
        markUnlockedTimestamp(context, uid)
    }

    fun markUnlockedTimestamp(context: Context, uid: String? = ActiveUserManager.getActiveUid(context)) {
        val resolvedUid = uid ?: return
        getPrefs(context, resolvedUid).edit()
            .putLong(PREF_LAST_ACTIVE_ELAPSED_MS, SystemClock.elapsedRealtime())
            .putInt(PREF_LAST_BOOT_COUNT, readBootCount(context))
            .apply()
    }

    /**
     * Validates 14-day rule & biometric state and unwraps the Keystore vault key without yet flipping
     * [VaultSecurityManager.vaultStatus], allowing UI to animate the open-lock + haptic smoothly.
     */
    fun prepareLevel1Unlock(context: Context): Result<ByteArray> {
        val uid = ActiveUserManager.getActiveUid(context)
            ?: return Result.failure(IllegalStateException("No active account"))
        if (is14DayPasswordExpired(context, uid)) {
            refreshState(context, uid)
            return Result.failure(IllegalStateException("14-day vault password verification is required"))
        }
        if (checkAndMarkBiometricChanges(context, uid)) {
            refreshState(context, uid)
            return Result.failure(IllegalStateException("Biometrics changed on this device. Please verify your vault password."))
        }
        val unwrapped = unwrapRememberedVaultKey(context, uid)
            ?: return Result.failure(IllegalStateException("No remembered vault key available. Please verify your vault password."))
        return Result.success(unwrapped)
    }

    fun finalizePreparedUnlock(context: Context, vaultKey: ByteArray) {
        val uid = ActiveUserManager.getActiveUid(context) ?: return
        VaultSecurityManager.commitUnlockedVaultKey(context, vaultKey, uid, markActive = true)
        refreshState(context, uid)
    }

    /**
     * Unlocks VaultSecurityManager using the Keystore-wrapped vault key after Level 1 (Biometric/PIN)
     * succeeds. Rejects if 14-day password check is due or biometrics changed.
     */
    fun unlockWithKeystoreAfterLevel1(context: Context): Result<ByteArray> {
        val prepared = prepareLevel1Unlock(context)
        prepared.onSuccess { key ->
            finalizePreparedUnlock(context, key)
        }
        return prepared
    }

    fun resolveUnlockRequirement(context: Context, uid: String? = ActiveUserManager.getActiveUid(context)): UnlockRequirement {
        if (!VaultSecurityManager.isVaultConfigured(context)) {
            return UnlockRequirement.SETUP_REQUIRED
        }
        if (VaultSecurityManager.isUnlocked()) {
            return UnlockRequirement.ALREADY_UNLOCKED
        }
        val resolvedUid = uid ?: return UnlockRequirement.LEVEL_2_VAULT_PASSWORD
        if (is14DayPasswordExpired(context, resolvedUid)) {
            return UnlockRequirement.LEVEL_2_VAULT_PASSWORD
        }
        if (!hasRememberedVaultKey(context, resolvedUid)) {
            return UnlockRequirement.LEVEL_2_VAULT_PASSWORD
        }
        if (checkAndMarkBiometricChanges(context, resolvedUid)) {
            return UnlockRequirement.LEVEL_2_VAULT_PASSWORD
        }
        if (!hasScreenLockOrBiometric(context)) {
            return UnlockRequirement.LEVEL_2_VAULT_PASSWORD
        }
        return if (isChatLockEnabled(context, resolvedUid)) {
            UnlockRequirement.LEVEL_1_BIOMETRIC_OR_DEVICE_CREDENTIAL
        } else {
            UnlockRequirement.LEVEL_2_VAULT_PASSWORD
        }
    }

    fun refreshState(context: Context, uid: String? = ActiveUserManager.getActiveUid(context)) {
        val resolvedUid = uid
        val prefs = resolvedUid?.let { getPrefs(context, it) }
        val expired = resolvedUid?.let { is14DayPasswordExpired(context, it) } ?: true
        val bioChanged = prefs?.getBoolean(PREF_BIO_CHANGED_FLAG, false) ?: false
        val requirement = resolveUnlockRequirement(context, resolvedUid)
        _uiState.value = ChatLockUiState(
            isChatLockEnabled = prefs?.getBoolean(PREF_LOCK_ENABLED, true) ?: true,
            askAgainMode = LockAskAgainMode.fromPref(prefs?.getString(PREF_ASK_AGAIN_MODE, null)),
            lastPasswordCheckWallMs = prefs?.getLong(PREF_LAST_PASSWORD_WALL_MS, 0L) ?: 0L,
            is14DayPasswordExpired = expired,
            hasRememberedKey = resolvedUid?.let { hasRememberedVaultKey(context, it) } ?: false,
            hasScreenLockOrBiometric = hasScreenLockOrBiometric(context),
            hasStrongBiometric = hasStrongBiometric(context),
            biometricEnrollmentChanged = bioChanged,
            requirement = requirement
        )
    }

    /**
     * Launches Level 1 BiometricPrompt:
     * - API 30+: BIOMETRIC_STRONG | DEVICE_CREDENTIAL (fingerprint/face first, device PIN fallback).
     * - API 28-29: BIOMETRIC_WEAK | DEVICE_CREDENTIAL (safe compat without IllegalArgumentException).
     */
    fun launchLevel1Prompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onUsePasswordFallback: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasScreenLockOrBiometric(activity)) {
            onUsePasswordFallback()
            return
        }

        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = try {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Chats")
                .setSubtitle("Use fingerprint, face, or device screen lock")
                .setAllowedAuthenticators(authenticators)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "PromptInfo failed for authenticators=$authenticators", e)
            onUsePasswordFallback()
            return
        }

        isBiometricPromptActive = true
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isBiometricPromptActive = false
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    isBiometricPromptActive = false
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> {
                            onUsePasswordFallback()
                        }
                        BiometricPrompt.ERROR_NO_BIOMETRICS,
                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                            onUsePasswordFallback()
                        }
                        else -> {
                            onError(errString.toString().ifBlank { "Authentication failed" })
                        }
                    }
                }
            }
        )

        try {
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            isBiometricPromptActive = false
            Log.w(TAG, "BiometricPrompt.authenticate failed", e)
            onUsePasswordFallback()
        }
    }

    /**
     * Debug-only helper to simulate 14-day vault password expiry immediately.
     */
    fun debugSimulate14DaysElapsed(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false
        val uid = ActiveUserManager.getActiveUid(context) ?: return false
        val fifteenDaysAgo = System.currentTimeMillis() - (PASSWORD_MAX_AGE_MS + 24L * 60L * 60L * 1000L)
        getPrefs(context, uid).edit()
            .putLong(PREF_LAST_PASSWORD_WALL_MS, fifteenDaysAgo)
            .apply()
        VaultSecurityManager.lock()
        refreshState(context, uid)
        return true
    }

    fun wipeForUid(context: Context, uid: String) {
        if (uid.isBlank()) return
        LegacySecurityMigration.deleteSharedPrefsCompat(context, prefsName(uid))
        deleteKeystoreAlias(rememberedKeyAlias(uid))
        deleteKeystoreAlias(bioProbeKeyAlias(uid))
        refreshState(context, uid)
    }

    private fun checkAndMarkBiometricChanges(context: Context, uid: String): Boolean {
        val prefs = getPrefs(context, uid)
        if (prefs.getBoolean(PREF_BIO_CHANGED_FLAG, false)) return true

        val hadBio = prefs.getBoolean(PREF_HAD_BIO_ENROLLED, false)
        val hasBioNow = hasBiometricEnrolled(context)
        if (hadBio && !hasBioNow) {
            invalidateRememberedKeyDueToBiometricChange(prefs, uid)
            return true
        }

        if (hasStrongBiometric(context)) {
            val probeValid = verifyBioProbeKeyStillValid(uid)
            if (!probeValid) {
                invalidateRememberedKeyDueToBiometricChange(prefs, uid)
                return true
            }
        }
        return false
    }

    private fun invalidateRememberedKeyDueToBiometricChange(prefs: SharedPreferences, uid: String) {
        prefs.edit()
            .putBoolean(PREF_BIO_CHANGED_FLAG, true)
            .remove(PREF_WRAPPED_VAULT_KEY)
            .apply()
        deleteKeystoreAlias(rememberedKeyAlias(uid))
        deleteKeystoreAlias(bioProbeKeyAlias(uid))
    }

    private fun wrapAndSaveVaultKey(context: Context, uid: String, vaultKey: ByteArray) {
        try {
            val wrapperKey = getOrCreateRememberedKeystoreKey(context, uid)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, wrapperKey)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(vaultKey)
            val payload = ByteBuffer.allocate(NONCE_SIZE_BYTES + ciphertext.size)
                .put(iv)
                .put(ciphertext)
                .array()
            val encoded = Base64.encodeToString(payload, Base64.NO_WRAP)
            getPrefs(context, uid).edit().putString(PREF_WRAPPED_VAULT_KEY, encoded).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed wrapping vault key with Keystore", e)
        }
    }

    private fun unwrapRememberedVaultKey(context: Context, uid: String): ByteArray? {
        val encoded = getPrefs(context, uid).getString(PREF_WRAPPED_VAULT_KEY, null) ?: return null
        return try {
            val payload = Base64.decode(encoded, Base64.DEFAULT)
            if (payload.size <= NONCE_SIZE_BYTES) return null
            val buffer = ByteBuffer.wrap(payload)
            val iv = ByteArray(NONCE_SIZE_BYTES)
            buffer.get(iv)
            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)

            val wrapperKey = getExistingKeystoreKey(rememberedKeyAlias(uid)) ?: return null
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, wrapperKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to unwrap remembered vault key from Keystore", e)
            null
        }
    }

    private fun getOrCreateRememberedKeystoreKey(context: Context, uid: String): SecretKey {
        val alias = rememberedKeyAlias(uid)
        getExistingKeystoreKey(alias)?.let { return it }

        val hasStrongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

        if (hasStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return generateAesGcmKey(alias, strongBox = true, requireUserAuth = false, invalidateOnBio = false)
            } catch (e: Exception) {
                Log.w(TAG, "StrongBox unavailable for $alias; falling back to TEE Keystore", e)
            }
        }
        return generateAesGcmKey(alias, strongBox = false, requireUserAuth = false, invalidateOnBio = false)
    }

    private fun ensureOrRecreateBioProbeKey(uid: String) {
        val alias = bioProbeKeyAlias(uid)
        if (verifyBioProbeKeyStillValid(uid)) return
        deleteKeystoreAlias(alias)
        try {
            generateAesGcmKey(alias, strongBox = false, requireUserAuth = true, invalidateOnBio = true)
        } catch (e: Exception) {
            Log.w(TAG, "Could not create biometric probe key", e)
        }
    }

    private fun verifyBioProbeKeyStillValid(uid: String): Boolean {
        val alias = bioProbeKeyAlias(uid)
        val key = getExistingKeystoreKey(alias) ?: return true
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            true
        } catch (_: KeyPermanentlyInvalidatedException) {
            false
        } catch (_: Exception) {
            true
        }
    }

    private fun generateAesGcmKey(
        alias: String,
        strongBox: Boolean,
        requireUserAuth: Boolean,
        invalidateOnBio: Boolean
    ): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(requireUserAuth)

        if (invalidateOnBio) {
            builder.setInvalidatedByBiometricEnrollment(true)
        }
        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    private fun getExistingKeystoreKey(alias: String): SecretKey? {
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (ks.containsAlias(alias)) {
                (ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun deleteKeystoreAlias(alias: String) {
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (ks.containsAlias(alias)) {
                ks.deleteEntry(alias)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed deleting Keystore alias $alias", e)
        }
    }

    private fun readBootCount(context: Context): Int {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        } catch (_: Exception) {
            -1
        }
    }
}
