package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.util.Log
import com.example.AuthState
import com.example.AuthViewModel
import com.example.SupabaseClient
import com.example.SupabaseEvent
import com.example.UserProfile
import com.example.realtime.VoiceRoomRealtimeManager
import com.example.realtime.RealtimeTableEvent
import com.example.util.CustomTabsHelper
import com.example.ui.screens.home.components.HomeBottomBar
import com.example.ui.screens.home.dialogs.*
import com.example.ui.screens.home.models.*
import com.example.ui.screens.home.tabs.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.BuildConfig
import com.example.SupabaseSessionManager
import com.example.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit,
    openCreateRoomTrigger: Boolean = false,
    onConsumeCreateRoomTrigger: () -> Unit = {},
    onRoomClick: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenSupportChat: (reportId: String?) -> Unit = {},
    onOpenHelpSupport: () -> Unit = {},
    onOpenFeedbackHub: (initialKind: String?) -> Unit = {},
    onDeleteAccountClick: () -> Unit = {}
) {
    val authState by viewModel.authState.collectAsState()
    val supabaseProfiles by viewModel.supabaseProfiles.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchSupabaseProfiles(context)
        com.example.NotificationsManager.init(context)
        com.example.SupportChatManager.init(context)
        com.example.repository.GroupChatRepository.init(context)
        val prefs = context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE)
        val savedCode = prefs.getString("pending_group_invite_code", null)
        if (!savedCode.isNullOrBlank() && com.example.repository.GroupChatRepository.pendingInviteCode.value.isNullOrBlank()) {
            com.example.repository.GroupChatRepository.setPendingInviteCode(savedCode)
        }
    }

    var showCreateRoomBottomSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var myModerationStatus by remember { mutableStateOf<com.example.MyModerationStatus?>(null) }
    val homeScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        myModerationStatus = com.example.ModerationManager.fetchMyModerationStatus(context)
    }

    LaunchedEffect(openCreateRoomTrigger) {
        if (openCreateRoomTrigger) {
            showCreateRoomBottomSheet = true
            onConsumeCreateRoomTrigger()
        }
    }

    // Join Code Dialog States
    var showJoinCodeDialog by remember { mutableStateOf(false) }
    var joinCodeInputText by remember { mutableStateOf("") }
    var joinCodeLoading by remember { mutableStateOf(false) }

    // Dynamic state trackers
    var activeTab by remember { mutableStateOf("home") } // "home", "rooms", "moments", "chats", "profile"
    var hideBottomBarForMomentsOverlay by remember { mutableStateOf(false) }
    var hideBottomBarForDiscoverOverlay by remember { mutableStateOf(false) }
    var hideBottomBarForChatsOverlay by remember { mutableStateOf(false) }
    var hideBottomBarForProfileOverlay by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLanguageFilters by remember { mutableStateOf<Set<String>>(emptySet()) }
    var filterSpeakOrLearnMode by remember { mutableStateOf("They speak") }

    // Advanced search filter states
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterOnlyLive by remember { mutableStateOf(false) }
    var filterByLevel by remember { mutableStateOf("All") }
    var selectedSortOption by remember { mutableStateOf("Popular") }
    
    // Remote events / Amusements
    var showAmusementEventsDialog by remember { mutableStateOf(false) }
    var selectedAmusementEvent by remember { mutableStateOf<SupabaseEvent?>(null) }

    // Live Leaderboard & Special Offers Spin UI
    var showLeaderboardScreen by remember { mutableStateOf(false) }
    var showSpinOfferDialog by remember { mutableStateOf(false) }
    var inspectedLeaderboardEntry by remember { mutableStateOf<com.example.leaderboard.LeaderboardEntry?>(null) }

    // Direct Chat active messages thread dialog
    var activeChatWindowContact by remember { mutableStateOf<DirectChat?>(null) }
    val chatMessagesStorage = remember { mutableStateMapOf<String, List<Pair<String, Boolean>>>() } // Pair: text, isMe

    LaunchedEffect(authState) {
        if (viewModel.deletionReceipt.value != null) {
            return@LaunchedEffect
        }
        if (authState !is AuthState.Success) {
            onLogoutSuccess()
        }
    }

    val currentUserProfile = remember(authState) {
        (authState as? AuthState.Success)?.user ?: UserProfile(
            uid = "guest",
            email = "learner@funkytalk.com",
            displayName = "Batto",
            isEmailVerified = true,
            username = "batto_learner",
            avatar = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
            nativeLanguage = "Hindi",
            learningLanguage = "English"
        )
    }

    // Dynamic Rooms & Friends state hydrated synchronously from AppCacheStore (cache-first SWR)
    val cachedHomeFeed = remember(currentUserProfile.uid) {
        com.example.ui.screens.home.HomeFeedLoader.loadCached(context)
    }
    var activeFriends by remember { mutableStateOf<List<FollowedFriend>>(cachedHomeFeed?.friends.orEmpty()) }
    var activeEventsList by remember { mutableStateOf<List<SupabaseEvent>>(cachedHomeFeed?.events.orEmpty()) }
    val blockedUserIds by com.example.repository.BlockRepository.blockedUserIds.collectAsState()

    val activeFriendsList = remember(activeFriends, supabaseProfiles, blockedUserIds) {
        val mapped = if (activeFriends.isNotEmpty()) {
            activeFriends
                .filterNot { blockedUserIds.contains(it.id) }
                .map { f ->
                    val p = supabaseProfiles.find { it.uid == f.id }
                    if (p != null) {
                        f.copy(
                            name = p.displayName ?: p.username ?: f.name,
                            avatarUrl = p.avatar ?: f.avatarUrl,
                            flagCode = p.countryCode ?: f.flagCode
                        )
                    } else {
                        f
                    }
                }
        } else {
            activeFriends.filterNot { blockedUserIds.contains(it.id) }
        }
        // Place genuine online users first, followed by offline users with moon badges
        mapped.sortedByDescending { it.isOnline }
    }

    var roomsList by remember { mutableStateOf<List<CustomRoom>>(cachedHomeFeed?.rooms.orEmpty()) }
    val visibleRoomsList = remember(roomsList, blockedUserIds) {
        roomsList.filterNot { blockedUserIds.contains(it.hostId) }
    }
    var homeUiState by remember {
        mutableStateOf<com.example.util.UiState<Unit>>(
            if (cachedHomeFeed != null && (cachedHomeFeed.rooms.isNotEmpty() || cachedHomeFeed.friends.isNotEmpty())) {
                com.example.util.UiState.Content(Unit, isRefreshing = true, isFromCache = true)
            } else {
                com.example.util.UiState.Loading
            }
        )
    }
    var homeLoadError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    val isOnline by com.example.ui.components.AppNetworkManager.isOnline.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    fun isNetworkOfflineError(t: Throwable): Boolean {
        var cur: Throwable? = t
        while (cur != null) {
            if (cur is java.net.UnknownHostException ||
                cur is java.net.SocketTimeoutException ||
                cur is java.net.ConnectException ||
                cur is java.io.IOException
            ) {
                return true
            }
            cur = cur.cause
        }
        return false
    }

    val currentUserId = currentUserProfile.uid
    LaunchedEffect(currentUserId) {
        com.example.leaderboard.LeaderboardRepository.recordLocalAppLogin(context, currentUserId)
    }

    LaunchedEffect(currentUserId, supabaseProfiles, retryTrigger) {
        VoiceRoomRealtimeManager.start()

        suspend fun refreshRoomsAndFriends() {
            if (roomsList.isEmpty() && homeUiState !is com.example.util.UiState.Content) {
                homeUiState = com.example.util.UiState.Loading
            }
            com.example.ui.screens.home.HomeFeedLoader.fetchFresh(
                context = context,
                currentUserId = currentUserId,
                supabaseProfiles = supabaseProfiles
            ).onSuccess { fresh ->
                roomsList = fresh.rooms
                if (fresh.friends.isNotEmpty() || supabaseProfiles.isNotEmpty()) {
                    activeFriends = fresh.friends
                }
                activeEventsList = fresh.events
                homeLoadError = null
                homeUiState = if (fresh.rooms.isEmpty()) {
                    com.example.util.UiState.Empty(
                        title = "No live rooms right now",
                        subtitle = "Be the first to start a voice circle!",
                        actionLabel = "Create Room"
                    )
                } else {
                    com.example.util.UiState.Content(Unit, isRefreshing = false, isFromCache = false)
                }
            }.onFailure { err ->
                val friendly = com.example.util.ErrorMapper.map(err)
                homeLoadError = friendly
                if (roomsList.isEmpty()) {
                    homeUiState = com.example.util.UiState.Error(friendly, canRetry = true)
                } else {
                    homeUiState = com.example.util.UiState.Content(Unit, isRefreshing = false, isFromCache = true)
                }
            }
        }

        refreshRoomsAndFriends()

        // Background polling fallback every 8 seconds only while UI is foregrounded (STARTED)
        launch {
            while (isActive) {
                delay(8000)
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    try {
                        refreshRoomsAndFriends()
                    } catch (ce: kotlinx.coroutines.CancellationException) {
                        throw ce
                    } catch (_: Throwable) {}
                }
            }
        }

        // Real-time updates from Supabase postgres_changes, live participants, and room broadcasts
        launch {
            try {
                VoiceRoomRealtimeManager.events.collect { event ->
                    if (event is RealtimeTableEvent.VoiceRoomChanged) {
                        val record = event.record
                        val rId = record?.optString("id")
                        val isLive = record?.optBoolean("is_live", true) ?: true
                        val status = record?.optString("status") ?: "active"
                        if (!rId.isNullOrBlank() && (!isLive || status == "ended" || event.eventType.equals("DELETE", ignoreCase = true))) {
                            roomsList = roomsList.filterNot { it.id == rId }
                        }
                    } else if (event is RealtimeTableEvent.RoomLifecycleEvent && event.action == "room_ended") {
                        roomsList = roomsList.filterNot { it.id == event.roomId }
                    }
                    refreshRoomsAndFriends()
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (_: Throwable) {}
        }
        launch {
            try {
                VoiceRoomRealtimeManager.roomLiveParticipants.collect {
                    refreshRoomsAndFriends()
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (_: Throwable) {}
        }
    }

    // Real conversations list - user-initiated contacts from Discover or Empty
    val dynamicInitiatedChats = remember { mutableStateListOf<DirectChat>() }
    var activeGroupChat by remember { mutableStateOf<com.example.db.LocalGroupConversation?>(null) }
    var inspectedFriendProfile by remember { mutableStateOf<FollowedFriend?>(null) }

    val pendingChatTarget by com.example.service.PushNotificationManager.pendingChatNavigation.collectAsState()
    val vaultStatus by com.example.crypto.VaultSecurityManager.vaultStatus.collectAsState()

    var showVaultGateDialog by remember { mutableStateOf(false) }
    var pendingPostUnlockAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(vaultStatus) {
        if (vaultStatus == com.example.crypto.VaultStatus.UNLOCKED && showVaultGateDialog) {
            showVaultGateDialog = false
            val action = pendingPostUnlockAction
            pendingPostUnlockAction = null
            action?.invoke()
        }
    }

    fun requireUnlockedVault(action: () -> Unit) {
        if (!com.example.crypto.VaultSecurityManager.isVaultLocked() &&
            vaultStatus == com.example.crypto.VaultStatus.UNLOCKED
        ) {
            action()
        } else {
            pendingPostUnlockAction = action
            showVaultGateDialog = true
        }
    }

    fun startDirectChat(userId: String, name: String, avatarUrl: String, flagCode: String) {
        requireUnlockedVault {
            val newChat = DirectChat(
                id = userId.ifBlank { name.hashCode().toString() },
                name = name,
                avatar = avatarUrl,
                lastMsg = "🔒 Secure chat started",
                time = "Just now",
                unreadCount = 0,
                onlineStatus = "online",
                isNativeLanguage = flagCode
            )
            if (dynamicInitiatedChats.none { it.id == newChat.id }) {
                dynamicInitiatedChats.add(0, newChat)
            }
            activeChatWindowContact = newChat

            if (userId.isNotBlank() && !userId.startsWith("partner_mock")) {
                homeScope.launch {
                    try {
                        com.example.repository.EncryptedChatRepository.init(context)
                        val ready = com.example.repository.EncryptedChatRepository.checkRecipientKeyStatus(context, userId)
                        if (ready) {
                            val res = com.example.repository.EncryptedChatRepository.sendMessage(
                                context = context,
                                recipientUserId = userId,
                                recipientName = name,
                                recipientAvatar = avatarUrl,
                                text = "Hi"
                            )
                            if (res.isFailure) {
                                android.util.Log.w("HomeScreen", "Wave deferred: ${res.exceptionOrNull()?.message}")
                            }
                        } else {
                            android.util.Log.d("HomeScreen", "E2EE keys missing, Secure Chat Unavailable flow active.")
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        android.util.Log.e("HomeScreen", "Error waving to user: ${e.javaClass.simpleName}", e)
                    }
                }
            }
        }
    }

    // Handle push notification tap routing to DM or Group chat (with Vault unlock gate & inbox drain)
    LaunchedEffect(pendingChatTarget, vaultStatus) {
        val target = pendingChatTarget ?: return@LaunchedEffect

        // Always switch to the Chats tab first
        activeTab = "chats"

        // If the vault is locked (or not configured), keep chat overlays closed so the Vault unlock screen is displayed first
        if (com.example.crypto.VaultSecurityManager.isVaultLocked() ||
            vaultStatus != com.example.crypto.VaultStatus.UNLOCKED
        ) {
            activeChatWindowContact = null
            activeGroupChat = null
            return@LaunchedEffect
        }

        // Vault is unlocked -> ensure repositories are initialized and drain inbox before resolving target chat
        try {
            com.example.repository.EncryptedChatRepository.init(context)
            com.example.repository.GroupChatRepository.init(context)
            com.example.repository.EncryptedChatRepository.drainPendingDmMessages(context)
            com.example.repository.GroupChatRepository.drainPendingGroupMessages(context)
        } catch (e: Exception) {
            Log.w("HomeScreen", "Error draining inbox during push navigation: ${e.message}")
        }

        val isGroupTarget = target.kind.equals("group", ignoreCase = true) || !target.groupId.isNullOrBlank()
        if (isGroupTarget) {
            val gid = target.groupId
            if (!gid.isNullOrBlank()) {
                val joinedGroups = com.example.repository.GroupChatRepository.joinedGroups.value
                val allGroups = com.example.repository.GroupChatRepository.allMyGroups.value
                val resolvedGroup = joinedGroups.find { it.id == gid }
                    ?: allGroups.find { it.id == gid }
                    ?: com.example.db.LocalGroupConversation(
                        id = gid,
                        name = "Group Chat",
                        myRole = "member",
                        myStatus = "joined"
                    )
                activeChatWindowContact = null
                activeGroupChat = resolvedGroup
                com.example.service.PushNotificationManager.cancelNotificationForChat(context, groupId = gid)
            }
            com.example.service.PushNotificationManager.clearPendingChatNavigation()
        } else {
            val dbConvs = com.example.repository.EncryptedChatRepository.conversations.value
            val convByConvId = target.conversationId?.takeIf { it.isNotBlank() }?.let { cid ->
                dbConvs.find { it.id == cid }
            }
            val partnerId = (target.partnerUserId ?: target.senderId ?: convByConvId?.otherUserId)?.takeIf { it.isNotBlank() }
            if (!partnerId.isNullOrBlank()) {
                val conv = convByConvId ?: dbConvs.find { it.otherUserId == partnerId }
                val supaPartner = supabaseProfiles.find { it.uid == partnerId }
                val partnerName = conv?.otherUserName?.takeIf { it.isNotBlank() }
                    ?: supaPartner?.displayName?.takeIf { it.isNotBlank() }
                    ?: supaPartner?.username?.takeIf { it.isNotBlank() }
                    ?: "Chat"
                val partnerAvatar = conv?.otherUserAvatar?.takeIf { it.isNotBlank() }
                    ?: supaPartner?.avatar?.takeIf { it.isNotBlank() }
                    ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200"
                val partnerLang = conv?.otherUserFlag?.takeIf { it.isNotBlank() }
                    ?: supaPartner?.countryCode?.takeIf { it.isNotBlank() }
                    ?: "English"

                val directChat = DirectChat(
                    id = partnerId,
                    name = partnerName,
                    avatar = partnerAvatar,
                    lastMsg = conv?.lastMessageText?.ifBlank { "🔒 Encrypted conversation" } ?: "🔒 Encrypted conversation",
                    time = "Just now",
                    unreadCount = 0,
                    onlineStatus = "online",
                    isNativeLanguage = partnerLang
                )
                if (dynamicInitiatedChats.none { it.id == directChat.id }) {
                    dynamicInitiatedChats.add(0, directChat)
                }
                activeGroupChat = null
                activeChatWindowContact = directChat
                com.example.service.PushNotificationManager.cancelNotificationForChat(
                    context = context,
                    conversationId = target.conversationId ?: conv?.id,
                    partnerUserId = partnerId
                )
            }
            com.example.service.PushNotificationManager.clearPendingChatNavigation()
        }
    }

    // Dialogs
    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout(context)
            }
        )
    }

    fun handleJoinEvent(event: SupabaseEvent) {
        selectedAmusementEvent = null
        showAmusementEventsDialog = false
        if (!event.join_room_id.isNullOrBlank()) {
            Toast.makeText(context, "Entering ${event.title}...", Toast.LENGTH_SHORT).show()
            viewModel.joinRoom(event.join_room_id, event.title, context)
            onRoomClick()
        } else if (!event.join_url.isNullOrBlank()) {
            CustomTabsHelper.openUrl(context, event.join_url)
        } else {
            Toast.makeText(context, "No voice room or link available for this event.", Toast.LENGTH_SHORT).show()
        }
    }

    if (showAmusementEventsDialog) {
        AmusementEventsDialog(
            events = activeEventsList,
            onDismiss = { showAmusementEventsDialog = false },
            onSelectEvent = { event ->
                selectedAmusementEvent = event
            },
            onJoinEventDirectly = { event ->
                handleJoinEvent(event)
            }
        )
    }

    selectedAmusementEvent?.let { event ->
        EventDetailsDialog(
            event = event,
            onDismiss = { selectedAmusementEvent = null },
            onJoinEvent = {
                handleJoinEvent(event)
            }
        )
    }

    val dmUnreadCount by com.example.repository.EncryptedChatRepository.totalUnreadCount.collectAsState()
    val joinedGroupsForBadge by com.example.repository.GroupChatRepository.joinedGroups.collectAsState()
    val totalUnreadChatsBadge = remember(dmUnreadCount, joinedGroupsForBadge) {
        dmUnreadCount + joinedGroupsForBadge.sumOf { it.unreadCount.coerceAtLeast(0) }
    }
    val supportUnreadCount by com.example.SupportChatManager.unreadCount.collectAsState()
    val feedbackUpdatesCount by com.example.repository.FeedbackRepository.myUpdatesCount.collectAsState()
    val profileAlertBadgeCount = remember(supportUnreadCount, feedbackUpdatesCount) {
        supportUnreadCount.coerceAtLeast(0) + feedbackUpdatesCount.coerceAtLeast(0)
    }

    val shouldHideBottomBar = remember(
        activeTab,
        hideBottomBarForMomentsOverlay,
        hideBottomBarForDiscoverOverlay,
        hideBottomBarForChatsOverlay,
        hideBottomBarForProfileOverlay,
        activeChatWindowContact,
        activeGroupChat,
        inspectedFriendProfile,
        inspectedLeaderboardEntry,
        showLeaderboardScreen,
        showSpinOfferDialog,
        showCreateRoomBottomSheet,
        showFilterDialog,
        showAmusementEventsDialog,
        selectedAmusementEvent,
        showJoinCodeDialog
    ) {
        val tabOverlayOpen = when (activeTab) {
            "moments" -> hideBottomBarForMomentsOverlay
            "discover", "rooms" -> hideBottomBarForDiscoverOverlay
            "chats" -> hideBottomBarForChatsOverlay
            "profile" -> hideBottomBarForProfileOverlay
            else -> false
        }
        tabOverlayOpen ||
            activeChatWindowContact != null ||
            activeGroupChat != null ||
            inspectedFriendProfile != null ||
            inspectedLeaderboardEntry != null ||
            showLeaderboardScreen ||
            showSpinOfferDialog ||
            showCreateRoomBottomSheet ||
            showFilterDialog ||
            showAmusementEventsDialog ||
            selectedAmusementEvent != null ||
            showJoinCodeDialog
    }

    val funkyColors = com.example.ui.theme.LocalFunkyColors.current

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = funkyColors.bg,
            bottomBar = {
                if (!shouldHideBottomBar) {
                    HomeBottomBar(
                        activeTab = activeTab,
                        unreadChatCount = totalUnreadChatsBadge,
                        profileBadgeCount = profileAlertBadgeCount,
                        onTabSelected = { activeTab = it }
                    )
                }
            }
        ) { innerPadding ->
            val contentModifier = if (shouldHideBottomBar) {
                Modifier
            } else if (activeTab == "moments") {
                Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            } else {
                Modifier.padding(innerPadding)
            }
            Crossfade(
                targetState = activeTab,
                label = "HomeSubNavigation",
                modifier = contentModifier
            ) { currentTab ->
                when (currentTab) {
                    "home" -> {
                        HomeTabContent(
                            currentUserProfile = currentUserProfile,
                            activeFriendsList = activeFriendsList,
                            roomsList = visibleRoomsList,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedLanguageFilters = selectedLanguageFilters,
                            onLanguageSelected = { lang ->
                                selectedLanguageFilters = if (lang == "All") emptySet() else setOf(lang)
                            },
                            filterSpeakOrLearnMode = filterSpeakOrLearnMode,
                            filterOnlyLive = filterOnlyLive,
                            filterByLevel = filterByLevel,
                            selectedSortOption = selectedSortOption,
                            hasActiveEvents = activeEventsList.isNotEmpty(),
                            onOpenEvents = {
                                showAmusementEventsDialog = true
                            },
                            onOpenNotifications = onOpenNotifications,
                            onOpenLeaderboard = { showLeaderboardScreen = true },
                            onOpenSpinOffers = { showSpinOfferDialog = true },
                            onCreateRoom = {
                                if (!isOnline) {
                                    Toast.makeText(context, com.example.util.ErrorMapper.MSG_OFFLINE, Toast.LENGTH_SHORT).show()
                                } else {
                                    showCreateRoomBottomSheet = true
                                }
                            },
                            onJoinWithCode = {
                                if (!isOnline) {
                                    Toast.makeText(context, com.example.util.ErrorMapper.MSG_OFFLINE, Toast.LENGTH_SHORT).show()
                                } else {
                                    showJoinCodeDialog = true
                                }
                            },
                            onOpenFilter = { showFilterDialog = true },
                            onJoinRoom = { room ->
                                if (!isOnline) {
                                    Toast.makeText(context, com.example.util.ErrorMapper.MSG_OFFLINE, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Welcome to ${room.title}! Connecting audio...", Toast.LENGTH_LONG).show()
                                    val myRole = if (room.hostId == currentUserId) "speaker" else "listener"
                                    viewModel.joinRoom(room.id, room.title, context, role = myRole, hostId = room.hostId)
                                    onRoomClick()
                                }
                            },
                            homeUiState = homeUiState,
                            isOnline = isOnline,
                            homeLoadError = homeLoadError,
                            onRetryLoad = {
                                homeLoadError = null
                                if (roomsList.isEmpty()) {
                                    homeUiState = com.example.util.UiState.Loading
                                }
                                retryTrigger++
                            },
                            onFriendClick = { friend ->
                                if (friend.id.isNotBlank()) {
                                    inspectedFriendProfile = friend
                                }
                            }
                        )
                    }
                    "discover", "rooms" -> {
                        DiscoverTabContent(
                            supabaseProfiles = supabaseProfiles,
                            onCreateRoom = { showCreateRoomBottomSheet = true },
                            onStartDirectChat = ::startDirectChat,
                            onOverlayVisibilityChange = { hideBottomBarForDiscoverOverlay = it }
                        )
                    }
                    "moments" -> {
                        MomentsTabContent(
                            currentUserNativeLanguage = currentUserProfile.nativeLanguage.orEmpty(),
                            currentUserId = currentUserId,
                            currentUserName = currentUserProfile.displayName ?: currentUserProfile.username.orEmpty(),
                            currentUserAvatarUrl = currentUserProfile.avatar.orEmpty(),
                            currentUserCountryCode = currentUserProfile.countryCode.orEmpty(),
                            onCreatePostVisibilityChange = { hideBottomBarForMomentsOverlay = it },
                            onStartDirectChat = ::startDirectChat
                        )
                    }
                    "chats" -> {
                        ChatsTabContent(
                            conversationsList = dynamicInitiatedChats,
                            supabaseProfiles = supabaseProfiles,
                            onSelectChat = { chat ->
                                requireUnlockedVault {
                                    activeChatWindowContact = chat
                                }
                            },
                            onOpenSupportChat = onOpenSupportChat,
                            onNavigateToDiscover = { activeTab = "rooms" },
                            onOpenGroupChat = { grp ->
                                activeGroupChat = grp
                            },
                            onOverlayVisibilityChange = { hideBottomBarForChatsOverlay = it }
                        )
                    }
                    "profile" -> {
                        ProfileTabContent(
                            currentUserProfile = currentUserProfile,
                            onLogoutClick = { showLogoutDialog = true },
                            viewModel = viewModel,
                            onOpenSupportChat = onOpenSupportChat,
                            onOpenHelpSupport = onOpenHelpSupport,
                            onOpenFeedbackHub = onOpenFeedbackHub,
                            onDeleteAccountClick = onDeleteAccountClick,
                            onOverlayVisibilityChange = { hideBottomBarForProfileOverlay = it }
                        )
                    }
                }
            }
        }

        activeChatWindowContact?.let { contact ->
            val currentThread = chatMessagesStorage[contact.id] ?: emptyList()
            DirectChatDialog(
                contact = contact,
                messages = currentThread,
                onDismiss = { activeChatWindowContact = null },
                onSendMessage = { text ->
                    val updated = currentThread + (text to true)
                    chatMessagesStorage[contact.id] = updated
                }
            )
        }

        activeGroupChat?.let { grp ->
            com.example.ui.screens.chat.GroupFlowContainer(
                initialGroup = grp,
                onCloseGroup = { activeGroupChat = null }
            )
        }

        val pendingInviteCode by com.example.repository.GroupChatRepository.pendingInviteCode.collectAsState()
        val joinedGroupsList by com.example.repository.GroupChatRepository.joinedGroups.collectAsState()
        pendingInviteCode?.let { code ->
            com.example.ui.screens.chat.GroupInviteCodePreviewSheet(
                inviteCode = code,
                onDismiss = {
                    com.example.repository.GroupChatRepository.setPendingInviteCode(null)
                    context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .remove("pending_group_invite_code")
                        .apply()
                },
                onJoinedGroup = { joinedGroupId ->
                    com.example.repository.GroupChatRepository.setPendingInviteCode(null)
                    context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .remove("pending_group_invite_code")
                        .apply()
                    activeTab = "chats"
                    val found = joinedGroupsList.find { it.id == joinedGroupId }
                        ?: com.example.db.LocalGroupConversation(
                            id = joinedGroupId,
                            name = "Group Chat",
                            myRole = "member",
                            myStatus = "joined"
                        )
                    activeGroupChat = found
                }
            )
        }

        inspectedFriendProfile?.let { friend ->
            val matchedProfile = supabaseProfiles.find { it.uid == friend.id }
            val resolvedName = matchedProfile?.displayName?.takeIf { it.isNotBlank() } ?: friend.name
            val resolvedAvatar = matchedProfile?.avatar?.takeIf { it.isNotBlank() } ?: friend.avatarUrl
            val resolvedFlag = matchedProfile?.countryCode?.takeIf { it.isNotBlank() } ?: friend.flagCode
            UserProfileFeedScreen(
                userId = friend.id,
                username = matchedProfile?.username ?: friend.name,
                profileName = resolvedName,
                avatarUrl = resolvedAvatar,
                coverUrl = matchedProfile?.coverUrl.orEmpty(),
                countryCode = resolvedFlag,
                nativeLanguages = matchedProfile?.nativeLanguage.orEmpty(),
                learningLanguages = matchedProfile?.learningLanguage.orEmpty(),
                hobbies = matchedProfile?.hobbies ?: emptyList(),
                bio = matchedProfile?.bio.orEmpty(),
                onBack = { inspectedFriendProfile = null },
                onStartChat = {
                    inspectedFriendProfile = null
                    startDirectChat(friend.id, resolvedName, resolvedAvatar, resolvedFlag)
                }
            )
        }

        if (showLeaderboardScreen) {
            com.example.ui.screens.leaderboard.LeaderboardScreen(
                supabaseProfiles = supabaseProfiles,
                onBack = { showLeaderboardScreen = false },
                onOpenUserProfile = { entry ->
                    inspectedLeaderboardEntry = entry
                }
            )
        }

        inspectedLeaderboardEntry?.let { entry ->
            UserProfileFeedScreen(
                userId = entry.userId,
                username = entry.username.ifBlank { entry.displayName },
                profileName = entry.displayName,
                avatarUrl = entry.avatarUrl,
                coverUrl = entry.coverUrl,
                countryCode = entry.countryCode,
                nativeLanguages = entry.nativeLanguages.joinToString(", "),
                learningLanguages = entry.learningLanguages.joinToString(", "),
                hobbies = entry.hobbies,
                bio = entry.bio,
                onBack = { inspectedLeaderboardEntry = null },
                onStartChat = {
                    inspectedLeaderboardEntry = null
                    showLeaderboardScreen = false
                    startDirectChat(entry.userId, entry.displayName, entry.avatarUrl, entry.countryCode)
                }
            )
        }

        if (showSpinOfferDialog) {
            com.example.ui.screens.store.SpinOfferWheelDialog(
                onDismiss = { showSpinOfferDialog = false }
            )
        }

        val hasHomeOverlayOpen = selectedAmusementEvent != null ||
            showAmusementEventsDialog ||
            showFilterDialog ||
            showJoinCodeDialog ||
            showCreateRoomBottomSheet ||
            showLogoutDialog ||
            showVaultGateDialog ||
            pendingInviteCode != null ||
            inspectedFriendProfile != null ||
            inspectedLeaderboardEntry != null ||
            showLeaderboardScreen ||
            showSpinOfferDialog ||
            activeGroupChat != null ||
            activeChatWindowContact != null

        BackHandler(enabled = hasHomeOverlayOpen || activeTab != "home") {
            when {
                selectedAmusementEvent != null -> selectedAmusementEvent = null
                showAmusementEventsDialog -> showAmusementEventsDialog = false
                showFilterDialog -> showFilterDialog = false
                showJoinCodeDialog -> {
                    showJoinCodeDialog = false
                    joinCodeInputText = ""
                }
                showCreateRoomBottomSheet -> showCreateRoomBottomSheet = false
                showLogoutDialog -> showLogoutDialog = false
                showVaultGateDialog -> {
                    showVaultGateDialog = false
                    pendingPostUnlockAction = null
                }
                pendingInviteCode != null -> {
                    com.example.repository.GroupChatRepository.setPendingInviteCode(null)
                    context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .remove("pending_group_invite_code")
                        .apply()
                }
                inspectedLeaderboardEntry != null -> inspectedLeaderboardEntry = null
                showLeaderboardScreen -> showLeaderboardScreen = false
                showSpinOfferDialog -> showSpinOfferDialog = false
                inspectedFriendProfile != null -> inspectedFriendProfile = null
                activeGroupChat != null -> activeGroupChat = null
                activeChatWindowContact != null -> activeChatWindowContact = null
                activeTab != "home" -> activeTab = "home"
            }
        }
    }

    if (showVaultGateDialog) {
        com.example.ui.screens.chat.VaultManagementDialogs(
            onDismissRequest = {
                showVaultGateDialog = false
                pendingPostUnlockAction = null
            }
        )
    }

    val isAccountSuspended = myModerationStatus?.action_type == "suspend" &&
        com.example.ModerationManager.isActionActive(myModerationStatus)

    // Modal Sheet to Create Language / Private Rooms
    if (showCreateRoomBottomSheet) {
        CreateRoomBottomSheet(
            viewModel = viewModel,
            isSuspended = isAccountSuspended,
            onDismiss = { showCreateRoomBottomSheet = false },
            onRoomCreated = {
                showCreateRoomBottomSheet = false
                onRoomClick()
            }
        )
    }

    // Active Suspension Alert on Home Screen
    if (isAccountSuspended) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss without resolving */ },
            containerColor = funkyColors.surface,
            icon = {
                Icon(
                    painter = painterResource(id = com.example.R.drawable.ic_alert),
                    contentDescription = "Suspended",
                    tint = funkyColors.danger,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Account Suspended",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = funkyColors.danger
                )
            },
            text = {
                Column {
                    Text(
                        text = "Your account has been placed under active suspension by administrators for violating community safety policies.",
                        fontSize = 13.sp,
                        color = funkyColors.ink
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = funkyColors.danger.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Reason: ${myModerationStatus?.reason ?: "Violation of Community Guidelines"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = funkyColors.danger
                            )
                            if (!myModerationStatus?.expires_at.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Suspension expires: ${myModerationStatus?.expires_at}",
                                    fontSize = 12.sp,
                                    color = funkyColors.danger.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        homeScope.launch {
                            myModerationStatus = com.example.ModerationManager.fetchMyModerationStatus(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = funkyColors.danger)
                ) {
                    Text("Refresh Status", color = Color.White)
                }
            }
        )
    }

    if (showFilterDialog) {
        ModalBottomSheet(
            onDismissRequest = { showFilterDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = funkyColors.bg,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            com.example.ui.screens.voiceroom.FilterRoomsSheetContent(
                initialSortBy = selectedSortOption,
                initialLanguages = selectedLanguageFilters,
                initialLevel = filterByLevel,
                initialSpeakOrLearnMode = filterSpeakOrLearnMode,
                onApply = { sortBy, langs, lvl, mode ->
                    selectedSortOption = sortBy
                    selectedLanguageFilters = langs
                    filterByLevel = lvl
                    filterSpeakOrLearnMode = mode
                    showFilterDialog = false
                },
                onReset = {
                    selectedSortOption = "Popular"
                    selectedLanguageFilters = emptySet()
                    filterByLevel = "All Levels"
                    filterSpeakOrLearnMode = "They speak"
                    showFilterDialog = false
                }
            )
        }
    }

    if (showJoinCodeDialog) {
        val coroutineScope = rememberCoroutineScope()
        JoinPrivateRoomDialog(
            onDismiss = {
                showJoinCodeDialog = false
                joinCodeInputText = ""
            },
            isLoading = joinCodeLoading,
            onJoin = { enteredPasscode ->
                joinCodeLoading = true
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val token = SupabaseSessionManager.getBearerToken(context)
                        val response = SupabaseClient.api.joinRoomByCode(
                            auth = token,
                            body = mapOf("entered_code" to enteredPasscode)
                        )
                        val rawBody = response.body()?.string()?.trim() ?: ""
                        if (BuildConfig.DEBUG) {
                            android.util.Log.d("JoinRoomByCode", "RPC response status: ${response.code()}")
                        }

                        var targetRoomId: String? = null
                        var targetRoomTitle: String? = null
                        var targetHostId: String? = null

                        if (response.isSuccessful && rawBody.isNotBlank() && rawBody != "null") {
                            if (rawBody.startsWith("{")) {
                                val json = org.json.JSONObject(rawBody)
                                targetRoomId = json.optString("room_id").ifBlank { json.optString("id") }
                                targetRoomTitle = json.optString("title").ifBlank { "Voice Room" }
                                targetHostId = json.optString("host_id").ifBlank { null }
                            } else if (rawBody.startsWith("[")) {
                                val array = org.json.JSONArray(rawBody)
                                if (array.length() > 0) {
                                    val json = array.getJSONObject(0)
                                    targetRoomId = json.optString("room_id").ifBlank { json.optString("id") }
                                    targetRoomTitle = json.optString("title").ifBlank { "Voice Room" }
                                    targetHostId = json.optString("host_id").ifBlank { null }
                                }
                            }
                        }

                        withContext(Dispatchers.Main) {
                            joinCodeLoading = false
                            if (!targetRoomId.isNullOrBlank()) {
                                val finalTitle = targetRoomTitle ?: "Private Voice Room"
                                showJoinCodeDialog = false
                                joinCodeInputText = ""
                                Toast.makeText(context, "Entering $finalTitle...", Toast.LENGTH_SHORT).show()
                                viewModel.joinRoom(targetRoomId, finalTitle, context, hostId = targetHostId)
                                onRoomClick()
                            } else {
                                Toast.makeText(context, "Invalid or expired room passcode.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        android.util.Log.e("JoinRoomByCode", "Error joining room by code", e)
                        withContext(Dispatchers.Main) {
                            joinCodeLoading = false
                            Toast.makeText(context, "Invalid or expired room passcode.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }
}
