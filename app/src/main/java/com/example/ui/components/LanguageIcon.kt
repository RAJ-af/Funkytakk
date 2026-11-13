package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

data class ProgrammingBadgeSpec(
    val letters: String,
    val brandColor: Color,
    val secondaryColor: Color = brandColor,
    val textColor: Color = Color.White,
    val deviconSlug: String? = null,
    val useLightCard: Boolean = true
)

object ProgrammingLanguagesRegistry {
    private val specs = mapOf(
        "python" to ProgrammingBadgeSpec("Py", Color(0xFF3776AB), Color(0xFFFFD43B), Color(0xFF1E3A5F), "python", true),
        "py" to ProgrammingBadgeSpec("Py", Color(0xFF3776AB), Color(0xFFFFD43B), Color(0xFF1E3A5F), "python", true),
        "prog_python" to ProgrammingBadgeSpec("Py", Color(0xFF3776AB), Color(0xFFFFD43B), Color(0xFF1E3A5F), "python", true),

        "javascript" to ProgrammingBadgeSpec("JS", Color(0xFFF7DF1E), Color(0xFFD97706), Color(0xFF1F2937), "javascript", false),
        "js" to ProgrammingBadgeSpec("JS", Color(0xFFF7DF1E), Color(0xFFD97706), Color(0xFF1F2937), "javascript", false),
        "prog_javascript" to ProgrammingBadgeSpec("JS", Color(0xFFF7DF1E), Color(0xFFD97706), Color(0xFF1F2937), "javascript", false),

        "typescript" to ProgrammingBadgeSpec("TS", Color(0xFF3178C6), Color(0xFF1D4ED8), Color.White, "typescript", false),
        "ts" to ProgrammingBadgeSpec("TS", Color(0xFF3178C6), Color(0xFF1D4ED8), Color.White, "typescript", false),
        "prog_typescript" to ProgrammingBadgeSpec("TS", Color(0xFF3178C6), Color(0xFF1D4ED8), Color.White, "typescript", false),

        "kotlin" to ProgrammingBadgeSpec("Kt", Color(0xFF7F52FF), Color(0xFFE44857), Color(0xFF4C1D95), "kotlin", true),
        "kt" to ProgrammingBadgeSpec("Kt", Color(0xFF7F52FF), Color(0xFFE44857), Color(0xFF4C1D95), "kotlin", true),
        "prog_kotlin" to ProgrammingBadgeSpec("Kt", Color(0xFF7F52FF), Color(0xFFE44857), Color(0xFF4C1D95), "kotlin", true),

        "java" to ProgrammingBadgeSpec("Jv", Color(0xFFEA580C), Color(0xFF2563EB), Color(0xFF9A3412), "java", true),
        "jv" to ProgrammingBadgeSpec("Jv", Color(0xFFEA580C), Color(0xFF2563EB), Color(0xFF9A3412), "java", true),
        "prog_java" to ProgrammingBadgeSpec("Jv", Color(0xFFEA580C), Color(0xFF2563EB), Color(0xFF9A3412), "java", true),

        "c++" to ProgrammingBadgeSpec("C++", Color(0xFF00599C), Color(0xFF38BDF8), Color(0xFF0C4A6E), "cplusplus", true),
        "cpp" to ProgrammingBadgeSpec("C++", Color(0xFF00599C), Color(0xFF38BDF8), Color(0xFF0C4A6E), "cplusplus", true),
        "cplusplus" to ProgrammingBadgeSpec("C++", Color(0xFF00599C), Color(0xFF38BDF8), Color(0xFF0C4A6E), "cplusplus", true),
        "prog_cpp" to ProgrammingBadgeSpec("C++", Color(0xFF00599C), Color(0xFF38BDF8), Color(0xFF0C4A6E), "cplusplus", true),

        "ruby" to ProgrammingBadgeSpec("Rb", Color(0xFFCC342D), Color(0xFFFB7185), Color(0xFF991B1B), "ruby", true),
        "rb" to ProgrammingBadgeSpec("Rb", Color(0xFFCC342D), Color(0xFFFB7185), Color(0xFF991B1B), "ruby", true),
        "prog_ruby" to ProgrammingBadgeSpec("Rb", Color(0xFFCC342D), Color(0xFFFB7185), Color(0xFF991B1B), "ruby", true),

        "sql" to ProgrammingBadgeSpec("SQL", Color(0xFF336791), Color(0xFF0284C7), Color(0xFF075985), "postgresql", true),
        "postgresql" to ProgrammingBadgeSpec("SQL", Color(0xFF336791), Color(0xFF0284C7), Color(0xFF075985), "postgresql", true),
        "prog_sql" to ProgrammingBadgeSpec("SQL", Color(0xFF336791), Color(0xFF0284C7), Color(0xFF075985), "postgresql", true),

        "html/css" to ProgrammingBadgeSpec("H5", Color(0xFFE34F26), Color(0xFF2563EB), Color(0xFF9A3412), "html5", true),
        "html / css" to ProgrammingBadgeSpec("H5", Color(0xFFE34F26), Color(0xFF2563EB), Color(0xFF9A3412), "html5", true),
        "html" to ProgrammingBadgeSpec("H5", Color(0xFFE34F26), Color(0xFFF97316), Color(0xFF9A3412), "html5", true),
        "css" to ProgrammingBadgeSpec("CS", Color(0xFF264DE4), Color(0xFF38BDF8), Color(0xFF1E40AF), "css3", true),
        "prog_html" to ProgrammingBadgeSpec("H5", Color(0xFFE34F26), Color(0xFFF97316), Color(0xFF9A3412), "html5", true),
        "prog_css" to ProgrammingBadgeSpec("CS", Color(0xFF264DE4), Color(0xFF38BDF8), Color(0xFF1E40AF), "css3", true),

        "r" to ProgrammingBadgeSpec("R", Color(0xFF276DC3), Color(0xFF60A5FA), Color(0xFF1E40AF), "r", true),
        "r programming" to ProgrammingBadgeSpec("R", Color(0xFF276DC3), Color(0xFF60A5FA), Color(0xFF1E40AF), "r", true),
        "prog_r" to ProgrammingBadgeSpec("R", Color(0xFF276DC3), Color(0xFF60A5FA), Color(0xFF1E40AF), "r", true),

        "scala" to ProgrammingBadgeSpec("Sc", Color(0xFFDC322F), Color(0xFFF43F5E), Color(0xFF991B1B), "scala", true),
        "prog_scala" to ProgrammingBadgeSpec("Sc", Color(0xFFDC322F), Color(0xFFF43F5E), Color(0xFF991B1B), "scala", true),

        "shell/bash" to ProgrammingBadgeSpec("\$_", Color(0xFF2E3440), Color(0xFF4EAA25), Color(0xFF4ADE80), "bash", false),
        "shell / bash" to ProgrammingBadgeSpec("\$_", Color(0xFF2E3440), Color(0xFF4EAA25), Color(0xFF4ADE80), "bash", false),
        "shell" to ProgrammingBadgeSpec("\$_", Color(0xFF2E3440), Color(0xFF4EAA25), Color(0xFF4ADE80), "bash", false),
        "bash" to ProgrammingBadgeSpec("\$_", Color(0xFF2E3440), Color(0xFF4EAA25), Color(0xFF4ADE80), "bash", false),
        "sh" to ProgrammingBadgeSpec("\$_", Color(0xFF2E3440), Color(0xFF4EAA25), Color(0xFF4ADE80), "bash", false),
        "prog_shell" to ProgrammingBadgeSpec("\$_", Color(0xFF2E3440), Color(0xFF4EAA25), Color(0xFF4ADE80), "bash", false),

        "go" to ProgrammingBadgeSpec("Go", Color(0xFF00ADD8), Color(0xFF06B6D4), Color(0xFF0E7490), "go", true),
        "golang" to ProgrammingBadgeSpec("Go", Color(0xFF00ADD8), Color(0xFF06B6D4), Color(0xFF0E7490), "go", true),
        "go (golang)" to ProgrammingBadgeSpec("Go", Color(0xFF00ADD8), Color(0xFF06B6D4), Color(0xFF0E7490), "go", true),
        "prog_go" to ProgrammingBadgeSpec("Go", Color(0xFF00ADD8), Color(0xFF06B6D4), Color(0xFF0E7490), "go", true),

        "rust" to ProgrammingBadgeSpec("Rs", Color(0xFFCE412B), Color(0xFFF59E0B), Color(0xFF7C2D12), "rust", true),
        "rs" to ProgrammingBadgeSpec("Rs", Color(0xFFCE412B), Color(0xFFF59E0B), Color(0xFF7C2D12), "rust", true),
        "prog_rust" to ProgrammingBadgeSpec("Rs", Color(0xFFCE412B), Color(0xFFF59E0B), Color(0xFF7C2D12), "rust", true),

        "swift" to ProgrammingBadgeSpec("Sw", Color(0xFFF05138), Color(0xFFFB923C), Color(0xFF9A3412), "swift", true),
        "prog_swift" to ProgrammingBadgeSpec("Sw", Color(0xFFF05138), Color(0xFFFB923C), Color(0xFF9A3412), "swift", true),

        "php" to ProgrammingBadgeSpec("PHP", Color(0xFF777BB4), Color(0xFF6366F1), Color(0xFF3730A3), "php", true),
        "prog_php" to ProgrammingBadgeSpec("PHP", Color(0xFF777BB4), Color(0xFF6366F1), Color(0xFF3730A3), "php", true),

        "c#" to ProgrammingBadgeSpec("C#", Color(0xFF512BD4), Color(0xFF9333EA), Color(0xFF5B21B6), "csharp", true),
        "csharp" to ProgrammingBadgeSpec("C#", Color(0xFF512BD4), Color(0xFF9333EA), Color(0xFF5B21B6), "csharp", true),
        "cs" to ProgrammingBadgeSpec("C#", Color(0xFF512BD4), Color(0xFF9333EA), Color(0xFF5B21B6), "csharp", true),
        "prog_csharp" to ProgrammingBadgeSpec("C#", Color(0xFF512BD4), Color(0xFF9333EA), Color(0xFF5B21B6), "csharp", true),

        "dart" to ProgrammingBadgeSpec("Dt", Color(0xFF0175C2), Color(0xFF13B9FD), Color(0xFF0369A1), "dart", true),
        "flutter" to ProgrammingBadgeSpec("Fl", Color(0xFF02569B), Color(0xFF54C5F8), Color(0xFF0369A1), "flutter", true),
        "prog_dart" to ProgrammingBadgeSpec("Dt", Color(0xFF0175C2), Color(0xFF13B9FD), Color(0xFF0369A1), "dart", true),

        "c" to ProgrammingBadgeSpec("C", Color(0xFF3949AB), Color(0xFF60A5FA), Color(0xFF1E3A8A), "c", true),
        "prog_c" to ProgrammingBadgeSpec("C", Color(0xFF3949AB), Color(0xFF60A5FA), Color(0xFF1E3A8A), "c", true)
    )

