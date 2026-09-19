package com.example.ui.screens.home.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.UserProfile
import com.example.ui.screens.home.components.*
import com.example.ui.screens.home.models.CustomRoom
import com.example.ui.screens.home.models.FollowedFriend

@Composable
fun HomeTabContent(
    currentUserProfile: UserProfile,
    activeFriendsList: List<FollowedFriend>,
    roomsList: List<CustomRoom>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedLanguageFilter: String,
    onLanguageSelected: (String) -> Unit,
    filterOnlyLive: Boolean,
    filterByLevel: String,
    selectedSortOption: String,
    onOpenEvents: () -> Unit,
    onCreateRoom: () -> Unit,
    onOpenFilter: () -> Unit,
    onJoinRoom: (CustomRoom) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. BRAND HEADER & CREATION PILL
        item {
            HomeHeader(
                onOpenEvents = onOpenEvents,
                onCreateRoom = onCreateRoom
            )
        }

        // 2. MOTIVATIONAL GREETING CARD (SUN & PAPER PLANE)
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
                selectedLanguageFilter = selectedLanguageFilter,
                onLanguageSelected = onLanguageSelected
            )
        }

        // 5. HIGH-FIDELITY ACTIVE ROOM CARDS LIST
        val filteredRooms = roomsList.filter {
            val matchesLang = (selectedLanguageFilter == "All" || it.language.equals(selectedLanguageFilter, ignoreCase = true))
            val matchesQuery = (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.subtitle.contains(searchQuery, ignoreCase = true) || it.language.contains(searchQuery, ignoreCase = true))
            val matchesLive = (!filterOnlyLive || it.badgeText == "LIVE")
            val matchesLevel = when (filterByLevel) {
                "All" -> true
                "Beginner" -> it.tag.contains("Beginner", ignoreCase = true) || it.subtitle.contains("Beginner", ignoreCase = true) || it.title.contains("Beginner", ignoreCase = true)
                "Intermediate" -> it.tag.contains("Intermediate", ignoreCase = true) || it.subtitle.contains("Intermediate", ignoreCase = true) || it.title.contains("Intermediate", ignoreCase = true)
                "Advanced" -> it.tag.contains("Fluent", ignoreCase = true) || it.title.contains("Idioms", ignoreCase = true) || it.tag.contains("hospitality", ignoreCase = true)
                else -> true
            }
            matchesLang && matchesQuery && matchesLive && matchesLevel
        }.sortedWith(
            when (selectedSortOption) {
                "Popular" -> compareByDescending { it.speakingCount + it.listeningCount }
                "Most Listeners" -> compareByDescending { it.listeningCount }
                else -> compareBy { it.id } // Order by original ID sequence for Recently Active
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
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching rooms found",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
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
