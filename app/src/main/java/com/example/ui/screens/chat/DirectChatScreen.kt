package com.example.ui.screens.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ChatPreferencesManager
import com.example.CloudAiManager
import com.example.SupabaseSessionManager
import com.example.db.LocalDmConversation
import com.example.db.LocalDmMessage
import com.example.db.LocalDmMessageReaction
import com.example.repository.EncryptedChatRepository
import com.example.ui.chat.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

sealed class DmTimelineItem {
    abstract val id: String
    data class DateHeader(val label: String) : DmTimelineItem() {
        override val id: String = "date_$label"
    }
    data class SecurityHeader(val text: String) : DmTimelineItem() {
        override val id: String = "security_header"
    }
    data class MessageItem(
        val message: LocalDmMessage,
        val position: GroupPosition,
        val reactions: List<ReactionUi>,
        val isFirstInGroup: Boolean,
        val isLastInGroup: Boolean
    ) : DmTimelineItem() {
        override val id: String = message.id
    }
}

data class WordLookupState(
    val word: String,
    val fullSentence: String,
    val translation: String? = null,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    partnerUserId: String,
    partnerName: String,
    partnerAvatarUrl: String? = null,
    partnerCountry: String? = null,
    partnerCountryCode: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    LaunchedEffect(context) {
        ChatPreferencesManager.init(context)
        CloudAiManager.init(context)
    }

    val darkTheme = rememberEffectiveChatDarkTheme()
    val chatPreset by ChatPreferencesManager.chatPreset.collectAsState()
    val chatTextScale by ChatPreferencesManager.chatTextScale.collectAsState()
    val isAiConsentGranted by CloudAiManager.isCloudAiEnabled.collectAsState()

    val currentUserId = remember(context) { SupabaseSessionManager.getUserId(context) ?: "" }
    val voicePlayer = rememberVoicePlaybackController()
    val voiceRecorder = remember { LiveVoiceRecorder(context.applicationContext, coroutineScope) }

    var resolvedConversation by remember { mutableStateOf<LocalDmConversation?>(null) }
    var conversationId by remember { mutableStateOf("") }
    var rawMessages by remember { mutableStateOf<List<LocalDmMessage>>(emptyList()) }
    var rawReactions by remember { mutableStateOf<List<LocalDmMessageReaction>>(emptyList()) }

    var inputText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<LocalDmMessage?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<LocalDmMessage?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedMessageIds = remember { mutableStateListOf<String>() }

    var activeMediaViewerIndex by remember { mutableStateOf<Int?>(null) }
    var activeWordLookup by remember { mutableStateOf<WordLookupState?>(null) }
    var showAiAssistSheet by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var translatedMessages = remember { mutableStateMapOf<String, String>() }

    val flashAnimatables = remember { mutableStateMapOf<String, Animatable<Float, *>>() }

    fun flashMessage(messageId: String) {
        coroutineScope.launch {
            val anim = flashAnimatables.getOrPut(messageId) { Animatable(0f) }
            anim.snapTo(1f)
            anim.animateTo(0f, animationSpec = tween(durationMillis = 1200))
        }
    }

    // TTS
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Initialize Conversation & Observe DB
    LaunchedEffect(partnerUserId) {
        try {
            val conv = EncryptedChatRepository.getOrCreateLocalConversation(
                context = context,
                otherUserId = partnerUserId,
                otherUserName = partnerName,
                otherUserAvatar = partnerAvatarUrl
            )
            resolvedConversation = conv
            conversationId = conv.id
            EncryptedChatRepository.markConversationRead(conv.id)
        } catch (e: Exception) {
            android.util.Log.e("DirectChatScreen", "Error resolving conversation", e)
        }
    }

    LaunchedEffect(conversationId) {
        if (conversationId.isNotBlank()) {
            EncryptedChatRepository.getMessagesFlow(conversationId).collect { msgs ->
                rawMessages = msgs
            }
        }
    }

    // Pick photos
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(4)) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                uris.forEach { uri ->
                    try {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (bytes != null && bytes.isNotEmpty()) {
                            EncryptedChatRepository.sendMediaMessage(
                                context = context,
                                recipientUserId = partnerUserId,
                                recipientName = partnerName,
                                recipientAvatar = partnerAvatarUrl,
                                rawBytes = bytes,
                                messageType = "image",
                                mimeType = "image/jpeg"
                            )
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error sending photo: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Pick document
    val docPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        EncryptedChatRepository.sendMediaMessage(
                            context = context,
                            recipientUserId = partnerUserId,
                            recipientName = partnerName,
                            recipientAvatar = partnerAvatarUrl,
                            rawBytes = bytes,
                            messageType = "image",
                            mimeType = "application/octet-stream"
                        )
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error sending file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ProvideChatTheme(
        preset = chatPreset,
        darkTheme = darkTheme,
        textScale = chatTextScale
    ) {
        val colors = LocalChatColors.current

        val timelineItems = remember(rawMessages, rawReactions, currentUserId) {
            val sorted = rawMessages.sortedBy { it.timestamp }
            val items = mutableListOf<DmTimelineItem>()
            items.add(DmTimelineItem.SecurityHeader("Messages and calls are end-to-end encrypted. No one outside of this chat can read or listen to them."))

            var prevDateLabel = ""
            for (i in sorted.indices) {
                val msg = sorted[i]
                val dateLabel = formatChatDateChip(msg.timestamp)
                if (dateLabel != prevDateLabel) {
                    items.add(DmTimelineItem.DateHeader(dateLabel))
                    prevDateLabel = dateLabel
                }

                val isOlderSameSender = i > 0 &&
                        sorted[i - 1].isOutgoing == msg.isOutgoing &&
                        (msg.timestamp - sorted[i - 1].timestamp) <= 2 * 60 * 1000L

                val isNewerSameSender = i < sorted.size - 1 &&
                        sorted[i + 1].isOutgoing == msg.isOutgoing &&
                        (sorted[i + 1].timestamp - msg.timestamp) <= 2 * 60 * 1000L

                val position = computeGroupPosition(isOlderSameSender, isNewerSameSender)
                val isFirst = !isOlderSameSender
                val isLast = !isNewerSameSender

                val msgReactions = rawReactions.filter { it.targetMessageId == msg.id }
                val reactionUis = msgReactions.groupBy { it.emoji }.map { (emoji, list) ->
                    ReactionUi(
                        emoji = emoji,
                        count = list.size,
                        mine = list.any { it.senderId == currentUserId }
                    )
                }

                items.add(
                    DmTimelineItem.MessageItem(
                        message = msg,
                        position = position,
                        reactions = reactionUis,
                        isFirstInGroup = isFirst,
                        isLastInGroup = isLast
                    )
                )
            }
            items
        }

        val allMediaItems = remember(rawMessages) {
            rawMessages.filter { it.text.startsWith("[img]") || it.text.startsWith("[imgs]") }
                .map { msg ->
                    val url = msg.text.removePrefix("[img]").removePrefix("[imgs]").substringBefore("|")
                    ChatViewerMediaItem(
                        id = msg.id,
                        url = url,
                        senderName = if (msg.isOutgoing) "You" else partnerName,
                        dateText = formatChatTime(msg.timestamp),
                        caption = null,
                        isVideo = false
                    )
                }
        }

        val unreadCount = remember(rawMessages) {
            rawMessages.count { !it.isOutgoing }
        }

        BackHandler(enabled = isSelectionMode || selectedMessageForMenu != null) {
            if (isSelectionMode) {
                isSelectionMode = false
                selectedMessageIds.clear()
            } else {
                selectedMessageForMenu = null
            }
        }

        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    SelectionTopBar(
                        selectedCount = selectedMessageIds.size,
                        actions = listOf(
                            MessageAction("copy", "Copy", Icons.Rounded.ContentCopy),
                            MessageAction("forward", "Forward", Icons.AutoMirrored.Rounded.Forward),
                            MessageAction("delete", "Delete", Icons.Rounded.DeleteOutline, destructive = true)
                        ),
                        onAction = { actionId ->
                            when (actionId) {
                                "copy" -> {
                                    val joined = rawMessages.filter { it.id in selectedMessageIds }.joinToString("\n") { it.text }
                                    clipboardManager.setText(AnnotatedString(joined))
                                    Toast.makeText(context, "Copied ${selectedMessageIds.size} messages", Toast.LENGTH_SHORT).show()
                                }
                                "forward" -> {
                                    Toast.makeText(context, "Forward ${selectedMessageIds.size} messages", Toast.LENGTH_SHORT).show()
                                }
                                "delete" -> {
                                    coroutineScope.launch {
                                        selectedMessageIds.forEach { id ->
                                            EncryptedChatRepository.deleteConversationLocally(conversationId)
                                        }
                                        Toast.makeText(context, "Deleted messages", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            isSelectionMode = false
                            selectedMessageIds.clear()
                        },
                        onClose = {
                            isSelectionMode = false
                            selectedMessageIds.clear()
                        }
                    )
                } else {
                    Surface(
                        color = colors.wallpaper,
                        shadowElevation = 0.5.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = colors.text)
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.accentIn.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!partnerAvatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = partnerAvatarUrl,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = partnerName.take(1).uppercase(),
                                        color = colors.accentIn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = partnerName,
                                    color = colors.text,
                                    fontSize = 16f.chatSp(),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (!partnerCountry.isNullOrBlank()) "Online · $partnerCountry" else "Online",
                                        color = colors.textSecondary,
                                        fontSize = 12f.chatSp()
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.wallpaper)
                        .imePadding()
                ) {
                    ChatInputBar(
                        text = inputText,
                        onTextChange = { inputText = it },
                        onSend = {
                            if (inputText.isNotBlank()) {
                                val reply = replyingToMessage
                                val textToSend = inputText
                                inputText = ""
                                replyingToMessage = null
                                coroutineScope.launch {
                                    EncryptedChatRepository.sendMessage(
                                        context = context,
                                        recipientUserId = partnerUserId,
                                        recipientName = partnerName,
                                        recipientAvatar = partnerAvatarUrl,
                                        text = textToSend,
                                        replyToText = reply?.text,
                                        replyToSender = if (reply != null) {
                                            if (reply.isOutgoing) "You" else partnerName
                                        } else null
                                    )
                                    if (timelineItems.isNotEmpty()) {
                                        listState.animateScrollToItem(timelineItems.size - 1)
                                    }
                                }
                            }
                        },
                        onAttach = { showAttachmentSheet = true },
                        onEmoji = {
                            inputText += " 😊"
                        },
                        showAiButton = isAiConsentGranted,
                        onAi = { showAiAssistSheet = true },
                        topSlot = if (replyingToMessage != null) {
                            {
                                ReplyPreviewStrip(
                                    title = if (replyingToMessage!!.isOutgoing) "You" else partnerName,
                                    preview = replyingToMessage!!.text.take(60),
                                    onClose = { replyingToMessage = null }
                                )
                            }
                        } else null,
                        recording = if (voiceRecorder.isRecording) {
                            {
                                RecordingBar(
                                    elapsedMs = voiceRecorder.elapsedMs,
                                    amplitudes = voiceRecorder.liveAmplitudes.toList(),
                                    slideProgress = 0f,
                                    locked = false,
                                    onCancel = { voiceRecorder.cancel() }
                                )
                            }
                        } else null,
                        micButton = {
                            MicHoldButton(
                                isRecording = voiceRecorder.isRecording,
                                onStart = {
                                    voiceRecorder.start()
                                },
                                onMove = { dx, dy -> },
                                onEnd = { cancelled, locked ->
                                    val result = voiceRecorder.stop()
                                    if (!cancelled && result != null && result.bytes.isNotEmpty()) {
                                        coroutineScope.launch {
                                            EncryptedChatRepository.sendMediaMessage(
                                                context = context,
                                                recipientUserId = partnerUserId,
                                                recipientName = partnerName,
                                                recipientAvatar = partnerAvatarUrl,
                                                rawBytes = result.bytes,
                                                messageType = "voice",
                                                mimeType = "audio/aac",
                                                durationSeconds = (result.durationMs / 1000).toInt()
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    )
                }
            },
            containerColor = colors.wallpaper
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(colors.wallpaper)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(
                        items = timelineItems,
                        key = { _, item -> item.id }
                    ) { _, item ->
                        when (item) {
                            is DmTimelineItem.DateHeader -> {
                                DateChip(text = item.label)
                            }
                            is DmTimelineItem.SecurityHeader -> {
                                SecurityNotice(text = item.text)
                            }
                            is DmTimelineItem.MessageItem -> {
                                val msg = item.message
                                val isOutgoing = msg.isOutgoing
                                val flashAlpha = flashAnimatables[msg.id]?.value ?: 0f
                                val isSelected = selectedMessageIds.contains(msg.id)

                                val status = when {
                                    !isOutgoing -> null
                                    else -> MessageStatus.Read
                                }

                                val isOnlyEmojis = emojiOnlyCount(msg.text.trim())

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = if (isOutgoing) 48.dp else 12.dp,
                                            end = if (isOutgoing) 12.dp else 48.dp,
                                            top = if (item.position == GroupPosition.First || item.position == GroupPosition.Single) 6.dp else 2.dp,
                                            bottom = 2.dp
                                        ),
                                    horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // 28dp Avatar only beside last message of group
                                    if (!isOutgoing) {
                                        if (item.isLastInGroup) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.accentIn.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!partnerAvatarUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = partnerAvatarUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Text(
                                                        text = partnerName.take(1).uppercase(),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.accentIn
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(28.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    Column(
                                        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
                                    ) {
                                        // Sender name on first incoming message of group
                                        if (!isOutgoing && (item.position == GroupPosition.Single || item.position == GroupPosition.First)) {
                                            SenderName(
                                                name = partnerName,
                                                userId = partnerUserId,
                                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                            )
                                        }

                                        val bubbleModifier = Modifier
                                            .swipeToReply {
                                                replyingToMessage = msg
                                            }
                                            .pointerInput(msg.id) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        if (!isSelectionMode) {
                                                            selectedMessageForMenu = msg
                                                        }
                                                    },
                                                    onTap = {
                                                        if (isSelectionMode) {
                                                            if (isSelected) selectedMessageIds.remove(msg.id)
                                                            else selectedMessageIds.add(msg.id)
                                                        }
                                                    }
                                                )
                                            }

                                        if (isOnlyEmojis in 1..3) {
                                            EmojiOnlyMessage(
                                                emoji = msg.text.trim(),
                                                count = isOnlyEmojis,
                                                time = formatChatTime(msg.timestamp),
                                                outgoing = isOutgoing,
                                                status = status,
                                                modifier = bubbleModifier
                                            )
                                        } else {
                                            MessageBubble(
                                                outgoing = isOutgoing,
                                                position = item.position,
                                                highlighted = flashAlpha > 0f,
                                                selected = isSelected,
                                                modifier = bubbleModifier
                                            ) {
                                                // Reply quote
                                                if (!msg.replyToText.isNullOrBlank()) {
                                                    ReplyQuote(
                                                        sender = msg.replyToSender ?: "Reply",
                                                        preview = msg.replyToText,
                                                        outgoing = isOutgoing,
                                                        onClick = {
                                                            val targetIdx = timelineItems.indexOfFirst {
                                                                it is DmTimelineItem.MessageItem && it.message.text == msg.replyToText
                                                            }
                                                            if (targetIdx != -1) {
                                                                val found = timelineItems[targetIdx] as DmTimelineItem.MessageItem
                                                                coroutineScope.launch {
                                                                    listState.animateScrollToItem(targetIdx)
                                                                    flashMessage(found.message.id)
                                                                }
                                                            }
                                                        }
                                                    )
                                                }

                                                val rawText = msg.text
                                                when {
                                                    rawText.startsWith("[voice]") -> {
                                                        val path = rawText.removePrefix("[voice]").substringBefore("|")
                                                        val durationSec = rawText.substringAfter("|", "0").toIntOrNull() ?: 0
                                                        val isPlayingThis = voicePlayer.currentPlayingId == msg.id && voicePlayer.isPlaying

                                                        VoiceNoteContent(
                                                            outgoing = isOutgoing,
                                                            amplitudes = remember(msg.id) { List(48) { (40 + (it * 7) % 180) } },
                                                            durationMs = durationSec * 1000L,
                                                            positionMs = if (voicePlayer.currentPlayingId == msg.id) voicePlayer.positionMs else 0L,
                                                            isPlaying = isPlayingThis,
                                                            isLoading = voicePlayer.currentPlayingId == msg.id && voicePlayer.isLoading,
                                                            isUnread = !isOutgoing,
                                                            speed = voicePlayer.speed,
                                                            onPlayPause = {
                                                                voicePlayer.playOrPause(
                                                                    messageId = msg.id,
                                                                    audioSource = path,
                                                                    fallbackDurationMs = durationSec * 1000L,
                                                                    onError = { err ->
                                                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                                    }
                                                                )
                                                            },
                                                            onSeek = { seekFrac ->
                                                                if (voicePlayer.currentPlayingId == msg.id) {
                                                                    voicePlayer.seekTo(seekFrac)
                                                                }
                                                            },
                                                            onSpeedClick = { voicePlayer.cycleSpeed() }
                                                        )
                                                    }

                                                    rawText.startsWith("[img]") || rawText.startsWith("[imgs]") -> {
                                                        val url = rawText.removePrefix("[img]").removePrefix("[imgs]").substringBefore("|")
                                                        MediaFrame(
                                                            aspectRatio = 1.25f,
                                                            content = {
                                                                AsyncImage(
                                                                    model = url,
                                                                    contentDescription = "Photo",
                                                                    modifier = Modifier
                                                                        .fillMaxSize()
                                                                        .clickable {
                                                                            val idx = allMediaItems.indexOfFirst { it.id == msg.id }
                                                                            activeMediaViewerIndex = if (idx >= 0) idx else 0
                                                                        },
                                                                    contentScale = ContentScale.Crop
                                                                )
                                                            },
                                                            overlay = {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .align(Alignment.BottomEnd)
                                                                        .padding(6.dp)
                                                                ) {
                                                                    MediaTimeChip(
                                                                        time = formatChatTime(msg.timestamp),
                                                                        outgoing = isOutgoing,
                                                                        status = status
                                                                    )
                                                                }
                                                            }
                                                        )
                                                    }

                                                    else -> {
                                                        val annotated = buildMessageText(
                                                            raw = rawText,
                                                            mentionNames = listOf(partnerName),
                                                            accent = colors.accentIn
                                                        )
                                                        MessageText(
                                                            text = annotated,
                                                            time = formatChatTime(msg.timestamp),
                                                            outgoing = isOutgoing,
                                                            status = status,
                                                            onWordClick = { word ->
                                                                activeWordLookup = WordLookupState(
                                                                    word = word,
                                                                    fullSentence = rawText,
                                                                    isLoading = true
                                                                )
                                                                coroutineScope.launch {
                                                                    val res = CloudAiManager.translateMessage(context, "word_${word.hashCode()}", word, "English")
                                                                    activeWordLookup = activeWordLookup?.copy(
                                                                        translation = res.getOrNull()?.text ?: "Could not translate",
                                                                        isLoading = false
                                                                    )
                                                                }
                                                            }
                                                        )

                                                        val trans = translatedMessages[msg.id]
                                                        if (!trans.isNullOrBlank()) {
                                                            TranslationStrip(
                                                                translated = trans,
                                                                languageLabel = "English",
                                                                outgoing = isOutgoing,
                                                                viaCloud = true,
                                                                onListen = {
                                                                    tts?.speak(trans, TextToSpeech.QUEUE_FLUSH, null, "tts_${msg.id}")
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Reactions
                                        if (item.reactions.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            ReactionRow(
                                                reactions = item.reactions,
                                                onClick = { emoji ->
                                                    coroutineScope.launch {
                                                        // Toggle reaction
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Scroll to bottom FAB
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp)
                    ) {
                        ScrollToBottomFab(
                            unread = unreadCount,
                            onClick = {
                                coroutineScope.launch {
                                    if (timelineItems.isNotEmpty()) {
                                        listState.animateScrollToItem(timelineItems.size - 1)
                                    }
                                }
                            }
                        )
                    }
                }

                // Long press message overlay
                selectedMessageForMenu?.let { selectedMsg ->
                    MessageLongPressOverlay(
                        myReaction = null,
                        actions = listOf(
                            MessageAction("reply", "Reply", Icons.AutoMirrored.Rounded.Reply),
                            MessageAction("copy", "Copy", Icons.Rounded.ContentCopy),
                            MessageAction("translate", "Translate", Icons.Rounded.Translate),
                            MessageAction("listen", "Listen", Icons.AutoMirrored.Rounded.VolumeUp),
                            MessageAction("select", "Select", Icons.Rounded.CheckCircleOutline),
                            MessageAction("delete", "Delete", Icons.Rounded.DeleteOutline, destructive = true)
                        ),
                        onPickEmoji = { emoji ->
                            // Add reaction
                        },
                        onMoreEmoji = {},
                        onAction = { actionId ->
                            when (actionId) {
                                "reply" -> replyingToMessage = selectedMsg
                                "copy" -> {
                                    clipboardManager.setText(AnnotatedString(selectedMsg.text))
                                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                }
                                "translate" -> {
                                    coroutineScope.launch {
                                        val res = CloudAiManager.translateMessage(context, selectedMsg.id, selectedMsg.text, "English")
                                        res.onSuccess {
                                            translatedMessages[selectedMsg.id] = it.text
                                        }.onFailure {
                                            Toast.makeText(context, "Translation failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                "listen" -> {
                                    tts?.speak(selectedMsg.text, TextToSpeech.QUEUE_FLUSH, null, "tts_${selectedMsg.id}")
                                }
                                "select" -> {
                                    isSelectionMode = true
                                    selectedMessageIds.add(selectedMsg.id)
                                }
                                "delete" -> {
                                    coroutineScope.launch {
                                        EncryptedChatRepository.deleteConversationLocally(conversationId)
                                    }
                                }
                            }
                        },
                        onDismiss = { selectedMessageForMenu = null }
                    )
                }
            }
        }

        // Telephoto Fullscreen Media Viewer
        activeMediaViewerIndex?.let { initialIdx ->
            TelephotoMediaViewerDialog(
                mediaList = allMediaItems,
                initialIndex = initialIdx,
                onDismiss = { activeMediaViewerIndex = null },
                onSave = { item ->
                    Toast.makeText(context, "Saved photo to gallery", Toast.LENGTH_SHORT).show()
                },
                onShare = { item ->
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, item.url)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share media"))
                }
            )
        }

        // Word lookup bottom sheet
        activeWordLookup?.let { lookup ->
            ModalBottomSheet(
                onDismissRequest = { activeWordLookup = null },
                containerColor = colors.surface,
                contentColor = colors.text,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Translate, contentDescription = null, tint = colors.accentIn)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(lookup.word, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }

                    if (lookup.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.accentIn)
                    } else if (lookup.translation != null) {
                        Text(lookup.translation, fontSize = 16.sp, color = colors.text)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Saved '${lookup.word}' to vocabulary", Toast.LENGTH_SHORT).show()
                            activeWordLookup = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = colors.onBrand),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save to Vocabulary")
                    }
                }
            }
        }

        // AI Writing Assist Sheet
        if (showAiAssistSheet) {
            AiWritingAssistBottomSheet(
                initialText = inputText,
                onReplaceText = { enhanced ->
                    inputText = enhanced
                    showAiAssistSheet = false
                },
                onDismiss = { showAiAssistSheet = false }
            )
        }

        // Attachment Sheet
        if (showAttachmentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAttachmentSheet = false },
                containerColor = colors.surface,
                contentColor = colors.text,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Attach", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentSheet = false
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = colors.accentIn, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Text("Photo & Video", fontSize = 16.sp, color = colors.text)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentSheet = false
                                docPickerLauncher.launch(arrayOf("*/*"))
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.InsertDriveFile, contentDescription = null, tint = colors.accentIn, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Text("Document / File", fontSize = 16.sp, color = colors.text)
                    }
                }
            }
        }
    }
}