    // Unambiguous full names / prog_ prefixes that NEVER collide with 2-letter ISO country codes
    private val unambiguousProgrammingKeys = setOf(
        "python", "javascript", "typescript", "kotlin", "cplusplus", "c++",
        "csharp", "c#", "postgresql", "html5", "css3", "html/css", "html / css",
        "golang", "go (golang)", "r programming", "shell/bash", "shell / bash",
        "flutter", "dart", "rust", "ruby", "swift", "scala", "php", "sql", "bash"
    )

    fun findSpec(languageOrCode: String, allowShortCodes: Boolean = true): ProgrammingBadgeSpec? {
        val clean = languageOrCode.trim().lowercase()
        if (clean.isBlank()) return null

        val base = clean.substringBefore("(").trim()

        if (!allowShortCodes && !clean.startsWith("prog_")) {
            specs[clean]?.let { spec ->
                if (clean in unambiguousProgrammingKeys) return spec
            }
            specs[base]?.let { spec ->
                if (base in unambiguousProgrammingKeys) return spec
            }
            return null
        }

        specs[clean]?.let { return it }
        specs[base]?.let { return it }

        // Only match explicit prefixed forms or multi-character exact words, never arbitrary substrings like "c" in "canada"/"cn"
        for ((key, spec) in specs) {
            if (key.length >= 3 && (clean == key || base == key || clean.startsWith("$key ") || clean.contains("($key)"))) {
                return spec
            }
        }
        return null
    }

