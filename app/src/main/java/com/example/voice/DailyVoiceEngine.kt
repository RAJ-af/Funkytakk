package com.example.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Adapter stub for Daily RTC SDK.
 * Emits a clear "not implemented yet" error message without crashing.
 */
class DailyVoiceEngine : VoiceEngine {
    private val _connectionState = MutableStateFlow<VoiceConnectionState>(
        VoiceConnectionState.Failed("Daily provider is not implemented yet")
    )
    override val connectionState: StateFlow<VoiceConnectionState> = _connectionState.asStateFlow()

    private val _speakingUserIds = MutableStateFlow<Set<String>>(emptySet())
    override val speakingUserIds: StateFlow<Set<String>> = _speakingUserIds.asStateFlow()

    private val _remoteUserIds = MutableStateFlow<Set<String>>(emptySet())
    override val remoteUserIds: StateFlow<Set<String>> = _remoteUserIds.asStateFlow()

    private val _errorFlow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 16).apply {
        tryEmit("Daily provider is not implemented yet")
    }
    override val errorFlow: Flow<String> = _errorFlow.asSharedFlow()

    override suspend fun join(
        roomId: String,
        token: String,
        appId: String,
        uid: String,
        canPublish: Boolean
    ) {
        val message = "Daily provider is not implemented yet"
        _connectionState.value = VoiceConnectionState.Failed(message)
        _errorFlow.tryEmit(message)
    }

    override suspend fun leave() {
        _connectionState.value = VoiceConnectionState.Idle
    }

    override fun setMicEnabled(enabled: Boolean) {}

    override suspend fun upgradeToPublisher(newToken: String) {}

    override fun downgradeToListener() {}

    override fun renewToken(newToken: String) {}
}
