package com.example.ui.screens.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.db.LocalGroupConversation
import com.example.repository.GroupChatRepository
import com.example.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun GroupSettingsScreen(
    group: LocalGroupConversation,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onLeaveGroupConfirmed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var approveMembers by remember { mutableStateOf(true) }
    var joinNotifications by remember { mutableStateOf(true) }
    var mediaAutoDownload by remember { mutableStateOf(true) }
    var muteNotifications by remember { mutableStateOf(false) }

    var whoCanSend by remember { mutableStateOf("Everyone") }
    var whoCanEdit by remember { mutableStateOf("Admins only") }
    var whoCanAdd by remember { mutableStateOf("Admins only") }
    var disappearingTime by remember { mutableStateOf("Off") }

    var showLeaveConfirmDialog by remember { mutableStateOf(false) }

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

                Text(
                    text = "Group Settings",
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

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
                    Box(modifier = Modifier.size(56.dp)) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (group.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = group.avatarUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                GroupUsersIcon(size = 28.dp, color = Color(0xFFB45309))
                            }
                        }
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

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                Toast.makeText(context, "Group name: ${group.name}", Toast.LENGTH_SHORT).show()
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Group name", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(group.name, fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                            GroupChevronRightIcon(size = 14.dp, color = Color(0xFF94A3B8))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                Toast.makeText(context, "Edit description", Toast.LENGTH_SHORT).show()
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Description", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Language practice & casual conversations.", fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            GroupChevronRightIcon(size = 14.dp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }

            // 2. Permissions Card
            SettingsSectionCard(indicatorColor = Color(0xFFF59E0B), title = "Permissions") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SettingsRowItem(
                        icon = { GroupChatBubbleIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Who can send messages",
                        value = whoCanSend,
                        onClick = {
                            if (isAdmin) {
                                whoCanSend = if (whoCanSend == "Everyone") "Admins only" else "Everyone"
                            }
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    SettingsRowItem(
                        icon = { GroupEditPencilIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Who can edit group info",
                        value = whoCanEdit,
                        onClick = {
                            if (isAdmin) {
                                whoCanEdit = if (whoCanEdit == "Admins only") "Everyone" else "Admins only"
                            }
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    SettingsRowItem(
                        icon = { GroupPersonAddIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Who can add members",
                        value = whoCanAdd,
                        onClick = {
                            if (isAdmin) {
                                whoCanAdd = if (whoCanAdd == "Admins only") "Everyone" else "Admins only"
                            }
                        }
                    )
                }
            }

            // 3. New members Card
            SettingsSectionCard(indicatorColor = Color(0xFFF59E0B), title = "New members") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SettingsToggleItem(
                        icon = { GroupUsersIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Approve new members",
                        subtitle = "Admins need to approve join requests",
                        checked = approveMembers,
                        onCheckedChange = { approveMembers = it }
                    )
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    SettingsToggleItem(
                        icon = { GroupBellIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Show join notifications",
                        subtitle = "Notify when someone joins the group",
                        checked = joinNotifications,
                        onCheckedChange = { joinNotifications = it }
                    )
                }
            }

            // 4. Messages Card
            SettingsSectionCard(indicatorColor = Color(0xFFF59E0B), title = "Messages") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SettingsRowItem(
                        icon = { GroupBellIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Mute notifications",
                        value = if (muteNotifications) "Muted" else "Off",
                        onClick = { muteNotifications = !muteNotifications }
                    )
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    SettingsRowItem(
                        icon = { GroupClockIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Disappearing messages",
                        value = disappearingTime,
                        onClick = {
                            disappearingTime = when (disappearingTime) {
                                "Off" -> "24 hours"
                                "24 hours" -> "7 days"
                                else -> "Off"
                            }
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    SettingsToggleItem(
                        icon = { GroupMediaIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Media auto-download",
                        subtitle = null,
                        checked = mediaAutoDownload,
                        onCheckedChange = { mediaAutoDownload = it }
                    )
                }
            }

            // 5. Privacy & safety Card
            SettingsSectionCard(indicatorColor = Color(0xFFF59E0B), title = "Privacy & safety") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SettingsRowItem(
                        icon = { GroupUsersIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Who can see member list",
                        value = "Members only",
                        onClick = {}
                    )
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    SettingsRowItem(
                        icon = { GroupShieldIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Report group",
                        value = null,
                        onClick = { Toast.makeText(context, "Group reported", Toast.LENGTH_SHORT).show() }
                    )
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    SettingsRowItem(
                        icon = { GroupBlockIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Block group",
                        value = null,
                        onClick = { Toast.makeText(context, "Group blocked", Toast.LENGTH_SHORT).show() }
                    )
                }
            }

            // 6. Danger zone Card
            SettingsSectionCard(indicatorColor = Color(0xFFDC2626), title = "Danger zone", titleColor = Color(0xFFDC2626)) {
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
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            title = { Text("Leave Group?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to leave this end-to-end encrypted group? You won't receive future group messages.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveConfirmDialog = false
                        onLeaveGroupConfirmed()
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
            GroupChevronRightIcon(size = 14.dp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    checked: Boolean,
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
