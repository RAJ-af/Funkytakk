package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ActiveRoomInviteInfo(
    val inviteId: String,
    val roomId: String,
    val inviterName: String,
    val inviterAvatarUrl: String?,
    val roomTitle: String
)

object RoomInviteOverlayManager {
    var currentInvite by mutableStateOf<ActiveRoomInviteInfo?>(null)

    fun checkAndShowPendingInvite(context: Context) {
        val uid = SupabaseSessionManager.getUserId(context) ?: return
        val token = SupabaseSessionManager.getBearerToken(context)
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = SupabaseClient.api.getPendingRoomInvites(auth = token, inviteeIdEq = "eq.$uid")
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val newest = res.body()!!.first()
                    val inviterRes = SupabaseClient.api.getProfile(auth = token, authUserIdEq = "eq.${newest.inviter_id}")
                    val inviterProfile = inviterRes.body()?.firstOrNull()
                    val roomRes = SupabaseClient.api.getVoiceRooms(auth = token)
                    val roomObj = roomRes.body()?.find { it.id == newest.room_id && it.is_live == true }
                    if (roomObj != null) {
                        val inviterName = inviterProfile?.profile_name?.ifBlank { null }
                            ?: inviterProfile?.username?.ifBlank { null }
                            ?: "A friend"
                        withContext(Dispatchers.Main) {
                            currentInvite = ActiveRoomInviteInfo(
                                inviteId = newest.id,
                                roomId = newest.room_id,
                                inviterName = inviterName,
                                inviterAvatarUrl = inviterProfile?.avatar_url,
                                roomTitle = roomObj.title ?: "Voice Room"
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun PendingRoomInviteOverlay(
    onJoinRoom: (roomId: String) -> Unit
) {
    val invite = RoomInviteOverlayManager.currentInvite ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = {
        RoomInviteOverlayManager.currentInvite = null
    }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFFFDF5),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                            .clickable {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val token = SupabaseSessionManager.getBearerToken(context)
                                        SupabaseClient.api.respondToRoomInvite(
                                            auth = token,
                                            body = mapOf("p_invite_id" to invite.inviteId, "p_accept" to false)
                                        )
                                    } catch (_: Exception) {}
                                }
                                RoomInviteOverlayManager.currentInvite = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFFFB800), CircleShape)
                ) {
                    AsyncImage(
                        model = invite.inviterAvatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                        contentDescription = invite.inviterName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${invite.inviterName} invited you!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Join \"${invite.roomTitle}\" to talk now.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val token = SupabaseSessionManager.getBearerToken(context)
                                    SupabaseClient.api.respondToRoomInvite(
                                        auth = token,
                                        body = mapOf("p_invite_id" to invite.inviteId, "p_accept" to false)
                                    )
                                } catch (_: Exception) {}
                            }
                            RoomInviteOverlayManager.currentInvite = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Dismiss", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val token = SupabaseSessionManager.getBearerToken(context)
                                    val res = SupabaseClient.api.respondToRoomInvite(
                                        auth = token,
                                        body = mapOf("p_invite_id" to invite.inviteId, "p_accept" to true)
                                    )
                                    if (res.isSuccessful) {
                                        withContext(Dispatchers.Main) {
                                            RoomInviteOverlayManager.currentInvite = null
                                            onJoinRoom(invite.roomId)
                                        }
                                    } else {
                                        val errBody = res.errorBody()?.string() ?: ""
                                        withContext(Dispatchers.Main) {
                                            RoomInviteOverlayManager.currentInvite = null
                                            if (errBody.contains("room_not_live")) {
                                                Toast.makeText(context, "Room has ended", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Could not join room", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        RoomInviteOverlayManager.currentInvite = null
                                        Toast.makeText(context, "Error joining room: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Join Room", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
