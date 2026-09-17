package com.example

import android.content.Context
import android.content.SharedPreferences

object SupabaseSessionManager {
    private const val PREFS_NAME = "supabase_session_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    @Volatile
    private var cachedUserId: String? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setSession(context: Context, accessToken: String, refreshToken: String? = null, userId: String? = null) {
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken
        cachedUserId = userId

        getPrefs(context).edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_ID, userId)
            apply()
        }
    }

    fun getAccessToken(context: Context): String? {
        if (cachedAccessToken != null) return cachedAccessToken
        val token = getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
        cachedAccessToken = token
        return token
    }

    fun getBearerToken(context: Context): String {
        val token = getAccessToken(context)
        return if (!token.isNullOrBlank()) "Bearer $token" else "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
    }

    fun getUserId(context: Context): String? {
        if (cachedUserId != null) return cachedUserId
        val uid = getPrefs(context).getString(KEY_USER_ID, null)
        cachedUserId = uid
        return uid
    }

    fun clearSession(context: Context) {
        cachedAccessToken = null
        cachedRefreshToken = null
        cachedUserId = null
        getPrefs(context).edit().clear().apply()
    }
}
