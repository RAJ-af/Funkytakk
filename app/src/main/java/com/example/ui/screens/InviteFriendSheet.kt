package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.PresenceAvatar
import com.example.ui.theme.LocalFunkyColors
import com.example.util.RoomShareHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InviteFriendBottomSheetContent(
    isDarkMode: Boolean,
    roomName: String = "FunkyTalk Room",
    roomId: String = "",
    inviteLink: String = RoomShareHelper.buildDefaultInviteLink(roomId),
    onDismiss: () -> Unit,
    onDiscoverPeople: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val funkyColors = LocalFunkyColors.current
    val textColor = funkyColors.ink
    val subColor = funkyColors.mute
    val chipBg = funkyColors.soft

    var isLoading by remember { mutableStateOf(true) }
    var invitableUsers by remember { mutableStateOf<List<com.example.InvitableUserResponse>>(emptyList()) }
    val invitedUserIds = remember { mutableStateMapOf<String, Boolean>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(roomId, reloadTrigger) {
        if (roomId.isNotBlank()) {
            isLoading = true
            errorMessage = null
            try {
                val token = com.example.SupabaseSessionManager.getBearerToken(context)
                val res = com.example.SupabaseClient.api.listInvitableFollowing(
                    auth = token,
                    body = mapOf("p_room_id" to roomId)
                )
                if (res.isSuccessful && res.body() != null) {
                    invitableUsers = res.body() ?: emptyList()
                } else {
                    errorMessage = "Could not load followed friends"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to connect"
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .background(funkyColors.bg)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Invite Friend",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(chipBg)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Close",
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Share Platforms Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val platforms = listOf(
                Triple("Copy Link", R.drawable.ic_copy, Color(0xFF0EA5E9)),
                Triple("WhatsApp", R.drawable.ic_share, Color(0xFF22B573)),
                Triple("Discover", R.drawable.ic_discover, Color(0xFFF59E0B)),
                Triple("More", R.drawable.ic_more_vert, funkyColors.ink)
            )
            platforms.forEach { (name, iconRes, iconTint) ->
                val testTag = when (name) {
                    "Copy Link" -> "share_copy_link_button"
                    "WhatsApp" -> "share_whatsapp_button"
                    "Discover" -> "share_facebook_button"
                    else -> "share_more_button"
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .testTag(testTag)
                        .clickable {
                            when (name) {
                                "Copy Link" -> {
                                    RoomShareHelper.copyLinkToClipboard(context, inviteLink)
                                    onDismiss()
                                }
                                "WhatsApp" -> {
                                    RoomShareHelper.shareToWhatsApp(context, roomName, roomId, inviteLink)
                                    onDismiss()
                                }
                                "Discover" -> {
                                    onDismiss()
                                    onDiscoverPeople?.invoke()
                                }
                                "More" -> {
                                    RoomShareHelper.shareMore(context, roomName, roomId, inviteLink)
                                    onDismiss()
                                }
                                else -> onDismiss()
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(chipBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = name,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = name, fontSize = 12.sp, color = subColor, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Friends You Follow",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = funkyColors.mango, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading followed friends...",
                            fontSize = 13.sp,
                            color = subColor
                        )
                    }
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Failed to load followed friends.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "Unknown error occurred",
                            fontSize = 12.sp,
                            color = funkyColors.danger,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { reloadTrigger++ },
                            colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango, contentColor = funkyColors.onMango),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_refresh),
                                contentDescription = "Retry",
                                tint = funkyColors.onMango,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = funkyColors.onMango, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            invitableUsers.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "You are not following anyone yet. Follow people first to invite them here.",
                            fontSize = 14.sp,
                            color = subColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                onDiscoverPeople?.invoke()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango, contentColor = funkyColors.onMango),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = "Discover",
                                tint = funkyColors.onMango,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Discover People",
                                color = funkyColors.onMango,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(
                        items = invitableUsers,
                        key = { it.user_id }
                    ) { user ->
                        val isPending = user.invite_status == "pending" || invitedUserIds[user.user_id] == true
                        val isInRoom = user.in_room
                        val username = user.username?.ifBlank { "Learner" } ?: "Learner"
                        val flagCode = user.country_code?.ifBlank { null }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PresenceAvatar(
                                    avatarUrl = user.avatar_url,
                                    size = 46.dp,
                                    isSpeaking = false,
                                    isOnline = user.is_online,
                                    countryCode = flagCode,
                                    showFlagBadge = true
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = username,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isInRoom) "In room" else if (user.is_online) "Online" else "Offline",
                                        fontSize = 12.sp,
                                        color = if (isInRoom) Color(0xFF3B82F6) else if (user.is_online) funkyColors.ok else subColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            val buttonText = when {
                                isInRoom -> "In room"
                                isPending -> "Invited"
                                else -> "Invite"
                            }
                            val isButtonDisabled = isInRoom || isPending

                            Button(
                                onClick = {
                                    if (!isButtonDisabled) {
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val token = com.example.SupabaseSessionManager.getBearerToken(context)
                                                val res = com.example.SupabaseClient.api.sendRoomInvite(
                                                    auth = token,
                                                    body = mapOf("p_room_id" to roomId, "p_user_id" to user.user_id)
                                                )
                                                if (res.isSuccessful) {
                                                    invitedUserIds[user.user_id] = true
                                                    withContext(Dispatchers.Main) {
                                                        android.widget.Toast.makeText(context, "Invite sent to $username!", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    val errBody = res.errorBody()?.string() ?: res.message()
                                                    val msg = when {
                                                        errBody.contains("already_invited") -> "Already invited, try again in a minute"
                                                        errBody.contains("not_following") -> "You can only invite people you follow"
                                                        errBody.contains("already_in_room") -> "User is already in the room"
                                                        errBody.contains("not_in_room") -> "You must be in the room to invite others"
                                                        errBody.contains("room_not_live") -> "This room has ended"
                                                        errBody.contains("rate_limited") -> "Too many invites, try later"
                                                        errBody.contains("cannot_invite_self") -> "You cannot invite yourself"
                                                        else -> "Could not send invite"
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    android.widget.Toast.makeText(context, "Error sending invite", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = !isButtonDisabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isButtonDisabled) chipBg else funkyColors.mango,
                                    disabledContainerColor = chipBg
                                ),
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = buttonText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isButtonDisabled) subColor else funkyColors.onMango
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
