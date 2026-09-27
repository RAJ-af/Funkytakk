package com.example.ui.screens.home.dialogs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ChatPreferencesManager
import com.example.repository.EncryptedChatRepository
import com.example.ui.components.*
import com.example.ui.screens.home.models.DirectChat
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatLongPressBottomSheet(
    chat: DirectChat,
    onDismiss: () -> Unit,
    onChatDeleted: () -> Unit = {},
    onChatCleared: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isMuted = ChatPreferencesManager.isChatMuted(chat.id)
    val isPinned = ChatPreferencesManager.isChatPinned(chat.id)
    val isBlocked = ChatPreferencesManager.isUserBlocked(chat.id)
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            // Header with User Avatar, Name, and Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(54.dp)) {
                    UserAvatarImage(
                        avatarUrl = chat.avatar,
                        contentDescription = chat.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = chat.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            SoftPinIcon(size = 15.dp, color = Color(0xFFD97706))
                        }
                        if (isMuted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            SoftMuteIcon(size = 14.dp, color = Color(0xFF9CA3AF))
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = chat.lastMsg,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Pin / Unpin
            ChatCustomActionItem(
                icon = { SoftPinIcon(size = 20.dp, color = if (isPinned) Color(0xFFD97706) else Color(0xFF4B5563)) },
                iconBg = if (isPinned) Color(0xFFFEF3C7) else Color(0xFFF3F4F6),
                title = if (isPinned) "Unpin from top" else "Pin to top",
                subtitle = if (isPinned) "Remove from top of chats list" else "Keep this conversation pinned at the top",
                onClick = {
                    val pinnedNow = ChatPreferencesManager.togglePinChat(context, chat.id)
                    Toast.makeText(context, if (pinnedNow) "Chat pinned to top" else "Chat unpinned", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // Action: Mute / Unmute Notifications
            ChatCustomActionItem(
                icon = {
                    if (isMuted) {
                        SoftVolumeIcon(size = 20.dp, color = Color(0xFF10B981))
                    } else {
                        SoftMuteIcon(size = 20.dp, color = Color(0xFF4B5563))
                    }
                },
                iconBg = if (isMuted) Color(0xFFD1FAE5) else Color(0xFFF3F4F6),
                title = if (isMuted) "Unmute notifications" else "Mute notifications (Silent)",
                subtitle = if (isMuted) "Turn on alerts for ${chat.name}" else "Silence message alerts from ${chat.name}",
                onClick = {
                    val mutedNow = ChatPreferencesManager.toggleMuteChat(context, chat.id)
                    Toast.makeText(context, if (mutedNow) "Notifications muted (Silent)" else "Notifications unmuted", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // Action: Mark as Read / Unread
            ChatCustomActionItem(
                icon = { SoftMailIcon(size = 20.dp, color = Color(0xFF3B82F6)) },
                iconBg = Color(0xFFDBEAFE),
                title = if (chat.unreadCount > 0) "Mark as read" else "Mark as unread",
                subtitle = "Toggle unread status badge",
                onClick = {
                    scope.launch {
                        if (chat.unreadCount > 0) {
                            EncryptedChatRepository.markConversationRead(chat.id)
                            ChatPreferencesManager.clearUnreadOverride(context, chat.id)
                            Toast.makeText(context, "Marked as read", Toast.LENGTH_SHORT).show()
                        } else {
                            EncryptedChatRepository.markConversationUnread(chat.id)
                            ChatPreferencesManager.toggleUnreadOverride(context, chat.id)
                            Toast.makeText(context, "Marked as unread", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                }
            )

            // Action: Clear Chat History
            ChatCustomActionItem(
                icon = { SoftBroomIcon(size = 20.dp, color = Color(0xFF6B7280)) },
                iconBg = Color(0xFFF3F4F6),
                title = "Clear chat history",
                subtitle = "Erase all decrypted messages in this thread",
                onClick = {
                    showClearConfirmDialog = true
                }
            )

            // Action: Delete Chat
            ChatCustomActionItem(
                icon = { SoftTrashIcon(size = 20.dp, color = Color(0xFFDC2626)) },
                iconBg = Color(0xFFFEE2E2),
                title = "Delete conversation",
                subtitle = "Remove this chat thread from your list",
                isDestructive = true,
                onClick = {
                    showDeleteConfirmDialog = true
                }
            )

            // Action: Block / Unblock User
            if (isBlocked) {
                ChatCustomActionItem(
                    icon = { SoftShieldIcon(size = 20.dp, color = Color(0xFF059669)) },
                    iconBg = Color(0xFFD1FAE5),
                    title = "Unblock ${chat.name}",
                    subtitle = "Restore chat messages and voice calls",
                    onClick = {
                        ChatPreferencesManager.unblockUser(context, chat.id)
                        Toast.makeText(context, "${chat.name} unblocked successfully", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )
            } else {
                ChatCustomActionItem(
                    icon = { SoftBlockIcon(size = 20.dp, color = Color(0xFFDC2626)) },
                    iconBg = Color(0xFFFEE2E2),
                    title = "Block ${chat.name}",
                    subtitle = "Prevent this user from messaging or calling you",
                    isDestructive = true,
                    onClick = {
                        showBlockConfirmDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Custom Luxury Premium Block Confirmation Dialog
    if (showBlockConfirmDialog) {
        Dialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showBlockConfirmDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    shadowElevation = 24.dp,
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // User Avatar with glowing red ban badge
                        Box(
                            modifier = Modifier.size(76.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            UserAvatarImage(
                                avatarUrl = chat.avatar,
                                contentDescription = chat.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.5.dp, Color(0xFFFECDD3), CircleShape)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE11D48))
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                SoftBlockIcon(size = 14.dp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Block ${chat.name}?",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Protection Highlights Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFFFF1F2),
                            border = BorderStroke(1.dp, Color(0xFFFFE4E6))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SoftBlockIcon(size = 15.dp, color = Color(0xFFE11D48))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cannot send messages or voice notes",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF9F1239),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SoftMuteIcon(size = 15.dp, color = Color(0xFFE11D48))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cannot call you or see your online status",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF9F1239),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SoftShieldIcon(size = 15.dp, color = Color(0xFFE11D48))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Hidden from your conversations list",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF9F1239),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "💡 You can unblock this user anytime from 3-dots Menu > Blocked Users.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showBlockConfirmDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = Color(0xFF374151),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Button(
                                onClick = {
                                    showBlockConfirmDialog = false
                                    ChatPreferencesManager.blockUser(context, chat.id)
                                    ChatPreferencesManager.markChatDeleted(context, chat.id)
                                    Toast.makeText(
                                        context,
                                        "${chat.name} blocked. You can unblock anytime in 3-dots Menu > Blocked Users.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onChatDeleted()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                            ) {
                                Text(
                                    text = "Block User",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Beautiful Luxury Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        Dialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showDeleteConfirmDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftTrashIcon(size = 32.dp, color = Color(0xFFDC2626))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Delete Conversation?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Are you sure you want to delete this conversation with ${chat.name}? All local encrypted messages will be removed.",
                            fontSize = 13.5.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Text("Cancel", color = Color(0xFF374151), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Button(
                                onClick = {
                                    showDeleteConfirmDialog = false
                                    scope.launch {
                                        EncryptedChatRepository.deleteConversationLocally(chat.id)
                                        ChatPreferencesManager.markChatDeleted(context, chat.id)
                                        Toast.makeText(context, "Conversation deleted", Toast.LENGTH_SHORT).show()
                                        onChatDeleted()
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                            ) {
                                Text("Delete", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Beautiful Luxury Clear Messages Confirmation Dialog
    if (showClearConfirmDialog) {
        Dialog(
            onDismissRequest = { showClearConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showClearConfirmDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftBroomIcon(size = 32.dp, color = Color(0xFFD97706))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Clear Messages?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Are you sure you want to clear message history with ${chat.name}? The conversation thread will remain in your list.",
                            fontSize = 13.5.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showClearConfirmDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Text("Cancel", color = Color(0xFF374151), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Button(
                                onClick = {
                                    showClearConfirmDialog = false
                                    scope.launch {
                                        EncryptedChatRepository.clearConversationMessagesLocally(chat.id)
                                        Toast.makeText(context, "Messages cleared", Toast.LENGTH_SHORT).show()
                                        onChatCleared()
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                            ) {
                                Text("Clear History", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatCustomActionItem(
    icon: @Composable () -> Unit,
    iconBg: Color,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDestructive) Color(0xFFDC2626) else Color(0xFF111827)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}
