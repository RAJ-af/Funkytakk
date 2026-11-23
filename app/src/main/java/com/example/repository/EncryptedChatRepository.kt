package com.example.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.ActiveUserManager
import com.example.AppwriteUploader
import com.example.BuildConfig
import com.example.CloudinaryUploader
import com.example.CreateSignedUrlRequest
import com.example.GetOrCreateDmConversationRequest
import com.example.SendDmMessageRequest
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.crypto.ChatTimingTracker
import com.example.crypto.E2EEManager
import com.example.crypto.E2EEMediaManager
import com.example.crypto.VaultSecurityManager
import com.example.crypto.VaultStatus
import com.example.db.EncryptedChatDatabase
import com.example.db.LocalDmConversation
import com.example.db.LocalDmMessage
import com.example.db.LocalMessageStatus
import com.example.realtime.RealtimeTableEvent
import com.example.realtime.VoiceRoomRealtimeManager
import com.example.ui.components.AppNetworkManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RecipientNotReadyException(val recipientName: String) : Exception(
    "$recipientName isn't ready for secure chat yet. This user has not opened the app or registered encryption keys yet."
)

object EncryptedChatRepository {
    private const val TAG = "EncryptedChatRepo"
    private const val KEY_CACHE_TTL_MS = 10 * 60 * 1000L
    private const val MAX_AUTO_RETRIES = 3
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class CachedPublicKey(
        val publicKey: String,
        val keyVersion: Int,
        val fetchedAt: Long = System.currentTimeMillis()
    )

    @Volatile
    private var database: EncryptedChatDatabase? = null

    fun getDatabase(context: Context? = null): EncryptedChatDatabase? = database

    suspend fun drainPendingDmMessages(context: Context) = fetchAndProcessPendingInbox(context)

    private val _conversations = MutableStateFlow<List<LocalDmConversation>>(emptyList())
    val conversations: StateFlow<List<LocalDmConversation>> = _conversations.asStateFlow()

    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount.asStateFlow()

    // Instant in-memory optimistic overlay so the bubble shows in the exact tap frame even on slow eMMC devices
    private val _optimisticMessages = MutableStateFlow<List<LocalDmMessage>>(emptyList())
    val optimisticMessages: StateFlow<List<LocalDmMessage>> = _optimisticMessages.asStateFlow()

    private val _conversationIdAliases = MutableStateFlow<Map<String, String>>(emptyMap())
    val conversationIdAliases: StateFlow<Map<String, String>> = _conversationIdAliases.asStateFlow()

    private val processedMessageIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val publicKeyCache = ConcurrentHashMap<String, CachedPublicKey>()
    private val resolvedUserIdCache = ConcurrentHashMap<String, String>()
    private val verifiedServerConvIds = ConcurrentHashMap.newKeySet<String>()
    private val offlineTempConvIds = ConcurrentHashMap.newKeySet<String>()
    private val senderProfileCache = ConcurrentHashMap<String, Triple<String, String?, String?>>()
    private val conversationSendMutexes = ConcurrentHashMap<String, Mutex>()
    private val outboxResumeMutex = Mutex()
    private val activeSendClientIds = ConcurrentHashMap.newKeySet<String>()

    private var conversationCollectJob: Job? = null
    private var realtimeListenerJob: Job? = null
    private var vaultObserverJob: Job? = null
    private var networkObserverJob: Job? = null

    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        val appContext = context.applicationContext
        VaultSecurityManager.init(appContext)
        AppNetworkManager.startMonitoring(appContext)
        if (isInitialized) {
            if (VaultSecurityManager.vaultStatus.value == VaultStatus.UNLOCKED && database == null) {
                VaultSecurityManager.getActiveVaultKey(appContext)?.let { openDatabase(appContext, it) }
            }
            return
        }
        isInitialized = true

