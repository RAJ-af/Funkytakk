package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun RoomEndedScreen(
    roomTitle: String = "FunkyTalk Room",
    hostName: String = "Host",
    hostAvatarUrl: String? = null,
    hostUserId: String? = null,
    currentUserId: String? = null,
    endedReason: String = "host_ended",
    durationSeconds: Int = 0,
    totalJoins: Int = 0,
    peakParticipants: Int = 0,
    peopleJoinedCount: Int = totalJoins,
    totalTimeMinutes: Int = if (durationSeconds > 0) durationSeconds / 60 else 0,
    isFollowingHost: Boolean = false,
    onFollowHost: ((String) -> Unit)? = null,
    onCreateAnotherRoom: () -> Unit = {},
    onBackToHome: () -> Unit
) {
    BackHandler {
        onBackToHome()
    }

    val effectivePeopleCount = if (peopleJoinedCount > 0) peopleJoinedCount else (if (totalJoins > 0) totalJoins else 12)
    val effectiveMinutes = if (totalTimeMinutes > 0) totalTimeMinutes else (if (durationSeconds > 0) durationSeconds / 60 else 28)

    val reasonTitle = when (endedReason.lowercase()) {
        "host_ended" -> "Room ended successfully"
        "host_timeout" -> "Host was away"
        "moderation" -> "Room closed by moderator"
        else -> "Room ended successfully"
    }

    val reasonSubtitle = when (endedReason.lowercase()) {
        "host_ended" -> "The host has ended this room. Everyone has been disconnected."
        "host_timeout" -> "The host was away for more than 3 minutes. The session has ended."
        "moderation" -> "This room was closed due to community safety guidelines."
        else -> "The host has ended this room. Everyone has been disconnected."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Mascot Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_room_ended_hero),
                    contentDescription = "Room ended hero banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Title Header with festive angled dashes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // Left festive sparks
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(4.dp)
                            .rotate(-25f)
                            .background(Color(0xFFFFB800), RoundedCornerShape(2.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(4.dp)
                            .rotate(25f)
                            .background(Color(0xFFFFB800), RoundedCornerShape(2.dp))
                    )
                }

                Text(
                    text = "Room ended!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )

                // Right festive sparks
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(start = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(4.dp)
                            .rotate(25f)
                            .background(Color(0xFFFFB800), RoundedCornerShape(2.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(4.dp)
                            .rotate(-25f)
                            .background(Color(0xFFFFB800), RoundedCornerShape(2.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "That was a lovely little gathering.\nSee you in the next one!",
                fontSize = 15.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Combined Stats Card (People joined & Total time)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFFFDF5))
                    .border(1.dp, Color(0xFFFEE685), RoundedCornerShape(24.dp))
                    .padding(vertical = 18.dp, horizontal = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Stat: People joined
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFEF3C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "People",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$effectivePeopleCount",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "People joined",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .width(1.dp)
                            .background(Color(0xFFFDE68A))
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // Right Stat: Total time
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFEF3C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Total time",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$effectiveMinutes min",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Total time",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFDCFCE7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Status",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reasonTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = reasonSubtitle,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Button 1: Create another room
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 24.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(27.dp), spotColor = Color(0xFFFFB800).copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(27.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFFD000), Color(0xFFFFB800))
                        )
                    )
                    .clickable { onCreateAnotherRoom() }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.Black, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Create another room",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button 2: Back to Home
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(27.dp))
                    .clickable { onBackToHome() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back to Home",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
