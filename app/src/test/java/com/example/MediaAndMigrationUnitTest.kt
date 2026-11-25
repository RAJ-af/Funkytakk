package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.db.DmMediaDownloadState
import com.example.db.EncryptedChatDatabase
import com.example.db.LocalDmMessage
import com.example.db.LocalGroupMessage
import com.example.db.LocalMessageStatus
import com.example.repository.EncryptedChatRepository
import com.example.repository.GroupChatRepository
import com.example.ui.chat.ChatAnimatedStickers
import com.example.ui.chat.ChatVideoHelper
import com.example.ui.chat.VoicePlaybackController
import com.example.ui.chat.parseDmAudioSpec
import com.example.ui.chat.parseDmVideoSpec
import com.example.ui.chat.parseGroupAudioSpec
import com.example.ui.chat.parseGroupVideoSpec
import com.example.util.ErrorMapper
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaAndMigrationUnitTest {

    @Test
    fun `Room MIGRATION_10_11 preserves existing rows adds 5 columns and queries pending downloads`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_mig_10_11_${System.nanoTime()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        // Create authentic v10 schema via SQLiteDatabase
        val sqlite = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqlite.use { raw ->
            raw.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `dm_conversations` (
                    `id` TEXT NOT NULL,
                    `otherUserId` TEXT NOT NULL,
                    `otherUserName` TEXT NOT NULL,
                    `otherUserAvatar` TEXT,
                    `otherUserFlag` TEXT,
                    `lastMessageText` TEXT NOT NULL,
                    `lastMessageType` TEXT NOT NULL,
                    `lastMessageTime` INTEGER NOT NULL,
                    `unreadCount` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            raw.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_dm_conversations_otherUserId` ON `dm_conversations` (`otherUserId`)")

            raw.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `dm_messages` (
                    `id` TEXT NOT NULL,
                    `conversationId` TEXT NOT NULL,
                    `senderId` TEXT NOT NULL,
                    `recipientId` TEXT NOT NULL,
                    `text` TEXT NOT NULL,
                    `messageType` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `isOutgoing` INTEGER NOT NULL,
                    `replyToText` TEXT,
                    `replyToSender` TEXT,
                    `clientId` TEXT,
                    `status` TEXT NOT NULL,
                    `failReason` TEXT,
                    `retryCount` INTEGER NOT NULL,
                    `nextRetryAt` INTEGER NOT NULL,
                    `localMediaPath` TEXT,
                    `mediaMimeType` TEXT,
                    `mediaDurationSec` INTEGER,
                    `mediaCaption` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_dm_messages_conversationId` ON `dm_messages` (`conversationId`)")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_dm_messages_timestamp` ON `dm_messages` (`timestamp`)")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_dm_messages_clientId` ON `dm_messages` (`clientId`)")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_dm_messages_status` ON `dm_messages` (`status`)")

            raw.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `dm_message_reactions` (
                    `conversation_id` TEXT NOT NULL,
                    `target_message_id` TEXT NOT NULL,
                    `sender_id` TEXT NOT NULL,
                    `emoji` TEXT NOT NULL,
                    `ts` INTEGER NOT NULL,
                    PRIMARY KEY(`target_message_id`, `sender_id`)
                )
                """.trimIndent()
            )
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_dm_message_reactions_conversation_id` ON `dm_message_reactions` (`conversation_id`)")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_dm_message_reactions_target_message_id` ON `dm_message_reactions` (`target_message_id`)")

            raw.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `group_conversations` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `avatarUrl` TEXT NOT NULL,
                    `createdBy` TEXT NOT NULL,
                    `isCreator` INTEGER NOT NULL,
                    `myRole` TEXT NOT NULL,
                    `myStatus` TEXT NOT NULL,
                    `memberCount` INTEGER NOT NULL,
                    `pendingRequestsCount` INTEGER NOT NULL,
                    `mutedUntil` INTEGER,
                    `notifyLevel` TEXT NOT NULL,
                    `mediaAutoDownload` INTEGER NOT NULL,
                    `whoCanSend` TEXT NOT NULL,
                    `whoCanEditInfo` TEXT NOT NULL,
                    `whoCanAddMembers` TEXT NOT NULL,
                    `approveNewMembers` INTEGER NOT NULL,
                    `showJoinNotifications` INTEGER NOT NULL,
                    `disappearingSeconds` INTEGER NOT NULL,
                    `memberListVisibility` TEXT NOT NULL,
                    `lastMessageText` TEXT NOT NULL,
                    `lastMessageType` TEXT NOT NULL,
                    `lastMessageSenderName` TEXT NOT NULL,
                    `lastMessageTime` INTEGER NOT NULL,
                    `unreadCount` INTEGER NOT NULL,
                    `hasUnreadMention` INTEGER NOT NULL,
                    `invitedByName` TEXT,
                    `isArchived` INTEGER NOT NULL,
                    `tombstoneReason` TEXT,
                    `tombstoneDeletedByName` TEXT,
                    `lastSyncedEventAt` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            raw.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `group_messages` (
                    `id` TEXT NOT NULL,
                    `groupId` TEXT NOT NULL,
                    `senderId` TEXT NOT NULL,
                    `senderName` TEXT NOT NULL,
                    `senderAvatar` TEXT,
                    `text` TEXT NOT NULL,
                    `messageType` TEXT NOT NULL,
                    `eventType` TEXT,
                    `storagePath` TEXT,
                    `localMediaPath` TEXT,
                    `mediaKey` TEXT,
                    `mediaNonce` TEXT,
                    `mimeType` TEXT,
                    `timestamp` INTEGER NOT NULL,
                    `expiresAt` INTEGER,
                    `isOutgoing` INTEGER NOT NULL,
                    `mentionsJson` TEXT NOT NULL,
                    `isMentioned` INTEGER NOT NULL,
                    `replyToMessageId` TEXT,
                    `replyToSenderId` TEXT,
                    `replyToSenderName` TEXT,
                    `replyToPreview` TEXT,
                    `clientId` TEXT,
                    `status` TEXT NOT NULL,
                    `failReason` TEXT,
                    `retryCount` INTEGER NOT NULL,
                    `nextRetryAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_group_messages_groupId` ON `group_messages` (`groupId`)")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_group_messages_timestamp` ON `group_messages` (`timestamp`)")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_group_messages_expiresAt` ON `group_messages` (`expiresAt`)")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_group_messages_clientId` ON `group_messages` (`clientId`)")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_group_messages_status` ON `group_messages` (`status`)")

            raw.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `message_reactions` (
                    `group_id` TEXT NOT NULL,
                    `target_message_id` TEXT NOT NULL,
                    `sender_id` TEXT NOT NULL,
                    `emoji` TEXT NOT NULL,
                    `ts` INTEGER NOT NULL,
                    PRIMARY KEY(`target_message_id`, `sender_id`)
                )
                """.trimIndent()
            )
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_message_reactions_target_message_id` ON `message_reactions` (`target_message_id`)")
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_message_reactions_group_id` ON `message_reactions` (`group_id`)")

            raw.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `message_translations` (
                    `message_id` TEXT NOT NULL,
                    `translated_text` TEXT NOT NULL,
                    `target_language` TEXT NOT NULL,
                    `provider` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    PRIMARY KEY(`message_id`)
                )
                """.trimIndent()
            )
            raw.execSQL("CREATE INDEX IF NOT EXISTS `index_message_translations_message_id` ON `message_translations` (`message_id`)")

            raw.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `inbox_failures` (
                    `messageId` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `reason` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    PRIMARY KEY(`messageId`)
                )
                """.trimIndent()
            )

            // Seed a pre-migration v10 row
            raw.execSQL(
                """
                INSERT INTO dm_messages (
                    id, conversationId, senderId, recipientId, text, messageType,
                    timestamp, isOutgoing, status, retryCount, nextRetryAt
                ) VALUES (
                    'msg_v10_1', 'conv_1', 'u_other', 'u_me', 'Hello v10', 'text',
                    1000, 0, 'SENT', 0, 0
                )
                """.trimIndent()
            )
            raw.version = 10
        }

        val migratedDb = Room.databaseBuilder(context, EncryptedChatDatabase::class.java, dbName)
            .addMigrations(EncryptedChatDatabase.MIGRATION_10_11)
            .allowMainThreadQueries()
            .build()

        try {
            val dao = migratedDb.messageDao()
            val legacyMsg = dao.getMessage("msg_v10_1")
            assertNotNull(legacyMsg)
            assertEquals("Hello v10", legacyMsg?.text)
            assertNull(legacyMsg?.storagePath)
            assertNull(legacyMsg?.mediaDownloadState)

            // Insert PENDING (older) and FAILED (newer) rows and confirm newest-first ordering
            dao.insertMessage(
                LocalDmMessage(
                    id = "dl_old",
                    conversationId = "conv_1",
                    senderId = "u_other",
                    recipientId = "u_me",
                    text = "[img]downloading...",
                    messageType = "image",
                    timestamp = 2000L,
                    isOutgoing = false,
                    status = LocalMessageStatus.SENT,
                    storagePath = "appwrite:file1",
                    mediaKey = "key1",
                    mediaNonce = "nonce1",
                    mediaDownloadState = DmMediaDownloadState.PENDING
                )
            )
            dao.insertMessage(
                LocalDmMessage(
                    id = "dl_new",
                    conversationId = "conv_1",
                    senderId = "u_other",
                    recipientId = "u_me",
                    text = "[vid]downloading...|10",
                    messageType = "video",
                    timestamp = 3000L,
                    isOutgoing = false,
                    status = LocalMessageStatus.SENT,
                    storagePath = "appwrite:file2",
                    mediaKey = "key2",
                    mediaNonce = "nonce2",
                    mediaDownloadState = DmMediaDownloadState.FAILED,
                    mediaFailReason = "Network error"
                )
            )

            val pending = dao.getPendingOrFailedMediaDownloads()
            assertEquals(listOf("dl_new", "dl_old"), pending.map { it.id })

            dao.markMediaDownloadReady(
                id = "dl_new",
                text = "[vid]file:///tmp/v.mp4|10",
                localMediaPath = "/tmp/v.mp4",
                mediaMimeType = "video/mp4"
            )
            val updated = dao.getMessage("dl_new")
            assertEquals(DmMediaDownloadState.READY, updated?.mediaDownloadState)
            assertEquals("/tmp/v.mp4", updated?.localMediaPath)
            assertNull(updated?.mediaFailReason)
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `VoicePlaybackController resolveAudioSourceToLocalFile resolves schemes and rejects missing or empty files`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val controller = VoicePlaybackController(context, scope)
        val tmpDir = File(context.cacheDir, "voice_test_${System.nanoTime()}").apply { mkdirs() }
        val nonEmpty = File(tmpDir, "voice.m4a").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val emptyFile = File(tmpDir, "empty.m4a").apply { writeBytes(byteArrayOf()) }
        val missingFile = File(tmpDir, "missing.m4a")

        try {
            assertEquals(nonEmpty.absolutePath, controller.resolveAudioSourceToLocalFile(context, "file://${nonEmpty.absolutePath}")?.absolutePath)
            assertEquals(nonEmpty.absolutePath, controller.resolveAudioSourceToLocalFile(context, "file:///${nonEmpty.absolutePath.trimStart('/')}")?.absolutePath)
            assertEquals(nonEmpty.absolutePath, controller.resolveAudioSourceToLocalFile(context, nonEmpty.absolutePath)?.absolutePath)
            assertNull(controller.resolveAudioSourceToLocalFile(context, emptyFile.absolutePath))
            assertNull(controller.resolveAudioSourceToLocalFile(context, missingFile.absolutePath))
            assertNull(controller.resolveAudioSourceToLocalFile(context, "downloading..."))
            assertNull(controller.resolveAudioSourceToLocalFile(context, ""))
        } finally {
            controller.release()
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `EncryptedChatRepository resolveExtensionForMedia distinguishes video mp4 from audio mp4 and mpeg`() {
        assertEquals("mp4", EncryptedChatRepository.resolveExtensionForMedia("video/mp4", "video"))
        assertEquals("m4a", EncryptedChatRepository.resolveExtensionForMedia("audio/mp4", "voice"))
        assertEquals("m4a", EncryptedChatRepository.resolveExtensionForMedia("audio/aac", "audio"))
        assertEquals("mp3", EncryptedChatRepository.resolveExtensionForMedia("audio/mpeg", "audio"))
        assertEquals("ogg", EncryptedChatRepository.resolveExtensionForMedia("audio/ogg", "audio"))
        assertEquals("jpg", EncryptedChatRepository.resolveExtensionForMedia("image/jpeg", "image"))
    }

    @Test
    fun `Appwrite project ID extraction handles appwrite hosts and ignores others`() {
        val appwriteUrl = "https://fra.cloud.appwrite.io/v1/storage/buckets/b1/files/f1/download?project=proj_999&mode=admin"
        assertEquals("proj_999", EncryptedChatRepository.extractAppwriteProjectId(appwriteUrl))
        assertEquals("proj_999", GroupChatRepository.extractAppwriteProjectId(appwriteUrl))

        val otherUrl = "https://example.supabase.co/storage/v1/object/chat?project=should_ignore"
        assertNull(EncryptedChatRepository.extractAppwriteProjectId(otherUrl))
        assertNull(GroupChatRepository.extractAppwriteProjectId(otherUrl))
    }

    @Test
    fun `Media specs and JSON descriptor fields parse cleanly across DM and Group models`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tmpDir = File(context.cacheDir, "media_spec_${System.nanoTime()}").apply { mkdirs() }
        val vidFile = File(tmpDir, "clip.mp4").apply { writeBytes(byteArrayOf(0x01, 0x02)) }
        val audFile = File(tmpDir, "song.mp3").apply { writeBytes(byteArrayOf(0x03, 0x04)) }

        try {
            val dmVid = LocalDmMessage(
                id = "v1",
                conversationId = "c1",
                senderId = "u1",
                recipientId = "u2",
                text = "[vid]file://${vidFile.absolutePath}|14|Sunset",
                messageType = "video",
                isOutgoing = true,
                localMediaPath = vidFile.absolutePath
            )
            val parsedDmVid = parseDmVideoSpec(dmVid)
            assertEquals(vidFile.absolutePath, parsedDmVid.localPath)
            assertEquals(14, parsedDmVid.durationSec)
            assertEquals("Sunset", parsedDmVid.caption)
            assertEquals(DmMediaDownloadState.READY, parsedDmVid.downloadState)

            val dmAud = LocalDmMessage(
                id = "a1",
                conversationId = "c1",
                senderId = "u1",
                recipientId = "u2",
                text = "[audio]downloading...|95|LoFi Beat.mp3",
                messageType = "audio",
                isOutgoing = false,
                mediaDownloadState = DmMediaDownloadState.FAILED,
                mediaFailReason = "Timeout"
            )
            val parsedDmAud = parseDmAudioSpec(dmAud)
            assertNull(parsedDmAud.localPath)
            assertEquals(95, parsedDmAud.durationSec)
            assertEquals("LoFi Beat.mp3", parsedDmAud.title)
            assertEquals(DmMediaDownloadState.FAILED, parsedDmAud.downloadState)
            assertEquals("Timeout", parsedDmAud.failReason)

            val grpAud = LocalGroupMessage(
                id = "ga1",
                groupId = "g1",
                senderId = "u1",
                senderName = "Sam",
                text = "Audio|42|Podcast.m4a",
                messageType = "audio",
                isOutgoing = false,
                localMediaPath = audFile.absolutePath
            )
            val parsedGrpAud = parseGroupAudioSpec(grpAud)
            assertEquals(audFile.absolutePath, parsedGrpAud.localPath)
            assertEquals(42, parsedGrpAud.durationSec)
            assertEquals("Podcast.m4a", parsedGrpAud.title)

            val grpVidPending = LocalGroupMessage(
                id = "gv1",
                groupId = "g1",
                senderId = "u1",
                senderName = "Sam",
                text = "Video|8",
                messageType = "video",
                isOutgoing = false,
                storagePath = "appwrite:123",
                mediaKey = "k",
                mediaNonce = "n"
            )
            val parsedGrpVid = parseGroupVideoSpec(grpVidPending, DmMediaDownloadState.EXPIRED to "Media expired. Ask them to send it again.")
            assertNull(parsedGrpVid.localPath)
            assertEquals(8, parsedGrpVid.durationSec)
            assertEquals(DmMediaDownloadState.EXPIRED, parsedGrpVid.downloadState)

            // Verify group descriptor JSON contract
            val envelope = JSONObject().apply {
                put("message_type", "audio")
                put("duration_seconds", 42)
                put("file_name", "Podcast.m4a")
                put("caption", "Morning episode")
            }
            val parsed = JSONObject(envelope.toString())
            assertEquals("audio", parsed.getString("message_type"))
            assertEquals(42, parsed.getInt("duration_seconds"))
            assertEquals("Podcast.m4a", parsed.getString("file_name"))
            assertEquals("Morning episode", parsed.getString("caption"))
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `ChatVideoHelper heap headroom check rejects oversized or memory-tight payloads`() {
        val mb = 1024L * 1024L
        assertTrue(ChatVideoHelper.hasHeapHeadroomForBytes(0L, freeHeapBytes = 5L * mb))
        assertFalse(ChatVideoHelper.hasHeapHeadroomForBytes(ChatVideoHelper.MAX_MEDIA_BYTES + 1L, freeHeapBytes = 512L * mb))
        // 15 MB needs 40 MB free heap
        assertFalse(ChatVideoHelper.hasHeapHeadroomForBytes(15L * mb, freeHeapBytes = 30L * mb, deviceAvailMemBytes = 500L * mb))
        assertTrue(ChatVideoHelper.hasHeapHeadroomForBytes(15L * mb, freeHeapBytes = 50L * mb, deviceAvailMemBytes = 500L * mb))
        // Tight system RAM check (3x rule)
        assertFalse(ChatVideoHelper.hasHeapHeadroomForBytes(15L * mb, freeHeapBytes = 80L * mb, deviceAvailMemBytes = 32L * mb))
    }

    @Test
    fun `ErrorMapper 413 maps to MSG_PAYLOAD_TOO_LARGE and ChatAnimatedStickers round-trips`() {
        assertEquals(ErrorMapper.MSG_PAYLOAD_TOO_LARGE, ErrorMapper.mapHttp(413, "Payload too large"))

        val sample = ChatAnimatedStickers.allStickers.first()
        val encoded = ChatAnimatedStickers.encode(sample)
        val parsed = ChatAnimatedStickers.parse(encoded)
        assertNotNull(parsed)
        assertEquals(sample.id, parsed?.sticker?.id)
        assertTrue(ChatAnimatedStickers.readableLabel(encoded).contains(sample.label))
    }
}
