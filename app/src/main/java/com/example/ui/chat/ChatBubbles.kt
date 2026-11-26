package com.example.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

// ---------------------------------------------------------------------------
// Basics
// ---------------------------------------------------------------------------

enum class GroupPosition { Single, First, Middle, Last }
enum class MessageStatus { Sending, Sent, Delivered, Read, Failed }

fun bubbleShape(outgoing: Boolean, position: GroupPosition): RoundedCornerShape {
    val big = ChatDimens.BubbleBig
    val small = ChatDimens.BubbleSmall
    return if (outgoing) {
        when (position) {
            GroupPosition.Single -> RoundedCornerShape(big, big, big, big)
            GroupPosition.First -> RoundedCornerShape(big, big, small, big)
            GroupPosition.Middle -> RoundedCornerShape(big, small, small, big)
            GroupPosition.Last -> RoundedCornerShape(big, small, big, big)
        }
    } else {
        when (position) {
            GroupPosition.Single -> RoundedCornerShape(big, big, big, big)
            GroupPosition.First -> RoundedCornerShape(big, big, big, small)
            GroupPosition.Middle -> RoundedCornerShape(small, big, big, small)
            GroupPosition.Last -> RoundedCornerShape(small, big, big, big)
        }
    }
}

/** Screen-reader sentence for a message, e.g. "Hacker, 12:51, hello, read". */
fun messageDescription(sender: String?, time: String, body: String, status: MessageStatus?): String = buildString {
    if (sender != null) append(sender).append(", ")
    append(time).append(", ").append(body)
    when (status) {
        MessageStatus.Sending -> append(", sending")
        MessageStatus.Sent -> append(", sent")
        MessageStatus.Delivered -> append(", delivered")
        MessageStatus.Read -> append(", read")
        MessageStatus.Failed -> append(", failed to send")
        null -> Unit
    }
}

@Composable
fun AnimatedBubbleEntrance(
    animateEntrance: Boolean,
    outgoing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    if (!animateEntrance || reduceMotion) {
        Box(modifier = modifier) { content() }
        return
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing))
    }
    val density = LocalDensity.current
    val slideXPx = with(density) { (if (outgoing) 10.dp else (-10).dp).toPx() }
    val slideYPx = with(density) { 12.dp.toPx() }
    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.value
            alpha = p
            translationX = (1f - p) * slideXPx
            translationY = (1f - p) * slideYPx
        }
    ) {
        content()
    }
}

