package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import com.example.ui.components.CircleFlag
import com.example.util.RoomShareHelper

@Composable
fun InviteFriendBottomSheetContent(
    isDarkMode: Boolean,
    roomName: String = "FunkyTalk Room",
    roomId: String = "",
    inviteLink: String = RoomShareHelper.buildDefaultInviteLink(roomId),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textColor = if (isDarkMode) Color.White else Color.Black
    val bgColors = if (isDarkMode) Color(0xFF1F2937) else Color(0xFFF3F4F6)

    var isLoading by remember { mutableStateOf(true) }
    var invitableUsers by remember { mutableStateOf<List<com.example.InvitableUserResponse>>(emptyList()) }
    val invitedUserIds = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(roomId) {
        if (roomId.isNotBlank()) {
            isLoading = true
            try {
                val token = com.example.SupabaseSessionManager.getBearerToken(context)
                val res = com.example.SupabaseClient.api.listInvitableFollowing(
                    auth = token,
                    body = mapOf("p_room_id" to roomId)
                )
                if (res.isSuccessful && res.body() != null) {
                    invitableUsers = res.body() ?: emptyList()
                } else {
                    android.util.Log.d("InviteFriendSheet", "Error listing invitable following: ${res.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.d("InviteFriendSheet", "Exception listing invitable following: ${e.message}")
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
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 24.dp)
    ) {
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

        Spacer(modifier = Modifier.height(20.dp))

        // Share Platforms
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
                                else -> {
                                    onDismiss()
                                }
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = name, fontSize = 12.sp, color = if (isDarkMode) Color.White.copy(alpha=0.7f) else Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Friends You Follow",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color(0xFFFFB800),
                    modifier = Modifier.size(32.dp)
                )
            }
        } else if (invitableUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Follow people to invite them to your room",
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Gray
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                invitableUsers.forEach { user ->
                    val isPending = user.invite_status == "pending" || invitedUserIds[user.user_id] == true
                    val isInRoom = user.in_room
                    val username = user.username?.ifBlank { "User" } ?: "User"
                    val avatarUrl = user.avatar_url?.ifBlank { null }
                        ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"
                    val flagCode = user.country_code?.ifBlank { "in" } ?: "in"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp)) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = username,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                                if (user.is_online) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(12.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                            .border(1.5.dp, Color.White, CircleShape)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 2.dp, y = 2.dp)
                                        .size(16.dp)
                                        .background(if (isDarkMode) Color(0xFF1E293B) else Color.White, CircleShape)
                                        .padding(1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircleFlag(
                                        countryCode = flagCode,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = username, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isInRoom) "In room" else if (user.is_online) "Online" else "Offline",
                                    fontSize = 12.sp,
                                    color = if (isInRoom) Color(0xFF3B82F6) else if (user.is_online) Color(0xFF10B981) else Color.Gray
                                )
                            }
                        }

                        val buttonText = when {
                            isInRoom -> "In room"
                            isPending -> "Invited"
                            else -> "Invite"
                        }
                        val isButtonDisabled = isInRoom || isPending

                        Button(
                            onClick = {
                                if (!isButtonDisabled) {
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            val token = com.example.SupabaseSessionManager.getBearerToken(context)
                                            val res = com.example.SupabaseClient.api.sendRoomInvite(
                                                auth = token,
                                                body = mapOf("p_room_id" to roomId, "p_user_id" to user.user_id)
                                            )
                                            if (res.isSuccessful) {
                                                invitedUserIds[user.user_id] = true
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    android.widget.Toast.makeText(context, "Invite sent to $username!", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                val errBody = res.errorBody()?.string() ?: ""
                                                val msg = when {
                                                    errBody.contains("already_invited") -> "Already invited, try again in a minute"
                                                    errBody.contains("rate_limited") -> "Too many invites, try later"
                                                    errBody.contains("already_in_room") -> "Already in the room"
                                                    errBody.contains("room_not_live") -> "This room has ended"
                                                    else -> "Could not send invite"
                                                }
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                android.widget.Toast.makeText(context, "Error sending invite: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isButtonDisabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isButtonDisabled) bgColors else Color(0xFFFBBF24),
                                disabledContainerColor = bgColors
                            ),
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = buttonText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isButtonDisabled) (if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray) else Color(0xFF78350F)
                            )
                        }
                    }
                }
            }
        }
    }
}
