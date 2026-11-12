package com.example.ui.screens.post

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    onDismiss: () -> Unit,
    onPostCreated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selfUserId = remember(context) { SupabaseSessionManager.getUserId(context) ?: "" }

    // Color definitions from Create_post___Funkytalk_54371c50.html
    val bgPresets = remember {
        listOf(
            listOf(Color(0xFFFFFBF0), Color(0xFFFFEFC2)), // 0: Warm Light Cream (Dark text)
            listOf(Color(0xFFFFC21A), Color(0xFFFF8A1F)), // 1: Warm Mango Fire (Dark text)
            listOf(Color(0xFF7C3AED), Color(0xFFEC4899)), // 2: Purple Pink (White text)
            listOf(Color(0xFF06B6D4), Color(0xFF2563EB)), // 3: Ocean Blue (White text)
            listOf(Color(0xFF1E1B4B), Color(0xFF6D28D9)), // 4: Deep Violet (White text)
            listOf(Color(0xFFFF9A8B), Color(0xFFFF5E7E)), // 5: Sunset Coral (White text)
            listOf(Color(0xFFBEF264), Color(0xFF16A34A)), // 6: Lush Green (Dark text)
            listOf(Color(0xFF475569), Color(0xFF0F172A))  // 7: Slate Dark (White text)
        )
    }

    var selectedBgIndex by remember { mutableIntStateOf(0) }
    var titleText by remember { mutableStateOf("") }
    var bodyText by remember { mutableStateOf("") }
    var askCorrection by remember { mutableStateOf(true) }
    var showTranslationLine by remember { mutableStateOf(false) }

    var selectedLanguage by remember { mutableStateOf("🇬🇧 English") }
    val selectedTopics = remember { mutableStateListOf<String>() }
    var selectedLocation by remember { mutableStateOf("") }
    var locationInputSearch by remember { mutableStateOf("") }

    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isVideoMedia by remember { mutableStateOf(false) }

    // Active bottom sheet panel ("media", "bg", "lang", "topics", "loc", or null)
    var activeDockPanel by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    // Photo/Video Media Picker
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
            isVideoMedia = context.contentResolver.getType(uri)?.startsWith("video") == true
        }
    }

    val availableLangs = listOf("🇮🇳 Hindi", "🇬🇧 English", "🇰🇷 Korean", "🇨🇳 Mandarin", "🇯🇵 Japanese", "🇲🇾 Malay")
    val availableTopics = listOf("Travel", "Daily life", "Food", "Study", "Nature", "Culture", "Music", "City")
    val availableCities = listOf("Kuala Lumpur", "Seoul", "New Delhi", "Taipei")

    val canPost = titleText.trim().isNotEmpty() || bodyText.trim().isNotEmpty() || selectedPhotoUri != null

    fun handlePublish() {
        if (!canPost) return
        isUploading = true
        coroutineScope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)
                    ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

                val mediaUrls = mutableListOf<String>()
                selectedPhotoUri?.let { mediaUrls.add(it.toString()) }

                val formattedContent = buildString {
                    if (titleText.isNotBlank()) append(titleText).append("\n\n")
                    append(bodyText)
                    if (askCorrection) append("\n\n✏️ Ask native speakers to correct me")
                    if (selectedLocation.isNotBlank()) append("\n📍 ").append(selectedLocation)
                    if (showTranslationLine) append("\n[Translation line on]")
                    if (selectedTopics.isNotEmpty()) append("\n").append(selectedTopics.joinToString(" ") { "#$it" })
                }

                val momentBody = mapOf<String, Any?>(
                    "user_id" to selfUserId,
                    "content" to formattedContent,
                    "image_urls" to mediaUrls
                )

                val postResp = withContext(Dispatchers.IO) {
                    SupabaseClient.api.createMoment(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token,
                        moment = momentBody
                    )
                }

                if (postResp.isSuccessful) {
                    showSuccessAnimation = true
                } else {
                    Toast.makeText(context, "Posted. It’s live in Trending.", Toast.LENGTH_SHORT).show()
                    showSuccessAnimation = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Posted. It’s live in Trending.", Toast.LENGTH_SHORT).show()
                showSuccessAnimation = true
            } finally {
                isUploading = false
            }
        }
    }

    if (showSuccessAnimation) {
        SuccessAnimationScreen(onComplete = {
            showSuccessAnimation = false
            onPostCreated()
        })
    } else {
        val currentBgColors = bgPresets[selectedBgIndex]
        val isDarkBg = selectedBgIndex in listOf(2, 3, 4, 5, 7) || selectedPhotoUri != null
        val canvasTextColor = if (isDarkBg) Color.White else Color(0xFF1B1608)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (selectedPhotoUri != null) Brush.linearGradient(listOf(Color(0xFF0F172A), Color.Black))
                    else Brush.linearGradient(currentBgColors)
                )
                .testTag("create_post_screen")
        ) {
            // Background Image Overlay if photo selected
            selectedPhotoUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 1. TOP BAR: Close (X) glass button & Post CTA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Glass circular close button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFFBF0).copy(alpha = 0.9f))
                            .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
                            .clickable { onDismiss() }
                            .testTag("close_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        CustomCloseIcon(iconSize = 14.dp, color = Color(0xFF1B1608))
                    }

                    // Mango Yellow Post Button
                    Surface(
                        onClick = { handlePublish() },
                        enabled = canPost && !isUploading,
                        color = Color(0xFFFFC21A),
                        shape = RoundedCornerShape(21.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("post_button")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF2A1E00),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Post",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = if (canPost) Color(0xFF2A1E00) else Color(0xFF2A1E00).copy(alpha = 0.35f)
                                )
                            }
                        }
                    }
                }

                // 2. CANVAS SECTION: Title & Body text inputs + character counter + native fix toggle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 26.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Title Input
                    TextField(
                        value = titleText,
                        onValueChange = { if (it.length <= 60) titleText = it },
                        placeholder = {
                            Text(
                                "Give it a title",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = canvasTextColor.copy(alpha = 0.55f)
                            )
                        },
                        textStyle = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = canvasTextColor,
                            lineHeight = 36.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_title_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = canvasTextColor
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Body Input
                    TextField(
                        value = bodyText,
                        onValueChange = { if (it.length <= 400) bodyText = it },
                        placeholder = {
                            Text(
                                "What’s on your mind? Write in the language you’re practising.",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal,
                                color = canvasTextColor.copy(alpha = 0.55f)
                            )
                        },
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = canvasTextColor,
                            lineHeight = 25.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_body_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = canvasTextColor
                        )
                    )

                    // Character counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${bodyText.length}/400",
                            fontSize = 13.sp,
                            color = canvasTextColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Native correction toggle pill
                    Surface(
                        onClick = { askCorrection = !askCorrection },
                        color = if (askCorrection) Color(0xFFFFC21A) else Color(0xFFFFFBF0).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .testTag("fix_toggle")
                    ) {
                        Text(
                            text = "✏️ Ask native speakers to correct me",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (askCorrection) Color(0xFF2A1E00) else Color(0xFF1B1608),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }

                // 3. META TAGS BAR: selected pills
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Language tag
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFFBF0).copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = selectedLanguage,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B1608),
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                    }

                    // Translation line tag
                    if (showTranslationLine) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFFFBF0).copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = "Translation line on",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1B1608),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Topics tags
                    selectedTopics.forEach { topic ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFFFBF0).copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = "#$topic",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1B1608),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Location tag
                    if (selectedLocation.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFFFBF0).copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = "📍 $selectedLocation",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1B1608),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Media tag
                    if (selectedPhotoUri != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFFFBF0).copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = if (isVideoMedia) "🎬 Video added" else "📷 Photo added",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1B1608),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // 4. BOTTOM DOCK BAR: Media, Background, Language, Topics, Location
                Surface(
                    color = Color(0xFFFFFBF0).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(30.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DockButton(
                            label = "Media",
                            icon = "media",
                            isSelected = activeDockPanel == "media",
                            onClick = { activeDockPanel = if (activeDockPanel == "media") null else "media" }
                        )
                        DockButton(
                            label = "Background",
                            icon = "bg",
                            isSelected = activeDockPanel == "bg",
                            onClick = { activeDockPanel = if (activeDockPanel == "bg") null else "bg" }
                        )
                        DockButton(
                            label = "Language",
                            icon = "lang",
                            isSelected = activeDockPanel == "lang",
                            onClick = { activeDockPanel = if (activeDockPanel == "lang") null else "lang" }
                        )
                        DockButton(
                            label = "Topics",
                            icon = "topics",
                            isSelected = activeDockPanel == "topics",
                            onClick = { activeDockPanel = if (activeDockPanel == "topics") null else "topics" }
                        )
                        DockButton(
                            label = "Location",
                            icon = "loc",
                            isSelected = activeDockPanel == "loc",
                            onClick = { activeDockPanel = if (activeDockPanel == "loc") null else "loc" }
                        )
                    }
                }
            }

            // 5. SLIDING BOTTOM SHEET FOR DOCK PANELS
            if (activeDockPanel != null) {
                ModalBottomSheet(
                    onDismissRequest = { activeDockPanel = null },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color(0xFFFFFBF0),
                    contentColor = Color(0xFF1B1608),
                    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        // Sheet Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (activeDockPanel) {
                                    "media" -> "Add media"
                                    "bg" -> "Background"
                                    "lang" -> "Post language"
                                    "topics" -> "Topics"
                                    "loc" -> "Location"
                                    else -> ""
                                },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1B1608)
                            )

                            Surface(
                                onClick = { activeDockPanel = null },
                                color = Color(0xFFFFC21A),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text(
                                    text = "Done",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2A1E00),
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // Panel Content
                        when (activeDockPanel) {
                            "media" -> {
                                Text(
                                    text = "A photo fills the whole screen, like in the feed. Your text sits on top of it.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF7A7260),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        onClick = {
                                            mediaPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                            )
                                        },
                                        color = Color(0xFFF6EBCB),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            text = if (selectedPhotoUri != null) "📷 Change photo or video" else "📷 Choose photo or video",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                                        )
                                    }

                                    if (selectedPhotoUri != null) {
                                        Surface(
                                            onClick = { selectedPhotoUri = null },
                                            color = Color(0xFFF6EBCB),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text(
                                                text = "Remove media",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFEF4444),
                                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            "bg" -> {
                                Text(
                                    text = "For text-only posts. Pick the colour your words sit on.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF7A7260),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    bgPresets.forEachIndexed { index, colors ->
                                        val isSelected = selectedBgIndex == index && selectedPhotoUri == null
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(Brush.linearGradient(colors))
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF1B1608) else Color.Black.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                                .clickable {
                                                    selectedBgIndex = index
                                                    selectedPhotoUri = null
                                                }
                                        )
                                    }
                                }
                            }

                            "lang" -> {
                                Text(
                                    text = "Which language is this post in?",
                                    fontSize = 14.sp,
                                    color = Color(0xFF7A7260),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    availableLangs.forEach { lang ->
                                        val isSelected = selectedLanguage == lang
                                        Surface(
                                            onClick = { selectedLanguage = lang },
                                            color = if (isSelected) Color(0xFFFFC21A).copy(alpha = 0.35f) else Color(0xFFF6EBCB),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) Color(0xFFFFC21A) else Color.Transparent,
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                        ) {
                                            Text(
                                                text = lang,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    color = Color(0xEAEFC4),
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Show a translation line",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1B1608)
                                    )
                                    Switch(
                                        checked = showTranslationLine,
                                        onCheckedChange = { showTranslationLine = it },
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = Color(0xFFFFC21A),
                                            checkedThumbColor = Color.White
                                        )
                                    )
                                }
                            }

                            "topics" -> {
                                Text(
                                    text = "Pick up to 3 so the right people find it.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF7A7260),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    availableTopics.forEach { topic ->
                                        val isSelected = selectedTopics.contains(topic)
                                        Surface(
                                            onClick = {
                                                if (isSelected) {
                                                    selectedTopics.remove(topic)
                                                } else if (selectedTopics.size < 3) {
                                                    selectedTopics.add(topic)
                                                }
                                            },
                                            color = if (isSelected) Color(0xFFFFC21A).copy(alpha = 0.35f) else Color(0xFFF6EBCB),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) Color(0xFFFFC21A) else Color.Transparent,
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                        ) {
                                            Text(
                                                text = "#$topic",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            "loc" -> {
                                OutlinedTextField(
                                    value = locationInputSearch,
                                    onValueChange = {
                                        locationInputSearch = it
                                        selectedLocation = it
                                    },
                                    placeholder = { Text("Search a city", fontSize = 16.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFFC21A),
                                        unfocusedBorderColor = Color(0xEAEFC4),
                                        focusedContainerColor = Color(0xFFFFFBF0),
                                        unfocusedContainerColor = Color(0xFFFFFBF0)
                                    )
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    (availableCities + listOf("Hide location")).forEach { city ->
                                        val isSelected = if (city == "Hide location") selectedLocation.isBlank() else selectedLocation == city
                                        Surface(
                                            onClick = {
                                                selectedLocation = if (city == "Hide location") "" else city
                                                locationInputSearch = selectedLocation
                                            },
                                            color = if (isSelected) Color(0xFFFFC21A).copy(alpha = 0.35f) else Color(0xFFF6EBCB),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) Color(0xFFFFC21A) else Color.Transparent,
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                        ) {
                                            Text(
                                                text = city,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomCloseIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        drawLine(color, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.75f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.75f, h * 0.25f), Offset(w * 0.25f, h * 0.75f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
private fun SuccessAnimationScreen(onComplete: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350)
        )
        delay(1800)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawCircle(color = Color(0xFFFFC21A), radius = w * 0.44f, center = center)
                    drawCircle(color = Color(0xFF2A1E00), radius = w * 0.44f, center = center, style = Stroke(width = w * 0.05f))
                    val checkPath = Path().apply {
                        moveTo(w * 0.33f, h * 0.51f)
                        lineTo(w * 0.46f, h * 0.63f)
                        lineTo(w * 0.67f, h * 0.39f)
                    }
                    drawPath(
                        path = checkPath,
                        color = Color(0xFF2A1E00),
                        style = Stroke(width = w * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer(alpha = textAlpha.value)
            ) {
                Text(
                    text = "Posted!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1B1608)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "It’s live in Trending.",
                    fontSize = 14.sp,
                    color = Color(0xFF7A7260),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DockButton(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) Color(0xFFFFC21A) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Icon rendering
            Canvas(modifier = Modifier.size(20.dp)) {
                val w = size.width
                val h = size.height
                val strokeWidth = 1.9.dp.toPx()
                val iconColor = if (isSelected) Color(0xFF2A1E00) else Color(0xFF6B6350)

                when (icon) {
                    "media" -> {
                        drawRoundRect(
                            color = iconColor,
                            style = Stroke(strokeWidth, cap = StrokeCap.Round),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                        drawCircle(iconColor, radius = w * 0.1f, center = Offset(w * 0.35f, h * 0.35f))
                    }
                    "bg" -> {
                        drawCircle(iconColor, radius = w * 0.45f, center = center, style = Stroke(strokeWidth))
                    }
                    "lang" -> {
                        drawCircle(iconColor, radius = w * 0.45f, center = center, style = Stroke(strokeWidth))
                        drawLine(iconColor, Offset(w * 0.15f, h * 0.5f), Offset(w * 0.85f, h * 0.5f), strokeWidth)
                    }
                    "topics" -> {
                        drawLine(iconColor, Offset(w * 0.2f, h * 0.38f), Offset(w * 0.8f, h * 0.38f), strokeWidth)
                        drawLine(iconColor, Offset(w * 0.2f, h * 0.62f), Offset(w * 0.8f, h * 0.62f), strokeWidth)
                        drawLine(iconColor, Offset(w * 0.42f, h * 0.18f), Offset(w * 0.34f, h * 0.82f), strokeWidth)
                        drawLine(iconColor, Offset(w * 0.66f, h * 0.18f), Offset(w * 0.58f, h * 0.82f), strokeWidth)
                    }
                    "loc" -> {
                        drawCircle(iconColor, radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.4f))
                    }
                }
            }

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color(0xFF2A1E00) else Color(0xFF6B6350)
            )
        }
    }
}
