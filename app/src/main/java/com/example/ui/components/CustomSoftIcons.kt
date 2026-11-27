package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom Soft Icons:
 * Beautiful, rounded, luxurious 3D-feel icons designed specifically for FunkyTalk's
 * secure chat vault and modern chat interfaces (no harsh generic Material icons).
 */

@Composable
fun SoftLockIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    isUnlocked: Boolean = false,
    gradientColors: List<Color> = if (isUnlocked) {
        listOf(Color(0xFF34D399), Color(0xFF059669)) // Emerald/Teal when unlocked
    } else {
        listOf(Color(0xFF818CF8), Color(0xFF4F46E5)) // Indigo/Violet when locked
    },
    shackleColor: Color = Color(0xFFCBD5E1)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // 1. Shackle (curved arch)
        val shackleStroke = w * 0.12f
        val shackleWidth = w * 0.44f
        val shackleHeight = h * 0.45f
        val shackleLeft = (w - shackleWidth) / 2f
        val shackleTop = h * 0.08f

        val shacklePath = Path().apply {
            if (!isUnlocked) {
                // Closed shackle arch
                moveTo(shackleLeft, h * 0.46f)
                lineTo(shackleLeft, shackleTop + shackleHeight * 0.5f)
                arcTo(
                    rect = Rect(
                        left = shackleLeft,
                        top = shackleTop,
                        right = shackleLeft + shackleWidth,
                        bottom = shackleTop + shackleHeight
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                lineTo(shackleLeft + shackleWidth, h * 0.46f)
            } else {
                // Open shackle arch (shifted up and swung slightly)
                moveTo(shackleLeft - w * 0.04f, h * 0.42f)
                lineTo(shackleLeft - w * 0.04f, shackleTop * 0.4f + shackleHeight * 0.4f)
                arcTo(
                    rect = Rect(
                        left = shackleLeft - w * 0.04f,
                        top = shackleTop * 0.4f,
                        right = shackleLeft + shackleWidth - w * 0.04f,
                        bottom = shackleTop * 0.4f + shackleHeight
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                lineTo(shackleLeft + shackleWidth - w * 0.04f, shackleTop + shackleHeight * 0.35f)
            }
        }

        drawPath(
            path = shacklePath,
            color = shackleColor,
            style = Stroke(
                width = shackleStroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 2. Lock Body (Soft rounded squircle with gradient)
        val bodyTop = h * 0.38f
        val bodyHeight = h * 0.54f
        val bodyWidth = w * 0.76f
        val bodyLeft = (w - bodyWidth) / 2f
        val bodyCorner = bodyWidth * 0.28f

        // Soft ambient shadow behind body
        drawRoundRect(
            color = gradientColors.last().copy(alpha = 0.25f),
            topLeft = Offset(bodyLeft, bodyTop + 2.dp.toPx()),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(bodyCorner, bodyCorner)
        )

        // Gradient body
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = gradientColors,
                startY = bodyTop,
                endY = bodyTop + bodyHeight
            ),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(bodyCorner, bodyCorner)
        )

        // Top glossy highlight reflection on body
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                startY = bodyTop,
                endY = bodyTop + bodyHeight * 0.4f
            ),
            topLeft = Offset(bodyLeft + 1.5.dp.toPx(), bodyTop + 1.5.dp.toPx()),
            size = Size(bodyWidth - 3.dp.toPx(), bodyHeight * 0.35f),
            cornerRadius = CornerRadius(bodyCorner * 0.8f, bodyCorner * 0.8f)
        )

        // 3. Keyhole (Circle + tapered slot)
        val keyholeRadius = w * 0.08f
        val keyholeCenter = Offset(w / 2f, bodyTop + bodyHeight * 0.42f)

        drawCircle(
            color = Color.White,
            radius = keyholeRadius,
            center = keyholeCenter
        )

        val slotPath = Path().apply {
            moveTo(keyholeCenter.x - keyholeRadius * 0.75f, keyholeCenter.y)
            lineTo(keyholeCenter.x + keyholeRadius * 0.75f, keyholeCenter.y)
            lineTo(keyholeCenter.x + keyholeRadius * 0.45f, keyholeCenter.y + bodyHeight * 0.28f)
            lineTo(keyholeCenter.x - keyholeRadius * 0.45f, keyholeCenter.y + bodyHeight * 0.28f)
            close()
        }
        drawPath(path = slotPath, color = Color.White)
    }
}

