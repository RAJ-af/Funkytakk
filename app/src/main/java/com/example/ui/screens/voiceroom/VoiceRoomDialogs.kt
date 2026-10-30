package com.example.ui.screens.voiceroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.CircleFlag

/**
 * Microphone Access Consent Dialog
 * Matches Image 3 (3D cloud mascot header, title, description, 3 feature pills, action buttons)
 */
@Composable
fun MicrophoneAccessDialog(
    onDismiss: () -> Unit,
    onAllow: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFFFFFDF5))
                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Header Mascot Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_leave_room_mascot),
                        contentDescription = "Microphone Mascot",
                        modifier = Modifier.fillMaxHeight(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = "Allow Microphone Access?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "We need your microphone permission to enable voice and video chat, so you can talk, make friends, and enjoy all the features on FunkyTalk.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3 Feature Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice Chat
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFEF08A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color(0xFF854D0E),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Voice Chat", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    }

                    // Video Chat
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFBBF7D0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color(0xFF166534),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Video Chat", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    }

                    // Meet New Friends
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFBCFE8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = Color(0xFF9D174D),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Meet New Friends", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Not Now Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color(0xFFFFFDF5))
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(25.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Not Now",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    // Allow Access Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .shadow(4.dp, RoundedCornerShape(25.dp), spotColor = Color(0xFFFFB800).copy(alpha = 0.4f))
                            .clip(RoundedCornerShape(25.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFD000), Color(0xFFFFB800))
                                )
                            )
                            .clickable { onAllow() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Allow Access",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Speaker Seat Invitation Dialog
 * Matches Image 7 (3D cloud banner with host avatar, title "... invites you to talk", feature pills, Join Talk button)
 */
@Composable
fun InviteToTalkDialog(
    hostName: String,
    hostAvatarUrl: String? = null,
    hostCountryCode: String = "in",
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFFFFFDF5))
                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Host Avatar + Cloud Header
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFFFFD000), CircleShape)
                    ) {
                        AsyncImage(
                            model = hostAvatarUrl ?: R.drawable.cool_avatar_1781324820431,
                            contentDescription = hostName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Country Flag Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                    ) {
                        CircleFlag(countryCode = hostCountryCode, modifier = Modifier.fillMaxSize())
                    }
                }

                // Admin/Host Badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFEF08A), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "👑 Admin",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF854D0E)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = "$hostName invites you to talk",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = "Join the conversation and connect with people in this room.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Feature Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color(0xFFFEF08A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF854D0E), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Voice Chat", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color(0xFFBBF7D0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Video Chat", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color(0xFFFBCFE8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF9D174D), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Meet New Friends", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color(0xFFFFFDF5))
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(25.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Not Now", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .shadow(4.dp, RoundedCornerShape(25.dp), spotColor = Color(0xFFFFB800).copy(alpha = 0.4f))
                            .clip(RoundedCornerShape(25.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFFFD000), Color(0xFFFFB800)))
                            )
                            .clickable { onAccept() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👋 Join Talk", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    }
                }
            }
        }
    }
}
