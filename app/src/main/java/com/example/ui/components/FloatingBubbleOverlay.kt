package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.AuthViewModel
import com.example.service.OverlayPermissionHelper
import com.example.service.VoiceRoomOverlayManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * In-App Side Dynamic Island Overlay:
 * Docked flush to the left or right screen edge with concave edge-flared
 * obsidian geometry, displaying quick Return-to-Room, Mute/Unmute, and Leave Room controls.
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
    val islandWidthPx = with(density) { 62.dp.toPx() }
    val islandHeightPx = with(density) { 206.dp.toPx() }

    var offsetX by remember(screenWidthPx) { mutableStateOf((screenWidthPx - islandWidthPx).coerceAtLeast(0f)) }
    var offsetY by remember { mutableStateOf(with(density) { 180.dp.toPx() }) }
    var isSnappedToRight by remember { mutableStateOf(true) }

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
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = animatedOffsetX.value.roundToInt().coerceIn(0, (screenWidthPx - islandWidthPx).roundToInt().coerceAtLeast(0)),
                        y = offsetY.roundToInt().coerceIn(80, (screenHeightPx - islandHeightPx - 80f).roundToInt().coerceAtLeast(80))
                    )
                }
        ) {
            FloatingBubbleLayout(
                avatarUrl = avatarUrl,
                isMicEnabled = isMicEnabled,
                isSpeaking = isSpeaking,
                isOptionsOpen = true,
                isSnappedToRight = isSnappedToRight,
                onToggleOptions = {},
                onShortTap = { onNavigateToRoom() },
                onDrag = { dx, dy ->
                    offsetX = (offsetX + dx).coerceIn(0f, (screenWidthPx - islandWidthPx).coerceAtLeast(0f))
                    offsetY = (offsetY + dy).coerceIn(80f, (screenHeightPx - islandHeightPx - 80f).coerceAtLeast(80f))
                },
                onDragEnd = {
                    val centerX = offsetX + islandWidthPx / 2f
                    val targetX = if (centerX < screenWidthPx / 2f) {
                        isSnappedToRight = false
                        0f
                    } else {
                        isSnappedToRight = true
                        (screenWidthPx - islandWidthPx).coerceAtLeast(0f)
                    }
                    offsetX = targetX
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
                    viewModel.leaveRoom(context, trigger = "dynamic_island_leave_button")
                }
            )
        }
    }
}

/**
 * Reusable Docked Side Dynamic Island Control Surface
 * Compatible signature for both in-app FloatingBubbleOverlay and system VoiceRoomOverlayService.
 */
@Composable
fun FloatingBubbleLayout(
    avatarUrl: String,
    isMicEnabled: Boolean,
    isSpeaking: Boolean,
    @Suppress("UNUSED_PARAMETER") isOptionsOpen: Boolean,
    isSnappedToRight: Boolean,
    @Suppress("UNUSED_PARAMETER") onToggleOptions: () -> Unit,
    onShortTap: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onToggleMic: () -> Unit,
    onLeaveRoom: () -> Unit
) {
    val goldBorder = Color(0xFFFFB627)
    val greenBorder = Color(0xFF10B981)
    val coralRed = Color(0xFFFF4D6D)
    val obsidian = Color(0xFF0B0A09)

    val infiniteTransition = rememberInfiniteTransition(label = "islandPulse")
    val speakAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speakAlpha"
    )

    Box(
        modifier = Modifier
            .width(62.dp)
            .height(206.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = { onDragEnd() }
                )
            }
            .drawBehind {
                val w = size.width
                val h = size.height
                val flare = 12.dp.toPx()
                val cornerR = 24.dp.toPx()

                val islandPath = Path().apply {
                    if (isSnappedToRight) {
                        // Docked on RIGHT screen edge: top & bottom flare flush to x = w, inner edge curves left
                        moveTo(w, 0f)
                        cubicTo(w, flare * 0.7f, w - cornerR * 0.25f, flare, cornerR, flare)
                        quadraticTo(0f, flare, 0f, flare + cornerR)
                        lineTo(0f, h - flare - cornerR)
                        quadraticTo(0f, h - flare, cornerR, h - flare)
                        cubicTo(w - cornerR * 0.25f, h - flare, w, h - flare * 0.7f, w, h)
                        close()
                    } else {
                        // Docked on LEFT screen edge: top & bottom flare flush to x = 0, inner edge curves right
                        moveTo(0f, 0f)
                        cubicTo(0f, flare * 0.7f, cornerR * 0.25f, flare, w - cornerR, flare)
                        quadraticTo(w, flare, w, flare + cornerR)
                        lineTo(w, h - flare - cornerR)
                        quadraticTo(w, h - flare, w - cornerR, h - flare)
                        cubicTo(cornerR * 0.25f, h - flare, 0f, h - flare * 0.7f, 0f, h)
                        close()
                    }
                }

                drawPath(path = islandPath, color = obsidian)
                drawPath(
                    path = islandPath,
                    color = Color.White.copy(alpha = 0.14f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .padding(
                start = if (isSnappedToRight) 6.dp else 4.dp,
                end = if (isSnappedToRight) 4.dp else 6.dp,
                top = 18.dp,
                bottom = 18.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Return to Voice Room (Avatar with speaking/amber ring)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onShortTap() }
                    .testTag("dynamic_island_return_room")
                    .semantics { contentDescription = "Return to Voice Room" },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(44.dp)) {
                    val strokeW = 2.6.dp.toPx()
                    drawCircle(
                        color = Color(0xFF27221B),
                        radius = (size.minDimension - strokeW) / 2f,
                        style = Stroke(width = strokeW)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                goldBorder.copy(alpha = if (isSpeaking) speakAlpha else 0.9f),
                                Color(0xFFF59E0B),
                                goldBorder.copy(alpha = 0.35f),
                                goldBorder.copy(alpha = if (isSpeaking) speakAlpha else 0.9f)
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = if (isSpeaking) 300f else 220f,
                        useCenter = false,
                        style = Stroke(width = strokeW)
                    )
                }

                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1612))
                )

                // Subtle speaking status dot
                if (isSpeaking) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-3).dp, y = 3.dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(goldBorder)
                            .border(1.5.dp, obsidian, CircleShape)
                    )
                }
            }

            // 2. Mute / Unmute Mic Control
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onToggleMic() }
                    .testTag("dynamic_island_toggle_mic")
                    .semantics {
                        contentDescription = if (isMicEnabled) "Mute microphone" else "Unmute microphone"
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(
                            elevation = if (isMicEnabled) 8.dp else 0.dp,
                            shape = CircleShape,
                            spotColor = greenBorder
                        )
                        .clip(CircleShape)
                        .background(if (isMicEnabled) Color(0xFF10241A) else Color(0xFF191715))
                        .border(
                            BorderStroke(
                                width = if (isMicEnabled) 2.dp else 1.3.dp,
                                color = if (isMicEnabled) greenBorder else Color(0xFF47423B)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isMicEnabled) {
                        SoftMicIcon(size = 18.dp, color = Color.White)
                    } else {
                        SoftMicOffIcon(size = 18.dp, color = Color(0xFF9C8E7B))
                    }
                }
            }

            // 3. Leave Room Control
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onLeaveRoom() }
                    .testTag("dynamic_island_leave_room")
                    .semantics { contentDescription = "Leave voice room" },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(8.dp, CircleShape, spotColor = coralRed)
                        .clip(CircleShape)
                        .background(Color(0xFF281216))
                        .border(BorderStroke(2.dp, coralRed), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    SoftCloseIcon(size = 16.dp, color = Color.White)
                }
            }
        }
    }
}
