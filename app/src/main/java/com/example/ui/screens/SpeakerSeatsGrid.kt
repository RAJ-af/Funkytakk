package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.SupabaseSessionManager
import com.example.SpeakerRequestInsert
import com.example.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SpeakerSeatsGrid(
    hostSeat: ChillRoomSpeaker,
    speakerSeats: List<ChillRoomSpeaker>,
    speakerReactions: MutableMap<String, List<FloatingEmojiId>>,
    currentTheme: RoomTheme,
    isRoomDarkMode: Boolean,
    activeRoomId: String,
    currentUserId: String,
    scope: CoroutineScope,
    onHostClick: () -> Unit,
    onSpeakerClick: (Int, ChillRoomSpeaker) -> Unit,
    isSpeakerRequestPending: Boolean = false,
    isCurrentUserSpeakerOrHost: Boolean = false,
    isCurrentUserHostOrModerator: Boolean = false,
    onHostInviteSeatClick: () -> Unit = {},
    onRequestSpeakerSeat: () -> Unit = {}
) {
    val context = LocalContext.current

    val handleSlotRequest = {
        if (onRequestSpeakerSeat != {}) {
            onRequestSpeakerSeat()
        } else {
            scope.launch(Dispatchers.IO) {
                try {
                    val token = SupabaseSessionManager.getBearerToken(context)
                    val uid = SupabaseSessionManager.getUserId(context) ?: currentUserId
                    SupabaseClient.api.createSpeakerRequest(
                        auth = token,
                        body = SpeakerRequestInsert(
                            room_id = activeRoomId,
                            user_id = uid,
                            status = "pending"
                        )
                    )
                } catch (_: Exception) {}
            }
            Toast.makeText(context, "Requested to join as speaker!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // First Row - Seat 1 (Host), Seat 2 (Speaker 1), Seat 3 (Speaker 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Seat 1: Host
            VoiceSpeakerItem(
                speaker = hostSeat,
                reactions = speakerReactions[hostSeat.name] ?: emptyList(),
                theme = currentTheme,
                isDarkMode = isRoomDarkMode,
                onReactionComplete = { rid ->
                    speakerReactions[hostSeat.name] = (speakerReactions[hostSeat.name] ?: emptyList()).filter { it.id != rid }
                },
                onClick = onHostClick
            )

            // Seat 2: Speaker 1 or Empty / Request
            val sp1 = speakerSeats.getOrNull(0)
            if (sp1 != null) {
                VoiceSpeakerItem(
                    speaker = sp1,
                    reactions = speakerReactions[sp1.name] ?: emptyList(),
                    theme = currentTheme,
                    isDarkMode = isRoomDarkMode,
                    onReactionComplete = { rid ->
                        speakerReactions[sp1.name] = (speakerReactions[sp1.name] ?: emptyList()).filter { it.id != rid }
                    },
                    onClick = { onSpeakerClick(0, sp1) }
                )
            } else {
                EmptySpeakerSeatItem(
                    theme = currentTheme,
                    isSpeakerRequestPending = isSpeakerRequestPending,
                    isCurrentUserSpeakerOrHost = isCurrentUserSpeakerOrHost,
                    isCurrentUserHostOrModerator = isCurrentUserHostOrModerator,
                    onHostInviteSeatClick = onHostInviteSeatClick,
                    onRequestSlot = handleSlotRequest
                )
            }

            // Seat 3: Speaker 2 or Empty / Request
            val sp2 = speakerSeats.getOrNull(1)
            if (sp2 != null) {
                VoiceSpeakerItem(
                    speaker = sp2,
                    reactions = speakerReactions[sp2.name] ?: emptyList(),
                    theme = currentTheme,
                    isDarkMode = isRoomDarkMode,
                    onReactionComplete = { rid ->
                        speakerReactions[sp2.name] = (speakerReactions[sp2.name] ?: emptyList()).filter { it.id != rid }
                    },
                    onClick = { onSpeakerClick(1, sp2) }
                )
            } else {
                EmptySpeakerSeatItem(
                    theme = currentTheme,
                    isSpeakerRequestPending = isSpeakerRequestPending,
                    isCurrentUserSpeakerOrHost = isCurrentUserSpeakerOrHost,
                    isCurrentUserHostOrModerator = isCurrentUserHostOrModerator,
                    onHostInviteSeatClick = onHostInviteSeatClick,
                    onRequestSlot = handleSlotRequest
                )
            }
        }

        // Second Row - Seat 4 (Speaker 3), Seat 5 (Speaker 4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            val sp3 = speakerSeats.getOrNull(2)
            if (sp3 != null) {
                VoiceSpeakerItem(
                    speaker = sp3,
                    reactions = speakerReactions[sp3.name] ?: emptyList(),
                    theme = currentTheme,
                    isDarkMode = isRoomDarkMode,
                    onReactionComplete = { rid ->
                        speakerReactions[sp3.name] = (speakerReactions[sp3.name] ?: emptyList()).filter { it.id != rid }
                    },
                    onClick = { onSpeakerClick(2, sp3) }
                )
            } else {
                EmptySpeakerSeatItem(
                    theme = currentTheme,
                    isSpeakerRequestPending = isSpeakerRequestPending,
                    isCurrentUserSpeakerOrHost = isCurrentUserSpeakerOrHost,
                    isCurrentUserHostOrModerator = isCurrentUserHostOrModerator,
                    onHostInviteSeatClick = onHostInviteSeatClick,
                    onRequestSlot = handleSlotRequest
                )
            }

            val sp4 = speakerSeats.getOrNull(3)
            if (sp4 != null) {
                VoiceSpeakerItem(
                    speaker = sp4,
                    reactions = speakerReactions[sp4.name] ?: emptyList(),
                    theme = currentTheme,
                    isDarkMode = isRoomDarkMode,
                    onReactionComplete = { rid ->
                        speakerReactions[sp4.name] = (speakerReactions[sp4.name] ?: emptyList()).filter { it.id != rid }
                    },
                    onClick = { onSpeakerClick(3, sp4) }
                )
            } else {
                EmptySpeakerSeatItem(
                    theme = currentTheme,
                    isSpeakerRequestPending = isSpeakerRequestPending,
                    isCurrentUserSpeakerOrHost = isCurrentUserSpeakerOrHost,
                    isCurrentUserHostOrModerator = isCurrentUserHostOrModerator,
                    onHostInviteSeatClick = onHostInviteSeatClick,
                    onRequestSlot = handleSlotRequest
                )
            }
        }
    }
}

