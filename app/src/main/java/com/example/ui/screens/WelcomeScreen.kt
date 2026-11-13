package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.annotation.DrawableRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFFAFAEE) // Warm off-white / cream background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. LOGO: FunkyTalk branding (Black + Yellow, no extra emoji)
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFF141414), fontWeight = FontWeight.Black)) {
                        append("Funky")
                    }
                    withStyle(style = SpanStyle(color = Color(0xFFF6C945), fontWeight = FontWeight.Black)) {
                        append("Talk")
                    }
                },
                fontSize = 24.sp,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.testTag("welcome_app_title")
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2. HEADLINE (Large, bold, highly readable, natural line wrapping)
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFF141414))) {
                        append("Learn a new language\nand get a ")
                    }
                    withStyle(style = SpanStyle(color = Color(0xFFF6C945))) {
                        append("new soul.")
                    }
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 39.sp,
                letterSpacing = (-1.0).sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. SUBTITLE (Exact requested copy)
            Text(
                text = "Connect with real people, practice naturally and discover new cultures.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF5A5A5A),
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. FEATURE ROW: Chat | Make Friends | Explore (Compact & subtle line icons, no emojis)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubtleFeatureChip(
                    iconRes = R.drawable.ic_comment,
                    label = "Chat",
                    backgroundColor = Color(0xFFFFF9E4),
                    modifier = Modifier.weight(1f)
                )
                SubtleFeatureChip(
                    iconRes = R.drawable.ic_discover,
                    label = "Make Friends",
                    backgroundColor = Color(0xFFFDF0F2),
                    modifier = Modifier.weight(1.35f)
                )
                SubtleFeatureChip(
                    iconRes = R.drawable.ic_globe,
                    label = "Explore",
                    backgroundColor = Color(0xFFF0F4FF),
                    modifier = Modifier.weight(1.15f)
                )
            }

            // 5. HERO IMAGE: Directly below feature row without huge gap
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.a_cutout_png_style_portrait_image_on_a_transparent),
                    contentDescription = "FunkyTalk members connecting",
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. SLIDE TO CONNECT CTA
            SlideToConnectButton(
                onConnected = onGetStartedClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("get_started_button")
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SubtleFeatureChip(
    @DrawableRes iconRes: Int,
    label: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color(0xFF2C2C2C),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2C2C2C),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SlideToConnectButton(
    onConnected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val thumbSizeDp = 48.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }

    val offsetX = remember { Animatable(0f) }
    val maxDragPx = (trackWidthPx - thumbSizePx).coerceAtLeast(0f)

    var isLockedState by remember { mutableStateOf(false) }

    val progress = if (maxDragPx > 0f) (offsetX.value / maxDragPx).coerceIn(0f, 1f) else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_arrow")
    val chevronAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chevron_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(29.dp))
            .clip(RoundedCornerShape(29.dp))
            .background(Color(0xFFF6C945)) // FunkyTalk Yellow
            .onGloballyPositioned { coordinates ->
                trackWidthPx = coordinates.size.width.toFloat()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Center label & right »» indicator
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 56.dp, end = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isLockedState || progress >= 0.85f) "Let's Connect" else "Slide to Connect",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = (-0.2).sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(if (isLockedState) 1f else (1f - progress * 0.7f).coerceIn(0.2f, 1f))
                    .offset { IntOffset((progress * 15).roundToInt(), 0) }
            )

            // Right side subtle »» indicator
            Text(
                text = "»»",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .alpha(((chevronAlpha + progress * 0.5f).coerceIn(0.3f, 1f)) * (1f - (if (isLockedState) 1f else 0f)))
            )
        }

        // White circular draggable thumb on LEFT
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .padding(5.dp)
                .size(thumbSizeDp)
                .shadow(elevation = 3.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(maxDragPx) {
                    if (isLockedState) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (maxDragPx > 0f && offsetX.value >= maxDragPx * 0.85f) {
                                    isLockedState = true
                                    try {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    } catch (_: Exception) {}
                                    offsetX.animateTo(
                                        targetValue = maxDragPx,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    delay(200)
                                    onConnected()
                                } else {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f)
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (!isLockedState) {
                                coroutineScope.launch {
                                    val target = (offsetX.value + dragAmount).coerceIn(0f, maxDragPx)
                                    offsetX.snapTo(target)
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = "Slide to Connect",
                tint = Color.Black,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}



