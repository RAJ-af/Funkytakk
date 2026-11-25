package com.example

import com.example.db.DmMediaDownloadState
import com.example.db.LocalDmMessage
import com.example.db.LocalGroupMessage
import com.example.repository.EncryptedChatRepository
import com.example.repository.GroupChatRepository
import com.example.ui.chat.ChatAnimatedStickers
import com.example.ui.chat.ChatVideoHelper
import com.example.ui.chat.parseDmAudioSpec
import com.example.ui.chat.parseDmVideoSpec
import com.example.ui.chat.parseGroupAudioSpec
import com.example.ui.chat.parseGroupVideoSpec
import com.example.util.ErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PureMediaLogicUnitTest {

    @Test
    fun `resolveExtensionForMedia maps video and audio mimes accurately`() {
        assertEquals("mp4", EncryptedChatRepository.resolveExtensionForMedia("video/mp4", "video"))
        assertEquals("m4a", EncryptedChatRepository.resolveExtensionForMedia("audio/mp4", "voice"))
        assertEquals("m4a", EncryptedChatRepository.resolveExtensionForMedia("audio/aac", "audio"))
        assertEquals("mp3", EncryptedChatRepository.resolveExtensionForMedia("audio/mpeg", "audio"))
        assertEquals("ogg", EncryptedChatRepository.resolveExtensionForMedia("audio/ogg", "audio"))
        assertEquals("jpg", EncryptedChatRepository.resolveExtensionForMedia("image/jpeg", "image"))
    }

    @Test
    fun `extractAppwriteProjectId extracts query param only for appwrite hosts`() {
        val url = "https://fra.cloud.appwrite.io/v1/storage/buckets/b1/files/f1/download?project=proj_999&mode=admin"
        assertEquals("proj_999", EncryptedChatRepository.extractAppwriteProjectId(url))
        assertEquals("proj_999", GroupChatRepository.extractAppwriteProjectId(url))

        val nonAppwrite = "https://example.supabase.co/storage/v1/object/chat?project=should_ignore"
        assertNull(EncryptedChatRepository.extractAppwriteProjectId(nonAppwrite))
        assertNull(GroupChatRepository.extractAppwriteProjectId(nonAppwrite))
    }

    @Test
    fun `hasHeapHeadroomForBytes prevents OOM on oversized or tight-memory payloads`() {
        val mb = 1024L * 1024L
        assertTrue(ChatVideoHelper.hasHeapHeadroomForBytes(0L, freeHeapBytes = 5L * mb))
        assertFalse(ChatVideoHelper.hasHeapHeadroomForBytes(ChatVideoHelper.MAX_MEDIA_BYTES + 1L, freeHeapBytes = 512L * mb))
        assertFalse(ChatVideoHelper.hasHeapHeadroomForBytes(15L * mb, freeHeapBytes = 30L * mb, deviceAvailMemBytes = 500L * mb))
        assertTrue(ChatVideoHelper.hasHeapHeadroomForBytes(15L * mb, freeHeapBytes = 50L * mb, deviceAvailMemBytes = 500L * mb))
        assertFalse(ChatVideoHelper.hasHeapHeadroomForBytes(15L * mb, freeHeapBytes = 80L * mb, deviceAvailMemBytes = 32L * mb))
    }

    @Test
    fun `video and audio specs handle local files and missing or failed states safely`() {
        val tmpDir = kotlin.io.path.createTempDirectory("media_pure_test").toFile()
        val vidFile = File(tmpDir, "clip.mp4").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val audFile = File(tmpDir, "track.mp3").apply { writeBytes(byteArrayOf(4, 5, 6)) }

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
            val parsedVid = parseDmVideoSpec(dmVid)
            assertEquals(vidFile.absolutePath, parsedVid.localPath)
            assertEquals(14, parsedVid.durationSec)
            assertEquals("Sunset", parsedVid.caption)
            assertEquals(DmMediaDownloadState.READY, parsedVid.downloadState)

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
            val parsedAud = parseDmAudioSpec(dmAud)
            assertNull(parsedAud.localPath)
            assertEquals(95, parsedAud.durationSec)
            assertEquals("LoFi Beat.mp3", parsedAud.title)
            assertEquals(DmMediaDownloadState.FAILED, parsedAud.downloadState)

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

            val grpVidExpired = LocalGroupMessage(
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
            val parsedGrpVid = parseGroupVideoSpec(grpVidExpired, DmMediaDownloadState.EXPIRED to "Media expired")
            assertNull(parsedGrpVid.localPath)
            assertEquals(8, parsedGrpVid.durationSec)
            assertEquals(DmMediaDownloadState.EXPIRED, parsedGrpVid.downloadState)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `ErrorMapper 413 and ChatAnimatedStickers round-trip succeed on pure JVM`() {
        assertEquals(ErrorMapper.MSG_PAYLOAD_TOO_LARGE, ErrorMapper.mapHttp(413, "Payload too large"))
        val sample = ChatAnimatedStickers.allStickers.first()
        val encoded = ChatAnimatedStickers.encode(sample)
        val parsed = ChatAnimatedStickers.parse(encoded)
        assertNotNull(parsed)
        assertEquals(sample.id, parsed?.sticker?.id)
        assertTrue(ChatAnimatedStickers.readableLabel(encoded).contains(sample.label))
    }
}
