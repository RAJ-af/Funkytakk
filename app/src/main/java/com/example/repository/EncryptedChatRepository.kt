package com.example.repository

import android.content.Context
import android.util.Log
import com.example.GetOrCreateDmConversationRequest
import com.example.SendDmMessageRequest
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.crypto.E2EEManager
import com.example.crypto.VaultSecurityManager
import com.example.crypto.VaultStatus
import com.example.db.EncryptedChatDatabase
import com.example.db.LocalDmConversation
import com.example.db.LocalDmMessage
import com.example.realtime.RealtimeTableEvent
import com.example.realtime.VoiceRoomRealtimeManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.util.UUID

class RecipientNotReadyException(val recipientName: String) : Exception(
    "$recipientName is not ready for secure chat yet. This user has not opened the app or registered encryption keys."
)

object EncryptedChatRepository {
    private const val TAG = "EncryptedChatRepo"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var database: EncryptedChatDatabase? = null

    private val _conversations = MutableStateFlow<List<LocalDmConversation>>(emptyList())
    val conversations: StateFlow<List<LocalDmConversation>> = _conversations.asStateFlow()

    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount.asStateFlow()

    private var conversationCollectJob: Job? = null
    private var realtimeListenerJob: Job? = null

    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        VaultSecurityManager.init(context)

        // Observe vault status
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

