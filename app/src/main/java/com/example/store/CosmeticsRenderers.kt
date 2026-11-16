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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
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
    enableFrameAnimation: Boolean = true,
    content: @Composable () -> Unit
) {
    if (config == null) {
        Box(modifier = modifier.size(sizeDp), contentAlignment = Alignment.Center) {
            content()
        }
        return
    }

    val context = LocalContext.current
    val asset = config.asset
    var isAssetLoadFailed by remember(asset?.url) { mutableStateOf(false) }
    val isAssetFrame = config.kind.equals("asset", ignoreCase = true) &&
        asset != null &&
        asset.url.isNotBlank() &&
        !isAssetLoadFailed

    if (isAssetFrame && asset != null) {
        val ratio = asset.avatarRatio.coerceIn(0.25f, 1.0f)
        val frameSize = sizeDp / ratio
        val imageRequest = remember(context, asset.url, enableFrameAnimation) {
            ImageRequest.Builder(context)
                .data(asset.url)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .apply {
                    if (enableFrameAnimation && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        decoderFactory(coil.decode.ImageDecoderDecoder.Factory())
                    } else {
                        decoderFactory(coil.decode.BitmapFactoryDecoder.Factory())
                    }
                }
                .listener(
                    onError = { _, _ -> isAssetLoadFailed = true }
                )
                .build()
        }

        Box(
            modifier = modifier.size(sizeDp),
            contentAlignment = Alignment.Center
        ) {
            // Core Avatar Container (zIndex 1f, no ring padding)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                content()
            }

            // Transparent Frame Image Asset Overlaid ON TOP of Avatar (zIndex 2f, unclipped overhang)
            AsyncImage(
                model = imageRequest,
                contentDescription = "Avatar Frame Asset",
                modifier = Modifier
                    .size(frameSize)
                    .offset(x = asset.offset.xDp.dp, y = asset.offset.yDp.dp)
                    .zIndex(2f),
                contentScale = ContentScale.Fit
            )
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

        // Ring Canvas (zIndex 2f so ring sits crisply on DP perimeter)
        val ringScale = (sizeDp.value / 48f).coerceIn(0.6f, 1.8f)
        val scaledStrokeDp = (ring.widthDp * ringScale).coerceAtLeast(1.5f)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f)
                .rotate(rotationAngle)
        ) {
            val strokeW = scaledStrokeDp.dp.toPx()
            val stroke = Stroke(width = strokeW)
            val brush = if (ringColors.size > 1) {
                Brush.sweepGradient(ringColors)
            } else SolidColor(ringColors.firstOrNull() ?: Color(0xFFFFD700))

            val center = Offset(size.width / 2f, size.height / 2f)
            val half = size.width / 2f
            val radius = half - strokeW * 0.5f

            when (ring.shape.lowercase()) {
                "hexagon" -> {
                    val apothem = half - strokeW * 0.45f
                    val hexRadius = apothem / 0.8660254f
                    val p = Path()
                    for (i in 0 until 6) {
                        val angleRad = Math.toRadians((i * 60 - 30).toDouble())
                        val x = center.x + hexRadius * cos(angleRad).toFloat()
                        val y = center.y + hexRadius * sin(angleRad).toFloat()
                        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                    p.close()
                    drawPath(p, brush, style = Stroke(width = strokeW, join = StrokeJoin.Round))
                }
                "scalloped" -> {
                    val valleyR = half - strokeW * 0.35f
                    val crestR = valleyR * 1.13f
                    val p = Path()
                    val lobes = 12
                    for (i in 0 until lobes) {
                        val aStart = Math.toRadians((i * (360.0 / lobes)).toDouble())
                        val aMid = Math.toRadians(((i + 0.5) * (360.0 / lobes)).toDouble())
                        val aEnd = Math.toRadians(((i + 1) * (360.0 / lobes)).toDouble())
                        val startX = center.x + valleyR * cos(aStart).toFloat()
                        val startY = center.y + valleyR * sin(aStart).toFloat()
                        val ctrlX = center.x + crestR * cos(aMid).toFloat()
                        val ctrlY = center.y + crestR * sin(aMid).toFloat()
                        val endX = center.x + valleyR * cos(aEnd).toFloat()
                        val endY = center.y + valleyR * sin(aEnd).toFloat()
                        if (i == 0) p.moveTo(startX, startY)
                        p.quadraticTo(ctrlX, ctrlY, endX, endY)
                    }
                    p.close()
                    drawPath(p, brush, style = Stroke(width = strokeW * 0.85f, join = StrokeJoin.Round))
                }
                "double" -> {
                    val innerStrokeW = (strokeW * 0.55f).coerceAtLeast(1.5f)
                    val outerStrokeW = (strokeW * 0.42f).coerceAtLeast(1.2f)
                    val innerRadius = half - strokeW * 0.65f
                    val outerRadius = half - outerStrokeW * 0.4f
                    drawCircle(brush, radius = outerRadius, center = center, style = Stroke(width = outerStrokeW))
                    drawCircle(brush, radius = innerRadius, center = center, style = Stroke(width = innerStrokeW))
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
                    val px = center.x + (radius + strokeW * 0.75f) * cos(angle).toFloat()
                    val py = center.y + (radius + strokeW * 0.75f) * sin(angle).toFloat()
                    val pColor = pColors[i % pColors.size]
                    drawCircle(pColor, radius = 2.dp.toPx(), center = Offset(px, py))
                }
            }
        }

        // Inner Avatar (flush against inner stroke boundary)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .padding((scaledStrokeDp * 0.8f).dp)
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
                    .zIndex(3f)
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
    fontWeight: FontWeight? = null,
    defaultColor: Color = Color.Unspecified,
    animate: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    if (config == null) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight ?: FontWeight.Bold,
            color = defaultColor,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier
        )
        return
    }

    val fallbackColor = if (defaultColor != Color.Unspecified) defaultColor else Color(0xFFFFFFFF)
    val colors = remember(config.colorsHex, fallbackColor) {
        val parsed = config.colorsHex.map { parseHexColor(it, fallbackColor) }
        if (parsed.isEmpty()) listOf(fallbackColor) else parsed
    }
    val glowColor = remember(config.glowHex) {
        config.glowHex?.let { parseHexColor(it, Color.Transparent) }?.takeIf { it != Color.Transparent }
    }
    val resolvedWeight = if (config.bold) FontWeight.ExtraBold else (fontWeight ?: FontWeight.Bold)
    val animType = config.animation.trim().lowercase()
    val speedMs = config.speedMs.toInt().coerceIn(600, 12000)

    when (animType) {
        "glow_pulse" -> {
            val baseColor = colors.first()
            val pulseGlow = glowColor ?: baseColor
            val pulse = if (animate) {
                val infiniteTransition = rememberInfiniteTransition(label = "nameGlowPulse")
                val p by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween((speedMs / 2).coerceAtLeast(350), easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glowPulse"
                )
                p
            } else 0.5f

            val shadow = Shadow(
                color = pulseGlow.copy(alpha = 0.42f + 0.55f * pulse),
                blurRadius = 6f + 14f * pulse
            )
            Text(
                text = text,
                style = TextStyle(
                    color = baseColor,
                    fontSize = fontSize,
                    fontWeight = resolvedWeight,
                    shadow = shadow
                ),
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier
            )
        }
        "shimmer" -> {
            val c0 = colors.first()
            val c1 = colors.getOrElse(1) { c0 }
            val sweepProgress = if (animate) {
                val infiniteTransition = rememberInfiniteTransition(label = "nameShimmer")
                val p by infiniteTransition.animateFloat(
                    initialValue = -0.35f,
                    targetValue = 1.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(speedMs, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "shimmer"
                )
                p
            } else 0.5f

            val shimmerColors = if (animate) {
                listOf(c0, c1, Color.White, c1, c0)
            } else if (colors.size > 1) {
                colors
            } else {
                listOf(c0, c0)
            }
            val spanPx = 420f
            val centerPx = sweepProgress * spanPx
            val brush = if (animate) {
                Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(centerPx - 140f, 0f),
                    end = Offset(centerPx + 140f, 90f),
                    tileMode = TileMode.Clamp
                )
            } else {
                Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset.Zero,
                    end = Offset(240f, 0f)
                )
            }
            Text(
                text = text,
                style = TextStyle(
                    brush = brush,
                    fontSize = fontSize,
                    fontWeight = resolvedWeight,
                    shadow = glowColor?.let { Shadow(color = it.copy(alpha = 0.7f), blurRadius = 8f) }
                ),
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier
            )
        }
        "gradient_flow" -> {
            val spanPx = 360f
            val flowColors = remember(colors) {
                if (colors.size > 1) colors + colors.first() else listOf(colors.first(), colors.first())
            }
            val flowOffset = if (animate) {
                val infiniteTransition = rememberInfiniteTransition(label = "nameGradientFlow")
                val off by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = spanPx,
                    animationSpec = infiniteRepeatable(
                        animation = tween(speedMs, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "gradientFlow"
                )
                off
            } else 0f

            val brush = Brush.linearGradient(
                colors = flowColors,
                start = Offset(flowOffset, 0f),
                end = Offset(flowOffset + spanPx, 0f),
                tileMode = TileMode.Repeated
            )
            Text(
                text = text,
                style = TextStyle(
                    brush = brush,
                    fontSize = fontSize,
                    fontWeight = resolvedWeight,
                    shadow = glowColor?.let { Shadow(color = it.copy(alpha = 0.65f), blurRadius = 10f) }
                ),
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier
            )
        }
        else -> {
            // "none" or unknown animation -> static text in colors[0]
            Text(
                text = text,
                style = TextStyle(
                    color = colors.first(),
                    fontSize = fontSize,
                    fontWeight = resolvedWeight,
                    shadow = glowColor?.let { Shadow(color = it.copy(alpha = 0.7f), blurRadius = 8f) }
                ),
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier
            )
        }
    }
}

