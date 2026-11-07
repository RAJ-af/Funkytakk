package com.example.ui.screens.voiceroom

import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayDeque
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Registry to track real-time screen coordinates of seat avatars via onGloballyPositioned.
 */
object SeatPositionRegistry {
    private val seatPositions = java.util.concurrent.ConcurrentHashMap<String, IntOffset>()

    fun updatePosition(userId: String, coordinates: LayoutCoordinates) {
        if (userId.isBlank()) return
        if (coordinates.isAttached) {
            val rootPos = coordinates.positionInRoot()
            val size = coordinates.size
            val centerX = (rootPos.x + size.width / 2f).toInt()
            val centerY = (rootPos.y + size.height / 2f).toInt()
            seatPositions[userId] = IntOffset(centerX, centerY)
        }
    }

    fun getPosition(userId: String?): IntOffset? {
        if (userId.isNullOrBlank()) return null
        return seatPositions[userId]
    }

    fun clear() {
        seatPositions.clear()
    }
}

/**
 * Data payload for triggering a gift animation overlay.
 */
data class GiftAnimationData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val iconUrl: String,
    val animationTier: Int = 1, // 1 = cheap (~1.5s), 2 = medium (~2.5s), 3 = expensive (~4.0s)
    val animationUrl: String? = null,
    val quantity: Int = 1,
    val senderName: String,
    val recipientName: String,
    val recipientId: String? = null,
    val giftName: String? = null
)

/**
 * Thread-safe singleton Queue Manager for gift animations.
 * Controls maximum queue capacity (5 items), drops Tier 1 items when full,
 * handles foreground/background lifecycle cleanup, and checks system animation settings.
 */
object GiftAnimationManager {
    private const val MAX_QUEUE_SIZE = 5

    private val queue = ArrayDeque<GiftAnimationData>()
    private val _currentAnimation = MutableStateFlow<GiftAnimationData?>(null)
    val currentAnimation = _currentAnimation.asStateFlow()

    @Volatile
    private var isProcessing = false

    /**
     * Enqueues a new gift animation.
     * Drops Tier 1 animations if queue length >= 5.
     */
    fun enqueueAnimation(data: GiftAnimationData, context: Context) {
        if (areAnimationsDisabled(context)) {
            return
        }

        synchronized(queue) {
            if (queue.size >= MAX_QUEUE_SIZE) {
                // If queue is full, drop Tier 1 items to make room
                val iterator = queue.iterator()
                while (iterator.hasNext()) {
                    val item = iterator.next()
                    if (item.animationTier <= 1) {
                        iterator.remove()
                        break
                    }
                }
            }

            if (queue.size < MAX_QUEUE_SIZE) {
                queue.addLast(data)
            }
        }

        processQueue()
    }

    private fun processQueue() {
        if (isProcessing) return
        isProcessing = true

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            while (true) {
                val nextItem: GiftAnimationData? = synchronized(queue) {
                    if (queue.isNotEmpty()) queue.removeFirst() else null
                }

                if (nextItem == null) {
                    _currentAnimation.value = null
                    isProcessing = false
                    break
                }

                _currentAnimation.value = nextItem

                val durationMs = when (nextItem.animationTier) {
                    3 -> 4000L
                    2 -> 2500L
                    else -> 1500L
                }

                delay(durationMs)
            }
        }
    }

    /**
     * Clears pending queue and stops active animation when app enters background (Lifecycle ON_STOP/ON_PAUSE).
     */
    fun clearQueue() {
        synchronized(queue) {
            queue.clear()
        }
        _currentAnimation.value = null
        isProcessing = false
    }

    /**
     * Checks if user has disabled animations in Android system developer settings.
     */
    fun areAnimationsDisabled(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Non-blocking Overlay Composable that renders Tier 1, Tier 2, and Tier 3 gift animations over the Room Screen.
 * Pointer input passes directly through to underlying seat grid and controls.
 */
@Composable
fun GiftAnimationOverlay(
    modifier: Modifier = Modifier,
    hostUserId: String? = null,
    speakerUserIds: List<String> = emptyList()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeAnimation by GiftAnimationManager.currentAnimation.collectAsState()

    // Lifecycle observer to clear animations on app background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                GiftAnimationManager.clearQueue()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val item = activeAnimation ?: return

    // Non-blocking full-screen overlay Box (no clickable / touch interceptors)
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Ensure layer optimization without intercepting input
            },
        contentAlignment = Alignment.Center
    ) {
        when (item.animationTier) {
            3 -> Tier3GrandAnimation(data = item)
            2 -> Tier2MediumAnimation(data = item)
            else -> Tier1FloatingAnimation(
                data = item,
                hostUserId = hostUserId,
                speakerUserIds = speakerUserIds
            )
        }
    }
}

