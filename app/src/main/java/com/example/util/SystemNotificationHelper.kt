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

object SystemNotificationHelper {
    private const val TAG = "SystemNotificationHelper"

    const val CHANNEL_MESSAGES_ID = "funkytalk_messages_channel"
    const val CHANNEL_UPDATES_ID = "funkytalk_updates_channel"

    private const val CHANNEL_MESSAGES_NAME = "Messages & Direct Chats"
    private const val CHANNEL_UPDATES_NAME = "System Updates & Alerts"

    @Volatile
    private var channelsCreated = false

    fun createNotificationChannels(context: Context) {
        if (channelsCreated || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Messages channel (Importance HIGH for heads-up notification banner, sound, vibration)
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                CHANNEL_MESSAGES_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming E2EE chat messages, voice notes, and direct replies."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(true)
            }

            // 2. System updates channel
            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES_ID,
                CHANNEL_UPDATES_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "System updates, room invites, and profile activity."
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(updatesChannel)
            channelsCreated = true
            Log.d(TAG, "Notification channels created successfully.")
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
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt(),
        channelId: String = CHANNEL_MESSAGES_ID
    ) {
        createNotificationChannels(context)

        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Notification permission POST_NOTIFICATIONS not granted. Cannot post notification.")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (!partnerUserId.isNullOrBlank()) {
                    putExtra("partnerUserId", partnerUserId)
                    putExtra("navigate_to", "direct_chat")
                } else {
                    putExtra("navigate_to", "notifications")
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "System Notification posted successfully: $title - $message")
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
