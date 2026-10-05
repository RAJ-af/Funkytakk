package com.example.ui.screens.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FunkyMicIcon

@Composable
fun ChatInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onAiSparkleClick: (() -> Unit)? = null,
    onMicClick: () -> Unit,
    replySender: String? = null,
    replyText: String? = null,
    onCancelReply: (() -> Unit)? = null,
    isRecordingVoice: Boolean = false,
    recordingSeconds: Int = 0,
    onCancelRecording: (() -> Unit)? = null,
    onSendRecording: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) DarkChatColors else LightChatColors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // 1. Reply Preview Strip above field
        if (!replySender.isNullOrBlank() && !replyText.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = colors.replyBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .background(colors.accent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = replySender,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                        Text(
                            text = replyText,
                            fontSize = 12.5.sp,
                            color = colors.text.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                    if (onCancelReply != null) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onCancelReply() },
                            contentAlignment = Alignment.Center
                        ) {
                            AppSvgIcon(
                                icon = ChatIconAsset.Close,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Recording Bar or Normal Composer Bar
        if (isRecordingVoice) {
            Surface(
                color = Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recording: ${String.format("%d:%02d", recordingSeconds / 60, recordingSeconds % 60)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF92400E)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onCancelRecording != null) {
                            Text(
                                text = "Cancel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFDC2626),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onCancelRecording() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (onSendRecording != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD97706))
                                    .clickable { onSendRecording() },
                                contentAlignment = Alignment.Center
                            ) {
                                AppSvgIcon(
                                    icon = ChatIconAsset.Send,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach Button (+)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { onAttachClick() },
                    contentAlignment = Alignment.Center
                ) {
                    AppSvgIcon(
                        icon = ChatIconAsset.Attachment,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Rounded Input Field
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            1.dp,
                            colors.secondaryText.copy(alpha = 0.20f),
                            RoundedCornerShape(24.dp)
                        ),
                    color = colors.incomingBubble,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Emoji Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { onEmojiClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            AppSvgIcon(
                                icon = ChatIconAsset.Smiley,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Text Input
                        BasicTextField(
                            value = messageText,
                            onValueChange = onMessageChange,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 24.dp, max = 120.dp),
                            textStyle = TextStyle(
                                fontSize = 15.5.sp,
                                color = colors.text,
                                lineHeight = 21.sp
                            ),
                            cursorBrush = SolidColor(colors.accent),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (messageText.isEmpty()) {
                                        Text(
                                            text = "Message...",
                                            fontSize = 15.5.sp,
                                            color = colors.secondaryText
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // AI Sparkles Button (shown when text non-empty and AI available)
                        if (messageText.isNotBlank() && onAiSparkleClick != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { onAiSparkleClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                AppSvgIcon(
                                    icon = ChatIconAsset.Sparkles,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Right Action Button: Mic <-> Send Morph in Yellow (`#F6C945`) Circular Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF6C945))
                        .clickable {
                            if (messageText.isNotBlank()) {
                                onSend()
                            } else {
                                onMicClick()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = messageText.isNotBlank(),
                        transitionSpec = {
                            fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                        },
                        label = "micSendMorph"
                    ) { isSendingState ->
                        if (isSendingState) {
                            AppSvgIcon(
                                icon = ChatIconAsset.Send,
                                tint = Color(0xFF1B1B1B),
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            FunkyMicIcon(
                                color = Color(0xFF1B1B1B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
