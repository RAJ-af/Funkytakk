package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.sin

/**
 * 1. Recording State
 * Live waveform while you speak, pulsating mic, slide to cancel / lock hints.
 */
@Composable
fun VoiceRecordingLiveBar(
    recordingDurationSeconds: Int,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onLockRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liveWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val micPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(28.dp), ambientColor = Color(0xFFFFD1D5), spotColor = Color(0xFFF43F5E))
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFFFF1F2))
                .border(1.dp, Color(0xFFFFCCD3), RoundedCornerShape(28.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE11D48))
                        .clickable { onStopRecording() },
                    contentAlignment = Alignment.Center
                ) {
                    FunkyStopSquareIcon(color = Color.White, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Live dynamic waveform
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                ) {
                    val barCount = 20
                    for (i in 0 until barCount) {
                        val heightFraction = remember(i, wavePhase) {
                            val base = (sin(wavePhase + (i * 0.45f)) + 1f) / 2f
                            val centerWeight = 1f - (kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)) * 0.4f
                            (base * centerWeight).coerceIn(0.2f, 1f)
                        }
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((24 * heightFraction).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFE11D48).copy(alpha = if (i > 14) 0.45f else 0.9f))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Timer
                val minutes = recordingDurationSeconds / 60
                val seconds = recordingDurationSeconds % 60
                Text(
                    text = String.format("%d:%02d", minutes, seconds),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Pulsing Mic Icon on Right
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFFFDA4AF).copy(alpha = 0.35f),
                                radius = size.minDimension / 2f * micPulse
                            )
                        }
                        .clip(CircleShape)
                        .background(Color(0xFFFFE4E6)),
                    contentAlignment = Alignment.Center
                ) {
                    FunkyMicIcon(color = Color(0xFFE11D48), modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Slide helpers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onCancelRecording() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE11D48)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Slide left to cancel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4B5563)
                )
            }

            Row(
                modifier = Modifier.clickable { onLockRecording() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                FunkyLockIcon(color = Color(0xFF4B5563), modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Slide up to lock recording",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4B5563)
                )
            }
        }
    }
}

/**
 * 2. After recording (preview) State
 * Quick preview before sending with Play, Waveform, Duration, Send & Delete buttons.
 */
