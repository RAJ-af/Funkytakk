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
import com.example.UserProfile
import com.example.realtime.VoiceRoomRealtimeManager
import com.example.ui.screens.home.components.HomeBottomBar
import com.example.ui.screens.home.dialogs.*
import com.example.ui.screens.home.models.*
import com.example.ui.screens.home.tabs.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.SupabaseSessionManager
import com.example.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit,
    onCreateLanguageRoom: (isPrivate: Boolean) -> Unit = {},
    onRoomClick: () -> Unit = {}
) {
    val authState by viewModel.authState.collectAsState()
    val supabaseProfiles by viewModel.supabaseProfiles.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchSupabaseProfiles(context)
    }

    var showCreateRoomBottomSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

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
    var selectedAmusementEvent by remember { mutableStateOf<AmusementEvent?>(null) }

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
        mapped.filter { f ->
            val status = f.statusText.lowercase()
            status != "offline" && status.isNotBlank()
        }
    }

    val amusementEvents = remember {
        listOf(
            AmusementEvent(
                id = "evt_1",
                title = "Language Fiesta 2026",
                description = "Join 500+ speakers across 10 languages for an epic mega-room party!",
                date = "June 16, 2026 - 15:00 UTC",
                tag = "Mega Event",
                cardColorDark = Color(0xFFA855F7),
                cardColorLight = Color(0xFFF3E8FF),
                imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800"
            ),
            AmusementEvent(
                id = "evt_2",
                title = "Movie Night: Korean Cinema",
                description = "Watch and discuss Parasite with native Korean speakers.",
                date = "June 18, 2026 - 20:00 KST",
                tag = "Culture",
                cardColorDark = Color(0xFFEF4444),
                cardColorLight = Color(0xFFFEE2E2),
                imageUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800"
            ),
            AmusementEvent(
                id = "evt_3",
                title = "Grammar Escape Room",
                description = "Solve English grammar and vocabulary puzzles to escape the room!",
                date = "June 20, 2026 - 10:00 EST",
                tag = "Game",
                cardColorDark = Color(0xFF3B82F6),
                cardColorLight = Color(0xFFEFF6FF),
                imageUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=800"
            )
        )
    }

    // Dynamic Rooms list state loaded from Supabase voice_rooms in real-time
    var roomsList by remember { mutableStateOf<List<CustomRoom>>(emptyList()) }

    val currentUserId = currentUserProfile.uid
    LaunchedEffect(currentUserId, supabaseProfiles) {
        VoiceRoomRealtimeManager.start()

        suspend fun refreshRoomsAndFriends() {
            try {
                // 1. Fetch voice rooms from Supabase
                val roomsRes = SupabaseClient.api.getVoiceRooms()
                val supaRooms = roomsRes.body() ?: emptyList()

                // 2. Fetch room participants from Supabase
                val participantsRes = SupabaseClient.api.getAllRoomParticipants()
                val supaParticipants = participantsRes.body() ?: emptyList()

                // 3. Derive friends & presence from room_participants & supabaseProfiles
                if (supabaseProfiles.isNotEmpty()) {
                    val otherProfiles = supabaseProfiles.filter {
                        it.uid != currentUserId
                    }
                    val derivedFriends = otherProfiles.take(8).map { p ->
                        val pId = p.uid
                        val activePart = supaParticipants.firstOrNull { it.user_id == pId || it.user_id == p.uid }
                        val isSpeaking = activePart != null && activePart.role == "speaker" && activePart.is_mic_enabled
                        val statusText = when {
                            activePart == null -> "Online"
                            activePart.role == "speaker" -> "Speaking"
                            else -> "Listening"
                        }
                        FollowedFriend(
                            id = pId,
                            name = p.displayName ?: p.username ?: "Learner",
                            avatarUrl = p.avatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                            flagCode = p.countryCode ?: "us",
                            isSpeaking = isSpeaking,
                            statusText = statusText,
                            currentRoomId = activePart?.room_id ?: ""
                        )
                    }
                    if (derivedFriends.isNotEmpty()) {
                        activeFriends = derivedFriends
                    }
                }

                // 4. Map Supabase voice_rooms to CustomRoom
                if (supaRooms.isNotEmpty()) {
                    val mapped = supaRooms.mapIndexed { index, r ->
                        val rId = r.id ?: "room_$index"
                        val roomParts = supaParticipants.filter { it.room_id == rId }
                        val realSpeaking = if (roomParts.isNotEmpty()) {
                            roomParts.count { it.role == "speaker" }.coerceAtLeast(1)
                        } else {
                            r.speaking_count ?: 1
                        }
                        val realListening = if (roomParts.isNotEmpty()) {
                            roomParts.count { it.role == "listener" }
                        } else {
                            r.listening_count ?: 0
                        }

                        val decidedTag = r.status_tag?.ifBlank { r.tag }?.ifBlank { "LIVE" } ?: "LIVE"
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
                            flagCode = r.language_code.orEmpty().ifBlank { r.country_code.orEmpty() }.ifBlank { "us" },
                            speakingCount = realSpeaking,
                            listeningCount = realListening,
                            tag = r.level_tag.orEmpty().ifBlank { r.tag.orEmpty() }.ifBlank { "All Levels" },
                            bgGradient = gradBrush,
                            imageUrl = r.banner_image_url.orEmpty().ifBlank { "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=800" },
                            overlayColor = overlayVal,
                            badgeText = decidedTag,
                            badgeColor = badgeColor,
                            avatars = roomAvatars
                        )
                    }
                    roomsList = mapped
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error refreshing rooms & participants from Supabase", e)
            }
        }

        refreshRoomsAndFriends()

        // Real-time updates from Supabase postgres_changes
        VoiceRoomRealtimeManager.events.collect {
            refreshRoomsAndFriends()
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

    // DM Contacts inbox data
    val conversationsList = remember {
        listOf(
            DirectChat("c_1", "Sarah Jenkins", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200", "Hey! Are you free to practice English now?", "9:41 AM", 2, "online", "English"),
            DirectChat("c_2", "Park Min-jun", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200", "Let's join a room together tonight!", "Yesterday", 0, "online", "Korean"),
            DirectChat("c_3", "Yuki Tanaka", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200", "Your Japanese is improving so fast!", "Sunday", 0, "offline", "Japanese"),
            DirectChat("c_4", "Chen Wei", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200", "Thanks for helping me with English yesterday!", "May 10", 0, "idle", "Chinese"),
            DirectChat("c_5", "Lucía Morales", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200", "¡Fue una gran conversación!", "May 8", 0, "offline", "Spanish"),
            DirectChat("c_6", "Thomas Müller", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=200", "Want to practice tomorrow?", "May 6", 0, "online", "German")
        )
    }

    // Ensure initial message queues exist
    LaunchedEffect(Unit) {
        conversationsList.forEach { chat ->
            if (!chatMessagesStorage.containsKey(chat.id)) {
                chatMessagesStorage[chat.id] = listOf(
                    "Hello! Glad we connected here." to false,
                    chat.lastMsg to false
                )
            }
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

    if (showAmusementEventsDialog) {
        AmusementEventsDialog(
            events = amusementEvents,
            onDismiss = { showAmusementEventsDialog = false },
            onSelectEvent = { event ->
                selectedAmusementEvent = event
            }
        )
    }

    selectedAmusementEvent?.let { event ->
        EventDetailsDialog(
            event = event,
            onDismiss = { selectedAmusementEvent = null },
            onJoinEvent = {
                Toast.makeText(context, "You are registered for ${event.title}!", Toast.LENGTH_LONG).show()
                selectedAmusementEvent = null
                showAmusementEventsDialog = false
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
                            onOpenEvents = { showAmusementEventsDialog = true },
                            onCreateRoom = { showCreateRoomBottomSheet = true },
                            onJoinWithCode = { showJoinCodeDialog = true },
                            onOpenFilter = { showFilterDialog = true },
                            onJoinRoom = { room ->
                                Toast.makeText(context, "Welcome to ${room.title}! Connecting audio...", Toast.LENGTH_LONG).show()
                                viewModel.joinRoom(room.id, room.title)
                                onRoomClick()
                            }
                        )
                    }
                    "rooms" -> {
                        RoomsTabContent(
                            onCreateRoom = { showCreateRoomBottomSheet = true }
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
                            conversationsList = conversationsList,
                            onSelectChat = { chat ->
                                activeChatWindowContact = chat
                            }
                        )
                    }
                    "profile" -> {
                        ProfileTabContent(
                            currentUserProfile = currentUserProfile,
                            onLogoutClick = { showLogoutDialog = true }
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
    }

    // Modal Sheet to Create Language / Private Rooms
    if (showCreateRoomBottomSheet) {
        CreateRoomBottomSheet(
            onDismiss = { showCreateRoomBottomSheet = false },
            onCreateRoom = { isPrivate ->
                showCreateRoomBottomSheet = false
                onCreateLanguageRoom(isPrivate)
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