@Composable
fun SoftKeyIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    color: Color = Color(0xFFF59E0B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Soft rotated key
        val headRadius = w * 0.24f
        val headCenter = Offset(w * 0.32f, h * 0.32f)

        // Outer ring
        drawCircle(
            brush = Brush.linearGradient(
                listOf(color, color.copy(alpha = 0.8f)),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            radius = headRadius,
            center = headCenter
        )
        // Inner cutout
        drawCircle(
            color = Color.White,
            radius = headRadius * 0.48f,
            center = headCenter
        )

        // Shaft
        val strokeW = w * 0.12f
        val shaftStart = Offset(headCenter.x + headRadius * 0.7f, headCenter.y + headRadius * 0.7f)
        val shaftEnd = Offset(w * 0.86f, h * 0.86f)

        drawLine(
            color = color,
            start = shaftStart,
            end = shaftEnd,
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Bit teeth
        val tooth1Start = Offset(w * 0.72f, h * 0.72f)
        val tooth1End = Offset(w * 0.84f, h * 0.60f)
        drawLine(
            color = color,
            start = tooth1Start,
            end = tooth1End,
            strokeWidth = strokeW * 0.85f,
            cap = StrokeCap.Round
        )

        val tooth2Start = Offset(w * 0.82f, h * 0.82f)
        val tooth2End = Offset(w * 0.92f, h * 0.72f)
        drawLine(
            color = color,
            start = tooth2Start,
            end = tooth2End,
            strokeWidth = strokeW * 0.85f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SoftShieldIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color? = null,
    gradientColors: List<Color> = if (color != null) listOf(color, color) else listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
    showCheck: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val shieldPath = Path().apply {
            moveTo(w * 0.5f, h * 0.06f)
            cubicTo(
                w * 0.78f, h * 0.08f,
                w * 0.92f, h * 0.16f,
                w * 0.92f, h * 0.40f
            )
            cubicTo(
                w * 0.92f, h * 0.68f,
                w * 0.65f, h * 0.88f,
                w * 0.5f, h * 0.96f
            )
            cubicTo(
                w * 0.35f, h * 0.88f,
                w * 0.08f, h * 0.68f,
                w * 0.08f, h * 0.40f
            )
            cubicTo(
                w * 0.08f, h * 0.16f,
                w * 0.22f, h * 0.08f,
                w * 0.5f, h * 0.06f
            )
            close()
        }

        // Soft glow shadow
        drawPath(
            path = shieldPath,
            color = gradientColors.last().copy(alpha = 0.25f)
        )

        // Gradient fill
        drawPath(
            path = shieldPath,
            brush = Brush.verticalGradient(
                colors = gradientColors,
                startY = 0f,
                endY = h
            )
        )

        // Soft inner contour
        drawPath(
            path = shieldPath,
            color = Color.White.copy(alpha = 0.25f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        if (showCheck) {
            // Smooth checkmark
            val checkPath = Path().apply {
                moveTo(w * 0.32f, h * 0.50f)
                lineTo(w * 0.45f, h * 0.63f)
                lineTo(w * 0.68f, h * 0.38f)
            }
            drawPath(
                path = checkPath,
                color = Color.White,
                style = Stroke(
                    width = w * 0.1f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun SoftEyeIcon(
    visible: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = Color(0xFF6B7280)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val strokeW = w * 0.1f

        // Smooth almond eye contour
        val eyePath = Path().apply {
            moveTo(w * 0.1f, h * 0.5f)
            cubicTo(
                w * 0.3f, h * 0.2f,
                w * 0.7f, h * 0.2f,
                w * 0.9f, h * 0.5f
            )
            cubicTo(
                w * 0.7f, h * 0.8f,
                w * 0.3f, h * 0.8f,
                w * 0.1f, h * 0.5f
            )
            close()
        }

        drawPath(
            path = eyePath,
            color = tint,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Center pupil/iris
        drawCircle(
            color = tint,
            radius = w * 0.18f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Diagonal slash if hidden
        if (!visible) {
            drawLine(
                color = tint,
                start = Offset(w * 0.15f, h * 0.15f),
                end = Offset(w * 0.85f, h * 0.85f),
                strokeWidth = strokeW * 1.1f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun SoftChatBubbleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color(0xFF6366F1)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val bubblePath = Path().apply {
            val r = w * 0.25f
            addRoundRect(
                RoundRect(
                    rect = Rect(w * 0.08f, h * 0.08f, w * 0.92f, h * 0.76f),
                    cornerRadius = CornerRadius(r, r)
                )
            )
            // Soft rounded tail
            moveTo(w * 0.26f, h * 0.74f)
            lineTo(w * 0.16f, h * 0.92f)
            lineTo(w * 0.42f, h * 0.76f)
            close()
        }

        drawPath(
            path = bubblePath,
            brush = Brush.linearGradient(
                colors = listOf(color, color.copy(alpha = 0.8f)),
                start = Offset.Zero,
                end = Offset(w, h)
            )
        )

        // 3 soft dots
        val dotRadius = w * 0.05f
        val dotY = h * 0.42f
        drawCircle(color = Color.White, radius = dotRadius, center = Offset(w * 0.36f, dotY))
        drawCircle(color = Color.White, radius = dotRadius, center = Offset(w * 0.50f, dotY))
        drawCircle(color = Color.White, radius = dotRadius, center = Offset(w * 0.64f, dotY))
    }
}

@Composable
fun SoftFingerprintIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    color: Color = Color(0xFF6366F1)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = w * 0.09f

        // Concentric soft curves
        val center = Offset(w * 0.5f, h * 0.56f)

        // Inner arch
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - w * 0.14f, center.y - h * 0.18f),
            size = Size(w * 0.28f, h * 0.36f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Middle arch
        drawArc(
            color = color,
            startAngle = 160f,
            sweepAngle = 210f,
            useCenter = false,
            topLeft = Offset(center.x - w * 0.26f, center.y - h * 0.32f),
            size = Size(w * 0.52f, h * 0.58f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Outer arch
        drawArc(
            color = color,
            startAngle = 150f,
            sweepAngle = 230f,
            useCenter = false,
            topLeft = Offset(center.x - w * 0.38f, center.y - h * 0.46f),
            size = Size(w * 0.76f, h * 0.80f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}



/**
 * Modern soft badge container that wraps an icon in a warm, glossy, or glowing pill.
 */
@Composable
fun SoftIconBadge(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 56.dp,
    backgroundColor: Color = Color(0xFFEEF2FF),
    borderColor: Color = Color(0xFFC7D2FE),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(badgeSize)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(badgeSize * 0.36f),
                ambientColor = borderColor.copy(alpha = 0.3f),
                spotColor = borderColor.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(badgeSize * 0.36f))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(badgeSize * 0.36f)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun SoftCheckBadge(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    badgeColor: Color = Color(0xFF3B82F6),
    checkColor: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Circular background with subtle glow
        drawCircle(
            color = badgeColor,
            radius = w / 2f,
            center = Offset(w / 2f, h / 2f)
        )

        // Smooth checkmark
        val checkPath = Path().apply {
            moveTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.68f)
            lineTo(w * 0.72f, h * 0.36f)
        }
        drawPath(
            path = checkPath,
            color = checkColor,
            style = Stroke(
                width = w * 0.14f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun SoftHeadsetIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color(0xFF10B981)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = w * 0.12f

        // Headband arch
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.15f, h * 0.12f),
            size = Size(w * 0.70f, h * 0.65f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Left earcup
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.10f, h * 0.44f),
            size = Size(w * 0.18f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )

        // Right earcup
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.72f, h * 0.44f),
            size = Size(w * 0.18f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )

        // Microphone arm
        val micPath = Path().apply {
            moveTo(w * 0.78f, h * 0.72f)
            cubicTo(
                w * 0.78f, h * 0.90f,
                w * 0.55f, h * 0.92f,
                w * 0.42f, h * 0.88f
            )
        }
        drawPath(
            path = micPath,
            color = color,
            style = Stroke(width = strokeW * 0.85f, cap = StrokeCap.Round)
        )
        // Mic tip
        drawCircle(
            color = color,
            radius = w * 0.07f,
            center = Offset(w * 0.40f, h * 0.88f)
        )
    }
}

@Composable
fun SoftCloseIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFF6B7280)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = w * 0.12f

        drawLine(
            color = color,
            start = Offset(w * 0.25f, h * 0.25f),
            end = Offset(w * 0.75f, h * 0.75f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.75f, h * 0.25f),
            end = Offset(w * 0.25f, h * 0.75f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SoftWarningIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Color(0xFFEF4444)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Soft rounded triangle
        val trianglePath = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.90f, h * 0.82f)
            lineTo(w * 0.10f, h * 0.82f)
            close()
        }

        drawPath(
            path = trianglePath,
            color = color.copy(alpha = 0.15f)
        )
        drawPath(
            path = trianglePath,
            color = color,
            style = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Exclamation stem
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.38f),
            end = Offset(w * 0.5f, h * 0.58f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
        // Exclamation dot
        drawCircle(
            color = color,
            radius = w * 0.05f,
            center = Offset(w * 0.5f, h * 0.70f)
        )
    }
}

@Composable
fun SoftArrowRightIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color = Color(0xFF6B7280)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = w * 0.12f

        val arrowPath = Path().apply {
            moveTo(w * 0.35f, h * 0.20f)
            lineTo(w * 0.65f, h * 0.50f)
            lineTo(w * 0.35f, h * 0.80f)
        }
        drawPath(
            path = arrowPath,
            color = color,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun SoftBackIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Color(0xFF1F2937)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = w * 0.12f

        val path = Path().apply {
            moveTo(w * 0.62f, h * 0.22f)
            lineTo(w * 0.32f, h * 0.50f)
            lineTo(w * 0.62f, h * 0.78f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun SoftTinyLockIcon(
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    tint: Color = Color(0xFF15803D)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Shackle arch
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.24f, h * 0.08f),
            size = Size(w * 0.52f, h * 0.44f),
            style = Stroke(width = w * 0.15f, cap = StrokeCap.Round)
        )

        // Body
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.15f, h * 0.40f),
            size = Size(w * 0.70f, h * 0.54f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
        )
    }
}

@Composable
fun SoftEmptyChatIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Glowing soft ambient circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFEF3C7).copy(alpha = 0.8f), Color(0xFFF3F4F6).copy(alpha = 0.1f)),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.48f
                ),
                radius = w * 0.45f,
                center = Offset(w * 0.5f, h * 0.5f)
            )

            // Primary soft chat bubble (yellow/gold)
            val bubble1 = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(w * 0.22f, h * 0.20f, w * 0.78f, h * 0.65f),
                        cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
                    )
                )
                moveTo(w * 0.32f, h * 0.64f)
                lineTo(w * 0.24f, h * 0.76f)
                lineTo(w * 0.44f, h * 0.65f)
                close()
            }
            drawPath(
                path = bubble1,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFD54F), Color(0xFFFFB300)),
                    start = Offset(w * 0.2f, h * 0.2f),
                    end = Offset(w * 0.8f, h * 0.7f)
                )
            )

            // Inner gloss line
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(w * 0.34f, h * 0.34f),
                end = Offset(w * 0.66f, h * 0.34f),
                strokeWidth = w * 0.045f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(w * 0.34f, h * 0.46f),
                end = Offset(w * 0.54f, h * 0.46f),
                strokeWidth = w * 0.045f,
                cap = StrokeCap.Round
            )

            // Small secondary bubble (indigo)
            val bubble2 = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(w * 0.52f, h * 0.50f, w * 0.88f, h * 0.82f),
                        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
                    )
                )
                moveTo(w * 0.82f, h * 0.80f)
                lineTo(w * 0.88f, h * 0.90f)
                lineTo(w * 0.74f, h * 0.82f)
                close()
            }
            drawPath(
                path = bubble2,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF818CF8), Color(0xFF4F46E5)),
                    start = Offset(w * 0.5f, h * 0.5f),
                    end = Offset(w * 0.9f, h * 0.9f)
                )
            )

            // Sparkle accent
            drawCircle(
                color = Color(0xFFFFC529),
                radius = w * 0.04f,
                center = Offset(w * 0.18f, h * 0.28f)
            )
            drawCircle(
                color = Color(0xFF60A5FA),
                radius = w * 0.03f,
                center = Offset(w * 0.85f, h * 0.36f)
            )
        }
    }
}

