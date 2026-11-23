package com.example.crypto

import android.content.Context
import android.util.Log
import com.example.db.EncryptedChatDatabase
import com.example.db.LocalDmConversation
import com.example.db.LocalDmMessage
import com.example.db.LocalDmMessageReaction
import com.example.db.LocalGroupConversation
import com.example.db.LocalGroupMessage
import com.example.db.LocalMessageReaction
import com.example.util.ChatBackupPreferencesManager
import com.example.util.DriveRestApiClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

data class DriveBackupInfo(
    val hasBackup: Boolean,
    val dateString: String = "",
    val sizeString: String = "",
    val sizeBytes: Long = 0L,
    val headerJson: String? = null,
    val uid: String? = null
)

data class BackupChunkMetadata(
    val id: String, // e.g. "c_12345.enc" or "m_abcde.enc"
    val type: String, // "messages" or "media"
    val conversationOrGroupId: String,
    val messageIdRange: String,
    val sha256: String,
    val sizeBytes: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type)
        put("conv_id", conversationOrGroupId)
        put("range", messageIdRange)
        put("sha256", sha256)
        put("size", sizeBytes)
    }

    companion object {
        fun fromJson(obj: JSONObject): BackupChunkMetadata = BackupChunkMetadata(
            id = obj.getString("id"),
            type = obj.getString("type"),
            conversationOrGroupId = obj.optString("conv_id", ""),
            messageIdRange = obj.optString("range", ""),
            sha256 = obj.optString("sha256", ""),
            sizeBytes = obj.optLong("size", 0L)
        )
    }
}

data class BackupManifest(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val chunks: List<BackupChunkMetadata> = emptyList()
) {
    fun toJson(): String = JSONObject().apply {
        put("version", version)
        put("timestamp", timestamp)
        val arr = JSONArray()
        chunks.forEach { arr.put(it.toJson()) }
        put("chunks", arr)
    }.toString()

    companion object {
        fun fromJson(jsonStr: String): BackupManifest {
            val obj = JSONObject(jsonStr)
            val arr = obj.optJSONArray("chunks") ?: JSONArray()
            val list = mutableListOf<BackupChunkMetadata>()
            for (i in 0 until arr.length()) {
                list.add(BackupChunkMetadata.fromJson(arr.getJSONObject(i)))
            }
            return BackupManifest(
                version = obj.optInt("version", 1),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                chunks = list
            )
        }
    }
}

object DriveBackupEngine {
    private const val TAG = "DriveBackupEngine"
    private const val HEADER_FILE_NAME = "ft_backup_header.json"
    private const val MANIFEST_FILE_NAME = "ft_manifest.enc"
    private const val CHUNK_MAX_MESSAGES = 1000

