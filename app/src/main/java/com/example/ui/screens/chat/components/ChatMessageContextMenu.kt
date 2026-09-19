package com.example.ui.screens.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.screens.chat.model.ChatMessage

@Composable
fun ChatMessageContextMenuOverlay(
    targetMessage: ChatMessage,
    onDismiss: () -> Unit,
    onSelectEmoji: (String) -> Unit,
    onTranslate: () -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onReactMore: () -> Unit,
    onMoreActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quickEmojis = listOf("❤️", "😂", "👍", "😮", "😢")

    // Full screen dimmed backdrop
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume click */ }
        ) {
            // 1. Highlighted Bubble Preview in Center
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (targetMessage.isMe) Color(0xFFFCD34D) else Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = targetMessage.text.ifEmpty { "Media content" },
                    fontSize = 15.sp,
                    color = Color.Black,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // 2. Reaction Emoji Pill
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickEmojis.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onSelectEmoji(emoji)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            FluentEmojiIcon(
                                emoji = emoji,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Plus Button inside circle
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                            .clickable {
                                onReactMore()
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        FunkyPlusIcon(
                            color = Color(0xFF4B5563),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // 3. Context Menu Actions Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Action 1: Translate (Yellow highlighted row matching Image 2)
                    ContextMenuItemRow(
                        icon = { FunkyTranslateIcon(color = Color.Black, modifier = Modifier.size(18.dp)) },
                        label = "Translate",
                        isHighlighted = true,
                        onClick = {
                            onTranslate()
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Action 2: Reply
                    ContextMenuItemRow(
                        icon = { FunkyReplyIcon(color = Color.Black, modifier = Modifier.size(18.dp)) },
                        label = "Reply",
                        onClick = {
                            onReply()
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Action 3: Copy
                    ContextMenuItemRow(
                        icon = { FunkyCopyIcon(color = Color.Black, modifier = Modifier.size(18.dp)) },
                        label = "Copy",
                        onClick = {
                            onCopy()
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Action 4: React
                    ContextMenuItemRow(
                        icon = { FunkySmileyIcon(color = Color.Black, modifier = Modifier.size(18.dp)) },
                        label = "React",
                        onClick = {
                            onReactMore()
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Action 5: More (with right chevron)
                    ContextMenuItemRow(
                        icon = { FunkyMoreIcon(color = Color.Black, modifier = Modifier.size(18.dp)) },
                        label = "More",
                        trailing = {
                            FunkyChevronRightIcon(color = Color.Black, modifier = Modifier.size(14.dp))
                        },
                        onClick = {
                            onMoreActions()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextMenuItemRow(
    icon: @Composable () -> Unit,
    label: String,
    isHighlighted: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isHighlighted) Color(0xFFFEF3C7) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 14.5.sp,
            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
            color = Color(0xFF111827),
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}
