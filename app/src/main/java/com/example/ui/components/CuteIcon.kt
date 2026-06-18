package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Explicit non-generic compile-safe helpers
private class SliderLine(val x: Float, val y: Float, val color: Color)
private class SoundWave(val len: Float, val color: Color)

/**
 * Custom "Soft & Cute" Dynamic Vector Icon Canvas drawing system.
 * Absolutely NO literal standard Android wireframes/Material icons AND NO raw emojis!
 * Drawn purely with high performance Compose Canvas for beautiful pastel UI layouts.
 */
@Composable
fun CuteIcon(
    iconName: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp, // Maintained parameter name for caller's named arguments compatibility
    badgeColor: Color? = null // Optional custom background override
) {
    val dpSize = size // Local alias to prevent shadowing issues in DrawScope

    // Map of common icon names to custom background gradients
    val bgBrush = remember(iconName) {
        when (iconName.lowercase().trim()) {
            "arrow_back", "back", "chevron_left" -> Brush.sweepGradient(listOf(Color(0xFFFFEDD5), Color(0xFFFEF3C7)))
            "arrow_forward", "forward", "chevron_right", "next" -> Brush.sweepGradient(listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5)))
            "arrow_up", "arrow_down" -> Brush.linearGradient(listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)))
            "add", "plus", "create" -> Brush.linearGradient(listOf(Color(0xFFFDF4FF), Color(0xFFF5E1FF)))
            "lock", "private" -> Brush.linearGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)))
            "close", "clear", "dismiss" -> Brush.linearGradient(listOf(Color(0xFFFEE2E2), Color(0xFFFCA5A5)))
            "home" -> Brush.linearGradient(listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5)))
            "mic", "microphone", "audio", "speaking" -> Brush.linearGradient(listOf(Color(0xFFEEF2F6), Color(0xFFE2E8F0)))
            "listener", "listening" -> Brush.linearGradient(listOf(Color(0xFFEFF6FF), Color(0xFFBFDBFE)))
            "language", "globe", "moments", "explore" -> Brush.linearGradient(listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7)))
            "chat", "chats", "message", "bubble" -> Brush.linearGradient(listOf(Color(0xFFEEF2F6), Color(0xFFE0E7FF)))
            "profile", "person", "account", "human" -> Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)))
            "campaign", "broadcast", "announcement" -> Brush.linearGradient(listOf(Color(0xFFFFF7ED), Color(0xFFFFE4E6)))
            "search" -> Brush.linearGradient(listOf(Color(0xFFF9FAFB), Color(0xFFF3F4F6)))
            "tune", "filter", "settings" -> Brush.linearGradient(listOf(Color(0xFFFFF1F2), Color(0xFFFFDEE2)))
            "favorite", "heart", "liked" -> Brush.linearGradient(listOf(Color(0xFFFFF1F2), Color(0xFFFFD1D7)))
            "favorite_border", "heart_empty", "unliked" -> Brush.linearGradient(listOf(Color(0xFFF9FAFB), Color(0xFFF3F4F6)))
            "share", "send_post" -> Brush.linearGradient(listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5)))
            "school", "learn", "study" -> Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF9C3)))
            "logout", "signout", "exit" -> Brush.linearGradient(listOf(Color(0xFFFEE2E2), Color(0xFFFFCDCD)))
            "email", "mail" -> Brush.linearGradient(listOf(Color(0xFFEEF2F6), Color(0xFFE0E7FF)))
            "visibility", "eye", "show" -> Brush.linearGradient(listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE)))
            "visibility_off", "hide" -> Brush.linearGradient(listOf(Color(0xFFF3F4F6), Color(0xFFE5E7EB)))
            "send", "paper_plane" -> Brush.linearGradient(listOf(Color(0xFFECFDF5), Color(0xFFA7F3D0)))
            "shield", "security", "privacy" -> Brush.linearGradient(listOf(Color(0xFFFFF1F2), Color(0xFFFCE7F3)))
            "description", "document", "terms" -> Brush.linearGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)))
            "open_in_new", "external_link" -> Brush.linearGradient(listOf(Color(0xFFF3F4F6), Color(0xFFE5E7EB)))
            "star", "rating", "gold_star" -> Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)))
            "crown", "vip", "premium" -> Brush.linearGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFCD34D)))
            "warning", "alert" -> Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)))
            "trash", "delete", "clear_all" -> Brush.linearGradient(listOf(Color(0xFFFEE2E2), Color(0xFFFFC5C5)))
            "voice_podium", "podcasts" -> Brush.linearGradient(listOf(Color(0xFFF5F3FF), Color(0xFFDDD6FE)))
            "more_vert", "options" -> Brush.linearGradient(listOf(Color(0xFFF9FAFB), Color(0xFFF3F4F6)))
            "crown_outline" -> Brush.linearGradient(listOf(Color(0xFFFFFAEB), Color(0xFFFEF3C7)))
            "translate" -> Brush.linearGradient(listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF)))
            else -> Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)))
        }
    }

    val finalBackgroundModifier = if (badgeColor != null) {
        Modifier.background(badgeColor, CircleShape)
    } else {
        Modifier.background(bgBrush, CircleShape)
    }

    Box(
        modifier = modifier
            .size(dpSize)
            .clip(CircleShape)
            .then(finalBackgroundModifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(dpSize * 0.65f)) {
            val width = this.size.width
            val height = this.size.height
            val cx = width / 2f
            val cy = height / 2f
            val rad = width / 2f

            when (iconName.lowercase().trim()) {
                "arrow_back", "back", "chevron_left" -> {
                    // Thick rounded arrow back
                    val path = Path().apply {
                        moveTo(cx + rad * 0.4f, cy - rad * 0.4f)
                        lineTo(cx - rad * 0.3f, cy)
                        lineTo(cx + rad * 0.4f, cy + rad * 0.4f)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFB45309),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                "arrow_forward", "forward", "chevron_right", "next" -> {
                    // Thick rounded arrow forward
                    val path = Path().apply {
                        moveTo(cx - rad * 0.4f, cy - rad * 0.4f)
                        lineTo(cx + rad * 0.3f, cy)
                        lineTo(cx - rad * 0.4f, cy + rad * 0.4f)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFB45309),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                "arrow_up" -> {
                    val path = Path().apply {
                        moveTo(cx - rad * 0.4f, cy + rad * 0.2f)
                        lineTo(cx, cy - rad * 0.4f)
                        lineTo(cx + rad * 0.4f, cy + rad * 0.2f)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF1D4ED8),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                "arrow_down" -> {
                    val path = Path().apply {
                        moveTo(cx - rad * 0.4f, cy - rad * 0.2f)
                        lineTo(cx, cy + rad * 0.4f)
                        lineTo(cx + rad * 0.4f, cy - rad * 0.2f)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF1D4ED8),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                "add", "plus", "create" -> {
                    // Cute chubby plus sign
                    drawRoundRect(
                        color = Color(0xFF701A75),
                        topLeft = Offset(cx - 2.5.dp.toPx(), cy - rad * 0.6f),
                        size = Size(5.dp.toPx(), rad * 1.2f),
                        cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFF701A75),
                        topLeft = Offset(cx - rad * 0.6f, cy - 2.5.dp.toPx()),
                        size = Size(rad * 1.2f, 5.dp.toPx()),
                        cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                    )
                }
                "lock", "private" -> {
                    // Lock body
                    drawRoundRect(
                        color = Color(0xFFD97706),
                        topLeft = Offset(cx - rad * 0.5f, cy - rad * 0.1f),
                        size = Size(rad * 1.0f, rad * 0.7f),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    // Shackle
                    val path = Path().apply {
                        moveTo(cx - rad * 0.3f, cy - rad * 0.1f)
                        quadraticTo(cx - rad * 0.3f, cy - rad * 0.6f, cx, cy - rad * 0.6f)
                        quadraticTo(cx + rad * 0.3f, cy - rad * 0.6f, cx + rad * 0.3f, cy - rad * 0.1f)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF78350F),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                "close", "clear", "dismiss" -> {
                    // X symbol
                    val path1 = Path().apply {
                        moveTo(cx - rad * 0.4f, cy - rad * 0.4f)
                        lineTo(cx + rad * 0.4f, cy + rad * 0.4f)
                    }
                    val path2 = Path().apply {
                        moveTo(cx + rad * 0.4f, cy - rad * 0.4f)
                        lineTo(cx - rad * 0.4f, cy + rad * 0.4f)
                    }
                    val strokeStyle = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    drawPath(path1, color = Color(0xFF991B1B), style = strokeStyle)
                    drawPath(path2, color = Color(0xFF991B1B), style = strokeStyle)
                }
                "home" -> {
                    // House roof
                    val roofPath = Path().apply {
                        moveTo(cx - rad * 0.55f, cy + rad * 0.1f)
                        lineTo(cx, cy - rad * 0.5f)
                        lineTo(cx + rad * 0.55f, cy + rad * 0.1f)
                        close()
                    }
                    drawPath(roofPath, color = Color(0xFF047857))
                    // House base
                    drawRoundRect(
                        color = Color(0xFF0F9F6E),
                        topLeft = Offset(cx - rad * 0.4f, cy + rad * 0.1f),
                        size = Size(rad * 0.8f, rad * 0.5f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    // Lit Window
                    drawCircle(
                        color = Color(0xFFFDE047),
                        radius = 2.5.dp.toPx(),
                        center = Offset(cx, cy + rad * 0.25f)
                    )
                }
                "mic", "microphone", "audio", "speaking" -> {
                    // Rounded mic pill
                    drawRoundRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(cx - 3.5.dp.toPx(), cy - rad * 0.5f),
                        size = Size(7.dp.toPx(), rad * 0.9f),
                        cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())
                    )
                    // Stand hook
                    val standPath = Path().apply {
                        moveTo(cx - 6.dp.toPx(), cy)
                        lineTo(cx - 6.dp.toPx(), cy + 4.dp.toPx())
                        quadraticTo(cx - 6.dp.toPx(), cy + 9.dp.toPx(), cx, cy + 9.dp.toPx())
                        quadraticTo(cx + 6.dp.toPx(), cy + 9.dp.toPx(), cx + 6.dp.toPx(), cy + 4.dp.toPx())
                        lineTo(cx + 6.dp.toPx(), cy)
                    }
                    drawPath(
                        path = standPath,
                        color = Color(0xFF64748B),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Little standing peg
                    drawRoundRect(
                        color = Color(0xFF64748B),
                        topLeft = Offset(cx - 1.5.dp.toPx(), cy + 8.dp.toPx()),
                        size = Size(3.dp.toPx(), 5.dp.toPx()),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                }
                "listener", "listening" -> {
                    // Headphone curved arch
                    val headsetPath = Path().apply {
                        moveTo(cx - rad * 0.5f, cy + rad * 0.2f)
                        cubicTo(cx - rad * 0.5f, cy - rad * 0.6f, cx + rad * 0.5f, cy - rad * 0.6f, cx + rad * 0.5f, cy + rad * 0.2f)
                    }
                    drawPath(
                        path = headsetPath,
                        color = Color(0xFF1D4ED8),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Ear pads
                    drawRoundRect(
                        color = Color(0xFF2563EB),
                        topLeft = Offset(cx - rad * 0.6f, cy),
                        size = Size(4.dp.toPx(), rad * 0.4f),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFF2563EB),
                        topLeft = Offset(cx + rad * 0.5f, cy),
                        size = Size(4.dp.toPx(), rad * 0.4f),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                }
                "language", "globe", "moments", "explore" -> {
                    // Earth base sphere
                    drawCircle(
                        color = Color(0xFF60A5FA),
                        radius = rad * 0.7f,
                        center = Offset(cx, cy)
                    )
                    // Latitudes
                    drawCircle(
                        color = Color.White,
                        radius = rad * 0.7f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    // Oval lines representing longitudinal globe curves
                    val path = Path().apply {
                        moveTo(cx, cy - rad * 0.7f)
                        cubicTo(cx - rad * 0.35f, cy - rad * 0.3f, cx - rad * 0.35f, cy + rad * 0.3f, cx, cy + rad * 0.7f)
                        moveTo(cx, cy - rad * 0.7f)
                        cubicTo(cx + rad * 0.35f, cy - rad * 0.3f, cx + rad * 0.35f, cy + rad * 0.3f, cx, cy + rad * 0.7f)
                        moveTo(cx - rad * 0.7f, cy)
                        lineTo(cx + rad * 0.7f, cy)
                    }
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.8f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                "chat", "chats", "message", "bubble" -> {
                    // Conversation balloon body
                    val balloonPath = Path().apply {
                        moveTo(cx - rad * 0.5f, cy - rad * 0.4f)
                        lineTo(cx + rad * 0.5f, cy - rad * 0.4f)
                        quadraticTo(cx + rad * 0.65f, cy - rad * 0.4f, cx + rad * 0.65f, cy)
                        quadraticTo(cx + rad * 0.65f, cy + rad * 0.3f, cx + rad * 0.5f, cy + rad * 0.3f)
                        // Arrow pointing down left
                        lineTo(cx - rad * 0.2f, cy + rad * 0.3f)
                        lineTo(cx - rad * 0.45f, cy + rad * 0.55f)
                        lineTo(cx - rad * 0.45f, cy + rad * 0.3f)
                        lineTo(cx - rad * 0.5f, cy + rad * 0.3f)
                        quadraticTo(cx - rad * 0.65f, cy + rad * 0.3f, cx - rad * 0.65f, cy)
                        quadraticTo(cx - rad * 0.65f, cy - rad * 0.4f, cx - rad * 0.5f, cy - rad * 0.4f)
                        close()
                    }
                    drawPath(
                        path = balloonPath,
                        color = Color(0xFF4338CA)
                    )
                    // Inner happy dots
                    drawCircle(color = Color.White, radius = 1.25.dp.toPx(), center = Offset(cx - 3.5.dp.toPx(), cy - 1.dp.toPx()))
                    drawCircle(color = Color.White, radius = 1.25.dp.toPx(), center = Offset(cx, cy - 1.dp.toPx()))
                    drawCircle(color = Color.White, radius = 1.25.dp.toPx(), center = Offset(cx + 3.5.dp.toPx(), cy - 1.dp.toPx()))
                }
                "profile", "person", "account", "human" -> {
                    // Smiling rounded character buddy (Happy Chick base)
                    // Face base
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = rad * 0.65f,
                        center = Offset(cx, cy + 1.dp.toPx())
                    )
                    // Tiny black eyes
                    drawCircle(
                        color = Color(0xFF334155),
                        radius = 1.25.dp.toPx(),
                        center = Offset(cx - 3.dp.toPx(), cy - 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF334155),
                        radius = 1.25.dp.toPx(),
                        center = Offset(cx + 3.dp.toPx(), cy - 1.5.dp.toPx())
                    )
                    // Blushing cheeks
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.6f),
                        radius = 2.dp.toPx(),
                        center = Offset(cx - 5.5.dp.toPx(), cy + 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.6f),
                        radius = 2.dp.toPx(),
                        center = Offset(cx + 5.5.dp.toPx(), cy + 1.dp.toPx())
                    )
                    // Smile curve
                    val smilePath = Path().apply {
                        moveTo(cx - 1.5.dp.toPx(), cy + 1.dp.toPx())
                        quadraticTo(cx, cy + 2.5.dp.toPx(), cx + 1.5.dp.toPx(), cy + 1.dp.toPx())
                    }
                    drawPath(
                        path = smilePath,
                        color = Color(0xFF451A03),
                        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                "campaign", "broadcast", "announcement" -> {
                    // Megaphone body
                    val bodyPath = Path().apply {
                        moveTo(cx - rad * 0.3f, cy - rad * 0.1f)
                        lineTo(cx + rad * 0.3f, cy - rad * 0.4f)
                        lineTo(cx + rad * 0.3f, cy + rad * 0.3f)
                        lineTo(cx - rad * 0.3f, cy)
                        close()
                    }
                    drawPath(bodyPath, color = Color(0xFFEA580C))
                    // Bell open cap
                    drawRoundRect(
                        color = Color(0xFFC2410C),
                        topLeft = Offset(cx + rad * 0.25f, cy - rad * 0.45f),
                        size = Size(4.dp.toPx(), rad * 0.85f),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                    // Handle
                    drawRoundRect(
                        color = Color(0xFF9A3412),
                        topLeft = Offset(cx - rad * 0.25f, cy),
                        size = Size(5.dp.toPx(), rad * 0.4f),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                }
                "search" -> {
                    // Custom magnifying glass
                    drawCircle(
                        color = Color(0xFF475569),
                        radius = rad * 0.4f,
                        center = Offset(cx - 2.dp.toPx(), cy - 2.dp.toPx()),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    // Chubby diagonal handle
                    val handlePath = Path().apply {
                        moveTo(cx + 1.dp.toPx(), cy + 1.dp.toPx())
                        lineTo(cx + rad * 0.55f, cy + rad * 0.55f)
                    }
                    drawPath(
                        path = handlePath,
                        color = Color(0xFF334155),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                "tune", "filter", "settings" -> {
                    // Cute candy lollipop sliders (Three vertical bars with tiny bubble sliders)
                    val lines = listOf(
                        SliderLine(cx - 5.dp.toPx(), cy - 1.dp.toPx(), Color(0xFFE11D48)),
                        SliderLine(cx, cy + 2.dp.toPx(), Color(0xFFBE123C)),
                        SliderLine(cx + 5.dp.toPx(), cy - 3.dp.toPx(), Color(0xFFE11D48))
                    )
                    for (line in lines) {
                        val lx = line.x
                        val sy = line.y
                        val col = line.color
                        drawLine(
                            color = Color(0xFFFDA4AF),
                            start = Offset(lx, cy - rad * 0.55f),
                            end = Offset(lx, cy + rad * 0.55f),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = col,
                            radius = 2.5.dp.toPx(),
                            center = Offset(lx, sy)
                        )
                    }
                }
                "favorite", "heart", "liked" -> {
                    // Masterpiece chubby bright pink heart with reflecting bubble!
                    val path = Path().apply {
                        moveTo(cx, cy + rad * 0.45f)
                        cubicTo(cx - rad * 0.5f, cy - rad * 0.1f, cx - rad * 0.6f, cy - rad * 0.55f, cx - rad * 0.25f, cy - rad * 0.65f)
                        cubicTo(cx - rad * 0.05f, cy - rad * 0.7f, cx, cy - rad * 0.35f, cx, cy - rad * 0.35f)
                        cubicTo(cx, cy - rad * 0.35f, cx + rad * 0.05f, cy - rad * 0.7f, cx + rad * 0.25f, cy - rad * 0.65f)
                        cubicTo(cx + rad * 0.6f, cy - rad * 0.55f, cx + rad * 0.5f, cy - rad * 0.1f, cx, cy + rad * 0.45f)
                        close()
                    }
                    drawPath(path, color = Color(0xFFE11D48))
                    // Shining little white bubble on the top right lobe
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 1.25.dp.toPx(),
                        center = Offset(cx - rad * 0.25f, cy - rad * 0.35f)
                    )
                }
                "favorite_border", "heart_empty", "unliked" -> {
                    val path = Path().apply {
                        moveTo(cx, cy + rad * 0.45f)
                        cubicTo(cx - rad * 0.5f, cy - rad * 0.1f, cx - rad * 0.6f, cy - rad * 0.55f, cx - rad * 0.25f, cy - rad * 0.65f)
                        cubicTo(cx - rad * 0.05f, cy - rad * 0.7f, cx, cy - rad * 0.35f, cx, cy - rad * 0.35f)
                        cubicTo(cx, cy - rad * 0.35f, cx + rad * 0.05f, cy - rad * 0.7f, cx + rad * 0.25f, cy - rad * 0.65f)
                        cubicTo(cx + rad * 0.6f, cy - rad * 0.55f, cx + rad * 0.5f, cy - rad * 0.1f, cx, cy + rad * 0.45f)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFF43F5E),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                "share", "send_post" -> {
                    // Outgoing neat envelope/box
                    drawRoundRect(
                        color = Color(0xFF047857),
                        topLeft = Offset(cx - rad * 0.45f, cy - rad * 0.2f),
                        size = Size(rad * 0.9f, rad * 0.65f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    // Upward share arrow
                    val path = Path().apply {
                        moveTo(cx, cy + rad * 0.1f)
                        lineTo(cx, cy - rad * 0.45f)
                        moveTo(cx - rad * 0.2f, cy - rad * 0.25f)
                        lineTo(cx, cy - rad * 0.45f)
                        lineTo(cx + rad * 0.2f, cy - rad * 0.25f)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFD1FAE5),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                "school", "learn", "study" -> {
                    // Cute round yellow academic pencil
                    rotate(degrees = -45f, pivot = Offset(cx, cy)) {
                        drawRoundRect(
                            color = Color(0xFFD97706),
                            topLeft = Offset(cx - 3.dp.toPx(), cy - rad * 0.5f),
                            size = Size(6.dp.toPx(), rad * 0.8f),
                            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                        )
                        // Pencil eraser Cap
                        drawRoundRect(
                            color = Color(0xFFF43F5E),
                            topLeft = Offset(cx - 3.dp.toPx(), cy - rad * 0.65f),
                            size = Size(6.dp.toPx(), rad * 0.15f),
                            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                        )
                    }
                }
                "logout", "signout", "exit" -> {
                    // Rounded door shape
                    drawRoundRect(
                        color = Color(0xFF991B1B),
                        topLeft = Offset(cx - rad * 0.45f, cy - rad * 0.5f),
                        size = Size(rad * 0.55f, rad * 1.0f),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Escaping Arrow pointing right
                    val path = Path().apply {
                        moveTo(cx - rad * 0.15f, cy)
                        lineTo(cx + rad * 0.5f, cy)
                        moveTo(cx + rad * 0.3f, cy - rad * 0.2f)
                        lineTo(cx + rad * 0.5f, cy)
                        lineTo(cx + rad * 0.3f, cy + rad * 0.2f)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFB91C1C),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                "email", "mail" -> {
                    // Rectangular envelope fold
                    val envelopeTopLeft = Offset(cx - rad * 0.55f, cy - rad * 0.35f)
                    val envelopeSize = Size(rad * 1.1f, rad * 0.75f)
                    
                    drawRoundRect(
                        color = Color(0xFF4338CA),
                        topLeft = envelopeTopLeft,
                        size = envelopeSize,
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    // Folds meeting in the center
                    val linePath = Path().apply {
                        moveTo(cx - rad * 0.53f, cy - rad * 0.31f)
                        lineTo(cx, cy + rad * 0.05f)
                        lineTo(cx + rad * 0.53f, cy - rad * 0.31f)
                    }
                    drawPath(
                        path = linePath,
                        color = Color(0xFFC7D2FE),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                "visibility", "eye", "show" -> {
                    // Eye oval frame
                    val eyePath = Path().apply {
                        moveTo(cx - rad * 0.55f, cy)
                        quadraticTo(cx, cy - rad * 0.45f, cx + rad * 0.55f, cy)
                        quadraticTo(cx, cy + rad * 0.45f, cx - rad * 0.55f, cy)
                    }
                    drawPath(
                        path = eyePath,
                        color = Color(0xFF6D28D9),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Pupil
                    drawCircle(
                        color = Color(0xFF4C1D95),
                        radius = rad * 0.2f,
                        center = Offset(cx, cy)
                    )
                    // Spark reflection dot
                    drawCircle(
                        color = Color.White,
                        radius = 1.dp.toPx(),
                        center = Offset(cx - 1.dp.toPx(), cy - 1.dp.toPx())
                    )
                }
                "visibility_off", "hide" -> {
                    val eyePath = Path().apply {
                        moveTo(cx - rad * 0.55f, cy)
                        quadraticTo(cx, cy - rad * 0.45f, cx + rad * 0.55f, cy)
                        quadraticTo(cx, cy + rad * 0.45f, cx - rad * 0.55f, cy)
                    }
                    drawPath(
                        path = eyePath,
                        color = Color(0xFF4B5563),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Diagonal sleep/closed lashes
                    drawLine(
                        color = Color(0xFF374151),
                        start = Offset(cx - rad * 0.45f, cy - rad * 0.35f),
                        end = Offset(cx + rad * 0.45f, cy + rad * 0.35f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                "send", "paper_plane" -> {
                    // Beautiful angled paper plane flying up right
                    val planePath = Path().apply {
                        moveTo(cx - rad * 0.5f, cy + rad * 0.1f)
                        lineTo(cx + rad * 0.5f, cy - rad * 0.45f)
                        lineTo(cx + 0.1f, cy + rad * 0.45f)
                        lineTo(cx - rad * 0.1f, cy + rad * 0.1f)
                        close()
                    }
                    drawPath(path = planePath, color = Color(0xFF047857))
                    
                    val internalFold = Path().apply {
                        moveTo(cx - rad * 0.1f, cy + rad * 0.1f)
                        lineTo(cx + rad * 0.5f, cy - rad * 0.45f)
                    }
                    drawPath(
                        path = internalFold,
                        color = Color(0xFFA7F3D0),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                "shield", "security", "privacy" -> {
                    // Shield symbol path
                    val shieldPath = Path().apply {
                        moveTo(cx - rad * 0.45f, cy - rad * 0.45f)
                        lineTo(cx + rad * 0.45f, cy - rad * 0.45f)
                        lineTo(cx + rad * 0.45f, cy)
                        quadraticTo(cx + rad * 0.45f, cy + rad * 0.4f, cx, cy + rad * 0.6f)
                        quadraticTo(cx - rad * 0.45f, cy + rad * 0.4f, cx - rad * 0.45f, cy)
                        close()
                    }
                    drawPath(shieldPath, color = Color(0xFFBE185D))
                    // Inner lighter crescent
                    val shieldPathInner = Path().apply {
                        moveTo(cx - rad * 0.3f, cy - rad * 0.35f)
                        lineTo(cx + rad * 0.3f, cy - rad * 0.35f)
                        lineTo(cx + rad * 0.3f, cy)
                        quadraticTo(cx + rad * 0.3f, cy + rad * 0.25f, cx, cy + rad * 0.42f)
                        quadraticTo(cx - rad * 0.3f, cy + rad * 0.25f, cx - rad * 0.3f, cy)
                        close()
                    }
                    drawPath(shieldPathInner, color = Color(0xFFFCE7F3))
                }
                "description", "document", "terms" -> {
                    // Beautiful mini document scroll
                    drawRoundRect(
                        color = Color(0xFFD97706),
                        topLeft = Offset(cx - rad * 0.4f, cy - rad * 0.5f),
                        size = Size(rad * 0.8f, rad * 1.0f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    // Paper inner sheets
                    drawRoundRect(
                        color = Color(0xFFFEF3C7),
                        topLeft = Offset(cx - rad * 0.3f, cy - rad * 0.4f),
                        size = Size(rad * 0.6f, rad * 0.8f),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                    // Multi script row lines
                    val lineList = floatArrayOf(cy - 2.dp.toPx(), cy, cy + 2.dp.toPx())
                    for (ly in lineList) {
                        drawLine(
                            color = Color(0xFFD97706),
                            start = Offset(cx - rad * 0.2f, ly),
                            end = Offset(cx + rad * 0.2f, ly),
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                "open_in_new", "external_link" -> {
                    // Card boundary
                    drawRoundRect(
                        color = Color(0xFF4B5563),
                        topLeft = Offset(cx - rad * 0.5f, cy - rad * 0.1f),
                        size = Size(rad * 0.7f, rad * 0.7f),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Out-linking arrow
                    val path = Path().apply {
                        moveTo(cx - rad * 0.1f, cy + rad * 0.2f)
                        lineTo(cx + rad * 0.45f, cy - rad * 0.35f)
                        moveTo(cx + rad * 0.15f, cy - rad * 0.35f)
                        lineTo(cx + rad * 0.45f, cy - rad * 0.35f)
                        lineTo(cx + rad * 0.45f, cy - rad * 0.05f)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF374151),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                "star", "rating", "gold_star" -> {
                    // Perfectly drawn bulky five peak yellow Star
                    val path = Path().apply {
                        val spikes = 5
                        val outerRadius = rad * 0.65f
                        val innerRadius = rad * 0.3f
                        var angle = Math.PI / 2 * 3
                        val step = Math.PI / spikes

                        moveTo(
                            (cx + Math.cos(angle) * outerRadius).toFloat(),
                            (cy + Math.sin(angle) * outerRadius).toFloat()
                        )
                        for (i in 0 until spikes) {
                            angle += step
                            lineTo(
                                (cx + Math.cos(angle) * innerRadius).toFloat(),
                                (cy + Math.sin(angle) * innerRadius).toFloat()
                            )
                            angle += step
                            lineTo(
                                (cx + Math.cos(angle) * outerRadius).toFloat(),
                                (cy + Math.sin(angle) * outerRadius).toFloat()
                            )
                        }
                        close()
                    }
                    drawPath(path, color = Color(0xFFD97706))
                    
                    val innerFillPath = Path().apply {
                        val spikes = 5
                        val outerRadius = rad * 0.55f
                        val innerRadius = rad * 0.25f
                        var angle = Math.PI / 2 * 3
                        val step = Math.PI / spikes

                        moveTo(
                            (cx + Math.cos(angle) * outerRadius).toFloat(),
                            (cy + Math.sin(angle) * outerRadius).toFloat()
                        )
                        for (i in 0 until spikes) {
                            angle += step
                            lineTo(
                                (cx + Math.cos(angle) * innerRadius).toFloat(),
                                (cy + Math.sin(angle) * innerRadius).toFloat()
                            )
                            angle += step
                            lineTo(
                                (cx + Math.cos(angle) * outerRadius).toFloat(),
                                (cy + Math.sin(angle) * outerRadius).toFloat()
                            )
                        }
                        close()
                    }
                    drawPath(innerFillPath, color = Color(0xFFFBBF24))
                }
                "crown", "vip", "premium", "crown_outline" -> {
                    // Royal golden Crown!
                    val crownPath = Path().apply {
                        moveTo(cx - rad * 0.55f, cy + rad * 0.45f)
                        lineTo(cx - rad * 0.65f, cy - rad * 0.15f)
                        lineTo(cx - rad * 0.25f, cy + rad * 0.1f)
                        lineTo(cx, cy - rad * 0.45f)
                        lineTo(cx + rad * 0.25f, cy + rad * 0.1f)
                        lineTo(cx + rad * 0.65f, cy - rad * 0.15f)
                        lineTo(cx + rad * 0.55f, cy + rad * 0.45f)
                        close()
                    }
                    drawPath(crownPath, color = Color(0xFFD97706))
                    // Shimmer highlights
                    val crownInner = Path().apply {
                        moveTo(cx - rad * 0.5f, cy + rad * 0.4f)
                        lineTo(cx - rad * 0.58f, cy - rad * 0.1f)
                        lineTo(cx - rad * 0.23f, cy + rad * 0.13f)
                        lineTo(cx, cy - rad * 0.35f)
                        lineTo(cx + rad * 0.23f, cy + rad * 0.13f)
                        lineTo(cx + rad * 0.58f, cy - rad * 0.1f)
                        lineTo(cx + rad * 0.5f, cy + rad * 0.4f)
                        close()
                    }
                    drawPath(crownInner, color = Color(0xFFFBBF24))
                    // Jewel peaks
                     drawCircle(color = Color(0xFFEF4444), radius = 1.25.dp.toPx(), center = Offset(cx - rad * 0.65f, cy - rad * 0.15f))
                    drawCircle(color = Color(0xFF3B82F6), radius = 1.5.dp.toPx(), center = Offset(cx, cy - rad * 0.45f))
                    drawCircle(color = Color(0xFFEF4444), radius = 1.25.dp.toPx(), center = Offset(cx + rad * 0.65f, cy - rad * 0.15f))
                }
                "warning", "alert" -> {
                    // Soft alert triangle
                    val path = Path().apply {
                        moveTo(cx, cy - rad * 0.55f)
                        lineTo(cx - rad * 0.6f, cy + rad * 0.45f)
                        lineTo(cx + rad * 0.6f, cy + rad * 0.45f)
                        close()
                    }
                    drawPath(path, color = Color(0xFFD97706))
                    drawPath(
                        path = path,
                        color = Color(0xFFFBBF24),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Exclamation point mark
                    drawRoundRect(
                        color = Color(0xFF78350F),
                        topLeft = Offset(cx - 1.dp.toPx(), cy - rad * 0.15f),
                        size = Size(2.dp.toPx(), rad * 0.35f),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF78350F),
                        radius = 1.dp.toPx(),
                        center = Offset(cx, cy + rad * 0.3f)
                    )
                }
                "trash", "delete", "clear_all" -> {
                    // Small trash bin container
                    drawRoundRect(
                        color = Color(0xFF991B1B),
                        topLeft = Offset(cx - rad * 0.35f, cy - rad * 0.2f),
                        size = Size(rad * 0.7f, rad * 0.7f),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                    // Lid cap
                    drawRoundRect(
                        color = Color(0xFF7F1D1D),
                        topLeft = Offset(cx - rad * 0.45f, cy - rad * 0.4f),
                        size = Size(rad * 0.9f, 3.5.dp.toPx()),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                    // Inner bin script slot lines
                    val xSlots = floatArrayOf(cx - 3.dp.toPx(), cx, cx + 3.dp.toPx())
                    for (lx in xSlots) {
                        drawLine(
                            color = Color(0xFFFCA5A5),
                            start = Offset(lx, cy - rad * 0.05f),
                            end = Offset(lx, cy + rad * 0.35f),
                            strokeWidth = 1.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                "voice_podium", "podcasts" -> {
                    // Cosmic podcasts sound track base sphere
                    drawCircle(
                        color = Color(0xFF6D28D9),
                        radius = rad * 0.65f,
                        center = Offset(cx, cy)
                    )
                    // Horizontal sound amplitude lines representing waves
                    val soundWaves = listOf(
                        SoundWave(3.dp.toPx(), Color(0xFFA78BFA)),
                        SoundWave(6.dp.toPx(), Color(0xFFC084FC)),
                        SoundWave(4.dp.toPx(), Color(0xFFA78BFA))
                    )
                    for (i in soundWaves.indices) {
                        val wave = soundWaves[i]
                        val len = wave.len
                        val col = wave.color
                        val offsetDx = (i - 1) * 4.dp.toPx()
                        drawLine(
                            color = col,
                            start = Offset(cx + offsetDx, cy - len),
                            end = Offset(cx + offsetDx, cy + len),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    // Sound orbiting track
                    drawCircle(
                        color = Color(0xFFDDD6FE).copy(alpha = 0.5f),
                        radius = rad * 0.65f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                "more_vert", "options" -> {
                    // Vertical stacking sweet triple dango balls on a cute skewer stick!
                    drawLine(
                        color = Color(0xFFD1D5DB),
                        start = Offset(cx, cy - rad * 0.6f),
                        end = Offset(cx, cy + rad * 0.6f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawCircle(color = Color(0xFFFCA5A5), radius = 2.5.dp.toPx(), center = Offset(cx, cy - 6.dp.toPx()))
                    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(color = Color(0xFFA7F3D0), radius = 2.5.dp.toPx(), center = Offset(cx, cy + 6.dp.toPx()))
                }
                "translate" -> {
                    // Interwoven beautiful Speech Cards (Japanese "A" and English "A")
                    drawRoundRect(
                        color = Color(0xFF8B5CF6),
                        topLeft = Offset(cx - rad * 0.55f, cy - rad * 0.45f),
                        size = Size(rad * 0.65f, rad * 0.55f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFFEC4899),
                        topLeft = Offset(cx - rad * 0.05f, cy - rad * 0.1f),
                        size = Size(rad * 0.6f, rad * 0.55f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    // Connecting cute ribbon curves
                    val connect = Path().apply {
                        moveTo(cx - rad * 0.2f, cy - rad * 0.1f)
                        quadraticTo(cx, cy, cx + rad * 0.2f, cy + rad * 0.1f)
                    }
                    drawPath(
                        path = connect,
                        color = Color.White,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                else -> {
                    // Fallback to cute shiny star
                    val path = Path().apply {
                        val spikes = 5
                        val outerRadius = rad * 0.6f
                        val innerRadius = rad * 0.25f
                        var angle = Math.PI / 2 * 3
                        val step = Math.PI / spikes

                        moveTo(
                            (cx + Math.cos(angle) * outerRadius).toFloat(),
                            (cy + Math.sin(angle) * outerRadius).toFloat()
                        )
                        for (i in 0 until spikes) {
                            angle += step
                            lineTo(
                                (cx + Math.cos(angle) * innerRadius).toFloat(),
                                (cy + Math.sin(angle) * innerRadius).toFloat()
                            )
                            angle += step
                            lineTo(
                                (cx + Math.cos(angle) * outerRadius).toFloat(),
                                (cy + Math.sin(angle) * outerRadius).toFloat()
                            )
                        }
                        close()
                    }
                    drawPath(path, color = Color(0xFFF59E0B))
                }
            }
        }
    }
}

/**
 * Compatible ImageVector overlay helper that automatically maps any standard Material Design
 * ImageVector (e.g. Icons.Default.Home) into our cute/soft emoji representations.
 */
@Composable
fun Icon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val iconName = remember(imageVector.name) {
        val cleanName = imageVector.name.substringAfterLast(".").lowercase()
        when {
            cleanName.contains("arrowback") || cleanName.contains("back") -> "back"
            cleanName.contains("arrowforward") || cleanName.contains("forward") || cleanName.contains("arrowright") || cleanName.contains("keyboardarrowright") -> "forward"
            cleanName.contains("arrowup") -> "arrow_up"
            cleanName.contains("arrowdown") -> "arrow_down"
            cleanName.contains("add") || cleanName.contains("plus") -> "add"
            cleanName.contains("lock") -> "lock"
            cleanName.contains("close") || cleanName.contains("clear") || cleanName.contains("dismiss") || cleanName.contains("searchoff") -> "close"
            cleanName.contains("home") -> "home"
            cleanName.contains("mic") || cleanName.contains("microphone") -> "mic"
            cleanName.contains("headset") || cleanName.contains("listener") -> "listener"
            cleanName.contains("language") || cleanName.contains("globe") || cleanName.contains("explore") -> "language"
            cleanName.contains("chat") || cleanName.contains("message") || cleanName.contains("bubble") -> "chat"
            cleanName.contains("person") || cleanName.contains("profile") || cleanName.contains("account") -> "profile"
            cleanName.contains("campaign") || cleanName.contains("broadcast") -> "campaign"
            cleanName.contains("search") -> "search"
            cleanName.contains("tune") || cleanName.contains("filter") -> "tune"
            cleanName.contains("favoriteborder") || cleanName.contains("heart_empty") -> "favorite_border"
            cleanName.contains("favorite") || cleanName.contains("heart") -> "favorite"
            cleanName.contains("share") -> "share"
            cleanName.contains("school") || cleanName.contains("learn") -> "school"
            cleanName.contains("logout") -> "logout"
            cleanName.contains("email") || cleanName.contains("mail") -> "email"
            cleanName.contains("visibilityoff") || cleanName.contains("hide") -> "visibility_off"
            cleanName.contains("visibility") || cleanName.contains("show") -> "visibility"
            cleanName.contains("send") -> "send"
            cleanName.contains("shield") || cleanName.contains("security") || cleanName.contains("privacy") -> "shield"
            cleanName.contains("description") || cleanName.contains("document") -> "description"
            cleanName.contains("openinnew") -> "open_in_new"
            cleanName.contains("star") -> "star"
            cleanName.contains("crown") || cleanName.contains("vip") -> "crown"
            cleanName.contains("podcasts") || cleanName.contains("voice_podium") -> "voice_podium"
            cleanName.contains("morevert") -> "more_vert"
            cleanName.contains("translate") -> "translate"
            else -> "star"
        }
    }
    CuteIcon(
        iconName = iconName,
        modifier = modifier
    )
}

/**
 * Highly tactile "Soft & Cute" Button
 */
@Composable
fun CuteIconButton(
    onClick: () -> Unit,
    iconName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    badgeColor: Color? = null,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Disable standard rigid Android ripples for a organic squish feel
                onClick = onClick
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        CuteIcon(
            iconName = iconName,
            size = size - 4.dp,
            badgeColor = badgeColor
        )
    }
}
