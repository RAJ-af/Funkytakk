package com.example.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CircleFlag
import com.example.ui.theme.LocalFunkyColors

@Composable
fun LanguageFilterChips(
    selectedLanguageFilter: String,
    selectedLanguages: Set<String> = emptySet(),
    onLanguageSelected: (String) -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    val categories = listOf(
        "All" to "",
        "English" to "us",
        "Hindi" to "in",
        "Korean" to "kr",
        "Japanese" to "jp",
        "Chinese" to "cn",
        "Russian" to "ru"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (label, flagCode) ->
            val isAll = label == "All"
            val isActive = if (isAll) {
                selectedLanguages.isEmpty() && (selectedLanguageFilter == "All" || selectedLanguageFilter.isBlank())
            } else {
                selectedLanguages.any { it.equals(label, ignoreCase = true) } ||
                    selectedLanguageFilter.equals(label, ignoreCase = true)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isActive) funkyColors.mango else funkyColors.surface)
                    .border(
                        width = 1.dp,
                        color = if (isActive) funkyColors.mango else funkyColors.line,
                        shape = RoundedCornerShape(100.dp)
                    )
                    .clickable {
                        onLanguageSelected(label)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isAll) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_globe),
                            contentDescription = null,
                            tint = if (isActive) funkyColors.onMango else funkyColors.mango,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        CircleFlag(
                            countryCode = flagCode,
                            allowProgramming = false,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                        )
                    }
                    Text(
                        text = label,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) funkyColors.onMango else funkyColors.ink
                    )
                }
            }
        }
    }
}
