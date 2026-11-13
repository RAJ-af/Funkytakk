package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.screens.Language
import com.example.ui.screens.LanguageData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomLanguageSelectionSheet(
    selectedLanguageName: String,
    isDark: Boolean = isSystemInDarkTheme(),
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") } // "All", "Spoken", "Programming"

    val baseList = remember(selectedCategory) {
        when (selectedCategory) {
            "Programming" -> LanguageData.programmingLanguages
            "Spoken" -> LanguageData.allLanguages.filter { !it.flagCode.startsWith("prog_") }
            else -> LanguageData.allLanguages
        }
    }

    val filteredLanguages = remember(searchQuery, baseList) {
        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim()
            LanguageData.allLanguages.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.nativeName.contains(q, ignoreCase = true) ||
                    it.code.contains(q, ignoreCase = true)
            }
        }
    }

    // Warm Cream / Dark FunkyTalk Palette
    val sheetBg = if (isDark) Color(0xFF231D15) else Color(0xFFFFF8E7)
    val inkColor = if (isDark) Color(0xFFFFF3DC) else Color(0xFF2B2118)
    val muteColor = if (isDark) Color(0xFFA89A82) else Color(0xFF8A7B66)
    val softSurface = if (isDark) Color(0xFF3A2D12) else Color(0xFFFFEFC2)
    val cardSurface = if (isDark) Color(0xFF2B2118) else Color.White
    val dividerColor = if (isDark) Color(0xFF34291A) else Color(0xFFF0E3C4)
    val mangoColor = Color(0xFFFFB627)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(dividerColor)
            )
        }
    ) {
        CompositionLocalProvider(LocalContentColor provides inkColor) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(36.dp))
                    Text(
                        text = "Select Language",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = inkColor,
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(softSurface)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = inkColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Warm Cream Search Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(softSurface)
                        .border(1.dp, dividerColor, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = muteColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(
                                color = inkColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(mangoColor),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search language or tech (e.g. Python)...",
                                        color = muteColor,
                                        fontSize = 13.5.sp
                                    )
                                }
                                inner()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("language_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Clear search",
                            tint = muteColor,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                // Category Tabs ("All", "Spoken", "Programming")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Spoken", "Programming").forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) mangoColor else softSurface)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF2B2118) else muteColor
                            )
                        }
                    }
                }

                // Popular Languages / Tech block
                if (searchQuery.isBlank()) {
                    val popularItems = remember(selectedCategory) {
                        when (selectedCategory) {
                            "Programming" -> listOf(
                                Triple("Python", "prog_python", "AI & Backend"),
                                Triple("JavaScript", "prog_javascript", "Web & JS"),
                                Triple("Kotlin", "prog_kotlin", "Android"),
                                Triple("Java", "prog_java", "JVM"),
                                Triple("C++", "prog_cpp", "Systems")
                            )
                            else -> listOf(
                                Triple("Japanese", "jp", "日本語"),
                                Triple("Python", "prog_python", "Coding"),
                                Triple("English", "us", "English"),
                                Triple("Spanish", "es", "Español"),
                                Triple("Korean", "kr", "한국어")
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (selectedCategory == "Programming") "Popular Tech" else "Popular Languages",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = inkColor
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            popularItems.forEach { (name, code, _) ->
                                val isSelected = selectedLanguageName.equals(name, ignoreCase = true)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            val matchedLang = LanguageData.allLanguages.firstOrNull {
                                                it.name.equals(name, ignoreCase = true)
                                            } ?: Language(
                                                name = name,
                                                nativeName = name,
                                                flagCode = code,
                                                code = code,
                                                initials = name.take(2)
                                            )
                                            onLanguageSelected(matchedLang)
                                            onDismiss()
                                        }
                                        .padding(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) softSurface else cardSurface)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) mangoColor else dividerColor,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LanguageIcon(
                                            language = if (selectedCategory == "Programming") name else code,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = name,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) Color(0xFFD97706) else inkColor
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                }

                // All languages / tech list
                Text(
                    text = if (selectedCategory == "Programming") "Programming Languages (${filteredLanguages.size})" else "All Languages (${filteredLanguages.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = inkColor
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLanguages, key = { it.name + "_" + it.code }) { language ->
                        val isChosen = selectedLanguageName.equals(language.name, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isChosen) softSurface else cardSurface.copy(alpha = 0.7f))
                                .border(
                                    width = if (isChosen) 1.5.dp else 1.dp,
                                    color = if (isChosen) mangoColor else dividerColor,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    onLanguageSelected(language)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                LanguageIcon(
                                    language = language.flagCode.ifBlank { language.name },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = language.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = inkColor
                                    )
                                    Text(
                                        text = "${language.nativeName} · ${language.code}",
                                        fontSize = 12.sp,
                                        color = muteColor
                                    )
                                }
                            }

                            if (isChosen) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(mangoColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Selected",
                                        tint = Color(0xFF2B2118),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
