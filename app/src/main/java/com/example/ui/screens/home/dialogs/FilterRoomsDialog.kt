package com.example.ui.screens.home.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.GoldAccent

@Composable
fun FilterRoomsDialog(
    selectedSortOption: String,
    onSortOptionChange: (String) -> Unit,
    selectedLanguageFilter: String,
    onLanguageFilterChange: (String) -> Unit,
    filterByLevel: String,
    onLevelFilterChange: (String) -> Unit,
    filterOnlyLive: Boolean,
    onFilterOnlyLiveChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) { /* Prevent bubble-up dismissal */ },
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    // Decorative bottom sheet gesture notch bar
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color.LightGray.copy(alpha = 0.5f))
                            .align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Header Title & Reset button layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter Rooms",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Text(
                            text = "Reset",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier
                                .clickable {
                                    onSortOptionChange("Popular")
                                    onLanguageFilterChange("All")
                                    onLevelFilterChange("All")
                                    onFilterOnlyLiveChange(false)
                                    Toast.makeText(context, "Filters reset", Toast.LENGTH_SHORT).show()
                                }
                                .padding(8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 1. SORT BY SECTION
                    Text(
                       text = "Sort By",
                       fontSize = 13.sp,
                       fontWeight = FontWeight.Bold,
                       color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sortOptions = listOf(
                            "Popular" to "🔥 Popular",
                            "Recently Active" to "🕒 Recently Active",
                            "Most Listeners" to "👥 Most Listeners"
                        )
                        sortOptions.forEach { (option, label) ->
                            val isSel = selectedSortOption == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) GoldAccent else Color.White)
                                    .border(
                                        width = if (isSel) 1.5.dp else 1.dp,
                                        color = if (isSel) GoldAccent else Color.LightGray.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSortOptionChange(option) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.Black else Color.DarkGray,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 2. LANGUAGE SECTION
                    Text(
                        text = "Language",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val langList = listOf(
                        "All" to Pair("🌐", "All Languages"),
                        "English" to Pair("🇺🇸", "English"),
                        "Hindi" to Pair("🇮🇳", "Hindi"),
                        "Russian" to Pair("🇷🇺", "Russian"),
                        "Korean" to Pair("🇰🇷", "Korean"),
                        "Japanese" to Pair("🇯🇵", "Japanese"),
                        "Chinese" to Pair("🇨🇳", "Chinese"),
                        "Spanish" to Pair("🇪🇸", "Spanish"),
                        "French" to Pair("🇫🇷", "French"),
                        "German" to Pair("🇩🇪", "German"),
                        "Arabic" to Pair("🇸🇦", "Arabic")
                    )
                    
                    val pairs = langList.chunked(2)
                    pairs.forEach { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { (langVal, details) ->
                                val (emoji, label) = details
                                val isSel = (langVal == "All" && selectedLanguageFilter == "All") || 
                                            (langVal != "All" && selectedLanguageFilter == langVal)
                                
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White)
                                        .border(
                                            width = if (isSel) 1.5.dp else 1.dp,
                                            color = if (isSel) GoldAccent else Color.LightGray.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            onLanguageFilterChange(langVal)
                                        }
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                    
                                    if (isSel) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(GoldAccent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (pair.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 3. LEVEL SECTION
                    Text(
                        text = "Level",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val levelPairs = listOf(
                        listOf(
                            "All" to Pair("All Levels", null),
                            "Beginner" to Pair("Beginner", Color(0xFF10B981))
                        ),
                        listOf(
                            "Intermediate" to Pair("Intermediate", Color(0xFF3B82F6)),
                            "Advanced" to Pair("Advanced", Color(0xFF8B5CF6))
                        )
                    )
                    
                    levelPairs.forEach { levPair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            levPair.forEach { (levVal, details) ->
                                val (label, dotColor) = details
                                val isSel = filterByLevel == levVal
                                
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White)
                                        .border(
                                            width = if (isSel) 1.5.dp else 1.dp,
                                            color = if (isSel) GoldAccent else Color.LightGray.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onLevelFilterChange(levVal) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (dotColor != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                    
                                    if (isSel) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(GoldAccent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(26.dp))
                    
                    // 4. APPLY SOLID BOTTOM CAPSULE BUTTON
                    Button(
                        onClick = { 
                            onApply()
                            Toast.makeText(context, "Filters applied!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "Apply Filters",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // 5. CANCEL TEXT LABEL
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDismiss() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
