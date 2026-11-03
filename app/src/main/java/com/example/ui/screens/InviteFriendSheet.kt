package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PresenceAvatar
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
    val textColor = if (isDarkMode) Color.White else Color.Black
    val bgColors = if (isDarkMode) Color(0xFF1F2937) else Color(0xFFF3F4F6)

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
                android.util.Log.d("InviteFriend", "Calling list_invitable_following for roomId=$roomId")
                val res = com.example.SupabaseClient.api.listInvitableFollowing(
                    auth = token,
                    body = mapOf("p_room_id" to roomId)
                )
                if (res.isSuccessful && res.body() != null) {
                    val users = res.body() ?: emptyList()
                    android.util.Log.d("InviteFriend", "list_invitable_following SUCCESS: received ${users.size} followed users")
                    invitableUsers = users
                } else {
                    val err = res.errorBody()?.string() ?: res.message()
                    android.util.Log.d("InviteFriend", "list_invitable_following ERROR code=${res.code()}: $err")
                    errorMessage = "Could not load followed friends"
                }
            } catch (e: Exception) {
                android.util.Log.d("InviteFriend", "list_invitable_following EXCEPTION: ${e.message}")
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
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
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
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(bgColors, CircleShape)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Share Platforms Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val platforms = listOf(
                "Copy Link" to Icons.Default.Share,
                "WhatsApp" to Icons.Default.Add,
                "Facebook" to Icons.Default.Face,
                "More" to Icons.Default.MoreVert
            )
            platforms.forEach { (name, icon) ->
                val testTag = when (name) {
                    "Copy Link" -> "share_copy_link_button"
                    "WhatsApp" -> "share_whatsapp_button"
                    "Facebook" -> "share_facebook_button"
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
                                "Facebook" -> {
                                    RoomShareHelper.shareToFacebook(context, roomName, roomId, inviteLink)
                                    onDismiss()
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
                            .background(bgColors, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = name, tint = textColor, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = name, fontSize = 12.sp, color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray)
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

        // Content Area: Loading / Error / Empty / LazyList
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFFB800), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading followed friends...",
                            fontSize = 13.sp,
                            color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Gray
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
                            color = Color.Red.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { reloadTrigger++ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
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
                            text = "आप अभी किसी को follow नहीं करते। Invite करने के लिए पहले लोगों को follow करें।",
                            fontSize = 14.sp,
                            color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color(0xFF374151),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                onDiscoverPeople?.invoke()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Discover", tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Discover",
                                color = Color.Black,
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
                                    countryCode = flagCode
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
                                        color = if (isInRoom) Color(0xFF3B82F6) else if (user.is_online) Color(0xFF10B981) else Color.Gray
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
                                                    android.util.Log.d("InviteFriend", "send_room_invite ERROR for user ${user.user_id}: $errBody")
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
                                    containerColor = if (isButtonDisabled) bgColors else Color(0xFFFFB800),
                                    disabledContainerColor = bgColors
                                ),
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = buttonText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isButtonDisabled) (if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray) else Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