/**
 * TIER 1: Floating Gift Animation (~1.5s)
 * Icon emerges near recipient's seat position, floats upwards, and fades out gracefully.
 */
@Composable
private fun Tier1FloatingAnimation(
    data: GiftAnimationData,
    hostUserId: String?,
    speakerUserIds: List<String>
) {
    val transition = rememberInfiniteTransition(label = "tier1_float")
    val animScale = remember { Animatable(0.3f) }
    val animAlpha = remember { Animatable(1.0f) }
    val animOffsetY = remember { Animatable(0f) }

    LaunchedEffect(data.id) {
        animScale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    LaunchedEffect(data.id) {
        animOffsetY.animateTo(
            targetValue = -50f,
            animationSpec = tween(durationMillis = 1400, easing = LinearOutSlowInEasing)
        )
    }

    LaunchedEffect(data.id) {
        delay(900)
        animAlpha.animateTo(
            targetValue = 0.0f,
            animationSpec = tween(durationMillis = 500, easing = LinearEasing)
        )
    }

    // Determine target seat position offset
    val (targetAlignment, offsetYDp) = remember(data.recipientId, hostUserId, speakerUserIds) {
        when {
            data.recipientId != null && data.recipientId == hostUserId -> Alignment.TopCenter to 140.dp
            data.recipientId != null && speakerUserIds.contains(data.recipientId) -> {
                val idx = speakerUserIds.indexOf(data.recipientId)
                when (idx) {
                    0 -> Alignment.TopStart to 240.dp
                    1 -> Alignment.TopEnd to 240.dp
                    2 -> Alignment.TopStart to 340.dp
                    else -> Alignment.TopEnd to 340.dp
                }
            }
            else -> Alignment.Center to (-40).dp
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = targetAlignment
    ) {
        Box(
            modifier = Modifier
                .offset(y = offsetYDp)
                .graphicsLayer {
                    scaleX = animScale.value
                    scaleY = animScale.value
                    translationY = animOffsetY.value
                    alpha = animAlpha.value
                },
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7))
                    .border(2.dp, Color(0xFFFFC527), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (data.iconUrl.isNotBlank()) {
                    AsyncImage(
                        model = data.iconUrl,
                        contentDescription = data.giftName ?: "Gift",
                        modifier = Modifier.size(46.dp)
                    )
                } else {
                    Text(text = "🎁", fontSize = 32.sp)
                }
            }

            // Quantity Badge ("x5")
            if (data.quantity > 1) {
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = (-4).dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFFFFC527))
                        .border(1.dp, Color.White, RoundedCornerShape(100.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "x${data.quantity}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E1B18)
                    )
                }
            }
        }
    }
}

/**
 * TIER 2: Medium Gift Animation (~2.5s)
 * Icon pops up in center with overshoot bounce, 20 glowing particles, and "Raj → Lime" name banner.
 */
