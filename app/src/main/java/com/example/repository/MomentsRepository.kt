package com.example.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    fun resolveTopicChipText(slugOrLabel: String): String {
        val topics = getCachedOptions().topics
        val match = topics.firstOrNull {
            it.slug.equals(slugOrLabel, ignoreCase = true) ||
                it.label.equals(slugOrLabel, ignoreCase = true)
        }
        val label = (match?.label ?: slugOrLabel).replace(" ", "")
        return if (label.startsWith("#")) label else "#$label"
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
            val uid = SupabaseSessionManager.ensureActiveSession(context)
            var token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            if (uid.isNullOrBlank() || token.isNullOrBlank()) {
                return@withContext Result.failure(
                    MomentRpcException("not_authenticated", mapMomentError("not_authenticated"))
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
                    Result.failure(MomentRpcException("empty_response", mapMomentError(null)))
                }
            } else {
                val errStr = response.errorBody()?.string()
                val raw = extractRawError(response.code(), errStr)
                Log.w(TAG, "RPC error code=${response.code()} raw=$raw")
                Result.failure(
                    MomentRpcException(
                        rawCode = raw,
                        message = mapMomentError(raw),
                        isNotFound = isNotFoundErrorCode(raw)
                    )
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "RPC exception: ${e.message}", e)
            Result.failure(MomentRpcException("network_error", mapMomentError(e.message)))
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
        // Warm options cache in background/parallel on first load so topic labels are available
        if (cachedOptions == null) {
            getMomentOptions(context)
        }
        return executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.getMomentsFeed(
                auth = token,
                body = GetMomentsFeedRequest(p_tab = tab, p_limit = limit, p_offset = offset)
            )
        }
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
        return executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.getUserMoments(
                auth = token,
                body = GetUserMomentsRequest(p_user_id = userId, p_limit = limit, p_offset = offset)
            )
        }
    }

    suspend fun getMoment(context: Context, momentId: String): Result<MomentDto> {
        return executeAuthenticatedRpc(context) { token ->
            SupabaseClient.api.getMomentRpc(
                auth = token,
                body = MomentIdRequest(p_moment_id = momentId)
            )
        }
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
        fixRequested: Boolean
    ): Result<MomentDto> = withContext(Dispatchers.IO) {
        val uid = SupabaseSessionManager.ensureActiveSession(context)
        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
        if (uid.isNullOrBlank() || token.isNullOrBlank()) {
            return@withContext Result.failure(
                MomentRpcException("not_authenticated", mapMomentError("not_authenticated"))
            )
        }

        val uploadedMedia = mutableListOf<MomentMediaDto>()
        for (uri in mediaUris) {
            val uploaded = CloudinaryUploader.uploadMomentMedia(context, token, uri)
            if (uploaded == null) {
                return@withContext Result.failure(
                    MomentRpcException("invalid_media", mapMomentError("invalid_media"))
                )
            }
            uploadedMedia.add(uploaded)
        }

        val hasMedia = uploadedMedia.isNotEmpty()
        executeAuthenticatedRpc(context) { authToken ->
            SupabaseClient.api.createMomentRpc(
                auth = authToken,
                body = CreateMomentRequest(
                    p_title = title?.trim()?.takeIf { it.isNotEmpty() },
                    p_content = content?.trim()?.takeIf { it.isNotEmpty() },
                    p_language = language.trim(),
                    p_translation = translation?.trim()?.takeIf { it.isNotEmpty() },
                    p_bg_key = if (hasMedia) null else bgKey,
                    p_media = if (hasMedia) uploadedMedia else null,
                    p_topics = topics.takeIf { it.isNotEmpty() },
                    p_location = location?.trim()?.takeIf { it.isNotEmpty() },
                    p_fix_requested = fixRequested
                )
            )
        }
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(MomentRpcException("follow_failed", mapMomentError(e.message)))
        }
    }
}
