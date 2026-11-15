package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeFormatter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ModerationManager {
    private const val TAG = "ModerationManager"

    var pendingReportIdToOpen: String? by mutableStateOf<String?>(null)
    var showModerationHubDialog: Boolean by mutableStateOf(false)

    fun resetForLogout() {
        pendingReportIdToOpen = null
        showModerationHubDialog = false
    }

    suspend fun fetchMyModerationStatus(context: Context): MyModerationStatus? = withContext(Dispatchers.IO) {
        try {
            // Ensure session is fresh
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            val myUserId = SupabaseSessionManager.getUserId(context)
            val myFirebaseUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

            if (auth == null || (myUserId.isNullOrBlank() && myFirebaseUid.isNullOrBlank())) {
                Log.d(TAG, "Cannot fetch moderation status: No authenticated session or user ID")
                return@withContext null
            }

            val targetIdParam = myUserId ?: myFirebaseUid ?: ""

            // Method 1: Try direct query to user_moderation_status table filtered strictly by my user_id
            try {
                if (targetIdParam.isNotBlank()) {
                    val directRes = SupabaseClient.api.getUserModerationStatus(
                        auth = auth,
                        userIdEq = "eq.$targetIdParam"
                    )
                    if (directRes.isSuccessful) {
                        val bodyStr = directRes.body()?.string()?.trim()
                        if (bodyStr == "[]" || bodyStr.isNullOrBlank() || bodyStr == "null") {
                            // Definitive: current user has no moderation restrictions
                            Log.d(TAG, "Direct user_moderation_status query: No active restrictions for $targetIdParam")
                            return@withContext null
                        }
                        val array = JSONArray(bodyStr)
                        if (array.length() > 0) {
                            val item = array.getJSONObject(0)
                            val action = item.optString("action_type").takeIf { it.isNotBlank() && it != "null" }
                            if (!action.isNullOrBlank()) {
                                val status = MyModerationStatus(
                                    user_id = item.optString("user_id").takeIf { it.isNotBlank() && it != "null" },
                                    action_type = action,
                                    started_at = item.optString("started_at").takeIf { it.isNotBlank() && it != "null" },
                                    expires_at = item.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
                                    reason = item.optString("reason").takeIf { it.isNotBlank() && it != "null" },
                                    report_id = item.optString("report_id").takeIf { it.isNotBlank() && it != "null" }
                                )
                                Log.d(TAG, "Direct moderation status found for $targetIdParam: action_type=${status.action_type}")
                                return@withContext status
                            }
                        }
                    }
                }
            } catch (dEx: Exception) {
                Log.w(TAG, "Direct user_moderation_status query fallback: ${dEx.message}")
            }

            // Method 2: Fallback to RPC get_my_moderation_status passing p_user_id
            val rpcBody = if (targetIdParam.isNotBlank()) mapOf("p_user_id" to targetIdParam) else emptyMap()
            val response = SupabaseClient.api.getMyModerationStatus(auth = auth, body = rpcBody)
            if (!response.isSuccessful) {
                Log.w(TAG, "getMyModerationStatus returned code ${response.code()}: ${response.errorBody()?.string()}")
                return@withContext null
            }

            val bodyStr = response.body()?.string()?.trim() ?: return@withContext null
            if (bodyStr.isEmpty() || bodyStr == "null" || bodyStr == "[]") {
                return@withContext null
            }

            val jsonObject: JSONObject? = when {
                bodyStr.startsWith("[") -> {
                    val array = JSONArray(bodyStr)
                    if (array.length() > 0) array.getJSONObject(0) else null
                }
                bodyStr.startsWith("{") -> JSONObject(bodyStr)
                else -> null
            }

            if (jsonObject == null) return@withContext null

            // Validate that the returned record strictly belongs to the current user
            val returnedUserId = jsonObject.optString("user_id").takeIf { it.isNotBlank() && it != "null" }
            if (returnedUserId != null) {
                val matchesMe = (myUserId != null && returnedUserId.equals(myUserId, ignoreCase = true)) ||
                                (myFirebaseUid != null && returnedUserId.equals(myFirebaseUid, ignoreCase = true))
                if (!matchesMe) {
                    Log.w(TAG, "Rejecting moderation record for user '$returnedUserId': does not match current user '$myUserId' / '$myFirebaseUid'")
                    return@withContext null
                }
            }

            val actionType = jsonObject.optString("action_type").takeIf { it.isNotBlank() && it != "null" }
            if (actionType.isNullOrBlank()) return@withContext null

            val status = MyModerationStatus(
                user_id = returnedUserId,
                action_type = actionType,
                started_at = jsonObject.optString("started_at").takeIf { it.isNotBlank() && it != "null" },
                expires_at = jsonObject.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
                reason = jsonObject.optString("reason").takeIf { it.isNotBlank() && it != "null" },
                report_id = jsonObject.optString("report_id").takeIf { it.isNotBlank() && it != "null" }
            )

            Log.d(TAG, "Current moderation status: action_type=${status.action_type}, expires_at=${status.expires_at}")
            return@withContext status
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching moderation status", e)
            return@withContext null
        }
    }

    fun isActionActive(status: MyModerationStatus?): Boolean {
        if (status == null) return false
        val action = status.action_type?.lowercase()?.trim() ?: return false
        if (action !in listOf("suspend", "warning", "mute")) return false

        // Mute stays active until replaced
        if (action == "mute") return true

        // For suspend and warning, check expiration if available
        val expiresAt = status.expires_at
        if (!expiresAt.isNullOrBlank()) {
            try {
                val expiryInstant = Instant.parse(expiresAt)
                if (Instant.now().isAfter(expiryInstant)) {
                    return false
                }
            } catch (e: Exception) {
                // If parsing fails, fall back to checking if action is set
                Log.w(TAG, "Unable to parse expires_at '$expiresAt': ${e.message}")
            }
        }

        return true
    }

    fun isMessagingBlocked(status: MyModerationStatus?): Boolean {
        return isActionActive(status)
    }

    fun mapModerationError(error: String?, defaultMessage: String = "An unexpected error occurred."): String {
        if (error == null) return defaultMessage
        val err = error.lowercase()
        return when {
            err.contains("not_authenticated") -> "Please log in to continue."
            err.contains("admin_required") -> "Admin privileges are required for this action."
            err.contains("take_charge_first") -> "You must take charge of this report first before taking action."
            err.contains("already_claimed") -> "This report is already claimed by another admin."
            err.contains("conflict_of_interest") -> "You cannot moderate or reopen a report where you are the reporter or reported user."
            err.contains("report_closed") -> "This report is already resolved or closed."
            err.contains("report_not_found") -> "Report not found."
            err.contains("target_mismatch") -> "Target user does not match the reported user."
            err.contains("brief_too_short") -> "Brief must be at least 10 characters."
            err.contains("brief_too_long") -> "Brief cannot exceed 2000 characters."
            err.contains("report_not_resolved") -> "Only resolved reports can be reopened."
            err.contains("reopen_window_closed") -> "Reopen window has expired (must be within 30 days of resolution)."
            err.contains("already_reopened") -> "This report already has an active reopen request."
            err.contains("not_original_admin") -> "Only the original admin who resolved this report can challenge the reopen."
            err.contains("not_open") -> "This reopen request is no longer open for challenge."
            err.contains("owner_required") -> "Only the owner (superadmin) can decide disputes."
            err.contains("already_decided") -> "This dispute has already been decided."
            err.contains("staff_required") -> "Administrator permissions required."
            err.contains("empty_message") -> "Message cannot be empty."
            err.contains("invalid_open_hours") -> "Invalid open hours selected."
            err.contains("target_user_not_found") -> "Target user not found."
            err.contains("reopen_not_found") -> "Reopen record not found."
            else -> defaultMessage
        }
    }

    suspend fun closeSupportChat(context: Context, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Please log in to continue."))
            val res = SupabaseClient.api.adminCloseSupportChat(
                auth = auth,
                body = AdminCloseSupportChatRequest(p_user_id = userId)
            )
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                val errBody = res.errorBody()?.string() ?: res.message()
                Result.failure(Exception(mapModerationError(errBody, "Failed to close support chat.")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapModerationError(e.message, "Network error closing support chat.")))
        }
    }

    suspend fun takeCharge(context: Context, reportId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Please log in to continue."))
            val res = SupabaseClient.api.adminTakeCharge(auth = auth, body = AdminTakeChargeRequest(reportId))
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string() ?: "{}"
                val json = JSONObject(bodyStr)
                val expiresAt = json.optString("claim_expires_at", "")
                Result.success(expiresAt)
            } else {
                val errBody = res.errorBody()?.string() ?: res.message()
                Log.e(TAG, "admin_take_charge failed (${res.code()}): $errBody")
                Result.failure(Exception(mapModerationError(errBody, "Failed to take charge of report.")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in takeCharge", e)
            Result.failure(Exception(mapModerationError(e.message, "Network error taking charge of report.")))
        }
    }

    suspend fun releaseReport(context: Context, reportId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Please log in to continue."))
            val res = SupabaseClient.api.adminReleaseReport(auth = auth, body = AdminReleaseReportRequest(reportId))
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                val errBody = res.errorBody()?.string() ?: res.message()
                Result.failure(Exception(mapModerationError(errBody, "Failed to release report.")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapModerationError(e.message, "Network error releasing report.")))
        }
    }

    suspend fun dismissReport(context: Context, reportId: String, reason: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Please log in to continue."))

            // Take charge first if needed
            if (reportId.isNotBlank()) {
                val claimRes = takeCharge(context, reportId)
                if (claimRes.isFailure) {
                    val claimErr = claimRes.exceptionOrNull()?.message ?: ""
                    if (!claimErr.contains("already_mine", ignoreCase = true)) {
                        return@withContext Result.failure(Exception(mapModerationError(claimErr, "Please take charge of this report first.")))
                    }
                }
            }

            val res = SupabaseClient.api.adminDismissReport(
                auth = auth,
                body = AdminDismissReportRequest(reportId, reason?.takeIf { it.isNotBlank() })
            )
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                val errBody = res.errorBody()?.string() ?: res.message()
                Result.failure(Exception(mapModerationError(errBody, "Failed to dismiss report.")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapModerationError(e.message, "Network error dismissing report.")))
        }
    }

    suspend fun reopenReport(context: Context, reportId: String, brief: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = brief.trim()
        if (trimmed.length < 10) return@withContext Result.failure(Exception("Brief must be at least 10 characters."))
        if (trimmed.length > 2000) return@withContext Result.failure(Exception("Brief cannot exceed 2000 characters."))
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Please log in to continue."))
            val res = SupabaseClient.api.adminReopenReport(auth = auth, body = AdminReopenReportRequest(reportId, trimmed))
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string() ?: "{}"
                val json = JSONObject(bodyStr)
                val reopenId = json.optString("reopen_id", "")
                Result.success(reopenId)
            } else {
                val errBody = res.errorBody()?.string() ?: res.message()
                Result.failure(Exception(mapModerationError(errBody, "Failed to reopen report.")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapModerationError(e.message, "Network error reopening report.")))
        }
    }

    suspend fun challengeReopen(context: Context, reopenId: String, brief: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = brief.trim()
        if (trimmed.length < 10) return@withContext Result.failure(Exception("Brief must be at least 10 characters."))
        if (trimmed.length > 2000) return@withContext Result.failure(Exception("Brief cannot exceed 2000 characters."))
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Please log in to continue."))
            val res = SupabaseClient.api.adminChallengeReopen(auth = auth, body = AdminChallengeReopenRequest(reopenId, trimmed))
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                val errBody = res.errorBody()?.string() ?: res.message()
                Result.failure(Exception(mapModerationError(errBody, "Failed to challenge reopen.")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapModerationError(e.message, "Network error challenging reopen.")))
        }
    }

    suspend fun ownerDecideDispute(context: Context, reopenId: String, decision: String, note: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Please log in to continue."))
            val res = SupabaseClient.api.ownerDecideDispute(
                auth = auth,
                body = OwnerDecideDisputeRequest(p_reopen_id = reopenId, p_decision = decision, p_note = note?.takeIf { it.isNotBlank() })
            )
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                val errBody = res.errorBody()?.string() ?: res.message()
                Result.failure(Exception(mapModerationError(errBody, "Failed to decide dispute.")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapModerationError(e.message, "Network error deciding dispute.")))
        }
    }

    suspend fun fetchReportQueue(
        context: Context,
        status: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<AdminReportQueueItem>> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Please log in to continue."))
            val res = SupabaseClient.api.adminGetReportQueue(
                auth = auth,
                body = AdminGetReportQueueRequest(p_status = status, p_limit = limit, p_offset = offset)
            )
            if (res.isSuccessful) {
                Result.success(res.body() ?: emptyList())
            } else {
                val errBody = res.errorBody()?.string() ?: res.message()
                Result.failure(Exception(mapModerationError(errBody, "Failed to fetch report queue.")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapModerationError(e.message, "Network error fetching report queue.")))
        }
    }

    suspend fun fetchReportDetail(context: Context, reportId: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Please log in to continue."))
            val res = SupabaseClient.api.adminGetReportDetail(
                auth = auth,
                body = AdminGetReportDetailRequest(reportId)
            )
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string() ?: "{}"
                Result.success(JSONObject(bodyStr))
            } else {
                val errBody = res.errorBody()?.string() ?: res.message()
                Result.failure(Exception(mapModerationError(errBody, "Failed to fetch report detail.")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapModerationError(e.message, "Network error fetching report detail.")))
        }
    }

    suspend fun fetchMyAccountReview(context: Context): Result<MyAccountReviewResponse> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.success(MyAccountReviewResponse(under_review = false))
            val res = SupabaseClient.api.getMyAccountReview(auth = auth)
            if (res.isSuccessful) {
                Result.success(res.body() ?: MyAccountReviewResponse(under_review = false))
            } else {
                val errBody = res.errorBody()?.string() ?: res.message()
                Log.w(TAG, "get_my_account_review HTTP ${res.code()}: $errBody")
                Result.success(MyAccountReviewResponse(under_review = false))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception in fetchMyAccountReview: ${e.message}")
            Result.success(MyAccountReviewResponse(under_review = false))
        }
    }

    suspend fun applyModerationAction(
        context: Context,
        reportId: String,
        targetUserId: String,
        actionType: String,
        durationHours: Int?,
        reason: String?,
        roomId: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedTarget = targetUserId.trim()
        if (!SupabaseSessionManager.isValidUuid(trimmedTarget)) {
            val err = "Target user ID '$trimmedTarget' is not a valid Supabase UUID."
            Log.e(TAG, err)
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        val normalizedAction = actionType.lowercase().trim()
        if (normalizedAction !in listOf("suspend", "warning", "mute")) {
            val err = "Invalid action type: $normalizedAction. Allowed: suspend, warning, mute"
            Log.e(TAG, err)
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        val resolvedDuration = when (normalizedAction) {
            "suspend" -> {
                val dur = durationHours ?: 24
                if (dur < 1 || dur > 90) {
                    return@withContext Result.failure(IllegalArgumentException("Suspension duration must be between 1 and 90 hours."))
                }
                dur
            }
            "warning" -> 1 // Fixed exactly 1 hour
            "mute" -> null // No duration
            else -> null
        }

        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            if (auth == null) {
                val err = "Active administrator session required to enforce moderation. Please re-login."
                Log.e(TAG, err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            // Ensure claim first
            if (reportId.isNotBlank()) {
                val claimRes = takeCharge(context, reportId)
                if (claimRes.isFailure) {
                    val claimErr = claimRes.exceptionOrNull()?.message ?: ""
                    if (!claimErr.contains("already_mine", ignoreCase = true)) {
                        return@withContext Result.failure(Exception(mapModerationError(claimErr, "Please take charge of this report first.")))
                    }
                }
            }

            val req = AdminApplyModerationActionRequest(
                p_report_id = reportId,
                p_target_user_id = trimmedTarget,
                p_action_type = normalizedAction,
                p_duration_hours = resolvedDuration,
                p_reason = reason?.takeIf { it.isNotBlank() }
            )

            Log.d(TAG, "Applying moderation action: reportId=$reportId, target=$trimmedTarget, action=$normalizedAction, hours=$resolvedDuration")
            val response = SupabaseClient.api.adminApplyModerationAction(auth = auth, body = req)

            if (response.isSuccessful) {
                Log.d(TAG, "adminApplyModerationAction succeeded: code=${response.code()}")
                com.example.realtime.VoiceRoomRealtimeManager.broadcastModerationAction(
                    roomId = roomId,
                    targetUserId = trimmedTarget,
                    actionType = normalizedAction,
                    reason = reason
                )
                Result.success("Moderation action '$normalizedAction' successfully applied.")
            } else {
                val errBody = response.errorBody()?.string() ?: response.message()
                Log.e(TAG, "adminApplyModerationAction failed (${response.code()}): $errBody")
                Result.failure(Exception(mapModerationError(errBody, "Failed (${response.code()}): $errBody")))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception in applyModerationAction", e)
            Result.failure(Exception(mapModerationError(e.message, "Network error applying moderation action.")))
        }
    }

    suspend fun revokeModerationAction(
        context: Context,
        targetUserId: String,
        reason: String? = null,
        roomId: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedTarget = targetUserId.trim()
        if (!SupabaseSessionManager.isValidUuid(trimmedTarget)) {
            val err = "Target user ID '$trimmedTarget' is not a valid Supabase UUID."
            Log.e(TAG, err)
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            if (auth == null) {
                val err = "Active administrator session required to revoke moderation. Please re-login."
                Log.e(TAG, err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            val effectiveReason = reason?.takeIf { it.isNotBlank() } ?: "Revoked by administrator"
            val req = AdminRevokeModerationActionRequest(
                p_target_user_id = trimmedTarget,
                p_reason = effectiveReason
            )

            Log.d(TAG, "Revoking moderation action: target=$trimmedTarget, reason=$effectiveReason")
            val response = SupabaseClient.api.adminRevokeModerationAction(auth = auth, body = req)

            if (response.isSuccessful) {
                Log.d(TAG, "adminRevokeModerationAction succeeded: code=${response.code()}")
                com.example.realtime.VoiceRoomRealtimeManager.broadcastModerationAction(
                    roomId = roomId,
                    targetUserId = trimmedTarget,
                    actionType = "revoked",
                    reason = effectiveReason
                )
                Result.success("Moderation action successfully revoked.")
            } else {
                val errBody = response.errorBody()?.string() ?: response.message()
                Log.w(TAG, "adminRevokeModerationAction RPC failed (${response.code()}): $errBody. Attempting direct status deletion fallback...")
                
                val delResponse = SupabaseClient.api.deleteUserModerationStatus(
                    auth = auth,
                    userIdEq = "eq.$trimmedTarget"
                )
                if (delResponse.isSuccessful) {
                    Log.d(TAG, "deleteUserModerationStatus fallback succeeded: code=${delResponse.code()}")
                    com.example.realtime.VoiceRoomRealtimeManager.broadcastModerationAction(
                        roomId = roomId,
                        targetUserId = trimmedTarget,
                        actionType = "revoked",
                        reason = effectiveReason
                    )
                    Result.success("Moderation action successfully revoked.")
                } else {
                    val delErr = delResponse.errorBody()?.string() ?: delResponse.message()
                    Log.e(TAG, "Both RPC and delete fallback failed: $delErr")
                    Result.failure(Exception("Failed to revoke: $errBody"))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception in revokeModerationAction", e)
            Result.failure(e)
        }
    }

    suspend fun fetchActiveModerationPenalties(context: Context): List<MyModerationStatus> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@withContext emptyList()
            val response = SupabaseClient.api.getAllActiveModerationStatuses(auth = auth)
            if (response.isSuccessful) {
                val list = response.body() ?: emptyList()
                return@withContext list.filter { isActionActive(it) }
            } else {
                Log.w(TAG, "getAllActiveModerationStatuses returned code ${response.code()}: ${response.errorBody()?.string()}")
                return@withContext emptyList()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching active moderation penalties", e)
            emptyList()
        }
    }

    suspend fun checkIsAdmin(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@withContext false
            val response = SupabaseClient.api.getMyUserRoles(auth = auth)
            if (response.isSuccessful) {
                val roles = response.body() ?: emptyList()
                val isAdmin = roles.any { item ->
                    val r = item.role?.lowercase()?.trim() ?: ""
                    r in listOf("admin", "superadmin")
                }
                Log.d(TAG, "checkIsAdmin: fetched roles=$roles, isAdmin=$isAdmin")
                return@withContext isAdmin
            } else {
                Log.w(TAG, "getMyUserRoles failed with code ${response.code()}: ${response.errorBody()?.string()}")
                return@withContext false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error checking admin role", e)
            false
        }
    }

    suspend fun checkIsSuperAdmin(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@withContext false
            val response = SupabaseClient.api.getMyUserRoles(auth = auth)
            if (response.isSuccessful) {
                val roles = response.body() ?: emptyList()
                return@withContext roles.any { item ->
                    item.role?.lowercase()?.trim() == "superadmin"
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking superadmin role", e)
            false
        }
    }
}
