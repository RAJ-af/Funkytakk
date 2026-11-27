package com.example.diagnostics

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.ActiveUserManager
import com.example.BuildConfig
import com.example.ModerationManager
import com.example.SupabaseSessionManager
import com.example.TelemetryManager
import com.example.crypto.VaultSecurityManager
import com.example.ui.components.AppNetworkManager
import com.example.ui.components.SoftCloseIcon
import com.example.ui.components.SoftWarningIcon
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

data class RecordedIssue(
    val id: String,
    val timestampMs: Long,
    val severity: String, // "CRASH", "ERROR", "SLOW_UI"
    val source: String,
    val message: String,
    val stackTrace: String,
    val screen: String,
    val fullMessage: String = message,
    val fullStackTrace: String = stackTrace,
    val causeChain: String = "",
    val threadName: String = "",
    val uptimeMs: Long = 0L
)

data class DiagnosticBreadcrumb(
    val timestampMs: Long,
    val category: String,
    val detail: String
)

/**
 * Global in-app Error Finder, Main-Thread Stall Watchdog, and Crash Reporter.
 * Strictly redacts auth tokens, JWTs, keys, emails, and message plaintext before persistence.
 * Includes an Admin-Only Verbose Mode verified against public.user_roles for full cause chains,
 * breadcrumbs, and runtime telemetry.
 */
object CrashAndErrorReporter {
    const val SUPPORT_EMAIL = "heyitsraj4@gmail.com"
    private const val TAG = "CrashAndErrorReporter"
    private const val PREFS_NAME = "funky_crash_reporter_prefs"
    private const val KEY_ISSUES_JSON = "recorded_issues_v1"
    private const val MAX_ISSUES = 35
    private const val MAX_BREADCRUMBS = 50
    private const val STALL_THRESHOLD_MS = 2600L

    private val installed = AtomicBoolean(false)
    private val uncaughtCrashInProgress = AtomicBoolean(false)
    private val processStartedAtElapsedMs = SystemClock.elapsedRealtime()

    @Volatile
    private var isAppInForeground = true
    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var previousHandler: Thread.UncaughtExceptionHandler? = null
    @Volatile
    private var lastStallRecordedAt = 0L
    @Volatile
    private var lastBannerShownAt = 0L
    @Volatile
    private var verifiedAdminUid: String? = null

    private val _issues = MutableStateFlow<List<RecordedIssue>>(emptyList())
    val issues: StateFlow<List<RecordedIssue>> = _issues.asStateFlow()

    private val _pendingBannerIssue = MutableStateFlow<RecordedIssue?>(null)
    val pendingBannerIssue: StateFlow<RecordedIssue?> = _pendingBannerIssue.asStateFlow()

    private val _showReportDialog = MutableStateFlow(false)
    val showReportDialog: StateFlow<Boolean> = _showReportDialog.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isCheckingAdmin = MutableStateFlow(false)
    val isCheckingAdmin: StateFlow<Boolean> = _isCheckingAdmin.asStateFlow()

    private val _isVerboseMode = MutableStateFlow(false)
    val isVerboseMode: StateFlow<Boolean> = _isVerboseMode.asStateFlow()

    private val breadcrumbLock = Any()
    private val breadcrumbBuffer = ArrayDeque<DiagnosticBreadcrumb>(MAX_BREADCRUMBS)
    private val _breadcrumbs = MutableStateFlow<List<DiagnosticBreadcrumb>>(emptyList())
    val breadcrumbs: StateFlow<List<DiagnosticBreadcrumb>> = _breadcrumbs.asStateFlow()

    private val jwtRegex = Regex("""eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{4,}""")
    private val bearerRegex = Regex("""(?i)Bearer\s+[A-Za-z0-9._-]{12,}""")
    private val emailRegex = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
    private val keyFieldRegex = Regex(
        """(?i)(media_key|media_nonce|identity_private_key|ciphertext|token|refresh_token|access_token|apikey|api_key|authorization|secret|password)\s*[:=]\s*["']?[^,\s"'}&]+["']?"""
    )

