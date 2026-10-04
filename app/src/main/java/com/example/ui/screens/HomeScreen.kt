package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
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
    onCreateLanguageRoom: (isPrivate: Boolean) -> Unit = {},
    onRoomClick: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenSupportChat: (reportId: String?) -> Unit = {}
) {
    val authState by viewModel.authState.collectAsState()
    val supabaseProfiles by viewModel.supabaseProfiles.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchSupabaseProfiles(context)
        com.example.NotificationsManager.init(context)
        com.example.SupportChatManager.init(context)
    }

    var showCreateRoomBottomSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var myModerationStatus by remember { mutableStateOf<com.example.MyModerationStatus?>(null) }
    val homeScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        myModerationStatus = com.example.ModerationManager.fetchMyModerationStatus(context)
    }

    // Join Code Dialog States
    var showJoinCodeDialog by remember { mutableStateOf(false) }
    var joinCodeInputText by remember { mutableStateOf("") }
    var joinCodeLoading by remember { mutableStateOf(false) }

    // Dynamic state trackers
    var activeTab by remember { mutableStateOf("home") } // "home", "rooms", "moments", "chats", "profile"
    var searchQuery by remember { mutableStateOf("") }
    var selectedLanguageFilter by remember { mutableStateOf("All") }

    // Advanced search filter states
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterOnlyLive by remember { mutableStateOf(false) }
    var filterByLevel by remember { mutableStateOf("All") }
    var selectedSortOption by remember { mutableStateOf("Popular") }
    
    // Remote events / Amusements
    var showAmusementEventsDialog by remember { mutableStateOf(false) }
    var activeEventsList by remember { mutableStateOf<List<SupabaseEvent>>(emptyList()) }
    var selectedAmusementEvent by remember { mutableStateOf<SupabaseEvent?>(null) }

    // Direct Chat active messages thread dialog
    var activeChatWindowContact by remember { mutableStateOf<DirectChat?>(null) }
    val chatMessagesStorage = remember { mutableStateMapOf<String, List<Pair<String, Boolean>>>() } // Pair: text, isMe

    LaunchedEffect(authState) {
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

    // Followed friends state loaded from Firestore in real-time
    var activeFriends by remember { mutableStateOf<List<FollowedFriend>>(emptyList()) }

    val activeFriendsList = remember(activeFriends, supabaseProfiles) {
        val mapped = if (activeFriends.isNotEmpty()) {
            activeFriends.map { f ->
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
            activeFriends
        }
        // Place genuine online users first, followed by offline users with moon badges
        mapped.sortedByDescending { it.isOnline }
    }

    // Dynamic Rooms list state loaded from Supabase voice_rooms in real-time
    var roomsList by remember { mutableStateOf<List<CustomRoom>>(emptyList()) }

    val currentUserId = currentUserProfile.uid
    LaunchedEffect(currentUserId, supabaseProfiles) {
        VoiceRoomRealtimeManager.start()

        suspend fun refreshRoomsAndFriends() {
            try {
                // 1. Fetch voice rooms from Supabase (only active, live rooms)
                val roomsRes = SupabaseClient.api.getVoiceRooms()
                val supaRooms = (roomsRes.body() ?: emptyList()).filter {
                    (it.is_live == true) && (it.status != "ended")
                }

                // 2. Fetch active events from Supabase events table (ordered by event_time asc)
                try {
                    val eventsRes = SupabaseClient.api.getActiveEvents()
                    if (eventsRes.isSuccessful) {
                        activeEventsList = eventsRes.body() ?: emptyList()
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("HomeScreen", "Error fetching active events from Supabase", e)
                }

                // 3. Fetch room participants from Supabase and merge with live Realtime participants
                val participantsRes = SupabaseClient.api.getAllRoomParticipants()
                val supaParticipants = participantsRes.body() ?: emptyList()
                val liveParticipants = com.example.realtime.VoiceRoomRealtimeManager.getAllLiveParticipants()
                val allParticipants = (supaParticipants + liveParticipants).distinctBy { "${it.room_id}_${it.user_id}" }

                // 3. Derive real followed friends & presence from Supabase DB followers and room participants
                val followedUserIds = try {
                    val followersResp = SupabaseClient.api.getFollowers(
                        auth = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
                        followerIdEq = "eq.$currentUserId"
                    )
                    if (followersResp.isSuccessful) {
                        followersResp.body()?.map { it.following_id }?.toSet() ?: emptySet()
                    } else emptySet()
                } catch (e: Exception) {
                    Log.w("HomeScreen", "Could not fetch followed IDs: ${e.message}")
                    emptySet()
                }

                if (supabaseProfiles.isNotEmpty()) {
                    val followedProfiles = supabaseProfiles.filter {
                        it.uid != currentUserId && followedUserIds.contains(it.uid)
                    }
                    val targetProfiles = if (followedProfiles.isNotEmpty()) {
                        followedProfiles
                    } else {
                        // User has not followed anyone yet: showcase community learners
                        supabaseProfiles.filter { it.uid != currentUserId }.take(10)
                    }

                    val derivedFriends = targetProfiles.map { p ->
                        val pId = p.uid
                        val activePart = allParticipants.firstOrNull { it.user_id == pId || it.user_id == p.uid }
                        val isOnline = activePart != null
                        val isSpeaking = isOnline && (activePart.role == "speaker" || activePart.role == "host") && activePart.is_mic_enabled
                        val statusText = when {
                            !isOnline -> "Offline"
                            activePart?.role == "speaker" || activePart?.role == "host" -> "Speaking"
                            else -> "Listening"
                        }
                        FollowedFriend(
                            id = pId,
                            name = p.displayName ?: p.username ?: "Learner",
                            avatarUrl = p.avatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                            flagCode = p.countryCode?.ifBlank { null } ?: "in",
                            isSpeaking = isSpeaking,
                            statusText = statusText,
                            currentRoomId = activePart?.room_id ?: "",
                            isOnline = isOnline
                        )
                    }
                    activeFriends = derivedFriends
                }

                // 4. Map Supabase voice_rooms to CustomRoom
                if (supaRooms.isNotEmpty()) {
                    val mapped = supaRooms.mapIndexed { index, r ->
                        val rId = r.id ?: "room_$index"
                        val roomParts = allParticipants.filter { it.room_id == rId }
                        val realSpeaking = if (roomParts.isNotEmpty()) {
                            roomParts.count { it.role == "speaker" || it.role == "host" }.coerceAtLeast(1)
                        } else {
                            r.speaking_count ?: 1
                        }
                        val realListening = if (roomParts.isNotEmpty()) {
                            roomParts.count { it.role == "listener" || (it.role != "speaker" && it.role != "host") }
                        } else {
                            r.listening_count ?: 0
                        }

                        val decidedTag = decideSupabaseRoomTag(
                            room = r,
                            allRooms = supaRooms,
                            realSpeakingCount = realSpeaking,
                            realListeningCount = realListening
                        )
                        val badgeColor = getBadgeColor(decidedTag)
                        val overlayVal = when (index % 4) {
                            0 -> Color(0x22FCD34D)
                            1 -> Color(0x22D8B4FE)
                            2 -> Color(0x22FDA4AF)
                            else -> Color(0x223B82F6)
                        }
                        val gradBrush = Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)))

                        val rawAvatars = r.preview_avatars.orEmpty()
                        val roomAvatars = if (rawAvatars.isNotEmpty()) {
                            rawAvatars
                        } else {
                            supabaseProfiles.shuffled().take(3).mapNotNull { it.avatar }.ifEmpty {
                                listOf("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150")
                            }
                        }

                        CustomRoom(
                            id = rId,
                            title = r.title ?: "Voice Circle",
                            subtitle = r.description ?: "Practice and speak together",
                            language = r.language ?: "English",
                            flagCode = LanguageData.resolveFlagCode(
                                languageName = r.language,
                                storedCode = r.language_code.orEmpty().ifBlank { r.country_code.orEmpty() }
                            ),
                            speakingCount = realSpeaking,
                            listeningCount = realListening,
                            tag = r.level_tag.orEmpty().ifBlank { r.tag.orEmpty() }.ifBlank { "All Levels" },
                            bgGradient = gradBrush,
                            imageUrl = r.banner_image_url.orEmpty().ifBlank { "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=800" },
                            overlayColor = overlayVal,
                            badgeText = decidedTag,
                            badgeColor = badgeColor,
                            avatars = roomAvatars,
                            hostId = r.host_id ?: ""
                        )
                    }
                    roomsList = mapped
                } else {
                    roomsList = emptyList()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("HomeScreen", "Supabase database table access temporarily unavailable or undergoing schema maintenance: ${e.message}")
            }
        }

        refreshRoomsAndFriends()

        // Background polling fallback every 8 seconds to ensure voice rooms and friends stay in sync
        launch {
            while (isActive) {
                delay(8000)
                refreshRoomsAndFriends()
            }
        }

        // Real-time updates from Supabase postgres_changes, live participants, and room broadcasts
        launch {
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
        }
        launch {
            VoiceRoomRealtimeManager.roomLiveParticipants.collect {
                refreshRoomsAndFriends()
            }
        }
    }

    // Social moments feed data
    val momentsPosts = remember {
        mutableStateListOf(
            MomentPost(
                id = "post_1",
                author = "Sarah Jenkins",
                username = "sarah_travels",
                avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                timeAgo = "2 hours ago",
                content = "Just finished a wonderful Korean practice room on FunkyTalk! Loving the friendly nature of speakers here. Any suggestions to level up speaking quickly?",
                imageUrl = "https://images.unsplash.com/photo-1484589065579-248adc015074?w=600",
                initialLikes = 42,
                commentsCount = 11
            ),
            MomentPost(
                id = "post_2",
                author = "Park Min-jun",
                username = "minjun_seoul",
                avatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                timeAgo = "4 hours ago",
                content = "Beautiful sunset here in Seoul today! Hope everyone is having a fabulous language exchange. Ready for another English speech session later?",
                imageUrl = "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=600",
                initialLikes = 89,
                commentsCount = 24
            ),
            MomentPost(
                id = "post_3",
                author = "Kenji Sato",
                username = "kenji_tokyo",
                avatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                timeAgo = "1 day ago",
                content = "Enjoyed reading English materials together with friends in a listening room. Small steps everyday make the difference! Keep going guys!",
                imageUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=600",
                initialLikes = 31,
                commentsCount = 6
            )
        )
    }

    // Real conversations list - user-initiated contacts from Discover or Empty
    val dynamicInitiatedChats = remember { mutableStateListOf<DirectChat>() }
    var activeGroupChat by remember { mutableStateOf<com.example.db.LocalGroupConversation?>(null) }

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

    if (showFilterDialog) {
        FilterRoomsDialog(
            selectedSortOption = selectedSortOption,
            onSortOptionChange = { selectedSortOption = it },
            selectedLanguageFilter = selectedLanguageFilter,
            onLanguageFilterChange = { selectedLanguageFilter = it },
            filterByLevel = filterByLevel,
            onLevelFilterChange = { filterByLevel = it },
            filterOnlyLive = filterOnlyLive,
            onFilterOnlyLiveChange = { filterOnlyLive = it },
            onDismiss = { showFilterDialog = false },
            onApply = { showFilterDialog = false }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFFFAFAEE),
            bottomBar = {
                HomeBottomBar(
                    activeTab = activeTab,
                    onTabSelected = { activeTab = it }
                )
            }
        ) { innerPadding ->
            Crossfade(
                targetState = activeTab,
                label = "HomeSubNavigation",
                modifier = Modifier.padding(innerPadding)
            ) { currentTab ->
                when (currentTab) {
                    "home" -> {
                        HomeTabContent(
                            currentUserProfile = currentUserProfile,
                            activeFriendsList = activeFriendsList,
                            roomsList = roomsList,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedLanguageFilter = selectedLanguageFilter,
                            onLanguageSelected = { selectedLanguageFilter = it },
                            filterOnlyLive = filterOnlyLive,
                            filterByLevel = filterByLevel,
                            selectedSortOption = selectedSortOption,
                            hasActiveEvents = activeEventsList.isNotEmpty(),
                            onOpenEvents = {
                                showAmusementEventsDialog = true
                            },
                            onOpenNotifications = onOpenNotifications,
                            onCreateRoom = { showCreateRoomBottomSheet = true },
                            onJoinWithCode = { showJoinCodeDialog = true },
                            onOpenFilter = { showFilterDialog = true },
                            onJoinRoom = { room ->
                                Toast.makeText(context, "Welcome to ${room.title}! Connecting audio...", Toast.LENGTH_LONG).show()
                                val myRole = if (room.hostId == currentUserId) "speaker" else "listener"
                                viewModel.joinRoom(room.id, room.title, context, role = myRole, hostId = room.hostId)
                                onRoomClick()
                            }
                        )
                    }
                    "discover", "rooms" -> {
                        DiscoverTabContent(
                            supabaseProfiles = supabaseProfiles,
                            onCreateRoom = { showCreateRoomBottomSheet = true },
                            onStartDirectChat = { userId, name, avatarUrl, flagCode ->
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

                                // Wave send "Hi" using existing E2EE chat system
                                if (userId.isNotBlank() && !userId.startsWith("partner_mock")) {
                                    homeScope.launch {
                                        try {
                                            com.example.repository.EncryptedChatRepository.init(context)
                                            val ready = com.example.repository.EncryptedChatRepository.checkRecipientKeyStatus(context, userId)
                                            if (ready) {
                                                com.example.repository.EncryptedChatRepository.sendMessage(
                                                    context = context,
                                                    recipientUserId = userId,
                                                    recipientName = name,
                                                    recipientAvatar = avatarUrl,
                                                    text = "Hi"
                                                )
                                                android.util.Log.d("HomeScreen", "Successfully waved hello (E2EE sent) to $name")
                                            } else {
                                                android.util.Log.d("HomeScreen", "E2EE keys missing for $name, Secure Chat Unavailable flow active.")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("HomeScreen", "Error waving to $name: ${e.message}", e)
                                        }
                                    }
                                }
                            }
                        )
                    }
                    "moments" -> {
                        MomentsTabContent(
                            momentsPosts = momentsPosts,
                            onToggleLike = { post ->
                                val index = momentsPosts.indexOf(post)
                                if (index != -1) {
                                    momentsPosts[index] = post.copy(isLiked = !post.isLiked)
                                }
                            }
                        )
                    }
                    "chats" -> {
                        ChatsTabContent(
                            conversationsList = dynamicInitiatedChats,
                            onSelectChat = { chat ->
                                activeChatWindowContact = chat
                            },
                            onOpenSupportChat = onOpenSupportChat,
                            onNavigateToDiscover = { activeTab = "rooms" },
                            onOpenGroupChat = { grp ->
                                activeGroupChat = grp
                            }
                        )
                    }
                    "profile" -> {
                        ProfileTabContent(
                            currentUserProfile = currentUserProfile,
                            onLogoutClick = { showLogoutDialog = true },
                            viewModel = viewModel,
                            onOpenSupportChat = onOpenSupportChat
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
                onDismiss = { com.example.repository.GroupChatRepository.setPendingInviteCode(null) },
                onJoinedGroup = { joinedGroupId ->
                    com.example.repository.GroupChatRepository.setPendingInviteCode(null)
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
    }

    // Modal Sheet to Create Language / Private Rooms
    if (showCreateRoomBottomSheet) {
        CreateRoomBottomSheet(
            onDismiss = { showCreateRoomBottomSheet = false },
            onCreateRoom = { isPrivate ->
                if (myModerationStatus?.action_type == "suspend" && com.example.ModerationManager.isActionActive(myModerationStatus)) {
                    Toast.makeText(context, "Cannot create room: Your account is currently suspended.", Toast.LENGTH_LONG).show()
                } else {
                    showCreateRoomBottomSheet = false
                    onCreateLanguageRoom(isPrivate)
                }
            }
        )
    }

    // Active Suspension Alert on Home Screen
    if (myModerationStatus?.action_type == "suspend" && com.example.ModerationManager.isActionActive(myModerationStatus)) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss without resolving */ },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Suspended",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Account Suspended",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Your account has been placed under active suspension by administrators for violating community safety policies.",
                        fontSize = 13.sp,
                        color = Color(0xFF374151)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Reason: ${myModerationStatus?.reason ?: "Violation of Community Guidelines"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF991B1B)
                            )
                            if (!myModerationStatus?.expires_at.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Suspension expires: ${myModerationStatus?.expires_at}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB91C1C)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Refresh Status", color = Color.White)
                }
            }
        )
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
                        android.util.Log.d("JoinRoomByCode", "RPC response status: ${response.code()}, body: $rawBody")

                        var targetRoomId: String? = null
                        var targetRoomTitle: String? = null

                        if (response.isSuccessful && rawBody.isNotBlank() && rawBody != "null") {
                            if (rawBody.startsWith("{")) {
                                val json = org.json.JSONObject(rawBody)
                                targetRoomId = json.optString("room_id").ifBlank { json.optString("id") }
                                targetRoomTitle = json.optString("title").ifBlank { "Voice Room" }
                            } else if (rawBody.startsWith("[")) {
                                val array = org.json.JSONArray(rawBody)
                                if (array.length() > 0) {
                                    val json = array.getJSONObject(0)
                                    targetRoomId = json.optString("room_id").ifBlank { json.optString("id") }
                                    targetRoomTitle = json.optString("title").ifBlank { "Voice Room" }
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
                                viewModel.joinRoom(targetRoomId, finalTitle, context)
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
