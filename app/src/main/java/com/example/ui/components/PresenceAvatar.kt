package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

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
    onClick: (() -> Unit)? = null
) {
    val finalCountryCode = (flagCountryCode ?: countryCode)?.trim()?.ifBlank { null }
    val effectiveStatus = when {
        isSpeaking -> "Speaking"
        isOnline -> "Online"
        else -> "Offline"
    }

    val clickableModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    // Outer container with padding so flag & moon badge offsets don't get clipped by parent
    Box(
        modifier = clickableModifier
            .padding(3.dp)
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

            // Avatar Core Container with Ring
            val ringModifier = when {
                isSpeaking -> Modifier.border(2.5.dp, Color(0xFF10B981), CircleShape)
                isOnline -> Modifier.border(2.5.dp, Color(0xFF10B981), CircleShape)
                else -> Modifier // Offline: No outer ring
            }

            val paddingInsideRing = if (isSpeaking || isOnline) 3.dp else 0.dp

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

            // 2. Offline State: Moon icon overlay at TopEnd (Top Right)
            if (!isSpeaking && !isOnline) {
                val moonBadgeSize = (size.value * 0.28f).dp.coerceAtLeast(14.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .zIndex(4f)
                        .size(moonBadgeSize)
                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Nightlight,
                        contentDescription = "Offline Moon",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 3. Country Flag Badge Overlay at BottomStart (Bottom Left)
            if (!finalCountryCode.isNullOrBlank()) {
                val flagBadgeSize = (size.value * 0.28f).dp.coerceAtLeast(14.dp)
                val surfaceColor = MaterialTheme.colorScheme.surface
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-3).dp, y = 3.dp)
                        .zIndex(5f) // High zIndex over ring & avatar
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
