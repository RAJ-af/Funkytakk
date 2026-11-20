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

            // Core FramedAvatar (ALWAYS exact requested size, unclipped overflow)
            FramedAvatar(
                avatarUrl = avatarUrl,
                avatarSize = size,
                frame = effectiveFrame,
                enableAnimation = enableFrameAnimation,
                contentDescription = effectiveStatus,
                modifier = Modifier.zIndex(1f)
            )

            // Online / Speaking green perimeter ring when user has no avatar frame equipped
            if (effectiveFrame == null && (isSpeaking || isOnline)) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .zIndex(2f)
                        .border(2.5.dp, Color(0xFF10B981), CircleShape)
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
