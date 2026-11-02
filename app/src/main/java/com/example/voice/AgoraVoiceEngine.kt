package com.example.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class AgoraVoiceEngine(
    private val context: Context,
    private val onTokenWillExpireCallback: (() -> Unit)? = null
) : VoiceEngine {

    companion object {
        private const val TAG = "AgoraVoiceEngine"
        private const val VOLUME_THRESHOLD = 15
    }

    private val _connectionState = MutableStateFlow<VoiceConnectionState>(VoiceConnectionState.Idle)
    override val connectionState: StateFlow<VoiceConnectionState> = _connectionState.asStateFlow()

    private val _speakingUserIds = MutableStateFlow<Set<String>>(emptySet())
    override val speakingUserIds: StateFlow<Set<String>> = _speakingUserIds.asStateFlow()

    private val _remoteUserIds = MutableStateFlow<Set<String>>(emptySet())
    override val remoteUserIds: StateFlow<Set<String>> = _remoteUserIds.asStateFlow()

    private var rtcEngine: RtcEngine? = null
    private var joinConfig: VoiceJoinConfig? = null
    private var localUid: Int = 0

    // Map Agora integer UID to Supabase string userId
    private val uidToAccountMap = ConcurrentHashMap<Int, String>()

    // Audio Manager & Focus
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isHeadsetPlugged = false

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == Intent.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", -1)
                isHeadsetPlugged = (state == 1)
                Log.d(TAG, "Headset plug event: connected=$isHeadsetPlugged")
                updateAudioRouting()
            } else if (action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                Log.d(TAG, "Audio becoming noisy (headset disconnected)")
                isHeadsetPlugged = false
                updateAudioRouting()
            }
        }
    }

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d(TAG, "onAudioFocusChange: focusChange=$focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                rtcEngine?.muteAllRemoteAudioStreams(true)
                rtcEngine?.muteLocalAudioStream(true)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                rtcEngine?.adjustPlaybackSignalVolume(30)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                rtcEngine?.adjustPlaybackSignalVolume(100)
                rtcEngine?.muteAllRemoteAudioStreams(false)
                val shouldUnmuteMic = com.example.realtime.RoomMicController.micOn.value
                val isSpeaker = joinConfig?.canPublish == true
                if (isSpeaker && shouldUnmuteMic) {
                    rtcEngine?.muteLocalAudioStream(false)
                }
            }
        }
    }

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onLocalUserRegistered(uid: Int, userAccount: String?) {
            Log.d(TAG, "onLocalUserRegistered: uid=$uid, account=$userAccount")
            localUid = uid
            if (!userAccount.isNullOrBlank()) {
                uidToAccountMap[uid] = userAccount
            }
        }

        override fun onUserInfoUpdated(uid: Int, userInfo: UserInfo?) {
            Log.d(TAG, "onUserInfoUpdated: uid=$uid, account=${userInfo?.userAccount}")
            if (userInfo != null && !userInfo.userAccount.isNullOrBlank()) {
                uidToAccountMap[uid] = userInfo.userAccount
                // Update remoteUserIds set
                if (uid != localUid && uid != 0) {
                    val updated = _remoteUserIds.value.toMutableSet()
                    updated.add(userInfo.userAccount)
                    _remoteUserIds.value = updated
                }
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "onUserJoined: uid=$uid, elapsed=$elapsed")
            var account = uidToAccountMap[uid]
            if (account == null) {
                val info = UserInfo()
                val ret = rtcEngine?.getUserInfoByUid(uid, info) ?: -1
                if (ret == 0 && !info.userAccount.isNullOrBlank()) {
                    account = info.userAccount
                    uidToAccountMap[uid] = account
                }
            }
            if (account != null && account != joinConfig?.userId) {
                val updated = _remoteUserIds.value.toMutableSet()
                updated.add(account)
                _remoteUserIds.value = updated
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "onUserOffline: uid=$uid, reason=$reason")
            val account = uidToAccountMap.remove(uid)
            if (account != null) {
                val remotes = _remoteUserIds.value.toMutableSet()
                remotes.remove(account)
                _remoteUserIds.value = remotes

                val speaking = _speakingUserIds.value.toMutableSet()
                speaking.remove(account)
                _speakingUserIds.value = speaking
            }
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.d(TAG, "onConnectionStateChanged: state=$state, reason=$reason")
            when (state) {
                Constants.CONNECTION_STATE_CONNECTING -> {
                    _connectionState.value = VoiceConnectionState.Connecting
                }
                Constants.CONNECTION_STATE_CONNECTED -> {
                    _connectionState.value = VoiceConnectionState.Connected
                }
                Constants.CONNECTION_STATE_RECONNECTING -> {
                    _connectionState.value = VoiceConnectionState.Reconnecting
                }
                Constants.CONNECTION_STATE_FAILED -> {
                    _connectionState.value = VoiceConnectionState.Failed("Agora connection failed (reason=$reason)")
                }
                Constants.CONNECTION_STATE_DISCONNECTED -> {
                    if (_connectionState.value != VoiceConnectionState.Idle) {
                        _connectionState.value = VoiceConnectionState.Reconnecting
                    }
                }
            }
        }

        override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
            if (speakers == null) {
                _speakingUserIds.value = emptySet()
                return
            }
            val activeSpeakers = mutableSetOf<String>()
            for (speaker in speakers) {
                if (speaker.volume >= VOLUME_THRESHOLD) {
                    val targetUid = speaker.uid
                    if (targetUid == 0 || targetUid == localUid) {
                        joinConfig?.userId?.let { activeSpeakers.add(it) }
                    } else {
                        var account = uidToAccountMap[targetUid]
                        if (account == null) {
                            val info = UserInfo()
                            if (rtcEngine?.getUserInfoByUid(targetUid, info) == 0 && !info.userAccount.isNullOrBlank()) {
                                account = info.userAccount
                                uidToAccountMap[targetUid] = account
                            }
                        }
                        if (account != null) {
                            activeSpeakers.add(account)
                        }
                    }
                }
            }
            _speakingUserIds.value = activeSpeakers
        }

        override fun onTokenPrivilegeWillExpire(token: String?) {
            Log.d(TAG, "onTokenPrivilegeWillExpire callback received")
            onTokenWillExpireCallback?.invoke()
        }

        override fun onRequestToken() {
            Log.d(TAG, "onRequestToken callback received")
            onTokenWillExpireCallback?.invoke()
        }

        override fun onError(err: Int) {
            Log.d(TAG, "Agora onError code=$err")
            if (err == Constants.ERR_TOKEN_EXPIRED || err == Constants.ERR_INVALID_TOKEN) {
                _connectionState.value = VoiceConnectionState.Failed("Agora token invalid or expired (code=$err)")
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            Log.d(TAG, "onLeaveChannel")
            _speakingUserIds.value = emptySet()
            _remoteUserIds.value = emptySet()
            _connectionState.value = VoiceConnectionState.Idle
        }
    }

    override suspend fun join(cfg: VoiceJoinConfig) {
        joinConfig = cfg
        _connectionState.value = VoiceConnectionState.Connecting
        Log.d(TAG, "Joining Agora channel roomId=${cfg.roomId}, userId=${cfg.userId}, canPublish=${cfg.canPublish}")

        requestAudioFocus()
        registerHeadsetReceiver()

        try {
            val rtcConfig = RtcEngineConfig().apply {
                mContext = context.applicationContext
                mAppId = cfg.appId
                mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
                mEventHandler = rtcEventHandler
            }
            rtcEngine = RtcEngine.create(rtcConfig)
            val engine = rtcEngine ?: run {
                _connectionState.value = VoiceConnectionState.Failed("Failed to instantiate Agora RtcEngine")
                return
            }

            // Sync with existing AgoraVoiceManager reflection delegate for backward compat
            com.example.realtime.AgoraVoiceManager.setRtcEngine(engine)

            // Audio config: speakerphone default
            engine.setDefaultAudioRoutetoSpeakerphone(true)
            engine.setEnableSpeakerphone(!isHeadsetPlugged)

            // Volume indication: interval=200ms, smooth=3, reportVad=true
            engine.enableAudioVolumeIndication(200, 3, true)

            // Audio only profile
            engine.enableAudio()
            engine.disableVideo()

            val options = ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
                clientRoleType = if (cfg.canPublish) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
                publishMicrophoneTrack = cfg.canPublish
                autoSubscribeAudio = true
            }

            val joinRes = engine.joinChannelWithUserAccount(cfg.token, cfg.roomId, cfg.userId, options)
            Log.d(TAG, "joinChannelWithUserAccount result code=$joinRes")
            if (joinRes != 0) {
                _connectionState.value = VoiceConnectionState.Failed("Agora joinChannel failed (code=$joinRes)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Agora join", e)
            _connectionState.value = VoiceConnectionState.Failed(e.localizedMessage ?: "Unknown join error")
        }
    }

    override suspend fun leave() {
        Log.d(TAG, "Leaving Agora voice channel")
        abandonAudioFocus()
        unregisterHeadsetReceiver()

        try {
            rtcEngine?.leaveChannel()
        } catch (e: Exception) {
            Log.d(TAG, "leaveChannel exception: ${e.message}")
        }

        try {
            RtcEngine.destroy()
        } catch (e: Exception) {
            Log.d(TAG, "RtcEngine.destroy exception: ${e.message}")
        }

        rtcEngine = null
        com.example.realtime.AgoraVoiceManager.setRtcEngine(null)
        uidToAccountMap.clear()
        _speakingUserIds.value = emptySet()
        _remoteUserIds.value = emptySet()
        _connectionState.value = VoiceConnectionState.Idle
    }

    override fun setMicEnabled(enabled: Boolean) {
        val isSpeaker = joinConfig?.canPublish == true
        Log.d(TAG, "setMicEnabled enabled=$enabled, isSpeaker=$isSpeaker")
        if (!isSpeaker && enabled) {
            Log.d(TAG, "Ignored setMicEnabled(true) because user is not a publisher")
            return
        }
        try {
            rtcEngine?.muteLocalAudioStream(!enabled)
            rtcEngine?.enableLocalAudio(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Error in setMicEnabled", e)
        }
    }

    override suspend fun upgradeToPublisher(newToken: String) {
        Log.d(TAG, "upgradeToPublisher: renewing token and setting client role BROADCASTER")
        joinConfig = joinConfig?.copy(canPublish = true)
        try {
            // 1. Call renewToken(newToken) first
            rtcEngine?.renewToken(newToken)

            // 2. Update media options to BROADCASTER with microphone track
            val options = ChannelMediaOptions().apply {
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                publishMicrophoneTrack = true
            }
            rtcEngine?.updateChannelMediaOptions(options)
            rtcEngine?.enableLocalAudio(true)

            val micOn = com.example.realtime.RoomMicController.micOn.value
            rtcEngine?.muteLocalAudioStream(!micOn)
        } catch (e: Exception) {
            Log.e(TAG, "Error during upgradeToPublisher", e)
        }
    }

    override fun downgradeToListener() {
        Log.d(TAG, "downgradeToListener: setting client role AUDIENCE")
        joinConfig = joinConfig?.copy(canPublish = false)
        try {
            val options = ChannelMediaOptions().apply {
                clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                publishMicrophoneTrack = false
            }
            rtcEngine?.updateChannelMediaOptions(options)
            rtcEngine?.muteLocalAudioStream(true)
            rtcEngine?.enableLocalAudio(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error during downgradeToListener", e)
        }
    }

    override fun renewToken(newToken: String) {
        Log.d(TAG, "renewToken called for Agora")
        try {
            rtcEngine?.renewToken(newToken)
        } catch (e: Exception) {
            Log.e(TAG, "Error renewing token", e)
        }
    }

    private fun updateAudioRouting() {
        val speakerphone = !isHeadsetPlugged
        Log.d(TAG, "updateAudioRouting: speakerphone=$speakerphone")
        rtcEngine?.setEnableSpeakerphone(speakerphone)
    }

    private fun registerHeadsetReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_HEADSET_PLUG)
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            }
            context.registerReceiver(headsetReceiver, filter)
        } catch (e: Exception) {
            Log.d(TAG, "registerHeadsetReceiver error: ${e.message}")
        }
    }

    private fun unregisterHeadsetReceiver() {
        try {
            context.unregisterReceiver(headsetReceiver)
        } catch (_: Exception) {}
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .build()
                audioFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusListener,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "requestAudioFocus error: ${e.message}")
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusListener)
            }
        } catch (_: Exception) {}
    }
}
