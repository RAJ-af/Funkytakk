package com.example.ui.screens.home.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.UserProfile
import com.example.ui.components.FriendlyErrorRetryCard
import com.example.ui.components.HomeFeedSkeleton
import com.example.ui.screens.home.components.*
import com.example.ui.screens.home.models.CustomRoom
import com.example.ui.screens.home.models.FollowedFriend
import com.example.ui.theme.LocalFunkyColors
import com.example.util.UiState

@Composable
fun HomeTabContent(
    currentUserProfile: UserProfile,
    activeFriendsList: List<FollowedFriend>,
    roomsList: List<CustomRoom>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedLanguageFilters: Set<String>,
    onLanguageSelected: (String) -> Unit,
    filterSpeakOrLearnMode: String = "They speak",
    filterOnlyLive: Boolean,
    filterByLevel: String,
    selectedSortOption: String,
    hasActiveEvents: Boolean = false,
    onOpenEvents: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    onOpenLeaderboard: () -> Unit = {},
    onOpenSpinOffers: () -> Unit = {},
    onCreateRoom: () -> Unit,
    onJoinWithCode: () -> Unit,
    onOpenFilter: () -> Unit,
    onJoinRoom: (CustomRoom) -> Unit,
    homeUiState: UiState<Unit> = UiState.Content(Unit),
    isOnline: Boolean = true,
    homeLoadError: String? = null,
    onRetryLoad: () -> Unit = {},
    onFriendClick: (FollowedFriend) -> Unit = {}
) {
    val funkyColors = LocalFunkyColors.current
    val chipSelectedLanguage = when (selectedLanguageFilters.size) {
        0 -> "All"
        1 -> selectedLanguageFilters.first()
        else -> ""
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. BRAND HEADER & CREATION PILL
        item {
            HomeHeader(
                hasActiveEvents = hasActiveEvents,
                isOnline = isOnline,
                onOpenEvents = onOpenEvents,
                onOpenNotifications = onOpenNotifications,
                onCreateRoom = onCreateRoom,
                onJoinWithCode = onJoinWithCode
            )
        }

        // 2. MOTIVATIONAL GREETING CARD
        item {
            HomeGreetingCard(
                currentUserProfile = currentUserProfile
            )
        }

        // 2.2 QUICK ACCESS: LIVE LEADERBOARD & SPECIAL OFFERS SPIN
        item {
            HomeQuickAccessStrip(
                onOpenLeaderboard = onOpenLeaderboard,
                onOpenSpinOffers = onOpenSpinOffers
            )
        }

        // 2.5 FOLLOWED FRIENDS STORIES WITH LIVE WAVEFORM SPEAKING OVERLAYS
        if (activeFriendsList.isNotEmpty()) {
            item {
                ActiveFriendsRow(
                    activeFriendsList = activeFriendsList,
                    onFriendClick = onFriendClick
                )
            }
        }

        // 3. SEARCH & FILTER ROW
        item {
            HomeSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onOpenFilter = onOpenFilter
            )
        }

        // 4. LANGUAGE CATEGORIES HORIZONTAL PILLS ROW
        item {
            LanguageFilterChips(
                selectedLanguageFilter = chipSelectedLanguage,
                selectedLanguages = selectedLanguageFilters,
                onLanguageSelected = onLanguageSelected
            )
        }

        if (!homeLoadError.isNullOrBlank() && roomsList.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = funkyColors.surface,
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("home_offline_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_alert),
                                contentDescription = "Connection status",
                                tint = funkyColors.mango,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = homeLoadError,
                                color = funkyColors.ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = onRetryLoad,
                            enabled = isOnline,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = funkyColors.mango,
                                contentColor = funkyColors.onMango,
                                disabledContainerColor = funkyColors.soft,
                                disabledContentColor = funkyColors.mute
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("home_retry_button")
                        ) {
                            Text(
                                text = "Retry",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. HIGH-FIDELITY ACTIVE ROOM CARDS LIST
        val filteredRooms = roomsList.filter { room ->
            val matchesLang = if (selectedLanguageFilters.isEmpty()) {
                true
            } else if (filterSpeakOrLearnMode == "They're learning") {
                selectedLanguageFilters.any { targetLang ->
                    room.hostLearningLanguages.any { it.equals(targetLang, ignoreCase = true) } ||
                        room.language.equals(targetLang, ignoreCase = true) ||
                        room.subtitle.contains(targetLang, ignoreCase = true) ||
                        room.title.contains(targetLang, ignoreCase = true)
                }
            } else {
                selectedLanguageFilters.any { targetLang ->
                    room.language.equals(targetLang, ignoreCase = true) ||
                        room.hostNativeLanguages.any { it.equals(targetLang, ignoreCase = true) }
                }
            }
            val matchesQuery = searchQuery.isBlank() ||
                room.title.contains(searchQuery, ignoreCase = true) ||
                room.subtitle.contains(searchQuery, ignoreCase = true) ||
                room.language.contains(searchQuery, ignoreCase = true)
            val matchesLive = !filterOnlyLive || room.badgeText == "LIVE"
            val matchesLevel = when (filterByLevel) {
                "All", "All Levels" -> true
                "Beginner" -> room.tag.contains("Beginner", ignoreCase = true) ||
                    room.subtitle.contains("Beginner", ignoreCase = true) ||
                    room.title.contains("Beginner", ignoreCase = true)
                "Intermediate" -> room.tag.contains("Intermediate", ignoreCase = true) ||
                    room.subtitle.contains("Intermediate", ignoreCase = true) ||
                    room.title.contains("Intermediate", ignoreCase = true)
                "Advanced" -> room.tag.contains("Advanced", ignoreCase = true) ||
                    room.subtitle.contains("Advanced", ignoreCase = true) ||
                    room.title.contains("Advanced", ignoreCase = true) ||
                    room.tag.contains("Fluent", ignoreCase = true) ||
                    room.title.contains("Idioms", ignoreCase = true) ||
                    room.tag.contains("hospitality", ignoreCase = true)
                else -> true
            }
            matchesLang && matchesQuery && matchesLive && matchesLevel
        }.sortedWith(
            when (selectedSortOption) {
                "Popular" -> compareByDescending { it.speakingCount + it.listeningCount }
                "Most Listeners" -> compareByDescending { it.listeningCount }
                else -> compareBy { it.id }
            }
        )

        when {
            homeUiState is UiState.Loading && roomsList.isEmpty() -> {
                item {
                    HomeFeedSkeleton(modifier = Modifier.testTag("home_feed_skeleton"))
                }
            }
            homeUiState is UiState.Error && roomsList.isEmpty() -> {
                item {
                    FriendlyErrorRetryCard(
                        message = homeUiState.message,
                        onRetry = onRetryLoad,
                        isOnline = isOnline,
                        testTagPrefix = "home",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
            }
            filteredRooms.isEmpty() -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search_off),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = funkyColors.mute.copy(alpha = 0.65f)
                            )
                            Text(
                                text = if (roomsList.isEmpty()) "No live rooms right now" else "No matching rooms found",
                                fontWeight = FontWeight.SemiBold,
                                color = funkyColors.ink
                            )
                            if (roomsList.isEmpty()) {
                                Text(
                                    text = if (isOnline) "Be the first to start a voice circle!" else "Connect to the internet to join or create live rooms.",
                                    fontSize = 13.sp,
                                    color = funkyColors.mute
                                )
                                Button(
                                    onClick = onCreateRoom,
                                    enabled = isOnline,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = funkyColors.mango,
                                        contentColor = funkyColors.onMango,
                                        disabledContainerColor = funkyColors.soft,
                                        disabledContentColor = funkyColors.mute
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("home_empty_create_room_button")
                                ) {
                                    Text("Create Room", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                itemsIndexed(filteredRooms) { index, room ->
                    LiveVoiceRoomCard(
                        room = room,
                        index = index,
                        onJoin = { onJoinRoom(room) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeQuickAccessStrip(
    onOpenLeaderboard: () -> Unit,
    onOpenSpinOffers: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            onClick = onOpenLeaderboard,
            shape = RoundedCornerShape(20.dp),
            color = funkyColors.surface,
            border = BorderStroke(1.dp, funkyColors.line),
            shadowElevation = 1.dp,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 66.dp)
                .testTag("home_leaderboard_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = funkyColors.mango.copy(alpha = 0.16f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_crown_filled),
                            contentDescription = "Live Leaderboard",
                            tint = funkyColors.mango,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "Leaderboard",
                            color = funkyColors.ink,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = androidx.compose.ui.graphics.Color(0xFF22C55E).copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "LIVE",
                                color = androidx.compose.ui.graphics.Color(0xFF16A34A),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Voice · Popular · Logins",
                        color = funkyColors.mute,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        Surface(
            onClick = onOpenSpinOffers,
            shape = RoundedCornerShape(20.dp),
            color = funkyColors.surface,
            border = BorderStroke(1.dp, funkyColors.line),
            shadowElevation = 1.dp,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 66.dp)
                .testTag("home_spin_offers_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = funkyColors.soft,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_gift),
                            contentDescription = "Special Offers Spin",
                            tint = funkyColors.mango,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "Spin Offers",
                            color = funkyColors.ink,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = funkyColors.soft
                        ) {
                            Text(
                                text = "SOON",
                                color = funkyColors.mango,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Coin Discounts",
                        color = funkyColors.mute,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
