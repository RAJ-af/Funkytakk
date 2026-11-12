package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VerifyAccountErrorScreen(
    onRetry: () -> Unit = {},
    onLogout: () -> Unit = {},
    onContactSupport: () -> Unit = {}
) {
    val systemIsDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemIsDark) }
    var retryCount by remember { mutableIntStateOf(0) }
    var isChecking by remember { mutableStateOf(false) }
    var subtitleText by remember {
        mutableStateOf("Your session may have expired or the connection dropped. Give it another go.")
    }

    val coroutineScope = rememberCoroutineScope()

    // Color definitions according to reference HTML spec
    val bgColor = if (isDarkTheme) Color(0xFF16120D) else Color(0xFFFFF8E7)
    val surfaceColor = if (isDarkTheme) Color(0xFF231D15) else Color(0xFFFFFFFF)
    val softColor = if (isDarkTheme) Color(0xFF3A2D12) else Color(0xFFFFEFC2)
    val inkColor = if (isDarkTheme) Color(0xFFFFF3DC) else Color(0xFF2B2118)
    val muteColor = if (isDarkTheme) Color(0xFFA89A82) else Color(0xFF8A7B66)
    val mangoColor = Color(0xFFFFB627)
    val onMangoColor = Color(0xFF2B2118)
    val lineColor = if (isDarkTheme) Color(0xFF34291A) else Color(0xFFF0E3C4)
    val badColor = Color(0xFFFF5A4D)

    // Animated pulsing ring effect
    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val ringOpacity by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "opacity"
    )

    // Core wobble rotation animation (-8deg to 7deg)
    val coreWobble by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble"
    )

    // Spinner rotation during checking
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing)
        ),
        label = "spin"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("verify_account_error_screen"),
        color = bgColor,
        contentColor = inkColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Theme Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(softColor)
                        .clickable { isDarkTheme = !isDarkTheme },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.NightlightRound,
                        contentDescription = "Toggle Theme",
                        tint = inkColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Mid Section: Orb with Warning Badge, Title, Subtitle, Error 401 Badge
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Orb with pulsing rings
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Ring pulse 1
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(ringScale)
                            .graphicsLayer(alpha = ringOpacity)
                            .border(2.dp, lineColor, CircleShape)
                    )

                    // Soft background circle
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(CircleShape)
                            .background(softColor)
                    )

                    // Core warning box with wobble rotation
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .rotate(coreWobble)
                            .clip(RoundedCornerShape(34.dp))
                            .background(mangoColor),
                        contentAlignment = Alignment.Center
                    ) {
                        // Warning Triangle Icon (Custom canvas line drawing)
                        WarningAlertIcon(modifier = Modifier.size(56.dp), color = onMangoColor)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Heading Title
                Text(
                    text = "Hmm, we couldn’t verify you",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = inkColor,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.testTag("error_title")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subtitle
                Text(
                    text = subtitleText,
                    fontSize = 15.sp,
                    color = muteColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .testTag("error_subtitle")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Error Code Pill Badge
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = softColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(badColor)
                        )
                        Text(
                            text = "Error 401 · Unauthorized",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = inkColor
                        )
                    }
                }
            }

            // Bottom Section: Action Buttons & Help Link
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Try again Button (Mango Yellow Main CTA)
                Surface(
                    onClick = {
                        if (!isChecking) {
                            isChecking = true
                            coroutineScope.launch {
                                delay(1200)
                                isChecking = false
                                retryCount++
                                subtitleText = if (retryCount > 1) {
                                    "Still no luck. Logging out and back in usually fixes this."
                                } else {
                                    "Still can’t reach your account. Check your connection and retry."
                                }
                                onRetry()
                            }
                        }
                    },
                    color = mangoColor,
                    shape = RoundedCornerShape(99.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("retry_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = onMangoColor,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(if (isChecking) spinAngle else 0f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isChecking) "Checking…" else "Try again",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = onMangoColor
                        )
                    }
                }

                // Log out Ghost Button
                Surface(
                    onClick = onLogout,
                    color = Color.Transparent,
                    shape = RoundedCornerShape(99.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .border(2.dp, lineColor, RoundedCornerShape(99.dp))
                        .testTag("logout_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Log out",
                            tint = inkColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log out",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = inkColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer Help Link
                Row(
                    modifier = Modifier
                        .clickable { onContactSupport() }
                        .padding(vertical = 6.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Still stuck? ",
                        fontSize = 13.sp,
                        color = muteColor
                    )
                    Text(
                        text = "Contact support",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = inkColor
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningAlertIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF2B2118)) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 3.dp.toPx()

        val trianglePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.88f, h * 0.82f)
            lineTo(w * 0.12f, h * 0.82f)
            close()
        }

        drawPath(
            path = trianglePath,
            color = color.copy(alpha = 0.25f)
        )
        drawPath(
            path = trianglePath,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )

        // Draw warning exclamation mark inside triangle
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.38f),
            end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.6f),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        drawCircle(
            color = color,
            radius = strokeWidth * 0.7f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.71f)
        )
    }
}
