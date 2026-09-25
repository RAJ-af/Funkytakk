package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Fluent Emoji Asset Provider
 * Sources official Microsoft Fluent 3D Emoji assets.
 */
object FluentEmojiAssets {
    private const val BASE_URL = "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets"

    val ALL_EMOJIS = listOf(
        "$BASE_URL/Smiling%20face%20with%20hearts/3D/smiling_face_with_hearts_3d.png" to "🥰",
        "$BASE_URL/Smiling%20face%20with%20sunglasses/3D/smiling_face_with_sunglasses_3d.png" to "😎",
        "$BASE_URL/Fire/3D/fire_3d.png" to "🔥",
        "$BASE_URL/Partying%20face/3D/partying_face_3d.png" to "🥳",
        "$BASE_URL/Red%20heart/3D/red_heart_3d.png" to "❤️",
        "$BASE_URL/Face%20with%20tears%20of%20joy/3D/face_with_tears_of_joy_3d.png" to "😂",
        "$BASE_URL/Sparkles/3D/sparkles_3d.png" to "✨",
        "$BASE_URL/Party%20popper/3D/party_popper_3d.png" to "🎉",
        "$BASE_URL/Star-struck/3D/star-struck_3d.png" to "🤩",
        "$BASE_URL/Ghost/3D/ghost_3d.png" to "👻",
        "$BASE_URL/Thumbs%20up/Default/3D/thumbs_up_3d_default.png" to "👍",
        "$BASE_URL/Clapping%20hands/Default/3D/clapping_hands_3d_default.png" to "👏",
        "$BASE_URL/Hushed%20face/3D/hushed_face_3d.png" to "😮",
        "$BASE_URL/Crying%20face/3D/crying_face_3d.png" to "😭"
    )

    fun getUrlForEmoji(emoji: String): String {
        val trimmed = emoji.trim()
        val found = ALL_EMOJIS.firstOrNull { it.second == trimmed }
        if (found != null) return found.first

        return when (trimmed) {
            "❤️", "heart" -> "$BASE_URL/Red%20heart/3D/red_heart_3d.png"
            "😂", "laugh" -> "$BASE_URL/Face%20with%20tears%20of%20joy/3D/face_with_tears_of_joy_3d.png"
            "👍", "thumbs_up", "thumbsup" -> "$BASE_URL/Thumbs%20up/Default/3D/thumbs_up_3d_default.png"
            "😮", "😲", "wow", "hushed" -> "$BASE_URL/Hushed%20face/3D/hushed_face_3d.png"
            "😢", "😭", "sad", "cry" -> "$BASE_URL/Crying%20face/3D/crying_face_3d.png"
            "🔥", "fire" -> "$BASE_URL/Fire/3D/fire_3d.png"
            "🎉", "party" -> "$BASE_URL/Party%20popper/3D/party_popper_3d.png"
            "🤩", "star" -> "$BASE_URL/Star-struck/3D/star-struck_3d.png"
            "✨", "sparkle" -> "$BASE_URL/Sparkles/3D/sparkles_3d.png"
            "👏", "clap" -> "$BASE_URL/Clapping%20hands/Default/3D/clapping_hands_3d_default.png"
            "😍", "love" -> "$BASE_URL/Smiling%20face%20with%20hearts/3D/smiling_face_with_hearts_3d.png"
            "😎" -> "$BASE_URL/Smiling%20face%20with%20sunglasses/3D/smiling_face_with_sunglasses_3d.png"
            "🥳" -> "$BASE_URL/Partying%20face/3D/partying_face_3d.png"
            "👻" -> "$BASE_URL/Ghost/3D/ghost_3d.png"
            else -> "$BASE_URL/Red%20heart/3D/red_heart_3d.png"
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

/**
 * Emoji picker tray that slides up/down when tapping the smiley button.
 */
@Composable
fun FluentEmojiPickerTray(
    visible: Boolean,
    onEmojiSelected: (unicode: String, url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color(0xFFF3F4F6))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "3D Fluent Emojis",
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "Tap to insert",
                        fontSize = 11.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(FluentEmojiAssets.ALL_EMOJIS) { (url, unicode) ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF9FAFB))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                                .clickable { onEmojiSelected(unicode, url) }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = unicode,
                                modifier = Modifier.size(34.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Inline Text with 3D Fluent Emojis
 */
@Composable
fun FluentEmojiRenderedText(
    text: String,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val emojis = FluentEmojiAssets.ALL_EMOJIS

    // Map each unicode to inline content
    val inlineContentMap = remember(emojis) {
        emojis.associate { (url, unicode) ->
            unicode to InlineTextContent(
                Placeholder(
                    width = 22.sp,
                    height = 22.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = unicode,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    // Build annotated string replacing known unicodes with inline markers
    val annotatedString = remember(text, emojis) {
        buildAnnotatedString {
            var index = 0
            while (index < text.length) {
                var matched = false
                for ((_, unicode) in emojis) {
                    if (text.startsWith(unicode, index)) {
                        appendInlineContent(unicode, unicode)
                        index += unicode.length
                        matched = true
                        break
                    }
                }
                if (!matched) {
                    append(text[index])
                    index++
                }
            }
        }
    }

    Text(
        text = annotatedString,
        inlineContent = inlineContentMap,
        style = style,
        color = color,
        modifier = modifier
    )
}