// ====================================================
// 5. DECORATED USER NAME COMPOSABLE
// ====================================================
@Composable
fun DecoratedUserName(
    userId: String?,
    name: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight? = null,
    defaultColor: Color = Color.Unspecified,
    animate: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val userCosmeticsMap by CosmeticsManager.userCosmeticsMap.collectAsState()
    val nameEffectConfig = remember(userId, userCosmeticsMap) {
        if (!userId.isNullOrBlank()) {
            val userRow = userCosmeticsMap[userId] ?: CosmeticsManager.getCachedCosmetics(userId)
            val rawConfig = userRow?.name_effect?.config
            if (rawConfig != null) {
                runCatching { CosmeticsConfigParser.parseNameEffect(rawConfig) }.getOrNull()
            } else null
        } else null
    }

    if (nameEffectConfig != null) {
        StyledName(
            text = name,
            config = nameEffectConfig,
            modifier = modifier,
            fontSize = fontSize,
            fontWeight = fontWeight,
            defaultColor = defaultColor,
            animate = animate,
            maxLines = maxLines,
            overflow = overflow
        )
    } else {
        Text(
            text = name,
            fontSize = fontSize,
            fontWeight = fontWeight ?: FontWeight.Bold,
            color = defaultColor,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier
        )
    }
}
