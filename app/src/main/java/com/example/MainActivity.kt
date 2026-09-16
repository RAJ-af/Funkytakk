package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authViewModel.initializeFirebaseIfNeeded(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    var currentScreen by rememberSaveable { mutableStateOf("welcome") }

                    val authState by authViewModel.authState.collectAsState()

                    // Temporary registers to compile setup profiles atomically at Step 5
                    var tempUsername by rememberSaveable { mutableStateOf("") }
                    var tempDisplayName by rememberSaveable { mutableStateOf("") }
                    var tempAvatar by rememberSaveable { mutableStateOf("") }
                    var tempDob by rememberSaveable { mutableStateOf("") }
                    var tempGender by rememberSaveable { mutableStateOf("") }
                    var tempCountry by rememberSaveable { mutableStateOf("") }
                    var tempCountryCode by rememberSaveable { mutableStateOf("") }
                    var tempNativeLanguage by rememberSaveable { mutableStateOf("") }
                    var tempLearningLanguage by rememberSaveable { mutableStateOf("") }
                    var tempHobbies by remember { mutableStateOf<List<String>>(emptyList()) }

                    LaunchedEffect(authState) {
                        if (authState is AuthState.Success) {
                            val user = (authState as AuthState.Success).user
                            if (!user.isEmailVerified) {
                                currentScreen = "verify_email"
                            } else {
                                val prefs = applicationContext.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE)
                                val isConfigured = prefs.getBoolean("profile_configured_${user.uid}", false) || 
                                        (!user.username.isNullOrBlank() && !user.dob.isNullOrBlank() && !user.gender.isNullOrBlank() && !user.nativeLanguage.isNullOrBlank() && !user.learningLanguage.isNullOrBlank() && !user.country.isNullOrBlank() && user.hobbies.isNotEmpty())
                                if (isConfigured) {
                                    currentScreen = "home"
                                } else {
                                    currentScreen = "onboarding_profile"
                                }
                            }
                        }
                    }

                    Crossfade(
                        targetState = currentScreen,
                        label = "screen_routing"
                    ) { screen ->
                        when (screen) {
                            "welcome" -> WelcomeScreen(
                                onGetStartedClick = {
                                    currentScreen = "privacy"
                                }
                            )
                            "privacy" -> PrivacyScreen(
                                onContinueClick = {
                                    currentScreen = "auth"
                                }
                            )
                            "auth" -> AuthScreen(
                                viewModel = authViewModel,
                                onAuthSuccess = {
                                    // Managed by LaunchedEffect auto checking
                                },
                                onVerificationWaiting = {
                                    currentScreen = "verify_email"
                                }
                            )
                            "verify_email" -> VerifyEmailScreen(
                                viewModel = authViewModel,
                                onVerifiedSuccess = {
                                    // Managed by LaunchedEffect auto checking
                                },
                                onBackClick = {
                                    currentScreen = "auth"
                                }
                            )
                            "onboarding_profile" -> ProfileStep1QuickCard(
                                viewModel = authViewModel,
                                initialDisplayName = tempDisplayName,
                                initialUsername = tempUsername,
                                initialAvatar = tempAvatar,
                                initialGender = tempGender,
                                initialDob = tempDob,
                                onNext = { displayName, username, avatar, gender, dob ->
                                    tempDisplayName = displayName
                                    tempUsername = username
                                    tempAvatar = avatar
                                    tempGender = gender
                                    tempDob = dob
                                    currentScreen = "onboarding_personalize"
                                },
                                onBack = {
                                    authViewModel.logout(applicationContext)
                                    currentScreen = "auth"
                                }
                            )
                            "onboarding_personalize" -> ProfileStep2Personalize(
                                initialNativeLanguage = tempNativeLanguage,
                                initialLearningLanguage = tempLearningLanguage,
                                initialCountry = tempCountry,
                                initialCountryCode = tempCountryCode,
                                initialHobbies = tempHobbies,
                                onComplete = { nativeLang, learningLang, country, countryCode, hobbies ->
                                    tempNativeLanguage = nativeLang
                                    tempLearningLanguage = learningLang
                                    tempCountry = country
                                    tempCountryCode = countryCode
                                    tempHobbies = hobbies
                                    authViewModel.updateProfile(
                                        context = applicationContext,
                                        username = tempUsername,
                                        displayName = tempDisplayName,
                                        avatar = tempAvatar,
                                        dob = tempDob,
                                        gender = tempGender,
                                        nativeLanguage = tempNativeLanguage,
                                        learningLanguage = tempLearningLanguage,
                                        country = tempCountry,
                                        countryCode = tempCountryCode,
                                        hobbies = tempHobbies
                                    ) {
                                        currentScreen = "home"
                                    }
                                },
                                onSkip = {
                                    authViewModel.updateProfile(
                                        context = applicationContext,
                                        username = tempUsername,
                                        displayName = tempDisplayName,
                                        avatar = tempAvatar,
                                        dob = tempDob,
                                        gender = tempGender,
                                        nativeLanguage = if (tempNativeLanguage.isNotBlank()) tempNativeLanguage else "English",
                                        learningLanguage = if (tempLearningLanguage.isNotBlank()) tempLearningLanguage else "Spanish",
                                        country = if (tempCountry.isNotBlank()) tempCountry else "United States",
                                        countryCode = if (tempCountryCode.isNotBlank()) tempCountryCode else "us",
                                        hobbies = tempHobbies
                                    ) {
                                        currentScreen = "home"
                                    }
                                },
                                onBack = {
                                    currentScreen = "onboarding_profile"
                                }
                            )
                            "home" -> HomeScreen(
                                viewModel = authViewModel,
                                onLogoutSuccess = {
                                    currentScreen = "welcome"
                                },
                                onCreateLanguageRoom = {
                                    currentScreen = "create_language_room"
                                },
                                onRoomClick = {
                                    currentScreen = "voice_room"
                                }
                            )
                            "create_language_room" -> CreateLanguageRoomScreen(
                                viewModel = authViewModel,
                                onBack = { currentScreen = "home" },
                                onCreate = { currentScreen = "home" }
                            )
                            "voice_room" -> VoiceRoomScreen(
                                viewModel = authViewModel,
                                onBack = {
                                    authViewModel.leaveRoom()
                                    currentScreen = "home"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
