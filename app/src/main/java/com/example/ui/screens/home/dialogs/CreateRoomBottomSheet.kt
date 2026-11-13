package com.example.ui.screens.home.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.AuthState
import com.example.AuthViewModel
import com.example.CloudinaryUploader
import com.example.R
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.SupabaseVoiceRoom
import com.example.ui.components.AppLoadingManager
import com.example.ui.components.LanguageIcon
import com.example.ui.components.RoomLanguageSelectionSheet
import com.example.ui.components.getProgrammingIconUrl
import com.example.ui.screens.LanguageData
import com.example.ui.theme.LocalAppDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

private fun parseBannerGradientColors(gradientSpec: String): List<Color> {
    val hexes = gradientSpec.removePrefix("gradient:").split(",")
    return hexes.mapNotNull { hex ->
        try {
            val clean = hex.trim()
            val formatted = if (clean.startsWith("#")) clean else "#$clean"
            Color(android.graphics.Color.parseColor(formatted))
        } catch (_: Exception) {
            null
        }
    }.ifEmpty { listOf(Color(0xFF6366F1), Color(0xFFEC4899)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomBottomSheet(
    viewModel: AuthViewModel,
    initialIsPrivate: Boolean = false,
    isSuspended: Boolean = false,
    onDismiss: () -> Unit,
    onRoomCreated: (createdRoomId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalAppDarkTheme.current

    val bgColor = if (isDark) Color(0xFF16120D) else Color(0xFFFFF8E7)
    val surfColor = if (isDark) Color(0xFF231D15) else Color.White
    val softSurface = if (isDark) Color(0xFF3A2D12) else Color(0xFFFFEFC2)
    val inkColor = if (isDark) Color(0xFFFFF3DC) else Color(0xFF2B2118)
    val muteColor = if (isDark) Color(0xFFA89A82) else Color(0xFF8A7B66)
    val lineColor = if (isDark) Color(0xFF34291A) else Color(0xFFF0E3C4)
    val mangoColor = Color(0xFFFFB627)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val authState by viewModel.authState.collectAsState()
    val userProfile = (authState as? AuthState.Success)?.user
    val userAvatar = userProfile?.avatar?.takeIf { it.isNotBlank() }
        ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"

    val defaultLanguage = remember {
        LanguageData.popularLanguages.firstOrNull { it.name == "Japanese" }
            ?: LanguageData.allLanguages.first()
    }
    var selectedLanguage by remember { mutableStateOf(defaultLanguage) }

    val suggestedBanners = remember(selectedLanguage.name) {
        languageBanners[selectedLanguage.name] ?: defaultBannersList
    }

    var selectedBannerTab by remember { mutableStateOf("Suggested") }
    var selectedBannerOption by remember { mutableStateOf(suggestedBanners.first()) }
    var customBannerUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(selectedLanguage.name) {
        if (customBannerUri == null && selectedBannerTab == "Suggested") {
            selectedBannerOption = suggestedBanners.first()
        }
    }

    var roomTitle by remember { mutableStateOf("") }
    var roomDescription by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("Beginner") }

    var isUserVip by remember { mutableStateOf(userProfile?.isEmailVerified == true) }
    var showVipTag by remember { mutableStateOf(false) }
    var isPrivateRoom by remember { mutableStateOf(initialIsPrivate) }
    var joinCodeInput by remember { mutableStateOf((100000..999999).random().toString()) }

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showVipSheet by remember { mutableStateOf(false) }
    var isCreatingRoom by remember { mutableStateOf(false) }

    var showCreatedSuccessDialog by remember { mutableStateOf(false) }
    var createdSuccessRoomId by remember { mutableStateOf("") }
    var createdSuccessJoinCode by remember { mutableStateOf("") }
    var createdSuccessRoomTitle by remember { mutableStateOf("") }

    val pickCustomBannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customBannerUri = uri
        }
    }

    val activeBannerCatalog = remember(selectedBannerTab, suggestedBanners) {
        when (selectedBannerTab) {
            "India" -> BannerPack.indianScenics
            "GIFs" -> BannerPack.animatedGifs
            "World" -> BannerPack.worldwideScenics
            "Cozy" -> BannerPack.cozyVibes
            "Anime" -> BannerPack.animeAndAnimated
            else -> suggestedBanners
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isCreatingRoom) onDismiss() },
        sheetState = sheetState,
        containerColor = bgColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(lineColor)
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
                        text = if (isPrivateRoom) "New private room" else "New room",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = inkColor
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(softSurface)
                            .testTag("create_room_close_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = inkColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // LIVE ROOM PREVIEW CARD
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF2B2118))
                ) {
                    when {
                        customBannerUri != null -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(customBannerUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Custom room banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        selectedBannerOption.url.startsWith("gradient:") -> {
                            val colors = parseBannerGradientColors(selectedBannerOption.url)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(colors))
                            )
                        }
                        else -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(selectedBannerOption.url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = selectedBannerOption.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Bottom Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.22f),
                                        Color.Black.copy(alpha = 0.78f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Frosted Badges Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = Color(0x8C14100A)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    LanguageIcon(
                                        language = selectedLanguage.flagCode.ifBlank { selectedLanguage.name },
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedLanguage.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (showVipTag) {
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = mangoColor
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_crown_filled),
                                            contentDescription = null,
                                            tint = Color(0xFF2B2118),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "VIP",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF2B2118)
                                        )
                                    }
                                }
                            }

                            if (isPrivateRoom) {
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = Color(0x8C14100A)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_lock),
                                            contentDescription = null,
                                            tint = mangoColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
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

                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = Color(0x8C14100A)
                            ) {
                                Text(
                                    text = "$selectedLevel · Voice room",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.92f),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Preview Title & Subtitle
                        Column {
                            Text(
                                text = roomTitle.ifBlank { "Your room title" },
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (roomTitle.isNotBlank()) Color.White else Color.White.copy(alpha = 0.62f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = roomDescription.ifBlank { "Add a short description" },
                                fontSize = 13.sp,
                                color = if (roomDescription.isNotBlank()) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // BANNER PICKER SECTION + CATEGORY TABS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Banner",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = inkColor
                    )
                    Text(
                        text = if (customBannerUri != null) "Custom Gallery" else selectedBannerOption.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = muteColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Tabs
                val bannerTabs = listOf("Suggested", "India", "GIFs", "World", "Cozy", "Anime")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(bannerTabs) { tab ->
                        val isActive = selectedBannerTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (isActive) inkColor else softSurface)
                                .clickable { selectedBannerTab = tab }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tab,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) bgColor else muteColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal Banner Swatches + Custom VIP Upload
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        val isCustomSelected = customBannerUri != null
                        Box(
                            modifier = Modifier
                                .width(108.dp)
                                .height(76.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(softSurface)
                                .border(
                                    width = if (isCustomSelected) 3.dp else 1.5.dp,
                                    color = if (isCustomSelected) mangoColor else muteColor.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    if (!isUserVip) {
                                        showVipSheet = true
                                    } else {
                                        pickCustomBannerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                }
                                .testTag("banner_custom_upload_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customBannerUri != null) {
                                AsyncImage(
                                    model = customBannerUri,
                                    contentDescription = "Selected custom banner",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_photo),
                                        contentDescription = "Upload banner",
                                        tint = inkColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Upload",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = muteColor
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = mangoColor,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "VIP",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2B2118)
                                    )
                                    if (!isUserVip) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_lock),
                                            contentDescription = "Locked",
                                            tint = Color(0xFF2B2118),
                                            modifier = Modifier.size(9.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(activeBannerCatalog, key = { it.name + "_" + it.url }) { banner ->
                        val isSelected = customBannerUri == null && selectedBannerOption.url == banner.url
                        Box(
                            modifier = Modifier
                                .width(108.dp)
                                .height(76.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) mangoColor else lineColor,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    customBannerUri = null
                                    selectedBannerOption = banner
                                }
                        ) {
                            if (banner.url.startsWith("gradient:")) {
                                val colors = parseBannerGradientColors(banner.url)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(colors))
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(banner.url)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = banner.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            if (banner.url.contains(".gif", ignoreCase = true)) {
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = Color(0xBA16120D),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "GIF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = mangoColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(mangoColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Selected",
                                        tint = Color(0xFF2B2118),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ROOM TITLE FIELD
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(softSurface)
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
                            color = muteColor
                        )
                        Text(
                            text = "${roomTitle.length}/50",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = muteColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicTextField(
                        value = roomTitle,
                        onValueChange = { if (it.length <= 50) roomTitle = it },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = inkColor
                        ),
                        cursorBrush = SolidColor(mangoColor),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (roomTitle.isEmpty()) {
                                Text(
                                    text = "e.g., Let's talk in ${selectedLanguage.name}!",
                                    fontSize = 15.sp,
                                    color = muteColor.copy(alpha = 0.7f)
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_room_title_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // DESCRIPTION FIELD
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(softSurface)
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
                            color = muteColor
                        )
                        Text(
                            text = "${roomDescription.length}/120",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = muteColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicTextField(
                        value = roomDescription,
                        onValueChange = { if (it.length <= 120) roomDescription = it },
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = inkColor
                        ),
                        cursorBrush = SolidColor(mangoColor),
                        maxLines = 3,
                        decorationBox = { innerTextField ->
                            if (roomDescription.isEmpty()) {
                                Text(
                                    text = "Add topic guidelines or target practice levels...",
                                    fontSize = 14.sp,
                                    color = muteColor.copy(alpha = 0.7f)
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_room_description_input")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // LANGUAGE SELECTOR ROW (Opens RoomLanguageSelectionSheet)
                Text(
                    text = "Language",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = inkColor
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(softSurface)
                        .border(1.dp, lineColor, RoundedCornerShape(22.dp))
                        .clickable { showLanguageSheet = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("create_room_language_selector"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(surfColor)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LanguageIcon(
                                language = selectedLanguage.flagCode.ifBlank { selectedLanguage.name },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = selectedLanguage.name,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkColor
                            )
                            Text(
                                text = "${selectedLanguage.nativeName} · Tap to change",
                                fontSize = 12.sp,
                                color = muteColor
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(surfColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_down),
                            contentDescription = "Select language",
                            tint = inkColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // LEVEL SEGMENTED CONTROL (Beginner, Intermediate, Advanced)
                Text(
                    text = "Level",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = inkColor
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(99.dp))
                        .background(softSurface)
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val levels = listOf(
                        "Beginner" to R.drawable.ic_sprout,
                        "Intermediate" to R.drawable.ic_bars,
                        "Advanced" to R.drawable.ic_star
                    )
                    levels.forEach { (lvl, iconRes) ->
                        val isSelected = selectedLevel == lvl
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (isSelected) surfColor else Color.Transparent)
                                .clickable { selectedLevel = lvl }
                                .testTag("create_room_level_${lvl.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    tint = if (isSelected) mangoColor else muteColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = lvl,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) inkColor else muteColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = lineColor, thickness = 1.5.dp)

                // SHOW VIP TAG ROW
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(softSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_crown_filled),
                                contentDescription = null,
                                tint = mangoColor,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Show VIP tag",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkColor
                            )
                            Text(
                                text = "Stand out in the lobby",
                                fontSize = 12.5.sp,
                                color = muteColor
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
                            checkedThumbColor = surfColor,
                            checkedTrackColor = mangoColor,
                            uncheckedThumbColor = surfColor,
                            uncheckedTrackColor = lineColor
                        )
                    )
                }

                HorizontalDivider(color = lineColor, thickness = 1.5.dp)

                // PRIVATE ROOM ROW + 6-DIGIT PASSCODE EDITOR
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(softSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_lock),
                                    contentDescription = null,
                                    tint = inkColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Private room",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = inkColor
                                )
                                Text(
                                    text = "Only people with the 6-digit code can join",
                                    fontSize = 12.5.sp,
                                    color = muteColor
                                )
                            }
                        }

                        Switch(
                            checked = isPrivateRoom,
                            onCheckedChange = { checked ->
                                isPrivateRoom = checked
                                if (checked && joinCodeInput.length != 6) {
                                    joinCodeInput = (100000..999999).random().toString()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = surfColor,
                                checkedTrackColor = mangoColor,
                                uncheckedThumbColor = surfColor,
                                uncheckedTrackColor = lineColor
                            ),
                            modifier = Modifier.testTag("create_room_private_switch")
                        )
                    }

                    AnimatedVisibility(visible = isPrivateRoom) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 54.dp, bottom = 12.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(softSurface)
                                .border(1.dp, lineColor, RoundedCornerShape(18.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "6-DIGIT PASSCODE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = muteColor,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                BasicTextField(
                                    value = joinCodeInput,
                                    onValueChange = { next ->
                                        val digitsOnly = next.filter { it.isDigit() }
                                        if (digitsOnly.length <= 6) {
                                            joinCodeInput = digitsOnly
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = inkColor,
                                        letterSpacing = 2.sp
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    cursorBrush = SolidColor(mangoColor),
                                    singleLine = true,
                                    modifier = Modifier.testTag("create_room_passcode_input")
                                )
                            }

                            IconButton(
                                onClick = {
                                    joinCodeInput = (100000..999999).random().toString()
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(surfColor)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_refresh),
                                    contentDescription = "Generate new passcode",
                                    tint = inkColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // PRIMARY CTA BUTTON: CREATE ROOM
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(8.dp, RoundedCornerShape(99.dp), spotColor = mangoColor.copy(alpha = 0.45f))
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFFC72C), mangoColor))
                        )
                        .clickable(enabled = !isCreatingRoom) {
                            if (isSuspended) {
                                Toast.makeText(
                                    context,
                                    "Cannot create room: Your account is currently suspended.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@clickable
                            }

                            if (isPrivateRoom) {
                                if (joinCodeInput.isBlank()) {
                                    joinCodeInput = (100000..999999).random().toString()
                                }
                                if (joinCodeInput.length != 6) {
                                    Toast.makeText(
                                        context,
                                        "Passcode must be exactly 6 digits!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@clickable
                                }
                            }

                            val hostId = SupabaseSessionManager.getUserId(context)
                                ?: userProfile?.uid
                                ?: UUID.randomUUID().toString()
                            val bearerToken = SupabaseSessionManager.getBearerToken(context)
                            val roomUuid = UUID.randomUUID().toString()
                            val chosenTitle = roomTitle.trim().ifBlank {
                                if (isPrivateRoom) "Private ${selectedLanguage.name} Room"
                                else "Let's talk in ${selectedLanguage.name}!"
                            }
                            val chosenDesc = roomDescription.trim().ifBlank {
                                "Daily conversation practice in ${selectedLanguage.name}"
                            }

                            isCreatingRoom = true
                            AppLoadingManager.show("Setting up your voice room...")

                            scope.launch {
                                try {
                                    val resolvedBannerUrl = if (customBannerUri != null) {
                                        val uploadRes = withContext(Dispatchers.IO) {
                                            CloudinaryUploader.uploadMomentMedia(
                                                context = context,
                                                token = bearerToken,
                                                uri = customBannerUri!!
                                            )
                                        }
                                        val uploaded = uploadRes.getOrNull()?.url
                                        if (uploaded.isNullOrBlank()) {
                                            AppLoadingManager.hide()
                                            isCreatingRoom = false
                                            Toast.makeText(
                                                context,
                                                "Failed to upload custom banner. Please try again.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@launch
                                        }
                                        uploaded
                                    } else {
                                        selectedBannerOption.url
                                    }

                                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                                        timeZone = TimeZone.getTimeZone("UTC")
                                    }
                                    val nowIso = isoFormat.format(Date())

                                    val newRoom = SupabaseVoiceRoom(
                                        id = roomUuid,
                                        title = chosenTitle,
                                        language = selectedLanguage.name,
                                        host_id = hostId,
                                        participants = 1,
                                        created_at = nowIso,
                                        country_code = selectedLanguage.flagCode,
                                        tag = selectedLevel,
                                        room_type = "voice",
                                        description = chosenDesc,
                                        banner_image_url = resolvedBannerUrl,
                                        card_background_type = "image",
                                        host_is_verified = false,
                                        host_is_vip = (isUserVip || showVipTag),
                                        is_live = true,
                                        language_code = selectedLanguage.flagCode,
                                        language_flag = getProgrammingIconUrl(selectedLanguage.name).ifBlank {
                                            if (selectedLanguage.flagCode.startsWith("prog_")) {
                                                getProgrammingIconUrl(selectedLanguage.flagCode)
                                            } else {
                                                selectedLanguage.flagCode
                                            }
                                        },
                                        level_tag = selectedLevel,
                                        listening_count = 0,
                                        speaking_count = 1,
                                        preview_avatars = listOf(userAvatar),
                                        status = "active",
                                        status_tag = "NEW",
                                        total_visits_24h = 1,
                                        last_1h_joins = 0,
                                        last_2h_growth_rate = 0.0,
                                        last_activity_at = nowIso,
                                        is_private = isPrivateRoom,
                                        join_code = if (isPrivateRoom) joinCodeInput else null
                                    )

                                    val insertRes = withContext(Dispatchers.IO) {
                                        SupabaseClient.api.createVoiceRoom(
                                            auth = bearerToken,
                                            room = newRoom
                                        )
                                    }

                                    if (!insertRes.isSuccessful) {
                                        AppLoadingManager.hide()
                                        isCreatingRoom = false
                                        Toast.makeText(
                                            context,
                                            "Could not create room (${insertRes.code()}). Try again.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@launch
                                    }

                                    AppLoadingManager.hide()
                                    viewModel.joinRoom(
                                        roomId = roomUuid,
                                        title = chosenTitle,
                                        context = context,
                                        role = "speaker",
                                        hostId = hostId
                                    )

                                    if (isPrivateRoom) {
                                        createdSuccessRoomId = roomUuid
                                        createdSuccessJoinCode = joinCodeInput
                                        createdSuccessRoomTitle = chosenTitle
                                        showCreatedSuccessDialog = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Voice Room Created Successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onRoomCreated(roomUuid)
                                    }
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    AppLoadingManager.hide()
                                    Toast.makeText(
                                        context,
                                        e.message ?: "Failed to create room",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    isCreatingRoom = false
                                }
                            }
                        }
                        .testTag("create_room_submit_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sparkle),
                            contentDescription = null,
                            tint = Color(0xFF2B2118),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create room",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2B2118)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null,
                            tint = Color(0xFF2B2118),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            if (showLanguageSheet) {
                RoomLanguageSelectionSheet(
                    selectedLanguageName = selectedLanguage.name,
                    isDark = isDark,
                    onLanguageSelected = { lang ->
                        selectedLanguage = lang
                        val nextBanners = languageBanners[lang.name] ?: defaultBannersList
                        if (customBannerUri == null && selectedBannerTab == "Suggested") {
                            selectedBannerOption = nextBanners.first()
                        }
                    },
                    onDismiss = { showLanguageSheet = false }
                )
            }

            if (showVipSheet) {
                FunkyTalkVipModalSheet(
                    isDark = isDark,
                    onDismiss = { showVipSheet = false },
                    onUnlockVip = {
                        isUserVip = true
                        showVipTag = true
                        showVipSheet = false
                        Toast.makeText(context, "FunkyTalk VIP Unlocked!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (showCreatedSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { /* Require explicit action */ },
                    containerColor = surfColor,
                    shape = RoundedCornerShape(28.dp),
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(softSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_lock),
                                    contentDescription = null,
                                    tint = mangoColor,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Private Room Created!",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = inkColor
                            )
                        }
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Share this 6-digit code with friends so they can join:",
                                fontSize = 13.sp,
                                color = muteColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = softSurface,
                                border = BorderStroke(1.5.dp, mangoColor)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "ROOM PASSCODE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = muteColor,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = createdSuccessJoinCode.chunked(3).joinToString(" "),
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = inkColor,
                                        letterSpacing = 3.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText("FunkyTalk Room Code", createdSuccessJoinCode)
                                        )
                                        Toast.makeText(
                                            context,
                                            "Code $createdSuccessJoinCode copied!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, lineColor)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_copy),
                                        contentDescription = null,
                                        tint = inkColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = inkColor)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Join my private voice room \"$createdSuccessRoomTitle\" on FunkyTalk! Passcode: $createdSuccessJoinCode"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Room Code"))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, lineColor)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_share),
                                        contentDescription = null,
                                        tint = inkColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = inkColor)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCreatedSuccessDialog = false
                                onRoomCreated(createdSuccessRoomId)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = mangoColor)
                        ) {
                            Text(
                                text = "Enter Room Now",
                                color = Color(0xFF2B2118),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunkyTalkVipModalSheet(
    isDark: Boolean = LocalAppDarkTheme.current,
    onDismiss: () -> Unit,
    onUnlockVip: () -> Unit
) {
    val context = LocalContext.current
    val bgColor = if (isDark) Color(0xFF16120D) else Color(0xFFFFF8E7)
    val surfColor = if (isDark) Color(0xFF231D15) else Color.White
    val softSurface = if (isDark) Color(0xFF3A2D12) else Color(0xFFFFEFC2)
    val inkColor = if (isDark) Color(0xFFFFF3DC) else Color(0xFF2B2118)
    val muteColor = if (isDark) Color(0xFFA89A82) else Color(0xFF8A7B66)
    val lineColor = if (isDark) Color(0xFF34291A) else Color(0xFFF0E3C4)
    val mangoColor = Color(0xFFFFB627)
    val okColor = Color(0xFF22B573)

    var selectedPlan by remember { mutableStateOf("yearly") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bgColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(lineColor)
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .border(8.dp, softSurface, CircleShape)
                    .clip(CircleShape)
                    .background(mangoColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_crown_filled),
                    contentDescription = null,
                    tint = Color(0xFF2B2118),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Unlock FunkyTalk VIP",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = inkColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Premium features for language lovers.",
                fontSize = 14.sp,
                color = muteColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            val perks = listOf(
                Triple(R.drawable.ic_palette, "Upload custom banners", "Use any image from your gallery."),
                Triple(R.drawable.ic_crown_filled, "VIP badge on your rooms", "Stand out in the lobby."),
                Triple(R.drawable.ic_sparkle, "Exclusive banner collection", "Limited edition banners."),
                Triple(R.drawable.ic_rocket, "Early access", "Try new features first.")
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                perks.forEach { (iconRes, title, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(softSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = mangoColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkColor
                            )
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                color = muteColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Plan Selector Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = { selectedPlan = "monthly" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    color = surfColor,
                    border = BorderStroke(
                        width = if (selectedPlan == "monthly") 2.5.dp else 1.5.dp,
                        color = if (selectedPlan == "monthly") mangoColor else lineColor
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Monthly", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = muteColor)
                        Text("₹249 / mo", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = inkColor)
                        Text("Cancel anytime", fontSize = 11.sp, color = muteColor)
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { selectedPlan = "yearly" },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = surfColor,
                        border = BorderStroke(
                            width = if (selectedPlan == "yearly") 2.5.dp else 1.5.dp,
                            color = if (selectedPlan == "yearly") mangoColor else lineColor
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Yearly", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = muteColor)
                            Text("₹1,999 / yr", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = inkColor)
                            Text("Save 33%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = okColor)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = mangoColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-10).dp, y = (-8).dp)
                    ) {
                        Text(
                            text = "Best Value",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2B2118),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onUnlockVip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = mangoColor),
                shape = RoundedCornerShape(27.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_crown_filled),
                    contentDescription = null,
                    tint = Color(0xFF2B2118),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlock VIP Access",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2B2118)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = Color(0xFF2B2118),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = null,
                    tint = muteColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Secure payment · Cancel anytime · 7-day money back",
                    fontSize = 11.5.sp,
                    color = muteColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Restore Purchases",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = muteColor,
                modifier = Modifier.clickable {
                    Toast.makeText(context, "Purchases restored.", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
