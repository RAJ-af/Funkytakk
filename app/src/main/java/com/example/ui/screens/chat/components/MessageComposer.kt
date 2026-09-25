package com.example.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.chat.model.ChatMessage

@Composable
fun MessageComposer(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAddClick: () -> Unit,
    onSmileyClick: () -> Unit,
    onMicClick: () -> Unit,
    replyingToMessage: ChatMessage? = null,
    onCancelReply: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Reply Preview Bar
        if (replyingToMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFEF3C7))
                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(30.dp)
                        .background(Color(0xFFD97706), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Replying to ${if (replyingToMessage.isMe) "Yourself" else "Partner"}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                    Text(
                        text = replyingToMessage.text.ifBlank { if (replyingToMessage.getAllImages().isNotEmpty()) "Photo" else "Voice Message" },
                        fontSize = 12.5.sp,
                        color = Color(0xFF78350F),
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onCancelReply() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel reply",
                        tint = Color(0xFF92400E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Circular Add (+) Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                AppSvgIcon(
                    icon = ChatIconAsset.Add,
                    tint = Color(0xFF111827),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Middle Input Pill
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF3F4F6))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text Input
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (messageText.isEmpty()) {
                        Text(
                            text = "Type a message...",
                            fontSize = 14.5.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }

                    BasicTextField(
                        value = messageText,
                        onValueChange = onMessageChange,
                        textStyle = TextStyle(
                            fontSize = 14.5.sp,
                            color = Color(0xFF111827)
                        ),
                        cursorBrush = SolidColor(Color(0xFFF59E0B)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (messageText.isNotBlank()) {
                                onSendMessage()
                            }
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Smiley Icon Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onSmileyClick() },
                    contentAlignment = Alignment.Center
                ) {
                    AppSvgIcon(
                        icon = ChatIconAsset.Smiley,
                        tint = Color(0xFF374151),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Microphone Icon Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onMicClick() },
                    contentAlignment = Alignment.Center
                ) {
                    AppSvgIcon(
                        icon = ChatIconAsset.Microphone,
                        tint = Color(0xFF374151),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right Circular Send Button (Brand Yellow)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFBBF24))
                    .clickable {
                        if (messageText.isNotBlank()) {
                            onSendMessage()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AppSvgIcon(
                    icon = ChatIconAsset.Send,
                    tint = Color(0xFF111827),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
