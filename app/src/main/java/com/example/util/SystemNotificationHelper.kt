package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.crypto.VaultSecurityManager
import com.example.service.PushNotificationManager

object SystemNotificationHelper {
    private const val TAG = "SystemNotificationHelper"

    const val CHANNEL_MESSAGES_ID = "messages"
    const val CHANNEL_UPDATES_ID = "funkytalk_updates_channel"

    private const val CHANNEL_MESSAGES_NAME = "Messages & Chats"
    private const val CHANNEL_UPDATES_NAME = "System Updates & Alerts"

    @Volatile
    private var channelsCreated = false

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Messages channel (Importance HIGH for heads-up notification banner, sound, vibration)
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                CHANNEL_MESSAGES_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications for direct and group chat messages"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }

            // 2. System updates channel
            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES_ID,
                CHANNEL_UPDATES_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "System updates, room invites, and profile activity."
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }

            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(updatesChannel)
            if (!channelsCreated) {
                channelsCreated = true
                Log.d(TAG, "Notification channels created successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification channels", e)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showSystemNotification(
        context: Context,
        title: String,
        message: String,
        partnerUserId: String? = null,
        conversationId: String? = null,
        groupId: String? = null,
        senderId: String? = null,
        kind: String? = null,
        notificationId: Int? = null,
        channelId: String = CHANNEL_MESSAGES_ID
    ) {
        createNotificationChannels(context)

        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Notification permission POST_NOTIFICATIONS not granted. Cannot post notification.")
            return
        }

        val effectivePartnerId = partnerUserId?.takeIf { it.isNotBlank() } ?: senderId?.takeIf { it.isNotBlank() }
        val isChatNotification = !conversationId.isNullOrBlank() ||
            !groupId.isNullOrBlank() ||
            !effectivePartnerId.isNullOrBlank() ||
            !kind.isNullOrBlank()

        // Suppress notifications for the conversation that is currently open in the foreground
        if (isChatNotification && PushNotificationManager.isChatOpenInForeground(
                conversationId = conversationId,
                groupId = groupId,
                partnerUserId = effectivePartnerId
            )
        ) {
            Log.d(TAG, "Suppressing system notification for active foreground conversation")
            return
        }

        val resolvedNotificationId = notificationId ?: if (isChatNotification) {
            PushNotificationManager.notificationIdForChat(
                conversationId = conversationId,
                groupId = groupId,
                fallbackPartnerId = effectivePartnerId
            )
        } else {
            (System.currentTimeMillis() % 100000).toInt()
        }

        val isVaultLocked = isChatNotification && VaultSecurityManager.isVaultLocked()
        val displayTitle = if (isVaultLocked) "FunkyTalk" else title
        val displayMessage = if (isVaultLocked) "New encrypted message received • Tap to unlock" else message

        try {
            val resolvedKind = kind?.takeIf { it.isNotBlank() } ?: when {
                !groupId.isNullOrBlank() -> "group"
                !conversationId.isNullOrBlank() || !effectivePartnerId.isNullOrBlank() -> "dm"
                else -> null
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (isChatNotification) {
                    putExtra("from_push", true)
                    putExtra("navigate_to", "home")
                    if (!resolvedKind.isNullOrBlank()) putExtra("kind", resolvedKind)
                    if (!conversationId.isNullOrBlank()) putExtra("conversation_id", conversationId)
                    if (!groupId.isNullOrBlank()) putExtra("group_id", groupId)
                    if (!senderId.isNullOrBlank()) putExtra("sender_id", senderId)
                    if (!effectivePartnerId.isNullOrBlank()) putExtra("partnerUserId", effectivePartnerId)
                } else {
                    putExtra("navigate_to", "notifications")
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                resolvedNotificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val publicNotification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("FunkyTalk")
                .setContentText("New message")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(displayTitle)
                .setContentText(displayMessage)
                .setStyle(NotificationCompat.BigTextStyle().bigText(displayMessage))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicNotification)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(resolvedNotificationId, builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification", e)
        }
    }

    fun triggerTestPushNotification(context: Context) {
        showSystemNotification(
            context = context,
            title = "FunkyTalk Push Notification",
            message = "Push notifications are active! You will get system notifications when messages arrive.",
            channelId = CHANNEL_MESSAGES_ID
        )
    }
}
