package com.example.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response

data class MomentBgPreset(
    val key: String,
    val colors: List<Color>,
    val isDarkText: Boolean
)

class MomentRpcException(
    val rawCode: String,
    override val message: String,
    val isNotFound: Boolean = false
) : Exception(message)

object MomentsRepository {
    private const val TAG = "MomentsRepository"
    private const val FEED_TAG = "MomentsFeed"

    val bgPresets: List<MomentBgPreset> = listOf(
        MomentBgPreset("cream", listOf(Color(0xFFFFFBF0), Color(0xFFFFEFC2)), isDarkText = true),
        MomentBgPreset("mango", listOf(Color(0xFFFFC21A), Color(0xFFFF8A1F)), isDarkText = true),
        MomentBgPreset("purple_pink", listOf(Color(0xFF7C3AED), Color(0xFFEC4899)), isDarkText = false),
        MomentBgPreset("ocean", listOf(Color(0xFF06B6D4), Color(0xFF2563EB)), isDarkText = false),
        MomentBgPreset("deep_violet", listOf(Color(0xFF1E1B4B), Color(0xFF6D28D9)), isDarkText = false),
        MomentBgPreset("sunset_coral", listOf(Color(0xFFFF9A8B), Color(0xFFFF5E7E)), isDarkText = false),
        MomentBgPreset("lush_green", listOf(Color(0xFFBEF264), Color(0xFF16A34A)), isDarkText = true),
        MomentBgPreset("slate_dark", listOf(Color(0xFF475569), Color(0xFF0F172A)), isDarkText = false)
    )

