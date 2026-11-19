package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.repository.DeviceSessionRepository
import com.example.ui.components.SecureWindowRegistry
import com.example.ui.screens.profile.formatRelativeLastActive
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeviceSessionAndIdentityTest {

    @Test
    fun `device id matches spec regex and persists across session clear`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val id1 = DeviceIdentity.getDeviceId(context)
        assertTrue(
            "Device ID '$id1' must match ^[A-Za-z0-9._-]{8,64}$",
            DeviceIdentity.DEVICE_ID_REGEX.matches(id1)
        )

        SupabaseSessionManager.clearSession(context)
        val id2 = DeviceIdentity.getDeviceId(context)
        assertEquals(id1, id2)

        val meta = DeviceIdentity.getDeviceMetadata(context)
        assertEquals(id1, meta.deviceId)
        assertTrue(meta.name.isNotBlank() && meta.name.length <= 80)
        assertTrue(meta.os.startsWith("Android "))
        assertEquals(BuildConfig.VERSION_NAME, meta.appVersion)
    }

    @Test
    fun `verify firebase token request serializes device and kind without duplicate firebaseIdToken`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(VerifyFirebaseTokenRequest::class.java)
        val json = adapter.toJson(
            VerifyFirebaseTokenRequest(
                firebase_token = "fb-tok-123",
                device = VerifyDevicePayload(
                    device_id = "device-uuid-1234",
                    name = "Samsung SM-M022G",
                    os = "Android 11",
                    app_version = "0.9.0-beta1"
                ),
                kind = SupabaseSessionManager.KIND_LOGIN
            )
        )
        assertTrue(json.contains("\"firebase_token\":\"fb-tok-123\""))
        assertTrue(json.contains("\"kind\":\"login\""))
        assertTrue(json.contains("\"device_id\":\"device-uuid-1234\""))
        assertFalse("Duplicate firebaseIdToken must be removed", json.contains("firebaseIdToken"))
    }

    @Test
    fun `verifyTokenWithBackoff stops immediately on 403 device_revoked`() = runTest {
        var calls = 0
        val payload = VerifyDevicePayload("dev-id-12345678", "Pixel 8", "Android 14", "0.9.0-beta1")
        val result = SupabaseSessionManager.verifyTokenWithBackoff(
            firebaseIdToken = "fake-fb-jwt",
            devicePayload = payload,
            kind = SupabaseSessionManager.KIND_REFRESH
        ) {
            calls++
            val err = """{"code":"device_revoked"}""".toResponseBody("application/json".toMediaType())
            Response.error(403, err)
        }
        assertEquals(1, calls)
        assertTrue(result is SupabaseSessionManager.ExchangeResult.DeviceRevoked)
    }

    @Test
    fun `verifyTokenWithBackoff retries 503 session_mint_failed and surfaces Couldn't sign in message`() = runTest {
        var calls = 0
        val payload = VerifyDevicePayload("dev-id-12345678", "Pixel 8", "Android 14", "0.9.0-beta1")
        val result = SupabaseSessionManager.verifyTokenWithBackoff(
            firebaseIdToken = "fake-fb-jwt",
            devicePayload = payload,
            kind = SupabaseSessionManager.KIND_LOGIN
        ) { req ->
            assertEquals("login", req.kind)
            calls++
            val err = """{"code":"session_mint_failed"}""".toResponseBody("application/json".toMediaType())
            Response.error(503, err)
        }
        assertEquals(4, calls)
        assertTrue(result is SupabaseSessionManager.ExchangeResult.Failure)
        val failure = result as SupabaseSessionManager.ExchangeResult.Failure
        assertEquals(503, failure.httpCode)
        assertEquals("Couldn't sign in. Try again.", failure.message)
    }

    @Test
    fun `DeviceSessionRepository parses touch_device and get_my_devices JSON cleanly`() {
        val touchObj = DeviceSessionRepository.parseTouchDeviceResponse(
            """{"success":true,"known":true,"revoked":true}"""
        )
        assertEquals(TouchDeviceResponse(success = true, known = true, revoked = true), touchObj)

        val touchArr = DeviceSessionRepository.parseTouchDeviceResponse(
            """[{"success":true,"known":false,"revoked":false}]"""
        )
        assertEquals(TouchDeviceResponse(success = true, known = false, revoked = false), touchArr)

        val rows = DeviceSessionRepository.parseDeviceRows(
            """[
              {"id":"row-1","device_name":"Pixel 8","os":"Android 14","app_version":"0.9.0-beta1","last_seen_at":"2026-11-19T00:00:00Z","revoked":false,"is_current":true},
              {"id":42,"device_name":"Samsung SM-M022G","os":"Android 11","app_version":"0.8.9","last_login_at":"2026-11-18T12:00:00Z","revoked":true,"is_current":false}
            ]"""
        )
        assertEquals(2, rows.size)
        assertEquals("row-1", rows[0].id)
        assertTrue(rows[0].is_current)
        assertFalse(rows[0].revoked)
        assertEquals("42", rows[1].id)
        assertTrue(rows[1].revoked)
    }

    @Test
    fun `SecureWindowRegistry reference counts overlapping tokens`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).get()
        val window = activity.window
        val tokenA = Any()
        val tokenB = Any()

        SecureWindowRegistry.acquire(window, tokenA)
        assertTrue(SecureWindowRegistry.isSecureActiveForTest(window))

        SecureWindowRegistry.acquire(window, tokenB)
        assertTrue(SecureWindowRegistry.isSecureActiveForTest(window))

        SecureWindowRegistry.release(window, tokenA)
        assertTrue(SecureWindowRegistry.isSecureActiveForTest(window))

        SecureWindowRegistry.release(window, tokenB)
        assertFalse(SecureWindowRegistry.isSecureActiveForTest(window))
    }

    @Test
    fun `formatRelativeLastActive handles missing and iso timestamps`() {
        assertEquals("Last active recently", formatRelativeLastActive(null))
        val formatted = formatRelativeLastActive(java.time.Instant.now().minusSeconds(300).toString())
        assertTrue(formatted.startsWith("Last active "))
        assertTrue(formatted.contains("min ago"))
    }
}
