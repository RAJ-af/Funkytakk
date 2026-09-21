package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.Language
import com.example.ui.screens.LanguageData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomLanguageSelectionSheet(
    selectedLanguageName: String,
    isDark: Boolean = false,
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
            // When user types a search query, search across all languages including programming
            LanguageData.allLanguages.filter {
                it.name.contains(q, ignoreCase = true) ||
                it.nativeName.contains(q, ignoreCase = true) ||
                it.code.contains(q, ignoreCase = true)
            }
        }
    }

    val sheetBg = if (isDark) Color(0xFF0F172A) else Color.White
    val contentTextColor = if (isDark) Color.White else Color(0xFF111827)
    val subtitleTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)
    val inputBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF9FAFB)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF3F4F6)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.4f)
            )
        }
    ) {
        CompositionLocalProvider(LocalContentColor provides contentTextColor) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                Text(
                    text = "Select Language",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = contentTextColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Search box with explicit high-contrast text color
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { 
                        Text(
                            "Search language or tech (e.g. Python)...", 
                            color = subtitleTextColor, 
                            fontSize = 13.sp
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = contentTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = subtitleTextColor
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = subtitleTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = contentTextColor,
                        unfocusedTextColor = contentTextColor,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedBorderColor = Color(0xFFFFC107),
                        unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE5E7EB),
                        cursorColor = Color(0xFFD97706),
                        focusedPlaceholderColor = subtitleTextColor,
                        unfocusedPlaceholderColor = subtitleTextColor
                    )
                )

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
                                .background(
                                    if (isSelected) (if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFEF3C7))
                                    else (if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6))
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFFF59E0B) else (if (isDark) Color.White.copy(alpha = 0.1f) else Color.Transparent),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) (if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309))
                                else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563))
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
                            fontSize = 13.sp,
                            color = contentTextColor
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
                                            val matchedLang = LanguageData.allLanguages.firstOrNull { it.name.equals(name, ignoreCase = true) }
                                                ?: Language(name = name, nativeName = name, flagCode = code, code = code, initials = name.take(2))
                                            onLanguageSelected(matchedLang)
                                            onDismiss()
                                        }
                                        .padding(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                if (isSelected) (if (isDark) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFFFBEB))
                                                else (if (isDark) Color(0xFF1E293B) else Color(0xFFF9FAFB)),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .border(
                                                1.5.dp,
                                                if (isSelected) Color(0xFFFFC107) else (if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFF3F4F6)),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircleFlag(
                                            countryCode = code,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) (if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706))
                                        else contentTextColor
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
                    fontSize = 13.sp,
                    color = contentTextColor
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isChosen) (if (isDark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFFFBEB))
                                    else Color.Transparent
                                )
                                .clickable {
                                    onLanguageSelected(language)
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                CircleFlag(
                                    countryCode = language.flagCode,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = language.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) (if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706))
                                        else contentTextColor
                                    )
                                    Text(
                                        text = "${language.nativeName} · ${language.code}",
                                        fontSize = 12.sp,
                                        color = if (isChosen) (if (isDark) Color(0xFFFCD34D) else Color(0xFFD97706).copy(alpha = 0.85f))
                                        else subtitleTextColor
                                    )
                                }
                            }

                            if (isChosen) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
