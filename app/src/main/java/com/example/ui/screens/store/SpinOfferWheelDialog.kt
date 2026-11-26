package com.example.ui.screens.store

import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.example.R
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalFunkyColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class SpinOfferSlice(
    val badge: String,
    val iconRes: Int,
    val accentColor: Color
)

/**
 * Special Offers Coin Discount Spin Wheel UI (Mockup / Currently Closed state).
 * Mirrors the layout of Reference 2 while using FunkyTalk's Mango & Espresso/Cream theme tokens.
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
            SpinOfferSlice("50% OFF", R.drawable.ic_coin, funkyColors.mango),
            SpinOfferSlice("15% OFF", R.drawable.ic_gift, Color(0xFF22B573)),
            SpinOfferSlice("+100", R.drawable.ic_sparkle, Color(0xFFF59E0B)),
            SpinOfferSlice("30% OFF", R.drawable.ic_crown_filled, Color(0xFFFB7185)),
            SpinOfferSlice("2x Coins", R.drawable.ic_coin, funkyColors.mango),
            SpinOfferSlice("20% OFF", R.drawable.ic_gift, Color(0xFF38BDF8)),
            SpinOfferSlice("+50", R.drawable.ic_flame, Color(0xFFF97316)),
            SpinOfferSlice("40% OFF", R.drawable.ic_star_filled, Color(0xFFA78BFA))
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { win ->
                win.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                win.setBackgroundDrawableResource(android.R.color.transparent)
                @Suppress("DEPRECATION")
                win.statusBarColor = android.graphics.Color.TRANSPARENT
                @Suppress("DEPRECATION")
                win.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.setDecorFitsSystemWindows(win, false)
                WindowCompat.getInsetsController(win, dialogView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
                @Suppress("DEPRECATION")
                win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }

        BackHandler(onBack = onDismiss)

        val backdropColor = if (isDark) funkyColors.bg else Color(0xFFFFF8E7)
        val sliceAltA = if (isDark) Color(0xFF282017) else Color(0xFFFFFFFF)
        val sliceAltB = if (isDark) Color(0xFF32271B) else Color(0xFFFFF0C8)
        val hubBg = if (isDark) Color(0xFF2E2318) else Color(0xFFFFFFFF)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Bar: X Close Button + Accent Subtitle
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = funkyColors.surface.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, funkyColors.line),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterStart)
                            .testTag("spin_close_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Close",
                                tint = funkyColors.ink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "Special Offers · Coin Discounts",
                        color = funkyColors.mango,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Special Offers Spin is\nCurrently Closed",
                    color = funkyColors.ink,
                    fontSize = 26.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = funkyColors.soft,
                    border = BorderStroke(1.dp, funkyColors.mango.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_lock),
                            contentDescription = null,
                            tint = funkyColors.mango,
                            modifier = Modifier.size(13.dp)
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

                Spacer(modifier = Modifier.height(14.dp))

                // Glowing Downward Triangle Pointer (above wheel)
                Canvas(
                    modifier = Modifier
                        .size(width = 36.dp, height = 28.dp)
                ) {
                    val path = Path().apply {
                        moveTo(size.width * 0.14f, size.height * 0.14f)
                        lineTo(size.width * 0.86f, size.height * 0.14f)
                        lineTo(size.width * 0.50f, size.height * 0.88f)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = funkyColors.mango.copy(alpha = 0.30f),
                        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = funkyColors.mango,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 8-Slice Radial Spin Wheel (matching Reference 2)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val wheelSize = minOf(maxWidth, maxHeight, 330.dp)
                    Box(
                        modifier = Modifier.size(wheelSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.minDimension / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val sweep = 360f / slices.size
                            val sliceGapDeg = 2.2f

                            // Outer soft rim glow
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        funkyColors.mango.copy(alpha = 0.22f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = r * 1.04f
                                ),
                                radius = r * 1.04f,
                                center = center
                            )

                            slices.forEachIndexed { idx, slice ->
                                val startDeg = -90f - sweep / 2f + idx * sweep + sliceGapDeg / 2f
                                val baseBg = if (idx % 2 == 0) sliceAltA else sliceAltB
                                drawArc(
                                    color = baseBg,
                                    startAngle = startDeg,
                                    sweepAngle = sweep - sliceGapDeg,
                                    useCenter = true,
                                    topLeft = Offset(center.x - r, center.y - r),
                                    size = Size(r * 2f, r * 2f)
                                )
                                drawArc(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            slice.accentColor.copy(alpha = if (isDark) 0.24f else 0.18f),
                                            Color.Transparent
                                        ),
                                        center = center,
                                        radius = r
                                    ),
                                    startAngle = startDeg,
                                    sweepAngle = sweep - sliceGapDeg,
                                    useCenter = true,
                                    topLeft = Offset(center.x - r, center.y - r),
                                    size = Size(r * 2f, r * 2f)
                                )
                            }

                            // Crisp wheel outer ring
                            drawCircle(
                                color = funkyColors.line,
                                radius = r - 1.dp.toPx(),
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Slice Artwork + Discount Pills positioned radially inside each wedge
                        slices.forEachIndexed { idx, slice ->
                            val centerAngleDeg = -90f + idx * (360f / slices.size)
                            val rad = centerAngleDeg * PI / 180.0
                            val iconR = wheelSize * 0.36f
                            val pillR = wheelSize * 0.245f

                            val iconOffX = (cos(rad) * iconR.value)
                            val iconOffY = (sin(rad) * iconR.value)
                            val pillOffX = (cos(rad) * pillR.value)
                            val pillOffY = (sin(rad) * pillR.value)

                            Box(
                                modifier = Modifier
                                    .offset(x = iconOffX.dp, y = iconOffY.dp)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(slice.accentColor.copy(alpha = 0.16f))
                                    .alpha(0.88f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = slice.iconRes),
                                    contentDescription = slice.badge,
                                    tint = slice.accentColor,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = if (isDark) Color(0xFFFFF8E7) else Color(0xFF2B2118),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .offset(x = pillOffX.dp, y = pillOffY.dp)
                                    .rotate(centerAngleDeg + 90f)
                            ) {
                                Text(
                                    text = slice.badge,
                                    color = if (isDark) Color(0xFF2B2118) else Color(0xFFFFF8E7),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Center Glowing Hub showing CLOSED status (matches Reference 2's glowing center disc)
                        Surface(
                            shape = CircleShape,
                            color = hubBg,
                            border = BorderStroke(3.dp, funkyColors.mango.copy(alpha = 0.80f)),
                            shadowElevation = 10.dp,
                            modifier = Modifier.size(wheelSize * 0.36f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_lock),
                                    contentDescription = "Closed",
                                    tint = funkyColors.mango,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "CLOSED",
                                    color = funkyColors.ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = "Soon",
                                    color = funkyColors.mute,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // "Skip animation" toggle row (matches Reference 2)
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
                            uncheckedTrackColor = funkyColors.line
                        ),
                        modifier = Modifier.testTag("spin_skip_animation_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Special coin purchase offers are currently closed.\nDiscounts & bonus coin spins will unlock soon!",
                    color = funkyColors.mute,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Full-width Pill CTA Button (matches Reference 2)
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
                        .height(58.dp)
                        .shadow(8.dp, RoundedCornerShape(99.dp), spotColor = funkyColors.mango.copy(alpha = 0.4f))
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFC94D), Color(0xFFFF9F1C))
                            )
                        )
                        .testTag("spin_closed_cta_button")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_lock),
                        contentDescription = null,
                        tint = funkyColors.onMango,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Special Offers Closed",
                        color = funkyColors.onMango,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_coin),
                        contentDescription = null,
                        tint = funkyColors.onMango,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}
