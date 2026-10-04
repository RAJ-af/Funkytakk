package com.example.ui.screens.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.SupabaseSessionManager
import com.example.db.LocalGroupConversation
import com.example.repository.GroupChatRepository
import com.example.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun GroupSettingsScreen(
    group: LocalGroupConversation,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onLeaveGroupConfirmed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myUserId = remember { SupabaseSessionManager.getUserId(context).orEmpty() }

    val canEditInfo = isAdmin || group.whoCanEditInfo.equals("members", ignoreCase = true)
    val isCreator = group.isCreator || (group.createdBy.isNotBlank() && group.createdBy == myUserId)

    var isUploadingAvatar by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditDescriptionDialog by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showMuteDialog by remember { mutableStateOf(false) }
    var showNotifyLevelDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showBlockedGroupsSheet by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isActionInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(group.id) {
        GroupChatRepository.getGroupOverview(context, group.id)
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && canEditInfo) {
            scope.launch {
                isUploadingAvatar = true
                val (bytes, mime) = withContext(Dispatchers.IO) {
                    val b = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    val m = context.contentResolver.getType(uri) ?: "image/jpeg"
                    b to m
                }
                if (bytes != null && bytes.isNotEmpty()) {
                    val res = GroupChatRepository.uploadAndSetGroupAvatar(context, group.id, bytes, mime)
                    isUploadingAvatar = false
                    res.onSuccess {
                        Toast.makeText(context, "Group photo updated", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: "Failed to update group photo", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isUploadingAvatar = false
                }
            }
        }
    }

    val isCurrentlyMuted = remember(group.mutedUntil) {
        val until = group.mutedUntil ?: 0L
        until > System.currentTimeMillis()
    }

    val whoCanSendLabel = if (group.whoCanSend.equals("admins", ignoreCase = true)) "Admins only" else "Everyone"
    val whoCanEditLabel = if (group.whoCanEditInfo.equals("members", ignoreCase = true)) "All members" else "Admins only"
    val whoCanAddLabel = if (group.whoCanAddMembers.equals("members", ignoreCase = true)) "All members" else "Admins only"
    val disappearingLabel = GroupChatRepository.formatDisappearingLabel(group.disappearingSeconds)
    val memberVisibilityLabel = if (group.memberListVisibility.equals("admins", ignoreCase = true)) "Admins only" else "All members"
    val notifyLevelLabel = when (group.notifyLevel.lowercase()) {
        "mentions" -> "Mentions only"
        "none" -> "None"
        else -> "All messages"
    }

    BackHandler { onBack() }

    Scaffold(
        containerColor = Color(0xFFFAFAF9),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    GroupBackIcon(size = 20.dp, color = Color(0xFF0F172A))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Group Settings",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    if (!isAdmin) {
                        Text(
                            text = "Admin settings are view-only",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(42.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Group info Card
            SettingsSectionCard(indicatorColor = Color(0xFFF59E0B), title = "Group info") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .clickable(enabled = canEditInfo && !isUploadingAvatar) {
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingAvatar) {
                                CircularProgressIndicator(
                                    color = Color(0xFFB45309),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (group.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = group.avatarUrl,
                                    contentDescription = "Group Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                GroupUsersIcon(size = 28.dp, color = Color(0xFFB45309))
                            }
                        }
                        if (canEditInfo) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFC529)),
                                contentAlignment = Alignment.Center
                            ) {
                                GroupCameraIcon(size = 11.dp, color = Color.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = canEditInfo) {
                                    showEditNameDialog = true
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Group name", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(group.name, fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (canEditInfo) {
                                GroupChevronRightIcon(size = 14.dp, color = Color(0xFF94A3B8))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = canEditInfo) {
                                    showEditDescriptionDialog = true
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Description", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(
                                    text = group.description.ifBlank { "No description set" },
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (canEditInfo) {
                                GroupChevronRightIcon(size = 14.dp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }

            // 2. Permissions Card (Admin-only)
            if (isAdmin) {
                SettingsSectionCard(indicatorColor = Color(0xFFF59E0B), title = "Permissions") {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        SettingsRowItem(
                            icon = { GroupChatBubbleIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                            title = "Who can send messages",
                            value = whoCanSendLabel,
                            enabled = true,
                            onClick = {
                                val nextVal = if (group.whoCanSend.equals("admins", ignoreCase = true)) "everyone" else "admins"
                                scope.launch {
                                    val res = GroupChatRepository.updateGroupSettings(
                                        context,
                                        group.id,
                                        mapOf("who_can_send" to nextVal)
                                    )
                                    res.onFailure { err ->
                                        Toast.makeText(context, err.message ?: "Failed to update setting", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF8FAFC))
                        SettingsRowItem(
                            icon = { GroupEditPencilIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                            title = "Who can edit group info",
                            value = whoCanEditLabel,
                            enabled = true,
                            onClick = {
                                val nextVal = if (group.whoCanEditInfo.equals("members", ignoreCase = true)) "admins" else "members"
                                scope.launch {
                                    val res = GroupChatRepository.updateGroupSettings(
                                        context,
                                        group.id,
                                        mapOf("who_can_edit_info" to nextVal)
                                    )
                                    res.onFailure { err ->
                                        Toast.makeText(context, err.message ?: "Failed to update setting", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF8FAFC))
                        SettingsRowItem(
                            icon = { GroupPersonAddIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                            title = "Who can add members",
                            value = whoCanAddLabel,
                            enabled = true,
                            onClick = {
                                val nextVal = if (group.whoCanAddMembers.equals("members", ignoreCase = true)) "admins" else "members"
                                scope.launch {
                                    val res = GroupChatRepository.updateGroupSettings(
                                        context,
                                        group.id,
                                        mapOf("who_can_add_members" to nextVal)
                                    )
                                    res.onFailure { err ->
                                        Toast.makeText(context, err.message ?: "Failed to update setting", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }

                // 3. New members Card (Admin-only)
                SettingsSectionCard(indicatorColor = Color(0xFFF59E0B), title = "New members") {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        SettingsToggleItem(
                            icon = { GroupUsersIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                            title = "Approve new members",
                            subtitle = "Admins must approve anyone joining via invite link",
                            checked = group.approveNewMembers,
                            enabled = true,
                            onCheckedChange = { newValue ->
                                scope.launch {
                                    val res = GroupChatRepository.updateGroupSettings(
                                        context,
                                        group.id,
                                        mapOf("approve_new_members" to newValue)
                                    )
                                    res.onFailure { err ->
                                        Toast.makeText(context, err.message ?: "Failed to update setting", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF8FAFC))
                        SettingsToggleItem(
                            icon = { GroupBellIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                            title = "Show join notifications",
                            subtitle = "Show system messages when members join or leave",
                            checked = group.showJoinNotifications,
                            enabled = true,
                            onCheckedChange = { newValue ->
                                scope.launch {
                                    val res = GroupChatRepository.updateGroupSettings(
                                        context,
                                        group.id,
                                        mapOf("show_join_notifications" to newValue)
                                    )
                                    res.onFailure { err ->
                                        Toast.makeText(context, err.message ?: "Failed to update setting", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 4. Messages & Personal Notifications Card
            SettingsSectionCard(indicatorColor = Color(0xFFF59E0B), title = "Messages & notifications") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SettingsRowItem(
                        icon = { GroupBellIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Mute notifications",
                        value = if (isCurrentlyMuted) "Muted" else "Off",
                        enabled = true,
                        onClick = { showMuteDialog = true }
                    )
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    SettingsRowItem(
                        icon = { GroupBellIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Notification level",
                        value = notifyLevelLabel,
                        enabled = true,
                        onClick = { showNotifyLevelDialog = true }
                    )
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    SettingsRowItem(
                        icon = { GroupClockIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Disappearing messages",
                        value = disappearingLabel,
                        enabled = isAdmin,
                        onClick = {
                            if (isAdmin) {
                                showDisappearingDialog = true
                            }
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    SettingsToggleItem(
                        icon = { GroupMediaIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Media auto-download",
                        subtitle = "Automatically decrypt & save photos/voice notes",
                        checked = group.mediaAutoDownload,
                        enabled = true,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                val res = GroupChatRepository.updateMyGroupPrefs(
                                    context = context,
                                    groupId = group.id,
                                    mediaAutoDownload = enabled
                                )
                                res.onFailure { err ->
                                    Toast.makeText(context, err.message ?: "Failed to update preference", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            // 5. Privacy & safety Card
            SettingsSectionCard(indicatorColor = Color(0xFFF59E0B), title = "Privacy & safety") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    if (isAdmin) {
                        SettingsRowItem(
                            icon = { GroupUsersIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                            title = "Who can see member list",
                            value = memberVisibilityLabel,
                            enabled = true,
                            onClick = {
                                val nextVal = if (group.memberListVisibility.equals("admins", ignoreCase = true)) "members" else "admins"
                                scope.launch {
                                    val res = GroupChatRepository.updateGroupSettings(
                                        context,
                                        group.id,
                                        mapOf("member_list_visibility" to nextVal)
                                    )
                                    res.onFailure { err ->
                                        Toast.makeText(context, err.message ?: "Failed to update visibility", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF8FAFC))
                    }
                    SettingsRowItem(
                        icon = { GroupShieldIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Report group",
                        value = null,
                        enabled = true,
                        onClick = { showReportDialog = true }
                    )
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    SettingsRowItem(
                        icon = { GroupBlockIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Block group",
                        value = null,
                        enabled = true,
                        onClick = { showBlockConfirmDialog = true }
                    )
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    SettingsRowItem(
                        icon = { GroupBlockBanIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Blocked groups",
                        value = null,
                        enabled = true,
                        onClick = { showBlockedGroupsSheet = true }
                    )
                }
            }

            // 6. Danger zone Card
            SettingsSectionCard(indicatorColor = Color(0xFFDC2626), title = "Danger zone", titleColor = Color(0xFFDC2626)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLeaveConfirmDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GroupLeaveDoorIcon(size = 18.dp, color = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Leave group",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                        GroupChevronRightIcon(size = 14.dp, color = Color(0xFFDC2626))
                    }

                    if (isCreator) {
                        HorizontalDivider(color = Color(0xFFFEE2E2))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeleteConfirmDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GroupTrashIcon(size = 18.dp, color = Color(0xFFB91C1C))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Delete group",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB91C1C)
                                    )
                                    Text(
                                        text = "Creator only · permanently deletes group for all members",
                                        fontSize = 11.sp,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                            GroupChevronRightIcon(size = 14.dp, color = Color(0xFFB91C1C))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showBlockedGroupsSheet) {
        com.example.ui.screens.home.dialogs.BlockedGroupsBottomSheet(
            onDismiss = { showBlockedGroupsSheet = false }
        )
    }

    // Edit Group Name Dialog
    if (showEditNameDialog) {
        var draftName by remember(group.name) { mutableStateOf(group.name) }
        AlertDialog(
            onDismissRequest = { if (!isActionInProgress) showEditNameDialog = false },
            title = { Text("Edit Group Name", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { if (it.length <= 60) draftName = it },
                        label = { Text("Group name (1–60 chars)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${draftName.trim().length}/60",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isActionInProgress && draftName.trim().isNotEmpty(),
                    onClick = {
                        val newName = draftName.trim()
                        if (newName == group.name) {
                            showEditNameDialog = false
                            return@TextButton
                        }
                        scope.launch {
                            isActionInProgress = true
                            val res = GroupChatRepository.updateGroupSettings(
                                context,
                                group.id,
                                mapOf("name" to newName)
                            )
                            isActionInProgress = false
                            res.onSuccess {
                                showEditNameDialog = false
                                Toast.makeText(context, "Group name updated", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to update group name", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Edit Group Description Dialog
    if (showEditDescriptionDialog) {
        var draftDesc by remember(group.description) { mutableStateOf(group.description) }
        AlertDialog(
            onDismissRequest = { if (!isActionInProgress) showEditDescriptionDialog = false },
            title = { Text("Edit Group Description", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = draftDesc,
                    onValueChange = { if (it.length <= 300) draftDesc = it },
                    label = { Text("Description") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isActionInProgress,
                    onClick = {
                        val newDesc = draftDesc.trim()
                        if (newDesc == group.description) {
                            showEditDescriptionDialog = false
                            return@TextButton
                        }
                        scope.launch {
                            isActionInProgress = true
                            val res = GroupChatRepository.updateGroupSettings(
                                context,
                                group.id,
                                mapOf("description" to newDesc)
                            )
                            isActionInProgress = false
                            res.onSuccess {
                                showEditDescriptionDialog = false
                                Toast.makeText(context, "Description updated", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to update description", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDescriptionDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Disappearing Messages Dialog (0 | 86400 | 604800 | 7776000)
    if (showDisappearingDialog) {
        val options = listOf(
            0 to "Off",
            86400 to "24 hours",
            604800 to "7 days",
            7776000 to "90 days"
        )
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text("Disappearing Messages", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "New messages in this group will disappear from this device after the selected duration.",
                        fontSize = 12.5.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    options.forEach { (seconds, label) ->
                        val selected = group.disappearingSeconds == seconds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color(0xFFFEF3C7) else Color.Transparent)
                                .clickable {
                                    showDisappearingDialog = false
                                    if (seconds != group.disappearingSeconds) {
                                        scope.launch {
                                            val res = GroupChatRepository.updateGroupSettings(
                                                context,
                                                group.id,
                                                mapOf("disappearing_seconds" to seconds)
                                            )
                                            res.onFailure { err ->
                                                Toast.makeText(context, err.message ?: "Failed to update setting", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = Color(0xFF0F172A))
                            RadioButton(selected = selected, onClick = null)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisappearingDialog = false }) {
                    Text("Close", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Mute Notifications Dialog
    if (showMuteDialog) {
        AlertDialog(
            onDismissRequest = { showMuteDialog = false },
            title = { Text("Mute Notifications", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val muteOptions = listOf(
                        "unmute" to "Unmute (Receive notifications)",
                        "8h" to "Mute for 8 hours",
                        "1w" to "Mute for 1 week",
                        "always" to "Mute always"
                    )
                    muteOptions.forEach { (key, label) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showMuteDialog = false
                                    scope.launch {
                                        val now = System.currentTimeMillis()
                                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                                            timeZone = TimeZone.getTimeZone("UTC")
                                        }
                                        val res = when (key) {
                                            "unmute" -> GroupChatRepository.updateMyGroupPrefs(
                                                context = context,
                                                groupId = group.id,
                                                clearMute = true
                                            )
                                            "8h" -> GroupChatRepository.updateMyGroupPrefs(
                                                context = context,
                                                groupId = group.id,
                                                mutedUntilIso = sdf.format(Date(now + 8 * 3600 * 1000L)),
                                                clearMute = false
                                            )
                                            "1w" -> GroupChatRepository.updateMyGroupPrefs(
                                                context = context,
                                                groupId = group.id,
                                                mutedUntilIso = sdf.format(Date(now + 7 * 24 * 3600 * 1000L)),
                                                clearMute = false
                                            )
                                            else -> GroupChatRepository.updateMyGroupPrefs(
                                                context = context,
                                                groupId = group.id,
                                                mutedUntilIso = "2099-12-31T23:59:59Z",
                                                clearMute = false
                                            )
                                        }
                                        res.onSuccess {
                                            Toast.makeText(
                                                context,
                                                if (key == "unmute") "Notifications unmuted" else "Group muted",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.message ?: "Failed to update mute setting", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMuteDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Notification Level Dialog ('all' | 'mentions' | 'none')
    if (showNotifyLevelDialog) {
        val levels = listOf(
            "all" to "All messages",
            "mentions" to "Mentions only",
            "none" to "None"
        )
        AlertDialog(
            onDismissRequest = { showNotifyLevelDialog = false },
            title = { Text("Notification Level", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    levels.forEach { (lvl, label) ->
                        val selected = group.notifyLevel.equals(lvl, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color(0xFFFEF3C7) else Color.Transparent)
                                .clickable {
                                    showNotifyLevelDialog = false
                                    scope.launch {
                                        val res = GroupChatRepository.updateMyGroupPrefs(
                                            context = context,
                                            groupId = group.id,
                                            notifyLevel = lvl
                                        )
                                        res.onFailure { err ->
                                            Toast.makeText(context, err.message ?: "Failed to update notification level", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = Color(0xFF0F172A))
                            RadioButton(selected = selected, onClick = null)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifyLevelDialog = false }) {
                    Text("Close", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Report Group Dialog (context_type = 'group' via report_group RPC)
    if (showReportDialog) {
        val categories = listOf("spam", "harassment", "inappropriate", "scam", "other")
        var selectedCategory by remember { mutableStateOf("spam") }
        var reportReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { if (!isActionInProgress) showReportDialog = false },
            title = { Text("Report Group", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a reason for reporting \"${group.name}\":", fontSize = 13.sp, color = Color(0xFF64748B))
                    categories.forEach { cat ->
                        val selected = selectedCategory == cat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Color(0xFFFEF3C7) else Color(0xFFF8FAFC))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = cat.replaceFirstChar { it.uppercase() },
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = Color(0xFF0F172A)
                            )
                            RadioButton(selected = selected, onClick = null)
                        }
                    }
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text("Additional details (optional)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isActionInProgress,
                    onClick = {
                        scope.launch {
                            isActionInProgress = true
                            val res = GroupChatRepository.reportGroup(
                                context = context,
                                groupId = group.id,
                                category = selectedCategory,
                                reason = reportReason.ifBlank { "Reported for $selectedCategory" }
                            )
                            isActionInProgress = false
                            res.onSuccess {
                                showReportDialog = false
                                Toast.makeText(context, "Report submitted to moderators", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to submit report", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Submit Report", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Block Group Confirmation Dialog (leaves and prevents re-invite)
    if (showBlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isActionInProgress) showBlockConfirmDialog = false },
            title = { Text("Block & Leave Group?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Blocking \"${group.name}\" will immediately remove you from the group and prevent anyone from inviting you back in the future.")
            },
            confirmButton = {
                TextButton(
                    enabled = !isActionInProgress,
                    onClick = {
                        scope.launch {
                            isActionInProgress = true
                            val res = GroupChatRepository.blockGroup(context, group.id)
                            isActionInProgress = false
                            res.onSuccess {
                                showBlockConfirmDialog = false
                                Toast.makeText(context, "Group blocked and left", Toast.LENGTH_SHORT).show()
                                onLeaveGroupConfirmed()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to block group", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Block Group", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Leave Group Confirmation Dialog
    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isActionInProgress) showLeaveConfirmDialog = false },
            title = { Text("Leave Group?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to leave \"${group.name}\"? You won't receive future messages from this group.") },
            confirmButton = {
                TextButton(
                    enabled = !isActionInProgress,
                    onClick = {
                        scope.launch {
                            isActionInProgress = true
                            val res = GroupChatRepository.leaveGroup(context, group.id)
                            isActionInProgress = false
                            res.onSuccess {
                                showLeaveConfirmDialog = false
                                Toast.makeText(context, "You left the group", Toast.LENGTH_SHORT).show()
                                onLeaveGroupConfirmed()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to leave group", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Leave", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Group Confirmation Dialog (Creator only)
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isActionInProgress) showDeleteConfirmDialog = false },
            title = { Text("Delete Group Permanently?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete \"${group.name}\" for all members. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    enabled = !isActionInProgress,
                    onClick = {
                        scope.launch {
                            isActionInProgress = true
                            val res = GroupChatRepository.deleteGroup(context, group.id)
                            isActionInProgress = false
                            res.onSuccess {
                                showDeleteConfirmDialog = false
                                Toast.makeText(context, "Group deleted", Toast.LENGTH_SHORT).show()
                                onLeaveGroupConfirmed()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to delete group", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SettingsSectionCard(
    indicatorColor: Color,
    title: String,
    titleColor: Color = Color(0xFF0F172A),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(13.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(indicatorColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: @Composable () -> Unit,
    title: String,
    value: String?,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F172A)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    fontSize = 12.5.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (enabled) {
                GroupChevronRightIcon(size = 14.dp, color = Color(0xFF94A3B8))
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A)
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFFC529),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE2E8F0)
            )
        )
    }
}
