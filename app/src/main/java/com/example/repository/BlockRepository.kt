package com.example.repository

import android.content.Context
import android.util.Log
import com.example.BlockUserRequest
import com.example.BlockedUserRow
import com.example.BuildConfig
import com.example.ChatPreferencesManager
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

const val USER_BLOCKED_ERROR_MESSAGE = "You can't interact with this user."

class UserBlockedException(message: String = USER_BLOCKED_ERROR_MESSAGE) : Exception(message)

/**
 * Single source of truth for Supabase-backed user blocks (`user_blocks` table via RPCs).
 * Keeps in-memory StateFlows & ChatPreferencesManager synchronized so all UI lists filter locally
 * without per-item RPC calls.
 */
object BlockRepository {
    private const val TAG = "BlockRepository"

    private val _blockedUsers = MutableStateFlow<List<BlockedUserRow>>(emptyList())
    val blockedUsers: StateFlow<List<BlockedUserRow>> = _blockedUsers.asStateFlow()

    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun isUserBlockedError(rawError: String?): Boolean {
        if (rawError.isNullOrBlank()) return false
        return rawError.contains("user_blocked", ignoreCase = true)
    }

    fun isBlockedLocally(userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        return _blockedUserIds.value.contains(userId)
    }

    fun init(context: Context) {
        val appCtx = context.applicationContext
        ChatPreferencesManager.init(appCtx)
        val cachedIds = ChatPreferencesManager.blockedUsers.value
        if (cachedIds.isNotEmpty() && _blockedUserIds.value.isEmpty()) {
            _blockedUserIds.value = cachedIds
        }
    }

    suspend fun refreshBlockedUsers(context: Context): Result<List<BlockedUserRow>> = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        init(appCtx)
        _isLoading.value = true
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)
            if (token.isNullOrBlank()) {
                _isLoading.value = false
                return@withContext Result.failure(IllegalStateException("Not authenticated"))
            }
            val resp = SupabaseClient.api.getMyBlockedUsersRpc(auth = token)
            if (resp.isSuccessful) {
                val list = resp.body().orEmpty()
                val ids = list.map { it.user_id }.filter { it.isNotBlank() }.toSet()
                _blockedUsers.value = list
                _blockedUserIds.value = ids
                ChatPreferencesManager.replaceBlockedUsers(appCtx, ids)
                _isLoading.value = false
                Result.success(list)
            } else {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "get_my_blocked_users failed: HTTP ${resp.code()}")
                }
                _isLoading.value = false
                Result.failure(Exception("Could not load blocked users (${resp.code()})"))
            }
        } catch (e: CancellationException) {
            _isLoading.value = false
            throw e
        } catch (e: Exception) {
            _isLoading.value = false
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "get_my_blocked_users exception: ${e.javaClass.simpleName}")
            }
            Result.failure(e)
        }
    }

    suspend fun blockUser(context: Context, targetUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        init(appCtx)
        if (targetUserId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid user ID"))
        }
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)
                ?: return@withContext Result.failure(IllegalStateException("Please sign in to block users."))
            val resp = SupabaseClient.api.blockUserRpc(
                auth = token,
                body = BlockUserRequest(p_user_id = targetUserId)
            )
            if (resp.isSuccessful) {
                val updatedIds = _blockedUserIds.value + targetUserId
                _blockedUserIds.value = updatedIds
                ChatPreferencesManager.blockUser(appCtx, targetUserId)
                refreshBlockedUsers(appCtx)
                Result.success(Unit)
            } else {
                val errBody = try {
                    resp.errorBody()?.string().orEmpty()
                } catch (_: Exception) {
                    ""
                }
                val msg = when {
                    errBody.contains("cannot_block_yourself", ignoreCase = true) -> "You can't block yourself."
                    errBody.contains("user_not_found", ignoreCase = true) -> "User not found."
                    else -> "Could not block user. Please try again."
                }
                Result.failure(Exception(msg))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception("Could not block user. Please try again."))
        }
    }

    suspend fun unblockUser(context: Context, targetUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        init(appCtx)
        if (targetUserId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid user ID"))
        }
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)
                ?: return@withContext Result.failure(IllegalStateException("Please sign in to unblock users."))
            val resp = SupabaseClient.api.unblockUserRpc(
                auth = token,
                body = BlockUserRequest(p_user_id = targetUserId)
            )
            if (resp.isSuccessful) {
                _blockedUserIds.value = _blockedUserIds.value - targetUserId
                _blockedUsers.value = _blockedUsers.value.filterNot { it.user_id == targetUserId }
                ChatPreferencesManager.unblockUser(appCtx, targetUserId)
                refreshBlockedUsers(appCtx)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Could not unblock user. Please try again."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception("Could not unblock user. Please try again."))
        }
    }

    suspend fun isBlockedWith(context: Context, targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        if (targetUserId.isBlank()) return@withContext false
        if (_blockedUserIds.value.contains(targetUserId)) return@withContext true
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)
            if (token.isNullOrBlank()) return@withContext false
            val resp = SupabaseClient.api.isBlockedWithRpc(
                auth = token,
                body = BlockUserRequest(p_user_id = targetUserId)
            )
            if (!resp.isSuccessful) return@withContext _blockedUserIds.value.contains(targetUserId)
            val raw = resp.body()?.string()?.trim().orEmpty()
            raw.equals("true", ignoreCase = true)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _blockedUserIds.value.contains(targetUserId)
        }
    }

    fun resetForLogout() {
        _blockedUsers.value = emptyList()
        _blockedUserIds.value = emptySet()
        _isLoading.value = false
    }
}
