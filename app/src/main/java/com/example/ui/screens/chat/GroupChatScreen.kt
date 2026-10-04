package com.example.ui.screens.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.SupabaseGroupMember
import com.example.SupabaseSessionManager
import com.example.db.LocalGroupConversation
import com.example.db.LocalGroupMessage
import com.example.repository.GroupChatRepository
import com.example.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GroupChatScreen(
    group: LocalGroupConversation,
    onBack: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToManageMembers: (openAddSheet: Boolean) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myUserId = remember { SupabaseSessionManager.getUserId(context) ?: "" }

    val isAdmin = group.myRole.equals("admin", ignoreCase = true)
    val canAddMembers = isAdmin || group.whoCanAddMembers.equals("members", ignoreCase = true)
    val isSendingRestricted = group.whoCanSend.equals("admins", ignoreCase = true) && !isAdmin
    val isMuted = remember(group.mutedUntil) {
        (group.mutedUntil ?: 0L) > System.currentTimeMillis()
    }

    val messagesFlow = remember(group.id) { GroupChatRepository.getGroupMessagesFlow(group.id) }
    val messages by messagesFlow.collectAsState(initial = emptyList())

    val visibleMessages = remember(messages, group.showJoinNotifications) {
        messages.filter { msg ->
            if (msg.messageType == "system" || msg.senderId == "system") {
                val evType = msg.eventType?.lowercase().orEmpty()
                val isJoinOrLeave = evType == "joined" || evType == "left" ||
                    (evType.isBlank() && (msg.text.endsWith("joined the group") || msg.text.endsWith("left the group")))
                if (isJoinOrLeave) {
                    group.showJoinNotifications
                } else {
                    true
                }
            } else {
                true
            }
        }
    }

    var groupMembers by remember { mutableStateOf<List<SupabaseGroupMember>>(emptyList()) }
    val selectedMentions = remember { mutableStateMapOf<String, String>() }
    var replyingToMessage by remember { mutableStateOf<LocalGroupMessage?>(null) }

    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isUploadingMedia by remember { mutableStateOf(false) }
    var showHeaderMenu by remember { mutableStateOf(false) }
    var showInviteSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showMediaLinksSheet by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    // Voice recording state
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedVoiceFile by remember { mutableStateOf<File?>(null) }

    val listState = rememberLazyListState()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && !isSendingRestricted) {
            scope.launch {
                isUploadingMedia = true
                try {
                    val (rawBytes, mime) = withContext(Dispatchers.IO) {
                        val b = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        val m = context.contentResolver.getType(uri) ?: "image/jpeg"
                        b to m
                    }
                    if (rawBytes != null && rawBytes.isNotEmpty()) {
                        val res = GroupChatRepository.sendGroupMediaMessage(
                            context = context,
                            groupId = group.id,
                            rawBytes = rawBytes,
                            mimeType = mime,
                            messageType = "image"
                        )
                        res.onSuccess { outcome ->
                            if (outcome.skippedCount > 0) {
                                Toast.makeText(
                                    context,
                                    "Photo sent (${outcome.skippedCount} member(s) skipped: no E2EE key)",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }.onFailure { err ->
                            Toast.makeText(context, err.message ?: "Failed to send photo", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error encrypting photo: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingMedia = false
                }
            }
        }
    }

    fun startVoiceRecording() {
        try {
            val outFile = File(context.cacheDir, "group_voice_${System.currentTimeMillis()}.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            recordedVoiceFile = outFile
            recordingSeconds = 0
            isRecordingVoice = true
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to start microphone: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopAndSendVoiceRecording(cancel: Boolean = false) {
        val recorder = mediaRecorder
        val file = recordedVoiceFile
        mediaRecorder = null
        recordedVoiceFile = null
        isRecordingVoice = false
        try {
            recorder?.stop()
        } catch (_: Exception) {}
        try {
            recorder?.release()
        } catch (_: Exception) {}

        if (cancel || file == null || !file.exists() || file.length() < 200L) {
            file?.delete()
            return
        }

        scope.launch {
            isUploadingMedia = true
            try {
                val bytes = withContext(Dispatchers.IO) { file.readBytes() }
                file.delete()
                val res = GroupChatRepository.sendGroupMediaMessage(
                    context = context,
                    groupId = group.id,
                    rawBytes = bytes,
                    mimeType = "audio/mp4",
                    messageType = "voice"
                )
                res.onSuccess { outcome ->
                    if (outcome.skippedCount > 0) {
                        Toast.makeText(
                            context,
                            "Voice note sent (${outcome.skippedCount} member(s) skipped: no E2EE key)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "Failed to send voice message", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isUploadingMedia = false
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceRecording()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isRecordingVoice) {
        while (isRecordingVoice) {
            delay(1000L)
            recordingSeconds++
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(group.id, lifecycleOwner) {
        GroupChatRepository.setActiveOpenGroup(group.id)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    GroupChatRepository.markGroupRead(group.id)
                    GroupChatRepository.getGroupOverview(context, group.id)
                    GroupChatRepository.syncGroupEvents(context, group.id)
                    GroupChatRepository.fetchAndProcessPendingGroupMessages(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            GroupChatRepository.setActiveOpenGroup(null)
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (_: Exception) {}
        }
    }

    BackHandler {
        if (isRecordingVoice) {
            stopAndSendVoiceRecording(cancel = true)
        } else {
            onBack()
        }
    }

    suspend fun refreshOverviewMembersAndEvents() {
        GroupChatRepository.markGroupRead(group.id)
        GroupChatRepository.getGroupOverview(context, group.id)
        val memRes = GroupChatRepository.getGroupMembersResult(context, group.id)
        memRes.onSuccess { list ->
            groupMembers = list.filter { it.status.equals("joined", ignoreCase = true) }
        }
        GroupChatRepository.syncGroupEvents(context, group.id)
        GroupChatRepository.fetchAndProcessPendingGroupMessages(context)
    }

    LaunchedEffect(group.id) {
        refreshOverviewMembersAndEvents()
    }

    LaunchedEffect(group.id) {
        GroupChatRepository.liveGroupUpdates.collect { updatedId ->
            if (updatedId == group.id) {
                GroupChatRepository.getGroupOverview(context, group.id)
                GroupChatRepository.getGroupMembersResult(context, group.id).onSuccess { list ->
                    groupMembers = list.filter { it.status.equals("joined", ignoreCase = true) }
                }
            }
        }
    }

    LaunchedEffect(visibleMessages.size) {
        if (visibleMessages.isNotEmpty()) {
            listState.animateScrollToItem(0)
            GroupChatRepository.markGroupRead(group.id)
        }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAF9),
        topBar = {
            Surface(
                color = Color(0xFFFAFAF9),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            GroupBackIcon(size = 20.dp, color = Color(0xFF0F172A))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigateToInfo() }
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7))
                                    .border(1.5.dp, Color(0xFFFDE68A), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (group.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = group.avatarUrl,
                                        contentDescription = group.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    GroupUsersIcon(size = 22.dp, color = Color(0xFFB45309))
                                }
                            }

                            Spacer(modifier = Modifier.width(11.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.name,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val memberCountLabel = if (group.memberCount == 1) "1 member" else "${group.memberCount} members"
                                val extraBadge = buildString {
                                    append(memberCountLabel)
                                    if (group.disappearingSeconds > 0) {
                                        append(" · ⏱ ")
                                        append(GroupChatRepository.formatDisappearingLabel(group.disappearingSeconds))
                                    } else {
                                        append(" · E2EE")
                                    }
                                }
                                Text(
                                    text = extraBadge,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 3-Dots Header Menu
                    Box {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .clickable { showHeaderMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            GroupThreeDotsIcon(size = 20.dp, color = Color(0xFF0F172A))
                        }

                        DropdownMenu(
                            expanded = showHeaderMenu,
                            onDismissRequest = { showHeaderMenu = false },
                            modifier = Modifier
                                .width(250.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Group info",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                },
                                leadingIcon = { GroupInfoCircleIcon(size = 19.dp, color = Color(0xFF0F172A)) },
                                onClick = {
                                    showHeaderMenu = false
                                    onNavigateToInfo()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Manage members (${group.memberCount})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                },
                                leadingIcon = { GroupUsersIcon(size = 19.dp, color = Color(0xFF0F172A)) },
                                onClick = {
                                    showHeaderMenu = false
                                    onNavigateToManageMembers(false)
                                }
                            )

                            if (canAddMembers) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Add members",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    },
                                    leadingIcon = { GroupPersonAddIcon(size = 19.dp, color = Color(0xFF0F172A)) },
                                    onClick = {
                                        showHeaderMenu = false
                                        onNavigateToManageMembers(true)
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Invite link",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                },
                                leadingIcon = { GroupLinkIcon(size = 19.dp, color = Color(0xFF0F172A)) },
                                onClick = {
                                    showHeaderMenu = false
                                    showInviteSheet = true
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Search in conversation",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                },
                                leadingIcon = { GroupSearchIcon(size = 19.dp, color = Color(0xFF0F172A)) },
                                onClick = {
                                    showHeaderMenu = false
                                    showSearchSheet = true
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Media, links & files",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                },
                                leadingIcon = { GroupMediaIcon(size = 19.dp, color = Color(0xFF0F172A)) },
                                onClick = {
                                    showHeaderMenu = false
                                    showMediaLinksSheet = true
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isMuted) "Unmute notifications" else "Mute notifications",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                },
                                leadingIcon = { GroupBellIcon(size = 19.dp, color = Color(0xFF0F172A)) },
                                onClick = {
                                    showHeaderMenu = false
                                    scope.launch {
                                        val res = if (isMuted) {
                                            GroupChatRepository.updateMyGroupPrefs(
                                                context = context,
                                                groupId = group.id,
                                                clearMute = true
                                            )
                                        } else {
                                            GroupChatRepository.updateMyGroupPrefs(
                                                context = context,
                                                groupId = group.id,
                                                mutedUntilIso = "2099-12-31T23:59:59Z",
                                                clearMute = false
                                            )
                                        }
                                        res.onSuccess {
                                            Toast.makeText(
                                                context,
                                                if (isMuted) "Notifications unmuted" else "Notifications muted",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.message ?: "Failed to update mute", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Disappearing messages (${GroupChatRepository.formatDisappearingLabel(group.disappearingSeconds)})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                },
                                leadingIcon = { GroupClockIcon(size = 19.dp, color = Color(0xFF0F172A)) },
                                onClick = {
                                    showHeaderMenu = false
                                    if (isAdmin) {
                                        showDisappearingDialog = true
                                    } else {
                                        onNavigateToSettings()
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Group settings",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                },
                                leadingIcon = { GroupGearIcon(size = 19.dp, color = Color(0xFF0F172A)) },
                                onClick = {
                                    showHeaderMenu = false
                                    onNavigateToSettings()
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = Color(0xFFF1F5F9)
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Clear chat (this device)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                },
                                leadingIcon = { GroupBroomIcon(size = 19.dp, color = Color(0xFF64748B)) },
                                onClick = {
                                    showHeaderMenu = false
                                    showClearChatDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Report group",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFEF4444)
                                    )
                                },
                                leadingIcon = { GroupShieldIcon(size = 19.dp, color = Color(0xFFEF4444)) },
                                onClick = {
                                    showHeaderMenu = false
                                    showReportDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Leave group",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFEF4444)
                                    )
                                },
                                leadingIcon = { GroupLeaveDoorIcon(size = 19.dp, color = Color(0xFFEF4444)) },
                                onClick = {
                                    showHeaderMenu = false
                                    showLeaveDialog = true
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color(0xFFFAFAF9),
                shadowElevation = 0.dp
            ) {
                if (isSendingRestricted) {
                    // Disabled composer notice when who_can_send = 'admins' and user is not admin
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GroupLockIcon(size = 16.dp, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Only admins can send messages in this group",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                } else if (isRecordingVoice) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { stopAndSendVoiceRecording(cancel = true) }) {
                            Text("Cancel", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(Locale.US, "Recording %02d:%02d", recordingSeconds / 60, recordingSeconds % 60),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Button(
                            onClick = { stopAndSendVoiceRecording(cancel = false) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC529), contentColor = Color.Black)
                        ) {
                            Text("Send", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Determine if typing "@" should show the member picker
                    val activeMentionQuery: String? = remember(inputText, selectedMentions.values.toList()) {
                        val lastAt = inputText.lastIndexOf('@')
                        if (lastAt >= 0 && (lastAt == 0 || inputText[lastAt - 1].isWhitespace())) {
                            val afterAt = inputText.substring(lastAt + 1)
                            val alreadyCompleted = selectedMentions.values.any { name ->
                                afterAt.startsWith("$name ", ignoreCase = true)
                            }
                            if (!alreadyCompleted && !afterAt.contains('\n') && afterAt.length <= 32) {
                                afterAt
                            } else null
                        } else null
                    }

                    val mentionCandidates = remember(activeMentionQuery, groupMembers, myUserId) {
                        if (activeMentionQuery == null) {
                            emptyList()
                        } else {
                            val q = activeMentionQuery.trim()
                            groupMembers.filter { m ->
                                m.user_id != myUserId && (
                                    q.isEmpty() ||
                                        (m.profile_name?.contains(q, ignoreCase = true) == true) ||
                                        (m.username?.contains(q, ignoreCase = true) == true)
                                )
                            }.take(6)
                        }
                    }

                    val knownMentionNames = remember(groupMembers, selectedMentions.values.toList()) {
                        buildSet {
                            addAll(selectedMentions.values)
                            groupMembers.forEach { m ->
                                m.profile_name?.takeIf { it.isNotBlank() }?.let { add(it) }
                                m.username?.takeIf { it.isNotBlank() }?.let { add(it) }
                            }
                        }
                    }

                    val mentionVisualTransformation = remember(knownMentionNames) {
                        VisualTransformation { text ->
                            val raw = text.text
                            val annotated = buildAnnotatedString {
                                append(raw)
                                knownMentionNames.forEach { handle ->
                                    val token = "@$handle"
                                    var startIdx = raw.indexOf(token, ignoreCase = true)
                                    while (startIdx >= 0) {
                                        val endIdx = startIdx + token.length
                                        addStyle(
                                            SpanStyle(
                                                color = Color(0xFFB45309),
                                                fontWeight = FontWeight.ExtraBold,
                                                background = Color(0xFFFEF3C7)
                                            ),
                                            startIdx,
                                            endIdx
                                        )
                                        startIdx = raw.indexOf(token, startIdx + 1, ignoreCase = true)
                                    }
                                }
                            }
                            TransformedText(annotated, OffsetMapping.Identity)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        // @Mention Member Picker Popup above composer
                        if (mentionCandidates.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                shadowElevation = 6.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 210.dp)
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Mention a member",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                    )
                                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                        items(mentionCandidates, key = { it.user_id }) { member ->
                                            val displayName = member.profile_name?.takeIf { it.isNotBlank() }
                                                ?: member.username?.takeIf { it.isNotBlank() }
                                                ?: "Member"
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val lastAt = inputText.lastIndexOf('@')
                                                        if (lastAt >= 0) {
                                                            inputText = inputText.substring(0, lastAt) + "@$displayName "
                                                            selectedMentions[member.user_id] = displayName
                                                        }
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFEF3C7)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (!member.avatar_url.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = member.avatar_url,
                                                            contentDescription = displayName,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(CircleShape)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = displayName.take(1).uppercase(),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFB45309)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = displayName,
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (!member.username.isNullOrBlank()) {
                                                        Text(
                                                            text = "@${member.username}",
                                                            fontSize = 11.5.sp,
                                                            color = Color(0xFF64748B),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Reply Banner above composer
                        replyingToMessage?.let { replyTarget ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Replying to ${replyTarget.senderName}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFB45309)
                                        )
                                        Text(
                                            text = replyTarget.text,
                                            fontSize = 12.sp,
                                            color = Color(0xFF475569),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .clickable { replyingToMessage = null },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GroupCloseIcon(size = 13.dp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Photo Picker Button
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                                    .clickable(enabled = !isUploadingMedia) {
                                        mediaPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingMedia) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFD97706),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    GroupPlusIcon(size = 20.dp, color = Color(0xFF475569))
                                }
                            }

                            // Voice Note Button
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                                    .clickable(enabled = !isUploadingMedia) {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            startVoiceRecording()
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                GroupMicIcon(size = 19.dp, color = Color(0xFF475569))
                            }

                            // Message Input Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp, max = 110.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = "Message (type @ to mention)...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    textStyle = TextStyle(
                                        color = Color(0xFF0F172A),
                                        fontSize = 14.5.sp
                                    ),
                                    visualTransformation = mentionVisualTransformation,
                                    cursorBrush = SolidColor(Color(0xFF0F172A)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Yellow Send Circle
                            val canSend = inputText.trim().isNotEmpty() && !isSending
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (canSend) Color(0xFFFFC529) else Color(0xFFFDE68A))
                                    .clickable(enabled = canSend) {
                                        val textToSend = inputText.trim()
                                        val replyTarget = replyingToMessage
                                        val resolvedMentions = buildSet {
                                            selectedMentions.forEach { (uid, name) ->
                                                if (textToSend.contains("@$name", ignoreCase = true)) {
                                                    add(uid)
                                                }
                                            }
                                            groupMembers.forEach { m ->
                                                val pName = m.profile_name?.trim().orEmpty()
                                                val uName = m.username?.trim().orEmpty()
                                                if (pName.isNotEmpty() && textToSend.contains("@$pName", ignoreCase = true)) {
                                                    add(m.user_id)
                                                }
                                                if (uName.isNotEmpty() && textToSend.contains("@$uName", ignoreCase = true)) {
                                                    add(m.user_id)
                                                }
                                            }
                                            if (replyTarget != null && replyTarget.senderId.isNotBlank() && replyTarget.senderId != myUserId) {
                                                add(replyTarget.senderId)
                                            }
                                        }.toList()

                                        inputText = ""
                                        selectedMentions.clear()
                                        replyingToMessage = null
                                        isSending = true
                                        scope.launch {
                                            val res = GroupChatRepository.sendGroupMessage(
                                                context = context,
                                                groupId = group.id,
                                                text = textToSend,
                                                messageType = "text",
                                                storagePath = null,
                                                mentionedUserIds = resolvedMentions,
                                                replyToMessage = replyTarget
                                            )
                                            isSending = false
                                            res.onSuccess { outcome ->
                                                if (outcome.skippedCount > 0) {
                                                    Toast.makeText(
                                                        context,
                                                        "Sent (${outcome.skippedCount} member(s) skipped: no E2EE key)",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }.onFailure { err ->
                                                Toast.makeText(
                                                    context,
                                                    err.message ?: "Failed to send message",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    GroupSendPaperPlaneIcon(size = 20.dp, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (visibleMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GroupCuteEmptyChatIllustration(modifier = Modifier.size(135.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No messages yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Messages in ${group.name} are end-to-end encrypted for all joined members.",
                        fontSize = 13.5.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleMessages, key = { it.id }) { msg ->
                    if (msg.messageType == "system" || msg.senderId == "system") {
                        GroupSystemEventRow(text = msg.text)
                    } else {
                        val isMe = msg.isOutgoing || msg.senderId == myUserId
                        val mentionIds = remember(msg.mentionsJson) {
                            GroupChatRepository.parseMentionsJson(msg.mentionsJson)
                        }
                        val replyToId = msg.replyToMessageId.orEmpty()
                        val isReplyToMe = remember(replyToId, myUserId, messages) {
                            replyToId.isNotBlank() && messages.any { it.id == replyToId && (it.senderId == myUserId || it.isOutgoing) }
                        }
                        val isMentionedMe = (myUserId.isNotBlank() && mentionIds.contains(myUserId)) || isReplyToMe

                        GroupMessageItem(
                            message = msg,
                            isMe = isMe,
                            isMentioned = isMentionedMe,
                            onReplyClick = { replyingToMessage = msg },
                            onImageClick = { url -> previewImageUrl = url },
                            onRequestDownloadMedia = { targetMsg ->
                                scope.launch {
                                    val dl = GroupChatRepository.downloadGroupMessageMediaOnDemand(context, targetMsg)
                                    dl.onFailure { err ->
                                        Toast.makeText(context, err.message ?: "Failed to decrypt media", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showInviteSheet) {
        GroupInviteBottomSheet(
            group = group,
            onDismiss = { showInviteSheet = false }
        )
    }

    if (showSearchSheet) {
        LocalGroupSearchBottomSheet(
            groupId = group.id,
            onDismiss = { showSearchSheet = false }
        )
    }

    if (showMediaLinksSheet) {
        LocalGroupMediaBottomSheet(
            groupId = group.id,
            onDismiss = { showMediaLinksSheet = false }
        )
    }

    if (showDisappearingDialog && isAdmin) {
        val options = listOf(
            0 to "Off",
            86400 to "24 hours",
            604800 to "7 days",
            7776000 to "90 days"
        )
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text("Disappearing Messages", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEach { (seconds, label) ->
                        val selected = group.disappearingSeconds == seconds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color(0xFFFEF3C7) else Color.Transparent)
                                .clickable {
                                    showDisappearingDialog = false
                                    if (seconds != group.disappearingSeconds) {
                                        scope.launch {
                                            val res = GroupChatRepository.updateGroupSettings(
                                                context,
                                                group.id,
                                                mapOf("disappearing_seconds" to seconds)
                                            )
                                            res.onFailure { err ->
                                                Toast.makeText(context, err.message ?: "Failed to update setting", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = Color(0xFF0F172A))
                            RadioButton(selected = selected, onClick = null)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisappearingDialog = false }) {
                    Text("Close", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Clear Chat Locally?", fontWeight = FontWeight.Bold) },
            text = { Text("This will delete all decrypted messages for \"${group.name}\" stored on this phone. Other group members are not affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearChatDialog = false
                        scope.launch {
                            GroupChatRepository.clearGroupChatLocally(group.id)
                            Toast.makeText(context, "Chat cleared on this device", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Clear", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showReportDialog) {
        val categories = listOf("spam", "harassment", "inappropriate", "scam", "other")
        var selectedCategory by remember { mutableStateOf("spam") }
        var reportReason by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showReportDialog = false },
            title = { Text("Report Group", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        val selected = selectedCategory == cat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Color(0xFFFEF3C7) else Color(0xFFF8FAFC))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            RadioButton(selected = selected, onClick = null)
                        }
                    }
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text("Details (optional)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSubmitting,
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            val res = GroupChatRepository.reportGroup(
                                context = context,
                                groupId = group.id,
                                category = selectedCategory,
                                reason = reportReason.ifBlank { "Reported for $selectedCategory" }
                            )
                            isSubmitting = false
                            res.onSuccess {
                                showReportDialog = false
                                Toast.makeText(context, "Report submitted", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to report group", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Report", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    previewImageUrl?.let { url ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { previewImageUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .clickable { previewImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = "Preview image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 520.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { previewImageUrl = null },
                    contentAlignment = Alignment.Center
                ) {
                    GroupCloseIcon(size = 16.dp, color = Color.White)
                }
            }
        }
    }

    if (showLeaveDialog) {
        var isLeaving by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isLeaving) showLeaveDialog = false },
            title = { Text("Leave Group?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to leave \"${group.name}\"? You will no longer receive encrypted messages from this group.") },
            confirmButton = {
                TextButton(
                    enabled = !isLeaving,
                    onClick = {
                        scope.launch {
                            isLeaving = true
                            val res = GroupChatRepository.leaveGroup(context, group.id)
                            isLeaving = false
                            res.onSuccess {
                                showLeaveDialog = false
                                Toast.makeText(context, "Left group", Toast.LENGTH_SHORT).show()
                                onBack()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to leave group", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Leave", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun GroupSystemEventRow(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GroupMessageItem(
    message: LocalGroupMessage,
    isMe: Boolean,
    isMentioned: Boolean = false,
    onReplyClick: () -> Unit = {},
    onImageClick: (String) -> Unit,
    onRequestDownloadMedia: (LocalGroupMessage) -> Unit
) {
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }
    val isVoice = message.messageType == "voice"
    val isImage = message.messageType == "image" || (!isVoice && !message.localMediaPath.isNullOrBlank())
    val localFile = remember(message.localMediaPath) {
        message.localMediaPath?.let { File(it) }?.takeIf { it.exists() }
    }

    val styledMessageText = remember(message.text) {
        buildAnnotatedString {
            val raw = message.text
            append(raw)
            val mentionRegex = Regex("@[\\w.\\- ]+")
            mentionRegex.findAll(raw).forEach { match ->
                addStyle(
                    SpanStyle(
                        color = Color(0xFFB45309),
                        fontWeight = FontWeight.ExtraBold
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }
    }

    var isPlayingVoice by remember { mutableStateOf(false) }
    var voicePlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(message.id) {
        onDispose {
            try {
                voicePlayer?.stop()
                voicePlayer?.release()
            } catch (_: Exception) {}
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                if (!message.senderAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = message.senderAvatar,
                        contentDescription = message.senderName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = message.senderName.take(1).uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isMe) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = message.senderName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    if (isMentioned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Text(
                                text = "@ Mentioned you",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.clickable { onReplyClick() },
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMe) 18.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 18.dp
                ),
                color = when {
                    isMe -> Color(0xFFFDE68A)
                    isMentioned -> Color(0xFFFFFBEB)
                    else -> Color.White
                },
                border = BorderStroke(
                    width = if (isMentioned && !isMe) 1.5.dp else 1.dp,
                    color = when {
                        isMentioned && !isMe -> Color(0xFFF59E0B)
                        isMe -> Color(0xFFFCD34D)
                        else -> Color(0xFFF1F5F9)
                    }
                ),
                shadowElevation = if (isMentioned && !isMe) 2.dp else 0.5.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    val hasReply = !message.replyToPreview.isNullOrBlank() || !message.replyToSenderName.isNullOrBlank()
                    if (hasReply) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isMe) Color(0xFFFEF3C7) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                if (!message.replyToSenderName.isNullOrBlank()) {
                                    Text(
                                        text = message.replyToSenderName,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                if (!message.replyToPreview.isNullOrBlank()) {
                                    Text(
                                        text = message.replyToPreview,
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    when {
                        isImage -> {
                            if (localFile != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onImageClick(localFile.absolutePath) }
                                ) {
                                    AsyncImage(
                                        model = localFile,
                                        contentDescription = "Encrypted image attachment",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onRequestDownloadMedia(message) },
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        GroupImagePlaceholderIcon(size = 20.dp, color = Color(0xFFB45309))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Tap to download encrypted photo",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        isVoice -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (localFile == null) {
                                            onRequestDownloadMedia(message)
                                        } else if (isPlayingVoice) {
                                            try {
                                                voicePlayer?.stop()
                                                voicePlayer?.release()
                                            } catch (_: Exception) {}
                                            voicePlayer = null
                                            isPlayingVoice = false
                                        } else {
                                            try {
                                                val mp = MediaPlayer().apply {
                                                    setDataSource(localFile.absolutePath)
                                                    prepare()
                                                    setOnCompletionListener {
                                                        isPlayingVoice = false
                                                        release()
                                                        voicePlayer = null
                                                    }
                                                    start()
                                                }
                                                voicePlayer = mp
                                                isPlayingVoice = true
                                            } catch (_: Exception) {
                                                isPlayingVoice = false
                                            }
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFC529)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    GroupMicIcon(size = 16.dp, color = Color.Black)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = when {
                                        localFile == null -> "Tap to download voice note"
                                        isPlayingVoice -> "Playing voice note..."
                                        else -> "Play encrypted voice note"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        else -> {
                            Text(
                                text = styledMessageText,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (message.expiresAt != null && message.expiresAt > 0L) {
                            GroupClockIcon(
                                size = 10.dp,
                                color = if (isMe) Color(0xFF78350F) else Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Text(
                            text = timeStr,
                            fontSize = 10.sp,
                            color = if (isMe) Color(0xFF78350F) else Color(0xFF94A3B8)
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            GroupCheckIcon(size = 11.dp, color = Color(0xFF78350F))
                        }
                    }
                }
            }
        }
    }
}
