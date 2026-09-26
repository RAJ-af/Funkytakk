package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Pastel color theme specification for hobby chips
 */
data class HobbyTheme(
    val bgColor: Color,
    val contentColor: Color
)

object HobbyColorPalette {
    val Rose = HobbyTheme(bgColor = Color(0xFFFFE4E6), contentColor = Color(0xFFE11D48))
    val Purple = HobbyTheme(bgColor = Color(0xFFF3E8FF), contentColor = Color(0xFF9333EA))
    val Sky = HobbyTheme(bgColor = Color(0xFFE0F2FE), contentColor = Color(0xFF0284C7))
    val Amber = HobbyTheme(bgColor = Color(0xFFFEF3C7), contentColor = Color(0xFFD97706))
    val Orange = HobbyTheme(bgColor = Color(0xFFFFEDD5), contentColor = Color(0xFFEA580C))
    val Violet = HobbyTheme(bgColor = Color(0xFFEDE9FE), contentColor = Color(0xFF7C3AED))
    val Emerald = HobbyTheme(bgColor = Color(0xFFD1FAE5), contentColor = Color(0xFF059669))
    val Teal = HobbyTheme(bgColor = Color(0xFFCCFBF1), contentColor = Color(0xFF0D9488))
    val Indigo = HobbyTheme(bgColor = Color(0xFFE0E7FF), contentColor = Color(0xFF4F46E5))
    val Pink = HobbyTheme(bgColor = Color(0xFFFCE7F3), contentColor = Color(0xFFDB2777))
    val Lime = HobbyTheme(bgColor = Color(0xFFECFCCB), contentColor = Color(0xFF65A30D))
    val Cyan = HobbyTheme(bgColor = Color(0xFFCFFAFE), contentColor = Color(0xFF0891B2))
    val Gray = HobbyTheme(bgColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569))

    fun getThemeForHobby(hobbyName: String): HobbyTheme {
        val lower = hobbyName.trim().lowercase()
        return when {
            lower.contains("music") || lower.contains("sing") || lower.contains("guitar") || lower.contains("piano") -> Rose
            lower.contains("photo") || lower.contains("camera") || lower.contains("art") || lower.contains("draw") -> Purple
            lower.contains("travel") || lower.contains("flight") || lower.contains("plane") || lower.contains("explore") -> Sky
            lower.contains("drama") || lower.contains("kdrama") || lower.contains("k-drama") || lower.contains("acting") -> Pink
            lower.contains("food") || lower.contains("cook") || lower.contains("bake") || lower.contains("eat") || lower.contains("ramen") -> Orange
            lower.contains("movie") || lower.contains("cinema") || lower.contains("film") || lower.contains("anime") -> Violet
            lower.contains("game") || lower.contains("gaming") || lower.contains("esport") -> Indigo
            lower.contains("gym") || lower.contains("fitness") || lower.contains("workout") || lower.contains("sport") -> Emerald
            lower.contains("read") || lower.contains("book") || lower.contains("write") || lower.contains("poetry") -> Teal
            lower.contains("code") || lower.contains("tech") || lower.contains("program") || lower.contains("dev") -> Cyan
            lower.contains("dance") || lower.contains("kpop") || lower.contains("k-pop") -> Rose
            lower.contains("swim") || lower.contains("surf") || lower.contains("beach") -> Sky
            lower.contains("yoga") || lower.contains("meditat") || lower.contains("peace") -> Lime
            lower.contains("cycle") || lower.contains("bike") || lower.contains("cycling") -> Emerald
            lower.contains("hike") || lower.contains("camp") || lower.contains("nature") -> Lime
            lower.contains("coffee") || lower.contains("cafe") || lower.contains("tea") -> Amber
            lower.contains("pet") || lower.contains("dog") || lower.contains("cat") || lower.contains("animal") -> Amber
            lower.contains("fashion") || lower.contains("style") || lower.contains("cloth") -> Pink
            lower.contains("shop") || lower.contains("mall") -> Purple
            lower.contains("chess") || lower.contains("board") || lower.contains("puzzle") -> Indigo
            lower.contains("astro") || lower.contains("space") || lower.contains("star") -> Violet
            lower.contains("run") || lower.contains("jog") || lower.contains("marathon") -> Emerald
            lower.contains("tennis") || lower.contains("badminton") || lower.contains("squash") -> Lime
            lower.contains("basket") || lower.contains("soccer") || lower.contains("football") -> Orange
            lower.contains("garden") || lower.contains("plant") || lower.contains("flower") -> Emerald
            lower.contains("podcast") || lower.contains("radio") || lower.contains("audio") -> Violet
            lower.contains("craft") || lower.contains("diy") || lower.contains("knit") -> Pink
            lower.contains("drive") || lower.contains("car") || lower.contains("road") -> Cyan
            lower.contains("lang") || lower.contains("talk") || lower.contains("chat") -> Sky
            else -> {
                val palettes = listOf(Rose, Purple, Sky, Amber, Orange, Violet, Emerald, Teal, Indigo, Pink, Lime, Cyan)
                val idx = Math.abs(lower.hashCode()) % palettes.size
                palettes[idx]
            }
        }
    }
}

