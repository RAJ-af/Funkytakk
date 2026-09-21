package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Custom Soft 3D-styled icons designed with multi-layer gradients,
 * tactile depth, specular highlights, and soft ambient shadows.
 * Zero emojis or raw material icons used.
 */

@Composable
fun Soft3DFloatingBubble(modifier: Modifier = Modifier.size(36.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Outer soft ambient shadow
        drawCircle(
            color = Color(0x25F59E0B),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.54f)
        )

        // Main soft 3D sphere/bubble gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706)),
                center = Offset(w * 0.38f, h * 0.35f),
                radius = w * 0.55f
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.48f, h * 0.46f)
        )

        // Specular 3D highlight (top glossy reflection)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0f)),
                center = Offset(w * 0.36f, h * 0.28f),
                radius = w * 0.22f
            ),
            radius = w * 0.22f,
            center = Offset(w * 0.36f, h * 0.28f)
        )

        // Chat tail at bottom-left
        val tailPath = Path().apply {
            moveTo(w * 0.25f, h * 0.68f)
            quadraticTo(w * 0.16f, h * 0.84f, w * 0.10f, h * 0.86f)
            quadraticTo(w * 0.22f, h * 0.76f, w * 0.38f, h * 0.74f)
            close()
        }
        drawPath(
            path = tailPath,
            brush = Brush.linearGradient(
                listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                start = Offset(w * 0.3f, h * 0.7f),
                end = Offset(w * 0.1f, h * 0.86f)
            )
        )

        // 3 Soft 3D dots inside
        val dotRadius = w * 0.05f
        val dotY = h * 0.46f
        listOf(w * 0.34f, w * 0.48f, w * 0.62f).forEach { dotX ->
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = dotRadius,
                center = Offset(dotX, dotY)
            )
            drawCircle(
                color = Color(0x30B45309),
                radius = dotRadius * 0.6f,
                center = Offset(dotX, dotY + 1f)
            )
        }
    }
}

@Composable
fun Soft3DReportShield(modifier: Modifier = Modifier.size(36.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ambient shadow
        drawCircle(
            color = Color(0x22EF4444),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.54f)
        )

        // Shield base path
        val shieldPath = Path().apply {
            moveTo(w * 0.5f, h * 0.14f)
            cubicTo(w * 0.78f, h * 0.14f, w * 0.86f, h * 0.22f, w * 0.86f, h * 0.44f)
            cubicTo(w * 0.86f, h * 0.70f, w * 0.65f, h * 0.86f, w * 0.5f, h * 0.90f)
            cubicTo(w * 0.35f, h * 0.86f, w * 0.14f, h * 0.70f, w * 0.14f, h * 0.44f)
            cubicTo(w * 0.14f, h * 0.22f, w * 0.22f, h * 0.14f, w * 0.5f, h * 0.14f)
            close()
        }

        // 3D Gradient Fill
        drawPath(
            path = shieldPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFDA4AF), Color(0xFFF43F5E), Color(0xFFBE123C)),
                center = Offset(w * 0.42f, h * 0.32f),
                radius = w * 0.55f
            )
        )

        // Glossy highlight curve
        val highlightPath = Path().apply {
            moveTo(w * 0.5f, h * 0.18f)
            cubicTo(w * 0.70f, h * 0.18f, w * 0.76f, h * 0.24f, w * 0.76f, h * 0.40f)
            cubicTo(w * 0.65f, h * 0.35f, w * 0.45f, h * 0.35f, w * 0.30f, h * 0.45f)
            cubicTo(w * 0.24f, h * 0.35f, w * 0.32f, h * 0.18f, w * 0.5f, h * 0.18f)
            close()
        }
        drawPath(
            path = highlightPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.05f))
            )
        )

        // Central 3D Alert Exclamation Mark
        val barTop = h * 0.36f
        val barBottom = h * 0.56f
        drawLine(
            color = Color.White,
            start = Offset(w * 0.5f, barTop),
            end = Offset(w * 0.5f, barBottom),
            strokeWidth = w * 0.085f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.045f,
            center = Offset(w * 0.5f, h * 0.69f)
        )
    }
}

