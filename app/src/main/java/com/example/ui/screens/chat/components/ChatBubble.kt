package com.example.ui.screens.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.FunkyDoubleCheckIcon
import com.example.ui.components.FunkySingleCheckIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    messageId: String,
    senderId: String,
    senderName: String?,
    text: String,
    timeText: String,
    isOutgoing: Boolean,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    isSingleInGroup: Boolean,
    showSenderName: Boolean = false,
    showAvatar: Boolean = false,
    avatarUrl: String? = null,
    replySenderName: String? = null,
    replyText: String? = null,
    replyThumbnailUrl: String? = null,
    statusCheck: Int = 2, // 0 = pending clock, 1 = single check, 2 = double check
    translationText: String? = null,
    isDisappearing: Boolean = false,
    reactions: List<ChatReactionItem> = emptyList(),
    onLongClick: () -> Unit,
    onMentionClick: ((String) -> Unit)? = null,
    onReplyQuoteClick: (() -> Unit)? = null,
    onToggleReaction: ((String) -> Unit)? = null,
    onShowReactionDetails: (() -> Unit)? = null,
    isFlashed: Boolean = false,
    mediaContent: (@Composable () -> Unit)? = null,
    voiceContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) DarkChatColors else LightChatColors

    val senderColor = remember(senderId, isDark) {
        ChatTheme.getSenderColor(senderId, isDark)
    }

    val bubbleShape = remember(isOutgoing, isFirstInGroup, isLastInGroup, isSingleInGroup) {
        ChatTheme.getBubbleShape(
            isOutgoing = isOutgoing,
            isFirstInGroup = isFirstInGroup,
            isLastInGroup = isLastInGroup,
            isSingle = isSingleInGroup
        )
    }

    val bgFlashAnim = remember { Animatable(0f) }
    LaunchedEffect(isFlashed) {
        if (isFlashed) {
            bgFlashAnim.snapTo(1f)
            bgFlashAnim.animateTo(0f, animationSpec = tween(1200))
        }
    }

    val containerBg = if (isOutgoing) colors.outgoingBubble else colors.incomingBubble
    val flashColor = colors.accent.copy(alpha = 0.15f * bgFlashAnim.value)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = if (isFirstInGroup || isSingleInGroup) 4.dp else 1.dp,
                bottom = if (isLastInGroup || isSingleInGroup) 4.dp else 1.dp,
                start = 8.dp,
                end = 8.dp
            ),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Incoming Avatar logic: 28dp avatar ONLY beside LAST message of incoming group, 28dp spacer for others
        if (!isOutgoing) {
            if (showAvatar && isLastInGroup) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(senderColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = senderName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = (senderName ?: "U").take(1).uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = senderColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Spacer(modifier = Modifier.width(34.dp))
            }
        }

        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Surface(
                shape = bubbleShape,
                color = containerBg,
                shadowElevation = 0.dp,
                border = if (bgFlashAnim.value > 0.05f) BorderStroke(1.5.dp, colors.accent) else null,
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(flashColor)
                    .combinedClickable(
                        onClick = { /* Tap */ },
                        onLongClick = onLongClick
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    // 1. Sender name ONLY on FIRST incoming message of group
                    if (!isOutgoing && showSenderName && !senderName.isNullOrBlank()) {
                        Text(
                            text = senderName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = senderColor,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // 2. Reply Quote preview if replying to a message
                    if (!replySenderName.isNullOrBlank()) {
                        ReplyQuote(
                            senderName = replySenderName,
                            text = replyText.orEmpty(),
                            mediaThumbnailUrl = replyThumbnailUrl,
                            onClick = onReplyQuoteClick,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // 3. Custom Media or Voice Content
                    if (mediaContent != null) {
                        mediaContent()
                    } else if (voiceContent != null) {
                        voiceContent()
                    } else {
                        // 4. Message Text with Inline @Mentions
                        val annotatedText = remember(text, colors.accent, colors.text) {
                            buildAnnotatedString {
                                val parts = text.split(" ")
                                parts.forEachIndexed { index, part ->
                                    if (part.startsWith("@") && part.length > 1) {
                                        pushStringAnnotation(tag = "MENTION", annotation = part)
                                        pushStyle(SpanStyle(color = colors.accent, fontWeight = FontWeight.Medium))
                                        append(part)
                                        pop()
                                        pop()
                                    } else {
                                        pushStyle(SpanStyle(color = colors.text))
                                        append(part)
                                        pop()
                                    }
                                    if (index < parts.size - 1) append(" ")
                                }
                            }
                        }

                        ClickableText(
                            text = annotatedText,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 22.sp
                            ),
                            onClick = { offset ->
                                annotatedText.getStringAnnotations("MENTION", offset, offset)
                                    .firstOrNull()?.let { annotation ->
                                        onMentionClick?.invoke(annotation.item)
                                    }
                            }
                        )
                    }

                    // 5. Translation text if available
                    if (!translationText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(
                            color = colors.secondaryText.copy(alpha = 0.20f),
                            thickness = 0.5.dp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌐 ", fontSize = 11.sp)
                            Text(
                                text = translationText,
                                fontSize = 13.sp,
                                color = colors.secondaryText,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // 6. Meta: Time + Ticks inline at bottom-right
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (isDisappearing) {
                            AppSvgIcon(
                                icon = ChatIconAsset.Clock,
                                tint = colors.secondaryText,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }

                        Text(
                            text = timeText,
                            fontSize = 11.sp,
                            color = colors.secondaryText
                        )

                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(3.dp))
                            when (statusCheck) {
                                0 -> AppSvgIcon(
                                    icon = ChatIconAsset.Clock,
                                    tint = colors.secondaryText,
                                    modifier = Modifier.size(10.dp)
                                ) // Pending clock
                                1 -> FunkySingleCheckIcon(color = colors.secondaryText, modifier = Modifier.size(12.dp))
                                else -> FunkyDoubleCheckIcon(color = colors.accent, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // 7. Reaction Chips Row under bubble
            if (reactions.isNotEmpty() && onToggleReaction != null && onShowReactionDetails != null) {
                ReactionRow(
                    reactions = reactions,
                    onToggleReaction = onToggleReaction,
                    onShowReactionDetails = onShowReactionDetails
                )
            }
        }
    }
}