/**
 * Master Composable to render a Hobby Chip with auto-resolved custom canvas icon and pastel styling
 */
@Composable
fun DynamicHobbyChip(
    hobby: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 13.dp
) {
    val safeHobby = hobby.ifBlank { "Interest" }
    val theme = HobbyColorPalette.getThemeForHobby(safeHobby)

    Box(
        modifier = modifier
            .background(theme.bgColor, RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            RenderHobbyIcon(hobby = safeHobby, color = theme.contentColor, modifier = Modifier.size(iconSize))
            Text(
                text = safeHobby,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = theme.contentColor
            )
        }
    }
}

/**
 * Icon router that maps any hobby name to one of the 50+ custom canvas icons.
 * If no icon matches (or for any custom interest), it gracefully renders a custom Star Icon.
 */
@Composable
fun RenderHobbyIcon(hobby: String, color: Color, modifier: Modifier = Modifier) {
    val lower = hobby.trim().lowercase()
    if (lower.isEmpty()) {
        HobbyIconStarBadge(color, modifier)
        return
    }
    when {
        // 1. Music & Audio
        lower.contains("music") -> HobbyIconMusic(color, modifier)
        lower.contains("sing") -> HobbyIconMicrophone(color, modifier)
        lower.contains("guitar") -> HobbyIconGuitar(color, modifier)
        lower.contains("piano") -> HobbyIconPiano(color, modifier)
        lower.contains("podcast") || lower.contains("radio") -> HobbyIconHeadphones(color, modifier)
        
        // 2. Visual Arts & Media
        lower.contains("photo") || lower.contains("camera") -> HobbyIconCamera(color, modifier)
        lower.contains("art") || lower.contains("draw") || lower.contains("paint") -> HobbyIconPalette(color, modifier)
        lower.contains("movie") || lower.contains("cinema") || lower.contains("film") -> HobbyIconFilmReel(color, modifier)
        lower.contains("drama") || lower.contains("kdrama") || lower.contains("k-drama") -> HobbyIconClapperboard(color, modifier)
        lower.contains("anime") || lower.contains("manga") -> HobbyIconSparkleStar(color, modifier)
        
        // 3. Travel & Outdoors
        lower.contains("travel") || lower.contains("flight") || lower.contains("plane") -> HobbyIconAirplane(color, modifier)
        lower.contains("camp") || lower.contains("tent") -> HobbyIconTent(color, modifier)
        lower.contains("hike") || lower.contains("mountain") -> HobbyIconMountain(color, modifier)
        lower.contains("nature") || lower.contains("tree") -> HobbyIconTree(color, modifier)
        lower.contains("beach") || lower.contains("sun") || lower.contains("summer") -> HobbyIconSun(color, modifier)
        
        // 4. Food & Drinks
        lower.contains("food") || lower.contains("eat") || lower.contains("dine") -> HobbyIconCutlery(color, modifier)
        lower.contains("cook") || lower.contains("chef") -> HobbyIconChefHat(color, modifier)
        lower.contains("bake") || lower.contains("baking") || lower.contains("cake") -> HobbyIconCake(color, modifier)
        lower.contains("coffee") || lower.contains("cafe") -> HobbyIconCoffeeCup(color, modifier)
        lower.contains("tea") || lower.contains("boba") -> HobbyIconTeaCup(color, modifier)
        lower.contains("ramen") || lower.contains("noodle") -> HobbyIconBowl(color, modifier)
        lower.contains("pizza") || lower.contains("burger") -> HobbyIconFastFood(color, modifier)
        
        // 5. Sports & Fitness
        lower.contains("gym") || lower.contains("fitness") || lower.contains("workout") -> HobbyIconDumbbell(color, modifier)
        lower.contains("run") || lower.contains("jog") -> HobbyIconRunningShoe(color, modifier)
        lower.contains("swim") -> HobbyIconWaves(color, modifier)
        lower.contains("cycle") || lower.contains("bike") -> HobbyIconBicycle(color, modifier)
        lower.contains("yoga") || lower.contains("meditat") -> HobbyIconLotus(color, modifier)
        lower.contains("dance") || lower.contains("dancing") -> HobbyIconDance(color, modifier)
        lower.contains("soccer") || lower.contains("football") -> HobbyIconSoccer(color, modifier)
        lower.contains("basket") -> HobbyIconBasketball(color, modifier)
        lower.contains("tennis") || lower.contains("badminton") -> HobbyIconTennisRacket(color, modifier)
        lower.contains("skate") -> HobbyIconSkateboard(color, modifier)
        
        // 6. Tech, Gaming & Study
        lower.contains("game") || lower.contains("gaming") -> HobbyIconGamepad(color, modifier)
        lower.contains("code") || lower.contains("program") || lower.contains("tech") || lower.contains("dev") -> HobbyIconCode(color, modifier)
        lower.contains("read") || lower.contains("book") -> HobbyIconBook(color, modifier)
        lower.contains("write") || lower.contains("journal") || lower.contains("poetry") -> HobbyIconPen(color, modifier)
        lower.contains("lang") || lower.contains("talk") -> HobbyIconGlobe(color, modifier)
        lower.contains("chess") -> HobbyIconChessKnight(color, modifier)
        lower.contains("board") || lower.contains("puzzle") -> HobbyIconPuzzle(color, modifier)
        
        // 7. Lifestyle & Pets
        lower.contains("pet") || lower.contains("dog") || lower.contains("cat") || lower.contains("animal") -> HobbyIconPaw(color, modifier)
        lower.contains("plant") || lower.contains("garden") -> HobbyIconSprout(color, modifier)
        lower.contains("fashion") || lower.contains("style") || lower.contains("dress") -> HobbyIconHanger(color, modifier)
        lower.contains("shop") || lower.contains("shopping") -> HobbyIconShoppingBag(color, modifier)
        lower.contains("makeup") || lower.contains("beauty") -> HobbyIconLipstick(color, modifier)
        lower.contains("craft") || lower.contains("diy") || lower.contains("knit") -> HobbyIconScissors(color, modifier)
        lower.contains("drive") || lower.contains("car") || lower.contains("road") -> HobbyIconCar(color, modifier)
        lower.contains("astro") || lower.contains("space") || lower.contains("star") -> HobbyIconPlanet(color, modifier)
        lower.contains("volunteer") || lower.contains("charity") || lower.contains("help") -> HobbyIconHeartHand(color, modifier)
        
        // Default Fallback
        else -> HobbyIconStarBadge(color, modifier)
    }
}

