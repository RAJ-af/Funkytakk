package com.example.ui.screens.chat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.ui.components.SoftCloseIcon
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
    val isTyping = messageText.trim().isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth()
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
                    SoftCloseIcon(
                        size = 14.dp,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }

        // Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small Circular Add (+) Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF4F4F0))
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                AppSvgIcon(
                    icon = ChatIconAsset.Add,
                    tint = Color(0xFF111827),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Large Rounded Message Field
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF4F4F0))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text Input Field
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (messageText.isEmpty()) {
                        Text(
                            text = "Message…",
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
                        cursorBrush = SolidColor(Color(0xFFF6C945)),
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

                // Emoji Button inside input field
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { onSmileyClick() },
                    contentAlignment = Alignment.Center
                ) {
                    AppSvgIcon(
                        icon = ChatIconAsset.Smiley,
                        tint = Color(0xFF4B5563),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Circular Button (FunkyTalk Yellow #F6C945): Mic when empty, Send arrow when typing
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF6C945))
                    .clickable {
                        if (isTyping) {
                            onSendMessage()
                        } else {
                            onMicClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isTyping,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "mic_to_send"
                ) { typing ->
                    if (typing) {
                        AppSvgIcon(
                            icon = ChatIconAsset.Send,
                            tint = Color(0xFF111827),
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        AppSvgIcon(
                            icon = ChatIconAsset.Microphone,
                            tint = Color(0xFF111827),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
