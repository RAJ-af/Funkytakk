package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Fluent Emoji Asset Provider
 * Sources official Microsoft Fluent 3D Emoji assets (via jsdelivr CDN / Github).
 */
object FluentEmojiAssets {
    private const val BASE_URL = "https://cdn.jsdelivr.net/gh/microsoft/fluentui-emoji@main/assets"

    val HEART = "$BASE_URL/Red%20heart/3D/red_heart_3d.png"
    val LAUGHING = "$BASE_URL/Face%20with%20tears%20of%20joy/3D/face_with_tears_of_joy_3d.png"
    val THUMBS_UP = "$BASE_URL/Thumbs%20up/Default/3D/thumbs_up_3d_default.png"
    val HUSHED = "$BASE_URL/Hushed%20face/3D/hushed_face_3d.png"
    val CRYING = "$BASE_URL/Crying%20face/3D/crying_face_3d.png"
    val FIRE = "$BASE_URL/Fire/3D/fire_3d.png"
    val PARTY_POPPER = "$BASE_URL/Party%20popper/3D/party_popper_3d.png"
    val STAR_STRUCK = "$BASE_URL/Star-struck/3D/star-struck_3d.png"
    val SPARKLES = "$BASE_URL/Sparkles/3D/sparkles_3d.png"
    val CLAPPING = "$BASE_URL/Clapping%20hands/Default/3D/clapping_hands_3d_default.png"
    val HEART_EYES = "$BASE_URL/Smiling%20face%20with%20heart-eyes/3D/smiling_face_with_heart-eyes_3d.png"

    fun getUrlForEmoji(emoji: String): String {
        return when (emoji.trim()) {
            "❤️", "heart" -> HEART
            "😂", "laugh" -> LAUGHING
            "👍", "thumbs_up", "thumbsup" -> THUMBS_UP
            "😮", "😲", "wow", "hushed" -> HUSHED
            "😢", "😭", "sad", "cry" -> CRYING
            "🔥", "fire" -> FIRE
            "🎉", "party" -> PARTY_POPPER
            "🤩", "star" -> STAR_STRUCK
            "✨", "sparkle" -> SPARKLES
            "👏", "clap" -> CLAPPING
            "😍", "love" -> HEART_EYES
            else -> HEART
        }
    }
}

@Composable
fun FluentEmojiIcon(
    emoji: String,
    modifier: Modifier = Modifier.size(24.dp),
    contentDescription: String? = null
) {
    val url = FluentEmojiAssets.getUrlForEmoji(emoji)
    AsyncImage(
        model = url,
        contentDescription = contentDescription ?: emoji,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