        // Start listening to realtime events
        startRealtimeListener(context)
    }

    private fun openDatabase(context: Context, vaultKey: ByteArray) {
        try {
            if (database == null) {
                database = EncryptedChatDatabase.buildDatabase(context, vaultKey)
                Log.d(TAG, "SQLCipher EncryptedChatDatabase opened successfully.")
            }

            conversationCollectJob?.cancel()
            conversationCollectJob = scope.launch {
                database?.conversationDao()?.getAllConversations()?.collect { list ->
                    _conversations.value = list
                    _totalUnreadCount.value = list.sumOf { it.unreadCount }
                }
            }

            // Sync identity key & process mailbox
            scope.launch {
                val myUserId = SupabaseSessionManager.getUserId(context)
                if (!myUserId.isNullOrBlank()) {
                    E2EEManager.ensureIdentityKeyInitialized(context)
                    VoiceRoomRealtimeManager.subscribeDmInbox(myUserId)
                    fetchAndProcessPendingInbox(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening EncryptedChatDatabase", e)
        }
    }

    fun closeDatabase() {
        conversationCollectJob?.cancel()
        conversationCollectJob = null
        try {
            database?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing database", e)
        }
        database = null
        _conversations.value = emptyList()
        _totalUnreadCount.value = 0
    }

    private fun startRealtimeListener(context: Context) {
        realtimeListenerJob?.cancel()
        realtimeListenerJob = scope.launch {
            VoiceRoomRealtimeManager.events.collect { event ->
                when (event) {
                    is RealtimeTableEvent.DmMessageReceived -> {
                        val myUserId = SupabaseSessionManager.getUserId(context)
                        if (!myUserId.isNullOrBlank() && event.recipientId == myUserId) {
                            Log.d(TAG, "New live DM message received for me! Processing...")
                            processIncomingMessageRecord(context, event.record)
                        }
                    }
                    is RealtimeTableEvent.Reconnected -> {
                        Log.d(TAG, "Realtime reconnected; checking pending DM mailbox...")
                        fetchAndProcessPendingInbox(context)
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Polls the temporary mailbox on Supabase (dm_messages where recipient_id = self),
     * decrypts each message, stores it in Room, and immediately deletes it from server.
     */
    suspend fun fetchAndProcessPendingInbox(context: Context) = withContext(Dispatchers.IO) {
        try {
            val myUserId = SupabaseSessionManager.getUserId(context) ?: return@withContext
            val token = SupabaseSessionManager.getBearerToken(context)
            if (token.isBlank()) return@withContext

            val resp = SupabaseClient.api.getPendingDmMessages(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                recipientIdFilter = "eq.$myUserId"
            )

            if (!resp.isSuccessful) {
                Log.w(TAG, "fetchAndProcessPendingInbox failed: ${resp.code()} ${resp.message()}")
                return@withContext
            }

            val list = resp.body() ?: emptyList()
            if (list.isEmpty()) return@withContext

            Log.d(TAG, "Found ${list.size} pending encrypted messages in mailbox")
            for (msg in list) {
                try {
                    // Decrypt ciphertext using our private keyset
                    val plaintext = E2EEManager.decryptMessage(context, msg.ciphertext)

                    // Store in local encrypted database
                    saveDecryptedMessageLocally(
                        context = context,
                        messageId = msg.id,
                        conversationId = msg.conversation_id,
                        senderId = msg.sender_id,
                        recipientId = msg.recipient_id,
                        plaintext = plaintext,
                        timestamp = parseTimestampToEpoch(msg.created_at)
                    )

                    // Immediately DELETE from server to enforce ephemeral mailbox rule
                    deleteMessageFromServer(token, msg.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decrypt and process message ${msg.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchAndProcessPendingInbox", e)
        }
    }

    private suspend fun processIncomingMessageRecord(context: Context, record: JSONObject?) {
        if (record == null) return
        try {
            val msgId = record.optString("id")
            val convId = record.optString("conversation_id")
            val senderId = record.optString("sender_id")
            val recipientId = record.optString("recipient_id")
            val ciphertext = record.optString("ciphertext")
            val createdAt = record.optString("created_at")

            if (msgId.isBlank() || ciphertext.isBlank()) return

            val plaintext = E2EEManager.decryptMessage(context, ciphertext)

            saveDecryptedMessageLocally(
                context = context,
                messageId = msgId,
                conversationId = convId,
                senderId = senderId,
                recipientId = recipientId,
                plaintext = plaintext,
                timestamp = parseTimestampToEpoch(createdAt)
            )

            // Delete from server immediately
            val token = SupabaseSessionManager.getBearerToken(context)
            deleteMessageFromServer(token, msgId)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing incoming message record", e)
        }
    }

    private suspend fun saveDecryptedMessageLocally(
        context: Context,
        messageId: String,
        conversationId: String,
        senderId: String,
        recipientId: String,
        plaintext: String,
        timestamp: Long
    ) {
        val db = database
        if (db == null) {
            Log.w(TAG, "Vault locked or database null; message $messageId will remain in server queue until unlock")
            return
        }

        // Insert message
        val localMsg = LocalDmMessage(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            recipientId = recipientId,
            text = plaintext,
            timestamp = timestamp,
            isOutgoing = false
        )
        db.messageDao().insertMessage(localMsg)

        // Find or create conversation
        val existingConv = db.conversationDao().getConversation(conversationId)
        if (existingConv != null) {
            db.conversationDao().updateLastMessage(
                conversationId = conversationId,
                text = plaintext,
                time = timestamp,
                unreadDelta = 1
            )
        } else {
            // Fetch sender profile details if available
            val senderName = fetchSenderDisplayName(context, senderId)
            val newConv = LocalDmConversation(
                id = conversationId,
                otherUserId = senderId,
                otherUserName = senderName,
                lastMessageText = plaintext,
                lastMessageTime = timestamp,
                unreadCount = 1,
                createdAt = timestamp
            )
            db.conversationDao().insertOrUpdate(newConv)
        }
    }

    private suspend fun deleteMessageFromServer(token: String, messageId: String) {
        try {
            val resp = SupabaseClient.api.deleteDmMessage(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                idFilter = "eq.$messageId"
            )
            if (resp.isSuccessful) {
                Log.d(TAG, "Deleted processed message $messageId from Supabase mailbox")
            } else {
                Log.w(TAG, "Failed to delete message $messageId: code=${resp.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting message $messageId from server", e)
        }
    }

    /**
     * Sends an encrypted 1:1 message to recipient.
     */
    suspend fun sendMessage(
        context: Context,
        recipientUserId: String,
        recipientName: String,
        recipientAvatar: String?,
        text: String,
        replyToText: String? = null,
        replyToSender: String? = null
    ): Result<LocalDmMessage> = withContext(Dispatchers.IO) {
        try {
            val db = database ?: return@withContext Result.failure(IllegalStateException("Chat vault is locked"))
            val myUserId = SupabaseSessionManager.ensureActiveSession(context)
                ?: SupabaseSessionManager.getUserId(context)
                ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: SupabaseSessionManager.getBearerToken(context)

            // 1. Fetch recipient's public key from Supabase user_public_keys
            val displayName = recipientName.ifBlank { "Ye user" }
            if (recipientUserId.isBlank() || recipientUserId.startsWith("partner_mock")) {
                return@withContext Result.failure(
                    RecipientNotReadyException(displayName)
                )
            }

            val resolvedUserId = resolveRecipientAuthUserId(context, recipientUserId) ?: recipientUserId

            val keyResp = SupabaseClient.api.getUserPublicKey(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                userIdFilter = "eq.$resolvedUserId"
            )
            if (!keyResp.isSuccessful || keyResp.body().isNullOrEmpty()) {
                Log.w(TAG, "Recipient $recipientUserId (resolved: $resolvedUserId) has no entry in user_public_keys (user hasn't opened app yet).")
                return@withContext Result.failure(
                    RecipientNotReadyException(displayName)
                )
            }
            val keyItem = keyResp.body()!!.first()
            val recipientPublicKey = keyItem.identity_public_key
            if (recipientPublicKey.isNullOrBlank()) {
                Log.w(TAG, "Recipient $recipientUserId (resolved: $resolvedUserId) public key is blank in user_public_keys.")
                return@withContext Result.failure(
                    RecipientNotReadyException(displayName)
                )
            }

            // 2. Call RPC get_or_create_dm_conversation
            val rpcResp = SupabaseClient.api.getOrCreateDmConversation(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GetOrCreateDmConversationRequest(p_other_user_id = resolvedUserId)
            )
            if (!rpcResp.isSuccessful) {
                return@withContext Result.failure(
                    IllegalStateException("Could not establish conversation: ${rpcResp.message()}")
                )
            }
            val rawUuid = rpcResp.body()?.string()?.trim() ?: ""
            val conversationId = parseUuidResponse(rawUuid)
                ?: return@withContext Result.failure(IllegalStateException("Failed to get valid conversation ID"))

            // 3. Encrypt message plaintext with Tink HPKE
            val ciphertext = E2EEManager.encryptMessage(recipientPublicKey, text)

            // 4. Insert into dm_messages (ephemeral server mailbox)
            val sendResp = SupabaseClient.api.sendDmMessage(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = SendDmMessageRequest(
                    conversation_id = conversationId,
                    sender_id = myUserId,
                    recipient_id = resolvedUserId,
                    ciphertext = ciphertext,
                    nonce = "",
                    key_version = keyItem.key_version
                )
            )
            if (!sendResp.isSuccessful) {
                return@withContext Result.failure(
                    IllegalStateException("Failed to send message: code ${sendResp.code()}")
                )
            }

            // 5. Save plaintext in local encrypted Room database
            val now = System.currentTimeMillis()
            val localMessageId = UUID.randomUUID().toString()
            val localMessage = LocalDmMessage(
                id = localMessageId,
                conversationId = conversationId,
                senderId = myUserId,
                recipientId = recipientUserId,
                text = text,
                timestamp = now,
                isOutgoing = true,
                replyToText = replyToText,
                replyToSender = replyToSender
            )
            db.messageDao().insertMessage(localMessage)

            // 6. Update or insert conversation in local Room DB
            val existingConv = db.conversationDao().getConversation(conversationId)
            val updatedConv = if (existingConv != null) {
                existingConv.copy(
                    lastMessageText = text,
                    lastMessageTime = now,
                    otherUserName = recipientName.ifBlank { existingConv.otherUserName },
                    otherUserAvatar = recipientAvatar ?: existingConv.otherUserAvatar
                )
            } else {
                LocalDmConversation(
                    id = conversationId,
                    otherUserId = recipientUserId,
                    otherUserName = recipientName,
                    otherUserAvatar = recipientAvatar,
                    lastMessageText = text,
                    lastMessageTime = now,
                    unreadCount = 0,
                    createdAt = now
                )
            }
            db.conversationDao().insertOrUpdate(updatedConv)

            Log.d(TAG, "Sent message encrypted with Tink HPKE to user $recipientUserId, saved to local DB.")
            Result.success(localMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }

    fun getMessagesFlow(conversationId: String): Flow<List<LocalDmMessage>> {
        val db = database ?: return flowOf(emptyList())
        return db.messageDao().getMessages(conversationId)
    }

    suspend fun markConversationRead(conversationId: String) = withContext(Dispatchers.IO) {
        try {
            database?.conversationDao()?.markRead(conversationId)
        } catch (e: Exception) {
            Log.w(TAG, "Error marking conversation read", e)
        }
    }

    /**
     * Resolves a recipient user ID or username/display name to their exact Supabase auth_user_id UUID.
     */
    suspend fun resolveRecipientAuthUserId(context: Context, inputIdOrName: String): String? = withContext(Dispatchers.IO) {
        if (inputIdOrName.isBlank() || inputIdOrName.startsWith("partner_mock") || inputIdOrName.startsWith("c_")) {
            return@withContext null
        }

        // 1. Ensure active session and token
        SupabaseSessionManager.ensureActiveSession(context)
        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            ?: SupabaseSessionManager.getBearerToken(context)

        // 2. If it's already a valid UUID, check if user_public_keys has a row directly
        if (SupabaseSessionManager.isValidUuid(inputIdOrName)) {
            try {
                val keyResp = SupabaseClient.api.getUserPublicKey(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    userIdFilter = "eq.$inputIdOrName"
                )
                if (keyResp.isSuccessful && !keyResp.body().isNullOrEmpty()) {
                    Log.d(TAG, "resolveRecipientAuthUserId: $inputIdOrName is valid UUID with user_public_keys row")
                    return@withContext inputIdOrName
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct check of user_public_keys for $inputIdOrName failed: ${e.message}")
            }
        }

        // 3. Look up profile by auth_user_id or profile_name / username / email
        try {
            val profileRes = SupabaseClient.api.getProfile(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                authUserIdEq = "eq.$inputIdOrName"
            )
            if (profileRes.isSuccessful && !profileRes.body().isNullOrEmpty()) {
                val p = profileRes.body()!!.first()
                if (!p.auth_user_id.isNullOrBlank()) {
                    Log.d(TAG, "resolveRecipientAuthUserId: matched $inputIdOrName to auth_user_id=${p.auth_user_id} via getProfile auth_user_id")
                    return@withContext p.auth_user_id
                }
            }
        } catch (_: Exception) {}

        try {
            val allProfilesRes = SupabaseClient.api.getAllProfiles(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = token
            )
            if (allProfilesRes.isSuccessful && !allProfilesRes.body().isNullOrEmpty()) {
                val match = allProfilesRes.body()!!.find { p ->
                    p.auth_user_id?.equals(inputIdOrName, ignoreCase = true) == true ||
                    p.profile_name?.equals(inputIdOrName, ignoreCase = true) == true ||
                    p.username?.equals(inputIdOrName, ignoreCase = true) == true ||
                    p.email?.equals(inputIdOrName, ignoreCase = true) == true
                }
                if (match?.auth_user_id != null) {
                    Log.d(TAG, "resolveRecipientAuthUserId: matched $inputIdOrName to auth_user_id=${match.auth_user_id} via getAllProfiles match")
                    return@withContext match.auth_user_id
                }
            }
        } catch (_: Exception) {}

        if (SupabaseSessionManager.isValidUuid(inputIdOrName)) {
            return@withContext inputIdOrName
        }

        return@withContext null
    }

    suspend fun getOrCreateLocalConversation(
        context: Context,
        otherUserId: String,
        otherUserName: String,
        otherUserAvatar: String? = null
    ): LocalDmConversation = withContext(Dispatchers.IO) {
        val db = database ?: throw IllegalStateException("Chat vault is locked")
        val resolvedUserId = resolveRecipientAuthUserId(context, otherUserId) ?: otherUserId

        val existing = db.conversationDao().getConversationByOtherUser(resolvedUserId)
        if (existing != null) return@withContext existing

        // Establish conversation via RPC if online
        var convId = UUID.randomUUID().toString()
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: SupabaseSessionManager.getBearerToken(context)
            val rpcResp = SupabaseClient.api.getOrCreateDmConversation(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GetOrCreateDmConversationRequest(p_other_user_id = resolvedUserId)
            )
            if (rpcResp.isSuccessful) {
                val raw = rpcResp.body()?.string()?.trim() ?: ""
                val parsed = parseUuidResponse(raw)
                if (!parsed.isNullOrBlank()) {
                    convId = parsed
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch RPC conversation ID, using temporary UUID", e)
        }

        val newConv = LocalDmConversation(
            id = convId,
            otherUserId = resolvedUserId,
            otherUserName = otherUserName,
            otherUserAvatar = otherUserAvatar,
            lastMessageText = "",
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = 0
        )
        db.conversationDao().insertOrUpdate(newConv)
        newConv
    }

    suspend fun checkRecipientKeyStatus(context: Context, recipientUserId: String): Boolean = withContext(Dispatchers.IO) {
        if (recipientUserId.isBlank() || recipientUserId.startsWith("partner_mock")) return@withContext false
        try {
            // 1. Ensure authenticated session
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: SupabaseSessionManager.getBearerToken(context)

            // 2. Resolve to exact Supabase auth_user_id UUID
            val resolvedUserId = resolveRecipientAuthUserId(context, recipientUserId) ?: recipientUserId

            Log.d(TAG, "checkRecipientKeyStatus: input=$recipientUserId -> resolved=$resolvedUserId")
            Log.d(TAG, "checkRecipientKeyStatus request: GET rest/v1/user_public_keys?user_id=eq.$resolvedUserId with token present=${!token.isBlank()}")

            val resp = SupabaseClient.api.getUserPublicKey(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                userIdFilter = "eq.$resolvedUserId"
            )

            Log.d(TAG, "checkRecipientKeyStatus response: code=${resp.code()}, body=${resp.body()}")

            if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                val key = resp.body()!!.first().identity_public_key
                !key.isNullOrBlank()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking recipient key status", e)
            false
        }
    }

    private fun parseUuidResponse(raw: String): String? {
        val clean = raw.trim('"', ' ', '\n', '\r')
        if (clean.contains("-") && clean.length >= 32) {
            return clean
        }
        return try {
            val json = JSONObject(clean)
            json.optString("id").ifBlank { json.optString("conversation_id") }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchSenderDisplayName(context: Context, senderId: String): String {
        return try {
            val token = SupabaseSessionManager.getBearerToken(context)
            val resp = SupabaseClient.api.getProfile(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                authUserIdEq = "eq.$senderId"
            )
            if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                val profile = resp.body()!!.first()
                profile.profile_name ?: profile.username ?: "FunkyTalk User"
            } else {
                "FunkyTalk User"
            }
        } catch (e: Exception) {
            "FunkyTalk User"
        }
    }

    private fun parseTimestampToEpoch(isoStr: String?): Long {
        if (isoStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(isoStr.take(19))?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
