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
import com.example.ui.theme.LocalFunkyColors
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
    val funkyColors = LocalFunkyColors.current
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
        containerColor = funkyColors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = funkyColors.line) },
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
                            color = funkyColors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            SoftPinIcon(size = 15.dp, color = funkyColors.mango)
                        }
                        if (isMuted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            SoftMuteIcon(size = 14.dp, color = funkyColors.mute)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = chat.lastMsg,
                        fontSize = 13.sp,
                        color = funkyColors.mute,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = funkyColors.line, thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Pin / Unpin
            ChatCustomActionItem(
                icon = { SoftPinIcon(size = 20.dp, color = if (isPinned) funkyColors.mango else funkyColors.mute) },
                iconBg = if (isPinned) funkyColors.soft else funkyColors.bg,
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
                        SoftVolumeIcon(size = 20.dp, color = funkyColors.ok)
                    } else {
                        SoftMuteIcon(size = 20.dp, color = funkyColors.mute)
                    }
                },
                iconBg = if (isMuted) funkyColors.ok.copy(alpha = 0.14f) else funkyColors.bg,
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
                iconBg = Color(0xFF3B82F6).copy(alpha = 0.14f),
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
                icon = { SoftBroomIcon(size = 20.dp, color = funkyColors.mute) },
                iconBg = funkyColors.bg,
                title = "Clear chat history",
                subtitle = "Erase all decrypted messages in this thread",
                onClick = {
                    showClearConfirmDialog = true
                }
            )

            // Action: Delete Chat
            ChatCustomActionItem(
                icon = { SoftTrashIcon(size = 20.dp, color = funkyColors.danger) },
                iconBg = funkyColors.danger.copy(alpha = 0.14f),
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
                    icon = { SoftShieldIcon(size = 20.dp, color = funkyColors.ok) },
                    iconBg = funkyColors.ok.copy(alpha = 0.14f),
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
                    icon = { SoftBlockIcon(size = 20.dp, color = funkyColors.danger) },
                    iconBg = funkyColors.danger.copy(alpha = 0.14f),
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
                    color = funkyColors.surface,
                    shadowElevation = 24.dp,
                    border = BorderStroke(1.dp, funkyColors.danger.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // User Avatar with red ban badge
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
                                    .border(2.5.dp, funkyColors.danger.copy(alpha = 0.4f), CircleShape)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(funkyColors.danger)
                                    .border(2.dp, funkyColors.surface, CircleShape),
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
                            color = funkyColors.ink,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Protection Highlights Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = funkyColors.danger.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, funkyColors.danger.copy(alpha = 0.22f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SoftBlockIcon(size = 15.dp, color = funkyColors.danger)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cannot send messages or voice notes",
                                        fontSize = 12.5.sp,
                                        color = funkyColors.ink,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SoftMuteIcon(size = 15.dp, color = funkyColors.danger)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cannot call you or see your online status",
                                        fontSize = 12.5.sp,
                                        color = funkyColors.ink,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SoftShieldIcon(size = 15.dp, color = funkyColors.danger)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Hidden from your conversations list",
                                        fontSize = 12.5.sp,
                                        color = funkyColors.ink,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Tip: You can unblock this user anytime from 3-dots Menu > Blocked Users.",
                            fontSize = 12.sp,
                            color = funkyColors.mute,
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
                                border = BorderStroke(1.dp, funkyColors.line)
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = funkyColors.ink,
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
                                colors = ButtonDefaults.buttonColors(containerColor = funkyColors.danger)
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

    // Custom Delete Confirmation Dialog
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
                    color = funkyColors.surface,
                    shadowElevation = 16.dp,
                    border = BorderStroke(1.dp, funkyColors.line)
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
                                .background(funkyColors.danger.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftTrashIcon(size = 32.dp, color = funkyColors.danger)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Delete Conversation?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = funkyColors.ink,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Are you sure you want to delete this conversation with ${chat.name}? All local encrypted messages will be removed.",
                            fontSize = 13.5.sp,
                            color = funkyColors.mute,
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
                                border = BorderStroke(1.dp, funkyColors.line)
                            ) {
                                Text("Cancel", color = funkyColors.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                colors = ButtonDefaults.buttonColors(containerColor = funkyColors.danger)
                            ) {
                                Text("Delete", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Clear Messages Confirmation Dialog
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
                    color = funkyColors.surface,
                    shadowElevation = 16.dp,
                    border = BorderStroke(1.dp, funkyColors.line)
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
                                .background(funkyColors.soft),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftBroomIcon(size = 32.dp, color = funkyColors.mango)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Clear Messages?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = funkyColors.ink,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Are you sure you want to clear message history with ${chat.name}? The conversation thread will remain in your list.",
                            fontSize = 13.5.sp,
                            color = funkyColors.mute,
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
                                border = BorderStroke(1.dp, funkyColors.line)
                            ) {
                                Text("Cancel", color = funkyColors.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango)
                            ) {
                                Text("Clear History", color = funkyColors.onMango, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
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
    val funkyColors = LocalFunkyColors.current
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
                color = if (isDestructive) funkyColors.danger else funkyColors.ink
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = funkyColors.mute
            )
        }
    }
}
