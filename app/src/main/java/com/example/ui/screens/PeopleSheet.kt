package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.CircleFlag

@Composable
fun PeopleBottomSheetContent(
    selectedThemeIndex: Int,
    hostSeat: ChillRoomSpeaker,
    speakerSeats: List<ChillRoomSpeaker>,
    listenerSeats: List<Triple<String, String, String>>,
    listenerUserIds: List<String> = emptyList(),
    isCurrentUserHost: Boolean = false,
    currentUserId: String = "",
    pendingSpeakerRequests: List<com.example.SpeakerRequestItem> = emptyList(),
    onRespondToSpeakerRequest: (requestId: String, accept: Boolean) -> Unit = { _, _ -> },
    onUserClick: (PrivateChatItem) -> Unit = {},
    onInviteClick: () -> Unit = {},
    onModeratorAction: (targetUserId: String, targetUserName: String, actionType: String) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    val currentTheme = roomThemes[selectedThemeIndex]
    val isDark = currentTheme.isDark
    val totalCount = 1 + speakerSeats.size + listenerSeats.size
    val speakingCount = 1 + speakerSeats.size
    val listeningCount = listenerSeats.size

    var selectedUserForAction by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<ModerationTargetUser?>(null)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(currentTheme.startColor, currentTheme.endColor)
                    )
                )
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Total People / Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (isDark) currentTheme.themeColor else ChillGoldIcon, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CustomPeopleGroupIcon(tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "$totalCount People",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$speakingCount Speaking",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(4.dp)
                                    .background(if (isDark) currentTheme.themeColor else ChillGoldIcon, CircleShape)
                            )
                            Text(
                                text = "$listeningCount Listening",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray
                            )
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6), CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    CustomCloseIcon(tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isCurrentUserHost && pendingSpeakerRequests.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    CustomRaisedHandIcon(
                        tint = if (isDark) currentTheme.themeColor else ChillGoldIcon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Speaker Requests (${pendingSpeakerRequests.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF1E293B)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pendingSpeakerRequests.forEach { req ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color(0xFFFFFBEB),
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFFDE68A),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = req.requesterAvatar,
                                    contentDescription = req.requesterName,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = req.requesterName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Wants to speak",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Decline Button
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFF1F5F9),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onRespondToSpeakerRequest(req.id, false) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Decline",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF475569)
                                    )
                                }

                                // Accept Button
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isDark) currentTheme.themeColor else ChillGoldIcon,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onRespondToSpeakerRequest(req.id, true) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Accept",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Speaking Now Section Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                CustomMicIcon(tint = if (isDark) currentTheme.themeColor else ChillGoldIcon, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Speaking Now",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Gray
                )
            }

            // Host & Speakers Row
            val allSpeakers = listOf(hostSeat) + speakerSeats
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                allSpeakers.forEach { sp ->
                    PeopleSheetSpeakerItem(
                        name = sp.name,
                        imageUrl = sp.imageUrl,
                        badgeText = if (sp.isHost) "Host" else "Speaker",
                        badgeColor = if (sp.isHost) Color(0xFFFEF3C7) else Color(0xFFF3F4F6),
                        isHost = sp.isHost,
                        flagEmoji = mapCountryCodeToFlagEmoji(sp.flagCode),
                        isDarkMode = isDark,
                        onClick = {
                            if (isCurrentUserHost && !sp.isHost && sp.userId != currentUserId) {
                                selectedUserForAction = ModerationTargetUser(
                                    userId = sp.userId,
                                    name = sp.name,
                                    imageUrl = sp.imageUrl,
                                    flagCode = sp.flagCode,
                                    role = "speaker"
                                )
                            } else {
                                onUserClick(PrivateChatItem(sp.userId.ifBlank { sp.name.hashCode().toString() }, sp.name, sp.imageUrl, sp.flagCode, "", "Now", 0, true, false))
                            }
                        }
                    )
                }
            }

            // Listening Section Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                CustomHeadphoneIcon(tint = if (isDark) currentTheme.themeColor else ChillGoldIcon, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Listening ($listeningCount)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Gray
                )
            }

            if (listenerSeats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No listeners in the room yet. Invite friends!",
                        fontSize = 13.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Gray
                    )
                }
            } else {
                val listenerRows = listenerSeats.chunked(4)
                listenerRows.forEachIndexed { rowIndex, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEachIndexed { colIndex, (name, imageUrl, flagCode) ->
                            val globalIndex = rowIndex * 4 + colIndex
                            val targetUserId = if (globalIndex < listenerUserIds.size) listenerUserIds[globalIndex] else ""
                            PeopleSheetListenerItem(
                                name = name,
                                imageUrl = imageUrl,
                                modifier = Modifier.weight(1f),
                                flagEmoji = mapCountryCodeToFlagEmoji(flagCode),
                                isDarkMode = isDark,
                                onClick = {
                                    if (isCurrentUserHost && targetUserId.isNotBlank() && targetUserId != currentUserId) {
                                        selectedUserForAction = ModerationTargetUser(
                                            userId = targetUserId,
                                            name = name,
                                            imageUrl = imageUrl,
                                            flagCode = flagCode,
                                            role = "listener"
                                        )
                                    } else {
                                        onUserClick(
                                            PrivateChatItem(
                                                targetUserId.ifBlank { name.hashCode().toString() },
                                                name,
                                                imageUrl,
                                                flagCode,
                                                "",
                                                "Now",
                                                0,
                                                true,
                                                false
                                            )
                                        )
                                    }
                                }
                            )
                        }
                        if (rowItems.size < 4) {
                            repeat(4 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Large prominent "Invite Friend" CTA Button matching the image perfectly
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        if (isDark) currentTheme.themeColor else ChillGoldIcon,
                        RoundedCornerShape(28.dp)
                    )
                    .clickable { onInviteClick() }
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CustomPersonAddIcon(tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Invite Friend",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(if (isDark) Color.Black.copy(alpha = 0.2f) else Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else ChillGoldIcon,
                            modifier = Modifier.offset(y = (-1).dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(36.dp))
        }

        // Moderation Action Overlay Sheet
        selectedUserForAction?.let { target ->
            ModerationActionDialog(
                target = target,
                isDark = isDark,
                onDismiss = { selectedUserForAction = null },
                onMessage = {
                    val uid = target.userId.ifBlank { target.name.hashCode().toString() }
                    onUserClick(PrivateChatItem(uid, target.name, target.imageUrl, target.flagCode, "", "Now", 0, true, false))
                    selectedUserForAction = null
                },
                onMute = {
                    onModeratorAction(target.userId, target.name, "mute_mic")
                    selectedUserForAction = null
                },
                onKick = {
                    onModeratorAction(target.userId, target.name, "kick_from_room")
                    selectedUserForAction = null
                },
                onBan = {
                    onModeratorAction(target.userId, target.name, "ban_user")
                    selectedUserForAction = null
                }
            )
        }
    }
}

data class ModerationTargetUser(
    val userId: String,
    val name: String,
    val imageUrl: String,
    val flagCode: String,
    val role: String
)

@Composable
fun ModerationActionDialog(
    target: ModerationTargetUser,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onMessage: () -> Unit,
    onMute: () -> Unit,
    onKick: () -> Unit,
    onBan: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) Color(0xFF1E1F2E) else Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Target User Avatar & Info
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = target.imageUrl,
                    contentDescription = target.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = target.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Role: ${target.role.replaceFirstChar { it.uppercase() }}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))
            
            // Actions List
            // 1. Send Direct Message
            ModerationActionButton(
                label = "Send Message",
                iconText = "💬",
                containerColor = if (isDark) Color(0xFF2A2B3D) else Color(0xFFF3F4F6),
                textColor = if (isDark) Color.White else Color.Black,
                onClick = onMessage
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Mute Microphone
            if (target.role == "speaker") {
                ModerationActionButton(
                    label = "Mute Microphone",
                    iconText = "🔇",
                    containerColor = if (isDark) Color(0xFF332A1E) else Color(0xFFFEF3C7),
                    textColor = Color(0xFFD97706),
                    onClick = onMute
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. Kick from Room
            ModerationActionButton(
                label = "Kick from Room",
                iconText = "🚪",
                containerColor = if (isDark) Color(0xFF3A1F1F) else Color(0xFFFEE2E2),
                textColor = Color(0xFFDC2626),
                onClick = onKick
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Ban User
            ModerationActionButton(
                label = "Ban User",
                iconText = "🚫",
                containerColor = if (isDark) Color(0xFF451A1A) else Color(0xFFFFE4E6),
                textColor = Color(0xFFB91C1C),
                onClick = onBan
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel Button
            Text(
                text = "Cancel",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Gray,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun ModerationActionButton(
    label: String,
    iconText: String,
    containerColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(text = iconText, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun PeopleSheetSpeakerItem(
    name: String,
    imageUrl: String,
    badgeText: String,
    badgeColor: Color,
    isHost: Boolean,
    flagEmoji: String = "🇮🇳",
    isDarkMode: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.size(86.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(if (isHost) ChillGoldIcon else Color.Transparent)
                    .padding(if (isHost) 3.dp else 0.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-2).dp)
                    .size(20.dp)
                    .background(if (isDarkMode) Color(0xFF1E293B) else Color.White, CircleShape)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                CircleFlag(
                    countryCode = mapFlagEmojiToCode(flagEmoji),
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else Color.Black
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .background(if (isDarkMode) Color.White.copy(alpha = 0.12f) else badgeColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHost) {
                    if (isDarkMode) ChillGoldIcon else Color(0xFFC2410C)
                } else {
                    if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray
                }
            )
        }
    }
}

@Composable
fun PeopleSheetListenerItem(
    name: String,
    imageUrl: String,
    modifier: Modifier = Modifier,
    flagEmoji: String = "🇮🇳",
    isDarkMode: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-2).dp)
                    .size(16.dp)
                    .background(if (isDarkMode) Color(0xFF1E293B) else Color.White, CircleShape)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                CircleFlag(
                    countryCode = mapFlagEmojiToCode(flagEmoji),
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) Color.White else Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CustomCloseIcon(tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.25f), end = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.75f), strokeWidth = w * 0.12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.25f), end = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.75f), strokeWidth = w * 0.12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
fun CustomHeadphoneIcon(tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.12f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.2f, h * 0.7f)
            quadraticTo(w * 0.2f, h * 0.2f, w * 0.5f, h * 0.2f)
            quadraticTo(w * 0.8f, h * 0.2f, w * 0.8f, h * 0.7f)
        }
        drawPath(path, color = tint, style = stroke)
        drawRoundRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.6f), size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.25f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
        drawRoundRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.6f), size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.25f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
    }
}

@Composable
fun CustomPersonAddIcon(tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = tint, radius = w * 0.2f, center = androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.35f))
        val body = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.1f, h * 0.85f)
            quadraticTo(w * 0.15f, h * 0.55f, w * 0.4f, h * 0.55f)
            quadraticTo(w * 0.65f, h * 0.55f, w * 0.7f, h * 0.85f)
        }
        drawPath(body, color = tint)
        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.45f), end = androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.45f), strokeWidth = w * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.35f), end = androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.55f), strokeWidth = w * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

