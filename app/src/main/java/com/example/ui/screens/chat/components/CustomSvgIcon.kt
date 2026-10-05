package com.example.ui.screens.chat.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

sealed class ChatIconAsset {
    object ArrowBack : ChatIconAsset()
    object MoreVertical : ChatIconAsset()
    object ChevronRight : ChatIconAsset()
    object OnlineStatus : ChatIconAsset()
    object LanguagePartner : ChatIconAsset()
    object Translate : ChatIconAsset()
    object Reply : ChatIconAsset()
    object Copy : ChatIconAsset()
    object More : ChatIconAsset()
    object Add : ChatIconAsset()
    object Send : ChatIconAsset()
    object Microphone : ChatIconAsset()
    object Play : ChatIconAsset()
    object Pause : ChatIconAsset()
    object Download : ChatIconAsset()
    object Reaction : ChatIconAsset()
    object Image : ChatIconAsset()
    object VoiceWave : ChatIconAsset()
    object DoubleCheck : ChatIconAsset()
    object Book : ChatIconAsset()
    object Lightbulb : ChatIconAsset()
    object Smiley : ChatIconAsset()
    object Attachment : ChatIconAsset()
    object Close : ChatIconAsset()
    object Sparkles : ChatIconAsset()
    object Clock : ChatIconAsset()
    object Search : ChatIconAsset()
    object Trash : ChatIconAsset()
    object Share : ChatIconAsset()
    object Forward : ChatIconAsset()
    object Check : ChatIconAsset()
    object Lock : ChatIconAsset()
    object MicOff : ChatIconAsset()
}

