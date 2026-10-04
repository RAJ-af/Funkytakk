package com.example.ui.screens.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.db.LocalDmMessage
import com.example.db.LocalDmMessageReaction
import com.example.ui.components.FunkyCopyIcon
import com.example.ui.components.FunkyPlusIcon
import com.example.ui.components.FunkyReplyIcon
import com.example.ui.components.SoftShieldIcon

@Composable
private fun ContextMenuItemRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F172A),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Floating Long-Press Reaction Bar & Context Menu for 1:1 Direct Messages
 */
@Composable
fun DmMessageReactionOverlay(
    message: LocalDmMessage,
    myReaction: String?,
    onDismiss: () -> Unit,
    onSelectEmoji: (emoji: String, isRemove: Boolean) -> Unit,
    onOpenFullPicker: () -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onTranslate: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume click */ }
        ) {
            // 1. Highlighted Bubble Preview in Center
            val isMedia = message.text.startsWith("[img]") || message.text.startsWith("[imgs]") || message.text.startsWith("[voice]")
            val previewText = when {
                message.text.startsWith("[img]") || message.text.startsWith("[imgs]") -> "Photo"
                message.text.startsWith("[voice]") -> "Voice Note"
                else -> message.text
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (message.isOutgoing) Color(0xFFF6C945) else Color(0xFFF3F4F6),
                border = BorderStroke(1.dp, if (message.isOutgoing) Color(0xFFEAB308).copy(alpha = 0.4f) else Color(0xFFE5E7EB)),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = previewText.ifBlank { "Message" },
                    fontSize = 14.5.sp,
                    color = Color(0xFF111827),
                    lineHeight = 20.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // 2. Quick Reaction Emoji Bar (👍 ❤️ 😂 😮 😢 🙏 +)
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QUICK_REACTION_EMOJIS.forEach { emoji ->
                        val isSelected = myReaction == emoji
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFFEF3C7) else Color.Transparent)
                                .border(
                                    if (isSelected) BorderStroke(1.5.dp, Color(0xFFF59E0B)) else BorderStroke(0.dp, Color.Transparent),
                                    CircleShape
                                )
                                .clickable {
                                    onSelectEmoji(emoji, isSelected)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }

                    // "+" Button for Full Picker
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable {
                                onOpenFullPicker()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        FunkyPlusIcon(color = Color(0xFF475569), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // 3. Context Menu Actions Card (Reply, Translate, Copy, Report)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Action 1: Reply
                    ContextMenuItemRow(
                        icon = { FunkyReplyIcon(color = Color(0xFF0F172A), modifier = Modifier.size(18.dp)) },
                        label = "Reply",
                        onClick = {
                            onReply()
                            onDismiss()
                        }
                    )

                    // Action 2: Translate (if incoming text message)
                    if (!message.isOutgoing && !isMedia && onTranslate != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        ContextMenuItemRow(
                            icon = { Text("🌐", fontSize = 16.sp) },
                            label = "Translate",
                            onClick = {
                                onTranslate()
                                onDismiss()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Action 3: Copy (if text message)
                    if (!isMedia && message.text.isNotBlank()) {
                        ContextMenuItemRow(
                            icon = { FunkyCopyIcon(color = Color(0xFF0F172A), modifier = Modifier.size(18.dp)) },
                            label = "Copy text",
                            onClick = {
                                onCopy()
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Action 4: Report (if incoming)
                    if (!message.isOutgoing && onReport != null) {
                        ContextMenuItemRow(
                            icon = { SoftShieldIcon(color = Color(0xFFEF4444), size = 18.dp) },
                            label = "Report message",
                            onClick = {
                                onReport()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Horizontal Row of reaction chips below a 1:1 message bubble
 */
@Composable
fun DmReactionChipsRow(
    reactions: List<LocalDmMessageReaction>,
    currentUserId: String,
    onChipClick: (emoji: String) -> Unit,
    onAddReactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (reactions.isEmpty()) return

    val grouped = remember(reactions) {
        reactions.groupBy { it.emoji }
    }

    LazyRow(
        modifier = modifier.padding(top = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(grouped.entries.toList(), key = { it.key }) { (emoji, list) ->
            val containsMe = list.any { it.senderId == currentUserId }
            val count = list.size

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (containsMe) Color(0xFFFEF3C7) else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (containsMe) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                ),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onChipClick(emoji) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.5.dp, vertical = 2.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = emoji, fontSize = 13.sp)
                    if (count > 1) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = count.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (containsMe) Color(0xFFB45309) else Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Sheet showing details of who reacted to a 1:1 message
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DmReactionDetailsBottomSheet(
    reactions: List<LocalDmMessageReaction>,
    currentUserId: String,
    partnerName: String,
    partnerAvatarUrl: String?,
    onRemoveMyReaction: (emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedEmojiFilter by remember { mutableStateOf<String?>(null) }

    val allEmojis = remember(reactions) {
        reactions.map { it.emoji }.distinct()
    }

    val filteredReactions = remember(reactions, selectedEmojiFilter) {
        if (selectedEmojiFilter == null) reactions
        else reactions.filter { it.emoji == selectedEmojiFilter }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Header Tabs: "All (x)", "👍 1", "❤️ 1"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reactions",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.weight(1f))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedEmojiFilter == null,
                            onClick = { selectedEmojiFilter = null },
                            label = { Text("All (${reactions.size})", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEF3C7),
                                selectedLabelColor = Color(0xFFB45309)
                            )
                        )
                    }
                    items(allEmojis) { emoji ->
                        val count = reactions.count { it.emoji == emoji }
                        FilterChip(
                            selected = selectedEmojiFilter == emoji,
                            onClick = { selectedEmojiFilter = emoji },
                            label = { Text("$emoji $count", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEF3C7),
                                selectedLabelColor = Color(0xFFB45309)
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // User Reactions List
            if (filteredReactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No reactions", color = Color(0xFF94A3B8), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    items(filteredReactions, key = { it.senderId + "_" + it.emoji }) { reaction ->
                        val isMe = reaction.senderId == currentUserId
                        val displayName = if (isMe) "You" else partnerName
                        val avatarUrl = if (isMe) null else partnerAvatarUrl

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = isMe) {
                                    onRemoveMyReaction(reaction.emoji)
                                    onDismiss()
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User Avatar
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Text(
                                        text = displayName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                if (isMe) {
                                    Text(
                                        text = "Tap to remove reaction",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }

                            Text(
                                text = reaction.emoji,
                                fontSize = 22.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
