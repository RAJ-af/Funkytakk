package com.example.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldAccent

@Composable
fun LanguageFilterChips(
    selectedLanguageFilter: String,
    onLanguageSelected: (String) -> Unit
) {
    val categories = listOf("All", "🇺🇸 English", "🇮🇳 Hindi", "🇰🇷 Korean", "🇯🇵 Japanese", "🇨🇳 Chinese", "🇷🇺 Russian")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val catNormal = when {
                cat.contains("English") -> "English"
                cat.contains("Hindi") -> "Hindi"
                cat.contains("Korean") -> "Korean"
                cat.contains("Japanese") -> "Japanese"
                cat.contains("Chinese") -> "Chinese"
                cat.contains("Russian") -> "Russian"
                else -> cat
            }
            val isActive = selectedLanguageFilter == catNormal

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isActive) GoldAccent else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (isActive) GoldAccent else Color.LightGray.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(100.dp)
                    )
                    .clickable {
                        onLanguageSelected(catNormal)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cat,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.Black else Color.DarkGray
                )
            }
        }
    }
}
