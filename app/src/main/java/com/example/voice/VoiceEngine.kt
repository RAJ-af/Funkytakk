package com.example.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface VoiceConnectionState {
    object Idle : VoiceConnectionState
    object Connecting : VoiceConnectionState
    object Connected : VoiceConnectionState
    object Reconnecting : VoiceConnectionState
    data class Failed(val reason: String) : VoiceConnectionState
}

data class VoiceJoinConfig(
    val roomId: String,
    val userId: String,
    val userName: String = "",
    val provider: String = "AGORA",
    val appId: String = "",
    val token: String = "",
    val canPublish: Boolean = false
)

interface VoiceEngine {
    /**
     * Primary join signature: join(roomId, token, appId, uid, canPublish)
     */
    suspend fun join(
        roomId: String,
        token: String,
        appId: String,
        uid: String,
        canPublish: Boolean
    )

    /**
     * Structured configuration join overload
     */
    suspend fun join(cfg: VoiceJoinConfig) {
        join(
            roomId = cfg.roomId,
            token = cfg.token,
            appId = cfg.appId,
            uid = cfg.userId,
            canPublish = cfg.canPublish
        )
    }

    suspend fun leave()
    fun setMicEnabled(enabled: Boolean)
    suspend fun upgradeToPublisher(newToken: String)
    fun downgradeToListener()
    fun renewToken(newToken: String)

    /** Connection state flow */
    val connectionState: StateFlow<VoiceConnectionState>

    /** Speaking & volume indication events flow */
    val speakingUserIds: StateFlow<Set<String>>
    val speakingEvents: StateFlow<Set<String>> get() = speakingUserIds

    /** Remote users present in channel */
    val remoteUserIds: StateFlow<Set<String>>

    /** Error events flow */
    val errorFlow: Flow<String>
}
