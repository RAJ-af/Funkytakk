package com.example.ui.screens.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.ChatPreferencesManager
import com.example.CloudAiManager
import com.example.SupabaseSessionManager
import com.example.db.LocalGroupConversation
import com.example.db.LocalGroupMessage
import com.example.db.LocalMessageReaction
import com.example.repository.GroupChatRepository
import com.example.ui.chat.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

sealed class GroupTimelineItem {
    abstract val id: String
    data class DateHeader(val label: String) : GroupTimelineItem() {
        override val id: String = "date_$label"
    }
    data class SecurityHeader(val text: String) : GroupTimelineItem() {
        override val id: String = "security_header"
    }
    data class SystemEvent(val eventId: String, val text: String) : GroupTimelineItem() {
        override val id: String = eventId
    }
    data class MessageItem(
        val message: LocalGroupMessage,
        val position: GroupPosition,
        val reactions: List<ReactionUi>,
        val isFirstInGroup: Boolean,
        val isLastInGroup: Boolean
    ) : GroupTimelineItem() {
        override val id: String = message.id
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    group: LocalGroupConversation,
    onBack: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToManageMembers: (openAdd: Boolean) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    LaunchedEffect(context) {
        ChatPreferencesManager.init(context)
        CloudAiManager.init(context)
    }

    val darkTheme = rememberEffectiveChatDarkTheme() // Force false for light mode
    val chatPreset by ChatPreferencesManager.chatPreset.collectAsState()
    val chatTextScale by ChatPreferencesManager.chatTextScale.collectAsState()
    val isAiConsentGranted by CloudAiManager.isCloudAiEnabled.collectAsState()

    var rawMessages by remember { mutableStateOf<List<LocalGroupMessage>>(emptyList()) }
    var rawReactions by remember { mutableStateOf<List<LocalMessageReaction>>(emptyList()) }
    var currentUserId by remember { mutableStateOf("") }

    var inputText by remember { mutableStateOf("") }
    var isEmojiPickerOpen by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<LocalGroupMessage?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<LocalGroupMessage?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedMessageIds = remember { mutableStateListOf<String>() }

    var activeWordLookup by remember { mutableStateOf<WordLookupState?>(null) }
    var showAiAssistSheet by remember { mutableStateOf(false) }
    var activeMediaViewerIndex by remember { mutableStateOf<Int?>(null) }

    val voicePlayer = rememberVoicePlaybackController()
    val voiceRecorder = rememberLiveVoiceRecorder()

    // TTS engine
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsEngine?.language = Locale.getDefault()
            }
        }
        ttsEngine = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Flash animatables for jump to message
    val flashAnimatables = remember { mutableStateMapOf<String, Animatable<Float, *>>() }
    fun flashMessage(msgId: String) {
        coroutineScope.launch {
            val anim = flashAnimatables.getOrPut(msgId) { Animatable(0f) }
            anim.snapTo(1f)
            anim.animateTo(0f, animationSpec = tween(1200))
        }
    }

    // Audio record permission launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceRecorder.start()
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                uris.forEach { uri ->
                    try {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (bytes != null && bytes.isNotEmpty()) {
                            GroupChatRepository.sendGroupMediaMessage(
                                context = context,
                                groupId = group.id,
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

    // Observe messages and reactions
    LaunchedEffect(group.id) {
        currentUserId = SupabaseSessionManager.getUserId(context) ?: ""
        launch {
            GroupChatRepository.getGroupMessagesFlow(group.id).collect { msgs ->
                rawMessages = msgs
            }
        }
        launch {
            GroupChatRepository.getGroupReactionsFlow(group.id).collect { reacts ->
                rawReactions = reacts
            }
        }
    }

    ProvideChatTheme(
        preset = chatPreset,
        darkTheme = false,
        textScale = chatTextScale
    ) {
        val colors = LocalChatColors.current

        val timelineItems = remember(rawMessages, rawReactions, currentUserId) {
            val sorted = rawMessages.sortedBy { it.timestamp }
            val items = mutableListOf<GroupTimelineItem>()
            items.add(GroupTimelineItem.SecurityHeader("Messages and media in this group are end-to-end encrypted."))

            var prevDateLabel = ""
            for (i in sorted.indices) {
                val msg = sorted[i]
                val dateLabel = formatChatDateChip(msg.timestamp)
                if (dateLabel != prevDateLabel) {
                    items.add(GroupTimelineItem.DateHeader(dateLabel))
                    prevDateLabel = dateLabel
                }

                if (msg.messageType == "system") {
                    items.add(GroupTimelineItem.SystemEvent(msg.id, msg.text))
                    continue
                }

                val isOlderSameSender = i > 0 &&
                        sorted[i - 1].senderId == msg.senderId &&
                        (msg.timestamp - sorted[i - 1].timestamp) <= 2 * 60 * 1000L

                val isNewerSameSender = i < sorted.size - 1 &&
                        sorted[i + 1].senderId == msg.senderId &&
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
                    GroupTimelineItem.MessageItem(
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
            rawMessages.filter { it.messageType == "image" || it.messageType == "video" || it.text.startsWith("[img]") }
                .map { msg ->
                    val url = msg.localMediaPath ?: msg.text.removePrefix("[img]").removePrefix("[imgs]").substringBefore("|")
                    ChatViewerMediaItem(
                        id = msg.id,
                        url = url,
                        senderName = if (msg.senderId == currentUserId) "You" else msg.senderName,
                        dateText = formatChatTime(msg.timestamp),
                        caption = null,
                        isVideo = msg.messageType == "video"
                    )
                }
        }

        val unreadCount = remember(rawMessages, currentUserId) {
            rawMessages.count { it.senderId != currentUserId }
        }

        BackHandler(enabled = isEmojiPickerOpen || isSelectionMode || selectedMessageForMenu != null) {
            if (isEmojiPickerOpen) {
                isEmojiPickerOpen = false
            } else if (isSelectionMode) {
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
                            MessageAction("copy", "Copy", { ChatIconCopy(tint = colors.text) }),
                            MessageAction("forward", "Forward", { ChatIconForward(tint = colors.text) }),
                            MessageAction("delete", "Delete", { ChatIconTrash(tint = colors.danger) }, destructive = true)
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
                                    Toast.makeText(context, "Delete ${selectedMessageIds.size} messages", Toast.LENGTH_SHORT).show()
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
                        color = colors.surface,
                        shadowElevation = 1.dp,
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
                                ChatIconBack(tint = colors.text, modifier = Modifier.size(24.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.accentIn.copy(alpha = 0.15f))
                                    .clickable(onClick = onNavigateToInfo),
                                contentAlignment = Alignment.Center
                            ) {
                                if (group.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = group.avatarUrl,
                                        contentDescription = "Group Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = group.name.take(1).uppercase(),
                                        color = colors.accentIn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClick = onNavigateToInfo)
                            ) {
                                Text(
                                    text = group.name,
                                    color = colors.text,
                                    fontSize = 16f.chatSp(),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${group.memberCount} members",
                                    color = colors.textSecondary,
                                    fontSize = 12f.chatSp()
                                )
                            }

                            IconButton(onClick = { onNavigateToManageMembers(true) }) {
                                ChatIconAddUser(tint = colors.text, modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                ChatIconSettings(tint = colors.text, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .imePadding()
                ) {
                    ChatInputBar(
                        text = inputText,
                        onTextChange = {
                            inputText = it
                            if (isEmojiPickerOpen && it.isNotEmpty()) {
                                isEmojiPickerOpen = false
                            }
                        },
                        onSend = {
                            if (inputText.isNotBlank()) {
                                val reply = replyingToMessage
                                val textToSend = inputText
                                inputText = ""
                                replyingToMessage = null
                                isEmojiPickerOpen = false
                                coroutineScope.launch {
                                    GroupChatRepository.sendGroupMessage(
                                        context = context,
                                        groupId = group.id,
                                        text = textToSend,
                                        replyToMessage = reply
                                    )
                                    if (timelineItems.isNotEmpty()) {
                                        listState.animateScrollToItem(timelineItems.size - 1)
                                    }
                                }
                            }
                        },
                        onAttach = {
                            isEmojiPickerOpen = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onEmojiToggle = {
                            isEmojiPickerOpen = !isEmojiPickerOpen
                        },
                        isEmojiOpen = isEmojiPickerOpen,
                        showAiButton = isAiConsentGranted,
                        onAi = { showAiAssistSheet = true },
                        topSlot = if (replyingToMessage != null) {
                            {
                                ReplyPreviewStrip(
                                    title = replyingToMessage!!.senderName,
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
                                    onCancel = { voiceRecorder.cancel() },
                                    onSend = {
                                        val result = voiceRecorder.stop()
                                        if (result != null && result.bytes.isNotEmpty()) {
                                            coroutineScope.launch {
                                                GroupChatRepository.sendGroupMediaMessage(
                                                    context = context,
                                                    groupId = group.id,
                                                    rawBytes = result.bytes,
                                                    messageType = "voice",
                                                    mimeType = "audio/aac"
                                                )
                                                if (timelineItems.isNotEmpty()) {
                                                    listState.animateScrollToItem(timelineItems.size - 1)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        } else null,
                        micButton = {
                            MicHoldButton(
                                isRecording = voiceRecorder.isRecording,
                                onClick = {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!voiceRecorder.isRecording) {
                                        if (hasPermission) {
                                            voiceRecorder.start()
                                        } else {
                                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    } else {
                                        val result = voiceRecorder.stop()
                                        if (result != null && result.bytes.isNotEmpty()) {
                                            coroutineScope.launch {
                                                GroupChatRepository.sendGroupMediaMessage(
                                                    context = context,
                                                    groupId = group.id,
                                                    rawBytes = result.bytes,
                                                    messageType = "voice",
                                                    mimeType = "audio/aac"
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    )

                    // Fluent Emoji Picker Drawer
                    AnimatedVisibility(
                        visible = isEmojiPickerOpen,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        FluentEmojiPicker(
                            onEmojiSelect = { emoji ->
                                inputText += emoji
                            },
                            onBackspace = {
                                if (inputText.isNotEmpty()) {
                                    val lastCharCount = Character.charCount(inputText.codePointBefore(inputText.length))
                                    inputText = inputText.dropLast(lastCharCount)
                                }
                            }
                        )
                    }
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
                            is GroupTimelineItem.DateHeader -> {
                                DateChip(text = item.label)
                            }
                            is GroupTimelineItem.SecurityHeader -> {
                                SecurityNotice(text = item.text)
                            }
                            is GroupTimelineItem.SystemEvent -> {
                                SystemEventLine(text = item.text)
                            }
                            is GroupTimelineItem.MessageItem -> {
                                val msg = item.message
                                val isOutgoing = msg.senderId == currentUserId
                                val flashAlpha = flashAnimatables[msg.id]?.value ?: 0f
                                val isSelected = selectedMessageIds.contains(msg.id)

                                val status = when {
                                    !isOutgoing -> null
                                    else -> MessageStatus.Read
                                }

                                val rawText = msg.text
                                val isOnlyEmojis = emojiOnlyCount(rawText.trim())

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
                                                    .background(colors.senderColor(msg.senderId).copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!msg.senderAvatar.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = msg.senderAvatar,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Text(
                                                        text = msg.senderName.take(1).uppercase(),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.senderColor(msg.senderId)
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
                                                name = msg.senderName,
                                                userId = msg.senderId,
                                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                            )
                                        }

                                        val bubbleModifier = Modifier
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
                                                emoji = rawText.trim(),
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
                                                if (!msg.replyToMessageId.isNullOrBlank()) {
                                                    ReplyQuote(
                                                        sender = msg.replyToSenderName ?: "Reply",
                                                        preview = msg.replyToPreview ?: "Message",
                                                        outgoing = isOutgoing,
                                                        onClick = {
                                                            val targetIdx = timelineItems.indexOfFirst {
                                                                it is GroupTimelineItem.MessageItem && it.message.id == msg.replyToMessageId
                                                            }
                                                            if (targetIdx != -1) {
                                                                val found = timelineItems[targetIdx] as GroupTimelineItem.MessageItem
                                                                coroutineScope.launch {
                                                                    listState.animateScrollToItem(targetIdx)
                                                                    flashMessage(found.message.id)
                                                                }
                                                            }
                                                        }
                                                    )
                                                }

                                                when {
                                                    msg.messageType == "voice" || rawText.startsWith("[voice]") -> {
                                                        val path = msg.localMediaPath ?: rawText.removePrefix("[voice]").substringBefore("|")
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

                                                    msg.messageType == "image" || rawText.startsWith("[img]") || rawText.startsWith("[imgs]") -> {
                                                        val url = msg.localMediaPath ?: rawText.removePrefix("[img]").removePrefix("[imgs]").substringBefore("|")
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
                                                            mentionNames = rawMessages.map { it.senderName }.distinct(),
                                                            accent = colors.accentIn
                                                        )
                                                        MessageText(
                                                            text = annotated,
                                                            time = formatChatTime(msg.timestamp),
                                                            outgoing = isOutgoing,
                                                            status = status,
                                                            onLongPress = {
                                                                if (!isSelectionMode) {
                                                                    selectedMessageForMenu = msg
                                                                }
                                                            },
                                                            onWordClick = { word ->
                                                                activeWordLookup = WordLookupState(
                                                                    word = word,
                                                                    fullSentence = rawText,
                                                                    isLoading = true
                                                                )
                                                                coroutineScope.launch {
                                                                    val res = CloudAiManager.translateMessage(context, "word_${word.hashCode()}", word, "English")
                                                                    res.onSuccess { aiResult ->
                                                                        activeWordLookup = activeWordLookup?.copy(
                                                                            translation = aiResult.text,
                                                                            isLoading = false
                                                                        )
                                                                    }.onFailure {
                                                                        activeWordLookup = activeWordLookup?.copy(
                                                                            translation = "Could not translate word",
                                                                            isLoading = false
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Reaction Row
                                        if (item.reactions.isNotEmpty()) {
                                            ReactionRow(
                                                reactions = item.reactions,
                                                onClick = { emoji ->
                                                    coroutineScope.launch {
                                                        GroupChatRepository.sendGroupReaction(context, group.id, msg.id, emoji)
                                                    }
                                                },
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Scroll to bottom button
                if (listState.canScrollForward) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
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
            }
        }

        // Long-Press Action Overlay Menu
        if (selectedMessageForMenu != null) {
            val selectedMsg = selectedMessageForMenu!!
            MessageLongPressOverlay(
                myReaction = rawReactions.find { it.targetMessageId == selectedMsg.id && it.senderId == currentUserId }?.emoji,
                actions = listOf(
                    MessageAction("reply", "Reply", { ChatIconReply(tint = colors.text) }),
                    MessageAction("copy", "Copy Text", { ChatIconCopy(tint = colors.text) }),
                    MessageAction("translate", "Translate", { ChatIconTranslate(tint = colors.text) }),
                    MessageAction("listen", "Listen", { ChatIconVolume(tint = colors.text) }),
                    MessageAction("forward", "Forward", { ChatIconForward(tint = colors.text) }),
                    MessageAction("select", "Select", { ChatIconCheck(tint = colors.text) }),
                    MessageAction("delete", "Delete", { ChatIconTrash(tint = colors.danger) }, destructive = true)
                ),
                onPickEmoji = { emoji ->
                    coroutineScope.launch {
                        GroupChatRepository.sendGroupReaction(context, group.id, selectedMsg.id, emoji)
                    }
                },
                onMoreEmoji = {
                    isEmojiPickerOpen = true
                },
                onAction = { actionId ->
                    when (actionId) {
                        "reply" -> replyingToMessage = selectedMsg
                        "copy" -> {
                            clipboardManager.setText(AnnotatedString(selectedMsg.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        "translate" -> {
                            coroutineScope.launch {
                                val res = CloudAiManager.translateMessage(context, selectedMsg.id, selectedMsg.text, "English")
                                res.onSuccess {
                                    Toast.makeText(context, "Translation: ${it.text}", Toast.LENGTH_LONG).show()
                                }.onFailure {
                                    Toast.makeText(context, "Translation failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        "listen" -> {
                            ttsEngine?.speak(selectedMsg.text, TextToSpeech.QUEUE_FLUSH, null, "msg_${selectedMsg.id}")
                        }
                        "forward" -> {
                            Toast.makeText(context, "Forwarding message", Toast.LENGTH_SHORT).show()
                        }
                        "select" -> {
                            isSelectionMode = true
                            selectedMessageIds.add(selectedMsg.id)
                        }
                        "delete" -> {
                            Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                    selectedMessageForMenu = null
                },
                onDismiss = { selectedMessageForMenu = null }
            )
        }

        // Telephoto Full-Screen Media Viewer
        if (activeMediaViewerIndex != null && allMediaItems.isNotEmpty()) {
            TelephotoMediaViewerDialog(
                mediaItems = allMediaItems,
                initialIndex = activeMediaViewerIndex!!.coerceIn(0, allMediaItems.size - 1),
                onDismiss = { activeMediaViewerIndex = null }
            )
        }

        // Word Lookup Bottom Sheet
        if (activeWordLookup != null) {
            val lookup = activeWordLookup!!
            ModalBottomSheet(
                onDismissRequest = { activeWordLookup = null },
                containerColor = colors.surface,
                contentColor = colors.text,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChatIconTranslate(tint = colors.accentIn, modifier = Modifier.size(24.dp))
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
                        Text("Save to Vocabulary", fontWeight = FontWeight.Bold)
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
    }
}
