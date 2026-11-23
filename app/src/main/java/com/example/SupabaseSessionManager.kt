package com.example

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

object SupabaseSessionManager {
    private const val TAG = "SupabaseSession"
    private const val PREFS_NAME = "supabase_session_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id" // Must always be Supabase Auth UUID

    private const val PROACTIVE_REFRESH_BUFFER_MS = 5 * 60 * 1000L
    private const val HARD_EXPIRY_BUFFER_MS = 60_000L
    private const val MAX_VERIFY_ATTEMPTS = 4
    private val VERIFY_BACKOFF_DELAYS_MS = longArrayOf(2000L, 4000L, 8000L)

    const val KIND_LOGIN = "login"
    const val KIND_REFRESH = "refresh"

    private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    // Single-flight guard so concurrent callers share one verify-firebase-token call
    private val refreshMutex = Mutex()

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    @Volatile
    private var cachedUserId: String? = null

    sealed class ExchangeResult {
        data class Success(
            val accessToken: String,
            val refreshToken: String?,
            val userId: String
        ) : ExchangeResult()

        data class DeviceRevoked(
            val message: String = com.example.repository.DeviceSessionRepository.DEVICE_REVOKED_MESSAGE
        ) : ExchangeResult()

        data class Failure(
            val httpCode: Int? = null,
            val code: String? = null,
            val message: String
        ) : ExchangeResult()
    }

    fun isValidUuid(str: String?): Boolean {
        if (str.isNullOrBlank()) return false
        return UUID_REGEX.matches(str.trim())
    }

