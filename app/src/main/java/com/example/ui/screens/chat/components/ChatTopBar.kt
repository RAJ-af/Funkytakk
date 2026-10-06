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
import com.example.ui.components.FunkyBackArrowIcon
import com.example.ui.components.FunkyVerticalMoreIcon

@Composable
fun ChatTopBar(
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
        else -> "us"
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
                .size(38.dp)
                .clip(CircleShape)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            FunkyBackArrowIcon(color = Color.Black, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Partner Avatar with Circular Flag Icon (Replacing green dot)
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

        // Partner Info (Name & Country / Online status)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = partnerName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = (-0.2).sp
            )

            Spacer(modifier = Modifier.height(1.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) Color(0xFF22C55E) else Color(0xFF94A3B8))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isOnline) "Online" else "Offline",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 3 Dots Menu Button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            FunkyVerticalMoreIcon(color = Color.Black, modifier = Modifier.size(20.dp))
        }
    }
}
