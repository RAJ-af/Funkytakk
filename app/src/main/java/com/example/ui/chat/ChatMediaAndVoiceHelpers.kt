package com.example.ui.chat

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.CloudAiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ===========================================================================
// 1. Media3 ExoPlayer Voice Note Controller
// ===========================================================================

@androidx.annotation.OptIn(UnstableApi::class)
@Stable
class VoicePlaybackController(private val context: Context, private val scope: CoroutineScope) {
    private val TAG = "VoicePlaybackController"

    private val _currentPlayingId = mutableStateOf<String?>(null)
    var currentPlayingId: String?
        get() = _currentPlayingId.value
        set(value) { _currentPlayingId.value = value }

    private val _isPlaying = mutableStateOf(false)
    var isPlaying: Boolean
        get() = _isPlaying.value
        set(value) { _isPlaying.value = value }

    private val _isLoading = mutableStateOf(false)
    var isLoading: Boolean
        get() = _isLoading.value
        set(value) { _isLoading.value = value }

    private val _positionMs = mutableStateOf(0L)
    var positionMs: Long
        get() = _positionMs.value
        set(value) { _positionMs.value = value }

    private val _durationMs = mutableStateOf(0L)
    var durationMs: Long
        get() = _durationMs.value
        set(value) { _durationMs.value = value }

    private val _speed = mutableStateOf(1.0f)
    var speed: Float
        get() = _speed.value
        set(value) { _speed.value = value }

    private var exoPlayer: ExoPlayer? = null
    private var progressJob: kotlinx.coroutines.Job? = null

    init {
        initPlayer()
    }

    private fun initPlayer() {
        try {
            exoPlayer?.release()
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            this@VoicePlaybackController._isPlaying.value = false
                            this@VoicePlaybackController._positionMs.value = 0L
                            stopProgressPolling()
                        } else if (playbackState == Player.STATE_READY) {
                            this@VoicePlaybackController._isLoading.value = false
                            this@VoicePlaybackController._durationMs.value = duration.coerceAtLeast(0L)
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        this@VoicePlaybackController._isPlaying.value = playing
                        if (playing) startProgressPolling() else stopProgressPolling()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "ExoPlayer playback error: ${error.message}", error)
                        this@VoicePlaybackController._isPlaying.value = false
                        this@VoicePlaybackController._isLoading.value = false
                        stopProgressPolling()
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ExoPlayer", e)
        }
    }

    fun playOrPause(
        messageId: String,
        audioSource: Any, // File, Uri, String path, or ByteArray
        fallbackDurationMs: Long = 0L,
        onError: (String) -> Unit = {}
    ) {
        if (currentPlayingId == messageId) {
            val player = exoPlayer ?: return
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
            return
        }

        stop()
        currentPlayingId = messageId
        isLoading = true
        positionMs = 0L
        durationMs = fallbackDurationMs

        scope.launch(Dispatchers.IO) {
            try {
                val preparedFile = prepareAudioFile(messageId, audioSource)
                if (preparedFile == null || !preparedFile.exists() || preparedFile.length() == 0L) {
                    throw IllegalStateException("Audio file unavailable or empty")
                }

                withContext(Dispatchers.Main) {
                    val player = exoPlayer ?: ExoPlayer.Builder(context).build().also { exoPlayer = it }
                    val mediaItem = MediaItem.fromUri(Uri.fromFile(preparedFile))
                    player.setMediaItem(mediaItem)
                    player.setPlaybackSpeed(speed)
                    player.prepare()
                    player.play()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Voice note playback failed for message $messageId", e)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    isPlaying = false
                    currentPlayingId = null
                    onError("Voice playback failed: ${e.message ?: "File unreadable"}")
                }
            }
        }
    }

    private fun prepareAudioFile(messageId: String, source: Any): File? {
        return try {
            when (source) {
                is File -> {
                    if (source.exists() && source.length() > 0) source else null
                }
                is String -> {
                    val clean = source.removePrefix("file://").trim()
                    val f = File(clean)
                    if (f.exists() && f.length() > 0) f else null
                }
                is ByteArray -> {
                    val cacheFile = File(context.cacheDir, "voice_play_${messageId}.m4a")
                    FileOutputStream(cacheFile).use { fos ->
                        fos.write(source)
                        fos.flush()
                        try { fos.fd.sync() } catch (_: Exception) {}
                    }
                    cacheFile
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing voice playback cache file", e)
            null
        }
    }

    fun seekTo(progress: Float) {
        val player = exoPlayer ?: return
        val target = if (durationMs > 0) (progress * durationMs).toLong() else (progress * player.duration).toLong()
        player.seekTo(target.coerceAtLeast(0L))
        positionMs = target
    }

    fun cycleSpeed() {
        speed = when (speed) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        exoPlayer?.setPlaybackSpeed(speed)
    }

    fun stop() {
        stopProgressPolling()
        try {
            exoPlayer?.stop()
        } catch (_: Exception) {}
        isPlaying = false
        isLoading = false
        currentPlayingId = null
        positionMs = 0L
    }

    fun release() {
        stop()
        try {
            exoPlayer?.release()
        } catch (_: Exception) {}
        exoPlayer = null
    }

    private fun startProgressPolling() {
        stopProgressPolling()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val player = exoPlayer
                if (player != null && player.isPlaying) {
                    positionMs = player.currentPosition.coerceAtLeast(0L)
                    val d = player.duration
                    if (d > 0) durationMs = d
                }
                delay(50L)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }
}

@Composable
fun rememberVoicePlaybackController(): VoicePlaybackController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { VoicePlaybackController(context.applicationContext, scope) }
    DisposableEffect(Unit) {
        onDispose {
            controller.release()
        }
    }
    return controller
}

