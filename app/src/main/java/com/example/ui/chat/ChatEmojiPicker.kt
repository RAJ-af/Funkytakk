package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class EmojiCategory(val label: String, val iconEmoji: String) {
    SMILEYS("Smileys", "😀"),
    GESTURES("Hands", "👍"),
    HEARTS("Hearts", "❤️"),
    ANIMALS("Nature", "🐶"),
    FOOD("Food", "☕"),
    ACTIVITIES("Fun", "🎉"),
    OBJECTS("Things", "🔥"),
    TRAVEL("Travel", "🚀")
}

private val EmojiDataset = mapOf(
    EmojiCategory.SMILEYS to listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥲", "🥹", "☺️", "😊",
        "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "😋",
        "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥸", "🤩", "🥳", "😏",
        "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣", "😖", "😫", "😩", "🥺",
        "😢", "😭", "😮‍💨", "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱",
        "😨", "😰", "😥", "😓", "🤗", "🤔", "🫣", "🤭", "🫢", "🫡", "🤫", "🫠",
        "🤥", "😶", "😐", "😑", "🫥", "😯", "😦", "😧", "😮", "😲", "🥱", "😴",
        "🤤", "😪", "😵", "😵‍💫", "🤐", "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕"
    ),
    EmojiCategory.GESTURES to listOf(
        "👍", "👎", "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🤜", "🤛", "✊", "👊",
        "✌️", "🤞", "🫰", "🤟", "🤘", "👌", "🤌", "🤏", "👈", "👉", "👆", "👇",
        "☝️", "✋", "🤚", "🖐️", "🖖", "👋", "🤙", "🫵", "🖕", "✍️", "🙏", "💅",
        "🤳", "💪", "🫀", "🫁", "🧠", "👀", "👁️", "🧑‍💻", "🦸", "🦹", "🧙", "🧚"
    ),
    EmojiCategory.HEARTS to listOf(
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❤️‍🔥", "❤️‍🩹",
        "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "✨", "⭐", "🌟",
        "💫", "⚡", "💥", "💯", "💢", "🕊️", "🌹", "🌸", "💐", "🌺", "🌷", "🌻"
    ),
    EmojiCategory.ANIMALS to listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮",
        "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🐤", "🦆", "🦅", "🦉", "🦇", "🐺",
        "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞", "🐜", "🦟", "🐢", "🐍", "🐙"
    ),
    EmojiCategory.FOOD to listOf(
        "☕", "🍵", "🧋", "🥤", "🍺", "🍻", "🥂", "🍷", "🍹", "🍸", "🍕", "🍔",
        "🍟", "🌭", "🍿", "🧈", "🍳", "🧇", "🥞", "🍣", "🍱", "🍜", "🍲", "🍛",
        "🍚", "🍙", "🥟", "🍦", "🍧", "🍨", "🍩", "🍪", "🎂", "🍰", "🍫", "🍬"
    ),
    EmojiCategory.ACTIVITIES to listOf(
        "🎉", "🎊", "🎈", "🎆", "🎇", "🪄", "🪅", "⚽", "🏀", "🏈", "⚾", "🎾",
        "🏐", "🏉", "🥏", "🎱", "🏓", "🏸", "🥊", "🥋", "🎮", "🕹️", "🎲", "♟️",
        "🎭", "🎨", "🎬", "🎤", "🎧", "🎼", "🎹", "🥁", "🎷", "🎺", "🎸", "🪕"
    ),
    EmojiCategory.OBJECTS to listOf(
        "🔥", "💡", "💎", "👑", "🎯", "🚀", "📱", "💻", "📷", "🎥", "🎧", "🎵",
        "🎶", "🔔", "🔑", "🔒", "🎁", "🏆", "🥇", "🥈", "🥉", "🏅", "🎖️", "💰",
        "💵", "💳", "📦", "📫", "✉️", "📩", "📝", "📌", "📍", "⏰", "⏱️", "⏳"
    ),
    EmojiCategory.TRAVEL to listOf(
        "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "✈️", "🚀", "🏝️",
        "🏖️", "⛺", "🏕️", "🏠", "🏡", "🏢", "🏰", "🗼", "🗽", "⛲", "🎡", "🎢"
    )
)

@Composable
fun FluentEmojiPicker(
    onEmojiSelect: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    var selectedCategory by remember { mutableStateOf(EmojiCategory.SMILEYS) }
    var searchQuery by remember { mutableStateOf("") }

    val currentEmojis = remember(selectedCategory, searchQuery) {
        if (searchQuery.isBlank()) {
            EmojiDataset[selectedCategory] ?: emptyList()
        } else {
            val q = searchQuery.trim()
            EmojiDataset.values.flatten().distinct().filter { emoji ->
                emoji.contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .background(colors.surface)
    ) {
        // Divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.hairline)
        )

        // Category tabs row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                EmojiCategory.entries.forEach { cat ->
                    val isSelected = selectedCategory == cat && searchQuery.isBlank()
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) colors.brand.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable {
                                selectedCategory = cat
                                searchQuery = ""
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(cat.iconEmoji, fontSize = 18.sp)
                    }
                }
            }

            // Backspace button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .clickable(onClick = onBackspace),
                contentAlignment = Alignment.Center
            ) {
                ChatIconClose(modifier = Modifier.size(16.dp), tint = colors.text)
            }
        }

        // Emoji Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 42.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 6.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(currentEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEmojiSelect(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 24.sp)
                }
            }
        }
    }
}
