package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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

fun isProgrammingCode(code: String, allowShortCodes: Boolean = false): Boolean {
    val clean = code.lowercase().trim()
    if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("prog_")) return true
    val fullNames = setOf(
        "python", "javascript", "typescript", "kotlin",
        "cpp", "cplusplus", "c++", "csharp", "c#", "rust", "golang",
        "swift", "dart", "flutter", "php", "ruby", "sql", "postgresql",
        "html", "css", "html5", "css3", "html/css", "html / css", "scala", "shell", "bash", "shell/bash", "shell / bash",
        "r programming", "go (golang)"
    )
    if (clean in fullNames) return true
    if (allowShortCodes) {
        return clean in setOf("c", "r", "go", "py", "js", "ts", "kt", "rb", "rs", "cs", "sh", "java")
    }
    return false
}

fun getProgrammingIconUrl(languageOrCode: String, allowShortCodes: Boolean = false): String {
    val clean = languageOrCode.trim().lowercase()
    if (clean.startsWith("http://") || clean.startsWith("https://")) return languageOrCode
    val spec = ProgrammingLanguagesRegistry.findSpec(clean, allowShortCodes = allowShortCodes)
    return spec?.deviconSlug?.let { slug ->
        "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/$slug/$slug-original.svg"
    } ?: ""
}

fun getFlagOrDeviconUrl(code: String): String {
    val clean = code.lowercase().trim()
    if (clean.startsWith("http://") || clean.startsWith("https://")) return code
    val devicon = getProgrammingIconUrl(clean, allowShortCodes = false)
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
    if (clean.length == 2 && clean.all { it.isLetter() }) return clean
    return when (clean) {
        "india", "hindi" -> "in"
        "korea", "south korea", "republic of korea", "south_korea", "korean" -> "kr"
        "japan", "japanese" -> "jp"
        "malaysia", "malay" -> "my"
        "albania", "albanian" -> "al"
        "united arab emirates", "uae", "arabic" -> "ae"
        "united states", "usa", "us", "united states of america", "english" -> "us"
        "united kingdom", "uk", "great britain" -> "gb"
        "canada" -> "ca"
        "cameroon" -> "cm"
        "central african republic" -> "cf"
        "chile" -> "cl"
        "australia" -> "au"
        "germany", "german" -> "de"
        "france", "french" -> "fr"
        "spain", "spanish" -> "es"
        "italy", "italian" -> "it"
        "china", "chinese", "mandarin" -> "cn"
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
        "croatia", "croatian" -> "hr"
        "czech", "czechia", "czech republic" -> "cz"
        "denmark", "danish" -> "dk"
        "divehi" -> "mv"
        "finland", "finnish" -> "fi"
        "greece", "greek" -> "gr"
        "israel", "hebrew" -> "il"
        "hungary", "hungarian" -> "hu"
        "norway", "norwegian" -> "no"
        "romania", "romanian" -> "ro"
        "slovakia", "slovak" -> "sk"
        "serbia", "serbian" -> "rs"
        "paraguay" -> "py"
        "afar" -> "et"
        else -> "un"
    }
}

@Composable
fun CircleFlag(
    countryCode: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    allowProgramming: Boolean = false
) {
    val context = LocalContext.current
    val trimmed = countryCode.trim()

    val shouldRenderProgramming = ProgrammingLanguagesRegistry.isProgramming(
        trimmed,
        allowShortCodes = allowProgramming
    ) || isProgrammingCode(trimmed, allowShortCodes = allowProgramming)

    if (shouldRenderProgramming) {
        LanguageIcon(
            language = trimmed,
            modifier = modifier,
            contentDescription = contentDescription
        )
        return
    }

    val rawCode = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else if (trimmed.any { it.code >= 0x1F1E6 }) {
        flagEmojiToCountryCode(trimmed)
    } else {
        countryNameToCode(trimmed)
    }
    val cleanCode = if (rawCode.isBlank()) "un" else rawCode

    val flagUrl = if (cleanCode.startsWith("http://") || cleanCode.startsWith("https://")) {
        cleanCode
    } else {
        "https://hatscripts.github.io/circle-flags/flags/${cleanCode}.svg"
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(flagUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier
    )
}

@Composable
fun AvatarWithCircularFlag(
    avatarUrl: String,
    countryCode: String,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 48.dp,
    flagSize: Dp = 18.dp
) {
    PresenceAvatar(
        avatarUrl = avatarUrl,
        size = avatarSize,
        isOnline = true,
        countryCode = countryCode,
        showFlagBadge = false,
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
    }
}
