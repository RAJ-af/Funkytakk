package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

object CustomTabsHelper {
    fun openUrl(context: Context, rawUrl: String) {
        if (rawUrl.isBlank()) {
            Toast.makeText(context, "No URL available for this event", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            var url = rawUrl.trim()
            if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                url = "https://$url"
            }
            val uri = Uri.parse(url)

            val colorSchemeParams = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(android.graphics.Color.parseColor("#FFC107"))
                .build()

            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setDefaultColorSchemeParams(colorSchemeParams)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .build()

            customTabsIntent.launchUrl(context, uri)
        } catch (e: Exception) {
            android.util.Log.e("CustomTabsHelper", "Failed to launch Chrome Custom Tab", e)
            try {
                var url = rawUrl.trim()
                if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                    url = "https://$url"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (fallbackError: Exception) {
                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
