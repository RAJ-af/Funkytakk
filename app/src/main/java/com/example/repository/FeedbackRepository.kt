package com.example.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.AddFeedbackReplyRequest
import com.example.AdminListFeedbackRequest
import com.example.AdminSetFeedbackStatusRequest
import com.example.BuildConfig
import com.example.CreateFeedbackRequest
import com.example.FeedbackAuthorDto
import com.example.FeedbackIdRequest
import com.example.FeedbackPostDto
import com.example.FeedbackReplyDto
import com.example.GetFeedbackBoardRequest
import com.example.GetMyFeedbackRequest
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.TelemetryManager
import com.example.ToggleFeedbackVoteResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response

class FeedbackRpcException(
    val rawCode: String,
    override val message: String
) : Exception(message)

object FeedbackRepository {
    private const val TAG = "FeedbackRepository"

    private val _myUpdatesCount = MutableStateFlow(0)
    val myUpdatesCount: StateFlow<Int> = _myUpdatesCount.asStateFlow()

    private val _myFeedback = MutableStateFlow<List<FeedbackPostDto>>(emptyList())
    val myFeedback: StateFlow<List<FeedbackPostDto>> = _myFeedback.asStateFlow()

    fun getAppVersionString(): String {
        return "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})".take(40)
    }

    fun getDeviceInfoString(): String {
        val mfg = Build.MANUFACTURER?.trim().orEmpty()
        val model = Build.MODEL?.trim().orEmpty()
        val release = Build.VERSION.RELEASE?.trim().orEmpty()
        return "$mfg $model / Android $release".trim().take(200)
    }

