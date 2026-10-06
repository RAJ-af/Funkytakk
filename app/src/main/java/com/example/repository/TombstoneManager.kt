package com.example.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.GroupTombstoneRow
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GroupTombstoneAlert(
    val id: String,
    val groupId: String,
    val groupName: String,
    val reason: String, // "deleted", "removed", "request_rejected"
    val deletedByName: String?,
    val title: String,
    val message: String
)

object TombstoneManager {
    private const val TAG = "TombstoneManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val pendingAlertsQueue = mutableListOf<GroupTombstoneAlert>()
    private val _currentAlert = MutableStateFlow<GroupTombstoneAlert?>(null)
    val currentAlert: StateFlow<GroupTombstoneAlert?> = _currentAlert.asStateFlow()

    fun checkTombstones(context: Context) {
        scope.launch {
            try {
                SupabaseSessionManager.ensureActiveSession(context)
                val authToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: (SupabaseSessionManager.getAccessToken(context)?.let { "Bearer $it" })
                    ?: return@launch

                val resp = SupabaseClient.api.getGroupTombstones(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = authToken
                )
                if (resp.isSuccessful) {
                    val rows = resp.body().orEmpty()
                    for (row in rows) {
                        enqueueTombstone(row)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking group tombstones: ${e.message}")
            }
        }
    }

    fun handleRealtimeTombstoneInsert(row: GroupTombstoneRow) {
        enqueueTombstone(row)
    }

    private fun enqueueTombstone(row: GroupTombstoneRow) {
        val byName = row.deleted_by_name?.takeIf { it.isNotBlank() } ?: "An admin"
        val grpName = row.group_name.takeIf { it.isNotBlank() } ?: "the group"

        val (title, message) = when (row.reason.lowercase()) {
            "deleted" -> "Group Deleted" to "$byName deleted the group $grpName"
            "removed" -> "Removed from Group" to "You were removed from $grpName by $byName"
            "request_rejected" -> "Join Request Declined" to "Your request to join $grpName was declined"
            else -> "Group Update" to "Group $grpName was updated ($byName)"
        }

        val alert = GroupTombstoneAlert(
            id = row.id,
            groupId = row.group_id,
            groupName = grpName,
            reason = row.reason.lowercase(),
            deletedByName = byName,
            title = title,
            message = message
        )

        synchronized(pendingAlertsQueue) {
            if (pendingAlertsQueue.none { it.id == alert.id } && _currentAlert.value?.id != alert.id) {
                pendingAlertsQueue.add(alert)
                if (_currentAlert.value == null) {
                    _currentAlert.value = pendingAlertsQueue.removeAt(0)
                }
            }
        }
    }

    suspend fun dismissCurrentAlert(context: Context) = withContext(Dispatchers.IO) {
        val active = _currentAlert.value ?: return@withContext
        try {
            // 1. If deleted/removed, archive local group conversation so it's read-only and removed from active list
            if (active.reason == "deleted" || active.reason == "removed") {
                EncryptedChatRepository.getDatabase(context)?.groupConversationDao()?.archiveGroup(
                    groupId = active.groupId,
                    reason = active.reason,
                    deletedByName = active.deletedByName
                )
            }

            // 2. Delete tombstone row in Supabase
            val authToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: (SupabaseSessionManager.getAccessToken(context)?.let { "Bearer $it" })
            if (authToken != null && active.id.isNotBlank()) {
                SupabaseClient.api.deleteGroupTombstone(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = authToken,
                    idFilter = "eq.${active.id}"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error dismissing tombstone ${active.id}: ${e.message}")
        } finally {
            synchronized(pendingAlertsQueue) {
                _currentAlert.value = if (pendingAlertsQueue.isNotEmpty()) pendingAlertsQueue.removeAt(0) else null
            }
        }
    }
}