/**
 * Modern soft waving hand icon (pristine vector, no material icons, no emojis).
 */
@Composable
fun SoftWavingHandIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color(0xFF1E293B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f

        // Waving Hand Vector Path
        val handPath = Path().apply {
            moveTo(w * 0.35f, h * 0.88f)
            lineTo(w * 0.58f, h * 0.88f)
            cubicTo(w * 0.65f, h * 0.88f, w * 0.70f, h * 0.80f, w * 0.70f, h * 0.68f)
            // Pinky finger
            lineTo(w * 0.70f, h * 0.52f)
            cubicTo(w * 0.70f, h * 0.44f, w * 0.62f, h * 0.44f, w * 0.62f, h * 0.52f)
            // Ring finger
            lineTo(w * 0.62f, h * 0.40f)
            cubicTo(w * 0.62f, h * 0.32f, w * 0.54f, h * 0.32f, w * 0.54f, h * 0.40f)
            // Middle finger (tallest)
            lineTo(w * 0.54f, h * 0.28f)
            cubicTo(w * 0.54f, h * 0.20f, w * 0.45f, h * 0.20f, w * 0.45f, h * 0.28f)
            // Index finger
            lineTo(w * 0.45f, h * 0.36f)
            cubicTo(w * 0.45f, h * 0.28f, w * 0.37f, h * 0.28f, w * 0.37f, h * 0.36f)
            // Thumb extending out left
            lineTo(w * 0.37f, h * 0.52f)
            cubicTo(w * 0.30f, h * 0.54f, w * 0.18f, h * 0.58f, w * 0.18f, h * 0.68f)
            cubicTo(w * 0.18f, h * 0.76f, w * 0.28f, h * 0.76f, w * 0.35f, h * 0.70f)
            lineTo(w * 0.35f, h * 0.88f)
            close()
        }

        // Fill hand shape
        drawPath(path = handPath, color = color)

        // Wave motion arcs (top right)
        val waveArc1 = Path().apply {
            moveTo(w * 0.74f, h * 0.30f)
            cubicTo(w * 0.84f, h * 0.35f, w * 0.88f, h * 0.45f, w * 0.86f, h * 0.56f)
        }
        drawPath(path = waveArc1, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))

        val waveArc2 = Path().apply {
            moveTo(w * 0.86f, h * 0.18f)
            cubicTo(w * 0.98f, h * 0.26f, w * 1.02f, h * 0.42f, w * 0.98f, h * 0.58f)
        }
        drawPath(path = waveArc2, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

/**
 * Soft Exchange / Swap Icon (bidirectional arrows for language exchange).
 */
@Composable
fun SoftSwapIcon(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    color: Color = Color(0xFFD97706)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.15f

        // Top arrow pointing right ->
        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.35f),
            end = Offset(w * 0.85f, h * 0.35f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        val topArrowHead = Path().apply {
            moveTo(w * 0.65f, h * 0.20f)
            lineTo(w * 0.85f, h * 0.35f)
            lineTo(w * 0.65f, h * 0.50f)
        }
        drawPath(topArrowHead, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Bottom arrow pointing left <-
        drawLine(
            color = color,
            start = Offset(w * 0.85f, h * 0.65f),
            end = Offset(w * 0.15f, h * 0.65f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        val bottomArrowHead = Path().apply {
            moveTo(w * 0.35f, h * 0.50f)
            lineTo(w * 0.15f, h * 0.65f)
            lineTo(w * 0.35f, h * 0.80f)
        }
        drawPath(bottomArrowHead, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Custom Soft Checkmark Badge (no material check icon, no emoji).
 */
@Composable
fun SoftCheckmarkBadge(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    badgeColor: Color = Color(0xFFF59E0B),
    checkColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(size)
            .background(badgeColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.55f)) {
            val w = this.size.width
            val h = this.size.height
            val sw = w * 0.22f
            val checkPath = Path().apply {
                moveTo(w * 0.15f, h * 0.50f)
                lineTo(w * 0.42f, h * 0.78f)
                lineTo(w * 0.88f, h * 0.22f)
            }
            drawPath(
                path = checkPath,
                color = checkColor,
                style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * Custom Minimalist Outline Sparkle Icon (1.8-2px stroke, no material icons, no emojis).
 */
@Composable
fun SoftSparkleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.12f

        val sparklePath = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.92f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, h * 0.92f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.08f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, h * 0.08f)
            close()
        }
        drawPath(sparklePath, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Custom Minimalist Outline Gamepad Icon (for Interests chip).
 */
@Composable
fun SoftGamepadIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f

        val bodyPath = Path().apply {
            moveTo(w * 0.25f, h * 0.25f)
            lineTo(w * 0.75f, h * 0.25f)
            cubicTo(w * 0.92f, h * 0.25f, w * 0.95f, h * 0.52f, w * 0.90f, h * 0.75f)
            cubicTo(w * 0.85f, h * 0.90f, w * 0.65f, h * 0.85f, w * 0.60f, h * 0.65f)
            lineTo(w * 0.40f, h * 0.65f)
            cubicTo(w * 0.35f, h * 0.85f, w * 0.15f, h * 0.90f, w * 0.10f, h * 0.75f)
            cubicTo(w * 0.05f, h * 0.52f, w * 0.08f, h * 0.25f, w * 0.25f, h * 0.25f)
            close()
        }
        drawPath(bodyPath, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // D-Pad cross on left
        drawLine(color, Offset(w * 0.28f, h * 0.40f), Offset(w * 0.28f, h * 0.56f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.20f, h * 0.48f), Offset(w * 0.36f, h * 0.48f), sw, cap = StrokeCap.Round)

        // Action dots on right
        drawCircle(color, radius = sw * 0.7f, center = Offset(w * 0.72f, h * 0.40f))
        drawCircle(color, radius = sw * 0.7f, center = Offset(w * 0.80f, h * 0.48f))
    }
}

/**
 * Custom Minimalist Outline Globe Icon (for Language chip).
 */
@Composable
fun SoftGlobeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f
        val r = w * 0.42f

        drawCircle(color = color, radius = r, center = Offset(w / 2f, h / 2f), style = Stroke(sw))
        drawLine(color, Offset(w * 0.08f, h / 2f), Offset(w * 0.92f, h / 2f), sw, cap = StrokeCap.Round)

        val meridian = Path().apply {
            addOval(Rect(w * 0.28f, h * 0.08f, w * 0.72f, h * 0.92f))
        }
        drawPath(meridian, color, style = Stroke(sw))
    }
}

/**
 * Custom Minimalist Outline Vertical Three-Dots Menu Icon.
 */
@Composable
fun SoftThreeDotsIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val r = w * 0.08f

        drawCircle(color = color, radius = r, center = Offset(w / 2f, h * 0.22f))
        drawCircle(color = color, radius = r, center = Offset(w / 2f, h * 0.50f))
        drawCircle(color = color, radius = r, center = Offset(w / 2f, h * 0.78f))
    }
}

/**
 * Custom Minimalist Outline Star Icon (for Same Vibe chip).
 */
@Composable
fun SoftStarIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f

        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.62f, h * 0.35f)
            lineTo(w * 0.92f, h * 0.38f)
            lineTo(w * 0.70f, h * 0.58f)
            lineTo(w * 0.77f, h * 0.88f)
            lineTo(w * 0.50f, h * 0.72f)
            lineTo(w * 0.23f, h * 0.88f)
            lineTo(w * 0.30f, h * 0.58f)
            lineTo(w * 0.08f, h * 0.38f)
            lineTo(w * 0.38f, h * 0.35f)
            close()
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Custom Minimalist Outline Chevron Down Icon.
 */
@Composable
fun SoftChevronDownIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.12f

        val path = Path().apply {
            moveTo(w * 0.20f, h * 0.35f)
            lineTo(w * 0.50f, h * 0.65f)
            lineTo(w * 0.80f, h * 0.35f)
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SoftHeartIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    isLiked: Boolean = false,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.82f)
            cubicTo(w * 0.5f, h * 0.82f, w * 0.15f, h * 0.55f, w * 0.15f, h * 0.33f)
            cubicTo(w * 0.15f, h * 0.18f, w * 0.30f, h * 0.12f, w * 0.42f, h * 0.22f)
            lineTo(w * 0.5f, h * 0.32f)
            lineTo(w * 0.58f, h * 0.22f)
            cubicTo(w * 0.70f, h * 0.12f, w * 0.85f, h * 0.18f, w * 0.85f, h * 0.33f)
            cubicTo(w * 0.85f, h * 0.55f, w * 0.5f, h * 0.82f, w * 0.5f, h * 0.82f)
            close()
        }
        if (isLiked) {
            drawPath(path, color = Color(0xFFE11D48))
        } else {
            drawPath(path, color = color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}



@Composable
fun SoftShareIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f
        val path = Path().apply {
            moveTo(w * 0.1f, h * 0.85f)
            lineTo(w * 0.9f, h * 0.5f)
            lineTo(w * 0.1f, h * 0.15f)
            lineTo(w * 0.3f, h * 0.5f)
            close()
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(w * 0.3f, h * 0.5f), Offset(w * 0.9f, h * 0.5f), sw, cap = StrokeCap.Round)
    }
}