// -----------------------------------------------------------------------------
// 50+ DEDICATED CANVAS-DRAWN VECTOR ICONS (NO MATERIAL ICONS, NO EMOJIS)
// -----------------------------------------------------------------------------

@Composable
fun HobbyIconMusic(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.80f)
            lineTo(w * 0.35f, h * 0.25f)
            lineTo(w * 0.85f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.70f)
        }
        drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = w * 0.18f, center = Offset(w * 0.25f, h * 0.80f))
        drawCircle(color, radius = w * 0.18f, center = Offset(w * 0.75f, h * 0.70f))
    }
}

@Composable
fun HobbyIconMicrophone(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        drawRoundRect(color, topLeft = Offset(w * 0.3f, 0f), size = Size(w * 0.4f, h * 0.6f), cornerRadius = CornerRadius(w * 0.2f), style = Stroke(sw))
        val stand = Path().apply {
            moveTo(w * 0.15f, h * 0.35f)
            cubicTo(w * 0.15f, h * 0.75f, w * 0.85f, h * 0.75f, w * 0.85f, h * 0.35f)
        }
        drawPath(stand, color, style = Stroke(sw, cap = StrokeCap.Round))
        drawLine(color, start = Offset(w * 0.5f, h * 0.75f), end = Offset(w * 0.5f, h * 0.95f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun HobbyIconGuitar(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        drawLine(color, start = Offset(w * 0.85f, h * 0.15f), end = Offset(w * 0.45f, h * 0.55f), strokeWidth = sw, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.22f, center = Offset(w * 0.32f, h * 0.68f), style = Stroke(sw))
        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(sw))
    }
}

@Composable
fun HobbyIconPiano(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        drawRoundRect(color, topLeft = Offset(0f, h * 0.2f), size = Size(w, h * 0.65f), cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(sw))
        drawLine(color, start = Offset(w * 0.33f, h * 0.2f), end = Offset(w * 0.33f, h * 0.85f), strokeWidth = sw)
        drawLine(color, start = Offset(w * 0.66f, h * 0.2f), end = Offset(w * 0.66f, h * 0.85f), strokeWidth = sw)
        drawRect(color, topLeft = Offset(w * 0.25f, h * 0.2f), size = Size(w * 0.16f, h * 0.35f))
        drawRect(color, topLeft = Offset(w * 0.58f, h * 0.2f), size = Size(w * 0.16f, h * 0.35f))
    }
}

