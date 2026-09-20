package com.example.ui.screens.home.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun AnimatedRoomBanner(
    styleIndex: Int,
    imageUrl: String,
    overlayColor: Color,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bannerImageLoader = androidx.compose.runtime.remember(context) {
        coil.ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(coil.decode.ImageDecoderDecoder.Factory())
                } else {
                    add(coil.decode.GifDecoder.Factory())
                }
                add(coil.decode.SvgDecoder.Factory())
            }
            .build()
    }

    val formattedImageUrl = androidx.compose.runtime.remember(imageUrl) {
        if (imageUrl.contains("media.giphy.com/media/")) {
            imageUrl.replace("media.giphy.com/media/", "i.giphy.com/media/")
        } else {
            imageUrl
        }
    }

    if (imageUrl.startsWith("gradient:")) {
        val colorsString = imageUrl.removePrefix("gradient:")
        val colorHexes = colorsString.split(",")
        val colors = colorHexes.mapNotNull { hex ->
            try {
                val cleanHex = hex.trim()
                val finalHex = if (cleanHex.startsWith("#")) cleanHex else "#$cleanHex"
                Color(android.graphics.Color.parseColor(finalHex))
            } catch (e: Exception) {
                null
            }
        }.ifEmpty { listOf(Color(0xFF6366F1), Color(0xFFEC4899)) }

        val infiniteTransition = rememberInfiniteTransition(label = "live_gradient_anim")
        
        // 1. Shimmer/Shine translation
        val shimmerOffset by infiniteTransition.animateFloat(
            initialValue = -1.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(4500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "gradient_shimmer"
        )
        
        // 2. Pulse of the secondary neon layer
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(3500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "gradient_pulse"
        )

        Box(modifier = modifier) {
            // Background Base Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = colors,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(800f * (1f + (styleIndex % 3) * 0.15f), 1000f)
                        )
                    )
            )

            // Live glowing effects depending on Style Index
            when (styleIndex % 5) {
                0 -> {
                    // Style 0: Golden Sunburst Glow
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFD700).copy(alpha = pulseScale * 0.35f), Color.Transparent),
                                    radius = 550f
                                )
                            )
                    )
                }
                1 -> {
                    // Style 1: Cyberpunk Slanted Ribbon Sweep (Glassmorphic Accent)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val w = size.width
                                val h = size.height
                                val currentX = w * shimmerOffset
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0f),
                                            Color.White.copy(alpha = 0.28f),
                                            Color.White.copy(alpha = 0f)
                                        ),
                                        start = androidx.compose.ui.geometry.Offset(currentX - 150f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(currentX + 150f, h)
                                    )
                                )
                            }
                    )
                }
                2 -> {
                    // Style 2: Secondary Nebulae Float
                    val nebulaColor = colors.lastOrNull()?.copy(alpha = pulseScale * 0.45f) ?: Color.White.copy(alpha = 0.2f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(nebulaColor, Color.Transparent),
                                    radius = 700f,
                                    center = androidx.compose.ui.geometry.Offset(750f, 200f)
                                )
                            )
                    )
                }
                3 -> {
                    // Style 3: Sparkling Twinkle Dust
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val w = size.width
                                val h = size.height
                                drawCircle(
                                    color = Color.White.copy(alpha = pulseScale * 0.85f),
                                    radius = 5.5f,
                                    center = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.33f)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = (1f - pulseScale) * 0.8f),
                                    radius = 4.5f,
                                    center = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.48f)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = pulseScale * 0.65f),
                                    radius = 6f,
                                    center = androidx.compose.ui.geometry.Offset(w * 0.52f, h * 0.72f)
                                )
                            }
                    )
                }
                else -> {
                    // Style 4: Minimalist Deep Vignette
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = (1f - pulseScale) * 0.3f)),
                                    radius = 650f
                                )
                            )
                    )
                }
            }

            // Standard overlays to guarantee text legibility & elegant bottom fade
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.95f)),
                            startY = 100f
                        )
                    )
            )
        }
    } else {
        Box(modifier = modifier) {
            val infiniteTransition = rememberInfiniteTransition(label = "banner_transition")

            when (styleIndex % 10) {
                0 -> {
                    // Style 0: CINEMATIC ZOOM & DRIFT
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(8000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "zoom"
                    )
                    AsyncImage(
                        model = formattedImageUrl,
                        imageLoader = bannerImageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().scale(scale)
                    )
                    val radiantPulse by infiniteTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 0.55f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(4000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFD700).copy(alpha = radiantPulse * 0.4f), Color.Transparent),
                                    radius = 400f
                                )
                            )
                    )
                }
                1 -> {
                    // Style 1: AMBIENT GOLDEN GLOW PULSE
                    val pulseOpacity by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.55f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(3500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow"
                    )
                    AsyncImage(
                        model = formattedImageUrl,
                        imageLoader = bannerImageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFD97706).copy(alpha = pulseOpacity))
                    )
                }
                2 -> {
                    // Style 2: SLIDING GLASSMORPHIC PREMIUM SHIMMER
                    val shimmerOffsetX by infiniteTransition.animateFloat(
                        initialValue = -1.2f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(5000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "shimmer"
                    )
                    AsyncImage(
                        model = formattedImageUrl,
                        imageLoader = bannerImageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val width = size.width
                                val height = size.height
                                if (width > 0f) {
                                    val currentX = width * shimmerOffsetX
                                    val brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0f),
                                            Color.White.copy(alpha = 0.28f),
                                            Color.White.copy(alpha = 0f)
                                        ),
                                        start = androidx.compose.ui.geometry.Offset(currentX - 100f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(currentX + 100f, height)
                                    )
                                    drawRect(brush)
                                }
                            }
                    )
                }
                3 -> {
                    // Style 3: COSMIC NEBULA DRIFT
                    val nebulaColor by infiniteTransition.animateColor(
                        initialValue = Color(0xFF6D28D9), 
                        targetValue = Color(0xFFBFDBFE), 
                        animationSpec = infiniteRepeatable(
                            animation = tween(6000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "nebula"
                    )
                    AsyncImage(
                        model = formattedImageUrl,
                        imageLoader = bannerImageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(nebulaColor.copy(alpha = 0.25f))
                    )
                }
                4 -> {
                    // Style 4: HALO ROTATION
                    val rotationAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(15000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotate"
                    )
                    AsyncImage(
                        model = formattedImageUrl,
                        imageLoader = bannerImageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val cp = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                                this.rotate(rotationAngle) {
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                Color(0xFFFFD700).copy(alpha = 0.35f),
                                                Color.Transparent,
                                                Color(0xFFFFD700).copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        ),
                                        radius = size.width * 0.4f,
                                        center = cp
                                    )
                                }
                            }
                    )
                }
                5 -> {
                    // Style 5: BREATHING LUXURY VIGNETTE
                    val vignetteBreathe by infiniteTransition.animateFloat(
                        initialValue = 0.45f,
                        targetValue = 0.75f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(4000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "vignette"
                    )
                    AsyncImage(
                        model = formattedImageUrl,
                        imageLoader = bannerImageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = vignetteBreathe * 0.6f)),
                                    radius = 600f
                                )
                            )
                    )
                }
                6 -> {
                    // Style 6: AURORA GRADIENT SWEEP
                    val offsetTranslation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(10000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "aurora"
                    )
                    AsyncImage(
                        model = formattedImageUrl,
                        imageLoader = bannerImageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val w = size.width
                                val h = size.height
                                val startOffset = w * offsetTranslation
                                val brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF34D399).copy(alpha = 0.18f),
                                        Color(0xFF60A5FA).copy(alpha = 0.18f),
                                        Color(0xFF34D399).copy(alpha = 0.18f)
                                    ),
                                    start = androidx.compose.ui.geometry.Offset(startOffset, 0f),
                                    end = androidx.compose.ui.geometry.Offset(startOffset + w, h)
                                )
                                drawRect(brush)
                            }
                    )
                }
                7 -> {
                    // Style 7: GOLDEN DIAGONAL RIBBONS SLIDE
                    val ribbonTranslation by infiniteTransition.animateFloat(
                        initialValue = -250f,
                        targetValue = 250f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(7000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "ribbons"
                    )
                    AsyncImage(
                        model = formattedImageUrl,
                        imageLoader = bannerImageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRect(
                                    color = Color(0xFFFFD700).copy(alpha = 0.08f),
                                    topLeft = androidx.compose.ui.geometry.Offset(ribbonTranslation, 0f),
                                    size = androidx.compose.ui.geometry.Size(120f, size.height)
                                )
                                drawRect(
                                    color = Color(0xFFFFD700).copy(alpha = 0.05f),
                                    topLeft = androidx.compose.ui.geometry.Offset(ribbonTranslation + 180f, 0f),
                                    size = androidx.compose.ui.geometry.Size(60f, size.height)
                                )
                            }
                    )
                }
                8 -> {
                    // Style 8: CLASSIC SPARKLING STARS
                    val sparkleBreathe by infiniteTransition.animateFloat(
                        initialValue = 0.1f,
                        targetValue = 0.9f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "sparkle"
                    )
                    AsyncImage(
                        model = formattedImageUrl,
                        imageLoader = bannerImageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val w = size.width
                                val h = size.height
                                drawCircle(
                                    color = Color.White.copy(alpha = sparkleBreathe * 0.7f),
                                    radius = 6f,
                                    center = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.3f)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = (1f - sparkleBreathe) * 0.7f),
                                    radius = 4f,
                                    center = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.45f)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = sparkleBreathe * 0.6f),
                                    radius = 5f,
                                    center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.75f)
                                )
                            }
                    )
                }
                else -> {
                    // Style 9: SMOOTH DRIFT VELVET BLOBS
                    val blobOffset by infiniteTransition.animateFloat(
                        initialValue = -25f,
                        targetValue = 25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(4000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "blob"
                    )
                    AsyncImage(
                        model = formattedImageUrl,
                        imageLoader = bannerImageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                drawCircle(
                                    color = Color(0xFFFCD34D).copy(alpha = 0.15f),
                                    radius = size.width * 0.35f,
                                    center = androidx.compose.ui.geometry.Offset(cx + blobOffset, cy - blobOffset)
                                )
                                drawCircle(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.1f),
                                    radius = size.width * 0.25f,
                                    center = androidx.compose.ui.geometry.Offset(cx - blobOffset, cy + blobOffset)
                                )
                            }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.95f)),
                            startY = 100f
                        )
                    )
            )
        }
    }
}
