package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.AuthViewModel
import com.example.SupabaseClient
import java.util.Calendar

// Theme Accents
private val StepGold = Color(0xFFF6C343)
private val StepLightGold = Color(0xFFFFFBEA)

private val AvatarPresets = listOf(
    Pair(com.example.R.drawable.male_avatar_1781324792347, "avatar_male_1"),
    Pair(com.example.R.drawable.female_avatar_1781324805233, "avatar_female_1"),
    Pair(com.example.R.drawable.cool_avatar_1781324820431, "avatar_cool_1")
)

@Composable
fun CleanStepIndicator(
    currentStep: Int,
    totalSteps: Int = 2,
    title: String,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }

                Surface(
                    color = StepLightGold,
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, StepGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Step $currentStep of $totalSteps • $title",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.size(44.dp))
            }

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFFF1F5F9))
            ) {
                val fraction = currentStep.toFloat() / totalSteps.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(StepGold)
                )
            }
        }
    }
}

// ============================================================================
// REDESIGNED ONBOARDING STEP 1: QUICK PROFILE CARD (Compliant & Fast)
// Includes: Avatar, Full Name, Live Unique Username, Gender, DOB (Age 13+ Check)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileStep1QuickCard(
    viewModel: AuthViewModel,
    initialDisplayName: String = "",
    initialUsername: String = "",
    initialAvatar: String = "",
    initialGender: String = "",
    initialDob: String = "",
    onNext: (displayName: String, username: String, avatar: String, gender: String, dob: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var username by remember { mutableStateOf(initialUsername) }
    var selectedGender by remember { mutableStateOf(if (initialGender.isNotBlank()) initialGender else "Prefer not to say") }
    var dob by remember { mutableStateOf(initialDob) }
    var age by remember { mutableStateOf<Int?>(null) }
    var isUnderage by remember { mutableStateOf(false) }

    // Avatar state
    var customAvatarUri by remember { mutableStateOf(if (initialAvatar.startsWith("content://") || initialAvatar.startsWith("http")) initialAvatar else "") }
    var selectedAvatarIndex by remember { mutableStateOf(0) }

    // Username Live Validation state
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }

    // Photo picker launcher
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            customAvatarUri = uri.toString()
        }
    }

    // Debounced Username check
    LaunchedEffect(username) {
        val clean = username.trim().lowercase()
        if (clean.length < 3) {
            usernameError = if (clean.isNotEmpty()) "Must be at least 3 characters" else null
            isUsernameAvailable = null
            return@LaunchedEffect
        }
        if (!clean.matches(Regex("^[a-z0-9_.]+$"))) {
            usernameError = "Letters, numbers, '.', '_' only"
            isUsernameAvailable = null
            return@LaunchedEffect
        }

        isCheckingUsername = true
        usernameError = null
        kotlinx.coroutines.delay(400) // 400ms debounce
        
        try {
            val res = SupabaseClient.api.getUserByUsername(usernameEq = "eq.$clean")
            val users = res.body()
            if (res.isSuccessful && !users.isNullOrEmpty()) {
                usernameError = "Username is already taken"
                isUsernameAvailable = false
            } else {
                usernameError = null
                isUsernameAvailable = true
            }
        } catch (e: Exception) {
            isUsernameAvailable = true // fallback
        } finally {
            isCheckingUsername = false
        }
    }

    // Date Picker Dialog Logic
    val nowCalendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year: Int, month: Int, dayOfMonth: Int ->
                val selectedCal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                val today = Calendar.getInstance()
                var calculatedAge = today.get(Calendar.YEAR) - selectedCal.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < selectedCal.get(Calendar.DAY_OF_YEAR)) {
                    calculatedAge--
                }
                age = calculatedAge
                dob = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                isUnderage = calculatedAge < 13
            },
            nowCalendar.get(Calendar.YEAR) - 18,
            nowCalendar.get(Calendar.MONTH),
            nowCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Scaffold(
        topBar = {
            CleanStepIndicator(
                currentStep = 1,
                totalSteps = 2,
                title = "Quick Card",
                onBackClick = onBack
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Create Your Profile",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF111827),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Basic info to set up your account securely",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // AVATAR SELECTOR ROW
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(3.dp, StepGold, CircleShape)
                                .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (customAvatarUri.isNotBlank()) {
                                AsyncImage(
                                    model = customAvatarUri,
                                    contentDescription = "Avatar Preview",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AsyncImage(
                                    model = AvatarPresets[selectedAvatarIndex].first,
                                    contentDescription = "Preset Avatar",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Preset Avatars Selector
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarPresets.forEachIndexed { index, preset ->
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (customAvatarUri.isBlank() && selectedAvatarIndex == index) 2.dp else 1.dp,
                                            color = if (customAvatarUri.isBlank() && selectedAvatarIndex == index) StepGold else Color(0xFFE5E7EB),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            customAvatarUri = ""
                                            selectedAvatarIndex = index
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = preset.first,
                                        contentDescription = "Preset $index",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            // Upload custom image chip
                            OutlinedButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // FULL NAME INPUT
                item {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name *") },
                        placeholder = { Text("e.g. Alex Johnson") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_fullname_input")
                    )
                }

                // USERNAME INPUT (LIVE UNIQUENESS VALIDATION)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it.trim().lowercase() },
                            label = { Text("Username *") },
                            placeholder = { Text("unique_handle") },
                            singleLine = true,
                            isError = usernameError != null,
                            trailingIcon = {
                                if (isCheckingUsername) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = StepGold)
                                } else if (isUsernameAvailable == true) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = Color(0xFF10B981))
                                } else if (usernameError != null) {
                                    Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_username_input")
                        )
                        if (usernameError != null) {
                            Text(
                                text = usernameError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                            )
                        } else if (isUsernameAvailable == true) {
                            Text(
                                text = "Username is available!",
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                            )
                        }
                    }
                }

                // GENDER SELECTION PILLS
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Gender",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Male", "Female", "Other").forEach { genderOption ->
                                val isSelected = selectedGender.equals(genderOption, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedGender = genderOption },
                                    label = { Text(genderOption, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StepGold,
                                        selectedLabelColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // DATE OF BIRTH & AGE 13+ COMPLIANCE CHECK
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Date of Birth * (Must be 13+)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        OutlinedCard(
                            onClick = { datePickerDialog.show() },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isUnderage) MaterialTheme.colorScheme.error else Color(0xFFD1D5DB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF6B7280))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (dob.isNotBlank()) "$dob ${if (age != null) "($age yrs)" else ""}" else "Select your birth date",
                                        fontSize = 15.sp,
                                        color = if (dob.isNotBlank()) Color.Black else Color(0xFF9CA3AF)
                                    )
                                }
                                Text("Change", color = StepGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Underage Compliance Warning Banner
                        if (isUnderage) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "FunkyTalk requires users to be at least 13 years old for safety and privacy compliance.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM ACTION BUTTON
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Button(
                    onClick = {
                        if (displayName.trim().isBlank()) {
                            Toast.makeText(context, "Please enter your full name.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (username.trim().isBlank() || usernameError != null) {
                            Toast.makeText(context, "Please choose a valid unique username.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (dob.isBlank()) {
                            Toast.makeText(context, "Please select your date of birth.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isUnderage) {
                            Toast.makeText(context, "You must be at least 13 years old to use FunkyTalk.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val chosenAvatar = if (customAvatarUri.isNotBlank()) {
                            customAvatarUri
                        } else {
                            AvatarPresets[selectedAvatarIndex].second
                        }

                        onNext(displayName.trim(), username.trim(), chosenAvatar, selectedGender, dob)
                    },
                    enabled = !isUnderage && usernameError == null && displayName.isNotBlank() && dob.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = StepGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(56.dp)
                        .testTag("step1_continue_button")
                ) {
                    Text("Continue to Personalize", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

// ============================================================================
// REDESIGNED ONBOARDING STEP 2: PERSONALIZE (Languages + Country + Bio + Hobbies)
// All optional with smooth chips & "Skip for now" support
// ============================================================================
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileStep2Personalize(
    initialNativeLanguage: String = "English",
    initialLearningLanguage: String = "Spanish",
    initialCountry: String = "United States",
    initialCountryCode: String = "us",
    initialHobbies: List<String> = emptyList(),
    onComplete: (nativeLang: String, learningLang: String, country: String, countryCode: String, hobbies: List<String>) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedNativeLang by remember { mutableStateOf(if (initialNativeLanguage.isNotBlank()) initialNativeLanguage else "English") }
    var selectedLearningLang by remember { mutableStateOf(if (initialLearningLanguage.isNotBlank()) initialLearningLanguage else "Spanish") }
    var selectedCountry by remember { mutableStateOf(if (initialCountry.isNotBlank()) initialCountry else "United States") }
    var selectedCountryCode by remember { mutableStateOf(if (initialCountryCode.isNotBlank()) initialCountryCode else "us") }
    
    val selectedHobbies = remember { mutableStateListOf<String>().apply { addAll(initialHobbies.take(3)) } }
    var showCountryDialog by remember { mutableStateOf(false) }
    var showNativeLangDialog by remember { mutableStateOf(false) }
    var showLearningLangDialog by remember { mutableStateOf(false) }

    val quickHobbies = listOf("Music", "Travel", "Gaming", "Fitness", "Movies", "Tech", "Cooking", "Books", "Art")

    Scaffold(
        topBar = {
            CleanStepIndicator(
                currentStep = 2,
                totalSteps = 2,
                title = "Personalize",
                onBackClick = onBack
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Customize Your Experience",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "Help us match you with rooms and language partners.",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // LANGUAGES ROW SELECTION
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Language Preferences", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF374151))
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Native Language Card
                            OutlinedCard(
                                onClick = { showNativeLangDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Native Language", fontSize = 12.sp, color = Color(0xFF6B7280))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(selectedNativeLang, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                                }
                            }

                            // Learning Language Card
                            OutlinedCard(
                                onClick = { showLearningLangDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Learning Language", fontSize = 12.sp, color = Color(0xFF6B7280))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(selectedLearningLang, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                                }
                            }
                        }
                    }
                }

                // COUNTRY SELECTION
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Country / Region", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF374151))
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        OutlinedCard(
                            onClick = { showCountryDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Public, contentDescription = null, tint = StepGold)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(selectedCountry, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                }
                                Text("Change", color = StepGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // TOP 3 HOBBIES CHIPS (Interactive Chip Selection)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Top 3 Hobbies", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF374151))
                            Text("${selectedHobbies.size}/3 selected", fontSize = 12.sp, color = Color(0xFF6B7280))
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            quickHobbies.forEach { hobby ->
                                val isSelected = selectedHobbies.contains(hobby)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            selectedHobbies.remove(hobby)
                                        } else if (selectedHobbies.size < 3) {
                                            selectedHobbies.add(hobby)
                                        } else {
                                            Toast.makeText(context, "You can select up to 3 hobbies.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    label = { Text(hobby, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StepLightGold,
                                        selectedLabelColor = Color(0xFF92400E)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) StepGold else Color(0xFFE5E7EB)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // BOTTOM ACTIONS (Complete & Skip for now)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onComplete(selectedNativeLang, selectedLearningLang, selectedCountry, selectedCountryCode, selectedHobbies.toList())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StepGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("step2_finish_button")
                    ) {
                        Text("Complete & Explore FunkyTalk", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.testTag("step2_skip_button")
                    ) {
                        Text("Skip for now", color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // Modal Sheet / Dialogs for Languages and Countries selection
    if (showCountryDialog) {
        AlertDialog(
            onDismissRequest = { showCountryDialog = false },
            title = { Text("Select Country") },
            text = {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(CountryData.allCountries) { country ->
                        Text(
                            text = country.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCountry = country.name
                                    selectedCountryCode = country.flagCode
                                    showCountryDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            fontSize = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountryDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showNativeLangDialog) {
        AlertDialog(
            onDismissRequest = { showNativeLangDialog = false },
            title = { Text("Select Native Language") },
            text = {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(LanguageData.popularLanguages) { lang ->
                        Text(
                            text = "${lang.name} (${lang.nativeName})",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedNativeLang = lang.name
                                    showNativeLangDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            fontSize = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNativeLangDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showLearningLangDialog) {
        AlertDialog(
            onDismissRequest = { showLearningLangDialog = false },
            title = { Text("Select Learning Language") },
            text = {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(LanguageData.popularLanguages) { lang ->
                        Text(
                            text = "${lang.name} (${lang.nativeName})",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLearningLang = lang.name
                                    showLearningLangDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            fontSize = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLearningLangDialog = false }) { Text("Cancel") }
            }
        )
    }
}