@Composable
fun HobbyIconHeadphones(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val arch = Path().apply {
            moveTo(w * 0.15f, h * 0.65f)
            cubicTo(w * 0.15f, h * 0.05f, w * 0.85f, h * 0.05f, w * 0.85f, h * 0.65f)
        }
        drawPath(arch, color, style = Stroke(sw, cap = StrokeCap.Round))
        drawRoundRect(color, topLeft = Offset(w * 0.05f, h * 0.50f), size = Size(w * 0.22f, h * 0.40f), cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(sw))
        drawRoundRect(color, topLeft = Offset(w * 0.73f, h * 0.50f), size = Size(w * 0.22f, h * 0.40f), cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(sw))
    }
}

@Composable
fun HobbyIconCamera(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        drawRoundRect(color, topLeft = Offset(0f, h * 0.25f), size = Size(w, h * 0.75f), cornerRadius = CornerRadius(2.5.dp.toPx()), style = Stroke(sw))
        drawLine(color, start = Offset(w * 0.30f, h * 0.25f), end = Offset(w * 0.40f, h * 0.10f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.40f, h * 0.10f), end = Offset(w * 0.60f, h * 0.10f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.60f, h * 0.10f), end = Offset(w * 0.70f, h * 0.25f), strokeWidth = sw, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.20f, center = Offset(w * 0.5f, h * 0.62f), style = Stroke(sw))
    }
}

@Composable
fun HobbyIconPalette(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        drawCircle(color, radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(sw))
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.30f, h * 0.35f))
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.25f))
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.70f, h * 0.35f))
        drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.65f, h * 0.65f), style = Stroke(sw))
    }
}

@Composable
fun HobbyIconFilmReel(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        drawRoundRect(color, topLeft = Offset(0f, 0f), size = Size(w, h), cornerRadius = CornerRadius(2.5.dp.toPx()), style = Stroke(sw))
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.30f, h * 0.35f))
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.70f, h * 0.35f))
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.50f, h * 0.70f))
    }
}

@Composable
fun HobbyIconClapperboard(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        drawRoundRect(color, topLeft = Offset(0f, h * 0.15f), size = Size(w, h * 0.85f), cornerRadius = CornerRadius(2.5.dp.toPx()), style = Stroke(sw))
        drawLine(color, start = Offset(0f, h * 0.45f), end = Offset(w, h * 0.45f), strokeWidth = sw)
        drawLine(color, start = Offset(w * 0.35f, h * 0.15f), end = Offset(w * 0.45f, h * 0.45f), strokeWidth = sw)
        drawLine(color, start = Offset(w * 0.70f, h * 0.15f), end = Offset(w * 0.80f, h * 0.45f), strokeWidth = sw)
    }
}

@Composable
fun HobbyIconSparkleStar(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val star = Path().apply {
            moveTo(w * 0.5f, 0f)
            quadraticTo(w * 0.5f, h * 0.5f, w, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, h)
            quadraticTo(w * 0.5f, h * 0.5f, 0f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, 0f)
        }
        drawPath(star, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun HobbyIconAirplane(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.90f, h * 0.10f)
            lineTo(w * 0.40f, h * 0.60f)
            lineTo(w * 0.10f, h * 0.55f)
            lineTo(w * 0.45f, h * 0.35f)
            lineTo(w * 0.60f, 0f)
            lineTo(w * 0.70f, 0f)
            lineTo(w * 0.55f, h * 0.30f)
            lineTo(w * 0.90f, h * 0.10f)
            close()
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun HobbyIconTent(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.95f, h * 0.85f)
            lineTo(w * 0.05f, h * 0.85f)
            close()
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, start = Offset(w * 0.5f, h * 0.15f), end = Offset(w * 0.5f, h * 0.85f), strokeWidth = sw)
    }
}

@Composable
fun HobbyIconMountain(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val m1 = Path().apply {
            moveTo(w * 0.45f, h * 0.15f)
            lineTo(w * 0.90f, h * 0.85f)
            lineTo(0f, h * 0.85f)
            close()
        }
        drawPath(m1, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun HobbyIconTree(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val tree = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w * 0.85f, h * 0.65f)
            lineTo(w * 0.15f, h * 0.65f)
            close()
        }
        drawPath(tree, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, start = Offset(w * 0.5f, h * 0.65f), end = Offset(w * 0.5f, h * 0.95f), strokeWidth = sw * 1.5f, cap = StrokeCap.Round)
    }
}

