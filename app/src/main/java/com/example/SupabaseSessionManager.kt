package com.example

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
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

    private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    @Volatile
    private var cachedUserId: String? = null

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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract sub from JWT: ${e.message}")
            null
        }
    }

    fun isTokenExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true
        if (token == BuildConfig.SUPABASE_ANON_KEY) return false
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val decodedBytes = Base64.decode(
                    parts[1],
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
                )
                val json = JSONObject(String(decodedBytes, Charsets.UTF_8))
                val exp = json.optLong("exp", 0L)
                if (exp == 0L) false
                else (exp * 1000L) <= (System.currentTimeMillis() + 60_000L) // expired or expiring in 60s
            } else true
        } catch (e: Exception) {
            true
        }
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

        Log.d(TAG, "Session updated: hasAccessToken=${realAccessToken != null}, resolvedUuid=$resolvedUuid")
    }

    fun getAccessToken(context: Context): String? {
        if (cachedAccessToken != null) return cachedAccessToken
        val token = getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
        if (token == BuildConfig.SUPABASE_ANON_KEY) return null
        cachedAccessToken = token
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
        if (cachedUserId != null && isValidUuid(cachedUserId)) return cachedUserId

        val stored = getPrefs(context).getString(KEY_USER_ID, null)
        if (isValidUuid(stored)) {
            cachedUserId = stored
            return stored
        }

        // Try extracting sub from stored access token
        val token = getAccessToken(context)
        val extractedUuid = extractUserIdFromJwt(token)
        if (extractedUuid != null) {
            cachedUserId = extractedUuid
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
     * Ensures an active, valid Supabase session. If the token is missing or expired,
     * uses the current Firebase user token to mint a real Supabase session.
     * Returns the Supabase Auth UUID, or null if unauthenticated.
     */
    suspend fun ensureActiveSession(context: Context): String? = withContext(Dispatchers.IO) {
        val existingToken = getAccessToken(context)
        val existingUid = getUserId(context)

        if (!existingToken.isNullOrBlank() && !isTokenExpired(existingToken) && isValidUuid(existingUid)) {
            return@withContext existingUid
        }

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            Log.w(TAG, "ensureActiveSession: No Firebase user logged in")
            return@withContext null
        }

        try {
            Log.d(TAG, "ensureActiveSession: Refreshing Supabase session from Firebase token...")
            val idToken = withTimeoutOrNull(12000L) {
                firebaseUser.getIdToken(false).await()?.token
            }

            if (idToken.isNullOrBlank()) {
                Log.e(TAG, "ensureActiveSession: Firebase getIdToken returned null")
                return@withContext null
            }

            val verifyRes = SupabaseClient.api.verifyFirebaseToken(
                request = VerifyFirebaseTokenRequest(
                    firebase_token = idToken,
                    firebaseIdToken = idToken
                )
            )

            if (verifyRes.isSuccessful && verifyRes.body() != null) {
                val body = verifyRes.body()!!
                val accessToken = body.access_token
                val refreshToken = body.refresh_token
                val userId = body.user_id ?: body.user?.id ?: extractUserIdFromJwt(accessToken)

                if (!accessToken.isNullOrBlank()) {
                    setSession(context, accessToken, refreshToken, userId)
                    val resolvedId = getUserId(context)
                    Log.d(TAG, "ensureActiveSession: Successfully established Supabase session, uuid=$resolvedId")
                    return@withContext resolvedId
                } else {
                    Log.e(TAG, "ensureActiveSession: verifyFirebaseToken response missing access_token")
                }
            } else {
                val err = verifyRes.errorBody()?.string() ?: verifyRes.message()
                Log.e(TAG, "ensureActiveSession: verifyFirebaseToken failed (code ${verifyRes.code()}): $err")
            }
        } catch (e: Exception) {
            Log.e(TAG, "ensureActiveSession: Exception refreshing session", e)
        }

        val activeToken = getAccessToken(context)
        val activeUid = getUserId(context)
        if (!activeToken.isNullOrBlank() && !isTokenExpired(activeToken) && isValidUuid(activeUid)) {
            return@withContext activeUid
        }
        return@withContext null
    }

    fun clearSession(context: Context) {
        cachedAccessToken = null
        cachedRefreshToken = null
        cachedUserId = null
        getPrefs(context).edit().clear().apply()
    }
}
