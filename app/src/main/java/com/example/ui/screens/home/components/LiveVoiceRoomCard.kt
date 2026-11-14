package com.example.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.CircleFlag
import com.example.ui.components.SoftHeadsetIcon
import com.example.ui.screens.home.models.CustomRoom
import com.example.ui.theme.LocalFunkyColors

@Composable
fun LiveVoiceRoomCard(
    room: CustomRoom,
    index: Int,
    onJoin: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .height(210.dp)
            .clickable { onJoin() }
            .testTag("voice_room_card_${room.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedRoomBanner(
                styleIndex = index,
                imageUrl = room.imageUrl,
                overlayColor = room.overlayColor,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row (Flag pill & LIVE badge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(funkyColors.surface.copy(alpha = 0.92f))
                            .border(0.5.dp, funkyColors.line, RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        CircleFlag(
                            countryCode = room.flagCode.ifBlank { room.language },
                            allowProgramming = false,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = room.language,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = funkyColors.ink
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(room.badgeColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = room.badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Middle Row (Title, Subtitle & Level tag)
                Column {
                    Text(
                        text = room.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = room.subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(funkyColors.surface.copy(alpha = 0.92f))
                            .border(0.5.dp, funkyColors.line, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = room.tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = funkyColors.ink
                        )
                    }
                }

                // Bottom Row (Stacked participants, Stats counters & Join Room Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        room.avatars.take(3).forEachIndexed { idx, url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .offset(x = (-(idx * 10)).dp)
                                    .size(28.dp)
                                    .border(1.5.dp, funkyColors.surface, CircleShape)
                                    .clip(CircleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .offset(x = (-(room.avatars.take(3).size * 10)).dp)
                                .size(28.dp)
                                .border(1.5.dp, funkyColors.surface, CircleShape)
                                .clip(CircleShape)
                                .background(funkyColors.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${room.speakingCount + room.listeningCount}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(funkyColors.surface.copy(alpha = 0.94f))
                            .border(0.5.dp, funkyColors.line, RoundedCornerShape(100.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_mic),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = funkyColors.ink
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${room.speakingCount} Speaking",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoftHeadsetIcon(
                                size = 12.dp,
                                color = funkyColors.ink
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${room.listeningCount} Listening",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink
                            )
                        }

                        Button(
                            onClick = onJoin,
                            colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango),
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(
                                text = "Join",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = funkyColors.onMango,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
