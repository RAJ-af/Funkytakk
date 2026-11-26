package com.example.util

import android.content.Context
import com.example.SupabaseSessionManager
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Central error mapper converting technical exceptions, HTTP codes, and server codes
 * into concise, friendly, English-only messages without leaking URLs, stack traces, or raw JSON.
 */
object ErrorMapper {

    const val MSG_OFFLINE = "Can't reach FunkyTalk. Check your internet and try again."
    const val MSG_UNAUTHORIZED = "Please sign in again."
    const val MSG_DEVICE_REVOKED = "You were signed out on this device."
    const val MSG_RATE_LIMITED = "Too many tries. Please wait a moment."
    const val MSG_SERVER_BUSY = "Server is busy. Please try again."
    const val MSG_PAYLOAD_TOO_LARGE = "File is too large to upload (20 MB max)."
    const val MSG_GENERIC = "Something went wrong. Please try again."

    fun isNetworkException(throwable: Throwable?): Boolean {
        var cur = throwable
        while (cur != null) {
            when (cur) {
                is UnknownHostException,
                is ConnectException,
                is SocketTimeoutException,
                is EOFException,
                is SSLException,
                is IOException -> return true
            }
            cur = cur.cause
        }
        return false
    }

    fun mapCardDesignError(raw: String?): String? {
        val lower = raw?.lowercase().orEmpty()
        if (lower.isBlank()) return null
        return when {
            lower.contains("not_authenticated") || lower.contains("unauthorized") -> MSG_UNAUTHORIZED
            lower.contains("profile_required") -> "Finish setting up your profile before saving a card."
            lower.contains("design_too_large") -> "Your card has too many details. Try removing a few stickers."
            lower.contains("too_many_stickers") -> "You can add up to 12 stickers per card."
            lower.contains("invalid_sticker_position") -> "One of your stickers is outside the card bounds."
            lower.contains("invalid_sticker_rotation") -> "Sticker rotation must stay between -45 and 45 degrees."
            lower.contains("invalid_sticker_scale") -> "Sticker size must stay between 0.5x and 2.5x."
            lower.contains("invalid_sticker_style") -> "That sticker style is not supported."
            lower.contains("invalid_sticker_text") -> "Sticker words must be 1 to 14 characters."
            lower.contains("invalid_sticker_emoji") -> "Please pick a valid emoji sticker."
            lower.contains("sticker_pack_not_available") -> "One of those sticker packs isn't available to you yet."
            lower.contains("invalid_sticker_pack") -> "That sticker pack is invalid."
            lower.contains("invalid_sticker_kind") -> "One of those stickers isn't supported."
            lower.contains("invalid_hi_label") -> "Button text must be 12 characters or fewer."
            lower.contains("invalid_bg_style") -> "That background style isn't supported."
            lower.contains("invalid_border") -> "That border style isn't supported."
            lower.contains("invalid_radius") -> "Corner roundness must be between 0 and 32."
            lower.contains("invalid_color") -> "One of the selected colors isn't valid."
            lower.contains("invalid_show") -> "One of the visibility toggles isn't valid."
            lower.contains("invalid_design") -> "This card design looks invalid. Try resetting or editing it."
            lower.contains("not_available") -> "This card is no longer available to remix."
            else -> null
        }
    }

    fun map(throwable: Throwable?): String {
        if (throwable == null) return MSG_GENERIC
        if (throwable is MappedErrorException) return throwable.friendlyMessage
        if (isNetworkException(throwable)) return MSG_OFFLINE

        mapCardDesignError(throwable.message)?.let { return it }

        val msgLower = throwable.message?.lowercase().orEmpty()
        return when {
            msgLower.contains("device_revoked") -> MSG_DEVICE_REVOKED
            msgLower.contains("session_mint_failed") ||
                msgLower.contains("ai_not_configured") ||
                msgLower.contains("all_providers_failed") -> MSG_SERVER_BUSY
            msgLower.contains("rate_limited") ||
                msgLower.contains("daily_limit") ||
                msgLower.contains("too many") -> MSG_RATE_LIMITED
            msgLower.contains("not_authenticated") ||
                msgLower.contains("unauthorized") -> MSG_UNAUTHORIZED
            else -> MSG_GENERIC
        }
    }

