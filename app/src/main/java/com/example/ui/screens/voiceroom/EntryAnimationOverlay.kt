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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class EntryBannerConfig(
    val textColorHex: String = "#78350F",
    val bgColorHex: String = "#FEF3C7"
)

data class EntryAnimationConfig(
    val kind: String = "preset", // "preset" or "asset"
    val preset: String = "golden_beam", // "golden_beam", "butterflies", "portal_ring", "heart_bloom"
    val colorsHex: List<String> = emptyList(),
    val count: Int = 20,
    val durationMs: Long = 3000L,
    val bannerConfig: EntryBannerConfig = EntryBannerConfig(),
    val assetUrl: String? = null,
    val assetFormat: String? = null
)

data class EntryAnimationData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String,
    val userName: String,
    val config: EntryAnimationConfig
)

fun parseEntryAnimationConfig(rawConfig: Map<String, Any?>?): EntryAnimationConfig {
    if (rawConfig == null) return EntryAnimationConfig()
    val kind = rawConfig["kind"]?.toString() ?: "preset"
    val preset = rawConfig["preset"]?.toString() ?: "golden_beam"

    val rawColors = rawConfig["colors"]
    val colorsHex = when (rawColors) {
        is List<*> -> rawColors.mapNotNull { it?.toString() }
        else -> emptyList()
    }

    val count = (rawConfig["count"] as? Number)?.toInt()
        ?: rawConfig["count"]?.toString()?.toIntOrNull()
        ?: 20
    val durationMs = (rawConfig["duration_ms"] as? Number)?.toLong()
        ?: rawConfig["duration_ms"]?.toString()?.toLongOrNull()
        ?: 3000L

    val bannerMap = rawConfig["banner"] as? Map<*, *>
    val bannerText = bannerMap?.get("text")?.toString() ?: "#78350F"
    val bannerBg = bannerMap?.get("bg")?.toString() ?: "#FEF3C7"

    val assetMap = rawConfig["asset"] as? Map<*, *>
    val assetUrl = assetMap?.get("url")?.toString() ?: rawConfig["asset_url"]?.toString()
    val assetFormat = assetMap?.get("format")?.toString() ?: rawConfig["asset_format"]?.toString()

    return EntryAnimationConfig(
        kind = kind,
        preset = preset,
        colorsHex = colorsHex,
        count = count.coerceIn(5, 30), // Capped at max 30 for performance
        durationMs = durationMs.coerceIn(1000L, 6000L),
        bannerConfig = EntryBannerConfig(textColorHex = bannerText, bgColorHex = bannerBg),
        assetUrl = assetUrl,
        assetFormat = assetFormat
    )
}

private fun parseColorHex(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val cleanHex = hex.trim().removePrefix("#")
        val colorInt = when (cleanHex.length) {
            6 -> android.graphics.Color.parseColor("#$cleanHex")
            8 -> android.graphics.Color.parseColor("#$cleanHex")
            else -> return fallback
        }
        Color(colorInt)
    } catch (_: Exception) {
        fallback
    }
}

/**
 * Singleton queue manager for room entry animations.
 * Max queue size 5, drops oldest when full, non-touch blocking, background lifecycle cleanup.
 */
object EntryAnimationManager {
    private const val MAX_QUEUE_SIZE = 5

    private val queue = ArrayDeque<EntryAnimationData>()
    private val _currentAnimation = MutableStateFlow<EntryAnimationData?>(null)
    val currentAnimation = _currentAnimation.asStateFlow()

    @Volatile
    private var isProcessing = false

    fun enqueueAnimation(data: EntryAnimationData, context: Context) {
        synchronized(queue) {
            if (queue.size >= MAX_QUEUE_SIZE) {
                if (queue.isNotEmpty()) queue.removeFirst()
            }
            queue.addLast(data)
        }
        processQueue(context)
    }

    private fun processQueue(context: Context) {
        if (isProcessing) return
        isProcessing = true

        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                val nextItem: EntryAnimationData? = synchronized(queue) {
                    if (queue.isNotEmpty()) queue.removeFirst() else null
                }

                if (nextItem == null) {
                    _currentAnimation.value = null
                    isProcessing = false
                    break
                }

                _currentAnimation.value = nextItem
                val duration = nextItem.config.durationMs
                delay(duration)
            }
        }
    }

    fun clearQueue() {
        synchronized(queue) {
            queue.clear()
        }
        _currentAnimation.value = null
        isProcessing = false
    }

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
 * Non-touch blocking Compose overlay for displaying entry animations and entrance banners.
 */
