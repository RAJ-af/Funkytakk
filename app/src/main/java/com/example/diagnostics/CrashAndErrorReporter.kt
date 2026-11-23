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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.BuildConfig
import com.example.TelemetryManager
import com.example.ui.components.SoftCloseIcon
import com.example.ui.components.SoftWarningIcon
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
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
    val screen: String
)

/**
 * Global in-app Error Finder, Main-Thread Stall Watchdog, and Crash Reporter.
 * Strictly redacts auth tokens, JWTs, keys, emails, and message plaintext before persistence.
 * Sends formatted reports via email intent to heyitsraj4@gmail.com.
 */
object CrashAndErrorReporter {
    const val SUPPORT_EMAIL = "heyitsraj4@gmail.com"
    private const val TAG = "CrashAndErrorReporter"
    private const val PREFS_NAME = "funky_crash_reporter_prefs"
    private const val KEY_ISSUES_JSON = "recorded_issues_v1"
    private const val MAX_ISSUES = 35
    private const val STALL_THRESHOLD_MS = 2600L

    private val installed = AtomicBoolean(false)
    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var previousHandler: Thread.UncaughtExceptionHandler? = null
    @Volatile
    private var lastStallRecordedAt = 0L
    @Volatile
    private var lastBannerShownAt = 0L

    private val _issues = MutableStateFlow<List<RecordedIssue>>(emptyList())
    val issues: StateFlow<List<RecordedIssue>> = _issues.asStateFlow()

    private val _pendingBannerIssue = MutableStateFlow<RecordedIssue?>(null)
    val pendingBannerIssue: StateFlow<RecordedIssue?> = _pendingBannerIssue.asStateFlow()

    private val _showReportDialog = MutableStateFlow(false)
    val showReportDialog: StateFlow<Boolean> = _showReportDialog.asStateFlow()

    private val jwtRegex = Regex("""eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{4,}""")
    private val bearerRegex = Regex("""(?i)Bearer\s+[A-Za-z0-9._-]{12,}""")
    private val emailRegex = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
    private val keyFieldRegex = Regex("""(?i)(media_key|media_nonce|identity_private_key|ciphertext|token|apiKey|password)\s*[:=]\s*["']?[^,\s"'}]+["']?""")

