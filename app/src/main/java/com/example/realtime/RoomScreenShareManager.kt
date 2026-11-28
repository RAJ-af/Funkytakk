package com.example.realtime

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import co.daily.CallClient
import co.daily.CallClientListener
import co.daily.model.CallState
import co.daily.model.MediaStreamTrack
import co.daily.model.MeetingToken
import co.daily.model.Participant
import co.daily.model.ParticipantLeftReason
import co.daily.model.RequestListener
import co.daily.model.RequestListenerWithData
import co.daily.settings.BitRate
import co.daily.settings.CameraInputSettingsUpdate
import co.daily.settings.CameraPublishingSettingsUpdate
import co.daily.settings.ClientSettingsUpdate
import co.daily.settings.Disable
import co.daily.settings.Enable
import co.daily.settings.FrameRate
import co.daily.settings.InputSettingsUpdate
import co.daily.settings.MicrophoneInputSettingsUpdate
import co.daily.settings.MicrophonePublishingSettingsUpdate
import co.daily.settings.PublishingSettingsUpdate
import co.daily.settings.Scale
import co.daily.settings.ScreenVideoPublishingSettingsUpdate
import co.daily.settings.VideoEncodingsSettingsUpdate
import co.daily.settings.VideoEncodingSettingsUpdate
import co.daily.settings.VideoMaxQualityUpdate
import co.daily.settings.VideoSendSettingsUpdate
import co.daily.settings.subscription.AudioSubscriptionSettingsUpdate
import co.daily.settings.subscription.MediaSubscriptionSettingsUpdate
import co.daily.settings.subscription.Subscribed
import co.daily.settings.subscription.SubscriptionProfile
import co.daily.settings.subscription.Unsubscribed
import co.daily.settings.subscription.VideoSubscriptionSettingsUpdate
import com.example.AppFlagsRepository
import com.example.RoomScreenShareRow
import com.example.ScreenShareHeartbeatResponse
import com.example.ScreenShareIdRequest
import com.example.GetScreenShareTokenRequest
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.service.ScreenShareForegroundService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

enum class ScreenShareRole {
    NONE,
    SHARING,
    WATCHING
}

data class RoomScreenShareUiState(
    val activeRoomId: String? = null,
    val activeShareRow: RoomScreenShareRow? = null,
    val currentShareId: String? = null,
    val role: ScreenShareRole = ScreenShareRole.NONE,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val remoteScreenTrack: MediaStreamTrack? = null,
    val remoteViewerCount: Int = 0
)

internal data class ScreenShareTokenPayload(
    val provider: String,
    val shareId: String,
    val roomId: String,
    val hostId: String,
    val isSharer: Boolean,
    val dailyRoomName: String,
    val roomUrl: String,
    val domain: String,
    val token: String,
    val maxHeight: Int,
    val maxFps: Int,
    val heartbeatSeconds: Int
)

internal data class ScreenShareErrorResult(
    val userMessage: String? = null,
    val hideFeature: Boolean = false,
    val closeViewerSilently: Boolean = false,
    val requiresReauth: Boolean = false
)

sealed class ScreenShareNotice {
    data class ToastMessage(val message: String) : ScreenShareNotice()
    data class ReauthRequired(val message: String) : ScreenShareNotice()
}

/**
 * Manages Daily.co video-only screen sharing inside a voice room.
 *
 * Isolation guarantees:
 * - Room audio stays 100% on Agora; Daily joins with mic OFF, camera OFF, and audio
 *   subscriptions set to Unsubscribed.
 * - Viewers never auto-join Daily; they join ONLY after tapping "Watch".
 * - Every exit path calls `leave_screen_share`, leaves Daily, and stops the mediaProjection FGS.
 * - Never logs Daily meeting tokens or Supabase auth tokens.
 */
object RoomScreenShareManager {
    private const val TAG = "RoomScreenShare"
    private const val DEFAULT_MAX_HEIGHT = 720
    private const val DEFAULT_MAX_FPS = 15
    private const val DEFAULT_HEARTBEAT_SECONDS = 30
    private const val HOST_ZERO_VIEWER_TIMEOUT_MS = 60_000L
    private const val VIEWER_BACKGROUND_TIMEOUT_MS = 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionMutex = Mutex()

    private val _state = MutableStateFlow(RoomScreenShareUiState())
    val state: StateFlow<RoomScreenShareUiState> = _state.asStateFlow()

