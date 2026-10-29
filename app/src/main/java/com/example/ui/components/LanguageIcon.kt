package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

data class ProgrammingBadgeSpec(
    val letters: String,
    val brandColor: Color,
    val textColor: Color = Color.White,
    val deviconSlug: String? = null
)

object ProgrammingLanguagesRegistry {
    private val specs = mapOf(
        "python" to ProgrammingBadgeSpec("Py", Color(0xFF3776AB), Color.White, "python"),
        "py" to ProgrammingBadgeSpec("Py", Color(0xFF3776AB), Color.White, "python"),
        "prog_python" to ProgrammingBadgeSpec("Py", Color(0xFF3776AB), Color.White, "python"),

        "javascript" to ProgrammingBadgeSpec("JS", Color(0xFFF7DF1E), Color.Black, "javascript"),
        "js" to ProgrammingBadgeSpec("JS", Color(0xFFF7DF1E), Color.Black, "javascript"),
        "prog_javascript" to ProgrammingBadgeSpec("JS", Color(0xFFF7DF1E), Color.Black, "javascript"),

        "typescript" to ProgrammingBadgeSpec("TS", Color(0xFF3178C6), Color.White, "typescript"),
        "ts" to ProgrammingBadgeSpec("TS", Color(0xFF3178C6), Color.White, "typescript"),
        "prog_typescript" to ProgrammingBadgeSpec("TS", Color(0xFF3178C6), Color.White, "typescript"),

        "kotlin" to ProgrammingBadgeSpec("Kt", Color(0xFF7F52FF), Color.White, "kotlin"),
        "kt" to ProgrammingBadgeSpec("Kt", Color(0xFF7F52FF), Color.White, "kotlin"),
        "prog_kotlin" to ProgrammingBadgeSpec("Kt", Color(0xFF7F52FF), Color.White, "kotlin"),

        "java" to ProgrammingBadgeSpec("Jv", Color(0xFFED8B00), Color.White, "java"),
        "jv" to ProgrammingBadgeSpec("Jv", Color(0xFFED8B00), Color.White, "java"),
        "prog_java" to ProgrammingBadgeSpec("Jv", Color(0xFFED8B00), Color.White, "java"),

        "c++" to ProgrammingBadgeSpec("C++", Color(0xFF00599C), Color.White, "cplusplus"),
        "cpp" to ProgrammingBadgeSpec("C++", Color(0xFF00599C), Color.White, "cplusplus"),
        "cplusplus" to ProgrammingBadgeSpec("C++", Color(0xFF00599C), Color.White, "cplusplus"),
        "prog_cpp" to ProgrammingBadgeSpec("C++", Color(0xFF00599C), Color.White, "cplusplus"),

        "ruby" to ProgrammingBadgeSpec("Rb", Color(0xFFCC342D), Color.White, "ruby"),
        "rb" to ProgrammingBadgeSpec("Rb", Color(0xFFCC342D), Color.White, "ruby"),
        "prog_ruby" to ProgrammingBadgeSpec("Rb", Color(0xFFCC342D), Color.White, "ruby"),

        "sql" to ProgrammingBadgeSpec("SQL", Color(0xFF336791), Color.White, "postgresql"),
        "postgresql" to ProgrammingBadgeSpec("SQL", Color(0xFF336791), Color.White, "postgresql"),
        "prog_sql" to ProgrammingBadgeSpec("SQL", Color(0xFF336791), Color.White, "postgresql"),

        "html/css" to ProgrammingBadgeSpec("H5", Color(0xFFE34F26), Color.White, "html5"),
        "html" to ProgrammingBadgeSpec("H5", Color(0xFFE34F26), Color.White, "html5"),
        "css" to ProgrammingBadgeSpec("H5", Color(0xFFE34F26), Color.White, "css3"),
        "prog_html" to ProgrammingBadgeSpec("H5", Color(0xFFE34F26), Color.White, "html5"),
        "prog_css" to ProgrammingBadgeSpec("H5", Color(0xFFE34F26), Color.White, "css3"),

        "r" to ProgrammingBadgeSpec("R", Color(0xFF276DC3), Color.White, "r"),
        "prog_r" to ProgrammingBadgeSpec("R", Color(0xFF276DC3), Color.White, "r"),

        "scala" to ProgrammingBadgeSpec("Sc", Color(0xFFDC322F), Color.White, "scala"),
        "prog_scala" to ProgrammingBadgeSpec("Sc", Color(0xFFDC322F), Color.White, "scala"),

        "shell/bash" to ProgrammingBadgeSpec("sh", Color(0xFF4EAA25), Color.White, "bash"),
        "shell" to ProgrammingBadgeSpec("sh", Color(0xFF4EAA25), Color.White, "bash"),
        "bash" to ProgrammingBadgeSpec("sh", Color(0xFF4EAA25), Color.White, "bash"),
        "sh" to ProgrammingBadgeSpec("sh", Color(0xFF4EAA25), Color.White, "bash"),
        "prog_shell" to ProgrammingBadgeSpec("sh", Color(0xFF4EAA25), Color.White, "bash"),

        "go" to ProgrammingBadgeSpec("Go", Color(0xFF00ADD8), Color.White, "go"),
        "golang" to ProgrammingBadgeSpec("Go", Color(0xFF00ADD8), Color.White, "go"),
        "prog_go" to ProgrammingBadgeSpec("Go", Color(0xFF00ADD8), Color.White, "go"),

        "rust" to ProgrammingBadgeSpec("Rs", Color(0xFFCE412B), Color.White, "rust"),
        "rs" to ProgrammingBadgeSpec("Rs", Color(0xFFCE412B), Color.White, "rust"),
        "prog_rust" to ProgrammingBadgeSpec("Rs", Color(0xFFCE412B), Color.White, "rust"),

        "swift" to ProgrammingBadgeSpec("Sw", Color(0xFFF05138), Color.White, "swift"),
        "prog_swift" to ProgrammingBadgeSpec("Sw", Color(0xFFF05138), Color.White, "swift"),

        "php" to ProgrammingBadgeSpec("PHP", Color(0xFF777BB4), Color.White, "php"),
        "prog_php" to ProgrammingBadgeSpec("PHP", Color(0xFF777BB4), Color.White, "php"),

        "c#" to ProgrammingBadgeSpec("C#", Color(0xFF512BD4), Color.White, "csharp"),
        "csharp" to ProgrammingBadgeSpec("C#", Color(0xFF512BD4), Color.White, "csharp"),
        "cs" to ProgrammingBadgeSpec("C#", Color(0xFF512BD4), Color.White, "csharp"),
        "prog_csharp" to ProgrammingBadgeSpec("C#", Color(0xFF512BD4), Color.White, "csharp"),

        "dart" to ProgrammingBadgeSpec("Dt", Color(0xFF0175C2), Color.White, "dart"),
        "flutter" to ProgrammingBadgeSpec("Dt", Color(0xFF0175C2), Color.White, "flutter"),
        "prog_dart" to ProgrammingBadgeSpec("Dt", Color(0xFF0175C2), Color.White, "dart"),

        "c" to ProgrammingBadgeSpec("C", Color(0xFF5C6BC0), Color.White, "c"),
        "prog_c" to ProgrammingBadgeSpec("C", Color(0xFF5C6BC0), Color.White, "c")
    )