@Composable
fun SoftBookmarkIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    isBookmarked: Boolean = false,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.15f)
            lineTo(w * 0.8f, h * 0.15f)
            lineTo(w * 0.8f, h * 0.9f)
            lineTo(w * 0.5f, h * 0.72f)
            lineTo(w * 0.2f, h * 0.9f)
            close()
        }
        if (isBookmarked) {
            drawPath(path, color = color)
        } else {
            drawPath(path, color = color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun SoftFilterIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.12f
        drawLine(color, Offset(w * 0.1f, h * 0.3f), Offset(w * 0.9f, h * 0.3f), sw, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.35f, h * 0.3f))

        drawLine(color, Offset(w * 0.1f, h * 0.7f), Offset(w * 0.9f, h * 0.7f), sw, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.65f, h * 0.7f))
    }
}

@Composable
fun SoftPlusIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.15f
        drawLine(color, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.85f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.15f, h * 0.5f), Offset(w * 0.85f, h * 0.5f), sw, cap = StrokeCap.Round)
    }
}

@Composable
fun SoftVerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(color = Color(0xFF3B82F6), radius = w / 2f)
        val checkPath = Path().apply {
            moveTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.68f)
            lineTo(w * 0.74f, h * 0.32f)
        }
        drawPath(checkPath, color = Color.White, style = Stroke(width = w * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SoftPlayIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = w / 2f)
        val triangle = Path().apply {
            moveTo(w * 0.38f, h * 0.3f)
            lineTo(w * 0.72f, h * 0.5f)
            lineTo(w * 0.38f, h * 0.7f)
            close()
        }
        drawPath(triangle, color = Color.White)
    }
}

@Composable
fun SoftXIcon(modifier: Modifier = Modifier, size: Dp = 20.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.15f
        drawLine(color, Offset(w * 0.2f, h * 0.2f), Offset(w * 0.8f, h * 0.8f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.8f, h * 0.2f), Offset(w * 0.2f, h * 0.8f), sw, cap = StrokeCap.Round)
    }
}

@Composable
fun SoftNextIcon(modifier: Modifier = Modifier, size: Dp = 20.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.15f
        val path = Path().apply {
            moveTo(w * 0.3f, h * 0.2f)
            lineTo(w * 0.7f, h * 0.5f)
            lineTo(w * 0.3f, h * 0.8f)
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SoftImageIcon(modifier: Modifier = Modifier, size: Dp = 24.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.1f
        drawRect(color, topLeft = Offset(w * 0.15f, h * 0.2f), size = Size(w * 0.7f, h * 0.6f), style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.65f, h * 0.35f))
        val mountain = Path().apply {
            moveTo(w * 0.15f, h * 0.8f)
            lineTo(w * 0.4f, h * 0.45f)
            lineTo(w * 0.6f, h * 0.65f)
            lineTo(w * 0.85f, h * 0.3f)
            lineTo(w * 0.85f, h * 0.8f)
            close()
        }
        drawPath(mountain, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SoftVideoIcon(modifier: Modifier = Modifier, size: Dp = 24.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.1f
        drawRoundRect(color, topLeft = Offset(w * 0.1f, h * 0.25f), size = Size(w * 0.65f, h * 0.5f), cornerRadius = CornerRadius(w * 0.1f), style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val triangle = Path().apply {
            moveTo(w * 0.8f, h * 0.3f)
            lineTo(w * 0.95f, h * 0.5f)
            lineTo(w * 0.8f, h * 0.7f)
            close()
        }
        drawPath(triangle, color)
    }
}

@Composable
fun SoftTextIcon(modifier: Modifier = Modifier, size: Dp = 24.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.15f
        drawLine(color, Offset(w * 0.2f, h * 0.25f), Offset(w * 0.8f, h * 0.25f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.5f, h * 0.25f), Offset(w * 0.5f, h * 0.8f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.35f, h * 0.8f), Offset(w * 0.65f, h * 0.8f), sw, cap = StrokeCap.Round)
    }
}

@Composable
fun SoftPinIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFFD97706)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f

        // Pin head
        val headPath = Path().apply {
            moveTo(w * 0.35f, h * 0.15f)
            lineTo(w * 0.65f, h * 0.15f)
            lineTo(w * 0.58f, h * 0.35f)
            lineTo(w * 0.72f, h * 0.55f)
            lineTo(w * 0.28f, h * 0.55f)
            lineTo(w * 0.42f, h * 0.35f)
            close()
        }
        drawPath(headPath, color = color)

        // Pin needle
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.55f),
            end = Offset(w * 0.5f, h * 0.88f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SoftMuteIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFF9CA3AF)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.1f

        // Speaker cone
        val speakerPath = Path().apply {
            moveTo(w * 0.18f, h * 0.38f)
            lineTo(w * 0.32f, h * 0.38f)
            lineTo(w * 0.52f, h * 0.22f)
            lineTo(w * 0.52f, h * 0.78f)
            lineTo(w * 0.32f, h * 0.62f)
            lineTo(w * 0.18f, h * 0.62f)
            close()
        }
        drawPath(speakerPath, color = color)

        // Slash line
        drawLine(
            color = color,
            start = Offset(w * 0.62f, h * 0.32f),
            end = Offset(w * 0.88f, h * 0.68f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.88f, h * 0.32f),
            end = Offset(w * 0.62f, h * 0.68f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SoftVolumeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFF10B981)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.1f

        // Speaker cone
        val speakerPath = Path().apply {
            moveTo(w * 0.18f, h * 0.38f)
            lineTo(w * 0.32f, h * 0.38f)
            lineTo(w * 0.52f, h * 0.22f)
            lineTo(w * 0.52f, h * 0.78f)
            lineTo(w * 0.32f, h * 0.62f)
            lineTo(w * 0.18f, h * 0.62f)
            close()
        }
        drawPath(speakerPath, color = color)

        // Sound waves
        drawArc(
            color = color,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.42f, h * 0.30f),
            size = Size(w * 0.36f, h * 0.40f),
            style = Stroke(sw, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.52f, h * 0.20f),
            size = Size(w * 0.42f, h * 0.60f),
            style = Stroke(sw, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SoftMailIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFF3B82F6)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.1f

        // Envelope body
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.24f),
            size = Size(w * 0.76f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Envelope fold
        val foldPath = Path().apply {
            moveTo(w * 0.16f, h * 0.30f)
            lineTo(w * 0.50f, h * 0.54f)
            lineTo(w * 0.84f, h * 0.30f)
        }
        drawPath(foldPath, color = color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SoftTrashIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFFDC2626)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.1f

        // Lid top handle
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.38f, h * 0.12f),
            size = Size(w * 0.24f, h * 0.18f),
            style = Stroke(sw, cap = StrokeCap.Round)
        )

        // Lid line
        drawLine(
            color = color,
            start = Offset(w * 0.16f, h * 0.26f),
            end = Offset(w * 0.84f, h * 0.26f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )

        // Can body
        val canPath = Path().apply {
            moveTo(w * 0.24f, h * 0.28f)
            lineTo(w * 0.28f, h * 0.82f)
            quadraticTo(w * 0.29f, h * 0.88f, w * 0.36f, h * 0.88f)
            lineTo(w * 0.64f, h * 0.88f)
            quadraticTo(w * 0.71f, h * 0.88f, w * 0.72f, h * 0.82f)
            lineTo(w * 0.76f, h * 0.28f)
        }
        drawPath(canPath, color = color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Inner vertical rib lines
        drawLine(color, Offset(w * 0.42f, h * 0.40f), Offset(w * 0.42f, h * 0.76f), sw * 0.8f, StrokeCap.Round)
        drawLine(color, Offset(w * 0.58f, h * 0.40f), Offset(w * 0.58f, h * 0.76f), sw * 0.8f, StrokeCap.Round)
    }
}

