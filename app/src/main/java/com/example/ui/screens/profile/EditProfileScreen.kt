package com.example.ui.screens.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.AuthViewModel
import com.example.UserProfile
import com.example.ui.components.CircleFlag
import com.example.ui.components.LocationConsentDialog
import com.example.ui.screens.CountryData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    currentUserProfile: UserProfile,
    viewModel: AuthViewModel?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val prefs = remember { context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE) }
    val savedBio = remember { prefs.getString("profile_bio_${currentUserProfile.uid}", null) }
    val savedCity = remember { prefs.getString("profile_city_${currentUserProfile.uid}", null) }
    val savedAge = remember { prefs.getInt("profile_age_${currentUserProfile.uid}", 0).takeIf { it > 0 } }

    // Screen States mapping exactly to UserProfile model
    var editUsername by remember { mutableStateOf(currentUserProfile.username ?: "") }
    var editDisplayName by remember { mutableStateOf(currentUserProfile.displayName ?: "") }
    var editAvatar by remember { mutableStateOf(currentUserProfile.avatar ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300") }
    var editCover by remember { mutableStateOf(currentUserProfile.coverUrl ?: "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800") }

    var editBio by remember { mutableStateOf(currentUserProfile.bio ?: savedBio ?: currentUserProfile.dob ?: "Excited to practice languages and make new friends!") }
    var editAge by remember { mutableStateOf((currentUserProfile.age ?: savedAge ?: 28).toString()) }
    var editGender by remember { mutableStateOf(currentUserProfile.gender ?: "Male") }
    var editCountry by remember { mutableStateOf(currentUserProfile.country ?: "Malaysia") }
    var editCountryCode by remember { mutableStateOf(currentUserProfile.countryCode ?: "my") }
    var editCity by remember { mutableStateOf(currentUserProfile.city ?: savedCity ?: "Kuala Lumpur") }

    // Lists of Native & Learning languages
    val nativeLanguages = remember { mutableStateListOf<String>() }
    val learningLanguages = remember { mutableStateListOf<String>() }

    // Intersecting topics lists (interests)
    val selectedInterests = remember { mutableStateListOf<String>() }

    // Fetch live Supabase Profile on initial load
    LaunchedEffect(Unit) {
        try {
            val supaUid = com.example.SupabaseSessionManager.getUserId(context) ?: currentUserProfile.uid
            val bearerAuth = com.example.SupabaseSessionManager.getBearerToken(context)
            val res = com.example.SupabaseClient.api.getProfile(auth = bearerAuth, authUserIdEq = "eq.$supaUid")
            if (res.isSuccessful) {
                val p = res.body()?.firstOrNull()
                if (p != null) {
                    if (!p.profile_name.isNullOrBlank()) editDisplayName = p.profile_name
                    if (!p.username.isNullOrBlank()) editUsername = p.username
                    if (!p.avatar_url.isNullOrBlank()) editAvatar = p.avatar_url
                    if (!p.cover_url.isNullOrBlank()) editCover = p.cover_url
                    if (!p.bio.isNullOrBlank()) editBio = p.bio
                    if (p.age != null && p.age > 0) editAge = p.age.toString()
                    if (!p.gender.isNullOrBlank()) editGender = p.gender
                    if (!p.country.isNullOrBlank()) editCountry = p.country
                    if (!p.country_code.isNullOrBlank()) editCountryCode = p.country_code
                    if (!p.city.isNullOrBlank()) editCity = p.city

                    if (!p.native_languages.isNullOrEmpty()) {
                        nativeLanguages.clear()
                        nativeLanguages.addAll(p.native_languages)
                    } else if (currentUserProfile.nativeLanguage != null) {
                        nativeLanguages.clear()
                        nativeLanguages.addAll(currentUserProfile.nativeLanguage.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                    } else if (nativeLanguages.isEmpty()) {
                        nativeLanguages.addAll(listOf("English", "Spanish"))
                    }

                    if (!p.learning_languages.isNullOrEmpty()) {
                        learningLanguages.clear()
                        learningLanguages.addAll(p.learning_languages)
                    } else if (currentUserProfile.learningLanguage != null) {
                        learningLanguages.clear()
                        learningLanguages.addAll(currentUserProfile.learningLanguage.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                    } else if (learningLanguages.isEmpty()) {
                        learningLanguages.addAll(listOf("Arabic", "Korean"))
                    }

                    if (!p.hobbies.isNullOrEmpty()) {
                        selectedInterests.clear()
                        selectedInterests.addAll(p.hobbies)
                    } else if (currentUserProfile.hobbies.isNotEmpty()) {
                        selectedInterests.clear()
                        selectedInterests.addAll(currentUserProfile.hobbies)
                    } else if (selectedInterests.isEmpty()) {
                        selectedInterests.addAll(listOf("Music", "Movies", "Technology"))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EditProfileScreen", "Failed to load profile data from Supabase", e)
            if (nativeLanguages.isEmpty()) nativeLanguages.addAll(listOf("English", "Spanish"))
            if (learningLanguages.isEmpty()) learningLanguages.addAll(listOf("Arabic", "Korean"))
            if (selectedInterests.isEmpty()) selectedInterests.addAll(listOf("Music", "Movies", "Technology"))
        }
    }

    // Modal dialog pickers
    var showCountryPicker by remember { mutableStateOf(false) }
    var showAgePicker by remember { mutableStateOf(false) }
    var showGenderPicker by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }
    var showAddLanguagePanel by remember { mutableStateOf(false) }
    var showLocationConsentDialog by remember { mutableStateOf(false) }
    var showAddInterestDialog by remember { mutableStateOf(false) }
    var newInterestInput by remember { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        coroutineScope.launch {
            editCity = "Detecting..."
            val loc = com.example.util.LocationDetector.detectCurrentLocation(context)
            editCity = loc.city
            editCountry = loc.country
            editCountryCode = loc.countryCode
            Toast.makeText(context, "Location detected: ${loc.fullString}", Toast.LENGTH_SHORT).show()
        }
    }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Custom Device Cover & Avatar Launchers
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            editCover = uri.toString()
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            editAvatar = uri.toString()
        }
    }

    // Action to commit saved changes to Supabase & SharedPrefs
    fun saveProfileChanges() {
        errorMessage = null
        val cleanUsername = editUsername.trim().lowercase()
        if (cleanUsername.isBlank()) {
            errorMessage = "Username cannot be blank."
            return
        }
        if (cleanUsername.length < 3) {
            errorMessage = "Username must be at least 3 characters."
            return
        }
        if (editDisplayName.trim().isBlank()) {
            errorMessage = "Display Name cannot be blank."
            return
        }

        val ageInt = editAge.trim().toIntOrNull()
        if (ageInt == null || ageInt < 13 || ageInt > 120) {
            errorMessage = "Please enter a valid adult age (18-120)."
            return
        }

        isSaving = true
        coroutineScope.launch {
            try {
                if (viewModel != null) {
                    viewModel.updateProfile(
                        context = context,
                        username = cleanUsername,
                        displayName = editDisplayName.trim(),
                        avatar = editAvatar,
                        coverUrl = editCover,
                        dob = editBio,
                        gender = editGender,
                        nativeLanguage = nativeLanguages.joinToString(","),
                        learningLanguage = learningLanguages.joinToString(","),
                        country = editCountry,
                        countryCode = editCountryCode,
                        hobbies = selectedInterests.toList(),
                        age = ageInt,
                        bio = editBio.trim(),
                        city = editCity.trim(),
                        onError = { err ->
                            errorMessage = err
                            isSaving = false
                        },
                        onSuccess = {
                            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            isSaving = false
                            onDismiss()
                        }
                    )
                } else {
                    onDismiss()
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to save changes"
                Log.e("EditProfileScreen", "Error saving profile", e)
                isSaving = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF8F5)) // exact same warm cream background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. TOP HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Arrow Button (subtle circular card)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFECEBE6), CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    CustomBackArrowIcon(iconSize = 18.dp, color = Color(0xFF0F172A))
                }

                // Centered bold title
                Text(
                    text = "Edit Profile",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                // Yellow circular Save/Done Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD43B))
                        .clickable { saveProfileChanges() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF0F172A), strokeWidth = 2.dp)
                    } else {
                        CustomCheckOutlineIcon(iconSize = 16.dp, color = Color(0xFF0F172A))
                    }
                }
            }

            // Scrollable Profile Editing Fields Layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // 2. HERO COVER & OVERLAPPING AVATAR PROFILE PICTURE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    // Cover photo card background
                    AsyncImage(
                        model = editCover,
                        contentDescription = "Cover Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Edit Cover pill button in top right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { coverPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CustomCameraIcon(iconSize = 12.dp, color = Color.White)
                            Text("Edit Cover", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Overlapping Avatar Image on Bottom Left
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp)
                            .size(100.dp)
                    ) {
                        // Large Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(3.dp, Color.White, CircleShape)
                        ) {
                            AsyncImage(
                                model = editAvatar,
                                contentDescription = "Profile Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Overlapping Floating Camera Icon overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFECEBE6), CircleShape)
                                .clickable { avatarPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            CustomCameraIcon(iconSize = 12.dp, color = Color(0xFF0F172A))
                        }

                        // Floating country Flag badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 10.dp, y = (-2).dp)
                                .size(24.dp)
                                .clip(CircleShape)
                        ) {
                            CircleFlag(countryCode = editCountryCode, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error message banner
                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // 3. BASIC INFORMATION SECTION
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeaderWithAccentBar(title = "Basic Information")
                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFECEBE6))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Display Name Input Card Block
                            EditableFieldBlock(
                                labelText = "Display Name",
                                textValue = editDisplayName,
                                onValueChange = { editDisplayName = it },
                                charCountText = "${editDisplayName.length}/20",
                                maxLen = 20
                            )

                            // Username Input Card Block
                            EditableFieldBlock(
                                labelText = "Username",
                                textValue = editUsername,
                                onValueChange = { editUsername = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase() },
                                charCountText = "${editUsername.length}/30",
                                maxLen = 30
                            )

                            // Bio Input Card Block
                            EditableFieldBlock(
                                labelText = "Bio",
                                textValue = editBio,
                                onValueChange = { editBio = it },
                                charCountText = "${editBio.length}/150",
                                maxLen = 150
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. PERSONAL DETAILS SECTION
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeaderWithAccentBar(title = "Personal Details")
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Age drop selector card
                        DetailsSelectPillCard(
                            modifier = Modifier.weight(1f),
                            icon = { CustomCalendarLineIcon(iconSize = 16.dp, color = Color(0xFF64748B)) },
                            titleLabel = "Age",
                            selectedValue = editAge,
                            onClick = { showAgePicker = true }
                        )

                        // Gender drop selector card
                        DetailsSelectPillCard(
                            modifier = Modifier.weight(1f),
                            icon = { CustomGenderSymbolIcon(iconSize = 16.dp, color = Color(0xFF64748B)) },
                            titleLabel = "Gender",
                            selectedValue = editGender,
                            onClick = { showGenderPicker = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Country Drop selector card
                    DetailsSelectPillCard(
                        modifier = Modifier.fillMaxWidth(),
                        icon = { CustomGlobeGridIcon(iconSize = 16.dp, color = Color(0xFF64748B)) },
                        titleLabel = "Country",
                        selectedValue = editCountry,
                        onClick = { showCountryPicker = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row with City selector and GPS auto-detect side-by-side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // City card
                        DetailsSelectPillCard(
                            modifier = Modifier.weight(1.8f),
                            icon = { CustomLocationMarkerIcon(iconSize = 16.dp, color = Color(0xFF64748B)) },
                            titleLabel = "City (Auto-detected)",
                            selectedValue = editCity,
                            onClick = { showCityPicker = true }
                        )

                        // Yellow "Use Current" square GPS trigger button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(16.dp))
                                .clickable {
                                    if (com.example.util.LocationDetector.hasLocationPermission(context)) {
                                        coroutineScope.launch {
                                            editCity = "Detecting..."
                                            val loc = com.example.util.LocationDetector.detectCurrentLocation(context)
                                            editCity = loc.city
                                            editCountry = loc.country
                                            editCountryCode = loc.countryCode
                                            Toast.makeText(context, "Location auto-detected: ${loc.fullString}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        showLocationConsentDialog = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CustomCrosshairGpsIcon(iconSize = 16.dp, color = Color(0xFFB45309))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Use Current", color = Color(0xFFB45309), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. LANGUAGES CHIPS BOX SECTION
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeaderWithAccentBar(title = "Languages")
                        Text(
                            text = "Edit",
                            color = Color(0xFFFFB300),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showAddLanguagePanel = !showAddLanguagePanel }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFECEBE6))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                            // Green card: Native Languages
                            LanguageSubGroupContainer(
                                title = "Native Languages (2)",
                                containerColor = Color(0xFFECFDF5),
                                titleColor = Color(0xFF059669),
                                languages = nativeLanguages,
                                flags = mapOf("English" to "us", "Spanish" to "es"),
                                onAddClick = { showAddLanguagePanel = true }
                            )

                            // Blue card: Learning Languages
                            LanguageSubGroupContainer(
                                title = "Learning Languages (2)",
                                containerColor = Color(0xFFEFF6FF),
                                titleColor = Color(0xFF2563EB),
                                languages = learningLanguages,
                                flags = mapOf("Arabic" to "sa", "Korean" to "kr"),
                                onAddClick = { showAddLanguagePanel = true }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 7. INTERESTS SECTION
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeaderWithAccentBar(title = "Interests")
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Select topics you're interested in",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Wrapping flex chips for Interests
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val allInterestChips = listOf(
                            InterestChipItem("Gaming", Color(0xFF3B82F6), { CustomControllerLineIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Music", Color(0xFFFFD43B), { CustomMusicNoteLineIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Photography", Color(0xFF8B5CF6), { CustomCameraIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Travel", Color(0xFF0EA5E9), { CustomAirplaneLineIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Food", Color(0xFFEC4899), { CustomCrossedForkSpoonIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Movies", Color(0xFFF97316), { CustomClapperboardIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Sports", Color(0xFF10B981), { CustomTrophyLineIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Technology", Color(0xFFFFD43B), { CustomCodeTagsIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Writing", Color(0xFF8B5CF6), { CustomPenLineIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Cooking", Color(0xFFF97316), { CustomCookingHatIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("K-Drama", Color(0xFFEC4899), { CustomTvAntennaIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Fitness", Color(0xFF3B82F6), { CustomDumbbellLineIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Reading", Color(0xFF3B82F6), { CustomOpenBookLineIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Art", Color(0xFFEC4899), { CustomPaletteArtIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Nature", Color(0xFF10B981), { CustomLeafLineIcon(iconSize = 14.dp, color = it) }),
                            InterestChipItem("Pets", Color(0xFFEC4899), { CustomPawLineIcon(iconSize = 14.dp, color = it) })
                        )

                        allInterestChips.forEach { chipItem ->
                            val isSelected = selectedInterests.contains(chipItem.name)
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        if (isSelected) selectedInterests.remove(chipItem.name) else selectedInterests.add(chipItem.name)
                                    },
                                color = if (isSelected) chipItem.accentColor.copy(alpha = 0.1f) else Color.White,
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) chipItem.accentColor else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    chipItem.iconProvider(chipItem.accentColor)
                                    Text(
                                        text = chipItem.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) chipItem.accentColor else Color(0xFF0F172A)
                                    )

                                    if (isSelected) {
                                        // small checked circle badge
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(chipItem.accentColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CustomCheckOutlineIcon(iconSize = 6.dp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // Custom interests added by user
                        val defaultNames = setOf("Gaming", "Music", "Photography", "Travel", "Food", "Movies", "Sports", "Technology", "Writing", "Cooking", "K-Drama", "Fitness", "Reading", "Art", "Nature", "Pets")
                        val customInterests = selectedInterests.filter { !defaultNames.contains(it) }
                        customInterests.forEach { customName ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { selectedInterests.remove(customName) },
                                color = Color(0xFFFFD43B).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.ui.components.SoftHeartIcon(size = 14.dp, color = Color(0xFFD97706))
                                    Text(
                                        text = customName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFD97706)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CustomCheckOutlineIcon(iconSize = 6.dp, color = Color.White)
                                    }
                                }
                            }
                        }

                        // Dashed "+ Add Interest" pill
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .drawBehind {
                                    val stroke = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                    )
                                    drawRoundRect(
                                        color = Color(0xFFCBD5E1),
                                        style = stroke,
                                        cornerRadius = CornerRadius(18.dp.toPx())
                                    )
                                }
                                .clickable {
                                    newInterestInput = ""
                                    showAddInterestDialog = true
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                CustomPlusIcon(iconSize = 10.dp, color = Color(0xFF64748B))
                                Text("Add Interest", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // --- CUSTOM ADD INTEREST DIALOG ---
        if (showAddInterestDialog) {
            AlertDialog(
                onDismissRequest = { showAddInterestDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "Add Custom Interest",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter any hobby, topic, or language learning goal:",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newInterestInput,
                            onValueChange = { newInterestInput = it },
                            placeholder = { Text("e.g. Astrophysics, Anime, Poetry...", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = newInterestInput.trim()
                            if (trimmed.isNotBlank() && !selectedInterests.contains(trimmed)) {
                                selectedInterests.add(trimmed)
                            }
                            showAddInterestDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD43B),
                            contentColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddInterestDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                }
            )
        }

        // --- POPUP DIALOG PICKERS ---
        if (showCountryPicker) {
            AlertDialog(
                onDismissRequest = { showCountryPicker = false },
                containerColor = Color.White,
                title = { Text("Select Country", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A)) },
                text = {
                    Column(modifier = Modifier.height(260.dp)) {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(CountryData.allCountries.size) { idx ->
                                val c = CountryData.allCountries[idx]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editCountry = c.name
                                            editCountryCode = c.flagCode
                                            showCountryPicker = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircleFlag(countryCode = c.flagCode, modifier = Modifier.size(20.dp).clip(CircleShape))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(c.name, fontSize = 14.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCountryPicker = false }) { Text("Close", color = Color.Gray) }
                }
            )
        }

        if (showAgePicker) {
            var tempAgeInput by remember { mutableStateOf(editAge) }
            AlertDialog(
                onDismissRequest = { showAgePicker = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = { Text("Select Your Age", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Quick +/- Stepper & Direct input
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                                    .clickable {
                                        val cur = tempAgeInput.toIntOrNull() ?: 28
                                        if (cur > 13) tempAgeInput = (cur - 1).toString()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }

                            OutlinedTextField(
                                value = tempAgeInput,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }.take(3)
                                    tempAgeInput = filtered
                                },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                                modifier = Modifier.width(90.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                                    .clickable {
                                        val cur = tempAgeInput.toIntOrNull() ?: 28
                                        if (cur < 100) tempAgeInput = (cur + 1).toString()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                        }

                        Text("Quick select common ages:", fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))

                        // Age chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("18", "21", "25", "28", "32").forEach { quickAge ->
                                val isSel = tempAgeInput == quickAge
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { tempAgeInput = quickAge },
                                    color = if (isSel) Color(0xFFFFD43B) else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFEAB308) else Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = quickAge,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val valid = tempAgeInput.toIntOrNull() ?: 28
                            editAge = valid.coerceIn(13, 100).toString()
                            showAgePicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD43B)),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("Save Age", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAgePicker = false }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }

        if (showGenderPicker) {
            AlertDialog(
                onDismissRequest = { showGenderPicker = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = { Text("Select Gender", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female", "Non-binary", "Prefer not to say").forEach { g ->
                            Text(
                                text = g,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0F172A),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editGender = g
                                        showGenderPicker = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGenderPicker = false }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }

        if (showCityPicker) {
            var customCityText by remember { mutableStateOf(editCity) }
            AlertDialog(
                onDismissRequest = { showCityPicker = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = { Text("Enter Your City", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = customCityText,
                            onValueChange = { customCityText = it },
                            label = { Text("City Name") },
                            placeholder = { Text("e.g. Mumbai, New Delhi, London") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text("Quick Suggestions:", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)

                        // Wrap of quick cities
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("New Delhi", "Mumbai", "Kuala Lumpur", "London", "New York", "Tokyo", "Dubai", "Singapore", "Paris", "Toronto").forEach { c ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { customCityText = c },
                                    color = if (customCityText.equals(c, ignoreCase = true)) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (customCityText.equals(c, ignoreCase = true)) Color(0xFFF59E0B) else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = c,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customCityText.isNotBlank()) {
                                editCity = customCityText.trim()
                            }
                            showCityPicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD43B)),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("Save City", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCityPicker = false }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }

        if (showAddLanguagePanel) {
            AlertDialog(
                onDismissRequest = { showAddLanguagePanel = false },
                containerColor = Color.White,
                title = { Text("Add Language", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Japanese", "French", "German", "Russian", "Hindi").forEach { l ->
                            Text(
                                text = l,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!nativeLanguages.contains(l) && !learningLanguages.contains(l)) {
                                            learningLanguages.add(l)
                                        }
                                        showAddLanguagePanel = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddLanguagePanel = false }) { Text("Close", color = Color.Gray) }
                }
            )
        }

        if (showLocationConsentDialog) {
            LocationConsentDialog(
                onDismiss = { showLocationConsentDialog = false },
                onConfirmConsent = {
                    showLocationConsentDialog = false
                    if (com.example.util.LocationDetector.hasLocationPermission(context)) {
                        coroutineScope.launch {
                            editCity = "Detecting..."
                            val loc = com.example.util.LocationDetector.detectCurrentLocation(context)
                            editCity = loc.city
                            editCountry = loc.country
                            editCountryCode = loc.countryCode
                            Toast.makeText(context, "Location auto-detected: ${loc.fullString}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// HELPER COMPOSABLE BLOCKS
// -----------------------------------------------------------------------------

@Composable
fun SectionHeaderWithAccentBar(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Vertical yellow accent line on left
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .background(Color(0xFFFFD43B), RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
    }
}

@Composable
fun EditableFieldBlock(
    labelText: String,
    textValue: String,
    onValueChange: (String) -> Unit,
    charCountText: String,
    maxLen: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(labelText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
            Text(charCountText, fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        
        val placeholderText = when (labelText) {
            "Display Name" -> "Enter your full display name..."
            "Username" -> "Enter your username..."
            "Bio" -> "Write a short bio about yourself..."
            else -> "Enter $labelText..."
        }

        OutlinedTextField(
            value = textValue,
            onValueChange = { text ->
                if (text.length <= maxLen) onValueChange(text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (labelText == "Bio") Modifier.heightIn(min = 105.dp) else Modifier),
            singleLine = labelText != "Bio",
            maxLines = if (labelText == "Bio") 4 else 1,
            placeholder = {
                Text(
                    text = placeholderText,
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Normal
                )
            },
            textStyle = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            ),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFFFFF),
                unfocusedContainerColor = Color(0xFFF8FAFC),
                focusedBorderColor = Color(0xFFFFD43B),
                unfocusedBorderColor = Color(0xFFCBD5E1),
                cursorColor = Color(0xFF0F172A)
            )
        )
    }
}

@Composable
fun DetailsSelectPillCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    titleLabel: String,
    selectedValue: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(58.dp)
            .border(1.dp, Color(0xFFECEBE6), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Circular icon holder
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFAF9F5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                Column {
                    Text(titleLabel, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(selectedValue, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            }

            CustomChevronDownIcon(iconSize = 12.dp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
fun LanguageSubGroupContainer(
    title: String,
    containerColor: Color,
    titleColor: Color,
    languages: MutableList<String>,
    flags: Map<String, String>,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = titleColor)

            // Chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                languages.forEach { l ->
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Circular flag
                            val flagCode = flags[l] ?: "us"
                            CircleFlag(countryCode = flagCode, modifier = Modifier.size(16.dp).clip(CircleShape))

                            Text(l, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                                    .clickable { languages.remove(l) },
                                contentAlignment = Alignment.Center
                            ) {
                                CustomCloseIcon(iconSize = 6.dp, color = Color(0xFF0F172A))
                            }
                        }
                    }
                }

                // Plus Add Button inside chips row
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    CustomPlusIcon(iconSize = 10.dp, color = Color(0xFF0F172A))
                }
            }
        }
    }
}

data class InterestChipItem(
    val name: String,
    val accentColor: Color,
    val iconProvider: @Composable (Color) -> Unit
)

// -----------------------------------------------------------------------------
// CURATED CANVAS VECTOR LINE-ART ICONS (2PX STROKE / NO-MATERIAL DESIGN)
// -----------------------------------------------------------------------------

@Composable
fun CustomBackArrowIcon(modifier: Modifier = Modifier, iconSize: Dp = 18.dp, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.6f, h * 0.25f)
            lineTo(w * 0.35f, h * 0.5f)
            lineTo(w * 0.6f, h * 0.75f)
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(w * 0.35f, h * 0.5f), Offset(w * 0.75f, h * 0.5f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomCheckOutlineIcon(modifier: Modifier = Modifier, iconSize: Dp = 16.dp, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.5f)
            lineTo(w * 0.44f, h * 0.68f)
            lineTo(w * 0.75f, h * 0.32f)
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CustomCameraIcon(modifier: Modifier = Modifier, iconSize: Dp = 12.dp, color: Color = Color.White) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.1f, h * 0.28f),
            size = Size(w * 0.8f, h * 0.55f),
            cornerRadius = CornerRadius(w * 0.1f),
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Lens circle
        drawCircle(color, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.56f), style = Stroke(strokeWidth))
        // Top camera notch
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.28f)
            lineTo(w * 0.4f, h * 0.16f)
            lineTo(w * 0.6f, h * 0.16f)
            lineTo(w * 0.65f, h * 0.28f)
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CustomChevronDownIcon(modifier: Modifier = Modifier, iconSize: Dp = 12.dp, color: Color = Color(0xFF94A3B8)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.75f, h * 0.35f)
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CustomCalendarLineIcon(modifier: Modifier = Modifier, iconSize: Dp = 16.dp, color: Color = Color(0xFF64748B)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        // outer card
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.15f, h * 0.22f),
            size = Size(w * 0.7f, h * 0.63f),
            cornerRadius = CornerRadius(w * 0.08f),
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Horizontal grid line
        drawLine(color, Offset(w * 0.15f, h * 0.42f), Offset(w * 0.85f, h * 0.42f), strokeWidth)
        // Two vertical pegs on top
        drawLine(color, Offset(w * 0.32f, h * 0.12f), Offset(w * 0.32f, h * 0.22f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.68f, h * 0.12f), Offset(w * 0.68f, h * 0.22f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomGenderSymbolIcon(modifier: Modifier = Modifier, iconSize: Dp = 16.dp, color: Color = Color(0xFF64748B)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        // male symbol
        drawCircle(color, radius = w * 0.22f, center = Offset(w * 0.4f, h * 0.6f), style = Stroke(strokeWidth))
        drawLine(color, Offset(w * 0.55f, h * 0.45f), Offset(w * 0.78f, h * 0.22f), strokeWidth, cap = StrokeCap.Round)
        // arrow ticks
        drawLine(color, Offset(w * 0.58f, h * 0.22f), Offset(w * 0.78f, h * 0.22f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.78f, h * 0.42f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomGlobeGridIcon(modifier: Modifier = Modifier, iconSize: Dp = 16.dp, color: Color = Color(0xFF64748B)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        drawCircle(color, radius = w * 0.38f, center = center, style = Stroke(strokeWidth))
        // Equator
        drawLine(color, Offset(w * 0.12f, h * 0.5f), Offset(w * 0.88f, h * 0.5f), strokeWidth)
        // Longitude ellipses
        drawOval(color, topLeft = Offset(w * 0.3f, h * 0.12f), size = Size(w * 0.4f, h * 0.76f), style = Stroke(strokeWidth))
    }
}

@Composable
fun CustomLocationMarkerIcon(modifier: Modifier = Modifier, iconSize: Dp = 16.dp, color: Color = Color(0xFF64748B)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            cubicTo(w * 0.24f, h * 0.12f, w * 0.24f, h * 0.5f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.76f, h * 0.5f, w * 0.76f, h * 0.12f, w * 0.5f, h * 0.12f)
            close()
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = w * 0.1f, center = Offset(w * 0.5f, h * 0.38f), style = Stroke(strokeWidth))
    }
}

@Composable
fun CustomCrosshairGpsIcon(modifier: Modifier = Modifier, iconSize: Dp = 16.dp, color: Color = Color(0xFF64748B)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        drawCircle(color, radius = w * 0.26f, center = center, style = Stroke(strokeWidth))
        drawCircle(color, radius = w * 0.08f, center = center)
        drawLine(color, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.28f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.5f, h * 0.72f), Offset(w * 0.5f, h * 0.88f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.12f, h * 0.5f), Offset(w * 0.28f, h * 0.5f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.72f, h * 0.5f), Offset(w * 0.88f, h * 0.5f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomControllerLineIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        // gamepad outline
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.22f),
            size = Size(w * 0.76f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.18f),
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // simple D-pad plus
        drawLine(color, Offset(w * 0.25f, h * 0.5f), Offset(w * 0.42f, h * 0.5f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.33f, h * 0.4f), Offset(w * 0.33f, h * 0.6f), strokeWidth, cap = StrokeCap.Round)
        // action buttons
        drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.62f, h * 0.5f))
        drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.72f, h * 0.48f))
    }
}

@Composable
fun CustomMusicNoteLineIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        // note head
        drawCircle(color, radius = w * 0.16f, center = Offset(w * 0.35f, h * 0.7f), style = Stroke(strokeWidth))
        // stem line
        drawLine(color, Offset(w * 0.51f, h * 0.7f), Offset(w * 0.51f, h * 0.22f), strokeWidth, cap = StrokeCap.Round)
        // flag line
        val path = Path().apply {
            moveTo(w * 0.51f, h * 0.22f)
            quadraticTo(w * 0.68f, h * 0.24f, w * 0.78f, h * 0.38f)
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CustomAirplaneLineIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.62f, h * 0.42f)
            lineTo(w * 0.92f, h * 0.52f)
            lineTo(w * 0.62f, h * 0.62f)
            lineTo(w * 0.5f, h * 0.92f)
            lineTo(w * 0.38f, h * 0.62f)
            lineTo(w * 0.08f, h * 0.52f)
            lineTo(w * 0.38f, h * 0.42f)
            close()
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CustomCrossedForkSpoonIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.5.dp.toPx()
        // crossed handles
        drawLine(color, Offset(w * 0.22f, h * 0.78f), Offset(w * 0.78f, h * 0.22f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.78f, h * 0.78f), Offset(w * 0.22f, h * 0.22f), strokeWidth, cap = StrokeCap.Round)
        // Spoon head
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.72f, h * 0.28f), style = Stroke(strokeWidth))
        // Fork prongs
        drawLine(color, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.15f, h * 0.35f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.29f, h * 0.29f), Offset(w * 0.19f, h * 0.39f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomClapperboardIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.25f),
            size = Size(w * 0.76f, h * 0.55f),
            cornerRadius = CornerRadius(w * 0.08f),
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(color, Offset(w * 0.12f, h * 0.44f), Offset(w * 0.88f, h * 0.44f), strokeWidth)
        // clap details
        drawLine(color, Offset(w * 0.35f, h * 0.25f), Offset(w * 0.48f, h * 0.44f), strokeWidth)
        drawLine(color, Offset(w * 0.6f, h * 0.25f), Offset(w * 0.73f, h * 0.44f), strokeWidth)
    }
}

@Composable
fun CustomTrophyLineIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        // cup
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.25f, h * 0.18f),
            size = Size(w * 0.5f, h * 0.4f),
            cornerRadius = CornerRadius(w * 0.12f),
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // stand shaft
        drawLine(color, Offset(w * 0.5f, h * 0.58f), Offset(w * 0.5f, h * 0.78f), strokeWidth)
        // base plate
        drawLine(color, Offset(w * 0.3f, h * 0.78f), Offset(w * 0.7f, h * 0.78f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomCodeTagsIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        // left tag <
        val leftPath = Path().apply {
            moveTo(w * 0.32f, h * 0.32f)
            lineTo(w * 0.12f, h * 0.5f)
            lineTo(w * 0.32f, h * 0.68f)
        }
        drawPath(leftPath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // right tag >
        val rightPath = Path().apply {
            moveTo(w * 0.68f, h * 0.32f)
            lineTo(w * 0.88f, h * 0.5f)
            lineTo(w * 0.68f, h * 0.68f)
        }
        drawPath(rightPath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // slash tag /
        drawLine(color, Offset(w * 0.58f, h * 0.22f), Offset(w * 0.42f, h * 0.78f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomPenLineIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        // diagonal pen shaft
        drawLine(color, Offset(w * 0.22f, h * 0.78f), Offset(w * 0.72f, h * 0.28f), strokeWidth, cap = StrokeCap.Round)
        // pen tip
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.78f)
            lineTo(w * 0.22f, h * 0.62f)
            lineTo(w * 0.38f, h * 0.78f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun CustomCookingHatIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        // head chef hat puffs
        drawCircle(color, radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.3f), style = Stroke(strokeWidth))
        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.3f, h * 0.45f), style = Stroke(strokeWidth))
        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.7f, h * 0.45f), style = Stroke(strokeWidth))
        // bottom band
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.24f, h * 0.65f),
            size = Size(w * 0.52f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.04f),
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun CustomTvAntennaIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.15f, h * 0.35f),
            size = Size(w * 0.7f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.08f),
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // 2 antennas
        drawLine(color, Offset(w * 0.5f, h * 0.35f), Offset(w * 0.28f, h * 0.15f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.5f, h * 0.35f), Offset(w * 0.72f, h * 0.15f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomDumbbellLineIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        // horizontal shaft
        drawLine(color, Offset(w * 0.18f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth, cap = StrokeCap.Round)
        // weights left
        drawRoundRect(color, topLeft = Offset(w * 0.1f, h * 0.25f), size = Size(w * 0.12f, h * 0.5f), cornerRadius = CornerRadius(w * 0.03f), style = Stroke(strokeWidth))
        // weights right
        drawRoundRect(color, topLeft = Offset(w * 0.78f, h * 0.25f), size = Size(w * 0.12f, h * 0.5f), cornerRadius = CornerRadius(w * 0.03f), style = Stroke(strokeWidth))
    }
}

@Composable
fun CustomOpenBookLineIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        // left page curve
        val path1 = Path().apply {
            moveTo(w * 0.5f, h * 0.8f)
            quadraticTo(w * 0.35f, h * 0.65f, w * 0.12f, h * 0.7f)
            lineTo(w * 0.12f, h * 0.26f)
            quadraticTo(w * 0.35f, h * 0.2f, w * 0.5f, h * 0.35f)
        }
        drawPath(path1, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // right page curve
        val path2 = Path().apply {
            moveTo(w * 0.5f, h * 0.8f)
            quadraticTo(w * 0.65f, h * 0.65f, w * 0.88f, h * 0.7f)
            lineTo(w * 0.88f, h * 0.26f)
            quadraticTo(w * 0.65f, h * 0.2f, w * 0.5f, h * 0.35f)
        }
        drawPath(path2, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CustomPaletteArtIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        drawCircle(color, radius = w * 0.38f, center = center, style = Stroke(strokeWidth))
        // 3 paint dabs inside
        drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.42f, h * 0.38f))
        drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.6f, h * 0.48f))
        drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.45f, h * 0.62f))
    }
}

@Composable
fun CustomLeafLineIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.85f)
            cubicTo(w * 0.15f, h * 0.35f, w * 0.5f, h * 0.15f, w * 0.85f, h * 0.15f)
            cubicTo(w * 0.85f, h * 0.65f, w * 0.5f, h * 0.85f, w * 0.15f, h * 0.85f)
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(w * 0.15f, h * 0.85f), Offset(w * 0.85f, h * 0.15f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomPawLineIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color.Black) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        // center paw pad
        drawCircle(color, radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.64f))
        // 4 smaller toe circles
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.26f, h * 0.42f))
        drawCircle(color, radius = w * 0.09f, center = Offset(w * 0.4f, h * 0.28f))
        drawCircle(color, radius = w * 0.09f, center = Offset(w * 0.6f, h * 0.28f))
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.74f, h * 0.42f))
    }
}

@Composable
fun CustomCloseIcon(modifier: Modifier = Modifier, iconSize: Dp = 8.dp, color: Color = Color.White) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        drawLine(color, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.75f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.75f, h * 0.25f), Offset(w * 0.25f, h * 0.75f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomPlusIcon(modifier: Modifier = Modifier, iconSize: Dp = 16.dp, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.dp.toPx()
        drawLine(color, Offset(w * 0.5f, h * 0.22f), Offset(w * 0.5f, h * 0.78f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.22f, h * 0.5f), Offset(w * 0.78f, h * 0.5f), strokeWidth, cap = StrokeCap.Round)
    }
}
