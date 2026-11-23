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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.crypto.ChatTimingTracker
import com.example.db.LocalDmConversation
import com.example.db.LocalDmMessage
import com.example.db.LocalDmMessageReaction
import com.example.db.LocalMessageStatus
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

private fun mapDmStatus(msg: LocalDmMessage): MessageStatus? {
    if (!msg.isOutgoing) return null
    return when (msg.status) {
        LocalMessageStatus.SENDING -> MessageStatus.Sending
        LocalMessageStatus.FAILED -> MessageStatus.Failed
        else -> MessageStatus.Sent
    }
}

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
        override val id: String = message.clientId?.takeIf { it.isNotBlank() } ?: message.id
    }

    data class MediaAlbumItem(
        val messages: List<LocalDmMessage>,
        val photos: List<ChatPhotoNode>,
        val position: GroupPosition,
        val reactions: List<ReactionUi>,
        val isFirstInGroup: Boolean,
        val isLastInGroup: Boolean
    ) : DmTimelineItem {
        override val id: String = "album_${messages.first().clientId?.takeIf { it.isNotBlank() } ?: messages.first().id}"
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
    embeddedInSheet: Boolean = false,
    onBack: () -> Unit
) {
    com.example.ui.components.SecureWindow(enabled = true)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
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

    var conversationId by remember(partnerUserId) {
        mutableStateOf(
            EncryptedChatRepository.conversations.value
                .firstOrNull { it.otherUserId == partnerUserId }
                ?.id
                .orEmpty()
        )
    }
    var rawMessages by remember { mutableStateOf<List<LocalDmMessage>>(emptyList()) }
    val optimisticMessagesAll by EncryptedChatRepository.optimisticMessages.collectAsState()
    val conversationAliases by EncryptedChatRepository.conversationIdAliases.collectAsState()
    var rawReactions by remember { mutableStateOf<List<LocalDmMessageReaction>>(emptyList()) }
    var currentUserId by remember { mutableStateOf("") }

    var inputText by remember { mutableStateOf("") }
    var isEmojiPickerOpen by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<LocalDmMessage?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<LocalDmMessage?>(null) }
    var failedMessageForSheet by remember { mutableStateOf<LocalDmMessage?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedMessageIds = remember { mutableStateListOf<String>() }

    var activeWordLookup by remember { mutableStateOf<WordLookupState?>(null) }
    var showAiAssistSheet by remember { mutableStateOf(false) }
    var activeMediaViewerIndex by remember { mutableStateOf<Int?>(null) }
    var pendingPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
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

    // Photo picker launcher -> opens Telegram-style review & caption sheet before sending
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val merged = (pendingPhotoUris + uris).distinct().take(10)
            pendingPhotoUris = merged
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

    LaunchedEffect(conversationAliases, conversationId) {
        val mapped = conversationAliases[conversationId]
        if (!mapped.isNullOrBlank() && mapped != conversationId) {
            conversationId = mapped
        }
    }

    LaunchedEffect(conversationId, vaultStatus) {
        if (conversationId.isNotBlank() && vaultStatus == com.example.crypto.VaultStatus.UNLOCKED) {
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

    val mergedMessages = remember(rawMessages, optimisticMessagesAll, conversationId, partnerUserId, conversationAliases) {
        val existingKeys = HashSet<String>(rawMessages.size * 2)
        rawMessages.forEach { m ->
            existingKeys.add(m.id)
            m.clientId?.takeIf { it.isNotBlank() }?.let { existingKeys.add(it) }
        }
        val extraOptimistic = optimisticMessagesAll.filter { opt ->
            val optKey = opt.clientId?.takeIf { it.isNotBlank() } ?: opt.id
            val matchesConv = (conversationId.isNotBlank() && (opt.conversationId == conversationId || conversationAliases[opt.conversationId] == conversationId)) ||
                (partnerUserId.isNotBlank() && opt.recipientId == partnerUserId)
            matchesConv && !existingKeys.contains(opt.id) && !existingKeys.contains(optKey)
        }
        if (extraOptimistic.isEmpty()) {
            rawMessages
        } else {
            (rawMessages + extraOptimistic).sortedBy { it.timestamp }
        }
    }

    val seenBubbleKeys = remember(partnerUserId) { HashSet<String>() }
    var hasInitialHistoryLoaded by remember(partnerUserId) { mutableStateOf(false) }
    LaunchedEffect(mergedMessages.size) {
        if (!hasInitialHistoryLoaded && mergedMessages.isNotEmpty()) {
            mergedMessages.forEach { m ->
                seenBubbleKeys.add(m.clientId?.takeIf { it.isNotBlank() } ?: m.id)
            }
            hasInitialHistoryLoaded = true
        }
    }

    ProvideChatTheme(
        preset = chatPreset,
        darkTheme = false,
        textScale = chatTextScale
    ) {
        val colors = LocalChatColors.current

        val timelineItems = remember(mergedMessages, rawReactions, currentUserId) {
            val sorted = mergedMessages.sortedBy { it.timestamp }
            val items = mutableListOf<DmTimelineItem>()
            items.add(DmTimelineItem.SecurityHeader("Messages and calls are end-to-end encrypted. No one outside of this chat can read or listen to them."))

            var prevDateLabel = ""
            var i = 0
            while (i < sorted.size) {
                val msg = sorted[i]
                val dateLabel = formatChatDateChip(msg.timestamp)
                if (dateLabel != prevDateLabel) {
                    items.add(DmTimelineItem.DateHeader(dateLabel))
                    prevDateLabel = dateLabel
                }

                // Group consecutive same-sender images (within 60s, max 10) into a Telegram album collage
                if (isDmImageText(msg.text)) {
                    val albumMsgs = mutableListOf(msg)
                    var j = i + 1
                    while (j < sorted.size && albumMsgs.size < 10) {
                        val next = sorted[j]
                        if (isDmImageText(next.text) &&
                            next.isOutgoing == msg.isOutgoing &&
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
                        sorted[i - 1].isOutgoing == msg.isOutgoing &&
                        (msg.timestamp - sorted[i - 1].timestamp) <= 2 * 60 * 1000L &&
                        formatChatDateChip(sorted[i - 1].timestamp) == dateLabel

                    val isNewerSameSender = j < sorted.size &&
                        sorted[j].isOutgoing == lastInAlbum.isOutgoing &&
                        (sorted[j].timestamp - lastInAlbum.timestamp) <= 2 * 60 * 1000L &&
                        formatChatDateChip(sorted[j].timestamp) == dateLabel

                    val position = computeGroupPosition(isOlderSameSender, isNewerSameSender)
                    val firstReactions = rawReactions.filter { it.targetMessageId == msg.id }
                        .groupBy { it.emoji }
                        .map { (emoji, list) ->
                            ReactionUi(
                                emoji = emoji,
                                count = list.size,
                                mine = list.any { it.senderId == currentUserId }
                            )
                        }
                    val photoNodes = albumMsgs.map { m ->
                        parseDmPhotoNode(
                            id = m.id,
                            rawText = m.text,
                            timestamp = m.timestamp,
                            isOutgoing = m.isOutgoing
                        )
                    }
                    items.add(
                        DmTimelineItem.MediaAlbumItem(
                            messages = albumMsgs,
                            photos = photoNodes,
                            position = position,
                            reactions = firstReactions,
                            isFirstInGroup = !isOlderSameSender,
                            isLastInGroup = !isNewerSameSender
                        )
                    )
                    i = j
                    continue
                }

                val isOlderSameSender = i > 0 &&
                    sorted[i - 1].isOutgoing == msg.isOutgoing &&
                    (msg.timestamp - sorted[i - 1].timestamp) <= 2 * 60 * 1000L &&
                    formatChatDateChip(sorted[i - 1].timestamp) == dateLabel

                val isNewerSameSender = i < sorted.size - 1 &&
                    sorted[i + 1].isOutgoing == msg.isOutgoing &&
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
                    DmTimelineItem.MessageItem(
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

        val allMediaItems = remember(mergedMessages, partnerName) {
            mergedMessages.sortedBy { it.timestamp }
                .filter { isDmImageText(it.text) }
                .map { msg ->
                    val node = parseDmPhotoNode(msg.id, msg.text, msg.timestamp, msg.isOutgoing)
                    ChatViewerMediaItem(
                        id = msg.id,
                        url = node.source,
                        senderName = if (msg.isOutgoing) "You" else partnerName,
                        dateText = node.timeText,
                        caption = node.caption,
                        isVideo = false
                    )
                }
        }

        val unreadCount = remember(mergedMessages) {
            mergedMessages.count { !it.isOutgoing }
        }

        BackHandler {
            if (isEmojiPickerOpen) {
                isEmojiPickerOpen = false
            } else if (failedMessageForSheet != null) {
                failedMessageForSheet = null
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
            contentWindowInsets = if (embeddedInSheet) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
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
                                            EncryptedChatRepository.deleteDmMessage(context, id)
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
                                .then(if (embeddedInSheet) Modifier else Modifier.statusBarsPadding())
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                ChatIconBack(tint = colors.text, modifier = Modifier.size(24.dp))
                            }

                            com.example.ui.components.PresenceAvatar(
                                avatarUrl = resolvedAvatarUrl.ifBlank { null },
                                size = 40.dp,
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
                                    shape = RoundedCornerShape(20.dp),
                                    containerColor = colors.surface,
                                    shadowElevation = 10.dp,
                                    modifier = Modifier
                                        .widthIn(min = 200.dp)
                                        .border(1.dp, colors.hairline, RoundedCornerShape(20.dp))
                                ) {
                                    if (partnerUserId.isNotBlank()) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (isPartnerBlockedLocally) "Unblock user" else "Block user",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    com.example.ui.components.SoftBlockIcon(size = 17.dp, color = Color(0xFFEF4444))
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
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                            thickness = 0.6.dp,
                                            color = colors.hairline
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Report",
                                                color = Color(0xFFEF4444),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                com.example.ui.components.SoftShieldIcon(size = 17.dp, color = Color(0xFFEF4444))
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
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
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
                                val tapMs = ChatTimingTracker.nowMs()
                                ensureVaultUnlocked {
                                    inputText = ""
                                    replyingToMessage = null
                                    isEmojiPickerOpen = false
                                    val activeConvId = conversationId.ifBlank { "dm_pending_$partnerUserId" }
                                    EncryptedChatRepository.enqueueSendMessage(
                                        context = context,
                                        conversationId = activeConvId,
                                        recipientUserId = partnerUserId,
                                        recipientName = partnerName,
                                        recipientAvatar = partnerAvatarUrl,
                                        text = textToSend,
                                        replyToText = reply?.text,
                                        replyToSender = if (reply != null) {
                                            if (reply.isOutgoing) "You" else partnerName
                                        } else null,
                                        tapElapsedMs = tapMs
                                    )
                                    if (timelineItems.isNotEmpty()) {
                                        coroutineScope.launch {
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
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val tapMs = ChatTimingTracker.nowMs()
                                            ensureVaultUnlocked {
                                                val activeConvId = conversationId.ifBlank { "dm_pending_$partnerUserId" }
                                                EncryptedChatRepository.enqueueSendMediaMessage(
                                                    context = context,
                                                    conversationId = activeConvId,
                                                    recipientUserId = partnerUserId,
                                                    recipientName = partnerName,
                                                    recipientAvatar = partnerAvatarUrl,
                                                    rawBytes = result.bytes,
                                                    messageType = "voice",
                                                    mimeType = "audio/aac",
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
                                            ensureVaultUnlocked {
                                                val activeConvId = conversationId.ifBlank { "dm_pending_$partnerUserId" }
                                                EncryptedChatRepository.enqueueSendMediaMessage(
                                                    context = context,
                                                    conversationId = activeConvId,
                                                    recipientUserId = partnerUserId,
                                                    recipientName = partnerName,
                                                    recipientAvatar = partnerAvatarUrl,
                                                    rawBytes = result.bytes,
                                                    messageType = "voice",
                                                    mimeType = "audio/aac",
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
                            is DmTimelineItem.MediaAlbumItem -> {
                                val firstMsg = item.messages.first()
                                val isOutgoing = firstMsg.isOutgoing
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
                                            ChatTimingTracker.markBubbleShown("DM", m.clientId ?: m.id)
                                        }
                                    }
                                }

                                AnimatedBubbleEntrance(animateEntrance = isNewItem, outgoing = isOutgoing) {
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
                                        if (!isOutgoing && (item.position == GroupPosition.Single || item.position == GroupPosition.First)) {
                                            SenderName(
                                                name = partnerName,
                                                userId = partnerUserId,
                                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
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
                                                replyQuote = if (!firstMsg.replyToText.isNullOrBlank()) {
                                                    {
                                                        ReplyQuote(
                                                            sender = firstMsg.replyToSender ?: "Reply",
                                                            preview = firstMsg.replyToText,
                                                            outgoing = isOutgoing,
                                                            onClick = {
                                                                val replyKey = firstMsg.replyToText.orEmpty()
                                                                val targetIdx = timelineItems.indexOfFirst { tl ->
                                                                    when (tl) {
                                                                        is DmTimelineItem.MessageItem -> tl.message.text == replyKey
                                                                        is DmTimelineItem.MediaAlbumItem -> tl.messages.any { m ->
                                                                            m.text == replyKey ||
                                                                                (replyKey == "📷 Photo" && isDmImageText(m.text)) ||
                                                                                (replyKey.startsWith("📷 ") && parseDmPhotoNode(m.id, m.text, m.timestamp, isOutgoing).caption == replyKey.removePrefix("📷 ").trim())
                                                                        }
                                                                        else -> false
                                                                    }
                                                                }
                                                                if (targetIdx != -1) {
                                                                    val flashTargetId = when (val found = timelineItems[targetIdx]) {
                                                                        is DmTimelineItem.MessageItem -> found.message.id
                                                                        is DmTimelineItem.MediaAlbumItem -> found.messages.first().id
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
                                                }
                                            )
                                        }

                                        if (item.reactions.isNotEmpty()) {
                                            ReactionRow(
                                                reactions = item.reactions,
                                                onClick = { emoji ->
                                                    coroutineScope.launch {
                                                        EncryptedChatRepository.sendDmReaction(context, conversationId, partnerUserId, firstMsg.id, emoji)
                                                    }
                                                },
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                                }
                            }
                            is DmTimelineItem.MessageItem -> {
                                val msg = item.message
                                val isOutgoing = msg.isOutgoing
                                val flashAlpha = flashAnimatables[msg.id]?.value ?: 0f
                                val isSelected = selectedMessageIds.contains(msg.id)
                                val status = mapDmStatus(msg)
                                val itemKey = msg.clientId?.takeIf { it.isNotBlank() } ?: msg.id
                                val isNewItem = remember(itemKey) { hasInitialHistoryLoaded && seenBubbleKeys.add(itemKey) }
                                SideEffect {
                                    if (BuildConfig.DEBUG && isOutgoing) {
                                        ChatTimingTracker.markBubbleShown("DM", msg.clientId ?: msg.id)
                                    }
                                }

                                val isOnlyEmojis = emojiOnlyCount(msg.text.trim())

                                AnimatedBubbleEntrance(animateEntrance = isNewItem, outgoing = isOutgoing) {
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
                                                            val replyKey = msg.replyToText.orEmpty()
                                                            val targetIdx = timelineItems.indexOfFirst { tl ->
                                                                when (tl) {
                                                                    is DmTimelineItem.MessageItem -> tl.message.text == replyKey
                                                                    is DmTimelineItem.MediaAlbumItem -> tl.messages.any { m ->
                                                                        m.text == replyKey ||
                                                                            (replyKey == "📷 Photo" && isDmImageText(m.text)) ||
                                                                            (replyKey.startsWith("📷 ") && parseDmPhotoNode(m.id, m.text, m.timestamp, isOutgoing).caption == replyKey.removePrefix("📷 ").trim())
                                                                    }
                                                                    else -> false
                                                                }
                                                            }
                                                            if (targetIdx != -1) {
                                                                val flashTargetId = when (val found = timelineItems[targetIdx]) {
                                                                    is DmTimelineItem.MessageItem -> found.message.id
                                                                    is DmTimelineItem.MediaAlbumItem -> found.messages.first().id
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
                                EncryptedChatRepository.deleteDmMessage(context, selectedMsg.id)
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
                    EncryptedChatRepository.retryDmMessage(context, failedTarget.id)
                },
                onDelete = {
                    failedMessageForSheet = null
                    coroutineScope.launch {
                        EncryptedChatRepository.deleteDmMessage(context, failedTarget.id)
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
                recipientLabel = partnerName,
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
                onSend = { urisToSend, captionText ->
                    val replySnapshot = replyingToMessage
                    val tapMs = ChatTimingTracker.nowMs()
                    pendingPhotoUris = emptyList()
                    replyingToMessage = null
                    ensureVaultUnlocked {
                        val activeConvId = conversationId.ifBlank { "dm_pending_$partnerUserId" }
                        coroutineScope.launch(Dispatchers.IO) {
                            urisToSend.forEachIndexed { idx, uri ->
                                try {
                                    val bytes = readChatImageBytes(context, uri)
                                    if (bytes != null && bytes.isNotEmpty()) {
                                        EncryptedChatRepository.enqueueSendMediaMessage(
                                            context = context,
                                            conversationId = activeConvId,
                                            recipientUserId = partnerUserId,
                                            recipientName = partnerName,
                                            recipientAvatar = partnerAvatarUrl,
                                            rawBytes = bytes,
                                            messageType = "image",
                                            mimeType = "image/jpeg",
                                            replyToText = if (idx == 0) replySnapshot?.text else null,
                                            replyToSender = if (idx == 0 && replySnapshot != null) {
                                                if (replySnapshot.isOutgoing) "You" else partnerName
                                            } else null,
                                            caption = if (idx == 0) captionText.takeIf { it.isNotBlank() } else null,
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
                }
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
            val last20ProofMessages = remember(mergedMessages) {
                mergedMessages.takeLast(20).map { msg ->
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

        if (showVaultGateDialog || vaultStatus != com.example.crypto.VaultStatus.UNLOCKED) {
            VaultManagementDialogs(
                onDismissRequest = {
                    showVaultGateDialog = false
                    pendingVaultAction = null
                    if (com.example.crypto.VaultSecurityManager.isVaultLocked()) {
                        onBack()
                    }
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
