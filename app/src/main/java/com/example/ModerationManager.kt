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

object ModerationManager {
    private const val TAG = "ModerationManager"

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
                // Broadcast moderation action in realtime across the room
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
                Result.failure(Exception("Failed (${response.code()}): $errBody"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception in applyModerationAction", e)
            Result.failure(e)
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
                // Broadcast moderation revocation in realtime
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
                
                // Fallback attempt: delete directly from user_moderation_status table
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
                    r in listOf("admin", "moderator", "superadmin")
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
}