@Composable
fun HobbyIconSun(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        drawCircle(color, radius = w * 0.25f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(sw))
        for (i in 0 until 8) {
            val angle = i * (Math.PI / 4)
            val startX = (w * 0.5f + Math.cos(angle) * (w * 0.32f)).toFloat()
            val startY = (h * 0.5f + Math.sin(angle) * (h * 0.32f)).toFloat()
            val endX = (w * 0.5f + Math.cos(angle) * (w * 0.44f)).toFloat()
            val endY = (h * 0.5f + Math.sin(angle) * (h * 0.44f)).toFloat()
            drawLine(color, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun HobbyIconCutlery(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        drawLine(color, start = Offset(w * 0.30f, 0f), end = Offset(w * 0.30f, h), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.15f, 0f), end = Offset(w * 0.15f, h * 0.35f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.45f, 0f), end = Offset(w * 0.45f, h * 0.35f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.15f, h * 0.35f), end = Offset(w * 0.45f, h * 0.35f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.75f, 0f), end = Offset(w * 0.75f, h), strokeWidth = sw, cap = StrokeCap.Round)
        val knifeBlade = Path().apply {
            moveTo(w * 0.75f, 0f)
            cubicTo(w * 0.90f, h * 0.20f, w * 0.90f, h * 0.40f, w * 0.75f, h * 0.50f)
        }
        drawPath(knifeBlade, color, style = Stroke(sw, cap = StrokeCap.Round))
    }
}

@Composable
fun HobbyIconChefHat(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val hat = Path().apply {
            moveTo(w * 0.2f, h * 0.75f)
            cubicTo(w * 0.05f, h * 0.45f, w * 0.35f, h * 0.15f, w * 0.5f, h * 0.30f)
            cubicTo(w * 0.65f, h * 0.15f, w * 0.95f, h * 0.45f, w * 0.8f, h * 0.75f)
            close()
        }
        drawPath(hat, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawRoundRect(color, topLeft = Offset(w * 0.2f, h * 0.75f), size = Size(w * 0.6f, h * 0.2f), cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(sw))
    }
}

@Composable
fun HobbyIconCake(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        drawRoundRect(color, topLeft = Offset(w * 0.10f, h * 0.45f), size = Size(w * 0.80f, h * 0.50f), cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(sw))
        drawLine(color, start = Offset(w * 0.10f, h * 0.70f), end = Offset(w * 0.90f, h * 0.70f), strokeWidth = sw)
        drawLine(color, start = Offset(w * 0.50f, h * 0.20f), end = Offset(w * 0.50f, h * 0.45f), strokeWidth = sw, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.10f))
    }
}

@Composable
fun HobbyIconCoffeeCup(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val cup = Path().apply {
            moveTo(w * 0.15f, h * 0.25f)
            lineTo(w * 0.70f, h * 0.25f)
            lineTo(w * 0.62f, h * 0.85f)
            lineTo(w * 0.23f, h * 0.85f)
            close()
        }
        drawPath(cup, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val handle = Path().apply {
            moveTo(w * 0.68f, h * 0.35f)
            cubicTo(w * 0.95f, h * 0.35f, w * 0.90f, h * 0.70f, w * 0.64f, h * 0.70f)
        }
        drawPath(handle, color, style = Stroke(sw, cap = StrokeCap.Round))
    }
}

