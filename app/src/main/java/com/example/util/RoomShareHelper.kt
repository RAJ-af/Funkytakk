package com.example.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * Handles formatted room invitations for various share destinations:
 * - Copy Link: copies the invite link directly with a toast confirmation
 * - WhatsApp: friendly message with room info & deep link
 * - Facebook: shorter social-friendly message with platform-supported flow
 * - More (Sharesheet): clean general-purpose message via Android ACTION_SEND
 */
object RoomShareHelper {

    private const val TAG = "RoomShareHelper"

    /**
     * Determines whether a string represents a UUID.
     */
    fun isUuid(value: String): Boolean {
        val trimmed = value.trim()
        val clean = trimmed.replace("-", "")
        return clean.length == 32 || (trimmed.contains("-") && trimmed.length >= 24)
    }

    /**
     * Formats a short, human-friendly room display code (e.g. "FT5272") for the UI.
     * If an existing short room code already exists (e.g. "FT5272" or joinCode), it is used.
     * If roomId is a backend UUID and no short code exists, a deterministic 4-digit short code
     * prefixed with "FT" is generated without altering the real backend UUID.
     */
    fun getDisplayRoomCode(roomId: String?, joinCode: String? = null): String {
        val trimmedCode = joinCode?.trim().orEmpty()
        val trimmedId = roomId?.trim().orEmpty()

        // 1. If an existing short room code exists in joinCode
        if (trimmedCode.isNotBlank() && !isUuid(trimmedCode)) {
            return if (trimmedCode.startsWith("FT", ignoreCase = true)) {
                trimmedCode.uppercase()
            } else if (trimmedCode.all { it.isDigit() } && trimmedCode.length in 4..6) {
                "FT$trimmedCode"
            } else {
                trimmedCode.uppercase()
            }
        }

        // 2. If roomId itself is already a short code (e.g. "FT5272", "FT1902")
        if (trimmedId.isNotBlank() && !isUuid(trimmedId)) {
            return if (trimmedId.startsWith("FT", ignoreCase = true)) {
                trimmedId.uppercase()
            } else if (trimmedId.all { it.isDigit() } && trimmedId.length in 4..6) {
                "FT$trimmedId"
            } else {
                trimmedId.uppercase()
            }
        }

        // 3. Fallback if empty/blank
        if (trimmedId.isBlank()) {
            return "FT5272"
        }

        // 4. Derive deterministic 4-digit code (1000..9999) from UUID
        val hash = kotlin.math.abs(trimmedId.hashCode())
        val codeNumber = (hash % 9000) + 1000
        return "FT$codeNumber"
    }

    fun buildDefaultInviteLink(roomId: String): String {
        return "funkytalk://room/${roomId.trim()}"
    }

    fun buildWhatsAppMessage(roomName: String, roomId: String, inviteLink: String): String {
        return "🗣️ Join me on FunkyTalk!\n\n" +
                "Let’s practice languages and have a fun conversation. 🌍✨\n\n" +
                "Room: $roomName\n" +
                "Room ID: $roomId\n\n" +
                "👉 Join here:\n" +
                inviteLink
    }

    fun buildFacebookMessage(roomName: String, roomId: String, inviteLink: String): String {
        return "🗣️ Join me on FunkyTalk!\n" +
                "Let’s practice languages together. 🌍\n\n" +
                "Room: $roomName\n" +
                "Room ID: $roomId\n\n" +
                inviteLink
    }

    fun buildMoreMessage(roomName: String, roomId: String, inviteLink: String): String {
        return "🗣️ Join me on FunkyTalk!\n\n" +
                "Room: $roomName\n" +
                "Room ID: $roomId\n\n" +
                "Let’s practice languages and connect. 🌍✨\n\n" +
                "Join here:\n" +
                inviteLink
    }

    /**
     * Copy ONLY the actual invite link to clipboard.
     * Shows: "Link copied"
     */
    fun copyLinkToClipboard(context: Context, inviteLink: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("FunkyTalk Room Link", inviteLink)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error copying link to clipboard: ${e.message}")
            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * WhatsApp share with friendly formatted message and graceful fallback.
     */
    fun shareToWhatsApp(
        context: Context,
        roomName: String,
        roomId: String,
        inviteLink: String
    ) {
        val message = buildWhatsAppMessage(roomName, roomId, inviteLink)
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                // Try WhatsApp Business package
                val w4bIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp.w4b")
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(w4bIntent)
            } catch (e2: ActivityNotFoundException) {
                // Gracefully handle missing WhatsApp by notifying user and presenting native share options
                Toast.makeText(context, "WhatsApp is not installed. Opening share options...", Toast.LENGTH_SHORT).show()
                shareMore(context, roomName, roomId, inviteLink, customText = message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sharing to WhatsApp: ${e.message}")
            shareMore(context, roomName, roomId, inviteLink, customText = message)
        }
    }

    /**
     * Facebook share using platform-supported flow without requiring Facebook login.
     * Tries Facebook app first, then Facebook Lite, then web sharer URL, and falls back gracefully.
     */
    fun shareToFacebook(
        context: Context,
        roomName: String,
        roomId: String,
        inviteLink: String
    ) {
        val message = buildFacebookMessage(roomName, roomId, inviteLink)
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.facebook.katana")
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                val fbLiteIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.facebook.lite")
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fbLiteIntent)
            } catch (e2: ActivityNotFoundException) {
                // Platform-supported Facebook web share dialog without requiring in-app Facebook SDK login
                try {
                    val encodedLink = Uri.encode(inviteLink)
                    val encodedQuote = Uri.encode(message)
                    val webUrl = "https://www.facebook.com/sharer/sharer.php?u=$encodedLink&quote=$encodedQuote"
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                } catch (e3: Exception) {
                    Toast.makeText(context, "Facebook is not installed. Opening share options...", Toast.LENGTH_SHORT).show()
                    shareMore(context, roomName, roomId, inviteLink, customText = message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sharing to Facebook: ${e.message}")
            shareMore(context, roomName, roomId, inviteLink, customText = message)
        }
    }

    /**
     * More / Android native Sharesheet using ACTION_SEND with the clean general-purpose message.
     */
    fun shareMore(
        context: Context,
        roomName: String,
        roomId: String,
        inviteLink: String,
        customText: String? = null
    ) {
        val message = customText ?: buildMoreMessage(roomName, roomId, inviteLink)
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Join me on FunkyTalk!")
                putExtra(Intent.EXTRA_TEXT, message)
            }
            val chooser = Intent.createChooser(sendIntent, "Share Room").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching native Sharesheet: ${e.message}")
            Toast.makeText(context, "Unable to open share options", Toast.LENGTH_SHORT).show()
        }
    }
}