    fun install(application: Application) {
        appContext = application.applicationContext
        if (!installed.compareAndSet(false, true)) return

        loadStoredIssues()
        recordBreadcrumb("APP", "Installed diagnostics (v${BuildConfig.VERSION_NAME}, ${if (BuildConfig.DEBUG) "debug" else "release"})")

        Handler(Looper.getMainLooper()).post {
            runCatching {
                ProcessLifecycleOwner.get().lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                    override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                        isAppInForeground = true
                    }

                    override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                        isAppInForeground = false
                    }
                })
            }
        }

        // If previous run crashed and left an unacknowledged crash, surface the banner cleanly
        _issues.value.firstOrNull { it.severity == "CRASH" }?.let { latestCrash ->
            val recent = (System.currentTimeMillis() - latestCrash.timestampMs) < 24 * 60 * 60 * 1000L
            if (recent && !isBannerDismissedFor(latestCrash.id)) {
                _pendingBannerIssue.value = latestCrash
            }
        }

        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Prevent MainThreadWatchdog from logging a false SLOW_UI while Crashlytics flushes on main thread
            uncaughtCrashInProgress.set(true)
            try {
                recordCrashSynchronously(thread, throwable)
            } catch (_: Throwable) {
            } finally {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }

        startMainThreadStallWatchdog()
    }

    private fun startMainThreadStallWatchdog() {
        val mainHandler = Handler(Looper.getMainLooper())
        val worker = Thread({
            while (!Thread.currentThread().isInterrupted && !uncaughtCrashInProgress.get()) {
                try {
                    Thread.sleep(3500L)
                    if (uncaughtCrashInProgress.get()) return@Thread
                    if (!isAppInForeground) continue

                    val responded = AtomicBoolean(false)
                    val sentAt = System.currentTimeMillis()
                    mainHandler.post { responded.set(true) }

                    var waited = 0L
                    while (!responded.get() && waited < STALL_THRESHOLD_MS) {
                        if (uncaughtCrashInProgress.get()) return@Thread
                        Thread.sleep(250L)
                        waited += 250L
                    }

                    val now = System.currentTimeMillis()
                    if (!responded.get() &&
                        !uncaughtCrashInProgress.get() &&
                        (now - lastStallRecordedAt) > 45_000L
                    ) {
                        val mainStack = Looper.getMainLooper().thread.stackTrace
                            .take(24)
                            .joinToString("\n  at ") { it.toString() }
                        // Ignore Crashlytics/UncaughtExceptionHandler teardown waits on fatal crash
                        val isCrashTeardown = mainStack.contains("CrashlyticsController.handleUncaughtException") ||
                            mainStack.contains("CrashlyticsUncaughtExceptionHandler") ||
                            mainStack.contains("CrashAndErrorReporter")
                        if (!isCrashTeardown) {
                            lastStallRecordedAt = now
                            recordIssueInternal(
                                severity = "SLOW_UI",
                                source = "MainThreadWatchdog",
                                message = "UI thread paused for >${now - sentAt}ms",
                                stackTrace = mainStack,
                                causeChain = "",
                                threadName = Looper.getMainLooper().thread.name,
                                showBanner = false,
                                syncWrite = false
                            )
                        }
                    }
                } catch (_: InterruptedException) {
                    return@Thread
                } catch (_: Throwable) {
                }
            }
        }, "FT-UI-Watchdog")
        worker.isDaemon = true
        worker.start()
    }

    private fun safeStackTraceString(throwable: Throwable): String {
        // Android's Log.getStackTraceString explicitly returns "" when UnknownHostException is in the cause chain.
        return Log.getStackTraceString(throwable).ifBlank {
            throwable.stackTraceToString()
        }
    }

    private fun extractCauseChain(throwable: Throwable?): String {
        if (throwable == null) return ""
        val parts = mutableListOf<String>()
        var current: Throwable? = throwable
        var depth = 0
        while (current != null && depth < 8) {
            val name = current.javaClass.name
            val msg = current.message?.take(220).orEmpty()
            parts.add(if (msg.isNotBlank()) "$name: $msg" else name)
            current = current.cause
            depth++
        }
        return sanitize(parts.joinToString(" -> "))
    }

    private fun recordCrashSynchronously(thread: Thread, throwable: Throwable) {
        val trace = safeStackTraceString(throwable)
        recordIssueInternal(
            severity = "CRASH",
            source = "Uncaught(${thread.name})",
            message = throwable.message ?: throwable.javaClass.simpleName,
            stackTrace = trace,
            causeChain = extractCauseChain(throwable),
            threadName = thread.name,
            showBanner = true,
            syncWrite = true
        )
    }

    fun recordNonFatal(
        source: String,
        message: String,
        throwable: Throwable? = null,
        showBanner: Boolean = false
    ) {
        val trace = throwable?.let { safeStackTraceString(it) } ?: ""
        recordIssueInternal(
            severity = "ERROR",
            source = source,
            message = message,
            stackTrace = trace,
            causeChain = extractCauseChain(throwable),
            threadName = Thread.currentThread().name,
            showBanner = showBanner,
            syncWrite = false
        )
    }

    fun recordBreadcrumb(category: String, detail: String) {
        val cleanCategory = sanitize(category).take(18).uppercase(Locale.US)
        val cleanDetail = sanitize(detail).take(160)
        if (cleanCategory.isBlank() || cleanDetail.isBlank()) return
        val item = DiagnosticBreadcrumb(
            timestampMs = System.currentTimeMillis(),
            category = cleanCategory,
            detail = cleanDetail
        )
        synchronized(breadcrumbLock) {
            if (breadcrumbBuffer.size >= MAX_BREADCRUMBS) {
                breadcrumbBuffer.removeFirst()
            }
            breadcrumbBuffer.addLast(item)
            _breadcrumbs.value = breadcrumbBuffer.toList()
        }
    }

    private fun recordIssueInternal(
        severity: String,
        source: String,
        message: String,
        stackTrace: String,
        causeChain: String,
        threadName: String,
        showBanner: Boolean,
        syncWrite: Boolean
    ) {
        val cleanSource = sanitize(source).take(80)
        val sanitizedMsg = sanitize(message)
        val cleanMessage = sanitizedMsg.take(280)
        val cleanFullMessage = sanitizedMsg.take(1200)
        val sanitizedLines = sanitize(stackTrace).lines()
        val cleanTrace = sanitizedLines.take(28).joinToString("\n")
        val cleanFullTrace = sanitizedLines.take(120).joinToString("\n")
        val cleanCause = sanitize(causeChain).take(500)
        val cleanThread = sanitize(threadName).take(48)
        val screenName = runCatching { TelemetryManager.currentScreenName() }.getOrNull().orEmpty()
        val uptimeMs = (SystemClock.elapsedRealtime() - processStartedAtElapsedMs).coerceAtLeast(0L)

        val issue = RecordedIssue(
            id = UUID.randomUUID().toString(),
            timestampMs = System.currentTimeMillis(),
            severity = severity,
            source = cleanSource,
            message = cleanMessage,
            stackTrace = cleanTrace,
            screen = screenName,
            fullMessage = cleanFullMessage,
            fullStackTrace = cleanFullTrace,
            causeChain = cleanCause,
            threadName = cleanThread,
            uptimeMs = uptimeMs
        )
        val updated = (listOf(issue) + _issues.value).take(MAX_ISSUES)
        _issues.value = updated
        persistIssues(updated, syncWrite)
        recordBreadcrumb("ISSUE", "[$severity] $cleanSource: ${cleanMessage.take(70)}")

        if (showBanner) {
            val now = System.currentTimeMillis()
            if (severity == "CRASH" || (now - lastBannerShownAt) > 12_000L) {
                lastBannerShownAt = now
                _pendingBannerIssue.value = issue
            }
        }
    }

    fun onActiveUserChanged(uid: String?) {
        val normalized = uid?.takeIf { it.isNotBlank() }
        if (normalized == null || normalized != verifiedAdminUid) {
            resetAdminState()
        }
    }

    fun resetAdminState() {
        verifiedAdminUid = null
        _isAdmin.value = false
        _isVerboseMode.value = false
        _isCheckingAdmin.value = false
    }

    suspend fun refreshAdminAccess(context: Context): Boolean {
        val currentUid = ActiveUserManager.getActiveUid(context)
            ?: SupabaseSessionManager.getUserId(context)
        if (currentUid.isNullOrBlank()) {
            resetAdminState()
            return false
        }
        if (verifiedAdminUid != null && verifiedAdminUid != currentUid) {
            resetAdminState()
        }
        _isCheckingAdmin.value = true
        return try {
            val allowed = withContext(Dispatchers.IO) {
                ModerationManager.checkIsAdmin(context.applicationContext)
            }
            val uidAfterCheck = ActiveUserManager.getActiveUid(context)
                ?: SupabaseSessionManager.getUserId(context)
            val verified = allowed && uidAfterCheck == currentUid
            verifiedAdminUid = if (verified) currentUid else null
            _isAdmin.value = verified
            if (!verified) {
                _isVerboseMode.value = false
            }
            verified
        } catch (_: Exception) {
            resetAdminState()
            false
        } finally {
            _isCheckingAdmin.value = false
        }
    }

    fun setVerboseMode(enabled: Boolean) {
        if (!_isAdmin.value) {
            _isVerboseMode.value = false
            return
        }
        _isVerboseMode.value = enabled
        recordBreadcrumb("DIAG", "Admin verbose mode ${if (enabled) "enabled" else "disabled"}")
    }

    @androidx.annotation.VisibleForTesting
    internal fun setAdminAccessForTesting(isAdmin: Boolean) {
        _isAdmin.value = isAdmin
        if (!isAdmin) {
            _isVerboseMode.value = false
        }
    }

    fun dismissBanner() {
        val current = _pendingBannerIssue.value
        if (current != null) {
            appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()?.putString("dismissed_id", current.id)?.apply()
        }
        _pendingBannerIssue.value = null
    }

    fun openReportDialog() {
        _showReportDialog.value = true
    }

    fun closeReportDialog() {
        _showReportDialog.value = false
    }

    fun clearIssues() {
        _issues.value = emptyList()
        _pendingBannerIssue.value = null
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.remove(KEY_ISSUES_JSON)?.apply()
    }

    fun buildRuntimeSnapshotLines(): List<String> {
        val rt = Runtime.getRuntime()
        val usedMb = ((rt.totalMemory() - rt.freeMemory()) / (1024.0 * 1024.0))
        val maxMb = (rt.maxMemory() / (1024.0 * 1024.0))
        val uptimeSec = ((SystemClock.elapsedRealtime() - processStartedAtElapsedMs) / 1000L).coerceAtLeast(0L)
        val abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty().ifBlank { "unknown" }
        val online = runCatching { AppNetworkManager.isOnline.value }.getOrDefault(false)
        val vault = runCatching { VaultSecurityManager.vaultStatus.value.name }.getOrDefault("UNKNOWN")
        val screen = runCatching { TelemetryManager.currentScreenName() }.getOrNull() ?: "unknown"

        return listOf(
            "Build: ${if (BuildConfig.DEBUG) "debug" else "release"} | ABI: $abi | Threads: ${Thread.activeCount()}",
            String.format(Locale.US, "Heap: %.1f / %.1f MB | Uptime: %ds", usedMb, maxMb, uptimeSec),
            "Network: ${if (online) "online" else "offline"} | Vault: $vault | Screen: $screen",
            "Fingerprint: ${sanitize(Build.FINGERPRINT)}"
        )
    }

    fun buildDiagnosticsReportText(
        context: Context,
        userNotes: String = "",
        verbose: Boolean = _isVerboseMode.value
    ): String {
        val useVerbose = verbose && _isAdmin.value
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val sb = StringBuilder()
        sb.appendLine(if (useVerbose) "FunkyTalk Diagnostic Report [ADMIN VERBOSE MODE]" else "FunkyTalk Diagnostic Report")
        sb.appendLine("Generated: ${fmt.format(Date())}")
        sb.appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})")
        sb.appendLine("Current Screen: ${runCatching { TelemetryManager.currentScreenName() }.getOrNull() ?: "unknown"}")

        if (useVerbose) {
            sb.appendLine()
            sb.appendLine("Runtime & Environment (Admin Verbose):")
            buildRuntimeSnapshotLines().forEach { sb.appendLine("- $it") }
        }

        if (userNotes.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("User Notes:")
            sb.appendLine(sanitize(userNotes.trim()))
        }

        if (useVerbose) {
            val crumbs = _breadcrumbs.value.takeLast(40)
            if (crumbs.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("Recent Breadcrumbs (${crumbs.size}):")
                crumbs.forEach { crumb ->
                    sb.appendLine("${fmt.format(Date(crumb.timestampMs))} [${crumb.category}] ${crumb.detail}")
                }
            }
        }

        sb.appendLine()
        val list = _issues.value
        if (list.isEmpty()) {
            sb.appendLine("No recorded crashes or errors.")
        } else {
            val displayList = if (useVerbose) list else list.take(15)
            sb.appendLine("Recent Issues (${displayList.size}/${list.size}):")
            displayList.forEachIndexed { idx, issue ->
                sb.appendLine("---- #${idx + 1} [${issue.severity}] ${fmt.format(Date(issue.timestampMs))} ----")
                sb.appendLine("Source: ${issue.source}")
                if (issue.screen.isNotBlank()) sb.appendLine("Screen: ${issue.screen}")
                if (useVerbose) {
                    if (issue.threadName.isNotBlank()) sb.appendLine("Thread: ${issue.threadName}")
                    if (issue.uptimeMs > 0L) sb.appendLine("Uptime: ${issue.uptimeMs}ms")
                    if (issue.causeChain.isNotBlank()) sb.appendLine("Cause Chain: ${issue.causeChain}")
                    sb.appendLine("Message: ${issue.fullMessage.ifBlank { issue.message }}")
                    val trace = issue.fullStackTrace.ifBlank { issue.stackTrace }
                    if (trace.isNotBlank()) {
                        sb.appendLine("Full Trace:")
                        sb.appendLine(trace)
                    }
                } else {
                    sb.appendLine("Message: ${issue.message}")
                    if (issue.stackTrace.isNotBlank()) {
                        sb.appendLine("Trace:")
                        sb.appendLine(issue.stackTrace)
                    }
                }
            }
        }
        return sb.toString()
    }

    fun sendReportViaEmail(
        context: Context,
        userNotes: String = "",
        verbose: Boolean = _isVerboseMode.value
    ) {
        val body = buildDiagnosticsReportText(context, userNotes, verbose = verbose)
        val modeTag = if (verbose && _isAdmin.value) " [Verbose]" else ""
        val subject = "FunkyTalk Error Report$modeTag (v${BuildConfig.VERSION_NAME} / Android ${Build.VERSION.RELEASE})"
        val maxEmailChars = if (verbose && _isAdmin.value) 64_000 else 24_000

        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body.take(maxEmailChars))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (emailIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(Intent.createChooser(emailIntent, "Send error report").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return
            }
        } catch (_: Exception) {
        }

        try {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body.take(maxEmailChars))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Send error report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            copyReportToClipboard(context, body)
            Toast.makeText(
                context,
                "No email app found — report copied. Send to $SUPPORT_EMAIL",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun copyReportToClipboard(
        context: Context,
        text: String = buildDiagnosticsReportText(context)
    ) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("FunkyTalk Diagnostics", text))
            Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
        }
    }

    private fun sanitize(input: String): String {
        return input
            .replace(bearerRegex, "Bearer [REDACTED]")
            .replace(jwtRegex, "[JWT_REDACTED]")
            .replace(emailRegex, "[EMAIL_REDACTED]")
            .replace(keyFieldRegex) { match -> "${match.groupValues[1]}=[REDACTED]" }
    }

    private fun isBannerDismissedFor(id: String): Boolean {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return false
        return prefs.getString("dismissed_id", null) == id
    }

    private fun persistIssues(list: List<RecordedIssue>, syncWrite: Boolean) {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        try {
            val arr = JSONArray()
            list.forEach { issue ->
                arr.put(
                    JSONObject().apply {
                        put("id", issue.id)
                        put("ts", issue.timestampMs)
                        put("sev", issue.severity)
                        put("src", issue.source)
                        put("msg", issue.message)
                        put("tr", issue.stackTrace)
                        put("scr", issue.screen)
                        put("fmsg", issue.fullMessage)
                        put("ftr", issue.fullStackTrace)
                        put("cc", issue.causeChain)
                        put("thr", issue.threadName)
                        put("up", issue.uptimeMs)
                    }
                )
            }
            val editor = prefs.edit().putString(KEY_ISSUES_JSON, arr.toString())
            if (syncWrite) editor.commit() else editor.apply()
        } catch (_: Exception) {
        }
    }

    private fun loadStoredIssues() {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        try {
            val raw = prefs.getString(KEY_ISSUES_JSON, null) ?: return
            val arr = JSONArray(raw)
            val parsed = buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val msg = o.optString("msg", "")
                    val tr = o.optString("tr", "")
                    add(
                        RecordedIssue(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            timestampMs = o.optLong("ts", 0L),
                            severity = o.optString("sev", "ERROR"),
                            source = o.optString("src", "App"),
                            message = msg,
                            stackTrace = tr,
                            screen = o.optString("scr", ""),
                            fullMessage = o.optString("fmsg", "").ifBlank { msg },
                            fullStackTrace = o.optString("ftr", "").ifBlank { tr },
                            causeChain = o.optString("cc", ""),
                            threadName = o.optString("thr", ""),
                            uptimeMs = o.optLong("up", 0L)
                        )
                    )
                }
            }
            _issues.value = parsed
        } catch (_: Exception) {
        }
    }
}