@Composable
private fun Tier2MediumAnimation(data: GiftAnimationData) {
    val animScale = remember { Animatable(0.0f) }
    val animAlpha = remember { Animatable(1.0f) }

    LaunchedEffect(data.id) {
        animScale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    LaunchedEffect(data.id) {
        delay(1900)
        animAlpha.animateTo(
            targetValue = 0.0f,
            animationSpec = tween(durationMillis = 500, easing = LinearEasing)
        )
    }

    // Capped 20 glowing star particles for low-end phone performance
    val particles = remember(data.id) {
        List(20) {
            val angle = Random.nextDouble(0.0, Math.PI * 2)
            val distance = Random.nextFloat() * 120f + 50f
            val size = Random.nextFloat() * 6f + 4f
            ParticleData(angle, distance, size, Color(0xFFFFC527))
        }
    }

    val particleProgress = remember { Animatable(0f) }
    LaunchedEffect(data.id) {
        particleProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = animAlpha.value },
        contentAlignment = Alignment.Center
    ) {
        // Star Particles Canvas
        Canvas(modifier = Modifier.size(240.dp), onDraw = {
            val center = Offset(size.width / 2f, size.height / 2f)
            val prog = particleProgress.value
            particles.forEach { p ->
                val currentDist = p.distance * prog
                val x = center.x + (cos(p.angle) * currentDist).toFloat()
                val y = center.y + (sin(p.angle) * currentDist).toFloat()
                val pAlpha = (1f - prog).coerceIn(0f, 1f)
                drawCircle(
                    color = p.color.copy(alpha = pAlpha),
                    radius = p.size,
                    center = Offset(x, y)
                )
            }
        })

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = animScale.value
                scaleY = animScale.value
            }
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .shadow(12.dp, CircleShape, spotColor = Color(0xFFFFC527))
                        .clip(CircleShape)
                        .background(Color(0xFFFFFBEB))
                        .border(3.dp, Color(0xFFFFC527), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (data.iconUrl.isNotBlank()) {
                        AsyncImage(
                            model = data.iconUrl,
                            contentDescription = data.giftName ?: "Gift",
                            modifier = Modifier.size(58.dp)
                        )
                    } else {
                        Text(text = "🎁", fontSize = 42.sp)
                    }
                }

                if (data.quantity > 1) {
                    Box(
                        modifier = Modifier
                            .offset(x = 8.dp, y = (-4).dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFFFC527))
                            .border(1.5.dp, Color.White, RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "x${data.quantity}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E1B18)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name Banner Tag ("Raj → Lime")
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFFFEF3C7))
                    .border(1.5.dp, Color(0xFFFDE68A), RoundedCornerShape(100.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = data.senderName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F)
                )
                Text(
                    text = "➔",
                    fontSize = 12.sp,
                    color = Color(0xFFD97706)
                )
                Text(
                    text = data.recipientName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F)
                )
            }
        }
    }
}

/**
 * Disk cache helper for MP4 animations:
 * Downloads and caches the MP4 file to local disk (cacheDir/video_cache) so subsequent plays are instantaneous.
 */
object VideoDiskCacheHelper {
    private val memoryUrlToCachedPath = java.util.concurrent.ConcurrentHashMap<String, String>()

    suspend fun getCachedOrDownload(context: Context, videoUrl: String): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (videoUrl.isBlank()) return@withContext null

        memoryUrlToCachedPath[videoUrl]?.let { path ->
            val f = File(path)
            if (f.exists() && f.length() > 0) return@withContext path
        }

        val cacheDir = File(context.cacheDir, "video_cache").apply { if (!exists()) mkdirs() }
        val filename = "gift_vid_" + videoUrl.hashCode().toString() + ".mp4"
        val targetFile = File(cacheDir, filename)

        if (targetFile.exists() && targetFile.length() > 0) {
            memoryUrlToCachedPath[videoUrl] = targetFile.absolutePath
            return@withContext targetFile.absolutePath
        }

        try {
            val url = URL(videoUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 12000
                instanceFollowRedirects = true
            }
            if (connection.responseCode in 200..299) {
                val tempFile = File(cacheDir, "${filename}.tmp")
                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.length() > 0 && tempFile.renameTo(targetFile)) {
                    memoryUrlToCachedPath[videoUrl] = targetFile.absolutePath
                    return@withContext targetFile.absolutePath
                } else if (targetFile.exists() && targetFile.length() > 0) {
                    memoryUrlToCachedPath[videoUrl] = targetFile.absolutePath
                    return@withContext targetFile.absolutePath
                }
            }
        } catch (_: Exception) {}

        // Fallback: return remote URL if local caching failed
        return@withContext videoUrl
    }
}

/**
 * Full-screen silent MP4 video animation using Media3 (ExoPlayer).
 * Plays exactly once, scales to fit/fill entire screen, and signals onEnded or onError.
 */
