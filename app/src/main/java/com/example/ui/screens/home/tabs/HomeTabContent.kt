package com.example.ui.screens.home.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.UserProfile
import com.example.ui.screens.home.components.*
import com.example.ui.screens.home.models.CustomRoom
import com.example.ui.screens.home.models.FollowedFriend
import com.example.ui.theme.LocalFunkyColors

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
    onCreateRoom: () -> Unit,
    onJoinWithCode: () -> Unit,
    onOpenFilter: () -> Unit,
    onJoinRoom: (CustomRoom) -> Unit
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

        // 2.5 FOLLOWED FRIENDS STORIES WITH LIVE WAVEFORM SPEAKING OVERLAYS
        item {
            ActiveFriendsRow(
                activeFriendsList = activeFriendsList
            )
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

        if (filteredRooms.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search_off),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = funkyColors.mute.copy(alpha = 0.65f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching rooms found",
                            fontWeight = FontWeight.SemiBold,
                            color = funkyColors.mute
                        )
                    }
                }
            }
        } else {
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
