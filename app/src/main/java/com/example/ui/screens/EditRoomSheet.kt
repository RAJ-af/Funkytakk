package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CircleFlag
import com.example.ui.components.LanguageIcon
import com.example.ui.components.RoomLanguageSelectionSheet

@Composable
fun EditRoomBottomSheetContent(
    roomTitle: String,
    onTitleChange: (String) -> Unit,
    roomTags: String,
    onTagsChange: (String) -> Unit,
    roomDescription: String,
    onDescriptionChange: (String) -> Unit,
    roomLanguage: String,
    onLanguageChange: (String) -> Unit,
    roomCountryCode: String = "",
    onCountryCodeChange: (String) -> Unit = {},
    selectedThemeIndex: Int,
    onThemeChange: (Int) -> Unit,
    isPrivate: Boolean,
    onPrivateChange: (Boolean) -> Unit,
    joinCode: String,
    onJoinCodeChange: (String) -> Unit,
    isHost: Boolean,
    onApplyChanges: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val context = LocalContext.current

    // Modal state controllers
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }

    val currentTheme = roomThemes[selectedThemeIndex]
    val isDark = currentTheme.isDark

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Sheet Header (Beautiful, matching Picture 4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    CustomPencilIcon(
                        tint = if (isDark) Color.White else Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Edit Room",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Text(
                        text = "Update your room details",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray
                    )
                }
            }
            androidx.compose.material3.IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = if (isDark) Color.White else Color.DarkGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Room Name Field (Picture 4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Room Name",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black
            )
            Text(
                text = "${roomTitle.length}/60",
                fontSize = 12.sp,
                color = if (roomTitle.length > 50) Color.Red else (if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        androidx.compose.material3.OutlinedTextField(
            value = roomTitle,
            onValueChange = { if (it.length <= 60) onTitleChange(it) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            readOnly = !isHost,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (isDark) Color.White else Color.Black,
                unfocusedTextColor = if (isDark) Color.White else Color.Black,
                focusedBorderColor = if (isDark) currentTheme.themeColor else Color(0xFFF59E0B),
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFE5E7EB),
                focusedContainerColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White,
                unfocusedContainerColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White,
                cursorColor = if (isDark) Color.White else Color.Black
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Description Field (Picture 4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Description",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black
            )
            Text(
                text = "${roomDescription.length}/120",
                fontSize = 12.sp,
                color = if (roomDescription.length > 100) Color.Red else (if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        androidx.compose.material3.OutlinedTextField(
            value = roomDescription,
            onValueChange = { if (it.length <= 120) onDescriptionChange(it) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            minLines = 2,
            maxLines = 3,
            readOnly = !isHost,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (isDark) Color.White else Color.Black,
                unfocusedTextColor = if (isDark) Color.White else Color.Black,
                focusedBorderColor = if (isDark) currentTheme.themeColor else Color(0xFFF59E0B),
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFE5E7EB),
                focusedContainerColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White,
                unfocusedContainerColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White,
                cursorColor = if (isDark) Color.White else Color.Black
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Card Row 1: Language Selection (Picture 4 style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White)
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
                .clickable(enabled = isHost) { showLanguagePicker = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    val activeFlag = roomCountryCode.ifBlank { LanguageData.getFlagCodeForLanguage(roomLanguage) }
                    LanguageIcon(
                        language = activeFlag.ifBlank { roomLanguage },
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Language",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Text(
                        text = roomLanguage,
                        fontSize = 12.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Expand Language",
                tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Gray,
                modifier = Modifier
                    .size(24.dp)
                    .scale(-1f, 1f) // flip arrow horizontally to make it point right
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Card Row 2: Theme Selection (Picture 4 / 6 style)
        val selectedTheme = roomThemes[selectedThemeIndex]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White)
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
                .clickable(enabled = isHost) { showThemePicker = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    CustomPaletteIcon(
                        tint = if (isDark) Color.White else Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Theme",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Text(
                        text = "${selectedTheme.name} (${if (selectedTheme.isDark) "Immersive Dark" else "Vibrant Light"})",
                        fontSize = 12.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Color preview circle representing active theme style
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .shadow(1.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(selectedTheme.startColor, selectedTheme.endColor)
                            )
                        )
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Expand Theme",
                    tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Gray,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(-1f, 1f) // point right
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Card Row 3: Tags (Replacing Advanced Settings - Picture 8 style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White)
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    CustomTagsIcon(
                        tint = if (isDark) Color.White else Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Tags",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Text(
                        text = "Select one popular tag for your room",
                        fontSize = 11.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tag List containing selected active tag chip & "+ Add Tag" dashed border button (Picture 8 style)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (roomTags.isNotEmpty()) {
                    // Selected active tag chip with close button (Picture 8 style)
                    Box(
                        modifier = Modifier
                            .background(
                                if (isDark) currentTheme.themeColor.copy(alpha = 0.2f) else Color(0xFFFEF3C7),
                                RoundedCornerShape(50.dp)
                            )
                            .border(1.dp, if (isDark) currentTheme.themeColor else Color(0xFFFBBF24), RoundedCornerShape(50.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = roomTags,
                                fontSize = 12.sp,
                                color = if (isDark) Color.White else Color(0xFFD97706),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Tag",
                                tint = if (isDark) Color.White else Color(0xFFD97706),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable(enabled = isHost) { onTagsChange("") }
                            )
                        }
                    }
                }

                // Beautiful "+ Add Tag" button with dashed border style
                Box(
                    modifier = Modifier
                        .drawBehind {
                            val stroke = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                            drawRoundRect(
                                color = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFFF59E0B),
                                style = stroke,
                                cornerRadius = CornerRadius(50.dp.toPx(), 50.dp.toPx())
                            )
                        }
                        .clip(RoundedCornerShape(50.dp))
                        .clickable(enabled = isHost) { showTagPicker = true }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Icon",
                            tint = if (isDark) Color.White else Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Tag",
                            fontSize = 12.sp,
                            color = if (isDark) Color.White else Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (isHost) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE5E7EB), thickness = 1.dp)
            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = if (isDark) Color.White else Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Private Room Settings",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Text(
                        text = "Restrict access to your room via join code",
                        fontSize = 11.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray
                    )
                }
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { checked ->
                        onPrivateChange(checked)
                        if (checked && joinCode.isBlank()) {
                            onJoinCodeChange((100000..999999).random().toString())
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = if (isDark) currentTheme.themeColor else Color(0xFFF59E0B)
                    )
                )
            }

            if (isPrivate) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = joinCode,
                        onValueChange = { val clean = it.filter { c -> c.isDigit() }; if (clean.length <= 6) onJoinCodeChange(clean) },
                        placeholder = { Text("Passcode (6 digits)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        readOnly = !isHost,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black,
                            focusedBorderColor = if (isDark) currentTheme.themeColor else Color(0xFFF59E0B),
                            unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFE5E7EB)
                        )
                    )
                    androidx.compose.material3.Button(
                        onClick = { onJoinCodeChange((100000..999999).random().toString()) },
                        enabled = isHost,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Regen", color = if (isDark) Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Save / Close Bottom Button
        if (isHost) {
            androidx.compose.material3.Button(
                onClick = {
                    if (isPrivate && joinCode.length != 6) {
                        Toast.makeText(context, "Passcode must be exactly 6 digits!", Toast.LENGTH_SHORT).show()
                    } else {
                        onApplyChanges()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(100.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (isDark) currentTheme.themeColor else Color(0xFFF59E0B)
                )
            ) {
                Text(
                    text = "Apply Changes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else {
            androidx.compose.material3.Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(100.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color.Gray
                )
            ) {
                Text(
                    text = "Close Details",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal Popup 1: Language Picker Selector (Full screen sheet matching Room Creation!)
    if (showLanguagePicker) {
        RoomLanguageSelectionSheet(
            selectedLanguageName = roomLanguage,
            isDark = isDark,
            onLanguageSelected = { selectedLang ->
                onLanguageChange(selectedLang.name)
                onCountryCodeChange(selectedLang.flagCode)
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false }
        )
    }

    // Modal Popup 2: Choose Theme Dialog (Beautifully matches Picture 6!)
    if (showThemePicker) {
        val dialogBg = if (isDark) Color(0xFF1E293B) else Color.White
        val dialogTitleColor = if (isDark) Color.White else Color.Black
        val dialogSubtitleColor = if (isDark) Color(0xFF94A3B8) else Color.Gray
        val closeBtnBg = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFF3F4F6)
        val closeBtnTint = if (isDark) Color.White else Color.DarkGray

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Choose Theme", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = dialogTitleColor)
                        Text(text = "Pick a theme for your room", fontSize = 11.sp, color = dialogSubtitleColor)
                    }
                    androidx.compose.material3.IconButton(
                        onClick = { showThemePicker = false },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(closeBtnBg)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = closeBtnTint, modifier = Modifier.size(16.dp))
                    }
                }
            },
            text = {
                Box(modifier = Modifier.height(350.dp)) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(roomThemes.size) { index ->
                            val current = roomThemes[index]
                            val isSelected = selectedThemeIndex == index
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) (if (isDark) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFFFAEB))
                                        else (if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color.Transparent)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFFFBBF24) else (if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFF3F4F6)),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        onThemeChange(index)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .shadow(1.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(Brush.verticalGradient(colors = listOf(current.startColor, current.endColor)))
                                        .border(2.dp, if (isSelected) Color(0xFFD97706) else Color.Transparent, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = current.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFFBBF24) else dialogTitleColor
                                    )
                                    Text(
                                        text = current.description,
                                        fontSize = 10.sp,
                                        color = dialogSubtitleColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFFFBBF24) 
                                            else (if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE5E7EB))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = if (isDark) Color(0xFF451A03) else Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showThemePicker = false }
                ) {
                    Text("Apply Theme", fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                }
            },
            containerColor = dialogBg,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Modal Popup 3: Add Tags Dialog (Matches Picture 7 Popular Tags exactly!)
    if (showTagPicker) {
        val dialogBg = if (isDark) Color(0xFF1E293B) else Color.White
        val dialogTitleColor = if (isDark) Color.White else Color.Black
        val dialogSubtitleColor = if (isDark) Color(0xFF94A3B8) else Color.Gray
        val closeBtnBg = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFF3F4F6)
        val closeBtnTint = if (isDark) Color.White else Color.DarkGray

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTagPicker = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Add Tags", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = dialogTitleColor)
                        Text(text = "Popular Tags", fontSize = 11.sp, color = dialogSubtitleColor)
                    }
                    androidx.compose.material3.IconButton(
                        onClick = { showTagPicker = false },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(closeBtnBg)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = closeBtnTint, modifier = Modifier.size(16.dp))
                    }
                }
            },
            text = {
                val popularTagsList = listOf(
                    "Chill", "Coding", "Gaming", "Tech",
                    "Study", "Python", "Music", "Debate",
                    "Travel", "Poetry", "Anime", "Sports"
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val rows = popularTagsList.chunked(3)
                    rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            row.forEach { tag ->
                                val isSelected = roomTags == tag
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(
                                            if (isSelected) (if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFFFAEB))
                                            else (if (isDark) Color(0xFF0F172A) else Color(0xFFF3F4F6))
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFFFBBF24) else (if (isDark) Color.White.copy(alpha = 0.1f) else Color.Transparent),
                                            shape = RoundedCornerShape(50.dp)
                                        )
                                        .clickable {
                                            onTagsChange(tag)
                                            showTagPicker = false
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) (if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706))
                                        else (if (isDark) Color(0xFFE2E8F0) else Color.DarkGray)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = dialogBg,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
