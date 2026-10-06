package com.example.ui.chat

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Timeline items that are not bubbles
// ---------------------------------------------------------------------------

/** "Hacker updated group settings · 3 changes". Collapse same-type events by the same actor within 10 minutes. */
@Composable
fun SystemEventLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = LocalChatColors.current.textSecondary,
        fontSize = 12f.chatSp(),
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 24.dp),
    )
}

/** Also use as the sticky date header while scrolling. */
@Composable
fun DateChip(text: String, modifier: Modifier = Modifier) {
    val c = LocalChatColors.current
    Box(modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = c.textSecondary,
            fontSize = 12f.chatSp(),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(c.surface.copy(alpha = 0.94f))
                .border(0.5.dp, c.hairline, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun UnreadDivider(count: Int, modifier: Modifier = Modifier) {
    val c = LocalChatColors.current
    Box(modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            text = if (count == 1) "1 unread message" else "$count unread messages",
            color = c.accentIn,
            fontSize = 12f.chatSp(),
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(c.accentIn.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 3.dp),
        )
    }
}

/** First item of every chat; also reuse for "Security code changed" with [onClick]. */
@Composable
fun SecurityNotice(text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = LocalChatColors.current
    Box(modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(c.surface.copy(alpha = 0.94f))
                .border(0.5.dp, c.hairline, RoundedCornerShape(12.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ChatIconLock(modifier = Modifier.size(14.dp).padding(top = 2.dp), tint = c.textSecondary)
            Spacer(Modifier.width(6.dp))
            Text(text, color = c.textSecondary, fontSize = 12f.chatSp(), lineHeight = 17f.chatSp(), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun TypingIndicator(label: String, modifier: Modifier = Modifier) {
    val c = LocalChatColors.current
    val reduce = LocalReduceMotion.current
    val t by rememberInfiniteTransition(label = "typing").animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "typingDots",
    )
    Row(modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0..2) {
            Box(
                Modifier
                    .padding(end = 3.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(c.textSecondary.copy(alpha = if (reduce || t.toInt() == i) 1f else 0.35f)),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(label, color = c.textSecondary, fontSize = 12f.chatSp())
    }
}

@Composable
fun PinnedMessageBar(preview: String, positionText: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalChatColors.current
    Row(
        modifier.fillMaxWidth().background(c.surface).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(32.dp).clip(RoundedCornerShape(2.dp)).background(c.accentIn))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Pinned message", color = c.accentIn, fontSize = 12f.chatSp(), fontWeight = FontWeight.Medium)
            Text(preview, color = c.textSecondary, fontSize = 13f.chatSp(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (positionText != null) Text(positionText, color = c.textSecondary, fontSize = 12f.chatSp())
    }
}

// ---------------------------------------------------------------------------
// Floating buttons
// ---------------------------------------------------------------------------

@Composable
fun ChatFab(icon: @Composable () -> Unit, contentDescription: String, badge: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalChatColors.current
    Box(modifier.size(ChatDimens.FabSize + 10.dp), contentAlignment = Alignment.BottomStart) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = c.surface,
            shadowElevation = 3.dp,
            modifier = Modifier.size(ChatDimens.FabSize),
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }
        if (badge > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    .clip(CircleShape)
                    .background(c.brand),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (badge > 99) "99+" else badge.toString(),
                    color = c.onBrand,
                    fontSize = 11f.chatSp(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp),
                )
            }
        }
    }
}

@Composable
fun ScrollToBottomFab(unread: Int, onClick: () -> Unit, modifier: Modifier = Modifier) =
    ChatFab(icon = {
        Canvas(Modifier.size(20.dp)) {
            val sw = 2.dp.toPx()
            val p = Path().apply {
                moveTo(size.width * 0.25f, size.height * 0.38f)
                lineTo(size.width * 0.50f, size.height * 0.65f)
                lineTo(size.width * 0.75f, size.height * 0.38f)
            }
            drawPath(p, Color(0xFF6B6B66), style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }, contentDescription = "Scroll to latest message", badge = unread, onClick = onClick, modifier = modifier)

@Composable
fun MentionFab(unreadMentions: Int, onClick: () -> Unit, modifier: Modifier = Modifier) =
    ChatFab(icon = {
        Text("@", color = Color(0xFF2F6FDE), fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }, contentDescription = "Jump to next mention", badge = unreadMentions, onClick = onClick, modifier = modifier)

// ---------------------------------------------------------------------------
// Long-press: reaction bar + action menu
// ---------------------------------------------------------------------------

data class MessageAction(
    val id: String,
    val label: String,
    val icon: (@Composable () -> Unit)? = null,
    val destructive: Boolean = false,
)

@Composable
fun ReactionQuickBar(
    emojis: List<String>,
    mine: String?,
    onPick: (String) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalChatColors.current
    Surface(shape = RoundedCornerShape(28.dp), color = c.surface, shadowElevation = 8.dp, modifier = modifier) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            emojis.forEach { e ->
                Box(
                    Modifier
                        .size(ChatDimens.MinTouch)
                        .clip(CircleShape)
                        .background(if (e == mine) c.accentIn.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { onPick(e) },
                    contentAlignment = Alignment.Center,
                ) { Text(e, fontSize = 26f.chatSp()) }
            }
            Box(
                Modifier.size(ChatDimens.MinTouch).clip(CircleShape).clickable(onClick = onMore),
                contentAlignment = Alignment.Center,
            ) { Text("+", color = c.textSecondary, fontSize = 24f.chatSp()) }
        }
    }
}

@Composable
fun MessageActionsMenu(actions: List<MessageAction>, onAction: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalChatColors.current
    Surface(shape = RoundedCornerShape(16.dp), color = c.surface, shadowElevation = 8.dp, modifier = modifier.width(220.dp)) {
        Column(Modifier.padding(vertical = 6.dp)) {
            actions.forEach { a ->
                val tint = if (a.destructive) c.danger else c.text
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(ChatDimens.MinTouch)
                        .clickable { onAction(a.id) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (a.icon != null) {
                        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                            a.icon.invoke()
                        }
                        Spacer(Modifier.width(14.dp))
                    }
                    Text(a.label, color = tint, fontSize = 15f.chatSp(), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    actions: List<MessageAction>,
    onAction: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalChatColors.current
    Surface(color = c.surface, shadowElevation = 2.dp, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { ChatIconClose(tint = c.text) }
            Text("$selectedCount selected", color = c.text, fontSize = 17f.chatSp(), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            actions.forEach { a ->
                IconButton(onClick = { onAction(a.id) }) {
                    if (a.icon != null) a.icon.invoke()
                    else Text(a.label, color = if (a.destructive) c.danger else c.text, fontSize = 14f.chatSp(), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Composer
// ---------------------------------------------------------------------------

@Composable
fun ReplyPreviewStrip(title: String, preview: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalChatColors.current
    Row(modifier.fillMaxWidth().padding(start = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(c.accentIn))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.accentIn, fontSize = 12f.chatSp(), fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(preview, color = c.textSecondary, fontSize = 13f.chatSp(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onClose) { ChatIconClose(modifier = Modifier.size(16.dp), tint = c.textSecondary) }
    }
}

@Composable
fun SendButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalChatColors.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = c.brand,
        shadowElevation = 2.dp,
        modifier = modifier.size(ChatDimens.MinTouch),
    ) {
        Box(contentAlignment = Alignment.Center) {
            ChatIconSend(tint = c.onBrand, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Tap to toggle recording OR Hold to record.
 */
@Composable
fun MicHoldButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    onStart: () -> Unit = {},
    onMove: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    onEnd: (cancelled: Boolean, locked: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val c = LocalChatColors.current
    val scale by animateFloatAsState(if (isRecording) 1.20f else 1f, tween(150), label = "micScale")

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isRecording) c.danger else c.brand,
        shadowElevation = 2.dp,
        modifier = modifier
            .size(ChatDimens.MinTouch)
            .scale(scale),
    ) {
        Box(contentAlignment = Alignment.Center) {
            ChatIconMic(tint = if (isRecording) Color.White else c.onBrand, modifier = Modifier.size(22.dp))
        }
    }
}

/** Shown in place of the text field while recording. */
@Composable
fun RecordingBar(
    elapsedMs: Long,
    amplitudes: List<Int>,
    slideProgress: Float = 0f,
    locked: Boolean = false,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalChatColors.current
    val pulse by rememberInfiniteTransition(label = "rec").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "recDot",
    )
    Row(
        modifier = modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Red pulsing dot
        Box(Modifier.size(10.dp).alpha(if (LocalReduceMotion.current) 1f else pulse).clip(CircleShape).background(c.danger))
        Spacer(Modifier.width(8.dp))
        Text(formatDuration(elapsedMs), color = c.text, fontSize = 15f.chatSp(), fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Waveform(amplitudes, 1f, c.danger.copy(alpha = 0.4f), c.danger, Modifier.weight(1f).height(24.dp))
        Spacer(Modifier.width(8.dp))

        // Trash / Cancel button
        Surface(
            onClick = onCancel,
            shape = CircleShape,
            color = c.danger.copy(alpha = 0.15f),
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                ChatIconTrash(modifier = Modifier.size(18.dp), tint = c.danger)
            }
        }

        Spacer(Modifier.width(6.dp))

        // Send Voice Note button
        Surface(
            onClick = onSend,
            shape = CircleShape,
            color = c.brand,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                ChatIconSend(modifier = Modifier.size(18.dp), tint = c.onBrand)
            }
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onEmojiToggle: () -> Unit,
    isEmojiOpen: Boolean = false,
    showAiButton: Boolean = false,
    onAi: () -> Unit = {},
    micButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    topSlot: (@Composable () -> Unit)? = null,
    recording: (@Composable () -> Unit)? = null,
    placeholder: String = "Message",
) {
    val c = LocalChatColors.current
    Column(modifier = modifier.fillMaxWidth().background(c.surface)) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.hairline))
        topSlot?.invoke()
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
            if (recording == null) {
                IconButton(onClick = onAttach, modifier = Modifier.size(44.dp)) {
                    ChatIconPhoto(tint = c.textSecondary, modifier = Modifier.size(24.dp))
                }
                Surface(
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = c.surfaceVariant,
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        IconButton(onClick = onEmojiToggle, modifier = Modifier.size(44.dp)) {
                            ChatIconEmoji(tint = if (isEmojiOpen) c.accentIn else c.textSecondary)
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = onTextChange,
                            textStyle = TextStyle(color = c.text, fontSize = 16f.chatSp(), lineHeight = 22f.chatSp()),
                            cursorBrush = SolidColor(c.accentIn),
                            maxLines = 6,
                            modifier = Modifier.weight(1f).padding(vertical = 11.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (text.isEmpty()) Text(placeholder, color = c.textSecondary, fontSize = 16f.chatSp())
                                    inner()
                                }
                            },
                        )
                        if (showAiButton && text.isNotBlank()) {
                            IconButton(onClick = onAi, modifier = Modifier.size(44.dp)) {
                                ChatIconSparkles(tint = c.accentIn)
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.weight(1f)) { recording() }
            }
            Spacer(Modifier.width(4.dp))
            Crossfade(targetState = (text.isNotBlank() && recording == null), animationSpec = tween(150), label = "micOrSend") { showSend ->
                if (showSend) SendButton(onSend) else micButton()
            }
        }
    }
}
