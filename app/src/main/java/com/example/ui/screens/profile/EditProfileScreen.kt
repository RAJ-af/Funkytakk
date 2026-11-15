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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.AuthViewModel
import com.example.EditProfileSnapshot
import com.example.ProfileFieldLimitDto
import com.example.R
import com.example.UserProfile
import com.example.ui.components.CircleFlag
import com.example.ui.components.resolveAvatarModel
import com.example.ui.screens.CountryData
import com.example.ui.screens.LanguageData
import com.example.ui.screens.ProfileCalendarIcon
import com.example.ui.screens.profile.components.*
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.delay
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
    var editUsername by remember { mutableStateOf((currentUserProfile.username ?: "").trim().lowercase()) }
    var editDisplayName by remember { mutableStateOf((currentUserProfile.displayName ?: "").trim()) }
    var editAvatar by remember { mutableStateOf(currentUserProfile.avatar.orEmpty()) }
    var editCover by remember { mutableStateOf(currentUserProfile.coverUrl.orEmpty()) }

    var editBio by remember {
        mutableStateOf(
            (currentUserProfile.bio ?: savedBio ?: currentUserProfile.dob ?: "").trim()
        )
    }
    var editAge by remember { mutableStateOf((currentUserProfile.age ?: savedAge ?: 21).coerceAtLeast(15)) }
    var editGender by remember { mutableStateOf(currentUserProfile.gender ?: "Male") }
    var editCountry by remember { mutableStateOf(currentUserProfile.country ?: "India") }
    var editCountryCode by remember { mutableStateOf(currentUserProfile.countryCode ?: "in") }
    var editCity by remember { mutableStateOf((currentUserProfile.city ?: savedCity ?: "").trim()) }

    // Formatted Languages Lists capped at 3 each matching Supabase backend contract
    val nativeLanguages = remember { mutableStateListOf<String>() }
    val learningLanguages = remember { mutableStateListOf<String>() }

    // Selected Interests / Hobbies
    val selectedInterests = remember { mutableStateListOf<String>() }

    // Baseline snapshot of loaded server profile (used to compute diff-only PATCH)
    var loadedSnapshot by remember {
        mutableStateOf(
            EditProfileSnapshot(
                username = (currentUserProfile.username ?: "").trim().lowercase(),
                displayName = (currentUserProfile.displayName ?: "").trim(),
                avatar = currentUserProfile.avatar.orEmpty(),
                coverUrl = currentUserProfile.coverUrl.orEmpty(),
                bio = (currentUserProfile.bio ?: savedBio ?: currentUserProfile.dob ?: "").trim(),
                age = (currentUserProfile.age ?: savedAge ?: 21).coerceAtLeast(15),
                gender = currentUserProfile.gender ?: "Male",
                country = currentUserProfile.country ?: "India",
                countryCode = currentUserProfile.countryCode ?: "in",
                city = (currentUserProfile.city ?: savedCity ?: "").trim(),
                nativeLanguages = currentUserProfile.nativeLanguage.orEmpty()
                    .split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(3),
                learningLanguages = currentUserProfile.learningLanguage.orEmpty()
                    .split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(3),
                hobbies = currentUserProfile.hobbies
            )
        )
    }

    // Standard preset interests matching mockup
    val defaultInterestPresets = listOf(
        "Technology", "Writing", "Cooking", "K-Drama", "Fitness",
        "Reading", "Art", "Nature", "Pets", "coding", "Music", "Travel"
    )

    // Profile edit limits from server
    val profileLimits by (viewModel?.profileEditLimits?.collectAsState()
        ?: remember { mutableStateOf(emptyMap<String, ProfileFieldLimitDto>()) })

    val usernameLimit = profileLimits["username"]
    val displayNameLimit = profileLimits["profile_name"]
    val bioLimit = profileLimits["bio"]
    val ageLimit = profileLimits["age"]
    val genderLimit = profileLimits["gender"]
    val countryLimit = profileLimits["country_code"]
    val cityLimit = profileLimits["city"]
    val avatarLimit = profileLimits["avatar_url"]
    val coverLimit = profileLimits["cover_url"]

    val canChangeUsername = usernameLimit == null || usernameLimit.remaining > 0
    val canChangeDisplayName = displayNameLimit == null || displayNameLimit.remaining > 0
    val canChangeBio = bioLimit == null || bioLimit.remaining > 0
    val canChangeAge = ageLimit == null || ageLimit.remaining > 0
    val canChangeGender = genderLimit == null || genderLimit.remaining > 0
    val canChangeCountry = countryLimit == null || countryLimit.remaining > 0
    val canChangeCity = cityLimit == null || cityLimit.remaining > 0
    val canChangeAvatar = avatarLimit == null || avatarLimit.remaining > 0
    val canChangeCover = coverLimit == null || coverLimit.remaining > 0

    fun formatLockedCaption(limit: ProfileFieldLimitDto?): String? {
        if (limit == null || limit.remaining > 0) return null
        val localDate = viewModel?.formatIsoToLocalDate(limit.next_allowed_at)
            ?.ifBlank { limit.next_allowed_at?.take(10).orEmpty() }
            ?: limit.next_allowed_at?.take(10).orEmpty()
        return if (localDate.isNotBlank()) {
            "You can change this again on $localDate"
        } else {
            "You've reached the change limit for this field"
        }
    }

    val usernameWindowDays = ((usernameLimit?.window_hours ?: 720) / 24).coerceAtLeast(1)
    val usernameMaxChanges = usernameLimit?.max_changes ?: 1
    val usernameCooldownCaption = if (usernameMaxChanges <= 1) {
        "You can change your username once every $usernameWindowDays days."
    } else {
        "You can change your username up to $usernameMaxChanges times every $usernameWindowDays days."
    }

    // Initial data & limits fetch from Supabase
    LaunchedEffect(Unit) {
        launch {
            viewModel?.fetchProfileEditLimits(context)
        }
        try {
            val supaUid = com.example.SupabaseSessionManager.getUserId(context) ?: currentUserProfile.uid
            val bearerAuth = com.example.SupabaseSessionManager.getBearerToken(context)
            val res = com.example.SupabaseClient.api.getProfile(auth = bearerAuth, authUserIdEq = "eq.$supaUid")
            if (res.isSuccessful) {
                val p = res.body()?.firstOrNull()
                if (p != null) {
                    val serverUsername = (p.username ?: currentUserProfile.username.orEmpty()).trim().lowercase()
                    val serverDisplayName = (p.profile_name ?: currentUserProfile.displayName.orEmpty()).trim()
                    val serverAvatar = p.avatar_url ?: currentUserProfile.avatar.orEmpty()
                    val serverCover = p.cover_url ?: currentUserProfile.coverUrl.orEmpty()
                    val serverBio = (p.bio ?: "").trim()
                    val serverAge = (p.age ?: currentUserProfile.age ?: savedAge ?: 21).coerceAtLeast(15)
                    val serverGender = p.gender?.takeIf { it.isNotBlank() } ?: (currentUserProfile.gender ?: "Male")
                    val serverCountry = p.country?.takeIf { it.isNotBlank() } ?: (currentUserProfile.country ?: "India")
                    val serverCountryCode = p.country_code?.takeIf { it.isNotBlank() } ?: (currentUserProfile.countryCode ?: "in")
                    val serverCity = (p.city ?: "").trim()

                    editDisplayName = serverDisplayName
                    editUsername = serverUsername
                    editAvatar = serverAvatar
                    editCover = serverCover
                    editBio = serverBio
                    editAge = serverAge
                    editGender = serverGender
                    editCountry = serverCountry
                    editCountryCode = serverCountryCode
                    editCity = serverCity

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

                    loadedSnapshot = EditProfileSnapshot(
                        username = serverUsername,
                        displayName = serverDisplayName,
                        avatar = serverAvatar,
                        coverUrl = serverCover,
                        bio = serverBio,
                        age = serverAge,
                        gender = serverGender,
                        country = serverCountry,
                        countryCode = serverCountryCode,
                        city = serverCity,
                        nativeLanguages = nativeLanguages.toList(),
                        learningLanguages = learningLanguages.toList(),
                        hobbies = selectedInterests.toList()
                    )
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
            loadedSnapshot = loadedSnapshot.copy(nativeLanguages = nativeLanguages.toList())
        }
        if (learningLanguages.isEmpty() && !currentUserProfile.learningLanguage.isNullOrBlank()) {
            learningLanguages.addAll(
                currentUserProfile.learningLanguage.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(3)
                    .map { if (!it.contains("(")) "$it (Intermediate)" else it }
            )
            loadedSnapshot = loadedSnapshot.copy(learningLanguages = learningLanguages.toList())
        }
    }

    // Username availability check states
    val cleanUsername = editUsername.trim().lowercase().replace("@", "")
    val baselineUsername = loadedSnapshot.username.trim().lowercase()
    val isUsernameEdited = cleanUsername != baselineUsername
    val usernameRegex = remember { Regex("^[a-z0-9._]{3,20}$") }
    val isUsernameFormatValid = usernameRegex.matches(cleanUsername) &&
        !cleanUsername.startsWith(".") &&
        !cleanUsername.endsWith(".") &&
        !cleanUsername.contains("..")

    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameStatusText by remember { mutableStateOf<String?>(null) }
    var usernameStatusIsValid by remember { mutableStateOf(false) }
    var usernameStatusIsError by remember { mutableStateOf(false) }
    var showUsernameConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cleanUsername, baselineUsername, canChangeUsername) {
        if (!canChangeUsername || !isUsernameEdited) {
            isCheckingUsername = false
            usernameStatusText = null
            usernameStatusIsValid = false
            usernameStatusIsError = false
            return@LaunchedEffect
        }
        if (!isUsernameFormatValid) {
            isCheckingUsername = false
            usernameStatusText = "Use 3-20 letters, numbers, dots or underscores"
            usernameStatusIsValid = false
            usernameStatusIsError = true
            return@LaunchedEffect
        }
        isCheckingUsername = true
        usernameStatusText = "Checking availability..."
        usernameStatusIsValid = false
        usernameStatusIsError = false
        delay(400L)

        val result = viewModel?.checkUsernameAvailability(context, cleanUsername)
        isCheckingUsername = false
        result?.onSuccess { resp ->
            when (resp.reason) {
                "ok" -> {
                    usernameStatusText = if (resp.available) "Available" else "Already taken"
                    usernameStatusIsValid = resp.available
                    usernameStatusIsError = !resp.available
                }
                "invalid_format" -> {
                    usernameStatusText = "Use 3-20 letters, numbers, dots or underscores"
                    usernameStatusIsValid = false
                    usernameStatusIsError = true
                }
                "reserved" -> {
                    usernameStatusText = "This name is not allowed"
                    usernameStatusIsValid = false
                    usernameStatusIsError = true
                }
                "taken" -> {
                    usernameStatusText = "Already taken"
                    usernameStatusIsValid = false
                    usernameStatusIsError = true
                }
                "recently_used" -> {
                    usernameStatusText = "This name was used recently by someone else"
                    usernameStatusIsValid = false
                    usernameStatusIsError = true
                }
                "same" -> {
                    usernameStatusText = "This is your current username"
                    usernameStatusIsValid = false
                    usernameStatusIsError = false
                }
                else -> {
                    usernameStatusText = if (resp.available) "Available" else "Unavailable"
                    usernameStatusIsValid = resp.available
                    usernameStatusIsError = !resp.available
                }
            }
        }?.onFailure { err ->
            usernameStatusText = err.message ?: "Could not verify username"
            usernameStatusIsValid = false
            usernameStatusIsError = true
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

    val canSaveForm = !isSaving && (!isUsernameEdited || (canChangeUsername && !isCheckingUsername && usernameStatusIsValid))

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

    fun launchAvatarPickerIfAllowed() {
        if (!canChangeAvatar) {
            val locked = formatLockedCaption(avatarLimit) ?: "You've reached the profile photo limit."
            errorMessage = "You've reached the limit for profile photo. $locked"
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            return
        }
        avatarPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun launchCoverPickerIfAllowed() {
        if (!canChangeCover) {
            val locked = formatLockedCaption(coverLimit) ?: "You've reached the cover photo limit."
            errorMessage = "You've reached the limit for cover photo. $locked"
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            return
        }
        coverPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Profile Commit to Supabase & Local Cache
    fun performSave() {
        if (isSaving) return
        errorMessage = null
        isSaving = true
        coroutineScope.launch {
            try {
                val nativeCsv = nativeLanguages.take(3).joinToString(",")
                val learningCsv = learningLanguages.take(3).joinToString(",")

                if (viewModel != null) {
                    var usernameNextDate: String? = null
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
                        loadedSnapshot = loadedSnapshot,
                        onUsernameNextChange = { nextAt ->
                            usernameNextDate = nextAt
                        },
                        onError = { err ->
                            errorMessage = err
                            isSaving = false
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        },
                        onSuccess = {
                            isSaving = false
                            val msg = if (!usernameNextDate.isNullOrBlank()) {
                                "Username updated! Next change possible on ${viewModel.formatIsoToLocalDate(usernameNextDate)}"
                            } else {
                                "Profile updated successfully!"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            onDismiss()
                        }
                    )
                } else {
                    isSaving = false
                    Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Something went wrong. Try again"
                Log.e("EditProfileScreen", "Error saving profile", e)
                isSaving = false
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onSaveClicked() {
        if (isSaving) return
        if (cleanUsername.isBlank() || !isUsernameFormatValid) {
            errorMessage = "Use 3-20 letters, numbers, dots or underscores"
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            return
        }
        if (editDisplayName.trim().isBlank()) {
            errorMessage = "Display name cannot be blank"
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            return
        }
        if (editAge < 15) {
            errorMessage = "Minimum age requirement is 15 years"
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            return
        }
        if (isUsernameEdited) {
            if (!canChangeUsername) {
                errorMessage = formatLockedCaption(usernameLimit) ?: "You've reached the limit for username."
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                return
            }
            if (isCheckingUsername || !usernameStatusIsValid) {
                return
            }
            showUsernameConfirmDialog = true
            return
        }
        performSave()
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(funkyColors.soft)
                        .testTag("edit_profile_back_button")
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (canSaveForm) funkyColors.mango else funkyColors.line)
                        .testTag("edit_profile_save_button")
                        .clickable(enabled = canSaveForm) { onSaveClicked() },
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
                            tint = if (canSaveForm) funkyColors.onMango else funkyColors.mute,
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
                AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("edit_profile_error_banner")
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFDC2626)
                        )
                    }
                }

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
                            .testTag("edit_profile_cover_button")
                            .clickable { launchCoverPickerIfAllowed() }
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
                                        text = if (canChangeCover) "Add Profile Banner" else "Banner locked",
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
                                .defaultMinSize(minHeight = 36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = if (canChangeCover) 0.55f else 0.75f))
                                .clickable { launchCoverPickerIfAllowed() }
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
                                    text = if (canChangeCover) "Banner" else "Locked",
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
                                .testTag("edit_profile_avatar_button")
                                .clickable { launchAvatarPickerIfAllowed() }
                        )

                        // Camera overlay badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(if (canChangeAvatar) funkyColors.ink else funkyColors.mute)
                                .border(2.dp, funkyColors.surface, CircleShape)
                                .clickable { launchAvatarPickerIfAllowed() },
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

                val avatarLockedMsg = formatLockedCaption(avatarLimit)
                val coverLockedMsg = formatLockedCaption(coverLimit)
                if (avatarLockedMsg != null || coverLockedMsg != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (avatarLockedMsg != null) {
                            Text(
                                text = "Profile photo: $avatarLockedMsg",
                                fontSize = 12.sp,
                                color = funkyColors.mute
                            )
                        }
                        if (coverLockedMsg != null) {
                            Text(
                                text = "Cover photo: $coverLockedMsg",
                                fontSize = 12.sp,
                                color = funkyColors.mute
                            )
                        }
                    }
                }

                // Display Name Field Card
                val displayNameLockedMsg = formatLockedCaption(displayNameLimit)
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
                            .background(if (canChangeDisplayName) funkyColors.surface else funkyColors.soft)
                            .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = editDisplayName,
                            onValueChange = { if (canChangeDisplayName) editDisplayName = it },
                            enabled = canChangeDisplayName,
                            readOnly = !canChangeDisplayName,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (canChangeDisplayName) funkyColors.ink else funkyColors.mute
                            ),
                            cursorBrush = SolidColor(funkyColors.mango),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_profile_display_name_input")
                        )
                    }
                    if (displayNameLockedMsg != null) {
                        Text(
                            text = displayNameLockedMsg,
                            fontSize = 12.sp,
                            color = funkyColors.mute,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Username Field Card with 3-20 Counter, Rules, Cooldown & Live Availability
                val usernameLockedMsg = formatLockedCaption(usernameLimit)
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
                            text = "${editUsername.length}/20",
                            fontSize = 12.sp,
                            color = funkyColors.mute
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (canChangeUsername) funkyColors.surface else funkyColors.soft)
                            .border(
                                width = 1.dp,
                                color = when {
                                    usernameStatusIsError && isUsernameEdited -> Color(0xFFEF4444)
                                    usernameStatusIsValid && isUsernameEdited -> Color(0xFF10B981)
                                    else -> funkyColors.line
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "@",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = funkyColors.mute
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            BasicTextField(
                                value = editUsername,
                                onValueChange = { input ->
                                    if (canChangeUsername) {
                                        editUsername = input.lowercase().trimStart().take(20)
                                    }
                                },
                                enabled = canChangeUsername,
                                readOnly = !canChangeUsername,
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (canChangeUsername) funkyColors.ink else funkyColors.mute
                                ),
                                cursorBrush = SolidColor(funkyColors.mango),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_profile_username_input")
                            )
                            if (isCheckingUsername) {
                                CircularProgressIndicator(
                                    color = funkyColors.mango,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (!usernameStatusText.isNullOrBlank()) {
                        Text(
                            text = usernameStatusText.orEmpty(),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                usernameStatusIsValid -> Color(0xFF10B981)
                                usernameStatusIsError -> Color(0xFFEF4444)
                                else -> funkyColors.mute
                            },
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .testTag("edit_profile_username_status")
                        )
                    }

                    if (usernameLockedMsg != null) {
                        Text(
                            text = usernameLockedMsg,
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Text(
                        text = usernameCooldownCaption,
                        fontSize = 12.sp,
                        color = funkyColors.mute,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "3-20 lowercase letters a-z, digits, dot or underscore (no dot at start/end or ..).",
                        fontSize = 11.5.sp,
                        color = funkyColors.mute,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bio Field Card with 150 Counter
                val bioLockedMsg = formatLockedCaption(bioLimit)
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
                            .background(if (canChangeBio) funkyColors.surface else funkyColors.soft)
                            .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        BasicTextField(
                            value = editBio,
                            onValueChange = {
                                if (canChangeBio && it.length <= 150) editBio = it
                            },
                            enabled = canChangeBio,
                            readOnly = !canChangeBio,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = if (canChangeBio) funkyColors.ink else funkyColors.mute,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(funkyColors.mango),
                            decorationBox = { innerTextField ->
                                if (editBio.isEmpty()) {
                                    Text(
                                        text = "Tell people about yourself...",
                                        fontSize = 14.sp,
                                        color = funkyColors.mute
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_profile_bio_input")
                        )
                    }
                    if (bioLockedMsg != null) {
                        Text(
                            text = bioLockedMsg,
                            fontSize = 12.sp,
                            color = funkyColors.mute,
                            modifier = Modifier.padding(top = 6.dp)
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
                val ageLockedMsg = formatLockedCaption(ageLimit)
                val genderLockedMsg = formatLockedCaption(genderLimit)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Age Card
                    Column(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (canChangeAge) funkyColors.surface else funkyColors.soft)
                                .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                                .testTag("edit_profile_age_card")
                                .clickable(enabled = canChangeAge) { showAgeSheet = true }
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
                                        color = if (canChangeAge) funkyColors.ink else funkyColors.mute
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
                        if (ageLockedMsg != null) {
                            Text(
                                text = ageLockedMsg,
                                fontSize = 11.5.sp,
                                color = funkyColors.mute,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Gender Card
                    Column(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (canChangeGender) funkyColors.surface else funkyColors.soft)
                                .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                                .testTag("edit_profile_gender_card")
                                .clickable(enabled = canChangeGender) { showGenderSheet = true }
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
                                        color = if (canChangeGender) funkyColors.ink else funkyColors.mute
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
                        if (genderLockedMsg != null) {
                            Text(
                                text = genderLockedMsg,
                                fontSize = 11.5.sp,
                                color = funkyColors.mute,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2x2 Grid: Country & City (Row 2)
                val countryLockedMsg = formatLockedCaption(countryLimit)
                val cityLockedMsg = formatLockedCaption(cityLimit)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Country Card
                    Column(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (canChangeCountry) funkyColors.surface else funkyColors.soft)
                                .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                                .testTag("edit_profile_country_card")
                                .clickable(enabled = canChangeCountry) { showCountrySheet = true }
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
                                        color = if (canChangeCountry) funkyColors.ink else funkyColors.mute,
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
                        if (countryLockedMsg != null) {
                            Text(
                                text = countryLockedMsg,
                                fontSize = 11.5.sp,
                                color = funkyColors.mute,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // City Card
                    Column(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (canChangeCity) funkyColors.surface else funkyColors.soft)
                                .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                                .testTag("edit_profile_city_card")
                                .clickable(enabled = canChangeCity) { showCitySheet = true }
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
                                        text = editCity.ifBlank { "Select city" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canChangeCity && editCity.isNotBlank()) funkyColors.ink else funkyColors.mute,
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
                        if (cityLockedMsg != null) {
                            Text(
                                text = cityLockedMsg,
                                fontSize = 11.5.sp,
                                color = funkyColors.mute,
                                modifier = Modifier.padding(top = 4.dp)
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
                            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("edit_profile_edit_native_languages_button")
                            .clickable { showNativeLanguagesSheet = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
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
                            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("edit_profile_edit_learning_languages_button")
                            .clickable { showLearningLanguagesSheet = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
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
                            .defaultMinSize(minHeight = 48.dp)
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
                            .testTag("edit_profile_add_interest_button")
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
                if (canChangeCity) {
                    // Update city defaults for selected country only when city is editable
                    val popular = PlaceDbService.getPopularCities(code)
                    if (popular.isNotEmpty()) {
                        editCity = popular.first().name
                    }
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
                if (canChangeCountry) {
                    showCountrySheet = true
                }
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

    // Confirmation Dialog for Username Change
    if (showUsernameConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUsernameConfirmDialog = false },
            containerColor = funkyColors.surface,
            titleContentColor = funkyColors.ink,
            textContentColor = funkyColors.mute,
            title = {
                Text(
                    text = "Change username?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = funkyColors.ink
                )
            },
            text = {
                Text(
                    text = "You can change your username only once every $usernameWindowDays days. Change to @$cleanUsername?",
                    fontSize = 14.sp,
                    color = funkyColors.mute,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUsernameConfirmDialog = false
                        performSave()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = funkyColors.mango,
                        contentColor = funkyColors.onMango
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("confirm_username_change_button")
                ) {
                    Text(
                        text = "Change",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUsernameConfirmDialog = false },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("cancel_username_change_button")
                ) {
                    Text(
                        text = "Cancel",
                        color = funkyColors.ink,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}