@Composable
fun Soft3DLeaveDoor(modifier: Modifier = Modifier.size(36.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft ambient shadow
        drawCircle(
            color = Color(0x20DC2626),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.54f)
        )

        // Background Disc with coral gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFCA5A5), Color(0xFFEF4444), Color(0xFFB91C1C)),
                center = Offset(w * 0.4f, h * 0.36f),
                radius = w * 0.5f
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Specular highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                center = Offset(w * 0.36f, h * 0.28f),
                radius = w * 0.25f
            ),
            radius = w * 0.22f,
            center = Offset(w * 0.36f, h * 0.28f)
        )

        // Door frame (white rounded rect on left)
        val doorPath = Path().apply {
            moveTo(w * 0.45f, h * 0.26f)
            lineTo(w * 0.28f, h * 0.26f)
            lineTo(w * 0.28f, h * 0.74f)
            lineTo(w * 0.45f, h * 0.74f)
        }
        drawPath(
            path = doorPath,
            color = Color.White,
            style = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Outward Arrow pointing right
        val arrowY = h * 0.5f
        drawLine(
            color = Color.White,
            start = Offset(w * 0.38f, arrowY),
            end = Offset(w * 0.74f, arrowY),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
        val arrowHead = Path().apply {
            moveTo(w * 0.62f, arrowY - h * 0.12f)
            lineTo(w * 0.75f, arrowY)
            lineTo(w * 0.62f, arrowY + h * 0.12f)
        }
        drawPath(
            path = arrowHead,
            color = Color.White,
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// REPORT REASONS ICONS (Soft 3D Tactile Icons replacing emojis)
// -------------------------------------------------------------------------------------------------

@Composable
fun Soft3DHarassmentIcon(modifier: Modifier = Modifier.size(36.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft outer glow
        drawCircle(
            color = Color(0x25F97316),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.54f)
        )

        // 3D layered badge
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFDBA74), Color(0xFFF97316), Color(0xFFC2410C)),
                center = Offset(w * 0.38f, h * 0.35f),
                radius = w * 0.55f
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Specular highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.65f), Color.Transparent),
                center = Offset(w * 0.38f, h * 0.28f),
                radius = w * 0.22f
            ),
            radius = w * 0.22f,
            center = Offset(w * 0.38f, h * 0.28f)
        )

        // 3D Bolt / Disruption motif
        val boltPath = Path().apply {
            moveTo(w * 0.55f, h * 0.24f)
            lineTo(w * 0.38f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.50f)
            lineTo(w * 0.42f, h * 0.76f)
            lineTo(w * 0.64f, h * 0.46f)
            lineTo(w * 0.52f, h * 0.46f)
            close()
        }
        drawPath(path = boltPath, color = Color.White)
        drawPath(
            path = boltPath,
            color = Color(0x357C2D12),
            style = Stroke(width = 1.5f)
        )
    }
}

