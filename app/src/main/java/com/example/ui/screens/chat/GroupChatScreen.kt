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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.SupabaseClient
import com.example.SupabaseGroupMember
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    group: LocalGroupConversation,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myUserId = remember { SupabaseSessionManager.getUserId(context) ?: "" }
    val isAdmin = remember(group.myRole) { group.myRole == "admin" }

    val messages by GroupChatRepository.getGroupMessagesFlow(group.id).collectAsState(initial = emptyList())
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    var showRosterSheet by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

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
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showRosterSheet = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.dp, GoldAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (group.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = group.avatarUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                SoftGroupIcon(size = 20.dp, color = Color(0xFFB45309))
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = group.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isAdmin) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = "ADMIN",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "🔒 End-to-end encrypted · Tap for info",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { showRosterSheet = true }) {
                        FunkyMoreIcon(color = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // E2EE Notice banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF3C7))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🔒 Group messages are end-to-end encrypted for joined members only.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF92400E),
                    fontWeight = FontWeight.Medium
                )
            }

            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    GroupMessageItem(
                        message = msg,
                        isMe = msg.isOutgoing || msg.senderId == myUserId,
                        onImageClick = { previewImageUrl = it }
                    )
                }
            }

            // Bottom Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo Attachment Button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7))
                    ) {
                        SoftImageIcon(size = 20.dp, color = Color(0xFFB45309))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp, max = 120.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Message group...",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                            cursorBrush = SolidColor(Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    val canSend = inputText.isNotBlank() && !isSending
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (canSend) GoldAccent else Color(0xFFE2E8F0))
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
                            androidx.compose.material3.Icon(Icons.Default.Send, contentDescription = "Send", tint = if (canSend) Color.Black else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
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

    if (showRosterSheet) {
        GroupRosterSheet(
            group = group,
            isAdmin = isAdmin,
            myUserId = myUserId,
            onDismiss = { showRosterSheet = false },
            onAddMemberClick = {
                showRosterSheet = false
                showAddMemberDialog = true
            },
            onLeaveGroup = {
                scope.launch {
                    GroupChatRepository.removeGroupMember(context, group.id, myUserId)
                    Toast.makeText(context, "Left group", Toast.LENGTH_SHORT).show()
                    showRosterSheet = false
                    onBack()
                }
            }
        )
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            groupId = group.id,
            onDismiss = { showAddMemberDialog = false },
            onMemberAdded = {
                Toast.makeText(context, "Invitation sent to partner!", Toast.LENGTH_SHORT).show()
                showAddMemberDialog = false
            }
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
                    .size(28.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
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
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp
                ),
                color = if (isMe) Color(0xFFFCD34D) else Color.White,
                border = BorderStroke(1.dp, if (isMe) Color(0xFFFBBF24) else Color(0xFFE2E8F0)),
                shadowElevation = 0.5.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
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

                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupRosterSheet(
    group: LocalGroupConversation,
    isAdmin: Boolean,
    myUserId: String,
    onDismiss: () -> Unit,
    onAddMemberClick: () -> Unit,
    onLeaveGroup: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var membersList by remember { mutableStateOf<List<SupabaseGroupMember>>(emptyList()) }
    var isLoadingMembers by remember { mutableStateOf(true) }

    LaunchedEffect(group.id) {
        membersList = GroupChatRepository.getGroupMembers(context, group.id)
        isLoadingMembers = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Group Details & Members",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                IconButton(onClick = onDismiss) {
                    SoftCloseIcon(size = 14.dp, color = Color(0xFF475569))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Add Member (Admin only)
            if (isAdmin) {
                Button(
                    onClick = onAddMemberClick,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FunkyPlusIcon(color = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Member (Admin Only)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text(
                text = "Members (${membersList.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingMembers) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoldAccent)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(membersList) { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SoftGroupIcon(size = 16.dp, color = Color(0xFFB45309))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (member.user_id == myUserId) "You" else "Member (${member.user_id.take(6)}...)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${member.role.uppercase()} · ${member.status}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // Admin can remove other members
                            if (isAdmin && member.user_id != myUserId) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            GroupChatRepository.removeGroupMember(context, group.id, member.user_id)
                                            membersList = membersList.filterNot { it.user_id == member.user_id }
                                            Toast.makeText(context, "Removed member", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text("Remove", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Leave Group Button
            OutlinedButton(
                onClick = onLeaveGroup,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Leave Group", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AddMemberDialog(
    groupId: String,
    onDismiss: () -> Unit,
    onMemberAdded: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var candidates by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            if (!token.isNullOrBlank()) {
                val profilesRes = SupabaseClient.api.getAllProfiles(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                    auth = token
                )
                if (profilesRes.isSuccessful && profilesRes.body() != null) {
                    val myId = SupabaseSessionManager.getUserId(context)
                    candidates = profilesRes.body()!!
                        .filterNot { it.auth_user_id == myId }
                        .map { (it.auth_user_id ?: it.id.toString()) to (it.profile_name ?: it.username ?: "Partner") }
                }
            }
        } catch (_: Exception) {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Group Member", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select a partner to invite (they must accept the invitation before joining):", fontSize = 13.sp, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(candidates) { (uid, name) ->
                        val isSelected = selectedUserId == uid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFEF3C7) else Color.Transparent)
                                .clickable { selectedUserId = uid }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val uid = selectedUserId ?: return@Button
                    isSubmitting = true
                    scope.launch {
                        val res = GroupChatRepository.addGroupMember(context, groupId, uid)
                        isSubmitting = false
                        if (res.isSuccess) {
                            onMemberAdded()
                        } else {
                            Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = selectedUserId != null && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
            ) {
                Text("Invite", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Black)
            }
        }
    )
}
