package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.SupabaseSessionManager
import com.example.UserProfile
import com.example.util.ChatBackupPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val Context.startupDataStore: DataStore<Preferences> by preferencesDataStore(name = "funkytalk_startup_prefs")

data class LocalStartupSnapshot(
    val onboardingDone: Boolean,
    val privacyAccepted: Boolean,
    val lastUid: String?
)

/**
 * Local-only DataStore + fast SharedPreferences mirror used to decide the start destination
 * in milliseconds without waiting on any network call.
 */
object StartupPrefsStore {
    private const val FAST_PREFS_NAME = "funky_startup_fast_prefs"
    private const val LEGACY_PREFS_NAME = "funky_talk_prefs"

    private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    private val KEY_PRIVACY_ACCEPTED = booleanPreferencesKey("privacy_accepted")
    private val KEY_LAST_UID = stringPreferencesKey("last_uid")

    fun getFastSnapshot(context: Context): LocalStartupSnapshot {
        val fast = context.applicationContext.getSharedPreferences(FAST_PREFS_NAME, Context.MODE_PRIVATE)
        val legacy = context.applicationContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val firebaseUid = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
        val storedUid = fast.getString("last_uid", null)
            ?: SupabaseSessionManager.getUserId(context)
            ?: firebaseUid
        val legacyOnboarding = listOfNotNull(storedUid, firebaseUid)
            .distinct()
            .any { uid -> legacy.getBoolean("profile_onboarding_complete_$uid", false) }
        val onboardingDone = fast.getBoolean("onboarding_done", false) || legacyOnboarding
        val privacyAccepted = fast.getBoolean("privacy_accepted", false) ||
            SupabaseSessionManager.hasStoredAccount(context) ||
            onboardingDone
        return LocalStartupSnapshot(
            onboardingDone = onboardingDone,
            privacyAccepted = privacyAccepted,
            lastUid = storedUid
        )
    }

