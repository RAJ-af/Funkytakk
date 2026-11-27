package com.example.ui.screens.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.ui.components.CircleFlag
import com.example.ui.components.SoftBackChevronIcon
import com.example.ui.components.SoftMicIcon
import com.example.ui.components.SoftMicOffIcon
import com.example.ui.components.SoftPersonAddIcon
import com.example.ui.components.SoftPhoneEndIcon
import com.example.ui.components.SoftVoiceWaveIcon
import com.example.ui.components.SoftVolumeIcon
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * UI-level Voice Call screen overlay for 1-to-1 Direct Chat.
 * Purely UI-level preview (no backend/Agora call signaling attached yet) using bespoke
 * Canvas soft rounded icons and an immersive obsidian canvas.
 */
@Composable
fun DirectVoiceCallOverlay(
    partnerName: String,
    partnerAvatarUrl: String?,
    partnerCountryCode: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var isConnected by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    // Simulated UI-only call progression: "Calling..." -> live call timer
    LaunchedEffect(Unit) {
        delay(1400L)
        isConnected = true
        while (true) {
            delay(1000L)
            elapsedSeconds += 1
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "voiceCallPulse")
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringPulse"
    )
    val wavePhaseA by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveA"
    )
    val wavePhaseB by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 640, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveB"
    )
    val wavePhaseC by infiniteTransition.animateFloat(
        initialValue = 0.48f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 440, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveC"
    )

    val waveLevels = remember(isConnected, isMuted, wavePhaseA, wavePhaseB, wavePhaseC) {
        when {
            !isConnected -> listOf(0.32f, 0.50f, 0.65f, 0.50f, 0.32f)
            isMuted -> listOf(0.24f, 0.24f, 0.24f, 0.24f, 0.24f)
            else -> listOf(
                wavePhaseA * 0.62f,
                wavePhaseB,
                (wavePhaseC + 0.15f).coerceAtMost(1f),
                wavePhaseB * 0.85f,
                wavePhaseA * 0.55f
            )
        }
    }

    val timerText = remember(isConnected, elapsedSeconds) {
        if (!isConnected) {
            "Calling..."
        } else {
            val mins = elapsedSeconds / 60
            val secs = elapsedSeconds % 60
            String.format(Locale.US, "%d:%02d", mins, secs)
        }
    }

    val recordBgColor by animateColorAsState(
        targetValue = if (isRecording) Color(0xFF3A1216) else Color(0xFF14161A),
        animationSpec = tween(200),
        label = "recordBg"
    )
    val recordBorderColor by animateColorAsState(
        targetValue = if (isRecording) Color(0xFFEF4444).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.12f),
        animationSpec = tween(200),
        label = "recordBorder"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(110f)
            .background(Color(0xFF050608))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume underlying chat taps */ }
    ) {
        // Subtle ambient radial spotlight behind center avatar
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1B1F2A).copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.42f),
                    radius = size.minDimension * 0.58f
                ),
                radius = size.minDimension * 0.58f,
                center = Offset(size.width * 0.5f, size.height * 0.42f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Back Chevron + [Record] Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .testTag("voice_call_back_button")
                        .semantics { contentDescription = "Minimize voice call" },
                    contentAlignment = Alignment.Center
                ) {
                    SoftBackChevronIcon(
                        size = 24.dp,
                        color = Color.White
                    )
                }

                Surface(
                    onClick = {
                        isRecording = !isRecording
                        Toast.makeText(
                            context,
                            if (isRecording) "Recording UI preview enabled" else "Recording UI preview stopped",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    shape = RoundedCornerShape(99.dp),
                    color = recordBgColor,
                    border = BorderStroke(1.dp, recordBorderColor),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("voice_call_record_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Custom Canvas red record indicator with crisp outer ring
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val rOuter = size.minDimension * 0.46f
                            val rInner = size.minDimension * 0.28f
                            drawCircle(
                                color = Color(0xFFEF4444).copy(alpha = if (isRecording) 0.90f else 0.55f),
                                radius = rOuter,
                                style = Stroke(width = 1.4.dp.toPx())
                            )
                            drawCircle(
                                color = Color(0xFFEF4444),
                                radius = rInner
                            )
                        }
                        Text(
                            text = if (isRecording) "Recording" else "Record",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Center Avatar + Flag Badge + Peer Name + Waveform & Timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(168.dp)
            ) {
                // Gentle calling pulse halo while connecting
                if (!isConnected) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val baseR = size.minDimension * 0.41f
                        val expandR = baseR + (size.minDimension * 0.14f * ringPulse)
                        drawCircle(
                            color = Color.White.copy(alpha = (1f - ringPulse) * 0.22f),
                            radius = expandR,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Circular Peer Avatar
                Box(
                    modifier = Modifier
                        .size(138.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF2B2F3A), Color(0xFF171A21))
                            )
                        )
                        .border(2.dp, Color(0xFF232730), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!partnerAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = partnerAvatarUrl,
                            contentDescription = partnerName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = partnerName.take(1).uppercase(Locale.getDefault()),
                            color = Color.White,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom-Right Country Flag Badge
                val cleanCode = partnerCountryCode.orEmpty().trim()
                if (cleanCode.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 14.dp, bottom = 14.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF050608))
                            .border(2.dp, Color(0xFF050608), CircleShape)
                            .padding(1.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircleFlag(
                            countryCode = cleanCode,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = partnerName,
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SoftVoiceWaveIcon(
                    size = 22.dp,
                    color = if (isMuted) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                    levels = waveLevels
                )
                Text(
                    text = timerText,
                    color = Color(0xFF9CA3AF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Voice call UI preview",
                color = Color.White.copy(alpha = 0.36f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1.15f))

            // Bottom Floating Translucent Pill Control Dock
            Surface(
                shape = RoundedCornerShape(38.dp),
                color = Color(0xFF14161A).copy(alpha = 0.94f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Speaker Toggle
                    VoiceDockActionItem(
                        isActive = isSpeakerOn,
                        contentDescription = if (isSpeakerOn) "Speaker on" else "Speaker off",
                        testTagName = "voice_call_speaker_button",
                        onClick = { isSpeakerOn = !isSpeakerOn }
                    ) {
                        SoftVolumeIcon(
                            size = 24.dp,
                            color = if (isSpeakerOn) Color.White else Color(0xFF9CA3AF)
                        )
                    }

                    VoiceDockDivider()

                    // 2. Add Participant (UI preview notice)
                    VoiceDockActionItem(
                        isActive = false,
                        contentDescription = "Add participant",
                        testTagName = "voice_call_add_person_button",
                        onClick = {
                            Toast.makeText(
                                context,
                                "Invite participant is UI preview only",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        SoftPersonAddIcon(
                            size = 23.dp,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }

                    VoiceDockDivider()

                    // 3. Mute Microphone Toggle
                    VoiceDockActionItem(
                        isActive = isMuted,
                        contentDescription = if (isMuted) "Unmute microphone" else "Mute microphone",
                        testTagName = "voice_call_mute_button",
                        onClick = { isMuted = !isMuted }
                    ) {
                        if (isMuted) {
                            SoftMicOffIcon(
                                size = 23.dp,
                                color = Color.White
                            )
                        } else {
                            SoftMicIcon(
                                size = 23.dp,
                                color = Color(0xFFD1D5DB)
                            )
                        }
                    }

                    // 4. End Call (Vibrant Red Circle)
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .clickable(onClick = onDismiss)
                            .testTag("voice_call_end_button")
                            .semantics { contentDescription = "End voice call" },
                        contentAlignment = Alignment.Center
                    ) {
                        SoftPhoneEndIcon(
                            size = 27.dp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun VoiceDockActionItem(
    isActive: Boolean,
    contentDescription: String,
    testTagName: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) Color.White.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(180),
        label = "dockItemBg"
    )
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .testTag(testTagName)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun VoiceDockDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color.White.copy(alpha = 0.08f))
    )
}
