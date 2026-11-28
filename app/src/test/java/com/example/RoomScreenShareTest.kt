package com.example

import com.example.realtime.RoomScreenShareManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomScreenShareTest {

    @Test
    fun `parseFlagsJson parses screen_share_enabled true and false and fails closed when missing`() {
        val enabled = AppFlagsRepository.parseFlagsJson(
            """{"coin_store_enabled":true,"vip_enabled":true,"min_supported_version_code":5,"screen_share_enabled":true}"""
        )
        assertNotNull(enabled)
        assertTrue(enabled!!.screenShareEnabled)

        val disabled = AppFlagsRepository.parseFlagsJson(
            """{"coin_store_enabled":true,"vip_enabled":false,"min_supported_version_code":5,"screen_share_enabled":false}"""
        )
        assertNotNull(disabled)
        assertFalse(disabled!!.screenShareEnabled)

        val missing = AppFlagsRepository.parseFlagsJson(
            """{"coin_store_enabled":true,"vip_enabled":true,"min_supported_version_code":5}"""
        )
        assertNotNull(missing)
        assertFalse(missing!!.screenShareEnabled)

        val wrongType = AppFlagsRepository.parseFlagsJson(
            """{"screen_share_enabled":"yes"}"""
        )
        assertNotNull(wrongType)
        assertFalse(wrongType!!.screenShareEnabled)

        assertNull(AppFlagsRepository.parseFlagsJson(""))
        assertNull(AppFlagsRepository.parseFlagsJson("not-json"))
    }

    @Test
    fun `parseTokenSuccessJson parses Daily screen share payload with 720p 15fps and 30s heartbeat`() {
        val raw = """
            {
              "provider": "DAILY",
              "share_id": "share-abc-123",
              "room_id": "room-xyz",
              "host_id": "user-host-1",
              "is_sharer": true,
              "daily_room_name": "ft-ss-room-xyz",
              "room_url": "https://funkytalk.daily.co/ft-ss-room-xyz",
              "domain": "funkytalk.daily.co",
              "token": "eyJhbGciOiJIUzI1NiJ9.test.sig",
              "screen_quality": { "max_height": 720, "max_fps": 15 },
              "heartbeat_seconds": 30
            }
        """.trimIndent()

        val payload = RoomScreenShareManager.parseTokenSuccessJson(raw)
        assertNotNull(payload)
        assertEquals("DAILY", payload!!.provider)
        assertEquals("share-abc-123", payload.shareId)
        assertEquals("room-xyz", payload.roomId)
        assertEquals("user-host-1", payload.hostId)
        assertTrue(payload.isSharer)
        assertEquals("https://funkytalk.daily.co/ft-ss-room-xyz", payload.roomUrl)
        assertEquals(720, payload.maxHeight)
        assertEquals(15, payload.maxFps)
        assertEquals(30, payload.heartbeatSeconds)
    }

    @Test
    fun `parseTokenSuccessJson rejects incomplete or malformed payloads`() {
        assertNull(RoomScreenShareManager.parseTokenSuccessJson(""))
        assertNull(RoomScreenShareManager.parseTokenSuccessJson("""{"error":"host_only"}"""))
        assertNull(RoomScreenShareManager.parseTokenSuccessJson("""{"token":"abc","room_url":"https://x.daily.co/r"}"""))
    }

    @Test
    fun `mapErrorCodeToUserMessage maps all backend error codes accurately`() {
        val disabled = RoomScreenShareManager.mapErrorCodeToUserMessage("screen_share_disabled")
        assertTrue(disabled.hideFeature)
        assertNull(disabled.userMessage)

        assertEquals("Only the host can share", RoomScreenShareManager.mapErrorCodeToUserMessage("host_only").userMessage)
        assertEquals("Host is away", RoomScreenShareManager.mapErrorCodeToUserMessage("host_away").userMessage)

        val roomEnded = RoomScreenShareManager.mapErrorCodeToUserMessage("room_ended")
        assertTrue(roomEnded.closeViewerSilently)
        assertNull(roomEnded.userMessage)

        val shareEnded = RoomScreenShareManager.mapErrorCodeToUserMessage("share_ended")
        assertTrue(shareEnded.closeViewerSilently)
        assertNull(shareEnded.userMessage)

        assertEquals("Join the room first", RoomScreenShareManager.mapErrorCodeToUserMessage("not_in_room").userMessage)
        assertEquals("You can't watch this", RoomScreenShareManager.mapErrorCodeToUserMessage("blocked").userMessage)
        assertEquals("You can't watch this", RoomScreenShareManager.mapErrorCodeToUserMessage("user_restricted").userMessage)
        assertEquals("Too many people are watching", RoomScreenShareManager.mapErrorCodeToUserMessage("viewer_limit").userMessage)
        assertEquals(
            "Screen share is resting for now, try later",
            RoomScreenShareManager.mapErrorCodeToUserMessage("daily_quota_reached").userMessage
        )
        assertEquals("Couldn't start. Try again", RoomScreenShareManager.mapErrorCodeToUserMessage("daily_not_configured").userMessage)
        assertEquals("Couldn't start. Try again", RoomScreenShareManager.mapErrorCodeToUserMessage("daily_room_setup_failed").userMessage)
        assertEquals("Couldn't start. Try again", RoomScreenShareManager.mapErrorCodeToUserMessage("daily_token_failed").userMessage)

        val unauth = RoomScreenShareManager.mapErrorCodeToUserMessage("unauthenticated")
        assertTrue(unauth.requiresReauth)
        assertNotNull(unauth.userMessage)
    }

    @Test
    fun `extractErrorCodeFromJson extracts structured error code`() {
        assertEquals("viewer_limit", RoomScreenShareManager.extractErrorCodeFromJson("""{"error":"viewer_limit"}"""))
        assertNull(RoomScreenShareManager.extractErrorCodeFromJson("""{}"""))
        assertNull(RoomScreenShareManager.extractErrorCodeFromJson("not-json"))
    }
}
