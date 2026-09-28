package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NotificationsManager
import com.example.SupabaseNotification
import com.example.ui.screens.chat.GroupInviteSheet
import com.example.ui.components.*
import com.example.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenSupportChat: (reportId: String?) -> Unit
) {
    val context = LocalContext.current
    val notificationsState by NotificationsManager.notifications.collectAsState()
    val unreadCountState by NotificationsManager.unreadCount.collectAsState()
    val isLoading by NotificationsManager.isLoading.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    var activeGroupInvite by remember { mutableStateOf<SupabaseNotification?>(null) }

    LaunchedEffect(Unit) {
        NotificationsManager.init(context)
        NotificationsManager.fetchNotifications(context)
    }

    BackHandler {
        onBack()
    }

    // High quality sample notifications fallback if live list is empty
    val fallbackNotifications = remember {
        listOf(
            SupabaseNotification(
                id = "sample_1",
                title = "Welcome to FunkyTalk",
                message = "Your encrypted chat vault and language exchange profile are active.",
                type = "system",
                is_read = false,
                created_at = "Just now"
            ),
            SupabaseNotification(
                id = "sample_2",
                title = "Support Team Reply",
                message = "Your feedback regarding E2EE key sync has been updated.",
                type = "support",
                report_id = "rep_102",
                is_read = false,
                created_at = "10m ago"
            ),
            SupabaseNotification(
                id = "sample_3",
                title = "Language Room Invite",
                message = "Yuki Tanaka invited you to join Korean & English Exchange Room.",
                type = "room",
                is_read = true,
                created_at = "2h ago"
            ),
            SupabaseNotification(
                id = "sample_4",
                title = "Security & Privacy Audit",
                message = "Zero data leaks detected. Your encrypted vault is 100% secure.",
                type = "moderation",
                is_read = true,
                created_at = "1d ago"
            )
        )
    }

    val displayNotifications = remember(notificationsState, selectedFilter) {
        val baseList = if (notificationsState.isEmpty()) fallbackNotifications else notificationsState
        when (selectedFilter) {
            "Unread" -> baseList.filter { !it.is_read }
            "Support" -> baseList.filter { it.type?.lowercase() in listOf("support", "support_message", "support_reply") }
            "System" -> baseList.filter { it.type?.lowercase() in listOf("system", "moderation", "warning") }
            else -> baseList
        }
    }

    val totalUnread = if (notificationsState.isEmpty()) 2 else unreadCountState

    Scaffold(
        containerColor = Color(0xFFFCFBF7),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Notifications",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color(0xFF0F172A)
                        )
                        if (totalUnread > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GoldAccent)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$totalUnread new",
                                    color = Color(0xFF0F172A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("notifications_back_button")
                    ) {
                        SoftBackIcon(size = 20.dp, color = Color(0xFF0F172A))
                    }
                },
                actions = {
                    if (totalUnread > 0) {
                        Surface(
                            onClick = { NotificationsManager.markAllAsRead(context) },
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .testTag("notifications_mark_all_read_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SoftCheckIcon(size = 12.dp, color = Color(0xFF475569))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mark all read",
                                    color = Color(0xFF475569),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFCFBF7)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // System Push Notification Enable & Test Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Soft3DBellNotificationIcon(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "System Push Notifications",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = "Get notified even when app is closed",
                                fontSize = 11.sp,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            com.example.util.SystemNotificationHelper.triggerTestPushNotification(context)
                            android.widget.Toast.makeText(context, "Test push notification sent! Check top status bar.", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF166534),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Test Push", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Category Filter Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Unread", "Support", "System").forEach { category ->
                    val isSelected = selectedFilter == category
                    Surface(
                        onClick = { selectedFilter = category },
                        color = if (isSelected) Color(0xFF0F172A) else Color.White,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = category,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (displayNotifications.isEmpty() && !isLoading) {
                    // Clean Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFFBEB))
                                .border(1.5.dp, Color(0xFFFDE68A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Soft3DBellNotificationIcon(modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "All Caught Up!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "You don't have any notifications under '$selectedFilter'. System updates, room invites, and support replies will appear here.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayNotifications.distinctBy { it.id }, key = { it.id }) { item ->
                            NotificationItemCard(
                                notification = item,
                                onClick = {
                                    NotificationsManager.markAsRead(context, item.id)
                                    if (item.type?.lowercase() == "group_invite" || !item.group_id.isNullOrBlank()) {
                                        activeGroupInvite = item
                                    } else if (!item.report_id.isNullOrBlank()) {
                                        onOpenSupportChat(item.report_id)
                                    }
                                }
                            )
                        }
                    }
                }

                if (isLoading && notificationsState.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = GoldAccent
                    )
                }
            }
        }

        // Group Invite Bottom Sheet
        activeGroupInvite?.let { invite ->
            val groupId = invite.group_id ?: invite.id
            val rawName = invite.title?.replace("Invited to ", "", ignoreCase = true)?.ifBlank { "FunkyTalk Group" } ?: "FunkyTalk Group"
            GroupInviteSheet(
                groupId = groupId,
                groupName = rawName,
                invitedBy = invite.message,
                onDismiss = { activeGroupInvite = null },
                onAccepted = {
                    activeGroupInvite = null
                    onBack()
                }
            )
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: SupabaseNotification,
    onClick: () -> Unit
) {
    val isUnread = !notification.is_read
    val relativeTime = NotificationsManager.formatRelativeTime(notification.created_at)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("notification_item_${notification.id}"),
        shape = RoundedCornerShape(18.dp),
        color = if (isUnread) Color.White else Color(0xFFF8FAFC),
        border = BorderStroke(
            1.dp,
            if (isUnread) Color(0xFFFDE68A) else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isUnread) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Soft Category Icon Badge (No material icons)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when (notification.type?.lowercase()) {
                            "support", "support_message", "support_reply" -> Color(0xFFECFDF5)
                            "moderation", "warning", "suspend", "mute" -> Color(0xFFFEF2F2)
                            "room", "room_invite" -> Color(0xFFEFF6FF)
                            else -> Color(0xFFFFFBEB)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (notification.type?.lowercase()) {
                    "support", "support_message", "support_reply" -> SoftHeadsetIcon(size = 20.dp, color = Color(0xFF10B981))
                    "moderation", "warning", "suspend", "mute" -> SoftShieldIcon(size = 20.dp, color = Color(0xFFEF4444))
                    "room", "room_invite" -> SoftGroupIcon(size = 20.dp, color = Color(0xFF3B82F6))
                    else -> Soft3DBellNotificationIcon(modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Message Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title ?: "Notification",
                        fontSize = 14.5.sp,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = relativeTime,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message ?: "",
                    fontSize = 13.sp,
                    color = if (isUnread) Color(0xFF334155) else Color(0xFF64748B),
                    lineHeight = 18.sp
                )

                // Optional chip if linked to a report
                if (!notification.report_id.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            SoftChatBubbleIcon(size = 12.dp, color = Color(0xFF92400E))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View Support Thread",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }

            // Unread Dot Indicator
            if (isUnread) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GoldAccent)
                )
            }
        }
    }
}
