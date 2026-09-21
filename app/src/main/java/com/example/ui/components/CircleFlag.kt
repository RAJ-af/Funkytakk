package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
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
    return clean.startsWith("prog_") || clean in setOf(
        "python", "javascript", "typescript", "kotlin", "java",
        "cpp", "cplusplus", "csharp", "rust", "golang", "go",
        "swift", "dart", "php", "ruby", "sql", "html", "css", "scala", "shell", "bash"
    )
}

fun getFlagOrDeviconUrl(code: String): String {
    val clean = code.lowercase().trim()
    return when (clean) {
        "prog_python", "python", "py" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/python/python-original.svg"
        "prog_javascript", "javascript", "js" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/javascript/javascript-original.svg"
        "prog_typescript", "typescript", "ts" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/typescript/typescript-original.svg"
        "prog_kotlin", "kotlin", "kt" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/kotlin/kotlin-original.svg"
        "prog_java", "java" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/java/java-original.svg"
        "prog_cpp", "cpp", "cplusplus", "c++" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/cplusplus/cplusplus-original.svg"
        "prog_csharp", "csharp", "cs", "c#" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/csharp/csharp-original.svg"
        "prog_c", "c" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/c/c-original.svg"
        "prog_rust", "rust", "rs" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/rust/rust-original.svg"
        "prog_go", "golang", "go" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/go/go-original.svg"
        "prog_swift", "swift" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/swift/swift-original.svg"
        "prog_dart", "dart", "flutter" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/dart/dart-original.svg"
        "prog_php", "php" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/php/php-original.svg"
        "prog_ruby", "ruby", "rb" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/ruby/ruby-original.svg"
        "prog_sql", "sql" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/mysql/mysql-original.svg"
        "prog_html", "html", "html5" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/html5/html5-original.svg"
        "prog_css", "css", "css3" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/css3/css3-original.svg"
        "prog_r", "r" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/r/r-original.svg"
        "prog_scala", "scala" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/scala/scala-original.svg"
        "prog_shell", "bash", "sh" -> "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/bash/bash-original.svg"
        else -> "https://hatscripts.github.io/circle-flags/flags/${clean}.svg"
    }
}

@Composable
fun CircleFlag(
    countryCode: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(SvgDecoder.Factory())
        }
        .build()

    val cleanCode = if (countryCode.any { it.code >= 0x1F1E6 }) {
        flagEmojiToCountryCode(countryCode)
    } else {
        countryCode.lowercase().trim()
    }

    val isProg = isProgrammingCode(cleanCode)

    if (isProg) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9))
                .border(0.5.dp, Color(0xFFE2E8F0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(getFlagOrDeviconUrl(cleanCode))
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("https://hatscripts.github.io/circle-flags/flags/${cleanCode}.svg")
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
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
    Box(
        modifier = modifier.size(avatarSize),
        contentAlignment = Alignment.BottomEnd
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .border(1.5.dp, Color.White, CircleShape)
        )
        CircleFlag(
            countryCode = countryCode.trim(),
            modifier = Modifier
                .size(flagSize)
                .clip(CircleShape)
                .border(1.5.dp, Color.White, CircleShape)
        )
    }
}