        // Observe vault status
        vaultObserverJob?.cancel()
        vaultObserverJob = scope.launch {
            VaultSecurityManager.vaultStatus.collect { status ->
                when (status) {
                    VaultStatus.UNLOCKED -> {
                        val key = VaultSecurityManager.getActiveVaultKey(appContext)
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

        // Start listening to realtime & network restoration events
        startRealtimeListener(appContext)
        startNetworkObserver(appContext)
    }

    private fun startNetworkObserver(context: Context) {
        networkObserverJob?.cancel()
        networkObserverJob = scope.launch {
            AppNetworkManager.networkRestoredEvents.collect {
                Log.d(TAG, "Network restored; reconnecting realtime, draining DM inbox, and retrying outbox")
                VoiceRoomRealtimeManager.reconnectNowIfNeeded()
                launch { fetchAndProcessPendingInbox(context) }
                launch { resumePendingOutbox(context) }
            }
        }
    }

    fun onAppResumed(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            VoiceRoomRealtimeManager.reconnectNowIfNeeded()
            fetchAndProcessPendingInbox(appContext)
            resumePendingOutbox(appContext)
        }
    }

    private fun openDatabase(context: Context, vaultKey: ByteArray) {
        try {
            val uid = ActiveUserManager.getActiveUid(context) ?: return
            database = EncryptedChatDatabase.getInstance(context, vaultKey, uid)
            Log.d(TAG, "SQLCipher EncryptedChatDatabase opened successfully for UID $uid.")

            conversationCollectJob?.cancel()
            conversationCollectJob = scope.launch {
                database?.conversationDao()?.getAllConversations()?.collect { list ->
                    _conversations.value = list
                    _totalUnreadCount.value = list.sumOf { it.unreadCount }
                }
            }

            // Sync identity key, process mailbox, & resume persisted outbox
            scope.launch {
                val myUserId = SupabaseSessionManager.getUserId(context)
                if (!myUserId.isNullOrBlank()) {
                    launch { E2EEManager.ensureIdentityKeyInitialized(context) }
                    VoiceRoomRealtimeManager.subscribeDmInbox(myUserId)
                    launch { fetchAndProcessPendingInbox(context) }
                    launch { resumePendingOutbox(context, fromColdStart = true) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening EncryptedChatDatabase", e)
        }
    }

    fun closeDatabase() {
        conversationCollectJob?.cancel()
        conversationCollectJob = null
        database = null
        _conversations.value = emptyList()
        _totalUnreadCount.value = 0
        _optimisticMessages.value = emptyList()
        EncryptedChatDatabase.closeInstance()
    }

    fun resetForLogout() {
        closeDatabase()
        processedMessageIds.clear()
        publicKeyCache.clear()
        resolvedUserIdCache.clear()
        verifiedServerConvIds.clear()
        offlineTempConvIds.clear()
        senderProfileCache.clear()
        conversationSendMutexes.clear()
        activeSendClientIds.clear()
        _conversationIdAliases.value = emptyMap()
    }

    private fun startRealtimeListener(context: Context) {
        realtimeListenerJob?.cancel()
        realtimeListenerJob = scope.launch {
            VoiceRoomRealtimeManager.events.collect { event ->
                when (event) {
                    is RealtimeTableEvent.DmMessageReceived -> {
                        val myUserId = SupabaseSessionManager.getUserId(context)
                        if (!myUserId.isNullOrBlank() && event.recipientId == myUserId) {
                            processIncomingMessageRecord(context, event.record)
                        }
                    }
                    is RealtimeTableEvent.Reconnected -> {
                        Log.d(TAG, "Realtime reconnected; checking pending DM mailbox...")
                        launch { fetchAndProcessPendingInbox(context) }
                        launch { resumePendingOutbox(context) }
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
        var fetchedCount = 0
        var savedCount = 0
        var failedCount = 0
        var lastError: String? = null

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
                lastError = "HTTP ${resp.code()}: ${resp.message()}"
                Log.w(TAG, "fetchAndProcessPendingInbox failed: $lastError")
                InboxDrainTracker.recordDrainResult(0, 0, 0, lastError)
                return@withContext
            }

            val list = resp.body() ?: emptyList()
            fetchedCount = list.size
            if (list.isEmpty()) {
                InboxDrainTracker.recordDrainResult(0, 0, 0, null)
                return@withContext
            }

            Log.d(TAG, "Found $fetchedCount pending encrypted DM messages in mailbox")
            val semaphore = Semaphore(4)
            val results = coroutineScope {
                list.map { msg ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            try {
                                processIncomingDmMessage(
                                    context = context,
                                    token = token,
                                    msgId = msg.id,
                                    convId = msg.conversation_id,
                                    senderId = msg.sender_id,
                                    recipientId = msg.recipient_id,
                                    ciphertext = msg.ciphertext,
                                    messageType = msg.message_type ?: "text",
                                    storagePath = msg.storage_path,
                                    createdAtIso = msg.created_at
                                )
                            } catch (e: Exception) {
                                lastError = e.message
                                Log.w(TAG, "Failed to decrypt and process message ${msg.id}: ${e.message}")
                                false
                            }
                        }
                    }
                }.awaitAll()
            }
            savedCount = results.count { it }
            failedCount = results.count { !it }
        } catch (e: Exception) {
            lastError = e.message
            Log.w(TAG, "Error in fetchAndProcessPendingInbox: ${e.message}")
        } finally {
            InboxDrainTracker.recordDrainResult(fetchedCount, savedCount, failedCount, lastError)
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
            val messageType = record.optString("message_type", "text")
            val storagePath = record.optString("storage_path")
            val createdAt = record.optString("created_at")

            if (msgId.isBlank() || ciphertext.isBlank()) return

            val token = SupabaseSessionManager.getBearerToken(context)
            processIncomingDmMessage(
                context = context,
                token = token,
                msgId = msgId,
                convId = convId,
                senderId = senderId,
                recipientId = recipientId,
                ciphertext = ciphertext,
                messageType = messageType,
                storagePath = storagePath,
                createdAtIso = createdAt
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error processing incoming message record: ${e.message}")
        }
    }

    private suspend fun processIncomingDmMessage(
        context: Context,
        token: String,
        msgId: String,
        convId: String,
        senderId: String,
        recipientId: String,
        ciphertext: String,
        messageType: String = "text",
        storagePath: String? = null,
        createdAtIso: String? = null
    ): Boolean {
        if (msgId.isBlank() || ciphertext.isBlank()) return false

        val db = database
        if (db == null) {
            Log.w(TAG, "Vault locked or database null; skipping message $msgId")
            return false
        }

        // Deduplication
        if (processedMessageIds.contains(msgId) || db.messageDao().messageExists(msgId)) {
            processedMessageIds.add(msgId)
            scope.launch(Dispatchers.IO) {
                deleteMessageFromServer(context, token, msgId)
                db.inboxFailureDao().deleteFailure(msgId)
            }
            return true
        }

        val timestamp = parseTimestampToEpoch(createdAtIso)
        val rxStartMs = ChatTimingTracker.nowMs()

        // 1. Decrypt using Tink HybridDecrypt on Dispatchers.Default
        val decryptedText = try {
            E2EEManager.decryptMessageAsync(context, ciphertext)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decrypt DM message $msgId: ${e.message}")
            db.inboxFailureDao().insertFailure(
                com.example.db.InboxFailureEntity(
                    messageId = msgId,
                    type = "DM",
                    reason = "Decrypt failed: ${e.message ?: "Invalid key material"}"
                )
            )
            return false
        }
        val receiveLatencyMs = (ChatTimingTracker.nowMs() - rxStartMs).coerceAtLeast(0L)
        ChatTimingTracker.logServerEchoOrReceive("DM", msgId, receiveLatencyMs, isEcho = false)

        // 2. Check Reaction
        val trimmed = decryptedText.trim()
        val isReaction = messageType == "reaction" || (trimmed.startsWith("{") && trimmed.endsWith("}") && (trimmed.contains("\"t\":\"reaction\"") || trimmed.contains("\"reaction\"")))
        val reactionJson = if (isReaction) {
            try {
                val j = JSONObject(trimmed)
                if (j.optString("t") == "reaction" || j.has("target")) j else null
            } catch (_: Exception) {
                null
            }
        } else null

        if (reactionJson != null) {
            val targetId = reactionJson.optString("target").ifBlank { reactionJson.optString("target_message_id") }
            val emoji = reactionJson.optString("emoji")
            val op = reactionJson.optString("op", "set")
            val rxTs = reactionJson.optLong("ts", timestamp)

            if (targetId.isNotBlank()) {
                if (db.messageDao().messageExists(targetId)) {
                    if (op == "remove") {
                        db.dmMessageReactionDao().deleteReaction(targetId, senderId)
                    } else if (emoji.isNotBlank()) {
                        db.dmMessageReactionDao().insertReaction(
                            com.example.db.LocalDmMessageReaction(
                                conversationId = convId,
                                targetMessageId = targetId,
                                senderId = senderId,
                                emoji = emoji,
                                ts = rxTs
                            )
                        )
                        val targetMsg = db.messageDao().getMessage(targetId)
                        val myUserId = SupabaseSessionManager.getUserId(context)
                        if (targetMsg?.isOutgoing == true && senderId != myUserId &&
                            !com.example.service.PushNotificationManager.isChatOpenInForeground(
                                conversationId = convId,
                                partnerUserId = senderId
                            )
                        ) {
                            val senderName = fetchSenderDisplayName(context, senderId)
                            com.example.util.SystemNotificationHelper.showSystemNotification(
                                context = context,
                                title = "FunkyTalk Reaction",
                                message = "$senderName reacted $emoji to your message",
                                partnerUserId = senderId,
                                conversationId = convId,
                                senderId = senderId,
                                kind = "dm",
                                notificationId = com.example.service.PushNotificationManager.notificationIdForChat(
                                    conversationId = convId,
                                    fallbackPartnerId = senderId
                                ),
                                channelId = com.example.util.SystemNotificationHelper.CHANNEL_MESSAGES_ID
                            )
                        }
                    }
                }
            }
            processedMessageIds.add(msgId)
            deleteMessageFromServer(context, token, msgId)
            db.inboxFailureDao().deleteFailure(msgId)
            return true
        }

        // 3. Media or Text
        val mediaDescriptorJson = if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val json = JSONObject(trimmed)
                if (json.has("media_key") && json.has("media_nonce")) json else null
            } catch (_: Exception) {
                null
            }
        } else null

        return try {
            if (mediaDescriptorJson != null) {
                processIncomingMediaDescriptor(
                    context = context,
                    token = token,
                    msgId = msgId,
                    convId = convId,
                    senderId = senderId,
                    recipientId = recipientId,
                    descriptorJson = mediaDescriptorJson,
                    messageType = messageType.ifBlank { "image" },
                    storagePath = storagePath ?: "",
                    timestamp = timestamp
                )
            } else {
                saveDecryptedMessageLocally(
                    context = context,
                    messageId = msgId,
                    conversationId = convId,
                    senderId = senderId,
                    recipientId = recipientId,
                    plaintext = decryptedText,
                    timestamp = timestamp
                )
                deleteMessageFromServer(context, token, msgId)
            }
            processedMessageIds.add(msgId)
            db.inboxFailureDao().deleteFailure(msgId)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save decrypted text message $msgId locally: ${e.message}")
            db.inboxFailureDao().insertFailure(
                com.example.db.InboxFailureEntity(
                    messageId = msgId,
                    type = "DM",
                    reason = "Save failed: ${e.message}"
                )
            )
            false
        }
    }

    private fun cleanStoragePath(path: String): String {
        var clean = path.trim()
        while (clean.startsWith("/") || clean.startsWith("dm-media/")) {
            clean = clean.removePrefix("/").removePrefix("dm-media/")
        }
        return clean
    }

    private fun isJsonErrorBytes(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        val checkLen = minOf(bytes.size, 256)
        val snippet = String(bytes, 0, checkLen).trim()
        return snippet.startsWith("{\"error\"") ||
               snippet.startsWith("{\"statusCode\"") ||
               snippet.startsWith("{\"message\"") ||
               snippet.startsWith("<!DOCTYPE") ||
               snippet.startsWith("<html")
    }

    private suspend fun downloadDmMediaBlob(
        context: Context,
        authToken: String,
        rawPath: String,
        cleanPath: String,
        convId: String,
        msgId: String
    ): ByteArray? {
        if (rawPath.startsWith("data:")) {
            return try {
                val base64Data = rawPath.substringAfter(",")
                Base64.decode(base64Data, Base64.DEFAULT)
            } catch (e: Exception) {
                Log.e(TAG, "Failed decoding base64 data URI in downloadDmMediaBlob", e)
                null
            }
        }

        if (rawPath.startsWith("http://") || rawPath.startsWith("https://")) {
            try {
                val isSupa = rawPath.contains("supabase.co")
                val resp = SupabaseClient.api.downloadFromUrl(
                    url = rawPath,
                    apiKey = if (isSupa) com.example.BuildConfig.SUPABASE_ANON_KEY else null,
                    auth = if (isSupa) authToken else null
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val bytes = resp.body()!!.bytes()
                    if (bytes.isNotEmpty() && !isJsonErrorBytes(bytes)) {
                        return bytes
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed downloading from direct URL in downloadDmMediaBlob: $rawPath", e)
            }
        }

        val candidates = linkedSetOf<String>()
        val stripped = cleanPath.trim().removePrefix("/").removePrefix("dm-media/").removePrefix("/")
        if (stripped.isNotBlank()) {
            candidates.add(stripped)
            candidates.add("dm-media/$stripped")
        }
        val rawTrimmed = rawPath.trim().removePrefix("/")
        if (rawTrimmed.isNotBlank()) {
            candidates.add(rawTrimmed)
            candidates.add(rawTrimmed.removePrefix("dm-media/").removePrefix("/"))
        }
        if (convId.isNotBlank() && msgId.isNotBlank()) {
            candidates.add("$convId/$msgId")
            candidates.add("dm-media/$convId/$msgId")
        }

        val tokens = linkedSetOf<String>()
        if (authToken.isNotBlank()) tokens.add(authToken)
        val sessionBearer = SupabaseSessionManager.getAuthenticatedBearerToken(context)
        if (!sessionBearer.isNullOrBlank()) tokens.add(sessionBearer)
        val accessTok = SupabaseSessionManager.getAccessToken(context)
        if (!accessTok.isNullOrBlank()) tokens.add("Bearer $accessTok")
        tokens.add("Bearer ${com.example.BuildConfig.SUPABASE_ANON_KEY}")

        for (path in candidates) {
            for (authHeader in tokens) {
                // Strategy 1: Direct authenticated storage download
                try {
                    val resp = SupabaseClient.api.downloadDmMediaAuthenticated(
                        apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                        auth = authHeader,
                        path = path
                    )
                    if (resp.isSuccessful && resp.body() != null) {
                        val bytes = resp.body()!!.bytes()
                        if (bytes.isNotEmpty() && !isJsonErrorBytes(bytes)) {
                            Log.d(TAG, "Direct authenticated download succeeded for $path (${bytes.size} bytes)")
                            return bytes
                        }
                    }
                } catch (_: Exception) {}

                // Strategy 2: Direct standard storage download
                try {
                    val resp = SupabaseClient.api.downloadDmMedia(
                        apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                        auth = authHeader,
                        path = path
                    )
                    if (resp.isSuccessful && resp.body() != null) {
                        val bytes = resp.body()!!.bytes()
                        if (bytes.isNotEmpty() && !isJsonErrorBytes(bytes)) {
                            Log.d(TAG, "Standard object download succeeded for $path (${bytes.size} bytes)")
                            return bytes
                        }
                    }
                } catch (_: Exception) {}

                // Strategy 3: Supabase Storage Signed URL
                try {
                    val filename = path.removePrefix("dm-media/").removePrefix("/")
                    val signResp = SupabaseClient.api.createSignedUrl(
                        apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                        auth = authHeader,
                        bucket = "dm-media",
                        filename = filename,
                        body = CreateSignedUrlRequest(expiresIn = 3600)
                    )
                    if (signResp.isSuccessful && signResp.body() != null) {
                        val body = signResp.body()!!
                        val signedPath = body.getResolvedPath()
                        if (!signedPath.isNullOrBlank()) {
                            val baseUrl = com.example.BuildConfig.SUPABASE_URL.trimEnd('/')
                            val prefix = if (baseUrl.startsWith("http")) baseUrl else "https://$baseUrl"
                            val formattedPath = if (signedPath.startsWith("/")) signedPath else "/$signedPath"
                            val fullSignedUrl = if (formattedPath.startsWith("/storage/v1")) {
                                "$prefix$formattedPath"
                            } else {
                                "$prefix/storage/v1$formattedPath"
                            }

                            for (signedAuth in listOf(authHeader, null)) {
                                try {
                                    val signedDownloadResp = SupabaseClient.api.downloadFromUrl(
                                        url = fullSignedUrl,
                                        apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                                        auth = signedAuth
                                    )
                                    if (signedDownloadResp.isSuccessful && signedDownloadResp.body() != null) {
                                        val bytes = signedDownloadResp.body()!!.bytes()
                                        if (bytes.isNotEmpty() && !isJsonErrorBytes(bytes)) {
                                            Log.d(TAG, "Signed URL download succeeded for $path (${bytes.size} bytes)")
                                            return bytes
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        Log.w(TAG, "Media blob unavailable at path $cleanPath after trying all download strategies; falling back to placeholder")
        return null
    }

    private suspend fun processIncomingMediaDescriptor(
        context: Context,
        token: String,
        msgId: String,
        convId: String,
        senderId: String,
        recipientId: String,
        descriptorJson: JSONObject,
        messageType: String,
        storagePath: String,
        timestamp: Long
    ) {
        val rawPath = descriptorJson.optString("storage_path").ifBlank { storagePath }
        val cleanPath = cleanStoragePath(rawPath).ifBlank { "$convId/$msgId" }
        val keyBase64 = descriptorJson.optString("media_key")
        val nonceBase64 = descriptorJson.optString("media_nonce")
        val mimeType = descriptorJson.optString("mime_type", if (messageType == "image") "image/jpeg" else "audio/aac")
        val durationSecs = descriptorJson.optInt("duration_seconds", 0)
        val captionSuffix = descriptorJson.optString("caption", "")
            .replace("\n", " ")
            .replace("|", " ")
            .trim()
            .takeIf { it.isNotBlank() }

        val initialPlaceholder = if (messageType == "image") {
            if (captionSuffix != null) "[img]downloading...|$captionSuffix" else "[img]downloading..."
        } else {
            "[voice]downloading...|$durationSecs"
        }

        // 1. Save local message record in Room FIRST (Strict Order of Operations)
        saveDecryptedMessageLocally(
            context = context,
            messageId = msgId,
            conversationId = convId,
            senderId = senderId,
            recipientId = recipientId,
            plaintext = initialPlaceholder,
            timestamp = timestamp
        )

        // 2. Acknowledge server row ONLY after local record is written
        deleteMessageFromServer(context, token, msgId)

        // 3. Download & decrypt media payload in background with retries
        scope.launch(Dispatchers.IO) {
            try {
                if (keyBase64.isBlank() || nonceBase64.isBlank()) return@launch
                val keyBytes = Base64.decode(keyBase64, Base64.DEFAULT)
                val nonceBytes = Base64.decode(nonceBase64, Base64.DEFAULT)

                SupabaseSessionManager.ensureActiveSession(context)
                val authToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: (SupabaseSessionManager.getAccessToken(context)?.let { "Bearer $it" })
                    ?: token

                val encryptedBlobBytes = downloadDmMediaBlob(
                    context = context,
                    authToken = authToken,
                    rawPath = rawPath,
                    cleanPath = cleanPath,
                    convId = convId,
                    msgId = msgId
                )

                if (encryptedBlobBytes != null) {
                    val rawBytes = E2EEMediaManager.decryptMediaPayload(encryptedBlobBytes, keyBytes, nonceBytes)
                    val localFile = saveLocalMediaFile(context, msgId, rawBytes, mimeType)
                    val captionSuffix = descriptorJson.optString("caption", "")
                        .replace("\n", " ")
                        .replace("|", " ")
                        .trim()
                        .takeIf { it.isNotBlank() }
                    val localText = if (messageType == "image") {
                        if (captionSuffix != null) "[img]file://${localFile.absolutePath}|$captionSuffix" else "[img]file://${localFile.absolutePath}"
                    } else {
                        "[voice]file://${localFile.absolutePath}|$durationSecs"
                    }

                    saveDecryptedMessageLocally(
                        context = context,
                        messageId = msgId,
                        conversationId = convId,
                        senderId = senderId,
                        recipientId = recipientId,
                        plaintext = localText,
                        timestamp = timestamp
                    )

                    deleteStorageObjectFromServer(context, authToken, cleanPath)
                    Log.d(TAG, "Successfully downloaded, decrypted and updated media message $msgId")
                } else {
                    Log.w(TAG, "Media blob download returned null for $msgId; placeholder remains")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Background media processing failed for $msgId: ${e.message}")
            }
        }
    }

    private suspend fun saveLocalPlaceholderAndCleanup(
        context: Context,
        token: String,
        msgId: String,
        convId: String,
        senderId: String,
        recipientId: String,
        storagePath: String,
        timestamp: Long
    ) {
        saveDecryptedMessageLocally(
            context = context,
            messageId = msgId,
            conversationId = convId,
            senderId = senderId,
            recipientId = recipientId,
            plaintext = "⚠️ Media content unavailable",
            timestamp = timestamp
        )
        // Clean up from server so the client does not loop indefinitely and server copy is purged
        deleteMessageFromServer(context, token, msgId)
        deleteStorageObjectFromServer(context, token, storagePath)
    }

    private suspend fun deleteStorageObjectFromServer(context: Context, token: String, path: String) {
        if (path.isBlank()) return
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("data:")) {
            // Appwrite storage proxy or external URL - no supabase storage object to delete
            return
        }
        try {
            val cleanPath = cleanStoragePath(path)
            if (cleanPath.isBlank()) return
            val authToken = if (token.isNotBlank() && !token.contains(com.example.BuildConfig.SUPABASE_ANON_KEY)) {
                token
            } else {
                SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: (SupabaseSessionManager.getAccessToken(context)?.let { "Bearer $it" })
                    ?: token
            }
            val resp = SupabaseClient.api.deleteDmMedia(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = authToken,
                path = cleanPath
            )
            if (resp.isSuccessful) {
                Log.d(TAG, "Deleted storage object $cleanPath from dm-media bucket")
            } else {
                Log.w(TAG, "Failed to delete storage object $cleanPath: code=${resp.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting storage object $path: ${e.message}")
        }
    }

    private fun saveLocalMediaFile(context: Context, messageId: String, bytes: ByteArray, mimeType: String): File {
        val uid = com.example.ActiveUserManager.requireActiveUid(context)
        val dir = com.example.ActiveUserManager.mediaDir(context, uid).apply { mkdirs() }
        val ext = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("jpg") || mimeType.contains("jpeg") -> "jpg"
            mimeType.contains("aac") -> "aac"
            mimeType.contains("3gp") -> "3gp"
            mimeType.contains("m4a") -> "m4a"
            else -> "bin"
        }
        val file = File(dir, "${messageId}.$ext")
        file.writeBytes(bytes)
        return file
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

        verifiedServerConvIds.add(conversationId)

        // Insert message immediately into Room so it appears without waiting on profile network fetches
        val localMsg = LocalDmMessage(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            recipientId = recipientId,
            text = plaintext,
            timestamp = timestamp,
            isOutgoing = false,
            status = LocalMessageStatus.SENT
        )
        db.messageDao().insertMessage(localMsg)

        val isChatOpen = com.example.service.PushNotificationManager.isChatOpenInForeground(
            conversationId = conversationId,
            partnerUserId = senderId
        )

        val existingConv = db.conversationDao().getConversation(conversationId)
        val cachedProfile = senderProfileCache[senderId]
        val needsProfileSync = existingConv == null ||
            existingConv.otherUserAvatar.isNullOrBlank() ||
            existingConv.otherUserFlag.isNullOrBlank() ||
            existingConv.otherUserName.isBlank() ||
            existingConv.otherUserName == "FunkyTalk User"

        if (existingConv != null) {
            if (cachedProfile != null) {
                db.conversationDao().updateLastMessageAndProfile(
                    conversationId = conversationId,
                    text = plaintext,
                    time = timestamp,
                    unreadDelta = if (isChatOpen) 0 else 1,
                    name = cachedProfile.first.takeIf { it != "FunkyTalk User" },
                    avatar = cachedProfile.second ?: existingConv.otherUserAvatar,
                    flag = cachedProfile.third ?: existingConv.otherUserFlag
                )
            } else {
                db.conversationDao().updateLastMessage(
                    conversationId = conversationId,
                    text = plaintext,
                    time = timestamp,
                    unreadDelta = if (isChatOpen) 0 else 1
                )
            }
        } else {
            val newConv = LocalDmConversation(
                id = conversationId,
                otherUserId = senderId,
                otherUserName = cachedProfile?.first ?: "FunkyTalk User",
                otherUserAvatar = cachedProfile?.second,
                otherUserFlag = cachedProfile?.third,
                lastMessageText = plaintext,
                lastMessageTime = timestamp,
                unreadCount = if (isChatOpen) 0 else 1,
                createdAt = timestamp
            )
            db.conversationDao().insertOrUpdate(newConv)
        }

        if (needsProfileSync && cachedProfile == null) {
            scope.launch(Dispatchers.IO) {
                val fetched = fetchSenderProfileInfo(context, senderId)
                senderProfileCache[senderId] = fetched
                val current = db.conversationDao().getConversation(conversationId)
                if (current != null) {
                    db.conversationDao().updateLastMessageAndProfile(
                        conversationId = conversationId,
                        text = current.lastMessageText,
                        time = current.lastMessageTime,
                        unreadDelta = 0,
                        name = fetched.first.takeIf { it != "FunkyTalk User" },
                        avatar = fetched.second ?: current.otherUserAvatar,
                        flag = fetched.third ?: current.otherUserFlag
                    )
                }
            }
        }

        // Trigger Android System Push Notification for incoming messages (de-duplicated with PushNotificationManager by conversationId)
        val myUserId = SupabaseSessionManager.getUserId(context)
        if (!myUserId.isNullOrBlank() && senderId != myUserId && !isChatOpen) {
            val conv = db.conversationDao().getConversation(conversationId)
            val senderTitle = conv?.otherUserName?.ifBlank { "FunkyTalk Chat" } ?: "New Message"
            val displayPreview = when {
                plaintext.startsWith("[img]") || plaintext.startsWith("[imgs]") -> {
                    val cap = plaintext.substringAfter("|", "").trim()
                    if (cap.isNotBlank()) "📷 $cap" else "Sent a photo"
                }
                plaintext.startsWith("[voice]") -> "Sent a voice note"
                else -> plaintext
            }
            com.example.util.SystemNotificationHelper.showSystemNotification(
                context = context,
                title = senderTitle,
                message = displayPreview,
                partnerUserId = senderId,
                conversationId = conversationId,
                senderId = senderId,
                kind = "dm",
                notificationId = com.example.service.PushNotificationManager.notificationIdForChat(
                    conversationId = conversationId,
                    fallbackPartnerId = senderId
                ),
                channelId = com.example.util.SystemNotificationHelper.CHANNEL_MESSAGES_ID
            )
        }
    }

    private suspend fun deleteMessageFromServer(context: Context, token: String, messageId: String) {
        try {
            val authToken = if (token.isNotBlank() && !token.contains(com.example.BuildConfig.SUPABASE_ANON_KEY)) {
                token
            } else {
                SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: (SupabaseSessionManager.getAccessToken(context)?.let { "Bearer $it" })
                    ?: token
            }
            val resp = SupabaseClient.api.deleteDmMessage(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = authToken,
                idFilter = "eq.$messageId"
            )
            if (resp.isSuccessful) {
                Log.d(TAG, "Deleted processed message $messageId from Supabase mailbox")
            } else {
                Log.w(TAG, "Failed to delete message $messageId: code=${resp.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting message $messageId from server: ${e.message}")
        }
    }

    private suspend fun getRecipientPublicKeyCached(
        context: Context,
        token: String,
        resolvedUserId: String,
        displayName: String
    ): Result<Pair<String, Int>> {
        val now = System.currentTimeMillis()
        val cached = publicKeyCache[resolvedUserId]
        if (cached != null && (now - cached.fetchedAt) < KEY_CACHE_TTL_MS) {
            return Result.success(cached.publicKey to cached.keyVersion)
        }
        val keyResp = SupabaseClient.api.getUserPublicKey(
            apiKey = BuildConfig.SUPABASE_ANON_KEY,
            auth = token,
            userIdFilter = "eq.$resolvedUserId"
        )
        if (!keyResp.isSuccessful) {
            return Result.failure(IOException("Public key lookup HTTP ${keyResp.code()}"))
        }
        val keyItem = keyResp.body()?.firstOrNull()
        val pubKey = keyItem?.identity_public_key
        if (pubKey.isNullOrBlank()) {
            return Result.failure(RecipientNotReadyException(displayName))
        }
        publicKeyCache[resolvedUserId] = CachedPublicKey(pubKey, keyItem.key_version, now)
        return Result.success(pubKey to keyItem.key_version)
    }

    private suspend fun resolveOrConfirmServerConversationId(
        context: Context,
        db: EncryptedChatDatabase,
        token: String,
        localConversationId: String,
        resolvedUserId: String
    ): Result<String> {
        if (verifiedServerConvIds.contains(localConversationId)) {
            return Result.success(localConversationId)
        }
        // Only treat an existing Room conversation ID as a server ID if it wasn't created offline.
        if (!offlineTempConvIds.contains(localConversationId) && SupabaseSessionManager.isValidUuid(localConversationId)) {
            // Confirm once via RPC if not yet verified in this session to handle offline-created DB rows across restarts
            val rpcResp = SupabaseClient.api.getOrCreateDmConversation(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GetOrCreateDmConversationRequest(p_other_user_id = resolvedUserId)
            )
            if (!rpcResp.isSuccessful) {
                val errBody = runCatching { rpcResp.errorBody()?.string().orEmpty() }.getOrDefault("")
                if (BlockRepository.isUserBlockedError(errBody)) {
                    return Result.failure(UserBlockedException())
                }
                return Result.failure(IOException("Could not establish conversation HTTP ${rpcResp.code()}"))
            }
            val serverConvId = parseUuidResponse(rpcResp.body()?.string()?.trim().orEmpty())
                ?: return Result.failure(IllegalStateException("Invalid conversation ID returned"))
            verifiedServerConvIds.add(serverConvId)
            if (serverConvId != localConversationId) {
                migrateTempConversation(db, localConversationId, serverConvId)
            }
            return Result.success(serverConvId)
        }

        val rpcResp = SupabaseClient.api.getOrCreateDmConversation(
            apiKey = BuildConfig.SUPABASE_ANON_KEY,
            auth = token,
            body = GetOrCreateDmConversationRequest(p_other_user_id = resolvedUserId)
        )
        if (!rpcResp.isSuccessful) {
            val errBody = runCatching { rpcResp.errorBody()?.string().orEmpty() }.getOrDefault("")
            if (BlockRepository.isUserBlockedError(errBody)) {
                return Result.failure(UserBlockedException())
            }
            return Result.failure(IOException("Could not establish conversation HTTP ${rpcResp.code()}"))
        }
        val serverConvId = parseUuidResponse(rpcResp.body()?.string()?.trim().orEmpty())
            ?: return Result.failure(IllegalStateException("Invalid conversation ID returned"))
        verifiedServerConvIds.add(serverConvId)
        offlineTempConvIds.remove(localConversationId)
        if (serverConvId != localConversationId) {
            migrateTempConversation(db, localConversationId, serverConvId)
        }
        return Result.success(serverConvId)
    }

    private suspend fun migrateTempConversation(
        db: EncryptedChatDatabase,
        oldConvId: String,
        newConvId: String
    ) {
        db.messageDao().migrateConversationId(
            oldConvId = oldConvId,
            newConvId = newConvId,
            conversationDao = db.conversationDao(),
            reactionDao = db.dmMessageReactionDao()
        )
        _optimisticMessages.update { list ->
            list.map { if (it.conversationId == oldConvId) it.copy(conversationId = newConvId) else it }
        }
        _conversationIdAliases.update { it + (oldConvId to newConvId) }
    }

    private fun isPermanentSendFailure(error: Throwable): Boolean {
        if (error is UserBlockedException || error is RecipientNotReadyException) return true
        val msg = error.message.orEmpty()
        return msg.contains("HTTP 400") ||
            msg.contains("HTTP 401") ||
            msg.contains("HTTP 403") ||
            msg.contains("HTTP 404") ||
            msg.contains("HTTP 422") ||
            msg.contains("code 400") ||
            msg.contains("code 401") ||
            msg.contains("code 403") ||
            msg.contains("code 404") ||
            msg.contains("code 422") ||
            msg.contains("locked", ignoreCase = true) ||
            msg.contains("not authenticated", ignoreCase = true)
    }

    private fun friendlyFailureReason(error: Throwable): String = when (error) {
        is UserBlockedException -> "You can't send messages to this user"
        is RecipientNotReadyException -> error.message ?: "Recipient encryption keys not ready"
        is IOException -> "No internet connection"
        else -> {
            val raw = error.message.orEmpty()
            when {
                raw.contains("HTTP 401") || raw.contains("authenticated", ignoreCase = true) -> "Session expired — please tap Retry"
                raw.contains("HTTP 403") || raw.contains("blocked", ignoreCase = true) -> "Action not permitted"
                raw.contains("Unable to resolve host", ignoreCase = true) ||
                    raw.contains("timeout", ignoreCase = true) ||
                    raw.contains("network", ignoreCase = true) -> "Connection timed out"
                else -> "Failed to send — tap to retry"
            }
        }
    }

    private fun removeOptimisticOverlay(clientId: String) {
        _optimisticMessages.update { list -> list.filterNot { it.id == clientId || it.clientId == clientId } }
    }

    private suspend fun upsertConversationPreview(
        context: Context,
        db: EncryptedChatDatabase,
        conversationId: String,
        recipientUserId: String,
        recipientName: String,
        recipientAvatar: String?,
        previewText: String,
        messageType: String,
        now: Long,
        fetchProfileIfMissing: Boolean = true
    ) {
        val existingConv = db.conversationDao().getConversation(conversationId)
        val cachedProfile = senderProfileCache[recipientUserId]
        val partnerAvatar = recipientAvatar?.takeIf { it.isNotBlank() }
            ?: existingConv?.otherUserAvatar?.takeIf { it.isNotBlank() }
            ?: cachedProfile?.second
        val partnerFlag = existingConv?.otherUserFlag?.takeIf { it.isNotBlank() }
            ?: cachedProfile?.third
        val updatedConv = if (existingConv != null) {
            existingConv.copy(
                lastMessageText = previewText,
                lastMessageType = messageType,
                lastMessageTime = maxOf(existingConv.lastMessageTime, now),
                otherUserName = recipientName.ifBlank { existingConv.otherUserName },
                otherUserAvatar = partnerAvatar,
                otherUserFlag = partnerFlag ?: existingConv.otherUserFlag
            )
        } else {
            LocalDmConversation(
                id = conversationId,
                otherUserId = recipientUserId,
                otherUserName = recipientName.ifBlank { "FunkyTalk User" },
                otherUserAvatar = partnerAvatar,
                otherUserFlag = partnerFlag,
                lastMessageText = previewText,
                lastMessageType = messageType,
                lastMessageTime = now,
                unreadCount = 0,
                createdAt = now
            )
        }
        db.conversationDao().insertOrUpdate(updatedConv)
        if (fetchProfileIfMissing && (partnerFlag.isNullOrBlank() || partnerAvatar.isNullOrBlank())) {
            scope.launch(Dispatchers.IO) {
                val info = fetchSenderProfileInfo(context, recipientUserId)
                senderProfileCache[recipientUserId] = info
                val curr = db.conversationDao().getConversation(conversationId)
                if (curr != null) {
                    db.conversationDao().updateLastMessageAndProfile(
                        conversationId = conversationId,
                        text = curr.lastMessageText,
                        time = curr.lastMessageTime,
                        unreadDelta = 0,
                        name = info.first.takeIf { it != "FunkyTalk User" },
                        avatar = curr.otherUserAvatar ?: info.second,
                        flag = curr.otherUserFlag ?: info.third
                    )
                }
            }
        }
    }

    /**
     * Optimistic instant enqueue for 1:1 DM text messages.
     * Immediately updates the in-memory optimistic list synchronously for same-frame UI display,
     * persists to Room as SENDING, and dispatches background encryption + network delivery with retries.
     */
    fun enqueueSendMessage(
        context: Context,
        conversationId: String,
        recipientUserId: String,
        recipientName: String,
        recipientAvatar: String?,
        text: String,
        replyToText: String? = null,
        replyToSender: String? = null,
        tapElapsedMs: Long = ChatTimingTracker.nowMs()
    ): String {
        val appContext = context.applicationContext
        val clientId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val myUserId = SupabaseSessionManager.getUserId(appContext)
            ?: ActiveUserManager.getActiveUid(appContext)
            .orEmpty()
        ChatTimingTracker.markTap(clientId, tapElapsedMs)

        val optimisticMsg = LocalDmMessage(
            id = clientId,
            conversationId = conversationId,
            senderId = myUserId,
            recipientId = recipientUserId,
            text = text,
            messageType = "text",
            timestamp = now,
            isOutgoing = true,
            replyToText = replyToText,
            replyToSender = replyToSender,
            clientId = clientId,
            status = LocalMessageStatus.SENDING
        )

        // Synchronously publish into optimistic flow so UI renders in the exact same Compose frame
        _optimisticMessages.update { it + optimisticMsg }

        scope.launch(Dispatchers.IO) {
            val db = database ?: run {
                _optimisticMessages.update { list ->
                    list.map { if (it.id == clientId) it.copy(status = LocalMessageStatus.FAILED, failReason = "Chat vault is locked") else it }
                }
                return@launch
            }
            db.messageDao().insertMessage(optimisticMsg)
            removeOptimisticOverlay(clientId)
            upsertConversationPreview(
                context = appContext,
                db = db,
                conversationId = conversationId,
                recipientUserId = recipientUserId,
                recipientName = recipientName,
                recipientAvatar = recipientAvatar,
                previewText = text,
                messageType = "text",
                now = now
            )
            dispatchDmOutboxMessage(
                context = appContext,
                initialMessageId = clientId,
                recipientNameHint = recipientName,
                recipientAvatarHint = recipientAvatar,
                allowAutoRetry = true
            )
        }
        return clientId
    }

    /**
     * Optimistic instant enqueue for 1:1 DM encrypted media (image or voice note).
     * Writes the local decrypted media file + SENDING Room row immediately, then encrypts + uploads + sends in background.
     */
    fun enqueueSendMediaMessage(
        context: Context,
        conversationId: String,
        recipientUserId: String,
        recipientName: String,
        recipientAvatar: String?,
        rawBytes: ByteArray,
        messageType: String,
        mimeType: String,
        durationSeconds: Int? = null,
        replyToText: String? = null,
        replyToSender: String? = null,
        caption: String? = null,
        tapElapsedMs: Long = ChatTimingTracker.nowMs()
    ): String {
        val appContext = context.applicationContext
        val clientId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val myUserId = SupabaseSessionManager.getUserId(appContext)
            ?: ActiveUserManager.getActiveUid(appContext)
            .orEmpty()
        ChatTimingTracker.markTap(clientId, tapElapsedMs)

        val cleanCaption = caption?.replace("\n", " ")?.replace("|", " ")?.trim()?.takeIf { it.isNotBlank() }
        val localFile = saveLocalMediaFile(appContext, clientId, rawBytes, mimeType)
        val localText = if (messageType == "image") {
            if (cleanCaption != null) "[img]file://${localFile.absolutePath}|$cleanCaption" else "[img]file://${localFile.absolutePath}"
        } else {
            "[voice]file://${localFile.absolutePath}|${durationSeconds ?: 0}"
        }
        val previewText = if (messageType == "image") {
            if (cleanCaption != null) "📷 $cleanCaption" else "📷 Photo"
        } else {
            "🎙️ Voice message"
        }

        val optimisticMsg = LocalDmMessage(
            id = clientId,
            conversationId = conversationId,
            senderId = myUserId,
            recipientId = recipientUserId,
            text = localText,
            messageType = messageType,
            timestamp = now,
            isOutgoing = true,
            replyToText = replyToText,
            replyToSender = replyToSender,
            clientId = clientId,
            status = LocalMessageStatus.SENDING,
            localMediaPath = localFile.absolutePath,
            mediaMimeType = mimeType,
            mediaDurationSec = durationSeconds,
            mediaCaption = cleanCaption
        )

        _optimisticMessages.update { it + optimisticMsg }

        scope.launch(Dispatchers.IO) {
            val db = database ?: run {
                _optimisticMessages.update { list ->
                    list.map { if (it.id == clientId) it.copy(status = LocalMessageStatus.FAILED, failReason = "Chat vault is locked") else it }
                }
                return@launch
            }
            db.messageDao().insertMessage(optimisticMsg)
            removeOptimisticOverlay(clientId)
            upsertConversationPreview(
                context = appContext,
                db = db,
                conversationId = conversationId,
                recipientUserId = recipientUserId,
                recipientName = recipientName,
                recipientAvatar = recipientAvatar,
                previewText = previewText,
                messageType = messageType,
                now = now
            )
            dispatchDmOutboxMessage(
                context = appContext,
                initialMessageId = clientId,
                recipientNameHint = recipientName,
                recipientAvatarHint = recipientAvatar,
                allowAutoRetry = true
            )
        }
        return clientId
    }

    fun retryDmMessage(context: Context, messageId: String) {
        val appContext = context.applicationContext
        scope.launch(Dispatchers.IO) {
            val db = database ?: return@launch
            val existing = db.messageDao().getMessage(messageId) ?: return@launch
            db.messageDao().updateMessageStatus(
                id = messageId,
                status = LocalMessageStatus.SENDING,
                failReason = null,
                retryCount = existing.retryCount,
                nextRetryAt = 0L
            )
            dispatchDmOutboxMessage(
                context = appContext,
                initialMessageId = messageId,
                recipientNameHint = null,
                recipientAvatarHint = null,
                allowAutoRetry = true
            )
        }
    }

    suspend fun deleteDmMessage(context: Context, messageId: String) = withContext(Dispatchers.IO) {
        removeOptimisticOverlay(messageId)
        val db = database ?: return@withContext
        val existing = db.messageDao().getMessage(messageId)
        if (existing != null) {
            db.messageDao().deleteMessageAndRefreshConversation(
                messageId = messageId,
                conversationId = existing.conversationId,
                conversationDao = db.conversationDao()
            )
        } else {
            db.messageDao().deleteMessage(messageId)
        }
    }

    suspend fun resumePendingOutbox(context: Context, fromColdStart: Boolean = false) = withContext(Dispatchers.IO) {
        outboxResumeMutex.withLock {
            val db = database ?: return@withLock
            val pending = db.messageDao().getPendingOutgoingMessages()
            if (pending.isEmpty()) return@withLock

            val online = AppNetworkManager.checkConnection(context)
            if (!online) {
                if (fromColdStart) {
                    pending.filter { it.status == LocalMessageStatus.SENDING }.forEach { msg ->
                        db.messageDao().updateMessageStatus(
                            id = msg.id,
                            status = LocalMessageStatus.FAILED,
                            failReason = "No internet connection",
                            retryCount = msg.retryCount,
                            nextRetryAt = 0L
                        )
                    }
                }
                return@withLock
            }

            for (msg in pending) {
                if (activeSendClientIds.contains(msg.id)) continue
                val conv = db.conversationDao().getConversation(msg.conversationId)
                db.messageDao().updateMessageStatus(
                    id = msg.id,
                    status = LocalMessageStatus.SENDING,
                    failReason = null,
                    retryCount = msg.retryCount,
                    nextRetryAt = 0L
                )
                dispatchDmOutboxMessage(
                    context = context.applicationContext,
                    initialMessageId = msg.id,
                    recipientNameHint = conv?.otherUserName,
                    recipientAvatarHint = conv?.otherUserAvatar,
                    allowAutoRetry = false
                )
            }
        }
    }

    private suspend fun dispatchDmOutboxMessage(
        context: Context,
        initialMessageId: String,
        recipientNameHint: String?,
        recipientAvatarHint: String?,
        allowAutoRetry: Boolean
    ): Result<LocalDmMessage> {
        val db = database ?: return Result.failure(IllegalStateException("Chat vault is locked"))
        val initialRow = db.messageDao().getMessage(initialMessageId)
            ?: return Result.failure(IllegalStateException("Message deleted"))

        if (!activeSendClientIds.add(initialRow.id)) {
            return Result.success(initialRow)
        }
        val mutexKey = initialRow.recipientId.ifBlank { initialRow.conversationId }
        val mutex = conversationSendMutexes.getOrPut(mutexKey) { Mutex() }

        return try {
            mutex.withLock {
                var attempt = 0
                val maxAttempts = if (allowAutoRetry) MAX_AUTO_RETRIES else 1
                var lastErr: Throwable = IOException("Failed to send")

                while (attempt < maxAttempts) {
                    val currentRow = db.messageDao().getMessage(initialMessageId)
                        ?: return@withLock Result.failure(IllegalStateException("Message deleted"))
                    if (currentRow.status == LocalMessageStatus.SENT) {
                        return@withLock Result.success(currentRow)
                    }

                    if (!AppNetworkManager.checkConnection(context)) {
                        db.messageDao().updateMessageStatus(
                            id = currentRow.id,
                            status = LocalMessageStatus.FAILED,
                            failReason = "No internet connection",
                            retryCount = currentRow.retryCount,
                            nextRetryAt = 0L
                        )
                        return@withLock Result.failure(IOException("No internet connection"))
                    }

                    val result = executeDmSendAttempt(context, db, currentRow, recipientNameHint)
                    if (result.isSuccess) {
                        val sentMsg = result.getOrThrow()
                        db.messageDao().updateMessageStatus(
                            id = sentMsg.id,
                            status = LocalMessageStatus.SENT,
                            failReason = null,
                            retryCount = currentRow.retryCount,
                            nextRetryAt = 0L
                        )
                        return@withLock Result.success(sentMsg.copy(status = LocalMessageStatus.SENT, failReason = null))
                    }

                    val err = result.exceptionOrNull() ?: IOException("Unknown error")
                    lastErr = err
                    if (isPermanentSendFailure(err)) {
                        db.messageDao().updateMessageStatus(
                            id = currentRow.id,
                            status = LocalMessageStatus.FAILED,
                            failReason = friendlyFailureReason(err),
                            retryCount = currentRow.retryCount + 1,
                            nextRetryAt = 0L
                        )
                        return@withLock Result.failure(err)
                    }

                    attempt++
                    val nextRetryCount = currentRow.retryCount + 1
                    val backoffMs = minOf(15_000L, 1200L * (1 shl (attempt - 1).coerceAtMost(3)))
                    if (attempt < maxAttempts) {
                        db.messageDao().updateMessageStatus(
                            id = currentRow.id,
                            status = LocalMessageStatus.SENDING,
                            failReason = null,
                            retryCount = nextRetryCount,
                            nextRetryAt = System.currentTimeMillis() + backoffMs
                        )
                        delay(backoffMs)
                    } else {
                        db.messageDao().updateMessageStatus(
                            id = currentRow.id,
                            status = LocalMessageStatus.FAILED,
                            failReason = friendlyFailureReason(err),
                            retryCount = nextRetryCount,
                            nextRetryAt = 0L
                        )
                    }
                }
                Result.failure(lastErr)
            }
        } finally {
            activeSendClientIds.remove(initialMessageId)
        }
    }

    private suspend fun executeDmSendAttempt(
        context: Context,
        db: EncryptedChatDatabase,
        msg: LocalDmMessage,
        recipientNameHint: String?
    ): Result<LocalDmMessage> {
        return try {
            val myUserId = SupabaseSessionManager.ensureActiveSession(context)
                ?: SupabaseSessionManager.getUserId(context)
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: (SupabaseSessionManager.getAccessToken(context)?.let { "Bearer $it" })
                ?: SupabaseSessionManager.getBearerToken(context)
            if (token.isBlank()) {
                return Result.failure(IllegalStateException("User not authenticated"))
            }

            val displayName = recipientNameHint?.ifBlank { null }
                ?: db.conversationDao().getConversation(msg.conversationId)?.otherUserName?.ifBlank { null }
                ?: "User"

            if (msg.recipientId.isBlank() || msg.recipientId.startsWith("partner_mock")) {
                return Result.failure(RecipientNotReadyException(displayName))
            }

            val resolvedUserId = resolveRecipientAuthUserId(context, msg.recipientId) ?: msg.recipientId
            val keyPair = getRecipientPublicKeyCached(context, token, resolvedUserId, displayName).getOrElse {
                return Result.failure(it)
            }
            val (recipientPublicKey, keyVersion) = keyPair

            val serverConvId = resolveOrConfirmServerConversationId(
                context = context,
                db = db,
                token = token,
                localConversationId = msg.conversationId,
                resolvedUserId = resolvedUserId
            ).getOrElse { return Result.failure(it) }

            val clientId = msg.clientId ?: msg.id
            val isMedia = msg.messageType == "image" || msg.messageType == "voice"

            var storagePathForRequest: String? = null
            val encryptStartMs = ChatTimingTracker.nowMs()
            val ciphertext = if (isMedia) {
                val mediaPath = msg.localMediaPath
                    ?: msg.text.removePrefix("[img]").removePrefix("[voice]")
                        .substringBefore("|")
                        .removePrefix("file://")
                val localFile = File(mediaPath)
                if (!localFile.exists()) {
                    return Result.failure(IllegalStateException("Local media file missing"))
                }
                val rawBytes = withContext(Dispatchers.IO) { localFile.readBytes() }
                val mimeType = msg.mediaMimeType
                    ?: if (msg.messageType == "voice") "audio/aac" else "image/jpeg"

                val (encryptedMediaBytes, keyBytes, nonceBytes) = withContext(Dispatchers.Default) {
                    E2EEMediaManager.encryptMediaPayload(rawBytes)
                }
                val objectPath = "$serverConvId/$clientId.bin"
                val reqBody = encryptedMediaBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                val uploadRes = SupabaseClient.api.uploadDmMedia(
                    auth = token,
                    path = objectPath,
                    body = reqBody
                )
                if (!uploadRes.isSuccessful && uploadRes.code() != 409) {
                    return Result.failure(IOException("Storage upload HTTP ${uploadRes.code()}"))
                }
                storagePathForRequest = objectPath

                val keyBase64 = Base64.encodeToString(keyBytes, Base64.NO_WRAP)
                val nonceBase64 = Base64.encodeToString(nonceBytes, Base64.NO_WRAP)
                val descriptorJson = JSONObject().apply {
                    put("storage_path", objectPath)
                    put("media_key", keyBase64)
                    put("media_nonce", nonceBase64)
                    put("mime_type", mimeType)
                    msg.mediaDurationSec?.let { put("duration_seconds", it) }
                    msg.mediaCaption?.takeIf { it.isNotBlank() }?.let { put("caption", it) }
                }.toString()

                E2EEManager.encryptMessageAsync(recipientPublicKey, descriptorJson)
            } else {
                E2EEManager.encryptMessageAsync(recipientPublicKey, msg.text)
            }
            val encryptMs = (ChatTimingTracker.nowMs() - encryptStartMs).coerceAtLeast(0L)
            ChatTimingTracker.logEncrypt("DM", clientId, encryptMs, recipients = 1)

            val sendStartMs = ChatTimingTracker.nowMs()
            val sendResp = SupabaseClient.api.sendDmMessage(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = SendDmMessageRequest(
                    id = clientId,
                    conversation_id = serverConvId,
                    sender_id = myUserId,
                    recipient_id = resolvedUserId,
                    ciphertext = ciphertext,
                    nonce = "",
                    key_version = keyVersion,
                    message_type = msg.messageType,
                    storage_path = storagePathForRequest
                )
            )
            val sendMs = (ChatTimingTracker.nowMs() - sendStartMs).coerceAtLeast(0L)

            if (sendResp.isSuccessful) {
                ChatTimingTracker.logSendRequest("DM", clientId, sendMs, success = true)
                return Result.success(msg.copy(conversationId = serverConvId, status = LocalMessageStatus.SENT, failReason = null))
            }

            val errBody = runCatching { sendResp.errorBody()?.string().orEmpty() }.getOrDefault("")
            // Idempotent PK collision on retry -> earlier POST already delivered to server!
            if (sendResp.code() == 409 || errBody.contains("23505") || errBody.contains("duplicate key", ignoreCase = true)) {
                ChatTimingTracker.logSendRequest("DM", clientId, sendMs, success = true)
                return Result.success(msg.copy(conversationId = serverConvId, status = LocalMessageStatus.SENT, failReason = null))
            }

            ChatTimingTracker.logSendRequest("DM", clientId, sendMs, success = false)
            if (BlockRepository.isUserBlockedError(errBody)) {
                return Result.failure(UserBlockedException())
            }
            if (sendResp.code() == 401 || sendResp.code() == 403) {
                publicKeyCache.remove(resolvedUserId)
            }
            Result.failure(IllegalStateException("Failed to send message: HTTP ${sendResp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends an encrypted 1:1 message to recipient (supports both non-chat-screen callers and direct awaiting callers).
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
                ?: (SupabaseSessionManager.getAccessToken(context)?.let { "Bearer $it" })
                ?: SupabaseSessionManager.getBearerToken(context)

            val displayName = recipientName.ifBlank { "User" }
            if (recipientUserId.isBlank() || recipientUserId.startsWith("partner_mock")) {
                return@withContext Result.failure(RecipientNotReadyException(displayName))
            }

            val resolvedUserId = resolveRecipientAuthUserId(context, recipientUserId) ?: recipientUserId
            val (recipientPublicKey, keyVersion) = getRecipientPublicKeyCached(
                context, token, resolvedUserId, displayName
            ).getOrElse { return@withContext Result.failure(it) }

            val existingConv = db.conversationDao().getConversationByOtherUser(resolvedUserId)
            val localConvId = existingConv?.id ?: run {
                val rpcResp = SupabaseClient.api.getOrCreateDmConversation(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    body = GetOrCreateDmConversationRequest(p_other_user_id = resolvedUserId)
                )
                if (!rpcResp.isSuccessful) {
                    val errBody = runCatching { rpcResp.errorBody()?.string().orEmpty() }.getOrDefault("")
                    if (BlockRepository.isUserBlockedError(errBody)) {
                        return@withContext Result.failure(UserBlockedException())
                    }
                    return@withContext Result.failure(IllegalStateException("Could not establish conversation: ${rpcResp.message()}"))
                }
                val parsed = parseUuidResponse(rpcResp.body()?.string()?.trim().orEmpty())
                    ?: return@withContext Result.failure(IllegalStateException("Failed to get valid conversation ID"))
                verifiedServerConvIds.add(parsed)
                parsed
            }
            val serverConvId = resolveOrConfirmServerConversationId(
                context = context,
                db = db,
                token = token,
                localConversationId = localConvId,
                resolvedUserId = resolvedUserId
            ).getOrElse { return@withContext Result.failure(it) }

            val clientId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val optimistic = LocalDmMessage(
                id = clientId,
                conversationId = serverConvId,
                senderId = myUserId,
                recipientId = resolvedUserId,
                text = text,
                timestamp = now,
                isOutgoing = true,
                replyToText = replyToText,
                replyToSender = replyToSender,
                clientId = clientId,
                status = LocalMessageStatus.SENDING
            )
            db.messageDao().insertMessage(optimistic)
            upsertConversationPreview(
                context = context,
                db = db,
                conversationId = serverConvId,
                recipientUserId = resolvedUserId,
                recipientName = recipientName,
                recipientAvatar = recipientAvatar,
                previewText = text,
                messageType = "text",
                now = now
            )

            val encStart = ChatTimingTracker.nowMs()
            val ciphertext = E2EEManager.encryptMessageAsync(recipientPublicKey, text)
            ChatTimingTracker.logEncrypt("DM", clientId, (ChatTimingTracker.nowMs() - encStart).coerceAtLeast(0L))

            val sendStart = ChatTimingTracker.nowMs()
            val sendResp = SupabaseClient.api.sendDmMessage(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = SendDmMessageRequest(
                    id = clientId,
                    conversation_id = serverConvId,
                    sender_id = myUserId,
                    recipient_id = resolvedUserId,
                    ciphertext = ciphertext,
                    nonce = "",
                    key_version = keyVersion
                )
            )
            val sendMs = (ChatTimingTracker.nowMs() - sendStart).coerceAtLeast(0L)
            ChatTimingTracker.logSendRequest("DM", clientId, sendMs, sendResp.isSuccessful)

            if (!sendResp.isSuccessful) {
                val errBody = runCatching { sendResp.errorBody()?.string().orEmpty() }.getOrDefault("")
                if (BlockRepository.isUserBlockedError(errBody)) {
                    db.messageDao().updateMessageStatus(clientId, LocalMessageStatus.FAILED, "You can't send messages to this user", 1, 0L)
                    return@withContext Result.failure(UserBlockedException())
                }
                db.messageDao().updateMessageStatus(clientId, LocalMessageStatus.FAILED, "Failed to send — tap to retry", 1, 0L)
                return@withContext Result.failure(IllegalStateException("Failed to send message: code ${sendResp.code()}"))
            }

            db.messageDao().updateMessageStatus(clientId, LocalMessageStatus.SENT, null, 0, 0L)
            Result.success(optimistic.copy(status = LocalMessageStatus.SENT))
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }

    /**
     * Sends a 2-layer E2EE encrypted media message (image or voice note).
     */
    suspend fun sendMediaMessage(
        context: Context,
        recipientUserId: String,
        recipientName: String,
        recipientAvatar: String?,
        rawBytes: ByteArray,
        messageType: String, // "image" or "voice"
        mimeType: String, // "image/jpeg", "audio/aac"
        durationSeconds: Int? = null,
        replyToText: String? = null,
        replyToSender: String? = null,
        caption: String? = null
    ): Result<LocalDmMessage> = withContext(Dispatchers.IO) {
        try {
            val db = database ?: return@withContext Result.failure(IllegalStateException("Chat vault is locked"))
            val existingConv = db.conversationDao().getConversationByOtherUser(recipientUserId)
                ?: getOrCreateLocalConversation(context, recipientUserId, recipientName, recipientAvatar)
            val clientId = enqueueSendMediaMessage(
                context = context,
                conversationId = existingConv.id,
                recipientUserId = recipientUserId,
                recipientName = recipientName,
                recipientAvatar = recipientAvatar,
                rawBytes = rawBytes,
                messageType = messageType,
                mimeType = mimeType,
                durationSeconds = durationSeconds,
                replyToText = replyToText,
                replyToSender = replyToSender,
                caption = caption
            )
            val saved = db.messageDao().getMessage(clientId)
                ?: return@withContext Result.failure(IllegalStateException("Queued"))
            Result.success(saved)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending media message", e)
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

    suspend fun markConversationUnread(conversationId: String) = withContext(Dispatchers.IO) {
        try {
            database?.conversationDao()?.updateLastMessage(
                conversationId = conversationId,
                text = database?.conversationDao()?.getConversation(conversationId)?.lastMessageText ?: "",
                time = database?.conversationDao()?.getConversation(conversationId)?.lastMessageTime ?: System.currentTimeMillis(),
                unreadDelta = 1
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error marking conversation unread", e)
        }
    }

    suspend fun deleteConversationLocally(conversationId: String) = withContext(Dispatchers.IO) {
        try {
            database?.messageDao()?.clearConversationMessages(conversationId)
            database?.conversationDao()?.deleteConversation(conversationId)
            Log.d(TAG, "Deleted conversation and messages for: $conversationId")
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting conversation locally", e)
        }
    }

    suspend fun clearConversationMessages(conversationId: String) = clearConversationMessagesLocally(conversationId)

    suspend fun clearConversationMessagesLocally(conversationId: String) = withContext(Dispatchers.IO) {
        try {
            database?.messageDao()?.clearConversationMessages(conversationId)
            database?.conversationDao()?.updateLastMessage(
                conversationId = conversationId,
                text = "Chat history cleared",
                time = System.currentTimeMillis(),
                unreadDelta = 0
            )
            database?.conversationDao()?.markRead(conversationId)
            Log.d(TAG, "Cleared messages for conversation: $conversationId")
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing conversation messages", e)
        }
    }

    suspend fun clearAllChatsLocally() = withContext(Dispatchers.IO) {
        try {
            database?.messageDao()?.deleteAllMessages()
            database?.conversationDao()?.deleteAllConversations()
            Log.d(TAG, "Cleared all local conversations and messages")
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing all chats", e)
        }
    }

    /**
     * Resolves a recipient user ID or username/display name to their exact Supabase auth_user_id UUID.
     */
    suspend fun resolveRecipientAuthUserId(context: Context, inputIdOrName: String): String? = withContext(Dispatchers.IO) {
        if (inputIdOrName.isBlank() || inputIdOrName.startsWith("partner_mock") || inputIdOrName.startsWith("c_")) {
            return@withContext null
        }
        resolvedUserIdCache[inputIdOrName]?.let { return@withContext it }

        // Fast path: if already a valid UUID, treat directly as auth_user_id (key fetch will verify registration and cache it)
        if (SupabaseSessionManager.isValidUuid(inputIdOrName)) {
            resolvedUserIdCache[inputIdOrName] = inputIdOrName
            return@withContext inputIdOrName
        }

        // 1. Ensure active session and token
        SupabaseSessionManager.ensureActiveSession(context)
        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            ?: SupabaseSessionManager.getBearerToken(context)

        // 2. Look up profile by auth_user_id or profile_name / username / email
        try {
            val profileRes = SupabaseClient.api.getProfile(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                authUserIdEq = "eq.$inputIdOrName"
            )
            if (profileRes.isSuccessful && !profileRes.body().isNullOrEmpty()) {
                val p = profileRes.body()!!.first()
                if (!p.auth_user_id.isNullOrBlank()) {
                    resolvedUserIdCache[inputIdOrName] = p.auth_user_id
                    return@withContext p.auth_user_id
                }
            }
        } catch (_: Exception) {}

        try {
            val allProfilesRes = SupabaseClient.api.getAllProfiles(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
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
                    resolvedUserIdCache[inputIdOrName] = match.auth_user_id
                    return@withContext match.auth_user_id
                }
            }
        } catch (_: Exception) {}

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
        var isServerVerified = false
        var blockedByRpc = false
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: SupabaseSessionManager.getBearerToken(context)
            val rpcResp = SupabaseClient.api.getOrCreateDmConversation(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GetOrCreateDmConversationRequest(p_other_user_id = resolvedUserId)
            )
            if (rpcResp.isSuccessful) {
                val raw = rpcResp.body()?.string()?.trim() ?: ""
                val parsed = parseUuidResponse(raw)
                if (!parsed.isNullOrBlank()) {
                    convId = parsed
                    isServerVerified = true
                }
            } else {
                val errBody = runCatching { rpcResp.errorBody()?.string().orEmpty() }.getOrDefault("")
                if (BlockRepository.isUserBlockedError(errBody)) {
                    blockedByRpc = true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch RPC conversation ID, using temporary UUID", e)
        }
        if (blockedByRpc) {
            throw UserBlockedException()
        }
        if (isServerVerified) {
            verifiedServerConvIds.add(convId)
        } else {
            offlineTempConvIds.add(convId)
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

            Log.d(TAG, "checkRecipientKeyStatus response: code=${resp.code()}, hasKey=${!resp.body().isNullOrEmpty()}")

            if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                val key = resp.body()!!.first().identity_public_key
                !key.isNullOrBlank()
            } else {
                false
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
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
        return fetchSenderProfileInfo(context, senderId).first
    }

    private suspend fun fetchSenderProfileInfo(context: Context, senderId: String): Triple<String, String?, String?> {
        return try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: SupabaseSessionManager.getBearerToken(context)
            val resp = SupabaseClient.api.getProfile(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                authUserIdEq = "eq.$senderId"
            )
            if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                val profile = resp.body()!!.first()
                val name = profile.profile_name?.takeIf { it.isNotBlank() }
                    ?: profile.username?.takeIf { it.isNotBlank() }
                    ?: "FunkyTalk User"
                val avatar = profile.avatar_url?.takeIf { it.isNotBlank() }
                val flag = com.example.ui.screens.home.tabs.resolveCountryCode(profile.country, profile.country_code)
                    .takeIf { it.isNotBlank() }
                Triple(name, avatar, flag)
            } else {
                Triple("FunkyTalk User", null, null)
            }
        } catch (e: Exception) {
            Triple("FunkyTalk User", null, null)
        }
    }

    suspend fun sendDmReaction(
        context: Context,
        conversationId: String,
        recipientUserId: String,
        targetMessageId: String,
        emoji: String,
        op: String = "set" // "set" or "remove"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = database ?: return@withContext Result.failure(IllegalStateException("Chat vault is locked"))
            val myUserId = SupabaseSessionManager.ensureActiveSession(context)
                ?: SupabaseSessionManager.getUserId(context)
                ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: (SupabaseSessionManager.getAccessToken(context)?.let { "Bearer $it" })
                ?: SupabaseSessionManager.getBearerToken(context)

            val resolvedUserId = resolveRecipientAuthUserId(context, recipientUserId) ?: recipientUserId
            val ts = System.currentTimeMillis()

            // Update local DB reaction immediately
            if (op == "remove") {
                db.dmMessageReactionDao().deleteReaction(targetMessageId, myUserId)
            } else {
                db.dmMessageReactionDao().insertReaction(
                    com.example.db.LocalDmMessageReaction(
                        conversationId = conversationId,
                        targetMessageId = targetMessageId,
                        senderId = myUserId,
                        emoji = emoji,
                        ts = ts
                    )
                )
            }

            // If partner is dummy or invalid, stop after local save
            if (resolvedUserId.isBlank() || resolvedUserId.startsWith("partner_mock")) {
                return@withContext Result.success(Unit)
            }

            // Fetch recipient public key
            val keyResp = SupabaseClient.api.getUserPublicKey(
                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                userIdFilter = "eq.$resolvedUserId"
            )
            if (!keyResp.isSuccessful || keyResp.body().isNullOrEmpty()) {
                Log.w(TAG, "Recipient $recipientUserId has no public key registered for reaction.")
                return@withContext Result.success(Unit)
            }
            val keyItem = keyResp.body()!!.first()
            val recipientPublicKey = keyItem.identity_public_key ?: return@withContext Result.success(Unit)

            // Construct reaction payload JSON
            val payloadObj = JSONObject().apply {
                put("t", "reaction")
                put("target", targetMessageId)
                put("emoji", emoji)
                put("op", op)
                put("ts", ts)
            }
            val ciphertext = E2EEManager.encryptMessage(recipientPublicKey, payloadObj.toString())

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
                if (sendResp.code() == 429) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Slow down", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                Log.w(TAG, "sendDmReaction: failed with code ${sendResp.code()}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "sendDmReaction error", e)
            Result.failure(e)
        }
    }

    fun getDmReactionsFlow(conversationId: String): Flow<List<com.example.db.LocalDmMessageReaction>> {
        val db = database ?: return emptyFlow()
        return db.dmMessageReactionDao().getReactionsForConversation(conversationId)
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