@Composable
fun HobbyIconTeaCup(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        drawRoundRect(color, topLeft = Offset(w * 0.25f, h * 0.15f), size = Size(w * 0.5f, h * 0.75f), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(sw))
        drawLine(color, start = Offset(w * 0.6f, 0f), end = Offset(w * 0.4f, h * 0.9f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun HobbyIconBowl(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val bowl = Path().apply {
            moveTo(w * 0.1f, h * 0.35f)
            lineTo(w * 0.9f, h * 0.35f)
            cubicTo(w * 0.85f, h * 0.90f, w * 0.15f, h * 0.90f, w * 0.1f, h * 0.35f)
        }
        drawPath(bowl, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, start = Offset(w * 0.2f, h * 0.15f), end = Offset(w * 0.8f, h * 0.45f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun HobbyIconFastFood(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val bunTop = Path().apply {
            moveTo(w * 0.15f, h * 0.45f)
            cubicTo(w * 0.15f, h * 0.15f, w * 0.85f, h * 0.15f, w * 0.85f, h * 0.45f)
            close()
        }
        drawPath(bunTop, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawRoundRect(color, topLeft = Offset(w * 0.15f, h * 0.65f), size = Size(w * 0.7f, h * 0.2f), cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(sw))
    }
}

@Composable
fun HobbyIconDumbbell(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        drawLine(color, start = Offset(w * 0.25f, h * 0.5f), end = Offset(w * 0.75f, h * 0.5f), strokeWidth = sw * 1.4f, cap = StrokeCap.Round)
        drawRoundRect(color, topLeft = Offset(w * 0.12f, h * 0.25f), size = Size(w * 0.13f, h * 0.50f), cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(sw))
        drawRoundRect(color, topLeft = Offset(w * 0.75f, h * 0.25f), size = Size(w * 0.13f, h * 0.50f), cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(sw))
    }
}

@Composable
fun HobbyIconRunningShoe(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val shoe = Path().apply {
            moveTo(w * 0.1f, h * 0.4f)
            lineTo(w * 0.35f, h * 0.4f)
            lineTo(w * 0.5f, h * 0.6f)
            lineTo(w * 0.9f, h * 0.6f)
            lineTo(w * 0.9f, h * 0.85f)
            lineTo(w * 0.1f, h * 0.85f)
            close()
        }
        drawPath(shoe, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun HobbyIconWaves(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val w1 = Path().apply {
            moveTo(0f, h * 0.4f)
            quadraticTo(w * 0.25f, h * 0.2f, w * 0.5f, h * 0.4f)
            quadraticTo(w * 0.75f, h * 0.6f, w, h * 0.4f)
        }
        drawPath(w1, color, style = Stroke(sw, cap = StrokeCap.Round))
        val w2 = Path().apply {
            moveTo(0f, h * 0.75f)
            quadraticTo(w * 0.25f, h * 0.55f, w * 0.5f, h * 0.75f)
            quadraticTo(w * 0.75f, h * 0.95f, w, h * 0.75f)
        }
        drawPath(w2, color, style = Stroke(sw, cap = StrokeCap.Round))
    }
}

@Composable
fun HobbyIconBicycle(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        drawCircle(color, radius = w * 0.20f, center = Offset(w * 0.25f, h * 0.70f), style = Stroke(sw))
        drawCircle(color, radius = w * 0.20f, center = Offset(w * 0.75f, h * 0.70f), style = Stroke(sw))
        val frame = Path().apply {
            moveTo(w * 0.25f, h * 0.70f)
            lineTo(w * 0.50f, h * 0.40f)
            lineTo(w * 0.75f, h * 0.70f)
            lineTo(w * 0.45f, h * 0.70f)
            lineTo(w * 0.50f, h * 0.40f)
        }
        drawPath(frame, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun HobbyIconLotus(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        val p1 = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            cubicTo(w * 0.3f, h * 0.5f, w * 0.4f, h * 0.8f, w * 0.5f, h * 0.85f)
            cubicTo(w * 0.6f, h * 0.8f, w * 0.7f, h * 0.5f, w * 0.5f, h * 0.15f)
        }
        drawPath(p1, color, style = Stroke(sw, cap = StrokeCap.Round))
    }
}

@Composable
fun HobbyIconDance(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.2f), style = Stroke(sw))
        val body = Path().apply {
            moveTo(w * 0.5f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.25f, h * 0.95f)
            moveTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.75f, h * 0.85f)
            moveTo(w * 0.2f, h * 0.4f)
            lineTo(w * 0.8f, h * 0.45f)
        }
        drawPath(body, color, style = Stroke(sw, cap = StrokeCap.Round))
    }
}

@Composable
fun HobbyIconSoccer(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        drawCircle(color, radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(sw))
        val p = Path().apply {
            moveTo(w * 0.5f, h * 0.35f)
            lineTo(w * 0.65f, h * 0.45f)
            lineTo(w * 0.60f, h * 0.65f)
            lineTo(w * 0.40f, h * 0.65f)
            lineTo(w * 0.35f, h * 0.45f)
            close()
        }
        drawPath(p, color, style = Stroke(sw))
    }
}

@Composable
fun HobbyIconBasketball(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        drawCircle(color, radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(sw))
        drawLine(color, start = Offset(0f, h * 0.5f), end = Offset(w, h * 0.5f), strokeWidth = sw)
        drawLine(color, start = Offset(w * 0.5f, 0f), end = Offset(w * 0.5f, h), strokeWidth = sw)
    }
}

@Composable
fun HobbyIconTennisRacket(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        drawCircle(color, radius = w * 0.25f, center = Offset(w * 0.65f, h * 0.35f), style = Stroke(sw))
        drawLine(color, start = Offset(w * 0.48f, h * 0.52f), end = Offset(w * 0.15f, h * 0.85f), strokeWidth = sw * 1.5f, cap = StrokeCap.Round)
    }
}

@Composable
fun HobbyIconSkateboard(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val deck = Path().apply {
            moveTo(w * 0.05f, h * 0.4f)
            lineTo(w * 0.2f, h * 0.55f)
            lineTo(w * 0.8f, h * 0.55f)
            lineTo(w * 0.95f, h * 0.4f)
        }
        drawPath(deck, color, style = Stroke(sw * 1.3f, cap = StrokeCap.Round))
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.28f, h * 0.75f))
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.72f, h * 0.75f))
    }
}