// ===========================================================================
// 2. Live Voice Recorder with Amplitude Sampling
// ===========================================================================

data class LiveVoiceResult(
    val file: File,
    val durationMs: Long,
    val amplitudes: List<Int>,
    val bytes: ByteArray
)

class LiveVoiceRecorder(private val context: Context, private val scope: CoroutineScope) {
    private val TAG = "LiveVoiceRecorder"

    var isRecording by mutableStateOf(false)
    var elapsedMs by mutableStateOf(0L)
    var liveAmplitudes = mutableStateListOf<Int>()

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs = 0L
    private var samplingJob: kotlinx.coroutines.Job? = null

    fun start(): Boolean {
        cancel()
        return try {
            val file = File(context.cacheDir, "voice_rec_${System.currentTimeMillis()}.m4a")
            outputFile = file

            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setAudioEncodingBitRate(64000)
            rec.setAudioSamplingRate(44100)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()

            recorder = rec
            startTimeMs = System.currentTimeMillis()
            elapsedMs = 0L
            liveAmplitudes.clear()
            isRecording = true

            // Sample max amplitude every ~80ms
            samplingJob = scope.launch(Dispatchers.Default) {
                while (isActive && isRecording) {
                    delay(80L)
                    val rawAmp = try { rec.maxAmplitude } catch (_: Exception) { 0 }
                    // Normalize 0..32767 to 0..255
                    val normalized = ((rawAmp / 32767f) * 255f).toInt().coerceIn(10, 255)
                    withContext(Dispatchers.Main) {
                        elapsedMs = System.currentTimeMillis() - startTimeMs
                        if (liveAmplitudes.size >= 48) liveAmplitudes.removeAt(0)
                        liveAmplitudes.add(normalized)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start live voice recorder", e)
            cancel()
            false
        }
    }

    fun stop(): LiveVoiceResult? {
        samplingJob?.cancel()
        samplingJob = null
        val rec = recorder ?: return null
        val file = outputFile ?: return null
        val totalDuration = System.currentTimeMillis() - startTimeMs

        return try {
            rec.stop()
            rec.release()
            recorder = null
            outputFile = null
            isRecording = false

            if (file.exists() && file.length() > 200L && totalDuration >= 350L) {
                val bytes = file.readBytes()
                val finalAmps = if (liveAmplitudes.isEmpty()) List(48) { 40 } else liveAmplitudes.toList()
                LiveVoiceResult(file, totalDuration, finalAmps, bytes)
            } else {
                file.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping voice recorder", e)
            try { rec.release() } catch (_: Exception) {}
            recorder = null
            file.delete()
            outputFile = null
            isRecording = false
            null
        }
    }

    fun cancel() {
        samplingJob?.cancel()
        samplingJob = null
        try {
            recorder?.stop()
        } catch (_: Exception) {}
        try {
            recorder?.release()
        } catch (_: Exception) {}
        recorder = null
        outputFile?.delete()
        outputFile = null
        isRecording = false
        elapsedMs = 0L
        liveAmplitudes.clear()
    }
}

// ===========================================================================
// 3. Fullscreen Media Viewer with Telephoto Zoomable Image
// ===========================================================================

data class ChatViewerMediaItem(
    val id: String,
    val url: String,
    val senderName: String = "User",
    val dateText: String = "Today",
    val caption: String? = null,
    val isVideo: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelephotoMediaViewerDialog(
    mediaList: List<ChatViewerMediaItem>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onSave: (ChatViewerMediaItem) -> Unit = {},
    onShare: (ChatViewerMediaItem) -> Unit = {},
    onForward: (ChatViewerMediaItem) -> Unit = {},
    onDelete: (ChatViewerMediaItem) -> Unit = {}
) {
    if (mediaList.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, mediaList.size - 1)) { mediaList.size }
    var showChrome by remember { mutableStateOf(true) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BackHandler(onBack = onDismiss)

        val bgAlpha = (1f - (abs(dragOffsetY) / 600f)).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = bgAlpha))
                .pointerInput(Unit) {
                    detectTapGestures {
                        showChrome = !showChrome
                    }
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = mediaList[page]
                val zoomableState = rememberZoomableState()
                val zoomableImageState = rememberZoomableImageState(zoomableState)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = dragOffsetY
                        }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { _, dragAmount ->
                                    val currentZoom = zoomableState.zoomFraction ?: 0f
                                    if (currentZoom <= 0.05f) {
                                        dragOffsetY += dragAmount
                                    }
                                },
                                onDragEnd = {
                                    if (abs(dragOffsetY) > 160f) {
                                        onDismiss()
                                    } else {
                                        dragOffsetY = 0f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    ZoomableAsyncImage(
                        model = item.url,
                        contentDescription = "Zoomable photo",
                        state = zoomableImageState,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Top Chrome Bar
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(tween(150)) + slideInVertically(tween(150)),
                exit = fadeOut(tween(150)) + slideOutVertically(tween(150)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                val currentItem = mediaList.getOrNull(pagerState.currentPage) ?: mediaList[0]
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close", tint = Color.White)
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
                                text = "${currentItem.dateText} · ${pagerState.currentPage + 1} of ${mediaList.size}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        IconButton(onClick = { onShare(currentItem) }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.White)
                        }

                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                containerColor = Color(0xFF1E2128)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Save to gallery", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White) },
                                    onClick = {
                                        showMoreMenu = false
                                        onSave(currentItem)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Forward", color = Color.White) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Forward, contentDescription = null, tint = Color.White) },
                                    onClick = {
                                        showMoreMenu = false
                                        onForward(currentItem)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = Color(0xFFFF7066)) },
                                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFFF7066)) },
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

            // Bottom Caption Chrome Bar
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(tween(150)) + slideInVertically(tween(150), initialOffsetY = { it }),
                exit = fadeOut(tween(150)) + slideOutVertically(tween(150), targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val currentItem = mediaList.getOrNull(pagerState.currentPage)
                if (!currentItem?.caption.isNullOrBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = currentItem!!.caption!!,
                                color = Color.White,
                                fontSize = 14.5.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===========================================================================
// 4. AI Writing Assist Sheet for Composer (✨ Button)
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWritingAssistBottomSheet(
    initialText: String,
    onReplaceText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val c = LocalChatColors.current
    val scope = rememberCoroutineScope()

    var selectedStyle by remember { mutableStateOf("fix") }
    var resultText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun runImprove(style: String) {
        selectedStyle = style
        isLoading = true
        errorMessage = null
        resultText = null
        scope.launch {
            val res = CloudAiManager.improveDraftText(context, initialText, style)
            isLoading = false
            res.onSuccess { ai ->
                resultText = ai.text
            }.onFailure { err ->
                errorMessage = err.message ?: "Failed to improve text"
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
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = c.accentIn, modifier = Modifier.size(22.dp))
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

            // Result Container
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = c.surfaceVariant,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 90.dp)
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    when {
                        isLoading -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = c.accentIn, strokeWidth = 2.dp)
                                Text("Polishing with AI...", fontSize = 14.sp, color = c.textSecondary)
                            }
                        }
                        errorMessage != null -> {
                            Text(errorMessage!!, color = c.danger, fontSize = 14.sp)
                        }
                        resultText != null -> {
                            Text(resultText!!, color = c.text, fontSize = 15.sp, lineHeight = 21.sp)
                        }
                        else -> {
                            Text(initialText, color = c.textSecondary, fontSize = 14.sp)
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

                // Message Actions Card (Scale animation from 0.9 to 1.0)
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
