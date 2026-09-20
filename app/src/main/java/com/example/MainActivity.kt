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
                    color = Color(0xFFFAFAEE)
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
                    var createRoomIsPrivate by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(authState) {
                        if (authState is AuthState.Success) {
                            val user = (authState as AuthState.Success).user
                            if (!user.username.isNullOrBlank()) tempUsername = user.username
                            if (!user.displayName.isNullOrBlank()) tempDisplayName = user.displayName
                            if (!user.avatar.isNullOrBlank()) tempAvatar = user.avatar
                            if (!user.dob.isNullOrBlank()) tempDob = user.dob
                            if (!user.gender.isNullOrBlank()) tempGender = user.gender
                            if (!user.country.isNullOrBlank()) tempCountry = user.country
                            if (!user.countryCode.isNullOrBlank()) tempCountryCode = user.countryCode
                            if (!user.nativeLanguage.isNullOrBlank()) tempNativeLanguage = user.nativeLanguage
                            if (!user.learningLanguage.isNullOrBlank()) tempLearningLanguage = user.learningLanguage
                            if (user.hobbies.isNotEmpty()) tempHobbies = user.hobbies

                            if (!user.isEmailVerified) {
                                currentScreen = "verify_email"
                            } else if (user.onboardingComplete) {
                                currentScreen = "home"
                            } else {
                                currentScreen = when {
                                    user.username.isNullOrBlank() || user.displayName.isNullOrBlank() -> "onboarding_profile"
                                    (user.age == null || user.age <= 0) && user.dob.isNullOrBlank() -> "onboarding_age"
                                    user.gender.isNullOrBlank() -> "onboarding_gender"
                                    user.country.isNullOrBlank() -> "onboarding_country"
                                    user.nativeLanguage.isNullOrBlank() -> "onboarding_native_language"
                                    user.learningLanguage.isNullOrBlank() -> "onboarding_learning_language"
                                    else -> "onboarding_hobbies"
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
                                    authViewModel.patchStep1Profile(
                                        context = applicationContext,
                                        username = username,
                                        displayName = displayName,
                                        avatar = avatar,
                                        onSuccess = {
                                            currentScreen = "onboarding_age"
                                        }
                                    )
                                },
                                onBack = {
                                    authViewModel.logout(applicationContext)
                                    currentScreen = "auth"
                                }
                            )
                            "onboarding_age" -> OnboardingAgeScreen(
                                initialDob = tempDob,
                                onNext = { dobString, age ->
                                    tempDob = dobString
                                    authViewModel.patchProfileStep(
                                        context = applicationContext,
                                        updates = mapOf("age" to age),
                                        onSuccess = {
                                            currentScreen = "onboarding_gender"
                                        }
                                    )
                                },
                                onBack = {
                                    currentScreen = "onboarding_profile"
                                }
                            )
                            "onboarding_gender" -> OnboardingGenderScreen(
                                initialGender = tempGender,
                                onComplete = { gender ->
                                    tempGender = gender
                                    authViewModel.patchProfileStep(
                                        context = applicationContext,
                                        updates = mapOf("gender" to gender),
                                        onSuccess = {
                                            currentScreen = "onboarding_country"
                                        }
                                    )
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
                                    authViewModel.patchProfileStep(
                                        context = applicationContext,
                                        updates = mapOf("country" to countryName),
                                        onSuccess = {
                                            currentScreen = "onboarding_native_language"
                                        }
                                    )
                                },
                                onBack = {
                                    currentScreen = "onboarding_gender"
                                }
                            )
                            "onboarding_native_language" -> OnboardingNativeLanguageScreen(
                                initialNativeLanguages = tempNativeLanguage,
                                onComplete = { nativeLangString ->
                                    tempNativeLanguage = nativeLangString
                                    val langList = nativeLangString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    authViewModel.patchProfileStep(
                                        context = applicationContext,
                                        updates = mapOf("native_languages" to langList),
                                        onSuccess = {
                                            currentScreen = "onboarding_learning_language"
                                        }
                                    )
                                },
                                onBack = {
                                    currentScreen = "onboarding_country"
                                }
                            )
                            "onboarding_learning_language" -> OnboardingLearningLanguageScreen(
                                initialLearningLanguages = tempLearningLanguage,
                                onComplete = { learningLangString ->
                                    tempLearningLanguage = learningLangString
                                    val langList = learningLangString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    authViewModel.patchProfileStep(
                                        context = applicationContext,
                                        updates = mapOf("learning_languages" to langList),
                                        onSuccess = {
                                            currentScreen = "onboarding_hobbies"
                                        }
                                    )
                                },
                                onBack = {
                                    currentScreen = "onboarding_native_language"
                                }
                            )
                            "onboarding_hobbies" -> OnboardingHobbiesScreen(
                                initialHobbies = tempHobbies,
                                onComplete = { hobbiesList ->
                                    tempHobbies = hobbiesList
                                    authViewModel.patchProfileStep(
                                        context = applicationContext,
                                        updates = mapOf(
                                            "hobbies" to hobbiesList,
                                            "onboarding_complete" to true
                                        ),
                                        onSuccess = {
                                            currentScreen = "home"
                                        }
                                    )
                                },
                                onBack = {
                                    currentScreen = "onboarding_learning_language"
                                }
                            )
                            "home" -> HomeScreen(
                                viewModel = authViewModel,
                                onLogoutSuccess = {
                                    currentScreen = "welcome"
                                },
                                onCreateLanguageRoom = { isPrivate ->
                                    createRoomIsPrivate = isPrivate
                                    currentScreen = "create_language_room"
                                },
                                onRoomClick = {
                                    currentScreen = "voice_room"
                                }
                            )
                            "create_language_room" -> CreateLanguageRoomScreen(
                                viewModel = authViewModel,
                                initialIsPrivate = createRoomIsPrivate,
                                onBack = { currentScreen = "home" },
                                onCreate = { currentScreen = "voice_room" }
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