@Composable
fun HobbyIconGamepad(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        drawRoundRect(color, topLeft = Offset(w * 0.1f, h * 0.25f), size = Size(w * 0.8f, h * 0.5f), cornerRadius = CornerRadius(6.dp.toPx()), style = Stroke(sw))
        // D-Pad
        drawLine(color, start = Offset(w * 0.25f, h * 0.4f), end = Offset(w * 0.25f, h * 0.6f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.18f, h * 0.5f), end = Offset(w * 0.32f, h * 0.5f), strokeWidth = sw, cap = StrokeCap.Round)
        // Buttons
        drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.75f, h * 0.42f))
        drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.65f, h * 0.55f))
    }
}

@Composable
fun HobbyIconCode(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val leftChevron = Path().apply {
            moveTo(w * 0.35f, h * 0.25f)
            lineTo(w * 0.15f, h * 0.50f)
            lineTo(w * 0.35f, h * 0.75f)
        }
        drawPath(leftChevron, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val rightChevron = Path().apply {
            moveTo(w * 0.65f, h * 0.25f)
            lineTo(w * 0.85f, h * 0.50f)
            lineTo(w * 0.65f, h * 0.75f)
        }
        drawPath(rightChevron, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, start = Offset(w * 0.55f, h * 0.20f), end = Offset(w * 0.45f, h * 0.80f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun HobbyIconBook(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val book = Path().apply {
            moveTo(w * 0.5f, h * 0.25f)
            cubicTo(w * 0.3f, h * 0.15f, w * 0.1f, h * 0.25f, 0f, h * 0.25f)
            lineTo(0f, h * 0.85f)
            cubicTo(w * 0.1f, h * 0.85f, w * 0.3f, h * 0.75f, w * 0.5f, h * 0.85f)
            cubicTo(w * 0.7f, h * 0.75f, w * 0.9f, h * 0.85f, w, h * 0.85f)
            lineTo(w, h * 0.25f)
            cubicTo(w * 0.9f, h * 0.25f, w * 0.7f, h * 0.15f, w * 0.5f, h * 0.25f)
        }
        drawPath(book, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, start = Offset(w * 0.5f, h * 0.25f), end = Offset(w * 0.5f, h * 0.85f), strokeWidth = sw)
    }
}

@Composable
fun HobbyIconPen(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        drawLine(color, start = Offset(w * 0.85f, h * 0.15f), end = Offset(w * 0.30f, h * 0.70f), strokeWidth = sw * 1.5f, cap = StrokeCap.Round)
        val tip = Path().apply {
            moveTo(w * 0.30f, h * 0.70f)
            lineTo(w * 0.15f, h * 0.85f)
            lineTo(w * 0.25f, h * 0.65f)
            close()
        }
        drawPath(tip, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun HobbyIconGlobe(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        drawCircle(color, radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(sw))
        drawLine(color, start = Offset(w * 0.05f, h * 0.5f), end = Offset(w * 0.95f, h * 0.5f), strokeWidth = sw)
        val meridian = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            cubicTo(w * 0.15f, h * 0.25f, w * 0.15f, h * 0.75f, w * 0.5f, h * 0.95f)
            cubicTo(w * 0.85f, h * 0.75f, w * 0.85f, h * 0.25f, w * 0.5f, h * 0.05f)
        }
        drawPath(meridian, color, style = Stroke(sw))
    }
}

@Composable
fun HobbyIconChessKnight(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        val knight = Path().apply {
            moveTo(w * 0.2f, h * 0.9f)
            lineTo(w * 0.8f, h * 0.9f)
            lineTo(w * 0.7f, h * 0.7f)
            cubicTo(w * 0.85f, h * 0.5f, w * 0.7f, h * 0.15f, w * 0.5f, h * 0.15f)
            lineTo(w * 0.3f, h * 0.3f)
            lineTo(w * 0.15f, h * 0.4f)
            lineTo(w * 0.35f, h * 0.6f)
            close()
        }
        drawPath(knight, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun HobbyIconPuzzle(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        drawRoundRect(color, topLeft = Offset(w * 0.15f, h * 0.15f), size = Size(w * 0.7f, h * 0.7f), cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(sw))
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.15f))
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.85f, h * 0.5f))
    }
}

@Composable
fun HobbyIconPaw(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color, radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.65f))
        drawCircle(color, radius = w * 0.09f, center = Offset(w * 0.22f, h * 0.38f))
        drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.40f, h * 0.22f))
        drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.60f, h * 0.22f))
        drawCircle(color, radius = w * 0.09f, center = Offset(w * 0.78f, h * 0.38f))
    }
}