@OptIn(UnstableApi::class)
@Composable
private fun FullscreenMp4Player(
    videoUrl: String,
    onEnded: () -> Unit,
    onError: () -> Unit
) {
    val context = LocalContext.current
    var cachedPath by remember(videoUrl) { mutableStateOf<String?>(null) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(videoUrl) {
        val path = VideoDiskCacheHelper.getCachedOrDownload(context, videoUrl)
        if (path != null) {
            cachedPath = path
        } else {
            hasError = true
            onError()
        }
    }

    if (hasError) return

    val currentPath = cachedPath ?: return

    val exoPlayer = remember(context, currentPath) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f // Silent playback as requested
            repeatMode = Player.REPEAT_MODE_OFF
            val mediaItem = if (currentPath.startsWith("/")) {
                MediaItem.fromUri(Uri.fromFile(File(currentPath)))
            } else {
                MediaItem.fromUri(Uri.parse(currentPath))
            }
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onEnded()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                hasError = true
                onError()
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * TIER 3: Grand Expensive Gift Animation (~4.0s)
 * Supports .mp4 via Media3 ExoPlayer, .json via Lottie, .webp/.gif via Coil with disk cache,
 * and gracefully falls back to the high-impact icon animation on any error or missing asset.
 */
@Composable
private fun Tier3GrandAnimation(data: GiftAnimationData) {
    var hasMediaError by remember(data.id) { mutableStateOf(false) }
    val animUrl = (data.animationUrl ?: "").trim()

    val isMp4 = remember(animUrl) {
        animUrl.endsWith(".mp4", ignoreCase = true) ||
                animUrl.contains(".mp4?", ignoreCase = true) ||
                animUrl.contains("/video/upload/", ignoreCase = true)
    }

    val isLottie = remember(animUrl) {
        animUrl.endsWith(".json", ignoreCase = true) ||
                animUrl.contains(".json?", ignoreCase = true)
    }

    val isAnimatedImage = remember(animUrl) {
        animUrl.endsWith(".webp", ignoreCase = true) ||
                animUrl.contains(".webp?", ignoreCase = true) ||
                animUrl.endsWith(".gif", ignoreCase = true) ||
                animUrl.contains(".gif?", ignoreCase = true)
    }

    val animAlpha = remember { Animatable(1.0f) }
    LaunchedEffect(data.id) {
        delay(3300)
        animAlpha.animateTo(
            targetValue = 0.0f,
            animationSpec = tween(durationMillis = 700, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = animAlpha.value },
        contentAlignment = Alignment.Center
    ) {
        if (!hasMediaError && isMp4 && animUrl.isNotBlank()) {
            // Full-screen silent MP4 video via Media3
            FullscreenMp4Player(
                videoUrl = animUrl,
                onEnded = {
                    // When video completes, it gracefully fades away with animAlpha
                },
                onError = {
                    hasMediaError = true
                }
            )
            // Top overlay banner showing sender/recipient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(2.dp, Color(0xFFFFC527), RoundedCornerShape(100.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "👑", fontSize = 18.sp)
                    Text(
                        text = "${data.senderName} sent ${data.giftName ?: "Gift"} to ${data.recipientName}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF78350F),
                        textAlign = TextAlign.Center
                    )
                    Text(text = "✨", fontSize = 18.sp)
                }
            }
        } else if (!hasMediaError && isLottie && animUrl.isNotBlank()) {
            // Lottie .json animation with disk caching enabled
            val composition by rememberLottieComposition(
                LottieCompositionSpec.Url(animUrl)
            )
            val lottieAnimState = animateLottieCompositionAsState(
                composition = composition,
                iterations = 1,
                speed = 1.0f
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f * animAlpha.value)),
                contentAlignment = Alignment.Center
            ) {
                if (composition != null) {
                    LottieAnimation(
                        composition = composition,
                        progress = { lottieAnimState.progress },
                        modifier = Modifier
                            .fillMaxSize(0.85f)
                    )
                }

                // Top overlay banner
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFFEF3C7))
                            .border(2.dp, Color(0xFFFFC527), RoundedCornerShape(100.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "👑", fontSize = 18.sp)
                        Text(
                            text = "${data.senderName} sent ${data.giftName ?: "Gift"} to ${data.recipientName}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF78350F),
                            textAlign = TextAlign.Center
                        )
                        Text(text = "✨", fontSize = 18.sp)
                    }
                }
            }
        } else if (!hasMediaError && isAnimatedImage && animUrl.isNotBlank()) {
            // Animated WebP / GIF via Coil with disk cache
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f * animAlpha.value)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(animUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .crossfade(true)
                        .listener(
                            onError = { _, _ -> hasMediaError = true }
                        )
                        .build(),
                    contentDescription = data.giftName ?: "Gift Animation",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(260.dp)
                )

                // Top overlay banner
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFFEF3C7))
                            .border(2.dp, Color(0xFFFFC527), RoundedCornerShape(100.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "👑", fontSize = 18.sp)
                        Text(
                            text = "${data.senderName} sent ${data.giftName ?: "Gift"} to ${data.recipientName}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF78350F),
                            textAlign = TextAlign.Center
                        )
                        Text(text = "✨", fontSize = 18.sp)
                    }
                }
            }
        } else {
            // Standard / Fallback Icon Animation with Confetti & Pulsing Crown/Badge
            Tier3IconFallbackAnimation(data = data, animAlpha = animAlpha)
        }
    }
}

