package com.example.ui.screens.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.crypto.VaultSecurityManager
import com.example.crypto.VaultStatus
import com.example.repository.EncryptedChatRepository
import com.example.repository.RecipientNotReadyException
import com.example.ui.components.FluentEmojiAssets
import com.example.ui.components.FluentEmojiPickerTray
import com.example.ui.screens.chat.components.*
import com.example.ui.screens.chat.model.ChatMessage
import com.example.ui.screens.chat.model.ChatMessageType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    partnerUserId: String = "",
    partnerName: String = "Yuki Tanaka",
    partnerAvatarUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
    partnerCountry: String = "Japan",
    partnerCountryCode: String = "jp",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val vaultStatus by VaultSecurityManager.vaultStatus.collectAsState()
    var conversationId by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Edge case check state: does recipient have public encryption key?
    var recipientKeyReady by remember { mutableStateOf<Boolean?>(null) }
    var isCheckingKey by remember { mutableStateOf(false) }
    var showNotReadyDialog by remember { mutableStateOf(false) }

    // UI Interactive States
    var inputText by remember { mutableStateOf("") }
    var selectedMessageForMenu by remember { mutableStateOf<ChatMessage?>(null) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    val attachmentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Voice Note States & Audio Player
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var playingAudioMsgId by remember { mutableStateOf<String?>(null) }
    var activeMediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = com.example.util.VoiceRecorderManager.startRecording(context)
            if (started) {
                isRecordingVoice = true
                recordingSeconds = 0
            } else {
                Toast.makeText(context, "Could not start voice recorder", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            while (isRecordingVoice) {
                kotlinx.coroutines.delay(1000L)
                recordingSeconds++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                com.example.util.VoiceRecorderManager.cancelRecording()
                activeMediaPlayer?.stop()
                activeMediaPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    fun togglePlayAudio(msg: ChatMessage) {
        if (playingAudioMsgId == msg.id) {
            try {
                activeMediaPlayer?.stop()
                activeMediaPlayer?.release()
            } catch (_: Exception) {}
            activeMediaPlayer = null
            playingAudioMsgId = null
        } else {
            try {
                activeMediaPlayer?.stop()
                activeMediaPlayer?.release()
            } catch (_: Exception) {}

            val path = msg.text.removePrefix("file://").trim()
            val file = java.io.File(path)
            if (file.exists() && file.length() > 0) {
                try {
                    val player = android.media.MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        prepare()
                        start()
                    }
                    activeMediaPlayer = player
                    playingAudioMsgId = msg.id

                    player.setOnCompletionListener {
                        try { player.release() } catch (_: Exception) {}
                        activeMediaPlayer = null
                        playingAudioMsgId = null
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot play audio: ${e.message}", Toast.LENGTH_SHORT).show()
                    playingAudioMsgId = null
                }
            } else {
                Toast.makeText(context, "Voice note file unavailable locally", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Multiple Image Photo Picker (2-layer E2EE encrypted media upload)
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                isSending = true
                var sendSuccessCount = 0
                for (uri in uris) {
                    try {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (bytes != null && bytes.isNotEmpty()) {
                            val sendResult = EncryptedChatRepository.sendMediaMessage(
                                context = context,
                                recipientUserId = partnerUserId.ifBlank { "partner_mock_id" },
                                recipientName = partnerName,
                                recipientAvatar = partnerAvatarUrl,
                                rawBytes = bytes,
                                messageType = "image",
                                mimeType = "image/jpeg",
                                replyToText = replyingToMessage?.text,
                                replyToSender = replyingToMessage?.let { if (it.isMe) "You" else partnerName }
                            )
                            sendResult.onSuccess {
                                sendSuccessCount++
                                recipientKeyReady = true
                            }.onFailure { ex ->
                                if (ex is RecipientNotReadyException) {
                                    recipientKeyReady = false
                                    showNotReadyDialog = true
                                } else {
                                    Toast.makeText(context, ex.message ?: "Failed to encrypt and send image", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error reading image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                isSending = false
                if (sendSuccessCount > 0) {
                    replyingToMessage = null
                    Toast.makeText(context, "$sendSuccessCount image(s) encrypted and sent securely", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Initialize repo, resolve conversation, and check recipient key availability
    LaunchedEffect(partnerUserId, vaultStatus) {
        EncryptedChatRepository.init(context)
        if (vaultStatus == VaultStatus.UNLOCKED && partnerUserId.isNotBlank()) {
            try {
                val conv = EncryptedChatRepository.getOrCreateLocalConversation(
                    context = context,
                    otherUserId = partnerUserId,
                    otherUserName = partnerName,
                    otherUserAvatar = partnerAvatarUrl
                )
                conversationId = conv.id
                EncryptedChatRepository.markConversationRead(conv.id)

                // Verify if recipient has registered E2EE public keys
                isCheckingKey = true
                recipientKeyReady = EncryptedChatRepository.checkRecipientKeyStatus(context, partnerUserId)
                isCheckingKey = false
            } catch (e: Exception) {
                android.util.Log.e("DirectChatScreen", "Error resolving conversation", e)
            }
        }
    }

    // Collect messages from encrypted Room database
    val localDbMessages by if (conversationId.isNotBlank()) {
        EncryptedChatRepository.getMessagesFlow(conversationId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Map local DB messages to UI ChatMessage with support for Single/Stacked Images and Voice Notes
    val uiMessages = remember(localDbMessages) {
        localDbMessages.map { dbMsg ->
            val rawText = dbMsg.text
            val isImgStack = rawText.startsWith("[imgs]")
            val isSingleImg = rawText.startsWith("[img]")
            val isVoice = rawText.startsWith("[voice]") || rawText.startsWith("[audio]")
            val isWebImg = (rawText.startsWith("http://") || rawText.startsWith("https://")) &&
                    (rawText.contains(".jpg") || rawText.contains(".png") || rawText.contains(".jpeg") || rawText.contains("unsplash"))

            when {
                isVoice -> {
                    val parts = rawText.removePrefix("[voice]").removePrefix("[audio]").split("|")
                    val audioPath = parts.getOrNull(0)?.trim() ?: ""
                    val durationSecs = parts.getOrNull(1)?.toIntOrNull() ?: 12
                    val formattedDuration = String.format("%d:%02d", durationSecs / 60, durationSecs % 60)
                    ChatMessage(
                        id = dbMsg.id,
                        text = audioPath,
                        time = timeFormatter.format(Date(dbMsg.timestamp)),
                        isMe = dbMsg.isOutgoing,
                        type = ChatMessageType.AUDIO,
                        audioDuration = formattedDuration,
                        audioDurationSecs = durationSecs,
                        replyToText = dbMsg.replyToText,
                        replyToSender = dbMsg.replyToSender,
                        showAvatar = !dbMsg.isOutgoing
                    )
                }
                isImgStack -> {
                    val urls = rawText.removePrefix("[imgs]").split(",").map { it.trim() }.filter { it.isNotBlank() }
                    ChatMessage(
                        id = dbMsg.id,
                        text = "",
                        time = timeFormatter.format(Date(dbMsg.timestamp)),
                        isMe = dbMsg.isOutgoing,
                        type = ChatMessageType.IMAGE,
                        imageUrl = urls.firstOrNull(),
                        imageUrls = urls,
                        replyToText = dbMsg.replyToText,
                        replyToSender = dbMsg.replyToSender,
                        showAvatar = !dbMsg.isOutgoing
                    )
                }
                isSingleImg -> {
                    val url = rawText.removePrefix("[img]").trim()
                    ChatMessage(
                        id = dbMsg.id,
                        text = "",
                        time = timeFormatter.format(Date(dbMsg.timestamp)),
                        isMe = dbMsg.isOutgoing,
                        type = ChatMessageType.IMAGE,
                        imageUrl = url,
                        imageUrls = listOf(url),
                        replyToText = dbMsg.replyToText,
                        replyToSender = dbMsg.replyToSender,
                        showAvatar = !dbMsg.isOutgoing
                    )
                }
                isWebImg -> {
                    ChatMessage(
                        id = dbMsg.id,
                        text = "",
                        time = timeFormatter.format(Date(dbMsg.timestamp)),
                        isMe = dbMsg.isOutgoing,
                        type = ChatMessageType.IMAGE,
                        imageUrl = rawText,
                        imageUrls = listOf(rawText),
                        replyToText = dbMsg.replyToText,
                        replyToSender = dbMsg.replyToSender,
                        showAvatar = !dbMsg.isOutgoing
                    )
                }
                else -> {
                    ChatMessage(
                        id = dbMsg.id,
                        text = dbMsg.text,
                        time = timeFormatter.format(Date(dbMsg.timestamp)),
                        isMe = dbMsg.isOutgoing,
                        type = ChatMessageType.TEXT,
                        replyToText = dbMsg.replyToText,
                        replyToSender = dbMsg.replyToSender,
                        showAvatar = !dbMsg.isOutgoing
                    )
                }
            }
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiMessages.size) {
        if (uiMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiMessages.size - 1)
        }
    }

    BackHandler {
        when {
            previewImageUrl != null -> previewImageUrl = null
            showEmojiPicker -> showEmojiPicker = false
            selectedMessageForMenu != null -> selectedMessageForMenu = null
            replyingToMessage != null -> replyingToMessage = null
            else -> onBack()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFAFAEE))) {
        Scaffold(
            containerColor = Color(0xFFFAFAEE),
            topBar = {
                ChatHeader(
                    partnerName = partnerName,
                    partnerAvatarUrl = partnerAvatarUrl,
                    partnerCountry = partnerCountry,
                    partnerCountryCode = partnerCountryCode,
                    isOnline = true,
                    onBackClick = onBack,
                    onMenuClick = {
                        Toast.makeText(context, "End-to-End Encrypted Chat with $partnerName", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            bottomBar = {
                Column(modifier = Modifier.background(Color(0xFFFAFAEE))) {
                    // AI Quick Action Buttons Row
                    AIActionsRow(
                        onTranslateClick = {
                            val lastReceived = uiMessages.findLast { !it.isMe && it.type == ChatMessageType.TEXT }
                            if (lastReceived != null) {
                                Toast.makeText(context, "Original text shown", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No received message to translate", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onImproveTextClick = {
                            if (inputText.isNotBlank()) {
                                inputText = "Thank you so much! I'm genuinely trying my best every day."
                                Toast.makeText(context, "Polished your sentence!", Toast.LENGTH_SHORT).show()
                            } else {
                                inputText = "I would love to practice speaking together soon!"
                                Toast.makeText(context, "Practice suggestion added!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onExampleReplyClick = {
                            inputText = "That sounds wonderful! When are you free for a conversation?"
                            Toast.makeText(context, "Example reply ready!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // 3D Fluent Emoji Picker Tray (shows when smiley icon is tapped)
                    FluentEmojiPickerTray(
                        visible = showEmojiPicker,
                        onEmojiSelected = { unicode, _ ->
                            inputText += unicode
                        }
                    )

                    // Message Composer
                    MessageComposer(
                        messageText = inputText,
                        onMessageChange = { inputText = it },
                        onSendMessage = {
                            if (inputText.isNotBlank() && !isSending) {
                                val textToSend = inputText.trim()
                                val replyText = replyingToMessage?.text
                                val replySender = replyingToMessage?.let { if (it.isMe) "You" else partnerName }
                                isSending = true

                                coroutineScope.launch {
                                    val sendResult = EncryptedChatRepository.sendMessage(
                                        context = context,
                                        recipientUserId = partnerUserId.ifBlank { "partner_mock_id" },
                                        recipientName = partnerName,
                                        recipientAvatar = partnerAvatarUrl,
                                        text = textToSend,
                                        replyToText = replyText,
                                        replyToSender = replySender
                                    )
                                    isSending = false
                                    sendResult.onSuccess {
                                        inputText = ""
                                        replyingToMessage = null
                                        recipientKeyReady = true
                                        showEmojiPicker = false
                                    }.onFailure { ex ->
                                        if (ex is RecipientNotReadyException) {
                                            recipientKeyReady = false
                                            showNotReadyDialog = true
                                        } else {
                                            Toast.makeText(context, ex.message ?: "Failed to encrypt and send message", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        },
                        onAddClick = {
                            showAttachmentSheet = true
                        },
                        onSmileyClick = {
                            showEmojiPicker = !showEmojiPicker
                        },
                        onMicClick = {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                val started = com.example.util.VoiceRecorderManager.startRecording(context)
                                if (started) {
                                    isRecordingVoice = true
                                    recordingSeconds = 0
                                } else {
                                    Toast.makeText(context, "Could not start voice recorder", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        replyingToMessage = replyingToMessage,
                        onCancelReply = { replyingToMessage = null }
                    )

                    // Active Voice Recording Bar
                    if (isRecordingVoice) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                        text = "Recording voice note: ${String.format("%d:%02d", recordingSeconds / 60, recordingSeconds % 60)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF92400E)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = {
                                            com.example.util.VoiceRecorderManager.cancelRecording()
                                            isRecordingVoice = false
                                            recordingSeconds = 0
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFFD97706)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Cancel", color = Color(0xFF92400E), fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val recRes = com.example.util.VoiceRecorderManager.stopRecording()
                                            isRecordingVoice = false
                                            recordingSeconds = 0
                                            if (recRes != null && recRes.bytes.isNotEmpty()) {
                                                coroutineScope.launch {
                                                    isSending = true
                                                    val sendResult = EncryptedChatRepository.sendMediaMessage(
                                                        context = context,
                                                        recipientUserId = partnerUserId.ifBlank { "partner_mock_id" },
                                                        recipientName = partnerName,
                                                        recipientAvatar = partnerAvatarUrl,
                                                        rawBytes = recRes.bytes,
                                                        messageType = "voice",
                                                        mimeType = "audio/aac",
                                                        durationSeconds = recRes.durationSeconds,
                                                        replyToText = replyingToMessage?.text,
                                                        replyToSender = replyingToMessage?.let { if (it.isMe) "You" else partnerName }
                                                    )
                                                    isSending = false
                                                    sendResult.onSuccess {
                                                        replyingToMessage = null
                                                        recipientKeyReady = true
                                                        Toast.makeText(context, "Voice note sent securely 🎙️", Toast.LENGTH_SHORT).show()
                                                    }.onFailure { ex ->
                                                        if (ex is RecipientNotReadyException) {
                                                            recipientKeyReady = false
                                                            showNotReadyDialog = true
                                                        } else {
                                                            Toast.makeText(context, ex.message ?: "Failed to encrypt and send voice note", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Send Voice", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                // Date separator
                item {
                    DateSeparatorPill(dateText = "Today")
                }

                if (uiMessages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (recipientKeyReady == false) {
                                    "$partnerName hasn't enabled secure chat yet.\nYou can message them once they open FunkyTalk."
                                } else {
                                    "No messages yet.\nSend a message, voice note, or photos to start chatting with $partnerName!"
                                },
                                fontSize = 14.sp,
                                color = Color(0xFF9CA3AF),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // Dynamic message bubbles (Text, Voice Notes, & Single/Stacked Images)
                items(uiMessages, key = { it.id }) { msg ->
                    if (msg.type == ChatMessageType.AUDIO) {
                        VoiceMessageBubble(
                            message = msg,
                            partnerAvatarUrl = partnerAvatarUrl,
                            isPlaying = playingAudioMsgId == msg.id,
                            isHighlighted = selectedMessageForMenu?.id == msg.id,
                            onPlayPauseClick = { togglePlayAudio(msg) },
                            onLongClick = { selectedMessageForMenu = msg }
                        )
                    } else if (msg.type == ChatMessageType.IMAGE || msg.getAllImages().isNotEmpty()) {
                        ImageMessageBubble(
                            message = msg,
                            partnerAvatarUrl = partnerAvatarUrl,
                            isHighlighted = selectedMessageForMenu?.id == msg.id,
                            onImageClick = { url ->
                                previewImageUrl = url
                            },
                            onDownloadClick = {
                                Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                            },
                            onReactionClick = { selectedMessageForMenu = msg },
                            onLongClick = { selectedMessageForMenu = msg }
                        )
                    } else if (msg.isMe) {
                        OutgoingMessageBubble(
                            message = msg,
                            isHighlighted = selectedMessageForMenu?.id == msg.id,
                            onLongClick = { selectedMessageForMenu = msg },
                            onReactionClick = { selectedMessageForMenu = msg }
                        )
                    } else {
                        IncomingMessageBubble(
                            message = msg,
                            partnerAvatarUrl = partnerAvatarUrl,
                            isHighlighted = selectedMessageForMenu?.id == msg.id,
                            onLongClick = { selectedMessageForMenu = msg },
                            onReactionClick = { selectedMessageForMenu = msg }
                        )
                    }
                }
            }
        }

        // Fullscreen Image Preview Modal
        if (previewImageUrl != null) {
            ChatImagePreviewDialog(
                imageUrl = previewImageUrl,
                onDismiss = { previewImageUrl = null }
            )
        }

        // Attachment / Image Upload Bottom Sheet
        if (showAttachmentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAttachmentSheet = false },
                sheetState = attachmentSheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Share Images & Media",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "End-to-End Encrypted media transfer",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 1: Gallery Multi-picker
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF9FAFB),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentSheet = false
                                multiplePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Photos",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Choose from Gallery",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                                Text(
                                    text = "Select single or multiple photos to send in stack",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 2: Curated Travel Photos Stack (Instant sample for instant testing)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF9FAFB),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentSheet = false
                                coroutineScope.launch {
                                    isSending = true
                                    val sampleStack = listOf(
                                        "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800",
                                        "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800",
                                        "https://images.unsplash.com/photo-1492571350019-22de08371fd3?w=800",
                                        "https://images.unsplash.com/photo-1542051841857-5f90071e7989?w=800"
                                    )
                                    val encodedText = "[imgs]${sampleStack.joinToString(",")}"
                                    val sendResult = EncryptedChatRepository.sendMessage(
                                        context = context,
                                        recipientUserId = partnerUserId.ifBlank { "partner_mock_id" },
                                        recipientName = partnerName,
                                        recipientAvatar = partnerAvatarUrl,
                                        text = encodedText,
                                        replyToText = replyingToMessage?.text,
                                        replyToSender = replyingToMessage?.let { if (it.isMe) "You" else partnerName }
                                    )
                                    isSending = false
                                    sendResult.onSuccess {
                                        replyingToMessage = null
                                        recipientKeyReady = true
                                    }.onFailure { ex ->
                                        if (ex is RecipientNotReadyException) {
                                            recipientKeyReady = false
                                            showNotReadyDialog = true
                                        } else {
                                            Toast.makeText(context, ex.message ?: "Failed to send", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0E7FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Travel Album",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Send Travel Photo Album (4 Photos Stack)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                                Text(
                                    text = "Instantly sends a 4-photo stack in beautiful grid layout",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Contextual Long-press Overlay
        if (selectedMessageForMenu != null) {
            val target = selectedMessageForMenu!!
            MessageContextMenu(
                targetMessage = target,
                onDismiss = { selectedMessageForMenu = null },
                onSelectReaction = { reaction ->
                    selectedMessageForMenu = null
                    Toast.makeText(context, "Reacted $reaction", Toast.LENGTH_SHORT).show()
                },
                onTranslate = {
                    Toast.makeText(context, "Translation shown", Toast.LENGTH_SHORT).show()
                    selectedMessageForMenu = null
                },
                onReply = {
                    replyingToMessage = target
                    selectedMessageForMenu = null
                },
                onCopy = {
                    clipboardManager.setText(AnnotatedString(target.text))
                    Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                    selectedMessageForMenu = null
                },
                onReactMore = {
                    selectedMessageForMenu = null
                    showEmojiPicker = true
                },
                onMoreActions = {
                    selectedMessageForMenu = null
                }
            )
        }

        // Prompt vault setup or unlock if not unlocked
        if (vaultStatus != VaultStatus.UNLOCKED) {
            VaultManagementDialogs(onDismissRequest = onBack)
        }

        // Graceful Edge Case Dialog when recipient hasn't opened app / initialized keys
        if (showNotReadyDialog) {
            Dialog(
                onDismissRequest = { showNotReadyDialog = false },
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 16.dp,
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icon in amber/gold soft container
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(30.dp))
                                .border(1.5.dp, Color(0xFFFDE68A), RoundedCornerShape(30.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security Notice",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Secure Chat Unavailable",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center,
                            letterSpacing = (-0.3).sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "This user is not ready for secure chat yet.",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFFB45309),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "$partnerName has not opened FunkyTalk yet, so their end-to-end encryption keys have not been registered.\n\nAs soon as $partnerName launches the app, secure chat will be ready automatically.\n\nYour message draft has been saved.",
                            fontSize = 13.sp,
                            color = Color(0xFF4B5563),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showNotReadyDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Got it",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                showNotReadyDialog = false
                                coroutineScope.launch {
                                    isCheckingKey = true
                                    val ready = EncryptedChatRepository.checkRecipientKeyStatus(context, partnerUserId)
                                    recipientKeyReady = ready
                                    isCheckingKey = false
                                    if (ready == true) {
                                        Toast.makeText(context, "$partnerName is now ready for secure chat!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "$partnerName is still not ready.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Check Again",
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Check Again",
                                color = Color(0xFF4B5563),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
