package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NotificationsManager
import com.example.ui.components.FunkyHeartIcon
import com.example.ui.components.FunkyHomeIcon
import com.example.ui.components.FunkyLockIcon
import com.example.ui.theme.LocalFunkyColors

@Composable
fun HomeHeader(
    hasActiveEvents: Boolean = false,
    isOnline: Boolean = true,
    onOpenEvents: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    onCreateRoom: () -> Unit,
    onJoinWithCode: () -> Unit
) {
    val unreadNotifications by NotificationsManager.unreadCount.collectAsState()
    val funkyColors = LocalFunkyColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = funkyColors.ink, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)) {
                        append("Funky")
                    }
                    withStyle(style = SpanStyle(color = funkyColors.mango, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)) {
                        append("Talk")
                    }
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Talk. Learn. Connect.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = funkyColors.mute
                )
                FunkyHeartIcon(
                    color = funkyColors.mango,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Notification Bell with unread badge count
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(funkyColors.surface)
                    .border(1.dp, funkyColors.line, CircleShape)
                    .clickable { onOpenNotifications() }
                    .testTag("home_notification_bell_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = if (unreadNotifications > 0) funkyColors.ink else funkyColors.mute,
                    modifier = Modifier.size(21.dp)
                )
                if (unreadNotifications > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                            .clip(CircleShape)
                            .background(funkyColors.mango)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                            .heightIn(min = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadNotifications > 99) "99+" else "$unreadNotifications",
                            color = funkyColors.onMango,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Events / Schedule icon with badge dot
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(funkyColors.surface)
                    .border(1.dp, funkyColors.line, CircleShape)
                    .clickable { onOpenEvents() }
                    .testTag("home_events_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Event,
                    contentDescription = "Funky Events",
                    tint = funkyColors.mango,
                    modifier = Modifier.size(20.dp)
                )
                if (hasActiveEvents) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-9).dp, y = 9.dp)
                            .clip(CircleShape)
                            .background(funkyColors.mango)
                    )
                }
            }

            // Join with Code circular lock button (requires internet)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) funkyColors.surface else funkyColors.soft)
                    .border(1.dp, funkyColors.line, CircleShape)
                    .clickable(enabled = isOnline) { onJoinWithCode() }
                    .semantics { contentDescription = "Join room with code" }
                    .testTag("home_join_code_button"),
                contentAlignment = Alignment.Center
            ) {
                FunkyLockIcon(
                    color = if (isOnline) funkyColors.ink else funkyColors.mute,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Create Room circular button (requires internet)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) funkyColors.mango else funkyColors.soft)
                    .then(
                        if (!isOnline) Modifier.border(1.dp, funkyColors.line, CircleShape) else Modifier
                    )
                    .clickable(enabled = isOnline) { onCreateRoom() }
                    .testTag("home_create_room_button"),
                contentAlignment = Alignment.Center
            ) {
                FunkyHomeIcon(
                    color = if (isOnline) funkyColors.onMango else funkyColors.mute,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.0f
                )
            }
        }
    }
}
