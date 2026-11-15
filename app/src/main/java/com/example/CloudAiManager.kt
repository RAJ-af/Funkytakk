package com.example

import android.content.Context
import android.content.SharedPreferences
import com.example.db.EncryptedChatDatabase
import com.example.db.LocalMessageTranslation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AiResult(
    val text: String,
    val provider: String // "sarvam", "cerebras", "gemini", "on_device"
)

sealed class AiError(message: String) : Exception(message) {
    object ConsentRequired : AiError("AI Consent is required before using cloud features.")
    class RateLimited(msg: String = "Too many requests, try again in a minute / tomorrow") : AiError(msg)
    class TextTooLong(msg: String = "Text is too long (max 1,000 characters). Please shorten it.") : AiError(msg)
    class ServiceUnavailable(msg: String = "AI is unavailable right now") : AiError(msg)
    class Generic(msg: String) : AiError(msg)
}

object CloudAiManager {
    const val APP_AI_CONSENT_VERSION = 1
    private const val KEY_AI_CONSENT_ACCEPTED = "ai_consent_accepted"
    private const val KEY_AI_CONSENT_VERSION = "ai_consent_version"
    private const val KEY_MY_LANGUAGE = "ai_my_language"

    private val _isCloudAiEnabled = MutableStateFlow(false)
    val isCloudAiEnabled: StateFlow<Boolean> = _isCloudAiEnabled.asStateFlow()

    private val _myLanguage = MutableStateFlow("English")
    val myLanguage: StateFlow<String> = _myLanguage.asStateFlow()

    @Volatile
    private var initializedUid: String? = null

    private fun getPrefsForActiveUser(context: Context): SharedPreferences? {
        val uid = ActiveUserManager.getActiveUid(context) ?: return null
        return context.applicationContext.getSharedPreferences(
            ActiveUserManager.cloudAiPrefsName(uid),
            Context.MODE_PRIVATE
        )
    }

    fun init(context: Context) {
        val uid = ActiveUserManager.getActiveUid(context)
        if (uid == null) {
            resetForLogout()
            return
        }
        if (initializedUid == uid) return
        initializedUid = uid
        val prefs = context.applicationContext.getSharedPreferences(
            ActiveUserManager.cloudAiPrefsName(uid),
            Context.MODE_PRIVATE
        )
        val accepted = prefs.getBoolean(KEY_AI_CONSENT_ACCEPTED, false)
        val version = prefs.getInt(KEY_AI_CONSENT_VERSION, 0)
        _isCloudAiEnabled.value = accepted && version >= APP_AI_CONSENT_VERSION
        _myLanguage.value = prefs.getString(KEY_MY_LANGUAGE, "English") ?: "English"
    }

    fun resetForLogout() {
        initializedUid = null
        _isCloudAiEnabled.value = false
        _myLanguage.value = "English"
    }

    fun wipeForUid(context: Context, uid: String) {
        if (uid.isBlank()) return
        if (initializedUid == uid) {
            resetForLogout()
        }
        LegacySecurityMigration.deleteSharedPrefsCompat(
            context.applicationContext,
            ActiveUserManager.cloudAiPrefsName(uid)
        )
    }

    fun setMyLanguage(context: Context, language: String) {
        init(context)
        _myLanguage.value = language
        getPrefsForActiveUser(context)
            ?.edit()
            ?.putString(KEY_MY_LANGUAGE, language)
            ?.apply()
    }

    fun isConsentAcceptedLocally(context: Context): Boolean {
        init(context)
        val prefs = getPrefsForActiveUser(context) ?: return false
        val accepted = prefs.getBoolean(KEY_AI_CONSENT_ACCEPTED, false)
        val version = prefs.getInt(KEY_AI_CONSENT_VERSION, 0)
        return accepted && version >= APP_AI_CONSENT_VERSION
    }