@Composable
fun SoftBlockIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFFDC2626)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f

        // Outer circle
        drawCircle(
            color = color,
            radius = w * 0.38f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(sw)
        )

        // Diagonal slash
        val offset = w * 0.26f
        drawLine(
            color = color,
            start = Offset(w * 0.5f - offset, h * 0.5f - offset),
            end = Offset(w * 0.5f + offset, h * 0.5f + offset),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SoftGroupIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFFD97706)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f

        // Center person
        drawCircle(color, radius = w * 0.16f, center = Offset(w * 0.50f, h * 0.32f), style = Stroke(sw))
        val centerBody = Path().apply {
            moveTo(w * 0.26f, h * 0.84f)
            quadraticTo(w * 0.26f, h * 0.58f, w * 0.50f, h * 0.58f)
            quadraticTo(w * 0.74f, h * 0.58f, w * 0.74f, h * 0.84f)
        }
        drawPath(centerBody, color = color, style = Stroke(sw, cap = StrokeCap.Round))

        // Side person (left)
        drawCircle(color.copy(alpha = 0.7f), radius = w * 0.12f, center = Offset(w * 0.24f, h * 0.36f), style = Stroke(sw * 0.9f))
        val leftBody = Path().apply {
            moveTo(w * 0.10f, h * 0.82f)
            quadraticTo(w * 0.10f, h * 0.62f, w * 0.26f, h * 0.62f)
        }
        drawPath(leftBody, color = color.copy(alpha = 0.7f), style = Stroke(sw * 0.9f, cap = StrokeCap.Round))

        // Side person (right)
        drawCircle(color.copy(alpha = 0.7f), radius = w * 0.12f, center = Offset(w * 0.76f, h * 0.36f), style = Stroke(sw * 0.9f))
        val rightBody = Path().apply {
            moveTo(w * 0.74f, h * 0.62f)
            quadraticTo(w * 0.90f, h * 0.62f, w * 0.90f, h * 0.82f)
        }
        drawPath(rightBody, color = color.copy(alpha = 0.7f), style = Stroke(sw * 0.9f, cap = StrokeCap.Round))
    }
}

@Composable
fun SoftBroomIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFF6B7280)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.1f

        // Handle
        drawLine(
            color = color,
            start = Offset(w * 0.75f, h * 0.15f),
            end = Offset(w * 0.45f, h * 0.50f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )

        // Bristles base
        val bristlePath = Path().apply {
            moveTo(w * 0.35f, h * 0.45f)
            lineTo(w * 0.55f, h * 0.55f)
            lineTo(w * 0.30f, h * 0.85f)
            lineTo(w * 0.15f, h * 0.70f)
            close()
        }
        drawPath(bristlePath, color = color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Sparkle
        drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.75f, h * 0.60f))
        drawCircle(color, radius = w * 0.035f, center = Offset(w * 0.85f, h * 0.45f))
    }
}

@Composable
fun SoftCheckIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color.Black,
    strokeWidth: Float = 2.2f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth * density

        val checkPath = Path().apply {
            moveTo(w * 0.20f, h * 0.50f)
            lineTo(w * 0.42f, h * 0.74f)
            lineTo(w * 0.80f, h * 0.26f)
        }
        drawPath(
            path = checkPath,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun SoftPersonAddIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFFD97706)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.1f

        // Person Head
        drawCircle(color, radius = w * 0.18f, center = Offset(w * 0.40f, h * 0.32f), style = Stroke(sw))

        // Person Body
        val body = Path().apply {
            moveTo(w * 0.14f, h * 0.84f)
            quadraticTo(w * 0.14f, h * 0.58f, w * 0.40f, h * 0.58f)
            quadraticTo(w * 0.66f, h * 0.58f, w * 0.66f, h * 0.84f)
        }
        drawPath(body, color = color, style = Stroke(sw, cap = StrokeCap.Round))

        // Plus (+)
        drawLine(color, Offset(w * 0.80f, h * 0.35f), Offset(w * 0.80f, h * 0.65f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.65f, h * 0.50f), Offset(w * 0.95f, h * 0.50f), sw, StrokeCap.Round)
    }
}

