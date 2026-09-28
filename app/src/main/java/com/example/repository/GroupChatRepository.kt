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
            if (database == null) {
                database = EncryptedChatDatabase.buildDatabase(context, vaultKey)
            }

            groupCollectJob?.cancel()
            groupCollectJob = scope.launch {
                database?.groupConversationDao()?.getAllJoinedGroups()?.collect { list ->
                    _joinedGroups.value = list
                }
            }

            inviteCollectJob?.cancel()
            inviteCollectJob = scope.launch {
                database?.groupConversationDao()?.getPendingInvites()?.collect { list ->
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
                            if (membership.status == "invited") "Invited to join group" else "🔒 End-to-end encrypted group chat"
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
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val resp = SupabaseClient.api.acceptGroupInvite(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = AcceptGroupInviteRequest(p_group_id = groupId)
            )

            if (resp.isSuccessful) {
                database?.groupConversationDao()?.updateStatus(groupId, "joined")
                syncUserGroups(context)
                fetchAndProcessPendingGroupMessages(context)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string() ?: "Failed to accept invite"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decline group invitation:
     * Calls decline_group_invite(p_group_id) -> invite removed.
     */
    suspend fun declineGroupInvite(context: Context, groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val resp = SupabaseClient.api.declineGroupInvite(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = DeclineGroupInviteRequest(p_group_id = groupId)
            )

            if (resp.isSuccessful) {
                database?.groupConversationDao()?.deleteGroup(groupId)
                Result.success(Unit)
            } else {
                val err = resp.errorBody()?.string() ?: "Failed to decline invite"
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
            val myUserId = SupabaseSessionManager.getUserId(context)
                ?: return@withContext Result.failure(Exception("Not logged in"))
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            // 1. Fetch joined group members
            val membersResp = SupabaseClient.api.getGroupMembers(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                groupIdFilter = "eq.$groupId",
                statusFilter = "eq.joined"
            )

            if (!membersResp.isSuccessful || membersResp.body().isNullOrEmpty()) {
                return@withContext Result.failure(Exception("No joined members found in group"))
            }

            val joinedMembers = membersResp.body()!!
            val memberUserIds = joinedMembers.map { it.user_id }.toSet() + myUserId

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
                    p_group_id = groupId,
                    p_message_type = messageType,
                    p_recipients = recipientsPayload
                )
            )

            if (!sendResp.isSuccessful) {
                val err = sendResp.errorBody()?.string() ?: "Failed to send group message"
                return@withContext Result.failure(Exception(err))
            }

            // 4. Save local copy so sender immediately sees their message
            val msgId = UUID.randomUUID().toString()
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val myName = firebaseUser?.displayName ?: "You"
            val myAvatar = firebaseUser?.photoUrl?.toString()

            val localMsg = LocalGroupMessage(
                id = msgId,
                groupId = groupId,
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
                groupId = groupId,
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
            val requestBody = encResult.ciphertextBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())

            val uploadResp = SupabaseClient.api.uploadGroupMedia(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                path = relativeStoragePath,
                body = requestBody
            )
            if (!uploadResp.isSuccessful) {
                val err = uploadResp.errorBody()?.string() ?: "Failed to upload group media"
                return@withContext Result.failure(Exception("Storage upload error: $err"))
            }

            // 2. Layer 2: Prepare media descriptor JSON
            val descriptorJson = JSONObject().apply {
                put("storage_path", fullStoragePath)
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
                            storage_path = fullStoragePath
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
                text = "📷 Photo",
                messageType = "image",
                storagePath = fullStoragePath,
                localMediaPath = localFile.absolutePath,
                timestamp = System.currentTimeMillis(),
                isOutgoing = true
            )
            database?.groupMessageDao()?.insertMessage(localMsg)
            database?.groupConversationDao()?.updateLastMessage(
                groupId = groupId,
                text = "📷 Photo",
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
                            val relativePath = storagePath.removePrefix("group-media/").removePrefix("/")

                            if (keyBase64.isNotBlank() && nonceBase64.isNotBlank() && relativePath.isNotBlank()) {
                                val keyBytes = Base64.decode(keyBase64, Base64.DEFAULT)
                                val nonceBytes = Base64.decode(nonceBase64, Base64.DEFAULT)
                                val dlResp = SupabaseClient.api.downloadGroupMedia(
                                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                    auth = token,
                                    path = relativePath
                                )
                                if (dlResp.isSuccessful && dlResp.body() != null) {
                                    val encBytes = dlResp.body()!!.bytes()
                                    val rawBytes = E2EEMediaManager.decryptMediaPayload(encBytes, keyBytes, nonceBytes)
                                    val localDir = File(context.filesDir, "encrypted_vault_media").apply { mkdirs() }
                                    val localFile = File(localDir, "group_${row.message_id}.jpg")
                                    localFile.writeBytes(rawBytes)
                                    localMediaPath = localFile.absolutePath
                                    displayText = "📷 Photo"
                                    resolvedType = "image"
                                }
                            }
                        } catch (mediaErr: Exception) {
                            Log.w(TAG, "Failed decrypting group media blob for ${row.message_id}: ${mediaErr.message}")
                            displayText = "📷 Photo"
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

    fun getGroupMessagesFlow(groupId: String): Flow<List<LocalGroupMessage>> {
        return database?.groupMessageDao()?.getGroupMessages(groupId) ?: flowOf(emptyList())
    }

    suspend fun markGroupRead(groupId: String) {
        database?.groupConversationDao()?.markRead(groupId)
    }

    suspend fun getGroupMembers(context: Context, groupId: String): List<SupabaseGroupMember> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@withContext emptyList()
            val resp = SupabaseClient.api.getGroupMembers(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                groupIdFilter = "eq.$groupId"
            )
            return@withContext resp.body().orEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching group members", e)
        }
        emptyList()
    }
}
