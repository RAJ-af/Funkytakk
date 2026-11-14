package com.example.ui.screens.voiceroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FunkyVerticalMoreIcon
import com.example.ui.components.Soft3DReportShield
import com.example.ui.components.SoftBackIcon
import com.example.ui.components.SoftShareIcon

@Composable
fun VoiceRoomHeader(
    roomTitle: String,
    displayShortCode: String,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val contentColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val pillBg = if (isDarkMode) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f)
    val pillBorder = if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Arrow + Room Title Box
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(pillBg)
                    .border(1.dp, pillBorder, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                SoftBackIcon(
                    size = 20.dp,
                    color = contentColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Title & ID Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(pillBg)
                    .border(1.dp, pillBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            text = roomTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (displayShortCode.isNotBlank()) {
                            Text(
                                text = "ID: $displayShortCode",
                                fontSize = 10.sp,
                                color = contentColor.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Action Buttons: Share, Report, Overflow
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Share Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(pillBg)
                    .border(1.dp, pillBorder, CircleShape)
                    .clickable { onShare() },
                contentAlignment = Alignment.Center
            ) {
                SoftShareIcon(
                    size = 18.dp,
                    color = contentColor
                )
            }

            // Report Shield
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(pillBg)
                    .border(1.dp, pillBorder, CircleShape)
                    .clickable { onReport() },
                contentAlignment = Alignment.Center
            ) {
                Soft3DReportShield(modifier = Modifier.size(20.dp))
            }

            // Overflow Menu
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(pillBg)
                    .border(1.dp, pillBorder, CircleShape)
                    .clickable { onOpenMenu() },
                contentAlignment = Alignment.Center
            ) {
                FunkyVerticalMoreIcon(
                    color = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
