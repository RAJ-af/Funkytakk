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
            if (effectiveFrame != null) {
                val context = LocalContext.current
                val asset = effectiveFrame.asset
                var isAssetLoadFailed by remember(asset?.url) { mutableStateOf(false) }

                if (asset != null && asset.url.isNotBlank() && !isAssetLoadFailed) {
                    val frameSize = size * asset.scale
                    val imageRequest = remember(context, asset.url, enableFrameAnimation) {
                        ImageRequest.Builder(context)
                            .data(asset.url)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .crossfade(true)
                            .apply {
                                if (enableFrameAnimation && (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)) {
                                    decoderFactory(coil.decode.ImageDecoderDecoder.Factory())
                                }
                            }
                            .listener(
                                onError = { _, _ -> isAssetLoadFailed = true }
                            )
                            .build()
                    }

                    // Transparent Frame Image Asset Overlaid ON TOP of Avatar
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "Avatar Frame Asset",
                        modifier = Modifier
                            .size(frameSize)
                            .offset(x = asset.offset.xDp.dp, y = asset.offset.yDp.dp)
                            .zIndex(5f),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Procedural Ring Canvas fallback
                    val ring = effectiveFrame.ring
                    val ringColors = remember(ring.colorsHex) {
                        ring.colorsHex.map { com.example.store.parseHexColor(it, Color(0xFFFFD700)) }
                    }

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

                    // Frame Canvas: Ring & Shapes
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale)
                            .rotate(rotationAngle)
                    ) {
                        val strokeW = (ring.widthDp * (size.value / 48f).coerceIn(0.6f, 1.8f)).dp.toPx()
                        val stroke = Stroke(width = strokeW)
                        val brush = if (ringColors.size > 1) {
                            Brush.sweepGradient(ringColors)
                        } else androidx.compose.ui.graphics.SolidColor(ringColors.firstOrNull() ?: Color(0xFFFFD700))

                        val center = androidx.compose.ui.geometry.Offset(this.size.width / 2f, this.size.height / 2f)
                        val radius = (this.size.width - strokeW) / 2f

                        when (ring.shape.lowercase()) {
                            "hexagon" -> {
                                val p = androidx.compose.ui.graphics.Path()
                                for (i in 0 until 6) {
                                    val aRad = Math.toRadians((i * 60 - 30).toDouble())
                                    val x = center.x + (radius * cos(aRad)).toFloat()
                                    val y = center.y + (radius * sin(aRad)).toFloat()
                                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                                }
                                p.close()
                                drawPath(p, brush, style = stroke)
                            }
                            "scalloped" -> {
                                val p = androidx.compose.ui.graphics.Path()
                                val scallops = 12
                                for (i in 0 until scallops) {
                                    val a1 = Math.toRadians((i * (360.0 / scallops)).toDouble())
                                    val r = if (i % 2 == 0) radius else radius * 0.88f
                                    val x = center.x + (r * cos(a1)).toFloat()
                                    val y = center.y + (r * sin(a1)).toFloat()
                                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                                }
                                p.close()
                                drawPath(p, brush, style = stroke)
                            }
                            "double" -> {
                                drawCircle(brush, radius = radius, center = center, style = Stroke(width = strokeW * 0.6f))
                                drawCircle(brush, radius = radius * 0.82f, center = center, style = Stroke(width = strokeW * 0.4f))
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
                                val px = center.x + ((radius + strokeW) * cos(angle)).toFloat()
                                val py = center.y + ((radius + strokeW) * sin(angle)).toFloat()
                                val pColor = pColors[i % pColors.size]
                                drawCircle(pColor, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(px, py))
                            }
                        }
                    }

                    // Ornament Emoji
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

            // Avatar Core Container with Ring
            val ringModifier = when {
                effectiveFrame != null -> Modifier // Frame takes precedence
                isSpeaking -> Modifier.border(2.5.dp, Color(0xFF10B981), CircleShape)
                isOnline -> Modifier.border(2.5.dp, Color(0xFF10B981), CircleShape)
                else -> Modifier // Offline: No outer ring
            }

            val paddingInsideRing = when {
                effectiveFrame != null -> (effectiveFrame.ring.widthDp * (size.value / 48f).coerceIn(0.6f, 1.8f) + 1f).dp
                isSpeaking || isOnline -> 3.dp
                else -> 0.dp
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
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

            // Country Flag Badge Overlay only if explicitly requested (Bottom-Right corner)
            if (showFlagBadge && !finalCountryCode.isNullOrBlank()) {
                val flagBadgeSize = (size.value * 0.32f).dp.coerceAtLeast(15.dp)
                val surfaceColor = MaterialTheme.colorScheme.surface
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .zIndex(5f)
                        .size(flagBadgeSize)
                        .background(surfaceColor, CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircleFlag(
                        countryCode = finalCountryCode,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}
