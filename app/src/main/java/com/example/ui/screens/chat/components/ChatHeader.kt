package com.example.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PresenceAvatar

@Composable
fun ChatHeader(
    partnerName: String = "Hacker",
    partnerAvatarUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
    partnerCountry: String = "Japan",
    partnerCountryCode: String = "jp",
    isOnline: Boolean = true,
    isSpeaking: Boolean = false,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuContent: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            AppSvgIcon(
                icon = ChatIconAsset.ArrowBack,
                tint = Color(0xFF111827),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Partner Circular Avatar with Presence
        PresenceAvatar(
            avatarUrl = partnerAvatarUrl,
            isOnline = isOnline,
            isSpeaking = isSpeaking,
            size = 40.dp
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Partner Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = partnerName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                letterSpacing = (-0.2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(1.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Small Green Online Dot
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) Color(0xFF22C55E) else Color(0xFF9CA3AF))
                )
                Spacer(modifier = Modifier.width(4.5.dp))
                Text(
                    text = if (isOnline) "Online" else "Offline",
                    fontSize = 12.sp,
                    color = if (isOnline) Color(0xFF16A34A) else Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 3 Dots Menu Button & Anchored Menu
        Box(
            modifier = Modifier.wrapContentSize(Alignment.TopEnd)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                AppSvgIcon(
                    icon = ChatIconAsset.MoreVertical,
                    tint = Color(0xFF111827),
                    modifier = Modifier.size(20.dp)
                )
            }
            menuContent()
        }
    }
}
