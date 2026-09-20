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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.CircleFlag

@Composable
fun ChatHeader(
    partnerName: String = "Yuki Tanaka",
    partnerAvatarUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
    partnerCountry: String = "Japan",
    partnerCountryCode: String = "jp",
    isOnline: Boolean = true,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanFlagCode = when {
        partnerCountryCode.isNotBlank() -> partnerCountryCode.trim().lowercase()
        partnerCountry.contains("japan", ignoreCase = true) -> "jp"
        partnerCountry.contains("korea", ignoreCase = true) -> "kr"
        partnerCountry.contains("china", ignoreCase = true) -> "cn"
        partnerCountry.contains("spain", ignoreCase = true) -> "es"
        partnerCountry.contains("germany", ignoreCase = true) -> "de"
        partnerCountry.contains("france", ignoreCase = true) -> "fr"
        partnerCountry.contains("india", ignoreCase = true) -> "in"
        partnerCountry.contains("russia", ignoreCase = true) -> "ru"
        partnerCountry.contains("emirates", ignoreCase = true) || partnerCountry.contains("arab", ignoreCase = true) -> "ae"
        partnerCountry.contains("united states", ignoreCase = true) || partnerCountry.contains("usa", ignoreCase = true) || partnerCountry.contains("english", ignoreCase = true) -> "us"
        else -> "jp"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        Box(
            modifier = Modifier
                .size(40.dp)
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

        // Partner Avatar with Circular Country Flag Icon (Replacing green dot)
        Box(modifier = Modifier.size(42.dp)) {
            AsyncImage(
                model = partnerAvatarUrl,
                contentDescription = partnerName,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFFE5E7EB), CircleShape),
                contentScale = ContentScale.Crop
            )

            // Circular Flag positioned at bottom-end replacing the green dot
            CircleFlag(
                countryCode = cleanFlagCode,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Partner Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = partnerName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                letterSpacing = (-0.2).sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$partnerCountry • ",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
                // Green Online Status Indicator
                AppSvgIcon(
                    icon = ChatIconAsset.OnlineStatus,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(8.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Online",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 3 Dots Menu Button
        Box(
            modifier = Modifier
                .size(40.dp)
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
    }
}
