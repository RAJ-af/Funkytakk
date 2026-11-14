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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FunkyChatIcon
import com.example.ui.components.SoftExitIcon
import com.example.ui.components.SoftFaceIcon
import com.example.ui.components.SoftGiftBoxIcon
import com.example.ui.components.SoftGroupIcon
import com.example.ui.components.SoftMicIcon
import com.example.ui.components.SoftMicOffIcon

@Composable
fun VoiceRoomControls(
    isMicOn: Boolean,
    isSpeakerOrHost: Boolean,
    unreadCount: Int = 0,
    isDarkMode: Boolean,
    onToggleMic: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenPeople: () -> Unit,
    onOpenReactions: () -> Unit,
    onOpenGift: () -> Unit = {},
    onLeaveRoom: () -> Unit
) {
    val barBg = if (isDarkMode) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.9f)
    val barBorder = if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color(0xFFF1F5F9)
    val iconColor = if (isDarkMode) Color.White else Color(0xFF0F172A)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow( elevation = 8.dp, shape = RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(32.dp))
            .background(barBg)
            .border(1.dp, barBorder, RoundedCornerShape(32.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chat Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFF8FAFC))
                    .clickable { onOpenChat() },
                contentAlignment = Alignment.Center
            ) {
                FunkyChatIcon(
                    color = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .background(Color(0xFFEF4444), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // People Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFF8FAFC))
                    .clickable { onOpenPeople() },
                contentAlignment = Alignment.Center
            ) {
                SoftGroupIcon(
                    size = 20.dp,
                    color = iconColor
                )
            }

            // Mic Button (Primary Action - Larger)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow( elevation = 4.dp, shape = CircleShape, spotColor = Color(0xFFFFB800).copy(alpha = 0.4f))
                    .clip(CircleShape)
                    .background(
                        if (isMicOn && isSpeakerOrHost) {
                            Brush.horizontalGradient(listOf(Color(0xFFFFD000), Color(0xFFFFB800)))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFF64748B), Color(0xFF475569)))
                        }
                    )
                    .clickable { onToggleMic() },
                contentAlignment = Alignment.Center
            ) {
                if (isMicOn && isSpeakerOrHost) {
                    SoftMicIcon(
                        size = 24.dp,
                        color = Color.Black
                    )
                } else {
                    SoftMicOffIcon(
                        size = 24.dp,
                        color = Color.White
                    )
                }
            }

            // Reactions Overlay Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFF8FAFC))
                    .clickable { onOpenReactions() },
                contentAlignment = Alignment.Center
            ) {
                SoftFaceIcon(
                    size = 20.dp,
                    color = iconColor
                )
            }

            // Gift Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDarkMode) Color(0xFFEC4899).copy(alpha = 0.2f) else Color(0xFFFDF2F8)
                    )
                    .border(
                        1.dp,
                        if (isDarkMode) Color(0xFFEC4899).copy(alpha = 0.4f) else Color(0xFFFCE7F3),
                        CircleShape
                    )
                    .clickable { onOpenGift() },
                contentAlignment = Alignment.Center
            ) {
                SoftGiftBoxIcon(
                    size = 22.dp,
                    color = Color(0xFFEC4899)
                )
            }

            // Leave / Exit Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFFEF2F2))
                    .border(
                        1.dp,
                        if (isDarkMode) Color(0xFFEF4444).copy(alpha = 0.45f) else Color(0xFFFEE2E2),
                        CircleShape
                    )
                    .clickable { onLeaveRoom() },
                contentAlignment = Alignment.Center
            ) {
                SoftExitIcon(
                    size = 20.dp,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}