    suspend fun performBackup(
        context: Context,
        onProgress: ((percent: Int, text: String) -> Unit)? = null,
        isCancelled: () -> Boolean = { false }
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val accountEmail = ChatBackupPreferencesManager.getDriveAccountName(context)
                ?: throw IllegalStateException("No Google Account selected for Drive Backup.")

            val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw IllegalStateException("User not logged in.")

            val activeVaultKey = VaultSecurityManager.getVaultKeyForBackgroundBackup(context)
                ?: throw IllegalStateException("Vault is locked. Password required.")

            onProgress?.invoke(5, "Checking Google Drive storage...")

            val existingDriveFiles = DriveRestApiClient.listAppDataFiles(context, accountEmail)
            val headerFile = existingDriveFiles.find { it.name == HEADER_FILE_NAME }
                ?: throw IllegalStateException("Backup header not found on Google Drive. Please set up your backup password.")

            val masterKey = BackupCrypto.loadMasterKeyFromKeystore(context)
                ?: throw IllegalStateException("Backup master key is missing. Please set up your backup password.")

            if (isCancelled()) return@withContext false
            onProgress?.invoke(15, "Reading local chat database...")

            val db = EncryptedChatDatabase.getInstance(context, activeVaultKey)
            val dmConversations = db.conversationDao().getAllConversationsList()
            val groupConversations = db.groupConversationDao().getAllJoinedGroupsList()

            val totalChats = dmConversations.size + groupConversations.size
            if (totalChats == 0) {
                onProgress?.invoke(100, "No chats to back up.")
                return@withContext true
            }

            val includeMedia = ChatBackupPreferencesManager.getIncludeMedia(context)
            val currentTime = System.currentTimeMillis()

            val newChunks = mutableListOf<BackupChunkMetadata>()
            var processedChats = 0

            // 1. Process 1:1 Conversations and Messages
            for (conv in dmConversations) {
                if (isCancelled()) return@withContext false
                processedChats++
                onProgress?.invoke(
                    15 + ((processedChats.toFloat() / totalChats) * 50).toInt(),
                    "Backing up chat $processedChats of $totalChats"
                )

                val messages = db.messageDao().getMessagesList(conv.id)
                val reactions = db.dmMessageReactionDao().getReactionsForConversationList(conv.id)

                if (messages.isEmpty()) continue

                val messageBatches = messages.chunked(CHUNK_MAX_MESSAGES)
                for (batch in messageBatches) {
                    val ndjsonBuilder = StringBuilder()

                    val convJson = JSONObject().apply {
                        put("type", "dm_conv")
                        put("id", conv.id)
                        put("other_user_id", conv.otherUserId)
                        put("other_user_name", conv.otherUserName)
                        put("other_user_avatar", conv.otherUserAvatar ?: "")
                        put("last_msg_text", conv.lastMessageText)
                        put("last_msg_time", conv.lastMessageTime)
                    }
                    ndjsonBuilder.append(convJson.toString()).append("\n")

                    batch.forEach { msg ->
                        val msgJson = JSONObject().apply {
                            put("type", "dm_msg")
                            put("id", msg.id)
                            put("conv_id", msg.conversationId)
                            put("sender_id", msg.senderId)
                            put("recipient_id", msg.recipientId)
                            put("text", msg.text)
                            put("msg_type", msg.messageType)
                            put("timestamp", msg.timestamp)
                            put("is_outgoing", msg.isOutgoing)
                            put("reply_text", msg.replyToText ?: "")
                            put("reply_sender", msg.replyToSender ?: "")
                        }
                        ndjsonBuilder.append(msgJson.toString()).append("\n")
                    }

                    val batchMsgIds = batch.map { it.id }.toSet()
                    reactions.filter { it.targetMessageId in batchMsgIds }.forEach { r ->
                        val rJson = JSONObject().apply {
                            put("type", "dm_reaction")
                            put("conv_id", r.conversationId)
                            put("target_id", r.targetMessageId)
                            put("sender_id", r.senderId)
                            put("emoji", r.emoji)
                            put("ts", r.ts)
                        }
                        ndjsonBuilder.append(rJson.toString()).append("\n")
                    }

                    val gzipped = gzipCompress(ndjsonBuilder.toString().toByteArray(Charsets.UTF_8))
                    val chunkId = "c_${UUID.randomUUID()}.enc"
                    val encryptedChunk = BackupCrypto.encryptChunk(gzipped, chunkId, masterKey)
                    val sha256Hex = calculateSha256(encryptedChunk)

                    DriveRestApiClient.uploadSmallFile(
                        context = context,
                        accountEmail = accountEmail,
                        name = chunkId,
                        mimeType = "application/octet-stream",
                        content = encryptedChunk
                    )

                    val rangeStr = "${batch.first().timestamp}-${batch.last().timestamp}"
                    newChunks.add(
                        BackupChunkMetadata(
                            id = chunkId,
                            type = "messages",
                            conversationOrGroupId = conv.id,
                            messageIdRange = rangeStr,
                            sha256 = sha256Hex,
                            sizeBytes = encryptedChunk.size.toLong()
                        )
                    )
                }
            }

            // 2. Process Group Conversations and Messages
            for (group in groupConversations) {
                if (isCancelled()) return@withContext false
                processedChats++
                onProgress?.invoke(
                    15 + ((processedChats.toFloat() / totalChats) * 50).toInt(),
                    "Backing up group $processedChats of $totalChats"
                )

                val validGroupMsgs = db.groupMessageDao().getGroupMessagesList(group.id, currentTime)
                val groupReactions = db.messageReactionDao().getReactionsForGroupList(group.id)

                if (validGroupMsgs.isEmpty()) continue

                val messageBatches = validGroupMsgs.chunked(CHUNK_MAX_MESSAGES)
                for (batch in messageBatches) {
                    val ndjsonBuilder = StringBuilder()

                    val groupHeaderJson = JSONObject().apply {
                        put("type", "group_conv")
                        put("id", group.id)
                        put("name", group.name)
                        put("desc", group.description)
                        put("avatar_url", group.avatarUrl)
                        put("created_by", group.createdBy)
                        put("is_creator", group.isCreator)
                        put("my_role", group.myRole)
                        put("disappearing_sec", group.disappearingSeconds)
                    }
                    ndjsonBuilder.append(groupHeaderJson.toString()).append("\n")

                    batch.forEach { msg ->
                        val msgJson = JSONObject().apply {
                            put("type", "group_msg")
                            put("id", msg.id)
                            put("group_id", msg.groupId)
                            put("sender_id", msg.senderId)
                            put("sender_name", msg.senderName)
                            put("sender_avatar", msg.senderAvatar ?: "")
                            put("text", msg.text)
                            put("msg_type", msg.messageType)
                            put("event_type", msg.eventType ?: "")
                            put("local_media_path", msg.localMediaPath ?: "")
                            put("timestamp", msg.timestamp)
                            put("expires_at", msg.expiresAt ?: 0L)
                            put("is_outgoing", msg.isOutgoing)
                            put("reply_msg_id", msg.replyToMessageId ?: "")
                        }
                        ndjsonBuilder.append(msgJson.toString()).append("\n")

                        if (includeMedia && !msg.localMediaPath.isNullOrBlank()) {
                            val mediaFile = File(msg.localMediaPath)
                            if (mediaFile.exists() && mediaFile.length() < 100 * 1024 * 1024) {
                                val sha256 = calculateFileSha256(mediaFile)
                                val mediaChunkId = "m_$sha256.enc"
                                val existingChunk = newChunks.find { it.id == mediaChunkId }
                                if (existingChunk == null) {
                                    val tempEncryptedFile = File(context.cacheDir, mediaChunkId)
                                    try {
                                        FileInputStream(mediaFile).use { fis ->
                                            FileOutputStream(tempEncryptedFile).use { fos ->
                                                BackupCrypto.encryptMediaStream(fis, fos, masterKey, mediaChunkId.toByteArray())
                                            }
                                        }

                                        if (tempEncryptedFile.length() > 5 * 1024 * 1024) {
                                            DriveRestApiClient.uploadResumableFile(
                                                context = context,
                                                accountEmail = accountEmail,
                                                name = mediaChunkId,
                                                mimeType = "application/octet-stream",
                                                file = tempEncryptedFile
                                            )
                                        } else {
                                            DriveRestApiClient.uploadSmallFile(
                                                context = context,
                                                accountEmail = accountEmail,
                                                name = mediaChunkId,
                                                mimeType = "application/octet-stream",
                                                content = tempEncryptedFile.readBytes()
                                            )
                                        }

                                        newChunks.add(
                                            BackupChunkMetadata(
                                                id = mediaChunkId,
                                                type = "media",
                                                conversationOrGroupId = group.id,
                                                messageIdRange = msg.id,
                                                sha256 = sha256,
                                                sizeBytes = tempEncryptedFile.length()
                                            )
                                        )
                                    } finally {
                                        tempEncryptedFile.delete()
                                    }
                                }
                            }
                        }
                    }

                    val batchMsgIds = batch.map { it.id }.toSet()
                    groupReactions.filter { it.targetMessageId in batchMsgIds }.forEach { r ->
                        val rJson = JSONObject().apply {
                            put("type", "group_reaction")
                            put("group_id", r.groupId)
                            put("target_id", r.targetMessageId)
                            put("sender_id", r.senderId)
                            put("emoji", r.emoji)
                            put("ts", r.ts)
                        }
                        ndjsonBuilder.append(rJson.toString()).append("\n")
                    }

                    val gzipped = gzipCompress(ndjsonBuilder.toString().toByteArray(Charsets.UTF_8))
                    val chunkId = "c_${UUID.randomUUID()}.enc"
                    val encryptedChunk = BackupCrypto.encryptChunk(gzipped, chunkId, masterKey)
                    val sha256Hex = calculateSha256(encryptedChunk)

                    DriveRestApiClient.uploadSmallFile(
                        context = context,
                        accountEmail = accountEmail,
                        name = chunkId,
                        mimeType = "application/octet-stream",
                        content = encryptedChunk
                    )

                    val rangeStr = "${batch.first().timestamp}-${batch.last().timestamp}"
                    newChunks.add(
                        BackupChunkMetadata(
                            id = chunkId,
                            type = "messages",
                            conversationOrGroupId = group.id,
                            messageIdRange = rangeStr,
                            sha256 = sha256Hex,
                            sizeBytes = encryptedChunk.size.toLong()
                        )
                    )
                }
            }

            if (isCancelled()) return@withContext false
            onProgress?.invoke(85, "Uploading encrypted manifest...")

            val manifest = BackupManifest(
                version = 1,
                timestamp = System.currentTimeMillis(),
                chunks = newChunks
            )

            val manifestBytes = manifest.toJson().toByteArray(Charsets.UTF_8)
            val encryptedManifest = BackupCrypto.encryptChunk(manifestBytes, MANIFEST_FILE_NAME, masterKey)

            val existingManifestFile = existingDriveFiles.find { it.name == MANIFEST_FILE_NAME }
            if (existingManifestFile != null) {
                DriveRestApiClient.deleteFile(context, accountEmail, existingManifestFile.id)
            }

            DriveRestApiClient.uploadSmallFile(
                context = context,
                accountEmail = accountEmail,
                name = MANIFEST_FILE_NAME,
                mimeType = "application/octet-stream",
                content = encryptedManifest
            )

            onProgress?.invoke(95, "Cleaning up obsolete backup files...")
            val validChunkIds = newChunks.map { it.id }.toSet()
            val allRemoteFiles = DriveRestApiClient.listAppDataFiles(context, accountEmail)
            for (remoteFile in allRemoteFiles) {
                if (remoteFile.name != HEADER_FILE_NAME &&
                    remoteFile.name != MANIFEST_FILE_NAME &&
                    !validChunkIds.contains(remoteFile.name)
                ) {
                    try {
                        DriveRestApiClient.deleteFile(context, accountEmail, remoteFile.id)
                        Log.d(TAG, "Deleted orphan backup chunk ${remoteFile.name}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete orphan file ${remoteFile.name}", e)
                    }
                }
            }

            val totalSizeBytes = newChunks.sumOf { it.sizeBytes } + encryptedManifest.size
            ChatBackupPreferencesManager.setLastBackupTimestamp(context, System.currentTimeMillis())
            ChatBackupPreferencesManager.setLastBackupSizeBytes(context, totalSizeBytes)

            onProgress?.invoke(100, "Backup complete!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed: ${e.message}", e)
            throw e
        }
    }

    fun hasMasterKeyInKeystore(context: Context): Boolean {
        return BackupCrypto.loadMasterKeyFromKeystore(context) != null
    }

    suspend fun hasBackupHeaderOnDrive(context: Context, accountEmail: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveFiles = DriveRestApiClient.listAppDataFiles(context, accountEmail)
            driveFiles.any { it.name == HEADER_FILE_NAME }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking backup header on Drive", e)
            false
        }
    }

    suspend fun initializeBackupKeyHeader(
        context: Context,
        accountEmail: String,
        vaultPassword: String
    ): ByteArray = withContext(Dispatchers.IO) {
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User not logged in.")

        val existingDriveFiles = DriveRestApiClient.listAppDataFiles(context, accountEmail)
        val headerFile = existingDriveFiles.find { it.name == HEADER_FILE_NAME }

        val masterKey: ByteArray
        if (headerFile != null) {
            val headerBytes = DriveRestApiClient.downloadFileContent(context, accountEmail, headerFile.id)
            val headerJson = String(headerBytes, Charsets.UTF_8)
            masterKey = try {
                BackupCrypto.unwrapMasterKey(headerJson, vaultPassword, firebaseUid)
            } catch (e: Exception) {
                throw IllegalArgumentException("Wrong password")
            }
        } else {
            masterKey = BackupCrypto.loadMasterKeyFromKeystore(context) ?: BackupCrypto.generateMasterKey()
            val headerObj = BackupCrypto.wrapMasterKey(masterKey, vaultPassword, firebaseUid)
            val headerBytes = headerObj.toJson().toByteArray(Charsets.UTF_8)
            DriveRestApiClient.uploadSmallFile(
                context = context,
                accountEmail = accountEmail,
                name = HEADER_FILE_NAME,
                mimeType = "application/json",
                content = headerBytes
            )
        }

        BackupCrypto.saveMasterKeyToKeystore(context, masterKey)
        val vaultKey = VaultSecurityManager.getVaultKeyForBackgroundBackup(context)
        if (vaultKey != null) {
            BackupCrypto.saveVaultKeyToKeystore(context, vaultKey)
        }
        masterKey
    }

    suspend fun checkBackupOnDrive(context: Context, accountEmail: String): DriveBackupInfo? = withContext(Dispatchers.IO) {
        try {
            val driveFiles = DriveRestApiClient.listAppDataFiles(context, accountEmail)
            val headerFile = driveFiles.find { it.name == HEADER_FILE_NAME } ?: return@withContext null

            val headerBytes = DriveRestApiClient.downloadFileContent(context, accountEmail, headerFile.id)
            val headerJson = String(headerBytes, Charsets.UTF_8)
            val headerObj = BackupHeader.fromJson(headerJson)

            val totalSize = driveFiles.sumOf { it.size }
            val sizeStr = if (totalSize > 0) {
                String.format(java.util.Locale.US, "%.2f MB", totalSize.toDouble() / (1024 * 1024))
            } else "Unknown size"

            val lastTimestamp = ChatBackupPreferencesManager.getLastBackupTimestamp(context)
            val dateStr = if (lastTimestamp > 0L) {
                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                sdf.format(java.util.Date(lastTimestamp))
            } else "Available on Drive"

            DriveBackupInfo(
                hasBackup = true,
                dateString = dateStr,
                sizeString = sizeStr,
                sizeBytes = totalSize,
                headerJson = headerJson,
                uid = headerObj.uid
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking backup on Drive", e)
            null
        }
    }

    suspend fun performRestore(
        context: Context,
        vaultPassword: String,
        onProgress: ((percent: Int, text: String) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val accountEmail = ChatBackupPreferencesManager.getDriveAccountName(context)
                ?: throw IllegalStateException("No Google Account selected.")

            val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw IllegalStateException("User not logged in.")

            onProgress?.invoke(5, "Fetching backup files from Google Drive...")
            val driveFiles = DriveRestApiClient.listAppDataFiles(context, accountEmail)

            val headerFile = driveFiles.find { it.name == HEADER_FILE_NAME }
                ?: throw IllegalStateException("No backup header found in Google Drive.")

            val manifestFile = driveFiles.find { it.name == MANIFEST_FILE_NAME }
                ?: throw IllegalStateException("No backup manifest found in Google Drive.")

            val headerBytes = DriveRestApiClient.downloadFileContent(context, accountEmail, headerFile.id)
            val headerJson = String(headerBytes, Charsets.UTF_8)

            // 1. Verify UID and unwrap master key BK
            val masterKey = try {
                BackupCrypto.unwrapMasterKey(headerJson, vaultPassword, firebaseUid)
            } catch (e: IllegalArgumentException) {
                if (e.message?.contains("another FunkyTalk account", ignoreCase = true) == true) {
                    throw e
                }
                throw IllegalArgumentException("Wrong password")
            } catch (e: javax.crypto.AEADBadTagException) {
                throw IllegalArgumentException("Wrong password")
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("another FunkyTalk account", ignoreCase = true)) {
                    throw e
                }
                throw IllegalArgumentException("Wrong password")
            }

            BackupCrypto.saveMasterKeyToKeystore(context, masterKey)

            // Ensure local vault is configured / unlocked with this password
            val activeVaultKey = if (!VaultSecurityManager.isVaultConfigured(context)) {
                VaultSecurityManager.setupVaultWithPassword(context, vaultPassword).getOrThrow()
            } else {
                VaultSecurityManager.getActiveVaultKey()
                    ?: VaultSecurityManager.unlockWithPassword(context, vaultPassword).getOrThrow()
            }

            onProgress?.invoke(20, "Decrypting backup manifest...")
            val encryptedManifest = DriveRestApiClient.downloadFileContent(context, accountEmail, manifestFile.id)
            val decryptedManifestBytes = BackupCrypto.decryptChunk(encryptedManifest, MANIFEST_FILE_NAME, masterKey)
            val manifest = BackupManifest.fromJson(String(decryptedManifestBytes, Charsets.UTF_8))

            val db = EncryptedChatDatabase.getInstance(context, activeVaultKey)

            // Segregate text/metadata chunks from media chunks: text first!
            val messageChunks = manifest.chunks.filter { it.type == "messages" }
            val mediaChunks = manifest.chunks.filter { it.type == "media" }
            val totalMessageChunks = messageChunks.size

            onProgress?.invoke(25, "Restoring messages and conversations...")

            val restoredConvIds = mutableSetOf<String>()

            var processedMsgChunks = 0
            for (chunkMeta in messageChunks) {
                processedMsgChunks++
                val pct = 25 + if (totalMessageChunks > 0) ((processedMsgChunks.toFloat() / totalMessageChunks) * 50).toInt() else 0
                onProgress?.invoke(pct, "Restoring chat batch $processedMsgChunks of $totalMessageChunks")

                val chunkFile = driveFiles.find { it.name == chunkMeta.id } ?: continue
                val encryptedChunk = DriveRestApiClient.downloadFileContent(context, accountEmail, chunkFile.id)

                // Verify SHA256 of downloaded ciphertext
                val chunkSha = calculateSha256(encryptedChunk)
                if (chunkMeta.sha256.isNotBlank() && !chunkSha.equals(chunkMeta.sha256, ignoreCase = true)) {
                    Log.w(TAG, "SHA256 mismatch for chunk ${chunkMeta.id}")
                }

                // Decrypt with AES-256-GCM (verifies tag!)
                val gzipped = BackupCrypto.decryptChunk(encryptedChunk, chunkMeta.id, masterKey)
                val decompressedNdjson = String(gzipDecompress(gzipped), Charsets.UTF_8)

                val lines = decompressedNdjson.lines()
                val pendingDmMessages = mutableListOf<LocalDmMessage>()
                val pendingGroupMessages = mutableListOf<LocalGroupMessage>()
                val pendingDmReactions = mutableListOf<LocalDmMessageReaction>()
                val pendingGroupReactions = mutableListOf<LocalMessageReaction>()

                for (line in lines) {
                    if (line.isBlank()) continue
                    val json = JSONObject(line)
                    when (json.optString("type")) {
                        "dm_conv" -> {
                            val conv = LocalDmConversation(
                                id = json.getString("id"),
                                otherUserId = json.getString("other_user_id"),
                                otherUserName = json.getString("other_user_name"),
                                otherUserAvatar = json.optString("other_user_avatar").takeIf { it.isNotBlank() },
                                lastMessageText = json.optString("last_msg_text"),
                                lastMessageTime = json.optLong("last_msg_time", System.currentTimeMillis())
                            )
                            db.conversationDao().insertConversationIgnore(conv)
                            restoredConvIds.add(conv.id)
                        }
                        "dm_msg" -> {
                            val msg = LocalDmMessage(
                                id = json.getString("id"),
                                conversationId = json.getString("conv_id"),
                                senderId = json.getString("sender_id"),
                                recipientId = json.getString("recipient_id"),
                                text = json.getString("text"),
                                messageType = json.optString("msg_type", "text"),
                                timestamp = json.getLong("timestamp"),
                                isOutgoing = json.getBoolean("is_outgoing"),
                                replyToText = json.optString("reply_text").takeIf { it.isNotBlank() },
                                replyToSender = json.optString("reply_sender").takeIf { it.isNotBlank() }
                            )
                            pendingDmMessages.add(msg)
                            restoredConvIds.add(msg.conversationId)
                        }
                        "dm_reaction" -> {
                            val reaction = LocalDmMessageReaction(
                                conversationId = json.getString("conv_id"),
                                targetMessageId = json.getString("target_id"),
                                senderId = json.getString("sender_id"),
                                emoji = json.getString("emoji"),
                                ts = json.optLong("ts", System.currentTimeMillis())
                            )
                            pendingDmReactions.add(reaction)
                        }
                        "group_conv" -> {
                            val group = LocalGroupConversation(
                                id = json.getString("id"),
                                name = json.getString("name"),
                                description = json.optString("desc"),
                                avatarUrl = json.optString("avatar_url"),
                                createdBy = json.optString("created_by"),
                                isCreator = json.optBoolean("is_creator"),
                                myRole = json.optString("my_role", "member"),
                                disappearingSeconds = json.optInt("disappearing_sec", 0)
                            )
                            db.groupConversationDao().insertGroupIgnore(group)
                        }
                        "group_msg" -> {
                            val msg = LocalGroupMessage(
                                id = json.getString("id"),
                                groupId = json.getString("group_id"),
                                senderId = json.getString("sender_id"),
                                senderName = json.getString("sender_name"),
                                senderAvatar = json.optString("sender_avatar").takeIf { it.isNotBlank() },
                                text = json.getString("text"),
                                messageType = json.optString("msg_type", "text"),
                                eventType = json.optString("event_type").takeIf { it.isNotBlank() },
                                localMediaPath = json.optString("local_media_path").takeIf { it.isNotBlank() },
                                timestamp = json.getLong("timestamp"),
                                expiresAt = json.optLong("expires_at").takeIf { it > 0L },
                                isOutgoing = json.getBoolean("is_outgoing"),
                                replyToMessageId = json.optString("reply_msg_id").takeIf { it.isNotBlank() }
                            )
                            pendingGroupMessages.add(msg)
                        }
                        "group_reaction" -> {
                            val reaction = LocalMessageReaction(
                                groupId = json.getString("group_id"),
                                targetMessageId = json.getString("target_id"),
                                senderId = json.getString("sender_id"),
                                emoji = json.getString("emoji"),
                                ts = json.optLong("ts", System.currentTimeMillis())
                            )
                            pendingGroupReactions.add(reaction)
                        }
                    }
                }

                // Insert in transactions of ~500 messages with INSERT OR IGNORE
                if (pendingDmMessages.isNotEmpty()) {
                    pendingDmMessages.chunked(500).forEach { batch ->
                        db.runInTransaction {
                            kotlinx.coroutines.runBlocking {
                                db.messageDao().insertMessagesIgnore(batch)
                            }
                        }
                    }
                }
                if (pendingGroupMessages.isNotEmpty()) {
                    pendingGroupMessages.chunked(500).forEach { batch ->
                        db.runInTransaction {
                            kotlinx.coroutines.runBlocking {
                                db.groupMessageDao().insertMessagesIgnore(batch)
                            }
                        }
                    }
                }
                if (pendingDmReactions.isNotEmpty()) {
                    db.dmMessageReactionDao().insertReactionsIgnore(pendingDmReactions)
                }
                if (pendingGroupReactions.isNotEmpty()) {
                    db.messageReactionDao().insertReactionsIgnore(pendingGroupReactions)
                }
            }

            // Ensure 1:1 conversations reflect newest restored message and order
            for (convId in restoredConvIds) {
                val msgs = db.messageDao().getMessagesList(convId)
                val latestMsg = msgs.lastOrNull()
                if (latestMsg != null) {
                    db.conversationDao().updateLastMessage(
                        conversationId = convId,
                        text = latestMsg.text,
                        time = latestMsg.timestamp,
                        unreadDelta = 0
                    )
                }
            }

            // Attach restored group chats to server groups
            onProgress?.invoke(75, "Attaching groups to your account...")
            try {
                com.example.repository.GroupChatRepository.syncUserGroups(context)
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing user groups with server during restore", e)
            }

            // Process media in background: missing media shows placeholder, never crashes
            val totalMediaChunks = mediaChunks.size
            var processedMedia = 0
            val activeUid = com.example.ActiveUserManager.requireActiveUid(context)
            val mediaDir = com.example.ActiveUserManager.restoredMediaDir(context, activeUid).apply { mkdirs() }

            for (chunkMeta in mediaChunks) {
                processedMedia++
                val pct = 75 + if (totalMediaChunks > 0) ((processedMedia.toFloat() / totalMediaChunks) * 25).toInt() else 0
                onProgress?.invoke(pct, "Restoring media files ($processedMedia/$totalMediaChunks)...")

                try {
                    val chunkFile = driveFiles.find { it.name == chunkMeta.id }
                    if (chunkFile == null) {
                        Log.w(TAG, "Media chunk ${chunkMeta.id} not found on Drive, skipping gracefully")
                        continue
                    }

                    val targetMediaFile = File(mediaDir, "${chunkMeta.sha256}.media")
                    if (!targetMediaFile.exists()) {
                        val tempEncryptedMediaFile = File(context.cacheDir, "temp_${chunkMeta.id}")
                        try {
                            FileOutputStream(tempEncryptedMediaFile).use { fos ->
                                DriveRestApiClient.downloadFileToStream(context, accountEmail, chunkFile.id, fos)
                            }
                            FileInputStream(tempEncryptedMediaFile).use { fis ->
                                FileOutputStream(targetMediaFile).use { fos ->
                                    BackupCrypto.decryptMediaStream(fis, fos, masterKey, chunkMeta.id.toByteArray())
                                }
                            }
                        } finally {
                            tempEncryptedMediaFile.delete()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error restoring media chunk ${chunkMeta.id}: ${e.message}, skipping gracefully", e)
                }
            }

            ChatBackupPreferencesManager.setHasSkippedRestore(context, true)
            onProgress?.invoke(100, "Restore complete!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun changeBackupPassword(
        context: Context,
        oldPassword: String,
        newPassword: String
    ): Boolean = withContext(Dispatchers.IO) {
        val accountEmail = ChatBackupPreferencesManager.getDriveAccountName(context)
            ?: throw IllegalStateException("Google Drive not connected.")
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User not logged in.")

        val driveFiles = DriveRestApiClient.listAppDataFiles(context, accountEmail)
        val headerFile = driveFiles.find { it.name == HEADER_FILE_NAME }
            ?: throw IllegalStateException("No backup header found in Google Drive.")

        val headerBytes = DriveRestApiClient.downloadFileContent(context, accountEmail, headerFile.id)
        val headerJson = String(headerBytes, Charsets.UTF_8)

        val masterKey = BackupCrypto.unwrapMasterKey(headerJson, oldPassword, firebaseUid)
        val newHeader = BackupCrypto.wrapMasterKey(masterKey, newPassword, firebaseUid)
        val newHeaderBytes = newHeader.toJson().toByteArray(Charsets.UTF_8)

        DriveRestApiClient.deleteFile(context, accountEmail, headerFile.id)
        DriveRestApiClient.uploadSmallFile(
            context = context,
            accountEmail = accountEmail,
            name = HEADER_FILE_NAME,
            mimeType = "application/json",
            content = newHeaderBytes
        )

        ChatBackupPreferencesManager.setCachedVaultPassword(context, newPassword)
        true
    }

    suspend fun deleteBackupFromDrive(context: Context): Boolean = withContext(Dispatchers.IO) {
        val accountEmail = ChatBackupPreferencesManager.getDriveAccountName(context)
            ?: return@withContext false
        val driveFiles = DriveRestApiClient.listAppDataFiles(context, accountEmail)
        for (file in driveFiles) {
            try {
                DriveRestApiClient.deleteFile(context, accountEmail, file.id)
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting file ${file.name}", e)
            }
        }
        ChatBackupPreferencesManager.setLastBackupTimestamp(context, 0L)
        ChatBackupPreferencesManager.setLastBackupSizeBytes(context, 0L)
        BackupCrypto.clearMasterKeyFromKeystore(context)
        true
    }

    private fun gzipCompress(bytes: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzos ->
            gzos.write(bytes)
        }
        return bos.toByteArray()
    }

    private fun gzipDecompress(compressedBytes: ByteArray): ByteArray {
        val bis = ByteArrayInputStream(compressedBytes)
        val gzis = GZIPInputStream(bis)
        val bos = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var len: Int
        while (gzis.read(buffer).also { len = it } != -1) {
            bos.write(buffer, 0, len)
        }
        return bos.toByteArray()
    }

    private fun calculateSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        val sb = StringBuilder()
        for (b in hash) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun calculateFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        val hash = digest.digest()
        val sb = StringBuilder()
        for (b in hash) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