    fun findSpec(languageOrCode: String): ProgrammingBadgeSpec? {
        val clean = languageOrCode.trim().lowercase()
        specs[clean]?.let { return it }

        // Match partial
        for ((key, spec) in specs) {
            if (clean == key || clean.startsWith("$key ") || clean.contains("($key)") || clean.contains(key)) {
                return spec
            }
        }
        return null
    }

    fun isProgramming(languageOrCode: String): Boolean {
        val clean = languageOrCode.trim().lowercase()
        if (clean.startsWith("prog_")) return true
        return findSpec(clean) != null
    }
}

/**
 * Universal LanguageIcon composable used across the app.
 *
 * Rules:
 *  - Spoken languages: keep the country flag (via CircleFlag).
 *  - Programming languages: NEVER show a country flag.
 *  - Step 1: Rounded-square badge with defined letters and brand color.
 *  - Step 2: Tries official Devicon SVG from CDN and falls back to the badge on error.
 */
@Composable
fun LanguageIcon(
    language: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    contentDescription: String? = null
) {
    val clean = language.trim()
    val isHttp = clean.startsWith("http://") || clean.startsWith("https://")
    val spec = ProgrammingLanguagesRegistry.findSpec(clean)

    if (spec != null || isHttp) {
        val context = LocalContext.current
        val imageLoader = remember(context) {
            ImageLoader.Builder(context)
                .components { add(SvgDecoder.Factory()) }
                .build()
        }

        val badgeSpec = spec ?: ProgrammingBadgeSpec(
            letters = clean.take(2).uppercase(),
            brandColor = getStableColorForName(clean),
            textColor = Color.White
        )

        var svgLoadFailed by remember(clean) { mutableStateOf(false) }
        val deviconUrl = if (isHttp) clean else badgeSpec.deviconSlug?.let {
            "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/$it/$it-original.svg"
        }

        Box(
            modifier = modifier
                .clip(shape)
                .background(badgeSpec.brandColor)
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), shape),
            contentAlignment = Alignment.Center
        ) {
            if (deviconUrl != null && !svgLoadFailed) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(deviconUrl)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = contentDescription ?: clean,
                    modifier = Modifier.fillMaxSize(0.72f),
                    onError = {
                        svgLoadFailed = true
                    }
                )
            } else {
                Text(
                    text = badgeSpec.letters,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
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
            contentDescription = contentDescription
        )
    }
}
