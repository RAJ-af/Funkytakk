package com.example

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

object TelemetryManager {
    private const val TAG = "TelemetryManager"
    private const val PREFS_NAME = "funky_talk_prefs"
    private const val KEY_SHARE_USAGE_DATA = "share_anonymous_usage_data"

    private val _isUsageDataEnabled = MutableStateFlow(true)
    val isUsageDataEnabled: StateFlow<Boolean> = _isUsageDataEnabled.asStateFlow()

    @Volatile
    private var appContext: Context? = null

    private fun analytics(): FirebaseAnalytics? {
        val ctx = appContext ?: return null
        return try {
            if (FirebaseApp.getApps(ctx).isEmpty()) return null
            FirebaseAnalytics.getInstance(ctx)
        } catch (_: Exception) {
            null
        }
    }

    private fun crashlytics(): FirebaseCrashlytics? {
        val ctx = appContext ?: return null
        return try {
            if (FirebaseApp.getApps(ctx).isEmpty()) return null
            FirebaseCrashlytics.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = appContext!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_SHARE_USAGE_DATA, true)
        _isUsageDataEnabled.value = enabled
        applyCollectionFlags(enabled)
        try {
            crashlytics()?.apply {
                setCustomKey("app_version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
                setCustomKey("current_screen", "splash")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Crashlytics init key setup note: ${e.message}")
        }
    }

    fun setUsageDataEnabled(context: Context, enabled: Boolean) {
        appContext = context.applicationContext
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHARE_USAGE_DATA, enabled)
            .apply()
        _isUsageDataEnabled.value = enabled
        applyCollectionFlags(enabled)
    }

    private fun applyCollectionFlags(enabled: Boolean) {
        try {
            analytics()?.setAnalyticsCollectionEnabled(enabled)
        } catch (_: Exception) {}
        try {
            crashlytics()?.setCrashlyticsCollectionEnabled(enabled && !BuildConfig.DEBUG)
        } catch (_: Exception) {}
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    fun onLogin(uid: String) {
        if (uid.isBlank()) return
        val hashedUid = sha256Hex(uid)
        try {
            crashlytics()?.setUserId(hashedUid)
            analytics()?.setUserId(hashedUid)
        } catch (_: Exception) {}
    }

    fun onLogout() {
        try {
            crashlytics()?.setUserId("")
            analytics()?.setUserId(null)
        } catch (_: Exception) {}
    }

    @Volatile
    private var lastScreenName: String = "splash"

    fun currentScreenName(): String = lastScreenName

    fun setCurrentScreen(screen: String) {
        if (screen.isBlank()) return
        lastScreenName = screen
        try {
            crashlytics()?.setCustomKey("current_screen", screen)
        } catch (_: Exception) {}
        if (!_isUsageDataEnabled.value) return
        logEvent("screen_view", mapOf("current_screen" to screen))
    }

    fun recordNonFatal(tag: String, throwable: Throwable) {
        try {
            crashlytics()?.apply {
                setCustomKey("error_tag", tag.take(100))
                recordException(throwable)
            }
        } catch (_: Exception) {}
        com.example.diagnostics.CrashAndErrorReporter.recordNonFatal(
            source = tag,
            message = throwable.message ?: throwable.javaClass.simpleName,
            throwable = throwable,
            showBanner = false
        )
        Log.w(TAG, "Non-fatal [$tag]: ${throwable.message}")
    }

    fun recordRpcFailure(rpcName: String, httpCode: Int?, rawError: String?) {
        val codeText = rawError.orEmpty()
        // Never treat expected user validation, rate-limit, or offline/DNS connectivity failures as non-fatal crashes
        val ignoredCodes = listOf(
            "empty_message", "message_too_long", "body_too_long", "title_too_long",
            "title_too_short", "invalid_kind", "voting_closed", "rate_limited",
            "feedback_not_found", "forbidden"
        )
        if (ignoredCodes.any { codeText.contains(it, ignoreCase = true) }) return
        val offlineErrors = listOf(
            "unable to resolve host", "no address associated", "failed to connect",
            "network is unreachable", "connection reset", "software caused connection abort",
            "timeout", "timed out", "broken pipe", "sslhandshake"
        )
        if (httpCode == null && offlineErrors.any { codeText.contains(it, ignoreCase = true) }) return
        if (httpCode != null && httpCode in 400..499 && httpCode != 401 && httpCode != 403 && httpCode != 404) return

        val msg = "RPC $rpcName failed (HTTP ${httpCode ?: -1}): ${codeText.take(240)}"
        recordNonFatal("rpc_$rpcName", IllegalStateException(msg))
    }

    fun recordCloudinaryFailure(stage: String, message: String, throwable: Throwable? = null) {
        recordNonFatal(
            "cloudinary_$stage",
            throwable ?: IllegalStateException("Cloudinary $stage failed: ${message.take(200)}")
        )
    }

    fun recordMediaDecodeFailure(contextTag: String, throwable: Throwable) {
        recordNonFatal("media_decode_$contextTag", throwable)
    }

    fun triggerDebugNonFatal() {
        if (!BuildConfig.DEBUG) return
        val ex = RuntimeException("FunkyTalk Debug Test Non-Fatal Exception (${System.currentTimeMillis()})")
        try {
            crashlytics()?.recordException(ex)
        } catch (_: Exception) {}
        Log.e(TAG, "Debug test non-fatal triggered", ex)
    }

    private fun logEvent(name: String, params: Map<String, Any?> = emptyMap()) {
        if (!_isUsageDataEnabled.value) return
        val fa = analytics() ?: return
        try {
            val bundle = Bundle().apply {
                params.forEach { (k, v) ->
                    when (v) {
                        is String -> putString(k, v.take(100))
                        is Int -> putInt(k, v)
                        is Long -> putLong(k, v)
                        is Double -> putDouble(k, v)
                        is Boolean -> putString(k, v.toString())
                    }
                }
            }
            fa.logEvent(name, bundle)
        } catch (_: Exception) {}
    }

    fun logMomentPosted(kind: String) = logEvent("moment_posted", mapOf("kind" to kind))
    fun logMomentPostFailed(reason: String) = logEvent("moment_post_failed", mapOf("reason" to reason))
    fun logMomentLiked() = logEvent("moment_liked")
    fun logMomentCommentAdded() = logEvent("moment_comment_added")
    fun logCorrectionAdded() = logEvent("correction_added")
    fun logSupportChatOpened() = logEvent("support_chat_opened")
    fun logSupportMessageSent() = logEvent("support_message_sent")
    fun logFeedbackCreated(kind: String) = logEvent("feedback_created", mapOf("kind" to kind))
    fun logFeedbackVoted() = logEvent("feedback_voted")
    fun logRoomJoined() = logEvent("room_joined")
    fun logRoomLeft(durationSec: Long) = logEvent("room_left", mapOf("duration_sec" to durationSec.coerceAtLeast(0L)))
    fun logSignupCompleted() = logEvent("signup_completed")
    fun logLoginCompleted() = logEvent("login_completed")
}