@Composable
fun Soft3DSpamIcon(modifier: Modifier = Modifier.size(36.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Outer ambient halo
        drawCircle(
            color = Color(0x253B82F6),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.54f)
        )

        // Circular background with vibrant cyan-blue 3D depth
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF93C5FD), Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                center = Offset(w * 0.38f, h * 0.35f),
                radius = w * 0.55f
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Megaphone body
        val megaPath = Path().apply {
            moveTo(w * 0.30f, h * 0.40f)
            lineTo(w * 0.58f, h * 0.26f)
            lineTo(w * 0.58f, h * 0.70f)
            lineTo(w * 0.30f, h * 0.56f)
            close()
        }
        drawPath(path = megaPath, color = Color.White)

        // Megaphone back capsule
        drawRoundRect(
            color = Color(0xFFDBEAFE),
            topLeft = Offset(w * 0.22f, h * 0.40f),
            size = Size(w * 0.12f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )

        // Handle
        drawLine(
            color = Color.White,
            start = Offset(w * 0.36f, h * 0.54f),
            end = Offset(w * 0.30f, h * 0.72f),
            strokeWidth = w * 0.07f,
            cap = StrokeCap.Round
        )

        // Sound broadcast waves
        val wavePath = Path().apply {
            moveTo(w * 0.68f, h * 0.36f)
            quadraticTo(w * 0.76f, h * 0.48f, w * 0.68f, h * 0.60f)
        }
        drawPath(
            path = wavePath,
            color = Color.White,
            style = Stroke(width = w * 0.07f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun Soft3DHateIcon(modifier: Modifier = Modifier.size(36.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Outer glow
        drawCircle(
            color = Color(0x25E11D48),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.54f)
        )

        // Gradient Disc
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFDA4AF), Color(0xFFF43F5E), Color(0xFF9F1239)),
                center = Offset(w * 0.38f, h * 0.35f),
                radius = w * 0.55f
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Specular highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                center = Offset(w * 0.36f, h * 0.28f),
                radius = w * 0.22f
            ),
            radius = w * 0.22f,
            center = Offset(w * 0.36f, h * 0.28f)
        )

        // Stop Hand Motif (palm + fingers)
        // Palm
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.32f, h * 0.46f),
            size = Size(w * 0.36f, h * 0.28f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        // 4 fingers
        val fingerWidth = w * 0.065f
        val fingerSpacing = w * 0.082f
        val heights = listOf(h * 0.22f, h * 0.25f, h * 0.24f, h * 0.20f)
        heights.forEachIndexed { i, fHeight ->
            val fx = w * 0.33f + i * fingerSpacing
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(fx, h * 0.46f - fHeight),
                size = Size(fingerWidth, fHeight + h * 0.05f),
                cornerRadius = CornerRadius(fingerWidth / 2, fingerWidth / 2)
            )
        }
        // Thumb
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.24f, h * 0.50f),
            size = Size(fingerWidth * 1.1f, h * 0.16f),
            cornerRadius = CornerRadius(fingerWidth / 2, fingerWidth / 2)
        )
    }
}

@Composable
fun Soft3DSexualContentIcon(modifier: Modifier = Modifier.size(36.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ambient shadow
        drawCircle(
            color = Color(0x22EC4899),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.54f)
        )

        // Disc with warm berry-fuchsia 3D depth
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFBCFE8), Color(0xFFEC4899), Color(0xFFBE185D)),
                center = Offset(w * 0.38f, h * 0.35f),
                radius = w * 0.55f
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Soft 3D Heart in center
        val heartPath = Path().apply {
            moveTo(w * 0.5f, h * 0.72f)
            cubicTo(w * 0.24f, h * 0.56f, w * 0.22f, h * 0.36f, w * 0.36f, h * 0.30f)
            cubicTo(w * 0.46f, h * 0.26f, w * 0.5f, h * 0.38f, w * 0.5f, h * 0.38f)
            cubicTo(w * 0.5f, h * 0.38f, w * 0.54f, h * 0.26f, w * 0.64f, h * 0.30f)
            cubicTo(w * 0.78f, h * 0.36f, w * 0.76f, h * 0.56f, w * 0.5f, h * 0.72f)
            close()
        }
        drawPath(path = heartPath, color = Color.White)

        // Specular highlight on left lobe of heart
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = w * 0.055f,
            center = Offset(w * 0.38f, h * 0.36f)
        )
    }
}

