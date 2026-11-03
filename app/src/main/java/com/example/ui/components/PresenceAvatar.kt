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

/**
 * Unified PresenceAvatar component used across the app (Room Seats, Follow List, Invite List, Chat List).
 *
 * Priority Rule: Speaking > Online > Offline
 * 1. Speaking: Animated voice ripple / bars around avatar.
 * 2. Online: Outer green ring around avatar. No dot.
 * 3. Offline: Outline moon icon overlay on corner. No ring.
 */
@Composable
fun PresenceAvatar(
    avatarUrl: String?,
    size: Dp = 48.dp,
    isSpeaking: Boolean = false,
    isOnline: Boolean = false,
    countryCode: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
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

    Box(
        modifier = clickableModifier
            .size(size)
            .semantics { contentDescription = effectiveStatus },
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

        // 2. Offline State: Moon icon overlay at bottom-right corner
        if (!isSpeaking && !isOnline) {
            val moonBadgeSize = (size.value * 0.38f).dp.coerceAtLeast(14.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(moonBadgeSize)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape)
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

        // Optional Flag Badge Overlay if countryCode is provided (top-left or bottom-left)
        if (!countryCode.isNullOrBlank()) {
            val flagSize = (size.value * 0.32f).dp.coerceAtLeast(12.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .size(flagSize)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color.White, CircleShape)
            ) {
                CircleFlag(
                    countryCode = countryCode,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }
    }
}
