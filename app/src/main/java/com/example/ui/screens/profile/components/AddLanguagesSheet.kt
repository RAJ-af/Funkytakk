package com.example.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
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
import com.example.ui.screens.Language
import com.example.ui.screens.LanguageData

private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF4B5563)
private val TextPlaceholder = Color(0xFF9CA3AF)
private val PrimaryYellow = Color(0xFFF8C63D)
private val BorderInactive = Color(0xFFD1D5DB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLanguagesSheet(
    alreadySelectedFormatted: List<String>,
    onLanguagesAdded: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val pendingSelections = remember {
        mutableStateMapOf<String, String>().apply {
            alreadySelectedFormatted.forEach { item ->
                val regex = Regex("^(.*?)\\s*\\((.*?)\\)$")
                val match = regex.find(item)
                if (match != null) {
                    val lang = match.groupValues[1].trim()
                    val prof = match.groupValues[2].trim()
                    put(lang, prof)
                } else {
                    put(item.trim(), "Advanced")
                }
            }
        }
    }

    val proficiencyLevels = listOf("Beginner", "Intermediate", "Advanced", "Native")

    val filteredLanguages = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        val all = LanguageData.popularLanguages + LanguageData.allLanguages.filterNot { allL ->
            LanguageData.popularLanguages.any { it.name == allL.name }
        }
        if (q.isBlank()) {
            all
        } else {
            all.filter {
                it.name.lowercase().contains(q) || it.nativeName.lowercase().contains(q)
            }
        }
    }

    fun getBadgeBgColor(name: String): Color {
        return when (name.lowercase()) {
            "japanese" -> Color(0xFFFFE4E6)
            "french" -> Color(0xFFE0F2FE)
            "german" -> Color(0xFFFEF3C7)
            "russian" -> Color(0xFFF3E8FF)
            "hindi" -> Color(0xFFFED7AA)
            "chinese" -> Color(0xFFFFEDD5)
            "spanish" -> Color(0xFFDCFCE7)
            "english" -> Color(0xFFE0E7FF)
            else -> Color(0xFFF3F4F6)
        }
    }

    fun getBadgeTextColor(name: String): Color {
        return when (name.lowercase()) {
            "japanese" -> Color(0xFFBE123C)
            "french" -> Color(0xFF0369A1)
            "german" -> Color(0xFFB45309)
            "russian" -> Color(0xFF7E22CE)
            "hindi" -> Color(0xFFC2410C)
            "chinese" -> Color(0xFFC2410C)
            "spanish" -> Color(0xFF15803D)
            "english" -> Color(0xFF4338CA)
            else -> TextPrimary
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .background(Color(0xFFE5E7EB), RoundedCornerShape(2.dp))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Add languages",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Choose languages you know",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFFF9FAFB), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextPlaceholder,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(TextPrimary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search languages...",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = TextPlaceholder
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Languages List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredLanguages, key = { it.name }) { lang ->
                    val isSelected = pendingSelections.containsKey(lang.name)
                    val currentProficiency = pendingSelections[lang.name] ?: "Advanced"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFFFFFDF5) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFFEF3C7) else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                if (isSelected) {
                                    pendingSelections.remove(lang.name)
                                } else {
                                    pendingSelections[lang.name] = "Advanced"
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(getBadgeBgColor(lang.name)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lang.initials,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = getBadgeTextColor(lang.name)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = lang.name,
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    if (lang.nativeName != lang.name) {
                                        Text(
                                            text = lang.nativeName,
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(PrimaryYellow, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(1.5.dp, BorderInactive, CircleShape)
                                )
                            }
                        }

                        if (isSelected) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(proficiencyLevels) { level ->
                                    val isLevelSelected = currentProficiency == level
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isLevelSelected) PrimaryYellow else Color(0xFFF3F4F6))
                                            .clickable {
                                                pendingSelections[lang.name] = level
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = level,
                                            fontSize = 11.sp,
                                            fontWeight = if (isLevelSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val formattedList = pendingSelections.map { (lang, prof) ->
                        "$lang ($prof)"
                    }
                    onLanguagesAdded(formattedList)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryYellow,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Add selected (${pendingSelections.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 12.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }
    }
}
