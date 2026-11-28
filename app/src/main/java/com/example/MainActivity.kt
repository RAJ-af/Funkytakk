package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.screens.*
import com.example.ui.screens.chat.RestoreChatBackupScreen
import com.example.ui.components.FloatingBubbleOverlay
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex

class MainActivity : FragmentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private var pendingDeepLinkUri by mutableStateOf<Uri?>(null)
    private var pendingNavigateTo by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.ui.components.SecureWindowRegistry.init(applicationContext)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(!com.example.ui.components.SecureWindowRegistry.blockScreenshots.value)
        }
        enableEdgeToEdge()

        // Create System Notification Channels (including channel ID "messages" with importance HIGH)
        com.example.service.PushNotificationManager.initialize(applicationContext)
        com.example.service.PushNotificationManager.createNotificationChannels(applicationContext)
        com.example.util.SystemNotificationHelper.createNotificationChannels(applicationContext)

        handleDeepLinkIntent(intent)
        handlePushNotificationIntent(intent)
        authViewModel.initializeFirebaseIfNeeded(applicationContext)
        com.example.service.PushNotificationManager.onColdStart(applicationContext)
        setContent {
            // Request Notification permission on Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    android.util.Log.d("MainActivity", "POST_NOTIFICATIONS granted: $isGranted")
                    if (isGranted && SupabaseSessionManager.getUserId(applicationContext) != null) {
                        com.example.service.PushNotificationManager.onUserLoggedIn(applicationContext)
                    }
                }
                LaunchedEffect(Unit) {
                    if (!com.example.util.SystemNotificationHelper.hasNotificationPermission(applicationContext)) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            val blockScreenshots by com.example.ui.components.SecureWindowRegistry.blockScreenshots.collectAsState()
            LaunchedEffect(blockScreenshots) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    setRecentsScreenshotEnabled(!blockScreenshots)
                }
            }
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.example.ui.theme.LocalFunkyColors.current.bg
                ) {
                    var currentScreen by rememberSaveable { mutableStateOf("splash") }
                    var minSplashElapsed by rememberSaveable { mutableStateOf(false) }
                    var splashHardTimeoutReached by rememberSaveable { mutableStateOf(false) }
                    var localStartupSnapshot by remember {
                        mutableStateOf(com.example.data.StartupPrefsStore.getFastSnapshot(applicationContext))
                    }
                    LaunchedEffect(Unit) {
                        launch {
                            localStartupSnapshot = com.example.data.StartupPrefsStore.readSnapshot(applicationContext)
                        }
                        launch {
                            kotlinx.coroutines.delay(320)
                            minSplashElapsed = true
                        }
                        launch {
                            kotlinx.coroutines.delay(1500)
                            splashHardTimeoutReached = true
                        }
                    }

                    LaunchedEffect(pendingNavigateTo) {
                        pendingNavigateTo?.let { target ->
                            pendingNavigateTo = null
                            when (target) {
                                "voice_room" -> {
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
                                "direct_chat" -> {
                                    currentScreen = "home"
                                }
                                else -> {
                                    currentScreen = target
                                }
                            }
                        }
                    }

                    val authState by authViewModel.authState.collectAsState()
                    val sessionVerificationState by authViewModel.sessionVerificationState.collectAsState()
                    val myModerationStatus by authViewModel.myModerationStatus.collectAsState()
                    val isUnderReview by authViewModel.accountUnderReview.collectAsState()
                    val deletionStatus by authViewModel.deletionStatus.collectAsState()
                    val deletionReceipt by authViewModel.deletionReceipt.collectAsState()
                    var showSecurityResetDialog by remember {
                        mutableStateOf(LegacySecurityMigration.shouldShowSecurityResetDialog(applicationContext))
                    }

                    LaunchedEffect(Unit) {
                        com.example.ui.components.AppNetworkManager.startMonitoring(applicationContext)
                        authViewModel.runPendingEraseLocalChatsIfNeeded(applicationContext)
                    }

                    // Initialize authenticated services in parallel once session / cached user is ready
                    LaunchedEffect(authState, sessionVerificationState) {
                        if (authViewModel.deletionReceipt.value != null) {
                            return@LaunchedEffect
                        }
                        if (authState is AuthState.Success &&
                            sessionVerificationState !is SessionVerificationState.Failed
                        ) {
                            val authedUid = SupabaseSessionManager.getUserId(applicationContext)
                                ?: (authState as AuthState.Success).user.uid
                            ActiveUserManager.setActiveUid(authedUid)
                            ChatPreferencesManager.init(applicationContext)
                            CloudAiManager.init(applicationContext)
                            com.example.crypto.VaultSecurityManager.init(applicationContext)
                            TelemetryManager.onLogin(authedUid)

                            coroutineScope {
                                listOf(
                                    async {
                                        try {
                                            com.example.util.StartupTimingTracker.measure("chat_repo_init") {
                                                com.example.repository.EncryptedChatRepository.init(applicationContext)
                                            }
                                        } catch (ce: kotlinx.coroutines.CancellationException) {
                                            throw ce
                                        } catch (_: Throwable) {}
                                    },
                                    async {
                                        try {
                                            com.example.util.StartupTimingTracker.measure("group_repo_init") {
                                                com.example.repository.GroupChatRepository.init(applicationContext)
                                            }
                                        } catch (ce: kotlinx.coroutines.CancellationException) {
                                            throw ce
                                        } catch (_: Throwable) {}
                                    },
                                    async {
                                        try {
                                            com.example.util.StartupTimingTracker.measure("block_refresh") {
                                                com.example.repository.BlockRepository.init(applicationContext)
                                                com.example.repository.BlockRepository.refreshBlockedUsers(applicationContext)
                                            }
                                        } catch (ce: kotlinx.coroutines.CancellationException) {
                                            throw ce
                                        } catch (_: Throwable) {}
                                    },
                                    async {
                                        try {
                                            com.example.util.StartupTimingTracker.measure("moderation_status") {
                                                authViewModel.refreshModerationStatus(applicationContext)
                                            }
                                        } catch (ce: kotlinx.coroutines.CancellationException) {
                                            throw ce
                                        } catch (_: Throwable) {}
                                    },
                                    async {
                                        try {
                                            com.example.util.StartupTimingTracker.measure("account_review") {
                                                authViewModel.checkAccountReview(applicationContext)
                                            }
                                        } catch (ce: kotlinx.coroutines.CancellationException) {
                                            throw ce
                                        } catch (_: Throwable) {}
                                    },
                                    async {
                                        try {
                                            com.example.util.StartupTimingTracker.measure("deletion_status") {
                                                authViewModel.checkDeletionStatus(applicationContext)
                                            }
                                        } catch (ce: kotlinx.coroutines.CancellationException) {
                                            throw ce
                                        } catch (_: Throwable) {}
                                    },
                                    async {
                                        try {
                                            com.example.util.StartupTimingTracker.measure("support_init") {
                                                com.example.SupportChatManager.init(applicationContext)
                                            }
                                        } catch (ce: kotlinx.coroutines.CancellationException) {
                                            throw ce
                                        } catch (_: Throwable) {}
                                    },
                                    async {
                                        try {
                                            com.example.util.StartupTimingTracker.measure("feedback_badge") {
                                                com.example.repository.FeedbackRepository.refreshMyUpdatesCount(applicationContext)
                                            }
                                        } catch (ce: kotlinx.coroutines.CancellationException) {
                                            throw ce
                                        } catch (_: Throwable) {}
                                    }
                                ).awaitAll()
                            }
                            com.example.util.StartupTimingTracker.logSlowestSummary()
                        }
                    }

                    LaunchedEffect(currentScreen) {
                        TelemetryManager.setCurrentScreen(currentScreen)
                        if (currentScreen != "splash") {
                            com.example.util.StartupTimingTracker.markFirstContentRendered()
                        }
                    }

                    // Auto-open support chat when event emitted (open == true and unread > 0)
                    LaunchedEffect(Unit) {
                        com.example.SupportChatManager.autoOpenChatEvent.collect {
                            currentScreen = "support_chat"
                        }
                    }

                    // Observe remote device revocation / single-device sign-out
                    val remoteSignOutCounter by com.example.repository.DeviceSessionRepository.remoteSignOutCounter.collectAsState()
                    val pendingRevokedNotice by com.example.repository.DeviceSessionRepository.deviceRevokedNotice.collectAsState()
                    var activeRevokedDialogMessage by rememberSaveable { mutableStateOf<String?>(null) }

                    LaunchedEffect(remoteSignOutCounter) {
                        if (remoteSignOutCounter > 0L) {
                            authViewModel.clearStateOnRemoteSignOut()
                            val msg = com.example.repository.DeviceSessionRepository.deviceRevokedNotice.value
                            if (!msg.isNullOrBlank()) {
                                activeRevokedDialogMessage = msg
                                com.example.repository.DeviceSessionRepository.consumeDeviceRevokedNotice()
                            }
                            currentScreen = "auth"
                        }
                    }

                    LaunchedEffect(pendingRevokedNotice) {
                        val msg = pendingRevokedNotice
                        if (!msg.isNullOrBlank()) {
                            activeRevokedDialogMessage = msg
                            com.example.repository.DeviceSessionRepository.consumeDeviceRevokedNotice()
                        }
                    }

                    // Lifecycle observer: refresh app flags if stale, moderation status, push token, touch_device, and group events on resume
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                AppFlagsRepository.refreshIfStale(applicationContext)
                                // Keep Side Dynamic Island strictly in-app while foregrounded
                                com.example.service.VoiceRoomOverlayManager.stopService(this@MainActivity)
                                if (SupabaseSessionManager.hasStoredAccount(applicationContext) && authState is AuthState.Success) {
                                    authViewModel.refreshModerationStatus(applicationContext)
                                    authViewModel.checkAccountReview(applicationContext)
                                    com.example.SupportChatManager.fetchSupportSession(applicationContext)
                                    authViewModel.checkDeletionStatus(applicationContext)
                                    com.example.service.PushNotificationManager.onUserLoggedIn(applicationContext)
                                    com.example.repository.EncryptedChatRepository.onAppResumed(applicationContext)
                                    com.example.repository.GroupChatRepository.onAppResumed(applicationContext)
                                    com.example.ui.components.RoomInviteOverlayManager.checkAndShowPendingInvite(applicationContext)
                                    lifecycleOwner.lifecycleScope.launch {
                                        try {
                                            SupabaseSessionManager.ensureActiveSession(applicationContext)
                                            com.example.repository.DeviceSessionRepository.touchDevice(applicationContext)
                                        } catch (ce: kotlinx.coroutines.CancellationException) {
                                            throw ce
                                        } catch (_: Throwable) {}
                                    }
                                }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    // Foreground heartbeat every 60s while signed in and foregrounded
                    LaunchedEffect(authState, lifecycleOwner) {
                        if (authState !is AuthState.Success) return@LaunchedEffect
                        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            while (isActive) {
                                delay(60_000L)
                                try {
                                    if (SupabaseSessionManager.hasValidAuthenticatedSession(applicationContext)) {
                                        com.example.repository.DeviceSessionRepository.touchDevice(applicationContext)
                                    }
                                } catch (ce: kotlinx.coroutines.CancellationException) {
                                    throw ce
                                } catch (_: Throwable) {}
                            }
                        }
                    }

                    val appFlags by AppFlagsRepository.flags.collectAsState()
                    val updateRequired = appFlags.minSupportedVersionCode > 0 &&
                        BuildConfig.VERSION_CODE < appFlags.minSupportedVersionCode

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
                    var openCreateRoomTrigger by rememberSaveable { mutableStateOf(false) }
                    var activeSupportReportId by rememberSaveable { mutableStateOf<String?>(null) }
                    var feedbackInitialCreateKind by rememberSaveable { mutableStateOf<String?>(null) }
                    var pendingBackupStatus by remember { mutableStateOf<ChatBackupStatusResponse?>(null) }
                    var endedRoomPeopleCount by rememberSaveable { mutableStateOf(12) }
                    var endedRoomTotalTime by rememberSaveable { mutableStateOf(28) }

                    LaunchedEffect(authState) {
                        if (authState !is AuthState.Success && authState !is AuthState.Loading) {
                            tempUsername = ""
                            tempDisplayName = ""
                            tempAvatar = ""
                            tempDob = ""
                            tempGender = ""
                            tempCountry = ""
                            tempCountryCode = ""
                            tempNativeLanguage = ""
                            tempLearningLanguage = ""
                            tempHobbies = emptyList()
                        }
                    }

                    LaunchedEffect(authState, sessionVerificationState, minSplashElapsed, splashHardTimeoutReached, localStartupSnapshot) {
                        if (!minSplashElapsed && currentScreen == "splash" && !splashHardTimeoutReached) {
                            return@LaunchedEffect
                        }
                        if (sessionVerificationState is SessionVerificationState.Failed) {
                            if (currentScreen != "support_chat") {
                                currentScreen = "verify_error"
                            }
                            return@LaunchedEffect
                        }
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
                                val shouldShowRestore = com.example.data.StartupPrefsStore.shouldShowBackupRestore(applicationContext)
                                currentScreen = if (shouldShowRestore) "restore_chat_backup" else "home"
                            } else {
                                currentScreen = com.example.data.StartupPrefsStore.resolveOnboardingRoute(user)
                            }

                            // Run post-boot identity, push token, and cosmetic fetches in parallel off the main frame
                            coroutineScope {
                                listOf(
                                    async {
                                        com.example.util.StartupTimingTracker.measure("e2ee_init") {
                                            com.example.crypto.E2EEManager.ensureIdentityKeyInitialized(applicationContext)
                                        }
                                    },
                                    async {
                                        com.example.util.StartupTimingTracker.measure("push_token_register") {
                                            com.example.service.PushNotificationManager.registerCurrentToken(applicationContext, force = false)
                                        }
                                    },
                                    async {
                                        com.example.util.StartupTimingTracker.measure("cosmetics_me") {
                                            com.example.store.CosmeticsManager.refreshMyCosmetics(applicationContext, user.uid)
                                        }
                                    },
                                    async {
                                        com.example.data.StartupPrefsStore.recordActiveUid(applicationContext, user.uid, user.onboardingComplete)
                                    }
                                ).awaitAll()
                            }
                        } else if (currentScreen == "splash" && (splashHardTimeoutReached || (minSplashElapsed && authState !is AuthState.Loading))) {
                            val localUid = localStartupSnapshot.lastUid
                                ?: SupabaseSessionManager.getUserId(applicationContext)
                                ?: runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
                            val hasStoredAccount = SupabaseSessionManager.hasStoredAccount(applicationContext)
                            val cachedProfile = if (!localUid.isNullOrBlank()) {
                                com.example.data.StartupPrefsStore.buildLocalCachedUserProfile(applicationContext, localUid)
                            } else null
                            if (hasStoredAccount &&
                                !localUid.isNullOrBlank() &&
                                (localStartupSnapshot.onboardingDone || cachedProfile?.onboardingComplete == true)
                            ) {
                                currentScreen = "home"
                            } else {
                                currentScreen = com.example.data.StartupPrefsStore.resolveUnauthenticatedRoute(localStartupSnapshot)
                            }
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

                    if (updateRequired) {
                        UpdateRequiredGateScreen(
                            onOpenUpdateUrl = {
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppFlagsRepository.UPDATE_URL)))
                                } catch (_: Exception) {
                                    android.widget.Toast.makeText(
                                        applicationContext,
                                        "Unable to open update link",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    } else if (isSuspended) {
                        if (currentScreen == "support_chat") {
                            SupportChatScreen(
                                reportId = activeSupportReportId,
                                onBack = { currentScreen = "home" }
                            )
                        } else {
                            SuspendedAccountScreen(
                                moderationStatus = myModerationStatus,
                                onStatusRefreshed = {
                                    authViewModel.refreshModerationStatus(applicationContext)
                                },
                                onAppeal = {
                                    currentScreen = "support_chat"
                                }
                            )
                        }
                    } else if (isUnderReview) {
                        if (currentScreen == "support_chat") {
                            SupportChatScreen(
                                reportId = activeSupportReportId,
                                onBack = { currentScreen = "home" }
                            )
                        } else {
                            com.example.ui.screens.AccountUnderReviewScreen(
                                onContactSupport = {
                                    currentScreen = "support_chat"
                                },
                                onReviewEnded = {
                                    authViewModel.checkAccountReview(applicationContext)
                                    currentScreen = "home"
                                }
                            )
                        }
                    } else if (deletionReceipt != null) {
                        com.example.ui.screens.profile.AccountDeletionReceiptScreen(
                            receipt = deletionReceipt!!,
                            authViewModel = authViewModel,
                            onDone = {
                                currentScreen = "welcome"
                            }
                        )
                    } else if (deletionStatus is com.example.DeletionStatusState.Pending || deletionStatus is com.example.DeletionStatusState.Failed) {
                        com.example.ui.screens.profile.AccountDeletionGateScreen(
                            state = deletionStatus,
                            authViewModel = authViewModel,
                            onLogout = {
                                authViewModel.logout(applicationContext)
                                currentScreen = "welcome"
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Crossfade(
                                targetState = currentScreen,
                                label = "screen_routing"
                            ) { screen ->
                        when (screen) {
                            "splash" -> SplashScreen()
                            "verify_error" -> VerifyAccountErrorScreen(
                                onRetry = {
                                    authViewModel.retryBootSessionVerification(applicationContext)
                                },
                                onLogout = {
                                    authViewModel.logout(applicationContext)
                                    currentScreen = "welcome"
                                },
                                onContactSupport = {
                                    currentScreen = "support_chat"
                                },
                                onClose = {
                                    authViewModel.logout(applicationContext)
                                    currentScreen = "welcome"
                                }
                            )
                            "welcome" -> WelcomeScreen(
                                onGetStartedClick = {
                                    currentScreen = "privacy"
                                }
                            )
                            "privacy" -> PrivacyScreen(
                                onContinueClick = {
                                    currentScreen = "auth"
                                },
                                onBackClick = {
                                    currentScreen = "welcome"
                                }
                            )
                            "restore_chat_backup" -> RestoreChatBackupScreen(
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
                                },
                                onBackClick = {
                                    currentScreen = "privacy"
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
                                    lifecycleScope.launch {
                                        com.example.data.StartupPrefsStore.markOnboardingDone(
                                            applicationContext,
                                            SupabaseSessionManager.getUserId(applicationContext)
                                        )
                                    }
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
                                openCreateRoomTrigger = openCreateRoomTrigger,
                                onConsumeCreateRoomTrigger = {
                                    openCreateRoomTrigger = false
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
                                },
                                onOpenHelpSupport = {
                                    currentScreen = "help_support"
                                },
                                onOpenFeedbackHub = { initialKind ->
                                    feedbackInitialCreateKind = initialKind
                                    currentScreen = "feedback_hub"
                                },
                                onDeleteAccountClick = {
                                    currentScreen = "account_deletion"
                                }
                            )
                            "help_support" -> HelpAndSupportScreen(
                                onBack = { currentScreen = "home" },
                                onOpenSupportChat = {
                                    activeSupportReportId = null
                                    currentScreen = "support_chat"
                                },
                                onOpenBugReport = {
                                    feedbackInitialCreateKind = "bug"
                                    currentScreen = "feedback_hub"
                                },
                                onOpenFeedbackHub = {
                                    feedbackInitialCreateKind = null
                                    currentScreen = "feedback_hub"
                                }
                            )
                            "feedback_hub" -> FeedbackHubScreen(
                                initialCreateKind = feedbackInitialCreateKind,
                                onConsumeInitialCreateKind = { feedbackInitialCreateKind = null },
                                onBack = { currentScreen = "home" }
                            )
                            "account_deletion" -> com.example.ui.screens.profile.AccountDeletionScreen(
                                authViewModel = authViewModel,
                                onBack = { currentScreen = "home" },
                                onDeletionScheduled = { /* Receipt screen renders via deletionReceipt */ }
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
                                onBack = {
                                    currentScreen = if (sessionVerificationState is SessionVerificationState.Failed) {
                                        "verify_error"
                                    } else {
                                        "home"
                                    }
                                }
                            )
                            "voice_room" -> VoiceRoomScreen(
                                viewModel = authViewModel,
                                onBack = {
                                    currentScreen = "home"
                                },
                                onRoomEnded = { peopleCount, durationMins ->
                                    endedRoomPeopleCount = peopleCount
                                    endedRoomTotalTime = durationMins
                                    currentScreen = "room_ended"
                                }
                            )
                            "room_ended" -> RoomEndedScreen(
                                peopleJoinedCount = endedRoomPeopleCount,
                                totalTimeMinutes = endedRoomTotalTime,
                                onCreateAnotherRoom = {
                                    openCreateRoomTrigger = true
                                    currentScreen = "home"
                                },
                                onBackToHome = {
                                    currentScreen = "home"
                                }
                            )
                        }
                    }

                    // Floating Bubble Overlay (Strictly in-app Side Dynamic Island when in active room outside VoiceRoomScreen)
                    val activeRoomId by authViewModel.activeRoomId.collectAsState()

                    LaunchedEffect(currentScreen, activeRoomId) {
                        // Keep the Side Dynamic Island strictly in-app while the Activity is active
                        com.example.service.VoiceRoomOverlayManager.stopService(this@MainActivity)
                    }

                    if (activeRoomId != null && currentScreen != "voice_room" && currentScreen != "splash") {
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

                    com.example.ui.components.PendingRoomInviteOverlay(
                        onJoinRoom = { roomId ->
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

                    if (com.example.ModerationManager.showModerationHubDialog) {
                        com.example.ui.screens.home.dialogs.ModerationReviewDialog(
                            onDismiss = {
                                com.example.ModerationManager.showModerationHubDialog = false
                                com.example.ModerationManager.pendingReportIdToOpen = null
                            },
                            initialReportId = com.example.ModerationManager.pendingReportIdToOpen
                        )
                    }

                    if (showSecurityResetDialog) {
                        val colors = com.example.ui.theme.LocalFunkyColors.current
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = {
                                LegacySecurityMigration.dismissSecurityResetDialog(applicationContext)
                                showSecurityResetDialog = false
                            },
                            containerColor = colors.surface,
                            titleContentColor = colors.ink,
                            textContentColor = colors.ink,
                            title = {
                                androidx.compose.material3.Text(
                                    text = "Chat Security Upgraded",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                )
                            },
                            text = {
                                androidx.compose.material3.Text(
                                    text = "We upgraded chat security. Your chats on this device were reset."
                                )
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        LegacySecurityMigration.dismissSecurityResetDialog(applicationContext)
                                        showSecurityResetDialog = false
                                    },
                                    modifier = Modifier.testTag("security_reset_ok_button")
                                ) {
                                    androidx.compose.material3.Text("OK", color = colors.mango)
                                }
                            },
                            modifier = Modifier.testTag("security_reset_dialog")
                        )
                    }

                    // Global Connectivity Banner (Offline / 2s Back online)
                    com.example.ui.components.GlobalConnectivityBanner(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .zIndex(200f)
                    )

                    // Global Loading HUD & No Internet Connection Dialogs
                    com.example.ui.components.AppLoadingHUDOverlay()

                    val showNoInternetDialog by com.example.ui.components.AppNetworkManager.showNoInternetDialog.collectAsState()
                    if (showNoInternetDialog) {
                        com.example.ui.components.AppNoInternetDialog(
                            onDismiss = { com.example.ui.components.AppNetworkManager.dismissNoInternetDialog() }
                        )
                    }

                    activeRevokedDialogMessage?.let { revokedMsg ->
                        val colors = com.example.ui.theme.LocalFunkyColors.current
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { activeRevokedDialogMessage = null },
                            containerColor = colors.surface,
                            titleContentColor = colors.ink,
                            textContentColor = colors.ink,
                            title = {
                                androidx.compose.material3.Text(
                                    text = "Signed out",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                androidx.compose.material3.Text(text = revokedMsg)
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = { activeRevokedDialogMessage = null },
                                    modifier = Modifier.testTag("device_revoked_ok_button")
                                ) {
                                    androidx.compose.material3.Text("OK", color = colors.mango, fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier.testTag("device_revoked_dialog")
                        )
                    }
                    com.example.diagnostics.ErrorReportBannerAndDialogHost(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(210f)
                    )
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        com.example.ui.components.SecureWindowRegistry.setActivityPaused(this, true)
    }

    override fun onResume() {
        super.onResume()
        com.example.ui.components.SecureWindowRegistry.setActivityPaused(this, false)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(!com.example.ui.components.SecureWindowRegistry.blockScreenshots.value)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
        handlePushNotificationIntent(intent)
    }

    private fun handlePushNotificationIntent(intent: Intent?) {
        if (intent == null) return

        val fromPush = intent.getBooleanExtra("from_push", false) ||
            intent.getStringExtra("from_push")?.equals("true", ignoreCase = true) == true
        val kind = intent.getStringExtra("kind")?.takeIf { it.isNotBlank() }
        val conversationId = intent.getStringExtra("conversation_id")?.takeIf { it.isNotBlank() }
        val groupId = intent.getStringExtra("group_id")?.takeIf { it.isNotBlank() }
        val senderId = intent.getStringExtra("sender_id")?.takeIf { it.isNotBlank() }
        val partnerUserId = intent.getStringExtra("partnerUserId")?.takeIf { it.isNotBlank() }
        val navigateTo = intent.getStringExtra("navigate_to")?.takeIf { it.isNotBlank() }

        val hasChatExtras = !kind.isNullOrBlank() ||
            !conversationId.isNullOrBlank() ||
            !groupId.isNullOrBlank() ||
            !senderId.isNullOrBlank() ||
            !partnerUserId.isNullOrBlank()

        if (kind == "staff_report" || navigateTo == "moderation_hub") {
            val targetReportId = intent.getStringExtra("target_report_id") ?: intent.getStringExtra("report_id")
            com.example.ModerationManager.pendingReportIdToOpen = targetReportId
            com.example.ModerationManager.showModerationHubDialog = true
            intent.removeExtra("from_push")
            intent.removeExtra("kind")
            intent.removeExtra("event")
            intent.removeExtra("report_id")
            intent.removeExtra("target_report_id")
            intent.removeExtra("navigate_to")
            return
        }

        if (kind == "support_chat" || navigateTo == "support_chat") {
            pendingNavigateTo = "support_chat"
            com.example.SupportChatManager.fetchSupportSession(applicationContext)
            intent.removeExtra("from_push")
            intent.removeExtra("kind")
            intent.removeExtra("navigate_to")
            return
        }

        if (fromPush || hasChatExtras || navigateTo == "direct_chat") {
            pendingNavigateTo = "home"
            com.example.service.PushNotificationManager.triggerInboxDrainOnTap(applicationContext)

            if (hasChatExtras) {
                val resolvedKind = kind ?: when {
                    !groupId.isNullOrBlank() -> "group"
                    else -> "dm"
                }
                com.example.service.PushNotificationManager.setPendingChatNavigation(
                    com.example.service.PendingChatNavigationTarget(
                        kind = resolvedKind,
                        conversationId = conversationId,
                        groupId = groupId,
                        senderId = senderId,
                        partnerUserId = partnerUserId ?: senderId
                    )
                )
            }

            intent.removeExtra("from_push")
            intent.removeExtra("kind")
            intent.removeExtra("conversation_id")
            intent.removeExtra("group_id")
            intent.removeExtra("sender_id")
            intent.removeExtra("partnerUserId")
            intent.removeExtra("navigate_to")
        } else if (!navigateTo.isNullOrBlank()) {
            pendingNavigateTo = navigateTo
            intent.removeExtra("navigate_to")
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

    override fun onDestroy() {
        if (isFinishing) {
            com.example.realtime.RoomScreenShareManager.stopSessionAsync(
                context = applicationContext,
                reason = "activity_destroyed"
            )
        }
        super.onDestroy()
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

@Composable
private fun UpdateRequiredGateScreen(
    onOpenUpdateUrl: () -> Unit
) {
    val funkyColors = com.example.ui.theme.LocalFunkyColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(funkyColors.mango.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_rocket),
                contentDescription = "Update required",
                tint = funkyColors.mango,
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "Update required",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = funkyColors.ink
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "A newer version of FunkyTalk is required to continue. Please update your app to get the latest fixes and security improvements.",
            fontSize = 14.5.sp,
            color = funkyColors.mute,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )
        Spacer(modifier = Modifier.height(26.dp))
        Button(
            onClick = onOpenUpdateUrl,
            colors = ButtonDefaults.buttonColors(
                containerColor = funkyColors.mango,
                contentColor = funkyColors.onMango
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("update_required_button")
        ) {
            Text(
                text = "Update App",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.5.sp
            )
        }
        if (BuildConfig.DEBUG && AppFlagsRepository.hasDebugOverrides()) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = { AppFlagsRepository.clearDebugOverrides() },
                modifier = Modifier.testTag("debug_reset_update_gate_button")
            ) {
                Text(
                    text = "Debug: Clear Flag Overrides",
                    color = funkyColors.mute
                )
            }
        }
    }
}
