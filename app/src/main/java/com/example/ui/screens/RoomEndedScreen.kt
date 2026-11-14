package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.SoftGroupIcon
import com.example.ui.theme.LocalFunkyColors

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
    onClose: () -> Unit = {},
    onBackToHome: () -> Unit
) {
    // 1. Back button closes when user wants
    BackHandler {
        onClose()
        onBackToHome()
    }

    val funkyColors = LocalFunkyColors.current
    val effectivePeopleCount = if (peopleJoinedCount > 0) peopleJoinedCount else (if (totalJoins > 0) totalJoins else 1)
    val effectiveMinutes = if (totalTimeMinutes > 0) totalTimeMinutes else (if (durationSeconds > 0) durationSeconds / 60 else 0)

    val reasonTitle = when (endedReason.lowercase()) {
        "host_ended" -> "Host ended the room"
        "host_timeout" -> "Host was inactive for 3 mins"
        "moderation" -> "Closed by moderator"
        else -> "Room has ended"
    }

    val reasonSubtitle = when (endedReason.lowercase()) {
        "host_ended" -> "Host ended the room. All participants have been disconnected."
        "host_timeout" -> "Host was inactive for more than 3 minutes. Room closed automatically."
        "moderation" -> "This room was closed due to community safety guidelines."
        else -> "This voice room session has ended."
    }

    var totalDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(funkyColors.bg)
            // 2. Downward swipe gesture to close
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onDragEnd = {
                        if (totalDragY > 120f) {
                            onClose()
                            onBackToHome()
                        }
                        totalDragY = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragY += dragAmount
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Mascot Banner with Close button overlay
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

                // Top Floating Close Button (prominent pill)
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 16.dp, end = 16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable {
                                onClose()
                                onBackToHome()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Close",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
                            .background(funkyColors.mango, RoundedCornerShape(2.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(4.dp)
                            .rotate(25f)
                            .background(funkyColors.mango, RoundedCornerShape(2.dp))
                    )
                }

                Text(
                    text = "Room ended!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = funkyColors.ink
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
                            .background(funkyColors.mango, RoundedCornerShape(2.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(4.dp)
                            .rotate(-25f)
                            .background(funkyColors.mango, RoundedCornerShape(2.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Room Title display
            Text(
                text = roomTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = funkyColors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 28.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "That was a lovely little gathering.\nSee you in the next one!",
                fontSize = 14.sp,
                color = funkyColors.mute,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Combined Stats Card (People joined & Total time)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(funkyColors.surface)
                    .border(1.dp, funkyColors.line, RoundedCornerShape(24.dp))
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
                                .background(funkyColors.soft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftGroupIcon(
                                modifier = Modifier.size(22.dp),
                                color = funkyColors.mango
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$effectivePeopleCount",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = funkyColors.ink
                            )
                            Text(
                                text = "People joined",
                                fontSize = 12.sp,
                                color = funkyColors.mute,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .width(1.dp)
                            .background(funkyColors.line)
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
                                .background(funkyColors.soft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            RoomEndedClockIcon(
                                color = funkyColors.mango,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (effectiveMinutes > 0) "$effectiveMinutes min" else "${durationSeconds}s",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = funkyColors.ink
                            )
                            Text(
                                text = "Total time",
                                fontSize = 12.sp,
                                color = funkyColors.mute,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reason Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(funkyColors.surface)
                    .border(1.dp, funkyColors.line, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(funkyColors.ok.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = "Status",
                            tint = funkyColors.ok,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reasonTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = reasonSubtitle,
                            fontSize = 12.sp,
                            color = funkyColors.mute,
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
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(27.dp),
                        spotColor = funkyColors.mango.copy(alpha = 0.4f)
                    )
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
                                .background(funkyColors.onMango, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_plus),
                                contentDescription = null,
                                tint = funkyColors.mango,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Create another room",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.onMango
                        )
                    }

                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right),
                        contentDescription = "Next",
                        tint = funkyColors.onMango,
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
                    .background(funkyColors.surface)
                    .border(1.dp, funkyColors.line, RoundedCornerShape(27.dp))
                    .clickable {
                        onClose()
                        onBackToHome()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home),
                        contentDescription = "Home",
                        tint = funkyColors.ink,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back to Home",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun RoomEndedClockIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.11f
        val radius = (size.minDimension - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y - radius * 0.52f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x + radius * 0.42f, center.y + radius * 0.18f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
