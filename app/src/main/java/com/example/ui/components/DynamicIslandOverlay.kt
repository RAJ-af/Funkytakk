package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.AuthViewModel

@Composable
fun DynamicIslandOverlay(
    viewModel: AuthViewModel,
    onNavigateToRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeRoomId by viewModel.activeRoomId.collectAsState()
    val activeRoomTitle by viewModel.activeRoomTitle.collectAsState()
    val isMicEnabled by viewModel.isMicEnabled.collectAsState()
    val speakers by viewModel.activeRoomSpeakers.collectAsState()
    val pendingRequest by viewModel.pendingSpeakerRequest.collectAsState()
    val activeHostId by viewModel.activeRoomHostId.collectAsState()

    val currentUserId = (viewModel.authState.collectAsState().value as? com.example.AuthState.Success)?.user?.uid
    val isHost = currentUserId != null && activeHostId != null && currentUserId == activeHostId

    val showOverlay = activeRoomId != null

    AnimatedVisibility(
        visible = showOverlay,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = 14.dp, end = 14.dp)
            .zIndex(999f)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isStateB = isHost && pendingRequest != null

            Surface(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 410.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = if (isStateB) Color(0xFFF59E0B).copy(alpha = 0.5f) else Color.Black
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .clickable { onNavigateToRoom() }
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    .testTag(if (isStateB) "dynamic_island_state_b" else "dynamic_island_state_a"),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF0F0F12),
                border = BorderStroke(
                    width = if (isStateB) 1.5.dp else 1.dp,
                    color = if (isStateB) Color(0xFFF59E0B).copy(alpha = 0.7f) else Color(0xFF27272A)
                )
            ) {
                if (isStateB && pendingRequest != null) {
                    // STATE B: Host Speaker Request Overlay
                    DynamicIslandStateB(
                        requesterName = pendingRequest!!.requesterName,
                        requesterAvatar = pendingRequest!!.requesterAvatar,
                        isMicEnabled = isMicEnabled,
                        onToggleMic = { viewModel.toggleMic(context) },
                        onAccept = {
                            viewModel.respondToSpeakerRequest(
                                requestId = pendingRequest!!.id,
                                accept = true,
                                context = context
                            )
                        },
                        onDecline = {
                            viewModel.respondToSpeakerRequest(
                                requestId = pendingRequest!!.id,
                                accept = false,
                                context = context
                            )
                        }
                    )
                } else {
                    // STATE A: Normal Participant Overlay
                    DynamicIslandStateA(
                        roomTitle = activeRoomTitle ?: "Voice Room",
                        speakerAvatars = speakers.mapNotNull { it.avatar }.ifEmpty {
                            listOf("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400")
                        },
                        isMicEnabled = isMicEnabled,
                        onToggleMic = { viewModel.toggleMic(context) },
                        onLeaveRoom = { viewModel.leaveRoom(context) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicIslandStateA(
    roomTitle: String,
    speakerAvatars: List<String>,
    isMicEnabled: Boolean,
    onToggleMic: () -> Unit,
    onLeaveRoom: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Speakers Avatar Stack & Live Audio Waveform
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            AvatarStack(avatars = speakerAvatars.take(3))
            
            Spacer(modifier = Modifier.width(8.dp))

            LiveAudioWaveform()

            Spacer(modifier = Modifier.width(8.dp))

            // Room title and subtitle
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = roomTitle,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Live • Tap to open",
                    color = Color(0xFFA1A1AA),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Action Controls: Mic Toggle & Red Leave Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mic Toggle
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMicEnabled) Color(0xFF27272A) else Color(0xFF451A1A)
                    )
                    .border(
                        1.dp,
                        if (isMicEnabled) Color(0xFF3F3F46) else Color(0xFFEF4444).copy(alpha = 0.6f),
                        CircleShape
                    )
                    .clickable { onToggleMic() }
                    .testTag("dynamic_island_mic_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = if (isMicEnabled) "Mute" else "Unmute",
                    tint = if (isMicEnabled) Color(0xFFE4E4E7) else Color(0xFFEF4444),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Red Leave Room Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                        )
                    )
                    .clickable { onLeaveRoom() }
                    .testTag("dynamic_island_leave_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Leave Voice Room",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun DynamicIslandStateB(
    requesterName: String,
    requesterAvatar: String,
    isMicEnabled: Boolean,
    onToggleMic: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Requester Avatar with Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(requesterAvatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = requesterName,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFFF59E0B), CircleShape),
                    contentScale = ContentScale.Crop
                )
                // Small Hand Emoji badge
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✋", fontSize = 8.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = requesterName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "wants to speak",
                    color = Color(0xFFFCD34D),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Actions: Mic toggle, Decline, Accept
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Mic Toggle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isMicEnabled) Color(0xFF27272A) else Color(0xFF451A1A))
                    .clickable { onToggleMic() }
                    .testTag("dynamic_island_state_b_mic_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = if (isMicEnabled) "Mute" else "Unmute",
                    tint = if (isMicEnabled) Color.White else Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Decline Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF27272A))
                    .border(1.dp, Color(0xFF3F3F46), CircleShape)
                    .clickable { onDecline() }
                    .testTag("dynamic_island_decline_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Decline Speaker Request",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Accept Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF10B981), Color(0xFF059669))
                        )
                    )
                    .clickable { onAccept() }
                    .testTag("dynamic_island_accept_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Accept Speaker Request",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AvatarStack(avatars: List<String>) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier.height(28.dp)
    ) {
        avatars.forEachIndexed { index, url ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = "Speaker $index",
                modifier = Modifier
                    .offset(x = (index * 16).dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0xFF0F0F12), CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun LiveAudioWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    val bar1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3 by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = Modifier.height(18.dp)
    ) {
        // Green live dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF22C55E))
        )
        // 3 Animated waveform bars
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(bar1.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF22C55E))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(bar2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF22C55E))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(bar3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF22C55E))
        )
    }
}
