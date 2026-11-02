package com.example.voice

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.GetRoomTokenRequest
import com.example.GetRoomTokenResponse
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.realtime.RoomMicController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class RoomVoiceSession(
    private val context: Context,
    val roomId: String,
    val userId: String,
    val userName: String,
    private val scope: CoroutineScope
) {

    companion object {
        private const val TAG = "RoomVoiceSession"
        private const val TOKEN_EXPIRY_RENEW_DELAY_MS = 55 * 60 * 1000L // 55 minutes
        private const val MAX_RECONNECT_ATTEMPTS = 3
    }

    private val _connectionState = MutableStateFlow<VoiceConnectionState>(VoiceConnectionState.Idle)
    val connectionState: StateFlow<VoiceConnectionState> = _connectionState.asStateFlow()

    private val _speakingUserIds = MutableStateFlow<Set<String>>(emptySet())
    val speakingUserIds: StateFlow<Set<String>> = _speakingUserIds.asStateFlow()

    private val _remoteUserIds = MutableStateFlow<Set<String>>(emptySet())
    val remoteUserIds: StateFlow<Set<String>> = _remoteUserIds.asStateFlow()

    private var currentEngine: VoiceEngine? = null
    private var currentRole: String = "listener"
    private var reconnectAttempts = 0
    private var tokenRenewJob: Job? = null
    private var engineStateCollectJob: Job? = null
    private var micCollectJob: Job? = null
    private var reconnectWatchdogJob: Job? = null
    private var isLeaving = false

    fun start() {
        if (isLeaving) return
        Log.d(TAG, "Starting RoomVoiceSession for roomId=$roomId, userId=$userId")
        reconnectAttempts = 0
        connect()
    }

    private fun connect() {
        scope.launch(Dispatchers.IO) {
            _connectionState.value = VoiceConnectionState.Connecting
            val tokenResponse = fetchRoomToken()
            if (tokenResponse == null) {
                handleConnectionFailure("Failed to obtain voice room token from server")
                return@launch
            }

            val provider = tokenResponse.provider ?: "AGORA"
            val appId = tokenResponse.app_id ?: ""
            val token = tokenResponse.token ?: tokenResponse.rtc_token ?: ""
            val canPublish = tokenResponse.can_publish

            Log.d(TAG, "Token received: provider=$provider, canPublish=$canPublish, role=${tokenResponse.role}")

            val engine = VoiceEngineFactory.create(
                provider = provider,
                context = context,
                onTokenWillExpire = {
                    Log.d(TAG, "onTokenWillExpire triggered: renewing token")
                    renewToken()
                }
            )
            currentEngine = engine

            // Forward state and user flows
            engineStateCollectJob?.cancel()
            engineStateCollectJob = scope.launch {
                launch {
                    engine.connectionState.collect { state ->
                        _connectionState.value = state
                        if (state is VoiceConnectionState.Connected) {
                            reconnectAttempts = 0
                            scheduleTokenRenewal()
                            reconnectWatchdogJob?.cancel()
                        } else if (state is VoiceConnectionState.Reconnecting || state is VoiceConnectionState.Failed) {
                            startReconnectWatchdog()
                        }
                    }
                }
                launch {
                    engine.speakingUserIds.collect { speakers ->
                        _speakingUserIds.value = speakers
                    }
                }
                launch {
                    engine.remoteUserIds.collect { remotes ->
                        _remoteUserIds.value = remotes
                    }
                }
            }

            // Bind RoomMicController to engine
            micCollectJob?.cancel()
            micCollectJob = scope.launch {
                RoomMicController.micOn.collect { isMicOn ->
                    if (currentRole == "speaker" || currentRole == "host") {
                        engine.setMicEnabled(isMicOn)
                    } else {
                        engine.setMicEnabled(false)
                    }
                }
            }

            // Join engine
            val config = VoiceJoinConfig(
                roomId = roomId,
                userId = userId,
                userName = userName,
                provider = provider,
                appId = appId,
                token = token,
                canPublish = canPublish
            )

            try {
                engine.join(config)
            } catch (e: Exception) {
                Log.e(TAG, "engine.join error: ${e.message}")
                handleConnectionFailure(e.message ?: "Voice engine join failed")
            }
        }
    }

    private suspend fun fetchRoomToken(): GetRoomTokenResponse? {
        return try {
            val bearerToken = SupabaseSessionManager.getBearerToken(context)
            val res = SupabaseClient.api.getRoomToken(
                auth = bearerToken,
                body = GetRoomTokenRequest(room_id = roomId)
            )
            if (res.isSuccessful && res.body() != null) {
                res.body()
            } else {
                val code = res.code()
                val errBody = res.errorBody()?.string() ?: ""
                Log.d(TAG, "fetchRoomToken error: httpCode=$code, errorBody=$errBody")
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchRoomToken exception: ${e.message}")
            null
        }
    }

    fun onRoleChanged(newRole: String) {
        val oldRole = currentRole
        currentRole = newRole
        Log.d(TAG, "onRoleChanged: $oldRole -> $newRole")

        if ((newRole == "speaker" || newRole == "host") && oldRole == "listener") {
            // Upgrade: listener -> speaker
            scope.launch(Dispatchers.IO) {
                Log.d(TAG, "Upgrading role to publisher, requesting fresh publisher token")
                val response = fetchRoomToken()
                val freshToken = response?.token ?: response?.rtc_token
                if (!freshToken.isNullOrBlank()) {
                    currentEngine?.upgradeToPublisher(freshToken)
                    // Sync current mic state
                    val micOn = RoomMicController.micOn.value
                    currentEngine?.setMicEnabled(micOn)
                } else {
                    Log.d(TAG, "Failed to get fresh publisher token during upgrade")
                }
            }
        } else if (newRole == "listener" && (oldRole == "speaker" || oldRole == "host")) {
            // Downgrade: speaker -> listener
            Log.d(TAG, "Downgrading role to listener")
            currentEngine?.downgradeToListener()
            currentEngine?.setMicEnabled(false)
        }
    }

    fun renewToken() {
        scope.launch(Dispatchers.IO) {
            val response = fetchRoomToken()
            val freshToken = response?.token ?: response?.rtc_token
            if (!freshToken.isNullOrBlank()) {
                Log.d(TAG, "Renewing voice token on engine")
                currentEngine?.renewToken(freshToken)
                scheduleTokenRenewal()
            } else {
                Log.d(TAG, "Failed to fetch renewed token")
            }
        }
    }

    private fun scheduleTokenRenewal() {
        tokenRenewJob?.cancel()
        tokenRenewJob = scope.launch {
            delay(TOKEN_EXPIRY_RENEW_DELAY_MS)
            Log.d(TAG, "Auto-refreshing voice token at 55 minutes")
            renewToken()
        }
    }

    private fun startReconnectWatchdog() {
        if (reconnectWatchdogJob?.isActive == true) return
        reconnectWatchdogJob = scope.launch {
            delay(20_000L) // 20 seconds timeout
            if (_connectionState.value is VoiceConnectionState.Reconnecting || _connectionState.value is VoiceConnectionState.Failed) {
                handleConnectionFailure("Reconnection timed out (> 20s)")
            }
        }
    }

    private fun handleConnectionFailure(reason: String) {
        if (isLeaving) return
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            Log.d(TAG, "Voice connection attempt failed ($reason). Retrying attempt $reconnectAttempts of $MAX_RECONNECT_ATTEMPTS...")
            _connectionState.value = VoiceConnectionState.Reconnecting
            scope.launch {
                delay(2000L * reconnectAttempts)
                connect()
            }
        } else {
            Log.d(TAG, "Voice connection exhausted all $MAX_RECONNECT_ATTEMPTS attempts. Displaying retry.")
            _connectionState.value = VoiceConnectionState.Failed("Voice connection failed, tap to retry")
        }
    }

    fun retry() {
        Log.d(TAG, "User requested voice retry")
        reconnectAttempts = 0
        connect()
    }

    fun leave() {
        if (isLeaving) return
        isLeaving = true
        Log.d(TAG, "Leaving RoomVoiceSession roomId=$roomId")

        tokenRenewJob?.cancel()
        engineStateCollectJob?.cancel()
        micCollectJob?.cancel()
        reconnectWatchdogJob?.cancel()

        scope.launch(Dispatchers.IO) {
            try {
                currentEngine?.leave()
            } catch (e: Exception) {
                Log.d(TAG, "Error leaving engine: ${e.message}")
            } finally {
                currentEngine = null
                _connectionState.value = VoiceConnectionState.Idle
                _speakingUserIds.value = emptySet()
                _remoteUserIds.value = emptySet()
            }
        }
    }
}