@Composable
fun VoicePreviewBar(
    durationString: String = "0:18",
    isPlaying: Boolean,
    playbackProgress: Float, // 0f..1f
    onTogglePlay: () -> Unit,
    onDelete: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x1A000000), spotColor = Color(0x1F000000))
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(32.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        FunkyPauseIcon(color = Color.Black, modifier = Modifier.size(14.dp))
                    } else {
                        FunkyPlayIcon(color = Color.Black, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Waveform with played gold progress
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val staticHeights = listOf(
                        0.25f, 0.45f, 0.7f, 0.95f, 0.6f, 0.85f, 1.0f, 0.5f,
                        0.75f, 0.9f, 0.65f, 0.4f, 0.8f, 0.6f, 0.35f, 0.2f, 0.15f, 0.15f
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        staticHeights.forEachIndexed { index, frac ->
                            val itemFraction = index.toFloat() / staticHeights.size.toFloat()
                            val isPlayed = itemFraction <= playbackProgress
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height((22 * frac).coerceAtLeast(3f).dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(if (isPlayed) Color(0xFFF59E0B) else Color(0xFF9CA3AF))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Duration text
                Text(
                    text = durationString,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Send Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFBBF24))
                        .clickable { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    FunkySendIcon(color = Color.Black, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom Action Labels: Delete on left, Send on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDelete() }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FunkyDeleteIcon(color = Color(0xFF6B7280), modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Delete",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4B5563)
                )
            }

            Text(
                text = "Send",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF4B5563),
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}

/**
 * 3. Sent Voice Note Bubble (State 3) & 5. Playing State (State 5)
 * Clean & minimal yellow card with smooth moving dot progress during playback.
 */
@Composable
fun SentVoiceNoteBubble(
    senderAvatarUrl: String?,
    durationString: String = "0:18",
    timeString: String = "9:16 AM",
    isPlaying: Boolean,
    playbackProgress: Float, // 0f..1f
    onTogglePlay: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {
        // Bubble container
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .shadow(4.dp, RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp), ambientColor = Color(0x12000000))
                .clip(RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                .background(if (isPlaying) Color(0xFFFDE047) else Color(0xFFFEE589))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play / Pause Circle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onTogglePlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlaying) {
                            FunkyPauseIcon(color = Color.Black, modifier = Modifier.size(13.dp))
                        } else {
                            FunkyPlayIcon(color = Color.Black, modifier = Modifier.size(13.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Waveform with moving progress dot (Smooth progress with a moving dot)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val staticHeights = listOf(
                            0.3f, 0.5f, 0.75f, 1.0f, 0.7f, 0.9f, 0.6f, 0.4f,
                            0.2f, 0.15f, 0.15f, 0.15f, 0.15f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.5.dp)
                        ) {
                            staticHeights.forEachIndexed { index, frac ->
                                val itemProgress = index.toFloat() / staticHeights.size.toFloat()
                                val isPlayed = itemProgress <= playbackProgress
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height((20 * frac).coerceAtLeast(3f).dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(
                                            if (isPlayed) Color(0xFF1F2937) else Color(0xFF78716C)
                                        )
                                )
                            }
                        }

                        // Moving Indicator Dot (State 5)
                        if (isPlaying && playbackProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(playbackProgress.coerceIn(0.05f, 0.95f))
                                    .wrapContentWidth(Alignment.End)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B))
                                        .border(1.5.dp, Color.White, CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Duration
                    Text(
                        text = durationString,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Time + Double checkmark status on bottom right
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        fontSize = 10.5.sp,
                        color = Color(0xFF4B5563),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FunkyDoubleCheckIcon(color = Color(0xFF374151), modifier = Modifier.size(13.dp))
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Sender Avatar on Right
        AsyncImage(
            model = senderAvatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            contentDescription = "My Avatar",
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(1.dp, Color(0xFFE5E7EB), CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * 4. Received Voice Note Bubble (State 4)
 * Soft off-white card with speed pill control, transcript quote, and Fluent 3D emoji reaction.
 */
@Composable
fun ReceivedVoiceNoteBubble(
    partnerAvatarUrl: String?,
    durationString: String = "0:24",
    timeString: String = "9:17 AM",
    isPlaying: Boolean,
    playbackProgress: Float, // 0f..1f
    currentSpeed: Float = 1.0f,
    transcriptText: String? = "“Let's plan it together...”",
    reactionEmoji: String? = "❤️",
    reactionCount: Int = 1,
    onTogglePlay: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onReactionClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSpeedSheet by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Partner Avatar
        AsyncImage(
            model = partnerAvatarUrl ?: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
            contentDescription = "Partner Avatar",
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(1.dp, Color(0xFFE5E7EB), CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            // Main Bubble with reaction
            Box {
                Box(
                    modifier = Modifier
                        .widthIn(max = 290.dp)
                        .shadow(3.dp, RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp), ambientColor = Color(0x0D000000))
                        .clip(RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play / Pause Circle
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable { onTogglePlay() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPlaying) {
                                    FunkyPauseIcon(color = Color.Black, modifier = Modifier.size(13.dp))
                                } else {
                                    FunkyPlayIcon(color = Color.Black, modifier = Modifier.size(13.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Waveform
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val staticHeights = listOf(
                                    0.25f, 0.45f, 0.8f, 0.95f, 0.6f, 0.9f, 0.75f, 0.5f,
                                    0.3f, 0.15f, 0.15f, 0.15f, 0.15f
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.5.dp)
                                ) {
                                    staticHeights.forEachIndexed { index, frac ->
                                        val itemProgress = index.toFloat() / staticHeights.size.toFloat()
                                        val isPlayed = itemProgress <= playbackProgress
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height((20 * frac).coerceAtLeast(3f).dp)
                                                .clip(RoundedCornerShape(1.5.dp))
                                                .background(
                                                    if (isPlayed) Color(0xFF1F2937) else Color(0xFF9CA3AF)
                                                )
                                        )
                                    }
                                }

                                // Moving Dot during play
                                if (isPlaying && playbackProgress > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(playbackProgress.coerceIn(0.05f, 0.95f))
                                            .wrapContentWidth(Alignment.End)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1E293B))
                                                .border(1.5.dp, Color.White, CircleShape)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Duration
                            Text(
                                text = durationString,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bottom row: Speed control pill on left, Timestamp on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speed Pill
                            val speedText = if (currentSpeed == 1.0f) "1x" else "${currentSpeed}x"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE5E7EB))
                                    .clickable {
                                        val nextSpeed = when (currentSpeed) {
                                            0.5f -> 1.0f
                                            1.0f -> 1.2f
                                            1.2f -> 1.5f
                                            1.5f -> 2.0f
                                            else -> 1.0f
                                        }
                                        onSpeedChange(nextSpeed)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = speedText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF374151)
                                )
                            }

                            // Timestamp
                            Text(
                                text = timeString,
                                fontSize = 10.5.sp,
                                color = Color(0xFF6B7280),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Reaction pill on bottom-right corner (Fluent Emoji + count)
                if (reactionEmoji != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 10.dp)
                            .shadow(3.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                            .clickable { onReactionClick() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FluentEmojiIcon(
                                emoji = reactionEmoji,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$reactionCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            // Optional transcript quote underneath
            if (!transcriptText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = transcriptText,
                    fontSize = 12.5.sp,
                    color = Color(0xFF6B7280),
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

/**
 * 6. Speed options (simple) row
 * 0.5x, 1x (selected yellow), 1.5x, 2x
 */
@Composable
fun VoiceSpeedSelectorRow(
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val speedOptions = listOf(0.5f, 1.0f, 1.5f, 2.0f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        speedOptions.forEach { speed ->
            val isSelected = selectedSpeed == speed
            val label = if (speed == 1.0f) "1x" else "${speed}x"

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Color(0xFFFBBF24) else Color(0xFFF3F4F6))
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFFF59E0B) else Color(0xFFE5E7EB),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSpeedSelected(speed) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.Black else Color(0xFF4B5563)
                )
            }
        }
    }
}
