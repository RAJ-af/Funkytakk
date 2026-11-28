package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import co.daily.model.MediaStreamTrack
import co.daily.view.VideoView
import com.example.realtime.ScreenShareRole
import kotlinx.coroutines.delay

/**
 * Safe-share pre-flight dialog with mandatory 3-2-1 countdown before launching MediaProjection.
 */
@Composable
fun SafeShareCountdownDialog(
    isDarkMode: Boolean,
    onContinueToCapture: () -> Unit,
    onDismiss: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
        onContinueToCapture()
    }

    val containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val titleColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val bodyColor = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                SoftScreenShareIcon(size = 28.dp, color = Color(0xFFF59E0B))
            }
        },
        title = {
            Text(
                text = "Share your screen",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = titleColor
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Everything on your screen will be visible. Close private chats and notifications.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = bodyColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.14f))
                        .border(1.5.dp, Color(0xFFF59E0B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = secondsLeft.coerceAtLeast(1).toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD97706),
                        modifier = Modifier.testTag("safe_share_countdown_value")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onContinueToCapture,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("safe_share_continue_button")
            ) {
                Text(
                    text = if (secondsLeft > 0) "Starting in $secondsLeft..." else "Continue",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("safe_share_cancel_button")
            ) {
                Text(
                    text = "Cancel",
                    color = bodyColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

/**
 * Compact header button shown ONLY to the room host when `screen_share_enabled` is true.
 */
@Composable
fun HostScreenShareHeaderButton(
    role: ScreenShareRole,
    isConnecting: Boolean,
    cardBgColor: Color,
    cardBorderColor: Color,
    primaryTextColor: Color,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val isSharing = role == ScreenShareRole.SHARING
    val bgColor = if (isSharing) Color(0xFFEF4444).copy(alpha = 0.16f) else cardBgColor
    val borderColor = if (isSharing) Color(0xFFEF4444).copy(alpha = 0.7f) else cardBorderColor

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bgColor, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .clickable(enabled = !isConnecting) {
                if (isSharing) onStopClick() else onStartClick()
            }
            .testTag(if (isSharing) "host_stop_screen_share_button" else "host_screen_share_button"),
        contentAlignment = Alignment.Center
    ) {
        if (isSharing) {
            SoftStopCircleIcon(size = 20.dp, color = Color(0xFFEF4444))
        } else {
            SoftScreenShareIcon(size = 19.dp, color = primaryTextColor)
        }
    }
}

/**
 * Slim status banner shown to the Host while screen sharing is live.
 */
@Composable
fun HostScreenShareActiveBanner(
    isConnecting: Boolean,
    viewerCount: Int,
    isDarkMode: Boolean,
    onStopClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF2B1215) else Color(0xFFFEF2F2)
        ),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                SoftScreenShareIcon(size = 18.dp, color = Color(0xFFEF4444))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isConnecting) "Starting screen share..." else "You are sharing your screen",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF7F1D1D),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isConnecting) "Setting up video stream" else "$viewerCount watching • Mic & room audio stay on Agora",
                        fontSize = 11.sp,
                        color = if (isDarkMode) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onStopClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("host_banner_stop_share_button")
            ) {
                Text("Stop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Opt-in banner shown to viewers when `room_screen_shares.status == 'active'`.
 * Does NOT join Daily until the user taps Watch.
 */
@Composable
fun ViewerScreenShareBanner(
    hostName: String,
    isDarkMode: Boolean,
    onWatchClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("viewer_screen_share_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1F1912) else Color(0xFFFFFBEB)
        ),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    SoftTvWatchIcon(size = 18.dp, color = Color(0xFFF59E0B))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "$hostName is sharing their screen.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onWatchClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B),
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("viewer_watch_screen_share_button")
            ) {
                Text("Watch", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/**
 * Lightweight full-width single-VideoView stage for watching the host's Daily screen share.
 * Zero extra animations to keep memory/CPU overhead low on budget devices (Samsung Galaxy M02).
 */
@Composable
fun ViewerScreenShareStage(
    hostName: String,
    isConnecting: Boolean,
    remoteScreenTrack: MediaStreamTrack?,
    isDarkMode: Boolean,
    onStopWatching: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("viewer_screen_share_stage"),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF090D16),
        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top bar with Stop watching action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111827))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    SoftTvWatchIcon(size = 16.dp, color = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$hostName's screen",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.20f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onStopWatching)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("viewer_stop_screen_share_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SoftStopCircleIcon(size = 14.dp, color = Color(0xFFFCA5A5))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Stop watching",
                        color = Color(0xFFFECACA),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Single lightweight VideoView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (remoteScreenTrack != null) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                videoScaleMode = VideoView.VideoScaleMode.FIT
                                track = remoteScreenTrack
                            }
                        },
                        update = { view ->
                            if (view.track != remoteScreenTrack) {
                                view.track = remoteScreenTrack
                            }
                        },
                        onRelease = { view ->
                            view.track = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = if (isConnecting) "Connecting screen..." else "Waiting for host's screen...",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
