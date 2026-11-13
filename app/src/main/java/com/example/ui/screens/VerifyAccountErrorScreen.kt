package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 401 Session Verification Failed screen built to match Verify.dc_0b7f08e7.html.
 * Uses neo-brutalist comic cards & custom vector Canvas artwork with zero Material Icons.
 */
@Composable
fun VerifyAccountErrorScreen(
    onRetry: () -> Unit = {},
    onLogout: () -> Unit = {},
    onContactSupport: () -> Unit = {},
    onClose: () -> Unit = onLogout
) {
    val isDark = LocalAppDarkTheme.current
    var retryCount by remember { mutableIntStateOf(0) }
    var isChecking by remember { mutableStateOf(false) }
    var subtitleText by remember {
        mutableStateOf("Your session may have expired or the connection dropped. Give it another go.")
    }
    val coroutineScope = rememberCoroutineScope()

    // Exact palette from Verify.dc_0b7f08e7.html
    val bgColor = if (isDark) Color(0xFF1B120B) else Color(0xFFFFF4D6)
    val surfaceColor = if (isDark) Color(0xFF2A1D12) else Color(0xFFFFFBEF)
    val inkColor = if (isDark) Color(0xFFFFF1D2) else Color(0xFF2A1B0E)
    val mutedColor = if (isDark) Color(0xFFC2AC8C) else Color(0xFF6F5B45)
    val borderColor = if (isDark) Color(0xFF5A4228) else Color(0xFF2A1B0E)
    val shadowColor = if (isDark) Color(0xFF000000) else Color(0xFF2A1B0E)

    val mangoColor = Color(0xFFFFB400)
    val onMangoColor = Color(0xFF2A1B0E)
    val coralBg = Color(0xFFFFD9D3)
    val coralInk = Color(0xFF9A2417)

    val infiniteTransition = rememberInfiniteTransition(label = "verify_badge_anim")
    val gentleFloat by infiniteTransition.animateFloat(
        initialValue = -7.5f,
        targetValue = -4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing)
        ),
        label = "spin"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("verify_account_error_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Close Button Top-Right (.ib 44dp circular)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = surfaceColor,
                    border = BorderStroke(2.dp, borderColor),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("verify_close_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CloseCrossVector(
                            color = inkColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Center Hero Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // -6deg Tilted 150.dp Mango Broken-Lock Card with hard offset shadow
                Box(
                    modifier = Modifier
                        .size(156.dp)
                        .rotate(gentleFloat),
                    contentAlignment = Alignment.Center
                ) {
                    // Hard offset comic shadow (+6dp, +6dp)
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .offset(x = 6.dp, y = 6.dp)
                            .background(shadowColor, RoundedCornerShape(44.dp))
                    )
                    // Mango Card with 3dp border
                    Surface(
                        shape = RoundedCornerShape(44.dp),
                        color = mangoColor,
                        border = BorderStroke(3.dp, Color(0xFF2A1B0E)),
                        modifier = Modifier.size(150.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            BrokenLockVector(
                                color = onMangoColor,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Hmm, we couldn’t\nverify you",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 35.sp,
                    letterSpacing = (-0.6).sp,
                    color = inkColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("error_title")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = subtitleText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp,
                    color = mutedColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 310.dp)
                        .testTag("error_subtitle")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Error 401 · Unauthorized coral badge
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = coralBg,
                    border = BorderStroke(2.dp, coralInk)
                ) {
                    Text(
                        text = "Error 401 · Unauthorized",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = coralInk,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Bottom Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NeoBrutalistButton(
                    text = if (isChecking) "Checking…" else "Try again",
                    bgColor = mangoColor,
                    textColor = onMangoColor,
                    borderColor = Color(0xFF2A1B0E),
                    shadowColor = Color(0xFF2A1B0E),
                    isSpinning = isChecking,
                    spinAngle = spinAngle,
                    onClick = {
                        if (!isChecking) {
                            isChecking = true
                            coroutineScope.launch {
                                onRetry()
                                delay(1400)
                                isChecking = false
                                retryCount++
                                subtitleText = if (retryCount > 1) {
                                    "Still no luck. Logging out and back in usually fixes this."
                                } else {
                                    "Still can’t reach your account. Check your connection and retry."
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("retry_button")
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeoBrutalistButton(
                    text = "Log out",
                    bgColor = surfaceColor,
                    textColor = inkColor,
                    borderColor = borderColor,
                    shadowColor = shadowColor,
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logout_button")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = mutedColor, fontWeight = FontWeight.SemiBold)) {
                            append("Still stuck? ")
                        }
                        withStyle(
                            SpanStyle(
                                color = inkColor,
                                fontWeight = FontWeight.ExtraBold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("Contact support")
                        }
                    },
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { onContactSupport() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("contact_support_link")
                )
            }
        }
    }
}

@Composable
private fun NeoBrutalistButton(
    text: String,
    bgColor: Color,
    textColor: Color,
    borderColor: Color,
    shadowColor: Color,
    isSpinning: Boolean = false,
    spinAngle: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shift = if (isPressed) 2.dp else 0.dp
    val shadowShift = if (isPressed) 1.dp else 3.dp

    Box(modifier = modifier.height(59.dp)) {
        // Hard bottom-right offset shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset(x = shadowShift, y = shadowShift)
                .background(shadowColor, RoundedCornerShape(18.dp))
        )
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(2.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset(x = shift, y = shift)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSpinning) {
                    SpinnerArcVector(
                        color = textColor,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(spinAngle)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun CloseCrossVector(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val p = size.width * 0.25f
        val q = size.width * 0.75f
        drawLine(color, Offset(p, p), Offset(q, q), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color, Offset(q, p), Offset(p, q), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
private fun SpinnerArcVector(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 260f,
            useCenter = false,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Custom Canvas Broken Lock matching the exact SVG in Verify.dc_0b7f08e7.html:
 * rect(5,11,14,9,rx=2) + shackle path + diagonal strike-through (4,4 -> 20,20)
 */
@Composable
private fun BrokenLockVector(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val sw = (2.4f * sx).coerceAtLeast(2.2.dp.toPx())
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Lock body rect(5, 11, 14, 9, rx=2)
        drawRoundRect(
            color = color,
            topLeft = Offset(5f * sx, 11f * sy),
            size = Size(14f * sx, 9f * sy),
            cornerRadius = CornerRadius(2f * sx, 2f * sy),
            style = stroke
        )

        // Shackle path: M8 11V8a4 4 0 0 1 7.5 -2
        val shackle = Path().apply {
            moveTo(8f * sx, 11f * sy)
            lineTo(8f * sx, 8f * sy)
            cubicTo(
                8f * sx, 5.4f * sy,
                10.1f * sx, 4f * sy,
                12f * sx, 4f * sy
            )
            cubicTo(
                13.7f * sx, 4f * sy,
                15f * sx, 4.9f * sy,
                15.5f * sx, 6f * sy
            )
        }
        drawPath(shackle, color = color, style = stroke)

        // Diagonal slash: M4 4 L20 20 (with subtle mango gap shadow first so it reads cleanly)
        drawLine(
            color = Color(0xFFFFB400),
            start = Offset(4f * sx, 3.5f * sy),
            end = Offset(20f * sx, 19.5f * sy),
            strokeWidth = sw * 1.9f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(4f * sx, 4f * sy),
            end = Offset(20f * sx, 20f * sy),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}
