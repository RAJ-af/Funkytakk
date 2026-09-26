package com.example.ui.screens.home.tabs

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.ui.components.UserAvatarImage
import coil.compose.AsyncImage
import com.example.SupportChatManager
import com.example.crypto.VaultSecurityManager
import com.example.crypto.VaultStatus
import com.example.repository.EncryptedChatRepository
import com.example.ui.components.*
import com.example.ui.screens.chat.VaultManagementDialogs
import com.example.ui.screens.home.models.DirectChat
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.launch

@Composable
fun ChatsTabContent(
    conversationsList: List<DirectChat> = emptyList(),
    onSelectChat: (DirectChat) -> Unit,
    onOpenSupportChat: (reportId: String?) -> Unit = {},
    onNavigateToDiscover: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Unread", "Language", "Groups"
    var showOptionsMenu by remember { mutableStateOf(false) }

    val supportPreview by SupportChatManager.latestMessagePreview.collectAsState()
    val supportTime by SupportChatManager.latestMessageTime.collectAsState()
    val supportUnreadCount by SupportChatManager.unreadCount.collectAsState()

    val vaultStatus by VaultSecurityManager.vaultStatus.collectAsState()
    val dbConversations by EncryptedChatRepository.conversations.collectAsState()

    LaunchedEffect(Unit) {
        SupportChatManager.init(context)
        EncryptedChatRepository.init(context)
    }

    // Merge real encrypted conversations from Room DB with any user-initiated active chats
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

    val isVaultUnlocked = vaultStatus == VaultStatus.UNLOCKED

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFBF7))
    ) {
        // --- 1. Main Chat Content (BLURRED when password has not been entered) ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (isVaultUnlocked) 0.dp else 22.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // Top App Header with Brand Name and 3-dots Menu Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            if (isVaultUnlocked) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SoftLockIcon(size = 16.dp, isUnlocked = false)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Lock chats now", color = Color(0xFFDC2626), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        VaultSecurityManager.lock()
                                        Toast.makeText(context, "Chats locked.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
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
                        }
                    }
                }
            }

            // Headline: Conversations & Subtitle with Gold Heart
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

            // Search Bar
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

            // Filter Chips Row ("All", "Unread", "Language", "Groups")
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

            // PINNED OFFICIAL FUNKYTALK SUPPORT ENTRY (ALWAYS AT TOP)
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
                            // Left Avatar: FunkyTalk Official Avatar with Custom Soft Verified Badge
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

                                // Soft Verified check badge
                                SoftCheckBadge(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .border(1.5.dp, Color.White, CircleShape),
                                    badgeColor = Color(0xFF3B82F6),
                                    checkColor = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Center Details
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
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

                                // Middle line: Soft Support Headset icon
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SoftHeadsetIcon(
                                        size = 14.dp,
                                        color = Color(0xFF10B981)
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

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Real Conversations List or Beautiful Clean Empty State (NO MOCK CHATS)
            if (filteredList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SoftEmptyChatIllustration()

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "No conversations found" else "No conversations yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "No conversation matches '$searchQuery'"
                            } else {
                                "All 1:1 messages are end-to-end encrypted.\nFind partners in Discover or join a voice room to start chatting."
                            },
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onNavigateToDiscover,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC529)),
                                shape = RoundedCornerShape(50.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Discover People",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
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
                                if (isVaultUnlocked) {
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
                            Box(modifier = Modifier.size(56.dp)) {
                                UserAvatarImage(
                                    avatarUrl = chat.avatar,
                                    contentDescription = chat.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )

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

                            Column(modifier = Modifier.weight(1f)) {
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
                                                SoftTinyLockIcon(
                                                    size = 10.dp,
                                                    tint = Color(0xFF15803D)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
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

                                Row(verticalAlignment = Alignment.CenterVertically) {
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
        }

        // --- 2. BLUR GLASS OVERLAY & MODERN PASSWORD POPUP (When Vault is Locked / Not Configured) ---
        AnimatedVisibility(
            visible = !isVaultUnlocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable(enabled = true, onClick = {}), // intercepts touch
                contentAlignment = Alignment.Center
            ) {
                when (vaultStatus) {
                    VaultStatus.NOT_CONFIGURED -> {
                        ModernSetupPasswordCard()
                    }
                    VaultStatus.LOCKED -> {
                        ModernUnlockPasswordCard()
                    }
                    VaultStatus.UNLOCKED -> {}
                }
            }
        }
    }
}

/**
 * Modern good-looking Enter Password popup with custom soft icons.
 */
@Composable
private fun ModernUnlockPasswordCard() {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()

    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val hasBiometric = remember { VaultSecurityManager.isBiometricEnabled(context) }

    LaunchedEffect(hasBiometric) {
        if (hasBiometric && activity != null) {
            val cryptoObjResult = VaultSecurityManager.createBiometricDecryptCryptoObject(context)
            cryptoObjResult.onSuccess { cryptoObj ->
                promptBiometricUnlock(activity, cryptoObj) { /* Handled in callback */ }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 24.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Soft Badge with Custom Soft Lock Icon
            SoftIconBadge(
                badgeSize = 64.dp,
                backgroundColor = Color(0xFFEEF2FF),
                borderColor = Color(0xFFC7D2FE)
            ) {
                SoftLockIcon(
                    size = 32.dp,
                    isUnlocked = false,
                    gradientColors = listOf(Color(0xFF6366F1), Color(0xFF4338CA)),
                    shackleColor = Color(0xFFCBD5E1)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Enter Password",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Chats are locked for your privacy. Enter your password to view conversations.",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Chat Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { passwordVisible = !passwordVisible },
                        contentAlignment = Alignment.Center
                    ) {
                        SoftEyeIcon(
                            visible = passwordVisible,
                            size = 20.dp,
                            tint = if (passwordVisible) Color(0xFF4F46E5) else Color(0xFF9CA3AF)
                        )
                    }
                },
                textStyle = TextStyle(
                    color = Color(0xFF111827),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF111827),
                    focusedBorderColor = Color(0xFF4F46E5),
                    focusedLabelColor = Color(0xFF4338CA),
                    cursorColor = Color(0xFF111827),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (password.isNotBlank() && !isLoading) {
                        isLoading = true
                        scope.launch {
                            val res = VaultSecurityManager.unlockWithPassword(context, password)
                            isLoading = false
                            res.onFailure { ex ->
                                errorMessage = ex.message ?: "Incorrect password. Please try again."
                            }
                        }
                    }
                }),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SoftWarningIcon(size = 18.dp, color = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = err,
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Unlock Button
            Button(
                onClick = {
                    if (password.isBlank()) {
                        errorMessage = "Please enter your chat password."
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        val res = VaultSecurityManager.unlockWithPassword(context, password)
                        isLoading = false
                        res.onFailure { ex ->
                            errorMessage = ex.message ?: "Incorrect password. Please try again."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Unlock Chats",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (hasBiometric && activity != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val cryptoObjResult = VaultSecurityManager.createBiometricDecryptCryptoObject(context)
                        cryptoObjResult.onSuccess { cryptoObj ->
                            promptBiometricUnlock(activity, cryptoObj) { success ->
                                if (!success) {
                                    errorMessage = "Biometric authentication cancelled or failed."
                                }
                            }
                        }.onFailure { ex ->
                            errorMessage = ex.message ?: "Biometric unlock unavailable."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    SoftFingerprintIcon(size = 20.dp, color = Color(0xFF374151))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Unlock with Biometrics",
                        color = Color(0xFF374151),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(onClick = { showResetDialog = true }) {
                Text(
                    text = "Forgot password? Reset Vault",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftWarningIcon(size = 24.dp, color = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Chat Vault?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Text(
                    "Resetting the vault permanently wipes the local encryption keys and erases all chat history on this device.\n\nMessages cannot be recovered from the server.",
                    fontSize = 13.sp,
                    color = Color(0xFF4B5563)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        VaultSecurityManager.resetVault(context)
                        Toast.makeText(context, "Vault reset. Please set a new password.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Reset & Erase", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

/**
 * Modern good-looking Set Up Password popup with custom soft icons.
 */
@Composable
private fun ModernSetupPasswordCard() {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var enableBiometrics by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val canAuthenticateBiometric = remember {
        val bm = BiometricManager.from(context)
        bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 24.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SoftIconBadge(
                badgeSize = 64.dp,
                backgroundColor = Color(0xFFFFFBEB),
                borderColor = Color(0xFFFDE68A)
            ) {
                SoftLockIcon(
                    size = 32.dp,
                    isUnlocked = false,
                    gradientColors = listOf(Color(0xFFFBBF24), Color(0xFFD97706)),
                    shackleColor = Color(0xFF92400E)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Set Up Chat Lock",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Set a password to encrypt and secure your private conversations.",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Chat Password (min 4 chars)") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { passwordVisible = !passwordVisible },
                        contentAlignment = Alignment.Center
                    ) {
                        SoftEyeIcon(
                            visible = passwordVisible,
                            size = 20.dp,
                            tint = if (passwordVisible) Color(0xFFB45309) else Color(0xFF9CA3AF)
                        )
                    }
                },
                textStyle = TextStyle(
                    color = Color(0xFF111827),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF111827),
                    focusedBorderColor = Color(0xFFFFC529),
                    focusedLabelColor = Color(0xFFB45309),
                    cursorColor = Color(0xFF111827),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                label = { Text("Confirm Chat Password") },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { confirmPasswordVisible = !confirmPasswordVisible },
                        contentAlignment = Alignment.Center
                    ) {
                        SoftEyeIcon(
                            visible = confirmPasswordVisible,
                            size = 20.dp,
                            tint = if (confirmPasswordVisible) Color(0xFFB45309) else Color(0xFF9CA3AF)
                        )
                    }
                },
                textStyle = TextStyle(
                    color = Color(0xFF111827),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF111827),
                    focusedBorderColor = Color(0xFFFFC529),
                    focusedLabelColor = Color(0xFFB45309),
                    cursorColor = Color(0xFF111827),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            if (canAuthenticateBiometric) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF9FAFB),
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { enableBiometrics = !enableBiometrics }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SoftFingerprintIcon(
                            size = 22.dp,
                            color = if (enableBiometrics) Color(0xFF2563EB) else Color(0xFF9CA3AF)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Enable Biometric unlock",
                            fontSize = 13.sp,
                            color = Color(0xFF374151),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = enableBiometrics,
                            onCheckedChange = { enableBiometrics = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF111827)
                            )
                        )
                    }
                }
            }

            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SoftWarningIcon(size = 18.dp, color = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = err,
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (password.length < 4) {
                        errorMessage = "Password must be at least 4 characters."
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match."
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        val setupResult = VaultSecurityManager.setupVaultWithPassword(context, password)
                        setupResult.onSuccess {
                            if (enableBiometrics && canAuthenticateBiometric && activity != null) {
                                val cryptoResult = VaultSecurityManager.createBiometricEncryptCryptoObject()
                                cryptoResult.onSuccess { cryptoObject ->
                                    promptBiometricSetup(activity, cryptoObject) {
                                        isLoading = false
                                        Toast.makeText(context, "Chat lock active! Chats unlocked.", Toast.LENGTH_SHORT).show()
                                    }
                                }.onFailure {
                                    isLoading = false
                                    Toast.makeText(context, "Chat lock active! Chats unlocked.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                isLoading = false
                                Toast.makeText(context, "Chat lock active! Chats unlocked.", Toast.LENGTH_SHORT).show()
                            }
                        }.onFailure { ex ->
                            isLoading = false
                            errorMessage = ex.message ?: "Failed to set up chat password"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Set Password & Unlock",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun promptBiometricSetup(
    activity: FragmentActivity,
    cryptoObject: BiometricPrompt.CryptoObject,
    onComplete: (Boolean) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Enable Biometric Unlock")
        .setSubtitle("Confirm your fingerprint or face to protect your encrypted chats")
        .setNegativeButtonText("Skip for now")
        .build()

    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val cipher = result.cryptoObject?.cipher
                if (cipher != null) {
                    VaultSecurityManager.saveBiometricWrappedVaultKey(activity, cipher)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onComplete(false)
            }
        }
    )

    try {
        prompt.authenticate(promptInfo, cryptoObject)
    } catch (e: Exception) {
        onComplete(false)
    }
}

private fun promptBiometricUnlock(
    activity: FragmentActivity,
    cryptoObject: BiometricPrompt.CryptoObject,
    onComplete: (Boolean) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Chat Vault")
        .setSubtitle("Authenticate using your fingerprint or face")
        .setNegativeButtonText("Use Password")
        .build()

    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val cipher = result.cryptoObject?.cipher
                if (cipher != null) {
                    val res = VaultSecurityManager.unlockWithBiometricCipher(activity, cipher)
                    onComplete(res.isSuccess)
                } else {
                    onComplete(false)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onComplete(false)
            }
        }
    )

    try {
        prompt.authenticate(promptInfo, cryptoObject)
    } catch (e: Exception) {
        onComplete(false)
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
