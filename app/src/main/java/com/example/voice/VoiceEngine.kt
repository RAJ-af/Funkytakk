package com.example.voice

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
    val userName: String,
    val provider: String,
    val appId: String,
    val token: String,
    val canPublish: Boolean
)

interface VoiceEngine {
    suspend fun join(cfg: VoiceJoinConfig)
    suspend fun leave()
    fun setMicEnabled(enabled: Boolean)
    suspend fun upgradeToPublisher(newToken: String)
    fun downgradeToListener()
    fun renewToken(newToken: String)
    val connectionState: StateFlow<VoiceConnectionState>
    val speakingUserIds: StateFlow<Set<String>>
    val remoteUserIds: StateFlow<Set<String>>
}