@Composable
fun EntryAnimationOverlay(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeAnimation by EntryAnimationManager.currentAnimation.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                EntryAnimationManager.clearQueue()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val item = activeAnimation ?: return
    val sysDisabled = remember(context) { EntryAnimationManager.areAnimationsDisabled(context) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Non-touch blocking overlay
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        if (!sysDisabled) {
            when (item.config.kind) {
                "asset" -> AssetEntryAnimation(item = item)
                "preset" -> PresetCanvasAnimation(item = item)
                else -> {
                    // Unknown kind -> fallback banner only
                }
            }
        }

        // Bottom Entrance Banner ("<Name> entered")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            EntryBanner(
                userName = item.userName,
                bannerConfig = item.config.bannerConfig
            )
        }
    }
}

@Composable
private fun PresetCanvasAnimation(item: EntryAnimationData) {
    val config = item.config
    when (config.preset) {
        "golden_beam" -> GoldenBeamAnimation(config = config)
        "butterflies" -> ButterfliesAnimation(config = config)
        "portal_ring" -> PortalRingAnimation(config = config)
        "heart_bloom" -> HeartBloomAnimation(config = config)
        else -> {
            // Unknown preset -> Fallback to banner only
        }
    }
}

/**
 * Golden Beam preset: Vertical golden ray from top down + rising golden sparkles.
 */
