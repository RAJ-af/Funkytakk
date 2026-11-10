package com.example.realtime

import android.content.Context
import android.util.Log
import com.example.AuthViewModel
import com.example.BuildConfig
import com.example.SupabaseClient
import com.example.SupabaseProfile
import com.example.SupabaseSessionManager
import com.example.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class CachedVoiceProfile(
    val userId: String,
    val name: String,
    val avatar: String,
    val countryCode: String = "in",
    val username: String? = null
)

/**
 * Manages participant profiles for Voice Room (seats & People sheet).
 * 
 * Requirements:
 * (1) Authenticated Supabase client/session (never anonymous key or raw HTTP without user auth header).
 * (2) On HTTP 401, refresh session via forceRefreshSession once and retry immediately.
 * (3) Cache last known name and avatar per user id, showing them while loading, and falling back
 *     to role label ("Learner" / "Speaker") only if no name was ever loaded.
 * (4) Immediate fetch whenever a new participant joins the room.
 * (5) Log HTTP status code and response body on failure with Log.e.
 */
object VoiceRoomProfileManager {
    private const val TAG = "VoiceRoomProfileMgr"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // In-memory cache of user profiles (persists across room events)
    private val profileCache = ConcurrentHashMap<String, CachedVoiceProfile>()
    private val fetchingUserIds = ConcurrentHashMap.newKeySet<String>()

    private val _profilesFlow = MutableStateFlow<Map<String, CachedVoiceProfile>>(emptyMap())
    val profilesFlow: StateFlow<Map<String, CachedVoiceProfile>> = _profilesFlow.asStateFlow()

    fun getCachedProfile(userId: String): CachedVoiceProfile? = profileCache[userId]

    fun recordProfile(
        userId: String,
        name: String?,
        avatar: String?,
        countryCode: String? = null,
        username: String? = null
    ) {
        if (userId.isBlank()) return
        val current = profileCache[userId]
        val cleanName = name?.trim()?.takeIf { it.isNotBlank() && it != "Learner" && it != "Speaker" && it != "Listener" && it != "User" && it != "Host" }
            ?: current?.name
        val cleanAvatar = avatar?.trim()?.takeIf { it.isNotBlank() }
            ?: current?.avatar
        val cleanCountry = countryCode?.trim()?.takeIf { it.isNotBlank() }
            ?: current?.countryCode
            ?: "in"
        val cleanUsername = username?.trim()?.takeIf { it.isNotBlank() }
            ?: current?.username

        if (cleanName != null || cleanAvatar != null) {
            val updated = CachedVoiceProfile(
                userId = userId,
                name = cleanName ?: current?.name ?: "",
                avatar = cleanAvatar ?: current?.avatar ?: "",
                countryCode = cleanCountry,
                username = cleanUsername
            )
            profileCache[userId] = updated
            _profilesFlow.value = HashMap(profileCache)
            // Also keep RealtimeManager snippet synced
            try {
                VoiceRoomRealtimeManager.recordUserSnippet(userId, cleanName, cleanAvatar, cleanCountry)
            } catch (_: Exception) {}
        }
    }

    /**
     * Trigger immediate profile fetch for a participant.
     */
    fun loadParticipantProfile(context: Context, userId: String, authViewModel: AuthViewModel? = null) {
        if (userId.isBlank() || fetchingUserIds.contains(userId)) return
        val existing = profileCache[userId]
        if (existing != null && existing.name.isNotBlank() && existing.avatar.isNotBlank()) {
            return // already cached
        }

        fetchingUserIds.add(userId)
        scope.launch {
            try {
                fetchSingleProfileWithRetry(context, userId, authViewModel)
            } finally {
                fetchingUserIds.remove(userId)
            }
        }
    }

    /**
     * Batch fetch participant profiles for all active seats & listeners.
     */
    fun loadParticipants(context: Context, userIds: List<String>, authViewModel: AuthViewModel? = null) {
        val needed = userIds.filter { uid ->
            uid.isNotBlank() && !profileCache.containsKey(uid) && !fetchingUserIds.contains(uid)
        }
        if (needed.isEmpty()) return

        needed.forEach { fetchingUserIds.add(it) }
        scope.launch {
            try {
                fetchBatchProfilesWithRetry(context, needed, authViewModel)
            } finally {
                needed.forEach { fetchingUserIds.remove(it) }
            }
        }
    }

