package com.example.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.*
import com.example.crypto.E2EEManager
import com.example.crypto.E2EEMediaManager
import com.example.crypto.VaultSecurityManager
import com.example.crypto.VaultStatus
import com.example.db.EncryptedChatDatabase
import com.example.db.LocalGroupConversation
import com.example.db.LocalGroupMessage
import com.example.realtime.RealtimeTableEvent
import com.example.realtime.VoiceRoomRealtimeManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class GroupOverviewData(
    val groupId: String,
    val name: String,
    val description: String = "",
    val avatarUrl: String = "",
    val createdBy: String = "",
    val joinedCount: Int = 1,
    val pendingRequests: Int = 0,
    val myRole: String = "member",
    val myStatus: String = "joined",
    val mutedUntil: Long? = null,
    val notifyLevel: String = "all",
    val mediaAutoDownload: Boolean = true,
    val whoCanSend: String = "everyone",
    val whoCanEditInfo: String = "admins",
    val whoCanAddMembers: String = "admins",
    val approveNewMembers: Boolean = false,
    val showJoinNotifications: Boolean = true,
    val disappearingSeconds: Int = 0,
    val memberListVisibility: String = "members"
)

data class GroupInviteInfo(
    val code: String,
    val enabled: Boolean = true,
    val deepLink: String = "funkytalk://group/$code",
    val httpsLink: String = "https://funkytalk.app/group/$code"
)

data class GroupInvitePreview(
    val code: String,
    val groupId: String = "",
    val name: String = "Group Chat",
    val description: String = "",
    val avatarUrl: String = "",
    val joinedCount: Int = 1,
    val approveNewMembers: Boolean = false,
    val myStatus: String? = null
)

data class JoinGroupByInviteResult(
    val groupId: String,
    val status: String // "joined" | "requested"
)

data class SendGroupMessageOutcome(
    val messageId: String,
    val skippedCount: Int = 0
)

object GroupChatRepository {
    private const val TAG = "GroupChatRepo"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var database: EncryptedChatDatabase? = null
    private val _databaseState = MutableStateFlow<EncryptedChatDatabase?>(null)

    private val _joinedGroups = MutableStateFlow<List<LocalGroupConversation>>(emptyList())
    val joinedGroups: StateFlow<List<LocalGroupConversation>> = _joinedGroups.asStateFlow()

    private val _pendingInvites = MutableStateFlow<List<LocalGroupConversation>>(emptyList())
    val pendingInvites: StateFlow<List<LocalGroupConversation>> = _pendingInvites.asStateFlow()

    private val _allMyGroups = MutableStateFlow<List<LocalGroupConversation>>(emptyList())
    val allMyGroups: StateFlow<List<LocalGroupConversation>> = _allMyGroups.asStateFlow()

    // Deep-link invite code state (funkytalk://group/<code>)
    private val _pendingInviteCode = MutableStateFlow<String?>(null)
    val pendingInviteCode: StateFlow<String?> = _pendingInviteCode.asStateFlow()

    fun setPendingInviteCode(code: String?) {
        _pendingInviteCode.value = code?.trim()?.takeIf { it.isNotBlank() }
    }

    // Emits groupId whenever group_members, group_conversations, or group_events update for live UI refresh
    private val _liveGroupUpdates = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val liveGroupUpdates: SharedFlow<String> = _liveGroupUpdates.asSharedFlow()

    private val processedRecipientIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val processedEventIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    private var groupCollectJob: Job? = null
    private var inviteCollectJob: Job? = null
    private var allGroupsCollectJob: Job? = null
    private var realtimeListenerJob: Job? = null
    private var expiryCleanupJob: Job? = null

    @Volatile
    private var isInitialized = false

    /**
     * Maps backend RPC exception / error strings to friendly user-facing messages.
     */
    fun mapGroupError(raw: String?, fallback: String = "Something went wrong. Please try again."): String {
        if (raw.isNullOrBlank()) return fallback
        val lower = raw.lowercase()
        return when {
            lower.contains("not_a_member") -> "You are no longer a member of this group."
            lower.contains("admin_required") -> "Only group admins can perform this action."
            lower.contains("not_allowed_to_add_members") -> "Only admins are allowed to add members to this group."
            lower.contains("not_allowed_to_edit_info") -> "Only admins are allowed to edit group info."
            lower.contains("only_admins_can_send") -> "Only admins can send messages in this group."
            lower.contains("group_full") -> "This group has reached the maximum limit of 100 members."
            lower.contains("user_blocked_group") -> "This user cannot be invited because they blocked this group."
            lower.contains("already_member") -> "This user is already a member or has a pending invite."
            lower.contains("invalid_invite") -> "This invite link is invalid or has expired."
            lower.contains("invite_disabled") -> "Invite links are currently disabled for this group."
            lower.contains("cannot_remove_creator") -> "The group creator cannot be removed or demoted."
            lower.contains("last_admin") -> "Cannot complete action as you are the last admin."
            lower.contains("invalid_group_name") -> "Please enter a valid group name (1–60 characters)."
            else -> {
                try {
                    val json = JSONObject(raw)
                    val msg = json.optString("message").ifBlank { json.optString("error").ifBlank { json.optString("details") } }
                    if (msg.isNotBlank() && msg != raw) {
                        return mapGroupError(msg, msg)
                    }
                } catch (_: Exception) {}
                raw.take(140).ifBlank { fallback }
            }
        }
    }

    fun formatDisappearingLabel(seconds: Int): String {
        return when (seconds) {
            86400 -> "24 hours"
            604800 -> "7 days"
            7776000 -> "90 days"
            else -> "Off"
        }
    }

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext
        scope.launch {
            VaultSecurityManager.vaultStatus.collect { status ->
                when (status) {
                    VaultStatus.UNLOCKED -> {
                        val key = VaultSecurityManager.getActiveVaultKey()
                        if (key != null) {
                            openDatabase(appContext, key)
                        }
                    }
                    VaultStatus.LOCKED, VaultStatus.NOT_CONFIGURED -> {
                        closeDatabase()
                    }
                }
            }
        }

