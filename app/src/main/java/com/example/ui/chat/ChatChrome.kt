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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
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
                .background(c.surface.copy(alpha = 0.92f))
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
                .background(c.surface.copy(alpha = 0.92f))
                .border(0.5.dp, c.hairline, RoundedCornerShape(12.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(14.dp).padding(top = 2.dp))
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
fun ChatFab(icon: ImageVector, contentDescription: String, badge: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
                Icon(icon, contentDescription, tint = c.textSecondary, modifier = Modifier.size(22.dp))
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
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 5.dp),
                )
            }
        }
    }
}

@Composable
fun ScrollToBottomFab(unread: Int, onClick: () -> Unit, modifier: Modifier = Modifier) =
    ChatFab(Icons.Rounded.KeyboardArrowDown, "Scroll to latest message", unread, onClick, modifier)

@Composable
fun MentionFab(unreadMentions: Int, onClick: () -> Unit, modifier: Modifier = Modifier) =
    ChatFab(Icons.Rounded.AlternateEmail, "Jump to next mention", unreadMentions, onClick, modifier)

// ---------------------------------------------------------------------------
// Swipe right to reply (with arrow hint and a haptic tick at the threshold)
// ---------------------------------------------------------------------------

fun Modifier.swipeToReply(enabled: Boolean = true, onReply: () -> Unit): Modifier = composed {
    if (!enabled) return@composed Modifier
    val density = LocalDensity.current
    val c = LocalChatColors.current
    val threshold = with(density) { ChatDimens.SwipeReplyThreshold.toPx() }
    val maxDrag = with(density) { ChatDimens.SwipeReplyMax.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var armed by remember { mutableStateOf(false) }
    val reply by rememberUpdatedState(onReply)
    this
        .drawBehind {
            val p = (offsetX.value / threshold).coerceIn(0f, 1f)
            if (p > 0.05f) {
                val cx = 22.dp.toPx()
                val cy = size.height / 2f
                drawCircle(c.surface.copy(alpha = p), radius = 14.dp.toPx() * p, center = Offset(cx, cy))
                val a = 6.dp.toPx() * p
                val arrow = Path().apply {
                    moveTo(cx + a, cy - a * 0.9f)
                    lineTo(cx - a, cy)
                    lineTo(cx + a, cy + a * 0.9f)
                }
                drawPath(arrow, c.textSecondary.copy(alpha = p), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (armed) reply()
                    armed = false
                    scope.launch { offsetX.animateTo(0f, spring()) }
                },
                onDragCancel = {
                    armed = false
                    scope.launch { offsetX.animateTo(0f, spring()) }
                },
            ) { change, dragAmount ->
                change.consume()
                val next = (offsetX.value + dragAmount * 0.6f).coerceIn(0f, maxDrag)
                scope.launch { offsetX.snapTo(next) }
                val nowArmed = next >= threshold
                if (nowArmed && !armed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                armed = nowArmed
            }
        }
}

// ---------------------------------------------------------------------------
// Long-press: reaction bar + action menu (show them in a Popup/Dialog over a dimmed background)
// ---------------------------------------------------------------------------

data class MessageAction(
    val id: String, // "reply", "copy", "translate", "listen", "forward", "select", "pin", "delete" ...
    val label: String,
    val icon: ImageVector? = null,
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
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        Icon(a.icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(14.dp))
                    }
                    Text(a.label, color = tint, fontSize = 15f.chatSp())
                }
            }
        }
    }
}

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    actions: List<MessageAction>, // icons required here
    onAction: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalChatColors.current
    Surface(color = c.surface, shadowElevation = 2.dp, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Cancel selection", tint = c.text) }
            Text("$selectedCount selected", color = c.text, fontSize = 17f.chatSp(), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            actions.forEach { a ->
                IconButton(onClick = { onAction(a.id) }) {
                    if (a.icon != null) Icon(a.icon, a.label, tint = if (a.destructive) c.danger else c.text)
                    else Text(a.label, color = c.text, fontSize = 14f.chatSp())
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
            Text(title, color = c.accentIn, fontSize = 12f.chatSp(), fontWeight = FontWeight.Medium, maxLines = 1)
            Text(preview, color = c.textSecondary, fontSize = 13f.chatSp(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Cancel reply", tint = c.textSecondary) }
    }
}

@Composable
fun SendButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalChatColors.current
    Box(
        modifier.size(ChatDimens.MinTouch).clip(CircleShape).background(c.brand).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.AutoMirrored.Rounded.Send, "Send", tint = c.onBrand, modifier = Modifier.size(22.dp)) }
}

/**
 * Hold to record, slide left to cancel, slide up to lock. Stateless: the caller owns the recorder.
 * [onMove] receives pixel offsets from the touch-down point (negative dx = left, negative dy = up).
 * A press shorter than ~400ms should be treated as a tap by the caller ("Hold to record").
 */
@Composable
fun MicHoldButton(
    isRecording: Boolean,
    onStart: () -> Unit,
    onMove: (dx: Float, dy: Float) -> Unit,
    onEnd: (cancelled: Boolean, locked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalChatColors.current
    val start by rememberUpdatedState(onStart)
    val move by rememberUpdatedState(onMove)
    val end by rememberUpdatedState(onEnd)
    val scale by animateFloatAsState(if (isRecording) 1.35f else 1f, tween(150), label = "micScale")
    Box(
        modifier
            .size(ChatDimens.MinTouch)
            .scale(scale)
            .clip(CircleShape)
            .background(c.brand)
            .pointerInput(Unit) {
                val cancelPx = 96.dp.toPx()
                val lockPx = 80.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    start()
                    var cancelled = false
                    var locked = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val dx = change.position.x - down.position.x
                        val dy = change.position.y - down.position.y
                        move(dx, dy)
                        cancelled = dx < -cancelPx
                        if (dy < -lockPx) locked = true
                        change.consume()
                        if (!change.pressed) break
                    }
                    end(cancelled && !locked, locked)
                }
            },
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Mic, "Hold to record voice message", tint = c.onBrand, modifier = Modifier.size(22.dp)) }
}

