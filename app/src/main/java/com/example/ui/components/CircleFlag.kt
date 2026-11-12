package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

fun flagEmojiToCountryCode(emoji: String): String {
    if (emoji.length < 4) {
        if (emoji.length == 2 && emoji.all { it.isLetter() }) return emoji.lowercase()
        return "us"
    }
    return try {
        val firstChar = emoji.codePointAt(0)
        val secondChar = emoji.codePointAt(Character.charCount(firstChar))
        val c1 = (firstChar - 0x1F1E6 + 'a'.code).toChar()
        val c2 = (secondChar - 0x1F1E6 + 'a'.code).toChar()
        "$c1$c2".lowercase()
    } catch (e: Exception) {
        "us"
    }
}

fun isProgrammingCode(code: String): Boolean {
    val clean = code.lowercase().trim()
    return clean.startsWith("http") || clean.startsWith("prog_") || clean in setOf(
        "python", "javascript", "typescript", "kotlin", "java",
        "cpp", "cplusplus", "csharp", "c", "rust", "golang", "go",
        "swift", "dart", "php", "ruby", "sql", "postgresql", "html", "css", "html5", "r", "scala", "shell", "bash"
    )
}

fun getProgrammingIconUrl(languageOrCode: String): String {
    val clean = languageOrCode.trim().lowercase()
    if (clean.startsWith("http://") || clean.startsWith("https://")) return languageOrCode
    val slug = when {
        clean.contains("python") || clean == "py" || clean == "prog_python" -> "python"
        clean.contains("typescript") || clean == "ts" || clean == "prog_typescript" -> "typescript"
        clean.contains("javascript") || clean == "js" || clean == "prog_javascript" -> "javascript"
        clean.contains("kotlin") || clean == "kt" || clean == "prog_kotlin" -> "kotlin"
        clean.contains("java") && !clean.contains("script") || clean == "prog_java" -> "java"
        clean.contains("c++") || clean.contains("cpp") || clean.contains("cplusplus") || clean == "prog_cpp" -> "cplusplus"
        clean.contains("c#") || clean.contains("csharp") || clean == "cs" || clean == "prog_csharp" -> "csharp"
        clean == "c" || clean == "prog_c" -> "c"
        clean.contains("rust") || clean == "rs" || clean == "prog_rust" -> "rust"
        clean.contains("go") || clean == "golang" || clean == "prog_go" -> "go"
        clean.contains("swift") || clean == "prog_swift" -> "swift"
        clean.contains("dart") || clean.contains("flutter") || clean == "prog_dart" -> "dart"
        clean.contains("php") || clean == "prog_php" -> "php"
        clean.contains("ruby") || clean == "rb" || clean == "prog_ruby" -> "ruby"
        clean.contains("sql") || clean.contains("postgres") || clean.contains("mysql") || clean == "prog_sql" -> "postgresql"
        clean.contains("html") || clean.contains("css") || clean == "prog_html" || clean == "prog_css" -> "html5"
        clean == "r" || clean == "prog_r" -> "r"
        clean.contains("scala") || clean == "prog_scala" -> "scala"
        clean.contains("shell") || clean.contains("bash") || clean == "sh" || clean == "prog_shell" -> "bash"
        else -> null
    }
    return if (slug != null) {
        "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/$slug/$slug-original.svg"
    } else ""
}

fun getFlagOrDeviconUrl(code: String): String {
    val clean = code.lowercase().trim()
    if (clean.startsWith("http://") || clean.startsWith("https://")) return code
    val devicon = getProgrammingIconUrl(clean)
    if (devicon.isNotBlank()) return devicon
    return "https://hatscripts.github.io/circle-flags/flags/${clean}.svg"
}

fun getStableColorForName(name: String): Color {
    val palette = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFF06B6D4),
        Color(0xFFEC4899), Color(0xFF6366F1), Color(0xFF14B8A6)
    )
    val hash = kotlin.math.abs(name.hashCode())
    return palette[hash % palette.size]
}