@Composable
fun ErrorReportBannerAndDialogHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bannerIssue by CrashAndErrorReporter.pendingBannerIssue.collectAsState()
    val showDialog by CrashAndErrorReporter.showReportDialog.collectAsState()
    val issues by CrashAndErrorReporter.issues.collectAsState()
    val isAdmin by CrashAndErrorReporter.isAdmin.collectAsState()
    val isVerboseMode by CrashAndErrorReporter.isVerboseMode.collectAsState()
    val breadcrumbs by CrashAndErrorReporter.breadcrumbs.collectAsState()
    val funkyColors = LocalFunkyColors.current

    LaunchedEffect(showDialog) {
        if (showDialog) {
            CrashAndErrorReporter.refreshAdminAccess(context)
        }
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = bannerIssue != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            val issue = bannerIssue ?: return@AnimatedVisibility
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (funkyColors.isDark) Color(0xFF2B1618) else Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .testTag("error_reporter_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftWarningIcon(size = 18.dp, color = Color(0xFFEF4444))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (issue.severity == "CRASH") "Previous crash detected" else "App issue detected",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (funkyColors.isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                        )
                        Text(
                            text = "${issue.source}: ${issue.message}".ifBlank { "Tap to send diagnostic report" },
                            fontSize = 11.5.sp,
                            color = if (funkyColors.isDark) Color(0xFFFECACA) else Color(0xFFB91C1C),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .minimumInteractiveComponentSize()
                            .clickable {
                                CrashAndErrorReporter.dismissBanner()
                                CrashAndErrorReporter.openReportDialog()
                            }
                            .testTag("error_banner_send_report_button")
                    ) {
                        Text(
                            text = "Report",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                    IconButton(
                        onClick = { CrashAndErrorReporter.dismissBanner() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("error_banner_dismiss_button")
                    ) {
                        SoftCloseIcon(
                            size = 14.dp,
                            color = if (funkyColors.isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        var userNotes by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf("ALL") }
        var expandedIssueIds by remember { mutableStateOf<Set<String>>(emptySet()) }
        var showRuntimePanel by remember { mutableStateOf(false) }
        val fmt = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US) }

        val effectiveVerbose = isAdmin && isVerboseMode
        val filteredIssues = remember(issues, selectedFilter, effectiveVerbose) {
            if (!effectiveVerbose || selectedFilter == "ALL") issues
            else issues.filter { it.severity == selectedFilter }
        }
        val runtimeSnapshot = remember(showDialog, effectiveVerbose, issues.size) {
            if (effectiveVerbose) CrashAndErrorReporter.buildRuntimeSnapshotLines() else emptyList()
        }

        Dialog(
            onDismissRequest = { CrashAndErrorReporter.closeReportDialog() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = funkyColors.surface,
                border = BorderStroke(1.dp, funkyColors.line),
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 560.dp)
                    .fillMaxHeight(0.86f)
                    .testTag("error_reporter_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Error Finder & Diagnostics",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = funkyColors.ink
                            )
                            Text(
                                text = if (effectiveVerbose) {
                                    "Admin Verbose active — full stack traces & breadcrumbs"
                                } else {
                                    "Sends redacted logs to ${CrashAndErrorReporter.SUPPORT_EMAIL}"
                                },
                                fontSize = 12.sp,
                                color = if (effectiveVerbose) funkyColors.mango else funkyColors.mute
                            )
                        }
                        IconButton(
                            onClick = { CrashAndErrorReporter.closeReportDialog() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            SoftCloseIcon(size = 18.dp, color = funkyColors.ink)
                        }
                    }

                    // Admin-Only Verbose Mode Toggle (never shown or enabled for non-admins)
                    AnimatedVisibility(visible = isAdmin) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (effectiveVerbose) funkyColors.mango.copy(alpha = 0.14f) else funkyColors.soft,
                            border = BorderStroke(
                                1.dp,
                                if (effectiveVerbose) funkyColors.mango.copy(alpha = 0.55f) else funkyColors.line
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("error_finder_admin_panel")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = funkyColors.mango
                                        ) {
                                            Text(
                                                text = "ADMIN",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = funkyColors.onMango,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "Verbose Mode",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = funkyColors.ink
                                        )
                                    }
                                    Text(
                                        text = "Full cause chains, thread info, memory & nav breadcrumbs",
                                        fontSize = 11.sp,
                                        color = funkyColors.mute
                                    )
                                }
                                Switch(
                                    checked = isVerboseMode,
                                    onCheckedChange = { CrashAndErrorReporter.setVerboseMode(it) },
                                    modifier = Modifier.testTag("error_finder_verbose_switch")
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = userNotes,
                        onValueChange = { userNotes = it },
                        placeholder = {
                            Text(
                                "Optional: describe what happened (e.g. voice room lag, reaction didn't send)",
                                fontSize = 12.5.sp,
                                color = funkyColors.mute
                            )
                        },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("error_report_notes_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (effectiveVerbose) {
                        val crashCount = remember(issues) { issues.count { it.severity == "CRASH" } }
                        val errorCount = remember(issues) { issues.count { it.severity == "ERROR" } }
                        val slowCount = remember(issues) { issues.count { it.severity == "SLOW_UI" } }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                "ALL" to "All (${issues.size})",
                                "CRASH" to "Crash ($crashCount)",
                                "ERROR" to "Error ($errorCount)",
                                "SLOW_UI" to "Slow UI ($slowCount)"
                            ).forEach { (key, label) ->
                                val selected = selectedFilter == key
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedFilter = key },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    modifier = Modifier.testTag("error_finder_filter_${key.lowercase(Locale.US)}")
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (issues.isEmpty()) "Recent Events (Clean — no errors recorded)" else "Recent Events (${issues.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(funkyColors.bg)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (effectiveVerbose) {
                            item(key = "admin_runtime_panel") {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = funkyColors.surface,
                                    border = BorderStroke(1.dp, funkyColors.mango.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showRuntimePanel = !showRuntimePanel }
                                        .testTag("error_finder_runtime_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Runtime Snapshot & Breadcrumbs (${breadcrumbs.size})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = funkyColors.mango
                                            )
                                            Text(
                                                text = if (showRuntimePanel) "Hide" else "Show",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = funkyColors.mute
                                            )
                                        }
                                        if (showRuntimePanel) {
                                            runtimeSnapshot.forEach { line ->
                                                Text(
                                                    text = line,
                                                    fontSize = 10.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = funkyColors.ink
                                                )
                                            }
                                            if (breadcrumbs.isNotEmpty()) {
                                                HorizontalDivider(
                                                    color = funkyColors.line,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                                Text(
                                                    text = "Latest Breadcrumbs:",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = funkyColors.ink
                                                )
                                                breadcrumbs.takeLast(12).reversed().forEach { crumb ->
                                                    Text(
                                                        text = "${fmt.format(Date(crumb.timestampMs))} [${crumb.category}] ${crumb.detail}",
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = funkyColors.mute
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredIssues.isEmpty()) {
                            item {
                                Text(
                                    text = if (issues.isEmpty()) {
                                        "No crashes, UI stalls, or network/E2EE errors recorded in this session. You can still send device diagnostics if something feels slow."
                                    } else {
                                        "No issues match the selected filter."
                                    },
                                    fontSize = 12.5.sp,
                                    color = funkyColors.mute,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            items(filteredIssues, key = { it.id }) { issue ->
                                val isExpanded = effectiveVerbose && expandedIssueIds.contains(issue.id)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = funkyColors.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isExpanded) funkyColors.mango.copy(alpha = 0.55f) else funkyColors.line
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .then(
                                            if (effectiveVerbose) {
                                                Modifier.clickable {
                                                    expandedIssueIds = if (isExpanded) {
                                                        expandedIssueIds - issue.id
                                                    } else {
                                                        expandedIssueIds + issue.id
                                                    }
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .testTag("error_finder_issue_card_${issue.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "[${issue.severity}] ${issue.source}",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (issue.severity) {
                                                    "CRASH" -> Color(0xFFEF4444)
                                                    "SLOW_UI" -> Color(0xFFD97706)
                                                    else -> funkyColors.mango
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = fmt.format(Date(issue.timestampMs)),
                                                fontSize = 10.5.sp,
                                                color = funkyColors.mute
                                            )
                                        }
                                        if (effectiveVerbose) {
                                            val metaParts = buildList {
                                                if (issue.screen.isNotBlank()) add("screen=${issue.screen}")
                                                if (issue.threadName.isNotBlank()) add("thread=${issue.threadName}")
                                                if (issue.uptimeMs > 0L) add("uptime=${issue.uptimeMs}ms")
                                            }
                                            if (metaParts.isNotEmpty()) {
                                                Text(
                                                    text = metaParts.joinToString(" • "),
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = funkyColors.mute
                                                )
                                            }
                                            if (issue.causeChain.isNotBlank()) {
                                                Text(
                                                    text = "Cause: ${issue.causeChain}",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFEF4444),
                                                    maxLines = if (isExpanded) 8 else 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (effectiveVerbose) issue.fullMessage.ifBlank { issue.message } else issue.message,
                                            fontSize = 12.sp,
                                            color = funkyColors.ink,
                                            maxLines = if (isExpanded) 12 else 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val traceText = if (effectiveVerbose) {
                                            issue.fullStackTrace.ifBlank { issue.stackTrace }
                                        } else {
                                            issue.stackTrace
                                        }
                                        if (traceText.isNotBlank()) {
                                            Text(
                                                text = if (isExpanded) {
                                                    traceText
                                                } else {
                                                    traceText.lines().take(if (effectiveVerbose) 5 else 3).joinToString("\n")
                                                },
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = funkyColors.mute,
                                                maxLines = if (isExpanded) 120 else if (effectiveVerbose) 5 else 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (effectiveVerbose) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isExpanded) "Tap card to collapse" else "Tap card for full trace",
                                                    fontSize = 10.sp,
                                                    color = funkyColors.mango,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                TextButton(
                                                    onClick = {
                                                        val singleText = buildString {
                                                            appendLine("[${issue.severity}] ${issue.source}")
                                                            if (issue.screen.isNotBlank()) appendLine("Screen: ${issue.screen}")
                                                            if (issue.threadName.isNotBlank()) appendLine("Thread: ${issue.threadName}")
                                                            if (issue.causeChain.isNotBlank()) appendLine("Cause: ${issue.causeChain}")
                                                            appendLine("Message: ${issue.fullMessage.ifBlank { issue.message }}")
                                                            if (traceText.isNotBlank()) appendLine(traceText)
                                                        }
                                                        CrashAndErrorReporter.copyReportToClipboard(context, singleText)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.heightIn(min = 36.dp)
                                                ) {
                                                    Text("Copy Trace", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                CrashAndErrorReporter.copyReportToClipboard(
                                    context,
                                    CrashAndErrorReporter.buildDiagnosticsReportText(
                                        context = context,
                                        userNotes = userNotes,
                                        verbose = effectiveVerbose
                                    )
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("error_report_copy_button")
                        ) {
                            Text("Copy", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (issues.isNotEmpty()) {
                            TextButton(
                                onClick = { CrashAndErrorReporter.clearIssues() },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("error_report_clear_button")
                            ) {
                                Text("Clear", color = funkyColors.mute, fontSize = 13.sp)
                            }
                        }
                        Button(
                            onClick = {
                                CrashAndErrorReporter.sendReportViaEmail(
                                    context = context,
                                    userNotes = userNotes,
                                    verbose = effectiveVerbose
                                )
                                CrashAndErrorReporter.closeReportDialog()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = funkyColors.mango,
                                contentColor = funkyColors.onMango
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("error_report_send_email_button")
                        ) {
                            Text(
                                text = if (effectiveVerbose) "Send Verbose" else "Send Report",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
