package com.example.ui.screens.store

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.components.SoftCloseIcon
import com.example.ui.components.SoftFlameIcon
import com.example.ui.components.SoftLockIcon
import com.example.ui.components.SoftWheelCoinArtwork
import com.example.ui.components.SoftWheelCrownArtwork
import com.example.ui.components.SoftWheelGiftArtwork
import com.example.ui.components.SoftWheelSparkleArtwork
import com.example.ui.components.SoftWheelStarArtwork
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalFunkyColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class SpinSliceArtwork {
    COIN,
    GIFT,
    SPARKLE,
    CROWN,
    FLAME,
    STAR
}

private data class SpinOfferSlice(
    val badge: String,
    val artwork: SpinSliceArtwork,
    val accentColor: Color
)

/**
 * Special Offers Coin Discount Spin Wheel UI (Mockup / Currently Closed state).
 * Edge-to-edge full-screen Compose overlay (never clipped by Android Dialog window frames),
 * matching the reference layout and dimensional wedge lighting while harmonized with FunkyTalk's
 * Mango + Espresso/Cream design tokens and bespoke Canvas soft icons.
 */
@Composable
fun SpinOfferWheelDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val isDark = LocalAppDarkTheme.current
    var skipAnimation by remember { mutableStateOf(false) }

    val slices = remember(funkyColors) {
        listOf(
            SpinOfferSlice("50% OFF", SpinSliceArtwork.COIN, funkyColors.mango),
            SpinOfferSlice("15% OFF", SpinSliceArtwork.GIFT, Color(0xFF22C55E)),
            SpinOfferSlice("+100", SpinSliceArtwork.SPARKLE, Color(0xFFFBBF24)),
            SpinOfferSlice("30% OFF", SpinSliceArtwork.CROWN, Color(0xFFFB7185)),
            SpinOfferSlice("2x Coins", SpinSliceArtwork.COIN, funkyColors.mango),
            SpinOfferSlice("20% OFF", SpinSliceArtwork.GIFT, Color(0xFF38BDF8)),
            SpinOfferSlice("+50", SpinSliceArtwork.FLAME, Color(0xFFF97316)),
            SpinOfferSlice("40% OFF", SpinSliceArtwork.STAR, Color(0xFFA78BFA))
        )
    }

    BackHandler(onBack = onDismiss)

    val backdropBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF14100B),
                Color(0xFF19130C),
                Color(0xFF120E09)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFBF0),
                Color(0xFFFFF4D9),
                Color(0xFFFFF9EA)
            )
        )
    }

    // Deep obsidian-espresso wedges keep strong dimensional contrast with glowing beams in both themes
    val sliceAltA = if (isDark) Color(0xFF1D1712) else Color(0xFF251B12)
    val sliceAltB = if (isDark) Color(0xFF261E17) else Color(0xFF302317)
    val subtitleColor = if (isDark) funkyColors.mango else Color(0xFFD97706)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(120f)
            .background(backdropBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume backdrop taps */ }
    ) {
        // Soft ambient top/center spotlight aura
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        funkyColors.mango.copy(alpha = if (isDark) 0.14f else 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.46f),
                    radius = size.minDimension * 0.65f
                ),
                radius = size.minDimension * 0.65f,
                center = Offset(size.width * 0.5f, size.height * 0.46f)
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val availableHeight = maxHeight
            val availableWidth = maxWidth
            val wheelSize = minOf(
                availableWidth * 0.84f,
                (availableHeight - 365.dp).coerceAtLeast(210.dp),
                326.dp
            )
            val isCompactHeight = availableHeight < 680.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = if (isCompactHeight) 8.dp else 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Bar: Minimal Soft Close Button + Subtitle
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else funkyColors.surface.copy(alpha = 0.92f),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.14f) else funkyColors.line
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterStart)
                            .testTag("spin_close_button")
                            .semantics { contentDescription = "Close Special Offers Spin" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            SoftCloseIcon(
                                size = 18.dp,
                                color = funkyColors.ink
                            )
                        }
                    }

                    Text(
                        text = "Special Offers · Coin Discounts",
                        color = subtitleColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 52.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 12.dp))

                Text(
                    text = "Special Offers Spin is\nCurrently Closed",
                    color = funkyColors.ink,
                    fontSize = if (isCompactHeight) 22.sp else 25.sp,
                    lineHeight = if (isCompactHeight) 27.sp else 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = if (isDark) funkyColors.mango.copy(alpha = 0.14f) else funkyColors.soft,
                    border = BorderStroke(1.dp, funkyColors.mango.copy(alpha = 0.50f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SoftLockIcon(
                            size = 15.dp,
                            gradientColors = listOf(Color(0xFFFFD166), funkyColors.mango),
                            shackleColor = funkyColors.ink.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "CLOSED · OPENING SOON",
                            color = funkyColors.ink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 14.dp))

                // Glowing Downward Neon-Mango Pointer + 8-Slice Radial Wheel
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Radial Spin Wheel positioned cleanly under the overlapping top pointer
                    Box(
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .size(wheelSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.minDimension / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val sweep = 360f / slices.size
                            val sliceGapDeg = 3.0f

                            // Outer glowing bezel aura
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        funkyColors.mango.copy(alpha = if (isDark) 0.30f else 0.34f),
                                        funkyColors.mango.copy(alpha = 0.08f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = r * 1.12f
                                ),
                                radius = r * 1.12f,
                                center = center
                            )

                            // Dark wheel base disc so gaps between wedges are crisp deep espresso/obsidian
                            drawCircle(
                                color = Color(0xFF120D08),
                                radius = r,
                                center = center
                            )

                            slices.forEachIndexed { idx, slice ->
                                val startDeg = -90f - sweep / 2f + idx * sweep + sliceGapDeg / 2f
                                val arcSweep = sweep - sliceGapDeg
                                val baseBg = if (idx % 2 == 0) sliceAltA else sliceAltB

                                drawArc(
                                    color = baseBg,
                                    startAngle = startDeg,
                                    sweepAngle = arcSweep,
                                    useCenter = true,
                                    topLeft = Offset(center.x - r, center.y - r),
                                    size = Size(r * 2f, r * 2f)
                                )

                                // Luminous radial glow beam emanating outward from center hub
                                drawArc(
                                    brush = Brush.radialGradient(
                                        0.0f to slice.accentColor.copy(alpha = 0.56f),
                                        0.52f to slice.accentColor.copy(alpha = 0.30f),
                                        0.85f to slice.accentColor.copy(alpha = 0.07f),
                                        1.0f to Color.Transparent,
                                        center = center,
                                        radius = r
                                    ),
                                    startAngle = startDeg,
                                    sweepAngle = arcSweep,
                                    useCenter = true,
                                    topLeft = Offset(center.x - r, center.y - r),
                                    size = Size(r * 2f, r * 2f)
                                )

                                // Crisp inner rim highlight per slice
                                drawArc(
                                    color = slice.accentColor.copy(alpha = 0.28f),
                                    startAngle = startDeg + 1f,
                                    sweepAngle = arcSweep - 2f,
                                    useCenter = false,
                                    topLeft = Offset(center.x - r + 2.dp.toPx(), center.y - r + 2.dp.toPx()),
                                    size = Size((r - 2.dp.toPx()) * 2f, (r - 2.dp.toPx()) * 2f),
                                    style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // Outer golden-espresso wheel bezel ring
                            drawCircle(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        funkyColors.mango.copy(alpha = 0.72f),
                                        Color(0xFF4A3622),
                                        funkyColors.mango.copy(alpha = 0.65f)
                                    )
                                ),
                                radius = r - 1.dp.toPx(),
                                center = center,
                                style = Stroke(width = 2.4.dp.toPx())
                            )
                        }

                        // Slice Glossy Soft Artwork + Upright-Normalized Pill Badges
                        val iconSize = (wheelSize.value * 0.105f).coerceIn(24f, 34f).dp
                        slices.forEachIndexed { idx, slice ->
                            val centerAngleDeg = -90f + idx * (360f / slices.size)
                            val rad = centerAngleDeg * PI / 180.0
                            val iconR = wheelSize * 0.365f
                            val pillR = wheelSize * 0.242f

                            val iconOffX = (cos(rad) * iconR.value)
                            val iconOffY = (sin(rad) * iconR.value)
                            val pillOffX = (cos(rad) * pillR.value)
                            val pillOffY = (sin(rad) * pillR.value)

                            // Normalize pill rotation so bottom-half badges NEVER appear upside down
                            val rawRot = ((centerAngleDeg + 90f) % 360f + 360f) % 360f
                            val readablePillRotation = if (rawRot in 95f..265f) rawRot - 180f else rawRot

                            Box(
                                modifier = Modifier
                                    .offset(x = iconOffX.dp, y = iconOffY.dp)
                                    .size(iconSize),
                                contentAlignment = Alignment.Center
                            ) {
                                when (slice.artwork) {
                                    SpinSliceArtwork.COIN -> SoftWheelCoinArtwork(
                                        size = iconSize,
                                        goldColor = funkyColors.mango,
                                        highlightColor = Color(0xFFFFF3B0)
                                    )
                                    SpinSliceArtwork.GIFT -> SoftWheelGiftArtwork(
                                        size = iconSize,
                                        accentColor = slice.accentColor
                                    )
                                    SpinSliceArtwork.SPARKLE -> SoftWheelSparkleArtwork(
                                        size = iconSize,
                                        accentColor = slice.accentColor
                                    )
                                    SpinSliceArtwork.CROWN -> SoftWheelCrownArtwork(
                                        size = iconSize,
                                        accentColor = slice.accentColor
                                    )
                                    SpinSliceArtwork.FLAME -> SoftFlameIcon(
                                        size = iconSize,
                                        color = slice.accentColor
                                    )
                                    SpinSliceArtwork.STAR -> SoftWheelStarArtwork(
                                        size = iconSize,
                                        accentColor = slice.accentColor
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = Color(0xFFFFFBF2),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .offset(x = pillOffX.dp, y = pillOffY.dp)
                                    .rotate(readablePillRotation)
                            ) {
                                Text(
                                    text = slice.badge,
                                    color = Color(0xFF1F160C),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Luminous 3D Pearlescent Central Hub Disc (matches reference center sphere)
                        val hubSize = wheelSize * 0.355f
                        Box(
                            modifier = Modifier
                                .size(hubSize)
                                .shadow(
                                    elevation = 14.dp,
                                    shape = CircleShape,
                                    spotColor = funkyColors.mango.copy(alpha = 0.55f)
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0xFFFFF6E3),
                                            Color(0xFFF3DEC0)
                                        ),
                                        center = Offset(0.45f, 0.35f)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.95f),
                                            funkyColors.mango.copy(alpha = 0.85f)
                                        )
                                    ),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                SoftLockIcon(
                                    size = 20.dp,
                                    gradientColors = listOf(Color(0xFFFFC94D), Color(0xFFEA580C)),
                                    shackleColor = Color(0xFF6B4F2A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "CLOSED",
                                    color = Color(0xFF1F160C),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = "Soon",
                                    color = Color(0xFF6B5A45),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Glowing Neon-Mango Downward Triangle Pointer overlapping top bezel
                    Canvas(
                        modifier = Modifier
                            .size(width = 40.dp, height = 32.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width * 0.14f, size.height * 0.12f)
                            lineTo(size.width * 0.86f, size.height * 0.12f)
                            lineTo(size.width * 0.50f, size.height * 0.90f)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = funkyColors.mango.copy(alpha = 0.35f),
                            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = path,
                            color = Color(0xFF1A120B)
                        )
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFE598), funkyColors.mango)
                            ),
                            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 16.dp))

                // "Skip animation" toggle row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Skip animation",
                        color = funkyColors.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = skipAnimation,
                        onCheckedChange = { skipAnimation = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = funkyColors.onMango,
                            checkedTrackColor = funkyColors.mango,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = if (isDark) Color(0xFF3A2F24) else Color(0xFFE5DAC4),
                            uncheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("spin_skip_animation_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Special coin purchase offers are currently closed.\nDiscounts & bonus coin spins will unlock soon!",
                    color = funkyColors.mute,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.height(14.dp))

                // Full-width Pill CTA Button with guaranteed bottom clearance
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Special Offers Spin is currently closed. Opening soon!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = funkyColors.onMango
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(10.dp, RoundedCornerShape(99.dp), spotColor = funkyColors.mango.copy(alpha = 0.45f))
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFCE56), Color(0xFFFF9F1C))
                            )
                        )
                        .testTag("spin_closed_cta_button")
                ) {
                    SoftLockIcon(
                        size = 18.dp,
                        gradientColors = listOf(Color(0xFF452E12), Color(0xFF2B1C09)),
                        shackleColor = Color(0xFF452E12)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Special Offers Closed",
                        color = funkyColors.onMango,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SoftWheelCoinArtwork(
                        size = 20.dp,
                        goldColor = Color(0xFFFFD166),
                        highlightColor = Color(0xFFFFFAE0)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
