package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.AuthViewModel
import com.example.service.OverlayPermissionHelper
import com.example.service.VoiceRoomOverlayManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * In-App Floating Bubble Overlay:
 * Replacement for the Dynamic Island. Visible when user is in an active room
 * but navigated elsewhere inside the app.
 * Matches Facebook Messenger chat-head style with organic satellite controls.
 */
@Composable
fun FloatingBubbleOverlay(
    viewModel: AuthViewModel,
    onNavigateToRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeRoomId by viewModel.activeRoomId.collectAsState()
    val isMicEnabled by viewModel.isMicEnabled.collectAsState()
    val isSpeaking by VoiceRoomOverlayManager.isSpeaking.collectAsState()
    val isServiceRunning by VoiceRoomOverlayManager.isServiceRunning.collectAsState()

    // If the system-level overlay service is actively rendering over the screen,
    // don't render a duplicate inside the Compose hierarchy.
    if (isServiceRunning && OverlayPermissionHelper.canDrawOverlays(context)) {
        return
    }

    if (activeRoomId == null) return

    val currentUserProfile = (viewModel.authState.collectAsState().value as? com.example.AuthState.Success)?.user
    val avatarUrl = currentUserProfile?.avatar.orEmpty().ifBlank {
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var offsetX by remember { mutableStateOf(screenWidthPx - with(density) { 90.dp.toPx() }) }
    var offsetY by remember { mutableStateOf(with(density) { 180.dp.toPx() }) }
    var isSnappedToRight by remember { mutableStateOf(true) }
    var isOptionsOpen by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val animatedOffsetX = remember { Animatable(offsetX) }

    LaunchedEffect(offsetX) {
        animatedOffsetX.snapTo(offsetX)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(999f)
    ) {
        // Backdrop to dismiss options when tapped outside
        if (isOptionsOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            isOptionsOpen = false
                        }
                    }
            )
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = animatedOffsetX.value.roundToInt().coerceIn(0, (screenWidthPx - 100).roundToInt()),
                        y = offsetY.roundToInt().coerceIn(60, (screenHeightPx - 200).roundToInt())
                    )
                }
        ) {
            FloatingBubbleLayout(
                avatarUrl = avatarUrl,
                isMicEnabled = isMicEnabled,
                isSpeaking = isSpeaking,
                isOptionsOpen = isOptionsOpen,
                isSnappedToRight = isSnappedToRight,
                onToggleOptions = { isOptionsOpen = !isOptionsOpen },
                onShortTap = { onNavigateToRoom() },
                onDrag = { dx, dy ->
                    offsetX += dx
                    offsetY += dy
                },
                onDragEnd = {
                    val targetX = if (offsetX + 40f < screenWidthPx / 2f) {
                        isSnappedToRight = false
                        16f
                    } else {
                        isSnappedToRight = true
                        screenWidthPx - with(density) { 90.dp.toPx() }
                    }
                    coroutineScope.launch {
                        animatedOffsetX.animateTo(
                            targetValue = targetX,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                },
                onToggleMic = {
                    viewModel.toggleMic(context)
                },
                onLeaveRoom = {
                    isOptionsOpen = false
                    viewModel.leaveRoom(context, trigger = "floating_bubble_leave_button")
                }
            )
        }
    }
}

/**
 * Reusable Floating Bubble with Satellite Options
 * Perfectly styled after reference screenshots:
 * - Golden glowing avatar circle with top-right indicator dot
 * - Organic metaball connector aura
 * - Upper satellite: Mic toggle (active gold / muted charcoal)
 * - Lower satellite: Coral red X to leave / cancel
 */
