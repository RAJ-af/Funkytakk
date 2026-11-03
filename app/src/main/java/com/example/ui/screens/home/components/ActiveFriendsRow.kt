package com.example.ui.screens.home.components

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CircleFlag
import com.example.ui.components.FunJokeDialog
import com.example.ui.components.OfflineMoonBadge
import com.example.ui.components.UserAvatarImage
import com.example.ui.screens.home.models.FollowedFriend
import com.example.ui.screens.home.models.GoldDark
import com.example.ui.theme.GoldAccent

@Composable
fun ActiveFriendsRow(
    activeFriendsList: List<FollowedFriend>
) {
    val context = LocalContext.current
    var showJokeDialog by remember { mutableStateOf(false) }

    if (showJokeDialog) {
        FunJokeDialog(onDismiss = { showJokeDialog = false })
    }

    val onlineCount = activeFriendsList.count { it.isOnline }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Friends you follow",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }

            if (onlineCount > 0) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEBFDF2), androidx.compose.foundation.shape.RoundedCornerShape(100.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF16A34A), CircleShape)
                        )
                        Text(
                            text = "$onlineCount Online",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            } else if (activeFriendsList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF1F5F9), androidx.compose.foundation.shape.RoundedCornerShape(100.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "All offline 🌙",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 1. DEDICATED FUN CIRCLE ICON: "Joke Hub / Smile Time"
            item {
                val infiniteTransition = rememberInfiniteTransition(label = "joke_shimmer")
                val shimmerScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "jokePulse"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { showJokeDialog = true }
                        .width(74.dp)
                ) {
                    Box(
                        modifier = Modifier.size(68.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Vibrant animated gradient ring
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .scale(shimmerScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFFFD43B),
                                            Color(0xFFFB923C),
                                            Color(0xFFF43F5E),
                                            Color(0xFF8B5CF6),
                                            Color(0xFFFFD43B)
                                        )
                                    )
                                )
                                .padding(2.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFFBEB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🎭",
                                    fontSize = 24.sp
                                )
                            }
                        }

                        // Floating laugh badge on corner
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD43B))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("😂", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fun Jokes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tap for Laugh",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. FRIENDS LIST (Online or Offline with Moon Badge)
            itemsIndexed(activeFriendsList) { _, friend ->
                val isOnlineUser = friend.isOnline

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            if (isOnlineUser) {
                                val speakMsg = if (friend.isSpeaking) "Speaking in room now" else "Online now"
                                Toast.makeText(context, "${friend.name} is online! ($speakMsg)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "${friend.name} is currently offline (away sleeping 🌙)", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .width(74.dp)
                ) {
                    com.example.ui.components.PresenceAvatar(
                        avatarUrl = friend.avatarUrl,
                        size = 56.dp,
                        isSpeaking = friend.isSpeaking,
                        isOnline = isOnlineUser,
                        countryCode = friend.flagCode
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = friend.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOnlineUser) Color.Black else Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Subtitle text: ONLY say "Online" / "Speaking" if actually online! If offline, say "Offline 🌙"!
                    Text(
                        text = when {
                            isOnlineUser && friend.isSpeaking -> "Speaking 🎙️"
                            isOnlineUser -> "Online"
                            else -> "Offline 🌙"
                        },
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isOnlineUser && friend.isSpeaking -> GoldDark
                            isOnlineUser -> Color(0xFF16A34A)
                            else -> Color(0xFF94A3B8)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
