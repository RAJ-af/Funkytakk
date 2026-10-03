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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

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

    private val processedRecipientIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    private var groupCollectJob: Job? = null
    private var inviteCollectJob: Job? = null
    private var realtimeListenerJob: Job? = null

    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        scope.launch {
            VaultSecurityManager.vaultStatus.collect { status ->
                when (status) {
                    VaultStatus.UNLOCKED -> {
                        val key = VaultSecurityManager.getActiveVaultKey()
                        if (key != null) {
                            openDatabase(context, key)
                        }
                    }
                    VaultStatus.LOCKED, VaultStatus.NOT_CONFIGURED -> {
                        closeDatabase()
                    }
                }
            }
        }

        startRealtimeListener(context)
    }

    private fun openDatabase(context: Context, vaultKey: ByteArray) {
        try {
            val db = EncryptedChatDatabase.getInstance(context, vaultKey)
            database = db
            _databaseState.value = db

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
        groupCollectJob = null
        inviteCollectJob = null
        database = null
        _databaseState.value = null
        _joinedGroups.value = emptyList()
        _pendingInvites.value = emptyList()
    }

    private fun startRealtimeListener(context: Context) {
        realtimeListenerJob?.cancel()
        realtimeListenerJob = scope.launch {
            VoiceRoomRealtimeManager.events.collect { event ->
                when (event) {
                    is RealtimeTableEvent.GroupMessageReceived -> {
                        val myUserId = SupabaseSessionManager.getUserId(context)
                        if (!myUserId.isNullOrBlank() && event.recipientId == myUserId) {
                            Log.d(TAG, "Live group message recipient row received! Processing...")
                            fetchAndProcessPendingGroupMessages(context)
                        }
                    }
                    is RealtimeTableEvent.NotificationChanged -> {
                        if (event.record?.optString("type") == "group_invite") {
                            Log.d(TAG, "Group invite notification received; syncing groups...")
                            syncUserGroups(context)
                        }
                    }
                    is RealtimeTableEvent.Reconnected -> {
                        fetchAndProcessPendingGroupMessages(context)
                        syncUserGroups(context)
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Syncs user's group memberships and pending invitations from Supabase.
     */
    suspend fun syncUserGroups(context: Context) = withContext(Dispatchers.IO) {
        try {
            val myUserId = SupabaseSessionManager.getUserId(context) ?: return@withContext
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@withContext

            val memberResp = SupabaseClient.api.getGroupMembers(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                userIdFilter = "eq.$myUserId"
            )

            if (!memberResp.isSuccessful || memberResp.body() == null) return@withContext
            val myMemberships = memberResp.body()!!

            for (membership in myMemberships) {
                try {
                    val groupResp = SupabaseClient.api.getGroupConversations(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token,
                        idFilter = "eq.${membership.group_id}"
                    )
                    val groupData = groupResp.body()?.firstOrNull() ?: continue

                    // Get member count
                    val rosterResp = SupabaseClient.api.getGroupMembers(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token,
                        groupIdFilter = "eq.${membership.group_id}",
                        statusFilter = "eq.joined"
                    )
                    val memberCount = rosterResp.body()?.size ?: 1

                    val existing = database?.groupConversationDao()?.getGroup(groupData.id)
                    val localConv = LocalGroupConversation(
                        id = groupData.id,
                        name = groupData.name,
                        avatarUrl = groupData.avatar_url.orEmpty().ifBlank {
                            "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=200"
                        },
                        createdBy = groupData.created_by.orEmpty(),
                        myRole = membership.role,
                        myStatus = membership.status, // "joined", "invited", "left"
                        memberCount = memberCount,
                        lastMessageText = existing?.lastMessageText.orEmpty().ifBlank {
                            if (membership.status == "invited") "Invited to join group" else "End-to-end encrypted group chat"
                        },
                        lastMessageSenderName = existing?.lastMessageSenderName.orEmpty(),
                        lastMessageTime = existing?.lastMessageTime ?: System.currentTimeMillis(),
                        unreadCount = existing?.unreadCount ?: 0,
                        createdAt = System.currentTimeMillis()
                    )
                    database?.groupConversationDao()?.insertOrUpdate(localConv)
                } catch (e: Exception) {
                    Log.w(TAG, "Error syncing group ${membership.group_id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user groups", e)
        }
    }

    /**
     * Creating a group:
     * 1. Pick name + select members from contacts/followers.
     * 2. Call create_group(p_name, p_member_ids: uuid[]) -> returns group_id.
     * Creator is auto-joined as admin; everyone else is added with status 'invited'.
     */
    suspend fun createGroup(
        context: Context,
        name: String,
        memberUserIds: List<String>,
        avatarUrl: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val myUserId = SupabaseSessionManager.getUserId(context)
                ?: return@withContext Result.failure(Exception("Not logged in"))
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val resp = SupabaseClient.api.createGroup(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = CreateGroupRequest(
                    p_name = name.trim(),
                    p_member_ids = memberUserIds.filter { it.isNotBlank() && it != myUserId }
                )
            )

            if (!resp.isSuccessful) {
                val err = resp.errorBody()?.string() ?: "Failed to create group"
                return@withContext Result.failure(Exception(err))
            }

            val bodyStr = resp.body()?.string()?.trim().orEmpty()
            val groupId = parseGroupId(bodyStr) ?: UUID.randomUUID().toString()

            // Insert into local DB as joined admin
            val localGroup = LocalGroupConversation(
                id = groupId,
                name = name.trim(),
                avatarUrl = avatarUrl.orEmpty().ifBlank {
                    "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=200"
                },
                createdBy = myUserId,
                myRole = "admin",
                myStatus = "joined",
                memberCount = 1,
                lastMessageText = "Group created",
                lastMessageSenderName = "You",
                lastMessageTime = System.currentTimeMillis()
            )
            database?.groupConversationDao()?.insertOrUpdate(localGroup)

            // Sync with backend to ensure roster is stored
            syncUserGroups(context)

            Result.success(groupId)
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating group", e)
            Result.failure(e)
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
     * Accept group invitation:
     * Calls accept_group_invite(p_group_id) -> user becomes joined member.
     */
    suspend fun acceptGroupInvite(context: Context, groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Ensure active Supabase session is refreshed and valid
            val activeUid = SupabaseSessionManager.ensureActiveSession(context)
            val sessionToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            val sessionUserId = activeUid ?: SupabaseSessionManager.getUserId(context)
            val jwtAuthUid = SupabaseSessionManager.extractUserIdFromJwt(
                SupabaseSessionManager.getAccessToken(context)
            )
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val firebaseEmail = firebaseUser?.email
            val firebaseUid = firebaseUser?.uid

            Log.i(TAG, "=== [ACCEPT GROUP INVITE DIAGNOSTIC] ===")
            Log.i(TAG, "Requested Group ID: '$groupId' (length=${groupId.length})")
            Log.i(TAG, "Firebase User: uid=$firebaseUid, email=$firebaseEmail")
            Log.i(TAG, "Supabase Session: userId=$sessionUserId, JWT auth.uid()=$jwtAuthUid")
            Log.i(TAG, "Bearer Token Present: ${!sessionToken.isNullOrBlank()}")

            if (sessionToken.isNullOrBlank() || sessionUserId.isNullOrBlank()) {
                Log.e(TAG, "Accept invite rejected: No authenticated Supabase session found.")
                return@withContext Result.failure(Exception("Not authenticated. Please log in again."))
            }

            var targetGroupId = groupId.trim()

            // 2. Fetch group_members record to verify pending invite and target group_id
            try {
                // Check if direct groupId has pending invite
                val directMemberResp = SupabaseClient.api.getGroupMembers(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = sessionToken,
                    groupIdFilter = "eq.$targetGroupId",
                    userIdFilter = "eq.$sessionUserId"
                )
                val directMatches = directMemberResp.body().orEmpty()
                val hasDirectPending = directMatches.any { it.status == "invited" }

                if (!hasDirectPending) {
                    // Fallback check: lookup user's pending invites across all groups
                    val allPendingResp = SupabaseClient.api.getGroupMembers(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = sessionToken,
                        userIdFilter = "eq.$sessionUserId",
                        statusFilter = "eq.invited"
                    )
                    val pendingList = allPendingResp.body().orEmpty()
                    Log.i(TAG, "User '$sessionUserId' has ${pendingList.size} pending invite(s) in group_members: ${pendingList.map { it.group_id }}")
                    
                    val matchingInvite = pendingList.firstOrNull { it.group_id.equals(targetGroupId, ignoreCase = true) }
                        ?: pendingList.firstOrNull()
                    
                    if (matchingInvite != null) {
                        Log.i(TAG, "Found matching pending group_id: '${matchingInvite.group_id}' (replacing requested '$targetGroupId')")
                        targetGroupId = matchingInvite.group_id
                    }
                } else {
                    Log.i(TAG, "✅ Direct match verified: session user '$sessionUserId' is invited to group '$targetGroupId'")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Pre-check getGroupMembers threw: ${e.message}")
            }

            Log.i(TAG, "Sending accept_group_invite RPC with parameter p_group_id='$targetGroupId'")
            val resp = SupabaseClient.api.acceptGroupInvite(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = sessionToken,
                body = AcceptGroupInviteRequest(p_group_id = targetGroupId)
            )

            if (resp.isSuccessful) {
                Log.i(TAG, "✅ accept_group_invite RPC succeeded for group $targetGroupId by user $sessionUserId")
                database?.groupConversationDao()?.updateStatus(targetGroupId, "joined")
                syncUserGroups(context)
                fetchAndProcessPendingGroupMessages(context)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string() ?: "Failed to accept invite"
                Log.e(TAG, "❌ accept_group_invite RPC failed (${resp.code()}): $err (p_group_id: '$targetGroupId', auth.uid: $jwtAuthUid / $sessionUserId)")
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception accepting group invite", e)
            Result.failure(e)
        }
    }

    /**
     * Decline group invitation:
     * Calls decline_group_invite(p_group_id) -> invite removed.
     */
    suspend fun declineGroupInvite(context: Context, groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val targetGroupId = groupId.trim()
            Log.i(TAG, "Sending decline_group_invite RPC with parameter p_group_id='$targetGroupId'")
            val resp = SupabaseClient.api.declineGroupInvite(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = DeclineGroupInviteRequest(p_group_id = targetGroupId)
            )

            if (resp.isSuccessful) {
                database?.groupConversationDao()?.deleteGroup(targetGroupId)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string() ?: "Failed to decline invite"
                Log.e(TAG, "❌ decline_group_invite RPC failed (${resp.code()}): $err")
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add member later (admin only):
     * Calls add_group_member(p_group_id, p_user_id) -> sends invite to user.
     */
    suspend fun addGroupMember(context: Context, groupId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val resp = SupabaseClient.api.addGroupMember(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = AddGroupMemberRequest(p_group_id = groupId, p_user_id = userId)
            )

            if (resp.isSuccessful) {
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string() ?: "Failed to add member"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove member or leave group:
     * Pass own ID to leave, or another ID to remove if admin.
     */
    suspend fun removeGroupMember(context: Context, groupId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val myUserId = SupabaseSessionManager.getUserId(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val resp = SupabaseClient.api.removeGroupMember(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = RemoveGroupMemberRequest(p_group_id = groupId, p_user_id = userId)
            )

            if (resp.isSuccessful) {
                if (userId == myUserId) {
                    database?.groupConversationDao()?.deleteGroup(groupId)
                    database?.groupMessageDao()?.clearGroupMessages(groupId)
                }
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string() ?: "Failed to remove member"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sending a group message:
     * Fan-out E2EE using Tink HybridEncrypt:
     * 1. Get joined members (group_members where group_id = X and status = 'joined')
     * 2. Get their public keys from user_public_keys (including self)
     * 3. Encrypt the SAME plaintext separately for EACH member's public key
     * 4. Call send_group_message(p_group_id, p_message_type, p_recipients)
     * 5. Save local decrypted message to show immediately in UI
     */
    suspend fun sendGroupMessage(
        context: Context,
        groupId: String,
        text: String,
        messageType: String = "text",
        storagePath: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val activeUid = SupabaseSessionManager.ensureActiveSession(context)
            val myUserId = activeUid ?: SupabaseSessionManager.getUserId(context)
                ?: return@withContext Result.failure(Exception("Not logged in"))
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val cleanGroupId = groupId.trim().replace("\"", "")

            Log.i(TAG, "=== [SEND GROUP MESSAGE DIAGNOSTIC] ===")
            Log.i(TAG, "Sending group message to groupId='$cleanGroupId', senderId='$myUserId'")

            // 1. Fetch group members from backend
            val membersResp = SupabaseClient.api.getGroupMembers(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                groupIdFilter = "eq.$cleanGroupId"
            )

            Log.i(TAG, "getGroupMembers response code=${membersResp.code()}, isSuccessful=${membersResp.isSuccessful}")
            if (!membersResp.isSuccessful) {
                val errBody = membersResp.errorBody()?.string()
                Log.e(TAG, "getGroupMembers query failed: code=${membersResp.code()}, body=$errBody")
                return@withContext Result.failure(Exception("Failed to load group members (HTTP ${membersResp.code()}): $errBody"))
            }

            val allMembers = membersResp.body().orEmpty()
            Log.i(TAG, "Raw query group_id: '$cleanGroupId', Total group_members returned: ${allMembers.size}")
            for (m in allMembers) {
                Log.i(TAG, " - raw member: user_id='${m.user_id}', role='${m.role}', status='${m.status}'")
            }

            // Filter for members with status 'joined'
            var joinedMembers = allMembers.filter { it.status.trim().equals("joined", ignoreCase = true) }
            Log.i(TAG, "Joined members count: ${joinedMembers.size}")

            if (joinedMembers.isEmpty() && allMembers.isNotEmpty()) {
                Log.w(TAG, "No members have status='joined'. Statuses present: ${allMembers.map { "${it.user_id}:${it.status}" }}")
                val onlySenderPresent = allMembers.all { it.user_id == myUserId }
                if (onlySenderPresent) {
                    joinedMembers = allMembers
                } else {
                    return@withContext Result.failure(Exception("No joined members found in group '$cleanGroupId' (statuses: ${allMembers.map { it.status }.distinct()})"))
                }
            }

            val memberUserIds = (joinedMembers.map { it.user_id }.toSet() + myUserId).filter { it.isNotBlank() }
            Log.i(TAG, "Target recipient member user IDs for fan-out E2EE (${memberUserIds.size}): $memberUserIds")

            // 2. Fetch public keys for each member
            val recipientsPayload = mutableListOf<GroupRecipientPayload>()
            for (uid in memberUserIds) {
                val keyResp = SupabaseClient.api.getUserPublicKey(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    userIdFilter = "eq.$uid"
                )
                val keyItem = keyResp.body()?.firstOrNull()
                val pubKey = keyItem?.identity_public_key

                if (!pubKey.isNullOrBlank()) {
                    // Encrypt plaintext for this specific recipient
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
                    Log.w(TAG, "Recipient user '$uid' does not have a registered identity_public_key")
                }
            }

            if (recipientsPayload.isEmpty()) {
                return@withContext Result.failure(Exception("Could not encrypt message: no member public keys registered for group $cleanGroupId"))
            }

            // 3. Call send_group_message RPC
            Log.i(TAG, "Calling send_group_message RPC with ${recipientsPayload.size} recipients...")
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
                val err = sendResp.errorBody()?.string() ?: "Failed to send group message"
                Log.e(TAG, "send_group_message RPC failed: ${sendResp.code()} - $err")
                return@withContext Result.failure(Exception(err))
            }

            Log.i(TAG, "✅ send_group_message RPC succeeded for group $cleanGroupId")

            // 4. Save local copy so sender immediately sees their message
            val msgId = UUID.randomUUID().toString()
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val myName = firebaseUser?.displayName ?: "You"
            val myAvatar = firebaseUser?.photoUrl?.toString()

            val localMsg = LocalGroupMessage(
                id = msgId,
                groupId = cleanGroupId,
                senderId = myUserId,
                senderName = myName,
                senderAvatar = myAvatar,
                text = text,
                messageType = messageType,
                storagePath = storagePath,
                timestamp = System.currentTimeMillis(),
                isOutgoing = true
            )
            database?.groupMessageDao()?.insertMessage(localMsg)
            database?.groupConversationDao()?.updateLastMessage(
                groupId = cleanGroupId,
                text = text,
                senderName = "You",
                time = System.currentTimeMillis(),
                unreadDelta = 0
            )

            Result.success(msgId)
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending group message", e)
            Result.failure(e)
        }
    }

    /**
     * Sends a 2-layer E2EE media message (image/voice) to the group.
     * 1. Encrypts file payload with random AES-256-GCM key + nonce
     * 2. Uploads encrypted ciphertext to group-media bucket at group-media/{groupId}/{messageId}
     * 3. Creates descriptor JSON containing media_key, media_nonce, storage_path
     * 4. Encrypts the descriptor separately for each joined member's Tink public key (fan-out)
     * 5. Calls send_group_message RPC with storage_path in each recipient entry
     */
    suspend fun sendGroupMediaMessage(
        context: Context,
        groupId: String,
        rawBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val myUserId = SupabaseSessionManager.getUserId(context)
                ?: return@withContext Result.failure(Exception("Not logged in"))
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val messageId = UUID.randomUUID().toString()
            val relativeStoragePath = "$groupId/$messageId"
            val fullStoragePath = "group-media/$groupId/$messageId"

            // 1. Layer 1: Encrypt raw media bytes with one-time AES-256-GCM key + 12-byte nonce
            val encResult = E2EEMediaManager.encryptMediaPayload(rawBytes)

            // Step 2: Upload encrypted ciphertext blob to Appwrite Storage via upload-to-appwrite proxy
            Log.d(TAG, "sendGroupMediaMessage: uploading encrypted media blob via Appwrite proxy (groupId='$groupId', messageId='$messageId', size=${encResult.ciphertextBytes.size})...")
            val uploadResult = AppwriteUploader.uploadWithToken(
                token = token,
                fileBytes = encResult.ciphertextBytes,
                filename = "$messageId.bin",
                mimeType = "application/octet-stream",
                purpose = "group-media",
                folderSuffix = groupId,
                maxRetries = 1
            )

            val mediaUrl = when (uploadResult) {
                is AppwriteUploader.UploadResult.Success -> {
                    Log.d(TAG, "sendGroupMediaMessage: Appwrite upload success: url=${uploadResult.url}, fileId=${uploadResult.fileId}")
                    uploadResult.url
                }
                is AppwriteUploader.UploadResult.Error -> {
                    Log.e(TAG, "sendGroupMediaMessage: Appwrite upload failed (${uploadResult.statusCode}): ${uploadResult.message}")
                    return@withContext Result.failure(Exception(uploadResult.message))
                }
            }

            // 2. Layer 2: Prepare media descriptor JSON
            val descriptorJson = JSONObject().apply {
                put("storage_path", mediaUrl)
                put("media_key", Base64.encodeToString(encResult.keyBytes, Base64.NO_WRAP))
                put("media_nonce", Base64.encodeToString(encResult.nonceBytes, Base64.NO_WRAP))
                put("mime_type", mimeType)
            }.toString()

            // Save local decrypted copy immediately
            val localDir = File(context.filesDir, "encrypted_vault_media").apply { mkdirs() }
            val localFile = File(localDir, "group_$messageId.jpg")
            localFile.writeBytes(rawBytes)

            // 3. Fetch joined members and their public keys
            val membersResp = SupabaseClient.api.getGroupMembers(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                groupIdFilter = "eq.$groupId",
                statusFilter = "eq.joined"
            )
            val joinedMembers = membersResp.body().orEmpty()
            val memberUserIds = joinedMembers.map { it.user_id }.toSet() + myUserId

            val recipientsPayload = mutableListOf<GroupRecipientPayload>()
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
                }
            }

            if (recipientsPayload.isEmpty()) {
                return@withContext Result.failure(Exception("Could not encrypt media message: no member public keys registered"))
            }

            // 4. Call send_group_message RPC
            val sendResp = SupabaseClient.api.sendGroupMessage(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = SendGroupMessageRequest(
                    p_group_id = groupId,
                    p_message_type = "image",
                    p_recipients = recipientsPayload
                )
            )

            if (!sendResp.isSuccessful) {
                val err = sendResp.errorBody()?.string() ?: "Failed to send group media message"
                return@withContext Result.failure(Exception(err))
            }

            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val myName = firebaseUser?.displayName ?: "You"
            val myAvatar = firebaseUser?.photoUrl?.toString()

            val localMsg = LocalGroupMessage(
                id = messageId,
                groupId = groupId,
                senderId = myUserId,
                senderName = myName,
                senderAvatar = myAvatar,
                text = "Photo",
                messageType = "image",
                storagePath = fullStoragePath,
                localMediaPath = localFile.absolutePath,
                timestamp = System.currentTimeMillis(),
                isOutgoing = true
            )
            database?.groupMessageDao()?.insertMessage(localMsg)
            database?.groupConversationDao()?.updateLastMessage(
                groupId = groupId,
                text = "Photo",
                senderName = "You",
                time = System.currentTimeMillis(),
                unreadDelta = 0
            )

            Result.success(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending group media message", e)
            Result.failure(e)
        }
    }

    /**
     * Polls temporary group mailbox (group_message_recipients where recipient_id = self),
     * decrypts each message with own private key, stores in local Room DB, and deletes recipient row.
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

            Log.d(TAG, "Processing ${pendingList.size} pending group message recipient rows")
            for (row in pendingList) {
                if (processedRecipientIds.contains(row.id)) {
                    deleteRecipientRow(token, row.id)
                    continue
                }

                try {
                    // Decrypt using self Tink private key
                    val plaintext = E2EEManager.decryptMessage(context, row.ciphertext)

                    // Fetch group message details
                    val msgMetaResp = SupabaseClient.api.getGroupMessagesByIds(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token,
                        idFilter = "eq.${row.message_id}"
                    )
                    val meta = msgMetaResp.body()?.firstOrNull() ?: continue

                    // Check if decrypted payload is a 2-layer E2EE media descriptor
                    var displayText = plaintext
                    var localMediaPath: String? = null
                    var resolvedType = meta.message_type
                    val trimmed = plaintext.trim()
                    val mediaDescriptor = if (trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.contains("media_key")) {
                        try { JSONObject(trimmed) } catch (_: Exception) { null }
                    } else null

                    if (mediaDescriptor != null) {
                        try {
                            val keyBase64 = mediaDescriptor.optString("media_key")
                            val nonceBase64 = mediaDescriptor.optString("media_nonce")
                            val storagePath = mediaDescriptor.optString("storage_path").ifBlank { row.storage_path.orEmpty() }

                            if (keyBase64.isNotBlank() && nonceBase64.isNotBlank() && storagePath.isNotBlank()) {
                                val keyBytes = Base64.decode(keyBase64, Base64.DEFAULT)
                                val nonceBytes = Base64.decode(nonceBase64, Base64.DEFAULT)

                                val encBytes = if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
                                    val isSupa = storagePath.contains("supabase.co")
                                    val dlResp = SupabaseClient.api.downloadFromUrl(
                                        url = storagePath,
                                        apiKey = if (isSupa) BuildConfig.SUPABASE_ANON_KEY else null,
                                        auth = if (isSupa) token else null
                                    )
                                    if (dlResp.isSuccessful && dlResp.body() != null) {
                                        dlResp.body()!!.bytes()
                                    } else null
                                } else {
                                    val relativePath = storagePath.removePrefix("group-media/").removePrefix("/")
                                    val dlResp = SupabaseClient.api.downloadGroupMedia(
                                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                        auth = token,
                                        path = relativePath
                                    )
                                    if (dlResp.isSuccessful && dlResp.body() != null) {
                                        dlResp.body()!!.bytes()
                                    } else null
                                }

                                if (encBytes != null && encBytes.isNotEmpty()) {
                                    val rawBytes = E2EEMediaManager.decryptMediaPayload(encBytes, keyBytes, nonceBytes)
                                    val localDir = File(context.filesDir, "encrypted_vault_media").apply { mkdirs() }
                                    val localFile = File(localDir, "group_${row.message_id}.jpg")
                                    localFile.writeBytes(rawBytes)
                                    localMediaPath = localFile.absolutePath
                                    displayText = "Photo"
                                    resolvedType = "image"
                                }
                            }
                        } catch (mediaErr: Exception) {
                            Log.w(TAG, "Failed decrypting group media blob for ${row.message_id}: ${mediaErr.message}")
                            displayText = "Photo"
                            resolvedType = "image"
                        }
                    }

                    // Don't save duplicate if it was sent by self (already inserted during sending)
                    val isFromMe = meta.sender_id == myUserId
                    val db = database
                    if (db != null && !db.groupMessageDao().messageExists(row.message_id)) {
                        // Resolve sender real name & avatar
                        val senderName = resolveSenderName(context, token, meta.sender_id)
                        val senderAvatar = resolveSenderAvatar(context, token, meta.sender_id)

                        val localMsg = LocalGroupMessage(
                            id = row.message_id,
                            groupId = meta.group_id,
                            senderId = meta.sender_id,
                            senderName = senderName,
                            senderAvatar = senderAvatar,
                            text = displayText,
                            messageType = resolvedType,
                            storagePath = row.storage_path,
                            localMediaPath = localMediaPath,
                            timestamp = System.currentTimeMillis(),
                            isOutgoing = isFromMe
                        )
                        db.groupMessageDao().insertMessage(localMsg)
                        db.groupConversationDao().updateLastMessage(
                            groupId = meta.group_id,
                            text = displayText,
                            senderName = senderName,
                            time = System.currentTimeMillis(),
                            unreadDelta = if (isFromMe) 0 else 1
                        )
                    }

                    processedRecipientIds.add(row.id)
                    deleteRecipientRow(token, row.id)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed decrypting group message ${row.message_id}: ${e.message}")
                    // Still delete to avoid infinite decryption crash loops
                    deleteRecipientRow(token, row.id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchAndProcessPendingGroupMessages", e)
        }
    }

    private suspend fun deleteRecipientRow(token: String, rowId: String) {
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
        userProfileCache[senderId]?.first?.let { return it }
        try {
            val profResp = SupabaseClient.api.getDiscoverableProfiles(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                options = mapOf("auth_user_id" to "eq.$senderId")
            )
            val p = profResp.body()?.firstOrNull()
            val name = p?.profile_name ?: p?.username ?: "Member"
            val avatar = p?.avatar_url
            userProfileCache[senderId] = name to avatar
            return name
        } catch (_: Exception) {}
        return "Member"
    }

    private suspend fun resolveSenderAvatar(context: Context, token: String, senderId: String): String? {
        return userProfileCache[senderId]?.second
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getGroupMessagesFlow(groupId: String): Flow<List<LocalGroupMessage>> {
        val cleanGroupId = groupId.trim().replace("\"", "")
        return _databaseState.flatMapLatest { db ->
            db?.groupMessageDao()?.getGroupMessages(cleanGroupId) ?: flowOf(emptyList())
        }
    }

    suspend fun markGroupRead(groupId: String) {
        val cleanGroupId = groupId.trim().replace("\"", "")
        database?.groupConversationDao()?.markRead(cleanGroupId)
    }

    suspend fun getGroupMembers(context: Context, groupId: String): List<SupabaseGroupMember> = withContext(Dispatchers.IO) {
        try {
            val activeUid = SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@withContext emptyList()
            val cleanGroupId = groupId.trim().replace("\"", "")
            Log.i(TAG, "getGroupMembers: querying group_members for groupId='$cleanGroupId' (auth.uid: '$activeUid')")
            val resp = SupabaseClient.api.getGroupMembers(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                groupIdFilter = "eq.$cleanGroupId"
            )
            Log.i(TAG, "getGroupMembers: response code=${resp.code()}, count=${resp.body()?.size}")
            if (!resp.isSuccessful) {
                Log.e(TAG, "getGroupMembers query failed (${resp.code()}): ${resp.errorBody()?.string()}")
            }
            val list = resp.body().orEmpty()
            for (m in list) {
                Log.i(TAG, " - roster member: user_id='${m.user_id}', role='${m.role}', status='${m.status}'")
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching group members", e)
        }
        emptyList()
    }
}
