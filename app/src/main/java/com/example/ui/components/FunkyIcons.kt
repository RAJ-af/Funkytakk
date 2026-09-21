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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun FunkyHomeIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(24.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        val roofPath = Path().apply {
            moveTo(w * 0.16f, h * 0.44f)
            lineTo(w * 0.46f, h * 0.16f)
            quadraticTo(w * 0.5f, h * 0.12f, w * 0.54f, h * 0.16f)
            lineTo(w * 0.84f, h * 0.44f)
        }
        drawPath(
            path = roofPath,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val bodyPath = Path().apply {
            moveTo(w * 0.24f, h * 0.42f)
            lineTo(w * 0.24f, h * 0.82f)
            quadraticTo(w * 0.24f, h * 0.88f, w * 0.30f, h * 0.88f)
            lineTo(w * 0.70f, h * 0.88f)
            quadraticTo(w * 0.76f, h * 0.88f, w * 0.76f, h * 0.82f)
            lineTo(w * 0.76f, h * 0.42f)
        }
        drawPath(
            path = bodyPath,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val doorPath = Path().apply {
            moveTo(w * 0.42f, h * 0.88f)
            lineTo(w * 0.42f, h * 0.66f)
            quadraticTo(w * 0.50f, h * 0.60f, w * 0.58f, h * 0.66f)
            lineTo(w * 0.58f, h * 0.88f)
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
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        val capsuleWidth = w * 0.30f
        val capsuleHeight = h * 0.44f
        val capsuleLeft = (w - capsuleWidth) / 2f
        val capsuleTop = h * 0.14f
        drawRoundRect(
            color = color,
            topLeft = Offset(capsuleLeft, capsuleTop),
            size = Size(capsuleWidth, capsuleHeight),
            cornerRadius = CornerRadius(capsuleWidth / 2f, capsuleWidth / 2f),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val cradlePath = Path().apply {
            moveTo(w * 0.22f, h * 0.38f)
            cubicTo(
                w * 0.22f, h * 0.72f,
                w * 0.78f, h * 0.72f,
                w * 0.78f, h * 0.38f
            )
        }
        drawPath(
            path = cradlePath,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.68f),
            end = Offset(w * 0.5f, h * 0.86f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )

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
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        drawCircle(
            color = color,
            radius = w * 0.36f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = sw)
        )

        drawLine(
            color = color,
            start = Offset(w * 0.14f, h * 0.5f),
            end = Offset(w * 0.86f, h * 0.5f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )

        drawOval(
            color = color,
            topLeft = Offset(w * 0.33f, h * 0.14f),
            size = Size(w * 0.34f, h * 0.72f),
            style = Stroke(width = sw)
        )
    }
}

@Composable
fun FunkyChatIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(24.dp),
    isFilled: Boolean = false,
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        val chatPath = Path().apply {
            moveTo(w * 0.28f, h * 0.22f)
            lineTo(w * 0.72f, h * 0.22f)
            quadraticTo(w * 0.88f, h * 0.22f, w * 0.88f, h * 0.36f)
            lineTo(w * 0.88f, h * 0.60f)
            quadraticTo(w * 0.88f, h * 0.74f, w * 0.72f, h * 0.74f)
            lineTo(w * 0.44f, h * 0.74f)
            lineTo(w * 0.26f, h * 0.88f)
            quadraticTo(w * 0.22f, h * 0.90f, w * 0.22f, h * 0.82f)
            lineTo(w * 0.22f, h * 0.74f)
            quadraticTo(w * 0.12f, h * 0.74f, w * 0.12f, h * 0.60f)
            lineTo(w * 0.12f, h * 0.36f)
            quadraticTo(w * 0.12f, h * 0.22f, w * 0.28f, h * 0.22f)
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
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        drawCircle(
            color = color,
            radius = w * 0.17f,
            center = Offset(w * 0.5f, h * 0.32f),
            style = Stroke(width = sw)
        )

        val bodyPath = Path().apply {
            moveTo(w * 0.20f, h * 0.84f)
            cubicTo(
                w * 0.22f, h * 0.62f,
                w * 0.78f, h * 0.62f,
                w * 0.80f, h * 0.84f
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
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        drawCircle(
            color = color,
            radius = w * 0.27f,
            center = Offset(w * 0.42f, h * 0.42f),
            style = Stroke(width = sw)
        )

        drawLine(
            color = color,
            start = Offset(w * 0.61f, h * 0.61f),
            end = Offset(w * 0.84f, h * 0.84f),
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
fun FunkyVerticalMoreIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = h * 0.08f

        drawCircle(color = color, radius = r, center = Offset(w * 0.5f, h * 0.26f))
        drawCircle(color = color, radius = r, center = Offset(w * 0.5f, h * 0.50f))
        drawCircle(color = color, radius = r, center = Offset(w * 0.5f, h * 0.74f))
    }
}

@Composable
fun FunkyBackArrowIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(24.dp),
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        val arrowPath = Path().apply {
            moveTo(w * 0.50f, h * 0.28f)
            lineTo(w * 0.26f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.72f)
        }
        drawPath(
            path = arrowPath,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawLine(
            color = color,
            start = Offset(w * 0.26f, h * 0.50f),
            end = Offset(w * 0.76f, h * 0.50f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun FunkySendIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.20f)
            lineTo(w * 0.84f, h * 0.50f)
            lineTo(w * 0.22f, h * 0.80f)
            lineTo(w * 0.36f, h * 0.53f)
            lineTo(w * 0.58f, h * 0.50f)
            lineTo(w * 0.36f, h * 0.47f)
            close()
        }
        drawPath(path = path, color = color, style = Fill)
    }
}

@Composable
fun FunkyDoubleCheckIcon(
    color: Color = Color(0xFF6B7280),
    modifier: Modifier = Modifier.size(16.dp),
    strokeWidth: Float = 1.6f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        // First checkmark
        val p1 = Path().apply {
            moveTo(w * 0.12f, h * 0.52f)
            lineTo(w * 0.34f, h * 0.74f)
            lineTo(w * 0.68f, h * 0.32f)
        }
        drawPath(p1, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Second checkmark
        val p2 = Path().apply {
            moveTo(w * 0.42f, h * 0.74f)
            lineTo(w * 0.88f, h * 0.32f)
        }
        drawPath(p2, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun FunkyDownloadIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        // Arrow down
        drawLine(color, Offset(w * 0.5f, h * 0.22f), Offset(w * 0.5f, h * 0.64f), strokeWidth = sw, cap = StrokeCap.Round)
        val arrowHead = Path().apply {
            moveTo(w * 0.32f, h * 0.46f)
            lineTo(w * 0.50f, h * 0.64f)
            lineTo(w * 0.68f, h * 0.46f)
        }
        drawPath(arrowHead, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Bottom tray
        val tray = Path().apply {
            moveTo(w * 0.24f, h * 0.74f)
            lineTo(w * 0.24f, h * 0.82f)
            lineTo(w * 0.76f, h * 0.82f)
            lineTo(w * 0.76f, h * 0.74f)
        }
        drawPath(tray, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun FunkySmileyIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(22.dp),
    strokeWidth: Float = 1.6f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        // Face circle
        drawCircle(color, radius = w * 0.40f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = sw))

        // Eyes
        drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.36f, h * 0.40f), style = Fill)
        drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.64f, h * 0.40f), style = Fill)

        // Smile
        val smile = Path().apply {
            moveTo(w * 0.32f, h * 0.58f)
            quadraticTo(w * 0.50f, h * 0.74f, w * 0.68f, h * 0.58f)
        }
        drawPath(smile, color, style = Stroke(width = sw, cap = StrokeCap.Round))
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

@Composable
fun FunkyLightbulbIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        val bulb = Path().apply {
            moveTo(w * 0.35f, h * 0.65f)
            cubicTo(w * 0.15f, h * 0.50f, w * 0.20f, h * 0.18f, w * 0.50f, h * 0.18f)
            cubicTo(w * 0.80f, h * 0.18f, w * 0.85f, h * 0.50f, w * 0.65f, h * 0.65f)
            lineTo(w * 0.65f, h * 0.75f)
            lineTo(w * 0.35f, h * 0.75f)
            close()
        }
        drawPath(bulb, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawLine(color, Offset(w * 0.38f, h * 0.85f), Offset(w * 0.62f, h * 0.85f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun FunkyBookIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        val book = Path().apply {
            moveTo(w * 0.5f, h * 0.3f)
            cubicTo(w * 0.35f, h * 0.22f, w * 0.20f, h * 0.25f, w * 0.15f, h * 0.28f)
            lineTo(w * 0.15f, h * 0.78f)
            cubicTo(w * 0.20f, h * 0.75f, w * 0.35f, h * 0.72f, w * 0.50f, h * 0.80f)
            cubicTo(w * 0.65f, h * 0.72f, w * 0.80f, h * 0.75f, w * 0.85f, h * 0.78f)
            lineTo(w * 0.85f, h * 0.28f)
            cubicTo(w * 0.80f, h * 0.25f, w * 0.65f, h * 0.22f, w * 0.50f, h * 0.30f)
            close()
        }
        drawPath(book, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(w * 0.5f, h * 0.30f), Offset(w * 0.5f, h * 0.80f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun FunkyTranslateIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        // Top bar & vertical bar of '文'
        drawLine(color, Offset(w * 0.18f, h * 0.30f), Offset(w * 0.52f, h * 0.30f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.35f, h * 0.20f), Offset(w * 0.35f, h * 0.30f), strokeWidth = sw, cap = StrokeCap.Round)

        val strokeLeft = Path().apply {
            moveTo(w * 0.42f, h * 0.30f)
            quadraticTo(w * 0.35f, h * 0.50f, w * 0.18f, h * 0.62f)
        }
        drawPath(strokeLeft, color, style = Stroke(width = sw, cap = StrokeCap.Round))

        val strokeRight = Path().apply {
            moveTo(w * 0.28f, h * 0.38f)
            quadraticTo(w * 0.40f, h * 0.52f, w * 0.52f, h * 0.62f)
        }
        drawPath(strokeRight, color, style = Stroke(width = sw, cap = StrokeCap.Round))

        // 'A' letter on bottom right
        val letterA = Path().apply {
            moveTo(w * 0.60f, h * 0.80f)
            lineTo(w * 0.72f, h * 0.44f)
            lineTo(w * 0.84f, h * 0.80f)
        }
        drawPath(letterA, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(w * 0.64f, h * 0.68f), Offset(w * 0.80f, h * 0.68f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun FunkyReplyIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        val arrow = Path().apply {
            moveTo(w * 0.40f, h * 0.25f)
            lineTo(w * 0.20f, h * 0.45f)
            lineTo(w * 0.40f, h * 0.65f)
        }
        drawPath(arrow, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val curve = Path().apply {
            moveTo(w * 0.22f, h * 0.45f)
            lineTo(w * 0.55f, h * 0.45f)
            cubicTo(w * 0.75f, h * 0.45f, w * 0.82f, h * 0.58f, w * 0.82f, h * 0.75f)
        }
        drawPath(curve, color, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

@Composable
fun FunkyCopyIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        // Back card
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.32f, h * 0.18f),
            size = Size(w * 0.50f, h * 0.55f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Front card
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.18f, h * 0.32f),
            size = Size(w * 0.50f, h * 0.55f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun FunkyEditIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        val pencil = Path().apply {
            moveTo(w * 0.65f, h * 0.20f)
            lineTo(w * 0.80f, h * 0.35f)
            lineTo(w * 0.35f, h * 0.80f)
            lineTo(w * 0.20f, h * 0.80f)
            lineTo(w * 0.20f, h * 0.65f)
            close()
        }
        drawPath(pencil, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun FunkyForwardIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        val arrow = Path().apply {
            moveTo(w * 0.60f, h * 0.25f)
            lineTo(w * 0.80f, h * 0.45f)
            lineTo(w * 0.60f, h * 0.65f)
        }
        drawPath(arrow, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val curve = Path().apply {
            moveTo(w * 0.78f, h * 0.45f)
            lineTo(w * 0.45f, h * 0.45f)
            cubicTo(w * 0.25f, h * 0.45f, w * 0.18f, h * 0.58f, w * 0.18f, h * 0.75f)
        }
        drawPath(curve, color, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

@Composable
fun FunkyReportIcon(
    color: Color = Color(0xFFEF4444),
    modifier: Modifier = Modifier.size(20.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        // Pole
        drawLine(color, Offset(w * 0.22f, h * 0.15f), Offset(w * 0.22f, h * 0.85f), strokeWidth = sw, cap = StrokeCap.Round)

        // Flag body
        val flag = Path().apply {
            moveTo(w * 0.22f, h * 0.20f)
            lineTo(w * 0.78f, h * 0.35f)
            lineTo(w * 0.22f, h * 0.52f)
            close()
        }
        drawPath(flag, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun FunkyDeleteIcon(
    color: Color = Color(0xFFEF4444),
    modifier: Modifier = Modifier.size(20.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        // Lid
        drawLine(color, Offset(w * 0.20f, h * 0.28f), Offset(w * 0.80f, h * 0.28f), strokeWidth = sw, cap = StrokeCap.Round)
        val handle = Path().apply {
            moveTo(w * 0.38f, h * 0.28f)
            lineTo(w * 0.38f, h * 0.18f)
            lineTo(w * 0.62f, h * 0.18f)
            lineTo(w * 0.62f, h * 0.28f)
        }
        drawPath(handle, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Can
        val can = Path().apply {
            moveTo(w * 0.26f, h * 0.28f)
            lineTo(w * 0.30f, h * 0.82f)
            quadraticTo(w * 0.31f, h * 0.86f, w * 0.36f, h * 0.86f)
            lineTo(w * 0.64f, h * 0.86f)
            quadraticTo(w * 0.69f, h * 0.86f, w * 0.70f, h * 0.82f)
            lineTo(w * 0.74f, h * 0.28f)
        }
        drawPath(can, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Vertical ribs
        drawLine(color, Offset(w * 0.42f, h * 0.38f), Offset(w * 0.42f, h * 0.74f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.58f, h * 0.38f), Offset(w * 0.58f, h * 0.74f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun FunkyPlayIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(16.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val play = Path().apply {
            moveTo(w * 0.28f, h * 0.20f)
            lineTo(w * 0.82f, h * 0.50f)
            lineTo(w * 0.28f, h * 0.80f)
            close()
        }
        drawPath(play, color, style = Fill)
    }
}

@Composable
fun FunkyPauseIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(16.dp),
    strokeWidth: Float = 2.5f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        drawLine(color, Offset(w * 0.35f, h * 0.22f), Offset(w * 0.35f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.65f, h * 0.22f), Offset(w * 0.65f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun FunkyCloseIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(16.dp),
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        drawLine(color, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.22f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun FunkyStopSquareIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier.size(16.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.25f, h * 0.25f),
            size = Size(w * 0.50f, h * 0.50f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Fill
        )
    }
}

@Composable
fun FunkyLockIcon(
    color: Color = Color(0xFF6B7280),
    modifier: Modifier = Modifier.size(14.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        // Shackle
        val shackle = Path().apply {
            moveTo(w * 0.30f, h * 0.45f)
            lineTo(w * 0.30f, h * 0.28f)
            cubicTo(w * 0.30f, h * 0.12f, w * 0.70f, h * 0.12f, w * 0.70f, h * 0.28f)
            lineTo(w * 0.70f, h * 0.45f)
        }
        drawPath(shackle, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))

        // Body
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.20f, h * 0.42f),
            size = Size(w * 0.60f, h * 0.48f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Keyhole dot
        drawCircle(color = color, radius = sw * 0.8f, center = Offset(w * 0.50f, h * 0.62f))
    }
}

@Composable
fun FunkyPlusIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp),
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        drawLine(color, Offset(w * 0.50f, h * 0.22f), Offset(w * 0.50f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.22f, h * 0.50f), Offset(w * 0.78f, h * 0.50f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun FunkyChevronRightIcon(
    color: Color = Color(0xFF6B7280),
    modifier: Modifier = Modifier.size(16.dp),
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.25f)
            lineTo(w * 0.65f, h * 0.50f)
            lineTo(w * 0.35f, h * 0.75f)
        }
        drawPath(path, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun FunkyDiscoverIcon(
    color: Color = Color.Black,
    isFilled: Boolean = false,
    modifier: Modifier = Modifier.size(24.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        if (isFilled) {
            // Main user head
            drawCircle(
                color = color,
                radius = w * 0.16f,
                center = Offset(w * 0.40f, h * 0.30f)
            )
            // Main user body
            val mainBody = Path().apply {
                moveTo(w * 0.14f, h * 0.82f)
                cubicTo(w * 0.15f, h * 0.58f, w * 0.65f, h * 0.58f, w * 0.66f, h * 0.82f)
                close()
            }
            drawPath(mainBody, color = color)

            // Companion user head (behind, to the right)
            drawCircle(
                color = color,
                radius = w * 0.12f,
                center = Offset(w * 0.72f, h * 0.36f)
            )
            // Companion user body
            val companionBody = Path().apply {
                moveTo(w * 0.56f, h * 0.82f)
                cubicTo(w * 0.58f, h * 0.64f, w * 0.90f, h * 0.64f, w * 0.92f, h * 0.82f)
                close()
            }
            drawPath(companionBody, color = color)
        } else {
            // Main user head
            drawCircle(
                color = color,
                radius = w * 0.16f,
                center = Offset(w * 0.40f, h * 0.30f),
                style = Stroke(width = sw)
            )
            // Main user body
            val mainBody = Path().apply {
                moveTo(w * 0.15f, h * 0.82f)
                cubicTo(w * 0.16f, h * 0.60f, w * 0.64f, h * 0.60f, w * 0.65f, h * 0.82f)
            }
            drawPath(mainBody, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Companion user head
            drawCircle(
                color = color,
                radius = w * 0.12f,
                center = Offset(w * 0.72f, h * 0.36f),
                style = Stroke(width = sw)
            )
            // Companion user body
            val companionBody = Path().apply {
                moveTo(w * 0.60f, h * 0.74f)
                cubicTo(w * 0.65f, h * 0.64f, w * 0.87f, h * 0.64f, w * 0.91f, h * 0.82f)
            }
            drawPath(companionBody, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun FunkySlidersIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(20.dp),
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        // Line 1 (top track)
        drawLine(color, Offset(w * 0.15f, h * 0.30f), Offset(w * 0.85f, h * 0.30f), strokeWidth = sw, cap = StrokeCap.Round)
        // Knob 1
        drawCircle(color = Color.White, radius = w * 0.12f, center = Offset(w * 0.38f, h * 0.30f))
        drawCircle(color = color, radius = w * 0.12f, center = Offset(w * 0.38f, h * 0.30f), style = Stroke(width = sw))

        // Line 2 (bottom track)
        drawLine(color, Offset(w * 0.15f, h * 0.70f), Offset(w * 0.85f, h * 0.70f), strokeWidth = sw, cap = StrokeCap.Round)
        // Knob 2
        drawCircle(color = Color.White, radius = w * 0.12f, center = Offset(w * 0.65f, h * 0.70f))
        drawCircle(color = color, radius = w * 0.12f, center = Offset(w * 0.65f, h * 0.70f), style = Stroke(width = sw))
    }
}

@Composable
fun FunkyWaveHandIcon(
    color: Color = Color.Black,
    modifier: Modifier = Modifier.size(16.dp),
    strokeWidth: Float = 1.6f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth * density

        val handPath = Path().apply {
            moveTo(w * 0.38f, h * 0.88f)
            lineTo(w * 0.58f, h * 0.88f)
            cubicTo(w * 0.64f, h * 0.80f, w * 0.74f, h * 0.64f, w * 0.76f, h * 0.48f)
            cubicTo(w * 0.76f, h * 0.40f, w * 0.68f, h * 0.40f, w * 0.66f, h * 0.48f)
            cubicTo(w * 0.66f, h * 0.34f, w * 0.58f, h * 0.34f, w * 0.56f, h * 0.45f)
            cubicTo(w * 0.56f, h * 0.26f, w * 0.48f, h * 0.26f, w * 0.47f, h * 0.42f)
            cubicTo(w * 0.47f, h * 0.32f, w * 0.40f, h * 0.32f, w * 0.39f, h * 0.50f)
            cubicTo(w * 0.33f, h * 0.54f, w * 0.24f, h * 0.60f, w * 0.24f, h * 0.68f)
            cubicTo(w * 0.24f, h * 0.74f, w * 0.32f, h * 0.76f, w * 0.36f, h * 0.74f)
            lineTo(w * 0.38f, h * 0.88f)
            close()
        }
        drawPath(
            path = handPath,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Wave marks
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(w * 0.12f, h * 0.20f),
            size = androidx.compose.ui.geometry.Size(w * 0.35f, h * 0.35f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = 195f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = Offset(w * 0.02f, h * 0.10f),
            size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.52f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
    }
}