    fun mapHttp(statusCode: Int, rawBody: String? = null): String {
        val bodyLower = rawBody?.lowercase().orEmpty()
        mapCardDesignError(bodyLower)?.let { return it }
        return when {
            statusCode == 403 && bodyLower.contains("device_revoked") -> MSG_DEVICE_REVOKED
            statusCode == 401 -> MSG_UNAUTHORIZED
            statusCode == 413 || bodyLower.contains("payload too large") -> MSG_PAYLOAD_TOO_LARGE
            statusCode == 429 || bodyLower.contains("rate_limited") || bodyLower.contains("daily_limit") -> MSG_RATE_LIMITED
            statusCode == 503 && bodyLower.contains("session_mint_failed") -> MSG_SERVER_BUSY
            statusCode in 500..599 -> MSG_SERVER_BUSY
            else -> MSG_GENERIC
        }
    }

    fun mapFirebaseAuthError(throwable: Throwable?): String {
        if (throwable == null) return MSG_GENERIC
        if (isNetworkException(throwable)) return MSG_OFFLINE
        val lower = throwable.message?.lowercase().orEmpty()
        if (throwable is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            return when {
                throwable.errorCode.equals("ERROR_INVALID_EMAIL", ignoreCase = true) ||
                    lower.contains("badly formatted") ||
                    lower.contains("invalid-email") -> "Please enter a valid email address."
                else -> "Incorrect email or password."
            }
        }
        if (throwable is com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            return "No account found with this email."
        }
        if (throwable is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            return "This email already has an account. Try logging in instead."
        }
        return when {
            lower.contains("network") || lower.contains("timeout") -> MSG_OFFLINE
            lower.contains("too-many-requests") || lower.contains("too many") || lower.contains("blocked all requests") -> MSG_RATE_LIMITED
            lower.contains("already in use") || lower.contains("email-already-in-use") ->
                "This email already has an account. Try logging in instead."
            lower.contains("no user record") || lower.contains("user-not-found") ->
                "No account found with this email."
            lower.contains("password is invalid") ||
                lower.contains("wrong-password") ||
                lower.contains("invalid_login_credentials") ||
                lower.contains("invalid-credential") ||
                lower.contains("invalid credential") ||
                lower.contains("auth credential") ||
                lower.contains("malformed or has expired") ->
                "Incorrect email or password."
            lower.contains("badly formatted") || lower.contains("invalid-email") ->
                "Please enter a valid email address."
            lower.contains("weak-password") ->
                "Password must be at least 6 characters."
            else -> MSG_GENERIC
        }
    }

    /**
     * Executes a Retrofit call, performs at most ONE silent session refresh on HTTP 401,
     * and returns either the successful body/response or a mapped [MappedErrorException].
     */
    suspend fun <T> executeWithSingle401Retry(
        context: Context,
        call: suspend (bearerToken: String) -> Response<T>
    ): Result<Response<T>> {
        return try {
            val initialToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: SupabaseSessionManager.getBearerToken(context)
            var response = call(initialToken)
            if (response.code() == 401) {
                val refreshedUid = SupabaseSessionManager.forceRefreshSession(context)
                if (!refreshedUid.isNullOrBlank()) {
                    val freshToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                        ?: SupabaseSessionManager.getBearerToken(context)
                    response = call(freshToken)
                }
            }
            if (response.isSuccessful) {
                Result.success(response)
            } else {
                val rawBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
                Result.failure(MappedErrorException(mapHttp(response.code(), rawBody), response.code()))
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(MappedErrorException(map(t)))
        }
    }
}

class MappedErrorException(
    val friendlyMessage: String,
    val statusCode: Int = 0
) : Exception(friendlyMessage)
