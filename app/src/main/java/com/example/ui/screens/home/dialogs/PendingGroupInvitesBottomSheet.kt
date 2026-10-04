package com.example.ui.screens.home.dialogs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.db.LocalGroupConversation
import com.example.repository.GroupChatRepository
import com.example.ui.components.*
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingGroupInvitesBottomSheet(
    onDismiss: () -> Unit,
    onOpenGroupChat: (LocalGroupConversation) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pendingInvites by GroupChatRepository.pendingInvites.collectAsState()
    var processingGroupId by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 10.dp)
                    .width(42.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = Color(0xFFE5E7EB)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFFBEB))
                            .border(1.dp, Color(0xFFFEF08A), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        GroupUsersIcon(size = 24.dp, color = Color(0xFFB45309))
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Pending Invites",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF111827)
                            )
                            if (pendingInvites.isNotEmpty()) {
                                Surface(
                                    color = Color(0xFFFFC529),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${pendingInvites.size}",
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Group invitations sent to you",
                            fontSize = 12.5.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    SoftCloseIcon(size = 14.dp, color = Color(0xFF6B7280))
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                thickness = 1.dp,
                color = Color(0xFFF3F4F6)
            )

            // Join via Invite Link / Code Bar
            var inviteLinkInput by remember { mutableStateOf("") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inviteLinkInput,
                    onValueChange = { inviteLinkInput = it },
                    placeholder = { Text("Paste funkytalk://group/<code>...", fontSize = 12.5.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val code = GroupChatRepository.extractCleanInviteCode(inviteLinkInput)
                        if (code.isNotBlank()) {
                            inviteLinkInput = ""
                            onDismiss()
                            GroupChatRepository.setPendingInviteCode(code)
                        }
                    },
                    enabled = inviteLinkInput.trim().isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("Preview", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (pendingInvites.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF9FAFB)),
                        contentAlignment = Alignment.Center
                    ) {
                        GroupE2EEShieldIcon(size = 36.dp, color = Color(0xFF9CA3AF))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Pending Invites or Requests",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When someone invites you to a group or you request to join via link, it will appear right here.",
                        fontSize = 13.sp,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pendingInvites, key = { it.id }) { group ->
                        val isBusy = processingGroupId == group.id
                        val isRequested = group.myStatus.equals("requested", ignoreCase = true)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF9FAFB),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Group Avatar
                                    if (!group.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = group.avatarUrl,
                                            contentDescription = "Group Avatar",
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(16.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF1E293B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = group.name.take(1).uppercase(),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = group.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF111827),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Surface(
                                                color = Color(0xFFFEF3C7),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "${group.memberCount} members",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = if (isRequested) "Waiting for admin approval" else "You were invited to join this group",
                                            fontSize = 12.5.sp,
                                            color = if (isRequested) Color(0xFFB45309) else Color(0xFF6B7280),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            GroupE2EEShieldIcon(size = 11.dp, color = Color(0xFF10B981))
                                            Text(
                                                text = "End-to-End Encrypted",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (isRequested) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFFFFBEB),
                                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                    ) {
                                        Text(
                                            text = "Waiting for admin approval",
                                            modifier = Modifier.padding(vertical = 11.dp, horizontal = 14.dp),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Decline Button
                                        OutlinedButton(
                                            onClick = {
                                                if (isBusy) return@OutlinedButton
                                                processingGroupId = group.id
                                                scope.launch {
                                                    val res = GroupChatRepository.declineGroupInvite(context, group.id)
                                                    processingGroupId = null
                                                    if (res.isSuccess) {
                                                        Toast.makeText(context, "Invite declined", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to decline", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFF4B5563)
                                            ),
                                            enabled = !isBusy
                                        ) {
                                            Text(
                                                text = "Decline",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            )
                                        }

                                        // Accept Button
                                        Button(
                                            onClick = {
                                                if (isBusy) return@Button
                                                processingGroupId = group.id
                                                scope.launch {
                                                    val res = GroupChatRepository.acceptGroupInvite(context, group.id)
                                                    processingGroupId = null
                                                    if (res.isSuccess) {
                                                        Toast.makeText(context, "Joined ${group.name}!", Toast.LENGTH_SHORT).show()
                                                        onOpenGroupChat(group.copy(myStatus = "joined"))
                                                    } else {
                                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to join", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .height(44.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = GoldAccent,
                                                contentColor = Color.Black
                                            ),
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                            enabled = !isBusy
                                        ) {
                                            if (isBusy) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    color = Color.Black,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    GroupCheckIcon(size = 14.dp, color = Color.Black)
                                                    Text(
                                                        text = "Accept & Join",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 13.5.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
