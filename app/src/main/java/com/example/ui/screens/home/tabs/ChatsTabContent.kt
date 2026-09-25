package com.example.ui.screens.home.tabs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import com.example.SupportChatManager
import com.example.crypto.VaultSecurityManager
import com.example.crypto.VaultStatus
import com.example.repository.EncryptedChatRepository
import com.example.ui.components.CircleFlag
import com.example.ui.components.FunkyHeartIcon
import com.example.ui.components.FunkyMoreIcon
import com.example.ui.components.FunkySearchIcon
import com.example.ui.screens.chat.VaultManagementDialogs
import com.example.ui.screens.home.models.DirectChat
import com.example.ui.theme.GoldAccent

@Composable
fun ChatsTabContent(
    conversationsList: List<DirectChat>,
    onSelectChat: (DirectChat) -> Unit,
    onOpenSupportChat: (reportId: String?) -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Unread", "Language", "Groups"
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showVaultDialog by remember { mutableStateOf(false) }

    val supportPreview by SupportChatManager.latestMessagePreview.collectAsState()
    val supportTime by SupportChatManager.latestMessageTime.collectAsState()
    val supportUnreadCount by SupportChatManager.unreadCount.collectAsState()

    val vaultStatus by VaultSecurityManager.vaultStatus.collectAsState()
    val dbConversations by EncryptedChatRepository.conversations.collectAsState()

    LaunchedEffect(Unit) {
        SupportChatManager.init(context)
        EncryptedChatRepository.init(context)
    }

    // Merge real encrypted conversations with fallback contacts
    val mergedConversations = remember(conversationsList, dbConversations) {
        val dbMapped = dbConversations.map { conv ->
            DirectChat(
                id = conv.otherUserId,
                name = conv.otherUserName,
                avatar = conv.otherUserAvatar.orEmpty().ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200" },
                lastMsg = conv.lastMessageText.ifBlank { "🔒 Encrypted conversation" },
                time = formatTimeAgo(conv.lastMessageTime),
                unreadCount = conv.unreadCount,
                onlineStatus = "online",
                isNativeLanguage = conv.otherUserFlag ?: "English"
            )
        }
        val existingIds = dbMapped.map { it.id }.toSet()
        val rest = conversationsList.filterNot { it.id in existingIds }
        dbMapped + rest
    }

    val filteredList = remember(mergedConversations, searchQuery, selectedFilter) {
        mergedConversations.filter { chat ->
            val matchesSearch = searchQuery.isBlank() ||
                    chat.name.contains(searchQuery, ignoreCase = true) ||
                    chat.lastMsg.contains(searchQuery, ignoreCase = true) ||
                    chat.isNativeLanguage.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Unread" -> chat.unreadCount > 0
                "Language" -> true
                "Groups" -> false
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val showSupportInSearch = remember(searchQuery, selectedFilter, supportUnreadCount) {
        val matchesSearch = searchQuery.isBlank() ||
                "FunkyTalk".contains(searchQuery, ignoreCase = true) ||
                "Support".contains(searchQuery, ignoreCase = true) ||
                (supportPreview ?: "").contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "Unread" -> supportUnreadCount > 0
            "Groups" -> false
            else -> true
        }
        matchesSearch && matchesFilter
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFBF7)),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // 1. Top App Header with Brand Name and 3-dots Menu Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FunkyTalk. Brand Title with signature yellow accent
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 25.sp
                            )
                        ) {
                            append("Funky")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFFFFC529),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 25.sp
                            )
                        ) {
                            append("Talk.")
                        }
                    }
                )

                // Top Right: Functional 3-Dots Action Button with Dropdown Menu
                Box {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                            .clickable { showOptionsMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        FunkyMoreIcon(color = Color.Black, modifier = Modifier.size(18.dp))
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark all as read", color = Color.Black, fontSize = 14.sp) },
                            onClick = {
                                showOptionsMenu = false
                                Toast.makeText(context, "All conversations marked as read", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Contact Support", color = Color.Black, fontSize = 14.sp) },
                            onClick = {
                                showOptionsMenu = false
                                onOpenSupportChat(null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Chat settings", color = Color.Black, fontSize = 14.sp) },
                            onClick = {
                                showOptionsMenu = false
                                Toast.makeText(context, "Opening chat settings...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // 2. Headline: Conversations & Subtitle with Gold Heart
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Conversations",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Keep the conversations going ",
                        fontSize = 15.sp,
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Normal
                    )
                    FunkyHeartIcon(
                        color = Color(0xFFFFC529),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 3. Search Bar
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(50.dp))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FunkySearchIcon(
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search conversations...",
                                color = Color(0xFF9CA3AF),
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 4. Filter Chips Row ("All", "Unread", "Language", "Groups")
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("All", "Unread", "Language", "Groups").forEach { chipText ->
                    val isSelected = selectedFilter == chipText
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                if (isSelected) Color(0xFFFFC529) else Color(0xFFF3F4F6)
                            )
                            .clickable { selectedFilter = chipText }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else Color(0xFF1F2937)
                        )
                    }
                }
            }
        }

        // 4.5. ENCRYPTED CHAT VAULT STATUS BANNER
        item {
            Spacer(modifier = Modifier.height(14.dp))
            when (vaultStatus) {
                VaultStatus.NOT_CONFIGURED -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showVaultDialog = true },
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFEF3C7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Setup E2EE Chat Vault",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Protect 1:1 messages with Tink HPKE & local SQLCipher",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                            Button(
                                onClick = { showVaultDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Setup", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
                VaultStatus.LOCKED -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showVaultDialog = true },
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFDBEAFE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Chat Vault Locked",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E40AF)
                                )
                                Text(
                                    text = "Unlock with password or biometric to read messages",
                                    fontSize = 11.sp,
                                    color = Color(0xFF3B82F6)
                                )
                            }
                            Button(
                                onClick = { showVaultDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Unlock", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
                VaultStatus.UNLOCKED -> {
                    // Chat vault is active and unlocked; keep conversation UI clean and spacious
                }
            }
        }

        // 5. PINNED OFFICIAL FUNKYTALK SUPPORT ENTRY (ALWAYS AT TOP)
        if (showSupportInSearch) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onOpenSupportChat(null) }
                        .testTag("pinned_support_chat_item"),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFFFDE68A)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Avatar: FunkyTalk Official Avatar with Verified Badge
                        Box(
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(GoldAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "FT",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color.Black
                                )
                            }

                            // Circular Verified check badge
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6))
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Official",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Center Details
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Top line: Name & Timestamp & Official badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "FunkyTalk",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFFFBEB))
                                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "SUPPORT",
                                            color = Color(0xFFB45309),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Text(
                                    text = supportTime ?: "",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9CA3AF),
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Middle line: Subtitle
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Official Support Channel",
                                    color = Color(0xFF047857),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Bottom line: Last Message & Unread Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = supportPreview ?: "How can we help you today? Tap to chat.",
                                    color = if (supportUnreadCount > 0) Color(0xFF1F2937) else Color(0xFF6B7280),
                                    fontWeight = if (supportUnreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (supportUnreadCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(GoldAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$supportUnreadCount",
                                            color = Color.Black,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Regular Chat Cards List
        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(filteredList, key = { it.id }) { chat ->
            val flagCode = when (chat.isNativeLanguage.lowercase()) {
                "english" -> "us"
                "korean" -> "kr"
                "japanese" -> "jp"
                "chinese" -> "cn"
                "spanish" -> "es"
                "german" -> "de"
                "french" -> "fr"
                "hindi" -> "in"
                "russian" -> "ru"
                "arabic" -> "ae"
                else -> "us"
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable {
                        if (vaultStatus != VaultStatus.UNLOCKED) {
                            showVaultDialog = true
                        } else {
                            onSelectChat(chat)
                        }
                    },
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF0F0EE)),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Avatar with Status Dot
                    Box(
                        modifier = Modifier.size(56.dp)
                    ) {
                        AsyncImage(
                            model = chat.avatar,
                            contentDescription = chat.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )

                        // Circular Country Flag icon on bottom right replacing the green dot
                        CircleFlag(
                            countryCode = flagCode,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Center Details
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Top line: Name & Timestamp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = chat.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFDCFCE7))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "E2EE",
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(9.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "E2EE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = chat.time,
                                fontSize = 12.sp,
                                color = Color(0xFF9CA3AF),
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Middle line: Flag + Language Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircleFlag(
                                countryCode = flagCode,
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = chat.isNativeLanguage,
                                color = Color(0xFF6B7280),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Bottom line: Last Message & Unread Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chat.lastMsg,
                                color = if (chat.unreadCount > 0) Color(0xFF1F2937) else Color(0xFF6B7280),
                                fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (chat.unreadCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFC529)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${chat.unreadCount}",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVaultDialog) {
        VaultManagementDialogs(onDismissRequest = { showVaultDialog = false })
    }
}

private fun formatTimeAgo(epochMillis: Long): String {
    if (epochMillis <= 0L) return "Just now"
    val diff = System.currentTimeMillis() - epochMillis
    return when {
        diff < 60_000L -> "Just now"
        diff < 3600_000L -> "${diff / 60_000L}m ago"
        diff < 86400_000L -> "${diff / 3600_000L}h ago"
        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(epochMillis))
    }
}

