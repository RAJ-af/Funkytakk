package com.example.ui.screens.profile

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.AuthViewModel
import com.example.R
import com.example.UserProfile
import com.example.ui.components.CircleFlag
import com.example.ui.components.resolveAvatarModel
import com.example.ui.screens.CountryData
import com.example.ui.screens.LanguageData
import com.example.ui.screens.ProfileCalendarIcon
import com.example.ui.screens.profile.components.*
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    currentUserProfile: UserProfile,
    viewModel: AuthViewModel?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val funkyColors = LocalFunkyColors.current

    BackHandler { onDismiss() }

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
    var editCity by remember { mutableStateOf(currentUserProfile.city ?: savedCity ?: "") }

    // Formatted Languages Lists capped at 3 each matching Supabase backend contract
    val nativeLanguages = remember { mutableStateListOf<String>() }
    val learningLanguages = remember { mutableStateListOf<String>() }

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
                        nativeLanguages.clear()
                        nativeLanguages.addAll(
                            p.native_languages.take(3).map { item ->
                                val trimmed = item.trim()
                                if (!trimmed.contains("(")) "$trimmed (Native)" else trimmed
                            }
                        )
                    } else if (!currentUserProfile.nativeLanguage.isNullOrBlank()) {
                        nativeLanguages.clear()
                        nativeLanguages.addAll(
                            currentUserProfile.nativeLanguage.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .take(3)
                                .map { if (!it.contains("(")) "$it (Native)" else it }
                        )
                    }

                    if (!p.learning_languages.isNullOrEmpty()) {
                        learningLanguages.clear()
                        learningLanguages.addAll(
                            p.learning_languages.take(3).map { item ->
                                val trimmed = item.trim()
                                if (!trimmed.contains("(")) "$trimmed (Intermediate)" else trimmed
                            }
                        )
                    } else if (!currentUserProfile.learningLanguage.isNullOrBlank()) {
                        learningLanguages.clear()
                        learningLanguages.addAll(
                            currentUserProfile.learningLanguage.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .take(3)
                                .map { if (!it.contains("(")) "$it (Intermediate)" else it }
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

        if (nativeLanguages.isEmpty() && !currentUserProfile.nativeLanguage.isNullOrBlank()) {
            nativeLanguages.addAll(
                currentUserProfile.nativeLanguage.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(3)
                    .map { if (!it.contains("(")) "$it (Native)" else it }
            )
        }
        if (learningLanguages.isEmpty() && !currentUserProfile.learningLanguage.isNullOrBlank()) {
            learningLanguages.addAll(
                currentUserProfile.learningLanguage.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(3)
                    .map { if (!it.contains("(")) "$it (Intermediate)" else it }
            )
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
    var showNativeLanguagesSheet by remember { mutableStateOf(false) }
    var showLearningLanguagesSheet by remember { mutableStateOf(false) }
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

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            editCover = uri.toString()
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
                val nativeCsv = nativeLanguages.take(3).joinToString(",")
                val learningCsv = learningLanguages.take(3).joinToString(",")

                // Save to local SharedPreferences for instant UI access
                prefs.edit()
                    .putString("profile_bio_${currentUserProfile.uid}", editBio.trim())
                    .putString("profile_city_${currentUserProfile.uid}", editCity.trim())
                    .putInt("profile_age_${currentUserProfile.uid}", editAge)
                    .putString("profile_native_lang_${currentUserProfile.uid}", nativeCsv)
                    .putString("profile_learning_lang_${currentUserProfile.uid}", learningCsv)
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
                        nativeLanguage = nativeCsv,
                        learningLanguage = learningCsv,
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

    // Helper to extract country/programming flag from language string e.g. "Hindi (Advanced)" -> "in"
    fun getFlagForLanguage(langStr: String): String {
        val withoutLevel = langStr.replace(
            Regex("\\s*\\((?:Beginner|Elementary|Intermediate|Upper Intermediate|Advanced|Native / Fluent|Native|Fluent)\\)\\s*$", RegexOption.IGNORE_CASE),
            ""
        ).trim()
        val rawName = (if (withoutLevel != langStr) withoutLevel else langStr.substringBefore("(")).trim()
        val normalized = when (rawName.lowercase()) {
            "chinese", "mandarin", "chinese (mandarin)", "cantonese", "zh" -> "Chinese"
            "c", "c language" -> "C"
            else -> rawName
        }
        val match = LanguageData.popularLanguages.find { it.name.equals(normalized, ignoreCase = true) || it.code.equals(normalized, ignoreCase = true) }
            ?: LanguageData.allLanguages.find { it.name.equals(normalized, ignoreCase = true) || it.code.equals(normalized, ignoreCase = true) }
            ?: LanguageData.programmingLanguages.find { it.name.equals(normalized, ignoreCase = true) }
        return match?.flagCode ?: when (normalized.lowercase()) {
            "chinese" -> "cn"
            else -> "un"
        }
    }

    // Helper for colorful interest emoji badge (zero Material Icons)
    fun getInterestEmoji(interest: String): String {
        return when (interest.lowercase()) {
            "technology" -> "💻"
            "writing" -> "✍️"
            "cooking" -> "🍳"
            "k-drama", "movies" -> "🎬"
            "fitness", "gym" -> "💪"
            "reading", "books" -> "📚"
            "art" -> "🎨"
            "nature" -> "🌿"
            "pets", "animals" -> "🐾"
            "coding" -> "⚡"
            "music" -> "🎵"
            "travel" -> "✈️"
            else -> "✨"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(funkyColors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
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
                        .background(funkyColors.soft)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Back",
                        tint = funkyColors.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title
                Text(
                    text = "Edit profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = funkyColors.ink
                )

                // Save Checkmark Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(funkyColors.mango)
                        .clickable(enabled = !isSaving) { saveProfileChanges() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = funkyColors.onMango,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = "Save profile",
                            tint = funkyColors.onMango,
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

                // Cover Banner & Avatar Header Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    // Cover Banner Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFF8C63D))
                                )
                            )
                            .border(1.dp, funkyColors.line, RoundedCornerShape(16.dp))
                            .clickable {
                                coverPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        if (editCover.isNotBlank()) {
                            AsyncImage(
                                model = editCover,
                                contentDescription = "Profile Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_photo),
                                        contentDescription = "Add Cover",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Add Profile Banner",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Change Banner badge on top-end
                        Box(
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.TopEnd)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable {
                                    coverPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_photo),
                                    contentDescription = "Change Cover",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Banner",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Avatar overlapping bottom center
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 38.dp)
                            .size(92.dp)
                    ) {
                        AsyncImage(
                            model = resolveAvatarModel(editAvatar),
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .border(3.dp, funkyColors.surface, CircleShape)
                                .clickable {
                                    avatarPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        )

                        // Camera overlay badge
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(funkyColors.ink)
                                .border(2.dp, funkyColors.surface, CircleShape)
                                .clickable {
                                    avatarPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_photo),
                                contentDescription = "Change Avatar",
                                tint = funkyColors.bg,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(44.dp))

                // Display Name Field Card
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Display name",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = funkyColors.mute,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(funkyColors.surface)
                            .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
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
                                color = funkyColors.ink
                            ),
                            cursorBrush = SolidColor(funkyColors.mango),
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
                            color = funkyColors.mute
                        )
                        Text(
                            text = "${editUsername.length}/30",
                            fontSize = 12.sp,
                            color = funkyColors.mute
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(funkyColors.surface)
                            .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
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
                                color = funkyColors.ink
                            ),
                            cursorBrush = SolidColor(funkyColors.mango),
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
                            color = funkyColors.mute
                        )
                        Text(
                            text = "${editBio.length}/150",
                            fontSize = 12.sp,
                            color = funkyColors.mute
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 88.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(funkyColors.surface)
                            .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
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
                                color = funkyColors.ink,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(funkyColors.mango),
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
                            .background(funkyColors.mango, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Personal Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.ink
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
                            .background(funkyColors.surface)
                            .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                            .clickable { showAgeSheet = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Age",
                            fontSize = 11.sp,
                            color = funkyColors.mute,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProfileCalendarIcon(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFFD97706)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$editAge",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.ink
                                )
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_down),
                                contentDescription = null,
                                tint = funkyColors.mute,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Gender Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(funkyColors.surface)
                            .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                            .clickable { showGenderSheet = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Gender",
                            fontSize = 11.sp,
                            color = funkyColors.mute,
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
                                    painter = painterResource(id = R.drawable.ic_profile),
                                    contentDescription = "Gender",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = editGender,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.ink
                                )
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_down),
                                contentDescription = null,
                                tint = funkyColors.mute,
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
                            .background(funkyColors.surface)
                            .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                            .clickable { showCountrySheet = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Country",
                            fontSize = 11.sp,
                            color = funkyColors.mute,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                ) {
                                    CircleFlag(
                                        countryCode = editCountryCode,
                                        allowProgramming = false,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = editCountry,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.ink,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_down),
                                contentDescription = null,
                                tint = funkyColors.mute,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // City Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(funkyColors.surface)
                            .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                            .clickable { showCitySheet = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "City",
                            fontSize = 11.sp,
                            color = funkyColors.mute,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_pin),
                                    contentDescription = "City",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = editCity,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.ink,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_down),
                                contentDescription = null,
                                tint = funkyColors.mute,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ----------------------------------------------------
                // 4. LANGUAGES SECTION (Native & Learning, Max 3 each)
                // ----------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(16.dp)
                                .background(funkyColors.mango, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Native Languages (${nativeLanguages.size}/3)",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink
                        )
                    }

                    Text(
                        text = "Edit",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.mango,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showNativeLanguagesSheet = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (nativeLanguages.isEmpty()) {
                    Text(
                        text = "Tap Edit to add up to 3 native languages",
                        fontSize = 12.5.sp,
                        color = funkyColors.mute,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        nativeLanguages.forEach { langItem ->
                            val flagCode = getFlagForLanguage(langItem)
                            val badgeStyle = com.example.ui.components.getLanguageBadgeStyle(langItem)

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(badgeStyle.bgColor)
                                    .border(1.dp, badgeStyle.textColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
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
                                        allowProgramming = flagCode.startsWith("prog_"),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = langItem,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = badgeStyle.textColor
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Remove",
                                    tint = funkyColors.mute,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            nativeLanguages.remove(langItem)
                                        }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(16.dp)
                                .background(funkyColors.ok, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Learning Languages (${learningLanguages.size}/3)",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink
                        )
                    }

                    Text(
                        text = "Edit",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.mango,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showLearningLanguagesSheet = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (learningLanguages.isEmpty()) {
                    Text(
                        text = "Tap Edit to add up to 3 learning languages",
                        fontSize = 12.5.sp,
                        color = funkyColors.mute
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        learningLanguages.forEach { langItem ->
                            val flagCode = getFlagForLanguage(langItem)
                            val badgeStyle = com.example.ui.components.getLanguageBadgeStyle(langItem)

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(badgeStyle.bgColor)
                                    .border(1.dp, badgeStyle.textColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
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
                                        allowProgramming = flagCode.startsWith("prog_"),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = langItem,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = badgeStyle.textColor
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Remove",
                                    tint = funkyColors.mute,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            learningLanguages.remove(langItem)
                                        }
                                )
                            }
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
                            .background(funkyColors.mango, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Interests",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.ink
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
                                .background(if (isSelected) funkyColors.soft else funkyColors.surface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) funkyColors.mango else funkyColors.line,
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
                            Text(
                                text = getInterestEmoji(interest),
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = interest,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = funkyColors.ink
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(funkyColors.mango),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Selected",
                                        tint = funkyColors.onMango,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
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
                                    color = funkyColors.line,
                                    style = stroke,
                                    cornerRadius = CornerRadius(20.dp.toPx())
                                )
                            }
                            .clickable { showAddInterestDialog = true }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_plus),
                            contentDescription = "Add custom interest",
                            tint = funkyColors.mute,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Interest",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = funkyColors.mute
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

    // Native Languages Sheet (max 3)
    if (showNativeLanguagesSheet) {
        AddLanguagesSheet(
            alreadySelectedFormatted = nativeLanguages.toList(),
            title = "Native Languages",
            subtitle = "Choose up to 3 native languages",
            maxSelection = 3,
            onLanguagesAdded = { updatedList ->
                nativeLanguages.clear()
                nativeLanguages.addAll(updatedList.take(3))
            },
            onDismiss = { showNativeLanguagesSheet = false }
        )
    }

    // Learning Languages Sheet (max 3)
    if (showLearningLanguagesSheet) {
        AddLanguagesSheet(
            alreadySelectedFormatted = learningLanguages.toList(),
            title = "Learning Languages",
            subtitle = "Choose up to 3 languages you're learning",
            maxSelection = 3,
            onLanguagesAdded = { updatedList ->
                learningLanguages.clear()
                learningLanguages.addAll(updatedList.take(3))
            },
            onDismiss = { showLearningLanguagesSheet = false }
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
