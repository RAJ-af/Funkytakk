package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.example.ui.components.SoftCrownIcon
import com.example.ui.components.SoftPersonIcon
import com.example.ui.components.SoftPlusIcon
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
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.ui.screens.voiceroom.SeatPositionRegistry
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
    onRequestSpeakerSeat: () -> Unit = {},
    compactForKeyboard: Boolean = false
) {
    val context = LocalContext.current
    val speakerIdsKey = remember(hostSeat.userId, speakerSeats) {
        (listOf(hostSeat.userId) + speakerSeats.map { it.userId })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val fetchedCosmeticUids = remember { mutableSetOf<String>() }

    LaunchedEffect(speakerIdsKey) {
        val missing = speakerIdsKey.filter { it !in fetchedCosmeticUids }
        if (missing.isNotEmpty()) {
            fetchedCosmeticUids.addAll(missing)
            com.example.store.CosmeticsManager.batchFetchUserCosmetics(context, missing)
        }
    }

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
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Seat 1: Host
            VoiceSpeakerItem(
                speaker = hostSeat,
                reactions = speakerReactions[hostSeat.name] ?: emptyList(),
                theme = currentTheme,
                isDarkMode = isRoomDarkMode,
                modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(1f),
                    onHostInviteSeatClick = onHostInviteSeatClick,
                    onRequestSlot = handleSlotRequest
                )
            }
        }

        if (!compactForKeyboard) {
            // Second Row - Seat 4 (Speaker 3), Seat 5 (Speaker 4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Top
            ) {
            val sp3 = speakerSeats.getOrNull(2)
            if (sp3 != null) {
                VoiceSpeakerItem(
                    speaker = sp3,
                    reactions = speakerReactions[sp3.name] ?: emptyList(),
                    theme = currentTheme,
                    isDarkMode = isRoomDarkMode,
                    modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(1f),
                    onHostInviteSeatClick = onHostInviteSeatClick,
                    onRequestSlot = handleSlotRequest
                )
            }
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
    modifier: Modifier = Modifier,
    onHostInviteSeatClick: () -> Unit = {},
    onRequestSlot: () -> Unit = {}
) {
    val context = LocalContext.current
    val accentColor = theme.accentColor
    val secondaryText = theme.secondaryTextColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.widthIn(min = 82.dp)
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
                    SoftPersonIcon(
                        size = 22.dp,
                        color = secondaryText.copy(alpha = 0.35f)
                    )
                }
                else -> {
                    SoftPlusIcon(
                        size = 22.dp,
                        color = accentColor
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
    modifier: Modifier = Modifier,
    onReactionComplete: (Long) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val size = if (speaker.isCenter) 72.dp else 64.dp
    val primaryTextColor = theme?.primaryTextColor ?: (if (isDarkMode) Color.White else Color.Black)
    val micIconTint = theme?.accentColor ?: ChillPurpleAccent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(min = if (speaker.isCenter) 92.dp else 82.dp)
            .onGloballyPositioned { coords ->
                if (speaker.userId.isNotBlank()) {
                    SeatPositionRegistry.updatePosition(speaker.userId, coords)
                }
                if (speaker.name.isNotBlank()) {
                    SeatPositionRegistry.updatePosition(speaker.name, coords)
                }
            }
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            com.example.ui.components.PresenceAvatar(
                avatarUrl = speaker.imageUrl,
                size = size,
                isSpeaking = speaker.isSpeaking,
                isOnline = !speaker.isHostAway,
                countryCode = speaker.flagCode.ifBlank { null },
                userId = speaker.userId,
                enableFrameAnimation = true
            )

            // Host Badge / Crown
            if (speaker.isHost) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .background(Color(0xFFFFD700), RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        SoftCrownIcon(size = 10.dp, color = Color.Black)
                        Text(
                            text = "HOST",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Host Away subtle indicator badge
            if (speaker.isHost && speaker.isHostAway) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
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

            // Speaker floating reactions overlay starting from avatar center and rising upward beautifully
            reactions.forEach { reaction ->
                key(reaction.id) {
                    FloatingSpeakerEmoji(url = reaction.url, onComplete = { onReactionComplete(reaction.id) })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            com.example.store.DecoratedUserName(
                userId = speaker.userId,
                name = speaker.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                defaultColor = primaryTextColor
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
