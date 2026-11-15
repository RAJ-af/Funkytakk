package com.example.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InboxDrainStats(
    val fetchedCount: Int = 0,
    val savedCount: Int = 0,
    val failedCount: Int = 0,
    val lastDrainTime: Long = 0L,
    val lastFailureReason: String? = null
)

object InboxDrainTracker {
    private val _stats = MutableStateFlow(InboxDrainStats())
    val stats: StateFlow<InboxDrainStats> = _stats.asStateFlow()

    @Synchronized
    fun recordDrainResult(fetched: Int, saved: Int, failed: Int, failureReason: String? = null) {
        val current = _stats.value
        _stats.value = InboxDrainStats(
            fetchedCount = current.fetchedCount + fetched,
            savedCount = current.savedCount + saved,
            failedCount = current.failedCount + failed,
            lastDrainTime = System.currentTimeMillis(),
            lastFailureReason = failureReason ?: current.lastFailureReason
        )
    }

    @Synchronized
    fun setDrainSession(fetched: Int, saved: Int, failed: Int, failureReason: String? = null) {
        val current = _stats.value
        _stats.value = InboxDrainStats(
            fetchedCount = current.fetchedCount + fetched,
            savedCount = current.savedCount + saved,
            failedCount = current.failedCount + failed,
            lastDrainTime = System.currentTimeMillis(),
            lastFailureReason = failureReason ?: current.lastFailureReason
        )
    }

    @Synchronized
    fun reset() {
        _stats.value = InboxDrainStats()
    }
}