    private suspend fun fetchSingleProfileWithRetry(
        context: Context,
        userId: String,
        authViewModel: AuthViewModel?
    ) = withContext(Dispatchers.IO) {
        // Step 1: Ensure valid authenticated session
        SupabaseSessionManager.ensureActiveSession(context)
        var token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            ?: SupabaseSessionManager.getBearerToken(context)

        Log.d(TAG, "Fetching profile for user $userId using authenticated token (prefix=${token.take(15)}...)")
        var resp = try {
            SupabaseClient.api.getProfile(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                authUserIdEq = "eq.$userId"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling getProfile for user $userId: ${e.message}", e)
            null
        }

        // Step 2: On HTTP 401, refresh session once and retry
        if (resp != null && resp.code() == 401) {
            val errBody = resp.errorBody()?.string().orEmpty()
            Log.e(TAG, "GET /rest/v1/profiles returned 401 for user $userId: $errBody. Refreshing session and retrying...")
            SupabaseSessionManager.forceRefreshSession(context)
            val refreshedToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: SupabaseSessionManager.getBearerToken(context)
            token = refreshedToken
            resp = try {
                SupabaseClient.api.getProfile(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    authUserIdEq = "eq.$userId"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Retry exception calling getProfile for user $userId: ${e.message}", e)
                null
            }
        }

        if (resp != null && resp.isSuccessful) {
            val list = resp.body().orEmpty()
            val profile = list.firstOrNull()
            if (profile != null) {
                val name = profile.profile_name?.takeIf { it.isNotBlank() }
                    ?: profile.username?.takeIf { it.isNotBlank() }
                    ?: ""
                val avatar = profile.avatar_url?.takeIf { it.isNotBlank() } ?: ""
                val country = profile.country_code?.takeIf { it.isNotBlank() } ?: "in"

                recordProfile(userId, name, avatar, country, profile.username)
                Log.d(TAG, "Successfully loaded profile for user $userId: name='$name', avatar='$avatar'")

                // Also update AuthViewModel's global list if available
                if (authViewModel != null) {
                    val converted = UserProfile(
                        uid = profile.auth_user_id ?: userId,
                        email = profile.email ?: "",
                        displayName = name.ifBlank { null },
                        isEmailVerified = true,
                        username = profile.username,
                        avatar = avatar.ifBlank { null },
                        country = profile.country,
                        countryCode = country
                    )
                    authViewModel.updateOrAddProfile(converted)
                }
            } else {
                Log.w(TAG, "Profile for user $userId not found in database (empty list)")
            }
        } else if (resp != null) {
            val code = resp.code()
            val errBody = resp.errorBody()?.string().orEmpty()
            Log.e(TAG, "Failed to load profile for user $userId (HTTP $code): $errBody")
        }
    }

    private suspend fun fetchBatchProfilesWithRetry(
        context: Context,
        userIds: List<String>,
        authViewModel: AuthViewModel?
    ) = withContext(Dispatchers.IO) {
        SupabaseSessionManager.ensureActiveSession(context)
        var token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            ?: SupabaseSessionManager.getBearerToken(context)

        val inQuery = "in.(${userIds.joinToString(",")})"
        Log.d(TAG, "Batch fetching profiles for ${userIds.size} users: $inQuery")

        var resp = try {
            SupabaseClient.api.getProfilesIn(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                authUserIdIn = inQuery
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling getProfilesIn: ${e.message}", e)
            null
        }

        // On 401, refresh session and retry once
        if (resp != null && resp.code() == 401) {
            val errBody = resp.errorBody()?.string().orEmpty()
            Log.e(TAG, "GET /rest/v1/profiles returned 401 for batch query: $errBody. Refreshing session and retrying...")
            SupabaseSessionManager.forceRefreshSession(context)
            val refreshedToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: SupabaseSessionManager.getBearerToken(context)
            token = refreshedToken
            resp = try {
                SupabaseClient.api.getProfilesIn(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    authUserIdIn = inQuery
                )
            } catch (e: Exception) {
                Log.e(TAG, "Retry exception calling getProfilesIn: ${e.message}", e)
                null
            }
        }

        if (resp != null && resp.isSuccessful) {
            val list = resp.body().orEmpty()
            Log.d(TAG, "Loaded ${list.size} profiles from batch query")
            list.forEach { p ->
                val uid = p.auth_user_id ?: ""
                if (uid.isNotBlank()) {
                    val name = p.profile_name?.takeIf { it.isNotBlank() }
                        ?: p.username?.takeIf { it.isNotBlank() }
                        ?: ""
                    val avatar = p.avatar_url?.takeIf { it.isNotBlank() } ?: ""
                    val country = p.country_code?.takeIf { it.isNotBlank() } ?: "in"
                    recordProfile(uid, name, avatar, country, p.username)

                    if (authViewModel != null) {
                        val converted = UserProfile(
                            uid = uid,
                            email = p.email ?: "",
                            displayName = name.ifBlank { null },
                            isEmailVerified = true,
                            username = p.username,
                            avatar = avatar.ifBlank { null },
                            country = p.country,
                            countryCode = country
                        )
                        authViewModel.updateOrAddProfile(converted)
                    }
                }
            }
        } else if (resp != null) {
            val code = resp.code()
            val errBody = resp.errorBody()?.string().orEmpty()
            Log.e(TAG, "Failed batch getProfilesIn (HTTP $code): $errBody")
        }
    }
}
