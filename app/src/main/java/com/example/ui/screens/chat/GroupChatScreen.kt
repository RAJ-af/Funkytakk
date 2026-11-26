package com.example.ui.screens.chat

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.ChatPreferencesManager
import com.example.CloudAiManager
import com.example.SupabaseSessionManager
import com.example.crypto.ChatTimingTracker
import com.example.db.LocalGroupConversation
import com.example.db.LocalGroupMessage
import com.example.db.LocalMessageReaction
import com.example.db.LocalMessageStatus
import com.example.repository.GroupChatRepository
import com.example.ui.chat.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

private fun mapGroupStatus(msg: LocalGroupMessage, isOutgoing: Boolean): MessageStatus? {
    if (!isOutgoing) return null
    return when (msg.status) {
        LocalMessageStatus.SENDING -> MessageStatus.Sending
        LocalMessageStatus.FAILED -> MessageStatus.Failed
        else -> MessageStatus.Sent
    }
}

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
        override val id: String = message.clientId?.takeIf { it.isNotBlank() } ?: message.id
    }

    data class MediaAlbumItem(
        val messages: List<LocalGroupMessage>,
        val photos: List<ChatPhotoNode>,
        val position: GroupPosition,
        val reactions: List<ReactionUi>,
        val isFirstInGroup: Boolean,
        val isLastInGroup: Boolean
    ) : GroupTimelineItem() {
        override val id: String = "album_${messages.first().clientId?.takeIf { it.isNotBlank() } ?: messages.first().id}"
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
    com.example.ui.components.SecureWindow(enabled = true)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
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
    val optimisticGroupAll by GroupChatRepository.optimisticGroupMessages.collectAsState()
    var rawReactions by remember { mutableStateOf<List<LocalMessageReaction>>(emptyList()) }
    var currentUserId by remember { mutableStateOf("") }

    var inputText by remember { mutableStateOf("") }
    var isEmojiPickerOpen by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<LocalGroupMessage?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<LocalGroupMessage?>(null) }
    var failedMessageForSheet by remember { mutableStateOf<LocalGroupMessage?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedMessageIds = remember { mutableStateListOf<String>() }

    var activeWordLookup by remember { mutableStateOf<WordLookupState?>(null) }
    var showAiAssistSheet by remember { mutableStateOf(false) }
    var activeMediaViewerIndex by remember { mutableStateOf<Int?>(null) }
    var pendingPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showAttachmentChooser by remember { mutableStateOf(false) }
    var isPreparingMediaLabel by remember { mutableStateOf<String?>(null) }
    var inspectedMemberProfile by remember { mutableStateOf<Triple<String, String, String?>?>(null) }
    val groupMediaStates by GroupChatRepository.groupMediaStates.collectAsState()

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

    // Photo picker launcher -> opens Telegram-style review & caption sheet before sending
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val merged = (pendingPhotoUris + uris).distinct().take(10)
            pendingPhotoUris = merged
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isPreparingMediaLabel = "Compressing video..."
                val res = ChatVideoHelper.compressAndPrepareVideo(context, uri)
                isPreparingMediaLabel = null
                res.onSuccess { prep ->
                    val replySnapshot = replyingToMessage
                    val tapMs = ChatTimingTracker.nowMs()
                    replyingToMessage = null
                    GroupChatRepository.enqueueSendGroupMediaMessage(
                        context = context,
                        groupId = group.id,
                        rawBytes = prep.bytes,
                        messageType = "video",
                        mimeType = prep.mimeType,
                        durationSeconds = prep.durationSeconds,
                        replyToMessage = replySnapshot,
                        tapElapsedMs = tapMs
                    )
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "Could not prepare video", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isPreparingMediaLabel = "Preparing audio..."
                val res = ChatVideoHelper.prepareAudioFile(context, uri)
                isPreparingMediaLabel = null
                res.onSuccess { prep ->
                    val replySnapshot = replyingToMessage
                    val tapMs = ChatTimingTracker.nowMs()
                    replyingToMessage = null
                    GroupChatRepository.enqueueSendGroupMediaMessage(
                        context = context,
                        groupId = group.id,
                        rawBytes = prep.bytes,
                        messageType = "audio",
                        mimeType = prep.mimeType,
                        durationSeconds = prep.durationSeconds,
                        caption = prep.displayName,
                        replyToMessage = replySnapshot,
                        tapElapsedMs = tapMs
                    )
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "Could not prepare audio file", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    DisposableEffect(group.id) {
        GroupChatRepository.setActiveOpenGroup(group.id)
        onDispose {
            GroupChatRepository.setActiveOpenGroup(null)
        }
    }

    val vaultStatus by com.example.crypto.VaultSecurityManager.vaultStatus.collectAsState()

    // Observe messages and reactions
    LaunchedEffect(group.id, vaultStatus) {
        if (vaultStatus != com.example.crypto.VaultStatus.UNLOCKED) return@LaunchedEffect
        com.example.service.PushNotificationManager.cancelNotificationForChat(context, groupId = group.id)
        GroupChatRepository.markGroupRead(group.id)
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

    val mergedMessages = remember(rawMessages, optimisticGroupAll, group.id) {
        val existingKeys = HashSet<String>(rawMessages.size * 2)
        rawMessages.forEach { m ->
            existingKeys.add(m.id)
            m.clientId?.takeIf { it.isNotBlank() }?.let { existingKeys.add(it) }
        }
        val extraOptimistic = optimisticGroupAll.filter { opt ->
            val optKey = opt.clientId?.takeIf { it.isNotBlank() } ?: opt.id
            opt.groupId == group.id && !existingKeys.contains(opt.id) && !existingKeys.contains(optKey)
        }
        if (extraOptimistic.isEmpty()) {
            rawMessages
        } else {
            (rawMessages + extraOptimistic).sortedBy { it.timestamp }
        }
    }

    val seenBubbleKeys = remember(group.id) { HashSet<String>() }
    var hasInitialHistoryLoaded by remember(group.id) { mutableStateOf(false) }
    LaunchedEffect(mergedMessages.size) {
        if (!hasInitialHistoryLoaded && mergedMessages.isNotEmpty()) {
            mergedMessages.forEach { m ->
                seenBubbleKeys.add(m.clientId?.takeIf { it.isNotBlank() } ?: m.id)
            }
            hasInitialHistoryLoaded = true
        }
    }

    LaunchedEffect(mergedMessages) {
        val senderIds = mergedMessages.map { it.senderId }.filter { it.isNotBlank() }.distinct()
        if (senderIds.isNotEmpty()) {
            com.example.store.CosmeticsManager.batchFetchUserCosmetics(context, senderIds)
        }
    }

    ProvideChatTheme(
        preset = chatPreset,
        darkTheme = false,
        textScale = chatTextScale
    ) {
        val colors = LocalChatColors.current

        val timelineItems = remember(mergedMessages, rawReactions, currentUserId, groupMediaStates) {
            val sorted = mergedMessages.sortedBy { it.timestamp }
            val items = mutableListOf<GroupTimelineItem>()
            items.add(GroupTimelineItem.SecurityHeader("Messages and media in this group are end-to-end encrypted."))

            var prevDateLabel = ""
            var i = 0
            while (i < sorted.size) {
                val msg = sorted[i]
                val dateLabel = formatChatDateChip(msg.timestamp)
                if (dateLabel != prevDateLabel) {
                    items.add(GroupTimelineItem.DateHeader(dateLabel))
                    prevDateLabel = dateLabel
                }

                if (msg.messageType == "system") {
                    items.add(GroupTimelineItem.SystemEvent(msg.id, msg.text))
                    i++
                    continue
                }

                // Group consecutive same-sender images (<= 60s, max 10) into a Telegram album collage
                if (isGroupImageMessage(msg)) {
                    val albumMsgs = mutableListOf(msg)
                    var j = i + 1
                    while (j < sorted.size && albumMsgs.size < 10) {
                        val next = sorted[j]
                        if (next.messageType != "system" &&
                            isGroupImageMessage(next) &&
                            next.senderId == msg.senderId &&
                            formatChatDateChip(next.timestamp) == dateLabel &&
                            (next.timestamp - albumMsgs.last().timestamp) <= 60_000L
                        ) {
                            albumMsgs.add(next)
                            j++
                        } else {
                            break
                        }
                    }

                    val lastInAlbum = albumMsgs.last()
                    val isOlderSameSender = i > 0 &&
                        sorted[i - 1].messageType != "system" &&
                        sorted[i - 1].senderId == msg.senderId &&
                        (msg.timestamp - sorted[i - 1].timestamp) <= 2 * 60 * 1000L &&
                        formatChatDateChip(sorted[i - 1].timestamp) == dateLabel

                    val isNewerSameSender = j < sorted.size &&
                        sorted[j].messageType != "system" &&
                        sorted[j].senderId == lastInAlbum.senderId &&
                        (sorted[j].timestamp - lastInAlbum.timestamp) <= 2 * 60 * 1000L &&
                        formatChatDateChip(sorted[j].timestamp) == dateLabel

                    val position = computeGroupPosition(isOlderSameSender, isNewerSameSender)
                    val reactionUis = rawReactions.filter { it.targetMessageId == msg.id }
                        .groupBy { it.emoji }
                        .map { (emoji, list) ->
                            ReactionUi(
                                emoji = emoji,
                                count = list.size,
                                mine = list.any { it.senderId == currentUserId }
                            )
                        }
                    val photoNodes = albumMsgs.map { m ->
                        parseGroupPhotoNode(
                            msg = m,
                            isOutgoing = m.senderId == currentUserId,
                            groupState = groupMediaStates[m.id]
                        )
                    }
                    items.add(
                        GroupTimelineItem.MediaAlbumItem(
                            messages = albumMsgs,
                            photos = photoNodes,
                            position = position,
                            reactions = reactionUis,
                            isFirstInGroup = !isOlderSameSender,
                            isLastInGroup = !isNewerSameSender
                        )
                    )
                    i = j
                    continue
                }

                val isOlderSameSender = i > 0 &&
                    sorted[i - 1].messageType != "system" &&
                    sorted[i - 1].senderId == msg.senderId &&
                    (msg.timestamp - sorted[i - 1].timestamp) <= 2 * 60 * 1000L &&
                    formatChatDateChip(sorted[i - 1].timestamp) == dateLabel

                val isNewerSameSender = i < sorted.size - 1 &&
                    sorted[i + 1].messageType != "system" &&
                    sorted[i + 1].senderId == msg.senderId &&
                    (sorted[i + 1].timestamp - msg.timestamp) <= 2 * 60 * 1000L &&
                    formatChatDateChip(sorted[i + 1].timestamp) == dateLabel

                val position = computeGroupPosition(isOlderSameSender, isNewerSameSender)
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
                        isFirstInGroup = !isOlderSameSender,
                        isLastInGroup = !isNewerSameSender
                    )
                )
                i++
            }
            items
        }

        LaunchedEffect(timelineItems.size) {
            if (timelineItems.isNotEmpty()) {
                listState.scrollToItem(timelineItems.size - 1)
            }
        }

        val allMediaItems = remember(mergedMessages, currentUserId, groupMediaStates) {
            mergedMessages.sortedBy { it.timestamp }
                .mapNotNull { msg ->
                    when {
                        isGroupImageMessage(msg) -> {
                            val node = parseGroupPhotoNode(
                                msg = msg,
                                isOutgoing = msg.senderId == currentUserId,
                                groupState = groupMediaStates[msg.id]
                            )
                            if (node.source.isBlank()) null else ChatViewerMediaItem(
                                id = msg.id,
                                url = node.source,
                                senderName = if (msg.senderId == currentUserId) "You" else msg.senderName,
                                dateText = node.timeText,
                                caption = node.caption,
                                isVideo = false
                            )
                        }
                        msg.messageType == "video" || msg.text.startsWith("[vid]") -> {
                            val spec = parseGroupVideoSpec(msg, groupMediaStates[msg.id])
                            val path = spec.localPath
                            if (path.isNullOrBlank()) null else ChatViewerMediaItem(
                                id = msg.id,
                                url = path,
                                senderName = if (msg.senderId == currentUserId) "You" else msg.senderName,
                                dateText = formatChatTime(msg.timestamp),
                                caption = spec.caption,
                                isVideo = true
                            )
                        }
                        else -> null
                    }
                }
        }

        val unreadCount = remember(mergedMessages, currentUserId) {
            mergedMessages.count { it.senderId != currentUserId }
        }

        BackHandler(enabled = showAttachmentChooser || inspectedMemberProfile != null || isEmojiPickerOpen || failedMessageForSheet != null || isSelectionMode || selectedMessageForMenu != null) {
            if (showAttachmentChooser) {
                showAttachmentChooser = false
            } else if (inspectedMemberProfile != null) {
                inspectedMemberProfile = null
            } else if (isEmojiPickerOpen) {
                isEmojiPickerOpen = false
            } else if (failedMessageForSheet != null) {
                failedMessageForSheet = null
            } else if (isSelectionMode) {
                isSelectionMode = false
                selectedMessageIds.clear()
            } else {
                selectedMessageForMenu = null
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                                    val joined = mergedMessages.filter { it.id in selectedMessageIds }.joinToString("\n") { it.text }
                                    clipboardManager.setText(AnnotatedString(joined))
                                    Toast.makeText(context, "Copied ${selectedMessageIds.size} messages", Toast.LENGTH_SHORT).show()
                                }
                                "forward" -> {
                                    Toast.makeText(context, "Forward ${selectedMessageIds.size} messages", Toast.LENGTH_SHORT).show()
                                }
                                "delete" -> {
                                    val toDelete = selectedMessageIds.toList()
                                    coroutineScope.launch {
                                        toDelete.forEach { id ->
                                            GroupChatRepository.deleteGroupMessage(context, id)
                                        }
                                        Toast.makeText(context, "Deleted ${toDelete.size} messages", Toast.LENGTH_SHORT).show()
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
                        color = colors.surface.copy(alpha = 0.96f),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("group_chat_back_button")
                                ) {
                                    ChatIconBack(tint = colors.text, modifier = Modifier.size(22.dp))
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(onClick = onNavigateToInfo)
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(colors.accentIn.copy(alpha = 0.15f)),
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

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = group.name,
                                                color = colors.text,
                                                fontSize = 16f.chatSp(),
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(99.dp),
                                                color = colors.accentIn.copy(alpha = 0.12f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    ChatIconLock(tint = colors.accentIn, modifier = Modifier.size(10.dp))
                                                    Text(
                                                        text = "E2EE",
                                                        color = colors.accentIn,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${group.memberCount} members",
                                            color = colors.textSecondary,
                                            fontSize = 11.5f.chatSp(),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onNavigateToManageMembers(true) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    ChatIconAddUser(tint = colors.text, modifier = Modifier.size(21.dp))
                                }
                                IconButton(
                                    onClick = onNavigateToSettings,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    ChatIconSettings(tint = colors.text, modifier = Modifier.size(21.dp))
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(0.6.dp).background(colors.hairline))
                        }
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
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
                                val tapMs = ChatTimingTracker.nowMs()
                                inputText = ""
                                replyingToMessage = null
                                isEmojiPickerOpen = false
                                GroupChatRepository.enqueueSendGroupMessage(
                                    context = context,
                                    groupId = group.id,
                                    text = textToSend,
                                    replyToMessage = reply,
                                    tapElapsedMs = tapMs
                                )
                                if (timelineItems.isNotEmpty()) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(timelineItems.size - 1)
                                    }
                                }
                            }
                        },
                        onAttach = {
                            isEmojiPickerOpen = false
                            showAttachmentChooser = true
                        },
                        onEmojiToggle = {
                            isEmojiPickerOpen = !isEmojiPickerOpen
                        },
                        isEmojiOpen = isEmojiPickerOpen,
                        showAiButton = isAiConsentGranted,
                        onAi = { showAiAssistSheet = true },
                        topSlot = if (replyingToMessage != null || isPreparingMediaLabel != null) {
                            {
                                Column {
                                    if (isPreparingMediaLabel != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = colors.accentIn
                                            )
                                            Text(
                                                text = isPreparingMediaLabel ?: "",
                                                color = colors.textSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    if (replyingToMessage != null) {
                                        ReplyPreviewStrip(
                                            title = replyingToMessage!!.senderName,
                                            preview = ChatAnimatedStickers.readableLabel(replyingToMessage!!.text).take(60),
                                            onClose = { replyingToMessage = null }
                                        )
                                    }
                                }
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
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val tapMs = ChatTimingTracker.nowMs()
                                            GroupChatRepository.enqueueSendGroupMediaMessage(
                                                context = context,
                                                groupId = group.id,
                                                rawBytes = result.bytes,
                                                messageType = "voice",
                                                mimeType = "audio/mp4",
                                                durationSeconds = (result.durationMs / 1000).toInt().coerceAtLeast(1),
                                                tapElapsedMs = tapMs
                                            )
                                            if (timelineItems.isNotEmpty()) {
                                                coroutineScope.launch {
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
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val tapMs = ChatTimingTracker.nowMs()
                                            GroupChatRepository.enqueueSendGroupMediaMessage(
                                                context = context,
                                                groupId = group.id,
                                                rawBytes = result.bytes,
                                                messageType = "voice",
                                                mimeType = "audio/mp4",
                                                durationSeconds = (result.durationMs / 1000).toInt().coerceAtLeast(1),
                                                tapElapsedMs = tapMs
                                            )
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
                            },
                            onAnimatedStickerSelect = { sticker ->
                                val reply = replyingToMessage
                                val tapMs = ChatTimingTracker.nowMs()
                                replyingToMessage = null
                                isEmojiPickerOpen = false
                                GroupChatRepository.enqueueSendGroupMessage(
                                    context = context,
                                    groupId = group.id,
                                    text = ChatAnimatedStickers.encode(sticker),
                                    replyToMessage = reply,
                                    tapElapsedMs = tapMs
                                )
                                if (timelineItems.isNotEmpty()) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(timelineItems.size - 1)
                                    }
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
            ) {
                TelegramAmbientWallpaper(colors = colors, modifier = Modifier.fillMaxSize())
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
                            is GroupTimelineItem.MediaAlbumItem -> {
                                val firstMsg = item.messages.first()
                                val isOutgoing = firstMsg.senderId == currentUserId
                                val flashAlpha = flashAnimatables[firstMsg.id]?.value ?: 0f
                                val isSelected = item.messages.any { selectedMessageIds.contains(it.id) }
                                val failedMsgInAlbum = item.messages.firstOrNull { it.status == LocalMessageStatus.FAILED }
                                val status = if (!isOutgoing) {
                                    null
                                } else {
                                    when {
                                        failedMsgInAlbum != null -> MessageStatus.Failed
                                        item.messages.any { it.status == LocalMessageStatus.SENDING } -> MessageStatus.Sending
                                        else -> MessageStatus.Sent
                                    }
                                }
                                val itemKey = firstMsg.clientId?.takeIf { it.isNotBlank() } ?: firstMsg.id
                                val isNewItem = remember(itemKey) { hasInitialHistoryLoaded && seenBubbleKeys.add(itemKey) }
                                SideEffect {
                                    if (BuildConfig.DEBUG && isOutgoing) {
                                        item.messages.forEach { m ->
                                            ChatTimingTracker.markBubbleShown("GROUP", m.clientId ?: m.id)
                                        }
                                    }
                                }

                                AnimatedBubbleEntrance(animateEntrance = isNewItem, outgoing = isOutgoing) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isSelectionMode) {
                                                Modifier.clickable {
                                                    val albumIds = item.messages.map { it.id }
                                                    if (isSelected) selectedMessageIds.removeAll(albumIds)
                                                    else albumIds.forEach { if (!selectedMessageIds.contains(it)) selectedMessageIds.add(it) }
                                                }
                                            } else Modifier
                                        )
                                        .padding(
                                            start = if (isSelectionMode) 10.dp else if (isOutgoing) 48.dp else 12.dp,
                                            end = if (isOutgoing) 12.dp else 48.dp,
                                            top = if (item.position == GroupPosition.First || item.position == GroupPosition.Single) 6.dp else 2.dp,
                                            bottom = 2.dp
                                        ),
                                    horizontalArrangement = if (isSelectionMode) Arrangement.Start else if (isOutgoing) Arrangement.End else Arrangement.Start,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    if (isSelectionMode) {
                                        SelectionCheckBadge(
                                            selected = isSelected,
                                            modifier = Modifier
                                                .align(Alignment.CenterVertically)
                                                .padding(end = 8.dp)
                                        )
                                        if (isOutgoing) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                    if (!isOutgoing) {
                                        if (item.isLastInGroup) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.senderColor(firstMsg.senderId).copy(alpha = 0.2f))
                                                    .clickable(enabled = firstMsg.senderId.isNotBlank()) {
                                                        inspectedMemberProfile = Triple(firstMsg.senderId, firstMsg.senderName, firstMsg.senderAvatar)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!firstMsg.senderAvatar.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = firstMsg.senderAvatar,
                                                        contentDescription = "View ${firstMsg.senderName} profile",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Text(
                                                        text = firstMsg.senderName.take(1).uppercase(),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.senderColor(firstMsg.senderId)
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
                                        if (!isOutgoing && (item.position == GroupPosition.Single || item.position == GroupPosition.First)) {
                                            SenderName(
                                                name = firstMsg.senderName,
                                                userId = firstMsg.senderId,
                                                modifier = Modifier
                                                    .padding(start = 4.dp, bottom = 2.dp)
                                                    .clickable(enabled = firstMsg.senderId.isNotBlank()) {
                                                        inspectedMemberProfile = Triple(firstMsg.senderId, firstMsg.senderName, firstMsg.senderAvatar)
                                                    }
                                            )
                                        }

                                        MessageBubble(
                                            outgoing = isOutgoing,
                                            position = item.position,
                                            highlighted = flashAlpha > 0f,
                                            selected = isSelected,
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            TelegramMediaAlbumContent(
                                                photos = item.photos,
                                                outgoing = isOutgoing,
                                                position = item.position,
                                                status = status,
                                                replyQuote = if (!firstMsg.replyToMessageId.isNullOrBlank()) {
                                                    {
                                                        ReplyQuote(
                                                            sender = firstMsg.replyToSenderName ?: "Reply",
                                                            preview = firstMsg.replyToPreview ?: "Message",
                                                            outgoing = isOutgoing,
                                                            onClick = {
                                                                val targetIdx = timelineItems.indexOfFirst { tl ->
                                                                    (tl is GroupTimelineItem.MessageItem && tl.message.id == firstMsg.replyToMessageId) ||
                                                                        (tl is GroupTimelineItem.MediaAlbumItem && tl.messages.any { it.id == firstMsg.replyToMessageId })
                                                                }
                                                                if (targetIdx != -1) {
                                                                    val flashTargetId = when (val found = timelineItems[targetIdx]) {
                                                                        is GroupTimelineItem.MessageItem -> found.message.id
                                                                        is GroupTimelineItem.MediaAlbumItem -> found.messages.first().id
                                                                        else -> null
                                                                    }
                                                                    coroutineScope.launch {
                                                                        listState.animateScrollToItem(targetIdx)
                                                                        if (flashTargetId != null) flashMessage(flashTargetId)
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }
                                                } else null,
                                                onPhotoClick = { _, photo ->
                                                    val clickedMsg = item.messages.find { it.id == photo.id } ?: firstMsg
                                                    if (isSelectionMode) {
                                                        val clickedId = photo.id
                                                        if (selectedMessageIds.contains(clickedId)) selectedMessageIds.remove(clickedId)
                                                        else selectedMessageIds.add(clickedId)
                                                    } else if (clickedMsg.status == LocalMessageStatus.FAILED) {
                                                        failedMessageForSheet = clickedMsg
                                                    } else if (failedMsgInAlbum != null) {
                                                        failedMessageForSheet = failedMsgInAlbum
                                                    } else {
                                                        val idx = allMediaItems.indexOfFirst { it.id == photo.id }
                                                        if (idx >= 0) activeMediaViewerIndex = idx
                                                    }
                                                },
                                                onPhotoLongPress = { photo ->
                                                    if (!isSelectionMode) {
                                                        selectedMessageForMenu = item.messages.find { it.id == photo.id } ?: firstMsg
                                                    }
                                                },
                                                onDownloadClick = { photo ->
                                                    val targetMsg = item.messages.find { it.id == photo.id }
                                                    if (targetMsg != null) {
                                                        coroutineScope.launch {
                                                            val res = GroupChatRepository.downloadGroupMessageMediaOnDemand(context, targetMsg)
                                                            if (res.isFailure) {
                                                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Could not load photo", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }

                                        if (item.reactions.isNotEmpty()) {
                                            ReactionRow(
                                                reactions = item.reactions,
                                                onClick = { emoji ->
                                                    val isMine = item.reactions.any { it.emoji == emoji && it.mine }
                                                    coroutineScope.launch {
                                                        val res = GroupChatRepository.sendGroupReaction(
                                                            context = context,
                                                            groupId = group.id,
                                                            targetMessageId = firstMsg.id,
                                                            emoji = emoji,
                                                            op = if (isMine) "remove" else "set"
                                                        )
                                                        if (res.isFailure) {
                                                            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Could not update reaction", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                                }
                            }
                            is GroupTimelineItem.MessageItem -> {
                                val msg = item.message
                                val isOutgoing = msg.senderId == currentUserId
                                val flashAlpha = flashAnimatables[msg.id]?.value ?: 0f
                                val isSelected = selectedMessageIds.contains(msg.id)
                                val status = mapGroupStatus(msg, isOutgoing)
                                val itemKey = msg.clientId?.takeIf { it.isNotBlank() } ?: msg.id
                                val isNewItem = remember(itemKey) { hasInitialHistoryLoaded && seenBubbleKeys.add(itemKey) }
                                SideEffect {
                                    if (BuildConfig.DEBUG && isOutgoing) {
                                        ChatTimingTracker.markBubbleShown("GROUP", msg.clientId ?: msg.id)
                                    }
                                }

                                val rawText = msg.text
                                val isOnlyEmojis = emojiOnlyCount(rawText.trim())
                                val parsedSticker = ChatAnimatedStickers.parse(rawText)

                                AnimatedBubbleEntrance(animateEntrance = isNewItem, outgoing = isOutgoing) {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isSelectionMode) {
                                                Modifier.clickable {
                                                    if (isSelected) selectedMessageIds.remove(msg.id)
                                                    else selectedMessageIds.add(msg.id)
                                                }
                                            } else Modifier
                                        )
                                        .padding(
                                            start = if (isSelectionMode) 10.dp else if (isOutgoing) 48.dp else 12.dp,
                                            end = if (isOutgoing) 12.dp else 48.dp,
                                            top = if (item.position == GroupPosition.First || item.position == GroupPosition.Single) 6.dp else 2.dp,
                                            bottom = 2.dp
                                        ),
                                    horizontalArrangement = if (isSelectionMode) Arrangement.Start else if (isOutgoing) Arrangement.End else Arrangement.Start,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    if (isSelectionMode) {
                                        SelectionCheckBadge(
                                            selected = isSelected,
                                            modifier = Modifier
                                                .align(Alignment.CenterVertically)
                                                .padding(end = 8.dp)
                                        )
                                        if (isOutgoing) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                    // 28dp Avatar only beside last message of group
                                    if (!isOutgoing) {
                                        if (item.isLastInGroup) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.senderColor(msg.senderId).copy(alpha = 0.2f))
                                                    .clickable(enabled = msg.senderId.isNotBlank()) {
                                                        inspectedMemberProfile = Triple(msg.senderId, msg.senderName, msg.senderAvatar)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!msg.senderAvatar.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = msg.senderAvatar,
                                                        contentDescription = "View ${msg.senderName} profile",
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
                                                modifier = Modifier
                                                    .padding(start = 4.dp, bottom = 2.dp)
                                                    .clickable(enabled = msg.senderId.isNotBlank()) {
                                                        inspectedMemberProfile = Triple(msg.senderId, msg.senderName, msg.senderAvatar)
                                                    }
                                            )
                                        }

                                        val bubbleModifier = Modifier
                                            .pointerInput(msg.id, msg.status, isSelectionMode) {
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
                                                        } else if (msg.status == LocalMessageStatus.FAILED) {
                                                            failedMessageForSheet = msg
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
                                        } else if (parsedSticker != null) {
                                            Column(
                                                modifier = bubbleModifier.padding(vertical = 2.dp),
                                                horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
                                            ) {
                                                AnimatedStickerGraphic(
                                                    sticker = parsedSticker.sticker,
                                                    size = 124.dp,
                                                    emojiFontSizeSp = 52
                                                )
                                                MessageMeta(
                                                    time = formatChatTime(msg.timestamp),
                                                    outgoing = isOutgoing,
                                                    status = status,
                                                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                                                )
                                            }
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
                                                        preview = ChatAnimatedStickers.readableLabel(msg.replyToPreview ?: "Message"),
                                                        outgoing = isOutgoing,
                                                        onClick = {
                                                            val targetIdx = timelineItems.indexOfFirst { tl ->
                                                                (tl is GroupTimelineItem.MessageItem && tl.message.id == msg.replyToMessageId) ||
                                                                    (tl is GroupTimelineItem.MediaAlbumItem && tl.messages.any { it.id == msg.replyToMessageId })
                                                            }
                                                            if (targetIdx != -1) {
                                                                val flashTargetId = when (val found = timelineItems[targetIdx]) {
                                                                    is GroupTimelineItem.MessageItem -> found.message.id
                                                                    is GroupTimelineItem.MediaAlbumItem -> found.messages.first().id
                                                                    else -> null
                                                                }
                                                                coroutineScope.launch {
                                                                    listState.animateScrollToItem(targetIdx)
                                                                    if (flashTargetId != null) flashMessage(flashTargetId)
                                                                }
                                                            }
                                                        }
                                                    )
                                                }

                                                when {
                                                    msg.messageType == "voice" || rawText.startsWith("[voice]") -> {
                                                        val resolvedPath = msg.localMediaPath?.takeIf { it.isNotBlank() }
                                                            ?: rawText.removePrefix("[voice]").substringBefore("|")
                                                                .takeIf { it.startsWith("file://") || it.startsWith("/") || it.startsWith("http") }
                                                                .orEmpty()
                                                        val durationSec = rawText.substringAfter("|", "0").toIntOrNull() ?: 0
                                                        val mediaStatePair = groupMediaStates[msg.id]
                                                        val dlState = mediaStatePair?.first
                                                        val hasValidLocal = resolvedPath.isNotBlank() && resolvedPath != "downloading..." &&
                                                            (resolvedPath.startsWith("http") || java.io.File(resolvedPath.removePrefix("file://")).let { it.exists() && it.length() > 0L })
                                                        val isExpiredVoice = !hasValidLocal && dlState == com.example.db.DmMediaDownloadState.EXPIRED
                                                        val isFailedVoice = !hasValidLocal && dlState == com.example.db.DmMediaDownloadState.FAILED
                                                        val isDownloadingState = !hasValidLocal && dlState == com.example.db.DmMediaDownloadState.DOWNLOADING
                                                        val isPlayingThis = voicePlayer.currentPlayingId == msg.id && voicePlayer.isPlaying
                                                        var isDownloadingVoice by remember(msg.id) { mutableStateOf(false) }

                                                        fun playOrFetchGroupVoice(seekFrac: Float? = null) {
                                                            if (isExpiredVoice) {
                                                                Toast.makeText(
                                                                    context,
                                                                    mediaStatePair?.second ?: "Media expired. Ask them to send it again.",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                return
                                                            }
                                                            if (!hasValidLocal) {
                                                                if (isDownloadingVoice) return
                                                                isDownloadingVoice = true
                                                                coroutineScope.launch {
                                                                    val dl = GroupChatRepository.downloadGroupMessageMediaOnDemand(context, msg)
                                                                    isDownloadingVoice = false
                                                                    dl.onSuccess { localPath ->
                                                                        voicePlayer.playOrPause(
                                                                            messageId = msg.id,
                                                                            audioSource = localPath,
                                                                            fallbackDurationMs = durationSec * 1000L,
                                                                            seekFraction = seekFrac,
                                                                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                                                        )
                                                                    }.onFailure { err ->
                                                                        Toast.makeText(context, err.message ?: "Could not load voice note", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            } else {
                                                                voicePlayer.playOrPause(
                                                                    messageId = msg.id,
                                                                    audioSource = resolvedPath,
                                                                    fallbackDurationMs = durationSec * 1000L,
                                                                    seekFraction = seekFrac,
                                                                    onError = { err ->
                                                                        coroutineScope.launch {
                                                                            val dl = GroupChatRepository.downloadGroupMessageMediaOnDemand(context, msg)
                                                                            dl.onSuccess { localPath ->
                                                                                voicePlayer.playOrPause(
                                                                                    messageId = msg.id,
                                                                                    audioSource = localPath,
                                                                                    fallbackDurationMs = durationSec * 1000L,
                                                                                    seekFraction = seekFrac,
                                                                                    onError = { e2 -> Toast.makeText(context, e2, Toast.LENGTH_SHORT).show() }
                                                                                )
                                                                            }.onFailure {
                                                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                                            }
                                                                        }
                                                                    }
                                                                )
                                                            }
                                                        }

                                                        Column {
                                                            VoiceNoteContent(
                                                                outgoing = isOutgoing,
                                                                amplitudes = remember(msg.id) { List(48) { (40 + (it * 7) % 180) } },
                                                                durationMs = durationSec * 1000L,
                                                                positionMs = if (voicePlayer.currentPlayingId == msg.id) voicePlayer.positionMs else 0L,
                                                                isPlaying = isPlayingThis,
                                                                isLoading = isDownloadingVoice || isDownloadingState || (voicePlayer.currentPlayingId == msg.id && voicePlayer.isLoading),
                                                                isUnread = !isOutgoing && !voicePlayer.hasPlayed(msg.id),
                                                                speed = voicePlayer.speed,
                                                                onPlayPause = { playOrFetchGroupVoice(null) },
                                                                onSeek = { seekFrac -> playOrFetchGroupVoice(seekFrac) },
                                                                onSpeedClick = { voicePlayer.cycleSpeed() }
                                                            )
                                                            if (!hasValidLocal && (isExpiredVoice || isFailedVoice || isDownloadingState)) {
                                                                Text(
                                                                    text = when {
                                                                        isExpiredVoice -> mediaStatePair?.second ?: "Media expired. Ask them to send it again."
                                                                        isFailedVoice -> "Failed. Tap to retry"
                                                                        else -> "Downloading..."
                                                                    },
                                                                    color = if (isFailedVoice) colors.danger else if (isOutgoing) colors.textSecondaryOut else colors.textSecondary,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    modifier = Modifier.padding(top = 3.dp, start = 52.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    msg.messageType == "video" || rawText.startsWith("[vid]") -> {
                                                        val videoSpec = parseGroupVideoSpec(msg, groupMediaStates[msg.id])
                                                        VideoAttachmentBubble(
                                                            localPath = videoSpec.localPath,
                                                            durationSec = videoSpec.durationSec,
                                                            caption = videoSpec.caption,
                                                            timeText = formatChatTime(msg.timestamp),
                                                            outgoing = isOutgoing,
                                                            status = status,
                                                            downloadState = videoSpec.downloadState,
                                                            failReason = videoSpec.failReason,
                                                            onPlayClick = {
                                                                if (isSelectionMode) {
                                                                    if (isSelected) selectedMessageIds.remove(msg.id)
                                                                    else selectedMessageIds.add(msg.id)
                                                                } else {
                                                                    val idx = allMediaItems.indexOfFirst { it.id == msg.id }
                                                                    if (idx >= 0) {
                                                                        activeMediaViewerIndex = idx
                                                                    }
                                                                }
                                                            },
                                                            onDownloadClick = {
                                                                coroutineScope.launch {
                                                                    val dl = GroupChatRepository.downloadGroupMessageMediaOnDemand(context, msg)
                                                                    if (dl.isFailure) {
                                                                        Toast.makeText(context, dl.exceptionOrNull()?.message ?: "Could not load video", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            },
                                                            onLongPress = {
                                                                if (!isSelectionMode) {
                                                                    selectedMessageForMenu = msg
                                                                }
                                                            }
                                                        )
                                                    }

                                                    msg.messageType == "audio" || rawText.startsWith("[audio]") -> {
                                                        val audioSpec = parseGroupAudioSpec(msg, groupMediaStates[msg.id])
                                                        val isPlayingThis = voicePlayer.currentPlayingId == msg.id && voicePlayer.isPlaying
                                                        var isDownloadingAudio by remember(msg.id) { mutableStateOf(false) }
                                                        AudioAttachmentBubble(
                                                            title = audioSpec.title,
                                                            durationMs = audioSpec.durationSec * 1000L,
                                                            positionMs = if (voicePlayer.currentPlayingId == msg.id) voicePlayer.positionMs else 0L,
                                                            isPlaying = isPlayingThis,
                                                            isLoading = isDownloadingAudio || (voicePlayer.currentPlayingId == msg.id && voicePlayer.isLoading),
                                                            speed = voicePlayer.speed,
                                                            outgoing = isOutgoing,
                                                            downloadState = audioSpec.downloadState,
                                                            failReason = audioSpec.failReason,
                                                            hasLocalFile = !audioSpec.localPath.isNullOrBlank(),
                                                            onPlayPause = {
                                                                val src = audioSpec.localPath ?: return@AudioAttachmentBubble
                                                                voicePlayer.playOrPause(
                                                                    messageId = msg.id,
                                                                    audioSource = src,
                                                                    fallbackDurationMs = audioSpec.durationSec * 1000L,
                                                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                                                )
                                                            },
                                                            onDownloadClick = {
                                                                if (isDownloadingAudio) return@AudioAttachmentBubble
                                                                isDownloadingAudio = true
                                                                coroutineScope.launch {
                                                                    val dl = GroupChatRepository.downloadGroupMessageMediaOnDemand(context, msg)
                                                                    isDownloadingAudio = false
                                                                    dl.onFailure { err ->
                                                                        Toast.makeText(context, err.message ?: "Could not load audio", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            },
                                                            onSpeedClick = { voicePlayer.cycleSpeed() }
                                                        )
                                                    }

                                                    msg.messageType == "image" || rawText.startsWith("[img]") || rawText.startsWith("[imgs]") -> {
                                                        val singleNode = parseGroupPhotoNode(
                                                            msg = msg,
                                                            isOutgoing = isOutgoing,
                                                            groupState = groupMediaStates[msg.id]
                                                        )
                                                        TelegramMediaAlbumContent(
                                                            photos = listOf(singleNode),
                                                            outgoing = isOutgoing,
                                                            position = item.position,
                                                            status = status,
                                                            onPhotoClick = { _, _ ->
                                                                if (isSelectionMode) {
                                                                    if (isSelected) selectedMessageIds.remove(msg.id)
                                                                    else selectedMessageIds.add(msg.id)
                                                                } else {
                                                                    val idx = allMediaItems.indexOfFirst { it.id == msg.id }
                                                                    if (idx >= 0) activeMediaViewerIndex = idx
                                                                }
                                                            },
                                                            onPhotoLongPress = {
                                                                if (!isSelectionMode) selectedMessageForMenu = msg
                                                            },
                                                            onDownloadClick = {
                                                                coroutineScope.launch {
                                                                    val dl = GroupChatRepository.downloadGroupMessageMediaOnDemand(context, msg)
                                                                    if (dl.isFailure) {
                                                                        Toast.makeText(context, dl.exceptionOrNull()?.message ?: "Could not load photo", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }

                                                    else -> {
                                                        val annotated = buildMessageText(
                                                            raw = rawText,
                                                            mentionNames = mergedMessages.map { it.senderName }.distinct(),
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
                                                                if (isSelectionMode) {
                                                                    if (isSelected) selectedMessageIds.remove(msg.id)
                                                                    else selectedMessageIds.add(msg.id)
                                                                    return@MessageText
                                                                }
                                                                if (msg.status == LocalMessageStatus.FAILED) {
                                                                    failedMessageForSheet = msg
                                                                    return@MessageText
                                                                }
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
                                                    val isMine = item.reactions.any { it.emoji == emoji && it.mine }
                                                    coroutineScope.launch {
                                                        val res = GroupChatRepository.sendGroupReaction(
                                                            context = context,
                                                            groupId = group.id,
                                                            targetMessageId = msg.id,
                                                            emoji = emoji,
                                                            op = if (isMine) "remove" else "set"
                                                        )
                                                        if (res.isFailure) {
                                                            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Could not update reaction", Toast.LENGTH_SHORT).show()
                                                        }
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
            val currentMyReaction = rawReactions.find { it.targetMessageId == selectedMsg.id && it.senderId == currentUserId }?.emoji
            val isImageMsg = isGroupImageMessage(selectedMsg)
            val isVideoMsg = selectedMsg.messageType == "video" || selectedMsg.text.startsWith("[vid]")
            val isAudioMsg = selectedMsg.messageType == "audio" || selectedMsg.text.startsWith("[audio]")
            val isVoiceMsg = selectedMsg.messageType == "voice" || selectedMsg.text.startsWith("[voice]")
            val imageNodeForMenu = if (isImageMsg) {
                parseGroupPhotoNode(selectedMsg, selectedMsg.senderId == currentUserId, groupMediaStates[selectedMsg.id])
            } else null
            val videoSpecForMenu = if (isVideoMsg) parseGroupVideoSpec(selectedMsg, groupMediaStates[selectedMsg.id]) else null
            val audioSpecForMenu = if (isAudioMsg) parseGroupAudioSpec(selectedMsg, groupMediaStates[selectedMsg.id]) else null
            val voicePathForMenu = if (isVoiceMsg) {
                (selectedMsg.localMediaPath?.takeIf { it.isNotBlank() }
                    ?: selectedMsg.text.removePrefix("[voice]").substringBefore("|"))
                    .removePrefix("file://")
                    .takeIf { it.isNotBlank() && it != "downloading..." && java.io.File(it).exists() }
            } else null

            val canSaveGallery = (imageNodeForMenu != null && imageNodeForMenu.source.isNotBlank()) ||
                (videoSpecForMenu?.localPath != null)
            val isMediaOrVoice = isImageMsg || isVideoMsg || isAudioMsg || isVoiceMsg

            val actionItems = buildList {
                if (canSaveGallery) {
                    add(MessageAction("save_gallery", "Save to Gallery", { ChatIconDownload(tint = colors.text) }))
                }
                add(MessageAction("reply", "Reply", { ChatIconReply(tint = colors.text) }))
                if (!isMediaOrVoice) {
                    add(MessageAction("copy", "Copy Text", { ChatIconCopy(tint = colors.text) }))
                    add(MessageAction("translate", "Translate", { ChatIconTranslate(tint = colors.text) }))
                    add(MessageAction("listen", "Listen", { ChatIconVolume(tint = colors.text) }))
                }
                add(MessageAction("share", "Share", { ChatIconShare(tint = colors.text) }))
                add(MessageAction("select", "Select", { ChatIconCheck(tint = colors.text) }))
                add(MessageAction("delete", "Delete", { ChatIconTrash(tint = colors.danger) }, destructive = true))
            }
            MessageLongPressOverlay(
                myReaction = currentMyReaction,
                actions = actionItems,
                onPickEmoji = { emoji ->
                    val op = if (emoji == currentMyReaction) "remove" else "set"
                    coroutineScope.launch {
                        val res = GroupChatRepository.sendGroupReaction(
                            context = context,
                            groupId = group.id,
                            targetMessageId = selectedMsg.id,
                            emoji = emoji,
                            op = op
                        )
                        if (res.isFailure) {
                            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Could not send reaction", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onMoreEmoji = {
                    isEmojiPickerOpen = true
                },
                onAction = { actionId ->
                    when (actionId) {
                        "save_gallery" -> {
                            coroutineScope.launch {
                                val res = if (videoSpecForMenu?.localPath != null) {
                                    ChatMediaSaver.saveVideoToGallery(context, videoSpecForMenu.localPath)
                                } else if (imageNodeForMenu != null && imageNodeForMenu.source.isNotBlank()) {
                                    ChatMediaSaver.saveImageToGallery(context, imageNodeForMenu.source)
                                } else {
                                    Result.failure(IllegalStateException("Media file not downloaded yet"))
                                }
                                Toast.makeText(
                                    context,
                                    res.fold(
                                        onSuccess = { "Saved" },
                                        onFailure = { it.message ?: "Could not save media" }
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        "reply" -> replyingToMessage = selectedMsg
                        "copy" -> {
                            clipboardManager.setText(AnnotatedString(ChatAnimatedStickers.readableLabel(selectedMsg.text)))
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
                        "share" -> {
                            coroutineScope.launch {
                                when {
                                    imageNodeForMenu != null && imageNodeForMenu.source.isNotBlank() ->
                                        ChatMediaSaver.shareImage(context, imageNodeForMenu.source)
                                    videoSpecForMenu?.localPath != null ->
                                        ChatMediaSaver.shareMediaFile(context, videoSpecForMenu.localPath, "video/mp4", "Share video")
                                    audioSpecForMenu?.localPath != null ->
                                        ChatMediaSaver.shareMediaFile(context, audioSpecForMenu.localPath, selectedMsg.mimeType ?: "audio/mp4", "Share audio")
                                    voicePathForMenu != null ->
                                        ChatMediaSaver.shareMediaFile(context, voicePathForMenu, "audio/mp4", "Share voice note")
                                    else -> {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, ChatAnimatedStickers.readableLabel(selectedMsg.text))
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share message"))
                                    }
                                }
                            }
                        }
                        "select" -> {
                            isSelectionMode = true
                            selectedMessageIds.add(selectedMsg.id)
                        }
                        "delete" -> {
                            coroutineScope.launch {
                                GroupChatRepository.deleteGroupMessage(context, selectedMsg.id)
                                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    selectedMessageForMenu = null
                },
                onDismiss = { selectedMessageForMenu = null }
            )
        }

        // Failed message Retry / Delete sheet
        if (failedMessageForSheet != null) {
            val failedTarget = failedMessageForSheet!!
            FailedMessageActionSheet(
                failReason = failedTarget.failReason,
                onRetry = {
                    failedMessageForSheet = null
                    GroupChatRepository.retryGroupMessage(context, failedTarget.id)
                },
                onDelete = {
                    failedMessageForSheet = null
                    coroutineScope.launch {
                        GroupChatRepository.deleteGroupMessage(context, failedTarget.id)
                        Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { failedMessageForSheet = null }
            )
        }

        // Telegram Pre-Send Photo Review & Caption Sheet
        if (pendingPhotoUris.isNotEmpty()) {
            TelegramPendingMediaDialog(
                uris = pendingPhotoUris,
                recipientLabel = group.name,
                onDismiss = { pendingPhotoUris = emptyList() },
                onRemoveAt = { removeIdx ->
                    if (removeIdx in pendingPhotoUris.indices) {
                        pendingPhotoUris = pendingPhotoUris.toMutableList().apply { removeAt(removeIdx) }
                    }
                },
                onAddMore = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSendWithOptions = { urisToSend, captionText, rotations, isHd ->
                    val replySnapshot = replyingToMessage
                    val tapMs = ChatTimingTracker.nowMs()
                    pendingPhotoUris = emptyList()
                    replyingToMessage = null
                    coroutineScope.launch(Dispatchers.IO) {
                        urisToSend.forEachIndexed { idx, uri ->
                            try {
                                val bytes = ChatMediaSaver.prepareOutgoingImageBytes(
                                    context = context,
                                    uri = uri,
                                    extraRotationDegrees = rotations[idx] ?: 0,
                                    highQuality = isHd
                                )
                                if (bytes != null && bytes.isNotEmpty()) {
                                    GroupChatRepository.enqueueSendGroupMediaMessage(
                                        context = context,
                                        groupId = group.id,
                                        rawBytes = bytes,
                                        messageType = "image",
                                        mimeType = "image/jpeg",
                                        caption = if (idx == 0) captionText.takeIf { it.isNotBlank() } else null,
                                        replyToMessage = if (idx == 0) replySnapshot else null,
                                        tapElapsedMs = tapMs
                                    )
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error preparing photo: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            )
        }

        if (showAttachmentChooser) {
            ChatAttachmentChooserSheet(
                onPickPhotos = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickVideo = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onPickAudio = {
                    audioPickerLauncher.launch(arrayOf("audio/*"))
                },
                onDismiss = { showAttachmentChooser = false }
            )
        }

        // Telephoto Full-Screen Media Viewer
        if (activeMediaViewerIndex != null && allMediaItems.isNotEmpty()) {
            TelephotoMediaViewerDialog(
                mediaItems = allMediaItems,
                initialIndex = activeMediaViewerIndex!!.coerceIn(0, allMediaItems.size - 1),
                onDismiss = { activeMediaViewerIndex = null },
                onSave = { item ->
                    coroutineScope.launch {
                        val res = if (item.isVideo) {
                            ChatMediaSaver.saveVideoToGallery(context, item.url)
                        } else {
                            ChatMediaSaver.saveImageToGallery(context, item.url)
                        }
                        Toast.makeText(
                            context,
                            res.fold(
                                onSuccess = { "Saved" },
                                onFailure = { it.message ?: "Could not save media" }
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onShare = { item ->
                    coroutineScope.launch {
                        if (item.isVideo) {
                            ChatMediaSaver.shareMediaFile(context, item.url, "video/mp4", "Share video")
                        } else {
                            ChatMediaSaver.shareImage(context, item.url)
                        }
                    }
                },
                onDelete = { item ->
                    coroutineScope.launch {
                        GroupChatRepository.deleteGroupMessage(context, item.id)
                        Toast.makeText(context, if (item.isVideo) "Video deleted" else "Photo deleted", Toast.LENGTH_SHORT).show()
                        if (allMediaItems.size <= 1) {
                            activeMediaViewerIndex = null
                        }
                    }
                }
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

        if (vaultStatus != com.example.crypto.VaultStatus.UNLOCKED) {
            VaultManagementDialogs(
                onDismissRequest = {
                    if (com.example.crypto.VaultSecurityManager.isVaultLocked()) {
                        onBack()
                    }
                }
            )
        }
        if (inspectedMemberProfile != null) {
            val (uid, name, avatar) = inspectedMemberProfile!!
            Dialog(
                onDismissRequest = { inspectedMemberProfile = null },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                BackHandler { inspectedMemberProfile = null }
                com.example.ui.screens.UserProfileFeedScreen(
                    userId = uid,
                    username = name,
                    profileName = name,
                    avatarUrl = avatar.orEmpty(),
                    onBack = { inspectedMemberProfile = null },
                    onStartChat = { inspectedMemberProfile = null }
                )
            }
        }
    }
}
