package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom vector canvas icons for Group Chat UI.
 * Purely drawn with Compose Canvas — no Material Icons, no emojis.
 * Supports both size = X.dp and modifier = ...
 */

@Composable
fun GroupBackIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 2.4f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // Left arrow horizontal line
        drawLine(
            color = color,
            start = Offset(w * 0.82f, h * 0.5f),
            end = Offset(w * 0.22f, h * 0.5f),
            strokeWidth = strokeWidth * density,
            cap = StrokeCap.Round
        )
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.24f)
            lineTo(w * 0.20f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.76f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun GroupThreeDotsIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A)
) {
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width * 0.5f
        val r = this.size.width * 0.09f
        drawCircle(color = color, radius = r, center = Offset(cx, this.size.height * 0.24f))
        drawCircle(color = color, radius = r, center = Offset(cx, this.size.height * 0.50f))
        drawCircle(color = color, radius = r, center = Offset(cx, this.size.height * 0.76f))
    }
}

@Composable
fun GroupLockIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF92400E),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        // Body
        val bodyRect = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = w * 0.20f,
                    top = h * 0.44f,
                    right = w * 0.80f,
                    bottom = h * 0.90f,
                    cornerRadius = CornerRadius(w * 0.12f, h * 0.12f)
                )
            )
        }
        drawPath(path = bodyRect, color = color, style = Stroke(width = sw, join = StrokeJoin.Round))

        // Shackle
        val shackle = Path().apply {
            moveTo(w * 0.32f, h * 0.44f)
            lineTo(w * 0.32f, h * 0.28f)
            cubicTo(w * 0.32f, h * 0.14f, w * 0.68f, h * 0.14f, w * 0.68f, h * 0.28f)
            lineTo(w * 0.68f, h * 0.44f)
        }
        drawPath(path = shackle, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))

        // Keyhole
        drawCircle(color = color, radius = w * 0.055f, center = Offset(w * 0.5f, h * 0.63f))
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.65f),
            end = Offset(w * 0.5f, h * 0.75f),
            strokeWidth = sw * 0.9f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupTinyLockIcon(
    size: Dp = 12.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFF59E0B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = 1.4f * density

        // Shackle
        val shackle = Path().apply {
            moveTo(w * 0.30f, h * 0.42f)
            lineTo(w * 0.30f, h * 0.26f)
            cubicTo(w * 0.30f, h * 0.12f, w * 0.70f, h * 0.12f, w * 0.70f, h * 0.26f)
            lineTo(w * 0.70f, h * 0.42f)
        }
        drawPath(path = shackle, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))

        // Body
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.18f, h * 0.40f),
            size = Size(w * 0.64f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.10f, h * 0.10f),
            style = Fill
        )
    }
}

