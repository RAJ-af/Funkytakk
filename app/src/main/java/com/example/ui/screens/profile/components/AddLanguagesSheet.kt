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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CircleFlag
import com.example.ui.components.getLanguageBadgeStyle
import com.example.ui.screens.Language
import com.example.ui.screens.LanguageData
import com.example.ui.theme.LocalFunkyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLanguagesSheet(
    alreadySelectedFormatted: List<String>,
    onLanguagesAdded: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = funkyColors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .background(funkyColors.line, RoundedCornerShape(2.dp))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = "Add languages",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = funkyColors.ink
            )
            Text(
                text = "Choose languages you know",
                fontSize = 13.sp,
                color = funkyColors.mute,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(funkyColors.bg, RoundedCornerShape(14.dp))
                    .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = funkyColors.mute,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = funkyColors.ink,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(funkyColors.mango),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search languages...",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = funkyColors.mute
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
                            .background(if (isSelected) funkyColors.soft else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) funkyColors.mango.copy(alpha = 0.6f) else Color.Transparent,
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
                                val badgeStyle = getLanguageBadgeStyle(lang.name)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(badgeStyle.bgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (lang.flagCode.isNotBlank() && lang.flagCode.lowercase() != "un") {
                                        CircleFlag(
                                            countryCode = lang.flagCode,
                                            allowProgramming = lang.flagCode.startsWith("prog_"),
                                            contentDescription = lang.name,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Text(
                                            text = lang.initials,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeStyle.textColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = lang.name,
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = funkyColors.ink
                                    )
                                    if (lang.nativeName != lang.name) {
                                        Text(
                                            text = lang.nativeName,
                                            fontSize = 12.sp,
                                            color = funkyColors.mute
                                        )
                                    }
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(funkyColors.mango, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Selected",
                                        tint = funkyColors.onMango,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(1.5.dp, funkyColors.line, CircleShape)
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
                                            .background(if (isLevelSelected) funkyColors.mango else funkyColors.surface)
                                            .border(
                                                width = 1.dp,
                                                color = if (isLevelSelected) funkyColors.mango else funkyColors.line,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                pendingSelections[lang.name] = level
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = level,
                                            fontSize = 11.sp,
                                            fontWeight = if (isLevelSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isLevelSelected) funkyColors.onMango else funkyColors.ink
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
                    containerColor = funkyColors.mango,
                    contentColor = funkyColors.onMango
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
                    color = funkyColors.onMango
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
                    color = funkyColors.mute
                )
            }
        }
    }
}
