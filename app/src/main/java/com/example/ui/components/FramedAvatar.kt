package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.decode.ImageDecoderDecoder
import coil.drawable.MovieDrawable
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.repeatCount
import com.example.store.FrameAssetConfig
import com.example.store.FrameConfig
import com.example.store.parseHexColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * Canonical shared composable for rendering an avatar with an optional cosmetic frame
 * (both schema v2 animated/static WebP asset frames and procedural ring frames).
 *
 * Layout contract:
 * - Reserves [avatarSize] x [avatarSize] only; overflowing frame art is never clipped.
 * - Callers that need extra spacing for overflowing art can query [outerSize].
 * - The avatar photo is ALWAYS [avatarSize], centred, and clipped to [CircleShape].
 */
object FramedAvatar {

    val PROFILE_AVATAR_SIZE: Dp = 88.dp

    private fun resolveInnerRatio(asset: FrameAssetConfig): Float {
        val raw = when {
            asset.innerRatio > 0f -> asset.innerRatio
            asset.avatarRatio > 0f -> asset.avatarRatio
            else -> 0.6f
        }
        return if (raw.isFinite()) raw.coerceIn(0.25f, 0.98f) else 0.6f
    }

    fun canvasSize(avatarSize: Dp, frame: FrameConfig?): Dp {
        val asset = frame?.asset
        if (frame == null || !frame.kind.equals("asset", ignoreCase = true) || asset == null) {
            return avatarSize
        }
        return avatarSize / resolveInnerRatio(asset)
    }

    fun outerSize(avatarSize: Dp, frame: FrameConfig?): Dp {
        if (frame == null) return avatarSize
        val asset = frame.asset
        if (frame.kind.equals("asset", ignoreCase = true) && asset != null) {
            val canvas = canvasSize(avatarSize, frame)
            val overflow = if (asset.overflowRatio.isFinite()) asset.overflowRatio.coerceIn(0f, 0.5f) else 0f
            return canvas * (1f + overflow)
        }
        val ringScale = (avatarSize.value / 48f).coerceIn(0.6f, 1.8f)
        val strokeDp = (frame.ring.widthDp * ringScale).coerceAtLeast(1.5f).dp
        val shapeOverhang = when (frame.ring.shape.lowercase()) {
            "scalloped" -> avatarSize * 0.08f + strokeDp * 0.5f
            "double" -> strokeDp * 0.9f
            else -> strokeDp * 0.4f
        }
        val pulseOverhang = if (frame.ring.animation.equals("pulse", ignoreCase = true)) {
            avatarSize * 0.045f + strokeDp * 0.3f
        } else 0.dp
        val particleOverhang = if (frame.particles != null) strokeDp * 0.9f + 3.dp else 0.dp
        val ornamentOverhang = if (frame.ornament != null) 6.dp else 0.dp
        val sideMargin = maxOf(shapeOverhang, pulseOverhang, particleOverhang, ornamentOverhang)
        return avatarSize + sideMargin * 2
    }