@Composable
fun GroupSearchIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density
        // Circle
        val cx = w * 0.44f
        val cy = h * 0.44f
        val r = w * 0.30f
        drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(width = sw))

        // Handle
        drawLine(
            color = color,
            start = Offset(cx + r * 0.707f, cy + r * 0.707f),
            end = Offset(w * 0.88f, h * 0.88f),
            strokeWidth = sw * 1.1f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupMediaIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 1.9f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        // Frame
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.16f),
            size = Size(w * 0.76f, h * 0.68f),
            cornerRadius = CornerRadius(w * 0.14f, h * 0.14f),
            style = Stroke(width = sw)
        )

        // Sun
        drawCircle(color = color, radius = w * 0.08f, center = Offset(w * 0.35f, h * 0.38f))

        // Mountain path
        val mount = Path().apply {
            moveTo(w * 0.16f, h * 0.74f)
            lineTo(w * 0.42f, h * 0.48f)
            lineTo(w * 0.58f, h * 0.64f)
            lineTo(w * 0.70f, h * 0.54f)
            lineTo(w * 0.84f, h * 0.74f)
        }
        drawPath(path = mount, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun GroupBellIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        val bell = Path().apply {
            moveTo(w * 0.50f, h * 0.14f)
            cubicTo(w * 0.36f, h * 0.14f, w * 0.28f, h * 0.30f, w * 0.28f, h * 0.54f)
            cubicTo(w * 0.28f, h * 0.68f, w * 0.20f, h * 0.74f, w * 0.18f, h * 0.76f)
            lineTo(w * 0.82f, h * 0.76f)
            cubicTo(w * 0.80f, h * 0.74f, w * 0.72f, h * 0.68f, w * 0.72f, h * 0.54f)
            cubicTo(w * 0.72f, h * 0.30f, w * 0.64f, h * 0.14f, w * 0.50f, h * 0.14f)
            close()
        }
        drawPath(path = bell, color = color, style = Stroke(width = sw, join = StrokeJoin.Round))

        // Clapper
        val clapper = Path().apply {
            moveTo(w * 0.42f, h * 0.78f)
            cubicTo(w * 0.42f, h * 0.88f, w * 0.58f, h * 0.88f, w * 0.58f, h * 0.78f)
        }
        drawPath(path = clapper, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

@Composable
fun GroupTrashIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 1.9f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        // Lid horizontal
        drawLine(
            color = color,
            start = Offset(w * 0.18f, h * 0.24f),
            end = Offset(w * 0.82f, h * 0.24f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )

        // Handle
        val handle = Path().apply {
            moveTo(w * 0.38f, h * 0.24f)
            lineTo(w * 0.38f, h * 0.14f)
            lineTo(w * 0.62f, h * 0.14f)
            lineTo(w * 0.62f, h * 0.24f)
        }
        drawPath(path = handle, color = color, style = Stroke(width = sw, join = StrokeJoin.Round))

        // Can Body
        val body = Path().apply {
            moveTo(w * 0.25f, h * 0.28f)
            lineTo(w * 0.30f, h * 0.84f)
            cubicTo(w * 0.30f, h * 0.90f, w * 0.70f, h * 0.90f, w * 0.70f, h * 0.84f)
            lineTo(w * 0.75f, h * 0.28f)
        }
        drawPath(path = body, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Slats
        drawLine(
            color = color,
            start = Offset(w * 0.42f, h * 0.40f),
            end = Offset(w * 0.42f, h * 0.74f),
            strokeWidth = sw * 0.8f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.58f, h * 0.40f),
            end = Offset(w * 0.58f, h * 0.74f),
            strokeWidth = sw * 0.8f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupShieldIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        val shield = Path().apply {
            moveTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.82f, h * 0.24f)
            lineTo(w * 0.82f, h * 0.52f)
            cubicTo(w * 0.82f, h * 0.76f, w * 0.50f, h * 0.90f, w * 0.50f, h * 0.90f)
            cubicTo(w * 0.50f, h * 0.90f, w * 0.18f, h * 0.76f, w * 0.18f, h * 0.52f)
            lineTo(w * 0.18f, h * 0.24f)
            close()
        }
        drawPath(path = shield, color = color, style = Stroke(width = sw, join = StrokeJoin.Round))
    }
}

@Composable
fun GroupLeaveDoorIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFEF4444),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        // Door frame
        val frame = Path().apply {
            moveTo(w * 0.48f, h * 0.16f)
            lineTo(w * 0.22f, h * 0.16f)
            lineTo(w * 0.22f, h * 0.84f)
            lineTo(w * 0.48f, h * 0.84f)
        }
        drawPath(path = frame, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Arrow pointing right out of door
        drawLine(
            color = color,
            start = Offset(w * 0.42f, h * 0.50f),
            end = Offset(w * 0.84f, h * 0.50f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        val arrow = Path().apply {
            moveTo(w * 0.68f, h * 0.34f)
            lineTo(w * 0.84f, h * 0.50f)
            lineTo(w * 0.68f, h * 0.66f)
        }
        drawPath(path = arrow, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun GroupCameraIcon(
    size: Dp = 16.dp,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    strokeWidth: Float = 1.8f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        // Top bump
        val bump = Path().apply {
            moveTo(w * 0.36f, h * 0.24f)
            lineTo(w * 0.42f, h * 0.14f)
            lineTo(w * 0.58f, h * 0.14f)
            lineTo(w * 0.64f, h * 0.24f)
        }
        drawPath(path = bump, color = color, style = Stroke(width = sw, join = StrokeJoin.Round))

        // Body
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.14f, h * 0.24f),
            size = Size(w * 0.72f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.14f, h * 0.14f),
            style = Stroke(width = sw)
        )

        // Center lens
        drawCircle(color = color, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.55f), style = Stroke(width = sw))
    }
}