@Composable
fun MessageBubble(
    outgoing: Boolean,
    position: GroupPosition,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    selected: Boolean = false,
    transparent: Boolean = false,
    contentDescription: String? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = ChatDimens.BubblePadH,
        vertical = ChatDimens.BubblePadV,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalChatColors.current
    val accent = if (outgoing) c.accentOut else c.accentIn
    val overlayTarget = when {
        selected -> accent.copy(alpha = 0.18f)
        highlighted -> accent.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val overlay by animateColorAsState(overlayTarget, tween(600), label = "bubbleOverlay")
    val maxW = minOf(ChatDimens.BubbleMaxWidth, (LocalConfiguration.current.screenWidthDp * 0.82f).dp)
    Surface(
        shape = bubbleShape(outgoing, position),
        color = when {
            transparent -> Color.Transparent
            outgoing -> c.outgoingBubble
            else -> c.incomingBubble
        },
        shadowElevation = if (transparent) 0.dp else 0.5.dp,
        modifier = modifier
            .widthIn(max = maxW)
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) this.contentDescription = contentDescription
            },
    ) {
        Box(Modifier.background(overlay)) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun SelectionCheckBadge(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val c = LocalChatColors.current
    val fillColor by animateColorAsState(
        targetValue = if (selected) c.brand else Color.Transparent,
        animationSpec = tween(160),
        label = "selectionBadgeFill"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) c.brand else c.textSecondary.copy(alpha = 0.55f),
        animationSpec = tween(160),
        label = "selectionBadgeBorder"
    )
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(fillColor)
            .border(width = 1.8.dp, color = borderColor, shape = CircleShape)
            .semantics { contentDescription = if (selected) "Selected" else "Not selected" },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Canvas(modifier = Modifier.size(11.dp)) {
                val p = Path().apply {
                    moveTo(size.width * 0.14f, size.height * 0.52f)
                    lineTo(size.width * 0.42f, size.height * 0.80f)
                    lineTo(size.width * 0.88f, size.height * 0.22f)
                }
                drawPath(
                    path = p,
                    color = c.onBrand,
                    style = Stroke(
                        width = 2.1.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
fun SenderName(name: String, userId: String, modifier: Modifier = Modifier) {
    val c = LocalChatColors.current
    com.example.store.DecoratedUserName(
        userId = userId,
        name = name,
        defaultColor = c.senderColor(userId),
        fontSize = 13f.chatSp(),
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(bottom = 2.dp),
    )
}

// ---------------------------------------------------------------------------
// Text with inline time + ticks, mentions, search highlight, tappable words
// ---------------------------------------------------------------------------

fun buildMessageText(
    raw: String,
    mentionNames: List<String>,
    accent: Color,
    searchQuery: String = "",
    highlight: Color = Color.Transparent,
): AnnotatedString = buildAnnotatedString {
    append(raw)
    val tokens = mentionNames.map { "@$it" }
    for (t in tokens) {
        var from = 0
        while (true) {
            val i = raw.indexOf(t, from)
            if (i < 0) break
            addStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold), i, i + t.length)
            from = i + t.length
        }
    }
    if (searchQuery.isNotBlank()) {
        var from = 0
        while (true) {
            val i = raw.indexOf(searchQuery, from, ignoreCase = true)
            if (i < 0) break
            addStyle(SpanStyle(background = highlight), i, i + searchQuery.length)
            from = i + searchQuery.length
        }
    }
}

private fun Char.isWordChar(): Boolean {
    if (isLetterOrDigit() || this == '\'') return true
    val t = Character.getType(this)
    return t == Character.NON_SPACING_MARK.toInt() || t == Character.COMBINING_SPACING_MARK.toInt()
}

/** The word under [offset], or null (mentions are skipped). */
fun wordAt(s: String, offset: Int): String? {
    if (s.isEmpty()) return null
    var i = offset.coerceIn(0, s.length - 1)
    if (!s[i].isWordChar() && i > 0 && s[i - 1].isWordChar()) i -= 1
    if (!s[i].isWordChar()) return null
    var a = i
    var b = i
    while (a > 0 && s[a - 1].isWordChar()) a--
    while (b < s.length - 1 && s[b + 1].isWordChar()) b++
    if (a > 0 && s[a - 1] == '@') return null
    return s.substring(a, b + 1).trim('\'').ifEmpty { null }
}

@Composable
fun MessageText(
    text: AnnotatedString,
    time: String,
    outgoing: Boolean,
    status: MessageStatus?,
    modifier: Modifier = Modifier,
    edited: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onWordClick: ((String) -> Unit)? = null,
) {
    val c = LocalChatColors.current
    val metaSp = (if (status != null) 66f else 40f) + (if (edited) 32f else 0f)
    val metaWidth = metaSp.chatSp()
    val withSlot = remember(text, metaWidth) {
        buildAnnotatedString {
            append(text)
            append(" ")
            appendInlineContent("meta", "[meta]")
        }
    }
    val inline = remember(metaWidth) {
        mapOf("meta" to InlineTextContent(Placeholder(metaWidth, 1.sp, PlaceholderVerticalAlign.TextBottom)) {})
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val plain = text.text
    Box(modifier) {
        Text(
            text = withSlot,
            inlineContent = inline,
            color = c.text,
            fontSize = 16f.chatSp(),
            lineHeight = 22f.chatSp(),
            onTextLayout = { layout = it },
            modifier = if (onWordClick != null || onLongPress != null) {
                Modifier.pointerInput(plain) {
                    detectTapGestures(
                        onLongPress = { onLongPress?.invoke() },
                        onTap = { pos ->
                            val l = layout ?: return@detectTapGestures
                            wordAt(plain, l.getOffsetForPosition(pos))?.let { onWordClick?.invoke(it) }
                        }
                    )
                }
            } else {
                Modifier
            },
        )
        MessageMeta(time, outgoing, status, Modifier.align(Alignment.BottomEnd), edited = edited)
    }
}

@Composable
fun MessageMeta(
    time: String,
    outgoing: Boolean,
    status: MessageStatus?,
    modifier: Modifier = Modifier,
    edited: Boolean = false,
    onMedia: Boolean = false,
) {
    val c = LocalChatColors.current
    val reduceMotion = LocalReduceMotion.current
    val base = when {
        onMedia -> Color.White
        outgoing -> c.textSecondaryOut
        else -> c.textSecondary
    }
    val tick = if (onMedia) Color.White else if (status == MessageStatus.Read) c.tickRead else base

    var sawSending by remember { mutableStateOf(status == MessageStatus.Sending) }
    val tickScale = remember { Animatable(1f) }
    LaunchedEffect(status, reduceMotion) {
        if (status == MessageStatus.Sending) {
            sawSending = true
        } else if (sawSending && (status == MessageStatus.Sent || status == MessageStatus.Delivered || status == MessageStatus.Read)) {
            sawSending = false
            if (!reduceMotion) {
                tickScale.snapTo(0.72f)
                tickScale.animateTo(1.24f, tween(95))
                tickScale.animateTo(1.0f, tween(115))
            } else {
                tickScale.snapTo(1f)
            }
        } else if (status == MessageStatus.Failed) {
            sawSending = false
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (edited) Text("edited", color = base, fontSize = 11f.chatSp())
        Text(time, color = if (status == MessageStatus.Failed && !onMedia) c.danger else base, fontSize = 11f.chatSp())
        when (status) {
            MessageStatus.Sending -> ChatIconClock(
                modifier = Modifier
                    .size(12.dp)
                    .semantics { contentDescription = "Sending" },
                tint = base
            )
            MessageStatus.Sent -> TickMark(
                double = false,
                color = tick,
                modifier = Modifier.scale(tickScale.value)
            )
            MessageStatus.Delivered, MessageStatus.Read -> TickMark(
                double = true,
                color = tick,
                modifier = Modifier.scale(tickScale.value)
            )
            MessageStatus.Failed -> ChatIconFailedBadge(
                modifier = Modifier
                    .size(14.dp)
                    .semantics { contentDescription = "Message failed to send" },
                badgeColor = c.danger,
                symbolColor = Color.White
            )
            null -> Unit
        }
    }
}

@Composable
fun TickMark(double: Boolean, color: Color, modifier: Modifier = Modifier) {
    if (double) {
        ChatIconDoubleTick(modifier = modifier.size(16.dp), tint = color)
    } else {
        ChatIconSingleTick(modifier = modifier.size(12.dp), tint = color)
    }
}

// ---------------------------------------------------------------------------
// Reply quote, forwarded header, translation strip
// ---------------------------------------------------------------------------

@Composable
fun ReplyQuote(
    sender: String,
    preview: String,
    outgoing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: (@Composable () -> Unit)? = null,
) {
    val c = LocalChatColors.current
    val accent = if (outgoing) c.accentOut else c.accentIn
    Row(
        modifier = modifier
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(accent))
        Column(Modifier.weight(1f, fill = false).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(sender, color = accent, fontSize = 12f.chatSp(), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                preview,
                color = if (outgoing) c.textSecondaryOut else c.textSecondary,
                fontSize = 13f.chatSp(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (thumbnail != null) {
            Box(Modifier.size(34.dp)) { thumbnail() }
        }
    }
}

@Composable
fun ForwardedHeader(fromName: String, outgoing: Boolean, modifier: Modifier = Modifier) {
    val c = LocalChatColors.current
    Text(
        text = "Forwarded from $fromName",
        color = if (outgoing) c.accentOut else c.accentIn,
        fontSize = 12f.chatSp(),
        fontStyle = FontStyle.Italic,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(bottom = 2.dp),
    )
}

/** Translation under a message. Shows a small "Cloud AI" tag when the cloud was used. */
@Composable
fun TranslationStrip(
    translated: String,
    languageLabel: String,
    outgoing: Boolean,
    viaCloud: Boolean,
    onListen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalChatColors.current
    val muted = if (outgoing) c.textSecondaryOut else c.textSecondary
    Column(modifier.padding(top = 6.dp)) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.hairline))
        Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.Top) {
            ChatIconTranslate(modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = muted)
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(translated, color = c.text, fontSize = 14f.chatSp(), lineHeight = 19f.chatSp())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(languageLabel, color = muted, fontSize = 11f.chatSp())
                    if (viaCloud) {
                        Text(
                            "Cloud AI",
                            color = muted,
                            fontSize = 11f.chatSp(),
                            modifier = Modifier
                                .border(0.5.dp, muted, RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 0.dp),
                        )
                    }
                }
            }
            Box(
                Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onListen),
                contentAlignment = Alignment.Center,
            ) {
                ChatIconVolume(modifier = Modifier.size(18.dp), tint = muted)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Voice note
// ---------------------------------------------------------------------------

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
fun VoiceNoteContent(
    outgoing: Boolean,
    amplitudes: List<Int>,
    durationMs: Long,
    positionMs: Long,
    isPlaying: Boolean,
    isLoading: Boolean,
    isUnread: Boolean,
    speed: Float,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeedClick: () -> Unit,
    modifier: Modifier = Modifier,
    transcript: String? = null,
) {
    val c = LocalChatColors.current
    val muted = if (outgoing) c.textSecondaryOut else c.textSecondary
    Column(modifier.width(ChatDimens.VoiceWidth).padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayPauseButton(outgoing, isPlaying, isLoading, onPlayPause)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                val progress = if (durationMs > 0) (positionMs / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                Waveform(
                    amplitudes = amplitudes,
                    progress = progress,
                    idle = if (outgoing) c.waveIdleOut else c.waveIdleIn,
                    played = if (outgoing) c.wavePlayedOut else c.wavePlayedIn,
                    onSeek = onSeek,
                    modifier = Modifier.fillMaxWidth().height(30.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val timeLabel = if (isPlaying || positionMs > 0) {
                        "${formatDuration(positionMs)} / ${formatDuration(durationMs)}"
                    } else {
                        formatDuration(durationMs)
                    }
                    Text(timeLabel, color = muted, fontSize = 11f.chatSp(), fontWeight = FontWeight.Medium)
                    if (isPlaying || speed != 1f) {
                        Text(
                            text = if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x",
                            color = c.text,
                            fontSize = 11f.chatSp(),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(c.hairline)
                                .clickable(onClick = onSpeedClick)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    } else if (isUnread && positionMs == 0L) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (outgoing) c.accentOut else c.accentIn))
                    }
                }
            }
        }
        if (!transcript.isNullOrBlank()) {
            Text(transcript, color = c.text, fontSize = 14f.chatSp(), lineHeight = 19f.chatSp(), modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
fun PlayPauseButton(outgoing: Boolean, isPlaying: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    val c = LocalChatColors.current
    val bg = if (outgoing) c.voiceButtonOut else c.voiceButtonIn
    val fg = if (outgoing) c.onVoiceButtonOut else c.onVoiceButtonIn
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bg,
        shadowElevation = 2.dp,
        modifier = Modifier.size(42.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = fg, strokeWidth = 2.dp)
            } else {
                if (isPlaying) {
                    ChatIconPause(modifier = Modifier.size(18.dp), tint = fg)
                } else {
                    ChatIconPlay(modifier = Modifier.size(18.dp), tint = fg)
                }
            }
        }
    }
}

@Composable
fun Waveform(
    amplitudes: List<Int>,
    progress: Float,
    idle: Color,
    played: Color,
    modifier: Modifier = Modifier,
    onSeek: ((Float) -> Unit)? = null,
) {
    var widthPx by remember { mutableStateOf(0f) }
    val seek by rememberUpdatedState(onSeek)
    val gestures = if (onSeek != null) {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures { off -> if (widthPx > 0f) seek?.invoke((off.x / widthPx).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    if (widthPx > 0f) seek?.invoke((change.position.x / widthPx).coerceIn(0f, 1f))
                }
            }
    } else {
        Modifier
    }
    Canvas(modifier.onSizeChanged { widthPx = it.width.toFloat() }.then(gestures)) {
        val barW = 3.2.dp.toPx()
        val gap = 2.2.dp.toPx()
        val count = max(1, ((size.width + gap) / (barW + gap)).toInt())
        val src = if (amplitudes.isEmpty()) List(count) { 40 } else amplitudes
        for (i in 0 until count) {
            val a = src[(i * src.size / count).coerceAtMost(src.size - 1)] / 255f
            val h = max(4.dp.toPx(), a * size.height)
            drawRoundRect(
                color = if ((i + 0.5f) / count <= progress) played else idle,
                topLeft = Offset(i * (barW + gap), (size.height - h) / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Media, files, links, emoji-only
// ---------------------------------------------------------------------------

@Composable
fun MediaFrame(
    aspectRatio: Float,
    modifier: Modifier = Modifier,
    overlay: @Composable (androidx.compose.foundation.layout.BoxScope.() -> Unit) = {},
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio.coerceIn(0.6f, 1.8f))
            .clip(RoundedCornerShape(ChatDimens.MediaInnerRadius)),
    ) {
        content()
        overlay()
    }
}

@Composable
fun MediaTimeChip(time: String, outgoing: Boolean, status: MessageStatus?, modifier: Modifier = Modifier) {
    val c = LocalChatColors.current
    MessageMeta(
        time = time,
        outgoing = outgoing,
        status = status,
        onMedia = true,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.mediaChip)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
fun FileCard(
    extLabel: String,
    name: String,
    sizeText: String,
    outgoing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    val c = LocalChatColors.current
    val bg = if (outgoing) c.voiceButtonOut else c.voiceButtonIn
    val fg = if (outgoing) c.onVoiceButtonOut else c.onVoiceButtonIn
    Row(modifier.clickable(onClick = onClick).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            if (progress != null) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(28.dp), color = fg, strokeWidth = 2.dp)
            } else {
                Text(extLabel.take(4), color = fg, fontSize = 12f.chatSp(), fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f, fill = false)) {
            Text(name, color = c.text, fontSize = 15f.chatSp(), fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sizeText, color = if (outgoing) c.textSecondaryOut else c.textSecondary, fontSize = 12f.chatSp())
        }
    }
}

@Composable
fun LinkPreviewCard(
    siteName: String,
    title: String,
    description: String?,
    outgoing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    image: (@Composable () -> Unit)? = null,
) {
    val c = LocalChatColors.current
    val accent = if (outgoing) c.accentOut else c.accentIn
    Row(modifier.padding(bottom = 4.dp).clickable(onClick = onClick).height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(accent))
        Column(Modifier.padding(start = 8.dp)) {
            Text(siteName, color = accent, fontSize = 12f.chatSp(), fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(title, color = c.text, fontSize = 14f.chatSp(), fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!description.isNullOrBlank()) {
                Text(description, color = if (outgoing) c.textSecondaryOut else c.textSecondary, fontSize = 13f.chatSp(), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (image != null) {
                Box(Modifier.padding(top = 6.dp).fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))) { image() }
            }
        }
    }
}

/** 1 if the text is only 1-3 emoji (render big, no bubble), else 0. Returns the emoji count. */
fun emojiOnlyCount(text: String): Int {
    val t = text.trim()
    if (t.isEmpty() || t.length > 24) return 0
    var i = 0
    while (i < t.length) {
        val cp = t.codePointAt(i)
        val type = Character.getType(cp)
        val ok = Character.isWhitespace(cp) || cp == 0x200D ||
            type == Character.OTHER_SYMBOL.toInt() || type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.FORMAT.toInt() || type == Character.MODIFIER_SYMBOL.toInt() ||
            type == Character.ENCLOSING_MARK.toInt()
        if (!ok) return 0
        i += Character.charCount(cp)
    }
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(t)
    var n = 0
    while (it.next() != java.text.BreakIterator.DONE) n++
    return if (n in 1..3) n else 0
}

@Composable
fun EmojiOnlyMessage(
    emoji: String,
    count: Int,
    time: String,
    outgoing: Boolean,
    status: MessageStatus?,
    modifier: Modifier = Modifier,
) {
    val size = when (count) {
        1 -> 52f
        2 -> 42f
        else -> 34f
    }
    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(emoji, fontSize = size.chatSp())
        MessageMeta(time, outgoing, status)
    }
}

// ---------------------------------------------------------------------------
// Reactions
// ---------------------------------------------------------------------------

data class ReactionUi(val emoji: String, val count: Int, val mine: Boolean)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionRow(reactions: List<ReactionUi>, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    if (reactions.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        reactions.forEach { r -> ReactionChip(r.emoji, r.count, r.mine) { onClick(r.emoji) } }
    }
}

@Composable
fun ReactionChip(emoji: String, count: Int, mine: Boolean, onClick: () -> Unit) {
    val c = LocalChatColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .height(ChatDimens.ReactionHeight)
            .clip(shape)
            .background(if (mine) c.accentIn.copy(alpha = 0.16f) else c.incomingBubble)
            .border(if (mine) 1.dp else 0.5.dp, if (mine) c.accentIn else c.hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(emoji, fontSize = 13f.chatSp())
        if (count > 1) Text(count.toString(), color = c.text, fontSize = 12f.chatSp())
    }
}

// ---------------------------------------------------------------------------
// Video & Audio File Attachment Bubbles
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoAttachmentBubble(
    localPath: String?,
    durationSec: Int,
    caption: String?,
    timeText: String,
    outgoing: Boolean,
    status: MessageStatus?,
    downloadState: String?,
    failReason: String?,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalChatColors.current
    val context = LocalContext.current
    val hasFile = !localPath.isNullOrBlank() && java.io.File(localPath.removePrefix("file://")).let { it.exists() && it.length() > 0L }
    val cleanFilePath = localPath?.removePrefix("file://").orEmpty()

    val thumbnail by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = cleanFilePath, key2 = hasFile) {
        value = if (hasFile) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                ChatVideoHelper.extractThumbnail(context, android.net.Uri.fromFile(java.io.File(cleanFilePath)))
            }
        } else null
    }

    val isExpired = !hasFile && downloadState == com.example.db.DmMediaDownloadState.EXPIRED
    val isFailed = !hasFile && downloadState == com.example.db.DmMediaDownloadState.FAILED
    val isDownloading = !hasFile && downloadState == com.example.db.DmMediaDownloadState.DOWNLOADING
    val isPending = !hasFile && !isExpired && !isFailed && !isDownloading

    Column(modifier = modifier.widthIn(min = 216.dp, max = ChatDimens.BubbleMaxWidth)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.35f)
                .clip(RoundedCornerShape(ChatDimens.MediaInnerRadius))
                .background(Color(0xFF0F172A))
                .combinedClickable(
                    onClick = {
                        when {
                            hasFile -> onPlayClick()
                            isExpired -> Unit
                            else -> onDownloadClick()
                        }
                    },
                    onLongClick = onLongPress
                ),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = caption ?: "Video",
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    contentScale = ContentScale.Crop
                )
            }

            // Duration pill in top-left
            if (durationSec > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.62f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ChatIconVideo(tint = Color.White, modifier = Modifier.size(12.dp))
                        Text(
                            text = formatDuration(durationSec * 1000L),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Center control / download state
            when {
                hasFile -> {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.58f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            ChatIconPlay(tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                isExpired -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = failReason?.takeIf { it.isNotBlank() } ?: "Media expired",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                isFailed -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = CircleShape, color = Color(0xFFEF4444).copy(alpha = 0.88f)) {
                            Box(Modifier.padding(10.dp)) {
                                ChatIconRetry(tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text("Failed. Tap to retry", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                isDownloading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), color = Color.White, strokeWidth = 2.5.dp)
                        Text("Downloading...", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                    }
                }

                isPending -> {
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.62f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ChatIconDownload(tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Tap to download", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(7.dp)
                ) {
                    MediaTimeChip(time = timeText, outgoing = outgoing, status = status)
                }
            }
        }

        if (!caption.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                MessageText(
                    text = AnnotatedString(caption),
                    time = timeText,
                    outgoing = outgoing,
                    status = status
                )
            }
        }
    }
}

@Composable
fun AudioAttachmentBubble(
    title: String,
    durationMs: Long,
    positionMs: Long,
    isPlaying: Boolean,
    isLoading: Boolean,
    speed: Float,
    outgoing: Boolean,
    downloadState: String?,
    failReason: String?,
    hasLocalFile: Boolean,
    onPlayPause: () -> Unit,
    onDownloadClick: () -> Unit,
    onSpeedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalChatColors.current
    val muted = if (outgoing) c.textSecondaryOut else c.textSecondary
    val accent = if (outgoing) c.voiceButtonOut else c.voiceButtonIn
    val onAccent = if (outgoing) c.onVoiceButtonOut else c.onVoiceButtonIn
    val isExpired = !hasLocalFile && downloadState == com.example.db.DmMediaDownloadState.EXPIRED
    val isFailed = !hasLocalFile && downloadState == com.example.db.DmMediaDownloadState.FAILED
    val isDownloading = !hasLocalFile && downloadState == com.example.db.DmMediaDownloadState.DOWNLOADING
    val isPending = !hasLocalFile && !isExpired && !isFailed && !isDownloading

    Row(
        modifier = modifier
            .width(ChatDimens.VoiceWidth)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = {
                when {
                    hasLocalFile -> onPlayPause()
                    isExpired -> Unit
                    else -> onDownloadClick()
                }
            },
            shape = CircleShape,
            color = if (isFailed) c.danger else accent,
            shadowElevation = 2.dp,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    isLoading || isDownloading -> CircularProgressIndicator(Modifier.size(20.dp), color = onAccent, strokeWidth = 2.dp)
                    isFailed -> ChatIconRetry(modifier = Modifier.size(18.dp), tint = Color.White)
                    isPending -> ChatIconDownload(modifier = Modifier.size(18.dp), tint = onAccent)
                    isExpired -> ChatIconMusic(modifier = Modifier.size(18.dp), tint = onAccent.copy(alpha = 0.6f))
                    isPlaying -> ChatIconPause(modifier = Modifier.size(18.dp), tint = onAccent)
                    else -> ChatIconPlay(modifier = Modifier.size(18.dp), tint = onAccent)
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChatIconMusic(modifier = Modifier.size(14.dp), tint = if (outgoing) c.accentOut else c.accentIn)
                Text(
                    text = title.ifBlank { "Audio file" },
                    color = c.text,
                    fontSize = 14f.chatSp(),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (outgoing) c.wavePlayedOut else c.wavePlayedIn,
                trackColor = if (outgoing) c.waveIdleOut else c.waveIdleIn
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusLabel = when {
                    isExpired -> failReason ?: "Expired"
                    isFailed -> "Failed. Tap to retry"
                    isDownloading -> "Downloading..."
                    isPending -> "Tap to download"
                    isPlaying || positionMs > 0 -> "${formatDuration(positionMs)} / ${formatDuration(durationMs)}"
                    durationMs > 0 -> formatDuration(durationMs)
                    else -> "Audio"
                }
                Text(
                    text = statusLabel,
                    color = if (isFailed) c.danger else muted,
                    fontSize = 11f.chatSp(),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (hasLocalFile && (isPlaying || speed != 1f)) {
                    Text(
                        text = if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x",
                        color = c.text,
                        fontSize = 11f.chatSp(),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.hairline)
                            .clickable(onClick = onSpeedClick)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

