package com.example.ui.screens

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppDarkTheme

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppDarkTheme.current
    val bgColor = if (isDark) Color(0xFF16120D) else Color(0xFFFFF8E7)
    val inkColor = if (isDark) Color(0xFFFFF3DC) else Color(0xFF2B2118)
    val mangoColor = Color(0xFFFFB627)

    val infiniteTransition = rememberInfiniteTransition(label = "splash_dot_bounce")

    val jumpDp by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0f at 0 using FastOutSlowInEasing
                -16f at 360 using LinearOutSlowInEasing
                0f at 800 using FastOutLinearInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "splash_jump"
    )

    val scaleX by infiniteTransition.animateFloat(
        initialValue = 1.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                1.15f at 0
                0.92f at 360
                1.15f at 800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "splash_scale_x"
    )

    val scaleY by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0.85f at 0
                1.08f at 360
                0.85f at 800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "splash_scale_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = inkColor,
                            fontWeight = FontWeight.Black
                        )
                    ) {
                        append("Funky")
                    }
                    withStyle(
                        SpanStyle(
                            color = mangoColor,
                            fontWeight = FontWeight.Black
                        )
                    ) {
                        append("Talk")
                    }
                },
                fontSize = 40.sp,
                letterSpacing = (-1.6).sp
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .offset(y = jumpDp.dp)
                    .graphicsLayer {
                        this.scaleX = scaleX
                        this.scaleY = scaleY
                        this.transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(mangoColor)
            )
        }
    }
}
