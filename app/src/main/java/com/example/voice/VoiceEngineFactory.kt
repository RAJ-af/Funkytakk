package com.example.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object VoiceEngineFactory {
    private const val TAG = "VoiceEngineFactory"

    /**
     * Resolves the appropriate VoiceEngine based on the provider string from get-room-token.
     * Supported providers: "agora", "zego", "daily".
     * Unknown providers return a clean error engine and will never crash the app.
     */
    fun create(
        provider: String?,
        context: Context? = null,
        onTokenWillExpire: (() -> Unit)? = null
    ): VoiceEngine {
        val appContext = context?.applicationContext
            ?: com.example.FunkyTalkApplication.getSafeAppContext()
        val normalized = provider?.trim()?.uppercase() ?: "AGORA"
        val isContextNull = (appContext == null)

        Log.d(TAG, "Resolving VoiceEngine for provider='$provider' (normalized='$normalized'), isContextNull=$isContextNull (${appContext.javaClass.simpleName})")

        return when (normalized) {
            "AGORA" -> AgoraVoiceEngine(appContext, onTokenWillExpire)
            "ZEGO" -> ZegoVoiceEngine()
            "DAILY" -> DailyVoiceEngine()
            else -> {
                val errorMsg = "Unknown RTC provider: '$provider'. Supported providers: agora, zego, daily."
                Log.e(TAG, errorMsg)
                UnknownVoiceEngine(provider ?: "unknown")
            }
        }
    }
}

/**
 * Safe fallback engine for unrecognized RTC providers.
 * Displays a clean error without crashing.
 */
class UnknownVoiceEngine(private val providerName: String) : VoiceEngine {
    private val errorMessage = "Unknown RTC provider: '$providerName'"

    private val _connectionState = MutableStateFlow<VoiceConnectionState>(
        VoiceConnectionState.Failed(errorMessage)
    )
    override val connectionState: StateFlow<VoiceConnectionState> = _connectionState.asStateFlow()

    private val _speakingUserIds = MutableStateFlow<Set<String>>(emptySet())
    override val speakingUserIds: StateFlow<Set<String>> = _speakingUserIds.asStateFlow()

    private val _remoteUserIds = MutableStateFlow<Set<String>>(emptySet())
    override val remoteUserIds: StateFlow<Set<String>> = _remoteUserIds.asStateFlow()

    private val _errorFlow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 16).apply {
        tryEmit(errorMessage)
    }
    override val errorFlow: Flow<String> = _errorFlow.asSharedFlow()

    override suspend fun join(
        roomId: String,
        token: String,
        appId: String,
        uid: String,
        canPublish: Boolean
    ) {
        _connectionState.value = VoiceConnectionState.Failed(errorMessage)
        _errorFlow.tryEmit(errorMessage)
    }

    override suspend fun leave() {
        _connectionState.value = VoiceConnectionState.Idle
    }

    override fun setMicEnabled(enabled: Boolean) {}

    override suspend fun upgradeToPublisher(newToken: String) {}

    override fun downgradeToListener() {}

    override fun renewToken(newToken: String) {}
}