    private val _notices = MutableSharedFlow<ScreenShareNotice>(extraBufferCapacity = 16)
    val notices: SharedFlow<ScreenShareNotice> = _notices.asSharedFlow()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var callClient: CallClient? = null

    @Volatile
    private var lifecycleRegistered = false

    private var heartbeatJob: Job? = null
    private var zeroViewerAutoStopJob: Job? = null
    private var viewerBackgroundAutoStopJob: Job? = null
    private var pendingProjectionIntent: Intent? = null

    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            val current = _state.value
            if (current.role == ScreenShareRole.WATCHING && current.currentShareId != null) {
                startViewerBackgroundTimeout()
            }
        }

        override fun onStart(owner: LifecycleOwner) {
            viewerBackgroundAutoStopJob?.cancel()
            viewerBackgroundAutoStopJob = null
        }
    }

    private fun ensureProcessLifecycleObserver(context: Context) {
        if (lifecycleRegistered) return
        appContext = context.applicationContext
        lifecycleRegistered = true
        scope.launch(Dispatchers.Main) {
            runCatching {
                ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)
            }
        }
    }

    /**
     * Hydrates active screen share state on entering a room (`room_id = eq.<room>&status = eq.active`).
     * Never joins Daily automatically.
     */
    fun onRoomEntered(context: Context, roomId: String) {
        if (roomId.isBlank()) return
        val ctx = context.applicationContext
        ensureProcessLifecycleObserver(ctx)

        val previousRoomId = _state.value.activeRoomId
        if (!previousRoomId.isNullOrBlank() && previousRoomId != roomId) {
            stopSessionAsync(ctx, "switched_room")
        }

        _state.update {
            it.copy(
                activeRoomId = roomId,
                activeShareRow = if (it.activeRoomId == roomId) it.activeShareRow else null
            )
        }

        scope.launch {
            refreshActiveShareForRoom(ctx, roomId)
        }
    }

    suspend fun refreshActiveShareForRoom(context: Context, roomId: String) {
        if (roomId.isBlank()) return
        try {
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: SupabaseSessionManager.getBearerToken(context)
            val resp = SupabaseClient.api.getActiveRoomScreenShares(
                auth = auth,
                roomIdEq = "eq.$roomId"
            )
            if (resp.isSuccessful) {
                val activeRow = resp.body()?.firstOrNull()?.takeIf {
                    it.status.equals("active", ignoreCase = true)
                }
                if (_state.value.activeRoomId == roomId) {
                    _state.update { it.copy(activeShareRow = activeRow) }
                }
            }
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "Failed fetching active room_screen_shares: ${e.javaClass.simpleName}")
        }
    }

    /**
     * Applies Realtime `postgres_changes` events on `room_screen_shares` filtered by `room_id`.
     */
    fun onRealtimeShareEvent(context: Context, event: RealtimeTableEvent.RoomScreenShareChanged) {
        val ctx = context.applicationContext
        val currentRoomId = _state.value.activeRoomId
        val eventRoomId = event.roomId
        if (!currentRoomId.isNullOrBlank() && !eventRoomId.isNullOrBlank() && eventRoomId != currentRoomId) {
            return
        }

        val isDelete = event.eventType.equals("DELETE", ignoreCase = true)
        val status = event.status?.lowercase().orEmpty()
        val shareId = event.shareId

        if (isDelete || status == "ended") {
            val active = _state.value
            val matchesCurrentShare = shareId.isNullOrBlank() ||
                active.currentShareId == shareId ||
                active.activeShareRow?.id == shareId
            if (matchesCurrentShare) {
                _state.update { it.copy(activeShareRow = null) }
                if (active.role != ScreenShareRole.NONE &&
                    (shareId.isNullOrBlank() || active.currentShareId == shareId)
                ) {
                    stopSessionAsync(ctx, "realtime_share_ended")
                }
            }
            return
        }

        if (status == "active" && !shareId.isNullOrBlank()) {
            val row = RoomScreenShareRow(
                id = shareId,
                room_id = eventRoomId ?: currentRoomId.orEmpty(),
                host_id = event.hostId.orEmpty(),
                status = "active"
            )
            _state.update { it.copy(activeShareRow = row) }
        }
    }

    /**
     * Host flow: called after safe-share countdown + `MediaProjectionManager.createScreenCaptureIntent()`
     * granted `RESULT_OK` with non-null `projectionIntent`.
     */
    fun startHostScreenShare(
        context: Context,
        roomId: String,
        roomTitle: String?,
        projectionIntent: Intent
    ) {
        val ctx = context.applicationContext
        ensureProcessLifecycleObserver(ctx)
        if (roomId.isBlank()) return

        if (!AppFlagsRepository.flags.value.screenShareEnabled) {
            return
        }

        scope.launch {
            sessionMutex.withLock {
                // Clean up any leftover Daily session cleanly first
                stopSessionLocked(ctx, reason = "restart_host_share", callBackendLeave = true)

                _state.update {
                    it.copy(
                        activeRoomId = roomId,
                        isConnecting = true,
                        isConnected = false,
                        role = ScreenShareRole.SHARING,
                        remoteScreenTrack = null
                    )
                }

                val tokenResult = fetchScreenShareToken(
                    context = ctx,
                    request = GetScreenShareTokenRequest(action = "start", room_id = roomId)
                )

                val payload = when (tokenResult) {
                    is TokenFetchOutcome.Success -> tokenResult.payload
                    is TokenFetchOutcome.Failure -> {
                        handleTokenError(ctx, tokenResult.error)
                        _state.update {
                            it.copy(
                                isConnecting = false,
                                isConnected = false,
                                role = ScreenShareRole.NONE,
                                currentShareId = null
                            )
                        }
                        return@withLock
                    }
                }

                // Register stop callback for notification Stop action or task removal
                ScreenShareForegroundService.onStopRequested = { reason ->
                    stopSessionAsync(ctx, reason)
                }

                // Android 10–15+: enter mediaProjection foreground state BEFORE startScreenShare()
                ScreenShareForegroundService.start(ctx, roomTitle)
                val fgsReady = withTimeoutOrNull(3_000L) {
                    ScreenShareForegroundService.runningFlow.first { it }
                } ?: false

                if (!fgsReady) {
                    Log.w(TAG, "ScreenShareForegroundService did not enter foreground in time")
                    ScreenShareForegroundService.stop(ctx)
                    callLeaveScreenShareBackend(ctx, payload.shareId)
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            role = ScreenShareRole.NONE,
                            currentShareId = null
                        )
                    }
                    _notices.tryEmit(ScreenShareNotice.ToastMessage("Couldn't start. Try again"))
                    return@withLock
                }

                pendingProjectionIntent = projectionIntent
                _state.update {
                    it.copy(
                        currentShareId = payload.shareId,
                        activeShareRow = RoomScreenShareRow(
                            id = payload.shareId,
                            room_id = payload.roomId,
                            host_id = payload.hostId,
                            status = "active"
                        )
                    )
                }

                val joined = joinDailyRoom(
                    context = ctx,
                    payload = payload,
                    isSharer = true
                )

                if (!joined) {
                    pendingProjectionIntent = null
                    ScreenShareForegroundService.stop(ctx)
                    callLeaveScreenShareBackend(ctx, payload.shareId)
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            role = ScreenShareRole.NONE,
                            currentShareId = null,
                            activeShareRow = null
                        )
                    }
                    _notices.tryEmit(ScreenShareNotice.ToastMessage("Couldn't start. Try again"))
                    return@withLock
                }

                val captureStarted = startDailyScreenCapture(projectionIntent)
                pendingProjectionIntent = null

                if (!captureStarted) {
                    leaveDailyClientOnly()
                    ScreenShareForegroundService.stop(ctx)
                    callLeaveScreenShareBackend(ctx, payload.shareId)
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            role = ScreenShareRole.NONE,
                            currentShareId = null,
                            activeShareRow = null
                        )
                    }
                    _notices.tryEmit(ScreenShareNotice.ToastMessage("Couldn't start. Try again"))
                    return@withLock
                }

                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = true,
                        role = ScreenShareRole.SHARING
                    )
                }

                startHeartbeatLoop(ctx, payload.shareId, payload.heartbeatSeconds)
                // Evaluate initial 0-viewer auto-stop timer immediately after joining
                evaluateHostZeroViewerTimer(ctx)
            }
        }
    }

    /**
     * Viewer flow: joins Daily ONLY when the user explicitly taps Watch.
     */
    fun startWatchingScreenShare(
        context: Context,
        roomId: String,
        shareId: String
    ) {
        val ctx = context.applicationContext
        ensureProcessLifecycleObserver(ctx)
        if (roomId.isBlank() || shareId.isBlank()) return
        if (!AppFlagsRepository.flags.value.screenShareEnabled) return

        scope.launch {
            sessionMutex.withLock {
                stopSessionLocked(ctx, reason = "restart_watch", callBackendLeave = true)

                _state.update {
                    it.copy(
                        activeRoomId = roomId,
                        currentShareId = shareId,
                        isConnecting = true,
                        isConnected = false,
                        role = ScreenShareRole.WATCHING,
                        remoteScreenTrack = null
                    )
                }

                val tokenResult = fetchScreenShareToken(
                    context = ctx,
                    request = GetScreenShareTokenRequest(action = "watch", share_id = shareId)
                )

                val payload = when (tokenResult) {
                    is TokenFetchOutcome.Success -> tokenResult.payload
                    is TokenFetchOutcome.Failure -> {
                        handleTokenError(ctx, tokenResult.error)
                        _state.update {
                            it.copy(
                                isConnecting = false,
                                isConnected = false,
                                role = ScreenShareRole.NONE,
                                currentShareId = null,
                                remoteScreenTrack = null
                            )
                        }
                        return@withLock
                    }
                }

                val joined = joinDailyRoom(
                    context = ctx,
                    payload = payload,
                    isSharer = false
                )

                if (!joined) {
                    callLeaveScreenShareBackend(ctx, payload.shareId)
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            role = ScreenShareRole.NONE,
                            currentShareId = null,
                            remoteScreenTrack = null
                        )
                    }
                    _notices.tryEmit(ScreenShareNotice.ToastMessage("Couldn't start. Try again"))
                    return@withLock
                }

                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = true,
                        role = ScreenShareRole.WATCHING,
                        currentShareId = payload.shareId
                    )
                }

                updateRemoteScreenTrackFromClient()
                startHeartbeatLoop(ctx, payload.shareId, payload.heartbeatSeconds)
            }
        }
    }

    /**
     * Stops active sharing/watching from any UI button, notification action, or lifecycle exit.
     * Safe and idempotent to call even when no session is active.
     */
    fun stopSessionAsync(context: Context, reason: String) {
        val ctx = context.applicationContext
        val current = _state.value
        if (current.role == ScreenShareRole.NONE && current.currentShareId == null && !current.isConnecting) {
            ScreenShareForegroundService.stop(ctx)
            return
        }
        scope.launch {
            sessionMutex.withLock {
                stopSessionLocked(ctx, reason = reason, callBackendLeave = true)
            }
        }
    }

    /**
     * Called when leaving/ending a room or on Activity finish/destroy.
     */
    fun onRoomExited(context: Context, roomId: String? = null, reason: String = "left_room") {
        val ctx = context.applicationContext
        val current = _state.value
        if (roomId != null && current.activeRoomId != null && current.activeRoomId != roomId) {
            return
        }
        _state.update {
            it.copy(
                activeRoomId = null,
                activeShareRow = null,
                remoteScreenTrack = null,
                remoteViewerCount = 0
            )
        }
        stopSessionAsync(ctx, reason)
    }

    private suspend fun stopSessionLocked(
        context: Context,
        reason: String,
        callBackendLeave: Boolean
    ) {
        val shareIdToLeave = _state.value.currentShareId
        val wasRole = _state.value.role
        Log.i(TAG, "Stopping screen share session: role=$wasRole, reason=$reason, shareId=$shareIdToLeave")

        heartbeatJob?.cancel()
        heartbeatJob = null
        zeroViewerAutoStopJob?.cancel()
        zeroViewerAutoStopJob = null
        viewerBackgroundAutoStopJob?.cancel()
        viewerBackgroundAutoStopJob = null
        pendingProjectionIntent = null
        ScreenShareForegroundService.onStopRequested = null

        _state.update {
            it.copy(
                currentShareId = null,
                role = ScreenShareRole.NONE,
                isConnecting = false,
                isConnected = false,
                remoteScreenTrack = null,
                remoteViewerCount = 0,
                activeShareRow = if (wasRole == ScreenShareRole.SHARING) null else it.activeShareRow
            )
        }

        leaveDailyClientOnly()
        ScreenShareForegroundService.stop(context)

        if (callBackendLeave && !shareIdToLeave.isNullOrBlank()) {
            withContext(NonCancellable + Dispatchers.IO) {
                callLeaveScreenShareBackend(context, shareIdToLeave)
            }
        }
    }

    private suspend fun joinDailyRoom(
        context: Context,
        payload: ScreenShareTokenPayload,
        isSharer: Boolean
    ): Boolean {
        val client = withContext(Dispatchers.Main) {
            runCatching {
                callClient?.release()
            }
            val newClient = CallClient(context)
            newClient.addListener(dailyListener)
            callClient = newClient
            newClient
        }

        // Subscribe strictly to remote screenVideo for viewers; disable ALL audio in Daily
        // so Agora remains the sole room audio engine.
        val baseProfile = SubscriptionProfile("base")
        val subProfileSettings = MediaSubscriptionSettingsUpdate(
            microphone = AudioSubscriptionSettingsUpdate(subscriptionState = Unsubscribed()),
            camera = VideoSubscriptionSettingsUpdate(subscriptionState = Unsubscribed()),
            screenAudio = AudioSubscriptionSettingsUpdate(subscriptionState = Unsubscribed()),
            screenVideo = VideoSubscriptionSettingsUpdate(
                subscriptionState = if (isSharer) Unsubscribed() else Subscribed()
            )
        )
        val subResult = CompletableDeferred<Boolean>()
        withContext(Dispatchers.Main) {
            client.updateSubscriptionProfiles(
                mapOf(baseProfile to subProfileSettings),
                RequestListener { res ->
                    subResult.complete(!res.isError)
                }
            )
        }
        withTimeoutOrNull(5_000L) { subResult.await() }

        val screenHeightPx = context.resources.displayMetrics.heightPixels.coerceAtLeast(1)
        val targetMaxHeight = payload.maxHeight.coerceIn(360, 1080)
        val scaleDown = (screenHeightPx.toFloat() / targetMaxHeight.toFloat()).coerceIn(1.0f, 4.0f)
        val targetMaxFps = payload.maxFps.coerceIn(5, 30)

        val screenPublishingUpdate = if (isSharer) {
            ScreenVideoPublishingSettingsUpdate(
                isPublishing = Enable(),
                sendSettings = VideoSendSettingsUpdate(
                    maxQuality = VideoMaxQualityUpdate.high,
                    encodings = VideoEncodingsSettingsUpdate(
                        settings = mapOf(
                            VideoMaxQualityUpdate.high to VideoEncodingSettingsUpdate(
                                maxBitrate = BitRate(900_000),
                                maxFramerate = FrameRate(targetMaxFps),
                                scaleResolutionDownBy = Scale(scaleDown)
                            )
                        )
                    ),
                    allowAdaptiveLayers = Disable()
                )
            )
        } else {
            ScreenVideoPublishingSettingsUpdate(isPublishing = Disable())
        }

        val clientSettings = ClientSettingsUpdate(
            inputSettings = InputSettingsUpdate(
                camera = CameraInputSettingsUpdate(isEnabled = Disable()),
                microphone = MicrophoneInputSettingsUpdate(isEnabled = Disable())
            ),
            publishingSettings = PublishingSettingsUpdate(
                camera = CameraPublishingSettingsUpdate(isPublishing = Disable()),
                microphone = MicrophonePublishingSettingsUpdate(isPublishing = Disable()),
                screenVideo = screenPublishingUpdate
            )
        )

        val joinDone = CompletableDeferred<Boolean>()
        withContext(Dispatchers.Main) {
            client.join(
                payload.roomUrl,
                MeetingToken(payload.token),
                clientSettings,
                RequestListenerWithData { result ->
                    if (result.isError) {
                        Log.w(TAG, "Daily join error: ${result.error}")
                        joinDone.complete(false)
                    } else {
                        joinDone.complete(true)
                    }
                }
            )
        }

        return withTimeoutOrNull(15_000L) { joinDone.await() } ?: false
    }

    private suspend fun startDailyScreenCapture(projectionIntent: Intent): Boolean {
        val client = callClient ?: return false
        val started = CompletableDeferred<Boolean>()
        withContext(Dispatchers.Main) {
            try {
                client.startScreenShare(
                    projectionIntent,
                    RequestListener { result ->
                        if (result.isError) {
                            Log.w(TAG, "Daily startScreenShare error: ${result.error}")
                            started.complete(false)
                        } else {
                            started.complete(true)
                        }
                    }
                )
            } catch (se: SecurityException) {
                Log.e(TAG, "SecurityException starting screen capture: ${se.message}")
                started.complete(false)
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting screen capture: ${e.message}")
                started.complete(false)
            }
        }
        return withTimeoutOrNull(10_000L) { started.await() } ?: false
    }

    private suspend fun leaveDailyClientOnly() {
        val client = callClient ?: return
        withContext(NonCancellable + Dispatchers.Main) {
            runCatching {
                val stopDone = CompletableDeferred<Unit>()
                client.stopScreenShare(RequestListener { stopDone.complete(Unit) })
                withTimeoutOrNull(1_500L) { stopDone.await() }
            }
            runCatching {
                val leaveDone = CompletableDeferred<Unit>()
                client.leave(RequestListener { leaveDone.complete(Unit) })
                withTimeoutOrNull(2_000L) { leaveDone.await() }
            }
            runCatching {
                client.removeListener(dailyListener)
                client.release()
            }
            if (callClient === client) {
                callClient = null
            }
        }
    }

    private val dailyListener = object : CallClientListener {
        override fun onCallStateUpdated(state: CallState) {
            if (state == CallState.left) {
                val ctx = appContext ?: return
                val current = _state.value
                if (current.isConnected && current.currentShareId != null) {
                    stopSessionAsync(ctx, "daily_call_left")
                }
            }
        }

        override fun onParticipantJoined(participant: Participant) {
            handleDailyParticipantsChanged()
        }

        override fun onParticipantUpdated(participant: Participant) {
            handleDailyParticipantsChanged()
        }

        override fun onParticipantLeft(participant: Participant, reason: ParticipantLeftReason) {
            handleDailyParticipantsChanged()
        }

        override fun onError(message: String) {
            Log.w(TAG, "Daily CallClient onError: $message")
        }
    }

    private fun handleDailyParticipantsChanged() {
        val current = _state.value
        val client = callClient ?: return
        val remoteCount = runCatching {
            client.participants().all.values.count { !it.info.isLocal }
        }.getOrDefault(0)

        _state.update { it.copy(remoteViewerCount = remoteCount) }

        if (current.role == ScreenShareRole.SHARING && current.isConnected) {
            val ctx = appContext
            if (ctx != null) {
                evaluateHostZeroViewerTimer(ctx, remoteCount)
            }
        } else if (current.role == ScreenShareRole.WATCHING) {
            updateRemoteScreenTrackFromClient()
        }
    }

    private fun updateRemoteScreenTrackFromClient() {
        val client = callClient ?: return
        val remoteScreenTrack = runCatching {
            client.participants().all.values
                .firstOrNull { !it.info.isLocal && it.media?.screenVideo?.track != null }
                ?.media?.screenVideo?.track
        }.getOrNull()

        _state.update { it.copy(remoteScreenTrack = remoteScreenTrack) }
    }

    private fun evaluateHostZeroViewerTimer(context: Context, remoteCountOverride: Int? = null) {
        val count = remoteCountOverride ?: _state.value.remoteViewerCount
        if (count > 0) {
            zeroViewerAutoStopJob?.cancel()
            zeroViewerAutoStopJob = null
            return
        }
        if (zeroViewerAutoStopJob?.isActive == true) return
        zeroViewerAutoStopJob = scope.launch {
            delay(HOST_ZERO_VIEWER_TIMEOUT_MS)
            val current = _state.value
            if (current.role == ScreenShareRole.SHARING && current.remoteViewerCount == 0 && current.currentShareId != null) {
                Log.i(TAG, "Host has had 0 viewers for 60s; auto-stopping screen share to save minutes")
                _notices.tryEmit(ScreenShareNotice.ToastMessage("Screen share stopped (no viewers)"))
                sessionMutex.withLock {
                    stopSessionLocked(context, reason = "auto_stop_zero_viewers_60s", callBackendLeave = true)
                }
            }
        }
    }

    private fun startViewerBackgroundTimeout() {
        viewerBackgroundAutoStopJob?.cancel()
        viewerBackgroundAutoStopJob = scope.launch {
            delay(VIEWER_BACKGROUND_TIMEOUT_MS)
            val current = _state.value
            val ctx = appContext
            if (ctx != null && current.role == ScreenShareRole.WATCHING && current.currentShareId != null) {
                Log.i(TAG, "Viewer remained in background > 60s; stopping Daily watch cleanly")
                sessionMutex.withLock {
                    stopSessionLocked(ctx, reason = "viewer_background_timeout_60s", callBackendLeave = true)
                }
            }
        }
    }

    private fun startHeartbeatLoop(context: Context, shareId: String, heartbeatSeconds: Int) {
        heartbeatJob?.cancel()
        val intervalMs = heartbeatSeconds.coerceIn(10, 120) * 1000L
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                val activeShareId = _state.value.currentShareId
                if (activeShareId != shareId) break

                val stillActive = sendHeartbeat(context, shareId)
                if (!stillActive) {
                    Log.i(TAG, "screen_share_heartbeat returned active=false; closing screen share")
                    sessionMutex.withLock {
                        stopSessionLocked(context, reason = "heartbeat_inactive", callBackendLeave = true)
                    }
                    break
                }
            }
        }
    }

    private suspend fun sendHeartbeat(context: Context, shareId: String): Boolean {
        return try {
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: SupabaseSessionManager.getBearerToken(context)
            var resp = SupabaseClient.api.screenShareHeartbeat(
                auth = auth,
                body = ScreenShareIdRequest(p_share_id = shareId)
            )
            if (resp.code() == 401) {
                SupabaseSessionManager.forceRefreshSession(context)
                val refreshed = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)
                resp = SupabaseClient.api.screenShareHeartbeat(
                    auth = refreshed,
                    body = ScreenShareIdRequest(p_share_id = shareId)
                )
            }
            if (resp.isSuccessful) {
                resp.body()?.active == true
            } else {
                // Only force-close if server explicitly returned 404/410 or active=false;
                // transient 5xx can let the next 30s tick or server 90s sweep decide.
                resp.code() !in setOf(404, 410, 403)
            }
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "Heartbeat network error: ${e.javaClass.simpleName}")
            true
        }
    }

    private suspend fun callLeaveScreenShareBackend(context: Context, shareId: String) {
        try {
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: SupabaseSessionManager.getBearerToken(context)
            var resp = SupabaseClient.api.leaveScreenShare(
                auth = auth,
                body = ScreenShareIdRequest(p_share_id = shareId)
            )
            if (resp.code() == 401) {
                SupabaseSessionManager.forceRefreshSession(context)
                val refreshed = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)
                resp = SupabaseClient.api.leaveScreenShare(
                    auth = refreshed,
                    body = ScreenShareIdRequest(p_share_id = shareId)
                )
            }
            Log.d(TAG, "leave_screen_share completed with HTTP ${resp.code()}")
        } catch (e: Exception) {
            Log.w(TAG, "leave_screen_share error: ${e.javaClass.simpleName}")
        }
    }

    private sealed class TokenFetchOutcome {
        data class Success(val payload: ScreenShareTokenPayload) : TokenFetchOutcome()
        data class Failure(val error: ScreenShareErrorResult) : TokenFetchOutcome()
    }

    private suspend fun fetchScreenShareToken(
        context: Context,
        request: GetScreenShareTokenRequest
    ): TokenFetchOutcome {
        return try {
            SupabaseSessionManager.ensureActiveSession(context)
            var bearer = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return TokenFetchOutcome.Failure(mapErrorCodeToUserMessage("unauthenticated"))

            var resp = SupabaseClient.api.getScreenShareToken(auth = bearer, body = request)
            if (resp.code() == 401) {
                SupabaseSessionManager.forceRefreshSession(context)
                bearer = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: return TokenFetchOutcome.Failure(mapErrorCodeToUserMessage("unauthenticated"))
                resp = SupabaseClient.api.getScreenShareToken(auth = bearer, body = request)
            }

            if (resp.isSuccessful) {
                val raw = resp.body()?.string().orEmpty()
                val parsed = parseTokenSuccessJson(raw)
                if (parsed != null) {
                    TokenFetchOutcome.Success(parsed)
                } else {
                    val errorCode = extractErrorCodeFromJson(raw)
                    TokenFetchOutcome.Failure(mapErrorCodeToUserMessage(errorCode ?: "daily_token_failed"))
                }
            } else {
                val errRaw = resp.errorBody()?.string().orEmpty()
                val errorCode = extractErrorCodeFromJson(errRaw)
                    ?: if (resp.code() == 401) "unauthenticated" else "daily_token_failed"
                if (errorCode == "unauthenticated") {
                    SupabaseSessionManager.forceRefreshSession(context)
                    val retriedBearer = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    if (retriedBearer != null) {
                        val retryResp = SupabaseClient.api.getScreenShareToken(auth = retriedBearer, body = request)
                        if (retryResp.isSuccessful) {
                            val retryRaw = retryResp.body()?.string().orEmpty()
                            val parsed = parseTokenSuccessJson(retryRaw)
                            if (parsed != null) {
                                return TokenFetchOutcome.Success(parsed)
                            }
                        }
                    }
                }
                TokenFetchOutcome.Failure(mapErrorCodeToUserMessage(errorCode))
            }
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "get-screenshare-token request failed: ${e.javaClass.simpleName}")
            TokenFetchOutcome.Failure(mapErrorCodeToUserMessage("daily_token_failed"))
        }
    }

    private fun handleTokenError(context: Context, error: ScreenShareErrorResult) {
        if (error.hideFeature) {
            // Re-sync flags from server so UI hides screen share cleanly
            AppFlagsRepository.refreshAsync(context)
        }
        if (error.closeViewerSilently) {
            _state.update { it.copy(activeShareRow = null, remoteScreenTrack = null) }
        }
        if (error.requiresReauth) {
            _notices.tryEmit(ScreenShareNotice.ReauthRequired("Session expired. Please sign in again."))
        } else if (!error.userMessage.isNullOrBlank()) {
            _notices.tryEmit(ScreenShareNotice.ToastMessage(error.userMessage))
        }
    }

    internal fun parseTokenSuccessJson(raw: String): ScreenShareTokenPayload? {
        if (raw.isBlank() || !raw.trimStart().startsWith("{")) return null
        return try {
            val obj = JSONObject(raw)
            val token = obj.optString("token").trim()
            val roomUrl = obj.optString("room_url").trim()
            val shareId = obj.optString("share_id").trim()
            if (token.isBlank() || roomUrl.isBlank() || shareId.isBlank()) return null

            val qualityObj = obj.optJSONObject("screen_quality")
            val maxHeight = qualityObj?.optInt("max_height", DEFAULT_MAX_HEIGHT) ?: DEFAULT_MAX_HEIGHT
            val maxFps = qualityObj?.optInt("max_fps", DEFAULT_MAX_FPS) ?: DEFAULT_MAX_FPS
            val heartbeatSec = obj.optInt("heartbeat_seconds", DEFAULT_HEARTBEAT_SECONDS)

            ScreenShareTokenPayload(
                provider = obj.optString("provider", "DAILY"),
                shareId = shareId,
                roomId = obj.optString("room_id", ""),
                hostId = obj.optString("host_id", ""),
                isSharer = obj.optBoolean("is_sharer", false),
                dailyRoomName = obj.optString("daily_room_name", ""),
                roomUrl = roomUrl,
                domain = obj.optString("domain", ""),
                token = token,
                maxHeight = maxHeight.coerceIn(360, 1080),
                maxFps = maxFps.coerceIn(5, 30),
                heartbeatSeconds = heartbeatSec.coerceIn(10, 120)
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun extractErrorCodeFromJson(raw: String): String? {
        if (raw.isBlank() || !raw.trimStart().startsWith("{")) return null
        return try {
            JSONObject(raw).optString("error").trim().ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    internal fun mapErrorCodeToUserMessage(code: String?): ScreenShareErrorResult {
        return when (code?.trim()?.lowercase()) {
            "screen_share_disabled" -> ScreenShareErrorResult(
                userMessage = null,
                hideFeature = true
            )
            "host_only" -> ScreenShareErrorResult(
                userMessage = "Only the host can share"
            )
            "host_away" -> ScreenShareErrorResult(
                userMessage = "Host is away"
            )
            "room_ended", "share_ended" -> ScreenShareErrorResult(
                userMessage = null,
                closeViewerSilently = true
            )
            "not_in_room" -> ScreenShareErrorResult(
                userMessage = "Join the room first"
            )
            "blocked", "user_restricted" -> ScreenShareErrorResult(
                userMessage = "You can't watch this"
            )
            "viewer_limit" -> ScreenShareErrorResult(
                userMessage = "Too many people are watching"
            )
            "daily_quota_reached" -> ScreenShareErrorResult(
                userMessage = "Screen share is resting for now, try later"
            )
            "daily_not_configured", "daily_room_setup_failed", "daily_token_failed" -> ScreenShareErrorResult(
                userMessage = "Couldn't start. Try again"
            )
            "unauthenticated" -> ScreenShareErrorResult(
                userMessage = "Session expired. Please sign in again.",
                requiresReauth = true
            )
            else -> ScreenShareErrorResult(
                userMessage = "Couldn't start. Try again"
            )
        }
    }
}
