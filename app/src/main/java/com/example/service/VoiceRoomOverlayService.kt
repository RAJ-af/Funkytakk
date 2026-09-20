package com.example.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.animation.DecelerateInterpolator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.compose.AsyncImage
import com.example.MainActivity
import com.example.R
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.UpdateParticipantMicRequest
import com.example.ui.components.FloatingBubbleLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VoiceRoomOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_START = "com.example.service.action.START_OVERLAY"
        const val ACTION_STOP = "com.example.service.action.STOP_OVERLAY"
        const val ACTION_TOGGLE_MIC = "com.example.service.action.TOGGLE_MIC"
        const val ACTION_LEAVE_ROOM = "com.example.service.action.LEAVE_ROOM"
        private const val NOTIFICATION_ID = 9021
        private const val CHANNEL_ID = "voice_room_overlay_channel"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // Screen dimensions
    private var screenWidth = 1080
    private var screenHeight = 1920

    // Bubble state
    private val isSatelliteOpen = mutableStateOf(false)
    private val isSnappedToRight = mutableStateOf(true)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        createNotificationChannel()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        updateScreenDimensions()
        VoiceRoomOverlayManager.setServiceRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                removeOverlayView()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_MIC -> {
                handleToggleMic()
                return START_STICKY
            }
            ACTION_LEAVE_ROOM -> {
                handleLeaveRoom()
                return START_NOT_STICKY
            }
            else -> {
                try {
                    startForegroundNotification()
                    showOverlayView()
                    observeOverlayState()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return START_STICKY
    }

    private fun updateScreenDimensions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager?.currentWindowMetrics
            if (windowMetrics != null) {
                screenWidth = windowMetrics.bounds.width()
                screenHeight = windowMetrics.bounds.height()
            }
        } else {
            val display = windowManager?.defaultDisplay
            val size = android.graphics.Point()
            display?.getSize(size)
            screenWidth = size.x
            screenHeight = size.y
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Room Audio Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active voice room audio and quick floating bubble controls"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val roomTitle = VoiceRoomOverlayManager.roomTitle.value
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "voice_room")
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleMicIntent = Intent(this, VoiceRoomOverlayService::class.java).apply {
            action = ACTION_TOGGLE_MIC
        }
        val toggleMicPendingIntent = PendingIntent.getService(
            this,
            1,
            toggleMicIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val leaveIntent = Intent(this, VoiceRoomOverlayService::class.java).apply {
            action = ACTION_LEAVE_ROOM
        }
        val leavePendingIntent = PendingIntent.getService(
            this,
            2,
            leaveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isMicOn = VoiceRoomOverlayManager.isMicEnabled.value

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FunkyTalk Voice Circle")
            .setContentText("In ${roomTitle}'s voice room")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (isMicOn) android.R.drawable.ic_lock_silent_mode_off else android.R.drawable.ic_lock_silent_mode,
                if (isMicOn) "Mute" else "Unmute",
                toggleMicPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Leave Room",
                leavePendingIntent
            )
            .build()

        val hasRecordAudio = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (hasRecordAudio) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (hasRecordAudio) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
        } else {
            0
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && fgsType != 0) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    fgsType
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeOverlayState() {
        serviceScope.launch {
            VoiceRoomOverlayManager.isOverlayActive.collectLatest { active ->
                if (!active) {
                    removeOverlayView()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }

        serviceScope.launch {
            VoiceRoomOverlayManager.roomTitle.collectLatest {
                startForegroundNotification()
            }
        }

        serviceScope.launch {
            VoiceRoomOverlayManager.isMicEnabled.collectLatest {
                startForegroundNotification()
            }
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun showOverlayView() {
        if (overlayView != null) return
        if (!OverlayPermissionHelper.canDrawOverlays(this)) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = screenWidth - 220
            y = screenHeight / 3
        }
        layoutParams = params

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@VoiceRoomOverlayService)
            setViewTreeSavedStateRegistryOwner(this@VoiceRoomOverlayService)
            setViewTreeViewModelStoreOwner(this@VoiceRoomOverlayService)

            setContent {
                val avatarUrl by VoiceRoomOverlayManager.userAvatar.collectAsState()
                val isMicEnabled by VoiceRoomOverlayManager.isMicEnabled.collectAsState()
                val isSpeaking by VoiceRoomOverlayManager.isSpeaking.collectAsState()
                val isOptionsOpen by isSatelliteOpen
                val snappedRight by isSnappedToRight

                FloatingBubbleLayout(
                    avatarUrl = avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                    isMicEnabled = isMicEnabled,
                    isSpeaking = isSpeaking,
                    isOptionsOpen = isOptionsOpen,
                    isSnappedToRight = snappedRight,
                    onToggleOptions = {
                        isSatelliteOpen.value = !isSatelliteOpen.value
                    },
                    onShortTap = {
                        bringAppToForeground()
                    },
                    onDrag = { dx, dy ->
                        updatePosition(dx, dy)
                    },
                    onDragEnd = {
                        snapToNearestEdge()
                    },
                    onToggleMic = {
                        handleToggleMic()
                    },
                    onLeaveRoom = {
                        handleLeaveRoom()
                    }
                )
            }
        }

        overlayView = composeView
        try {
            windowManager?.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updatePosition(dx: Float, dy: Float) {
        val params = layoutParams ?: return
        params.x = (params.x + dx.toInt()).coerceIn(0, screenWidth - 140)
        params.y = (params.y + dy.toInt()).coerceIn(60, screenHeight - 200)
        try {
            windowManager?.updateViewLayout(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun snapToNearestEdge() {
        val params = layoutParams ?: return
        val currentX = params.x
        val targetX = if (currentX + 100 > screenWidth / 2) {
            isSnappedToRight.value = true
            screenWidth - 200
        } else {
            isSnappedToRight.value = false
            16
        }

        val animator = ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 240
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                params.x = va.animatedValue as Int
                try {
                    windowManager?.updateViewLayout(overlayView, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        animator.start()
    }

    private fun bringAppToForeground() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "voice_room")
        }
        startActivity(intent)
    }

    private fun handleToggleMic() {
        val current = VoiceRoomOverlayManager.isMicEnabled.value
        val newStatus = !current
        VoiceRoomOverlayManager.setMicEnabled(newStatus)
        VoiceRoomOverlayManager.onToggleMicAction?.invoke()

        val roomId = VoiceRoomOverlayManager.roomId.value
        if (roomId != null) {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val token = SupabaseSessionManager.getBearerToken(applicationContext)
                    val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        SupabaseClient.api.updateRoomParticipantMic(
                            auth = token,
                            roomIdEq = "eq.$roomId",
                            userIdEq = "eq.$userId",
                            body = UpdateParticipantMicRequest(is_mic_enabled = newStatus)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun handleLeaveRoom() {
        VoiceRoomOverlayManager.onLeaveRoomAction?.invoke()
        val roomId = VoiceRoomOverlayManager.roomId.value
        val context = applicationContext
        if (roomId != null) {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val token = SupabaseSessionManager.getBearerToken(context)
                    val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        SupabaseClient.api.leaveRoomParticipant(
                            auth = token,
                            roomIdEq = "eq.$roomId",
                            userIdEq = "eq.$userId"
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        VoiceRoomOverlayManager.clear()
        removeOverlayView()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun removeOverlayView() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    override fun onDestroy() {
        removeOverlayView()
        VoiceRoomOverlayManager.setServiceRunning(false)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
