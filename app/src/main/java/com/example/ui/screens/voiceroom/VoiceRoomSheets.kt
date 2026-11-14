package com.example.ui.screens.voiceroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CircleFlag
import com.example.ui.theme.LocalFunkyColors

/**
 * Filter Rooms Bottom Sheet Content
 * Supports dual light/dark themes, drawable icons (zero Material Icons & zero emojis),
 * functional "Select multiple" languages, and functional "They speak" / "They're learning" mode.
 */
@Composable
fun FilterRoomsSheetContent(
    initialSortBy: String = "Popular",
    initialLanguages: Set<String> = emptySet(),
    initialLevel: String = "All Levels",
    initialSpeakOrLearnMode: String = "They speak",
    onApply: (sortBy: String, languages: Set<String>, level: String, speakOrLearnMode: String) -> Unit,
    onReset: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    var sortBy by remember { mutableStateOf(initialSortBy) }
    var selectMultipleLanguage by remember { mutableStateOf(initialLanguages.size > 1) }
    var selectedLanguages by remember { mutableStateOf(initialLanguages) }
    var selectedLevel by remember { mutableStateOf(if (initialLevel == "All") "All Levels" else initialLevel) }
    var speakOrLearnTab by remember { mutableStateOf(initialSpeakOrLearnMode) }

    val selectedBg = if (funkyColors.isDark) funkyColors.mango.copy(alpha = 0.20f) else Color(0xFFFEF9C3)
    val selectedBorder = funkyColors.mango
    val selectedText = if (funkyColors.isDark) funkyColors.mango else Color(0xFF854D0E)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .background(funkyColors.bg)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Filter ",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink
                    )
                    Text(
                        text = "Rooms",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.mango
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Find the perfect room to join",
                    fontSize = 13.sp,
                    color = funkyColors.mute
                )
            }

            // Reset Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(funkyColors.soft)
                    .border(1.dp, funkyColors.line, RoundedCornerShape(20.dp))
                    .clickable {
                        sortBy = "Popular"
                        selectMultipleLanguage = false
                        selectedLanguages = emptySet()
                        selectedLevel = "All Levels"
                        speakOrLearnTab = "They speak"
                        onReset()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh),
                        contentDescription = "Reset",
                        tint = funkyColors.ink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sort By Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_sparkle),
                contentDescription = null,
                tint = funkyColors.mango,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sort By", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sortOptions = listOf(
                "Popular" to R.drawable.ic_flame,
                "Recently Active" to R.drawable.ic_refresh,
                "Most Listeners" to R.drawable.ic_profile
            )
            sortOptions.forEach { (label, iconRes) ->
                val isSelected = sortBy == label
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) selectedBg else funkyColors.surface)
                        .border(1.dp, if (isSelected) selectedBorder else funkyColors.line, RoundedCornerShape(20.dp))
                        .clickable { sortBy = label }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = if (isSelected) selectedText else funkyColors.mute,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) selectedText else funkyColors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Language Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_globe),
                    contentDescription = null,
                    tint = funkyColors.mango,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Language", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Select multiple", fontSize = 12.sp, color = funkyColors.mute)
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = selectMultipleLanguage,
                    onCheckedChange = { enabled ->
                        selectMultipleLanguage = enabled
                        if (!enabled && selectedLanguages.size > 1) {
                            selectedLanguages = setOf(selectedLanguages.first())
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = funkyColors.onMango,
                        checkedTrackColor = funkyColors.mango,
                        uncheckedThumbColor = funkyColors.mute,
                        uncheckedTrackColor = funkyColors.soft,
                        uncheckedBorderColor = funkyColors.line
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // "They speak" vs "They're learning" Segmented Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (funkyColors.isDark) funkyColors.soft else Color(0xFFFEF3C7))
                .border(1.dp, funkyColors.line, RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            listOf("They speak", "They're learning").forEach { option ->
                val isSelected = speakOrLearnTab == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) funkyColors.surface else Color.Transparent)
                        .clickable { speakOrLearnTab = option },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) funkyColors.ink else funkyColors.mute
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Language Grid (2 columns)
        val languages = listOf(
            "All Languages" to "",
            "English" to "us",
            "Hindi" to "in",
            "Russian" to "ru",
            "Korean" to "kr",
            "Japanese" to "jp",
            "Chinese" to "cn",
            "Spanish" to "es",
            "French" to "fr",
            "German" to "de",
            "Arabic" to "sa"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            languages.chunked(2).forEach { rowLangs ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowLangs.forEach { (langName, flagCode) ->
                        val isAll = langName == "All Languages"
                        val isSelected = if (isAll) {
                            selectedLanguages.isEmpty()
                        } else {
                            selectedLanguages.any { it.equals(langName, ignoreCase = true) }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) selectedBg else funkyColors.surface)
                                .border(1.dp, if (isSelected) selectedBorder else funkyColors.line, RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedLanguages = when {
                                        isAll -> emptySet()
                                        selectMultipleLanguage -> {
                                            if (selectedLanguages.any { it.equals(langName, ignoreCase = true) }) {
                                                selectedLanguages.filterNot { it.equals(langName, ignoreCase = true) }.toSet()
                                            } else {
                                                selectedLanguages + langName
                                            }
                                        }
                                        else -> setOf(langName)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isAll) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_globe),
                                            contentDescription = null,
                                            tint = if (isSelected) selectedText else funkyColors.mango,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        CircleFlag(
                                            countryCode = flagCode,
                                            allowProgramming = false,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = langName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = funkyColors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Checkmark / Radio Indicator
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(if (isSelected) funkyColors.mango else Color.Transparent, CircleShape)
                                        .border(1.dp, if (isSelected) funkyColors.mango else funkyColors.line, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_check),
                                            contentDescription = null,
                                            tint = funkyColors.onMango,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (rowLangs.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Level Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_bars),
                contentDescription = null,
                tint = funkyColors.mango,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Level", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
        }
        Spacer(modifier = Modifier.height(12.dp))

        val levels = listOf("All Levels", "Beginner", "Intermediate", "Advanced")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            levels.chunked(2).forEach { rowLevels ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowLevels.forEach { lvl ->
                        val isSelected = selectedLevel == lvl
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) selectedBg else funkyColors.surface)
                                .border(1.dp, if (isSelected) selectedBorder else funkyColors.line, RoundedCornerShape(16.dp))
                                .clickable { selectedLevel = lvl }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_bars),
                                        contentDescription = null,
                                        tint = if (isSelected) selectedText else funkyColors.mute,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = lvl,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = funkyColors.ink
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(if (isSelected) funkyColors.mango else Color.Transparent, CircleShape)
                                        .border(1.dp, if (isSelected) funkyColors.mango else funkyColors.line, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_check),
                                            contentDescription = null,
                                            tint = funkyColors.onMango,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Primary Apply Filters Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(4.dp, RoundedCornerShape(27.dp), spotColor = funkyColors.mango.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(27.dp))
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFFFD000), funkyColors.mango))
                )
                .clickable { onApply(sortBy, selectedLanguages, selectedLevel, speakOrLearnTab) },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Apply Filters",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = funkyColors.onMango
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = funkyColors.onMango,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Chat Backup Screen Content
 */
@Composable
fun ChatBackupScreenContent(
    onBack: () -> Unit
) {
    com.example.ui.screens.profile.ChatBackupScreen(onBackClick = onBack)
}