@Composable
fun Soft3DDangerousIcon(modifier: Modifier = Modifier.size(36.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Outer glow
        drawCircle(
            color = Color(0x28EAB308),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.54f)
        )

        // Golden amber 3D Disc
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFEAB308), Color(0xFFCA8A04)),
                center = Offset(w * 0.38f, h * 0.35f),
                radius = w * 0.55f
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Soft 3D Warning Triangle inside
        val triPath = Path().apply {
            moveTo(w * 0.5f, h * 0.22f)
            lineTo(w * 0.76f, h * 0.70f)
            lineTo(w * 0.24f, h * 0.70f)
            close()
        }
        drawPath(
            path = triPath,
            color = Color.White,
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Exclamation inside triangle
        drawLine(
            color = Color.White,
            start = Offset(w * 0.5f, h * 0.40f),
            end = Offset(w * 0.5f, h * 0.54f),
            strokeWidth = w * 0.075f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.04f,
            center = Offset(w * 0.5f, h * 0.63f)
        )
    }
}

@Composable
fun Soft3DOtherIcon(modifier: Modifier = Modifier.size(36.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ambient glow
        drawCircle(
            color = Color(0x228B5CF6),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.54f)
        )

        // Purple-violet 3D Disc
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFDDD6FE), Color(0xFF8B5CF6), Color(0xFF6D28D9)),
                center = Offset(w * 0.38f, h * 0.35f),
                radius = w * 0.55f
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Specular highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                center = Offset(w * 0.36f, h * 0.28f),
                radius = w * 0.22f
            ),
            radius = w * 0.22f,
            center = Offset(w * 0.36f, h * 0.28f)
        )

        // 3 tactile horizontal rounded pills (question/more motif)
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.30f, h * 0.36f),
            size = Size(w * 0.40f, h * 0.07f),
            cornerRadius = CornerRadius(w * 0.035f, w * 0.035f)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.30f, h * 0.48f),
            size = Size(w * 0.32f, h * 0.07f),
            cornerRadius = CornerRadius(w * 0.035f, w * 0.035f)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.30f, h * 0.60f),
            size = Size(w * 0.24f, h * 0.07f),
            cornerRadius = CornerRadius(w * 0.035f, w * 0.035f)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// EVIDENCE AUTOMATIC ATTACHMENT ICONS (Soft 3D Micro-Icons)
// -------------------------------------------------------------------------------------------------

@Composable
fun Soft3DMicrophoneIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Mic capsule with gradient
        val capW = w * 0.36f
        val capH = h * 0.46f
        val capX = (w - capW) / 2f
        val capY = h * 0.12f

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF60A5FA), Color(0xFF2563EB))
            ),
            topLeft = Offset(capX, capY),
            size = Size(capW, capH),
            cornerRadius = CornerRadius(capW / 2, capW / 2)
        )

        // Outer cradle curve
        val cradlePath = Path().apply {
            moveTo(capX - w * 0.08f, capY + capH * 0.45f)
            cubicTo(
                capX - w * 0.08f, capY + capH + h * 0.12f,
                capX + capW + w * 0.08f, capY + capH + h * 0.12f,
                capX + capW + w * 0.08f, capY + capH * 0.45f
            )
        }
        drawPath(
            path = cradlePath,
            color = Color(0xFF1D4ED8),
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        )

        // Stand stem and base
        drawLine(
            color = Color(0xFF1D4ED8),
            start = Offset(w * 0.5f, capY + capH + h * 0.08f),
            end = Offset(w * 0.5f, h * 0.86f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF1D4ED8),
            start = Offset(w * 0.32f, h * 0.86f),
            end = Offset(w * 0.68f, h * 0.86f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun Soft3DMessageStackIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background bubble (soft amber)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))
            ),
            topLeft = Offset(w * 0.28f, h * 0.12f),
            size = Size(w * 0.62f, h * 0.46f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
        )

        // Foreground bubble with depth (coral-rose)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFF43F5E), Color(0xFFE11D48))
            ),
            topLeft = Offset(w * 0.10f, h * 0.36f),
            size = Size(w * 0.62f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
        )

        // Inner white dots
        val dY = h * 0.60f
        listOf(w * 0.26f, w * 0.41f, w * 0.56f).forEach { dX ->
            drawCircle(color = Color.White, radius = w * 0.045f, center = Offset(dX, dY))
        }
    }
}