    fun install(application: Application) {
        appContext = application.applicationContext
        if (!installed.compareAndSet(false, true)) return

        loadStoredIssues()

        // If previous run crashed and left an unacknowledged crash, surface the banner cleanly
        _issues.value.firstOrNull { it.severity == "CRASH" }?.let { latestCrash ->
            val recent = (System.currentTimeMillis() - latestCrash.timestampMs) < 24 * 60 * 60 * 1000L
            if (recent && !isBannerDismissedFor(latestCrash.id)) {
                _pendingBannerIssue.value = latestCrash
            }
        }

        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
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
            while (!Thread.currentThread().isInterrupted) {
                try {
                    Thread.sleep(3500L)
                    val responded = AtomicBoolean(false)
                    val sentAt = System.currentTimeMillis()
                    mainHandler.post { responded.set(true) }

                    var waited = 0L
                    while (!responded.get() && waited < STALL_THRESHOLD_MS) {
                        Thread.sleep(250L)
                        waited += 250L
                    }

                    val now = System.currentTimeMillis()
                    if (!responded.get() && (now - lastStallRecordedAt) > 45_000L) {
                        lastStallRecordedAt = now
                        val mainStack = Looper.getMainLooper().thread.stackTrace
                            .take(18)
                            .joinToString("\n  at ") { it.toString() }
                        recordIssueInternal(
                            severity = "SLOW_UI",
                            source = "MainThreadWatchdog",
                            message = "UI thread paused for >${now - sentAt}ms",
                            stackTrace = mainStack,
                            showBanner = false,
                            syncWrite = false
                        )
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

    private fun recordCrashSynchronously(thread: Thread, throwable: Throwable) {
        val trace = Log.getStackTraceString(throwable)
        recordIssueInternal(
            severity = "CRASH",
            source = "Uncaught(${thread.name})",
            message = throwable.message ?: throwable.javaClass.simpleName,
            stackTrace = trace,
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
        val trace = throwable?.let { Log.getStackTraceString(it) } ?: ""
        recordIssueInternal(
            severity = "ERROR",
            source = source,
            message = message,
            stackTrace = trace,
            showBanner = showBanner,
            syncWrite = false
        )
    }

    private fun recordIssueInternal(
        severity: String,
        source: String,
        message: String,
        stackTrace: String,
        showBanner: Boolean,
        syncWrite: Boolean
    ) {
        val cleanSource = sanitize(source).take(80)
        val cleanMessage = sanitize(message).take(280)
        val cleanTrace = sanitize(stackTrace).lines().take(28).joinToString("\n")
        val issue = RecordedIssue(
            id = UUID.randomUUID().toString(),
            timestampMs = System.currentTimeMillis(),
            severity = severity,
            source = cleanSource,
            message = cleanMessage,
            stackTrace = cleanTrace,
            screen = runCatching { TelemetryManager.currentScreenName() }.getOrNull().orEmpty()
        )
        val updated = (listOf(issue) + _issues.value).take(MAX_ISSUES)
        _issues.value = updated
        persistIssues(updated, syncWrite)

        if (showBanner) {
            val now = System.currentTimeMillis()
            if (severity == "CRASH" || (now - lastBannerShownAt) > 12_000L) {
                lastBannerShownAt = now
                _pendingBannerIssue.value = issue
            }
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

    fun buildDiagnosticsReportText(context: Context, userNotes: String = ""): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val sb = StringBuilder()
        sb.appendLine("FunkyTalk Diagnostic Report")
        sb.appendLine("Generated: ${fmt.format(Date())}")
        sb.appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})")
        sb.appendLine("Current Screen: ${runCatching { TelemetryManager.currentScreenName() }.getOrNull() ?: "unknown"}")
        if (userNotes.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("User Notes:")
            sb.appendLine(sanitize(userNotes.trim()))
        }
        sb.appendLine()
        val list = _issues.value
        if (list.isEmpty()) {
            sb.appendLine("No recorded crashes or errors.")
        } else {
            sb.appendLine("Recent Issues (${list.size}):")
            list.take(15).forEachIndexed { idx, issue ->
                sb.appendLine("---- #${idx + 1} [${issue.severity}] ${fmt.format(Date(issue.timestampMs))} ----")
                sb.appendLine("Source: ${issue.source}")
                if (issue.screen.isNotBlank()) sb.appendLine("Screen: ${issue.screen}")
                sb.appendLine("Message: ${issue.message}")
                if (issue.stackTrace.isNotBlank()) {
                    sb.appendLine("Trace:")
                    sb.appendLine(issue.stackTrace)
                }
            }
        }
        return sb.toString()
    }

    fun sendReportViaEmail(context: Context, userNotes: String = "") {
        val body = buildDiagnosticsReportText(context, userNotes)
        val subject = "FunkyTalk Error Report (v${BuildConfig.VERSION_NAME} / Android ${Build.VERSION.RELEASE})"

        // Always copy a backup to clipboard if report is large or email app trims body
        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body.take(24_000))
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
                putExtra(Intent.EXTRA_TEXT, body.take(24_000))
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

    fun copyReportToClipboard(context: Context, text: String = buildDiagnosticsReportText(context)) {
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
                    add(
                        RecordedIssue(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            timestampMs = o.optLong("ts", 0L),
                            severity = o.optString("sev", "ERROR"),
                            source = o.optString("src", "App"),
                            message = o.optString("msg", ""),
                            stackTrace = o.optString("tr", ""),
                            screen = o.optString("scr", "")
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
    val funkyColors = LocalFunkyColors.current

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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                    IconButton(
                        onClick = { CrashAndErrorReporter.dismissBanner() },
                        modifier = Modifier
                            .size(28.dp)
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
        val fmt = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US) }

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
                    .widthIn(max = 540.dp)
                    .fillMaxHeight(0.84f)
                    .testTag("error_reporter_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                text = "Sends redacted logs to ${CrashAndErrorReporter.SUPPORT_EMAIL}",
                                fontSize = 12.sp,
                                color = funkyColors.mute
                            )
                        }
                        IconButton(onClick = { CrashAndErrorReporter.closeReportDialog() }) {
                            SoftCloseIcon(size = 18.dp, color = funkyColors.ink)
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
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("error_report_notes_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Text(
                        text = if (issues.isEmpty()) "Recent Events (Clean — no errors recorded)" else "Recent Events (${issues.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.ink
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(funkyColors.bg)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (issues.isEmpty()) {
                            item {
                                Text(
                                    text = "No crashes, UI stalls, or network/E2EE errors recorded in this session. You can still send device diagnostics if something feels slow.",
                                    fontSize = 12.5.sp,
                                    color = funkyColors.mute,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            items(issues, key = { it.id }) { issue ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = funkyColors.surface,
                                    border = BorderStroke(1.dp, funkyColors.line),
                                    modifier = Modifier.fillMaxWidth()
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
                                        Text(
                                            text = issue.message,
                                            fontSize = 12.sp,
                                            color = funkyColors.ink
                                        )
                                        if (issue.stackTrace.isNotBlank()) {
                                            Text(
                                                text = issue.stackTrace.lines().take(3).joinToString("\n"),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = funkyColors.mute,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
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
                            onClick = { CrashAndErrorReporter.copyReportToClipboard(context) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("error_report_copy_button")
                        ) {
                            Text("Copy", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (issues.isNotEmpty()) {
                            TextButton(
                                onClick = { CrashAndErrorReporter.clearIssues() },
                                modifier = Modifier
                                    .height(46.dp)
                                    .testTag("error_report_clear_button")
                            ) {
                                Text("Clear", color = funkyColors.mute, fontSize = 13.sp)
                            }
                        }
                        Button(
                            onClick = {
                                CrashAndErrorReporter.sendReportViaEmail(context, userNotes)
                                CrashAndErrorReporter.closeReportDialog()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = funkyColors.mango,
                                contentColor = funkyColors.onMango
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("error_report_send_email_button")
                        ) {
                            Text("Send Report", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