/**
 * Fallback icon animation: Confetti + Center pulsating icon container + Top banner.
 */
@Composable
private fun Tier3IconFallbackAnimation(
    data: GiftAnimationData,
    animAlpha: Animatable<Float, AnimationVector1D>
) {
    val animScale = remember { Animatable(0.0f) }
    val pulseScale = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(data.id) {
        animScale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    // Capped 30 falling confetti particles for performance
    val confettiColors = listOf(
        Color(0xFFFFC527), Color(0xFFEC4899), Color(0xFF3B82F6),
        Color(0xFF10B981), Color(0xFFA855F7), Color(0xFFF97316)
    )
    val particles = remember(data.id) {
        List(30) {
            val startX = Random.nextFloat() * 320f - 160f
            val startY = Random.nextFloat() * -200f - 100f
            val speedY = Random.nextFloat() * 400f + 300f
            val size = Random.nextFloat() * 8f + 5f
            val color = confettiColors[Random.nextInt(confettiColors.size)]
            ConfettiData(startX, startY, speedY, size, color)
        }
    }

    val confettiAnim = remember { Animatable(0f) }
    LaunchedEffect(data.id) {
        confettiAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3500, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f * animAlpha.value)),
        contentAlignment = Alignment.Center
    ) {
        // Confetti Canvas
        Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val time = confettiAnim.value

            particles.forEach { c ->
                val currentX = centerX + c.startX + sin(time * 6 + c.startX) * 20f
                val currentY = centerY + c.startY + (c.speedY * time)
                val cAlpha = (1f - time).coerceIn(0f, 1f)
                drawCircle(
                    color = c.color.copy(alpha = cAlpha),
                    radius = c.size,
                    center = Offset(currentX, currentY)
                )
            }
        })

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 60.dp)
        ) {
            // Top Banner Tag ("Raj sent Crown to Lime")
            Row(
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFFFEF3C7))
                    .border(2.dp, Color(0xFFFFC527), RoundedCornerShape(100.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "👑", fontSize = 18.sp)
                Text(
                    text = "${data.senderName} sent ${data.giftName ?: "Gift"} to ${data.recipientName}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF78350F),
                    textAlign = TextAlign.Center
                )
                Text(text = "✨", fontSize = 18.sp)
            }

            // Large Pulsing Icon Container
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = animScale.value * pulseScale.value
                        scaleY = animScale.value * pulseScale.value
                    },
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .shadow(20.dp, CircleShape, spotColor = Color(0xFFFFC527))
                        .clip(CircleShape)
                        .background(Color(0xFFFFFBEB))
                        .border(4.dp, Color(0xFFFFC527), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (data.iconUrl.isNotBlank()) {
                        AsyncImage(
                            model = data.iconUrl,
                            contentDescription = data.giftName ?: "Gift",
                            modifier = Modifier.size(80.dp)
                        )
                    } else {
                        Text(text = "🎁", fontSize = 60.sp)
                    }
                }

                if (data.quantity > 1) {
                    Box(
                        modifier = Modifier
                            .offset(x = 10.dp, y = (-6).dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFFFC527))
                            .border(2.dp, Color.White, RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "x${data.quantity}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E1B18)
                        )
                    }
                }
            }
        }
    }
}

private data class ParticleData(
    val angle: Double,
    val distance: Float,
    val size: Float,
    val color: Color
)

private data class ConfettiData(
    val startX: Float,
    val startY: Float,
    val speedY: Float,
    val size: Float,
    val color: Color
)
