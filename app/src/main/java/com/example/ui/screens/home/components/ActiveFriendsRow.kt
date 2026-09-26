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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.UserAvatarImage
import com.example.ui.components.CircleFlag
import com.example.ui.screens.home.models.FollowedFriend
import com.example.ui.screens.home.models.GoldDark
import com.example.ui.theme.GoldAccent

@Composable
fun ActiveFriendsRow(
    activeFriendsList: List<FollowedFriend>
) {
    val context = LocalContext.current

    if (activeFriendsList.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 18.dp)
    ) {
        Text(
            text = "Friends you follow (Active)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            itemsIndexed(activeFriendsList) { _, friend ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            val speakMsg = if (friend.isSpeaking) "Speaking now" else "Active now"
                            Toast.makeText(context, "${friend.name} is online! ($speakMsg)", Toast.LENGTH_SHORT).show()
                        }
                        .width(76.dp)
                ) {
                    Box(
                        modifier = Modifier.size(68.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Bouncing dynamic outer scale indicator on actively speaking followed friends
                        if (friend.isSpeaking) {
                            val infiniteTransition = rememberInfiniteTransition(label = "speaking_pulse")
                            val speakScale by infiniteTransition.animateFloat(
                                initialValue = 1.0f,
                                targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseScale"
                            )
                            val speakAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 0.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .scale(speakScale)
                                    .alpha(speakAlpha)
                                    .border(
                                        width = 2.dp,
                                        color = GoldAccent,
                                        shape = CircleShape
                                    )
                            )
                        }

                        // The visual Avatar card itself with a thin gold border frame
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .border(
                                    width = 1.5.dp,
                                    color = if (friend.isSpeaking) GoldAccent else Color.LightGray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                UserAvatarImage(
                                    avatarUrl = friend.avatarUrl,
                                    contentDescription = friend.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )

                                // Ambient audio wave equalizer overlay if currently speaking
                                if (friend.isSpeaking) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SpeakingWaveform()
                                    }
                                }
                            }
                        }

                        // Clean Physical-Masked Flag Badge completely overlapping the bottom-right corner of outer ring
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .border(2.dp, Color.White, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            CircleFlag(
                                countryCode = friend.flagCode,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = friend.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (friend.isSpeaking) "Speaking 🎙️" else "Online",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (friend.isSpeaking) GoldDark else Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