fun countryNameToCode(country: String): String {
    val clean = country.trim().lowercase()
    if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
    if (clean.length == 2) return clean
    return when (clean) {
        "india", "hindi" -> "in"
        "korea", "south korea", "republic of korea", "south_korea", "korean" -> "kr"
        "japan", "japanese" -> "jp"
        "malaysia", "malay" -> "my"
        "albania", "albanian" -> "al"
        "united arab emirates", "uae", "arabic" -> "sa"
        "united states", "usa", "us", "united states of america", "english" -> "us"
        "united kingdom", "uk", "great britain" -> "gb"
        "canada" -> "ca"
        "australia" -> "au"
        "germany", "german" -> "de"
        "france", "french" -> "fr"
        "spain", "spanish" -> "es"
        "italy", "italian" -> "it"
        "china", "chinese" -> "cn"
        "brazil", "portuguese" -> "br"
        "russia", "russian" -> "ru"
        "saudi arabia" -> "sa"
        "indonesia", "indonesian" -> "id"
        "turkey", "turkish" -> "tr"
        "vietnam", "vietnamese" -> "vn"
        "thailand", "thai" -> "th"
        "philippines", "tagalog", "filipino" -> "ph"
        "bangladesh", "bengali" -> "bd"
        "pakistan", "urdu" -> "pk"
        "netherlands", "dutch" -> "nl"
        "poland", "polish" -> "pl"
        "ukraine", "ukrainian" -> "ua"
        "sweden", "swedish" -> "se"
        "switzerland" -> "ch"
        "egypt" -> "eg"
        "mexico" -> "mx"
        "colombia" -> "co"
        "argentina" -> "ar"
        "cornish" -> "gb"
        "corsican" -> "fr"
        "cree" -> "ca"
        "croatian" -> "hr"
        "czech" -> "cz"
        "danish" -> "dk"
        "divehi" -> "mv"
        "finnish" -> "fi"
        "greek" -> "gr"
        "hebrew" -> "il"
        "hungarian" -> "hu"
        "norwegian" -> "no"
        "romanian" -> "ro"
        "slovak" -> "sk"
        else -> "us"
    }
}

@Composable
fun CircleFlag(
    countryCode: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current

    val rawCode = if (countryCode.startsWith("http://") || countryCode.startsWith("https://")) {
        countryCode
    } else if (countryCode.any { it.code >= 0x1F1E6 }) {
        flagEmojiToCountryCode(countryCode)
    } else {
        countryNameToCode(countryCode)
    }
    val cleanCode = if (rawCode.isBlank()) "in" else rawCode

    val isHttp = cleanCode.startsWith("http://") || cleanCode.startsWith("https://")
    if (ProgrammingLanguagesRegistry.isProgramming(countryCode) || isProgrammingCode(countryCode)) {
        LanguageIcon(
            language = countryCode,
            modifier = modifier,
            contentDescription = contentDescription
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("https://hatscripts.github.io/circle-flags/flags/${cleanCode}.svg")
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}

@Composable
fun AvatarWithCircularFlag(
    avatarUrl: String,
    countryCode: String,
    modifier: Modifier = Modifier,
    avatarSize: androidx.compose.ui.unit.Dp = 48.dp,
    flagSize: androidx.compose.ui.unit.Dp = 18.dp
) {
    PresenceAvatar(
        avatarUrl = avatarUrl,
        size = avatarSize,
        isOnline = true,
        countryCode = countryCode,
        modifier = modifier
    )
}

@Composable
fun DiscoverSquareAvatarWithFlag(
    avatarUrl: String,
    countryCode: String,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 80.dp,
    flagSize: Dp = 26.dp
) {
    Box(
        modifier = modifier.size(avatarSize),
        contentAlignment = Alignment.Center
    ) {
        val avatarModel = resolveAvatarModel(avatarUrl)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarModel)
                .crossfade(true)
                .placeholder(com.example.R.drawable.male_avatar_1781324792347)
                .error(com.example.R.drawable.male_avatar_1781324792347)
                .fallback(com.example.R.drawable.male_avatar_1781324792347)
                .build(),
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
        )

        // Bottom-Right Circular Country Flag
        CircleFlag(
            countryCode = countryCode.trim().ifBlank { "in" },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
                .size(flagSize)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}