@Composable
private fun GoldenBeamAnimation(config: EntryAnimationConfig) {
    val infiniteTransition = rememberInfiniteTransition(label = "golden_beam_anim")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beamProgress"
    )

    val colors = remember(config.colorsHex) {
        if (config.colorsHex.isNotEmpty()) {
            config.colorsHex.map { parseColorHex(it, Color(0xFFFFD700)) }
        } else {
            listOf(Color(0xFFFFD700), Color(0xFFFFF8DC), Color(0xFFFFA500), Color(0xFFFF8C00))
        }
    }

    val particleCount = remember(config.count) { config.count.coerceIn(5, 30) }
    val particles = remember(itemKey(config)) {
        List(particleCount) {
            GoldenParticle(
                xRatio = Random.nextFloat() * 0.5f + 0.25f,
                speedY = Random.nextFloat() * 0.4f + 0.3f,
                radius = Random.nextFloat() * 4f + 2f,
                seed = Random.nextFloat()
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Top-down golden ray beam
        val beamWidth = width * 0.45f
        val beamLeft = (width - beamWidth) / 2f

        val beamGradient = Brush.verticalGradient(
            colors = listOf(
                colors[0].copy(alpha = 0.65f),
                colors.getOrElse(1) { colors[0] }.copy(alpha = 0.35f),
                colors.getOrElse(2) { colors[0] }.copy(alpha = 0.10f),
                Color.Transparent
            ),
            startY = 0f,
            endY = height * 0.75f
        )

        drawRect(
            brush = beamGradient,
            topLeft = Offset(beamLeft, 0f),
            size = Size(beamWidth, height * 0.75f)
        )

        // Particles
        particles.forEach { p ->
            val py = height * 0.8f - ((progress + p.seed) % 1.0f) * (height * 0.75f)
            val px = width * p.xRatio + sin(progress * 6.28f + p.seed * 10f) * 20f
            val alpha = (1f - abs((py - height * 0.4f) / (height * 0.4f))).coerceIn(0f, 1f)

            drawCircle(
                color = colors[Random.nextInt(colors.size)].copy(alpha = alpha * 0.85f),
                radius = p.radius,
                center = Offset(px, py)
            )
        }
    }
}

private data class GoldenParticle(
    val xRatio: Float,
    val speedY: Float,
    val radius: Float,
    val seed: Float
)

/**
 * Butterflies preset: Flying butterflies moving along sine wave paths with flapping wings.
 */
@Composable
private fun ButterfliesAnimation(config: EntryAnimationConfig) {
    val infiniteTransition = rememberInfiniteTransition(label = "butterflies_anim")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "butterflyProgress"
    )

    val colors = remember(config.colorsHex) {
        if (config.colorsHex.isNotEmpty()) {
            config.colorsHex.map { parseColorHex(it, Color(0xFFC084FC)) }
        } else {
            listOf(Color(0xFFC084FC), Color(0xFFE879F9), Color(0xFFF472B6), Color(0xFFFFFFFF))
        }
    }

    val count = remember(config.count) { config.count.coerceIn(5, 30) }
    val butterflies = remember(itemKey(config)) {
        List(count) {
            ButterflyData(
                startX = Random.nextFloat() * 0.8f + 0.1f,
                startY = Random.nextFloat() * 0.3f + 0.65f,
                size = Random.nextFloat() * 12f + 10f,
                speed = Random.nextFloat() * 0.5f + 0.5f,
                seed = Random.nextFloat() * 6.28f,
                colorIdx = Random.nextInt(colors.size)
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        butterflies.forEach { b ->
            val cycle = (progress * b.speed + b.seed) % 1.0f
            val px = width * b.startX + sin(cycle * 12.56f + b.seed) * 50f
            val py = height * b.startY - cycle * (height * 0.6f)
            val alpha = (1f - cycle).coerceIn(0f, 1f)
            val wingScale = abs(sin(cycle * 30f + b.seed)) * 0.8f + 0.2f
            val color = colors[b.colorIdx % colors.size].copy(alpha = alpha * 0.9f)

            val s = b.size
            val leftWing = Path().apply {
                moveTo(px, py)
                cubicTo(px - s * wingScale, py - s * 0.8f, px - s * 1.2f * wingScale, py + s * 0.5f, px, py)
            }
            val rightWing = Path().apply {
                moveTo(px, py)
                cubicTo(px + s * wingScale, py - s * 0.8f, px + s * 1.2f * wingScale, py + s * 0.5f, px, py)
            }

            drawPath(leftWing, color = color)
            drawPath(rightWing, color = color)
            drawCircle(color = Color.White.copy(alpha = alpha), radius = 2f, center = Offset(px, py))
        }
    }
}

private data class ButterflyData(
    val startX: Float,
    val startY: Float,
    val size: Float,
    val speed: Float,
    val seed: Float,
    val colorIdx: Int
)

/**
 * Portal Ring preset: Expanding glowing portal ring with radiant particle energy.
 */
@Composable
private fun PortalRingAnimation(config: EntryAnimationConfig) {
    val infiniteTransition = rememberInfiniteTransition(label = "portal_ring_anim")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )

    val colors = remember(config.colorsHex) {
        if (config.colorsHex.isNotEmpty()) {
            config.colorsHex.map { parseColorHex(it, Color(0xFF38BDF8)) }
        } else {
            listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFF60A5FA), Color(0xFFFFFFFF))
        }
    }

    val count = remember(config.count) { config.count.coerceIn(5, 30) }
    val particles = remember(itemKey(config)) {
        List(count) {
            val angle = Random.nextFloat() * 6.28f
            val distance = Random.nextFloat() * 140f + 40f
            val radius = Random.nextFloat() * 4f + 2f
            PortalParticle(angle, distance, radius)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * 0.45f)
        val maxRadius = size.width * 0.35f * ringScale
        val alpha = (1f - ringScale).coerceIn(0f, 1f)

        // Concentric expanding glowing portal rings
        drawCircle(
            color = colors[0].copy(alpha = alpha * 0.7f),
            radius = maxRadius,
            center = center,
            style = Stroke(width = 6f)
        )
        drawCircle(
            color = colors.getOrElse(1) { colors[0] }.copy(alpha = alpha * 0.5f),
            radius = maxRadius * 0.75f,
            center = center,
            style = Stroke(width = 4f)
        )

        // Radial particles
        particles.forEach { p ->
            val pDist = p.distance * ringScale
            val px = center.x + cos(p.angle) * pDist
            val py = center.y + sin(p.angle) * pDist
            drawCircle(
                color = colors[Random.nextInt(colors.size)].copy(alpha = alpha),
                radius = p.radius,
                center = Offset(px, py)
            )
        }
    }
}

private data class PortalParticle(
    val angle: Float,
    val distance: Float,
    val radius: Float
)

/**
 * Heart Bloom preset: Blooming and floating hearts rising upwards.
 */
