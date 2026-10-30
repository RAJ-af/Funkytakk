package com.example.ui.screens.voiceroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CircleFlag

/**
 * Filter Rooms Bottom Sheet Content
 * Matches Image 5 (Title "Filter Rooms", Reset button, Sort By pills, Language grid with checkboxes, Level grid, Apply Filters yellow button)
 */
@Composable
fun FilterRoomsSheetContent(
    initialSortBy: String = "Popular",
    initialLanguage: String = "All Languages",
    initialLevel: String = "All Levels",
    onApply: (sortBy: String, language: String, level: String) -> Unit,
    onReset: () -> Unit
) {
    var sortBy by remember { mutableStateOf(initialSortBy) }
    var selectedLanguage by remember { mutableStateOf(initialLanguage) }
    var selectedLevel by remember { mutableStateOf(initialLevel) }
    var selectMultipleLanguage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .background(Color(0xFFFFFDF5))
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
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Rooms",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFB800)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Find the perfect room to join",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Reset Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable {
                        sortBy = "Popular"
                        selectedLanguage = "All Languages"
                        selectedLevel = "All Levels"
                        onReset()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sort By Section
        Text("✨ Sort By", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sortOptions = listOf(
                Triple("Popular", "🔥", Icons.Default.Whatshot),
                Triple("Recently Active", "🕒", Icons.Default.Schedule),
                Triple("Most Listeners", "👥", Icons.Default.Group)
            )
            sortOptions.forEach { (label, _, icon) ->
                val isSelected = sortBy == label
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color(0xFFFEF08A) else Color.White)
                        .border(1.dp, if (isSelected) Color(0xFFFFD000) else Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                        .clickable { sortBy = label }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF854D0E) else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF854D0E) else Color(0xFF334155)
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
                Text("🌐 Language", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Select multiple", fontSize = 12.sp, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = selectMultipleLanguage,
                    onCheckedChange = { selectMultipleLanguage = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFFB800))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Language Grid (2 columns)
        val languages = listOf(
            "All Languages" to "glob",
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
                        val isSelected = selectedLanguage == langName
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color(0xFFFEF9C3) else Color.White)
                                .border(1.dp, if (isSelected) Color(0xFFFFD000) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .clickable { selectedLanguage = langName }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircleFlag(countryCode = flagCode, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = langName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                // Checkmark / Radio Indicator
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(if (isSelected) Color(0xFFFFB800) else Color.Transparent, CircleShape)
                                        .border(1.dp, if (isSelected) Color(0xFFFFB800) else Color(0xFFCBD5E1), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
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
        Text("📶 Level", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
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
                                .background(if (isSelected) Color(0xFFFEF9C3) else Color.White)
                                .border(1.dp, if (isSelected) Color(0xFFFFD000) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
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
                                        imageVector = Icons.Default.SignalCellularAlt,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFFD97706) else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = lvl,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(if (isSelected) Color(0xFFFFB800) else Color.Transparent, CircleShape)
                                        .border(1.dp, if (isSelected) Color(0xFFFFB800) else Color(0xFFCBD5E1), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
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
                .shadow(4.dp, RoundedCornerShape(27.dp), spotColor = Color(0xFFFFB800).copy(alpha = 0.4f))
                .clip(RoundedCornerShape(27.dp))
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFFFD000), Color(0xFFFFB800)))
                )
                .clickable { onApply(sortBy, selectedLanguage, selectedLevel) },
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
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Chat Backup Screen Content
 * Matches Image 6 (3D cloud header banner, E2E card, Google Drive card, Backup status stats, Preferences toggles, Tip banner)
 */
@Composable
fun ChatBackupScreenContent(
    onBack: () -> Unit
) {
    var autoBackupFreq by remember { mutableStateOf("Off") }
    var includePhotos by remember { mutableStateOf(false) }
    var wifiOnly by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF5))
            .verticalScroll(rememberScrollState())
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Chat ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    Text("Backup", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFB800))
                }
                Text(
                    text = "Keep your chats safe and accessible across devices.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Top 3D Cloud Upload Icon
            com.example.ui.components.Soft3DCloudIcon(size = 64.dp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 1: End-to-End Encrypted Backups
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFEF08A).copy(alpha = 0.4f))
                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                com.example.ui.components.Soft3DShieldIcon(size = 54.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("End-to-End Encrypted Backups", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your backups are encrypted. If you forget your vault password, backups cannot be recovered.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Google Drive Storage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Google Drive Storage", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Not connected", fontSize = 12.sp, color = Color(0xFF64748B))
                    }

                    Image(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Drive",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Connect Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFFD000), Color(0xFFFFB800)))
                        )
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Connect Google Drive", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 3: Last Backup Stats
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color(0xFFFEF08A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF854D0E), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Last Backup", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("Never", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }

                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color(0xFFFEF08A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF854D0E), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Backup Size", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("0 KB", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Back up now button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Back up now", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 4: Backup Preferences
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Backup Preferences", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("Customize how your chats are backed up.", fontSize = 12.sp, color = Color(0xFF64748B))

                Spacer(modifier = Modifier.height(14.dp))

                Text("Auto Backup Frequency", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Off", "Daily", "Weekly").forEach { freq ->
                        val isSelected = autoBackupFreq == freq
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(if (isSelected) Color(0xFFFFD000) else Color(0xFFF8FAFC))
                                .border(1.dp, if (isSelected) Color(0xFFFFD000) else Color(0xFFE2E8F0), RoundedCornerShape(19.dp))
                                .clickable { autoBackupFreq = freq },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(freq, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Switch 1: Include Photos & Media
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Include Photos & Media", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        Text("Back up images, videos, voice notes and other media files.", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = includePhotos,
                        onCheckedChange = { includePhotos = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFFB800))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Switch 2: Back up over Wi-Fi only
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Back up over Wi-Fi only", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        Text("Avoid using cellular data for backups.", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = wifiOnly,
                        onCheckedChange = { wifiOnly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFFB800))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tip Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFEEF2FF))
                .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tip", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3730A3))
                    Text(
                        "Regular backups keep your conversations safe, even if you change or lose your device.",
                        fontSize = 11.sp,
                        color = Color(0xFF4338CA),
                        lineHeight = 15.sp
                    )
                }
                com.example.ui.components.Soft3DCloudIcon(size = 48.dp)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
