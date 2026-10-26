package com.example.ui.screens.profile

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.AuthViewModel
import com.example.UserProfile
import com.example.ui.components.CircleFlag
import com.example.ui.components.resolveAvatarModel
import com.example.ui.screens.CountryData
import com.example.ui.screens.LanguageData
import com.example.ui.screens.profile.components.*
import kotlinx.coroutines.launch

// High-contrast tokens ensuring NO white-on-white text issues
private val AppBackground = Color(0xFFFAFAEE)
private val SurfaceCard = Color.White
private val InputCardBg = Color(0xFFF8F9FA)
private val PrimaryYellow = Color(0xFFF8C63D)
private val AccentGold = Color(0xFFEAB308)
private val TextPrimary = Color(0xFF111827)     // Deep rich black
private val TextSecondary = Color(0xFF4B5563)   // Readable slate gray
private val TextPlaceholder = Color(0xFF9CA3AF) // Muted hint
private val BorderLight = Color(0xFFE5E7EB)
private val BorderActive = Color(0xFFF8C63D)

@OptIn(ExperimentalLayoutApi::class)
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

    // Screen States mapping directly to UserProfile model
    var editUsername by remember { mutableStateOf(currentUserProfile.username ?: "kali") }
    var editDisplayName by remember { mutableStateOf(currentUserProfile.displayName ?: "Hacker") }
    var editAvatar by remember { mutableStateOf(currentUserProfile.avatar.orEmpty()) }
    var editCover by remember { mutableStateOf(currentUserProfile.coverUrl.orEmpty()) }

    var editBio by remember {
        mutableStateOf(
            currentUserProfile.bio
                ?: savedBio
                ?: currentUserProfile.dob
                ?: "I love anime, coding and making new friends around the world! 🌍"
        )
    }
    var editAge by remember { mutableStateOf((currentUserProfile.age ?: savedAge ?: 21).coerceAtLeast(15)) }
    var editGender by remember { mutableStateOf(currentUserProfile.gender ?: "Male") }
    var editCountry by remember { mutableStateOf(currentUserProfile.country ?: "India") }
    var editCountryCode by remember { mutableStateOf(currentUserProfile.countryCode ?: "in") }
    var editCity by remember { mutableStateOf(currentUserProfile.city ?: savedCity ?: "Ghaziabad") }

    // Formatted Languages List e.g. ["Hindi (Advanced)", "English (Advanced)"]
    val userLanguages = remember { mutableStateListOf<String>() }

    // Selected Interests / Hobbies
    val selectedInterests = remember { mutableStateListOf<String>() }

    // Standard preset interests matching mockup
    val defaultInterestPresets = listOf(
        "Technology", "Writing", "Cooking", "K-Drama", "Fitness",
        "Reading", "Art", "Nature", "Pets", "coding", "Music", "Travel"
    )

    // Initial data fetch from Supabase
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
                    if (p.age != null && p.age >= 15) editAge = p.age
                    if (!p.gender.isNullOrBlank()) editGender = p.gender
                    if (!p.country.isNullOrBlank()) editCountry = p.country
                    if (!p.country_code.isNullOrBlank()) editCountryCode = p.country_code
                    if (!p.city.isNullOrBlank()) editCity = p.city

                    if (!p.native_languages.isNullOrEmpty()) {
                        userLanguages.clear()
                        userLanguages.addAll(p.native_languages)
                    } else if (!currentUserProfile.nativeLanguage.isNullOrBlank()) {
                        userLanguages.clear()
                        userLanguages.addAll(
                            currentUserProfile.nativeLanguage.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .map { if (!it.contains("(")) "$it (Advanced)" else it }
                        )
                    }

                    if (!p.hobbies.isNullOrEmpty()) {
                        selectedInterests.clear()
                        selectedInterests.addAll(p.hobbies)
                    } else if (currentUserProfile.hobbies.isNotEmpty()) {
                        selectedInterests.clear()
                        selectedInterests.addAll(currentUserProfile.hobbies)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("EditProfileScreen", "Failed to load profile data from Supabase", e)
        }

        // Fallback defaults matching mockup if lists are empty
        if (userLanguages.isEmpty()) {
            userLanguages.addAll(listOf("Hindi (Advanced)", "English (Advanced)"))
        }
        if (selectedInterests.isEmpty()) {
            selectedInterests.addAll(listOf("Technology", "Writing", "Cooking", "Fitness", "coding"))
        }
    }

    // BottomSheet states
    var showCountrySheet by remember { mutableStateOf(false) }
    var showCitySheet by remember { mutableStateOf(false) }
    var showAgeSheet by remember { mutableStateOf(false) }
    var showGenderSheet by remember { mutableStateOf(false) }
    var showLanguagesSheet by remember { mutableStateOf(false) }
    var showAddInterestDialog by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Zero-permission Photo Pickers
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            editAvatar = uri.toString()
        }
    }

    // Profile Commit to Supabase & Local Cache
    fun saveProfileChanges() {
        if (isSaving) return

        val cleanUsername = editUsername.trim().lowercase().replace("@", "")
        if (cleanUsername.isBlank()) {
            Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (cleanUsername.length < 3) {
            Toast.makeText(context, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show()
            return
        }
        if (editDisplayName.trim().isBlank()) {
            Toast.makeText(context, "Display Name cannot be blank", Toast.LENGTH_SHORT).show()
            return
        }
        if (editAge < 15) {
            Toast.makeText(context, "Minimum age requirement is 15 years", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        coroutineScope.launch {
            try {
                // Save to local SharedPreferences for instant UI access
                prefs.edit()
                    .putString("profile_bio_${currentUserProfile.uid}", editBio.trim())
                    .putString("profile_city_${currentUserProfile.uid}", editCity.trim())
                    .putInt("profile_age_${currentUserProfile.uid}", editAge)
                    .apply()

                if (viewModel != null) {
                    viewModel.updateProfile(
                        context = context,
                        username = cleanUsername,
                        displayName = editDisplayName.trim(),
                        avatar = editAvatar,
                        coverUrl = editCover.trim().ifBlank { null },
                        dob = editBio.trim(),
                        gender = editGender,
                        nativeLanguage = userLanguages.joinToString(","),
                        learningLanguage = "",
                        country = editCountry,
                        countryCode = editCountryCode,
                        hobbies = selectedInterests.toList(),
                        age = editAge,
                        bio = editBio.trim(),
                        city = editCity.trim(),
                        onError = { err ->
                            errorMessage = err
                            isSaving = false
                            Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                        },
                        onSuccess = {
                            isSaving = false
                            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )
                } else {
                    isSaving = false
                    Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to save profile changes"
                Log.e("EditProfileScreen", "Error saving profile", e)
                isSaving = false
                Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper to extract country flag from language string e.g. "Hindi (Advanced)" -> "in"
    fun getFlagForLanguage(langStr: String): String {
        val rawName = langStr.substringBefore("(").trim()
        val match = LanguageData.popularLanguages.find { it.name.equals(rawName, ignoreCase = true) }
            ?: LanguageData.allLanguages.find { it.name.equals(rawName, ignoreCase = true) }
        return match?.flagCode ?: "un"
    }

    // Helper for category vector icon
    fun getInterestIcon(interest: String): ImageVector {
        return when (interest.lowercase()) {
            "technology" -> Icons.Default.Code
            "writing" -> Icons.Default.Edit
            "cooking" -> Icons.Default.Restaurant
            "k-drama", "movies" -> Icons.Default.Tv
            "fitness", "gym" -> Icons.Default.FitnessCenter
            "reading", "books" -> Icons.Default.MenuBook
            "art" -> Icons.Default.Palette
            "nature" -> Icons.Default.Eco
            "pets", "animals" -> Icons.Default.Pets
            "coding" -> Icons.Default.Favorite
            "music" -> Icons.Default.MusicNote
            "travel" -> Icons.Default.Flight
            else -> Icons.Default.LocalOffer
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ----------------------------------------------------
            // 1. TOP BAR
            // ----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEAEAE0))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title
                Text(
                    text = "Edit profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Save Checkmark Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PrimaryYellow)
                        .clickable(enabled = !isSaving) { saveProfileChanges() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = TextPrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save profile",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ----------------------------------------------------
            // 2. SCROLLABLE FORM BODY
            // ----------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Avatar with Camera Badge Overlay
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(104.dp)) {
                        AsyncImage(
                            model = resolveAvatarModel(editAvatar),
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, Color(0xFFE5E7EB), CircleShape)
                                .clickable {
                                    avatarPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        )

                        // Camera overlay badge
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color(0xFF1F2937))
                                .border(2.dp, Color.White, CircleShape)
                                .clickable {
                                    avatarPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Change Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Display Name Field Card
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Display name",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3F4ED))
                            .border(1.dp, Color(0xFFE5E7DE), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = editDisplayName,
                            onValueChange = { editDisplayName = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            ),
                            cursorBrush = SolidColor(TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Username Field Card with 4/30 Counter
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Username",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = "${editUsername.length}/30",
                            fontSize = 12.sp,
                            color = TextPlaceholder
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3F4ED))
                            .border(1.dp, Color(0xFFE5E7DE), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = editUsername,
                            onValueChange = {
                                if (it.length <= 30) editUsername = it.lowercase().replace(" ", "")
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            ),
                            cursorBrush = SolidColor(TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bio Field Card with 48/150 Counter
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bio",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = "${editBio.length}/150",
                            fontSize = 12.sp,
                            color = TextPlaceholder
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 88.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3F4ED))
                            .border(1.dp, Color(0xFFE5E7DE), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        BasicTextField(
                            value = editBio,
                            onValueChange = {
                                if (it.length <= 150) editBio = it
                            },
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ----------------------------------------------------
                // 3. PERSONAL DETAILS SECTION (2x2 Grid)
                // ----------------------------------------------------
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(16.dp)
                            .background(AccentGold, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Personal Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // 2x2 Grid: Age & Gender (Row 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Age Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3F4ED))
                            .border(1.dp, Color(0xFFE5E7DE), RoundedCornerShape(14.dp))
                            .clickable { showAgeSheet = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Age",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Age",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$editAge",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Gender Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3F4ED))
                            .border(1.dp, Color(0xFFE5E7DE), RoundedCornerShape(14.dp))
                            .clickable { showGenderSheet = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Gender",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Gender",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = editGender,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2x2 Grid: Country & City (Row 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Country Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3F4ED))
                            .border(1.dp, Color(0xFFE5E7DE), RoundedCornerShape(14.dp))
                            .clickable { showCountrySheet = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Country",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                ) {
                                    CircleFlag(
                                        countryCode = editCountryCode,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = editCountry,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // City Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3F4ED))
                            .border(1.dp, Color(0xFFE5E7DE), RoundedCornerShape(14.dp))
                            .clickable { showCitySheet = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "City",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "City",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = editCity,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ----------------------------------------------------
                // 4. LANGUAGES SECTION
                // ----------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(16.dp)
                                .background(AccentGold, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Languages",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "Edit",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showLanguagesSheet = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Languages Chips FlowRow
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    userLanguages.forEach { langItem ->
                        val flagCode = getFlagForLanguage(langItem)

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF3F4ED))
                                .border(1.dp, Color(0xFFE5E7DE), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                            ) {
                                CircleFlag(
                                    countryCode = flagCode,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = langItem,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        userLanguages.remove(langItem)
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ----------------------------------------------------
                // 5. INTERESTS SECTION
                // ----------------------------------------------------
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(16.dp)
                            .background(AccentGold, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Interests",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // All unique interests including presets and user custom additions
                val allDisplayInterests = remember(selectedInterests.toList()) {
                    (defaultInterestPresets + selectedInterests).distinct()
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allDisplayInterests.forEach { interest ->
                        val isSelected = selectedInterests.contains(interest)

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFFFFF7ED) else Color(0xFFF3F4ED))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFFF97316) else Color(0xFFE5E7DE),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    if (isSelected) {
                                        selectedInterests.remove(interest)
                                    } else {
                                        selectedInterests.add(interest)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getInterestIcon(interest),
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFFEA580C) else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = interest,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFFC2410C) else TextPrimary
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // "+ Add Interest" Dashed Chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .drawBehind {
                                val stroke = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                )
                                drawRoundRect(
                                    color = Color(0xFFD1D5DB),
                                    style = stroke,
                                    cornerRadius = CornerRadius(20.dp.toPx())
                                )
                            }
                            .clickable { showAddInterestDialog = true }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add custom interest",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Interest",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // 6. MODULAR BOTTOM SHEETS & DIALOGS
    // ----------------------------------------------------

    // Country Sheet
    if (showCountrySheet) {
        ChooseCountrySheet(
            selectedCountryName = editCountry,
            onCountrySelected = { name, code ->
                editCountry = name
                editCountryCode = code
                // Update city defaults for selected country
                val popular = PlaceDbService.getPopularCities(code)
                if (popular.isNotEmpty()) {
                    editCity = popular.first().name
                }
            },
            onDismiss = { showCountrySheet = false }
        )
    }

    // City Sheet with PlaceDB
    if (showCitySheet) {
        ChooseCitySheet(
            currentCity = editCity,
            currentCountryName = editCountry,
            currentCountryCode = editCountryCode,
            onCountryClick = {
                showCitySheet = false
                showCountrySheet = true
            },
            onCitySelected = { cityName ->
                editCity = cityName
            },
            onDismiss = { showCitySheet = false }
        )
    }

    // Age Sheet (Strict 15+ years enforcement)
    if (showAgeSheet) {
        ChooseAgeSheet(
            initialAge = editAge,
            onAgeSelected = { age ->
                editAge = age
            },
            onDismiss = { showAgeSheet = false }
        )
    }

    // Gender Sheet
    if (showGenderSheet) {
        SelectGenderSheet(
            initialGender = editGender,
            onGenderSelected = { gender ->
                editGender = gender
            },
            onDismiss = { showGenderSheet = false }
        )
    }

    // Languages Sheet
    if (showLanguagesSheet) {
        AddLanguagesSheet(
            alreadySelectedFormatted = userLanguages.toList(),
            onLanguagesAdded = { updatedList ->
                userLanguages.clear()
                userLanguages.addAll(updatedList)
            },
            onDismiss = { showLanguagesSheet = false }
        )
    }

    // Custom Interest Dialog
    if (showAddInterestDialog) {
        AddCustomInterestDialog(
            onInterestAdded = { newInterest ->
                if (!selectedInterests.contains(newInterest)) {
                    selectedInterests.add(newInterest)
                }
            },
            onDismiss = { showAddInterestDialog = false }
        )
    }
}