@Composable
fun SoftDownloadIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f

        // Down Arrow stem
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.18f),
            end = Offset(w * 0.50f, h * 0.62f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )

        // Arrow head
        val arrowHead = Path().apply {
            moveTo(w * 0.30f, h * 0.44f)
            lineTo(w * 0.50f, h * 0.65f)
            lineTo(w * 0.70f, h * 0.44f)
        }
        drawPath(
            path = arrowHead,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Bottom tray line
        drawLine(
            color = color,
            start = Offset(w * 0.20f, h * 0.82f),
            end = Offset(w * 0.80f, h * 0.82f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SoftRefreshIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f

        // Circular arrow arc
        drawArc(
            color = color,
            startAngle = 40f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(w * 0.18f, h * 0.18f),
            size = Size(w * 0.64f, h * 0.64f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )

        // Arrow tip at top
        val tip = Path().apply {
            moveTo(w * 0.70f, h * 0.12f)
            lineTo(w * 0.86f, h * 0.24f)
            lineTo(w * 0.68f, h * 0.36f)
        }
        drawPath(path = tip, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SoftPauseIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.18f
        drawLine(color = color, start = Offset(w * 0.35f, h * 0.22f), end = Offset(w * 0.35f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.65f, h * 0.22f), end = Offset(w * 0.65f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun SoftGavelIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.12f

        // Handle angled
        drawLine(color = color, start = Offset(w * 0.22f, h * 0.78f), end = Offset(w * 0.52f, h * 0.48f), strokeWidth = sw, cap = StrokeCap.Round)

        // Head block
        rotate(degrees = -45f, pivot = Offset(w * 0.62f, h * 0.38f)) {
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.48f, h * 0.26f),
                size = Size(w * 0.28f, h * 0.24f),
                cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
            )
        }
    }
}

@Composable
fun SoftCopyIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f

        // Back sheet
        drawRoundRect(
            color = color.copy(alpha = 0.45f),
            topLeft = Offset(w * 0.30f, h * 0.10f),
            size = Size(w * 0.58f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )

        // Front sheet
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.28f),
            size = Size(w * 0.58f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SoftPersonIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f

        // Head
        drawCircle(
            color = color,
            radius = w * 0.20f,
            center = Offset(w * 0.50f, h * 0.32f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )

        // Shoulders arc
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.18f, h * 0.56f),
            size = Size(w * 0.64f, h * 0.38f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SoftSearchIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f

        // Glass circle
        drawCircle(
            color = color,
            radius = w * 0.28f,
            center = Offset(w * 0.44f, h * 0.44f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )

        // Handle
        drawLine(
            color = color,
            start = Offset(w * 0.64f, h * 0.64f),
            end = Offset(w * 0.84f, h * 0.84f),
            strokeWidth = sw * 1.2f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SoftMicIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.34f, h * 0.12f),
            size = Size(w * 0.32f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
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

        drawLine(color, Offset(w * 0.50f, h * 0.72f), Offset(w * 0.50f, h * 0.88f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.34f, h * 0.88f), Offset(w * 0.66f, h * 0.88f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun SoftMicOffIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFF9CA3AF)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.34f, h * 0.12f),
            size = Size(w * 0.32f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawArc(
            color = color,
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(w * 0.22f, h * 0.26f),
            size = Size(w * 0.56f, h * 0.46f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )

        drawLine(color, Offset(w * 0.50f, h * 0.72f), Offset(w * 0.50f, h * 0.88f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.34f, h * 0.88f), Offset(w * 0.66f, h * 0.88f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.16f, h * 0.18f), Offset(w * 0.84f, h * 0.84f), strokeWidth = sw * 1.1f, cap = StrokeCap.Round)
    }
}

@Composable
fun SoftAddPhotoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color(0xFFF59E0B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.22f),
            size = Size(w * 0.66f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val mountain = Path().apply {
            moveTo(w * 0.18f, h * 0.74f)
            lineTo(w * 0.36f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.66f)
            lineTo(w * 0.62f, h * 0.50f)
            lineTo(w * 0.72f, h * 0.74f)
        }
        drawPath(mountain, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Plus badge in top right
        drawCircle(color = color, radius = w * 0.16f, center = Offset(w * 0.78f, h * 0.24f))
        drawLine(Color.White, Offset(w * 0.78f, h * 0.15f), Offset(w * 0.78f, h * 0.33f), strokeWidth = sw * 0.9f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.69f, h * 0.24f), Offset(w * 0.87f, h * 0.24f), strokeWidth = sw * 0.9f, cap = StrokeCap.Round)
    }
}

@Composable
fun SoftMegaphoneIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f

        val horn = Path().apply {
            moveTo(w * 0.18f, h * 0.42f)
            lineTo(w * 0.74f, h * 0.20f)
            lineTo(w * 0.74f, h * 0.76f)
            lineTo(w * 0.18f, h * 0.54f)
            close()
        }
        drawPath(horn, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(w * 0.28f, h * 0.58f), Offset(w * 0.34f, h * 0.84f), strokeWidth = sw, cap = StrokeCap.Round)
        drawArc(
            color = color,
            startAngle = -35f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(w * 0.72f, h * 0.30f),
            size = Size(w * 0.18f, h * 0.36f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SoftPersonOffIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f

        drawCircle(color = color, radius = w * 0.18f, center = Offset(w * 0.50f, h * 0.30f), style = Stroke(width = sw))
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.18f, h * 0.54f),
            size = Size(w * 0.64f, h * 0.38f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
        drawLine(color, Offset(w * 0.16f, h * 0.16f), Offset(w * 0.84f, h * 0.84f), strokeWidth = sw * 1.1f, cap = StrokeCap.Round)
    }
}

@Composable
fun SoftHelpIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f

        drawCircle(color = color, radius = w * 0.40f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = sw))
        val qPath = Path().apply {
            moveTo(w * 0.38f, h * 0.36f)
            cubicTo(w * 0.38f, h * 0.24f, w * 0.62f, h * 0.24f, w * 0.62f, h * 0.38f)
            cubicTo(w * 0.62f, h * 0.48f, w * 0.50f, h * 0.48f, w * 0.50f, h * 0.58f)
        }
        drawPath(qPath, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))
        drawCircle(color = color, radius = sw * 0.65f, center = Offset(w * 0.50f, h * 0.72f))
    }
}

@Composable
fun SoftDocumentIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFFFFB300)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.20f, h * 0.12f),
            size = Size(w * 0.60f, h * 0.76f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(color, Offset(w * 0.34f, h * 0.36f), Offset(w * 0.66f, h * 0.36f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.34f, h * 0.52f), Offset(w * 0.66f, h * 0.52f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.34f, h * 0.68f), Offset(w * 0.54f, h * 0.68f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun SoftOpenInNewIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color = Color(0xFF8E8E93)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f

        val boxPath = Path().apply {
            moveTo(w * 0.48f, h * 0.22f)
            lineTo(w * 0.20f, h * 0.22f)
            lineTo(w * 0.20f, h * 0.80f)
            lineTo(w * 0.78f, h * 0.80f)
            lineTo(w * 0.78f, h * 0.52f)
        }
        drawPath(boxPath, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(w * 0.48f, h * 0.52f), Offset(w * 0.84f, h * 0.16f), strokeWidth = sw, cap = StrokeCap.Round)
        val head = Path().apply {
            moveTo(w * 0.60f, h * 0.16f)
            lineTo(w * 0.84f, h * 0.16f)
            lineTo(w * 0.84f, h * 0.40f)
        }
        drawPath(head, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SoftForwardArrowIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.12f

        drawLine(color, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.78f, h * 0.50f), strokeWidth = sw, cap = StrokeCap.Round)
        val head = Path().apply {
            moveTo(w * 0.52f, h * 0.24f)
            lineTo(w * 0.78f, h * 0.50f)
            lineTo(w * 0.52f, h * 0.76f)
        }
        drawPath(head, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SoftCakeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = Color(0xFFFFC529)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.08f

        // Cake base
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.16f, h * 0.46f),
            size = Size(w * 0.68f, h * 0.38f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Frosting wave
        val wave = Path().apply {
            moveTo(w * 0.16f, h * 0.60f)
            quadraticTo(w * 0.33f, h * 0.70f, w * 0.50f, h * 0.60f)
            quadraticTo(w * 0.67f, h * 0.70f, w * 0.84f, h * 0.60f)
        }
        drawPath(wave, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))
        // Candle & flame
        drawLine(color, Offset(w * 0.50f, h * 0.46f), Offset(w * 0.50f, h * 0.30f), strokeWidth = sw, cap = StrokeCap.Round)
        drawCircle(color = color, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.18f))
    }
}

@Composable
fun SoftLocationPinIcon(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.12f

        val pin = Path().apply {
            moveTo(w * 0.50f, h * 0.88f)
            cubicTo(w * 0.20f, h * 0.58f, w * 0.20f, h * 0.24f, w * 0.50f, h * 0.14f)
            cubicTo(w * 0.80f, h * 0.24f, w * 0.80f, h * 0.58f, w * 0.50f, h * 0.88f)
            close()
        }
        drawPath(pin, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color = color, radius = w * 0.10f, center = Offset(w * 0.50f, h * 0.40f))
    }
}

@Composable
fun SoftFaceIcon(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    color: Color = Color(0xFF3B82F6)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.08f

        drawCircle(color = color, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = sw))
        drawCircle(color = color, radius = w * 0.055f, center = Offset(w * 0.37f, h * 0.42f))
        drawCircle(color = color, radius = w * 0.055f, center = Offset(w * 0.63f, h * 0.42f))
        val smile = Path().apply {
            moveTo(w * 0.34f, h * 0.58f)
            quadraticTo(w * 0.50f, h * 0.72f, w * 0.66f, h * 0.58f)
        }
        drawPath(smile, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

@Composable
fun SoftShopBagIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFFFFB627)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.18f, h * 0.32f),
            size = Size(w * 0.64f, h * 0.54f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.32f, h * 0.12f),
            size = Size(w * 0.36f, h * 0.34f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SoftCoinIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color(0xFFF59E0B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(color = color, radius = w * 0.44f, center = Offset(w * 0.5f, h * 0.5f))
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = w * 0.28f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = w * 0.09f)
        )
    }
}

@Composable
fun SoftGiftBoxIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color(0xFFF59E0B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.18f, h * 0.36f),
            size = Size(w * 0.64f, h * 0.50f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(color, Offset(w * 0.14f, h * 0.36f), Offset(w * 0.86f, h * 0.36f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.50f, h * 0.36f), Offset(w * 0.50f, h * 0.86f), strokeWidth = sw, cap = StrokeCap.Round)
        drawCircle(color = color, radius = w * 0.10f, center = Offset(w * 0.38f, h * 0.24f), style = Stroke(width = sw))
        drawCircle(color = color, radius = w * 0.10f, center = Offset(w * 0.62f, h * 0.24f), style = Stroke(width = sw))
    }
}

