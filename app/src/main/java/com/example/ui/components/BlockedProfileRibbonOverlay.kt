package com.example.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

private data class RibbonSpec(
    val text: String,
    val angleDeg: Float,
    val yFraction: Float,
    val heightDp: Float,
    val colors: List<Color>,
    val borderColor: Color,
    val textColor: Color,
    val scrollLeft: Boolean,
    val durationMs: Int
)

/**
 * Full-screen kinetic diagonal red-ribbon overlay shown on top of a blurred profile
 * when the viewed user has blocked the current user.
 */
@Composable
fun BlockedProfileRibbonOverlay(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val textMeasurer = rememberTextMeasurer()
    val transition = rememberInfiniteTransition(label = "blockedRibbons")

    val phaseFast by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6400, easing = LinearEasing), RepeatMode.Restart),
        label = "phaseFast"
    )
    val phaseMedium by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7800, easing = LinearEasing), RepeatMode.Restart),
        label = "phaseMedium"
    )
    val phaseSlow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9200, easing = LinearEasing), RepeatMode.Restart),
        label = "phaseSlow"
    )

    val ribbons = remember {
        listOf(
            RibbonSpec(
                text = "USER BLOCKED YOU  •  ACCESS RESTRICTED  •  ",
                angleDeg = -17f,
                yFraction = 0.19f,
                heightDp = 42f,
                colors = listOf(Color(0xFF991B1B), Color(0xFFDC2626), Color(0xFFB91C1C)),
                borderColor = Color(0xFFFCA5A5).copy(alpha = 0.55f),
                textColor = Color.White,
                scrollLeft = true,
                durationMs = 7800
            ),
            RibbonSpec(
                text = "YOU HAVE BEEN BLOCKED  •  PROFILE HIDDEN  •  ",
                angleDeg = 13f,
                yFraction = 0.34f,
                heightDp = 44f,
                colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFF991B1B)),
                borderColor = Color(0xFFFEE2E2).copy(alpha = 0.65f),
                textColor = Color.White,
                scrollLeft = false,
                durationMs = 9200
            ),
            RibbonSpec(
                text = "USER BLOCKED YOU  •  USER BLOCKED YOU  •  ",
                angleDeg = -8f,
                yFraction = 0.51f,
                heightDp = 50f,
                colors = listOf(Color(0xFFB91C1C), Color(0xFFEF4444), Color(0xFFB91C1C)),
                borderColor = Color(0xFFFFFFFF).copy(alpha = 0.60f),
                textColor = Color.White,
                scrollLeft = true,
                durationMs = 6400
            ),
            RibbonSpec(
                text = "BLOCKED BY USER  •  NO ACCESS  •  ",
                angleDeg = 18f,
                yFraction = 0.69f,
                heightDp = 42f,
                colors = listOf(Color(0xFFDC2626), Color(0xFFE11D48), Color(0xFF9F1239)),
                borderColor = Color(0xFFFCA5A5).copy(alpha = 0.55f),
                textColor = Color.White,
                scrollLeft = false,
                durationMs = 7800
            ),
            RibbonSpec(
                text = "USER BLOCKED YOU  •  INTERACTION DISABLED  •  ",
                angleDeg = -13f,
                yFraction = 0.85f,
                heightDp = 40f,
                colors = listOf(Color(0xFF7F1D1D), Color(0xFFB91C1C), Color(0xFF991B1B)),
                borderColor = Color(0xFFFCA5A5).copy(alpha = 0.45f),
                textColor = Color.White,
                scrollLeft = true,
                durationMs = 9200
            )
        )
    }

    val textStyle = remember {
        TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.8.sp
        )
    }

    val measuredUnits = remember(ribbons, textStyle) {
        ribbons.map { spec ->
            textMeasurer.measure(
                text = spec.text,
                style = textStyle.copy(color = spec.textColor)
            )
        }
    }

    // Consume all background touches while keeping Back interactive
    val scrimAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.48f else 0.84f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B0F19).copy(alpha = scrimAlpha),
                        Color(0xFF1F0A0F).copy(alpha = (scrimAlpha + 0.08f).coerceAtMost(0.92f)),
                        Color(0xFF0B0F19).copy(alpha = scrimAlpha)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Block interaction with blurred profile underneath */ }
    ) {
        // Animated crisscrossing red ribbons canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val extraSpan = maxOf(w, h) * 1.9f

            ribbons.forEachIndexed { idx, spec ->
                val unit = measuredUnits[idx]
                val unitW = unit.size.width.toFloat().coerceAtLeast(1f)
                val unitH = unit.size.height.toFloat()
                val ribbonH = spec.heightDp.dp.toPx()
                val centerY = h * spec.yFraction
                val centerX = w * 0.5f

                val rawPhase = when (spec.durationMs) {
                    6400 -> phaseFast
                    7800 -> phaseMedium
                    else -> phaseSlow
                }
                val shiftPx = if (spec.scrollLeft) {
                    -rawPhase * unitW
                } else {
                    -(1f - rawPhase) * unitW
                }

                withTransform({
                    rotate(degrees = spec.angleDeg, pivot = Offset(centerX, centerY))
                }) {
                    val leftEdge = centerX - extraSpan / 2f
                    val topEdge = centerY - ribbonH / 2f

                    // Drop shadow under each ribbon for layered depth
                    drawRect(
                        color = Color.Black.copy(alpha = 0.34f),
                        topLeft = Offset(leftEdge, topEdge + 4.dp.toPx()),
                        size = Size(extraSpan, ribbonH)
                    )

                    // Gradient ribbon strip
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = spec.colors,
                            startX = leftEdge,
                            endX = leftEdge + extraSpan
                        ),
                        topLeft = Offset(leftEdge, topEdge),
                        size = Size(extraSpan, ribbonH)
                    )

                    // Top & bottom crisp highlight edge lines
                    val lineStroke = 1.4.dp.toPx()
                    drawLine(
                        color = spec.borderColor,
                        start = Offset(leftEdge, topEdge + 2.dp.toPx()),
                        end = Offset(leftEdge + extraSpan, topEdge + 2.dp.toPx()),
                        strokeWidth = lineStroke
                    )
                    drawLine(
                        color = spec.borderColor,
                        start = Offset(leftEdge, topEdge + ribbonH - 2.dp.toPx()),
                        end = Offset(leftEdge + extraSpan, topEdge + ribbonH - 2.dp.toPx()),
                        strokeWidth = lineStroke
                    )

                    // Seamless scrolling bold uppercase marquee text
                    val repeatCount = ceil(extraSpan / unitW).toInt() + 2
                    val textTop = centerY - unitH / 2f
                    for (rep in 0 until repeatCount) {
                        val drawX = leftEdge + shiftPx + rep * unitW
                        drawText(
                            textLayoutResult = unit,
                            topLeft = Offset(drawX, textTop)
                        )
                    }
                }
            }
        }

        // Center Frosted Notice Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141824).copy(alpha = 0.90f),
            border = BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.60f)),
            shadowElevation = 18.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .widthIn(max = 330.dp)
                .testTag("blocked_profile_overlay_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFEF4444).copy(alpha = 0.30f),
                                    Color(0xFF7F1D1D).copy(alpha = 0.25f)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    SoftBlockIcon(size = 24.dp, color = Color(0xFFFCA5A5))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "User Blocked You",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "You cannot view this profile or interact with this user.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(100.dp),
                    color = Color(0xFFDC2626),
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                        .testTag("blocked_profile_go_back_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Go Back",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Top-left floating Back button for quick exit
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .size(48.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF141824).copy(alpha = 0.90f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                .clickable { onBack() }
                .semantics { contentDescription = "Back" }
                .testTag("blocked_profile_top_back_button"),
            contentAlignment = Alignment.Center
        ) {
            SoftBackIcon(size = 18.dp, color = Color.White)
        }
    }
}
