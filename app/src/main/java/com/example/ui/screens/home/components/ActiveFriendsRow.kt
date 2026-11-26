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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.FunJokeDialog
import com.example.ui.components.FunkySmileyIcon
import com.example.ui.components.PresenceAvatar
import com.example.ui.screens.home.models.FollowedFriend
import com.example.ui.screens.home.models.GoldDark
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalFunkyColors

@Composable
fun ActiveFriendsRow(
    activeFriendsList: List<FollowedFriend>,
    onFriendClick: (FollowedFriend) -> Unit = {}
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val isDark = LocalAppDarkTheme.current
    var showJokeDialog by remember { mutableStateOf(false) }

    if (showJokeDialog) {
        FunJokeDialog(onDismiss = { showJokeDialog = false })
    }

    val onlineCount = activeFriendsList.count { it.isOnline }

    LaunchedEffect(activeFriendsList) {
        val uids = activeFriendsList.map { it.id }.filter { it.isNotBlank() }
        if (uids.isNotEmpty()) {
            com.example.store.CosmeticsManager.batchFetchUserCosmetics(context, uids)
        }
    }

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
            Text(
                text = "Friends you follow",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = funkyColors.mute
            )

            if (onlineCount > 0) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isDark) Color(0xFF143524) else Color(0xFFEBFDF2),
                            RoundedCornerShape(100.dp)
                        )
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
                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                        )
                    }
                }
            } else if (activeFriendsList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .background(funkyColors.soft, RoundedCornerShape(100.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "All offline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = funkyColors.mute
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 1. Fun Jokes circle
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
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .scale(shimmerScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFFFB627),
                                            Color(0xFFFB923C),
                                            Color(0xFFF43F5E),
                                            Color(0xFF8B5CF6),
                                            Color(0xFFFFB627)
                                        )
                                    )
                                )
                                .padding(2.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isDark) funkyColors.soft else Color(0xFFFFFBEB)),
                                contentAlignment = Alignment.Center
                            ) {
                                FunkySmileyIcon(
                                    color = funkyColors.mango,
                                    modifier = Modifier.size(26.dp),
                                    strokeWidth = 2.0f
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(funkyColors.mango)
                                .border(1.5.dp, funkyColors.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_sparkle),
                                contentDescription = "Fun Jokes",
                                tint = funkyColors.onMango,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fun Jokes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tap for Laugh",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) funkyColors.mango else Color(0xFFD97706),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. FRIENDS LIST with overlapping bottom-right country flag on circular DP
            itemsIndexed(activeFriendsList) { _, friend ->
                val isOnlineUser = friend.isOnline

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            if (friend.id.isNotBlank()) {
                                onFriendClick(friend)
                            } else {
                                Toast.makeText(
                                    context,
                                    if (isOnlineUser) "${friend.name} is online now" else "${friend.name} is currently offline",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .width(78.dp)
                ) {
                    PresenceAvatar(
                        avatarUrl = friend.avatarUrl,
                        size = 56.dp,
                        isSpeaking = friend.isSpeaking,
                        isOnline = isOnlineUser,
                        countryCode = friend.flagCode,
                        showFlagBadge = true,
                        userId = friend.id,
                        enableFrameAnimation = false
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    com.example.store.DecoratedUserName(
                        userId = friend.id,
                        name = friend.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        defaultColor = if (isOnlineUser) funkyColors.ink else funkyColors.mute
                    )

                    if (isOnlineUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (friend.isSpeaking) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_mic),
                                    contentDescription = null,
                                    tint = GoldDark,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Text(
                                text = if (friend.isSpeaking) "Speaking" else "Online",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (friend.isSpeaking) GoldDark else if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
