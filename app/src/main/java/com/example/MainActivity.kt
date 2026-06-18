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
                            "onboarding_profile" -> OnboardingProfileScreen(
                                viewModel = authViewModel,
                                initialUsername = tempUsername,
                                initialDisplayName = tempDisplayName,
                                initialAvatar = tempAvatar,
                                onNext = { username, displayName, avatar ->
                                    tempUsername = username
                                    tempDisplayName = displayName
                                    tempAvatar = avatar
                                    currentScreen = "onboarding_age"
                                },
                                onBack = {
                                    authViewModel.logout(applicationContext)
                                    currentScreen = "auth"
                                }
                            )
                            "onboarding_age" -> OnboardingAgeScreen(
                                initialDob = tempDob,
                                onNext = { dobString ->
                                    tempDob = dobString
                                    currentScreen = "onboarding_gender"
                                },
                                onBack = {
                                    currentScreen = "onboarding_profile"
                                }
                            )
                            "onboarding_gender" -> OnboardingGenderScreen(
                                initialGender = tempGender,
                                onComplete = { selectedGender ->
                                    tempGender = selectedGender
                                    currentScreen = "onboarding_country"
                                },
                                onBack = {
                                    currentScreen = "onboarding_age"
                                }
                            )
                            "onboarding_country" -> OnboardingCountryScreen(
                                initialCountry = tempCountry,
                                initialCountryCode = tempCountryCode,
                                onComplete = { countryName, countryCode ->
                                    tempCountry = countryName
                                    tempCountryCode = countryCode
                                    currentScreen = "onboarding_native"
                                },
                                onBack = {
                                    currentScreen = "onboarding_gender"
                                }
                            )
                            "onboarding_native" -> OnboardingNativeLanguageScreen(
                                onComplete = { selectedLanguage ->
                                    tempNativeLanguage = selectedLanguage
                                    currentScreen = "onboarding_learning"
                                },
                                onBack = {
                                    currentScreen = "onboarding_country"
                                }
                            )
                            "onboarding_learning" -> OnboardingLearningLanguageScreen(
                                onComplete = { selectedLanguage ->
                                    tempLearningLanguage = selectedLanguage
                                    currentScreen = "onboarding_hobbies"
                                },
                                onBack = {
                                    currentScreen = "onboarding_native"
                                }
                            )
                            "onboarding_hobbies" -> OnboardingHobbiesScreen(
                                onComplete = { selectedHobbies ->
                                    tempHobbies = selectedHobbies
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
                                onBack = {
                                    currentScreen = "onboarding_learning"
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
                                onCreate = { currentScreen = "voice_room" }
                            )
                            "voice_room" -> VoiceRoomScreen(
                                onBack = { currentScreen = "home" }
                            )
                        }
                    }
                }
            }
        }
    }
}
