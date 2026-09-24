package com.example.ui.screens

import androidx.compose.foundation.background
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
    roomId: String = "FT5272",
    inviteLink: String = RoomShareHelper.buildDefaultInviteLink(roomId),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val textColor = if (isDarkMode) Color.White else Color.Black
    val bgColors = if (isDarkMode) Color(0xFF1F2937) else Color(0xFFF3F4F6)

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
            text = "Online Friends",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        val friends = listOf(
            "Ananya" to "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400",
            "Vihaan" to "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400",
            "Meera" to "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400",
            "Rahul" to "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400"
        )
        
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            friends.forEach { (name, img) ->
                var isInvited by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp)) {
                            AsyncImage(
                                model = img,
                                contentDescription = name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .size(16.dp)
                                    .background(if (isDarkMode) Color(0xFF1E293B) else Color.White, CircleShape)
                                    .padding(1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val flagCodeOptional = when (name) {
                                    "Ananya" -> "in"
                                    "Vihaan" -> "jp"
                                    "Meera" -> "us"
                                    "Rahul" -> "gb"
                                    else -> "in"
                                }
                                CircleFlag(
                                    countryCode = flagCodeOptional,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Online", fontSize = 12.sp, color = Color(0xFF10B981))
                        }
                    }
                    
                    Button(
                        onClick = { isInvited = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isInvited) bgColors else Color(0xFFFBBF24)
                        ),
                        shape = RoundedCornerShape(100.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isInvited) "Invited" else "Invite",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInvited) (if (isDarkMode) Color.White else Color.Gray) else Color(0xFF78350F)
                        )
                    }
                }
            }
        }
    }
}
