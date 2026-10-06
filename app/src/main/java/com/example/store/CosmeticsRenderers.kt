package com.example.store

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// ====================================================
// 1. THEMED BUBBLE RENDERER
// ====================================================
@Composable
fun ThemedBubble(
    config: BubbleThemeConfig?,
    modifier: Modifier = Modifier,
    isOutgoing: Boolean = true,
    content: @Composable () -> Unit
) {
    if (config == null) {
        // Fallback default rendering
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (isOutgoing) Color(0xFFFFE8A3) else Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            content()
        }
        return
    }

    val bgColors = remember(config.bgColorsHex) {
        config.bgColorsHex.map { parseHexColor(it, if (isOutgoing) Color(0xFFFFE8A3) else Color.White) }
    }
    val brush = remember(bgColors, config.bgAngle) {
        if (bgColors.size > 1) {
            val rad = Math.toRadians(config.bgAngle.toDouble())
            Brush.linearGradient(
                colors = bgColors,
                start = Offset(0f, 0f),
                end = Offset(cos(rad).toFloat() * 300f, sin(rad).toFloat() * 300f)
            )
        } else {
            SolidColor(bgColors.firstOrNull() ?: if (isOutgoing) Color(0xFFFFE8A3) else Color.White)
        }
    }

    val radDp = config.radiusDp.coerceIn(4f, 32f).dp
    val tailRadDp = if (config.tailStyle.equals("sharp", ignoreCase = true)) 2.dp else radDp

    val shape = remember(isOutgoing, radDp, tailRadDp) {
        if (isOutgoing) {
            RoundedCornerShape(topStart = radDp, topEnd = radDp, bottomStart = radDp, bottomEnd = tailRadDp)
        } else {
            RoundedCornerShape(topStart = radDp, topEnd = radDp, bottomStart = tailRadDp, bottomEnd = radDp)
        }
    }

    val glowColor = remember(config.glowHex) {
        config.glowHex?.let { parseHexColor(it, Color.Transparent) }
    }

    var bubbleModifier = modifier

    if (glowColor != null && glowColor != Color.Transparent) {
        bubbleModifier = bubbleModifier.shadow(elevation = 6.dp, shape = shape, spotColor = glowColor, ambientColor = glowColor)
    }

    if (config.border != null) {
        val bColor = remember(config.border.colorHex) { parseHexColor(config.border.colorHex, Color.Gray) }
        bubbleModifier = bubbleModifier.border(width = config.border.widthDp.dp, color = bColor, shape = shape)
    }

    Box(
        modifier = bubbleModifier
            .clip(shape)
            .background(brush = brush)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        content()
    }
}