    fun isProgramming(languageOrCode: String, allowShortCodes: Boolean = true): Boolean {
        val clean = languageOrCode.trim().lowercase()
        if (clean.startsWith("prog_")) return true
        return findSpec(clean, allowShortCodes = allowShortCodes) != null
    }
}

/**
 * Universal LanguageIcon composable used across the app.
 *
 * Rules:
 *  - Spoken languages: keep the country flag (via CircleFlag).
 *  - Programming languages: NEVER show a country flag.
 *  - Renders a crisp frosted/gradient rounded badge with high-contrast Devicon SVG + stylized fallback.
 */
@Composable
fun LanguageIcon(
    language: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(9.dp),
    contentDescription: String? = null
) {
    val clean = language.trim()
    val isHttp = clean.startsWith("http://") || clean.startsWith("https://")
    val spec = ProgrammingLanguagesRegistry.findSpec(clean, allowShortCodes = true)

    if (spec != null || isHttp) {
        val context = LocalContext.current

        val badgeSpec = spec ?: ProgrammingBadgeSpec(
            letters = clean.take(2).uppercase(),
            brandColor = getStableColorForName(clean),
            secondaryColor = getStableColorForName(clean).copy(alpha = 0.7f),
            textColor = Color.White
        )

        var svgLoadFailed by remember(clean) { mutableStateOf(false) }
        val deviconUrl = if (isHttp) clean else badgeSpec.deviconSlug?.let {
            "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/$it/$it-original.svg"
        }

        val bgBrush = if (badgeSpec.useLightCard) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    badgeSpec.brandColor.copy(alpha = 0.14f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    badgeSpec.brandColor,
                    badgeSpec.secondaryColor
                )
            )
        }

        val borderColor = if (badgeSpec.useLightCard) {
            badgeSpec.brandColor.copy(alpha = 0.42f)
        } else {
            Color.White.copy(alpha = 0.28f)
        }

        Box(
            modifier = modifier
                .clip(shape)
                .background(bgBrush)
                .border(1.dp, borderColor, shape),
            contentAlignment = Alignment.Center
        ) {
            // Subtle glossy top highlight
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (badgeSpec.useLightCard) 0.5f else 0.22f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height * 0.45f),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }

            if (deviconUrl != null && !svgLoadFailed) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(deviconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription ?: clean,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(0.72f),
                    onError = {
                        svgLoadFailed = true
                    }
                )
            } else {
                // Crisp fallback badge with angled code stroke accent + monospace emblem
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = badgeSpec.secondaryColor.copy(alpha = 0.3f),
                        startAngle = -35f,
                        sweepAngle = 110f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = badgeSpec.letters,
                    fontSize = if (badgeSpec.letters.length >= 3) 9.sp else 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = badgeSpec.textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // Spoken language -> country flag
        CircleFlag(
            countryCode = clean,
            modifier = modifier,
            contentDescription = contentDescription,
            allowProgramming = false
        )
    }
}
