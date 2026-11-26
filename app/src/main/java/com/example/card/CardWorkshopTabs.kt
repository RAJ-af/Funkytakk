package com.example.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.StoreItemRow
import com.example.store.CosmeticsConfigParser
import com.example.ui.theme.LocalFunkyColors
import kotlin.math.roundToInt

enum class WorkshopTab(val label: String) {
    STICKERS("Stickers"),
    STYLE("Style"),
    SHOW("Show"),
    REMIX("Remix")
}

enum class StickerSubTab(val label: String) {
    WORDS("Words"),
    EMOJI("Emoji"),
    PACKS("Packs")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkshopStickersTab(
    subTab: StickerSubTab,
    onSelectSubTab: (StickerSubTab) -> Unit,
    stickerCount: Int,
    stickerPacks: List<StoreItemRow>,
    isLoadingPacks: Boolean,
    packsError: String?,
    onRetryPacks: () -> Unit,
    onAddWordSticker: (String) -> Unit,
    onAddEmojiSticker: (String) -> Unit,
    onAddPackSticker: (StoreItemRow, Int, String) -> Unit,
    onLockedPackClick: (StoreItemRow) -> Unit,
    modifier: Modifier = Modifier
) {
    val funkyColors = LocalFunkyColors.current
    var customWord by remember { mutableStateOf("") }
    var customWordError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub-tab pills + live sticker counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StickerSubTab.entries.forEach { tab ->
                    val selected = subTab == tab
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (selected) funkyColors.ink else funkyColors.surface,
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = if (selected) funkyColors.ink else funkyColors.line
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("workshop_stickers_subtab_${tab.name.lowercase()}")
                            .clip(RoundedCornerShape(100.dp))
                            .clickable { onSelectSubTab(tab) }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) funkyColors.surface else funkyColors.ink
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(100.dp),
                color = if (stickerCount >= CardDesignDefaults.MAX_STICKERS) {
                    funkyColors.danger.copy(alpha = 0.14f)
                } else {
                    funkyColors.soft
                },
                border = BorderStroke(1.dp, funkyColors.line)
            ) {
                Text(
                    text = "$stickerCount/${CardDesignDefaults.MAX_STICKERS} stickers",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (stickerCount >= CardDesignDefaults.MAX_STICKERS) funkyColors.danger else funkyColors.ink,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        when (subTab) {
            StickerSubTab.WORDS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customWord,
                        onValueChange = { next ->
                            val clamped = next.take(CardDesignDefaults.MAX_STICKER_TEXT_LEN)
                            customWord = clamped
                            customWordError = if (CardTextSafetyFilter.containsBlockedWord(clamped)) {
                                "Please choose another word"
                            } else {
                                null
                            }
                        },
                        label = { Text("Custom word (max 14)") },
                        singleLine = true,
                        isError = customWordError != null,
                        supportingText = {
                            if (customWordError != null) {
                                Text(customWordError.orEmpty(), color = funkyColors.danger)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("workshop_custom_word_input")
                    )

                    Button(
                        onClick = {
                            val trimmed = customWord.trim()
                            if (trimmed.isEmpty()) return@Button
                            if (CardTextSafetyFilter.containsBlockedWord(trimmed)) {
                                customWordError = "Please choose another word"
                                return@Button
                            }
                            onAddWordSticker(trimmed)
                            customWord = ""
                            customWordError = null
                        },
                        enabled = customWord.isNotBlank() && customWordError == null,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB800),
                            contentColor = Color(0xFF1A1A1A)
                        ),
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .defaultMinSize(minWidth = 64.dp, minHeight = 52.dp)
                            .testTag("workshop_add_custom_word_btn")
                    ) {
                        Text("Add", fontWeight = FontWeight.Black)
                    }
                }

                Text(
                    text = "Tap a word to stick it on",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = funkyColors.mute
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CardDesignDefaults.wordPresets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = funkyColors.surface,
                            border = BorderStroke(1.8.dp, funkyColors.ink),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("workshop_word_preset_$preset")
                                .clip(RoundedCornerShape(100.dp))
                                .clickable { onAddWordSticker(preset) }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = funkyColors.ink
                                )
                            }
                        }
                    }
                }
            }

            StickerSubTab.EMOJI -> {
                Text(
                    text = "Tap any emoji to peel onto your card",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = funkyColors.mute
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CardDesignDefaults.emojiPresets.forEach { emoji ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = funkyColors.surface,
                            border = BorderStroke(1.5.dp, funkyColors.line),
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("workshop_emoji_preset_$emoji")
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onAddEmojiSticker(emoji) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }

            StickerSubTab.PACKS -> {
                when {
                    isLoadingPacks -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFFB800))
                        }
                    }
                    packsError != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = packsError,
                                color = funkyColors.mute,
                                fontSize = 13.5.sp
                            )
                            OutlinedButton(
                                onClick = onRetryPacks,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Retry", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Try again")
                            }
                        }
                    }
                    stickerPacks.isEmpty() -> {
                        Text(
                            text = "No store sticker packs available right now.",
                            fontSize = 13.5.sp,
                            color = funkyColors.mute,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            stickerPacks.forEach { pack ->
                                val isUnlocked = pack.owned || pack.price_coins == 0
                                val emojis = remember(pack.id, pack.config) {
                                    CosmeticsConfigParser.parseStickerPack(pack.config).emojis
                                }
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = funkyColors.surface,
                                    border = BorderStroke(1.5.dp, funkyColors.line),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .alpha(if (isUnlocked) 1f else 0.72f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = pack.name,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = funkyColors.ink,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isUnlocked) {
                                                Surface(
                                                    shape = RoundedCornerShape(100.dp),
                                                    color = funkyColors.ok.copy(alpha = 0.16f)
                                                ) {
                                                    Text(
                                                        text = if (pack.owned) "Owned" else "Free",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = funkyColors.ok,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(100.dp),
                                                    color = funkyColors.soft,
                                                    modifier = Modifier
                                                        .defaultMinSize(minHeight = 32.dp)
                                                        .clip(RoundedCornerShape(100.dp))
                                                        .clickable { onLockedPackClick(pack) }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Lock,
                                                            contentDescription = "Locked",
                                                            tint = funkyColors.ink,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = "${pack.price_coins} coins",
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = funkyColors.ink
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            emojis.forEachIndexed { idx, emoji ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(funkyColors.bg.copy(alpha = 0.7f))
                                                        .clickable {
                                                            if (isUnlocked) {
                                                                onAddPackSticker(pack, idx, emoji)
                                                            } else {
                                                                onLockedPackClick(pack)
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = emoji, fontSize = 22.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkshopStyleTab(
    theme: CardTheme,
    onUpdateTheme: (CardTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val funkyColors = LocalFunkyColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pattern / Style
        SectionLabel("Card pattern")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardBgStyle.entries.forEach { bgStyle ->
                val selected = theme.bgStyle == bgStyle
                ChoiceChipPill(
                    label = bgStyle.label,
                    selected = selected,
                    tag = "workshop_style_${bgStyle.wire}",
                    onClick = { onUpdateTheme(theme.copy(bgStyle = bgStyle)) }
                )
            }
        }

        // Background color swatches
        SectionLabel(if (theme.bgStyle == CardBgStyle.GRADIENT) "Gradient start color" else "Background color")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CardDesignDefaults.bgSwatches.forEach { hex ->
                ColorSwatchCircle(
                    hex = hex,
                    selected = theme.bg.equals(hex, ignoreCase = true),
                    tag = "workshop_bg_swatch_$hex",
                    onClick = { onUpdateTheme(theme.copy(bg = hex)) }
                )
            }
        }

        if (theme.bgStyle == CardBgStyle.GRADIENT) {
            SectionLabel("Two-color gradient presets")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CardDesignDefaults.gradientPresets.forEachIndexed { idx, (startHex, endHex) ->
                    val c1 = parseCardColorOrDefault(startHex, Color(0xFFFFF3B0))
                    val c2 = parseCardColorOrDefault(endHex, Color(0xFFFDE68A))
                    val selected = theme.bg.equals(startHex, true) && theme.bg2.equals(endHex, true)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("workshop_gradient_preset_$idx")
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(c1, c2)))
                            .border(
                                width = if (selected) 3.dp else 1.5.dp,
                                color = if (selected) Color(0xFFFFB800) else funkyColors.ink.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                            .clickable { onUpdateTheme(theme.copy(bg = startHex, bg2 = endHex)) }
                    )
                }
            }

            SectionLabel("Gradient end color")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CardDesignDefaults.bgSwatches.forEach { hex ->
                    ColorSwatchCircle(
                        hex = hex,
                        selected = theme.bg2.equals(hex, ignoreCase = true),
                        tag = "workshop_bg2_swatch_$hex",
                        onClick = { onUpdateTheme(theme.copy(bg2 = hex)) }
                    )
                }
            }
        }

        // Accent color swatches
        SectionLabel("Accent & Hi button color")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CardDesignDefaults.accentSwatches.forEach { hex ->
                ColorSwatchCircle(
                    hex = hex,
                    selected = theme.accent.equals(hex, ignoreCase = true),
                    tag = "workshop_accent_swatch_$hex",
                    onClick = { onUpdateTheme(theme.copy(accent = hex)) }
                )
            }
        }

        // Border selector
        SectionLabel("Border")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardBorderStyle.entries.forEach { border ->
                ChoiceChipPill(
                    label = border.label,
                    selected = theme.border == border,
                    tag = "workshop_border_${border.wire}",
                    onClick = { onUpdateTheme(theme.copy(border = border)) }
                )
            }
        }

        // Corner radius slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionLabel("Corner roundness")
            Text(
                text = "${theme.radius} dp",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = funkyColors.ink
            )
        }
        Slider(
            value = theme.radius.toFloat(),
            onValueChange = { v -> onUpdateTheme(theme.copy(radius = v.roundToInt().coerceIn(0, 32))) },
            valueRange = 0f..32f,
            steps = 31,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFB800),
                activeTrackColor = Color(0xFFFFB800)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("workshop_radius_slider")
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun WorkshopShowTab(
    show: CardShowPrefs,
    hiLabel: String,
    onUpdateShow: (CardShowPrefs) -> Unit,
    onUpdateHiLabel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val funkyColors = LocalFunkyColors.current
    val hiError = remember(hiLabel) {
        if (CardTextSafetyFilter.containsBlockedWord(hiLabel)) "Please choose another word" else null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = hiLabel,
            onValueChange = { next ->
                onUpdateHiLabel(next.take(CardDesignDefaults.MAX_HI_LABEL_LEN))
            },
            label = { Text("Button text (max 12)") },
            placeholder = { Text("Hi") },
            singleLine = true,
            isError = hiError != null,
            supportingText = {
                if (hiError != null) {
                    Text(hiError, color = funkyColors.danger)
                } else {
                    Text("${hiLabel.length}/${CardDesignDefaults.MAX_HI_LABEL_LEN}", color = funkyColors.mute)
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("workshop_hi_label_input")
        )

        ShowToggleRow("Age", show.age, "age") { onUpdateShow(show.copy(age = it)) }
        ShowToggleRow("Gender", show.gender, "gender") { onUpdateShow(show.copy(gender = it)) }
        ShowToggleRow("Country", show.country, "country") { onUpdateShow(show.copy(country = it)) }
        ShowToggleRow("City", show.city, "city") { onUpdateShow(show.copy(city = it)) }
        ShowToggleRow("Languages", show.languages, "languages") { onUpdateShow(show.copy(languages = it)) }
        ShowToggleRow("Bio", show.bio, "bio") { onUpdateShow(show.copy(bio = it)) }
        ShowToggleRow("Hobbies", show.hobbies, "hobbies") { onUpdateShow(show.copy(hobbies = it)) }
        ShowToggleRow("Green online dot", show.onlineDot, "online_dot") { onUpdateShow(show.copy(onlineDot = it)) }

        Text(
            text = "This changes only your card. Your full profile stays the same.",
            fontSize = 12.5.sp,
            color = funkyColors.mute,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
    }
}

@Composable
fun WorkshopRemixTab(
    remixable: Boolean,
    onToggleRemixable: (Boolean) -> Unit,
    cards: List<RemixableCardItem>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onSelectCard: (RemixableCardItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val funkyColors = LocalFunkyColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = funkyColors.surface,
            border = BorderStroke(1.5.dp, funkyColors.line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Let others remix my card",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = funkyColors.ink
                )
                Switch(
                    checked = remixable,
                    onCheckedChange = onToggleRemixable,
                    modifier = Modifier.testTag("workshop_remixable_switch")
                )
            }
        }

        when {
            isLoading && cards.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFB800))
                }
            }
            errorMessage != null && cards.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(errorMessage, color = funkyColors.mute, fontSize = 13.5.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Try again")
                    }
                }
            }
            cards.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No remixable cards yet.",
                        color = funkyColors.mute,
                        fontSize = 14.sp
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(cards, key = { it.userId }) { item ->
                        val miniProfile = remember(item) {
                            PeopleCardProfile(
                                userId = item.userId,
                                name = item.creatorName,
                                username = item.creatorUsername,
                                avatarUrl = item.creatorAvatarUrl,
                                countryCode = item.countryCode,
                                nativeLang = "EN",
                                learningLang = "ES"
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("workshop_remix_card_${item.userId}")
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onSelectCard(item) },
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            PeopleCard(
                                profile = miniProfile,
                                design = item.design,
                                compactMini = true,
                                onCardClick = { onSelectCard(item) }
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.creatorName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = funkyColors.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = funkyColors.soft
                                ) {
                                    Text(
                                        text = "remixed ${item.remixCount} times",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = funkyColors.ink,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (hasMore) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = Color(0xFFFFB800)
                                    )
                                } else {
                                    OutlinedButton(
                                        onClick = onLoadMore,
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("workshop_remix_load_more")
                                    ) {
                                        Text("Load more", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val funkyColors = LocalFunkyColors.current
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        color = funkyColors.ink
    )
}

@Composable
private fun ChoiceChipPill(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = if (selected) Color(0xFFFFB800) else funkyColors.surface,
        border = BorderStroke(
            width = 1.6.dp,
            color = if (selected) Color(0xFF1A1A1A) else funkyColors.line
        ),
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag(tag)
            .clip(RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                color = if (selected) Color(0xFF1A1A1A) else funkyColors.ink
            )
        }
    }
}

@Composable
private fun ColorSwatchCircle(
    hex: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    val color = parseCardColorOrDefault(hex, Color(0xFFFFFBF0))
    Box(
        modifier = Modifier
            .size(48.dp)
            .testTag(tag)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.5.dp,
                color = if (selected) Color(0xFFFFB800) else funkyColors.ink.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ShowToggleRow(
    label: String,
    checked: Boolean,
    tagKey: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = funkyColors.surface,
        border = BorderStroke(1.dp, funkyColors.line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = funkyColors.ink
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag("workshop_show_toggle_$tagKey")
            )
        }
    }
}
