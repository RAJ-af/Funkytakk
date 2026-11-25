package com.example.util

import android.os.SystemClock
import android.util.Log
import com.example.BuildConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

data class StartupTimingReport(
    val coldStartToFirstContentMs: Long,
    val slowestRequestName: String?,
    val slowestRequestMs: Long,
    val timingsMs: Map<String, Long>
)

/**
 * Debug-only startup performance tracker recording cold start to first content rendered
 * and timing of every startup network request (never logs user content or tokens).
 */
object StartupTimingTracker {
    private const val TAG = "StartupTiming"

    @Volatile
    private var appCreateUptimeMs: Long = 0L

    @Volatile
    private var firstContentUptimeMs: Long = 0L

    private val timings = ConcurrentHashMap<String, Long>()
    private val summaryLogged = AtomicBoolean(false)

    fun markAppCreate(nowMs: Long = SystemClock.uptimeMillis()) {
        if (appCreateUptimeMs == 0L) {
            appCreateUptimeMs = nowMs
        }
    }

    fun markFirstContentRendered(nowMs: Long = SystemClock.uptimeMillis()): Long {
        if (appCreateUptimeMs == 0L) {
            appCreateUptimeMs = nowMs
        }
        if (firstContentUptimeMs == 0L) {
            firstContentUptimeMs = nowMs
        }
        val elapsed = (firstContentUptimeMs - appCreateUptimeMs).coerceAtLeast(0L)
        if (BuildConfig.DEBUG && summaryLogged.compareAndSet(false, true)) {
            runCatching { Log.i(TAG, "Cold start to first content rendered: ${elapsed}ms") }
        }
        return elapsed
    }

    fun recordRequest(name: String, durationMs: Long) {
        if (name.isBlank()) return
        val clamped = durationMs.coerceAtLeast(0L)
        timings[name] = clamped
        if (BuildConfig.DEBUG) {
            runCatching { Log.d(TAG, "Startup call [$name] completed in ${clamped}ms") }
        }
    }

    suspend fun <T> measure(name: String, block: suspend () -> T): T {
        val start = SystemClock.uptimeMillis()
        return try {
            block()
        } finally {
            recordRequest(name, SystemClock.uptimeMillis() - start)
        }
    }

    fun getReport(): StartupTimingReport {
        val coldStart = if (appCreateUptimeMs > 0L && firstContentUptimeMs >= appCreateUptimeMs) {
            firstContentUptimeMs - appCreateUptimeMs
        } else {
            0L
        }
        val snapshot = timings.toMap()
        val slowest = snapshot.maxByOrNull { it.value }
        return StartupTimingReport(
            coldStartToFirstContentMs = coldStart,
            slowestRequestName = slowest?.key,
            slowestRequestMs = slowest?.value ?: 0L,
            timingsMs = snapshot
        )
    }

    fun logSlowestSummary() {
        if (!BuildConfig.DEBUG) return
        val report = getReport()
        val slowestLabel = report.slowestRequestName ?: "none"
        runCatching {
            Log.i(
                TAG,
                "Startup timing summary: firstContent=${report.coldStartToFirstContentMs}ms, " +
                    "slowest=$slowestLabel (${report.slowestRequestMs}ms), trackedCalls=${report.timingsMs.size}"
            )
        }
    }

    fun resetForTest() {
        appCreateUptimeMs = 0L
        firstContentUptimeMs = 0L
        timings.clear()
        summaryLogged.set(false)
    }
}
