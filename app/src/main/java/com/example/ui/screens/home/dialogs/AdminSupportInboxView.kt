package com.example.ui.screens.home.dialogs

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.AdminSupportManager
import com.example.SupabaseSupportConversation
import com.example.SupabaseSupportMessage
import com.example.UserProfile
import com.example.realtime.VoiceRoomRealtimeManager
import com.example.ui.components.CircleFlag
import com.example.ui.theme.GoldPrimary

@Composable
fun AdminSupportInboxView(
    supabaseProfiles: List<UserProfile> = emptyList(),
    onOpenReport: (reportId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val conversations by AdminSupportManager.conversations.collectAsState()
    val messages by AdminSupportManager.messages.collectAsState()
    val lastMessagesMap by AdminSupportManager.lastMessagesMap.collectAsState()
    val unreadConversationsSet by AdminSupportManager.unreadConversationsSet.collectAsState()
    val appealUserIdsSet by AdminSupportManager.appealUserIdsSet.collectAsState()
    val isLoading by AdminSupportManager.isLoading.collectAsState()
    val isSending by AdminSupportManager.isSending.collectAsState()

    var activeConversation by remember { mutableStateOf<SupabaseSupportConversation?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf("") }
    var selectedOpenHours by remember { mutableIntStateOf(2) } // default 2 hours
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        AdminSupportManager.init(context)
        AdminSupportManager.fetchConversations(context)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (activeConversation != null) {
        // FULL CHAT SCREEN (admin view)
        val conv = activeConversation!!
        val targetUid = conv.user_id ?: ""
        val targetProfile = supabaseProfiles.find { it.uid == targetUid }
        val userSnippet = VoiceRoomRealtimeManager.getUserSnippet(targetUid)

        val targetName = targetProfile?.displayName?.ifBlank { null }
            ?: targetProfile?.username?.ifBlank { null }
            ?: userSnippet?.name?.ifBlank { null }
            ?: "Learner (${targetUid.take(8)})"

        val targetAvatar = targetProfile?.avatar?.ifBlank { null }
            ?: userSnippet?.avatar
            ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"

        val targetFlag = targetProfile?.countryCode?.ifBlank { null }
            ?: userSnippet?.countryCode
            ?: "in"

        val statusLine = AdminSupportManager.formatChatOpenStatus(conv.chat_open_until)
        val isOpen = statusLine.startsWith("Chat open until")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
        ) {
            // Header Bar
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { activeConversation = null },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to list",
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Target User Avatar
                    Box(modifier = Modifier.size(42.dp)) {
                        AsyncImage(
                            model = targetAvatar,
                            contentDescription = targetName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        CircleFlag(
                            countryCode = targetFlag,
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = targetName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isOpen) Color(0xFF16A34A) else Color(0xFF94A3B8))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = statusLine,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOpen) Color(0xFF15803D) else Color(0xFF64748B)
                            )
                        }
                    }

                    // Close Chat Button in Top Bar
                    if (isOpen) {
                        OutlinedButton(
                            onClick = {
                                AdminSupportManager.adminCloseSupportChat(
                                    context = context,
                                    userId = targetUid,
                                    onSuccess = {
                                        Toast.makeText(context, "Support chat closed", Toast.LENGTH_SHORT).show()
                                        val updated = conv.copy(chat_open_until = null)
                                        activeConversation = updated
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Close chat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = "Closed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Message List with Date Separators
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty() && isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldPrimary)
                    }
                } else if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No messages in this support conversation yet.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                } else {
                    val distinctMsgs = remember(messages) { messages.distinctBy { it.id } }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (i in distinctMsgs.indices) {
                            val msg = distinctMsgs[i]
                            val prevMsg = if (i > 0) distinctMsgs[i - 1] else null

                            val msgDateHeader = AdminSupportManager.formatDateHeader(msg.created_at)
                            val prevDateHeader = if (prevMsg != null) AdminSupportManager.formatDateHeader(prevMsg.created_at) else null

                            // Date Separator item
                            if (i == 0 || msgDateHeader != prevDateHeader) {
                                item(key = "date_hdr_$i") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            color = Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = msgDateHeader,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF475569),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Message Item
                            item(key = msg.id) {
                                AdminThreadMessageBubble(
                                    message = msg,
                                    targetName = targetName,
                                    targetAvatar = targetAvatar,
                                    supabaseProfiles = supabaseProfiles,
                                    onOpenReport = onOpenReport
                                )
                            }
                        }
                    }
                }
            }

            // Staff Reply Panel
            Surface(
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Segmented Duration Selector above input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Keep chat open:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedOpenHours == 1,
                                onClick = { selectedOpenHours = 1 },
                                label = { Text("1 hour", fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = Color.Black
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(30.dp)
                            )

                            FilterChip(
                                selected = selectedOpenHours == 2,
                                onClick = { selectedOpenHours = 2 },
                                label = { Text("2 hours (Default)", fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = Color.Black
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }

                    // Input Box + Send Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "Reply as FunkyTalk Support...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_support_reply_input"),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputText.isNotBlank() && !isSending) {
                                    val textToSend = inputText
                                    inputText = ""
                                    AdminSupportManager.adminSendMessage(
                                        context = context,
                                        userId = targetUid,
                                        messageText = textToSend,
                                        openHours = selectedOpenHours,
                                        onSuccess = {
                                            AdminSupportManager.fetchConversations(context)
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            })
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !isSending) {
                                    val textToSend = inputText
                                    inputText = ""
                                    AdminSupportManager.adminSendMessage(
                                        context = context,
                                        userId = targetUid,
                                        messageText = textToSend,
                                        openHours = selectedOpenHours,
                                        onSuccess = {
                                            AdminSupportManager.fetchConversations(context)
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            enabled = inputText.isNotBlank() && !isSending,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank() && !isSending) GoldPrimary else Color(0xFFE2E8F0))
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Reply",
                                    tint = if (inputText.isNotBlank()) Color.Black else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // INBOX CONVERSATION LIST VIEW
        val filteredList = remember(conversations, searchQuery, supabaseProfiles) {
            if (searchQuery.isBlank()) conversations
            else {
                val q = searchQuery.trim().lowercase()
                conversations.filter { conv ->
                    val p = supabaseProfiles.find { it.uid == conv.user_id }
                    val snippet = VoiceRoomRealtimeManager.getUserSnippet(conv.user_id ?: "")
                    val name = p?.displayName ?: p?.username ?: snippet?.name ?: ""
                    name.lowercase().contains(q) || (conv.user_id?.lowercase()?.contains(q) == true)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search support threads by user or UID...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }

            if (isLoading && conversations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GoldPrimary)
                    }
                }
            } else if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Support Conversations",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "When users message FunkyTalk Support, their threads will appear here.",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            } else {
                items(filteredList.distinctBy { it.id }, key = { it.id }) { conv ->
                    val uid = conv.user_id ?: ""
                    val p = supabaseProfiles.find { it.uid == uid }
                    val snippet = VoiceRoomRealtimeManager.getUserSnippet(uid)

                    val name = p?.displayName?.ifBlank { null }
                        ?: p?.username?.ifBlank { null }
                        ?: snippet?.name?.ifBlank { null }
                        ?: "Learner (${uid.take(8)})"

                    val avatar = p?.avatar?.ifBlank { null }
                        ?: snippet?.avatar
                        ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"

                    val flagCode = p?.countryCode?.ifBlank { null }
                        ?: snippet?.countryCode
                        ?: "in"

                    val relativeTime = AdminSupportManager.formatRelativeTime(conv.last_message_at)
                    val lastMsg = lastMessagesMap[conv.id]
                    val hasUnread = unreadConversationsSet.contains(conv.id)
                    val isAppealUser = appealUserIdsSet.contains(uid)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                activeConversation = conv
                                AdminSupportManager.selectConversation(context, conv)
                            }
                            .testTag("admin_support_item_${conv.id}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (hasUnread) Color(0xFFFFFBEB) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (hasUnread) Color(0xFFFCD34D) else Color(0xFFE2E8F0)
                        ),
                        shadowElevation = 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar + Unread Dot Indicator
                            Box(modifier = Modifier.size(48.dp)) {
                                AsyncImage(
                                    model = avatar,
                                    contentDescription = name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                                CircleFlag(
                                    countryCode = flagCode,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .border(1.5.dp, Color.White, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = name,
                                            fontWeight = if (hasUnread) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        // Red Unread Dot
                                        if (hasUnread) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEF4444))
                                            )
                                        }

                                        // Appeal Badge
                                        if (isAppealUser) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0xFFFEF2F2),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(0.5.dp, Color(0xFFFCA5A5))
                                            ) {
                                                Text(
                                                    text = "Appeal",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFDC2626),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = relativeTime,
                                        fontSize = 11.sp,
                                        color = if (hasUnread) Color(0xFFD97706) else Color(0xFF94A3B8),
                                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                // Last Message Snippet
                                Text(
                                    text = lastMsg?.body?.ifBlank { null } ?: "Tap to view conversation...",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (hasUnread) Color(0xFF1E293B) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminThreadMessageBubble(
    message: SupabaseSupportMessage,
    targetName: String,
    targetAvatar: String,
    supabaseProfiles: List<UserProfile>,
    onOpenReport: (reportId: String) -> Unit
) {
    val isAdmin = message.sender_type == "admin"
    val formattedTime = AdminSupportManager.formatRelativeTime(message.created_at)

    val staffProfile = remember(message.sender_id, supabaseProfiles) {
        if (isAdmin && !message.sender_id.isNullOrBlank()) {
            supabaseProfiles.find { it.uid == message.sender_id }
        } else null
    }

    val staffName = staffProfile?.displayName?.ifBlank { null }
        ?: staffProfile?.username?.ifBlank { null }
        ?: if (!message.sender_id.isNullOrBlank()) "Staff (${message.sender_id!!.take(6)})" else "Staff"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAdmin) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isAdmin) {
            // User Avatar on left
            AsyncImage(
                model = targetAvatar,
                contentDescription = targetName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isAdmin) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = if (isAdmin) "FunkyTalk Support" else targetName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )

                if (isAdmin) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "· sent by $staffName",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isAdmin) 16.dp else 4.dp,
                    bottomEnd = if (isAdmin) 4.dp else 16.dp
                ),
                color = if (isAdmin) GoldPrimary else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isAdmin) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                ),
                shadowElevation = 0.5.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    if (!message.report_id.isNullOrBlank()) {
                        Surface(
                            onClick = { onOpenReport(message.report_id!!) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAdmin) Color(0x22000000) else Color(0xFFEFF6FF),
                            border = BorderStroke(0.5.dp, if (isAdmin) Color(0x44000000) else Color(0xFFBFDBFE)),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = if (isAdmin) Color.Black else Color(0xFF1D4ED8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "View Report #${message.report_id!!.take(8)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAdmin) Color.Black else Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }

                    Text(
                        text = message.body,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = if (isAdmin) Color(0xFF451A03) else Color(0xFF94A3B8),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        if (isAdmin) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "FT",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = Color.Black
                )
            }
        }
    }
}
