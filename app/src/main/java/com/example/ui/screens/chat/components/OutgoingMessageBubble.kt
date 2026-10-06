package com.example.ui.screens.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FluentEmojiRenderedText
import com.example.ui.components.FunkyDoubleCheckIcon
import com.example.ui.screens.chat.model.ChatMessage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutgoingMessageBubble(
    message: ChatMessage,
    isHighlighted: Boolean = false,
    onLongClick: () -> Unit,
    onReactionClick: () -> Unit = onLongClick,
    modifier: Modifier = Modifier
) {
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
                color = Color(0xFFFCD34D), // Soft warm amber yellow
                border = if (isHighlighted) BorderStroke(2.dp, Color.White) else null,
                shadowElevation = if (isHighlighted) 6.dp else 0.dp,
                modifier = Modifier
                    .widthIn(max = 290.dp)
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
                                .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(26.dp)
                                    .background(Color(0xFFB45309), RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = message.replyToSender ?: "Replied message",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF78350F)
                                )
                                Text(
                                    text = message.replyToText,
                                    fontSize = 12.sp,
                                    color = Color(0xFF451A03),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Text with Fluent Emojis
                    FluentEmojiRenderedText(
                        text = message.text,
                        style = TextStyle(
                            fontSize = 14.5.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color(0xFF111827)
                    )

                    Spacer(modifier = Modifier.height(3.dp))

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
                    }
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
    }
}