@Composable
fun HobbyIconSprout(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        drawLine(color, start = Offset(w * 0.5f, h * 0.35f), end = Offset(w * 0.5f, h * 0.95f), strokeWidth = sw, cap = StrokeCap.Round)
        val l1 = Path().apply {
            moveTo(w * 0.5f, h * 0.6f)
            cubicTo(w * 0.15f, h * 0.4f, w * 0.2f, h * 0.2f, w * 0.5f, h * 0.35f)
        }
        drawPath(l1, color, style = Stroke(sw, cap = StrokeCap.Round))
        val l2 = Path().apply {
            moveTo(w * 0.5f, h * 0.5f)
            cubicTo(w * 0.85f, h * 0.3f, w * 0.8f, h * 0.1f, w * 0.5f, h * 0.25f)
        }
        drawPath(l2, color, style = Stroke(sw, cap = StrokeCap.Round))
    }
}

@Composable
fun HobbyIconHanger(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val hook = Path().apply {
            moveTo(w * 0.5f, h * 0.35f)
            cubicTo(w * 0.5f, 0f, w * 0.75f, h * 0.1f, w * 0.65f, h * 0.25f)
        }
        drawPath(hook, color, style = Stroke(sw, cap = StrokeCap.Round))
        val triangle = Path().apply {
            moveTo(w * 0.5f, h * 0.35f)
            lineTo(w * 0.9f, h * 0.75f)
            lineTo(w * 0.1f, h * 0.75f)
            close()
        }
        drawPath(triangle, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun HobbyIconShoppingBag(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        drawRoundRect(color, topLeft = Offset(w * 0.15f, h * 0.3f), size = Size(w * 0.7f, h * 0.65f), cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(sw))
        val handle = Path().apply {
            moveTo(w * 0.35f, h * 0.3f)
            cubicTo(w * 0.35f, h * 0.05f, w * 0.65f, h * 0.05f, w * 0.65f, h * 0.3f)
        }
        drawPath(handle, color, style = Stroke(sw, cap = StrokeCap.Round))
    }
}

@Composable
fun HobbyIconLipstick(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        drawRoundRect(color, topLeft = Offset(w * 0.3f, h * 0.45f), size = Size(w * 0.4f, h * 0.5f), cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(sw))
        val bullet = Path().apply {
            moveTo(w * 0.35f, h * 0.45f)
            lineTo(w * 0.35f, h * 0.25f)
            lineTo(w * 0.65f, h * 0.10f)
            lineTo(w * 0.65f, h * 0.45f)
            close()
        }
        drawPath(bullet, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun HobbyIconScissors(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        drawLine(color, start = Offset(w * 0.3f, h * 0.7f), end = Offset(w * 0.75f, h * 0.15f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.7f, h * 0.7f), end = Offset(w * 0.25f, h * 0.15f), strokeWidth = sw, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.25f, h * 0.8f), style = Stroke(sw))
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.75f, h * 0.8f), style = Stroke(sw))
    }
}

@Composable
fun HobbyIconCar(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        val car = Path().apply {
            moveTo(0f, h * 0.65f)
            lineTo(w * 0.2f, h * 0.65f)
            lineTo(w * 0.35f, h * 0.35f)
            lineTo(w * 0.75f, h * 0.35f)
            lineTo(w * 0.90f, h * 0.65f)
            lineTo(w, h * 0.65f)
            lineTo(w, h * 0.8f)
            lineTo(0f, h * 0.8f)
            close()
        }
        drawPath(car, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.25f, h * 0.82f))
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.75f, h * 0.82f))
    }
}

@Composable
fun HobbyIconPlanet(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        drawCircle(color, radius = w * 0.28f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(sw))
        val ring = Path().apply {
            moveTo(w * 0.05f, h * 0.65f)
            cubicTo(w * 0.15f, h * 0.25f, w * 0.85f, h * 0.25f, w * 0.95f, h * 0.35f)
        }
        drawPath(ring, color, style = Stroke(sw, cap = StrokeCap.Round))
    }
}

@Composable
fun HobbyIconHeartHand(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.5f * density
        val w = size.width
        val h = size.height
        val heart = Path().apply {
            moveTo(w * 0.5f, h * 0.7f)
            cubicTo(w * 0.15f, h * 0.45f, w * 0.15f, h * 0.15f, w * 0.5f, h * 0.35f)
            cubicTo(w * 0.85f, h * 0.15f, w * 0.85f, h * 0.45f, w * 0.5f, h * 0.7f)
        }
        drawPath(heart, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun HobbyIconStarBadge(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val outerRadius = min(w, h) * 0.46f
        val innerRadius = outerRadius * 0.42f
        
        val path = Path().apply {
            for (i in 0 until 10) {
                val angle = -Math.PI / 2.0 + i * (Math.PI / 5.0)
                val r = if (i % 2 == 0) outerRadius else innerRadius
                val px = (cx + r * cos(angle)).toFloat()
                val py = (cy + r * sin(angle)).toFloat()
                if (i == 0) {
                    moveTo(px, py)
                } else {
                    lineTo(px, py)
                }
            }
            close()
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