@Composable
fun AppSvgIcon(
    icon: ChatIconAsset,
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.Unspecified
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val densityFactor = density

        when (icon) {
            ChatIconAsset.ArrowBack -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 2f * densityFactor
                drawLine(c, Offset(w * 0.79f, h * 0.5f), Offset(w * 0.21f, h * 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.21f)
                    lineTo(w * 0.21f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.79f)
                }
                drawPath(path, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ChatIconAsset.MoreVertical -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val r = w * 0.083f
                drawCircle(c, radius = r, center = Offset(w * 0.5f, h * 0.21f))
                drawCircle(c, radius = r, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(c, radius = r, center = Offset(w * 0.5f, h * 0.79f))
            }
            ChatIconAsset.ChevronRight -> {
                val c = if (tint == Color.Unspecified) Color(0xFF6B7280) else tint
                val stroke = 2f * densityFactor
                val path = Path().apply {
                    moveTo(w * 0.375f, h * 0.25f)
                    lineTo(w * 0.625f, h * 0.5f)
                    lineTo(w * 0.375f, h * 0.75f)
                }
                drawPath(path, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ChatIconAsset.OnlineStatus -> {
                val c = if (tint == Color.Unspecified) Color(0xFF22C55E) else tint
                drawCircle(c, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f))
            }
            ChatIconAsset.LanguagePartner, ChatIconAsset.Lightbulb -> {
                val c = if (tint == Color.Unspecified) Color(0xFFEAB308) else tint
                val stroke = 1.8f * densityFactor
                val path = Path().apply {
                    moveTo(w * 0.375f, h * 0.75f)
                    lineTo(w * 0.625f, h * 0.75f)
                }
                drawPath(path, c, style = Stroke(stroke, cap = StrokeCap.Round))
                drawLine(c, Offset(w * 0.416f, h * 0.916f), Offset(w * 0.583f, h * 0.916f), strokeWidth = stroke, cap = StrokeCap.Round)
                val bulb = Path().apply {
                    moveTo(w * 0.5f, h * 0.083f)
                    cubicTo(w * 0.28f, h * 0.083f, w * 0.208f, h * 0.26f, w * 0.208f, h * 0.375f)
                    cubicTo(w * 0.208f, h * 0.52f, w * 0.30f, h * 0.60f, w * 0.35f, h * 0.68f)
                    lineTo(w * 0.35f, h * 0.72f)
                    lineTo(w * 0.65f, h * 0.72f)
                    lineTo(w * 0.65f, h * 0.68f)
                    cubicTo(w * 0.70f, h * 0.60f, w * 0.792f, h * 0.52f, w * 0.792f, h * 0.375f)
                    cubicTo(w * 0.792f, h * 0.26f, w * 0.72f, h * 0.083f, w * 0.5f, h * 0.083f)
                    close()
                }
                drawPath(bulb, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ChatIconAsset.Translate -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                drawLine(c, Offset(w * 0.21f, h * 0.33f), Offset(w * 0.46f, h * 0.83f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(c, Offset(w * 0.17f, h * 0.58f), Offset(w * 0.42f, h * 0.58f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(c, Offset(w * 0.08f, h * 0.21f), Offset(w * 0.50f, h * 0.21f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(c, Offset(w * 0.29f, h * 0.08f), Offset(w * 0.29f, h * 0.21f), strokeWidth = stroke, cap = StrokeCap.Round)
                
                val p2 = Path().apply {
                    moveTo(w * 0.50f, h * 0.91f)
                    lineTo(w * 0.71f, h * 0.50f)
                    lineTo(w * 0.91f, h * 0.91f)
                }
                drawPath(p2, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(c, Offset(w * 0.58f, h * 0.75f), Offset(w * 0.83f, h * 0.75f), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            ChatIconAsset.Reply -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                val p = Path().apply {
                    moveTo(w * 0.375f, h * 0.58f)
                    lineTo(w * 0.166f, h * 0.375f)
                    lineTo(w * 0.375f, h * 0.166f)
                }
                drawPath(p, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                val curve = Path().apply {
                    moveTo(w * 0.166f, h * 0.375f)
                    lineTo(w * 0.625f, h * 0.375f)
                    cubicTo(w * 0.833f, h * 0.375f, w * 0.833f, h * 0.583f, w * 0.833f, h * 0.708f)
                }
                drawPath(curve, c, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            ChatIconAsset.Copy -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                val frontRect = Path().apply {
                    moveTo(w * 0.375f, h * 0.375f)
                    lineTo(w * 0.916f, h * 0.375f)
                    lineTo(w * 0.916f, h * 0.916f)
                    lineTo(w * 0.375f, h * 0.916f)
                    close()
                }
                drawPath(frontRect, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                val backPath = Path().apply {
                    moveTo(w * 0.208f, h * 0.625f)
                    lineTo(w * 0.083f, h * 0.625f)
                    lineTo(w * 0.083f, h * 0.083f)
                    lineTo(w * 0.625f, h * 0.083f)
                    lineTo(w * 0.625f, h * 0.208f)
                }
                drawPath(backPath, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ChatIconAsset.More -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val r = w * 0.083f
                drawCircle(c, radius = r, center = Offset(w * 0.21f, h * 0.5f))
                drawCircle(c, radius = r, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(c, radius = r, center = Offset(w * 0.79f, h * 0.5f))
            }
            ChatIconAsset.Add -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 2f * densityFactor
                drawLine(c, Offset(w * 0.5f, h * 0.21f), Offset(w * 0.5f, h * 0.79f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(c, Offset(w * 0.21f, h * 0.5f), Offset(w * 0.79f, h * 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            ChatIconAsset.Send -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.85f)
                    lineTo(w * 0.90f, h * 0.50f)
                    lineTo(w * 0.15f, h * 0.15f)
                    lineTo(w * 0.15f, h * 0.42f)
                    lineTo(w * 0.60f, h * 0.50f)
                    lineTo(w * 0.15f, h * 0.58f)
                    close()
                }
                drawPath(path, c, style = Fill)
            }
            ChatIconAsset.Microphone -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                val micBody = Path().apply {
                    moveTo(w * 0.375f, h * 0.125f)
                    lineTo(w * 0.625f, h * 0.125f)
                    lineTo(w * 0.625f, h * 0.50f)
                    lineTo(w * 0.375f, h * 0.50f)
                    close()
                }
                drawPath(micBody, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                val arch = Path().apply {
                    moveTo(w * 0.208f, h * 0.416f)
                    cubicTo(w * 0.208f, h * 0.708f, w * 0.792f, h * 0.708f, w * 0.792f, h * 0.416f)
                }
                drawPath(arch, c, style = Stroke(stroke, cap = StrokeCap.Round))
                drawLine(c, Offset(w * 0.5f, h * 0.708f), Offset(w * 0.5f, h * 0.916f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(c, Offset(w * 0.333f, h * 0.916f), Offset(w * 0.666f, h * 0.916f), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            ChatIconAsset.Play -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val path = Path().apply {
                    moveTo(w * 0.28f, h * 0.18f)
                    lineTo(w * 0.82f, h * 0.50f)
                    lineTo(w * 0.28f, h * 0.82f)
                    close()
                }
                drawPath(path, c, style = Fill)
            }
            ChatIconAsset.Pause -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                drawRoundRect(c, topLeft = Offset(w * 0.25f, h * 0.18f), size = Size(w * 0.18f, h * 0.64f))
                drawRoundRect(c, topLeft = Offset(w * 0.57f, h * 0.18f), size = Size(w * 0.18f, h * 0.64f))
            }
            ChatIconAsset.Download -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                val tray = Path().apply {
                    moveTo(w * 0.875f, h * 0.625f)
                    lineTo(w * 0.875f, h * 0.875f)
                    lineTo(w * 0.125f, h * 0.875f)
                    lineTo(w * 0.125f, h * 0.625f)
                }
                drawPath(tray, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(c, Offset(w * 0.5f, h * 0.125f), Offset(w * 0.5f, h * 0.625f), strokeWidth = stroke, cap = StrokeCap.Round)
                val arrow = Path().apply {
                    moveTo(w * 0.29f, h * 0.416f)
                    lineTo(w * 0.50f, h * 0.625f)
                    lineTo(w * 0.71f, h * 0.416f)
                }
                drawPath(arrow, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ChatIconAsset.Reaction -> {
                val c = if (tint == Color.Unspecified) Color(0xFFEF4444) else tint
                val heart = Path().apply {
                    moveTo(w * 0.5f, h * 0.85f)
                    cubicTo(w * 0.15f, h * 0.60f, w * 0.05f, h * 0.35f, w * 0.25f, h * 0.18f)
                    cubicTo(w * 0.38f, h * 0.07f, w * 0.5f, h * 0.25f, w * 0.5f, h * 0.25f)
                    cubicTo(w * 0.5f, h * 0.25f, w * 0.62f, h * 0.07f, w * 0.75f, h * 0.18f)
                    cubicTo(w * 0.95f, h * 0.35f, w * 0.85f, h * 0.60f, w * 0.5f, h * 0.85f)
                    close()
                }
                drawPath(heart, c, style = Fill)
            }
            ChatIconAsset.Image -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                drawRoundRect(c, topLeft = Offset(w * 0.125f, h * 0.125f), size = Size(w * 0.75f, h * 0.75f), style = Stroke(stroke))
                drawCircle(c, radius = w * 0.08f, center = Offset(w * 0.35f, h * 0.35f))
                val mountain = Path().apply {
                    moveTo(w * 0.125f, h * 0.75f)
                    lineTo(w * 0.45f, h * 0.45f)
                    lineTo(w * 0.65f, h * 0.62f)
                    lineTo(w * 0.875f, h * 0.40f)
                }
                drawPath(mountain, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ChatIconAsset.VoiceWave -> {
                val c = if (tint == Color.Unspecified) Color(0xFF4B5563) else tint
                val stroke = 2.2f * densityFactor
                val heights = listOf(0.3f, 0.6f, 0.9f, 1.0f, 0.7f, 0.4f)
                val step = w / (heights.size + 1)
                heights.forEachIndexed { index, fraction ->
                    val x = (index + 1) * step
                    val barH = h * fraction * 0.7f
                    drawLine(
                        color = c,
                        start = Offset(x, (h - barH) / 2f),
                        end = Offset(x, (h + barH) / 2f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
            ChatIconAsset.DoubleCheck -> {
                val c = if (tint == Color.Unspecified) Color(0xFF78350F) else tint
                val stroke = 1.8f * densityFactor
                val check1 = Path().apply {
                    moveTo(w * 0.12f, h * 0.50f)
                    lineTo(w * 0.38f, h * 0.75f)
                    lineTo(w * 0.75f, h * 0.25f)
                }
                drawPath(check1, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                val check2 = Path().apply {
                    moveTo(w * 0.52f, h * 0.75f)
                    lineTo(w * 0.90f, h * 0.38f)
                }
                drawPath(check2, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ChatIconAsset.Book -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                val spine = Path().apply {
                    moveTo(w * 0.166f, h * 0.80f)
                    lineTo(w * 0.833f, h * 0.80f)
                }
                drawPath(spine, c, style = Stroke(stroke, cap = StrokeCap.Round))
                val bookCover = Path().apply {
                    moveTo(w * 0.27f, h * 0.083f)
                    lineTo(w * 0.833f, h * 0.083f)
                    lineTo(w * 0.833f, h * 0.916f)
                    lineTo(w * 0.27f, h * 0.916f)
                    cubicTo(w * 0.166f, h * 0.916f, w * 0.166f, h * 0.80f, w * 0.166f, h * 0.80f)
                    lineTo(w * 0.166f, h * 0.20f)
                    cubicTo(w * 0.166f, h * 0.083f, w * 0.27f, h * 0.083f, w * 0.27f, h * 0.083f)
                    close()
                }
                drawPath(bookCover, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ChatIconAsset.Smiley -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                drawCircle(c, radius = w * 0.416f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(stroke))
                drawCircle(c, radius = w * 0.04f, center = Offset(w * 0.375f, h * 0.375f))
                drawCircle(c, radius = w * 0.04f, center = Offset(w * 0.625f, h * 0.375f))
                val smile = Path().apply {
                    moveTo(w * 0.333f, h * 0.583f)
                    cubicTo(w * 0.416f, h * 0.72f, w * 0.583f, h * 0.72f, w * 0.666f, h * 0.583f)
                }
                drawPath(smile, c, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            ChatIconAsset.Attachment -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                val clip = Path().apply {
                    moveTo(w * 0.70f, h * 0.33f)
                    lineTo(w * 0.38f, h * 0.65f)
                    cubicTo(w * 0.28f, h * 0.75f, w * 0.15f, h * 0.75f, w * 0.15f, h * 0.58f)
                    cubicTo(w * 0.15f, h * 0.45f, w * 0.28f, h * 0.30f, w * 0.45f, h * 0.15f)
                    cubicTo(w * 0.62f, h * 0.08f, w * 0.82f, h * 0.18f, w * 0.85f, h * 0.38f)
                    cubicTo(w * 0.88f, h * 0.58f, w * 0.72f, h * 0.82f, w * 0.48f, h * 0.88f)
                    cubicTo(w * 0.30f, h * 0.92f, w * 0.12f, h * 0.80f, w * 0.12f, h * 0.60f)
                }
                drawPath(clip, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ChatIconAsset.Close -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 2.0f * densityFactor
                drawLine(c, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.75f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(c, Offset(w * 0.75f, h * 0.25f), Offset(w * 0.25f, h * 0.75f), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            ChatIconAsset.Sparkles -> {
                val c = if (tint == Color.Unspecified) Color(0xFFEAB308) else tint
                val stroke = 1.8f * densityFactor
                val mainStar = Path().apply {
                    moveTo(w * 0.50f, h * 0.10f)
                    cubicTo(w * 0.50f, h * 0.35f, w * 0.65f, h * 0.50f, w * 0.90f, h * 0.50f)
                    cubicTo(w * 0.65f, h * 0.50f, w * 0.50f, h * 0.65f, w * 0.50f, h * 0.90f)
                    cubicTo(w * 0.50f, h * 0.65f, w * 0.35f, h * 0.50f, w * 0.10f, h * 0.50f)
                    cubicTo(w * 0.35f, h * 0.50f, w * 0.50f, h * 0.35f, w * 0.50f, h * 0.10f)
                    close()
                }
                drawPath(mainStar, c, style = Fill)
            }
            ChatIconAsset.Clock -> {
                val c = if (tint == Color.Unspecified) Color.Gray else tint
                val stroke = 1.8f * densityFactor
                drawCircle(c, radius = w * 0.40f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(stroke))
                drawLine(c, Offset(w * 0.50f, h * 0.28f), Offset(w * 0.50f, h * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(c, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.68f, h * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            ChatIconAsset.Search -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                drawCircle(c, radius = w * 0.30f, center = Offset(w * 0.42f, h * 0.42f), style = Stroke(stroke))
                drawLine(c, Offset(w * 0.62f, h * 0.62f), Offset(w * 0.85f, h * 0.85f), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            ChatIconAsset.Trash -> {
                val c = if (tint == Color.Unspecified) Color(0xFFEF4444) else tint
                val stroke = 1.8f * densityFactor
                drawLine(c, Offset(w * 0.20f, h * 0.28f), Offset(w * 0.80f, h * 0.28f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(c, Offset(w * 0.38f, h * 0.18f), Offset(w * 0.62f, h * 0.18f), strokeWidth = stroke, cap = StrokeCap.Round)
                val bin = Path().apply {
                    moveTo(w * 0.28f, h * 0.28f)
                    lineTo(w * 0.32f, h * 0.82f)
                    lineTo(w * 0.68f, h * 0.82f)
                    lineTo(w * 0.72f, h * 0.28f)
                }
                drawPath(bin, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ChatIconAsset.Share -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                drawCircle(c, radius = w * 0.12f, center = Offset(w * 0.75f, h * 0.25f))
                drawCircle(c, radius = w * 0.12f, center = Offset(w * 0.25f, h * 0.50f))
                drawCircle(c, radius = w * 0.12f, center = Offset(w * 0.75f, h * 0.75f))
                drawLine(c, Offset(w * 0.35f, h * 0.44f), Offset(w * 0.65f, h * 0.30f), strokeWidth = stroke)
                drawLine(c, Offset(w * 0.35f, h * 0.56f), Offset(w * 0.65f, h * 0.70f), strokeWidth = stroke)
            }
            ChatIconAsset.Forward -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                val fwd = Path().apply {
                    moveTo(w * 0.30f, h * 0.25f)
                    lineTo(w * 0.70f, h * 0.50f)
                    lineTo(w * 0.30f, h * 0.75f)
                }
                drawPath(fwd, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ChatIconAsset.Check -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                val chk = Path().apply {
                    moveTo(w * 0.20f, h * 0.50f)
                    lineTo(w * 0.42f, h * 0.75f)
                    lineTo(w * 0.80f, h * 0.28f)
                }
                drawPath(chk, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ChatIconAsset.Lock -> {
                val c = if (tint == Color.Unspecified) Color.Black else tint
                val stroke = 1.8f * densityFactor
                drawRoundRect(c, topLeft = Offset(w * 0.25f, h * 0.45f), size = Size(w * 0.50f, h * 0.45f), style = Stroke(stroke))
                val arch = Path().apply {
                    moveTo(w * 0.35f, h * 0.45f)
                    cubicTo(w * 0.35f, h * 0.22f, w * 0.65f, h * 0.22f, w * 0.65f, h * 0.45f)
                }
                drawPath(arch, c, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            ChatIconAsset.MicOff -> {
                val c = if (tint == Color.Unspecified) Color.Red else tint
                val stroke = 1.8f * densityFactor
                drawLine(c, Offset(w * 0.20f, h * 0.20f), Offset(w * 0.80f, h * 0.80f), strokeWidth = stroke * 1.2f, cap = StrokeCap.Round)
            }
        }
    }
}
