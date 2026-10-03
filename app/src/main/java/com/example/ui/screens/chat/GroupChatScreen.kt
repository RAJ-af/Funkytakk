package com.example.ui.screens.chat

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.SupabaseSessionManager
import com.example.db.LocalGroupConversation
import com.example.db.LocalGroupMessage
import com.example.repository.GroupChatRepository
import com.example.ui.components.*
import com.example.ui.screens.chat.components.ChatImagePreviewDialog
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupChatScreen(
    group: LocalGroupConversation,
    onBack: () -> Unit,
    onNavigateToInfo: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myUserId = remember { SupabaseSessionManager.getUserId(context) ?: "" }
    val isAdmin = remember(group.myRole) { group.myRole.equals("admin", ignoreCase = true) }

    val messages by GroupChatRepository.getGroupMessagesFlow(group.id).collectAsState(initial = emptyList())
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showE2eeNotice by remember { mutableStateOf(true) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var memberCount by remember { mutableStateOf(group.memberCount.coerceAtLeast(2)) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    isSending = true
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val res = GroupChatRepository.sendGroupMediaMessage(
                            context = context,
                            groupId = group.id,
                            rawBytes = bytes,
                            mimeType = mime
                        )
                        if (res.isFailure) {
                            Toast.makeText(context, "Upload failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error selecting photo: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isSending = false
                }
            }
        }
    }

    BackHandler {
        if (previewImageUrl != null) {
            previewImageUrl = null
        } else {
            onBack()
        }
    }

    LaunchedEffect(group.id) {
        GroupChatRepository.markGroupRead(group.id)
        GroupChatRepository.syncUserGroups(context)
        GroupChatRepository.fetchAndProcessPendingGroupMessages(context)
        val members = GroupChatRepository.getGroupMembers(context, group.id)
        if (members.isNotEmpty()) {
            memberCount = members.size
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAF7),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button
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

                // Center Header: Avatar + Title + Status -> Clickable to navigate to Info
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToInfo() }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with online status dot
                    Box(modifier = Modifier.size(42.dp)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (group.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = group.avatarUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                GroupUsersIcon(size = 20.dp, color = Color(0xFFB45309))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = group.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$memberCount members  |",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            GroupTinyLockIcon(size = 10.dp, color = Color(0xFFD97706))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "End-to-end encrypted",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // 3-dots button with popup menu matching user's design image
                Box {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { showOptionsMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        GroupThreeDotsIcon(size = 20.dp, color = Color(0xFF0F172A))
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier
                            .width(230.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Search in conversation", fontSize = 13.5.sp, color = Color(0xFF0F172A)) },
                            leadingIcon = { GroupSearchIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                            onClick = {
                                showOptionsMenu = false
                                Toast.makeText(context, "Search in conversation", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Media, links & files", fontSize = 13.5.sp, color = Color(0xFF0F172A)) },
                            leadingIcon = { GroupMediaIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                            onClick = {
                                showOptionsMenu = false
                                onNavigateToInfo()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mute notifications", fontSize = 13.5.sp, color = Color(0xFF0F172A)) },
                            leadingIcon = { GroupBellIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                            onClick = {
                                showOptionsMenu = false
                                Toast.makeText(context, "Notifications muted", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear chat", fontSize = 13.5.sp, color = Color(0xFF0F172A)) },
                            leadingIcon = { GroupTrashIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                            onClick = {
                                showOptionsMenu = false
                                Toast.makeText(context, "Chat cleared", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        DropdownMenuItem(
                            text = { Text("Report group", fontSize = 13.5.sp, color = Color(0xFF0F172A)) },
                            leadingIcon = { GroupShieldIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                            onClick = {
                                showOptionsMenu = false
                                Toast.makeText(context, "Group reported", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Leave group", fontSize = 13.5.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { GroupLeaveDoorIcon(size = 18.dp, color = Color(0xFFEF4444)) },
                            onClick = {
                                showOptionsMenu = false
                                showLeaveDialog = true
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // E2EE Notice banner: exactly as in user's image (rounded card with yellow lock badge)
            if (showE2eeNotice) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFDE047)),
                            contentAlignment = Alignment.Center
                        ) {
                            GroupTinyLockIcon(size = 16.dp, color = Color(0xFF92400E))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Group messages are end-to-end encrypted for joined members only.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { showE2eeNotice = false },
                            contentAlignment = Alignment.Center
                        ) {
                            GroupCloseIcon(size = 13.dp, color = Color(0xFF92400E))
                        }
                    }
                }
            }

            // Message List or Empty State Illustration
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    GroupCuteEmptyChatIllustration(modifier = Modifier.size(140.dp))
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "No messages yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Be the first to start the conversation in this group.",
                        fontSize = 13.5.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages.distinctBy { it.id }, key = { it.id }) { msg ->
                        GroupMessageItem(
                            message = msg,
                            isMe = msg.isOutgoing || msg.senderId == myUserId,
                            onImageClick = { previewImageUrl = it }
                        )
                    }
                }
            }

            // Bottom Input Bar: floating rounded pill card matching user's image
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo Attachment Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFDF7D))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        GroupMediaIcon(size = 20.dp, color = Color.Black)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Center Input Field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Message group...",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.5.sp
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 13.5.sp),
                            cursorBrush = SolidColor(Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    val canSend = inputText.isNotBlank() && !isSending
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFDF7D))
                            .clickable(enabled = canSend) {
                                val text = inputText.trim()
                                inputText = ""
                                isSending = true
                                scope.launch {
                                    val res = GroupChatRepository.sendGroupMessage(
                                        context = context,
                                        groupId = group.id,
                                        text = text,
                                        messageType = "text"
                                    )
                                    isSending = false
                                    if (res.isFailure) {
                                        Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            GroupSendPlaneIcon(size = 18.dp, color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Image Preview Dialog
    previewImageUrl?.let { url ->
        ChatImagePreviewDialog(
            imageUrl = url,
            onDismiss = { previewImageUrl = null }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Group?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to leave ${group.name}? You will no longer receive encrypted messages from this group.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        scope.launch {
                            GroupChatRepository.removeGroupMember(context, group.id, myUserId)
                            Toast.makeText(context, "Left group", Toast.LENGTH_SHORT).show()
                            onBack()
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
private fun GroupMessageItem(
    message: LocalGroupMessage,
    isMe: Boolean,
    onImageClick: (String) -> Unit
) {
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }
    val isImage = message.messageType == "image" || !message.localMediaPath.isNullOrBlank()
    val mediaModel = message.localMediaPath ?: message.storagePath

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            AsyncImage(
                model = message.senderAvatar.orEmpty().ifBlank {
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200"
                },
                contentDescription = "Sender Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isMe) {
                Text(
                    text = message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMe) 18.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 18.dp
                ),
                color = if (isMe) Color(0xFFFDE68A) else Color.White,
                border = BorderStroke(1.dp, if (isMe) Color(0xFFFCD34D) else Color(0xFFF1F5F9)),
                shadowElevation = 0.5.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (isImage && !mediaModel.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClick(mediaModel) }
                        ) {
                            AsyncImage(
                                model = mediaModel,
                                contentDescription = "Encrypted image attachment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    } else {
                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.align(Alignment.End)
                    ) {
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
