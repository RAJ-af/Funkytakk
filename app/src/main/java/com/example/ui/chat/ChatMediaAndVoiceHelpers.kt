package com.example.ui.chat

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.CloudAiManager
import com.example.diagnostics.CrashAndErrorReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ===========================================================================
// 1. Voice Playback Controller (ExoPlayer + Cache Decryption)
// ===========================================================================

@OptIn(UnstableApi::class)
class VoicePlaybackController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null

    private val _currentPlayingId = mutableStateOf<String?>(null)
    val currentPlayingId: String? get() = _currentPlayingId.value

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: Boolean get() = _isPlaying.value

    private val _isLoading = mutableStateOf(false)
    val isLoading: Boolean get() = _isLoading.value

    private val _positionMs = mutableStateOf(0L)
    val positionMs: Long get() = _positionMs.value

    private val _durationMs = mutableStateOf(0L)
    val durationMs: Long get() = _durationMs.value

    private val _speed = mutableStateOf(1.0f)
    val speed: Float get() = _speed.value

    private val _playedIds = mutableStateListOf<String>()
    private var currentOnError: ((String) -> Unit)? = null

    fun hasPlayed(messageId: String): Boolean = _playedIds.contains(messageId)

    init {
        try {
            val speechAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build()

            exoPlayer = ExoPlayer.Builder(context).build().apply {
                setAudioAttributes(speechAttributes, true)
                setHandleAudioBecomingNoisy(true)
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            _currentPlayingId.value?.let { id ->
                                if (!_playedIds.contains(id)) _playedIds.add(id)
                            }
                            this@VoicePlaybackController._isPlaying.value = false
                            this@VoicePlaybackController._positionMs.value = 0L
                            stopProgressPolling()
                        } else if (playbackState == Player.STATE_READY) {
                            _currentPlayingId.value?.let { id ->
                                if (!_playedIds.contains(id)) _playedIds.add(id)
                            }
                            this@VoicePlaybackController._isLoading.value = false
                            this@VoicePlaybackController._durationMs.value = duration.coerceAtLeast(0L)
                        } else if (playbackState == Player.STATE_BUFFERING) {
                            this@VoicePlaybackController._isLoading.value = true
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        this@VoicePlaybackController._isPlaying.value = playing
                        if (playing) {
                            _currentPlayingId.value?.let { id ->
                                if (!_playedIds.contains(id)) _playedIds.add(id)
                            }
                            startProgressPolling()
                        } else {
                            stopProgressPolling()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("VoicePlayback", "ExoPlayer playback error: ${error.errorCodeName}", error)
                        CrashAndErrorReporter.recordNonFatal(
                            source = "VoicePlayback",
                            message = "Audio error (${error.errorCodeName})",
                            throwable = error
                        )
                        this@VoicePlaybackController._isLoading.value = false
                        this@VoicePlaybackController._isPlaying.value = false
                        stopProgressPolling()
                        currentOnError?.invoke("Could not play audio file (${error.errorCodeName})")
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("VoicePlayback", "Failed to initialize ExoPlayer", e)
            CrashAndErrorReporter.recordNonFatal("VoicePlayback", "Failed to init audio player", e)
        }
    }

    private fun ensureNormalMediaAudioRouting() {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (am.mode != AudioManager.MODE_NORMAL) {
                am.mode = AudioManager.MODE_NORMAL
            }
        } catch (_: Exception) {
        }
    }

    fun playOrPause(
        messageId: String,
        audioSource: String,
        fallbackDurationMs: Long = 0L,
        seekFraction: Float? = null,
        onError: (String) -> Unit = {}
    ) {
        val player = exoPlayer ?: run {
            onError("Audio player is not available")
            return
        }
        currentOnError = onError
        ensureNormalMediaAudioRouting()

        if (_currentPlayingId.value == messageId && seekFraction == null) {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
            return
        }

        if (_currentPlayingId.value == messageId && seekFraction != null) {
            seekTo(seekFraction)
            if (!player.isPlaying) player.play()
            return
        }

        // Switch to a new track
        player.stop()
        _currentPlayingId.value = messageId
        _isLoading.value = true
        _positionMs.value = 0L
        _durationMs.value = fallbackDurationMs

        scope.launch {
            try {
                val resolvedFile = resolveAudioSourceToLocalFile(context, audioSource)
                if (resolvedFile == null || !resolvedFile.exists() || resolvedFile.length() == 0L) {
                    withContext(Dispatchers.Main) {
                        _isLoading.value = false
                        _currentPlayingId.value = null
                        onError("Audio file could not be loaded")
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    val mediaItem = MediaItem.fromUri(Uri.fromFile(resolvedFile))
                    player.setMediaItem(mediaItem)
                    player.setPlaybackSpeed(_speed.value)
                    player.prepare()
                    if (seekFraction != null && fallbackDurationMs > 0L) {
                        player.seekTo((fallbackDurationMs * seekFraction.coerceIn(0f, 1f)).toLong())
                    }
                    player.play()
                }
            } catch (e: Exception) {
                Log.e("VoicePlayback", "Error playing voice note: ${e.javaClass.simpleName}: ${e.message}")
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _currentPlayingId.value = null
                    val mapped = com.example.util.ErrorMapper.map(e)
                    onError(
                        if (mapped == com.example.util.ErrorMapper.MSG_GENERIC) {
                            "Could not play audio. Please try again."
                        } else {
                            mapped
                        }
                    )
                }
            }
        }
    }

    fun seekTo(fraction: Float) {
        val player = exoPlayer ?: return
        val total = player.duration.takeIf { it > 0 } ?: _durationMs.value
        if (total > 0) {
            val targetMs = (total * fraction.coerceIn(0f, 1f)).toLong()
            player.seekTo(targetMs)
            _positionMs.value = targetMs
        }
    }

    fun cycleSpeed() {
        val newSpeed = when (_speed.value) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        _speed.value = newSpeed
        exoPlayer?.setPlaybackSpeed(newSpeed)
    }

    private fun startProgressPolling() {
        stopProgressPolling()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let {
                    if (it.isPlaying) {
                        _positionMs.value = it.currentPosition
                        if (it.duration > 0) {
                            _durationMs.value = it.duration
                        }
                    }
                }
                delay(50)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressPolling()
        exoPlayer?.release()
        exoPlayer = null
    }

    internal suspend fun resolveAudioSourceToLocalFile(context: Context, rawSource: String): File? = withContext(Dispatchers.IO) {
        val source = rawSource.trim()
        if (source.isBlank() || source == "downloading...") return@withContext null
        try {
            if (source.startsWith("http://") || source.startsWith("https://")) {
                val cacheDir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
                val ext = when {
                    source.contains(".m4a", ignoreCase = true) -> ".m4a"
                    source.contains(".aac", ignoreCase = true) -> ".aac"
                    source.contains(".ogg", ignoreCase = true) -> ".ogg"
                    source.contains(".mp3", ignoreCase = true) -> ".mp3"
                    else -> ".m4a"
                }
                val cachedFile = File(cacheDir, "voice_${source.hashCode()}$ext")
                if (cachedFile.exists() && cachedFile.length() > 0) {
                    return@withContext cachedFile
                }
                val url = java.net.URL(source)
                val conn = url.openConnection()
                val appwriteProject = com.example.repository.EncryptedChatRepository.extractAppwriteProjectId(source)
                if (!appwriteProject.isNullOrBlank()) {
                    conn.setRequestProperty("X-Appwrite-Project", appwriteProject)
                }
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.getInputStream().use { input ->
                    FileOutputStream(cachedFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
                return@withContext cachedFile.takeIf { it.exists() && it.length() > 0L }
            }

            if (source.startsWith("file://")) {
                val parsedPath = runCatching { Uri.parse(source).path }.getOrNull()
                    ?: source.removePrefix("file://")
                val file = File(parsedPath)
                if (file.exists() && file.length() > 0L) {
                    return@withContext file
                }
            }

            if (source.startsWith("content://")) {
                val cacheDir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
                val copied = File(cacheDir, "voice_content_${source.hashCode()}.m4a")
                if (copied.exists() && copied.length() > 0L) return@withContext copied
                context.contentResolver.openInputStream(Uri.parse(source))?.use { input ->
                    FileOutputStream(copied).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
                if (copied.exists() && copied.length() > 0L) return@withContext copied
            }

            val directFile = File(source)
            if (directFile.exists() && directFile.length() > 0L) {
                return@withContext directFile
            }

            val appCacheFile = File(context.cacheDir, source)
            if (appCacheFile.exists() && appCacheFile.length() > 0L) {
                return@withContext appCacheFile
            }
            null
        } catch (e: Exception) {
            Log.e("VoicePlayback", "Failed to resolve audio source: ${e.javaClass.simpleName}")
            null
        }
    }
}

@Composable
fun rememberVoicePlaybackController(): VoicePlaybackController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { VoicePlaybackController(context, scope) }
    DisposableEffect(Unit) {
        onDispose {
            controller.release()
        }
    }
    return controller
}

// ===========================================================================
// 2. Live Voice Recorder (Microphone + Amplitude Streaming)
// ===========================================================================

data class VoiceRecordResult(
    val file: File,
    val durationMs: Long,
    val amplitudes: List<Int>,
    val bytes: ByteArray
)

class LiveVoiceRecorder(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaRecorder: MediaRecorder? = null
    private var recordFile: File? = null
    private var startTimeMs: Long = 0L
    private var amplitudeJob: Job? = null

    var isRecording by mutableStateOf(false)
        private set

    var elapsedMs by mutableStateOf(0L)
        private set

    val liveAmplitudes = mutableStateListOf<Int>()

    fun start() {
        if (isRecording) return
        try {
            val voiceDir = File(context.cacheDir, "voice_recordings").apply { mkdirs() }
            val file = File(voiceDir, "rec_${System.currentTimeMillis()}.m4a")
            recordFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            startTimeMs = System.currentTimeMillis()
            isRecording = true
            elapsedMs = 0L
            liveAmplitudes.clear()

            amplitudeJob = scope.launch {
                while (isActive && isRecording) {
                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    val normalized = (maxAmp.toFloat() / 32767f * 255f).toInt().coerceIn(10, 255)
                    liveAmplitudes.add(normalized)
                    if (liveAmplitudes.size > 50) {
                        liveAmplitudes.removeAt(0)
                    }
                    elapsedMs = System.currentTimeMillis() - startTimeMs
                    delay(80)
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Failed to start recording", e)
            isRecording = false
            Toast.makeText(context, "Microphone is unavailable. Check permission and try again.", Toast.LENGTH_SHORT).show()
        }
    }

    fun stop(): VoiceRecordResult? {
        if (!isRecording) return null
        amplitudeJob?.cancel()
        amplitudeJob = null
        isRecording = false

        val duration = System.currentTimeMillis() - startTimeMs
        val file = recordFile

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w("VoiceRecorder", "Stop failed", e)
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Error releasing recorder", e)
        } finally {
            mediaRecorder = null
        }

        if (file != null && file.exists() && file.length() > 0 && duration >= 500L) {
            val bytes = file.readBytes()
            return VoiceRecordResult(
                file = file,
                durationMs = duration,
                amplitudes = liveAmplitudes.toList(),
                bytes = bytes
            )
        }
        runCatching { file?.delete() }
        Toast.makeText(context, "Hold the mic and speak", Toast.LENGTH_SHORT).show()
        return null
    }

    fun cancel() {
        if (!isRecording) return
        amplitudeJob?.cancel()
        amplitudeJob = null
        isRecording = false

        try {
            mediaRecorder?.apply {
                try { stop() } catch (e: Exception) {}
                release()
            }
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Error cancelling recorder", e)
        } finally {
            mediaRecorder = null
        }

        recordFile?.delete()
        recordFile = null
        liveAmplitudes.clear()
        elapsedMs = 0L
    }
}

@Composable
fun rememberLiveVoiceRecorder(): LiveVoiceRecorder {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember { LiveVoiceRecorder(context, scope) }
    DisposableEffect(Unit) {
        onDispose {
            if (recorder.isRecording) {
                recorder.cancel()
            }
        }
    }
    return recorder
}

// ===========================================================================
// 3. Fullscreen Media Viewer Dialog with Telephoto Zoomable Image
// ===========================================================================

data class ChatViewerMediaItem(
    val id: String,
    val url: String,
    val senderName: String,
    val dateText: String,
    val caption: String? = null,
    val isVideo: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelephotoMediaViewerDialog(
    mediaItems: List<ChatViewerMediaItem>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onShare: ((ChatViewerMediaItem) -> Unit)? = null,
    onSave: ((ChatViewerMediaItem) -> Unit)? = null,
    onForward: ((ChatViewerMediaItem) -> Unit)? = null,
    onDelete: ((ChatViewerMediaItem) -> Unit)? = null
) {
    if (mediaItems.isEmpty()) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, mediaItems.size - 1),
        pageCount = { mediaItems.size }
    )

    var showChrome by remember { mutableStateOf(true) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    fun triggerSave(item: ChatViewerMediaItem) {
        if (onSave != null) {
            onSave(item)
        } else if (!isSaving) {
            isSaving = true
            scope.launch {
                val res = if (item.isVideo) {
                    ChatMediaSaver.saveVideoToGallery(context, item.url)
                } else {
                    ChatMediaSaver.saveImageToGallery(context, item.url)
                }
                isSaving = false
                Toast.makeText(
                    context,
                    res.fold(
                        onSuccess = { "Saved" },
                        onFailure = { it.message ?: "Could not save media" }
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun triggerShare(item: ChatViewerMediaItem) {
        if (onShare != null) {
            onShare(item)
        } else {
            scope.launch {
                if (item.isVideo) {
                    ChatMediaSaver.shareMediaFile(context, item.url, "video/mp4", "Share video")
                } else {
                    ChatMediaSaver.shareImage(context, item.url)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        val bgAlpha = (1f - (abs(dragOffsetY) / 600f)).coerceIn(0.2f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = bgAlpha))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (abs(dragOffsetY) > 160f) {
                                onDismiss()
                            } else {
                                dragOffsetY = 0f
                            }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            dragOffsetY += dragAmount
                        }
                    )
                }
                .graphicsLayer {
                    translationY = dragOffsetY
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = mediaItems[page]
                val isCurrentPage = pagerState.currentPage == page
                if (item.isVideo) {
                    FullscreenVideoViewerPage(
                        videoSource = item.url,
                        isActivePage = isCurrentPage,
                        onToggleChrome = { showChrome = !showChrome }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                showChrome = !showChrome
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ZoomableAsyncImage(
                            model = item.url,
                            contentDescription = "Media item",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Top Header Chrome Bar
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(tween(150)) + slideInVertically(tween(150)),
                exit = fadeOut(tween(150)) + slideOutVertically(tween(150)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                val currentItem = mediaItems.getOrNull(pagerState.currentPage) ?: mediaItems[0]
                Surface(
                    color = Color.Black.copy(alpha = 0.70f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp)
                        ) {
                            ChatIconBack(tint = Color.White, modifier = Modifier.size(24.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentItem.senderName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentItem.dateText} · ${pagerState.currentPage + 1} of ${mediaItems.size}",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = { triggerSave(currentItem) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("media_viewer_save_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                ChatIconDownload(tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }

                        IconButton(
                            onClick = { triggerShare(currentItem) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("media_viewer_share_button")
                        ) {
                            ChatIconShare(tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                ChatIconMoreVert(tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                shape = RoundedCornerShape(18.dp),
                                containerColor = Color(0xFF1E2128),
                                shadowElevation = 12.dp,
                                modifier = Modifier
                                    .widthIn(min = 200.dp)
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Save to Gallery",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.White.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ChatIconDownload(tint = Color.White, modifier = Modifier.size(17.dp))
                                        }
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        triggerSave(currentItem)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Share",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.White.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ChatIconShare(tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        triggerShare(currentItem)
                                    }
                                )
                                if (onForward != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Forward",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color.White.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                ChatIconForward(tint = Color.White, modifier = Modifier.size(17.dp))
                                            }
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            onForward(currentItem)
                                        }
                                    )
                                }
                                if (onDelete != null) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        thickness = 0.6.dp,
                                        color = Color.White.copy(alpha = 0.12f)
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Delete",
                                                color = Color(0xFFFF7066),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFFF7066).copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                ChatIconTrash(tint = Color(0xFFFF7066), modifier = Modifier.size(17.dp))
                                            }
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            onDelete(currentItem)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Caption Chrome Bar
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { it },
                exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val currentItem = mediaItems.getOrNull(pagerState.currentPage) ?: mediaItems[0]
                if (!currentItem.caption.isNullOrBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentItem.caption,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenVideoViewerPage(
    videoSource: String,
    isActivePage: Boolean,
    onToggleChrome: () -> Unit
) {
    val context = LocalContext.current
    val player = remember(videoSource) {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (videoSource.startsWith("/")) {
                Uri.fromFile(File(videoSource))
            } else {
                Uri.parse(videoSource)
            }
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    LaunchedEffect(isActivePage) {
        if (isActivePage) {
            player.playWhenReady = true
        } else {
            player.pause()
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onToggleChrome() },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            update = { view ->
                if (view.player !== player) {
                    view.player = player
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ===========================================================================
// 4. AI Writing Assist Bottom Sheet
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWritingAssistBottomSheet(
    initialText: String,
    onReplaceText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = LocalChatColors.current

    var selectedStyle by remember { mutableStateOf("fix") }
    var resultText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun runImprove(style: String) {
        selectedStyle = style
        if (initialText.isBlank()) return
        isLoading = true
        errorText = null
        scope.launch {
            val res = CloudAiManager.improveDraftText(context, initialText, style)
            res.onSuccess {
                resultText = it.text
                isLoading = false
            }.onFailure {
                errorText = it.message ?: "Failed to improve text"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        runImprove("fix")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        contentColor = c.text,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChatIconSparkles(tint = c.accentIn, modifier = Modifier.size(24.dp))
                Text("AI Writing Assist", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.text)
            }

            // Style Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "fix" to "Fix grammar",
                    "friendly" to "Friendlier",
                    "formal" to "More formal",
                    "short" to "Shorter"
                ).forEach { (styleKey, label) ->
                    val isSelected = selectedStyle == styleKey
                    Surface(
                        onClick = { runImprove(styleKey) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) c.brand else c.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) c.onBrand else c.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Result Preview Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = c.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    when {
                        isLoading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = c.accentIn, strokeWidth = 2.dp)
                                Text("Refining text...", fontSize = 14.sp, color = c.textSecondary)
                            }
                        }
                        errorText != null -> {
                            Text(errorText!!, fontSize = 14.sp, color = c.danger)
                        }
                        resultText != null -> {
                            Text(resultText!!, fontSize = 15.sp, color = c.text, lineHeight = 21.sp)
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("Cancel", color = c.text)
                }
                Button(
                    onClick = {
                        val textToUse = resultText ?: initialText
                        onReplaceText(textToUse)
                        onDismiss()
                    },
                    enabled = resultText != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.brand, contentColor = c.onBrand),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("Replace", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ===========================================================================
// 5. Long-Press Popup Overlay with ReactionQuickBar + MessageActionsMenu
// ===========================================================================

@Composable
fun MessageLongPressOverlay(
    myReaction: String?,
    actions: List<MessageAction>,
    onPickEmoji: (String) -> Unit,
    onMoreEmoji: () -> Unit,
    onAction: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.50f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clickable(enabled = false) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick emoji reaction bar
                ReactionQuickBar(
                    emojis = listOf("👍", "❤️", "😂", "😮", "😢", "🙏"),
                    mine = myReaction,
                    onPick = {
                        onPickEmoji(it)
                        onDismiss()
                    },
                    onMore = {
                        onDismiss()
                        onMoreEmoji()
                    }
                )

                // Message Actions Card
                var scaleTarget by remember { mutableFloatStateOf(0.92f) }
                LaunchedEffect(Unit) {
                    scaleTarget = 1.0f
                }
                val scale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = scaleTarget,
                    animationSpec = tween(120),
                    label = "menuScale"
                )

                Box(modifier = Modifier.scale(scale)) {
                    MessageActionsMenu(
                        actions = actions,
                        onAction = {
                            onDismiss()
                            onAction(it)
                        }
                    )
                }
            }
        }
    }
}

// ===========================================================================
// 6. Timeline Grouping & Helpers
// ===========================================================================

fun computeGroupPosition(
    isSameSenderAsOlder: Boolean,
    isSameSenderAsNewer: Boolean
): GroupPosition = when {
    !isSameSenderAsOlder && !isSameSenderAsNewer -> GroupPosition.Single
    !isSameSenderAsOlder && isSameSenderAsNewer -> GroupPosition.First
    isSameSenderAsOlder && isSameSenderAsNewer -> GroupPosition.Middle
    else -> GroupPosition.Last
}

fun formatChatDateChip(timestamp: Long): String {
    val cal = Calendar.getInstance()
    val todayDay = cal.get(Calendar.DAY_OF_YEAR)
    val todayYear = cal.get(Calendar.YEAR)

    cal.timeInMillis = timestamp
    val msgDay = cal.get(Calendar.DAY_OF_YEAR)
    val msgYear = cal.get(Calendar.YEAR)

    return when {
        todayYear == msgYear && todayDay == msgDay -> "Today"
        todayYear == msgYear && todayDay - msgDay == 1 -> "Yesterday"
        todayYear == msgYear -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

fun formatChatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
