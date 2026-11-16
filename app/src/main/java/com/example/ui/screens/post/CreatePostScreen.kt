package com.example.ui.screens.post

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.MomentDto
import com.example.MomentLimitsDto
import com.example.MomentTopicDto
import com.example.R
import com.example.SupabaseSessionManager
import com.example.repository.MomentsRepository
import com.example.ui.components.AppLoadingManager
import com.example.ui.components.CircleFlag
import com.example.ui.components.LocationConsentDialog
import com.example.ui.screens.LanguageData
import com.example.ui.screens.profile.components.PlaceDbService
import com.example.ui.screens.profile.components.PlaceSettlement
import com.example.util.LocationDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    defaultLanguage: String = "",
    onDismiss: () -> Unit,
    onPostCreated: (MomentDto) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selfUserId = remember(context) { SupabaseSessionManager.getUserId(context) ?: "" }
    val prefs = remember(context) {
        context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE)
    }

    val bgPresets = remember { MomentsRepository.bgPresets }

    var optionsTopics by remember { mutableStateOf(MomentsRepository.getCachedOptions().topics) }
    var limits by remember { mutableStateOf(MomentsRepository.getCachedOptions().limits) }

    LaunchedEffect(Unit) {
        MomentsRepository.getMomentOptions(context).onSuccess { opts ->
            if (opts.topics.isNotEmpty()) optionsTopics = opts.topics
            limits = opts.limits
        }
    }

    val initialLanguage = remember(defaultLanguage, selfUserId) {
        val raw = defaultLanguage.takeIf { it.isNotBlank() }
            ?: prefs.getString("profile_nativeLanguage_${selfUserId}", null)
            ?: "Hindi"
        val firstEntry = raw.split(",").firstOrNull()?.trim()
        MomentsRepository.stripLanguageLevel(firstEntry).ifBlank { "Hindi" }
    }

    var selectedBgIndex by remember { mutableIntStateOf(0) }
    var titleText by remember { mutableStateOf("") }
    var bodyText by remember { mutableStateOf("") }
    var translationText by remember { mutableStateOf("") }
    var askCorrection by remember { mutableStateOf(true) }
    var showTranslationLine by remember { mutableStateOf(false) }

    var selectedLanguage by remember { mutableStateOf(initialLanguage) }
    var languageSearchQuery by remember { mutableStateOf("") }
    val selectedTopicSlugs = remember { mutableStateListOf<String>() }
    var customTopicInput by remember { mutableStateOf("") }
    val customTopics = remember { mutableStateListOf<MomentTopicDto>() }

    var selectedLocation by remember { mutableStateOf("") }
    var locationInputSearch by remember { mutableStateOf("") }
    var showLocationConsentDialog by remember { mutableStateOf(false) }
    var isDetectingLocation by remember { mutableStateOf(false) }
    var isSearchingLocation by remember { mutableStateOf(false) }
    var suggestedLocations by remember { mutableStateOf<List<PlaceSettlement>>(emptyList()) }
    var searchedLocations by remember { mutableStateOf<List<PlaceSettlement>>(emptyList()) }

    val selectedMediaUris = remember { mutableStateListOf<Uri>() }
    var isVideoMedia by remember { mutableStateOf(false) }

    // Active bottom sheet panel ("media", "bg", "lang", "topics", "loc", or null)
    var activeDockPanel by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var publishJob by remember { mutableStateOf<Job?>(null) }
    var createdMoment by remember { mutableStateOf<MomentDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var topicLimitWarning by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            publishJob?.cancel()
            AppLoadingManager.hide()
        }
    }

    val userCountryCode = remember(selfUserId) {
        prefs.getString("profile_countryCode_${selfUserId}", null)
            ?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().country.lowercase().ifBlank { "in" }
    }
    val userCountryName = remember(selfUserId, userCountryCode) {
        prefs.getString("profile_country_${selfUserId}", null)
            ?.trim()?.takeIf { it.isNotBlank() }
            ?: Locale("", userCountryCode.uppercase()).getDisplayCountry(Locale.ENGLISH)
    }

    LaunchedEffect(activeDockPanel) {
        if (activeDockPanel == "loc" && suggestedLocations.isEmpty()) {
            val popular = PlaceDbService.fetchPopularCities(userCountryCode, userCountryName)
            suggestedLocations = popular.ifEmpty { PlaceDbService.searchCitiesOnline("") }
        }
    }

    LaunchedEffect(activeDockPanel, locationInputSearch) {
        if (activeDockPanel != "loc") return@LaunchedEffect
        val q = locationInputSearch.trim()
        if (q.isBlank()) {
            searchedLocations = emptyList()
            isSearchingLocation = false
            return@LaunchedEffect
        }
        isSearchingLocation = true
        delay(280L)
        searchedLocations = PlaceDbService.searchCitiesOnline(query = q)
        isSearchingLocation = false
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            if (!LocationDetector.isGpsEnabled(context)) {
                Toast.makeText(context, "Location services (GPS) disabled on device", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                isDetectingLocation = true
                val detected = LocationDetector.detectCurrentLocation(context)
                isDetectingLocation = false
                if (detected != null && detected.city.isNotBlank()) {
                    val locLabel = detected.city.take(limits.location)
                    selectedLocation = locLabel
                    locationInputSearch = locLabel
                    Toast.makeText(context, "Detected: ${detected.city}, ${detected.country}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not detect location. Search your city below.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerPostLocationDetection() {
        if (!LocationDetector.hasLocationPermission(context)) {
            showLocationConsentDialog = true
        } else {
            if (!LocationDetector.isGpsEnabled(context)) {
                Toast.makeText(context, "Location services (GPS) disabled on device", Toast.LENGTH_SHORT).show()
                return
            }
            coroutineScope.launch {
                isDetectingLocation = true
                val detected = LocationDetector.detectCurrentLocation(context)
                isDetectingLocation = false
                if (detected != null && detected.city.isNotBlank()) {
                    val locLabel = detected.city.take(limits.location)
                    selectedLocation = locLabel
                    locationInputSearch = locLabel
                    Toast.makeText(context, "Detected: ${detected.city}, ${detected.country}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not detect location. Search your city below.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun handleAddCustomTopic() {
        val raw = customTopicInput.trim().removePrefix("#").trim()
        if (raw.isBlank()) return

        val slug = raw
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}\\s_]"), "")
            .trim()
            .replace(Regex("[\\s_]+"), "_")
            .take(32)
        if (slug.isBlank()) {
            topicLimitWarning = "Enter letters or numbers for a topic"
            return
        }

        val allAvailable = optionsTopics + customTopics
        val existing = allAvailable.firstOrNull {
            it.slug.equals(slug, ignoreCase = true) || it.label.equals(raw, ignoreCase = true)
        }
        val targetSlug = existing?.slug ?: run {
            val created = MomentTopicDto(slug = slug, label = raw.take(32))
            customTopics.add(created)
            created.slug
        }

        if (selectedTopicSlugs.contains(targetSlug)) {
            topicLimitWarning = null
        } else if (selectedTopicSlugs.size < limits.topics) {
            selectedTopicSlugs.add(targetSlug)
            topicLimitWarning = null
        } else {
            topicLimitWarning = "Choose up to ${limits.topics} topics"
        }
        customTopicInput = ""
    }

    val multiplePhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedMediaUris.clear()
            selectedMediaUris.addAll(uris.take(limits.media.coerceAtLeast(1)))
            isVideoMedia = false
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedMediaUris.clear()
            selectedMediaUris.add(uri)
            isVideoMedia = true
        }
    }

    val availableLangs = remember {
        val priority = listOf(
            "Hindi", "English", "Korean", "Chinese", "Japanese", "Malay",
            "Spanish", "French", "Arabic", "German", "Portuguese", "Indonesian"
        )
        val allNames = LanguageData.allLanguages.map { it.name }
        (priority + allNames).distinct()
    }
    val filteredLangs = remember(languageSearchQuery, availableLangs) {
        val q = languageSearchQuery.trim()
        if (q.isBlank()) availableLangs.take(18)
        else availableLangs.filter { it.contains(q, ignoreCase = true) }.take(30)
    }

    val hasMedia = selectedMediaUris.isNotEmpty()
    val canPost = titleText.trim().isNotEmpty() || bodyText.trim().isNotEmpty() || hasMedia

    fun handlePublish() {
        if (!canPost || isUploading) return
        val normalizedLanguage = MomentsRepository.stripLanguageLevel(selectedLanguage)
        if (normalizedLanguage.isBlank()) {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Choose the post language")
            }
            return
        }
        isUploading = true
        val job = coroutineScope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            try {
                val presetKey = bgPresets.getOrElse(selectedBgIndex) { bgPresets.first() }.key
                val result = MomentsRepository.createMoment(
                    context = context,
                    title = titleText.takeIf { it.isNotBlank() },
                    content = bodyText.takeIf { it.isNotBlank() },
                    language = normalizedLanguage,
                    translation = if (showTranslationLine) translationText.takeIf { it.isNotBlank() } else null,
                    bgKey = if (hasMedia) null else presetKey,
                    mediaUris = selectedMediaUris.toList(),
                    topics = selectedTopicSlugs.toList(),
                    location = selectedLocation.takeIf { it.isNotBlank() },
                    fixRequested = askCorrection,
                    onUploadProgress = { index, total, pct, isVideo ->
                        if (isVideo && pct != null) {
                            AppLoadingManager.updateProgress(
                                percent = pct,
                                message = "Uploading video... $pct%"
                            )
                        } else if (total > 1) {
                            AppLoadingManager.updateProgress(
                                percent = null,
                                message = "Uploading photo $index/$total..."
                            )
                        } else if (total == 1) {
                            AppLoadingManager.updateProgress(
                                percent = null,
                                message = "Uploading photo..."
                            )
                        }
                    }
                )
                result.onSuccess { dto ->
                    createdMoment = dto
                }.onFailure { err ->
                    // TODO remove detail before release
                    val msg = err.message ?: "Upload failed (${err.javaClass.simpleName}: unknown error)"
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = msg,
                        duration = SnackbarDuration.Long
                    )
                }
            } catch (_: CancellationException) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Upload cancelled")
            } catch (t: Throwable) {
                // TODO remove detail before release
                val msg = "Upload failed (${t.javaClass.simpleName}: ${t.message ?: "unknown error"})"
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Long
                )
            } finally {
                AppLoadingManager.hide()
                isUploading = false
                publishJob = null
            }
        }
        publishJob = job
        AppLoadingManager.show(
            message = if (hasMedia) {
                if (isVideoMedia) "Preparing video..." else "Uploading media..."
            } else "Publishing your moment...",
            progressPercent = if (hasMedia && isVideoMedia) 0 else null,
            onCancel = {
                publishJob?.cancel()
                AppLoadingManager.hide()
            }
        )
        job.start()
    }

    BackHandler {
        if (activeDockPanel != null) {
            activeDockPanel = null
        } else if (!isUploading) {
            onDismiss()
        }
    }

    if (createdMoment != null) {
        val posted = createdMoment!!
        SuccessAnimationScreen(onComplete = {
            createdMoment = null
            onPostCreated(posted)
        })
    } else {
            val currentPreset = bgPresets.getOrElse(selectedBgIndex) { bgPresets.first() }
            val isDarkBg = !currentPreset.isDarkText || hasMedia
            val canvasTextColor = if (isDarkBg) Color.White else Color(0xFF1B1608)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (hasMedia) Brush.linearGradient(listOf(Color(0xFF0F172A), Color.Black))
                        else Brush.linearGradient(currentPreset.colors)
                    )
                    .testTag("create_post_screen")
            ) {
            // Background Media Preview if photo(s) or video selected
            if (hasMedia) {
                if (!isVideoMedia && selectedMediaUris.size > 1) {
                    val previewPagerState = rememberPagerState(pageCount = { selectedMediaUris.size })
                    HorizontalPager(
                        state = previewPagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { idx ->
                        AsyncImage(
                            model = selectedMediaUris[idx],
                            contentDescription = "Selected media ${idx + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    AsyncImage(
                        model = selectedMediaUris.first(),
                        contentDescription = "Selected media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
                    .imePadding()
            ) {
                // 1. TOP BAR: Close (X) glass button & Post CTA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                // 2. CANVAS SECTION: Title & Body text inputs + optional translation + character counter + native fix toggle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 26.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    TextField(
                        value = titleText,
                        onValueChange = { if (it.length <= limits.title) titleText = it },
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

                    if (titleText.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${titleText.length}/${limits.title}",
                                fontSize = 11.sp,
                                color = canvasTextColor.copy(alpha = 0.65f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = bodyText,
                        onValueChange = { if (it.length <= limits.content) bodyText = it },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${bodyText.length}/${limits.content}",
                            fontSize = 13.sp,
                            color = canvasTextColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (showTranslationLine) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkBg) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                TextField(
                                    value = translationText,
                                    onValueChange = { if (it.length <= limits.translation) translationText = it },
                                    placeholder = {
                                        Text(
                                            "Add a translation (optional)...",
                                            fontSize = 14.sp,
                                            color = canvasTextColor.copy(alpha = 0.6f)
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        color = canvasTextColor,
                                        lineHeight = 20.sp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("post_translation_input"),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = canvasTextColor
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "${translationText.length}/${limits.translation}",
                                        fontSize = 11.sp,
                                        color = canvasTextColor.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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

                // Snackbar banner anchored above Meta Tags & Bottom Dock Toolbar
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) { data ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1B1608).copy(alpha = 0.94f),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = data.visuals.message,
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
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
                    // Language tag with CircleFlag
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFFBF0).copy(alpha = 0.9f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            val displayLanguageChip = MomentsRepository.stripLanguageLevel(selectedLanguage)
                            CircleFlag(
                                countryCode = LanguageData.getFlagCodeForLanguage(displayLanguageChip),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = displayLanguageChip,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1B1608)
                            )
                        }
                    }

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

                    selectedTopicSlugs.forEach { slug ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFFFBF0).copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = MomentsRepository.resolveTopicChipText(slug),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1B1608),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }

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

                    if (hasMedia) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFFFBF0).copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = if (isVideoMedia) "🎬 Video added" else "📷 ${selectedMediaUris.size} photo${if (selectedMediaUris.size > 1) "s" else ""}",
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
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
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

                        when (activeDockPanel) {
                            "media" -> {
                                Text(
                                    text = "Add up to ${limits.media} photos, or 1 video. Your text sits on top of it.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF7A7260),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            multiplePhotosLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        color = Color(0xFFF6EBCB),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            text = "📷 Choose photos (max ${limits.media})",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                                        )
                                    }

                                    Surface(
                                        onClick = {
                                            videoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        },
                                        color = Color(0xFFF6EBCB),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            text = "🎬 Choose video (1)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                                        )
                                    }

                                    if (hasMedia) {
                                        Surface(
                                            onClick = {
                                                selectedMediaUris.clear()
                                                isVideoMedia = false
                                            },
                                            color = Color(0xFFF6EBCB),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text(
                                                text = "Remove media",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFEF4444),
                                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }

                                if (selectedMediaUris.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        itemsIndexed(selectedMediaUris) { idx, uri ->
                                            Box(
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            ) {
                                                AsyncImage(
                                                    model = uri,
                                                    contentDescription = "Selected media item",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp)
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.65f))
                                                        .clickable {
                                                            selectedMediaUris.removeAt(idx)
                                                            if (selectedMediaUris.isEmpty()) isVideoMedia = false
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CustomCloseIcon(iconSize = 9.dp, color = Color.White)
                                                }
                                            }
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
                                    bgPresets.forEachIndexed { index, preset ->
                                        val isSelected = selectedBgIndex == index && !hasMedia
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(Brush.linearGradient(preset.colors))
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF1B1608) else Color.Black.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                                .clickable {
                                                    selectedBgIndex = index
                                                    selectedMediaUris.clear()
                                                    isVideoMedia = false
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
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                                OutlinedTextField(
                                    value = languageSearchQuery,
                                    onValueChange = { if (it.length <= limits.language) languageSearchQuery = it },
                                    placeholder = { Text("Search language...", fontSize = 15.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFFC21A),
                                        unfocusedBorderColor = Color(0xFFE5DFC9),
                                        focusedContainerColor = Color(0xFFFFFBF0),
                                        unfocusedContainerColor = Color(0xFFFFFBF0)
                                    )
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    filteredLangs.forEach { lang ->
                                        val isSelected = selectedLanguage.equals(lang, ignoreCase = true)
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
                                            ) {
                                                CircleFlag(
                                                    countryCode = LanguageData.getFlagCodeForLanguage(lang),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = lang,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    color = Color(0xFFE5DFC9),
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
                                    text = topicLimitWarning
                                        ?: "Pick up to ${limits.topics} topics or add your own custom #tag.",
                                    fontSize = 14.sp,
                                    color = if (topicLimitWarning != null) Color(0xFFDC2626) else Color(0xFF7A7260),
                                    fontWeight = if (topicLimitWarning != null) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customTopicInput,
                                        onValueChange = { if (it.length <= 28) customTopicInput = it },
                                        placeholder = { Text("Add custom #topic...", fontSize = 14.sp) },
                                        prefix = {
                                            Text(
                                                text = "#",
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF7A7260)
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("custom_topic_input"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFFFC21A),
                                            unfocusedBorderColor = Color(0xFFE5DFC9),
                                            focusedContainerColor = Color.White.copy(alpha = 0.75f),
                                            unfocusedContainerColor = Color.White.copy(alpha = 0.75f)
                                        )
                                    )

                                    Button(
                                        onClick = { handleAddCustomTopic() },
                                        enabled = customTopicInput.trim().removePrefix("#").isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFFC21A),
                                            contentColor = Color(0xFF2A1E00)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        modifier = Modifier
                                            .height(52.dp)
                                            .testTag("add_custom_topic_button")
                                    ) {
                                        Text(
                                            text = "+ Add",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                val allTopics = remember(optionsTopics, customTopics.toList()) {
                                    (optionsTopics + customTopics).distinctBy { it.slug.lowercase() }
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    allTopics.forEach { topic: MomentTopicDto ->
                                        val isSelected = selectedTopicSlugs.contains(topic.slug)
                                        Surface(
                                            onClick = {
                                                if (isSelected) {
                                                    selectedTopicSlugs.remove(topic.slug)
                                                    topicLimitWarning = null
                                                } else if (selectedTopicSlugs.size < limits.topics) {
                                                    selectedTopicSlugs.add(topic.slug)
                                                    topicLimitWarning = null
                                                } else {
                                                    topicLimitWarning = "Choose up to ${limits.topics} topics"
                                                }
                                            },
                                            color = if (isSelected) Color(0xFFFFC21A).copy(alpha = 0.35f) else Color(0xFFF6EBCB),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 44.dp)
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.dp,
                                                    color = if (isSelected) Color(0xFFFFC21A) else Color.Transparent,
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                                            ) {
                                                Text(
                                                    text = MomentsRepository.formatTopicLabelToHashtag(topic.label),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF1B1608)
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_check),
                                                        contentDescription = "Selected topic",
                                                        tint = Color(0xFF2A1E00),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "loc" -> {
                                // Search field backed by free Open-Meteo + CountriesNow city APIs
                                OutlinedTextField(
                                    value = locationInputSearch,
                                    onValueChange = {
                                        if (it.length <= limits.location) {
                                            locationInputSearch = it
                                        }
                                    },
                                    placeholder = { Text("Search any city worldwide...", fontSize = 15.sp) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_search),
                                            contentDescription = "Search city",
                                            tint = Color(0xFF7A7260),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isSearchingLocation) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier
                                                        .padding(end = 6.dp)
                                                        .size(16.dp),
                                                    color = Color(0xFFFFC21A),
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                            if (locationInputSearch.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { locationInputSearch = "" },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_close),
                                                        contentDescription = "Clear city search",
                                                        tint = Color(0xFF7A7260),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .testTag("post_location_search_input"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFFC21A),
                                        unfocusedBorderColor = Color(0xFFE5DFC9),
                                        focusedContainerColor = Color.White.copy(alpha = 0.75f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.75f)
                                    )
                                )

                                // GPS Auto-detect card
                                Surface(
                                    onClick = { triggerPostLocationDetection() },
                                    color = Color(0xFFFFC21A).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .border(1.dp, Color(0xFFFFC21A), RoundedCornerShape(16.dp))
                                        .testTag("post_location_gps_card")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(Color(0xFFFFC21A).copy(alpha = 0.35f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isDetectingLocation) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        color = Color(0xFF2A1E00),
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_target),
                                                        contentDescription = "Detect GPS",
                                                        tint = Color(0xFF2A1E00),
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = if (isDetectingLocation) "Detecting your city..." else "Use current location",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1B1608)
                                                )
                                                Text(
                                                    text = "Auto-detect via device GPS",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF7A7260)
                                                )
                                            }
                                        }
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_chevron_right),
                                            contentDescription = null,
                                            tint = Color(0xFF7A7260),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Hide location / Selected chip quick actions
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    val isHidden = selectedLocation.isBlank()
                                    Surface(
                                        onClick = {
                                            selectedLocation = ""
                                            locationInputSearch = ""
                                        },
                                        color = if (isHidden) Color(0xFFFFC21A).copy(alpha = 0.35f) else Color(0xFFF6EBCB),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.border(
                                            width = if (isHidden) 2.dp else 0.dp,
                                            color = if (isHidden) Color(0xFFFFC21A) else Color.Transparent,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                    ) {
                                        Text(
                                            text = "Hide location",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1B1608),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                        )
                                    }

                                    val typedCustom = locationInputSearch.trim()
                                    if (typedCustom.isNotBlank() &&
                                        searchedLocations.none { it.name.equals(typedCustom, ignoreCase = true) }
                                    ) {
                                        val isCustomSelected = selectedLocation.equals(typedCustom, ignoreCase = true)
                                        Surface(
                                            onClick = {
                                                selectedLocation = typedCustom.take(limits.location)
                                            },
                                            color = if (isCustomSelected) Color(0xFFFFC21A).copy(alpha = 0.35f) else Color(0xFFF6EBCB),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.border(
                                                width = if (isCustomSelected) 2.dp else 1.dp,
                                                color = if (isCustomSelected) Color(0xFFFFC21A) else Color(0xFFE5DFC9),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                        ) {
                                            Text(
                                                text = "Use \"$typedCustom\"",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1B1608),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                            )
                                        }
                                    }
                                }

                                val cityItems = if (locationInputSearch.isNotBlank()) {
                                    searchedLocations
                                } else {
                                    suggestedLocations
                                }

                                Text(
                                    text = if (locationInputSearch.isNotBlank()) "MATCHING CITIES" else "SUGGESTED CITIES",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7A7260),
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    cityItems.take(15).forEach { city ->
                                        val isSelected = selectedLocation.equals(city.name, ignoreCase = true)
                                        Surface(
                                            onClick = {
                                                selectedLocation = city.name.take(limits.location)
                                            },
                                            color = if (isSelected) Color(0xFFFFC21A).copy(alpha = 0.25f) else Color(0xFFF6EBCB).copy(alpha = 0.55f),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.dp,
                                                    color = if (isSelected) Color(0xFFFFC21A) else Color.Transparent,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_pin),
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color(0xFF2A1E00) else Color(0xFF7A7260),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = city.name,
                                                            fontSize = 14.5.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                            color = Color(0xFF1B1608),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        val subtitle = listOf(city.adminRegion, city.countryName)
                                                            .filter { it.isNotBlank() }
                                                            .distinct()
                                                            .joinToString(", ")
                                                        if (subtitle.isNotBlank()) {
                                                            Text(
                                                                text = subtitle,
                                                                fontSize = 12.sp,
                                                                color = Color(0xFF7A7260),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_check),
                                                        contentDescription = "Selected",
                                                        tint = Color(0xFF2A1E00),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            if (showLocationConsentDialog) {
                LocationConsentDialog(
                    onDismiss = { showLocationConsentDialog = false },
                    onConfirmConsent = {
                        showLocationConsentDialog = false
                        locationPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
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
