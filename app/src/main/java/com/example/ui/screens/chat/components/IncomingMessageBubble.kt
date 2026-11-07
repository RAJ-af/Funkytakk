package com.example.ui.screens.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.FluentEmojiRenderedText
import com.example.ui.screens.chat.model.ChatMessage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IncomingMessageBubble(
    message: ChatMessage,
    partnerAvatarUrl: String,
    isHighlighted: Boolean = false,
    onLongClick: () -> Unit,
    onReactionClick: () -> Unit = onLongClick,
    modifier: Modifier = Modifier,
    partnerUserId: String? = null
) {
    val userCosmeticsMap by com.example.store.CosmeticsManager.userCosmeticsMap.collectAsState()
    val senderBubbleConfig = remember(partnerUserId, userCosmeticsMap) {
        if (!partnerUserId.isNullOrBlank()) {
            val userRow = userCosmeticsMap[partnerUserId] ?: com.example.store.CosmeticsManager.getCachedCosmetics(partnerUserId)
            userRow?.bubble_theme?.config?.let {
                runCatching { com.example.store.CosmeticsConfigParser.parseBubbleTheme(it) }.getOrNull()
            }
        } else null
    }

    val bubbleShape = remember(senderBubbleConfig) {
        val rad = senderBubbleConfig?.radiusDp?.coerceIn(6f, 26f)?.dp ?: 18.dp
        val tailRad = if (senderBubbleConfig?.tailStyle?.equals("sharp", ignoreCase = true) == true) 2.dp else 4.dp
        RoundedCornerShape(topStart = rad, topEnd = rad, bottomStart = tailRad, bottomEnd = rad)
    }

    val bubbleColors = remember(senderBubbleConfig) {
        senderBubbleConfig?.bgColorsHex?.map { com.example.store.parseHexColor(it, Color(0xFFF3F4F6)) }
            ?: listOf(Color(0xFFF3F4F6))
    }

    val bubbleBrush = remember(bubbleColors) {
        if (bubbleColors.size > 1) {
            androidx.compose.ui.graphics.Brush.linearGradient(bubbleColors)
        } else androidx.compose.ui.graphics.SolidColor(bubbleColors.firstOrNull() ?: Color(0xFFF3F4F6))
    }

    val bubbleTextColor = remember(senderBubbleConfig) {
        senderBubbleConfig?.textColorHex?.let { com.example.store.parseHexColor(it, Color(0xFF111827)) }
            ?: Color(0xFF111827)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
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

        Row(verticalAlignment = Alignment.Bottom) {
            Column {
                Surface(
                    shape = bubbleShape,
                    color = Color.Transparent,
                    border = if (isHighlighted) BorderStroke(2.dp, Color(0xFFFFC529))
                             else senderBubbleConfig?.border?.let { BorderStroke(it.widthDp.dp, com.example.store.parseHexColor(it.colorHex, Color.Transparent)) },
                    shadowElevation = if (isHighlighted) 6.dp else 0.dp,
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .background(brush = bubbleBrush, shape = bubbleShape)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = onLongClick
                        )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        // Reply banner if replying to a message
                        if (!message.replyToText.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE5E7EB))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(26.dp)
                                        .background(Color(0xFF9CA3AF), RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = message.replyToSender ?: "Replied message",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF374151)
                                    )
                                    Text(
                                        text = message.replyToText,
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B7280),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        val displayText = if (message.isTranslated && message.translation != null) message.translation else message.text

                        FluentEmojiRenderedText(
                            text = displayText,
                            style = TextStyle(
                                fontSize = 14.5.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = bubbleTextColor
                        )
                    }
                }

                if (message.reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    ReactionBadge(
                        reactions = message.reactions,
                        onClick = onReactionClick
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
