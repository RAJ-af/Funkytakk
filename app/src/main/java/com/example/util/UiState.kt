package com.example.util

import kotlinx.coroutines.withTimeout
import java.net.SocketTimeoutException

/**
 * Unified UI state contract across screens to eliminate infinite spinners and blank states.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(
        val data: T,
        val isRefreshing: Boolean = false,
        val isFromCache: Boolean = false
    ) : UiState<T>
    data class Empty(
        val title: String,
        val subtitle: String,
        val actionLabel: String? = null
    ) : UiState<Nothing>
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : UiState<Nothing>
}

const val DEFAULT_SCREEN_LOAD_TIMEOUT_MS = 10_000L

/**
 * Enforces a hard loading timeout (default 10s) so screens transition cleanly to Error(Retry)
 * rather than spinning indefinitely.
 */
suspend fun <T> withLoadingTimeout(
    timeoutMs: Long = DEFAULT_SCREEN_LOAD_TIMEOUT_MS,
    block: suspend () -> T
): T {
    return try {
        withTimeout(timeoutMs) {
            block()
        }
    } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
        throw SocketTimeoutException("Loading timed out after ${timeoutMs}ms")
    }
}