@Composable
fun SoftInfoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f
        drawCircle(color = color, radius = w * 0.40f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = sw))
        drawCircle(color = color, radius = sw * 0.65f, center = Offset(w * 0.5f, h * 0.30f))
        drawLine(color, Offset(w * 0.5f, h * 0.46f), Offset(w * 0.5f, h * 0.72f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun SoftExitIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFFEF4444)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f
        val frame = Path().apply {
            moveTo(w * 0.52f, h * 0.16f)
            lineTo(w * 0.20f, h * 0.16f)
            lineTo(w * 0.20f, h * 0.84f)
            lineTo(w * 0.52f, h * 0.84f)
        }
        drawPath(frame, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(w * 0.42f, h * 0.50f), Offset(w * 0.84f, h * 0.50f), strokeWidth = sw, cap = StrokeCap.Round)
        val arrow = Path().apply {
            moveTo(w * 0.66f, h * 0.32f)
            lineTo(w * 0.84f, h * 0.50f)
            lineTo(w * 0.66f, h * 0.68f)
        }
        drawPath(arrow, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SoftBellIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f
        val bell = Path().apply {
            moveTo(w * 0.26f, h * 0.62f)
            lineTo(w * 0.26f, h * 0.42f)
            cubicTo(w * 0.26f, h * 0.22f, w * 0.74f, h * 0.22f, w * 0.74f, h * 0.42f)
            lineTo(w * 0.74f, h * 0.62f)
            lineTo(w * 0.84f, h * 0.72f)
            lineTo(w * 0.16f, h * 0.72f)
            close()
        }
        drawPath(bell, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(w * 0.42f, h * 0.84f), Offset(w * 0.58f, h * 0.84f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun SoftCrownIcon(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    color: Color = Color(0xFFFFB627)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val crown = Path().apply {
            moveTo(w * 0.14f, h * 0.76f)
            lineTo(w * 0.10f, h * 0.30f)
            lineTo(w * 0.34f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.66f, h * 0.52f)
            lineTo(w * 0.90f, h * 0.30f)
            lineTo(w * 0.86f, h * 0.76f)
            close()
        }
        drawPath(crown, color = color)
    }
}

@Composable
fun SoftMusicNoteIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f
        drawCircle(color = color, radius = w * 0.14f, center = Offset(w * 0.34f, h * 0.74f))
        drawCircle(color = color, radius = w * 0.14f, center = Offset(w * 0.72f, h * 0.66f))
        drawLine(color, Offset(w * 0.46f, h * 0.72f), Offset(w * 0.46f, h * 0.22f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.84f, h * 0.64f), Offset(w * 0.84f, h * 0.16f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.46f, h * 0.22f), Offset(w * 0.84f, h * 0.16f), strokeWidth = sw * 1.2f, cap = StrokeCap.Round)
    }
}

@Composable
fun SoftSettingsGearIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f
        val cx = w * 0.5f
        val cy = h * 0.5f
        drawCircle(color = color, radius = w * 0.24f, center = Offset(cx, cy), style = Stroke(width = sw))
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val sx = cx + (w * 0.28f * kotlin.math.cos(angle)).toFloat()
            val sy = cy + (h * 0.28f * kotlin.math.sin(angle)).toFloat()
            val ex = cx + (w * 0.40f * kotlin.math.cos(angle)).toFloat()
            val ey = cy + (h * 0.40f * kotlin.math.sin(angle)).toFloat()
            drawLine(color, Offset(sx, sy), Offset(ex, ey), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun SoftSunIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    color: Color = Color(0xFFF59E0B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f
        val cx = w * 0.5f
        val cy = h * 0.5f
        drawCircle(color = color, radius = w * 0.22f, center = Offset(cx, cy))
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val sx = cx + (w * 0.30f * kotlin.math.cos(angle)).toFloat()
            val sy = cy + (h * 0.30f * kotlin.math.sin(angle)).toFloat()
            val ex = cx + (w * 0.42f * kotlin.math.cos(angle)).toFloat()
            val ey = cy + (h * 0.42f * kotlin.math.sin(angle)).toFloat()
            drawLine(color, Offset(sx, sy), Offset(ex, ey), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun SoftMoonIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    color: Color = Color(0xFFFBBF24)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val moon = Path().apply {
            moveTo(w * 0.64f, h * 0.16f)
            cubicTo(w * 0.30f, h * 0.18f, w * 0.16f, h * 0.54f, w * 0.42f, h * 0.78f)
            cubicTo(w * 0.64f, h * 0.92f, w * 0.86f, h * 0.72f, w * 0.82f, h * 0.56f)
            cubicTo(w * 0.62f, h * 0.68f, w * 0.42f, h * 0.48f, w * 0.64f, h * 0.16f)
            close()
        }
        drawPath(moon, color = color)
    }
}

@Composable
fun SoftHourglassIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color = Color(0xFFB45309)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f
        drawLine(color, Offset(w * 0.24f, h * 0.16f), Offset(w * 0.76f, h * 0.16f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.24f, h * 0.84f), Offset(w * 0.76f, h * 0.84f), strokeWidth = sw, cap = StrokeCap.Round)
        val glass = Path().apply {
            moveTo(w * 0.28f, h * 0.16f)
            lineTo(w * 0.72f, h * 0.16f)
            lineTo(w * 0.50f, h * 0.50f)
            lineTo(w * 0.72f, h * 0.84f)
            lineTo(w * 0.28f, h * 0.84f)
            lineTo(w * 0.50f, h * 0.50f)
            close()
        }
        drawPath(glass, color = color, style = Stroke(width = sw * 0.85f, join = StrokeJoin.Round))
    }
}

@Composable
fun SoftBackChevronIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f
        val chevron = Path().apply {
            moveTo(w * 0.66f, h * 0.20f)
            lineTo(w * 0.34f, h * 0.50f)
            lineTo(w * 0.66f, h * 0.80f)
        }
        drawPath(
            path = chevron,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun SoftPhoneCallIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Color(0xFF1F160C)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.095f
        val handset = Path().apply {
            moveTo(w * 0.27f, h * 0.17f)
            cubicTo(w * 0.20f, h * 0.17f, w * 0.16f, h * 0.24f, w * 0.17f, h * 0.33f)
            cubicTo(w * 0.20f, h * 0.61f, w * 0.39f, h * 0.80f, w * 0.67f, h * 0.83f)
            cubicTo(w * 0.76f, h * 0.84f, w * 0.83f, h * 0.80f, w * 0.83f, h * 0.73f)
            lineTo(w * 0.83f, h * 0.65f)
            cubicTo(w * 0.83f, h * 0.60f, w * 0.78f, h * 0.57f, w * 0.72f, h * 0.57f)
            lineTo(w * 0.63f, h * 0.57f)
            cubicTo(w * 0.58f, h * 0.57f, w * 0.55f, h * 0.61f, w * 0.53f, h * 0.64f)
            cubicTo(w * 0.44f, h * 0.59f, w * 0.41f, h * 0.56f, w * 0.36f, h * 0.47f)
            cubicTo(w * 0.39f, h * 0.45f, w * 0.43f, h * 0.42f, w * 0.43f, h * 0.37f)
            lineTo(w * 0.43f, h * 0.28f)
            cubicTo(w * 0.43f, h * 0.22f, w * 0.40f, h * 0.17f, w * 0.35f, h * 0.17f)
            close()
        }
        drawPath(
            path = handset,
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Soft subtle call ring arc at top-right
        drawArc(
            color = color,
            startAngle = -80f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(w * 0.50f, h * 0.16f),
            size = Size(w * 0.34f, h * 0.34f),
            style = Stroke(width = sw * 0.85f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SoftPhoneEndIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    color: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val handset = Path().apply {
            moveTo(w * 0.14f, h * 0.56f)
            cubicTo(w * 0.14f, h * 0.41f, w * 0.31f, h * 0.33f, w * 0.50f, h * 0.33f)
            cubicTo(w * 0.69f, h * 0.33f, w * 0.86f, h * 0.41f, w * 0.86f, h * 0.56f)
            cubicTo(w * 0.86f, h * 0.62f, w * 0.81f, h * 0.66f, w * 0.74f, h * 0.66f)
            lineTo(w * 0.67f, h * 0.66f)
            cubicTo(w * 0.62f, h * 0.66f, w * 0.59f, h * 0.62f, w * 0.59f, h * 0.57f)
            lineTo(w * 0.59f, h * 0.52f)
            cubicTo(w * 0.53f, h * 0.50f, w * 0.47f, h * 0.50f, w * 0.41f, h * 0.52f)
            lineTo(w * 0.41f, h * 0.57f)
            cubicTo(w * 0.41f, h * 0.62f, w * 0.38f, h * 0.66f, w * 0.33f, h * 0.66f)
            lineTo(w * 0.26f, h * 0.66f)
            cubicTo(w * 0.19f, h * 0.66f, w * 0.14f, h * 0.62f, w * 0.14f, h * 0.56f)
            close()
        }
        drawPath(path = handset, color = color)
    }
}

@Composable
fun SoftVoiceWaveIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Color(0xFF9CA3AF),
    levels: List<Float> = listOf(0.42f, 0.78f, 1.0f, 0.68f, 0.38f)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val count = levels.size.coerceAtLeast(3)
        val barWidth = w / (count * 2f - 1f) * 0.88f
        val gap = (w - barWidth * count) / (count - 1).coerceAtLeast(1)
        val centerY = h * 0.5f

        levels.forEachIndexed { idx, rawLevel ->
            val level = rawLevel.coerceIn(0.18f, 1.0f)
            val barH = h * 0.84f * level
            val x = idx * (barWidth + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, centerY - barH / 2f),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun SoftFlameIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color = Color(0xFFF97316),
    coreColor: Color = Color(0xFFFDE047)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val outer = Path().apply {
            moveTo(w * 0.52f, h * 0.10f)
            cubicTo(w * 0.68f, h * 0.28f, w * 0.82f, h * 0.42f, w * 0.80f, h * 0.62f)
            cubicTo(w * 0.78f, h * 0.81f, w * 0.65f, h * 0.91f, w * 0.50f, h * 0.91f)
            cubicTo(w * 0.31f, h * 0.91f, w * 0.20f, h * 0.78f, w * 0.20f, h * 0.60f)
            cubicTo(w * 0.20f, h * 0.44f, w * 0.30f, h * 0.34f, w * 0.38f, h * 0.26f)
            cubicTo(w * 0.39f, h * 0.36f, w * 0.43f, h * 0.41f, w * 0.46f, h * 0.41f)
            cubicTo(w * 0.46f, h * 0.28f, w * 0.48f, h * 0.18f, w * 0.52f, h * 0.10f)
            close()
        }
        drawPath(
            path = outer,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFBBF24), color, Color(0xFFEA580C))
            )
        )
        val inner = Path().apply {
            moveTo(w * 0.50f, h * 0.44f)
            cubicTo(w * 0.62f, h * 0.54f, w * 0.66f, h * 0.65f, w * 0.63f, h * 0.76f)
            cubicTo(w * 0.60f, h * 0.84f, w * 0.40f, h * 0.84f, w * 0.37f, h * 0.75f)
            cubicTo(w * 0.34f, h * 0.64f, w * 0.40f, h * 0.52f, w * 0.50f, h * 0.44f)
            close()
        }
        drawPath(path = inner, color = coreColor.copy(alpha = 0.90f))
    }
}

