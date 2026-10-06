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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.StoreItemRow
import com.example.store.CosmeticsManager

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
    onStickerSelect: ((packId: Long, emoji: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalChatColors.current
    var pickerMode by remember { mutableStateOf(0) } // 0 = Emojis, 1 = Stickers
    var selectedCategory by remember { mutableStateOf(EmojiCategory.SMILEYS) }
    var searchQuery by remember { mutableStateOf("") }

    var ownedStickerPacks by remember { mutableStateOf<List<StoreItemRow>>(emptyList()) }
    var isLoadingStickers by remember { mutableStateOf(false) }

    LaunchedEffect(pickerMode) {
        if (pickerMode == 1) {
            isLoadingStickers = true
            val res = CosmeticsManager.getStoreItems(context, type = "sticker_pack", ownedOnly = true)
            ownedStickerPacks = res.getOrDefault(emptyList())
            isLoadingStickers = false
        }
    }

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
        // Mode Selector: Emojis vs Stickers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = colors.surfaceVariant,
                modifier = Modifier.padding(2.dp)
            ) {
                Row {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (pickerMode == 0) colors.brand else Color.Transparent)
                            .clickable { pickerMode = 0 }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Emojis 😀",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pickerMode == 0) colors.onBrand else colors.textSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (pickerMode == 1) colors.brand else Color.Transparent)
                            .clickable { pickerMode = 1 }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Stickers 🎁",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pickerMode == 1) colors.onBrand else colors.textSecondary
                        )
                    }
                }
            }
        }

        // Divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.hairline)
        )

        if (pickerMode == 0) {
            // Category tabs row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
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
        } else {
            // Stickers Mode
            if (isLoadingStickers) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.brand, modifier = Modifier.size(24.dp))
                }
            } else if (ownedStickerPacks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎁", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("No owned sticker packs", fontSize = 13.sp, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Visit the Funky Store in Profile to unlock animated stickers!",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                val packEmojis = remember(ownedStickerPacks) {
                    ownedStickerPacks.flatMap { pack ->
                        val packId = pack.id
                        val emojis = parseStickerPackEmojis(pack.config)
                        emojis.map { emoji -> Pair(packId, emoji) }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 56.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(packEmojis) { (packId, emoji) ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    onStickerSelect?.invoke(packId, emoji)
                                        ?: onEmojiSelect(emoji)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 32.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun parseStickerPackEmojis(config: Map<String, Any?>?): List<String> {
    if (config == null) return listOf("😍", "🔥", "❤️", "✨", "🎉", "👍", "😎", "🥳")
    val list = (config["emojis"] as? List<*>)?.mapNotNull { it?.toString() }
    return if (!list.isNullOrEmpty()) list else listOf("😍", "🔥", "❤️", "✨", "🎉", "👍", "😎", "🥳")
}
