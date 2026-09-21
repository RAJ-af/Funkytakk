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
    onSpeakerClick: (Int, ChillRoomSpeaker) -> Unit
) {
    val context = LocalContext.current

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

            // Seat 2: Speaker 1 or + Request
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(82.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .drawBehind {
                                drawCircle(
                                    color = currentTheme.accentColor.copy(alpha = 0.45f),
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                                    )
                                )
                            }
                            .clip(CircleShape)
                            .clickable {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val token = SupabaseSessionManager.getBearerToken(context)
                                        SupabaseClient.api.createSpeakerRequest(
                                            auth = token,
                                            body = SpeakerRequestInsert(
                                                room_id = activeRoomId,
                                                user_id = currentUserId
                                            )
                                        )
                                    } catch (_: Exception) {}
                                }
                                Toast.makeText(context, "Requested to join as speaker!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Request slot",
                            tint = currentTheme.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "+ Request",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.secondaryTextColor
                    )
                }
            }

            // Seat 3: Speaker 2 or + Request
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(82.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .drawBehind {
                                drawCircle(
                                    color = currentTheme.accentColor.copy(alpha = 0.45f),
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                                    )
                                )
                            }
                            .clip(CircleShape)
                            .clickable {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val token = SupabaseSessionManager.getBearerToken(context)
                                        SupabaseClient.api.createSpeakerRequest(
                                            auth = token,
                                            body = SpeakerRequestInsert(
                                                room_id = activeRoomId,
                                                user_id = currentUserId
                                            )
                                        )
                                    } catch (_: Exception) {}
                                }
                                Toast.makeText(context, "Requested to join as speaker!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Request slot",
                            tint = currentTheme.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "+ Request",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.secondaryTextColor
                    )
                }
            }
        }

        // Second Row - Seat 4 (Speaker 3), Seat 5 (Speaker 4), Request slot
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(82.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .drawBehind {
                                drawCircle(
                                    color = currentTheme.accentColor.copy(alpha = 0.45f),
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                                    )
                                )
                            }
                            .clip(CircleShape)
                            .clickable {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val token = SupabaseSessionManager.getBearerToken(context)
                                        SupabaseClient.api.createSpeakerRequest(
                                            auth = token,
                                            body = SpeakerRequestInsert(
                                                room_id = activeRoomId,
                                                user_id = currentUserId
                                            )
                                        )
                                    } catch (_: Exception) {}
                                }
                                Toast.makeText(context, "Requested to join as speaker!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Request slot",
                            tint = currentTheme.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "+ Request",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.secondaryTextColor
                    )
                }
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(82.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .drawBehind {
                                drawCircle(
                                    color = currentTheme.accentColor.copy(alpha = 0.45f),
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                                    )
                                )
                            }
                            .clip(CircleShape)
                            .clickable {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val token = SupabaseSessionManager.getBearerToken(context)
                                        SupabaseClient.api.createSpeakerRequest(
                                            auth = token,
                                            body = SpeakerRequestInsert(
                                                room_id = activeRoomId,
                                                user_id = currentUserId
                                            )
                                        )
                                    } catch (_: Exception) {}
                                }
                                Toast.makeText(context, "Requested to join as speaker!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Request slot",
                            tint = currentTheme.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "+ Request",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.secondaryTextColor
                    )
                }
            }
        }
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
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(if (speaker.isCenter) 100.dp else 86.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(size)
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
                tint = if (speaker.isMuted) Color.LightGray else micIconTint,
                modifier = Modifier.size(12.dp)
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
