package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AuthState
import com.example.AuthViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.SvgDecoder

// --- CORE UTILITY DIALOGUE COLORS ---
val GoldAccent = Color(0xFFFFC529)
val LightGoldBg = Color(0xFFFFFBEA)
private val CardBorderColor = Color(0xFFE2E8F0)
private val SelectedCardBorder = Color(0xFFFFC529)

// Predefined fun avatars to make profile customization extremely interactive!
private val AvailableAvatars = listOf(
    Pair(com.example.R.drawable.male_avatar_1781324792347, "avatar_male_1"),
    Pair(com.example.R.drawable.female_avatar_1781324805233, "avatar_female_1"),
    Pair(com.example.R.drawable.cool_avatar_1781324820431, "avatar_cool_1")
)

@Composable
fun OnboardingStepIndicator(
    currentStep: Int,
    totalSteps: Int,
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
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back instruction arrow",
                        tint = Color.Black
                    )
                }

                // Centered step status pill
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(100.dp),
                ) {
                    Text(
                        text = "Step $currentStep of $totalSteps",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Anchor spacer to correctly balance the row alignment
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Beautiful progressive golden screen line bar
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
                        .background(GoldAccent)
                )
            }
        }
    }
}

// ==================== STEP 1: CREATE PROFILE ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingProfileScreen(
    viewModel: AuthViewModel,
    initialUsername: String = "",
    initialDisplayName: String = "",
    initialAvatar: String = "",
    onNext: (username: String, displayName: String, avatar: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()

    var username by remember { mutableStateOf(initialUsername) }
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var isCheckingUsername by remember { mutableStateOf(false) }

    val hasInitialCustomAvatar = remember(initialAvatar) {
        initialAvatar.isNotEmpty() && !AvailableAvatars.any { it.second == initialAvatar }
    }
    var useCustomAvatar by remember { mutableStateOf(hasInitialCustomAvatar) }
    var customAvatarPath by remember { mutableStateOf(if (useCustomAvatar) initialAvatar else "") }
    var selectedAvatarIndex by remember {
        mutableStateOf(
            if (initialAvatar.isNotEmpty()) {
                val index = AvailableAvatars.indexOfFirst { it.second == initialAvatar }
                if (index == -1) 0 else index
            } else 0
        )
    }
    var isShowingAvatarGrid by remember { mutableStateOf(false) }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "custom_avatar_${System.currentTimeMillis()}.jpg")
                val outputStream = java.io.FileOutputStream(file)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                customAvatarPath = file.absolutePath
                useCustomAvatar = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val userEmail = remember(authState) {
        (authState as? AuthState.Success)?.user?.email ?: ""
    }

    Scaffold(
        topBar = {
            OnboardingStepIndicator(
                currentStep = 1,
                totalSteps = 7,
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Title header matching visual screenshots precisely
                    Text(
                        text = "Create Your Profile",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        lineHeight = 38.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tell others who you are",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }

                // PHOTO / AVATAR SELECT CONTAINER
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Main avatar circular box
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .drawBehind {
                                    drawCircle(
                                        color = GoldAccent.copy(alpha = 0.2f),
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                                floatArrayOf(12f, 12f),
                                                0f
                                            )
                                        )
                                    )
                                }
                                .clickable { isShowingAvatarGrid = !isShowingAvatarGrid },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(LightGoldBg),
                                contentAlignment = Alignment.Center
                            ) {
                                if (useCustomAvatar && customAvatarPath.isNotEmpty()) {
                                    AsyncImage(
                                        model = customAvatarPath,
                                        contentDescription = "Custom picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = AvailableAvatars[selectedAvatarIndex].first),
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            }
                        }

                        // Overlapping Yellow Camera Badge Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = (-8).dp, y = (-8).dp)
                                .clip(CircleShape)
                                .background(GoldAccent)
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Choose custom picture",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add Photo / Tap to cycle avatars",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable {
                            isShowingAvatarGrid = !isShowingAvatarGrid
                        }
                    )
                }

                // Avatar Horizontal Selection Panel
                if (isShowingAvatarGrid) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Choose your special avatar identity:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    AvailableAvatars.forEachIndexed { idx, avatar ->
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(if (idx == selectedAvatarIndex) GoldAccent.copy(alpha = 0.25f) else Color.Transparent)
                                                .clickable { selectedAvatarIndex = idx }
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.Image(
                                                painter = androidx.compose.ui.res.painterResource(id = avatar.first),
                                                contentDescription = "Avatar Options",
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // INPUT: USERNAME
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Username",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { input ->
                                // Restrict username to letters, numbers, underscore
                                username = input.filter { it.isLetterOrDigit() || it == '_' }
                            },
                            placeholder = { Text("Enter your username") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User info guide icon",
                                    tint = Color.Black.copy(alpha = 0.4f)
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color.Black.copy(alpha = 0.12f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This will be your unique ID on the platform",
                            fontSize = 12.sp,
                            color = Color.Black.copy(alpha = 0.45f)
                        )
                    }
                }

                // INPUT: DISPLAY NAME
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Display Name",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            placeholder = { Text("Enter your display name") },
                            leadingIcon = {
                                Text(
                                    text = "Aa",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(start = 12.dp, end = 2.dp)
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("display_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color.Black.copy(alpha = 0.12f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This is how your name will appear to others",
                            fontSize = 12.sp,
                            color = Color.Black.copy(alpha = 0.45f)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Next Step Action Pill
            Button(
                onClick = {
                    if (username.isBlank() || displayName.isBlank()) {
                        Toast.makeText(context, "Please enter both username and display name.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (username.length < 3) {
                        Toast.makeText(context, "Username must be at least 3 characters.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isCheckingUsername = true
                    val currentUserId = (authState as? AuthState.Success)?.user?.uid
                    viewModel.checkUsernameUnique(username, currentUserId) { isUnique ->
                        isCheckingUsername = false
                        if (isUnique) {
                            onNext(username, displayName, if (useCustomAvatar) customAvatarPath else AvailableAvatars[selectedAvatarIndex].second)
                        } else {
                            Toast.makeText(context, "This username is already taken. Please choose another one.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isCheckingUsername,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .testTag("onboarding_profile_next"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                shape = RoundedCornerShape(100.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isCheckingUsername) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Checking username...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    } else {
                        Text(
                            text = "Next Step",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward step arrow",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


// ==================== STEP 2: AGE SCREEN (DOB LIMIT >= 15) ====================
@Composable
fun OnboardingAgeScreen(
    initialDob: String = "",
    onNext: (dobString: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Wheels for Birth Date selection setup
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val yearsRange = (1950..currentYear).toList().reversed()
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val days = (1..31).toList()

    val parsedYear = remember(initialDob) {
        if (initialDob.isNotEmpty()) {
            initialDob.split("-").getOrNull(0)?.toIntOrNull() ?: 1998
        } else 1998
    }
    val parsedMonthIndex = remember(initialDob) {
        if (initialDob.isNotEmpty()) {
            val m = initialDob.split("-").getOrNull(1)?.toIntOrNull() ?: 5
            (m - 1).coerceIn(0, 11)
        } else 4
    }
    val parsedDay = remember(initialDob) {
        if (initialDob.isNotEmpty()) {
            initialDob.split("-").getOrNull(2)?.toIntOrNull() ?: 15
        } else 15
    }

    var selectedYear by remember { mutableStateOf(parsedYear) }
    var selectedMonthIndex by remember { mutableStateOf(parsedMonthIndex) }
    var selectedDay by remember { mutableStateOf(parsedDay) }

    val listStateMonth = rememberLazyListState(initialFirstVisibleItemIndex = selectedMonthIndex)
    val listStateDay = rememberLazyListState(initialFirstVisibleItemIndex = days.indexOf(selectedDay).coerceAtLeast(0))
    val listStateYear = rememberLazyListState(initialFirstVisibleItemIndex = yearsRange.indexOf(selectedYear).coerceAtLeast(0))

    LaunchedEffect(listStateMonth.firstVisibleItemIndex) {
        if (listStateMonth.isScrollInProgress) {
            val index = listStateMonth.firstVisibleItemIndex
            if (index in months.indices) {
                selectedMonthIndex = index
            }
        }
    }
    LaunchedEffect(listStateDay.firstVisibleItemIndex) {
        if (listStateDay.isScrollInProgress) {
            val index = listStateDay.firstVisibleItemIndex
            if (index in days.indices) {
                selectedDay = days[index]
            }
        }
    }
    LaunchedEffect(listStateYear.firstVisibleItemIndex) {
        if (listStateYear.isScrollInProgress) {
            val index = listStateYear.firstVisibleItemIndex
            if (index in yearsRange.indices) {
                selectedYear = yearsRange[index]
            }
        }
    }

    // Calculated age indicator check
    val calculatedAge = remember(selectedYear, selectedMonthIndex, selectedDay) {
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - selectedYear
        // month index starts at 0 for Calendar
        if (today.get(Calendar.MONTH) < selectedMonthIndex ||
            (today.get(Calendar.MONTH) == selectedMonthIndex && today.get(Calendar.DAY_OF_MONTH) < selectedDay)) {
            age--
        }
        age
    }

    val isAgeValid = calculatedAge >= 15

    Scaffold(
        topBar = {
            OnboardingStepIndicator(
                currentStep = 2,
                totalSteps = 7,
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Birthday cake visual badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(LightGoldBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title header
                    Text(
                        text = "What's Your Age?",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        lineHeight = 38.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This helps us personalize your experience",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }

                // WHEEL SELECTOR CONTROLLER
                item {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Horizontal columns representing Birth Year, Month, Day selectors
                    // This mimics the gorgeous wheel design showing multiple adjacent options
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                            .background(Color(0xFFFAFAFA))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // COLUMN 1: MONTH (Drop down / Simple Selection Wheel representation)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "MONTH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Box(
                                modifier = Modifier
                                    .height(140.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                // Background highlight band
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GoldAccent.copy(alpha = 0.18f))
                                )
                                
                                LazyColumn(
                                    state = listStateMonth,
                                    contentPadding = PaddingValues(vertical = 50.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    items(months.size) { index ->
                                        val month = months[index]
                                        val isSelected = index == selectedMonthIndex
                                        Text(
                                            text = month,
                                            fontSize = if (isSelected) 18.sp else 14.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                            color = if (isSelected) Color.Black else Color.Black.copy(alpha = 0.4f),
                                            modifier = Modifier
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    selectedMonthIndex = index 
                                                    scope.launch {
                                                        listStateMonth.animateScrollToItem(index)
                                                    }
                                                },
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // COLUMN 2: DAY
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "DAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Box(
                                modifier = Modifier
                                    .height(140.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GoldAccent.copy(alpha = 0.18f))
                                )
                                
                                LazyColumn(
                                    state = listStateDay,
                                    contentPadding = PaddingValues(vertical = 50.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    items(days.size) { index ->
                                        val day = days[index]
                                        val isSelected = day == selectedDay
                                        Text(
                                            text = day.toString(),
                                            fontSize = if (isSelected) 18.sp else 14.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                            color = if (isSelected) Color.Black else Color.Black.copy(alpha = 0.4f),
                                            modifier = Modifier
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    selectedDay = day 
                                                    scope.launch {
                                                        listStateDay.animateScrollToItem(index)
                                                    }
                                                },
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // COLUMN 3: YEAR
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text(text = "YEAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Box(
                                modifier = Modifier
                                    .height(140.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GoldAccent.copy(alpha = 0.18f))
                                )
                                
                                LazyColumn(
                                    state = listStateYear,
                                    contentPadding = PaddingValues(vertical = 50.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    items(yearsRange.size) { index ->
                                        val year = yearsRange[index]
                                        val isSelected = year == selectedYear
                                        Text(
                                            text = year.toString(),
                                            fontSize = if (isSelected) 18.sp else 14.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                            color = if (isSelected) Color.Black else Color.Black.copy(alpha = 0.4f),
                                            modifier = Modifier
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    selectedYear = year 
                                                    scope.launch {
                                                        listStateYear.animateScrollToItem(index)
                                                    }
                                                },
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected DOB indicator card with calculations
                item {
                    val dobText = "${months[selectedMonthIndex]} $selectedDay, $selectedYear"
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAgeValid) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = CardBorderColor.let {
                            CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(
                                    listOf(
                                        if (isAgeValid) Color(0xFF4CAF50).copy(alpha = 0.25f) else Color.Red.copy(alpha = 0.25f),
                                        if (isAgeValid) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f)
                                    )
                                )
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Selected Date of Birth:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAgeValid) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                                Text(
                                    text = dobText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }

                            Surface(
                                color = if (isAgeValid) Color(0xFF4CAF50) else Color(0xFFE53935),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(
                                    text = "Age: $calculatedAge ${if (isAgeValid) "✓" else "✕"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Minimum age warning requirement text
                if (!isAgeValid) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ You must be at least 15 years old to join FunkyTalk.",
                                color = Color(0xFFBE123C),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // LOCK Privacy text details from screenshot
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Public visibility icon",
                            tint = Color.Black.copy(alpha = 0.35f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Your age and display data will be visible on your profile",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black.copy(alpha = 0.45f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Bottom Continuous validation Button
            Button(
                onClick = {
                    if (!isAgeValid) {
                        Toast.makeText(context, "You must be at least 15 years old to proceed.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val dobStr = "$selectedYear-${selectedMonthIndex + 1}-$selectedDay"
                    onNext(dobStr)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .testTag("onboarding_age_continue"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                enabled = isAgeValid,
                shape = RoundedCornerShape(100.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward step arrow",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


// ==================== STEP 3: GENDER SCREEN ====================
@Composable
fun OnboardingGenderScreen(
    initialGender: String = "",
    onComplete: (gender: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedGender by remember { mutableStateOf(initialGender) }

    Scaffold(
        topBar = {
            OnboardingStepIndicator(
                currentStep = 3,
                totalSteps = 7,
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Gender circle visual element banner
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(LightGoldBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title header matching visual screenshots precisely
                    Text(
                        text = "What's Your Gender?",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        lineHeight = 38.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This helps us personalize your experience",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }

                // GENDER SELECT CARDS (Side by Side in a beautiful row setup!)
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card 1: Male representation
                        val isMaleSelected = selectedGender == "Male"
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(210.dp)
                                .clickable { selectedGender = "Male" }
                                .testTag("gender_male"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMaleSelected) Color(0xFFEFF6FF) else Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isMaleSelected) Color(0xFF3B82F6) else Color.Black.copy(alpha = 0.08f)
                                )
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isMaleSelected) 2.dp else 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp).padding(top = 8.dp),
                                    tint = Color(0xFF3B82F6)
                                )

                                Text(
                                    text = "Male",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )

                                // Custom radio button indicator
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isMaleSelected) 6.dp else 2.dp,
                                            color = if (isMaleSelected) Color(0xFF3B82F6) else Color.Black.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }

                        // Card 2: Female representation
                        val isFemaleSelected = selectedGender == "Female"
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(210.dp)
                                .clickable { selectedGender = "Female" }
                                .testTag("gender_female"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFemaleSelected) Color(0xFFFDF2F8) else Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isFemaleSelected) Color(0xFFEC4899) else Color.Black.copy(alpha = 0.08f)
                                )
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isFemaleSelected) 2.dp else 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp).padding(top = 8.dp),
                                    tint = Color(0xFFEC4899)
                                )

                                Text(
                                    text = "Female",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )

                                // Custom radio button indicator
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isFemaleSelected) 6.dp else 2.dp,
                                            color = if (isFemaleSelected) Color(0xFFEC4899) else Color.Black.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }

                // PUBLIC PRIVACY TEXT
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Public visibility icon",
                            tint = Color.Black.copy(alpha = 0.35f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Your gender will be visible on your profile",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black.copy(alpha = 0.45f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Bottom Continue complete button registration
            Button(
                onClick = {
                    if (selectedGender.isBlank()) {
                        Toast.makeText(context, "Please select your gender.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onComplete(selectedGender)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .testTag("onboarding_gender_complete"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                enabled = selectedGender.isNotEmpty(),
                shape = RoundedCornerShape(100.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward step arrow",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingCountryScreen(
    initialCountry: String = "",
    initialCountryCode: String = "",
    onComplete: (countryName: String, countryCode: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCountry by remember {
        mutableStateOf<Country?>(
            if (initialCountry.isNotEmpty() && initialCountryCode.isNotEmpty()) {
                Country(initialCountry, initialCountryCode)
            } else {
                Country("India", "in")
            }
        )
    }

    val popular = CountryData.popularCountries
    val filteredAll = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            CountryData.allCountries
        } else {
            CountryData.allCountries.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val groupedCountries = remember(filteredAll) {
        filteredAll.groupBy { it.name.first().uppercaseChar() }.toSortedMap()
    }

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    Scaffold(
        topBar = {
            OnboardingStepIndicator(
                currentStep = 4,
                totalSteps = 7,
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(LightGoldBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(52.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 16.dp, start = 16.dp)
                                    .size(24.dp)
                                    .background(GoldAccent, CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val annotatedTitle = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)) {
                            append("Where are ")
                        }
                        withStyle(style = SpanStyle(color = GoldAccent, fontWeight = FontWeight.Black)) {
                            append("you")
                        }
                        withStyle(style = SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)) {
                            append(" from?")
                        }
                    }
                    Text(
                        text = annotatedTitle,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This helps us connect you with people from your country and nearby.",
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search country...", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_country_field"),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.Gray
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        singleLine = true
                    )
                }

                selectedCountry?.let { country ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, GoldAccent, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = LightGoldBg),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    AsyncImage(
                                        model = "https://hatscripts.github.io/circle-flags/flags/${country.flagCode.lowercase()}.svg",
                                        contentDescription = "${country.name} Flag",
                                        imageLoader = imageLoader,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = country.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(GoldAccent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                if (searchQuery.isBlank()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Popular",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            val popularChunks = popular.chunked(2)
                            popularChunks.forEach { chunk ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    chunk.forEach { country ->
                                        val isSelected = selectedCountry?.flagCode == country.flagCode
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedCountry = country }
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) GoldAccent else Color(0xFFE2E8F0),
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) LightGoldBg else Color.White
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                AsyncImage(
                                                    model = "https://hatscripts.github.io/circle-flags/flags/${country.flagCode.lowercase()}.svg",
                                                    contentDescription = "Flag",
                                                    imageLoader = imageLoader,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text(
                                                    text = country.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = Color.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "All Countries" else "Search Results",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                for ((firstLetter, countries) in groupedCountries) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = firstLetter.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldAccent
                            )
                        }
                    }

                    countries.forEach { country ->
                        item {
                            val isSelected = selectedCountry?.flagCode == country.flagCode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) LightGoldBg else Color.Transparent)
                                    .clickable { selectedCountry = country }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    AsyncImage(
                                        model = "https://hatscripts.github.io/circle-flags/flags/${country.flagCode.lowercase()}.svg",
                                        contentDescription = "Flag",
                                        imageLoader = imageLoader,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = country.name,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.Black
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Select",
                                    tint = if (isSelected) GoldAccent else Color.LightGray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure lock",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Your exact location will never be shared.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val country = selectedCountry
                    if (country == null) {
                        Toast.makeText(context, "Please select your country.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onComplete(country.name, country.flagCode)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .testTag("onboarding_country_complete"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                shape = RoundedCornerShape(100.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward link arrow",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ==================== LANGUAGE ONBOARDING UI ====================
// Define selected language item helper state
data class SelectedLanguageWithLevel(
    val language: Language,
    var level: ProficiencyLevel
)

data class ProficiencyLevel(
    val tag: String,
    val text: String,
    val subtitle: String,
    val badge: String
)

val GenZLevels = listOf(
    ProficiencyLevel("Native / Fluent", "Native / Fluent", "Speak with high precision, naturally and effortlessly.", "C2"),
    ProficiencyLevel("Advanced", "Advanced", "Can hold deep conversations on a wide range of topics.", "C1"),
    ProficiencyLevel("Intermediate", "Intermediate", "Can understand core concepts and respond clearly.", "B2"),
    ProficiencyLevel("Beginner", "Beginner", "Knowing basic vocabulary, greetings and key phrases.", "A1")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingLanguageScreenBase(
    title: String,
    subtitle: String,
    currentStep: Int,
    onComplete: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val selectedLanguages = remember { mutableStateListOf<SelectedLanguageWithLevel>() }
    var isBrowseAllOpen by remember { mutableStateOf(false) }

    // Dialog trigger state
    var languageForLevelSelect by remember { mutableStateOf<Language?>(null) }
    var tempSelectedLevelCode by remember { mutableStateOf("Native / Fluent") }

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    val titleAnnotated = remember(title) {
        buildAnnotatedString {
            val parts = title.split(" ")
            parts.forEachIndexed { index, part ->
                if (part.equals("native", ignoreCase = true) || part.contains("learning", ignoreCase = true)) {
                    withStyle(style = SpanStyle(color = GoldAccent, fontWeight = FontWeight.Bold)) {
                        append(part)
                    }
                } else {
                    append(part)
                }
                if (index < parts.lastIndex) append(" ")
            }
        }
    }

    // Modal dialog to choose level
    if (languageForLevelSelect != null) {
        val editingLang = languageForLevelSelect!!
        AlertDialog(
            onDismissRequest = { languageForLevelSelect = null },
            title = {
                Column {
                    Text(
                        text = "Choose Level for ${editingLang.name}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                    Text(
                        text = "Show off how well you vibe in this language!",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    GenZLevels.forEach { level ->
                        val isSelected = tempSelectedLevelCode == level.tag
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempSelectedLevelCode = level.tag }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) GoldAccent else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) LightGoldBg else Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isSelected) GoldAccent.copy(alpha = 0.2f) else Color(0xFFF1F5F9),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(level.badge, fontSize = 18.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = level.tag,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = level.subtitle,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 6.dp else 2.dp,
                                            color = if (isSelected) GoldAccent else Color.LightGray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val levelObj = GenZLevels.first { it.tag == tempSelectedLevelCode }
                        val existingIndex = selectedLanguages.indexOfFirst { it.language.code == editingLang.code }
                        if (existingIndex >= 0) {
                            selectedLanguages[existingIndex] = SelectedLanguageWithLevel(editingLang, levelObj)
                        } else {
                            if (selectedLanguages.size >= 3) {
                                Toast.makeText(context, "Only up to 3 languages can be selected.", Toast.LENGTH_SHORT).show()
                            } else {
                                selectedLanguages.add(SelectedLanguageWithLevel(editingLang, levelObj))
                            }
                        }
                        languageForLevelSelect = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("Select & Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { languageForLevelSelect = null }) {
                    Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            OnboardingStepIndicator(
                currentStep = currentStep,
                totalSteps = 7,
                onBackClick = {
                    if (isBrowseAllOpen) {
                        isBrowseAllOpen = false
                    } else {
                        onBack()
                    }
                }
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(LightGoldBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Translation illustration icon",
                                tint = GoldAccent,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = titleAnnotated,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            lineHeight = 36.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            color = Color.Black.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (selectedLanguages.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Selected (${selectedLanguages.size}/3)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "Tap to change level",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    selectedLanguages.forEach { selected ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    tempSelectedLevelCode = selected.level.tag
                                                    languageForLevelSelect = selected.language
                                                },
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5)),
                                            border = BorderStroke(1.dp, GoldAccent),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    AsyncImage(
                                                        model = "https://hatscripts.github.io/circle-flags/flags/${selected.language.flagCode.lowercase()}.svg",
                                                        contentDescription = "${selected.language.name} flag",
                                                        imageLoader = imageLoader,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = selected.language.name,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Black
                                                        )
                                                        Text(
                                                            text = selected.level.tag,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = GoldAccent
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { selectedLanguages.remove(selected) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Text(
                                                        text = "✕",
                                                        fontSize = 14.sp,
                                                        color = Color.Black.copy(alpha = 0.5f),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                isBrowseAllOpen = true
                            },
                            placeholder = { Text("Search 250+ languages...", color = Color.Gray, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search icon",
                                    tint = Color.Black.copy(alpha = 0.4f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isBrowseAllOpen = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Black,
                                disabledBorderColor = Color.Black.copy(alpha = 0.12f),
                                disabledPlaceholderColor = Color.Black.copy(alpha = 0.4f),
                                disabledLeadingIconColor = Color.Black.copy(alpha = 0.4f),
                                disabledContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Popular",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            val chunkedPopular = remember { LanguageData.popularLanguages.chunked(3) }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (chunk in chunkedPopular) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        for (lang in chunk) {
                                            val isSelected = selectedLanguages.any { it.language.code == lang.code }
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        if (isSelected) {
                                                            selectedLanguages.removeAll { it.language.code == lang.code }
                                                        } else {
                                                            if (selectedLanguages.size >= 3) {
                                                                Toast.makeText(context, "Only up to 3 languages can be selected.", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                tempSelectedLevelCode = "Native / Fluent"
                                                                languageForLevelSelect = lang
                                                            }
                                                        }
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                border = BorderStroke(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) GoldAccent else Color(0xFFF1F5F9)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AsyncImage(
                                                        model = "https://hatscripts.github.io/circle-flags/flags/${lang.flagCode.lowercase()}.svg",
                                                        contentDescription = "${lang.name} flag",
                                                        imageLoader = imageLoader,
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = lang.name,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Black
                                                    )
                                                }
                                            }
                                        }
                                        if (chunk.size < 3) {
                                            repeat(3 - chunk.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isBrowseAllOpen = true },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(LightGoldBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Browse all languages",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = "250+ languages available",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Arrow right icon",
                                    tint = Color.Black.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock indicator",
                                tint = Color.Black.copy(alpha = 0.35f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Your selected languages and GenZ levels will be public so everyone knows what you speak!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black.copy(alpha = 0.45f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Button(
                    onClick = {
                        if (selectedLanguages.isNotEmpty()) {
                            val serialized = selectedLanguages.joinToString(", ") { "${it.language.name} (${it.level.tag})" }
                            onComplete(serialized)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    enabled = selectedLanguages.isNotEmpty(),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward arrow",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isBrowseAllOpen) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    var overlayQuery by remember { mutableStateOf("") }
                    val filteredGrouped = remember(overlayQuery) {
                        val source = if (overlayQuery.isBlank()) {
                            LanguageData.allLanguages
                        } else {
                            LanguageData.allLanguages.filter {
                                it.name.contains(overlayQuery, ignoreCase = true) ||
                                it.nativeName.contains(overlayQuery, ignoreCase = true)
                            }
                        }
                        source.groupBy { it.name.first().uppercaseChar() }.toSortedMap()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { isBrowseAllOpen = false }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back back",
                                    tint = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Choose Language (${selectedLanguages.size}/3)",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        OutlinedTextField(
                            value = overlayQuery,
                            onValueChange = { overlayQuery = it },
                            placeholder = { Text("Search 250+ languages...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search icon",
                                    tint = Color.Black.copy(alpha = 0.4f)
                                )
                            },
                            trailingIcon = {
                                if (overlayQuery.isNotEmpty()) {
                                    IconButton(onClick = { overlayQuery = "" }) {
                                        Text("✕", fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.5f))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color.Black.copy(alpha = 0.12f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (overlayQuery.isBlank()) {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Popular",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black.copy(alpha = 0.6f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    val popularChunked = LanguageData.popularLanguages.chunked(3)
                                    items(popularChunked) { chunk ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            for (pLang in chunk) {
                                                val isSelected = selectedLanguages.any { it.language.code == pLang.code }
                                                Card(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            if (isSelected) {
                                                                selectedLanguages.removeAll { it.language.code == pLang.code }
                                                            } else {
                                                                if (selectedLanguages.size >= 3) {
                                                                    Toast.makeText(context, "Only up to 3 languages can be selected.", Toast.LENGTH_SHORT).show()
                                                                } else {
                                                                    tempSelectedLevelCode = "Native / Fluent"
                                                                    languageForLevelSelect = pLang
                                                                    isBrowseAllOpen = false
                                                                }
                                                            }
                                                        },
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    border = BorderStroke(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) GoldAccent else Color(0xFFF1F5F9)
                                                    ),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 8.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        AsyncImage(
                                                            model = "https://hatscripts.github.io/circle-flags/flags/${pLang.flagCode.lowercase()}.svg",
                                                            contentDescription = "${pLang.name} flag",
                                                            imageLoader = imageLoader,
                                                            modifier = Modifier
                                                                .size(20.dp)
                                                                .clip(CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = pLang.name,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Black
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    item {
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Text(
                                            text = "All Languages",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }

                                for ((charHeader, languagesList) in filteredGrouped) {
                                    item {
                                        Text(
                                            text = charHeader.toString(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = GoldAccent,
                                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 12.dp)
                                        )
                                    }

                                    items(languagesList) { lang ->
                                        val isSelected = selectedLanguages.any { it.language.code == lang.code }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isSelected) {
                                                        selectedLanguages.removeAll { it.language.code == lang.code }
                                                    } else {
                                                        if (selectedLanguages.size >= 3) {
                                                            Toast.makeText(context, "Only up to 3 languages can be selected.", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            tempSelectedLevelCode = "Native / Fluent"
                                                            languageForLevelSelect = lang
                                                            isBrowseAllOpen = false
                                                        }
                                                    }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) LightGoldBg else Color(0xFFFFFBEB)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Text("✓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    } else {
                                                        Text(
                                                            text = lang.initials,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Black
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    text = lang.name,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.Black
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Select language navigation arrow",
                                                tint = if (isSelected) GoldAccent else Color.Black.copy(alpha = 0.25f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== STEP 5: NATIVE LANGUAGE SCREEN ====================
@Composable
fun OnboardingNativeLanguageScreen(
    onComplete: (String) -> Unit,
    onBack: () -> Unit
) {
    OnboardingLanguageScreenBase(
        title = "What's your native language?",
        subtitle = "Choose the language you grew up speaking.",
        currentStep = 5,
        onComplete = onComplete,
        onBack = onBack
    )
}

// ==================== STEP 6: LEARNING LANGUAGE SCREEN ====================
@Composable
fun OnboardingLearningLanguageScreen(
    onComplete: (String) -> Unit,
    onBack: () -> Unit
) {
    OnboardingLanguageScreenBase(
        title = "What language are you learning?",
        subtitle = "Choose the language you want to practice.",
        currentStep = 6,
        onComplete = onComplete,
        onBack = onBack
    )
}