@Composable
fun GroupPersonAddIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        // Person Head
        drawCircle(color = color, radius = w * 0.18f, center = Offset(w * 0.40f, h * 0.30f), style = Stroke(width = sw))

        // Person Body
        val body = Path().apply {
            moveTo(w * 0.12f, h * 0.84f)
            cubicTo(w * 0.12f, h * 0.62f, w * 0.68f, h * 0.62f, w * 0.68f, h * 0.84f)
        }
        drawPath(path = body, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))

        // Plus symbol
        drawLine(
            color = color,
            start = Offset(w * 0.78f, h * 0.28f),
            end = Offset(w * 0.78f, h * 0.48f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.68f, h * 0.38f),
            end = Offset(w * 0.88f, h * 0.38f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupLinkIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        // Interlocking oval chain links
        val link1 = Path().apply {
            moveTo(w * 0.42f, h * 0.28f)
            lineTo(w * 0.56f, h * 0.28f)
            cubicTo(w * 0.74f, h * 0.28f, w * 0.74f, h * 0.62f, w * 0.56f, h * 0.62f)
            lineTo(w * 0.42f, h * 0.62f)
        }
        drawPath(path = link1, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))

        val link2 = Path().apply {
            moveTo(w * 0.58f, h * 0.72f)
            lineTo(w * 0.44f, h * 0.72f)
            cubicTo(w * 0.26f, h * 0.72f, w * 0.26f, h * 0.38f, w * 0.44f, h * 0.38f)
            lineTo(w * 0.58f, h * 0.38f)
        }
        drawPath(path = link2, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

@Composable
fun GroupChevronRightIcon(
    size: Dp = 16.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF94A3B8),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density
        val p = Path().apply {
            moveTo(w * 0.35f, h * 0.22f)
            lineTo(w * 0.65f, h * 0.50f)
            lineTo(w * 0.35f, h * 0.78f)
        }
        drawPath(path = p, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun GroupUsersIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 1.9f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        // Main User Head
        drawCircle(color = color, radius = w * 0.16f, center = Offset(w * 0.38f, h * 0.32f), style = Stroke(width = sw))
        // Main User Body
        val body1 = Path().apply {
            moveTo(w * 0.14f, h * 0.84f)
            cubicTo(w * 0.14f, h * 0.62f, w * 0.62f, h * 0.62f, w * 0.62f, h * 0.84f)
        }
        drawPath(path = body1, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))

        // Secondary User Head
        drawCircle(color = color, radius = w * 0.13f, center = Offset(w * 0.70f, h * 0.38f), style = Stroke(width = sw))
        // Secondary User Body
        val body2 = Path().apply {
            moveTo(w * 0.62f, h * 0.65f)
            cubicTo(w * 0.72f, h * 0.65f, w * 0.88f, h * 0.72f, w * 0.88f, h * 0.84f)
        }
        drawPath(path = body2, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

@Composable
fun GroupGearIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 1.9f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val rOut = w * 0.36f
        val sw = strokeWidth * density

        // Center hole
        drawCircle(color = color, radius = w * 0.13f, center = Offset(cx, cy), style = Stroke(width = sw))

        // Gear outline
        drawCircle(color = color, radius = rOut, center = Offset(cx, cy), style = Stroke(width = sw))

        // 6 cogs
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60.0))
            val x1 = cx + (rOut * 0.95f) * Math.cos(angle).toFloat()
            val y1 = cy + (rOut * 0.95f) * Math.sin(angle).toFloat()
            val x2 = cx + (rOut * 1.25f) * Math.cos(angle).toFloat()
            val y2 = cy + (rOut * 1.25f) * Math.sin(angle).toFloat()
            drawLine(
                color = color,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = sw * 1.4f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun GroupChatBubbleIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        val p = Path().apply {
            moveTo(w * 0.20f, h * 0.20f)
            lineTo(w * 0.80f, h * 0.20f)
            cubicTo(w * 0.88f, h * 0.20f, w * 0.88f, h * 0.65f, w * 0.80f, h * 0.65f)
            lineTo(w * 0.40f, h * 0.65f)
            lineTo(w * 0.22f, h * 0.82f)
            lineTo(w * 0.22f, h * 0.65f)
            cubicTo(w * 0.12f, h * 0.65f, w * 0.12f, h * 0.20f, w * 0.20f, h * 0.20f)
            close()
        }
        drawPath(path = p, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun GroupEditPencilIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        val p = Path().apply {
            moveTo(w * 0.68f, h * 0.16f)
            lineTo(w * 0.84f, h * 0.32f)
            lineTo(w * 0.36f, h * 0.80f)
            lineTo(w * 0.18f, h * 0.82f)
            lineTo(w * 0.20f, h * 0.64f)
            close()
        }
        drawPath(path = p, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(
            color = color,
            start = Offset(w * 0.56f, h * 0.28f),
            end = Offset(w * 0.72f, h * 0.44f),
            strokeWidth = sw * 0.8f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupClockIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val sw = strokeWidth * density

        drawCircle(color = color, radius = w * 0.38f, center = Offset(cx, cy), style = Stroke(width = sw))
        drawLine(
            color = color,
            start = Offset(cx, cy),
            end = Offset(cx, cy - w * 0.22f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(cx, cy),
            end = Offset(cx + w * 0.18f, cy),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupBlockIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = w * 0.38f
        val sw = strokeWidth * density

        drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(width = sw))
        val d = r * 0.707f
        drawLine(
            color = color,
            start = Offset(cx - d, cy - d),
            end = Offset(cx + d, cy + d),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupCrownIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFD97706),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        val p = Path().apply {
            moveTo(w * 0.16f, h * 0.74f)
            lineTo(w * 0.84f, h * 0.74f)
            lineTo(w * 0.88f, h * 0.32f)
            lineTo(w * 0.65f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.22f)
            lineTo(w * 0.35f, h * 0.52f)
            lineTo(w * 0.12f, h * 0.32f)
            close()
        }
        drawPath(path = p, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color = color, radius = w * 0.045f, center = Offset(w * 0.12f, h * 0.30f))
        drawCircle(color = color, radius = w * 0.045f, center = Offset(w * 0.50f, h * 0.20f))
        drawCircle(color = color, radius = w * 0.045f, center = Offset(w * 0.88f, h * 0.30f))
    }
}

@Composable
fun GroupRestrictIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = w * 0.36f
        val sw = strokeWidth * density

        drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(width = sw))
        drawLine(
            color = color,
            start = Offset(w * 0.25f, h * 0.25f),
            end = Offset(w * 0.75f, h * 0.75f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupEyeIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0F172A),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        val eye = Path().apply {
            moveTo(w * 0.12f, h * 0.50f)
            cubicTo(w * 0.28f, h * 0.22f, w * 0.72f, h * 0.22f, w * 0.88f, h * 0.50f)
            cubicTo(w * 0.72f, h * 0.78f, w * 0.28f, h * 0.78f, w * 0.12f, h * 0.50f)
            close()
        }
        drawPath(path = eye, color = color, style = Stroke(width = sw, join = StrokeJoin.Round))
        drawCircle(color = color, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = sw))
    }
}

@Composable
fun GroupSendPlaneIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Paper plane pointed slightly up and to the right
        val plane = Path().apply {
            moveTo(w * 0.88f, h * 0.50f)
            lineTo(w * 0.18f, h * 0.18f)
            lineTo(w * 0.34f, h * 0.50f)
            lineTo(w * 0.18f, h * 0.82f)
            close()
        }
        drawPath(path = plane, color = color, style = Fill)
    }
}

