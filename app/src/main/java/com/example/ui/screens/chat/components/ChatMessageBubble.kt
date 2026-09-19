package com.example.ui.screens.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.FluentEmojiIcon
import com.example.ui.components.FunkyDoubleCheckIcon
import com.example.ui.components.FunkyDownloadIcon
import com.example.ui.components.FunkyPauseIcon
import com.example.ui.components.FunkyPlayIcon
import com.example.ui.screens.chat.model.ChatMessage
import com.example.ui.screens.chat.model.ChatMessageType

@Composable
fun DateSeparatorPill(
    dateText: String = "Today",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF3F4F6))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = dateText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    partnerAvatarUrl: String,
    isPlayingAudio: Boolean = false,
    isHighlighted: Boolean = false,
    onLongClick: () -> Unit,
    onToggleAudioPlay: () -> Unit = {},
    onReactionClick: () -> Unit = {},
    onDownloadImageClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (isHighlighted) 1.02f else 1f,
        animationSpec = spring(),
        label = "scale"
    )

    if (message.isMe) {
        // ==========================================
        // SENT MESSAGE (Right Aligned, Warm Yellow)
        // ==========================================
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                color = Color(0xFFFCD34D),
                border = if (isHighlighted) BorderStroke(2.dp, Color.White) else null,
                shadowElevation = if (isHighlighted) 6.dp else 0.dp,
                modifier = Modifier
                    .scale(scaleAnim)
                    .widthIn(max = 290.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text,
                        fontSize = 14.5.sp,
                        color = Color(0xFF111827),
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.time,
                            fontSize = 11.sp,
                            color = Color(0xFF78350F).copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FunkyDoubleCheckIcon(
                            color = Color(0xFF78350F),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    } else {
        // ==========================================
        // RECEIVED MESSAGE (Left Aligned, Light Grey)
        // ==========================================
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Partner Avatar or Empty Spacer
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

            Column {
                when (message.type) {
                    ChatMessageType.TEXT -> {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                                    color = Color(0xFFF3F4F6),
                                    border = if (isHighlighted) BorderStroke(2.dp, Color(0xFFFFC529)) else null,
                                    shadowElevation = if (isHighlighted) 6.dp else 0.dp,
                                    modifier = Modifier
                                        .scale(scaleAnim)
                                        .widthIn(max = 260.dp)
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = onLongClick
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                        Text(
                                            text = if (message.isTranslated && message.translation != null) message.translation else message.text,
                                            fontSize = 14.5.sp,
                                            color = Color.Black,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }

                                // Reaction Pill (e.g. ❤️ 1)
                                if (message.reactions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(12.dp))
                                            .clickable { onReactionClick() }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            FluentEmojiIcon(
                                                emoji = message.reactions.first(),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${message.reactions.size}",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1F2937)
                                            )
                                        }
                                    }
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

                    ChatMessageType.AUDIO -> {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Surface(
                                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                                color = Color(0xFFF3F4F6),
                                border = if (isHighlighted) BorderStroke(2.dp, Color(0xFFFFC529)) else null,
                                shadowElevation = if (isHighlighted) 6.dp else 0.dp,
                                modifier = Modifier
                                    .scale(scaleAnim)
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
                                    // Circular Yellow Play Button
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFBBF24))
                                            .clickable { onToggleAudioPlay() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPlayingAudio) {
                                            FunkyPauseIcon(color = Color.Black, modifier = Modifier.size(13.dp))
                                        } else {
                                            FunkyPlayIcon(color = Color.Black, modifier = Modifier.size(13.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Symmetrical Waveform Bars
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
                                                    .background(Color(0xFF4B5563))
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = message.audioDuration,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF6B7280),
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

                    ChatMessageType.IMAGE -> {
                        Column {
                            Box(modifier = Modifier.widthIn(max = 260.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color(0xFFF3F4F6),
                                    border = if (isHighlighted) BorderStroke(2.dp, Color(0xFFFFC529)) else null,
                                    shadowElevation = if (isHighlighted) 6.dp else 0.dp,
                                    modifier = Modifier
                                        .scale(scaleAnim)
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = onLongClick
                                        )
                                ) {
                                    AsyncImage(
                                        model = message.imageUrl,
                                        contentDescription = "Shared photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                    )
                                }

                                // Floating Download Button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 12.dp, y = (-28).dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), CircleShape)
                                        .clickable { onDownloadImageClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    FunkyDownloadIcon(color = Color.Black, modifier = Modifier.size(18.dp))
                                }

                                // Floating Reaction Pill (e.g. ❤️ 2)
                                if (message.reactions.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .offset(x = 12.dp, y = 14.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(12.dp))
                                            .clickable { onReactionClick() }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            FluentEmojiIcon(
                                                emoji = message.reactions.first(),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${message.reactions.size}",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1F2937)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = message.time,
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }
        }
    }
}
