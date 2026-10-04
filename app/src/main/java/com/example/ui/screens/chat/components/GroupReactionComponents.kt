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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.SupabaseGroupMember
import com.example.db.LocalGroupMessage
import com.example.db.LocalMessageReaction
import com.example.ui.components.FunkyCopyIcon
import com.example.ui.components.FunkyPlusIcon
import com.example.ui.components.FunkyReplyIcon
import com.example.ui.components.SoftShieldIcon

/**
 * Quick Emojis specified in prompt: 👍 ❤️ 😂 😮 😢 🙏 and "+" for full picker
 */
val QUICK_REACTION_EMOJIS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

val EXTENDED_REACTION_EMOJIS = listOf(
    "👍", "❤️", "😂", "😮", "😢", "🙏",
    "🔥", "🎉", "👏", "🥰", "😍", "🥳",
    "✨", "💯", "😎", "🤩", "🤝", "💪",
    "🙌", "🤔", "👀", "🚀", "⚡", "🌟",
    "💡", "🎈", "🎁", "💖", "💔", "😭",
    "😡", "🤯", "😴", "🤮", "💩", "👻"
)

/**
 * Floating Long-Press Reaction Bar & Context Menu for Group Messages
 */
@Composable
fun GroupMessageReactionOverlay(
    message: LocalGroupMessage,
    myReaction: String?,
    onDismiss: () -> Unit,
    onSelectEmoji: (emoji: String, isRemove: Boolean) -> Unit,
    onOpenFullPicker: () -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
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
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (message.isOutgoing) Color(0xFFFEF3C7) else Color.White,
                border = BorderStroke(1.dp, if (message.isOutgoing) Color(0xFFFDE68A) else Color(0xFFE2E8F0)),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = message.text.ifBlank { "Media message" },
                    fontSize = 14.5.sp,
                    color = Color(0xFF0F172A),
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

            // 3. Context Menu Actions Card (Reply, Copy, Report)
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

                    Spacer(modifier = Modifier.height(2.dp))

                    // Action 2: Copy
                    if (message.messageType == "text" && message.text.isNotBlank()) {
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

                    // Action 3: Report (if incoming)
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
 * Reaction chips rendered underneath a group message bubble.
 * Grouped by emoji with count, mine highlighted in FunkyTalk yellow.
 */
@Composable
fun GroupReactionChipsRow(
    reactions: List<LocalMessageReaction>,
    currentUserId: String,
    onChipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (reactions.isEmpty()) return

    val grouped = remember(reactions) {
        reactions.groupBy { it.emoji }
    }

    LazyRow(
        modifier = modifier
            .padding(top = 3.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(grouped.entries.toList(), key = { it.key }) { (emoji, list) ->
            val isMine = list.any { it.senderId == currentUserId }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isMine) Color(0xFFFEF3C7) else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isMine) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                ),
                shadowElevation = 0.5.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onChipClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.5.dp, vertical = 2.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = emoji, fontSize = 13.sp)
                    if (list.size > 1) {
                        Spacer(modifier = Modifier.width(3.5.dp))
                        Text(
                            text = "${list.size}",
                            fontSize = 11.5.sp,
                            fontWeight = if (isMine) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isMine) Color(0xFFB45309) else Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom sheet listing who reacted (names/avatars from group members).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupReactionDetailsBottomSheet(
    message: LocalGroupMessage,
    reactions: List<LocalMessageReaction>,
    groupMembers: List<SupabaseGroupMember>,
    currentUserId: String,
    onRemoveMyReaction: (emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilterEmoji by remember { mutableStateOf<String?>(null) }

    val distinctEmojis = remember(reactions) {
        reactions.map { it.emoji }.distinct()
    }

    val filteredReactions = remember(reactions, selectedFilterEmoji) {
        if (selectedFilterEmoji == null) reactions else reactions.filter { it.emoji == selectedFilterEmoji }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Header: Title & Total Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Reactions (${reactions.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            // Filter Tabs Row
            if (distinctEmojis.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilterEmoji == null,
                            onClick = { selectedFilterEmoji = null },
                            label = { Text("All (${reactions.size})", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEF3C7),
                                selectedLabelColor = Color(0xFFB45309),
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF475569)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilterEmoji == null,
                                borderColor = if (selectedFilterEmoji == null) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                            )
                        )
                    }
                    items(distinctEmojis, key = { it }) { emoji ->
                        val count = reactions.count { it.emoji == emoji }
                        FilterChip(
                            selected = selectedFilterEmoji == emoji,
                            onClick = { selectedFilterEmoji = emoji },
                            label = { Text("$emoji $count", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEF3C7),
                                selectedLabelColor = Color(0xFFB45309),
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF475569)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilterEmoji == emoji,
                                borderColor = if (selectedFilterEmoji == emoji) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Reactor List
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
                        .heightIn(max = 360.dp)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    items(filteredReactions, key = { it.senderId + "_" + it.emoji }) { reaction ->
                        val isMe = reaction.senderId == currentUserId
                        val member = groupMembers.find { it.user_id == reaction.senderId }
                        val displayName = when {
                            isMe -> "You"
                            member != null && !member.profile_name.isNullOrBlank() -> member.profile_name
                            member != null && !member.username.isNullOrBlank() -> member.username
                            else -> "Group Member"
                        }
                        val avatarUrl = member?.avatar_url

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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = displayName,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (member?.role.equals("admin", ignoreCase = true)) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                text = "Admin",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB45309),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
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

/**
 * Full Emoji Picker Bottom Sheet for selecting any emoji
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullEmojiPickerBottomSheet(
    myCurrentReaction: String?,
    onEmojiSelected: (emoji: String, isRemove: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "React to message",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(EXTENDED_REACTION_EMOJIS) { emoji ->
                    val isSelected = myCurrentReaction == emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFFFEF3C7) else Color(0xFFF8FAFC))
                            .border(
                                if (isSelected) BorderStroke(1.5.dp, Color(0xFFF59E0B)) else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onEmojiSelected(emoji, isSelected)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                    }
                }
            }
        }
    }
}