    fun mapFeedbackError(rawError: String?): String {
        val lower = rawError?.lowercase().orEmpty()
        return when {
            lower.contains("not_authenticated") || lower.contains("jwt") -> "Please log in again"
            lower.contains("rate_limited") -> "You're going too fast. Try again in a few minutes"
            lower.contains("empty_message") -> "Write a message first"
            lower.contains("message_too_long") || lower.contains("body_too_long") || lower.contains("title_too_long") -> "Text is too long"
            lower.contains("title_too_short") -> "Give it a short title (3+ letters)"
            lower.contains("invalid_kind") -> "Choose Bug or Feature"
            lower.contains("voting_closed") -> "Voting is closed for this request"
            lower.contains("feedback_not_found") -> "This item is no longer available"
            lower.contains("forbidden") -> "You can't do that"
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

    private fun JSONObject.optStrOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val v = optString(key, "")
        return if (v.isBlank() || v == "null") null else v
    }

    private fun parseAuthor(obj: JSONObject?): FeedbackAuthorDto? {
        if (obj == null) return null
        return FeedbackAuthorDto(
            id = obj.optStrOrNull("id") ?: "",
            username = obj.optStrOrNull("username"),
            name = obj.optStrOrNull("name"),
            avatar_url = obj.optStrOrNull("avatar_url")
        )
    }

    private fun parseReply(obj: JSONObject): FeedbackReplyDto {
        return FeedbackReplyDto(
            id = obj.optLong("id", 0L),
            body = obj.optString("body", ""),
            is_staff = obj.optBoolean("is_staff", false),
            created_at = obj.optStrOrNull("created_at"),
            is_mine = obj.optBoolean("is_mine", false),
            author = parseAuthor(obj.optJSONObject("author"))
        )
    }

    private fun parseFeedbackPost(obj: JSONObject): FeedbackPostDto {
        val repliesArr = obj.optJSONArray("replies")
        val replies = mutableListOf<FeedbackReplyDto>()
        if (repliesArr != null) {
            for (i in 0 until repliesArr.length()) {
                repliesArr.optJSONObject(i)?.let { replies.add(parseReply(it)) }
            }
        }
        return FeedbackPostDto(
            id = obj.optLong("id", 0L),
            kind = obj.optStrOrNull("kind") ?: "feature",
            title = obj.optString("title", ""),
            body = obj.optString("body", ""),
            status = obj.optStrOrNull("status") ?: "open",
            votes_count = obj.optInt("votes_count", 0),
            replies_count = obj.optInt("replies_count", 0),
            created_at = obj.optStrOrNull("created_at"),
            updated_at = obj.optStrOrNull("updated_at"),
            is_mine = obj.optBoolean("is_mine", false),
            has_voted = obj.optBoolean("has_voted", false),
            has_update = obj.optBoolean("has_update", false),
            app_version = obj.optStrOrNull("app_version"),
            device_info = obj.optStrOrNull("device_info"),
            author = parseAuthor(obj.optJSONObject("author")),
            replies = replies
        )
    }

    private fun parseFeedbackList(raw: String): List<FeedbackPostDto> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == "null") return emptyList()
        val arr = JSONArray(trimmed)
        val out = ArrayList<FeedbackPostDto>(arr.length())
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { out.add(parseFeedbackPost(it)) }
        }
        return out
    }

    private suspend fun <T> executeRawRpc(
        context: Context,
        rpcName: String,
        call: suspend (token: String) -> Response<ResponseBody>,
        parser: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val uid = SupabaseSessionManager.ensureActiveSession(context)
            var token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            if (uid.isNullOrBlank() || token.isNullOrBlank()) {
                return@withContext Result.failure(
                    FeedbackRpcException("not_authenticated", mapFeedbackError("not_authenticated"))
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
                val raw = response.body()?.string() ?: ""
                try {
                    Result.success(parser(raw))
                } catch (e: Exception) {
                    TelemetryManager.recordMediaDecodeFailure("feedback_$rpcName", e)
                    Result.failure(FeedbackRpcException("decode_error", mapFeedbackError(null)))
                }
            } else {
                val errBody = response.errorBody()?.string()
                val raw = extractRawError(response.code(), errBody)
                Log.w(TAG, "$rpcName failed (${response.code()}): $raw")
                TelemetryManager.recordRpcFailure(rpcName, response.code(), raw)
                Result.failure(FeedbackRpcException(raw, mapFeedbackError(raw)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "$rpcName exception: ${e.message}", e)
            TelemetryManager.recordRpcFailure(rpcName, null, e.message)
            Result.failure(FeedbackRpcException("network_error", mapFeedbackError(e.message)))
        }
    }

    suspend fun createFeedback(
        context: Context,
        kind: String,
        title: String,
        body: String
    ): Result<FeedbackPostDto> {
        val res = executeRawRpc(
            context = context,
            rpcName = "create_feedback",
            call = { token ->
                SupabaseClient.api.createFeedbackRpc(
                    auth = token,
                    body = CreateFeedbackRequest(
                        p_kind = kind,
                        p_title = title.trim(),
                        p_body = body.trim(),
                        p_app_version = getAppVersionString(),
                        p_device_info = getDeviceInfoString()
                    )
                )
            },
            parser = { raw -> parseFeedbackPost(JSONObject(raw.trim())) }
        )
        res.onSuccess {
            TelemetryManager.logFeedbackCreated(kind)
            refreshMyUpdatesCount(context)
        }
        return res
    }

    suspend fun getFeedbackBoard(
        context: Context,
        status: String? = null,
        sort: String = "top",
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<FeedbackPostDto>> {
        return executeRawRpc(
            context = context,
            rpcName = "get_feedback_board",
            call = { token ->
                SupabaseClient.api.getFeedbackBoardRpc(
                    auth = token,
                    body = GetFeedbackBoardRequest(
                        p_kind = "feature",
                        p_status = status,
                        p_sort = sort,
                        p_limit = limit,
                        p_offset = offset
                    )
                )
            },
            parser = { raw -> parseFeedbackList(raw) }
        )
    }

    suspend fun getMyFeedback(
        context: Context,
        limit: Int = 30,
        offset: Int = 0
    ): Result<List<FeedbackPostDto>> {
        val res = executeRawRpc(
            context = context,
            rpcName = "get_my_feedback",
            call = { token ->
                SupabaseClient.api.getMyFeedbackRpc(
                    auth = token,
                    body = GetMyFeedbackRequest(p_limit = limit, p_offset = offset)
                )
            },
            parser = { raw -> parseFeedbackList(raw) }
        )
        res.onSuccess { list ->
            if (offset == 0) {
                _myFeedback.value = list
                _myUpdatesCount.value = list.count { it.has_update }
            }
        }
        return res
    }

    suspend fun refreshMyUpdatesCount(context: Context) {
        getMyFeedback(context, limit = 30, offset = 0)
    }

    suspend fun getFeedback(context: Context, id: Long): Result<FeedbackPostDto> {
        return executeRawRpc(
            context = context,
            rpcName = "get_feedback",
            call = { token ->
                SupabaseClient.api.getFeedbackRpc(
                    auth = token,
                    body = FeedbackIdRequest(p_id = id)
                )
            },
            parser = { raw -> parseFeedbackPost(JSONObject(raw.trim())) }
        )
    }

    suspend fun toggleFeedbackVote(context: Context, id: Long): Result<ToggleFeedbackVoteResponse> =
        withContext(Dispatchers.IO) {
            try {
                val uid = SupabaseSessionManager.ensureActiveSession(context)
                var token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (uid.isNullOrBlank() || token.isNullOrBlank()) {
                    return@withContext Result.failure(
                        FeedbackRpcException("not_authenticated", mapFeedbackError("not_authenticated"))
                    )
                }
                var resp = SupabaseClient.api.toggleFeedbackVoteRpc(auth = token, body = FeedbackIdRequest(p_id = id))
                if (resp.code() == 401) {
                    val refreshedUid = SupabaseSessionManager.forceRefreshSession(context)
                    val refreshedToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    if (!refreshedUid.isNullOrBlank() && !refreshedToken.isNullOrBlank()) {
                        token = refreshedToken
                        resp = SupabaseClient.api.toggleFeedbackVoteRpc(auth = token, body = FeedbackIdRequest(p_id = id))
                    }
                }
                if (resp.isSuccessful && resp.body() != null) {
                    TelemetryManager.logFeedbackVoted()
                    Result.success(resp.body()!!)
                } else {
                    val raw = extractRawError(resp.code(), resp.errorBody()?.string())
                    TelemetryManager.recordRpcFailure("toggle_feedback_vote", resp.code(), raw)
                    Result.failure(FeedbackRpcException(raw, mapFeedbackError(raw)))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                TelemetryManager.recordRpcFailure("toggle_feedback_vote", null, e.message)
                Result.failure(FeedbackRpcException("network_error", mapFeedbackError(e.message)))
            }
        }

    suspend fun addFeedbackReply(context: Context, id: Long, body: String): Result<FeedbackReplyDto> {
        return executeRawRpc(
            context = context,
            rpcName = "add_feedback_reply",
            call = { token ->
                SupabaseClient.api.addFeedbackReplyRpc(
                    auth = token,
                    body = AddFeedbackReplyRequest(p_id = id, p_body = body.trim())
                )
            },
            parser = { raw -> parseReply(JSONObject(raw.trim())) }
        )
    }

    suspend fun markFeedbackSeen(context: Context, id: Long): Result<Boolean> {
        val res = executeRawRpc(
            context = context,
            rpcName = "mark_feedback_seen",
            call = { token ->
                SupabaseClient.api.markFeedbackSeenRpc(
                    auth = token,
                    body = FeedbackIdRequest(p_id = id)
                )
            },
            parser = { raw -> raw.trim().equals("true", ignoreCase = true) }
        )
        res.onSuccess {
            val updated = _myFeedback.value.map {
                if (it.id == id) it.copy(has_update = false) else it
            }
            _myFeedback.value = updated
            _myUpdatesCount.value = updated.count { it.has_update }
        }
        return res
    }

    suspend fun deleteFeedback(context: Context, id: Long): Result<Boolean> {
        val res = executeRawRpc(
            context = context,
            rpcName = "delete_feedback",
            call = { token ->
                SupabaseClient.api.deleteFeedbackRpc(
                    auth = token,
                    body = FeedbackIdRequest(p_id = id)
                )
            },
            parser = { raw -> raw.trim().equals("true", ignoreCase = true) }
        )
        res.onSuccess {
            val updated = _myFeedback.value.filterNot { it.id == id }
            _myFeedback.value = updated
            _myUpdatesCount.value = updated.count { it.has_update }
        }
        return res
    }

    suspend fun adminListFeedback(
        context: Context,
        kind: String? = null,
        status: String? = null,
        limit: Int = 30,
        offset: Int = 0
    ): Result<List<FeedbackPostDto>> {
        return executeRawRpc(
            context = context,
            rpcName = "admin_list_feedback",
            call = { token ->
                SupabaseClient.api.adminListFeedbackRpc(
                    auth = token,
                    body = AdminListFeedbackRequest(
                        p_kind = kind,
                        p_status = status,
                        p_limit = limit,
                        p_offset = offset
                    )
                )
            },
            parser = { raw -> parseFeedbackList(raw) }
        )
    }

    suspend fun adminSetFeedbackStatus(
        context: Context,
        id: Long,
        status: String,
        note: String?
    ): Result<Boolean> {
        return executeRawRpc(
            context = context,
            rpcName = "admin_set_feedback_status",
            call = { token ->
                SupabaseClient.api.adminSetFeedbackStatusRpc(
                    auth = token,
                    body = AdminSetFeedbackStatusRequest(
                        p_id = id,
                        p_status = status,
                        p_note = note?.trim()?.takeIf { it.isNotEmpty() }
                    )
                )
            },
            parser = { raw -> raw.trim().equals("true", ignoreCase = true) }
        )
    }
}
