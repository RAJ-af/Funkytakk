package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MoonCrescentCanvas(
    modifier: Modifier = Modifier,
    moonColor: Color = Color(0xFFFFD43B)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.75f, h * 0.12f)
            cubicTo(w * 0.35f, h * 0.12f, w * 0.12f, h * 0.38f, w * 0.12f, h * 0.68f)
            cubicTo(w * 0.12f, h * 0.94f, w * 0.32f, h * 1.0f, w * 0.48f, h * 0.98f)
            cubicTo(w * 0.28f, h * 0.82f, w * 0.24f, h * 0.52f, w * 0.45f, h * 0.32f)
            cubicTo(w * 0.54f, h * 0.22f, w * 0.68f, h * 0.16f, w * 0.75f, h * 0.12f)
            close()
        }
        drawPath(path, color = moonColor)

        // Draw a tiny night sparkle star beside the moon
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = w * 0.08f,
            center = Offset(w * 0.72f, h * 0.65f)
        )
    }
}

@Composable
fun OfflineMoonBadge(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 22.dp,
    moonSize: Dp = 13.dp
) {
    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(Color(0xFF0F172A)) // Deep midnight navy background
            .border(1.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        MoonCrescentCanvas(
            modifier = Modifier.size(moonSize),
            moonColor = Color(0xFFFFD43B)
        )
    }
}