    private val defaultBgPreset = MomentBgPreset(
        key = "default",
        colors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)),
        isDarkText = false
    )

    fun resolveBgPreset(bgKey: String?): MomentBgPreset {
        if (bgKey.isNullOrBlank()) return defaultBgPreset
        return bgPresets.firstOrNull { it.key.equals(bgKey.trim(), ignoreCase = true) } ?: defaultBgPreset
    }

    @Volatile
    private var cachedOptions: MomentOptionsResponse? = null

    fun getCachedOptions(): MomentOptionsResponse = cachedOptions ?: MomentOptionsResponse(
        topics = listOf(
            MomentTopicDto("travel", "Travel"),
            MomentTopicDto("daily_life", "Daily life"),
            MomentTopicDto("food", "Food"),
            MomentTopicDto("study", "Study"),
            MomentTopicDto("nature", "Nature"),
            MomentTopicDto("culture", "Culture"),
            MomentTopicDto("music", "Music"),
            MomentTopicDto("city", "City")
        ),
        limits = MomentLimitsDto()
    )

    fun stripLanguageLevel(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.trim().replace(Regex("\\s*\\([^)]*\\)\\s*$"), "").trim()
    }

    fun formatTopicLabelToHashtag(rawLabel: String): String {
        val clean = rawLabel.trim().removePrefix("#")
        if (clean.isBlank()) return ""
        val joined = clean
            .split(Regex("[\\s_]+"))
            .filter { it.isNotBlank() }
            .joinToString("") { word ->
                word.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(java.util.Locale.ROOT) else ch.toString()
                }
            }
        return "#$joined"
    }

    fun resolveTopicChipText(slugOrLabel: String): String {
        val topics = getCachedOptions().topics
        val match = topics.firstOrNull {
            it.slug.equals(slugOrLabel, ignoreCase = true) ||
                it.label.equals(slugOrLabel, ignoreCase = true)
        }
        return formatTopicLabelToHashtag(match?.label ?: slugOrLabel)
    }

    fun isNotFoundErrorCode(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val lower = raw.lowercase()
        return lower.contains("moment_not_found") ||
            lower.contains("comment_not_found") ||
            lower.contains("parent_not_found")
    }

    fun mapMomentError(raw: String?): String {
        val lower = raw?.lowercase().orEmpty()
        return when {
            lower.contains("not_authenticated") -> "Please log in again"
            lower.contains("account_restricted") -> "You can't post or comment right now"
            lower.contains("rate_limited") -> "You're going too fast. Try again in a few minutes"
            lower.contains("empty_moment") -> "Write something or add a photo"
            lower.contains("language_required") || lower.contains("invalid_language") -> "Choose the post language"
            lower.contains("title_too_long") ||
                lower.contains("content_too_long") ||
                lower.contains("translation_too_long") ||
                lower.contains("location_too_long") ||
                lower.contains("correction_too_long") -> "Text is too long"
            lower.contains("too_many_media") || lower.contains("video_must_be_alone") -> "Up to 10 photos, or 1 video"
            lower.contains("invalid_media") -> "Upload failed, please try again"
            lower.contains("too_many_topics") || lower.contains("invalid_topic") -> "Choose up to 3 topics"
            lower.contains("moment_not_found") ||
                lower.contains("comment_not_found") ||
                lower.contains("parent_not_found") -> "This post is no longer available"
            lower.contains("cannot_correct_own") -> "You can't correct your own post"
            lower.contains("original_not_in_post") ||
                lower.contains("no_change") ||
                lower.contains("correction_required") -> "Select the part of the post you want to fix and write the corrected version"
            else -> "Something went wrong. Try again"
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val v = opt(key)?.toString()?.trim()
        return if (v.isNullOrEmpty() || v == "null") null else v
    }

    private fun JSONObject.optIntCoerced(key: String, default: Int = 0): Int {
        if (!has(key) || isNull(key)) return default
        return when (val v = opt(key)) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull() ?: default
            else -> default
        }
    }

    private fun JSONObject.optBooleanCoerced(key: String, default: Boolean = false): Boolean {
        if (!has(key) || isNull(key)) return default
        return when (val v = opt(key)) {
            is Boolean -> v
            is String -> v.equals("true", ignoreCase = true)
            is Number -> v.toInt() != 0
            else -> default
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val list = ArrayList<String>(length())
        for (i in 0 until length()) {
            if (!isNull(i)) {
                val s = optString(i, "").trim()
                if (s.isNotEmpty() && s != "null") list.add(s)
            }
        }
        return list
    }

    private fun parseMomentMediaList(arr: JSONArray?): List<MomentMediaDto> {
        if (arr == null) return emptyList()
        val out = ArrayList<MomentMediaDto>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val url = obj.optStringOrNull("url") ?: continue
            out.add(
                MomentMediaDto(
                    type = obj.optStringOrNull("type") ?: "image",
                    url = url,
                    thumb_url = obj.optStringOrNull("thumb_url"),
                    width = if (obj.has("width") && !obj.isNull("width")) obj.optIntCoerced("width").takeIf { it > 0 } else null,
                    height = if (obj.has("height") && !obj.isNull("height")) obj.optIntCoerced("height").takeIf { it > 0 } else null,
                    duration_sec = if (obj.has("duration_sec") && !obj.isNull("duration_sec")) obj.optIntCoerced("duration_sec").takeIf { it >= 0 } else null
                )
            )
        }
        return out
    }

    private fun parseMomentAuthor(obj: JSONObject?): MomentAuthorDto {
        if (obj == null) return MomentAuthorDto()
        return MomentAuthorDto(
            id = obj.optStringOrNull("id") ?: "",
            username = obj.optStringOrNull("username"),
            name = obj.optStringOrNull("name"),
            avatar_url = obj.optStringOrNull("avatar_url"),
            city = obj.optStringOrNull("city"),
            country_code = obj.optStringOrNull("country_code"),
            is_verified = obj.optBooleanCoerced("is_verified", false),
            native_languages = obj.optJSONArray("native_languages").toStringList(),
            learning_languages = obj.optJSONArray("learning_languages").toStringList(),
            is_native = obj.optBooleanCoerced("is_native", false),
            is_me = obj.optBooleanCoerced("is_me", false),
            following = obj.optBooleanCoerced("following", false)
        )
    }

    private fun parseMomentDto(obj: JSONObject): MomentDto {
        val rawId = obj.opt("id")?.toString()?.takeIf { it.isNotBlank() && it != "null" }
            ?: throw IllegalArgumentException("Missing moment id")
        val rawLang = stripLanguageLevel(obj.optStringOrNull("language")).ifBlank { "English" }
        return MomentDto(
            id = rawId,
            title = obj.optStringOrNull("title"),
            content = obj.optStringOrNull("content"),
            translation = obj.optStringOrNull("translation"),
            language = rawLang,
            bg_key = obj.optStringOrNull("bg_key"),
            media = parseMomentMediaList(obj.optJSONArray("media")),
            topics = obj.optJSONArray("topics").toStringList(),
            location = obj.optStringOrNull("location"),
            fix_requested = obj.optBooleanCoerced("fix_requested", true),
            status = obj.optStringOrNull("status") ?: "active",
            likes_count = obj.optIntCoerced("likes_count", 0),
            comments_count = obj.optIntCoerced("comments_count", 0),
            corrections_count = obj.optIntCoerced("corrections_count", 0),
            created_at = obj.optStringOrNull("created_at"),
            liked_by_me = obj.optBooleanCoerced("liked_by_me", false),
            author = parseMomentAuthor(obj.optJSONObject("author"))
        )
    }

    private fun parseMomentListResilient(rawBody: String?): List<MomentDto> {
        val trimmed = rawBody?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed == "null") return emptyList()
        val array = JSONArray(trimmed)
        val result = ArrayList<MomentDto>(array.length())
        for (i in 0 until array.length()) {
            val rawItem = array.opt(i)
            val obj = array.optJSONObject(i)
            if (obj == null) {
                Log.e(FEED_TAG, "Skipping non-object item at index $i: raw=$rawItem")
                continue
            }
            try {
                result.add(parseMomentDto(obj))
            } catch (e: Exception) {
                Log.e(FEED_TAG, "Failed decoding moment item at index $i: ${e.javaClass.simpleName}: ${e.message} raw=$obj", e)
            }
        }
        return result
    }

    private fun extractRawError(code: Int, errorBody: String?): String {
        if (code == 401) return "not_authenticated"
        if (errorBody.isNullOrBlank()) return "unknown_error"
        return try {
            val json = JSONObject(errorBody)
            val msg = json.optString("message", "")
            val details = json.optString("details", "")
            val hint = json.optString("hint", "")
            listOf(msg, details, hint).firstOrNull { it.isNotBlank() } ?: errorBody
        } catch (_: Exception) {
            errorBody
        }
    }

    private suspend fun <T> executeAuthenticatedRpc(
        context: Context,
        call: suspend (token: String) -> Response<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            com.example.util.withLoadingTimeout {
                val uid = SupabaseSessionManager.ensureActiveSession(context)
                var token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (uid.isNullOrBlank() || token.isNullOrBlank()) {
                    return@withLoadingTimeout Result.failure(
                        MomentRpcException("not_authenticated", com.example.util.ErrorMapper.MSG_UNAUTHORIZED)
                    )
                }

                var response = call(token)
                if (response.code() == 401) {
                    val refreshedUid = SupabaseSessionManager.forceRefreshSession(context)
                    val refreshedToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    if (!refreshedUid.isNullOrBlank() && !refreshedToken.isNullOrBlank()) {
                        token = refreshedToken
                        response = call(token)
                    }
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Result.success(body)
                    } else {
                        Result.failure(MomentRpcException("empty_response", com.example.util.ErrorMapper.MSG_GENERIC))
                    }
                } else {
                    val errStr = response.errorBody()?.string()
                    val raw = extractRawError(response.code(), errStr)
                    Log.w(TAG, "RPC error code=${response.code()} raw=$raw")
                    TelemetryManager.recordRpcFailure("moments_rpc", response.code(), raw)
                    val domainMapped = mapMomentError(raw)
                    val resolvedMessage = if (domainMapped == "Something went wrong. Try again") {
                        com.example.util.ErrorMapper.mapHttp(response.code(), errStr)
                    } else {
                        domainMapped
                    }
                    Result.failure(
                        MomentRpcException(
                            rawCode = raw,
                            message = resolvedMessage,
                            isNotFound = isNotFoundErrorCode(raw)
                        )
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "RPC exception: ${e.message}", e)
            TelemetryManager.recordRpcFailure("moments_rpc", null, e.message)
            Result.failure(MomentRpcException("network_error", com.example.util.ErrorMapper.map(e)))
        }
    }

    private suspend fun <T> executeRawJsonRpc(
        context: Context,
        logTag: String = TAG,
        call: suspend (token: String) -> Response<ResponseBody>,
        parser: (rawBody: String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            com.example.util.withLoadingTimeout {
                val uid = SupabaseSessionManager.ensureActiveSession(context)
                var token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (uid.isNullOrBlank() || token.isNullOrBlank()) {
                    return@withLoadingTimeout Result.failure(
                        MomentRpcException("not_authenticated", com.example.util.ErrorMapper.MSG_UNAUTHORIZED)
                    )
                }

                var response = call(token)
                if (response.code() == 401) {
                    val refreshedUid = SupabaseSessionManager.forceRefreshSession(context)
                    val refreshedToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    if (!refreshedUid.isNullOrBlank() && !refreshedToken.isNullOrBlank()) {
                        token = refreshedToken
                        response = call(token)
                    }
                }

                if (response.isSuccessful) {
                    val rawBody = response.body()?.string() ?: ""
                    try {
                        Result.success(parser(rawBody))
                    } catch (e: Exception) {
                        Log.e(logTag, "JSON decode failed: ${e.javaClass.simpleName}: ${e.message} raw=$rawBody", e)
                        TelemetryManager.recordMediaDecodeFailure(logTag, e)
                        Result.failure(
                            MomentRpcException("decode_error", com.example.util.ErrorMapper.MSG_GENERIC)
                        )
                    }
                } else {
                    val errStr = response.errorBody()?.string()
                    val raw = extractRawError(response.code(), errStr)
                    Log.w(logTag, "RPC error code=${response.code()} raw=$raw")
                    TelemetryManager.recordRpcFailure(logTag, response.code(), raw)
                    val domainMapped = mapMomentError(raw)
                    val resolvedMessage = if (domainMapped == "Something went wrong. Try again") {
                        com.example.util.ErrorMapper.mapHttp(response.code(), errStr)
                    } else {
                        domainMapped
                    }
                    Result.failure(
                        MomentRpcException(
                            rawCode = raw,
                            message = resolvedMessage,
                            isNotFound = isNotFoundErrorCode(raw)
                        )
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(logTag, "RPC exception: ${e.javaClass.simpleName}: ${e.message}", e)
            TelemetryManager.recordRpcFailure(logTag, null, e.message)
            Result.failure(MomentRpcException("network_error", com.example.util.ErrorMapper.map(e)))
        }
    }

    suspend fun getMomentOptions(context: Context, forceRefresh: Boolean = false): Result<MomentOptionsResponse> {
        if (!forceRefresh && cachedOptions != null) {
            return Result.success(cachedOptions!!)
        }
        val res = executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.getMomentOptions(auth = token)
        }
        res.onSuccess { cachedOptions = it }
        return res
    }

    suspend fun getMomentsFeed(
        context: Context,
        tab: String,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<MomentDto>> {
        if (cachedOptions == null) {
            getMomentOptions(context)
        }
        return executeRawJsonRpc(
            context = context,
            logTag = FEED_TAG,
            call = { token ->
                SupabaseClient.api.getMomentsFeed(
                    auth = token,
                    body = GetMomentsFeedRequest(p_tab = tab, p_limit = limit, p_offset = offset)
                )
            },
            parser = { raw -> parseMomentListResilient(raw) }
        )
    }

    suspend fun getUserMoments(
        context: Context,
        userId: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<MomentDto>> {
        if (cachedOptions == null) {
            getMomentOptions(context)
        }
        return executeRawJsonRpc(
            context = context,
            logTag = FEED_TAG,
            call = { token ->
                SupabaseClient.api.getUserMoments(
                    auth = token,
                    body = GetUserMomentsRequest(p_user_id = userId, p_limit = limit, p_offset = offset)
                )
            },
            parser = { raw -> parseMomentListResilient(raw) }
        )
    }

    suspend fun getMoment(context: Context, momentId: String): Result<MomentDto> {
        return executeRawJsonRpc(
            context = context,
            logTag = FEED_TAG,
            call = { token ->
                SupabaseClient.api.getMomentRpc(
                    auth = token,
                    body = MomentIdRequest(p_moment_id = momentId)
                )
            },
            parser = { raw ->
                val trimmed = raw.trim()
                if (trimmed.startsWith("[")) {
                    val arr = JSONArray(trimmed)
                    parseMomentDto(arr.getJSONObject(0))
                } else {
                    parseMomentDto(JSONObject(trimmed))
                }
            }
        )
    }

    suspend fun createMoment(
        context: Context,
        title: String?,
        content: String?,
        language: String,
        translation: String?,
        bgKey: String?,
        mediaUris: List<Uri>,
        topics: List<String>,
        location: String?,
        fixRequested: Boolean,
        onUploadProgress: (index: Int, total: Int, percent: Int?, isVideo: Boolean) -> Unit = { _, _, _, _ -> }
    ): Result<MomentDto> = withContext(Dispatchers.IO) {
        val uid = SupabaseSessionManager.ensureActiveSession(context)
        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
        if (uid.isNullOrBlank() || token.isNullOrBlank()) {
            return@withContext Result.failure(
                MomentRpcException("not_authenticated", mapMomentError("not_authenticated"))
            )
        }

        val cleanLanguage = stripLanguageLevel(language)
        if (cleanLanguage.isBlank()) {
            return@withContext Result.failure(
                MomentRpcException("language_required", mapMomentError("language_required"))
            )
        }

        val uploadedMedia = mutableListOf<MomentMediaDto>()
        val totalMedia = mediaUris.size
        for ((index, uri) in mediaUris.withIndex()) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val isVideo = mime.startsWith("video", ignoreCase = true)
            onUploadProgress(index + 1, totalMedia, if (isVideo) 0 else null, isVideo)

            val uploadRes = CloudinaryUploader.uploadMomentMedia(
                context = context,
                token = token,
                uri = uri,
                onProgress = { pct ->
                    if (isVideo) {
                        onUploadProgress(index + 1, totalMedia, pct, true)
                    }
                }
            )
            val uploaded = uploadRes.getOrElse { err ->
                val isValidation = (err as? com.example.MomentUploadException)?.isValidationError == true
                val msg = if (isValidation) {
                    err.message ?: "Upload failed. Please try again."
                } else {
                    com.example.util.ErrorMapper.map(err)
                }
                TelemetryManager.logMomentPostFailed(if (isVideo) "video_upload_failed" else "photo_upload_failed")
                return@withContext Result.failure(
                    MomentRpcException(rawCode = "invalid_media", message = msg)
                )
            }
            uploadedMedia.add(uploaded)
        }

        val hasMedia = uploadedMedia.isNotEmpty()
        val kind = when {
            uploadedMedia.any { it.type == "video" } -> "video"
            hasMedia -> "photo"
            else -> "text"
        }
        val postResult = executeRawJsonRpc(
            context = context,
            logTag = FEED_TAG,
            call = { authToken ->
                SupabaseClient.api.createMomentRpc(
                    auth = authToken,
                    body = CreateMomentRequest(
                        p_title = title?.trim()?.takeIf { it.isNotEmpty() },
                        p_content = content?.trim()?.takeIf { it.isNotEmpty() },
                        p_language = cleanLanguage,
                        p_translation = translation?.trim()?.takeIf { it.isNotEmpty() },
                        p_bg_key = if (hasMedia) null else bgKey,
                        p_media = if (hasMedia) uploadedMedia else null,
                        p_topics = topics.takeIf { it.isNotEmpty() },
                        p_location = location?.trim()?.takeIf { it.isNotEmpty() },
                        p_fix_requested = fixRequested
                    )
                )
            },
            parser = { raw ->
                val trimmed = raw.trim()
                if (trimmed.startsWith("[")) {
                    val arr = JSONArray(trimmed)
                    parseMomentDto(arr.getJSONObject(0))
                } else {
                    parseMomentDto(JSONObject(trimmed))
                }
            }
        )
        postResult.onSuccess {
            TelemetryManager.logMomentPosted(kind)
        }.onFailure { err ->
            val reason = (err as? MomentRpcException)?.rawCode ?: "rpc_error"
            TelemetryManager.logMomentPostFailed(reason.take(60))
        }
        postResult
    }

    suspend fun deleteMoment(context: Context, momentId: String): Result<Unit> {
        return executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.deleteMomentRpc(
                auth = token,
                body = MomentIdRequest(p_moment_id = momentId)
            )
        }.map { Unit }
    }

    suspend fun toggleMomentLike(context: Context, momentId: String): Result<ToggleLikeResponse> {
        return executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.toggleMomentLikeRpc(
                auth = token,
                body = MomentIdRequest(p_moment_id = momentId)
            )
        }.onSuccess { resp ->
            if (resp.liked) TelemetryManager.logMomentLiked()
        }
    }

    suspend fun getMomentComments(
        context: Context,
        momentId: String,
        kind: String = "all",
        limit: Int = 30,
        offset: Int = 0
    ): Result<List<MomentCommentDto>> {
        return executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.getMomentCommentsRpc(
                auth = token,
                body = GetMomentCommentsRequest(
                    p_moment_id = momentId,
                    p_kind = kind,
                    p_limit = limit,
                    p_offset = offset
                )
            )
        }
    }

    suspend fun getCommentReplies(
        context: Context,
        commentId: String,
        limit: Int = 30,
        offset: Int = 0
    ): Result<List<MomentCommentDto>> {
        return executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.getCommentRepliesRpc(
                auth = token,
                body = GetCommentRepliesRequest(
                    p_comment_id = commentId,
                    p_limit = limit,
                    p_offset = offset
                )
            )
        }
    }

    suspend fun addMomentComment(
        context: Context,
        momentId: String,
        content: String,
        parentId: String? = null
    ): Result<MomentCommentDto> {
        return executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.addMomentCommentRpc(
                auth = token,
                body = AddMomentCommentRequest(
                    p_moment_id = momentId,
                    p_content = content.trim(),
                    p_parent_id = parentId
                )
            )
        }.onSuccess {
            TelemetryManager.logMomentCommentAdded()
        }
    }

    suspend fun addMomentCorrection(
        context: Context,
        momentId: String,
        original: String,
        corrected: String,
        explanation: String?
    ): Result<MomentCommentDto> {
        return executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.addMomentCorrectionRpc(
                auth = token,
                body = AddMomentCorrectionRequest(
                    p_moment_id = momentId,
                    p_original = original.trim(),
                    p_corrected = corrected.trim(),
                    p_explanation = explanation?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
        }.onSuccess {
            TelemetryManager.logCorrectionAdded()
        }
    }

    suspend fun toggleCommentLike(context: Context, commentId: String): Result<ToggleLikeResponse> {
        return executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.toggleCommentLikeRpc(
                auth = token,
                body = CommentIdRequest(p_comment_id = commentId)
            )
        }
    }

    suspend fun acceptCorrection(context: Context, commentId: String): Result<AcceptCorrectionResponse> {
        return executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.acceptCorrectionRpc(
                auth = token,
                body = CommentIdRequest(p_comment_id = commentId)
            )
        }
    }

    suspend fun deleteMomentComment(context: Context, commentId: String): Result<Unit> {
        return executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.deleteMomentCommentRpc(
                auth = token,
                body = CommentIdRequest(p_comment_id = commentId)
            )
        }.map { Unit }
    }

    suspend fun markMomentNotInterested(context: Context, momentId: String): Result<Unit> {
        return executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.markMomentNotInterestedRpc(
                auth = token,
                body = MomentIdRequest(p_moment_id = momentId)
            )
        }.map { Unit }
    }

    suspend fun toggleFollowUser(
        context: Context,
        targetUserId: String,
        currentlyFollowing: Boolean
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val uid = SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            if (uid.isNullOrBlank() || token.isNullOrBlank()) {
                return@withContext Result.failure(
                    MomentRpcException("not_authenticated", mapMomentError("not_authenticated"))
                )
            }
            val resp = if (!currentlyFollowing) {
                SupabaseClient.api.followUser(
                    auth = token,
                    follower = SupabaseFollower(follower_id = uid, following_id = targetUserId)
                ).isSuccessful
            } else {
                SupabaseClient.api.unfollowUser(
                    auth = token,
                    followerIdEq = "eq.$uid",
                    followingIdEq = "eq.$targetUserId"
                ).isSuccessful
            }
            if (resp) Result.success(!currentlyFollowing)
            else Result.failure(MomentRpcException("follow_failed", mapMomentError(null)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(MomentRpcException("follow_failed", mapMomentError(e.message)))
        }
    }
}
