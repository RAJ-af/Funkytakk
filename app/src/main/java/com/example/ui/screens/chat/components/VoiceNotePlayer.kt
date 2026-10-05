package com.example.ui.screens.chat.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FunkyPauseIcon
import com.example.ui.components.FunkyPlayIcon
import kotlin.math.max

@Composable
fun VoiceNotePlayer(
    isPlaying: Boolean,
    isDecrypting: Boolean = false,
    hasBeenPlayed: Boolean = true,
    audioDurationFormatted: String = "0:12",
    audioDurationSecs: Int = 12,
    playbackProgress: Float = 0f, // 0.0f to 1.0f
    amplitudes: List<Int>? = null, // 48 amplitude values (0-255)
    speedMultiplier: Float = 1.0f,
    onPlayPauseClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) DarkChatColors else LightChatColors

    // Generate pseudo-random realistic waveform if amplitudes not supplied
    val waveformBars = remember(amplitudes) {
        if (!amplitudes.isNullOrEmpty()) {
            val list = amplitudes.take(48)
            if (list.size < 48) {
                list + List(48 - list.size) { 40 }
            } else list
        } else {
            // Default 48 realistic bars matching mockup
            listOf(
                20, 35, 60, 110, 180, 220, 140, 90, 160, 240, 200, 130,
                80, 120, 190, 255, 210, 150, 95, 170, 230, 180, 110, 70,
                140, 210, 240, 160, 100, 180, 220, 150, 85, 130, 200, 240,
                170, 110, 65, 140, 190, 220, 160, 100, 60, 40, 25, 15
            )
        }
    }

    Row(
        modifier = modifier
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 38dp Circular Play/Pause Button (Funky Yellow `#F6C945` background with dark icon)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFF6C945))
                .clickable(enabled = !isDecrypting) { onPlayPauseClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isDecrypting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF1B1B1B),
                    strokeWidth = 2.dp
                )
            } else if (isPlaying) {
                FunkyPauseIcon(color = Color(0xFF1B1B1B), modifier = Modifier.size(15.dp))
            } else {
                FunkyPlayIcon(color = Color(0xFF1B1B1B), modifier = Modifier.size(15.dp))
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // 48 Waveform Bars Canvas (3dp wide, 2dp gap, max 28dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeek(frac)
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ ->
                            val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeek(frac)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val totalBars = waveformBars.size
                    val barWidthPx = 3.dp.toPx()
                    val barGapPx = 2.dp.toPx()
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val playedColor = if (isDark) Color(0xFFF6C945) else Color(0xFFD97706)
                    val remainingColor = colors.secondaryText.copy(alpha = 0.35f)

                    val playedBarsCount = (totalBars * playbackProgress.coerceIn(0f, 1f)).toInt()

                    for (i in 0 until totalBars) {
                        val amp = waveformBars[i].coerceIn(10, 255)
                        val barHeight = (canvasHeight * (amp / 255f)).coerceAtLeast(4.dp.toPx())
                        val x = i * (barWidthPx + barGapPx)
                        val y = (canvasHeight - barHeight) / 2f

                        if (x + barWidthPx <= canvasWidth) {
                            val color = if (i <= playedBarsCount) playedColor else remainingColor
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, y),
                                size = Size(barWidthPx, barHeight),
                                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Duration + Speed Chip (e.g. "0:12 · 1x")
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.secondaryText.copy(alpha = 0.12f),
                    modifier = Modifier.clickable { onSpeedClick() }
                ) {
                    Text(
                        text = "$audioDurationFormatted · ${if (speedMultiplier == 1.5f) "1.5x" else if (speedMultiplier == 2.0f) "2x" else "1x"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.secondaryText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Unread dot if not played yet
                if (!hasBeenPlayed) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                }
            }
        }
    }
}