@Composable
fun SoftWheelCoinArtwork(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    goldColor: Color = Color(0xFFFFB627),
    highlightColor: Color = Color(0xFFFFF0A6)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = w * 0.44f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(highlightColor, goldColor, Color(0xFFD97706)),
                center = Offset(cx - r * 0.25f, cy - r * 0.25f),
                radius = r * 1.35f
            ),
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFFFFF8D6).copy(alpha = 0.65f),
            radius = r * 0.72f,
            center = Offset(cx, cy),
            style = Stroke(width = w * 0.065f)
        )
        // Soft glossy heart/star center accent
        val star = Path().apply {
            moveTo(cx, cy - r * 0.36f)
            quadraticTo(cx, cy, cx + r * 0.36f, cy)
            quadraticTo(cx, cy, cx, cy + r * 0.36f)
            quadraticTo(cx, cy, cx - r * 0.36f, cy)
            quadraticTo(cx, cy, cx, cy - r * 0.36f)
            close()
        }
        drawPath(star, color = Color(0xFFFFFBE6))
    }
}

@Composable
fun SoftWheelGiftArtwork(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    accentColor: Color = Color(0xFF22C55E)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.08f
        // Box base
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = 0.95f), accentColor.copy(alpha = 0.70f))
            ),
            topLeft = Offset(w * 0.18f, h * 0.40f),
            size = Size(w * 0.64f, h * 0.46f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
        )
        // Box lid
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White.copy(alpha = 0.35f), accentColor)
            ),
            topLeft = Offset(w * 0.13f, h * 0.30f),
            size = Size(w * 0.74f, h * 0.15f),
            cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
        )
        // Ribbon stripe
        drawRoundRect(
            color = Color(0xFFFFE082),
            topLeft = Offset(w * 0.44f, h * 0.30f),
            size = Size(w * 0.12f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
        // Soft bow loops
        drawCircle(
            color = Color(0xFFFFE082),
            radius = w * 0.10f,
            center = Offset(w * 0.38f, h * 0.21f),
            style = Stroke(width = sw)
        )
        drawCircle(
            color = Color(0xFFFFE082),
            radius = w * 0.10f,
            center = Offset(w * 0.62f, h * 0.21f),
            style = Stroke(width = sw)
        )
    }
}

@Composable
fun SoftWheelCrownArtwork(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    accentColor: Color = Color(0xFFFB7185)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val crown = Path().apply {
            moveTo(w * 0.15f, h * 0.76f)
            lineTo(w * 0.12f, h * 0.32f)
            lineTo(w * 0.34f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.66f, h * 0.52f)
            lineTo(w * 0.88f, h * 0.32f)
            lineTo(w * 0.85f, h * 0.76f)
            close()
        }
        drawPath(
            path = crown,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFD6DF), accentColor, accentColor.copy(alpha = 0.82f))
            )
        )
        drawCircle(color = Color.White.copy(alpha = 0.90f), radius = w * 0.055f, center = Offset(w * 0.50f, h * 0.19f))
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = w * 0.045f, center = Offset(w * 0.12f, h * 0.30f))
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = w * 0.045f, center = Offset(w * 0.88f, h * 0.30f))
    }
}

@Composable
fun SoftWheelStarArtwork(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    accentColor: Color = Color(0xFFA78BFA)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.52f
        val outerR = w * 0.42f
        val innerR = w * 0.19f
        val star = Path().apply {
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) outerR else innerR
                val deg = -90.0 + i * 36.0
                val rad = Math.toRadians(deg)
                val x = cx + (r * kotlin.math.cos(rad)).toFloat()
                val y = cy + (r * kotlin.math.sin(rad)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(
            path = star,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFEDE9FE), accentColor, Color(0xFF7C3AED))
            )
        )
    }
}

@Composable
fun SoftWheelSparkleArtwork(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    accentColor: Color = Color(0xFFFBBF24)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        fun fourPoint(cx: Float, cy: Float, r: Float): Path = Path().apply {
            moveTo(cx, cy - r)
            quadraticTo(cx, cy, cx + r, cy)
            quadraticTo(cx, cy, cx, cy + r)
            quadraticTo(cx, cy, cx - r, cy)
            quadraticTo(cx, cy, cx, cy - r)
            close()
        }
        drawPath(
            path = fourPoint(w * 0.46f, h * 0.52f, w * 0.36f),
            brush = Brush.radialGradient(
                colors = listOf(Color.White, accentColor, accentColor.copy(alpha = 0.8f)),
                center = Offset(w * 0.44f, h * 0.48f),
                radius = w * 0.42f
            )
        )
        drawPath(
            path = fourPoint(w * 0.78f, h * 0.24f, w * 0.16f),
            color = Color(0xFFFFF8D6)
        )
    }
}

@Composable
fun SoftScreenShareIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFF0F172A)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f

        // Soft rounded monitor frame
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.11f, h * 0.16f),
            size = Size(w * 0.78f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Monitor stand base
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.84f),
            end = Offset(w * 0.66f, h * 0.84f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.68f),
            end = Offset(w * 0.50f, h * 0.84f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )

        // Upward share arrow inside screen
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.55f),
            end = Offset(w * 0.50f, h * 0.30f),
            strokeWidth = sw * 0.95f,
            cap = StrokeCap.Round
        )
        val chevron = Path().apply {
            moveTo(w * 0.37f, h * 0.41f)
            lineTo(w * 0.50f, h * 0.29f)
            lineTo(w * 0.63f, h * 0.41f)
        }
        drawPath(
            path = chevron,
            color = color,
            style = Stroke(width = sw * 0.95f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun SoftTvWatchIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFFF59E0B)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f

        drawRoundRect(
            color = color.copy(alpha = 0.16f),
            topLeft = Offset(w * 0.10f, h * 0.18f),
            size = Size(w * 0.80f, h * 0.54f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.10f, h * 0.18f),
            size = Size(w * 0.80f, h * 0.54f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.32f, h * 0.84f),
            end = Offset(w * 0.68f, h * 0.84f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        val play = Path().apply {
            moveTo(w * 0.43f, h * 0.34f)
            lineTo(w * 0.63f, h * 0.45f)
            lineTo(w * 0.43f, h * 0.56f)
            close()
        }
        drawPath(path = play, color = color)
    }
}

@Composable
fun SoftStopCircleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color = Color(0xFFEF4444)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.10f
        drawCircle(
            color = color,
            radius = w * 0.40f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = sw)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.34f, h * 0.34f),
            size = Size(w * 0.32f, h * 0.32f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
    }
}