    /**
     * Checks remote consent status via RPC get_ai_consent()
     */
    suspend fun checkConsentStatus(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            init(context)
            if (isConsentAcceptedLocally(context)) return@withContext true

            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext isConsentAcceptedLocally(context)

            val resp = SupabaseClient.api.getAiConsent(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token
            )

            if (resp.isSuccessful) {
                val body = resp.body()
                val accepted = body?.accepted == true
                val version = body?.consent_version ?: 0
                val valid = accepted && version >= APP_AI_CONSENT_VERSION

                getPrefsForActiveUser(context)
                    ?.edit()
                    ?.putBoolean(KEY_AI_CONSENT_ACCEPTED, valid)
                    ?.putInt(KEY_AI_CONSENT_VERSION, version)
                    ?.apply()
                _isCloudAiEnabled.value = valid
                valid
            } else {
                isConsentAcceptedLocally(context)
            }
        } catch (_: Exception) {
            isConsentAcceptedLocally(context)
        }
    }

    /**
     * Accepts AI consent via RPC accept_ai_consent(p_version = 1)
     */
    suspend fun acceptConsent(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val resp = SupabaseClient.api.acceptAiConsent(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = AcceptAiConsentRequest(p_version = APP_AI_CONSENT_VERSION)
            )

            if (resp.isSuccessful) {
                getPrefsForActiveUser(context)
                    ?.edit()
                    ?.putBoolean(KEY_AI_CONSENT_ACCEPTED, true)
                    ?.putInt(KEY_AI_CONSENT_VERSION, APP_AI_CONSENT_VERSION)
                    ?.apply()
                _isCloudAiEnabled.value = true
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string().orEmpty()
                Result.failure(Exception(err.ifBlank { "Failed to record consent" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Revokes AI consent via RPC revoke_ai_consent()
     */
    suspend fun revokeConsent(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)

            if (token != null) {
                try {
                    SupabaseClient.api.revokeAiConsent(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token
                    )
                } catch (_: Exception) {}
            }

            getPrefsForActiveUser(context)
                ?.edit()
                ?.putBoolean(KEY_AI_CONSENT_ACCEPTED, false)
                ?.putInt(KEY_AI_CONSENT_VERSION, 0)
                ?.apply()
            _isCloudAiEnabled.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Translates a message or text with caching in local Room DB.
     */
    suspend fun translateMessage(
        context: Context,
        messageId: String,
        text: String,
        targetLanguage: String = _myLanguage.value,
        forceCloud: Boolean = false,
        db: EncryptedChatDatabase? = null
    ): Result<AiResult> = withContext(Dispatchers.IO) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            return@withContext Result.failure(AiError.Generic("Text is empty"))
        }

        // 1. Check local translation cache first
        if (!forceCloud && db != null && messageId.isNotBlank()) {
            val cached = db.messageTranslationDao().getTranslation(messageId)
            if (cached != null && cached.targetLanguage.equals(targetLanguage, ignoreCase = true)) {
                return@withContext Result.success(AiResult(text = cached.translatedText, provider = cached.provider))
            }
        }

        // 2. Execute Translation call
        val result = executeAiRequest(
            context = context,
            request = AiAssistRequest(
                feature = "translate",
                text = cleanText.take(1000),
                target_language = targetLanguage
            )
        )

        result.onSuccess { aiRes ->
            if (db != null && messageId.isNotBlank()) {
                db.messageTranslationDao().insertOrUpdateTranslation(
                    LocalMessageTranslation(
                        messageId = messageId,
                        translatedText = aiRes.text,
                        targetLanguage = targetLanguage,
                        provider = aiRes.provider
                    )
                )
            }
        }

        result
    }

    /**
     * Improves draft message in composer (Fix grammar, Friendlier, More formal, Shorter)
     */
    suspend fun improveDraftText(
        context: Context,
        text: String,
        style: String, // "fix" | "friendly" | "formal" | "short"
        languageHint: String? = null
    ): Result<AiResult> = withContext(Dispatchers.IO) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            return@withContext Result.failure(AiError.Generic("Please enter a message to improve"))
        }
        if (cleanText.length > 1000) {
            return@withContext Result.failure(AiError.TextTooLong())
        }

        executeAiRequest(
            context = context,
            request = AiAssistRequest(
                feature = "improve",
                text = cleanText,
                style = style,
                language_hint = languageHint
            )
        )
    }

    /**
     * Executes the POST /functions/v1/ai-assist call with auto retry on 401
     */
    private suspend fun executeAiRequest(
        context: Context,
        request: AiAssistRequest,
        isRetry: Boolean = false
    ): Result<AiResult> {
        if (!isConsentAcceptedLocally(context)) {
            val hasRemoteConsent = checkConsentStatus(context)
            if (!hasRemoteConsent) {
                return Result.failure(AiError.ConsentRequired)
            }
        }

        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return Result.failure(AiError.Generic("Not authenticated"))

            val resp = SupabaseClient.api.callAiAssist(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = request
            )

            val code = resp.code()
            if (resp.isSuccessful) {
                val body = resp.body()
                val resultText = body?.text.orEmpty()
                val provider = body?.provider?.ifBlank { "sarvam" } ?: "sarvam"
                if (resultText.isBlank()) {
                    return Result.failure(AiError.ServiceUnavailable("No translation received"))
                }
                return Result.success(AiResult(text = resultText, provider = provider))
            }

            return when (code) {
                401 -> {
                    if (!isRetry) {
                        SupabaseSessionManager.ensureActiveSession(context)
                        executeAiRequest(context, request, isRetry = true)
                    } else {
                        Result.failure(AiError.Generic("Session expired. Please re-login."))
                    }
                }
                403 -> {
                    getPrefsForActiveUser(context)
                        ?.edit()
                        ?.putBoolean(KEY_AI_CONSENT_ACCEPTED, false)
                        ?.apply()
                    _isCloudAiEnabled.value = false
                    Result.failure(AiError.ConsentRequired)
                }
                429 -> Result.failure(AiError.RateLimited("Too many requests, try again in a minute / tomorrow"))
                400 -> Result.failure(AiError.TextTooLong("Text is too long (max 1,000 characters). Please shorten it."))
                503 -> Result.failure(AiError.ServiceUnavailable("AI is unavailable right now"))
                else -> {
                    val rawErr = resp.errorBody()?.string().orEmpty()
                    val mappedErr = mapAiError(rawErr)
                    Result.failure(AiError.Generic(mappedErr))
                }
            }
        } catch (e: Exception) {
            return Result.failure(AiError.Generic(e.message ?: "Failed to connect to AI service"))
        }
    }

    private fun mapAiError(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("rate_limited") || lower.contains("daily_limit") || lower.contains("too_many") ->
                "Too many requests, try again in a minute / tomorrow"
            lower.contains("invalid_text_length") || lower.contains("too long") ->
                "Text is too long (max 1,000 characters). Please shorten it."
            lower.contains("all_providers_failed") || lower.contains("ai_not_configured") || lower.contains("503") ->
                "AI is unavailable right now"
            lower.contains("consent_required") ->
                "AI Consent required"
            else -> {
                try {
                    val j = JSONObject(raw)
                    val m = j.optString("error").ifBlank { j.optString("message") }
                    if (m.isNotBlank()) return m
                } catch (_: Exception) {}
                "AI is unavailable right now"
            }
        }
    }
}
