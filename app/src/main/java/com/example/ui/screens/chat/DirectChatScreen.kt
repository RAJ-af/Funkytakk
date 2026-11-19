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
import androidx.compose.foundation.border
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
import com.example.BuildConfig
import com.example.ChatPreferencesManager
import com.example.CloudAiManager
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.db.LocalDmConversation
import com.example.db.LocalDmMessage
import com.example.db.LocalDmMessageReaction
import com.example.moderation.ReportUserDialog
import com.example.repository.BlockRepository
import com.example.repository.EncryptedChatRepository
import com.example.repository.USER_BLOCKED_ERROR_MESSAGE
import com.example.repository.UserBlockedException
import com.example.ui.chat.*
import com.example.ui.components.BlockUserConfirmDialog
import com.example.ui.components.CircleFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

sealed interface DmTimelineItem {
    val id: String

    data class DateHeader(val label: String) : DmTimelineItem {
        override val id: String = "date_$label"
    }

    data class SecurityHeader(val text: String) : DmTimelineItem {
        override val id: String = "sec_header"
    }

    data class MessageItem(
        val message: LocalDmMessage,
        val position: GroupPosition,
        val reactions: List<ReactionUi>,
        val isFirstInGroup: Boolean,
        val isLastInGroup: Boolean,
    ) : DmTimelineItem {
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
    com.example.ui.components.SecureWindow(enabled = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(context) {
        ChatPreferencesManager.init(context)
        CloudAiManager.init(context)
    }

    val darkTheme = rememberEffectiveChatDarkTheme()
    val chatPreset by ChatPreferencesManager.chatPreset.collectAsState()
    val chatTextScale by ChatPreferencesManager.chatTextScale.collectAsState()
    val isAiConsentGranted by CloudAiManager.isCloudAiEnabled.collectAsState()

    var conversationId by remember { mutableStateOf("") }
    var rawMessages by remember { mutableStateOf<List<LocalDmMessage>>(emptyList()) }
    var rawReactions by remember { mutableStateOf<List<LocalDmMessageReaction>>(emptyList()) }
    var currentUserId by remember { mutableStateOf("") }

    var inputText by remember { mutableStateOf("") }
    var isEmojiPickerOpen by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<LocalDmMessage?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<LocalDmMessage?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedMessageIds = remember { mutableStateListOf<String>() }

    var activeWordLookup by remember { mutableStateOf<WordLookupState?>(null) }
    var showAiAssistSheet by remember { mutableStateOf(false) }
    var activeMediaViewerIndex by remember { mutableStateOf<Int?>(null) }
    var showReportUserDialog by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var isBlockingUser by remember { mutableStateOf(false) }
    var isChatBlocked by remember { mutableStateOf(false) }
    val blockedUserIds by BlockRepository.blockedUserIds.collectAsState()
    val isPartnerBlockedLocally = partnerUserId.isNotBlank() && blockedUserIds.contains(partnerUserId)

    val vaultStatus by com.example.crypto.VaultSecurityManager.vaultStatus.collectAsState()
    var showVaultGateDialog by remember { mutableStateOf(false) }
    var pendingVaultAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun ensureVaultUnlocked(action: () -> Unit) {
        if (!com.example.crypto.VaultSecurityManager.isVaultLocked() &&
            vaultStatus == com.example.crypto.VaultStatus.UNLOCKED
        ) {
            action()
        } else {
            pendingVaultAction = action
            showVaultGateDialog = true
        }
    }

    LaunchedEffect(vaultStatus) {
        if (vaultStatus == com.example.crypto.VaultStatus.UNLOCKED && showVaultGateDialog) {
            showVaultGateDialog = false
            val action = pendingVaultAction
            pendingVaultAction = null
            action?.invoke()
        }
    }

    val listState = rememberLazyListState()
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

    // Flashing animation state for jump-to-message
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

    // Photo picker launcher (Direct - no docs/file popup)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                uris.forEach { uri ->
                    try {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (bytes != null && bytes.isNotEmpty()) {
                            val sendRes = EncryptedChatRepository.sendMediaMessage(
                                context = context,
                                recipientUserId = partnerUserId,
                                recipientName = partnerName,
                                recipientAvatar = partnerAvatarUrl,
                                rawBytes = bytes,
                                messageType = "image",
                                mimeType = "image/jpeg"
                            )
                            if (sendRes.isFailure) {
                                val err = sendRes.exceptionOrNull()
                                if (err is UserBlockedException) isChatBlocked = true
                                Toast.makeText(context, err?.message ?: "Could not send photo.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error sending photo: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    var resolvedAvatarUrl by remember(partnerAvatarUrl) { mutableStateOf(partnerAvatarUrl.orEmpty()) }
    var resolvedCountryCode by remember(partnerCountryCode) { mutableStateOf(partnerCountryCode.orEmpty()) }
    var resolvedCountryName by remember(partnerCountry) { mutableStateOf(partnerCountry.orEmpty()) }
    var resolvedIsOnline by remember { mutableStateOf(true) }

    LaunchedEffect(partnerUserId) {
        if (partnerUserId.isNotBlank()) {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)
                    ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

                val discResp = SupabaseClient.api.getDiscoverableProfiles(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    options = emptyMap()
                )
                if (discResp.isSuccessful) {
                    val dp = discResp.body()?.find { it.auth_user_id == partnerUserId }
                    if (dp != null) {
                        if (!dp.avatar_url.isNullOrBlank()) resolvedAvatarUrl = dp.avatar_url
                        if (!dp.country_code.isNullOrBlank()) resolvedCountryCode = dp.country_code
                        if (!dp.country.isNullOrBlank()) resolvedCountryName = dp.country
                        resolvedIsOnline = dp.is_online == true
                    }
                }

                val profileRes = SupabaseClient.api.getProfile(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    authUserIdEq = "eq.$partnerUserId"
                )
                if (profileRes.isSuccessful) {
                    val p = profileRes.body()?.firstOrNull()
                    if (p != null) {
                        if (!p.avatar_url.isNullOrBlank()) resolvedAvatarUrl = p.avatar_url
                        if (!p.country_code.isNullOrBlank()) resolvedCountryCode = p.country_code
                        if (!p.country.isNullOrBlank()) resolvedCountryName = p.country
                    }
                }
                // Fetch partner's equipped cosmetics for real-time display
                com.example.store.CosmeticsManager.batchFetchUserCosmetics(context, listOf(partnerUserId))
            } catch (e: Exception) {
                android.util.Log.e("DirectChatScreen", "Error resolving partner profile", e)
            }
        }
    }

    // Setup DM conversation and observers
    DisposableEffect(partnerUserId, conversationId) {
        com.example.service.PushNotificationManager.setActiveOpenDm(
            conversationId = conversationId,
            partnerUserId = partnerUserId
        )
        com.example.service.PushNotificationManager.cancelNotificationForChat(
            context = context,
            conversationId = conversationId,
            partnerUserId = partnerUserId
        )
        onDispose {
            com.example.service.PushNotificationManager.setActiveOpenDm(null, null)
        }
    }

    LaunchedEffect(partnerUserId, vaultStatus) {
        if (com.example.crypto.VaultSecurityManager.isVaultLocked() ||
            vaultStatus != com.example.crypto.VaultStatus.UNLOCKED
        ) {
            showVaultGateDialog = true
            return@LaunchedEffect
        }
        try {
            EncryptedChatRepository.init(context)
            currentUserId = SupabaseSessionManager.getUserId(context) ?: ""
            val conv = EncryptedChatRepository.getOrCreateLocalConversation(context, partnerUserId, partnerName, partnerAvatarUrl)
            conversationId = conv.id
            EncryptedChatRepository.markConversationRead(conv.id)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: UserBlockedException) {
            isChatBlocked = true
            Toast.makeText(context, USER_BLOCKED_ERROR_MESSAGE, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("DirectChatScreen", "Error resolving conversation", e)
        }
    }

    LaunchedEffect(conversationId) {
        if (conversationId.isNotBlank()) {
            launch {
                EncryptedChatRepository.getMessagesFlow(conversationId).collect { msgs ->
                    rawMessages = msgs
                }
            }
            launch {
                EncryptedChatRepository.getDmReactionsFlow(conversationId).collect { reacts ->
                    rawReactions = reacts
                }
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

        LaunchedEffect(timelineItems.size) {
            if (timelineItems.isNotEmpty()) {
                listState.scrollToItem(timelineItems.size - 1)
            }
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

        BackHandler {
            if (isEmojiPickerOpen) {
                isEmojiPickerOpen = false
            } else if (isSelectionMode) {
                isSelectionMode = false
                selectedMessageIds.clear()
            } else if (selectedMessageForMenu != null) {
                selectedMessageForMenu = null
            } else if (showVaultGateDialog) {
                showVaultGateDialog = false
                pendingVaultAction = null
            } else {
                onBack()
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

                            com.example.ui.components.PresenceAvatar(
                                avatarUrl = resolvedAvatarUrl.ifBlank { null },
                                size = 42.dp,
                                userId = partnerUserId,
                                enableFrameAnimation = true,
                                countryCode = resolvedCountryCode.takeIf { it.isNotBlank() },
                                isOnline = resolvedIsOnline
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                com.example.store.DecoratedUserName(
                                    userId = partnerUserId,
                                    name = partnerName,
                                    fontSize = 16f.chatSp(),
                                    fontWeight = FontWeight.SemiBold,
                                    defaultColor = colors.text,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (resolvedIsOnline) Color(0xFF22C55E) else Color(0xFF94A3B8))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    val statusTxt = if (resolvedIsOnline) "Online" else "Offline"
                                    val displayLocation = if (resolvedCountryName.isNotBlank()) "$statusTxt · $resolvedCountryName" else statusTxt
                                    Text(
                                        text = displayLocation,
                                        color = colors.textSecondary,
                                        fontSize = 12f.chatSp()
                                    )
                                }
                            }

                            Box {
                                IconButton(onClick = { showTopMenu = true }) {
                                    com.example.ui.components.SoftThreeDotsIcon(size = 18.dp, color = colors.text)
                                }
                                DropdownMenu(
                                    expanded = showTopMenu,
                                    onDismissRequest = { showTopMenu = false },
                                    modifier = Modifier.background(colors.surface)
                                ) {
                                    if (partnerUserId.isNotBlank()) {
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    com.example.ui.components.SoftBlockIcon(size = 16.dp, color = Color(0xFFEF4444))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (isPartnerBlockedLocally) "Unblock user" else "Block user",
                                                        color = Color(0xFFEF4444),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            },
                                            onClick = {
                                                showTopMenu = false
                                                if (isPartnerBlockedLocally) {
                                                    coroutineScope.launch {
                                                        val res = BlockRepository.unblockUser(context, partnerUserId)
                                                        res.onSuccess {
                                                            isChatBlocked = false
                                                            Toast.makeText(context, "$partnerName unblocked", Toast.LENGTH_SHORT).show()
                                                        }.onFailure { err ->
                                                            Toast.makeText(context, err.message ?: "Could not unblock user.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } else {
                                                    showBlockConfirmDialog = true
                                                }
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                com.example.ui.components.SoftShieldIcon(size = 16.dp, color = Color(0xFFEF4444))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Report", color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                                            }
                                        },
                                        onClick = {
                                            showTopMenu = false
                                            showReportUserDialog = true
                                        }
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
                        .background(colors.surface)
                        .imePadding()
                ) {
                    if (isChatBlocked || isPartnerBlockedLocally) {
                        Surface(
                            color = colors.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = USER_BLOCKED_ERROR_MESSAGE,
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
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
                                ensureVaultUnlocked {
                                    inputText = ""
                                    replyingToMessage = null
                                    isEmojiPickerOpen = false
                                    coroutineScope.launch {
                                        val res = EncryptedChatRepository.sendMessage(
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
                                        if (res.isFailure) {
                                            Toast.makeText(
                                                context,
                                                res.exceptionOrNull()?.message ?: "Could not send message.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else if (timelineItems.isNotEmpty()) {
                                            listState.animateScrollToItem(timelineItems.size - 1)
                                        }
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
                                    onCancel = { voiceRecorder.cancel() },
                                    onSend = {
                                        val result = voiceRecorder.stop()
                                        if (result != null && result.bytes.isNotEmpty()) {
                                            coroutineScope.launch {
                                                val sendRes = EncryptedChatRepository.sendMediaMessage(
                                                    context = context,
                                                    recipientUserId = partnerUserId,
                                                    recipientName = partnerName,
                                                    recipientAvatar = partnerAvatarUrl,
                                                    rawBytes = result.bytes,
                                                    messageType = "voice",
                                                    mimeType = "audio/aac",
                                                    durationSeconds = (result.durationMs / 1000).toInt().coerceAtLeast(1)
                                                )
                                                if (sendRes.isFailure) {
                                                    val err = sendRes.exceptionOrNull()
                                                    if (err is UserBlockedException) isChatBlocked = true
                                                    Toast.makeText(context, err?.message ?: "Could not send voice note.", Toast.LENGTH_SHORT).show()
                                                } else if (timelineItems.isNotEmpty()) {
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
                                                val sendRes = EncryptedChatRepository.sendMediaMessage(
                                                    context = context,
                                                    recipientUserId = partnerUserId,
                                                    recipientName = partnerName,
                                                    recipientAvatar = partnerAvatarUrl,
                                                    rawBytes = result.bytes,
                                                    messageType = "voice",
                                                    mimeType = "audio/aac",
                                                    durationSeconds = (result.durationMs / 1000).toInt().coerceAtLeast(1)
                                                )
                                                if (sendRes.isFailure) {
                                                    val err = sendRes.exceptionOrNull()
                                                    if (err is UserBlockedException) isChatBlocked = true
                                                    Toast.makeText(context, err?.message ?: "Could not send voice note.", Toast.LENGTH_SHORT).show()
                                                }
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
                }
            },
            containerColor = colors.wallpaper
        ) { paddingValues ->
            val myCosmetics by com.example.store.CosmeticsManager.myCosmetics.collectAsState()
            val myChatBgConfig = remember(myCosmetics) {
                myCosmetics?.chat_bg?.config?.let {
                    runCatching { com.example.store.CosmeticsConfigParser.parseChatBg(it) }.getOrNull()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (myChatBgConfig != null) {
                    com.example.store.ChatBackground(config = myChatBgConfig, isDark = false, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(colors.wallpaper))
                }
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
                                                        EncryptedChatRepository.sendDmReaction(context, conversationId, partnerUserId, msg.id, emoji)
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
                        EncryptedChatRepository.sendDmReaction(context, conversationId, partnerUserId, selectedMsg.id, emoji)
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
                            coroutineScope.launch {
                                EncryptedChatRepository.deleteConversationLocally(conversationId)
                                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                            }
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

        // Report User Dialog
        if (showReportUserDialog) {
            val last20ProofMessages = remember(rawMessages) {
                rawMessages.takeLast(20).map { msg ->
                    val type = when {
                        msg.text.startsWith("[img]") || msg.text.startsWith("[imgs]") -> "image"
                        msg.text.startsWith("[voice]") -> "voice"
                        else -> "text"
                    }
                    val textVal = if (type == "text") msg.text else null
                    val isoTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.format(java.util.Date(msg.timestamp))
                    mapOf<String, Any?>(
                        "sender" to if (msg.isOutgoing) "me" else "them",
                        "type" to type,
                        "text" to textVal,
                        "sent_at" to isoTime
                    )
                }
            }

            ReportUserDialog(
                reportedUserId = partnerUserId,
                reportedUserName = partnerName,
                contextType = "dm",
                contextId = conversationId.ifBlank { partnerUserId },
                dmProofMessages = last20ProofMessages,
                existingScreenshotUrl = null,
                onDismiss = { showReportUserDialog = false },
                onReportSubmitted = { showReportUserDialog = false }
            )
        }

        if (showVaultGateDialog) {
            VaultManagementDialogs(
                onDismissRequest = {
                    showVaultGateDialog = false
                    pendingVaultAction = null
                }
            )
        }

        if (showBlockConfirmDialog) {
            BlockUserConfirmDialog(
                displayName = partnerName,
                isBlocking = isBlockingUser,
                onDismiss = { if (!isBlockingUser) showBlockConfirmDialog = false },
                onConfirm = {
                    isBlockingUser = true
                    coroutineScope.launch {
                        val res = BlockRepository.blockUser(context, partnerUserId)
                        isBlockingUser = false
                        showBlockConfirmDialog = false
                        res.onSuccess {
                            Toast.makeText(context, "$partnerName blocked", Toast.LENGTH_SHORT).show()
                            onBack()
                        }.onFailure { err ->
                            Toast.makeText(context, err.message ?: "Could not block user.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}