    @Composable
    operator fun invoke(
        avatarUrl: String?,
        avatarSize: Dp,
        frame: FrameConfig?,
        modifier: Modifier = Modifier,
        enableAnimation: Boolean = true,
        contentDescription: String? = "User Avatar"
    ) {
        val context = LocalContext.current
        val density = LocalDensity.current
        val avatarPx = with(density) { avatarSize.roundToPx() }

        val asset = frame?.asset
        val isAssetKind = frame != null && frame.kind.equals("asset", ignoreCase = true) && asset != null
        var isAssetLoadFailed by remember(asset?.url, asset?.version) { mutableStateOf(false) }

        val shouldDrawAssetFrame = isAssetKind &&
            asset != null &&
            asset.url.isNotBlank() &&
            !isAssetLoadFailed &&
            avatarPx >= asset.minRenderPx

        Box(
            modifier = modifier.size(avatarSize),
            contentAlignment = Alignment.Center
        ) {
            if (shouldDrawAssetFrame && asset != null) {
                val innerRatio = resolveInnerRatio(asset)
                val canvas = avatarSize / innerRatio
                val dx = canvas * asset.offsetX.coerceIn(-0.25f, 0.25f)
                val dy = canvas * asset.offsetY.coerceIn(-0.25f, 0.25f)
                val isBelow = asset.z.equals("below", ignoreCase = true)
                val animateAsset = enableAnimation && asset.animated
                val versionKey = "${asset.url}_v${asset.version}"
                val canvasPx = with(density) { canvas.roundToPx() }.coerceIn(64, 720)
                val memCacheKey = "${versionKey}_${canvasPx}_${if (animateAsset) (if (asset.loop) "loop" else "once") else "static"}"

                val imageRequest = remember(context, asset.url, asset.version, animateAsset, asset.loop, canvasPx) {
                    ImageRequest.Builder(context)
                        .data(asset.url)
                        .size(canvasPx, canvasPx)
                        .memoryCacheKey(memCacheKey)
                        .diskCacheKey(versionKey)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .crossfade(false)
                        .apply {
                            if (animateAsset && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                decoderFactory(ImageDecoderDecoder.Factory())
                                repeatCount(if (asset.loop) MovieDrawable.REPEAT_INFINITE else 0)
                            } else {
                                decoderFactory(BitmapFactoryDecoder.Factory())
                            }
                        }
                        .listener(onError = { _, _ -> isAssetLoadFailed = true })
                        .build()
                }

                if (isBelow) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier
                            .requiredSize(canvas)
                            .offset(x = dx, y = dy)
                            .zIndex(0f),
                        contentScale = ContentScale.Fit
                    )
                }

                UserAvatarImage(
                    avatarUrl = avatarUrl,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .size(avatarSize)
                        .zIndex(1f)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                if (!isBelow) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier
                            .requiredSize(canvas)
                            .offset(x = dx, y = dy)
                            .zIndex(2f),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                // Avatar photo is ALWAYS exactly avatarSize, centred, circular.
                UserAvatarImage(
                    avatarUrl = avatarUrl,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .size(avatarSize)
                        .zIndex(1f)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                // Procedural ring frames keep their rendering centered on avatarSize (skip if this is an asset frame suppressed by min_render_px)
                if (frame != null && !isAssetKind) {
                    ProceduralRingFrameOverlay(
                        frame = frame,
                        avatarSize = avatarSize,
                        enableAnimation = enableAnimation
                    )
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ProceduralRingFrameOverlay(
    frame: FrameConfig,
    avatarSize: Dp,
    enableAnimation: Boolean
) {
    val ring = frame.ring
    val ringColors = remember(ring.colorsHex) {
        ring.colorsHex.map { parseHexColor(it, Color(0xFFFFD700)) }
    }
    val ringScale = (avatarSize.value / 48f).coerceIn(0.6f, 1.8f)
    val scaledStrokeDp = (ring.widthDp * ringScale).coerceAtLeast(1.5f)

    val rotationAngle = if (enableAnimation && ring.animation.equals("rotate", ignoreCase = true)) {
        val infiniteTransition = rememberInfiniteTransition(label = "frameRotate")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(ring.speedMs.toInt().coerceAtLeast(1000), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotate"
        )
        angle
    } else 0f

    val pulseScale = if (enableAnimation && ring.animation.equals("pulse", ignoreCase = true)) {
        val infiniteTransition = rememberInfiniteTransition(label = "framePulse")
        val pScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(ring.speedMs.toInt().coerceAtLeast(800) / 2, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        pScale
    } else 1f

    Canvas(
        modifier = Modifier
            .requiredSize(avatarSize)
            .zIndex(2f)
            .scale(pulseScale)
            .rotate(rotationAngle)
    ) {
        val strokeW = scaledStrokeDp.dp.toPx()
        val stroke = Stroke(width = strokeW)
        val brush = if (ringColors.size > 1) {
            Brush.sweepGradient(ringColors)
        } else {
            SolidColor(ringColors.firstOrNull() ?: Color(0xFFFFD700))
        }

        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val half = this.size.width / 2f
        val radius = half - strokeW * 0.25f

        when (ring.shape.lowercase()) {
            "hexagon" -> {
                val apothem = half - strokeW * 0.2f
                val hexRadius = apothem / 0.8660254f
                val p = Path()
                for (i in 0 until 6) {
                    val aRad = Math.toRadians((i * 60 - 30).toDouble())
                    val x = center.x + (hexRadius * cos(aRad)).toFloat()
                    val y = center.y + (hexRadius * sin(aRad)).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, brush, style = Stroke(width = strokeW, join = StrokeJoin.Round))
            }
            "scalloped" -> {
                val valleyR = half - strokeW * 0.15f
                val crestR = valleyR * 1.13f
                val p = Path()
                val lobes = 12
                for (i in 0 until lobes) {
                    val aStart = Math.toRadians((i * (360.0 / lobes)).toDouble())
                    val aMid = Math.toRadians(((i + 0.5) * (360.0 / lobes)).toDouble())
                    val aEnd = Math.toRadians(((i + 1) * (360.0 / lobes)).toDouble())
                    val startX = center.x + (valleyR * cos(aStart)).toFloat()
                    val startY = center.y + (valleyR * sin(aStart)).toFloat()
                    val ctrlX = center.x + (crestR * cos(aMid)).toFloat()
                    val ctrlY = center.y + (crestR * sin(aMid)).toFloat()
                    val endX = center.x + (valleyR * cos(aEnd)).toFloat()
                    val endY = center.y + (valleyR * sin(aEnd)).toFloat()
                    if (i == 0) p.moveTo(startX, startY)
                    p.quadraticTo(ctrlX, ctrlY, endX, endY)
                }
                p.close()
                drawPath(p, brush, style = Stroke(width = strokeW * 0.85f, join = StrokeJoin.Round))
            }
            "double" -> {
                val innerStrokeW = (strokeW * 0.55f).coerceAtLeast(1.5f)
                val outerStrokeW = (strokeW * 0.42f).coerceAtLeast(1.2f)
                val innerRadius = half - innerStrokeW * 0.4f
                val outerRadius = half + outerStrokeW * 0.65f
                drawCircle(brush, radius = outerRadius, center = center, style = Stroke(width = outerStrokeW))
                drawCircle(brush, radius = innerRadius, center = center, style = Stroke(width = innerStrokeW))
            }
            else -> {
                drawCircle(brush, radius = radius, center = center, style = stroke)
            }
        }

        if (enableAnimation && frame.particles != null) {
            val parts = frame.particles
            val pColors = parts.colorsHex.map { parseHexColor(it, Color.Yellow) }
            val count = parts.count.coerceIn(1, 8)
            for (i in 0 until count) {
                val angle = Math.toRadians((i * (360.0 / count) + rotationAngle).toDouble())
                val px = center.x + ((radius + strokeW * 0.75f) * cos(angle)).toFloat()
                val py = center.y + ((radius + strokeW * 0.75f) * sin(angle)).toFloat()
                val pColor = pColors[i % pColors.size]
                drawCircle(pColor, radius = 2.dp.toPx(), center = Offset(px, py))
            }
        }
    }

    frame.ornament?.let { orn ->
        val align = when (orn.position) {
            "top" -> Alignment.TopCenter
            "bottom" -> Alignment.BottomCenter
            else -> Alignment.TopEnd
        }
        val fontSizeSp = (avatarSize.value * 0.32f * orn.scale).coerceIn(10f, 28f).sp
        Text(
            text = orn.emoji,
            fontSize = fontSizeSp,
            modifier = Modifier
                .align(align)
                .offset(x = 2.dp, y = (-2).dp)
                .zIndex(6f)
        )
    }
}
