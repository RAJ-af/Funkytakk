package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R

val PresetAvatarList = listOf(
    Pair(R.drawable.male_avatar_1781324792347, "avatar_male_1"),
    Pair(R.drawable.female_avatar_1781324805233, "avatar_female_1"),
    Pair(R.drawable.cool_avatar_1781324820431, "avatar_cool_1"),
    Pair(R.drawable.a_cutout_png_style_portrait_image_on_a_transparent, "avatar_portrait_1")
)

fun resolveAvatarModel(avatar: String?): Any {
    val clean = avatar?.trim().orEmpty()
    return when {
        clean.isBlank() -> R.drawable.male_avatar_1781324792347
        clean == "avatar_male_1" -> R.drawable.male_avatar_1781324792347
        clean == "avatar_female_1" -> R.drawable.female_avatar_1781324805233
        clean == "avatar_cool_1" -> R.drawable.cool_avatar_1781324820431
        clean == "avatar_portrait_1" -> R.drawable.a_cutout_png_style_portrait_image_on_a_transparent
        clean.startsWith("http://") || clean.startsWith("https://") -> clean
        clean.startsWith("content://") || clean.startsWith("file://") || clean.startsWith("/") -> clean
        else -> clean
    }
}

@Composable
fun UserAvatarImage(
    avatarUrl: String?,
    contentDescription: String? = "User Avatar",
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val model = resolveAvatarModel(avatarUrl)
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageRequest = androidx.compose.runtime.remember(model) {
        coil.request.ImageRequest.Builder(context)
            .data(model)
            .crossfade(true)
            .memoryCacheKey(model.toString())
            .diskCacheKey(model.toString())
            .placeholder(R.drawable.male_avatar_1781324792347)
            .error(R.drawable.male_avatar_1781324792347)
            .fallback(R.drawable.male_avatar_1781324792347)
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