// ====================================================
// 2. CHAT BACKGROUND RENDERER
// ====================================================
@Composable
fun ChatBackground(
    config: ChatBgConfig?,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (config == null) {
        Box(modifier = modifier.background(if (isDark) Color(0xFF0F1114) else Color(0xFFF6F4EE)))
        return
    }

    val style = if (isDark) config.dark else config.light

    when (style) {
        is ChatBgStyle.Solid -> {
            val color = remember(style.colorHex) { parseHexColor(style.colorHex, if (isDark) Color(0xFF0F1114) else Color(0xFFF6F4EE)) }
            Box(modifier = modifier.background(color))
        }
        is ChatBgStyle.Gradient -> {
            val colors = remember(style.colorsHex) { style.colorsHex.map { parseHexColor(it, Color.Gray) } }
            val rad = Math.toRadians(style.angle.toDouble())
            val brush = remember(colors, style.angle) {
                if (colors.size > 1) {
                    Brush.linearGradient(
                        colors = colors,
                        start = Offset(0f, 0f),
                        end = Offset(cos(rad).toFloat() * 1000f, sin(rad).toFloat() * 1000f)
                    )
                } else SolidColor(colors.firstOrNull() ?: Color.Gray)
            }
            Box(modifier = modifier.background(brush))
        }
        is ChatBgStyle.Pattern -> {
            val baseColor = remember(style.baseColorHex) { parseHexColor(style.baseColorHex, Color(0xFFF6F4EE)) }
            val patColor = remember(style.patternColorHex) { parseHexColor(style.patternColorHex, Color.Gray).copy(alpha = style.opacity) }

            Box(modifier = modifier.background(baseColor)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spacing = 64.dp.toPx()
                    val cols = (size.width / spacing).toInt() + 2
                    val rows = (size.height / spacing).toInt() + 2

                    for (r in 0..rows) {
                        for (c in 0..cols) {
                            val x = c * spacing + (if (r % 2 == 1) spacing / 2f else 0f)
                            val y = r * spacing
                            if (style.patternType.equals("stars", ignoreCase = true)) {
                                drawCircle(patColor, radius = 2.5.dp.toPx(), center = Offset(x, y))
                            } else {
                                // Petals pattern
                                val p = Path().apply {
                                    moveTo(x, y - 5f)
                                    cubicTo(x + 6f, y - 5f, x + 6f, y + 5f, x, y + 5f)
                                    cubicTo(x - 6f, y + 5f, x - 6f, y - 5f, x, y - 5f)
                                }
                                drawPath(p, patColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ====================================================
// 3. AVATAR FRAME RENDERER
// ====================================================
@Composable
fun AvatarFrame(
    config: AvatarFrameConfig?,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 48.dp,
    content: @Composable () -> Unit
) {
    if (config == null) {
        Box(modifier = modifier.size(sizeDp), contentAlignment = Alignment.Center) {
            content()
        }
        return
    }

    val ring = config.ring
    val ringColors = remember(ring.colorsHex) {
        ring.colorsHex.map { parseHexColor(it, Color(0xFFFFD700)) }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "frameAnim")

    val rotationAngle by if (ring.animation.equals("rotate", ignoreCase = true)) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(ring.speedMs.toInt().coerceAtLeast(1000), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotate"
        )
    } else remember { mutableStateOf(0f) }

    val pulseScale by if (ring.animation.equals("pulse", ignoreCase = true)) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(ring.speedMs.toInt().coerceAtLeast(800) / 2, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else remember { mutableStateOf(1f) }

    val glowColor = remember(config.glow?.colorHex) {
        config.glow?.colorHex?.let { parseHexColor(it, Color.Transparent) }
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glow
        if (glowColor != null && glowColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.1f)
                    .clip(CircleShape)
                    .background(RadialGradientShader(center = Offset(0.5f, 0.5f), radius = 100f, colors = listOf(glowColor.copy(alpha = 0.4f), Color.Transparent)).let { Color.Transparent })
            )
        }

        // Ring Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotationAngle)
        ) {
            val strokeW = ring.widthDp.dp.toPx()
            val stroke = Stroke(width = strokeW)
            val brush = if (ringColors.size > 1) {
                Brush.sweepGradient(ringColors)
            } else SolidColor(ringColors.firstOrNull() ?: Color(0xFFFFD700))

            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width - strokeW) / 2f

            when (ring.shape.lowercase()) {
                "hexagon" -> {
                    val p = Path()
                    for (i in 0 until 6) {
                        val angleRad = Math.toRadians((i * 60 - 30).toDouble())
                        val x = center.x + radius * cos(angleRad).toFloat()
                        val y = center.y + radius * sin(angleRad).toFloat()
                        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                    p.close()
                    drawPath(p, brush, style = stroke)
                }
                "scalloped" -> {
                    val p = Path()
                    val scallops = 12
                    for (i in 0 until scallops) {
                        val a1 = Math.toRadians((i * (360.0 / scallops)).toDouble())
                        val r = if (i % 2 == 0) radius else radius * 0.88f
                        val x = center.x + r * cos(a1).toFloat()
                        val y = center.y + r * sin(a1).toFloat()
                        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                    p.close()
                    drawPath(p, brush, style = stroke)
                }
                "double" -> {
                    drawCircle(brush, radius = radius, center = center, style = Stroke(width = strokeW * 0.6f))
                    drawCircle(brush, radius = radius * 0.82f, center = center, style = Stroke(width = strokeW * 0.4f))
                }
                else -> {
                    // Default circle
                    drawCircle(brush, radius = radius, center = center, style = stroke)
                }
            }

            // Particles on Canvas (Cap max 12)
            config.particles?.let { parts ->
                val pColors = parts.colorsHex.map { parseHexColor(it, Color.Yellow) }
                val count = parts.count.coerceIn(1, 12)
                for (i in 0 until count) {
                    val angle = Math.toRadians((i * (360.0 / count) + rotationAngle).toDouble())
                    val px = center.x + (radius + strokeW) * cos(angle).toFloat()
                    val py = center.y + (radius + strokeW) * sin(angle).toFloat()
                    val pColor = pColors[i % pColors.size]
                    drawCircle(pColor, radius = 2.dp.toPx(), center = Offset(px, py))
                }
            }
        }

        // Inner Avatar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding((ring.widthDp + 1f).dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            content()
        }

        // Ornament Emoji
        config.ornament?.let { orn ->
            val align = when (orn.position) {
                "top" -> Alignment.TopCenter
                "bottom" -> Alignment.BottomCenter
                else -> Alignment.TopEnd
            }
            Text(
                text = orn.emoji,
                fontSize = (14 * orn.scale).sp,
                modifier = Modifier
                    .align(align)
                    .offset(x = 2.dp, y = (-2).dp)
            )
        }
    }
}

// Dummy Radial Gradient helper
private fun RadialGradientShader(center: Offset, radius: Float, colors: List<Color>): Color = Color.Transparent

// ====================================================
// 4. STYLED NAME RENDERER
// ====================================================
@Composable
fun StyledName(
    text: String,
    config: NameEffectConfig?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight? = null
) {
    if (config == null) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight ?: FontWeight.Bold,
            modifier = modifier
        )
        return
    }

    val colors = remember(config.colorsHex) {
        config.colorsHex.map { parseHexColor(it, Color.Black) }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "nameAnim")

    val shimmerOffset by if (config.animation.equals("shimmer", ignoreCase = true) || config.animation.equals("gradient_flow", ignoreCase = true)) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 500f,
            animationSpec = infiniteRepeatable(
                animation = tween(config.speedMs.toInt().coerceAtLeast(1000), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer"
        )
    } else remember { mutableStateOf(0f) }

    val brush = remember(colors, shimmerOffset) {
        if (colors.size > 1) {
            Brush.linearGradient(
                colors = colors,
                start = Offset(shimmerOffset, 0f),
                end = Offset(shimmerOffset + 200f, 200f)
            )
        } else SolidColor(colors.firstOrNull() ?: Color.Black)
    }

    val glowColor = remember(config.glowHex) {
        config.glowHex?.let { parseHexColor(it, Color.Transparent) }
    }

    var textStyle = TextStyle(
        brush = brush,
        fontSize = fontSize,
        fontWeight = if (config.bold) FontWeight.ExtraBold else (fontWeight ?: FontWeight.Normal)
    )

    if (glowColor != null && glowColor != Color.Transparent) {
        textStyle = textStyle.copy(
            shadow = Shadow(color = glowColor, blurRadius = 8f)
        )
    }

    Text(
        text = text,
        style = textStyle,
        modifier = modifier
    )
}
