package com.example.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VoiceEngineFactory {
    private const val TAG = "VoiceEngineFactory"

    fun create(
        provider: String,
        context: Context,
        onTokenWillExpire: (() -> Unit)? = null
    ): VoiceEngine {
        val normalized = provider.trim().uppercase()
        Log.d(TAG, "Creating VoiceEngine for provider: $normalized")
        return when (normalized) {
            "AGORA" -> AgoraVoiceEngine(context, onTokenWillExpire)
            "ZEGO" -> StubVoiceEngine("ZEGO")
            "DAILY" -> StubVoiceEngine("DAILY")
            else -> StubVoiceEngine(normalized)
        }
    }
}

class StubVoiceEngine(private val providerName: String) : VoiceEngine {
    private val _connectionState = MutableStateFlow<VoiceConnectionState>(
        VoiceConnectionState.Failed("provider not implemented yet: $providerName")
    )
    override val connectionState: StateFlow<VoiceConnectionState> = _connectionState.asStateFlow()
    override val speakingUserIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())
    override val remoteUserIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())

    override suspend fun join(cfg: VoiceJoinConfig) {
        _connectionState.value = VoiceConnectionState.Failed("provider not implemented yet: $providerName")
    }

    override suspend fun leave() {
        _connectionState.value = VoiceConnectionState.Idle
    }

    override fun setMicEnabled(enabled: Boolean) {}

    override suspend fun upgradeToPublisher(newToken: String) {}

    override fun downgradeToListener() {}

    override fun renewToken(newToken: String) {}
}
