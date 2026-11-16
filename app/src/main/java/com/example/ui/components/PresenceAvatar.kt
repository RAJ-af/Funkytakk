package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlin.math.cos
import kotlin.math.sin

/**
 * Unified PresenceAvatar component used across the app (Room Seats, Follow List, Invite List, Chat List, Profile Header, Active Friends).
 *
 * Priority Rule: Speaking > Online > Offline
 * 1. Speaking: Animated voice ripple rings around avatar.
 * 2. Online: Outer green ring around avatar. No dot.
 * 3. Offline: Outline moon icon overlay badge at TopEnd (top-right). No ring.
 *
 * Badges:
 * - Country Flag Badge: BottomStart (bottom-left) corner (~28% size, 2.dp white border, zIndex high, slightly offset outwards).
 * - Offline Moon Badge: TopEnd (top-right) corner (~28% size, translucent dark background, white moon icon).
 */
@Composable
fun PresenceAvatar(
    avatarUrl: String?,
    size: Dp = 48.dp,
    isSpeaking: Boolean = false,
    isOnline: Boolean = false,
    countryCode: String? = null,
    flagCountryCode: String? = countryCode,
    modifier: Modifier = Modifier,
    userId: String? = null,
    frameConfig: com.example.store.AvatarFrameConfig? = null,
    enableFrameAnimation: Boolean = false,
    showFlagBadge: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val finalCountryCode = (flagCountryCode ?: countryCode)?.trim()?.ifBlank { null }
    val effectiveStatus = when {
        isSpeaking -> "Speaking"
        isOnline -> "Online"
        else -> "Offline"
    }

    // Resolve equipped avatar frame for this user (or fallback safely without crashing)
    val userCosmeticsMap by com.example.store.CosmeticsManager.userCosmeticsMap.collectAsState()
    val effectiveFrame = remember(frameConfig, userId, userCosmeticsMap) {
        if (frameConfig != null) {
            frameConfig
        } else if (!userId.isNullOrBlank()) {
            val userRow = userCosmeticsMap[userId] ?: com.example.store.CosmeticsManager.getCachedCosmetics(userId)
            val rawConfig = userRow?.avatar_frame?.config
            if (rawConfig != null) {
                runCatching { com.example.store.CosmeticsConfigParser.parseAvatarFrame(rawConfig) }.getOrNull()
            } else null
        } else null
    }

    val clickableModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    // Extra padding if there is a frame to prevent clipping ornaments & glows
    val outerPadding = if (effectiveFrame != null) 4.dp else 3.dp

    // Outer container with padding so flag & moon badge offsets don't get clipped by parent
    Box(
        modifier = clickableModifier
            .padding(outerPadding)
            .semantics { contentDescription = effectiveStatus },
        contentAlignment = Alignment.Center
    ) {
        // Core Avatar Box of requested size
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            // 1. Speaking State: Animated expanding ripple rings
            if (isSpeaking) {
                val infiniteTransition = rememberInfiniteTransition(label = "SpeakingRipple")
                val rippleScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "RippleScale"
                )
                val rippleAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 0.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "RippleAlpha"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = (this.size.minDimension / 2f) * rippleScale
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = rippleAlpha),
                        radius = radius,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            // Outer Frame Rendering (if user has equipped a frame)
            var isAssetLoadFailed by remember(effectiveFrame?.asset?.url) { mutableStateOf(false) }
            val asset = effectiveFrame?.asset
            val isAssetFrame = effectiveFrame != null &&
                effectiveFrame.kind.equals("asset", ignoreCase = true) &&
                asset != null &&
                asset.url.isNotBlank() &&
                !isAssetLoadFailed

            if (effectiveFrame != null) {
                val context = LocalContext.current

                if (isAssetFrame && asset != null) {
                    val ratio = asset.avatarRatio.coerceIn(0.25f, 1.0f)
                    val frameSize = size / ratio
                    val imageRequest = remember(context, asset.url, enableFrameAnimation) {
                        ImageRequest.Builder(context)
                            .data(asset.url)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .crossfade(true)
                            .apply {
                                if (enableFrameAnimation && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                    decoderFactory(coil.decode.ImageDecoderDecoder.Factory())
                                } else {
                                    decoderFactory(coil.decode.BitmapFactoryDecoder.Factory())
                                }
                            }
                            .listener(
                                onError = { _, _ -> isAssetLoadFailed = true }
                            )
                            .build()
                    }

                    // Transparent Frame Image Asset Overlaid ON TOP of Avatar (zIndex 2f)
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "Avatar Frame Asset",
                        modifier = Modifier
                            .size(frameSize)
                            .offset(x = asset.offset.xDp.dp, y = asset.offset.yDp.dp)
                            .zIndex(2f),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Procedural Ring Canvas fallback
                    val ring = effectiveFrame.ring
                    val ringColors = remember(ring.colorsHex) {
                        ring.colorsHex.map { com.example.store.parseHexColor(it, Color(0xFFFFD700)) }
                    }
                    val ringScale = (size.value / 48f).coerceIn(0.6f, 1.8f)
                    val scaledStrokeDp = (ring.widthDp * ringScale).coerceAtLeast(1.5f)

                    // Animation strictly controlled: only animate when enableFrameAnimation == true
                    val rotationAngle = if (enableFrameAnimation && ring.animation.equals("rotate", ignoreCase = true)) {
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

                    val pulseScale = if (enableFrameAnimation && ring.animation.equals("pulse", ignoreCase = true)) {
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

                    // Frame Canvas: Ring & Shapes (zIndex 2f) sitting flush on DP outline
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(2f)
                            .scale(pulseScale)
                            .rotate(rotationAngle)
                    ) {
                        val strokeW = scaledStrokeDp.dp.toPx()
                        val stroke = Stroke(width = strokeW)
                        val brush = if (ringColors.size > 1) {
                            Brush.sweepGradient(ringColors)
                        } else androidx.compose.ui.graphics.SolidColor(ringColors.firstOrNull() ?: Color(0xFFFFD700))

                        val center = androidx.compose.ui.geometry.Offset(this.size.width / 2f, this.size.height / 2f)
                        val half = this.size.width / 2f
                        // Inner edge of stroke meets DP perimeter cleanly with slight subpixel overlap (zero gap)
                        val radius = half - strokeW * 0.5f

                        when (ring.shape.lowercase()) {
                            "hexagon" -> {
                                val apothem = half - strokeW * 0.45f
                                val hexRadius = apothem / 0.8660254f
                                val p = androidx.compose.ui.graphics.Path()
                                for (i in 0 until 6) {
                                    val aRad = Math.toRadians((i * 60 - 30).toDouble())
                                    val x = center.x + (hexRadius * cos(aRad)).toFloat()
                                    val y = center.y + (hexRadius * sin(aRad)).toFloat()
                                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                                }
                                p.close()
                                drawPath(p, brush, style = Stroke(width = strokeW, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            }
                            "scalloped" -> {
                                val valleyR = half - strokeW * 0.35f
                                val crestR = valleyR * 1.13f
                                val p = androidx.compose.ui.graphics.Path()
                                val lobes = 12
                                for (i in 0 until lobes) {
                                    val aMid = Math.toRadians(((i + 0.5) * (360.0 / lobes)).toDouble())
                                    val aEnd = Math.toRadians(((i + 1) * (360.0 / lobes)).toDouble())
                                    val startX = center.x + (valleyR * cos(Math.toRadians((i * (360.0 / lobes))))).toFloat()
                                    val startY = center.y + (valleyR * sin(Math.toRadians((i * (360.0 / lobes))))).toFloat()
                                    val ctrlX = center.x + (crestR * cos(aMid)).toFloat()
                                    val ctrlY = center.y + (crestR * sin(aMid)).toFloat()
                                    val endX = center.x + (valleyR * cos(aEnd)).toFloat()
                                    val endY = center.y + (valleyR * sin(aEnd)).toFloat()
                                    if (i == 0) p.moveTo(startX, startY)
                                    p.quadraticTo(ctrlX, ctrlY, endX, endY)
                                }
                                p.close()
                                drawPath(p, brush, style = Stroke(width = strokeW * 0.85f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            }
                            "double" -> {
                                val innerStrokeW = (strokeW * 0.55f).coerceAtLeast(1.5f)
                                val outerStrokeW = (strokeW * 0.42f).coerceAtLeast(1.2f)
                                val innerRadius = half - strokeW * 0.65f
                                val outerRadius = half - outerStrokeW * 0.4f
                                drawCircle(brush, radius = outerRadius, center = center, style = Stroke(width = outerStrokeW))
                                drawCircle(brush, radius = innerRadius, center = center, style = Stroke(width = innerStrokeW))
                            }
                            else -> {
                                drawCircle(brush, radius = radius, center = center, style = stroke)
                            }
                        }

                        // Particles on Canvas
                        if (enableFrameAnimation && effectiveFrame.particles != null) {
                            val parts = effectiveFrame.particles
                            val pColors = parts.colorsHex.map { com.example.store.parseHexColor(it, Color.Yellow) }
                            val count = parts.count.coerceIn(1, 8)
                            for (i in 0 until count) {
                                val angle = Math.toRadians((i * (360.0 / count) + rotationAngle).toDouble())
                                val px = center.x + ((radius + strokeW * 0.75f) * cos(angle)).toFloat()
                                val py = center.y + ((radius + strokeW * 0.75f) * sin(angle)).toFloat()
                                val pColor = pColors[i % pColors.size]
                                drawCircle(pColor, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(px, py))
                            }
                        }
                    }

                    // Ornament Emoji (zIndex 6f)
                    effectiveFrame.ornament?.let { orn ->
                        val align = when (orn.position) {
                            "top" -> Alignment.TopCenter
                            "bottom" -> Alignment.BottomCenter
                            else -> Alignment.TopEnd
                        }
                        val fontSizeSp = (size.value * 0.32f * orn.scale).coerceIn(10f, 28f).sp
                        androidx.compose.material3.Text(
                            text = orn.emoji,
                            fontSize = fontSizeSp,
                            modifier = Modifier
                                .align(align)
                                .offset(x = 2.dp, y = (-2).dp)
                                .zIndex(6f)
                        )
                    }
                }
            }

            // Avatar Core Container with Ring (zIndex 1f)
            val ringModifier = when {
                effectiveFrame != null -> Modifier // Frame takes precedence
                isSpeaking -> Modifier.border(2.5.dp, Color(0xFF10B981), CircleShape)
                isOnline -> Modifier.border(2.5.dp, Color(0xFF10B981), CircleShape)
                else -> Modifier // Offline: No outer ring
            }

            val paddingInsideRing = when {
                isAssetFrame -> 0.dp // Asset frame sits flush around unpadded avatar
                effectiveFrame != null -> {
                    val ringScale = (size.value / 48f).coerceIn(0.6f, 1.8f)
                    val scaledStrokeDp = (effectiveFrame.ring.widthDp * ringScale).coerceAtLeast(1.5f)
                    // 0.8f * strokeWidth aligns DP edge flush under the inner stroke boundary with zero visible gap
                    (scaledStrokeDp * 0.8f).dp
                }
                isSpeaking || isOnline -> 2.dp
                else -> 0.dp
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .then(ringModifier)
                    .padding(paddingInsideRing)
                    .clip(CircleShape)
            ) {
                UserAvatarImage(
                    avatarUrl = avatarUrl,
                    contentDescription = effectiveStatus,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Country Flag Badge Overlay only if explicitly requested (Bottom-Right corner, zIndex 6f)
            if (showFlagBadge && !finalCountryCode.isNullOrBlank()) {
                val flagBadgeSize = (size.value * 0.32f).dp.coerceAtLeast(15.dp)
                val surfaceColor = com.example.ui.theme.LocalFunkyColors.current.surface
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .zIndex(6f)
                        .size(flagBadgeSize)
                        .background(surfaceColor, CircleShape)
                        .border(2.dp, surfaceColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircleFlag(
                        countryCode = finalCountryCode,
                        allowProgramming = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}