@Composable
fun GroupCloseIcon(
    size: Dp = 16.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF94A3B8),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density
        drawLine(
            color = color,
            start = Offset(w * 0.22f, h * 0.22f),
            end = Offset(w * 0.78f, h * 0.78f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.78f, h * 0.22f),
            end = Offset(w * 0.22f, h * 0.78f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupCheckIcon(
    size: Dp = 16.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF16A34A),
    strokeWidth: Float = 2.2f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density
        val p = Path().apply {
            moveTo(w * 0.20f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.76f)
            lineTo(w * 0.84f, h * 0.26f)
        }
        drawPath(path = p, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun GroupRefreshIcon(
    size: Dp = 18.dp,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF475569),
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density
        // Draw 270 degree circular arc with arrow head
        drawArc(
            color = color,
            startAngle = 45f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
        // Arrow head at 45 degrees
        val arrow = Path().apply {
            moveTo(w * 0.78f, h * 0.58f)
            lineTo(w * 0.88f, h * 0.78f)
            lineTo(w * 0.68f, h * 0.82f)
        }
        drawPath(path = arrow, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Exactly reproduces the cute pastel speech bubble empty chat illustration
 * shown in the user's provided screenshot.
 */
@Composable
fun GroupCuteEmptyChatIllustration(
    modifier: Modifier = Modifier.size(140.dp)
) {
    Canvas(modifier = modifier) {
        val w = this.size.width
        val h = this.size.height

        // 1. Soft grey secondary bubble in background
        val bgBubble = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = w * 0.44f,
                    top = h * 0.42f,
                    right = w * 0.82f,
                    bottom = h * 0.74f,
                    cornerRadius = CornerRadius(w * 0.12f, h * 0.12f)
                )
            )
            // Little tail
            moveTo(w * 0.72f, h * 0.74f)
            lineTo(w * 0.78f, h * 0.84f)
            lineTo(w * 0.62f, h * 0.74f)
        }
        drawPath(path = bgBubble, color = Color(0xFFE2E8F0).copy(alpha = 0.65f), style = Fill)

        // 2. Primary cute warm yellow speech bubble
        val fgBubble = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = w * 0.24f,
                    top = h * 0.34f,
                    right = w * 0.68f,
                    bottom = h * 0.70f,
                    cornerRadius = CornerRadius(w * 0.14f, h * 0.14f)
                )
            )
            // Bottom-left beak tail
            moveTo(w * 0.32f, h * 0.70f)
            lineTo(w * 0.28f, h * 0.82f)
            lineTo(w * 0.44f, h * 0.70f)
        }
        drawPath(path = fgBubble, color = Color(0xFFFDE68A), style = Fill)

        // 3. Three warm amber dots inside the main bubble
        val dotRadius = w * 0.038f
        val dotColor = Color(0xFFD97706)
        val cy = h * 0.52f
        drawCircle(color = dotColor, radius = dotRadius, center = Offset(w * 0.36f, cy))
        drawCircle(color = dotColor, radius = dotRadius, center = Offset(w * 0.46f, cy))
        drawCircle(color = dotColor, radius = dotRadius, center = Offset(w * 0.56f, cy))

        // 4. Subtle radiating sparkles / accent ticks around the bubble
        val sparkleColor = Color(0xFFF59E0B)
        val tickWidth = 3.2f * density

        // Top-right ticks
        drawLine(
            color = sparkleColor,
            start = Offset(w * 0.70f, h * 0.28f),
            end = Offset(w * 0.73f, h * 0.22f),
            strokeWidth = tickWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = sparkleColor,
            start = Offset(w * 0.76f, h * 0.34f),
            end = Offset(w * 0.83f, h * 0.30f),
            strokeWidth = tickWidth,
            cap = StrokeCap.Round
        )

        // Bottom-left ticks
        drawLine(
            color = sparkleColor,
            start = Offset(w * 0.18f, h * 0.58f),
            end = Offset(w * 0.12f, h * 0.58f),
            strokeWidth = tickWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = sparkleColor,
            start = Offset(w * 0.20f, h * 0.68f),
            end = Offset(w * 0.14f, h * 0.74f),
            strokeWidth = tickWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupE2EEShieldIcon(
    size: Dp = 20.dp,
    color: Color = Color(0xFF10B981),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.8f * density

        val shieldPath = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            cubicTo(w * 0.75f, h * 0.12f, w * 0.88f, h * 0.22f, w * 0.88f, h * 0.45f)
            cubicTo(w * 0.88f, h * 0.72f, w * 0.5f, h * 0.90f, w * 0.5f, h * 0.90f)
            cubicTo(w * 0.5f, h * 0.90f, w * 0.12f, h * 0.72f, w * 0.12f, h * 0.45f)
            cubicTo(w * 0.12f, h * 0.22f, w * 0.25f, h * 0.12f, w * 0.5f, h * 0.12f)
            close()
        }

        drawPath(
            path = shieldPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Lock / Check inside
        val checkPath = Path().apply {
            moveTo(w * 0.35f, h * 0.48f)
            lineTo(w * 0.46f, h * 0.60f)
            lineTo(w * 0.65f, h * 0.38f)
        }
        drawPath(
            path = checkPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun GroupPinIcon(
    size: Dp = 16.dp,
    color: Color = Color(0xFFEAB308),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.8f * density

        // Pin Head
        drawCircle(
            color = color,
            radius = w * 0.20f,
            center = Offset(w * 0.5f, h * 0.30f)
        )
        // Pin Stem
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.50f),
            end = Offset(w * 0.5f, h * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupMuteIcon(
    size: Dp = 16.dp,
    color: Color = Color(0xFF9CA3AF),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.8f * density

        val speakerPath = Path().apply {
            moveTo(w * 0.15f, h * 0.40f)
            lineTo(w * 0.35f, h * 0.40f)
            lineTo(w * 0.55f, h * 0.22f)
            lineTo(w * 0.55f, h * 0.78f)
            lineTo(w * 0.35f, h * 0.60f)
            lineTo(w * 0.15f, h * 0.60f)
            close()
        }
        drawPath(
            path = speakerPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Diagonal strike-through line
        drawLine(
            color = color,
            start = Offset(w * 0.20f, h * 0.80f),
            end = Offset(w * 0.80f, h * 0.20f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupMinimalistModeIcon(
    size: Dp = 20.dp,
    color: Color = Color(0xFF374151),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.8f * density

        // Two clean horizontal toggle lines representing minimalist compactness
        drawLine(
            color = color,
            start = Offset(w * 0.18f, h * 0.36f),
            end = Offset(w * 0.82f, h * 0.36f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.18f, h * 0.64f),
            end = Offset(w * 0.82f, h * 0.64f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Indicator points
        drawCircle(
            color = color,
            radius = w * 0.08f,
            center = Offset(w * 0.34f, h * 0.36f)
        )
        drawCircle(
            color = color,
            radius = w * 0.08f,
            center = Offset(w * 0.66f, h * 0.64f)
        )
    }
}

@Composable
fun GroupRequestReviewIcon(
    size: Dp = 20.dp,
    color: Color = Color(0xFF374151),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 1.8f * density

        // Badge / Document Outline
        val docPath = Path().apply {
            moveTo(w * 0.22f, h * 0.15f)
            lineTo(w * 0.62f, h * 0.15f)
            lineTo(w * 0.78f, h * 0.32f)
            lineTo(w * 0.78f, h * 0.85f)
            lineTo(w * 0.22f, h * 0.85f)
            close()
        }
        drawPath(
            path = docPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Lines inside document
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.44f),
            end = Offset(w * 0.66f, h * 0.44f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.58f),
            end = Offset(w * 0.56f, h * 0.58f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupInfoCircleIcon(
    size: Dp = 20.dp,
    color: Color = Color(0xFF0F172A),
    modifier: Modifier = Modifier,
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density
        drawCircle(
            color = color,
            radius = w * 0.38f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = sw)
        )
        drawCircle(
            color = color,
            radius = sw * 0.65f,
            center = Offset(w * 0.5f, h * 0.33f)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.46f),
            end = Offset(w * 0.5f, h * 0.68f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupBroomIcon(
    size: Dp = 20.dp,
    color: Color = Color(0xFF0F172A),
    modifier: Modifier = Modifier,
    strokeWidth: Float = 2.0f
) {
    GroupTrashIcon(size = size, color = color, modifier = modifier, strokeWidth = strokeWidth)
}

@Composable
fun GroupSendPaperPlaneIcon(
    size: Dp = 20.dp,
    color: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    GroupSendPlaneIcon(size = size, color = color, modifier = modifier)
}

@Composable
fun GroupImagePlaceholderIcon(
    size: Dp = 20.dp,
    color: Color = Color(0xFF0F172A),
    modifier: Modifier = Modifier
) {
    GroupMediaIcon(size = size, color = color, modifier = modifier)
}

@Composable
fun GroupPlusIcon(
    size: Dp = 20.dp,
    color: Color = Color(0xFF0F172A),
    modifier: Modifier = Modifier,
    strokeWidth: Float = 2.2f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.2f),
            end = Offset(w * 0.5f, h * 0.8f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.2f, h * 0.5f),
            end = Offset(w * 0.8f, h * 0.5f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupMicIcon(
    size: Dp = 20.dp,
    color: Color = Color(0xFF0F172A),
    modifier: Modifier = Modifier,
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.34f, h * 0.14f),
            size = Size(w * 0.32f, h * 0.44f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
            style = Stroke(width = sw)
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.22f, h * 0.26f),
            size = Size(w * 0.56f, h * 0.46f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.72f),
            end = Offset(w * 0.5f, h * 0.88f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.88f),
            end = Offset(w * 0.66f, h * 0.88f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GroupShareIcon(
    size: Dp = 20.dp,
    color: Color = Color(0xFF0F172A),
    modifier: Modifier = Modifier,
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density
        val r = w * 0.11f

        val cLeft = Offset(w * 0.24f, h * 0.50f)
        val cTop = Offset(w * 0.74f, h * 0.22f)
        val cBot = Offset(w * 0.74f, h * 0.78f)

        drawLine(
            color = color,
            start = Offset(cLeft.x + r * 0.8f, cLeft.y - r * 0.5f),
            end = Offset(cTop.x - r * 0.8f, cTop.y + r * 0.5f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(cLeft.x + r * 0.8f, cLeft.y + r * 0.5f),
            end = Offset(cBot.x - r * 0.8f, cBot.y - r * 0.5f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )

        drawCircle(color = color, radius = r, center = cLeft, style = Stroke(width = sw))
        drawCircle(color = color, radius = r, center = cTop, style = Stroke(width = sw))
        drawCircle(color = color, radius = r, center = cBot, style = Stroke(width = sw))
    }
}

@Composable
fun GroupBlockBanIcon(
    size: Dp = 20.dp,
    color: Color = Color(0xFFEF4444),
    modifier: Modifier = Modifier,
    strokeWidth: Float = 2.0f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density
        val radius = w * 0.38f
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = sw)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.24f, h * 0.24f),
            end = Offset(w * 0.76f, h * 0.76f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}