@Composable
private fun HeartBloomAnimation(config: EntryAnimationConfig) {
    val infiniteTransition = rememberInfiniteTransition(label = "heart_bloom_anim")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heartProgress"
    )

    val colors = remember(config.colorsHex) {
        if (config.colorsHex.isNotEmpty()) {
            config.colorsHex.map { parseColorHex(it, Color(0xFFF43F5E)) }
        } else {
            listOf(Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFFFDA4AF), Color(0xFFFFF1F2))
        }
    }

    val count = remember(config.count) { config.count.coerceIn(5, 30) }
    val hearts = remember(itemKey(config)) {
        List(count) {
            HeartData(
                xRatio = Random.nextFloat() * 0.7f + 0.15f,
                size = Random.nextFloat() * 16f + 10f,
                speed = Random.nextFloat() * 0.4f + 0.6f,
                seed = Random.nextFloat() * 6.28f,
                colorIdx = Random.nextInt(colors.size)
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        hearts.forEach { h ->
            val cycle = (progress * h.speed + h.seed) % 1.0f
            val px = width * h.xRatio + sin(cycle * 9.42f + h.seed) * 30f
            val py = height * 0.75f - cycle * (height * 0.55f)
            val alpha = (1f - cycle).coerceIn(0f, 1f)
            val color = colors[h.colorIdx % colors.size].copy(alpha = alpha * 0.9f)

            val s = h.size
            val heartPath = Path().apply {
                moveTo(px, py + s * 0.3f)
                cubicTo(px - s * 0.5f, py - s * 0.3f, px - s, py + s * 0.2f, px, py + s)
                cubicTo(px + s, py + s * 0.2f, px + s * 0.5f, py - s * 0.3f, px, py + s * 0.3f)
                close()
            }
            drawPath(heartPath, color = color)
        }
    }
}

private data class HeartData(
    val xRatio: Float,
    val size: Float,
    val speed: Float,
    val seed: Float,
    val colorIdx: Int
)

/**
 * Media asset fallback: Media3 ExoPlayer (.mp4), Lottie (.json), Coil (.webp/.gif)
 */
@Composable
private fun AssetEntryAnimation(item: EntryAnimationData) {
    var hasError by remember { mutableStateOf(false) }
    val url = item.config.assetUrl.orEmpty().trim()
    val format = item.config.assetFormat.orEmpty().lowercase()

    if (hasError || url.isBlank()) return

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            format == "mp4" || url.endsWith(".mp4", ignoreCase = true) -> {
                SilentMp4Player(
                    url = url,
                    onError = { hasError = true }
                )
            }
            format == "json" || url.endsWith(".json", ignoreCase = true) -> {
                val composition by rememberLottieComposition(LottieCompositionSpec.Url(url))
                val lottieState = animateLottieCompositionAsState(
                    composition = composition,
                    iterations = 1
                )
                if (composition != null) {
                    LottieAnimation(
                        composition = composition,
                        progress = { lottieState.progress },
                        modifier = Modifier.size(240.dp)
                    )
                }
            }
            else -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .listener(onError = { _, _ -> hasError = true })
                        .build(),
                    contentDescription = "Entry Animation",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(220.dp)
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun SilentMp4Player(url: String, onError: () -> Unit) {
    val context = LocalContext.current
    var hasError by remember { mutableStateOf(false) }

    val exoPlayer = remember(context, url) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_OFF
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
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

    if (hasError) return

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
 * Animated Entrance Banner at bottom of room screen.
 */
@Composable
private fun EntryBanner(
    userName: String,
    bannerConfig: EntryBannerConfig
) {
    val textColor = remember(bannerConfig.textColorHex) {
        parseColorHex(bannerConfig.textColorHex, Color(0xFF78350F))
    }
    val bgColor = remember(bannerConfig.bgColorHex) {
        parseColorHex(bannerConfig.bgColorHex, Color(0xFFFEF3C7))
    }

    val transition = rememberInfiniteTransition(label = "banner_pulse")
    val scale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bannerScale"
    )

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .border(2.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "👑", fontSize = 16.sp)
        Text(
            text = "$userName entered",
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            textAlign = TextAlign.Center
        )
        Text(text = "✨", fontSize = 16.sp)
    }
}

private fun itemKey(config: EntryAnimationConfig): String {
    return "${config.preset}_${config.count}_${config.colorsHex.joinToString()}"
}