/** Shown in place of the text field while recording. */
@Composable
fun RecordingBar(
    elapsedMs: Long,
    amplitudes: List<Int>, // last ~48 live samples
    slideProgress: Float, // 0f..1f toward the cancel zone
    locked: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalChatColors.current
    val pulse by rememberInfiniteTransition(label = "rec").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "recDot",
    )
    Row(
        modifier.fillMaxWidth().height(ChatDimens.MinTouch).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).alpha(if (LocalReduceMotion.current) 1f else pulse).clip(CircleShape).background(c.danger))
        Spacer(Modifier.width(8.dp))
        Text(formatDuration(elapsedMs), color = c.text, fontSize = 16f.chatSp(), fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(10.dp))
        Waveform(amplitudes, 1f, c.danger, c.danger, Modifier.weight(1f).height(24.dp))
        Spacer(Modifier.width(10.dp))
        if (locked) {
            Text(
                "Cancel",
                color = c.danger,
                fontSize = 14f.chatSp(),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onCancel).padding(8.dp),
            )
        } else {
            Text("‹ Slide to cancel", color = c.textSecondary, fontSize = 13f.chatSp(), modifier = Modifier.alpha((1f - slideProgress).coerceIn(0f, 1f)))
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onEmoji: () -> Unit,
    showAiButton: Boolean,
    onAi: () -> Unit,
    micButton: @Composable () -> Unit, // MicHoldButton(...)
    modifier: Modifier = Modifier,
    topSlot: (@Composable () -> Unit)? = null, // ReplyPreviewStrip / edit strip
    recording: (@Composable () -> Unit)? = null, // RecordingBar(...) replaces the field while recording
    forceSend: Boolean = false, // true while a locked recording waits for "send"
    placeholder: String = "Message",
) {
    val c = LocalChatColors.current
    Column(modifier.fillMaxWidth().background(c.surface)) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.hairline))
        topSlot?.invoke()
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
            if (recording == null) {
                IconButton(onClick = onAttach) { Icon(Icons.Rounded.AttachFile, "Attach", tint = c.textSecondary) }
                Surface(
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = c.surfaceVariant,
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        IconButton(onClick = onEmoji, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Rounded.EmojiEmotions, "Emoji", tint = c.textSecondary)
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
                                Icon(Icons.Rounded.AutoAwesome, "Improve text", tint = c.accentIn)
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.weight(1f)) { recording() }
            }
            Spacer(Modifier.width(4.dp))
            Crossfade(targetState = (text.isNotBlank() && recording == null) || forceSend, animationSpec = tween(150), label = "micOrSend") { showSend ->
                if (showSend) SendButton(onSend) else micButton()
            }
        }
    }
}
