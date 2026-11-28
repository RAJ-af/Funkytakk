package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R

/**
 * Foreground service of type `mediaProjection` required by Android 10–15+ while the room host
 * shares their screen over Daily.co.
 *
 * IMPORTANT: Start this service ONLY after the user grants `MediaProjection` permission via
 * `MediaProjectionManager.createScreenCaptureIntent()` (`Activity.RESULT_OK`), and before
 * calling Daily's `startScreenShare(...)`.
 */
class ScreenShareForegroundService : Service() {

    companion object {
        private const val TAG = "ScreenShareFGS"
        const val ACTION_START = "com.example.service.action.START_SCREEN_SHARE"
        const val ACTION_STOP = "com.example.service.action.STOP_SCREEN_SHARE"
        const val EXTRA_ROOM_TITLE = "extra_room_title"

        private const val CHANNEL_ID = "screen_share_channel"
        private const val NOTIFICATION_ID = 9042

        @Volatile
        var onStopRequested: ((reason: String) -> Unit)? = null

        private val _runningFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
        val runningFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _runningFlow

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context, roomTitle: String? = null) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, ScreenShareForegroundService::class.java).apply {
                action = ACTION_START
                if (!roomTitle.isNullOrBlank()) {
                    putExtra(EXTRA_ROOM_TITLE, roomTitle)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appCtx.startForegroundService(intent)
            } else {
                appCtx.startService(intent)
            }
        }

        fun stop(context: Context) {
            val appCtx = context.applicationContext
            if (!isRunning) {
                _runningFlow.value = false
                // Still send stopService safely in case startForegroundService was just queued
                runCatching { appCtx.stopService(Intent(appCtx, ScreenShareForegroundService::class.java)) }
                return
            }
            runCatching {
                val intent = Intent(appCtx, ScreenShareForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
                appCtx.startService(intent)
            }.onFailure {
                _runningFlow.value = false
                runCatching { appCtx.stopService(Intent(appCtx, ScreenShareForegroundService::class.java)) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "Stop action triggered from notification/client")
                onStopRequested?.invoke("notification_stop")
                isRunning = false
                _runningFlow.value = false
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val roomTitle = intent?.getStringExtra(EXTRA_ROOM_TITLE)?.takeIf { it.isNotBlank() }
                val notification = buildNotification(roomTitle)
                try {
                    val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    } else {
                        0
                    }
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgType)
                    isRunning = true
                    _runningFlow.value = true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to enter mediaProjection foreground state: ${e.message}", e)
                    isRunning = false
                    _runningFlow.value = false
                    onStopRequested?.invoke("fgs_start_failed")
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "App task removed while sharing screen — stopping share cleanly")
        onStopRequested?.invoke("task_removed")
        isRunning = false
        _runningFlow.value = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isRunning = false
        _runningFlow.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Room Screen Share",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when you are sharing your screen in a voice room"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(roomTitle: String?): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "voice_room")
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenShareForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtitle = if (roomTitle != null) {
            "Sharing screen in $roomTitle • Everything on your screen is visible"
        } else {
            "Everything on your screen is visible to viewers"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Screen sharing active")
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }
}
