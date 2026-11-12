package com.example.ui.screens.home.dialogs

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.random.Random

// Banner Option Model
data class RoomBannerOption(
    val id: String,
    val name: String,
    val gradient: Brush,
    val imageRes: Int? = null,
    val isVipOnly: Boolean = false
)

private val bannerOptions = listOf(
    RoomBannerOption(
        id = "kyoto_dusk",
        name = "Kyoto Dusk",
        gradient = Brush.verticalGradient(listOf(Color(0xFF2E1065), Color(0xFF7C3AED), Color(0xFFF43F5E))),
        imageRes = R.drawable.img_wallpaper_moonlight_valley_1781625422948
    ),
    RoomBannerOption(
        id = "sakura_canal",
        name = "Sakura Canal",
        gradient = Brush.verticalGradient(listOf(Color(0xFF831843), Color(0xFFDB2777), Color(0xFFF472B6))),
        imageRes = R.drawable.img_wallpaper_cherry_blossom_1781625392725
    ),
    RoomBannerOption(
        id = "tokyo_night",
        name = "Tokyo Night",
        gradient = Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF4338CA))),
        imageRes = R.drawable.img_wallpaper_balcony_night_1781625409367
    ),
    RoomBannerOption(
        id = "neon_akihabara",
        name = "Neon Akihabara",
        gradient = Brush.verticalGradient(listOf(Color(0xFF4C1D95), Color(0xFFC026D3), Color(0xFF06B6D4))),
        imageRes = R.drawable.img_wallpaper_day_street_1781625437347
    ),
    RoomBannerOption(
        id = "anime_red",
        name = "Anime Red",
        gradient = Brush.verticalGradient(listOf(Color(0xFF7F1D1D), Color(0xFFDC2626), Color(0xFFF97316))),
        imageRes = R.drawable.chill_vibes_room_bg_1781561393087
    ),
    RoomBannerOption(
        id = "vaporwave",
        name = "Vaporwave",
        gradient = Brush.verticalGradient(listOf(Color(0xFF312E81), Color(0xFF9333EA), Color(0xFFEC4899)))
    ),
    RoomBannerOption(
        id = "island_sunset",
        name = "Island Sunset",
        gradient = Brush.verticalGradient(listOf(Color(0xFF7C2D12), Color(0xFFEA580C), Color(0xFFFBBF24))),
        imageRes = R.drawable.img_wallpaper_lighthouse_sunset_1781625450547
    )
)

