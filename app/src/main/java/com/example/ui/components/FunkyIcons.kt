package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FunkyHomeIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(24.dp),
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        val roofPath = Path().apply {
            moveTo(w * 0.15f, h * 0.48f)
            lineTo(w * 0.5f, h * 0.16f)
            lineTo(w * 0.85f, h * 0.48f)
        }
        drawPath(
            path = roofPath,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val bodyPath = Path().apply {
            moveTo(w * 0.24f, h * 0.44f)
            lineTo(w * 0.24f, h * 0.86f)
            lineTo(w * 0.76f, h * 0.86f)
            lineTo(w * 0.76f, h * 0.44f)
        }
        drawPath(
            path = bodyPath,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val doorPath = Path().apply {
            moveTo(w * 0.41f, h * 0.86f)
            lineTo(w * 0.41f, h * 0.62f)
            lineTo(w * 0.59f, h * 0.62f)
            lineTo(w * 0.59f, h * 0.86f)
        }
        drawPath(
            path = doorPath,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun FunkyMicIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(24.dp),
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        // Mic capsule
        val capsuleWidth = w * 0.32f
        val capsuleHeight = h * 0.46f
        val capsuleLeft = (w - capsuleWidth) / 2f
        val capsuleTop = h * 0.14f
        drawRoundRect(
            color = color,
            topLeft = Offset(capsuleLeft, capsuleTop),
            size = Size(capsuleWidth, capsuleHeight),
            cornerRadius = CornerRadius(capsuleWidth / 2f, capsuleWidth / 2f),
            style = Stroke(width = sw)
        )

        // Mic cradle arc
        val cradlePath = Path().apply {
            moveTo(w * 0.22f, h * 0.40f)
            cubicTo(
                w * 0.22f, h * 0.74f,
                w * 0.78f, h * 0.74f,
                w * 0.78f, h * 0.40f
            )
        }
        drawPath(
            path = cradlePath,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Stem
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.69f),
            end = Offset(w * 0.5f, h * 0.86f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )

        // Base foot
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.86f),
            end = Offset(w * 0.66f, h * 0.86f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun FunkyGlobeIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(24.dp),
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        // Outer circle
        drawCircle(
            color = color,
            radius = w * 0.38f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = sw)
        )

        // Equator
        drawLine(
            color = color,
            start = Offset(w * 0.12f, h * 0.5f),
            end = Offset(w * 0.88f, h * 0.5f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )

        // Vertical meridian ellipse
        drawOval(
            color = color,
            topLeft = Offset(w * 0.31f, h * 0.12f),
            size = Size(w * 0.38f, h * 0.76f),
            style = Stroke(width = sw)
        )
    }
}

@Composable
fun FunkyChatIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(24.dp),
    isFilled: Boolean = false,
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        val chatPath = Path().apply {
            moveTo(w * 0.22f, h * 0.22f)
            lineTo(w * 0.78f, h * 0.22f)
            quadraticTo(w * 0.88f, h * 0.22f, w * 0.88f, h * 0.34f)
            lineTo(w * 0.88f, h * 0.62f)
            quadraticTo(w * 0.88f, h * 0.74f, w * 0.78f, h * 0.74f)
            lineTo(w * 0.42f, h * 0.74f)
            lineTo(w * 0.24f, h * 0.88f)
            lineTo(w * 0.24f, h * 0.74f)
            quadraticTo(w * 0.12f, h * 0.74f, w * 0.12f, h * 0.62f)
            lineTo(w * 0.12f, h * 0.34f)
            quadraticTo(w * 0.12f, h * 0.22f, w * 0.22f, h * 0.22f)
            close()
        }

        if (isFilled) {
            drawPath(path = chatPath, color = color, style = Fill)
        } else {
            drawPath(
                path = chatPath,
                color = color,
                style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun FunkyProfileIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(24.dp),
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        // Head
        drawCircle(
            color = color,
            radius = w * 0.18f,
            center = Offset(w * 0.5f, h * 0.32f),
            style = Stroke(width = sw)
        )

        // Shoulders arc
        val bodyPath = Path().apply {
            moveTo(w * 0.18f, h * 0.86f)
            cubicTo(
                w * 0.20f, h * 0.62f,
                w * 0.80f, h * 0.62f,
                w * 0.82f, h * 0.86f
            )
        }
        drawPath(
            path = bodyPath,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun FunkySearchIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp),
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        drawCircle(
            color = color,
            radius = w * 0.28f,
            center = Offset(w * 0.42f, h * 0.42f),
            style = Stroke(width = sw)
        )

        drawLine(
            color = color,
            start = Offset(w * 0.62f, h * 0.62f),
            end = Offset(w * 0.86f, h * 0.86f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun FunkyMoreIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = w * 0.08f

        drawCircle(color = color, radius = r, center = Offset(w * 0.26f, h * 0.5f))
        drawCircle(color = color, radius = r, center = Offset(w * 0.50f, h * 0.5f))
        drawCircle(color = color, radius = r, center = Offset(w * 0.74f, h * 0.5f))
    }
}

@Composable
fun FunkyHeartIcon(
    color: Color = Color(0xFFFFC529),
    modifier: Modifier = Modifier.size(16.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.85f)
            cubicTo(w * 0.15f, h * 0.60f, w * 0.05f, h * 0.35f, w * 0.15f, h * 0.18f)
            cubicTo(w * 0.25f, h * 0.05f, w * 0.42f, h * 0.08f, w * 0.5f, h * 0.24f)
            cubicTo(w * 0.58f, h * 0.08f, w * 0.75f, h * 0.05f, w * 0.85f, h * 0.18f)
            cubicTo(w * 0.95f, h * 0.35f, w * 0.85f, h * 0.60f, w * 0.5f, h * 0.85f)
            close()
        }
        drawPath(path = path, color = color, style = Fill)
    }
}