@Composable
private fun EmptySpeakerSeatItem(
    theme: RoomTheme,
    isSpeakerRequestPending: Boolean,
    isCurrentUserSpeakerOrHost: Boolean,
    isCurrentUserHostOrModerator: Boolean = false,
    onHostInviteSeatClick: () -> Unit = {},
    onRequestSlot: () -> Unit = {}
) {
    val context = LocalContext.current
    val accentColor = theme.accentColor
    val secondaryText = theme.secondaryTextColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(82.dp)
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .drawBehind {
                    drawCircle(
                        color = when {
                            isSpeakerRequestPending -> ChillGoldIcon.copy(alpha = 0.7f)
                            isCurrentUserSpeakerOrHost -> secondaryText.copy(alpha = 0.25f)
                            else -> accentColor.copy(alpha = 0.45f)
                        },
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                    )
                }
                .clip(CircleShape)
                .clickable {
                    when {
                        isCurrentUserHostOrModerator -> {
                            onHostInviteSeatClick()
                        }
                        isCurrentUserSpeakerOrHost -> {
                            Toast.makeText(context, "You are already a speaker in this room.", Toast.LENGTH_SHORT).show()
                        }
                        isSpeakerRequestPending -> {
                            Toast.makeText(context, "Your request to speak is pending host approval", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            onRequestSlot()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                isSpeakerRequestPending -> {
                    CustomPendingHourglassIcon(
                        tint = ChillGoldIcon,
                        modifier = Modifier.size(24.dp)
                    )
                }
                isCurrentUserSpeakerOrHost -> {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Available slot",
                        tint = secondaryText.copy(alpha = 0.35f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Request slot",
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                isCurrentUserSpeakerOrHost -> "Available"
                isSpeakerRequestPending -> "Request Sent"
                else -> "+ Request"
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                isSpeakerRequestPending -> ChillGoldIcon
                isCurrentUserSpeakerOrHost -> secondaryText.copy(alpha = 0.6f)
                else -> secondaryText
            },
            maxLines = 1
        )
    }
}

@Composable
fun VoiceSpeakerItem(
    speaker: ChillRoomSpeaker,
    reactions: List<FloatingEmojiId> = emptyList(),
    theme: RoomTheme? = null,
    isDarkMode: Boolean = false,
    onReactionComplete: (Long) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val size = if (speaker.isCenter) 94.dp else 80.dp
    val primaryTextColor = theme?.primaryTextColor ?: (if (isDarkMode) Color.White else Color.Black)
    val micIconTint = theme?.accentColor ?: ChillPurpleAccent
    
    val infiniteTransition = rememberInfiniteTransition(label = "speaker_ring")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(if (speaker.isCenter) 100.dp else 86.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(if (speaker.isSpeaking) size + 6.dp else size)
                    .then(
                        if (speaker.isSpeaking) {
                            Modifier
                                .scale(pulseScale)
                                .border(
                                    width = 3.dp,
                                    brush = Brush.sweepGradient(listOf(Color(0xFF22C55E), Color(0xFF10B981), Color(0xFF34D399), Color(0xFF22C55E))),
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                        } else Modifier
                    )
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors = speaker.gradientColors))
                    .padding(if (speaker.isCenter) 3.5.dp else 2.5.dp)
            ) {
                AsyncImage(
                    model = speaker.imageUrl,
                    contentDescription = speaker.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .alpha(if (speaker.isHostAway) 0.75f else 1f)
                )
            }

            // Host Badge / Crown
            if (speaker.isHost) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .background(Color(0xFFFFD700), RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "👑 HOST",
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Host Away subtle indicator badge
            if (speaker.isHost && speaker.isHostAway) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .background(Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AWAY",
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Circular Image Flag
            if (speaker.flagCode.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(26.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https://flagcdn.com/w40/${speaker.flagCode}.png",
                        contentDescription = "Flag",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Speaker floating reactions overlay starting from avatar center and rising upward beautifully
            reactions.forEach { reaction ->
                key(reaction.id) {
                    FloatingSpeakerEmoji(url = reaction.url, onComplete = { onReactionComplete(reaction.id) })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = speaker.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            CustomMicIcon(
                tint = if (speaker.isMuted || speaker.isHostAway) Color.LightGray else micIconTint,
                modifier = Modifier.size(12.dp)
            )
        }

        if (speaker.isHost && speaker.isHostAway) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Host Away",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
fun FloatingSpeakerEmoji(url: String, onComplete: () -> Unit) {
    var isStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isStarted = true
        kotlinx.coroutines.delay(1800)
        onComplete()
    }
    
    val offsetY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isStarted) -180f else 0f, 
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1800, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        label = "offsetY"
    )
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isStarted) 0f else 1f, 
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1800, easing = androidx.compose.animation.core.LinearEasing),
        label = "alpha"
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isStarted) 1.5f else 0.5f, 
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "scale"
    )
    
    val wobbleX = remember { (-25..25).random().dp }
    
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .offset(x = wobbleX, y = offsetY.dp)
            .size(40.dp)
            .scale(scale)
            .alpha(alpha)
    )
}
