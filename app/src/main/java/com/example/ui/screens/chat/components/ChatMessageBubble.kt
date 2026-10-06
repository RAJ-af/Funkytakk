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
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.05f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = dateText,
                fontSize = 11.5.sp,
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
    onImageClick: (String) -> Unit = {},
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
        // SENT MESSAGE (Right Aligned, FunkyTalk Yellow #F6C945)
        // ==========================================
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                    color = Color(0xFFF6C945),
                    shadowElevation = if (isHighlighted) 4.dp else 0.dp,
                    modifier = Modifier
                        .scale(scaleAnim)
                        .widthIn(max = 285.dp)
                        .combinedClickable(
                            onClick = {
                                if (message.type == ChatMessageType.IMAGE && message.getAllImages().isNotEmpty()) {
                                    onImageClick(message.getAllImages().first())
                                }
                            },
                            onLongClick = onLongClick
                        )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)) {
                        when (message.type) {
                            ChatMessageType.IMAGE -> {
                                val img = message.imageUrl ?: message.getAllImages().firstOrNull() ?: ""
                                AsyncImage(
                                    model = img,
                                    contentDescription = "Photo message",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 140.dp, max = 220.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onImageClick(img) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            ChatMessageType.AUDIO -> {
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF18181B))
                                            .clickable { onToggleAudioPlay() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPlayingAudio) {
                                            FunkyPauseIcon(color = Color.White, modifier = Modifier.size(13.dp))
                                        } else {
                                            FunkyPlayIcon(color = Color.White, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = message.audioDuration.ifBlank { "0:15" },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF18181B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            else -> {
                                Text(
                                    text = message.text,
                                    fontSize = 14.5.sp,
                                    color = Color(0xFF111827),
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message.time,
                                fontSize = 10.5.sp,
                                color = Color(0xFF78350F).copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Tiny Floating Reaction Pill underneath message
                if (message.reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(12.dp))
                            .clickable { onReactionClick() }
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FluentEmojiIcon(
                                emoji = message.reactions.first(),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.5.dp))
                            Text(
                                text = "${message.reactions.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        }
                    }
                }
            }
        }
    } else {
        // ==========================================
        // RECEIVED MESSAGE (Left Aligned, Soft Light Gray)
        // ==========================================
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            // Partner Avatar (shown beside message)
            if (message.showAvatar) {
                AsyncImage(
                    model = partnerAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFE5E7EB), CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }

            Column(horizontalAlignment = Alignment.Start) {
                Surface(
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                    color = Color(0xFFF3F4F6),
                    shadowElevation = if (isHighlighted) 4.dp else 0.dp,
                    modifier = Modifier
                        .scale(scaleAnim)
                        .widthIn(max = 280.dp)
                        .combinedClickable(
                            onClick = {
                                if (message.type == ChatMessageType.IMAGE && message.getAllImages().isNotEmpty()) {
                                    onImageClick(message.getAllImages().first())
                                }
                            },
                            onLongClick = onLongClick
                        )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)) {
                        when (message.type) {
                            ChatMessageType.IMAGE -> {
                                val img = message.imageUrl ?: message.getAllImages().firstOrNull() ?: ""
                                AsyncImage(
                                    model = img,
                                    contentDescription = "Shared photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 140.dp, max = 220.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onImageClick(img) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            ChatMessageType.AUDIO -> {
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF6C945))
                                            .clickable { onToggleAudioPlay() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPlayingAudio) {
                                            FunkyPauseIcon(color = Color(0xFF18181B), modifier = Modifier.size(13.dp))
                                        } else {
                                            FunkyPlayIcon(color = Color(0xFF18181B), modifier = Modifier.size(13.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Voice Capsule Waveform
                                    val waveHeights = listOf(0.3f, 0.5f, 0.8f, 1.0f, 0.6f, 0.9f, 0.7f, 0.4f, 0.8f, 0.5f, 0.3f)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        waveHeights.forEach { frac ->
                                            Box(
                                                modifier = Modifier
                                                    .width(2.5.dp)
                                                    .height((18 * frac).dp)
                                                    .clip(RoundedCornerShape(1.dp))
                                                    .background(Color(0xFF64748B))
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = message.audioDuration.ifBlank { "0:12" },
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            else -> {
                                Text(
                                    text = if (message.isTranslated && message.translation != null) message.translation else message.text,
                                    fontSize = 14.5.sp,
                                    color = Color(0xFF111827),
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }

                        Text(
                            text = message.time,
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }

                // Tiny Floating Reaction Pill underneath message
                if (message.reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(12.dp))
                            .clickable { onReactionClick() }
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FluentEmojiIcon(
                                emoji = message.reactions.first(),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.5.dp))
                            Text(
                                text = "${message.reactions.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        }
                    }
                }
            }
        }
    }
}
