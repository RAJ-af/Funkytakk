package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VoiceRoomOverlayManager {
    private val _isOverlayActive = MutableStateFlow(false)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _roomId = MutableStateFlow<String?>(null)
    val roomId: StateFlow<String?> = _roomId.asStateFlow()

    private val _roomTitle = MutableStateFlow("Voice Room")
    val roomTitle: StateFlow<String> = _roomTitle.asStateFlow()

    private val _userAvatar = MutableStateFlow<String?>(null)
    val userAvatar: StateFlow<String?> = _userAvatar.asStateFlow()

    val isMicEnabled: StateFlow<Boolean> = com.example.realtime.RoomMicController.micOn

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    var onToggleMicAction: (() -> Unit)? = null
    var onLeaveRoomAction: (() -> Unit)? = null

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun updateSession(
        roomId: String?,
        roomTitle: String?,
        avatarUrl: String?,
        isMicEnabled: Boolean
    ) {
        _roomId.value = roomId
        _roomTitle.value = if (!roomTitle.isNullOrBlank()) roomTitle else "Voice Room"
        _userAvatar.value = avatarUrl
        _isOverlayActive.value = (roomId != null)
    }

    fun setMicEnabled(enabled: Boolean) {
        com.example.realtime.RoomMicController.setMic(enabled, "OverlayManager")
    }

    fun setSpeaking(speaking: Boolean) {
        _isSpeaking.value = speaking
    }

    fun clear() {
        _roomId.value = null
        _roomTitle.value = "Voice Room"
        _userAvatar.value = null
        _isSpeaking.value = false
        _isOverlayActive.value = false
    }

    fun startServiceIfPermitted(context: Context) {
        if (!OverlayPermissionHelper.canDrawOverlays(context)) return
        try {
            val intent = Intent(context, VoiceRoomOverlayService::class.java).apply {
                action = VoiceRoomOverlayService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopService(context: Context) {
        val intent = Intent(context, VoiceRoomOverlayService::class.java).apply {
            action = VoiceRoomOverlayService.ACTION_STOP
        }
        context.stopService(intent)
    }
}
