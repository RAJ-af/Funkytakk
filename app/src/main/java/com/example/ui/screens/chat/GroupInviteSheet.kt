package com.example.ui.screens.chat

import android.content.Intent
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.SupabaseGroupMember
import com.example.db.LocalGroupConversation
import com.example.repository.GroupChatRepository
import com.example.repository.GroupInvitePreview
import com.example.ui.components.*
import kotlinx.coroutines.launch

/**
 * 1. Bottom Sheet for Group Info / Chat Menu:
 * Calls get_group_invite(p_group_id), reset_group_invite(p_group_id), set_group_invite_enabled(p_group_id, bool).
 * Builds funkytalk://group/<code> (+ https App Link) and opens Android share sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInviteBottomSheet(
    group: LocalGroupConversation,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isAdmin = group.myRole.equals("admin", ignoreCase = true)

    var isLoading by remember { mutableStateOf(true) }
    var isMutating by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }
    var inviteEnabled by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val deepLink = remember(inviteCode) {
        if (inviteCode.isNotBlank()) "funkytalk://group/$inviteCode" else ""
    }
    val httpsLink = remember(inviteCode) {
        if (inviteCode.isNotBlank()) "https://funkytalk.app/group/$inviteCode" else ""
    }

    fun loadInvite() {
        scope.launch {
            isLoading = true
            errorMessage = null
            val res = GroupChatRepository.getGroupInvite(context, group.id)
            isLoading = false
            res.onSuccess { info ->
                inviteCode = info.code
                inviteEnabled = info.enabled
            }.onFailure { err ->
                val msg = err.message.orEmpty()
                if (msg.contains("disabled", ignoreCase = true)) {
                    inviteEnabled = false
                    inviteCode = ""
                } else {
                    errorMessage = msg
                }
            }
        }
    }

    LaunchedEffect(group.id) {
        loadInvite()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7))
                    .border(1.5.dp, Color(0xFFFDE68A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                GroupLinkIcon(size = 26.dp, color = Color(0xFFB45309))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Group Invite Link",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            val subtitleText = if (group.approveNewMembers) {
                "Anyone with this link can request to join \"${group.name}\" (admin approval required)."
            } else {
                "Anyone on FunkyTalk with this link can join \"${group.name}\"."
            }
            Text(
                text = subtitleText,
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (isAdmin) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable invite link",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (inviteEnabled) "Link is active" else "Link is disabled",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = inviteEnabled,
                            enabled = !isMutating && !isLoading,
                            onCheckedChange = { newEnabled ->
                                scope.launch {
                                    isMutating = true
                                    val res = GroupChatRepository.setGroupInviteEnabled(context, group.id, newEnabled)
                                    isMutating = false
                                    res.onSuccess {
                                        inviteEnabled = newEnabled
                                        if (newEnabled) {
                                            loadInvite()
                                        }
                                        Toast.makeText(
                                            context,
                                            if (newEnabled) "Invite link enabled" else "Invite link disabled",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, err.message ?: "Failed to update link", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFFC529),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFC529), modifier = Modifier.size(30.dp))
                }
            } else if (!inviteEnabled) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Text(
                        text = "Invite links are currently disabled for this group.",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFB91C1C),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (errorMessage != null && inviteCode.isBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage ?: "Unable to load invite link",
                            fontSize = 13.sp,
                            color = Color(0xFFB91C1C),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { loadInvite() }) {
                            Text("Retry", fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = deepLink,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = httpsLink,
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(deepLink))
                                    Toast.makeText(context, "Invite link copied!", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFC529)
                        ) {
                            Text(
                                text = "Copy",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val shareBody = buildString {
                            append("Join \"${group.name}\" on FunkyTalk!\n")
                            append(deepLink)
                            if (httpsLink.isNotBlank()) {
                                append("\n")
                                append(httpsLink)
                            }
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Join ${group.name} on FunkyTalk")
                            putExtra(Intent.EXTRA_TEXT, shareBody)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Group Invite Link"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC529),
                        contentColor = Color.Black
                    )
                ) {
                    GroupShareIcon(size = 18.dp, color = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Invite Link",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isAdmin) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isMutating = true
                                val res = GroupChatRepository.resetGroupInvite(context, group.id)
                                isMutating = false
                                res.onSuccess { info ->
                                    inviteCode = info.code
                                    inviteEnabled = info.enabled
                                    Toast.makeText(context, "Invite link reset!", Toast.LENGTH_SHORT).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, err.message ?: "Failed to reset invite link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isMutating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569))
                    ) {
                        Text(
                            text = if (isMutating) "Resetting..." else "Reset Invite Link",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 2. Bottom Sheet for Accepting / Declining a direct Group Invite
 * Calls accept_group_invite(p_group_id) or decline_group_invite(p_group_id).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInviteSheet(
    groupId: String,
    groupName: String,
    groupAvatar: String = "",
    invitedBy: String? = null,
    onDismiss: () -> Unit,
    onAccepted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isProcessing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7))
                    .border(2.dp, Color(0xFFFFC529), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (groupAvatar.isNotBlank()) {
                    AsyncImage(
                        model = groupAvatar,
                        contentDescription = groupName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    GroupUsersIcon(size = 30.dp, color = Color(0xFFB45309))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = groupName,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = invitedBy?.takeIf { it.isNotBlank() }
                    ?: "You have been invited to join this end-to-end encrypted group chat.",
                fontSize = 13.5.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            val res = GroupChatRepository.declineGroupInvite(context, groupId)
                            isProcessing = false
                            res.onSuccess {
                                Toast.makeText(context, "Invitation declined", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to decline invite", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text("Decline", color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            val res = GroupChatRepository.acceptGroupInvite(context, groupId)
                            isProcessing = false
                            res.onSuccess {
                                Toast.makeText(context, "Joined $groupName!", Toast.LENGTH_SHORT).show()
                                onAccepted()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to accept invite", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC529),
                        contentColor = Color.Black
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Accept & Join", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * 3. Deep Link Invite Code Preview & Join Sheet (funkytalk://group/<code>)
 * Calls preview_group_by_invite(p_code) -> shows group card -> join_group_by_invite(p_code)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInviteCodePreviewSheet(
    inviteCode: String,
    onDismiss: () -> Unit,
    onJoinedGroup: (groupId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isLoading by remember { mutableStateOf(true) }
    var isJoining by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<GroupInvitePreview?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var joinStatusResult by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(inviteCode) {
        isLoading = true
        errorText = null
        val res = GroupChatRepository.previewGroupByInvite(context, inviteCode)
        isLoading = false
        res.onSuccess { p ->
            preview = p
            joinStatusResult = p.myStatus
        }.onFailure { err ->
            errorText = err.message ?: "Invalid or expired invite link"
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isJoining) onDismiss() },
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFC529))
                }
            } else if (errorText != null || preview == null) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    GroupLinkIcon(size = 26.dp, color = Color(0xFFEF4444))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Unable to Open Invite",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorText ?: "This invite link is invalid or disabled.",
                    fontSize = 13.5.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A), contentColor = Color.White)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            } else {
                val p = preview!!
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7))
                        .border(2.dp, Color(0xFFFFC529), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (p.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = p.avatarUrl,
                            contentDescription = p.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        GroupUsersIcon(size = 32.dp, color = Color(0xFFB45309))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = p.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (p.joinedCount == 1) "1 member" else "${p.joinedCount} members",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFB45309)
                )

                if (p.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = p.description,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (joinStatusResult?.equals("requested", ignoreCase = true) == true) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Text(
                            text = "Request sent, waiting for admin",
                            modifier = Modifier.padding(14.dp),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A), contentColor = Color.White)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (joinStatusResult?.equals("joined", ignoreCase = true) == true && p.groupId.isNotBlank()) {
                                onJoinedGroup(p.groupId)
                                return@Button
                            }
                            scope.launch {
                                isJoining = true
                                val res = GroupChatRepository.joinGroupByInvite(context, p.code)
                                isJoining = false
                                res.onSuccess { outcome ->
                                    if (outcome.status.equals("requested", ignoreCase = true)) {
                                        joinStatusResult = "requested"
                                        Toast.makeText(context, "Request sent, waiting for admin", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Joined ${p.name}!", Toast.LENGTH_SHORT).show()
                                        onJoinedGroup(outcome.groupId.ifBlank { p.groupId })
                                    }
                                }.onFailure { err ->
                                    Toast.makeText(context, err.message ?: "Failed to join group", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isJoining,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC529),
                            contentColor = Color.Black
                        )
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            val btnLabel = when {
                                joinStatusResult?.equals("joined", ignoreCase = true) == true -> "Open Group Chat"
                                p.approveNewMembers -> "Request to Join"
                                else -> "Join Group"
                            }
                            Text(btnLabel, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * 4. Admin Join Requests Sheet (for group_join_request notifications)
 * Calls get_group_members(p_group_id), filters status == 'requested', and lets admin approve/reject.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupJoinRequestsSheet(
    groupId: String,
    groupName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isLoading by remember { mutableStateOf(true) }
    var requests by remember { mutableStateOf<List<SupabaseGroupMember>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadRequests() {
        scope.launch {
            isLoading = true
            errorMessage = null
            val res = GroupChatRepository.getGroupMembersResult(context, groupId)
            isLoading = false
            res.onSuccess { list ->
                requests = list.filter { it.status.equals("requested", ignoreCase = true) }
            }.onFailure { err ->
                errorMessage = err.message
            }
        }
    }

    LaunchedEffect(groupId) {
        loadRequests()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Join Requests · $groupName",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Approve or reject users waiting to join this group.",
                fontSize = 12.5.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFC529))
                }
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Failed to load requests",
                    fontSize = 13.sp,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else if (requests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No pending join requests for this group.",
                        fontSize = 13.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(requests, key = { it.user_id }) { req ->
                        val displayName = req.profile_name?.takeIf { it.isNotBlank() }
                            ?: req.username?.takeIf { it.isNotBlank() }
                            ?: "User (${req.user_id.take(6)})"

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (!req.username.isNullOrBlank()) {
                                        Text(
                                            text = "@${req.username}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                scope.launch {
                                                    val res = GroupChatRepository.approveJoinRequest(
                                                        context,
                                                        groupId,
                                                        req.user_id
                                                    )
                                                    res.onSuccess {
                                                        Toast.makeText(context, "Approved $displayName", Toast.LENGTH_SHORT).show()
                                                        loadRequests()
                                                    }.onFailure { err ->
                                                        Toast.makeText(context, err.message ?: "Failed to approve", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                        color = Color(0xFFFFC529),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "Approve",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                scope.launch {
                                                    val res = GroupChatRepository.rejectJoinRequest(
                                                        context,
                                                        groupId,
                                                        req.user_id
                                                    )
                                                    res.onSuccess {
                                                        Toast.makeText(context, "Rejected request", Toast.LENGTH_SHORT).show()
                                                        loadRequests()
                                                    }.onFailure { err ->
                                                        Toast.makeText(context, err.message ?: "Failed to reject", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                        color = Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "Reject",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