@Composable
fun FloatingBubbleLayout(
    avatarUrl: String,
    isMicEnabled: Boolean,
    isSpeaking: Boolean,
    isOptionsOpen: Boolean,
    isSnappedToRight: Boolean,
    onToggleOptions: () -> Unit,
    onShortTap: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onToggleMic: () -> Unit,
    onLeaveRoom: () -> Unit
) {
    val goldBorder = Color(0xFFF59E0B)       // Vibrant amber/gold
    val speakingDotColor = Color(0xFFFBBF24) // Yellow dot indicator on top-right rim
    val redBorder = Color(0xFFFF4D6D)        // Coral red glowing ring for leave

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .wrapContentSize()
            .padding(8.dp)
    ) {
        // When snapped to right edge, satellites appear to the LEFT
        if (isSnappedToRight && isOptionsOpen) {
            SatelliteOptions(
                isMicEnabled = isMicEnabled,
                goldBorder = goldBorder,
                redBorder = redBorder,
                isLeftAligned = true,
                onToggleMic = onToggleMic,
                onLeaveRoom = onLeaveRoom,
                onClose = onToggleOptions
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Main Circular Avatar Bubble
        Box(
            modifier = Modifier
                .size(68.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        },
                        onDragEnd = { onDragEnd() }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (isOptionsOpen) {
                                onToggleOptions()
                            } else {
                                onShortTap()
                            }
                        },
                        onLongPress = {
                            onToggleOptions()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Speaking pulse glow
            if (isSpeaking) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(goldBorder.copy(alpha = 0.55f), Color.Transparent)
                            )
                        )
                )
            }

            // Outer avatar container with golden border
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(14.dp, CircleShape, spotColor = goldBorder)
                    .clip(CircleShape)
                    .border(BorderStroke(2.5.dp, goldBorder), CircleShape)
                    .background(Color(0xFF0F0F12)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Active Speaker Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            // Indicator dot at top-right rim (35-45 degrees, matching mockup)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(13.dp)
                    .shadow(6.dp, CircleShape, spotColor = speakingDotColor)
                    .clip(CircleShape)
                    .background(speakingDotColor)
                    .border(BorderStroke(1.5.dp, Color(0xFF0F0F12)), CircleShape)
            )
        }

        // When snapped to left edge, satellites appear to the RIGHT
        if (!isSnappedToRight && isOptionsOpen) {
            Spacer(modifier = Modifier.width(6.dp))
            SatelliteOptions(
                isMicEnabled = isMicEnabled,
                goldBorder = goldBorder,
                redBorder = redBorder,
                isLeftAligned = false,
                onToggleMic = onToggleMic,
                onLeaveRoom = onLeaveRoom,
                onClose = onToggleOptions
            )
        }
    }
}

/**
 * Satellite buttons column with fluid organic connector
 */
@Composable
private fun SatelliteOptions(
    isMicEnabled: Boolean,
    goldBorder: Color,
    redBorder: Color,
    isLeftAligned: Boolean,
    onToggleMic: () -> Unit,
    onLeaveRoom: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .drawBehind {
                // Organic fluid gradient aura connecting the satellites to the bubble
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E1A24).copy(alpha = 0.92f),
                            Color(0xFF18151E).copy(alpha = 0.6f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.maxDimension * 0.75f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            // Satellite 1: Mic Toggle (Upper Circle)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(
                        elevation = if (isMicEnabled) 12.dp else 4.dp,
                        shape = CircleShape,
                        spotColor = if (isMicEnabled) goldBorder else Color.Black
                    )
                    .clip(CircleShape)
                    .background(
                        if (isMicEnabled) Color(0xFF1E1A11) else Color(0xFF18181A)
                    )
                    .border(
                        BorderStroke(
                            width = if (isMicEnabled) 2.2.dp else 1.2.dp,
                            color = if (isMicEnabled) goldBorder else Color(0xFF3F3D45)
                        ),
                        CircleShape
                    )
                    .clickable { onToggleMic() },
                contentAlignment = Alignment.Center
            ) {
                if (isMicEnabled) {
                    SoftMicIcon(
                        size = 22.dp,
                        color = Color.White
                    )
                } else {
                    SoftMicOffIcon(
                        size = 22.dp,
                        color = Color(0xFF8C7A58)
                    )
                }
            }

            // Satellite 2: Leave / Close (Red X Circle - Lower Circle)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(12.dp, CircleShape, spotColor = redBorder)
                    .clip(CircleShape)
                    .background(Color(0xFF261216))
                    .border(BorderStroke(2.2.dp, redBorder), CircleShape)
                    .clickable { onLeaveRoom() },
                contentAlignment = Alignment.Center
            ) {
                SoftCloseIcon(
                    size = 20.dp,
                    color = Color.White
                )
            }
        }
    }
}