@Composable
fun Soft3DUsersGroupIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Left secondary user
        drawCircle(
            color = Color(0xFFC084FC),
            radius = w * 0.14f,
            center = Offset(w * 0.34f, h * 0.28f)
        )
        drawRoundRect(
            color = Color(0xFFC084FC),
            topLeft = Offset(w * 0.16f, h * 0.50f),
            size = Size(w * 0.36f, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
        )

        // Foreground primary user
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFA78BFA), Color(0xFF7C3AED)),
                center = Offset(w * 0.62f, h * 0.30f),
                radius = w * 0.2f
            ),
            radius = w * 0.16f,
            center = Offset(w * 0.66f, h * 0.34f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
            ),
            topLeft = Offset(w * 0.46f, h * 0.56f),
            size = Size(w * 0.42f, h * 0.38f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// ATTACHMENT ADD-ON ICONS (Screenshot / Video)
// -------------------------------------------------------------------------------------------------

@Composable
fun Soft3DScreenshotIcon(modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF34D399), Color(0xFF059669))
            ),
            topLeft = Offset(w * 0.08f, h * 0.14f),
            size = Size(w * 0.84f, h * 0.72f),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
        )

        // Sun dot
        drawCircle(
            color = Color.White,
            radius = w * 0.09f,
            center = Offset(w * 0.32f, h * 0.36f)
        )

        // Mountain path
        val mPath = Path().apply {
            moveTo(w * 0.20f, h * 0.72f)
            lineTo(w * 0.44f, h * 0.48f)
            lineTo(w * 0.60f, h * 0.62f)
            lineTo(w * 0.72f, h * 0.52f)
            lineTo(w * 0.80f, h * 0.72f)
            close()
        }
        drawPath(path = mPath, color = Color.White.copy(alpha = 0.9f))
    }
}

@Composable
fun Soft3DVideoCamIcon(modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Camera box
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF60A5FA), Color(0xFF2563EB))
            ),
            topLeft = Offset(w * 0.08f, h * 0.22f),
            size = Size(w * 0.56f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
        )

        // Lens cone
        val cone = Path().apply {
            moveTo(w * 0.66f, h * 0.36f)
            lineTo(w * 0.90f, h * 0.24f)
            lineTo(w * 0.90f, h * 0.76f)
            lineTo(w * 0.66f, h * 0.64f)
            close()
        }
        drawPath(
            path = cone,
            brush = Brush.verticalGradient(listOf(Color(0xFF93C5FD), Color(0xFF1D4ED8)))
        )
    }
}

// -------------------------------------------------------------------------------------------------
// REPORT SUBMITTED SUCCESS & STEP ICONS
// -------------------------------------------------------------------------------------------------

