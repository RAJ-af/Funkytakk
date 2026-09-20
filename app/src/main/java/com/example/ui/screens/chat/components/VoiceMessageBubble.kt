package com.example.ui.screens.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.screens.chat.model.ChatMessage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoiceMessageBubble(
    message: ChatMessage,
    partnerAvatarUrl: String,
    isPlaying: Boolean = false,
    isHighlighted: Boolean = false,
    onPlayPauseClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isMe) {
            if (message.showAvatar) {
                AsyncImage(
                    model = partnerAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFE5E7EB), CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Spacer(modifier = Modifier.width(34.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Surface(
                shape = if (message.isMe) {
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
                } else {
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
                },
                color = if (message.isMe) Color(0xFFFCD34D) else Color(0xFFF3F4F6),
                border = if (isHighlighted) BorderStroke(2.dp, Color(0xFFFFC529)) else null,
                shadowElevation = if (isHighlighted) 6.dp else 0.dp,
                modifier = Modifier
                    .widthIn(min = 210.dp, max = 260.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Play/Pause Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (message.isMe) Color.White.copy(alpha = 0.9f) else Color(0xFFFBBF24))
                            .clickable { onPlayPauseClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlaying) {
                            AppSvgIcon(
                                icon = ChatIconAsset.Pause,
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            AppSvgIcon(
                                icon = ChatIconAsset.Play,
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Waveform Bars
                    val waveHeights = listOf(
                        0.2f, 0.4f, 0.7f, 0.9f, 0.6f, 0.85f, 1.0f, 0.65f,
                        0.9f, 0.75f, 0.45f, 0.2f
                    )

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                    ) {
                        waveHeights.forEach { frac ->
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .height((20 * frac).coerceAtLeast(3f).dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(if (message.isMe) Color(0xFF78350F).copy(alpha = 0.75f) else Color(0xFF4B5563))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = message.audioDuration,
                        fontSize = 11.5.sp,
                        color = if (message.isMe) Color(0xFF78350F) else Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = message.time,
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
