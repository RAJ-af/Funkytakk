package com.example.realtime

import android.content.Context
import android.util.Log
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.UpdateParticipantMicRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * RoomMicController owns the local user's microphone state in a voice room.
 *
 * Rules:
 *  - Owns MutableStateFlow<Boolean> micOn.
 *  - Only modified by:
 *      (a) User tap on the mic button, or
 *      (b) The single moment user role changes from non-speaker to 'speaker'.
 *  - User's own seat & mic icons are drawn strictly from [micOn], never from database participant rows.
 *  - Server updates are debounced by 300ms, serialized, and skipped if equal to the last written value.
 *  - Server response is NEVER read back into [micOn] (no feedback loop).
 *  - All micOn transitions are logged with their source tag.
 */
object RoomMicController {
    private const val TAG = "RoomMicController"

    private val _micOn = MutableStateFlow(false)
    val micOn: StateFlow<Boolean> = _micOn.asStateFlow()

    private var currentRoomId: String? = null
    private var currentUserId: String? = null
    private var currentRole: String = "listener"
    private var lastWrittenServerValue: Boolean? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var debounceJob: Job? = null
    private val writeMutex = Mutex()

    fun initRoom(roomId: String, userId: String, initialRole: String, initialMic: Boolean) {
        currentRoomId = roomId
        currentUserId = userId
        currentRole = initialRole
        lastWrittenServerValue = initialMic
        val startMic = if (initialRole == "host" || initialRole == "speaker") initialMic else false
        Log.d(TAG, "initRoom roomId=$roomId, userId=$userId, role=$initialRole, startMic=$startMic [source: INIT]")
        _micOn.value = startMic
        AgoraVoiceManager.muteLocalAudioStream(!startMic)
    }

    fun onRoleChanged(newRole: String, serverMicValue: Boolean) {
        val oldRole = currentRole
        currentRole = newRole
        if (oldRole != "speaker" && newRole == "speaker") {
            Log.d(TAG, "Role changed '$oldRole' -> 'speaker', setting micOn to $serverMicValue [source: ROLE_CHANGE]")
            _micOn.value = serverMicValue
            AgoraVoiceManager.muteLocalAudioStream(!serverMicValue)
            lastWrittenServerValue = serverMicValue
        } else if (newRole == "listener") {
            Log.d(TAG, "Role changed to 'listener', turning micOn to false [source: ROLE_CHANGE_LISTENER]")
            _micOn.value = false
            AgoraVoiceManager.muteLocalAudioStream(true)
            lastWrittenServerValue = false
        }
    }

    fun toggleMic(context: Context? = null) {
        val next = !_micOn.value
        setMic(next, "USER_TAP", context)
    }

    fun setMic(enabled: Boolean, source: String, context: Context? = null) {
        if (_micOn.value == enabled) {
            Log.d(TAG, "setMic ignored, already $enabled [source: $source]")
            return
        }
        Log.d(TAG, "micOn changed from ${_micOn.value} to $enabled [source: $source]")
        _micOn.value = enabled
        AgoraVoiceManager.muteLocalAudioStream(!enabled)

        // Mirror to server debounced by 300ms, serialized
        scheduleServerMirror(context)
    }

    private fun scheduleServerMirror(context: Context?) {
        val targetValue = _micOn.value
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return

        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(300) // 300 ms debounce
            writeMutex.withLock {
                if (lastWrittenServerValue == targetValue) {
                    Log.d(TAG, "Skipping server write: value $targetValue matches lastWrittenServerValue")
                    return@withLock
                }
                try {
                    val token = if (context != null) SupabaseSessionManager.getBearerToken(context) else null
                    val authHeader = if (!token.isNullOrBlank()) token else "Bearer ${com.example.BuildConfig.SUPABASE_ANON_KEY}"
                    val res = SupabaseClient.api.updateRoomParticipantMic(
                        auth = authHeader,
                        roomIdEq = "eq.$roomId",
                        userIdEq = "eq.$userId",
                        body = UpdateParticipantMicRequest(is_mic_enabled = targetValue)
                    )
                    if (res.isSuccessful) {
                        lastWrittenServerValue = targetValue
                        Log.d(TAG, "Successfully mirrored mic state $targetValue to server for room=$roomId, user=$userId")
                    } else {
                        Log.d(TAG, "Server mirror returned error HTTP ${res.code()}: ${res.message()}")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Exception mirroring mic state to server: ${e.message}")
                }
            }
        }
    }

    fun reset() {
        debounceJob?.cancel()
        currentRoomId = null
        currentUserId = null
        currentRole = "listener"
        lastWrittenServerValue = null
        _micOn.value = false
    }
}