@Composable
fun Soft3DSuccessBadge(modifier: Modifier = Modifier.size(72.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft emerald ambient drop glow
        drawCircle(
            color = Color(0x3010B981),
            radius = w * 0.48f,
            center = Offset(w * 0.5f, h * 0.55f)
        )

        // Layer 1: Base rich emerald gradient disc
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF6EE7B7), Color(0xFF10B981), Color(0xFF047857)),
                center = Offset(w * 0.38f, h * 0.35f),
                radius = w * 0.56f
            ),
            radius = w * 0.44f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Layer 2: Glossy specular highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.65f), Color.Transparent),
                center = Offset(w * 0.36f, h * 0.28f),
                radius = w * 0.28f
            ),
            radius = w * 0.26f,
            center = Offset(w * 0.36f, h * 0.28f)
        )

        // Layer 3: Crisp 3D Checkmark
        val checkPath = Path().apply {
            moveTo(w * 0.32f, h * 0.52f)
            lineTo(w * 0.45f, h * 0.66f)
            lineTo(w * 0.70f, h * 0.36f)
        }
        // Shadow under checkmark
        drawPath(
            path = checkPath,
            color = Color(0x35064E3B),
            style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // White checkmark
        drawPath(
            path = checkPath,
            color = Color.White,
            style = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun Soft3DSearchAuditIcon(modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Magnifying glass lens rim
        val lensRadius = w * 0.28f
        val lensCenter = Offset(w * 0.40f, h * 0.40f)

        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFDE68A), Color(0xFFF59E0B)),
                center = lensCenter,
                radius = lensRadius
            ),
            radius = lensRadius,
            center = lensCenter,
            style = Stroke(width = w * 0.12f)
        )

        // Glass reflection
        drawCircle(
            color = Color(0x30FEF08A),
            radius = lensRadius * 0.75f,
            center = lensCenter
        )

        // Handle
        drawLine(
            brush = Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFB45309))),
            start = Offset(w * 0.62f, h * 0.62f),
            end = Offset(w * 0.88f, h * 0.88f),
            strokeWidth = w * 0.13f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun Soft3DRulesActionIcon(modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft 3D Shield with check
        val sPath = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            cubicTo(w * 0.78f, h * 0.15f, w * 0.84f, h * 0.25f, w * 0.84f, h * 0.48f)
            cubicTo(w * 0.84f, h * 0.72f, w * 0.65f, h * 0.86f, w * 0.5f, h * 0.90f)
            cubicTo(w * 0.35f, h * 0.86f, w * 0.16f, h * 0.72f, w * 0.16f, h * 0.48f)
            cubicTo(w * 0.16f, h * 0.25f, w * 0.22f, h * 0.15f, w * 0.5f, h * 0.15f)
            close()
        }

        drawPath(
            path = sPath,
            brush = Brush.radialGradient(
                listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706)),
                center = Offset(w * 0.42f, h * 0.35f),
                radius = w * 0.55f
            )
        )

        // Mini check inside
        val cPath = Path().apply {
            moveTo(w * 0.38f, h * 0.50f)
            lineTo(w * 0.48f, h * 0.60f)
            lineTo(w * 0.65f, h * 0.38f)
        }
        drawPath(
            path = cPath,
            color = Color.White,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun Soft3DPrivacyLockIcon(modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Shackle arch
        val shacklePath = Path().apply {
            moveTo(w * 0.32f, h * 0.46f)
            lineTo(w * 0.32f, h * 0.30f)
            cubicTo(w * 0.32f, h * 0.14f, w * 0.68f, h * 0.14f, w * 0.68f, h * 0.30f)
            lineTo(w * 0.68f, h * 0.46f)
        }
        drawPath(
            path = shacklePath,
            brush = Brush.verticalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF64748B))),
            style = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
        )

        // Padlock body with golden amber gradient
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706))),
            topLeft = Offset(w * 0.20f, h * 0.42f),
            size = Size(w * 0.60f, h * 0.46f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
        )

        // Keyhole dot
        drawCircle(color = Color.White, radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.60f))
        drawLine(
            color = Color.White,
            start = Offset(w * 0.5f, h * 0.60f),
            end = Offset(w * 0.5f, h * 0.74f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun Soft3DBellNotificationIcon(modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Bell Body
        val bellPath = Path().apply {
            moveTo(w * 0.5f, h * 0.16f)
            cubicTo(w * 0.34f, h * 0.16f, w * 0.26f, h * 0.36f, w * 0.26f, h * 0.62f)
            lineTo(w * 0.18f, h * 0.74f)
            lineTo(w * 0.82f, h * 0.74f)
            lineTo(w * 0.74f, h * 0.62f)
            cubicTo(w * 0.74f, h * 0.36f, w * 0.66f, h * 0.16f, w * 0.5f, h * 0.16f)
            close()
        }

        drawPath(
            path = bellPath,
            brush = Brush.radialGradient(
                listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706)),
                center = Offset(w * 0.45f, h * 0.35f),
                radius = w * 0.55f
            )
        )

        // Clapper
        drawCircle(
            color = Color(0xFFB45309),
            radius = w * 0.08f,
            center = Offset(w * 0.5f, h * 0.84f)
        )
    }
}