private val languageChoices = listOf(
    "Japanese" to "🇯🇵",
    "Korean" to "🇰🇷",
    "Chinese" to "🇨🇳",
    "English" to "🇬🇧",
    "Hindi" to "🇮🇳",
    "Spanish" to "🇪🇸",
    "French" to "🇫🇷",
    "German" to "🇩🇪",
    "Russian" to "🇷🇺",
    "Arabic" to "🇸🇦"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomBottomSheet(
    onDismiss: () -> Unit,
    onCreateRoom: (isPrivate: Boolean) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Form state
    var selectedBanner by remember { mutableStateOf(bannerOptions.first()) }
    var roomTitle by remember { mutableStateOf("") }
    var roomDescription by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("Japanese") }
    var selectedLanguageEmoji by remember { mutableStateOf("🇯🇵") }
    var selectedLevel by remember { mutableStateOf("Beginner") }
    var showVipTag by remember { mutableStateOf(false) }
    var isPrivateRoom by remember { mutableStateOf(false) }
    var generatedPassCode by remember { mutableStateOf("FT-${Random.nextInt(1000, 9999)}") }
    var isUserVip by remember { mutableStateOf(false) }
    var showVipSheet by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFFF8E7),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New room",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2B2118)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFFFEFC2), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF2B2118),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // LIVE ROOM PREVIEW CARD
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(selectedBanner.gradient)
                ) {
                    // Background Image if present
                    selectedBanner.imageRes?.let { resId ->
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Soft Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )

                    // Content over Preview Card
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Badges Row at Top of Preview
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Language Badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.55f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "$selectedLanguageEmoji $selectedLanguage",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // VIP Badge (if enabled)
                            if (showVipTag) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFFFB627)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFF2B2118),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "VIP",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF2B2118)
                                        )
                                    }
                                }
                            }

                            // Private Badge (if enabled)
                            if (isPrivateRoom) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.Black.copy(alpha = 0.55f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB627),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Private",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Category Badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.55f)
                            ) {
                                Text(
                                    text = "$selectedLevel Voice room",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Title & Description at Bottom of Preview
                        Column {
                            Text(
                                text = if (roomTitle.isNotBlank()) roomTitle else "Your room title",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (roomTitle.isNotBlank()) Color.White else Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (roomDescription.isNotBlank()) roomDescription else "Add a short description",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (roomDescription.isNotBlank()) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // BANNER CATALOG STRIP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Banner ${selectedBanner.name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2B2118)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(bannerOptions) { banner ->
                        val isSelected = selectedBanner.id == banner.id
                        Box(
                            modifier = Modifier
                                .width(106.dp)
                                .height(74.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(banner.gradient)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFFFB627) else Color(0xFFF0E3C4),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { selectedBanner = banner }
                        ) {
                            banner.imageRes?.let { res ->
                                Image(
                                    painter = painterResource(id = res),
                                    contentDescription = banner.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(20.dp)
                                        .background(Color(0xFFFFB627), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF2B2118),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Custom Upload Option (VIP Locked)
                    item {
                        Box(
                            modifier = Modifier
                                .width(106.dp)
                                .height(74.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFFFEFC2))
                                .border(
                                    width = 1.5.dp,
                                    color = Color(0xFF8A7B66),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    if (!isUserVip) {
                                        showVipSheet = true
                                    } else {
                                        Toast.makeText(context, "Custom gallery upload available!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Upload",
                                    tint = Color(0xFF2B2118),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // VIP Lock Pill
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFB627),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = "VIP 🔒",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2B2118),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ROOM TITLE INPUT (0/50)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFEFC2))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Room title",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8A7B66)
                        )
                        Text(
                            text = "${roomTitle.length}/50",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8A7B66)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicTextField(
                        value = roomTitle,
                        onValueChange = { if (it.length <= 50) roomTitle = it },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2B2118)
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (roomTitle.isEmpty()) {
                                Text(
                                    text = "e.g., Let's practice Japanese together!",
                                    fontSize = 15.sp,
                                    color = Color(0xFF8A7B66).copy(alpha = 0.7f)
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // DESCRIPTION INPUT (0/120)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFEFC2))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Description",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8A7B66)
                        )
                        Text(
                            text = "${roomDescription.length}/120",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8A7B66)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicTextField(
                        value = roomDescription,
                        onValueChange = { if (it.length <= 120) roomDescription = it },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2B2118)
                        ),
                        maxLines = 3,
                        decorationBox = { innerTextField ->
                            if (roomDescription.isEmpty()) {
                                Text(
                                    text = "Add topic guidelines or target practice levels...",
                                    fontSize = 14.sp,
                                    color = Color(0xFF8A7B66).copy(alpha = 0.7f)
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // LANGUAGE SELECTION CHIPS
                Text(
                    text = "Language",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B2118)
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(languageChoices) { (lang, emoji) ->
                        val isSelected = selectedLanguage == lang
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFFFFB627) else Color(0xFFFFEFC2))
                                .clickable {
                                    selectedLanguage = lang
                                    selectedLanguageEmoji = emoji
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = emoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = lang,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = Color(0xFF2B2118)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // LEVEL SEGMENTED CONTROL
                Text(
                    text = "Level",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B2118)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFFFEFC2))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val levels = listOf(
                        "Beginner" to "🌱",
                        "Medium" to "📊",
                        "Advanced" to "⭐"
                    )
                    levels.forEach { (lvl, icon) ->
                        val isSelected = selectedLevel == lvl
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { selectedLevel = lvl },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = lvl,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF2B2118) else Color(0xFF8A7B66)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFFF0E3C4), thickness = 1.dp)

                // SHOW VIP TAG TOGGLE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFFEFC2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFFFFB627),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Show VIP tag",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2B2118)
                            )
                            Text(
                                text = "Stand out in the lobby",
                                fontSize = 12.sp,
                                color = Color(0xFF8A7B66)
                            )
                        }
                    }

                    Switch(
                        checked = showVipTag,
                        onCheckedChange = { checked ->
                            if (checked && !isUserVip) {
                                showVipSheet = true
                            } else {
                                showVipTag = checked
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFFB627),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFF0E3C4)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFF0E3C4), thickness = 1.dp)

                // PRIVATE ROOM TOGGLE
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFFFEFC2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF2B2118),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Private room",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2B2118)
                                )
                                Text(
                                    text = "Only people with the code can join",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8A7B66)
                                )
                            }
                        }

                        Switch(
                            checked = isPrivateRoom,
                            onCheckedChange = { checked ->
                                isPrivateRoom = checked
                                if (checked && generatedPassCode.isBlank()) {
                                    generatedPassCode = "FT-${Random.nextInt(1000, 9999)}"
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFFB627),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFF0E3C4)
                            )
                        )
                    }

                    if (isPrivateRoom) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFEFC2),
                            modifier = Modifier.padding(start = 52.dp, bottom = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Code · $generatedPassCode",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2B2118)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CREATE ROOM PRIMARY BUTTON
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(6.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFFFFB627).copy(alpha = 0.5f))
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFFC72C), Color(0xFFFFB627)))
                        )
                        .clickable {
                            onDismiss()
                            onCreateRoom(isPrivateRoom)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Create room",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2B2118)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF2B2118),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }

            // VIP UNLOCK MODAL SHEET
            if (showVipSheet) {
                FunkyTalkVipModalSheet(
                    onDismiss = { showVipSheet = false },
                    onUnlockVip = {
                        isUserVip = true
                        showVipTag = true
                        showVipSheet = false
                        Toast.makeText(context, "VIP Activated!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// VIP UNLOCK MODAL SHEET COMPOSABLE
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunkyTalkVipModalSheet(
    onDismiss: () -> Unit,
    onUnlockVip: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFFF8E7),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Crown Circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFB627)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFF2B2118),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Unlock FunkyTalk VIP",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF2B2118)
            )

            Text(
                text = "Premium features for language lovers.",
                fontSize = 14.sp,
                color = Color(0xFF8A7B66)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Perks List
            val perks = listOf(
                "Upload custom banners" to "Use any image from your gallery.",
                "VIP badge on your rooms" to "Stand out in the lobby.",
                "Exclusive banner collection" to "Limited edition banners.",
                "Early access" to "Try new features first."
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                perks.forEach { (title, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFEFC2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFFFFB627),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2B2118)
                            )
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                color = Color(0xFF8A7B66)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CTA Button
            Button(
                onClick = onUnlockVip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB627)),
                shape = RoundedCornerShape(27.dp)
            ) {
                Text(
                    text = "Unlock VIP Access",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2B2118)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Secure payment · Cancel anytime · 7-day money back",
                fontSize = 12.sp,
                color = Color(0xFF8A7B66)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
