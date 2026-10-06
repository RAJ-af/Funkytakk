package com.example.ui.chat

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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom handcrafted canvas-drawn vector icons for the chat system.
 * Zero dependency on external Material Icons sets.
 */
object ChatIcons

@Composable
fun ChatIconBack(modifier: Modifier = Modifier.size(24.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 2.4.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.72f, h * 0.20f)
            lineTo(w * 0.32f, h * 0.50f)
            lineTo(w * 0.72f, h * 0.80f)
        }
        drawPath(path, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ChatIconSend(modifier: Modifier = Modifier.size(22.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.15f)
            lineTo(w * 0.90f, h * 0.50f)
            lineTo(w * 0.15f, h * 0.85f)
            lineTo(w * 0.32f, h * 0.50f)
            close()
        }
        drawPath(path, tint, style = Fill)
    }
}

@Composable
fun ChatIconMic(modifier: Modifier = Modifier.size(22.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 2.0.dp.toPx()
        // Mic capsule
        val capW = w * 0.36f
        val capH = h * 0.50f
        val left = (w - capW) / 2f
        drawRoundRect(
            color = tint,
            topLeft = Offset(left, h * 0.12f),
            size = Size(capW, capH),
            cornerRadius = CornerRadius(capW / 2f, capW / 2f),
            style = Stroke(width = sw)
        )
        // Stand arc
        val arcPath = Path().apply {
            moveTo(w * 0.20f, h * 0.42f)
            cubicTo(w * 0.20f, h * 0.72f, w * 0.80f, h * 0.72f, w * 0.80f, h * 0.42f)
        }
        drawPath(arcPath, tint, style = Stroke(width = sw, cap = StrokeCap.Round))
        // Base line
        drawLine(tint, Offset(w * 0.50f, h * 0.72f), Offset(w * 0.50f, h * 0.88f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.32f, h * 0.88f), Offset(w * 0.68f, h * 0.88f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun ChatIconAttach(modifier: Modifier = Modifier.size(22.dp), tint: Color = Color(0xFF6B6B66)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.9.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.45f, h * 0.75f)
            lineTo(w * 0.72f, h * 0.48f)
            cubicTo(w * 0.88f, h * 0.32f, w * 0.76f, h * 0.18f, w * 0.62f, h * 0.18f)
            cubicTo(w * 0.54f, h * 0.18f, w * 0.46f, h * 0.24f, w * 0.40f, h * 0.30f)
            lineTo(w * 0.20f, h * 0.50f)
            cubicTo(w * 0.08f, h * 0.62f, w * 0.08f, h * 0.82f, w * 0.22f, h * 0.92f)
            cubicTo(w * 0.36f, h * 1.02f, w * 0.56f, h * 0.98f, w * 0.70f, h * 0.84f)
            lineTo(w * 0.84f, h * 0.70f)
        }
        drawPath(path, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ChatIconEmoji(modifier: Modifier = Modifier.size(22.dp), tint: Color = Color(0xFF6B6B66)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.9.dp.toPx()
        // Circle face
        drawCircle(color = tint, radius = w * 0.42f, center = Offset(w / 2f, h / 2f), style = Stroke(width = sw))
        // Eyes
        drawCircle(color = tint, radius = 1.6.dp.toPx(), center = Offset(w * 0.36f, h * 0.40f))
        drawCircle(color = tint, radius = 1.6.dp.toPx(), center = Offset(w * 0.64f, h * 0.40f))
        // Smile
        val smile = Path().apply {
            moveTo(w * 0.32f, h * 0.60f)
            cubicTo(w * 0.40f, h * 0.75f, w * 0.60f, h * 0.75f, w * 0.68f, h * 0.60f)
        }
        drawPath(smile, tint, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

@Composable
fun ChatIconPlay(modifier: Modifier = Modifier.size(18.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.28f, h * 0.16f)
            lineTo(w * 0.86f, h * 0.50f)
            lineTo(w * 0.28f, h * 0.84f)
            close()
        }
        drawPath(path, tint, style = Fill)
    }
}

@Composable
fun ChatIconPause(modifier: Modifier = Modifier.size(18.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = w * 0.24f
        val cr = CornerRadius(2.dp.toPx())
        drawRoundRect(tint, Offset(w * 0.18f, h * 0.15f), Size(barW, h * 0.70f), cr)
        drawRoundRect(tint, Offset(w * 0.58f, h * 0.15f), Size(barW, h * 0.70f), cr)
    }
}

@Composable
fun ChatIconDoubleTick(modifier: Modifier = Modifier.size(16.dp), tint: Color = Color(0xFF2F6FDE)) {
    Canvas(modifier = modifier) {
        val s = size.height / 11f
        val sw = 1.6.dp.toPx()
        fun drawCheck(dx: Float) {
            val p = Path().apply {
                moveTo(dx + 1f * s, 6f * s)
                lineTo(dx + 4.2f * s, 9.2f * s)
                lineTo(dx + 10.2f * s, 2f * s)
            }
            drawPath(p, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawCheck(0f)
        drawCheck(5f * s)
    }
}

@Composable
fun ChatIconSingleTick(modifier: Modifier = Modifier.size(12.dp), tint: Color = Color(0xFF6B6B66)) {
    Canvas(modifier = modifier) {
        val s = size.height / 11f
        val sw = 1.6.dp.toPx()
        val p = Path().apply {
            moveTo(1f * s, 6f * s)
            lineTo(4.2f * s, 9.2f * s)
            lineTo(10.2f * s, 2f * s)
        }
        drawPath(p, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ChatIconLock(modifier: Modifier = Modifier.size(14.dp), tint: Color = Color(0xFF6B6B66)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.6.dp.toPx()
        // Shackle
        val shackleW = w * 0.44f
        val shackleH = h * 0.38f
        val shackleLeft = (w - shackleW) / 2f
        drawRoundRect(
            color = tint,
            topLeft = Offset(shackleLeft, h * 0.10f),
            size = Size(shackleW, shackleH),
            cornerRadius = CornerRadius(shackleW / 2f, shackleW / 2f),
            style = Stroke(width = sw)
        )
        // Body
        val bodyW = w * 0.76f
        val bodyH = h * 0.52f
        val bodyLeft = (w - bodyW) / 2f
        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, h * 0.42f),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Fill
        )
    }
}

@Composable
fun ChatIconSparkles(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFF2F6FDE)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        fun drawStar(cx: Float, cy: Float, r: Float) {
            val p = Path().apply {
                moveTo(cx, cy - r)
                cubicTo(cx, cy, cx, cy, cx + r, cy)
                cubicTo(cx, cy, cx, cy, cx, cy + r)
                cubicTo(cx, cy, cx, cy, cx - r, cy)
                cubicTo(cx, cy, cx, cy, cx, cy - r)
            }
            drawPath(p, tint, style = Fill)
        }
        drawStar(w * 0.50f, h * 0.45f, w * 0.38f)
        drawStar(w * 0.80f, h * 0.20f, w * 0.16f)
    }
}

@Composable
fun ChatIconClose(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 2.0.dp.toPx()
        drawLine(tint, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.22f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun ChatIconTrash(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFFD92D20)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.8.dp.toPx()
        // Lid
        drawLine(tint, Offset(w * 0.18f, h * 0.26f), Offset(w * 0.82f, h * 0.26f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.38f, h * 0.16f), Offset(w * 0.62f, h * 0.16f), strokeWidth = sw, cap = StrokeCap.Round)
        // Bin
        val body = Path().apply {
            moveTo(w * 0.26f, h * 0.26f)
            lineTo(w * 0.32f, h * 0.86f)
            cubicTo(w * 0.32f, h * 0.90f, w * 0.36f, h * 0.90f, w * 0.40f, h * 0.90f)
            lineTo(w * 0.60f, h * 0.90f)
            cubicTo(w * 0.64f, h * 0.90f, w * 0.68f, h * 0.90f, w * 0.68f, h * 0.86f)
            lineTo(w * 0.74f, h * 0.26f)
        }
        drawPath(body, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Lines inside
        drawLine(tint, Offset(w * 0.42f, h * 0.38f), Offset(w * 0.42f, h * 0.78f), strokeWidth = sw)
        drawLine(tint, Offset(w * 0.58f, h * 0.38f), Offset(w * 0.58f, h * 0.78f), strokeWidth = sw)
    }
}

@Composable
fun ChatIconSettings(modifier: Modifier = Modifier.size(22.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.9.dp.toPx()
        drawCircle(tint, radius = w * 0.20f, center = Offset(w / 2f, h / 2f), style = Stroke(width = sw))
        drawCircle(tint, radius = w * 0.40f, center = Offset(w / 2f, h / 2f), style = Stroke(width = sw))
    }
}

@Composable
fun ChatIconAddUser(modifier: Modifier = Modifier.size(22.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.9.dp.toPx()
        // Head
        drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.42f, h * 0.30f), style = Stroke(width = sw))
        // Body arc
        val body = Path().apply {
            moveTo(w * 0.15f, h * 0.82f)
            cubicTo(w * 0.15f, h * 0.60f, w * 0.69f, h * 0.60f, w * 0.69f, h * 0.82f)
        }
        drawPath(body, tint, style = Stroke(width = sw, cap = StrokeCap.Round))
        // Plus sign
        drawLine(tint, Offset(w * 0.80f, h * 0.32f), Offset(w * 0.80f, h * 0.56f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.68f, h * 0.44f), Offset(w * 0.92f, h * 0.44f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun ChatIconCopy(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.8.dp.toPx()
        // Back sheet
        drawRoundRect(tint, Offset(w * 0.32f, h * 0.14f), Size(w * 0.54f, h * 0.60f), CornerRadius(2.dp.toPx()), style = Stroke(width = sw))
        // Front sheet
        drawRoundRect(tint, Offset(w * 0.14f, h * 0.32f), Size(w * 0.54f, h * 0.60f), CornerRadius(2.dp.toPx()), style = Stroke(width = sw))
    }
}

@Composable
fun ChatIconTranslate(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.8.dp.toPx()
        // "A" character
        val p = Path().apply {
            moveTo(w * 0.20f, h * 0.70f)
            lineTo(w * 0.38f, h * 0.25f)
            lineTo(w * 0.56f, h * 0.70f)
        }
        drawPath(p, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(tint, Offset(w * 0.27f, h * 0.55f), Offset(w * 0.49f, h * 0.55f), strokeWidth = sw)
        // Asian glyph stroke hint
        drawLine(tint, Offset(w * 0.55f, h * 0.35f), Offset(w * 0.85f, h * 0.35f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.70f, h * 0.25f), Offset(w * 0.70f, h * 0.75f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.60f, h * 0.75f), Offset(w * 0.82f, h * 0.75f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun ChatIconVolume(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.8.dp.toPx()
        val speaker = Path().apply {
            moveTo(w * 0.20f, h * 0.38f)
            lineTo(w * 0.38f, h * 0.38f)
            lineTo(w * 0.58f, h * 0.20f)
            lineTo(w * 0.58f, h * 0.80f)
            lineTo(w * 0.38f, h * 0.62f)
            lineTo(w * 0.20f, h * 0.62f)
            close()
        }
        drawPath(speaker, tint, style = Fill)
        // Sound waves
        val wave1 = Path().apply {
            moveTo(w * 0.72f, h * 0.35f)
            cubicTo(w * 0.80f, h * 0.42f, w * 0.80f, h * 0.58f, w * 0.72f, h * 0.65f)
        }
        drawPath(wave1, tint, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

@Composable
fun ChatIconForward(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.9.dp.toPx()
        val arrow = Path().apply {
            moveTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.82f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.80f)
        }
        drawPath(arrow, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val body = Path().apply {
            moveTo(w * 0.20f, h * 0.80f)
            cubicTo(w * 0.20f, h * 0.50f, w * 0.40f, h * 0.50f, w * 0.80f, h * 0.50f)
        }
        drawPath(body, tint, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

@Composable
fun ChatIconReply(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.9.dp.toPx()
        val arrow = Path().apply {
            moveTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.18f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.80f)
        }
        drawPath(arrow, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val body = Path().apply {
            moveTo(w * 0.80f, h * 0.80f)
            cubicTo(w * 0.80f, h * 0.50f, w * 0.60f, h * 0.50f, w * 0.20f, h * 0.50f)
        }
        drawPath(body, tint, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

@Composable
fun ChatIconCheck(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 2.0.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.20f, h * 0.50f)
            lineTo(w * 0.42f, h * 0.72f)
            lineTo(w * 0.82f, h * 0.28f)
        }
        drawPath(path, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ChatIconPhoto(modifier: Modifier = Modifier.size(22.dp), tint: Color = Color(0xFF6B6B66)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.8.dp.toPx()
        drawRoundRect(tint, Offset(w * 0.12f, h * 0.15f), Size(w * 0.76f, h * 0.70f), CornerRadius(3.dp.toPx()), style = Stroke(width = sw))
        drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.32f, h * 0.38f))
        val mountain = Path().apply {
            moveTo(w * 0.16f, h * 0.75f)
            lineTo(w * 0.42f, h * 0.48f)
            lineTo(w * 0.58f, h * 0.62f)
            lineTo(w * 0.70f, h * 0.52f)
            lineTo(w * 0.84f, h * 0.75f)
            close()
        }
        drawPath(mountain, tint, style = Fill)
    }
}

@Composable
fun ChatIconPin(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.8.dp.toPx()
        drawLine(tint, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.25f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.35f, h * 0.25f), Offset(w * 0.35f, h * 0.55f), strokeWidth = sw)
        drawLine(tint, Offset(w * 0.65f, h * 0.25f), Offset(w * 0.65f, h * 0.55f), strokeWidth = sw)
        drawLine(tint, Offset(w * 0.20f, h * 0.55f), Offset(w * 0.80f, h * 0.55f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.50f, h * 0.55f), Offset(w * 0.50f, h * 0.85f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun ChatIconCloud(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.8.dp.toPx()
        val cloud = Path().apply {
            moveTo(w * 0.25f, h * 0.70f)
            cubicTo(w * 0.12f, h * 0.70f, w * 0.12f, h * 0.50f, w * 0.25f, h * 0.50f)
            cubicTo(w * 0.25f, h * 0.30f, w * 0.55f, h * 0.25f, w * 0.62f, h * 0.40f)
            cubicTo(w * 0.78f, h * 0.38f, w * 0.88f, h * 0.52f, w * 0.80f, h * 0.70f)
            close()
        }
        drawPath(cloud, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ChatIconBookmark(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.8.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.85f)
            lineTo(w * 0.50f, h * 0.65f)
            lineTo(w * 0.25f, h * 0.85f)
            close()
        }
        drawPath(path, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ChatIconSearch(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFF6B6B66)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.9.dp.toPx()
        drawCircle(tint, radius = w * 0.28f, center = Offset(w * 0.42f, h * 0.42f), style = Stroke(width = sw))
        drawLine(tint, Offset(w * 0.62f, h * 0.62f), Offset(w * 0.85f, h * 0.85f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun ChatIconMoreVert(modifier: Modifier = Modifier.size(20.dp), tint: Color = Color(0xFF1B1B1B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = 2.dp.toPx()
        drawCircle(tint, radius = r, center = Offset(w / 2f, h * 0.25f))
        drawCircle(tint, radius = r, center = Offset(w / 2f, h * 0.50f))
        drawCircle(tint, radius = r, center = Offset(w / 2f, h * 0.75f))
    }
}

@Composable
fun ChatIconAlert(modifier: Modifier = Modifier.size(24.dp), tint: Color = Color(0xFFDC2626)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 2.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.90f, h * 0.85f)
            lineTo(w * 0.10f, h * 0.85f)
            close()
        }
        drawPath(path, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(tint, Offset(w * 0.5f, h * 0.38f), Offset(w * 0.5f, h * 0.60f), strokeWidth = sw, cap = StrokeCap.Round)
        drawCircle(tint, radius = sw * 0.7f, center = Offset(w * 0.5f, h * 0.73f))
    }
}

@Composable
fun ChatIconLeave(modifier: Modifier = Modifier.size(24.dp), tint: Color = Color(0xFFDC2626)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 2.dp.toPx()
        // Door frame
        val door = Path().apply {
            moveTo(w * 0.55f, h * 0.20f)
            lineTo(w * 0.20f, h * 0.20f)
            lineTo(w * 0.20f, h * 0.80f)
            lineTo(w * 0.55f, h * 0.80f)
        }
        drawPath(door, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Exit arrow
        drawLine(tint, Offset(w * 0.38f, h * 0.50f), Offset(w * 0.85f, h * 0.50f), strokeWidth = sw, cap = StrokeCap.Round)
        val arrowHead = Path().apply {
            moveTo(w * 0.68f, h * 0.34f)
            lineTo(w * 0.85f, h * 0.50f)
            lineTo(w * 0.68f, h * 0.66f)
        }
        drawPath(arrowHead, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ChatIconEdit(modifier: Modifier = Modifier.size(22.dp), tint: Color = Color(0xFFD97706)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.9.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.65f, h * 0.18f)
            lineTo(w * 0.82f, h * 0.35f)
            lineTo(w * 0.35f, h * 0.82f)
            lineTo(w * 0.18f, h * 0.82f)
            lineTo(w * 0.18f, h * 0.65f)
            close()
        }
        drawPath(path, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(tint, Offset(w * 0.52f, h * 0.31f), Offset(w * 0.69f, h * 0.48f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun ChatIconClock(modifier: Modifier = Modifier.size(22.dp), tint: Color = Color(0xFFD97706)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.9.dp.toPx()
        drawCircle(tint, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = sw))
        drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.28f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.68f, h * 0.5f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun ChatIconBell(modifier: Modifier = Modifier.size(22.dp), tint: Color = Color(0xFFD97706)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 1.9.dp.toPx()
        val bell = Path().apply {
            moveTo(w * 0.5f, h * 0.18f)
            cubicTo(w * 0.32f, h * 0.22f, w * 0.25f, h * 0.45f, w * 0.25f, h * 0.62f)
            lineTo(w * 0.18f, h * 0.72f)
            lineTo(w * 0.82f, h * 0.72f)
            lineTo(w * 0.75f, h * 0.62f)
            cubicTo(w * 0.75f, h * 0.45f, w * 0.68f, h * 0.22f, w * 0.5f, h * 0.18f)
            close()
        }
        drawPath(bell, tint, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawArc(tint, 0f, 180f, false, topLeft = Offset(w * 0.42f, h * 0.74f), size = Size(w * 0.16f, h * 0.12f), style = Stroke(width = sw))
    }
}

@Composable
fun ChatIconBlock(modifier: Modifier = Modifier.size(22.dp), tint: Color = Color(0xFFDC2626)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = 2.dp.toPx()
        drawCircle(tint, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = sw))
        drawLine(tint, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.75f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