    suspend fun readSnapshot(context: Context): LocalStartupSnapshot = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        val fastSnap = getFastSnapshot(appCtx)
        return@withContext try {
            val prefs = appCtx.startupDataStore.data.first()
            val onboarding = (prefs[KEY_ONBOARDING_DONE] ?: false) || fastSnap.onboardingDone
            val privacy = (prefs[KEY_PRIVACY_ACCEPTED] ?: false) || fastSnap.privacyAccepted
            val uid = prefs[KEY_LAST_UID]?.takeIf { it.isNotBlank() } ?: fastSnap.lastUid
            syncFastMirror(appCtx, onboarding, privacy, uid)
            LocalStartupSnapshot(
                onboardingDone = onboarding,
                privacyAccepted = privacy,
                lastUid = uid
            )
        } catch (_: Exception) {
            fastSnap
        }
    }

    suspend fun markPrivacyAccepted(context: Context) = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        val current = getFastSnapshot(appCtx)
        syncFastMirror(appCtx, current.onboardingDone, true, current.lastUid)
        try {
            appCtx.startupDataStore.edit { prefs ->
                prefs[KEY_PRIVACY_ACCEPTED] = true
            }
        } catch (_: Exception) {
        }
    }

    suspend fun markOnboardingDone(context: Context, uid: String?) = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        val safeUid = uid?.takeIf { it.isNotBlank() }
        syncFastMirror(appCtx, true, true, safeUid)
        try {
            appCtx.startupDataStore.edit { prefs ->
                prefs[KEY_ONBOARDING_DONE] = true
                prefs[KEY_PRIVACY_ACCEPTED] = true
                if (!safeUid.isNullOrBlank()) {
                    prefs[KEY_LAST_UID] = safeUid
                }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun recordActiveUid(context: Context, uid: String?, onboardingDone: Boolean? = null) =
        withContext(Dispatchers.IO) {
            val appCtx = context.applicationContext
            val safeUid = uid?.takeIf { it.isNotBlank() } ?: return@withContext
            val current = getFastSnapshot(appCtx)
            val finalOnboarding = onboardingDone ?: current.onboardingDone
            syncFastMirror(appCtx, finalOnboarding, true, safeUid)
            try {
                appCtx.startupDataStore.edit { prefs ->
                    prefs[KEY_LAST_UID] = safeUid
                    prefs[KEY_PRIVACY_ACCEPTED] = true
                    if (onboardingDone != null) {
                        prefs[KEY_ONBOARDING_DONE] = onboardingDone
                    }
                }
            } catch (_: Exception) {
            }
        }

    fun clearSessionOnLogout(context: Context) {
        val appCtx = context.applicationContext
        val current = getFastSnapshot(appCtx)
        syncFastMirror(
            context = appCtx,
            onboardingDone = false,
            privacyAccepted = current.privacyAccepted,
            lastUid = null
        )
    }

    suspend fun clearSessionOnLogoutSuspend(context: Context) = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        clearSessionOnLogout(appCtx)
        try {
            appCtx.startupDataStore.edit { prefs ->
                prefs[KEY_ONBOARDING_DONE] = false
                prefs.remove(KEY_LAST_UID)
            }
        } catch (_: Exception) {
        }
    }

    fun buildLocalCachedUserProfile(context: Context, userId: String, fallbackEmail: String = ""): UserProfile {
        val prefs = context.applicationContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val supaUid = SupabaseSessionManager.getUserId(context)
        val keysUid = if (!supaUid.isNullOrBlank()) supaUid else userId

        fun str(key: String): String? =
            prefs.getString("${key}_$userId", null)
                ?: prefs.getString("${key}_$keysUid", null)

        val ageVal = prefs.getInt("profile_age_$userId", 0)
            .takeIf { it > 0 }
            ?: prefs.getInt("profile_age_$keysUid", 0).takeIf { it > 0 }

        val hobbiesRaw = str("profile_hobbies")
        val hobbies = hobbiesRaw?.split("||")?.filter { it.isNotBlank() }.orEmpty()
        val onboardingDone = prefs.getBoolean("profile_onboarding_complete_$userId", false) ||
            prefs.getBoolean("profile_onboarding_complete_$keysUid", false)
        val emailVerified = prefs.getBoolean("custom_email_verified_$userId", false) ||
            prefs.getBoolean("custom_email_verified_$keysUid", false) ||
            onboardingDone

        return UserProfile(
            uid = userId,
            email = fallbackEmail,
            displayName = str("profile_displayName"),
            isEmailVerified = emailVerified,
            username = str("profile_username"),
            avatar = str("profile_avatar"),
            coverUrl = str("profile_cover"),
            age = ageVal,
            dob = str("profile_dob"),
            bio = str("profile_bio") ?: str("profile_dob"),
            city = str("profile_city"),
            gender = str("profile_gender"),
            nativeLanguage = str("profile_nativeLanguage"),
            learningLanguage = str("profile_learningLanguage"),
            country = str("profile_country"),
            countryCode = str("profile_countryCode"),
            hobbies = hobbies,
            onboardingComplete = onboardingDone
        )
    }

    fun resolveOnboardingRoute(user: UserProfile): String {
        return when {
            user.username.isNullOrBlank() || user.displayName.isNullOrBlank() -> "onboarding_profile"
            (user.age == null || user.age <= 0) && user.dob.isNullOrBlank() -> "onboarding_age"
            user.gender.isNullOrBlank() -> "onboarding_gender"
            user.country.isNullOrBlank() -> "onboarding_country"
            user.nativeLanguage.isNullOrBlank() -> "onboarding_native_language"
            user.learningLanguage.isNullOrBlank() -> "onboarding_learning_language"
            else -> "onboarding_hobbies"
        }
    }

    /**
     * Resolves an unauthenticated start destination synchronously from DataStore snapshot.
     */
    fun resolveUnauthenticatedRoute(snapshot: LocalStartupSnapshot): String {
        return if (snapshot.privacyAccepted) "auth" else "welcome"
    }

    /**
     * Determines whether backup restore gate should be shown without touching UI thread DB work.
     */
    suspend fun shouldShowBackupRestore(context: Context): Boolean = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        val hasSkipped = ChatBackupPreferencesManager.hasSkippedRestore(appCtx)
        if (hasSkipped) return@withContext false
        val isVaultConfigured = com.example.crypto.VaultSecurityManager.isVaultConfigured(appCtx)
        if (!isVaultConfigured) return@withContext true
        val activeKey = com.example.crypto.VaultSecurityManager.getActiveVaultKey() ?: return@withContext false
        return@withContext try {
            val db = com.example.db.EncryptedChatDatabase.getInstance(appCtx, activeKey)
            db.conversationDao().getAllConversationsList().isEmpty() &&
                db.groupConversationDao().getAllJoinedGroupsList().isEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private fun syncFastMirror(
        context: Context,
        onboardingDone: Boolean,
        privacyAccepted: Boolean,
        lastUid: String?
    ) {
        val fast = context.applicationContext.getSharedPreferences(FAST_PREFS_NAME, Context.MODE_PRIVATE)
        fast.edit().apply {
            putBoolean("onboarding_done", onboardingDone)
            putBoolean("privacy_accepted", privacyAccepted)
            if (!lastUid.isNullOrBlank()) {
                putString("last_uid", lastUid)
            } else {
                remove("last_uid")
            }
            apply()
        }
    }
}
