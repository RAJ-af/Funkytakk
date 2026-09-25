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
import androidx.compose.runtime.Composable
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
    modifier: Modifier = Modifier
) {
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
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                    color = Color(0xFFF3F4F6),
                    border = if (isHighlighted) BorderStroke(2.dp, Color(0xFFFFC529)) else null,
                    shadowElevation = if (isHighlighted) 6.dp else 0.dp,
                    modifier = Modifier
                        .widthIn(max = 260.dp)
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
                            color = Color(0xFF111827)
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
