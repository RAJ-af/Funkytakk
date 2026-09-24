package com.example.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NotificationsManager
import com.example.ui.components.FunkyHomeIcon
import com.example.ui.screens.home.models.GraySubtitle
import com.example.ui.theme.GoldAccent

@Composable
fun HomeHeader(
    hasActiveEvents: Boolean = false,
    onOpenEvents: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    onCreateRoom: () -> Unit,
    onJoinWithCode: () -> Unit
) {
    val unreadNotifications by NotificationsManager.unreadCount.collectAsState()

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
                    withStyle(style = SpanStyle(color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)) {
                        append("Funky")
                    }
                    withStyle(style = SpanStyle(color = GoldAccent, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)) {
                        append("Talk")
                    }
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Talk. Learn. Connect. 💛",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = GraySubtitle
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Notification Bell with unread badge count
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onOpenNotifications() }
                    .testTag("home_notification_bell_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (unreadNotifications > 0) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    tint = if (unreadNotifications > 0) Color.Black else Color(0xFF4B5563),
                    modifier = Modifier.size(22.dp)
                )
                if (unreadNotifications > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                            .clip(CircleShape)
                            .background(GoldAccent)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                            .heightIn(min = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadNotifications > 99) "99+" else "$unreadNotifications",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Amusements/Events icon with badge dot
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onOpenEvents() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "Funky Events",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                if (hasActiveEvents) {
                    // Tiny dynamic badge dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                            .clip(CircleShape)
                            .background(GoldAccent)
                    )
                }
            }

            // Join with Code button
            Button(
                onClick = onJoinWithCode,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Code",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // Simple clean room icon button (home-like, matching the header circular styling)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GoldAccent)
                    .clickable { onCreateRoom() },
                contentAlignment = Alignment.Center
            ) {
                FunkyHomeIcon(
                    color = Color.Black,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.0f
                )
            }
        }
    }
}

