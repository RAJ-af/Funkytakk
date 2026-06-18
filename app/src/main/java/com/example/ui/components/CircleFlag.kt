package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
