package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.diagnostics.CrashAndErrorReporter
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrashAndErrorReporterTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        CrashAndErrorReporter.resetAdminState()
        CrashAndErrorReporter.clearIssues()
    }

    @After
    fun tearDown() {
        CrashAndErrorReporter.resetAdminState()
        CrashAndErrorReporter.clearIssues()
    }

    @Test
    fun `non-admin cannot enable verbose mode or export verbose diagnostics`() {
        CrashAndErrorReporter.recordBreadcrumb("NAV", "home -> moments")
        CrashAndErrorReporter.recordNonFatal(
            source = "UnitTest",
            message = "Sample failure Bearer secret_token_1234567890",
            throwable = IllegalStateException("Outer", IllegalArgumentException("RootCause"))
        )

        // Attempt to turn on verbose mode without admin privileges
        CrashAndErrorReporter.setVerboseMode(true)
        assertFalse(CrashAndErrorReporter.isAdmin.value)
        assertFalse(CrashAndErrorReporter.isVerboseMode.value)

        val report = CrashAndErrorReporter.buildDiagnosticsReportText(
            context = context,
            verbose = true
        )
        assertFalse("Non-admin report must not include ADMIN VERBOSE MODE header", report.contains("ADMIN VERBOSE MODE"))
        assertFalse("Non-admin report must not include Runtime & Environment dump", report.contains("Runtime & Environment"))
        assertFalse("Non-admin report must not include Recent Breadcrumbs", report.contains("Recent Breadcrumbs"))
        assertFalse("Tokens must always be redacted", report.contains("secret_token_1234567890"))
    }

    @Test
    fun `verified admin can toggle verbose mode and sees redacted verbose report`() {
        CrashAndErrorReporter.recordBreadcrumb("NAV", "splash -> home")
        CrashAndErrorReporter.recordNonFatal(
            source = "MomentsStore",
            message = "Test error user@example.com token=abc123456",
            throwable = ExceptionInInitializerError(IllegalStateException("TypeToken missing"))
        )

        CrashAndErrorReporter.setAdminAccessForTesting(true)
        CrashAndErrorReporter.setVerboseMode(true)
        assertTrue(CrashAndErrorReporter.isAdmin.value)
        assertTrue(CrashAndErrorReporter.isVerboseMode.value)

        val verboseReport = CrashAndErrorReporter.buildDiagnosticsReportText(context)
        assertTrue(verboseReport.contains("[ADMIN VERBOSE MODE]"))
        assertTrue(verboseReport.contains("Runtime & Environment (Admin Verbose):"))
        assertTrue(verboseReport.contains("Recent Breadcrumbs"))
        assertTrue(verboseReport.contains("Cause Chain:"))
        assertTrue(verboseReport.contains("Full Trace:"))
        assertFalse("Emails must remain redacted even in verbose mode", verboseReport.contains("user@example.com"))
        assertFalse("Tokens must remain redacted even in verbose mode", verboseReport.contains("abc123456"))

        // Revoking / logging out immediately disables verbose mode
        CrashAndErrorReporter.resetAdminState()
        assertFalse(CrashAndErrorReporter.isAdmin.value)
        assertFalse(CrashAndErrorReporter.isVerboseMode.value)
    }
}
