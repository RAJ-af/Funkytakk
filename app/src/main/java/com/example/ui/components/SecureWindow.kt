package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Tracks active [SecureWindow] scopes per Window and coordinates with Activity pause/resume on
 * Android < 33 so overlapping secure dialogs/screens do not prematurely strip FLAG_SECURE.
 */
object SecureWindowRegistry {
    private val windowTokens = IdentityHashMap<Window, MutableSet<Any>>()
    private val pausedPre33Activities = Collections.newSetFromMap(IdentityHashMap<Window, Boolean>())

    @Synchronized
    fun acquire(window: Window, token: Any) {
        val set = windowTokens.getOrPut(window) { Collections.newSetFromMap(IdentityHashMap()) }
        set.add(token)
        applyFlag(window)
    }

    @Synchronized
    fun release(window: Window, token: Any) {
        val set = windowTokens[window]
        set?.remove(token)
        if (set != null && set.isEmpty()) {
            windowTokens.remove(window)
        }
        applyFlag(window)
    }

    @Synchronized
    fun setActivityPaused(activity: Activity, paused: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        val window = activity.window ?: return
        if (paused) {
            pausedPre33Activities.add(window)
        } else {
            pausedPre33Activities.remove(window)
        }
        applyFlag(window)
    }

    @Synchronized
    fun isSecureActiveForTest(window: Window): Boolean {
        return !windowTokens[window].isNullOrEmpty() || pausedPre33Activities.contains(window)
    }

    private fun applyFlag(window: Window) {
        val shouldSecure = !windowTokens[window].isNullOrEmpty() || pausedPre33Activities.contains(window)
        if (shouldSecure) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Sets [WindowManager.LayoutParams.FLAG_SECURE] on the host Activity window (and any enclosing
 * Dialog/BottomSheet window) while [enabled] is true and this composable is in composition,
 * clearing it cleanly when leaving composition (unless another [SecureWindow] remains active).
 */
@Composable
fun SecureWindow(enabled: Boolean = true) {
    val context = LocalContext.current
    val view = LocalView.current
    val token = remember { Any() }

    val activityWindow = remember(context) { context.findActivity()?.window }
    val dialogWindow = remember(view) {
        (view.parent as? DialogWindowProvider)?.window
    }

    DisposableEffect(enabled, activityWindow, dialogWindow) {
        if (enabled) {
            activityWindow?.let { SecureWindowRegistry.acquire(it, token) }
            if (dialogWindow != null && dialogWindow !== activityWindow) {
                SecureWindowRegistry.acquire(dialogWindow, token)
            }
        }
        onDispose {
            activityWindow?.let { SecureWindowRegistry.release(it, token) }
            if (dialogWindow != null && dialogWindow !== activityWindow) {
                SecureWindowRegistry.release(dialogWindow, token)
            }
        }
    }
}