    fun extractUserIdFromJwt(token: String?): String? {
        if (token.isNullOrBlank()) return null
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val decodedBytes = Base64.decode(
                    parts[1],
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
                )
                val json = JSONObject(String(decodedBytes, Charsets.UTF_8))
                val sub = json.optString("sub", "").trim()
                if (isValidUuid(sub)) {
                    sub
                } else {
                    val meta = json.optJSONObject("user_metadata")
                    val metaId = meta?.optString("id", "")?.trim()
                    val metaSub = meta?.optString("sub", "")?.trim()
                    when {
                        isValidUuid(metaId) -> metaId
                        isValidUuid(metaSub) -> metaSub
                        else -> null
                    }
                }
            } else null
        } catch (_: Exception) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Failed to extract sub claim from JWT")
            }
            null
        }
    }

    fun getTokenExpirationMillis(token: String?): Long? {
        if (token.isNullOrBlank()) return null
        if (token == BuildConfig.SUPABASE_ANON_KEY) return Long.MAX_VALUE
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val decodedBytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )
            val json = JSONObject(String(decodedBytes, Charsets.UTF_8))
            val exp = json.optLong("exp", 0L)
            if (exp <= 0L) null else exp * 1000L
        } catch (_: Exception) {
            null
        }
    }

    fun shouldRefreshToken(token: String?): Boolean {
        if (token.isNullOrBlank()) return true
        if (token == BuildConfig.SUPABASE_ANON_KEY) return false
        val expMs = getTokenExpirationMillis(token) ?: return true
        return expMs <= (System.currentTimeMillis() + PROACTIVE_REFRESH_BUFFER_MS)
    }

    fun isTokenExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true
        if (token == BuildConfig.SUPABASE_ANON_KEY) return false
        val expMs = getTokenExpirationMillis(token) ?: return true
        return expMs <= (System.currentTimeMillis() + HARD_EXPIRY_BUFFER_MS)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setSession(context: Context, accessToken: String?, refreshToken: String? = null, userId: String? = null) {
        // Never save anon key as authenticated access token
        val realAccessToken = if (accessToken == BuildConfig.SUPABASE_ANON_KEY) null else accessToken

        // Resolve the true Supabase Auth UUID
        val resolvedUuid = when {
            isValidUuid(userId) -> userId
            realAccessToken != null -> extractUserIdFromJwt(realAccessToken)
            else -> null
        }

        cachedAccessToken = realAccessToken
        cachedRefreshToken = refreshToken
        cachedUserId = resolvedUuid
        ActiveUserManager.setActiveUid(resolvedUuid)

        getPrefs(context).edit().apply {
            if (realAccessToken != null) {
                putString(KEY_ACCESS_TOKEN, realAccessToken)
            } else {
                remove(KEY_ACCESS_TOKEN)
            }
            if (refreshToken != null) {
                putString(KEY_REFRESH_TOKEN, refreshToken)
            } else {
                remove(KEY_REFRESH_TOKEN)
            }
            if (resolvedUuid != null) {
                putString(KEY_USER_ID, resolvedUuid)
            } else {
                remove(KEY_USER_ID)
            }
            apply()
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Session updated: hasAccessToken=${realAccessToken != null}, hasUuid=${resolvedUuid != null}")
        }
        try {
            com.example.realtime.VoiceRoomRealtimeManager.setAuthToken(realAccessToken)
        } catch (_: Exception) {}
    }

    fun getAccessToken(context: Context): String? {
        if (cachedAccessToken != null) {
            try {
                com.example.realtime.VoiceRoomRealtimeManager.setAuthToken(cachedAccessToken)
            } catch (_: Exception) {}
            return cachedAccessToken
        }
        val token = getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
        if (token == BuildConfig.SUPABASE_ANON_KEY) return null
        cachedAccessToken = token
        try {
            com.example.realtime.VoiceRoomRealtimeManager.setAuthToken(token)
        } catch (_: Exception) {}
        return token
    }

    fun getBearerToken(context: Context): String {
        val token = getAccessToken(context)
        return if (!token.isNullOrBlank()) "Bearer $token" else "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
    }

    /**
     * Returns the Bearer auth header for an authenticated user.
     * Returns null if unauthenticated (strictly never falls back to anon key).
     */
    fun getAuthenticatedBearerToken(context: Context): String? {
        val token = getAccessToken(context)
        if (token.isNullOrBlank() || token == BuildConfig.SUPABASE_ANON_KEY || isTokenExpired(token)) {
            return null
        }
        return "Bearer $token"
    }

    fun getUserId(context: Context): String? {
        if (cachedUserId != null && isValidUuid(cachedUserId)) {
            ActiveUserManager.setActiveUid(cachedUserId)
            return cachedUserId
        }

        val stored = getPrefs(context).getString(KEY_USER_ID, null)
        if (isValidUuid(stored)) {
            cachedUserId = stored
            ActiveUserManager.setActiveUid(stored)
            return stored
        }

        // Try extracting sub from stored access token
        val token = getAccessToken(context)
        val extractedUuid = extractUserIdFromJwt(token)
        if (extractedUuid != null) {
            cachedUserId = extractedUuid
            ActiveUserManager.setActiveUid(extractedUuid)
            getPrefs(context).edit().putString(KEY_USER_ID, extractedUuid).apply()
            return extractedUuid
        }

        return null
    }

    fun hasValidAuthenticatedSession(context: Context): Boolean {
        val token = getAccessToken(context)
        val uid = getUserId(context)
        return !token.isNullOrBlank() && !isTokenExpired(token) && isValidUuid(uid)
    }

    /**
     * Returns true if this device currently has a signed-in user identity (Supabase UUID,
     * stored token, or active Firebase user), even if the short-lived 1h JWT needs refresh.
     */
    fun hasStoredAccount(context: Context): Boolean {
        if (com.example.repository.DeviceSessionRepository.isRevocationInProgress()) return false
        val uid = getUserId(context)
        val token = getAccessToken(context)
        val firebaseAuthed = runCatching { FirebaseAuth.getInstance().currentUser != null }.getOrDefault(false)
        return isValidUuid(uid) || !token.isNullOrBlank() || firebaseAuthed
    }

    /**
     * Ensures an active, valid Supabase session using a single-flight Mutex.
     * Proactively refreshes when the JWT is within 5 minutes of `exp` using kind = "refresh".
     * Returns the Supabase Auth UUID, or null if unauthenticated.
     */
    suspend fun ensureActiveSession(context: Context): String? = withContext(Dispatchers.IO) {
        if (com.example.repository.DeviceSessionRepository.isRevocationInProgress()) {
            return@withContext null
        }
        val existingToken = getAccessToken(context)
        val existingUid = getUserId(context)

        if (!existingToken.isNullOrBlank() && !shouldRefreshToken(existingToken) && isValidUuid(existingUid)) {
            return@withContext existingUid
        }

        return@withContext refreshMutex.withLock {
            if (com.example.repository.DeviceSessionRepository.isRevocationInProgress()) {
                return@withLock null
            }
            // Re-check inside lock so queued callers reuse the freshly minted session
            val tokenAfterLock = getAccessToken(context)
            val uidAfterLock = getUserId(context)
            if (!tokenAfterLock.isNullOrBlank() && !shouldRefreshToken(tokenAfterLock) && isValidUuid(uidAfterLock)) {
                return@withLock uidAfterLock
            }
            refreshSessionFromFirebaseLocked(context, forceFirebaseRefresh = false)
        }
    }

    /**
     * Force-refreshes the Supabase session from Firebase (e.g., on HTTP 401 or before logout when expired)
     * with kind = "refresh". Concurrent 401 handlers serialize on [refreshMutex] and reuse the updated
     * token if already rotated.
     */
    suspend fun forceRefreshSession(context: Context): String? = withContext(Dispatchers.IO) {
        if (com.example.repository.DeviceSessionRepository.isRevocationInProgress()) {
            return@withContext null
        }
        val tokenBeforeWait = getAccessToken(context)
        return@withContext refreshMutex.withLock {
            if (com.example.repository.DeviceSessionRepository.isRevocationInProgress()) {
                return@withLock null
            }
            val currentToken = getAccessToken(context)
            val currentUid = getUserId(context)
            if (!currentToken.isNullOrBlank() &&
                currentToken != tokenBeforeWait &&
                !shouldRefreshToken(currentToken) &&
                isValidUuid(currentUid)
            ) {
                return@withLock currentUid
            }
            refreshSessionFromFirebaseLocked(context, forceFirebaseRefresh = true)
        }
    }

    /**
     * Exchanges an explicit Firebase ID token under the single-flight [refreshMutex]
     * with device metadata and [kind] ("login" after interactive sign-in, "refresh" otherwise).
     */
    suspend fun exchangeFirebaseToken(
        context: Context,
        firebaseIdToken: String,
        fallbackUserId: String? = null,
        kind: String = KIND_LOGIN
    ): ExchangeResult = withContext(Dispatchers.IO) {
        if (kind == KIND_LOGIN) {
            com.example.repository.DeviceSessionRepository.resetRevokedGuardForInteractiveSignIn()
        }
        val devicePayload = DeviceIdentity.getVerifyDevicePayload(context)
        return@withContext refreshMutex.withLock {
            val outcome = verifyTokenWithBackoff(
                firebaseIdToken = firebaseIdToken,
                devicePayload = devicePayload,
                kind = kind
            )
            when (outcome) {
                is ExchangeResult.Success -> {
                    val resolvedUid = outcome.userId.takeIf { isValidUuid(it) } ?: fallbackUserId
                    setSession(context, outcome.accessToken, outcome.refreshToken, resolvedUid)
                    val finalUid = getUserId(context) ?: resolvedUid ?: outcome.userId
                    ExchangeResult.Success(
                        accessToken = outcome.accessToken,
                        refreshToken = outcome.refreshToken,
                        userId = finalUid
                    )
                }
                is ExchangeResult.DeviceRevoked -> {
                    com.example.repository.DeviceSessionRepository.triggerLocalSignOutForRevocation(
                        context = context,
                        showNotice = true
                    )
                    outcome
                }
                is ExchangeResult.Failure -> outcome
            }
        }
    }

    internal suspend fun verifyTokenWithBackoff(
        firebaseIdToken: String,
        devicePayload: VerifyDevicePayload,
        kind: String,
        invokeVerify: suspend (VerifyFirebaseTokenRequest) -> retrofit2.Response<VerifyFirebaseTokenResponse> = { req ->
            SupabaseClient.api.verifyFirebaseToken(request = req)
        }
    ): ExchangeResult {
        var lastCode: Int? = null
        var lastErrorCode: String? = null
        var lastMessage = com.example.repository.DeviceSessionRepository.SESSION_MINT_FAILED_MESSAGE

        for (attempt in 0 until MAX_VERIFY_ATTEMPTS) {
            if (attempt > 0) {
                delay(VERIFY_BACKOFF_DELAYS_MS[(attempt - 1).coerceIn(VERIFY_BACKOFF_DELAYS_MS.indices)])
            }
            try {
                val verifyRes = invokeVerify(
                    VerifyFirebaseTokenRequest(
                        firebase_token = firebaseIdToken,
                        device = devicePayload,
                        kind = kind
                    )
                )
                if (verifyRes.isSuccessful && verifyRes.body() != null) {
                    val body = verifyRes.body()!!
                    val accessToken = body.access_token
                    val refreshToken = body.refresh_token
                    val userId = body.user_id ?: body.user?.id ?: extractUserIdFromJwt(accessToken)
                    if (!accessToken.isNullOrBlank() && !userId.isNullOrBlank()) {
                        return ExchangeResult.Success(
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            userId = userId
                        )
                    }
                    lastCode = verifyRes.code()
                    lastErrorCode = body.code
                    lastMessage = body.error ?: com.example.repository.DeviceSessionRepository.SESSION_MINT_FAILED_MESSAGE
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "verify-firebase-token attempt ${attempt + 1} missing access_token")
                    }
                } else {
                    lastCode = verifyRes.code()
                    val rawErrorBody = try { verifyRes.errorBody()?.string() } catch (_: Exception) { null }
                    lastErrorCode = com.example.repository.DeviceSessionRepository.extractErrorCode(rawErrorBody)

                    if (lastCode == 403 && lastErrorCode == "device_revoked") {
                        Log.w(TAG, "verify-firebase-token returned HTTP 403 device_revoked; stopping immediately")
                        return ExchangeResult.DeviceRevoked()
                    }

                    lastMessage = when {
                        lastCode == 503 || lastErrorCode == "session_mint_failed" ->
                            com.example.repository.DeviceSessionRepository.SESSION_MINT_FAILED_MESSAGE
                        lastCode == 401 -> "Session expired. Please sign in again."
                        lastCode in 500..599 -> com.example.repository.DeviceSessionRepository.SESSION_MINT_FAILED_MESSAGE
                        else -> "Couldn't sign in. Try again."
                    }
                    Log.w(
                        TAG,
                        "verify-firebase-token HTTP $lastCode code=${lastErrorCode ?: "none"} on attempt ${attempt + 1}/$MAX_VERIFY_ATTEMPTS"
                    )
                    if (lastCode != 503 && lastCode !in 500..599) {
                        return ExchangeResult.Failure(lastCode, lastErrorCode, lastMessage)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastMessage = com.example.util.ErrorMapper.map(e)
                Log.w(TAG, "verify-firebase-token exception on attempt ${attempt + 1}/$MAX_VERIFY_ATTEMPTS: ${e.javaClass.simpleName}")
                if (attempt >= MAX_VERIFY_ATTEMPTS - 1) {
                    return ExchangeResult.Failure(lastCode, lastErrorCode, lastMessage)
                }
            }
        }
        return ExchangeResult.Failure(lastCode, lastErrorCode, lastMessage)
    }

    private suspend fun refreshSessionFromFirebaseLocked(
        context: Context,
        forceFirebaseRefresh: Boolean
    ): String? {
        val firebaseUser = try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }
        if (firebaseUser == null) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "ensureActiveSession: No Firebase user logged in")
            }
            return null
        }

        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Refreshing Supabase session from Firebase token (force=$forceFirebaseRefresh)")
            }
            val idToken = withTimeoutOrNull(12000L) {
                firebaseUser.getIdToken(forceFirebaseRefresh).await()?.token
            }

            if (idToken.isNullOrBlank()) {
                Log.e(TAG, "ensureActiveSession: Firebase getIdToken returned null")
                return null
            }

            val exchangeResult = verifyTokenWithBackoff(
                firebaseIdToken = idToken,
                devicePayload = DeviceIdentity.getVerifyDevicePayload(context),
                kind = KIND_REFRESH
            )

            if (exchangeResult is ExchangeResult.DeviceRevoked) {
                com.example.repository.DeviceSessionRepository.triggerLocalSignOutForRevocation(
                    context = context,
                    showNotice = true
                )
                return null
            }

            // Abort without writing the session if FirebaseAuth.currentUser is now null
            val currentFirebaseUser = try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }
            if (currentFirebaseUser == null || currentFirebaseUser.uid != firebaseUser.uid) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "FirebaseAuth.currentUser changed during refresh; aborting session write")
                }
                return null
            }

            if (exchangeResult is ExchangeResult.Success) {
                setSession(context, exchangeResult.accessToken, exchangeResult.refreshToken, exchangeResult.userId)
                val resolvedId = getUserId(context)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Established Supabase session successfully")
                }
                return resolvedId
            } else if (exchangeResult is ExchangeResult.Failure) {
                Log.e(TAG, "Session refresh failed: code=${exchangeResult.httpCode}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception refreshing session: ${e.javaClass.simpleName}")
        }

        val postCheckFirebaseUser = try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }
        if (postCheckFirebaseUser == null) {
            return null
        }

        val activeToken = getAccessToken(context)
        val activeUid = getUserId(context)
        if (!activeToken.isNullOrBlank() && !isTokenExpired(activeToken) && isValidUuid(activeUid)) {
            return activeUid
        }
        return null
    }

    fun clearSession(context: Context) {
        cachedAccessToken = null
        cachedRefreshToken = null
        cachedUserId = null
        ActiveUserManager.clear()
        getPrefs(context).edit().clear().commit()
        try {
            NotificationsManager.clear()
        } catch (_: Exception) {}
    }
}