        startRealtimeListener(appContext)
        startDisappearingExpiryWorker()
    }

    private fun startDisappearingExpiryWorker() {
        expiryCleanupJob?.cancel()
        expiryCleanupJob = scope.launch {
            while (isActive) {
                try {
                    database?.groupMessageDao()?.deleteExpiredMessages(System.currentTimeMillis())
                } catch (_: Exception) {}
                delay(15_000L)
            }
        }
    }

    private fun openDatabase(context: Context, vaultKey: ByteArray) {
        try {
            val db = EncryptedChatDatabase.getInstance(context, vaultKey)
            database = db
            _databaseState.value = db

            scope.launch {
                try {
                    db.groupMessageDao().deleteExpiredMessages(System.currentTimeMillis())
                } catch (_: Exception) {}
            }

            groupCollectJob?.cancel()
            groupCollectJob = scope.launch {
                db.groupConversationDao().getAllJoinedGroups().collect { list ->
                    _joinedGroups.value = list
                }
            }

            inviteCollectJob?.cancel()
            inviteCollectJob = scope.launch {
                db.groupConversationDao().getPendingInvites().collect { list ->
                    _pendingInvites.value = list
                }
            }

            allGroupsCollectJob?.cancel()
            allGroupsCollectJob = scope.launch {
                db.groupConversationDao().getAllMyGroups().collect { list ->
                    _allMyGroups.value = list
                }
            }

            scope.launch {
                val myUserId = SupabaseSessionManager.getUserId(context)
                if (!myUserId.isNullOrBlank()) {
                    VoiceRoomRealtimeManager.subscribeGroupInbox(myUserId)
                    syncUserGroups(context)
                    fetchAndProcessPendingGroupMessages(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening GroupChatDatabase", e)
        }
    }

    fun closeDatabase() {
        groupCollectJob?.cancel()
        inviteCollectJob?.cancel()
        allGroupsCollectJob?.cancel()
        groupCollectJob = null
        inviteCollectJob = null
        allGroupsCollectJob = null
        database = null
        _databaseState.value = null
        _joinedGroups.value = emptyList()
        _pendingInvites.value = emptyList()
        _allMyGroups.value = emptyList()
    }

    private fun startRealtimeListener(context: Context) {
        realtimeListenerJob?.cancel()
        realtimeListenerJob = scope.launch {
            VoiceRoomRealtimeManager.events.collect { event ->
                when (event) {
                    is RealtimeTableEvent.GroupMessageReceived -> {
                        val myUserId = SupabaseSessionManager.getUserId(context)
                        if (!myUserId.isNullOrBlank() && (event.recipientId == null || event.recipientId == myUserId)) {
                            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                            if (!token.isNullOrBlank() && !event.rowId.isNullOrBlank() && !event.ciphertext.isNullOrBlank() && !event.groupId.isNullOrBlank()) {
                                // Single-event fast path: row carries group_id, sender_id, message_type, ciphertext!
                                val row = SupabaseGroupRecipientRow(
                                    id = event.rowId,
                                    message_id = event.messageId.orEmpty().ifBlank { event.rowId },
                                    recipient_id = event.recipientId ?: myUserId,
                                    group_id = event.groupId,
                                    sender_id = event.senderId,
                                    message_type = event.messageType ?: "text",
                                    ciphertext = event.ciphertext,
                                    nonce = event.nonce.orEmpty(),
                                    storage_path = event.storagePath
                                )
                                processSingleRecipientRow(context, token, myUserId, row)
                            } else {
                                fetchAndProcessPendingGroupMessages(context)
                            }
                        }
                    }
                    is RealtimeTableEvent.GroupEventInserted -> {
                        handleRealtimeGroupEvent(context, event)
                    }
                    is RealtimeTableEvent.GroupMemberChanged -> {
                        val gid = event.groupId
                        if (!gid.isNullOrBlank()) {
                            syncUserGroups(context)
                            getGroupOverview(context, gid)
                            _liveGroupUpdates.tryEmit(gid)
                        }
                    }
                    is RealtimeTableEvent.GroupConversationChanged -> {
                        val gid = event.groupId
                        if (!gid.isNullOrBlank()) {
                            syncUserGroups(context)
                            getGroupOverview(context, gid)
                            _liveGroupUpdates.tryEmit(gid)
                        }
                    }
                    is RealtimeTableEvent.NotificationChanged -> {
                        val notifType = event.record?.optString("type")?.lowercase()
                        if (notifType == "group_invite" || notifType == "group_join_request") {
                            Log.d(TAG, "Group notification ($notifType) received; syncing groups...")
                            syncUserGroups(context)
                            val gid = event.record?.optString("group_id")
                            if (!gid.isNullOrBlank()) {
                                _liveGroupUpdates.tryEmit(gid)
                            }
                        }
                    }
                    is RealtimeTableEvent.Reconnected -> {
                        syncUserGroups(context)
                        fetchAndProcessPendingGroupMessages(context)
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun handleRealtimeGroupEvent(context: Context, event: RealtimeTableEvent.GroupEventInserted) {
        val groupId = event.groupId?.takeIf { it.isNotBlank() } ?: return
        val eventId = event.record?.optString("id") ?: "${groupId}_${event.groupEventType}_${System.currentTimeMillis()}"
        if (!processedEventIds.add(eventId)) return

        val db = database ?: return
        val localGroup = db.groupConversationDao().getGroup(groupId)
        val showJoinNotifs = localGroup?.showJoinNotifications ?: true
        val evType = event.groupEventType?.lowercase().orEmpty()

        // Sync overview & group list
        getGroupOverview(context, groupId)
        syncUserGroups(context)
        _liveGroupUpdates.tryEmit(groupId)

        if ((evType == "joined" || evType == "left") && !showJoinNotifs) {
            return
        }

        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context).orEmpty()
        val actorName = if (!event.actorId.isNullOrBlank() && token.isNotBlank()) {
            resolveSenderName(context, token, event.actorId)
        } else "A member"
        val targetName = if (!event.targetUserId.isNullOrBlank() && token.isNotBlank()) {
            resolveSenderName(context, token, event.targetUserId)
        } else null

        val systemText = when (evType) {
            "created" -> "$actorName created this group"
            "joined" -> "${targetName ?: actorName} joined the group"
            "left" -> "${targetName ?: actorName} left the group"
            "removed" -> if (targetName != null) "$actorName removed $targetName" else "A member was removed"
            "promoted" -> if (targetName != null) "$targetName is now an admin" else "$actorName promoted a member to admin"
            "demoted" -> if (targetName != null) "$targetName is no longer an admin" else "Admin privileges were updated"
            "info_updated" -> "$actorName updated the group info"
            "settings_updated" -> {
                val disSec = event.meta?.optInt("disappearing_seconds", -1) ?: -1
                if (disSec >= 0) {
                    val label = formatDisappearingLabel(disSec)
                    if (disSec == 0) "$actorName turned off disappearing messages"
                    else "$actorName set disappearing messages to $label"
                } else {
                    "$actorName updated group settings"
                }
            }
            "invite_reset" -> "$actorName reset the group invite link"
            else -> return
        }

        insertSystemMessage(groupId, systemText, eventId)
    }

    suspend fun insertSystemMessage(groupId: String, text: String, customId: String? = null) {
        val db = database ?: return
        val cleanGroupId = groupId.trim().replace("\"", "")
        val msgId = customId ?: "sys_${UUID.randomUUID()}"
        if (db.groupMessageDao().messageExists(msgId)) return
        val group = db.groupConversationDao().getGroup(cleanGroupId)
        val disSec = group?.disappearingSeconds ?: 0
        val now = System.currentTimeMillis()
        val expiresAt = if (disSec > 0) now + disSec * 1000L else null

        val sysMsg = LocalGroupMessage(
            id = msgId,
            groupId = cleanGroupId,
            senderId = "system",
            senderName = "System",
            senderAvatar = null,
            text = text,
            messageType = "system",
            timestamp = now,
            expiresAt = expiresAt,
            isOutgoing = false
        )
        db.groupMessageDao().insertMessage(sysMsg)
        db.groupConversationDao().updateLastMessage(
            groupId = cleanGroupId,
            text = text,
            senderName = "",
            time = now,
            unreadDelta = 0
        )
    }

    // ========================================================================
    // 1. LIST / CREATE RPCs
    // ========================================================================

    /**
     * Calls get_my_groups() RPC -> rows:
     * (group_id, name, avatar_url, my_role, my_status [joined|invited|requested],
     *  joined_count, muted_until, notify_level, who_can_send, disappearing_seconds, created_at)
     */
    suspend fun syncUserGroups(context: Context) = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@withContext
            val myUserId = SupabaseSessionManager.getUserId(context) ?: return@withContext

            val resp = SupabaseClient.api.getMyGroups(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token
            )

            if (!resp.isSuccessful || resp.body() == null) {
                Log.w(TAG, "get_my_groups RPC failed (${resp.code()}): ${resp.errorBody()?.string()}")
                return@withContext
            }

            val rows = resp.body()!!
            val db = database ?: return@withContext
            val validIds = mutableListOf<String>()

            for (row in rows) {
                val gid = row.group_id.trim()
                if (gid.isBlank()) continue
                validIds.add(gid)

                val existing = db.groupConversationDao().getGroup(gid)
                val status = row.my_status?.lowercase() ?: "joined"
                val joinedCount = (row.joined_count ?: existing?.memberCount ?: 1).coerceAtLeast(1)
                val mutedEpoch = parseIsoTimestampToMillis(row.muted_until)
                val createdMillis = parseIsoTimestampToMillis(row.created_at) ?: existing?.createdAt ?: System.currentTimeMillis()

                val defaultPreview = when (status) {
                    "invited" -> "Invited to join group"
                    "requested" -> "Waiting for admin approval"
                    else -> "End-to-end encrypted group chat"
                }

                val localConv = LocalGroupConversation(
                    id = gid,
                    name = row.name.ifBlank { existing?.name ?: "Group Chat" },
                    description = existing?.description.orEmpty(),
                    avatarUrl = row.avatar_url.orEmpty().ifBlank { existing?.avatarUrl.orEmpty() },
                    createdBy = existing?.createdBy.orEmpty(),
                    myRole = row.my_role ?: existing?.myRole ?: "member",
                    myStatus = status,
                    memberCount = joinedCount,
                    pendingRequestsCount = existing?.pendingRequestsCount ?: 0,
                    mutedUntil = mutedEpoch,
                    notifyLevel = row.notify_level ?: existing?.notifyLevel ?: "all",
                    mediaAutoDownload = existing?.mediaAutoDownload ?: true,
                    whoCanSend = row.who_can_send ?: existing?.whoCanSend ?: "everyone",
                    whoCanEditInfo = existing?.whoCanEditInfo ?: "admins",
                    whoCanAddMembers = existing?.whoCanAddMembers ?: "admins",
                    approveNewMembers = existing?.approveNewMembers ?: false,
                    showJoinNotifications = existing?.showJoinNotifications ?: true,
                    disappearingSeconds = row.disappearing_seconds ?: existing?.disappearingSeconds ?: 0,
                    memberListVisibility = existing?.memberListVisibility ?: "members",
                    lastMessageText = existing?.lastMessageText?.takeIf {
                        it.isNotBlank() &&
                            !(status == "joined" && (it == "Invited to join group" || it == "Waiting for admin approval"))
                    } ?: defaultPreview,
                    lastMessageSenderName = existing?.lastMessageSenderName.orEmpty(),
                    lastMessageTime = existing?.lastMessageTime ?: createdMillis,
                    unreadCount = existing?.unreadCount ?: 0,
                    createdAt = createdMillis
                )
                db.groupConversationDao().insertOrUpdate(localConv)

                if (status == "joined") {
                    VoiceRoomRealtimeManager.subscribeGroupRealtime(gid)
                }
            }

            if (validIds.isEmpty()) {
                db.groupConversationDao().deleteAllGroups()
            } else {
                db.groupConversationDao().deleteGroupsNotIn(validIds)
            }
            VoiceRoomRealtimeManager.subscribeGroupInbox(myUserId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncUserGroups (get_my_groups)", e)
        }
    }

    /**
     * Calls create_group(p_name, p_member_ids uuid[], p_description, p_avatar_url) -> group id
     * Max name 60 chars, max 99 extra members.
     * If localAvatarBytes is provided, uploads via upload-to-appwrite (folder group-media/{groupId})
     * and calls update_group_settings({avatar_url}).
     */
    suspend fun createGroup(
        context: Context,
        name: String,
        memberUserIds: List<String>,
        description: String? = null,
        avatarUrl: String? = null,
        localAvatarBytes: ByteArray? = null,
        localAvatarMime: String = "image/jpeg"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanName = name.trim()
            if (cleanName.isEmpty() || cleanName.length > 60) {
                return@withContext Result.failure(Exception(mapGroupError("invalid_group_name")))
            }

            SupabaseSessionManager.ensureActiveSession(context)
            val myUserId = SupabaseSessionManager.getUserId(context)
                ?: return@withContext Result.failure(Exception("Not logged in"))
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val extraMembers = memberUserIds.map { it.trim() }.filter { it.isNotBlank() && it != myUserId }.distinct()
            if (extraMembers.size > 99) {
                return@withContext Result.failure(Exception(mapGroupError("group_full")))
            }

            val remoteAvatarUrl = if (localAvatarBytes == null && !avatarUrl.isNullOrBlank() && avatarUrl.startsWith("http")) {
                avatarUrl
            } else null

            val resp = SupabaseClient.api.createGroup(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = CreateGroupRequest(
                    p_name = cleanName,
                    p_member_ids = extraMembers,
                    p_description = description?.trim()?.takeIf { it.isNotBlank() },
                    p_avatar_url = remoteAvatarUrl
                )
            )

            if (!resp.isSuccessful) {
                val err = resp.errorBody()?.string()
                return@withContext Result.failure(Exception(mapGroupError(err, "Failed to create group")))
            }

            val bodyStr = resp.body()?.string()?.trim().orEmpty()
            val groupId = parseGroupId(bodyStr)
                ?: return@withContext Result.failure(Exception("Invalid group ID returned from server"))

            var finalAvatarUrl = remoteAvatarUrl.orEmpty()
            if (localAvatarBytes != null && localAvatarBytes.isNotEmpty()) {
                val uploadRes = uploadAndSetGroupAvatar(context, groupId, localAvatarBytes, localAvatarMime)
                if (uploadRes.isSuccess) {
                    finalAvatarUrl = uploadRes.getOrNull().orEmpty()
                }
            }

            val now = System.currentTimeMillis()
            val localGroup = LocalGroupConversation(
                id = groupId,
                name = cleanName,
                description = description?.trim().orEmpty(),
                avatarUrl = finalAvatarUrl,
                createdBy = myUserId,
                myRole = "admin",
                myStatus = "joined",
                memberCount = 1,
                lastMessageText = "Group created",
                lastMessageSenderName = "You",
                lastMessageTime = now,
                createdAt = now
            )
            database?.groupConversationDao()?.insertOrUpdate(localGroup)
            insertSystemMessage(groupId, "You created the group \"$cleanName\"")

            VoiceRoomRealtimeManager.subscribeGroupRealtime(groupId)
            syncUserGroups(context)
            getGroupOverview(context, groupId)

            Result.success(groupId)
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating group", e)
            Result.failure(Exception(mapGroupError(e.message, "Failed to create group")))
        }
    }

    private fun parseGroupId(body: String): String? {
        if (body.isBlank()) return null
        if (!body.startsWith("{") && !body.startsWith("[")) {
            val clean = body.replace("\"", "").trim()
            if (clean.length > 10) return clean
        }
        try {
            val json = JSONObject(body)
            if (json.has("group_id")) return json.getString("group_id")
            if (json.has("id")) return json.getString("id")
        } catch (_: Exception) {}
        return null
    }

    /**
     * Accept group invitation: accept_group_invite(p_group_id)
     */
    suspend fun acceptGroupInvite(context: Context, groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val sessionToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated. Please log in again."))

            val targetGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.acceptGroupInvite(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = sessionToken,
                body = AcceptGroupInviteRequest(p_group_id = targetGroupId)
            )

            if (resp.isSuccessful) {
                database?.groupConversationDao()?.updateStatus(targetGroupId, "joined")
                VoiceRoomRealtimeManager.subscribeGroupRealtime(targetGroupId)
                syncUserGroups(context)
                getGroupOverview(context, targetGroupId)
                fetchAndProcessPendingGroupMessages(context)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to accept invite")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to accept invite")))
        }
    }

    /**
     * Decline group invitation: decline_group_invite(p_group_id)
     */
    suspend fun declineGroupInvite(context: Context, groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val targetGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.declineGroupInvite(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = DeclineGroupInviteRequest(p_group_id = targetGroupId)
            )

            if (resp.isSuccessful) {
                database?.groupConversationDao()?.deleteGroup(targetGroupId)
                syncUserGroups(context)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to decline invite")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to decline invite")))
        }
    }

    // ========================================================================
    // 2. GROUP INFO / SETTINGS RPCs
    // ========================================================================

    /**
     * Calls get_group_overview(p_group_id) -> jsonb with name, description, avatar_url,
     * joined_count, pending_requests (admins only), my_role, my_status, muted_until,
     * notify_level, media_auto_download and all group settings.
     * Uses joined_count for every "N members" label.
     */
    suspend fun getGroupOverview(context: Context, groupId: String): Result<GroupOverviewData> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.getGroupOverview(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GroupIdRequest(p_group_id = cleanGroupId)
            )

            if (!resp.isSuccessful) {
                val err = resp.errorBody()?.string()
                return@withContext Result.failure(Exception(mapGroupError(err, "Failed to load group overview")))
            }

            val rawBody = resp.body()?.string()?.trim().orEmpty()
            val json = when {
                rawBody.startsWith("[") -> JSONArray(rawBody).optJSONObject(0) ?: JSONObject()
                rawBody.startsWith("{") -> JSONObject(rawBody)
                else -> JSONObject()
            }

            val existing = database?.groupConversationDao()?.getGroup(cleanGroupId)
            val name = json.optString("name", existing?.name ?: "Group Chat")
            val description = if (json.isNull("description")) "" else json.optString("description", existing?.description.orEmpty())
            val avatarUrl = if (json.isNull("avatar_url")) "" else json.optString("avatar_url", existing?.avatarUrl.orEmpty())
            val createdBy = if (json.isNull("created_by")) existing?.createdBy.orEmpty() else json.optString("created_by", existing?.createdBy.orEmpty())
            val joinedCount = json.optInt("joined_count", existing?.memberCount ?: 1).coerceAtLeast(1)

            val pendingRequests = when (val pr = json.opt("pending_requests")) {
                is Number -> pr.toInt()
                is JSONArray -> pr.length()
                else -> json.optInt("pending_requests_count", existing?.pendingRequestsCount ?: 0)
            }

            val myRole = json.optString("my_role", existing?.myRole ?: "member")
            val myStatus = json.optString("my_status", existing?.myStatus ?: "joined")
            val mutedUntilStr = if (json.isNull("muted_until")) null else json.optString("muted_until")
            val mutedUntil = parseIsoTimestampToMillis(mutedUntilStr)
            val notifyLevel = json.optString("notify_level", existing?.notifyLevel ?: "all")
            val mediaAutoDownload = if (json.has("media_auto_download") && !json.isNull("media_auto_download")) {
                json.optBoolean("media_auto_download", true)
            } else {
                existing?.mediaAutoDownload ?: true
            }

            val whoCanSend = json.optString("who_can_send", existing?.whoCanSend ?: "everyone")
            val whoCanEditInfo = json.optString("who_can_edit_info", existing?.whoCanEditInfo ?: "admins")
            val whoCanAddMembers = json.optString("who_can_add_members", existing?.whoCanAddMembers ?: "admins")
            val approveNewMembers = json.optBoolean("approve_new_members", existing?.approveNewMembers ?: false)
            val showJoinNotifications = json.optBoolean("show_join_notifications", existing?.showJoinNotifications ?: true)
            val disappearingSeconds = json.optInt("disappearing_seconds", existing?.disappearingSeconds ?: 0)
            val memberListVisibility = json.optString("member_list_visibility", existing?.memberListVisibility ?: "members")

            // If disappearing_seconds changed from existing local value, insert a system message
            if (existing != null && existing.disappearingSeconds != disappearingSeconds) {
                val label = formatDisappearingLabel(disappearingSeconds)
                val sysText = if (disappearingSeconds == 0) {
                    "Disappearing messages were turned off"
                } else {
                    "Disappearing messages were set to $label"
                }
                insertSystemMessage(cleanGroupId, sysText)
            }

            val overview = GroupOverviewData(
                groupId = cleanGroupId,
                name = name,
                description = description,
                avatarUrl = avatarUrl,
                createdBy = createdBy,
                joinedCount = joinedCount,
                pendingRequests = pendingRequests,
                myRole = myRole,
                myStatus = myStatus,
                mutedUntil = mutedUntil,
                notifyLevel = notifyLevel,
                mediaAutoDownload = mediaAutoDownload,
                whoCanSend = whoCanSend,
                whoCanEditInfo = whoCanEditInfo,
                whoCanAddMembers = whoCanAddMembers,
                approveNewMembers = approveNewMembers,
                showJoinNotifications = showJoinNotifications,
                disappearingSeconds = disappearingSeconds,
                memberListVisibility = memberListVisibility
            )

            val updatedLocal = LocalGroupConversation(
                id = cleanGroupId,
                name = name,
                description = description,
                avatarUrl = avatarUrl,
                createdBy = createdBy,
                myRole = myRole,
                myStatus = myStatus,
                memberCount = joinedCount,
                pendingRequestsCount = pendingRequests,
                mutedUntil = mutedUntil,
                notifyLevel = notifyLevel,
                mediaAutoDownload = mediaAutoDownload,
                whoCanSend = whoCanSend,
                whoCanEditInfo = whoCanEditInfo,
                whoCanAddMembers = whoCanAddMembers,
                approveNewMembers = approveNewMembers,
                showJoinNotifications = showJoinNotifications,
                disappearingSeconds = disappearingSeconds,
                memberListVisibility = memberListVisibility,
                lastMessageText = existing?.lastMessageText ?: "End-to-end encrypted group chat",
                lastMessageSenderName = existing?.lastMessageSenderName.orEmpty(),
                lastMessageTime = existing?.lastMessageTime ?: System.currentTimeMillis(),
                unreadCount = existing?.unreadCount ?: 0,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            database?.groupConversationDao()?.insertOrUpdate(updatedLocal)

            Result.success(overview)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getGroupOverview", e)
            Result.failure(Exception(mapGroupError(e.message, "Failed to load group overview")))
        }
    }

    /**
     * Calls update_group_settings(p_group_id, p_patch jsonb) with ONLY changed keys.
     */
    suspend fun updateGroupSettings(
        context: Context,
        groupId: String,
        patch: Map<String, Any?>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (patch.isEmpty()) return@withContext Result.success(Unit)
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.updateGroupSettings(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = UpdateGroupSettingsRequest(
                    p_group_id = cleanGroupId,
                    p_patch = patch
                )
            )

            if (resp.isSuccessful) {
                if (patch.containsKey("disappearing_seconds")) {
                    val newSec = (patch["disappearing_seconds"] as? Number)?.toInt() ?: 0
                    val label = formatDisappearingLabel(newSec)
                    val sysText = if (newSec == 0) "You turned off disappearing messages"
                    else "You set disappearing messages to $label"
                    insertSystemMessage(cleanGroupId, sysText)
                }
                getGroupOverview(context, cleanGroupId)
                syncUserGroups(context)
                _liveGroupUpdates.tryEmit(cleanGroupId)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to update group settings")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to update group settings")))
        }
    }

    /**
     * Calls update_my_group_prefs(p_group_id, p_muted_until, p_clear_mute, p_notify_level, p_media_auto_download)
     */
    suspend fun updateMyGroupPrefs(
        context: Context,
        groupId: String,
        mutedUntilIso: String? = null,
        clearMute: Boolean? = null,
        notifyLevel: String? = null,
        mediaAutoDownload: Boolean? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.updateMyGroupPrefs(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = UpdateMyGroupPrefsRequest(
                    p_group_id = cleanGroupId,
                    p_muted_until = mutedUntilIso,
                    p_clear_mute = clearMute,
                    p_notify_level = notifyLevel,
                    p_media_auto_download = mediaAutoDownload
                )
            )

            if (resp.isSuccessful) {
                getGroupOverview(context, cleanGroupId)
                syncUserGroups(context)
                _liveGroupUpdates.tryEmit(cleanGroupId)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to update personal group preferences")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to update personal group preferences")))
        }
    }

    /**
     * Uploads group avatar via upload-to-appwrite edge function (folder group-media/{groupId}),
     * then calls update_group_settings({avatar_url}).
     */
    suspend fun uploadAndSetGroupAvatar(
        context: Context,
        groupId: String,
        fileBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanGroupId = groupId.trim().replace("\"", "")
            val ext = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val uploadRes = AppwriteUploader.upload(
                context = context,
                fileBytes = fileBytes,
                filename = "group_avatar_${System.currentTimeMillis()}.$ext",
                mimeType = mimeType,
                purpose = "group-media",
                folderSuffix = cleanGroupId
            )

            when (uploadRes) {
                is AppwriteUploader.UploadResult.Success -> {
                    val url = uploadRes.url
                    val patchRes = updateGroupSettings(context, cleanGroupId, mapOf("avatar_url" to url))
                    if (patchRes.isSuccess) {
                        Result.success(url)
                    } else {
                        Result.failure(patchRes.exceptionOrNull() ?: Exception("Failed to update group avatar setting"))
                    }
                }
                is AppwriteUploader.UploadResult.Error -> {
                    Result.failure(Exception(uploadRes.message))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to upload group avatar")))
        }
    }

    // ========================================================================
    // 3. MEMBERS RPCs
    // ========================================================================

    /**
     * Calls get_group_members(p_group_id) -> (user_id, role, status, joined_at, username, profile_name, avatar_url, presence_status).
     */
    suspend fun getGroupMembersResult(context: Context, groupId: String): Result<List<SupabaseGroupMember>> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))
            val cleanGroupId = groupId.trim().replace("\"", "")

            val resp = SupabaseClient.api.getGroupMembersRpc(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GroupIdRequest(p_group_id = cleanGroupId)
            )

            if (!resp.isSuccessful) {
                val err = resp.errorBody()?.string()
                return@withContext Result.failure(Exception(mapGroupError(err, "Failed to load group members")))
            }

            val list = resp.body().orEmpty()
            // Cache member names and avatars for message resolution
            for (m in list) {
                val displayName = m.profile_name?.takeIf { it.isNotBlank() }
                    ?: m.username?.takeIf { it.isNotBlank() }
                if (!displayName.isNullOrBlank()) {
                    userProfileCache[m.user_id] = displayName to m.avatar_url
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching group members via RPC", e)
            Result.failure(Exception(mapGroupError(e.message, "Failed to load group members")))
        }
    }

    suspend fun getGroupMembers(context: Context, groupId: String): List<SupabaseGroupMember> {
        return getGroupMembersResult(context, groupId).getOrElse { emptyList() }
    }

    suspend fun addGroupMember(context: Context, groupId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.addGroupMember(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = AddGroupMemberRequest(p_group_id = cleanGroupId, p_user_id = userId.trim())
            )

            if (resp.isSuccessful) {
                getGroupOverview(context, cleanGroupId)
                _liveGroupUpdates.tryEmit(cleanGroupId)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to add member")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to add member")))
        }
    }

    suspend fun removeGroupMember(context: Context, groupId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val myUserId = SupabaseSessionManager.getUserId(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            if (userId == myUserId) {
                return@withContext leaveGroup(context, cleanGroupId)
            }

            val resp = SupabaseClient.api.removeGroupMember(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = RemoveGroupMemberRequest(p_group_id = cleanGroupId, p_user_id = userId.trim())
            )

            if (resp.isSuccessful) {
                getGroupOverview(context, cleanGroupId)
                _liveGroupUpdates.tryEmit(cleanGroupId)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to remove member")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to remove member")))
        }
    }

    suspend fun setMemberRole(context: Context, groupId: String, userId: String, role: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.setMemberRole(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = SetMemberRoleRequest(
                    p_group_id = cleanGroupId,
                    p_user_id = userId.trim(),
                    p_role = role.lowercase()
                )
            )

            if (resp.isSuccessful) {
                getGroupOverview(context, cleanGroupId)
                _liveGroupUpdates.tryEmit(cleanGroupId)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to update member role")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to update member role")))
        }
    }

    suspend fun approveJoinRequest(context: Context, groupId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.approveJoinRequest(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GroupUserActionRequest(p_group_id = cleanGroupId, p_user_id = userId.trim())
            )

            if (resp.isSuccessful) {
                getGroupOverview(context, cleanGroupId)
                syncUserGroups(context)
                _liveGroupUpdates.tryEmit(cleanGroupId)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to approve join request")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to approve join request")))
        }
    }

    suspend fun rejectJoinRequest(context: Context, groupId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.rejectJoinRequest(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GroupUserActionRequest(p_group_id = cleanGroupId, p_user_id = userId.trim())
            )

            if (resp.isSuccessful) {
                getGroupOverview(context, cleanGroupId)
                _liveGroupUpdates.tryEmit(cleanGroupId)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to reject join request")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to reject join request")))
        }
    }

    suspend fun leaveGroup(context: Context, groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.leaveGroup(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GroupIdRequest(p_group_id = cleanGroupId)
            )

            if (resp.isSuccessful) {
                database?.groupConversationDao()?.deleteGroup(cleanGroupId)
                database?.groupMessageDao()?.clearGroupMessages(cleanGroupId)
                syncUserGroups(context)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to leave group")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to leave group")))
        }
    }

    suspend fun blockGroup(context: Context, groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.blockGroup(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GroupIdRequest(p_group_id = cleanGroupId)
            )

            if (resp.isSuccessful) {
                database?.groupConversationDao()?.deleteGroup(cleanGroupId)
                database?.groupMessageDao()?.clearGroupMessages(cleanGroupId)
                syncUserGroups(context)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to block group")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to block group")))
        }
    }

    suspend fun unblockGroup(context: Context, groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.unblockGroup(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GroupIdRequest(p_group_id = cleanGroupId)
            )

            if (resp.isSuccessful) {
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to unblock group")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to unblock group")))
        }
    }

    suspend fun deleteGroup(context: Context, groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.deleteGroup(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GroupIdRequest(p_group_id = cleanGroupId)
            )

            if (resp.isSuccessful) {
                database?.groupConversationDao()?.deleteGroup(cleanGroupId)
                database?.groupMessageDao()?.clearGroupMessages(cleanGroupId)
                syncUserGroups(context)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to delete group")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to delete group")))
        }
    }

    suspend fun reportGroup(
        context: Context,
        groupId: String,
        category: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.reportGroup(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = ReportGroupRequest(
                    p_group_id = cleanGroupId,
                    p_category = category.trim(),
                    p_reason = reason.trim()
                )
            )

            if (resp.isSuccessful) {
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to submit group report")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to submit group report")))
        }
    }

    // ========================================================================
    // 4. INVITE LINK RPCs
    // ========================================================================

    suspend fun getGroupInvite(context: Context, groupId: String): Result<GroupInviteInfo> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.getGroupInvite(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GroupIdRequest(p_group_id = cleanGroupId)
            )

            if (!resp.isSuccessful) {
                val err = resp.errorBody()?.string()
                return@withContext Result.failure(Exception(mapGroupError(err, "Failed to fetch invite link")))
            }

            val raw = resp.body()?.string()?.trim().orEmpty()
            val (code, enabled) = parseInviteResponse(raw)
            if (code.isBlank()) {
                return@withContext Result.failure(Exception(mapGroupError("invite_disabled", "Invite link is currently disabled")))
            }

            Result.success(
                GroupInviteInfo(
                    code = code,
                    enabled = enabled,
                    deepLink = "funkytalk://group/$code",
                    httpsLink = "https://funkytalk.app/group/$code"
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to fetch invite link")))
        }
    }

    suspend fun resetGroupInvite(context: Context, groupId: String): Result<GroupInviteInfo> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.resetGroupInvite(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GroupIdRequest(p_group_id = cleanGroupId)
            )

            if (!resp.isSuccessful) {
                val err = resp.errorBody()?.string()
                return@withContext Result.failure(Exception(mapGroupError(err, "Failed to reset invite link")))
            }

            val raw = resp.body()?.string()?.trim().orEmpty()
            val (code, enabled) = parseInviteResponse(raw)
            Result.success(
                GroupInviteInfo(
                    code = code,
                    enabled = enabled,
                    deepLink = "funkytalk://group/$code",
                    httpsLink = "https://funkytalk.app/group/$code"
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to reset invite link")))
        }
    }

    suspend fun setGroupInviteEnabled(context: Context, groupId: String, enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val resp = SupabaseClient.api.setGroupInviteEnabled(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = SetGroupInviteEnabledRequest(p_group_id = cleanGroupId, p_enabled = enabled)
            )

            if (resp.isSuccessful) {
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string()
                Result.failure(Exception(mapGroupError(err, "Failed to update invite link status")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to update invite link status")))
        }
    }

    private fun parseInviteResponse(raw: String): Pair<String, Boolean> {
        if (raw.isBlank() || raw == "null") return "" to false
        if (!raw.startsWith("{") && !raw.startsWith("[")) {
            val clean = raw.replace("\"", "").trim()
            return clean to clean.isNotBlank()
        }
        return try {
            val json = if (raw.startsWith("[")) JSONArray(raw).optJSONObject(0) ?: JSONObject() else JSONObject(raw)
            val code = json.optString("invite_code").ifBlank {
                json.optString("code").ifBlank { json.optString("token") }
            }
            val enabled = json.optBoolean("invite_enabled", json.optBoolean("enabled", code.isNotBlank()))
            code to enabled
        } catch (_: Exception) {
            "" to false
        }
    }

    suspend fun previewGroupByInvite(context: Context, code: String): Result<GroupInvitePreview> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanCode = extractCleanInviteCode(code)
            if (cleanCode.isBlank()) {
                return@withContext Result.failure(Exception(mapGroupError("invalid_invite")))
            }

            val resp = SupabaseClient.api.previewGroupByInvite(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = InviteCodeRequest(p_code = cleanCode)
            )

            if (!resp.isSuccessful) {
                val err = resp.errorBody()?.string()
                return@withContext Result.failure(Exception(mapGroupError(err, "Invalid or expired invite link")))
            }

            val raw = resp.body()?.string()?.trim().orEmpty()
            val json = when {
                raw.startsWith("[") -> JSONArray(raw).optJSONObject(0) ?: JSONObject()
                raw.startsWith("{") -> JSONObject(raw)
                else -> JSONObject()
            }

            val groupId = json.optString("group_id").ifBlank { json.optString("id") }
            if (groupId.isBlank() && !json.has("name")) {
                return@withContext Result.failure(Exception(mapGroupError("invalid_invite")))
            }

            Result.success(
                GroupInvitePreview(
                    code = cleanCode,
                    groupId = groupId,
                    name = json.optString("name", "FunkyTalk Group"),
                    description = if (json.isNull("description")) "" else json.optString("description", ""),
                    avatarUrl = if (json.isNull("avatar_url")) "" else json.optString("avatar_url", ""),
                    joinedCount = json.optInt("joined_count", json.optInt("member_count", 1)).coerceAtLeast(1),
                    approveNewMembers = json.optBoolean("approve_new_members", false),
                    myStatus = if (json.isNull("my_status")) null else json.optString("my_status")
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Invalid or expired invite link")))
        }
    }

    suspend fun joinGroupByInvite(context: Context, code: String): Result<JoinGroupByInviteResult> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanCode = extractCleanInviteCode(code)
            val resp = SupabaseClient.api.joinGroupByInvite(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = InviteCodeRequest(p_code = cleanCode)
            )

            if (!resp.isSuccessful) {
                val err = resp.errorBody()?.string()
                return@withContext Result.failure(Exception(mapGroupError(err, "Failed to join group")))
            }

            val raw = resp.body()?.string()?.trim().orEmpty()
            val json = when {
                raw.startsWith("[") -> JSONArray(raw).optJSONObject(0) ?: JSONObject()
                raw.startsWith("{") -> JSONObject(raw)
                else -> JSONObject()
            }

            val groupId = json.optString("group_id").ifBlank { json.optString("id") }
            val status = json.optString("status", "joined").lowercase()

            syncUserGroups(context)
            if (groupId.isNotBlank() && status == "joined") {
                VoiceRoomRealtimeManager.subscribeGroupRealtime(groupId)
                getGroupOverview(context, groupId)
                fetchAndProcessPendingGroupMessages(context)
            }

            Result.success(JoinGroupByInviteResult(groupId = groupId, status = status))
        } catch (e: Exception) {
            Result.failure(Exception(mapGroupError(e.message, "Failed to join group")))
        }
    }

    fun extractCleanInviteCode(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.contains("/group/")) {
            return trimmed.substringAfterLast("/group/").substringBefore("?").substringBefore("/").trim()
        }
        if (trimmed.startsWith("funkytalk://")) {
            return trimmed.removePrefix("funkytalk://").removePrefix("group/").substringBefore("?").trim('/')
        }
        return trimmed
    }

    // ========================================================================
    // 5. MESSAGING (E2EE Fan-out, Single-Event Realtime, Disappearing Messages)
    // ========================================================================

    /**
     * Sending a group message:
     * 1. Call get_group_members, take status='joined' members.
     * 2. Fetch each one's public key from user_public_keys (including self).
     * 3. Encrypt per recipient (same scheme as DMs).
     * 4. Skip members without a public key and warn the sender.
     * 5. Call send_group_message(p_group_id, p_message_type ['text'|'image'|'voice'], p_recipients).
     */
    suspend fun sendGroupMessage(
        context: Context,
        groupId: String,
        text: String,
        messageType: String = "text",
        storagePath: String? = null
    ): Result<SendGroupMessageOutcome> = withContext(Dispatchers.IO) {
        try {
            val activeUid = SupabaseSessionManager.ensureActiveSession(context)
            val myUserId = activeUid ?: SupabaseSessionManager.getUserId(context)
                ?: return@withContext Result.failure(Exception("Not logged in"))
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")

            // Ensure our own identity key is registered
            E2EEManager.ensureIdentityKeyInitialized(context)

            // 1. Fetch group members via get_group_members RPC
            val membersRes = getGroupMembersResult(context, cleanGroupId)
            if (membersRes.isFailure) {
                return@withContext Result.failure(membersRes.exceptionOrNull() ?: Exception("Failed to load group members"))
            }

            val allMembers = membersRes.getOrNull().orEmpty()
            val joinedMembers = allMembers.filter { it.status.trim().equals("joined", ignoreCase = true) }
            val memberUserIds = (joinedMembers.map { it.user_id }.toSet() + myUserId).filter { it.isNotBlank() }

            // 2. Fetch public keys for each joined member and encrypt per recipient
            val recipientsPayload = mutableListOf<GroupRecipientPayload>()
            var skippedNoKeyCount = 0

            for (uid in memberUserIds) {
                val keyResp = SupabaseClient.api.getUserPublicKey(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    userIdFilter = "eq.$uid"
                )
                val pubKey = keyResp.body()?.firstOrNull()?.identity_public_key

                if (!pubKey.isNullOrBlank()) {
                    val ciphertext = E2EEManager.encryptMessage(pubKey, text)
                    recipientsPayload.add(
                        GroupRecipientPayload(
                            recipient_id = uid,
                            ciphertext = ciphertext,
                            nonce = UUID.randomUUID().toString(),
                            storage_path = storagePath
                        )
                    )
                } else {
                    if (uid != myUserId) {
                        skippedNoKeyCount++
                    }
                    Log.w(TAG, "Recipient user '$uid' does not have a registered identity_public_key; skipping")
                }
            }

            if (recipientsPayload.isEmpty()) {
                return@withContext Result.failure(Exception("Could not encrypt message: no member public keys registered"))
            }

            // 3. Call send_group_message RPC
            val sendResp = SupabaseClient.api.sendGroupMessage(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = SendGroupMessageRequest(
                    p_group_id = cleanGroupId,
                    p_message_type = messageType,
                    p_recipients = recipientsPayload
                )
            )

            if (!sendResp.isSuccessful) {
                val err = sendResp.errorBody()?.string()
                return@withContext Result.failure(Exception(mapGroupError(err, "Failed to send group message")))
            }

            val rawResp = sendResp.body()?.string()?.trim().orEmpty()
            val serverMessageId = parseGroupId(rawResp) ?: UUID.randomUUID().toString()

            // 4. Save local copy with disappearing expiry if enabled
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val myName = firebaseUser?.displayName ?: "You"
            val myAvatar = firebaseUser?.photoUrl?.toString()
            val now = System.currentTimeMillis()
            val group = database?.groupConversationDao()?.getGroup(cleanGroupId)
            val disSec = group?.disappearingSeconds ?: 0
            val expiresAt = if (disSec > 0) now + disSec * 1000L else null

            val localMsg = LocalGroupMessage(
                id = serverMessageId,
                groupId = cleanGroupId,
                senderId = myUserId,
                senderName = myName,
                senderAvatar = myAvatar,
                text = text,
                messageType = messageType,
                storagePath = storagePath,
                timestamp = now,
                expiresAt = expiresAt,
                isOutgoing = true
            )
            database?.groupMessageDao()?.insertMessage(localMsg)
            database?.groupConversationDao()?.updateLastMessage(
                groupId = cleanGroupId,
                text = text,
                senderName = "You",
                time = now,
                unreadDelta = 0
            )

            Result.success(SendGroupMessageOutcome(messageId = serverMessageId, skippedCount = skippedNoKeyCount))
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending group message", e)
            Result.failure(Exception(mapGroupError(e.message, "Failed to send message")))
        }
    }

    /**
     * Sends a 2-layer E2EE media message ('image' or 'voice') to the group.
     */
    suspend fun sendGroupMediaMessage(
        context: Context,
        groupId: String,
        rawBytes: ByteArray,
        mimeType: String = "image/jpeg",
        messageType: String = "image" // "image" or "voice"
    ): Result<SendGroupMessageOutcome> = withContext(Dispatchers.IO) {
        try {
            val activeUid = SupabaseSessionManager.ensureActiveSession(context)
            val myUserId = activeUid ?: SupabaseSessionManager.getUserId(context)
                ?: return@withContext Result.failure(Exception("Not logged in"))
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")
            val messageId = UUID.randomUUID().toString()

            E2EEManager.ensureIdentityKeyInitialized(context)

            // 1. Layer 1: Encrypt raw media bytes with one-time AES-256-GCM key + 12-byte nonce
            val encResult = E2EEMediaManager.encryptMediaPayload(rawBytes)

            // 2. Upload encrypted ciphertext blob to Appwrite Storage via upload-to-appwrite proxy
            val uploadResult = AppwriteUploader.uploadWithToken(
                token = token,
                fileBytes = encResult.ciphertextBytes,
                filename = "$messageId.bin",
                mimeType = "application/octet-stream",
                purpose = "group-media",
                folderSuffix = cleanGroupId,
                maxRetries = 1
            )

            val mediaUrl = when (uploadResult) {
                is AppwriteUploader.UploadResult.Success -> uploadResult.url
                is AppwriteUploader.UploadResult.Error -> {
                    return@withContext Result.failure(Exception(uploadResult.message))
                }
            }

            val keyB64 = Base64.encodeToString(encResult.keyBytes, Base64.NO_WRAP)
            val nonceB64 = Base64.encodeToString(encResult.nonceBytes, Base64.NO_WRAP)

            // 3. Layer 2: Prepare media descriptor JSON
            val descriptorJson = JSONObject().apply {
                put("storage_path", mediaUrl)
                put("media_key", keyB64)
                put("media_nonce", nonceB64)
                put("mime_type", mimeType)
                put("message_type", messageType)
            }.toString()

            // Save local decrypted copy immediately
            val ext = if (messageType == "voice") "m4a" else "jpg"
            val localDir = File(context.filesDir, "encrypted_vault_media").apply { mkdirs() }
            val localFile = File(localDir, "group_$messageId.$ext")
            localFile.writeBytes(rawBytes)

            // 4. Fetch joined members via get_group_members RPC and encrypt per recipient
            val membersList = getGroupMembers(context, cleanGroupId)
            val joinedMembers = membersList.filter { it.status.trim().equals("joined", ignoreCase = true) }
            val memberUserIds = (joinedMembers.map { it.user_id }.toSet() + myUserId).filter { it.isNotBlank() }

            val recipientsPayload = mutableListOf<GroupRecipientPayload>()
            var skippedNoKeyCount = 0

            for (uid in memberUserIds) {
                val keyResp = SupabaseClient.api.getUserPublicKey(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    userIdFilter = "eq.$uid"
                )
                val pubKey = keyResp.body()?.firstOrNull()?.identity_public_key
                if (!pubKey.isNullOrBlank()) {
                    val ciphertext = E2EEManager.encryptMessage(pubKey, descriptorJson)
                    recipientsPayload.add(
                        GroupRecipientPayload(
                            recipient_id = uid,
                            ciphertext = ciphertext,
                            nonce = UUID.randomUUID().toString(),
                            storage_path = mediaUrl
                        )
                    )
                } else {
                    if (uid != myUserId) skippedNoKeyCount++
                }
            }

            if (recipientsPayload.isEmpty()) {
                return@withContext Result.failure(Exception("Could not encrypt media message: no member public keys registered"))
            }

            val effectiveType = if (messageType == "voice") "voice" else "image"

            // 5. Call send_group_message RPC
            val sendResp = SupabaseClient.api.sendGroupMessage(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = SendGroupMessageRequest(
                    p_group_id = cleanGroupId,
                    p_message_type = effectiveType,
                    p_recipients = recipientsPayload
                )
            )

            if (!sendResp.isSuccessful) {
                val err = sendResp.errorBody()?.string()
                return@withContext Result.failure(Exception(mapGroupError(err, "Failed to send group media message")))
            }

            val rawResp = sendResp.body()?.string()?.trim().orEmpty()
            val serverMessageId = parseGroupId(rawResp) ?: messageId

            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val myName = firebaseUser?.displayName ?: "You"
            val myAvatar = firebaseUser?.photoUrl?.toString()
            val now = System.currentTimeMillis()
            val group = database?.groupConversationDao()?.getGroup(cleanGroupId)
            val disSec = group?.disappearingSeconds ?: 0
            val expiresAt = if (disSec > 0) now + disSec * 1000L else null
            val displayLabel = if (effectiveType == "voice") "Voice message" else "Photo"

            val localMsg = LocalGroupMessage(
                id = serverMessageId,
                groupId = cleanGroupId,
                senderId = myUserId,
                senderName = myName,
                senderAvatar = myAvatar,
                text = displayLabel,
                messageType = effectiveType,
                storagePath = mediaUrl,
                localMediaPath = localFile.absolutePath,
                mediaKey = keyB64,
                mediaNonce = nonceB64,
                mimeType = mimeType,
                timestamp = now,
                expiresAt = expiresAt,
                isOutgoing = true
            )
            database?.groupMessageDao()?.insertMessage(localMsg)
            database?.groupConversationDao()?.updateLastMessage(
                groupId = cleanGroupId,
                text = displayLabel,
                senderName = "You",
                time = now,
                unreadDelta = 0
            )

            Result.success(SendGroupMessageOutcome(messageId = serverMessageId, skippedCount = skippedNoKeyCount))
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending group media message", e)
            Result.failure(Exception(mapGroupError(e.message, "Failed to send media message")))
        }
    }

    /**
     * Drains pending rows from group_message_recipients (recipient_id = my uid).
     * Each row carries group_id, sender_id and message_type, so one event is enough to decrypt.
     * Saves to local DB, then deletes the inbox row.
     */
    suspend fun fetchAndProcessPendingGroupMessages(context: Context) = withContext(Dispatchers.IO) {
        try {
            val myUserId = SupabaseSessionManager.getUserId(context) ?: return@withContext
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@withContext

            val resp = SupabaseClient.api.getPendingGroupMessageRecipients(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                recipientIdFilter = "eq.$myUserId"
            )

            if (!resp.isSuccessful || resp.body().isNullOrEmpty()) return@withContext
            val pendingList = resp.body()!!

            Log.d(TAG, "Draining ${pendingList.size} pending group_message_recipients rows")
            for (row in pendingList) {
                processSingleRecipientRow(context, token, myUserId, row)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchAndProcessPendingGroupMessages", e)
        }
    }

    private suspend fun processSingleRecipientRow(
        context: Context,
        token: String,
        myUserId: String,
        row: SupabaseGroupRecipientRow
    ) {
        if (row.id.isNotBlank() && processedRecipientIds.contains(row.id)) {
            deleteRecipientRow(token, row.id)
            return
        }

        try {
            val groupId = row.group_id?.trim().orEmpty()
            val senderId = row.sender_id?.trim().orEmpty()
            val rowMessageType = row.message_type?.trim()?.ifBlank { "text" } ?: "text"
            val msgId = row.message_id.ifBlank { row.id }

            if (groupId.isBlank()) {
                deleteRecipientRow(token, row.id)
                return
            }

            val db = database ?: return

            // If sent by self and already saved locally during sendGroupMessage, just delete the inbox row
            val isFromMe = senderId == myUserId
            if (db.groupMessageDao().messageExists(msgId)) {
                processedRecipientIds.add(row.id)
                deleteRecipientRow(token, row.id)
                return
            }

            // Decrypt ciphertext using local Tink private key
            val plaintext = E2EEManager.decryptMessage(context, row.ciphertext)

            val localGroup = db.groupConversationDao().getGroup(groupId)
            val autoDownload = localGroup?.mediaAutoDownload ?: true
            val disSec = localGroup?.disappearingSeconds ?: 0
            val now = System.currentTimeMillis()
            val expiresAt = if (disSec > 0) now + disSec * 1000L else null

            var displayText = plaintext
            var localMediaPath: String? = null
            var resolvedType = rowMessageType
            var savedKey: String? = null
            var savedNonce: String? = null
            var savedMime: String? = null
            var savedStoragePath: String? = row.storage_path

            val trimmed = plaintext.trim()
            val mediaDescriptor = if (trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.contains("media_key")) {
                try { JSONObject(trimmed) } catch (_: Exception) { null }
            } else null

            if (mediaDescriptor != null) {
                savedKey = mediaDescriptor.optString("media_key")
                savedNonce = mediaDescriptor.optString("media_nonce")
                savedMime = mediaDescriptor.optString("mime_type", "image/jpeg")
                savedStoragePath = mediaDescriptor.optString("storage_path").ifBlank { row.storage_path.orEmpty() }
                val descType = mediaDescriptor.optString("message_type")
                resolvedType = when {
                    descType == "voice" || rowMessageType == "voice" || savedMime.contains("audio") -> "voice"
                    else -> "image"
                }
                displayText = if (resolvedType == "voice") "Voice message" else "Photo"

                if (autoDownload && !savedKey.isNullOrBlank() && !savedNonce.isNullOrBlank() && !savedStoragePath.isNullOrBlank()) {
                    localMediaPath = downloadAndDecryptGroupMediaBlob(
                        context = context,
                        token = token,
                        messageId = msgId,
                        storagePath = savedStoragePath,
                        keyBase64 = savedKey,
                        nonceBase64 = savedNonce,
                        isVoice = resolvedType == "voice"
                    )
                }
            }

            val senderName = if (isFromMe) "You" else resolveSenderName(context, token, senderId)
            val senderAvatar = if (isFromMe) null else resolveSenderAvatar(context, token, senderId)

            val localMsg = LocalGroupMessage(
                id = msgId,
                groupId = groupId,
                senderId = senderId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                text = displayText,
                messageType = resolvedType,
                storagePath = savedStoragePath,
                localMediaPath = localMediaPath,
                mediaKey = savedKey,
                mediaNonce = savedNonce,
                mimeType = savedMime,
                timestamp = now,
                expiresAt = expiresAt,
                isOutgoing = isFromMe
            )
            db.groupMessageDao().insertMessage(localMsg)
            db.groupConversationDao().updateLastMessage(
                groupId = groupId,
                text = displayText,
                senderName = senderName,
                time = now,
                unreadDelta = if (isFromMe) 0 else 1
            )

            processedRecipientIds.add(row.id)
            deleteRecipientRow(token, row.id)
        } catch (e: Exception) {
            Log.w(TAG, "Failed decrypting group message row ${row.id}: ${e.message}")
            deleteRecipientRow(token, row.id)
        }
    }

    suspend fun downloadGroupMessageMediaOnDemand(
        context: Context,
        message: LocalGroupMessage
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!message.localMediaPath.isNullOrBlank() && File(message.localMediaPath).exists()) {
                return@withContext Result.success(message.localMediaPath)
            }
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))
            val storagePath = message.storagePath
                ?: return@withContext Result.failure(Exception("Missing media storage path"))
            val keyB64 = message.mediaKey
                ?: return@withContext Result.failure(Exception("Missing media encryption key"))
            val nonceB64 = message.mediaNonce
                ?: return@withContext Result.failure(Exception("Missing media encryption nonce"))

            val path = downloadAndDecryptGroupMediaBlob(
                context = context,
                token = token,
                messageId = message.id,
                storagePath = storagePath,
                keyBase64 = keyB64,
                nonceBase64 = nonceB64,
                isVoice = message.messageType == "voice"
            ) ?: return@withContext Result.failure(Exception("Failed to download or decrypt media"))

            database?.groupMessageDao()?.updateLocalMediaPath(message.id, path)
            Result.success(path)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun downloadAndDecryptGroupMediaBlob(
        context: Context,
        token: String,
        messageId: String,
        storagePath: String,
        keyBase64: String,
        nonceBase64: String,
        isVoice: Boolean
    ): String? {
        return try {
            val keyBytes = Base64.decode(keyBase64, Base64.DEFAULT)
            val nonceBytes = Base64.decode(nonceBase64, Base64.DEFAULT)

            val encBytes = if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
                val isSupa = storagePath.contains("supabase.co")
                val dlResp = SupabaseClient.api.downloadFromUrl(
                    url = storagePath,
                    apiKey = if (isSupa) BuildConfig.SUPABASE_ANON_KEY else null,
                    auth = if (isSupa) token else null
                )
                if (dlResp.isSuccessful && dlResp.body() != null) dlResp.body()!!.bytes() else null
            } else {
                val relativePath = storagePath.removePrefix("group-media/").removePrefix("/")
                val dlResp = SupabaseClient.api.downloadGroupMedia(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    path = relativePath
                )
                if (dlResp.isSuccessful && dlResp.body() != null) dlResp.body()!!.bytes() else null
            }

            if (encBytes != null && encBytes.isNotEmpty()) {
                val rawBytes = E2EEMediaManager.decryptMediaPayload(encBytes, keyBytes, nonceBytes)
                val ext = if (isVoice) "m4a" else "jpg"
                val localDir = File(context.filesDir, "encrypted_vault_media").apply { mkdirs() }
                val localFile = File(localDir, "group_${messageId}.$ext")
                localFile.writeBytes(rawBytes)
                localFile.absolutePath
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed downloading/decrypting media blob for $messageId: ${e.message}")
            null
        }
    }

    private suspend fun deleteRecipientRow(token: String, rowId: String) {
        if (rowId.isBlank()) return
        try {
            SupabaseClient.api.deleteGroupMessageRecipient(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                idFilter = "eq.$rowId"
            )
        } catch (_: Exception) {}
    }

    private val userProfileCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, String?>>()

    private suspend fun resolveSenderName(context: Context, token: String, senderId: String): String {
        if (senderId.isBlank()) return "Member"
        userProfileCache[senderId]?.first?.let { return it }
        try {
            val profResp = SupabaseClient.api.getDiscoverableProfiles(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                options = mapOf("auth_user_id" to "eq.$senderId")
            )
            val p = profResp.body()?.firstOrNull()
            val name = p?.profile_name?.takeIf { it.isNotBlank() }
                ?: p?.username?.takeIf { it.isNotBlank() }
                ?: "Member"
            val avatar = p?.avatar_url
            userProfileCache[senderId] = name to avatar
            return name
        } catch (_: Exception) {}
        return "Member"
    }

    private suspend fun resolveSenderAvatar(context: Context, token: String, senderId: String): String? {
        if (senderId.isBlank()) return null
        return userProfileCache[senderId]?.second
    }

    // ========================================================================
    // 6. LOCAL DB FLOWS & LOCAL-ONLY OPERATIONS (E2EE)
    // ========================================================================

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getGroupConversationFlow(groupId: String): Flow<LocalGroupConversation?> {
        val cleanGroupId = groupId.trim().replace("\"", "")
        return _databaseState.flatMapLatest { db ->
            db?.groupConversationDao()?.getGroupFlow(cleanGroupId) ?: flowOf(null)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getGroupMessagesFlow(groupId: String): Flow<List<LocalGroupMessage>> {
        val cleanGroupId = groupId.trim().replace("\"", "")
        return _databaseState.flatMapLatest { db ->
            db?.groupMessageDao()?.getGroupMessages(cleanGroupId, System.currentTimeMillis()) ?: flowOf(emptyList())
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getSharedMediaFlow(groupId: String): Flow<List<LocalGroupMessage>> {
        val cleanGroupId = groupId.trim().replace("\"", "")
        return _databaseState.flatMapLatest { db ->
            db?.groupMessageDao()?.getSharedMediaMessages(cleanGroupId, System.currentTimeMillis()) ?: flowOf(emptyList())
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getAllMediaAndLinksFlow(groupId: String): Flow<List<LocalGroupMessage>> {
        val cleanGroupId = groupId.trim().replace("\"", "")
        return _databaseState.flatMapLatest { db ->
            db?.groupMessageDao()?.getAllGroupMessagesForLinksAndMedia(cleanGroupId, System.currentTimeMillis()) ?: flowOf(emptyList())
        }
    }

    suspend fun searchMessagesLocally(groupId: String, query: String): List<LocalGroupMessage> = withContext(Dispatchers.IO) {
        val cleanGroupId = groupId.trim().replace("\"", "")
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()
        database?.groupMessageDao()?.searchGroupMessages(cleanGroupId, cleanQuery, System.currentTimeMillis()).orEmpty()
    }

    /**
     * Local-only clear chat (E2EE). Never calls the server.
     */
    suspend fun clearGroupChatLocally(groupId: String) = withContext(Dispatchers.IO) {
        val cleanGroupId = groupId.trim().replace("\"", "")
        database?.groupMessageDao()?.clearGroupMessages(cleanGroupId)
        database?.groupConversationDao()?.updateLastMessage(
            groupId = cleanGroupId,
            text = "Chat cleared locally",
            senderName = "",
            time = System.currentTimeMillis(),
            unreadDelta = 0
        )
    }

    suspend fun markGroupRead(groupId: String) {
        val cleanGroupId = groupId.trim().replace("\"", "")
        database?.groupConversationDao()?.markRead(cleanGroupId)
    }

    private fun parseIsoTimestampToMillis(iso: String?): Long? {
        if (iso.isNullOrBlank() || iso == "null") return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val dt = sdf.parse(iso)
                if (dt != null) return dt.time
            } catch (_: Exception) {}
        }
        return null
    }
}
