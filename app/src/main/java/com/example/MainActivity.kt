package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.*
import com.example.ui.screens.chat.RestoreChatBackupScreen
import com.example.ui.components.FloatingBubbleOverlay
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private var pendingDeepLinkUri by mutableStateOf<Uri?>(null)
    private var pendingNavigateTo by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create System Notification Channels
        com.example.util.SystemNotificationHelper.createNotificationChannels(applicationContext)

        handleDeepLinkIntent(intent)
        intent?.getStringExtra("navigate_to")?.let {
            pendingNavigateTo = it
        }
        authViewModel.initializeFirebaseIfNeeded(applicationContext)
        setContent {
            // Request Notification permission on Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    android.util.Log.d("MainActivity", "POST_NOTIFICATIONS granted: $isGranted")
                }
                LaunchedEffect(Unit) {
                    if (!com.example.util.SystemNotificationHelper.hasNotificationPermission(applicationContext)) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFAFAEE)
                ) {
                    var currentScreen by rememberSaveable { mutableStateOf("welcome") }

                    LaunchedEffect(pendingNavigateTo) {
                        pendingNavigateTo?.let { target ->
                            pendingNavigateTo = null
                            if (target == "voice_room") {
                                authViewModel.verifyAndNavigateToRoom(
                                    context = applicationContext,
                                    onRoomActive = {
                                        currentScreen = "voice_room"
                                    },
                                    onRoomEnded = {
                                        android.widget.Toast.makeText(applicationContext, "This room has ended.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                currentScreen = target
                            }
                        }
                    }

                    val authState by authViewModel.authState.collectAsState()
                    val myModerationStatus by authViewModel.myModerationStatus.collectAsState()

                    // Lifecycle observer: refresh moderation status and sync group events when app enters foreground / resumes
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                authViewModel.refreshModerationStatus(applicationContext)
                                if (SupabaseSessionManager.getUserId(applicationContext) != null) {
                                    com.example.repository.GroupChatRepository.onAppResumed(applicationContext)
                                }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    // Check moderation on startup or auth state change
                    LaunchedEffect(authState) {
                        authViewModel.refreshModerationStatus(applicationContext)
                    }

                    val isSuspended = remember(myModerationStatus) {
                        myModerationStatus != null &&
                            myModerationStatus?.action_type?.equals("suspend", ignoreCase = true) == true &&
                            com.example.ModerationManager.isActionActive(myModerationStatus)
                    }

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
                    var activeSupportReportId by rememberSaveable { mutableStateOf<String?>(null) }
                    var pendingBackupStatus by remember { mutableStateOf<ChatBackupStatusResponse?>(null) }

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
                                val supaAuth = SupabaseSessionManager.getBearerToken(applicationContext)
                                val isVaultConfigured = com.example.crypto.VaultSecurityManager.isVaultConfigured(applicationContext)
                                if (!supaAuth.isNullOrBlank() && !isVaultConfigured) {
                                    try {
                                        val statusResp = SupabaseClient.api.getChatBackupStatus(
                                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                            auth = supaAuth
                                        )
                                        if (statusResp.isSuccessful && statusResp.body()?.has_backup == true) {
                                            pendingBackupStatus = statusResp.body()
                                            currentScreen = "restore_chat_backup"
                                        } else {
                                            currentScreen = "home"
                                        }
                                    } catch (_: Exception) {
                                        currentScreen = "home"
                                    }
                                } else {
                                    currentScreen = "home"
                                }
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

                            // Initialize E2EE identity keys and encrypted repository
                            com.example.crypto.E2EEManager.ensureIdentityKeyInitialized(applicationContext)
                            com.example.repository.EncryptedChatRepository.init(applicationContext)
                        }
                    }

                    // Process deep links when user is on home screen or voice room, or when deep link arrives
                    LaunchedEffect(pendingDeepLinkUri, currentScreen, authState) {
                        val uri = pendingDeepLinkUri
                        if (uri != null) {
                            val isUserReady = currentScreen == "home" || currentScreen == "voice_room" ||
                                (authState is AuthState.Success && (authState as AuthState.Success).user.onboardingComplete)

                            if (isUserReady) {
                                pendingDeepLinkUri = null
                                when (uri.host?.lowercase()) {
                                    "notifications" -> {
                                        currentScreen = "notifications"
                                    }
                                    "support" -> {
                                        activeSupportReportId = uri.getQueryParameter("report_id")
                                        currentScreen = "support_chat"
                                    }
                                    else -> {
                                        authViewModel.processDeepLinkUri(
                                            uri = uri,
                                            context = applicationContext,
                                            onNavigateToRoom = {
                                                currentScreen = "voice_room"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isSuspended) {
                        SuspendedAccountScreen(
                            moderationStatus = myModerationStatus,
                            onStatusRefreshed = {
                                authViewModel.refreshModerationStatus(applicationContext)
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
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
                            "restore_chat_backup" -> RestoreChatBackupScreen(
                                backupStatus = pendingBackupStatus ?: ChatBackupStatusResponse(has_backup = true),
                                onRestoreSuccess = {
                                    currentScreen = "home"
                                },
                                onSkipRestore = {
                                    currentScreen = "home"
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
                                        updates = mapOf("country" to countryName, "country_code" to countryCode),
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
                                            currentScreen = "onboarding_chat_vault"
                                        }
                                    )
                                },
                                onBack = {
                                    currentScreen = "onboarding_learning_language"
                                }
                            )
                            "onboarding_chat_vault" -> OnboardingChatVaultScreen(
                                onComplete = {
                                    currentScreen = "home"
                                },
                                onBack = {
                                    currentScreen = "onboarding_hobbies"
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
                                },
                                onOpenNotifications = {
                                    currentScreen = "notifications"
                                },
                                onOpenSupportChat = { reportId ->
                                    activeSupportReportId = reportId
                                    currentScreen = "support_chat"
                                }
                            )
                            "notifications" -> NotificationsScreen(
                                onBack = { currentScreen = "home" },
                                onOpenSupportChat = { reportId ->
                                    activeSupportReportId = reportId
                                    currentScreen = "support_chat"
                                }
                            )
                            "support_chat" -> SupportChatScreen(
                                reportId = activeSupportReportId,
                                onBack = { currentScreen = "home" }
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
                                    currentScreen = "home"
                                }
                            )
                        }
                    }

                    // Floating Bubble Overlay (replacement for Dynamic Island when in active room but navigated elsewhere)
                    val activeRoomId by authViewModel.activeRoomId.collectAsState()

                    LaunchedEffect(currentScreen) {
                        if (currentScreen == "voice_room") {
                            com.example.service.VoiceRoomOverlayManager.stopService(this@MainActivity)
                        } else if (activeRoomId != null && com.example.service.OverlayPermissionHelper.canDrawOverlays(this@MainActivity)) {
                            com.example.service.VoiceRoomOverlayManager.startServiceIfPermitted(this@MainActivity)
                        }
                    }

                    if (activeRoomId != null && currentScreen != "voice_room") {
                        FloatingBubbleOverlay(
                            viewModel = authViewModel,
                            onNavigateToRoom = {
                                authViewModel.verifyAndNavigateToRoom(
                                    context = applicationContext,
                                    onRoomActive = {
                                        currentScreen = "voice_room"
                                    },
                                    onRoomEnded = {
                                        android.widget.Toast.makeText(applicationContext, "This room has ended.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        )
                    }
                }
                }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
        intent.getStringExtra("navigate_to")?.let {
            pendingNavigateTo = it
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (authViewModel.activeRoomId.value != null &&
            com.example.service.OverlayPermissionHelper.canDrawOverlays(this)
        ) {
            com.example.service.VoiceRoomOverlayManager.startServiceIfPermitted(this)
        }
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null) {
            val scheme = data.scheme?.lowercase() ?: ""
            val host = data.host?.lowercase() ?: ""
            val path = data.path ?: ""
            val isGroupDeepLink = (scheme == "funkytalk" && host == "group") ||
                ((scheme == "https" || scheme == "http") && (
                    path.startsWith("/g/") ||
                    path.startsWith("/group/") ||
                    host.equals(BuildConfig.GROUP_INVITE_HOST, ignoreCase = true)
                ))
            if (isGroupDeepLink) {
                val code = com.example.repository.GroupChatRepository.parseInviteCode(data.toString())
                if (code.isNotBlank()) {
                    com.example.repository.GroupChatRepository.setPendingInviteCode(code)
                    getSharedPreferences("funky_talk_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("pending_group_invite_code", code)
                        .apply()
                }
            }
            if (scheme == "funkytalk" || scheme == "https" || scheme == "http") {
                pendingDeepLinkUri = data
            }
        }
    }
}
