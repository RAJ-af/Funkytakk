package com.example

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.util.RoomShareHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FunkyTalk", appName)
  }

  @Test
  fun `parse funkytalk room deep link`() {
    val uri = Uri.parse("funkytalk://room/123456")
    assertEquals("funkytalk", uri.scheme)
    assertEquals("room", uri.host)
    assertEquals("123456", uri.pathSegments.firstOrNull())
  }

  @Test
  fun `verify room share helper formats whatsapp message correctly`() {
    val message = RoomShareHelper.buildWhatsAppMessage(
      roomName = "Hindi Voice Circle",
      roomId = "FT5272",
      inviteLink = "funkytalk://room/FT5272"
    )
    val expected = """🗣️ Join me on FunkyTalk!

Let’s practice languages and have a fun conversation. 🌍✨

Room: Hindi Voice Circle
Room ID: FT5272

👉 Join here:
funkytalk://room/FT5272"""
    assertEquals(expected, message)
  }

  @Test
  fun `verify room share helper formats facebook message correctly`() {
    val message = RoomShareHelper.buildFacebookMessage(
      roomName = "Hindi Voice Circle",
      roomId = "FT5272",
      inviteLink = "funkytalk://room/FT5272"
    )
    val expected = """🗣️ Join me on FunkyTalk!
Let’s practice languages together. 🌍

Room: Hindi Voice Circle
Room ID: FT5272

funkytalk://room/FT5272"""
    assertEquals(expected, message)
  }

  @Test
  fun `verify room share helper formats more message correctly`() {
    val message = RoomShareHelper.buildMoreMessage(
      roomName = "Hindi Voice Circle",
      roomId = "FT5272",
      inviteLink = "funkytalk://room/FT5272"
    )
    val expected = """🗣️ Join me on FunkyTalk!

Room: Hindi Voice Circle
Room ID: FT5272

Let’s practice languages and connect. 🌍✨

Join here:
funkytalk://room/FT5272"""
    assertEquals(expected, message)
  }

  @Test
  fun `verify copy link copies only actual invite link`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testLink = "funkytalk://room/FT5272"
    RoomShareHelper.copyLinkToClipboard(context, testLink)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip
    assertNotNull(clip)
    assertEquals(testLink, clip?.getItemAt(0)?.text?.toString())
  }

  @Test
  fun `verify getDisplayRoomCode returns existing short room code`() {
    val result = RoomShareHelper.getDisplayRoomCode(roomId = "FT5272", joinCode = null)
    assertEquals("FT5272", result)
  }

  @Test
  fun `verify getDisplayRoomCode derives short code from backend UUID without exposing full UUID`() {
    val backendUuid = "6a282928-1b20-463b-9a8c-6903bf2d5792"
    val shortCode = RoomShareHelper.getDisplayRoomCode(roomId = backendUuid, joinCode = null)
    
    // Must start with FT
    org.junit.Assert.assertTrue(shortCode.startsWith("FT"))
    // Must be short (e.g. FT + 4 digits = 6 characters)
    assertEquals(6, shortCode.length)
    // Must NOT be the raw UUID or contain hyphens
    org.junit.Assert.assertFalse(shortCode.contains("-"))
    org.junit.Assert.assertNotEquals(backendUuid, shortCode)
    
    // Deterministic: second call with same UUID yields same short code
    val shortCode2 = RoomShareHelper.getDisplayRoomCode(roomId = backendUuid, joinCode = null)
    assertEquals(shortCode, shortCode2)
  }

  @Test
  fun `verify getDisplayRoomCode uses join code when present`() {
    val backendUuid = "6a282928-1b20-463b-9a8c-6903bf2d5792"
    val result = RoomShareHelper.getDisplayRoomCode(roomId = backendUuid, joinCode = "FT5272")
    assertEquals("FT5272", result)

    val numericResult = RoomShareHelper.getDisplayRoomCode(roomId = backendUuid, joinCode = "5272")
    assertEquals("FT5272", numericResult)
  }

  @Test
  fun `verify getDisplayRoomCode default fallback for blank`() {
    val result = RoomShareHelper.getDisplayRoomCode(roomId = "", joinCode = "")
    assertEquals("FT5272", result)
  }
}

