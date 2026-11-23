package com.example

import com.example.data.LocalStartupSnapshot
import com.example.data.StartupPrefsStore
import com.example.util.ErrorMapper
import com.example.util.MappedErrorException
import com.example.util.StartupTimingTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorMapperAndCacheTest {

    @Test
    fun `ErrorMapper maps network exceptions to friendly offline message`() {
        assertEquals(ErrorMapper.MSG_OFFLINE, ErrorMapper.map(UnknownHostException("db.supabase.co")))
        assertEquals(ErrorMapper.MSG_OFFLINE, ErrorMapper.map(ConnectException("Failed to connect to 10.0.2.2")))
        assertEquals(ErrorMapper.MSG_OFFLINE, ErrorMapper.map(SocketTimeoutException("Read timed out")))
        assertEquals(ErrorMapper.MSG_OFFLINE, ErrorMapper.map(RuntimeException("wrapper", IOException("reset by peer"))))
    }

    @Test
    fun `ErrorMapper maps HTTP status codes and avoids leaking raw JSON or URLs`() {
        assertEquals(ErrorMapper.MSG_UNAUTHORIZED, ErrorMapper.mapHttp(401, """{"error":"invalid_jwt"}"""))
        assertEquals(
            ErrorMapper.MSG_DEVICE_REVOKED,
            ErrorMapper.mapHttp(403, """{"code":"device_revoked","url":"https://secret.example.com"}""")
        )
        assertEquals(ErrorMapper.MSG_RATE_LIMITED, ErrorMapper.mapHttp(429, "rate_limited"))
        assertEquals(ErrorMapper.MSG_SERVER_BUSY, ErrorMapper.mapHttp(503, """{"code":"session_mint_failed"}"""))
        assertEquals(ErrorMapper.MSG_SERVER_BUSY, ErrorMapper.mapHttp(500, "Internal SQL syntax error at line 42"))
        assertEquals(ErrorMapper.MSG_GENERIC, ErrorMapper.mapHttp(400, """{"detail":"raw trace https://host/path"}"""))

        val unknown = ErrorMapper.map(IllegalStateException("https://internal.api/v1/secret stacktrace java.lang.Foo"))
        assertEquals(ErrorMapper.MSG_GENERIC, unknown)
        assertFalse(unknown.contains("http"))
        assertFalse(unknown.contains("IllegalState"))

        val mapped = MappedErrorException(ErrorMapper.MSG_DEVICE_REVOKED, 403)
        assertEquals(ErrorMapper.MSG_DEVICE_REVOKED, ErrorMapper.map(mapped))
    }

    @Test
    fun `StartupTimingTracker computes cold start elapsed time and slowest request`() {
        StartupTimingTracker.resetForTest()
        StartupTimingTracker.markAppCreate(nowMs = 1000L)
        StartupTimingTracker.recordRequest("home_rooms", 210L)
        StartupTimingTracker.recordRequest("home_friends", 435L)
        StartupTimingTracker.recordRequest("home_events", 120L)
        val coldMs = StartupTimingTracker.markFirstContentRendered(nowMs = 1380L)

        assertEquals(380L, coldMs)
        val report = StartupTimingTracker.getReport()
        assertEquals(380L, report.coldStartToFirstContentMs)
        assertEquals("home_friends", report.slowestRequestName)
        assertEquals(435L, report.slowestRequestMs)
        assertEquals(3, report.timingsMs.size)
        StartupTimingTracker.resetForTest()
    }

    @Test
    fun `StartupPrefsStore resolves unauthenticated and onboarding routes deterministically`() {
        assertEquals(
            "welcome",
            StartupPrefsStore.resolveUnauthenticatedRoute(
                LocalStartupSnapshot(onboardingDone = false, privacyAccepted = false, lastUid = null)
            )
        )
        assertEquals(
            "auth",
            StartupPrefsStore.resolveUnauthenticatedRoute(
                LocalStartupSnapshot(onboardingDone = false, privacyAccepted = true, lastUid = null)
            )
        )

        val incompleteName = UserProfile(uid = "u1", displayName = "", username = "")
        assertEquals("onboarding_profile", StartupPrefsStore.resolveOnboardingRoute(incompleteName))

        val incompleteAge = UserProfile(uid = "u1", displayName = "Alex", username = "alex")
        assertEquals("onboarding_age", StartupPrefsStore.resolveOnboardingRoute(incompleteAge))

        val readyHobbies = UserProfile(
            uid = "u1",
            displayName = "Alex",
            username = "alex",
            age = 24,
            gender = "Non-binary",
            country = "Canada",
            nativeLanguage = "English",
            learningLanguage = "Spanish"
        )
        assertEquals("onboarding_hobbies", StartupPrefsStore.resolveOnboardingRoute(readyHobbies))
        assertTrue(ErrorMapper.isNetworkException(SocketTimeoutException("timeout")))
        assertNull(StartupTimingTracker.getReport().slowestRequestName)
    }
}
