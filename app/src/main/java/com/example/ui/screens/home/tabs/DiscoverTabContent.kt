package com.example.ui.screens.home.tabs

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.UserProfile
import com.example.ui.components.AvatarWithCircularFlag
import com.example.ui.components.CircleFlag
import com.example.ui.components.FunkyMicIcon
import com.example.ui.components.FunkySearchIcon
import com.example.ui.components.FunkySlidersIcon
import com.example.ui.components.FunkyWaveHandIcon

data class DiscoverPartner(
    val id: String,
    val name: String,
    val age: Int,
    val isOnline: Boolean,
    val avatarUrl: String,
    val countryCode: String,
    val nativeLang: String,
    val learningLang: String,
    val bio: String,
    val interests: List<String>,
    val vibe: String = "Chill",
    val isWaved: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverTabContent(
    supabaseProfiles: List<UserProfile> = emptyList(),
    onCreateRoom: () -> Unit = {},
    onStartDirectChat: (userId: String, name: String, avatarUrl: String, flagCode: String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current

    // Curated initial global partners directly inspired by the design
    val defaultPartners = remember {
        listOf(
            DiscoverPartner(
                id = "p1",
                name = "Yohan",
                age = 20,
                isOnline = true,
                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400",
                countryCode = "id",
                nativeLang = "EN",
                learningLang = "KO",
                bio = "I love anime, music and meeting new friends!",
                interests = listOf("Anime", "Music", "Languages", "Gaming"),
                vibe = "Same Vibe"
            ),
            DiscoverPartner(
                id = "p2",
                name = "piaa",
                age = 19,
                isOnline = true,
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                countryCode = "id",
                nativeLang = "ID",
                learningLang = "EN",
                bio = "Hello everyone, I am piaa, nice to meet you all!",
                interests = listOf("K-Drama", "Food", "Travel", "Photography"),
                vibe = "Same Vibe"
            ),
            DiscoverPartner(
                id = "p3",
                name = "允真",
                age = 21,
                isOnline = false,
                avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400",
                countryCode = "cn",
                nativeLang = "CN",
                learningLang = "EN",
                bio = "奇怪的人不要靠近我 :)",
                interests = listOf("Music", "Games", "Photography", "Art"),
                vibe = "Interests"
            ),
            DiscoverPartner(
                id = "p4",
                name = "Bunny",
                age = 18,
                isOnline = true,
                avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=400",
                countryCode = "eg",
                nativeLang = "EN",
                learningLang = "KO",
                bio = "I don't care about losing anyone. Um, this reflects my personality, but...",
                interests = listOf("Art", "Movies", "Cats", "Design"),
                vibe = "Same Vibe"
            ),
            DiscoverPartner(
                id = "p5",
                name = "Ju Ju",
                age = 19,
                isOnline = true,
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400",
                countryCode = "bd",
                nativeLang = "EN",
                learningLang = "KO",
                bio = "Silent moon, always positive and kind...",
                interests = listOf("Books", "Nature", "Self Care", "Poetry"),
                vibe = "Same Vibe"
            ),
            DiscoverPartner(
                id = "p6",
                name = "Alyan Khan",
                age = 22,
                isOnline = false,
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400",
                countryCode = "pk",
                nativeLang = "EN",
                learningLang = "KO",
                bio = "Hello everyone, I am Alyan Khan, nice to meet you all",
                interests = listOf("Sports", "Tech", "Travel", "Coding"),
                vibe = "Interests"
            ),
            DiscoverPartner(
                id = "p7",
                name = "Kenji",
                age = 23,
                isOnline = true,
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400",
                countryCode = "jp",
                nativeLang = "JA",
                learningLang = "EN",
                bio = "Tokyo native learning English! Let's talk about anime, ramen, and culture.",
                interests = listOf("Anime", "Cooking", "Music", "Manga"),
                vibe = "Same Vibe"
            ),
            DiscoverPartner(
                id = "p8",
                name = "Elena",
                age = 21,
                isOnline = true,
                avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400",
                countryCode = "es",
                nativeLang = "ES",
                learningLang = "EN",
                bio = "Hola! Looking for friendly language exchange partners to practice speaking English.",
                interests = listOf("Travel", "Photography", "Fitness", "Art"),
                vibe = "Same Vibe"
            )
        )
    }

    // Dynamic state of partners with wave tracking
    var partnersList by remember { mutableStateOf(defaultPartners) }

    // Merge Supabase profiles if available
    LaunchedEffect(supabaseProfiles) {
        if (supabaseProfiles.isNotEmpty()) {
            val fromSupabase = supabaseProfiles.mapNotNull { p ->
                val name = p.displayName ?: p.username
                if (!name.isNullOrBlank()) {
                    DiscoverPartner(
                        id = p.uid,
                        name = name,
                        age = p.age ?: 21,
                        isOnline = true,
                        avatarUrl = p.avatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                        countryCode = p.countryCode ?: "in",
                        nativeLang = p.nativeLanguage?.take(2)?.uppercase() ?: "EN",
                        learningLang = p.learningLanguage?.take(2)?.uppercase() ?: "KO",
                        bio = if (!p.country.isNullOrBlank()) "From ${p.country}! Let's connect & practice languages 🌟" else "Excited to meet new friends and exchange languages!",
                        interests = p.hobbies?.ifEmpty { listOf("Languages", "Culture", "Music") } ?: listOf("Languages", "Culture", "Music"),
                        vibe = "Same Vibe"
                    )
                } else null
            }
            val existingIds = defaultPartners.map { it.id }.toSet()
            val newOnes = fromSupabase.filterNot { it.id in existingIds }
            partnersList = defaultPartners + newOnes
        }
    }

    // Interactive States
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeCategoryFilter by remember { mutableStateOf("All") } // "All", "Online", "Same Vibe", "Interests", "Language"
    var selectedLanguageFilter by remember { mutableStateOf("All") }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showInterestsSheet by remember { mutableStateOf(false) }
    var showFilterSettingsSheet by remember { mutableStateOf(false) }
    var selectedInterestTag by remember { mutableStateOf<String?>(null) }

    // User Interaction Modals
    var waveDialogUser by remember { mutableStateOf<DiscoverPartner?>(null) }
    var waveCustomMessage by remember { mutableStateOf("") }
    var profileSheetUser by remember { mutableStateOf<DiscoverPartner?>(null) }

    // Filter logic
    val filteredPartners = remember(partnersList, searchQuery, activeCategoryFilter, selectedLanguageFilter, selectedInterestTag) {
        partnersList.filter { partner ->
            // Search query filter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                partner.name.contains(searchQuery, ignoreCase = true) ||
                partner.bio.contains(searchQuery, ignoreCase = true) ||
                partner.nativeLang.contains(searchQuery, ignoreCase = true) ||
                partner.learningLang.contains(searchQuery, ignoreCase = true) ||
                partner.interests.any { it.contains(searchQuery, ignoreCase = true) }
            }

            // Category filter
            val matchesCategory = when (activeCategoryFilter) {
                "Online" -> partner.isOnline
                "Same Vibe" -> partner.vibe == "Same Vibe"
                "Interests" -> if (selectedInterestTag != null) partner.interests.contains(selectedInterestTag) else true
                else -> true
            }

            // Language filter
            val matchesLanguage = if (selectedLanguageFilter == "All") true else {
                partner.nativeLang.equals(selectedLanguageFilter, ignoreCase = true) ||
                partner.learningLang.equals(selectedLanguageFilter, ignoreCase = true)
            }

            matchesSearch && matchesCategory && matchesLanguage
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAEE))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Header (matching Image 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo & Tagline
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Funky",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E1E1E)
                        )
                        Text(
                            text = "Talk",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "✦",
                            fontSize = 14.sp,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        text = "Real People. Real Conversations.",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Action Icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                            .clickable { isSearchActive = !isSearchActive },
                        contentAlignment = Alignment.Center
                    ) {
                        FunkySearchIcon(
                            color = if (isSearchActive) Color(0xFFD97706) else Color.Black,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Filter Sliders Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                            .clickable { showFilterSettingsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        FunkySlidersIcon(
                            color = Color.Black,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // 2. Expandable Search Bar
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("Search by name, country, interest, language...", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD43B),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }

            // 3. Filter Chips Row (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All Chip
                DiscoverFilterChip(
                    text = "All",
                    isSelected = activeCategoryFilter == "All",
                    onClick = {
                        activeCategoryFilter = "All"
                        selectedInterestTag = null
                        selectedLanguageFilter = "All"
                    }
                )

                // Online Chip with green dot
                DiscoverFilterChip(
                    text = "Online",
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                    },
                    isSelected = activeCategoryFilter == "Online",
                    onClick = {
                        activeCategoryFilter = if (activeCategoryFilter == "Online") "All" else "Online"
                    }
                )

                // Same Vibe Chip
                DiscoverFilterChip(
                    text = "Same Vibe",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (activeCategoryFilter == "Same Vibe") Color.Black else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    isSelected = activeCategoryFilter == "Same Vibe",
                    onClick = {
                        activeCategoryFilter = if (activeCategoryFilter == "Same Vibe") "All" else "Same Vibe"
                    }
                )

                // Interests Chip
                DiscoverFilterChip(
                    text = selectedInterestTag ?: "Interests",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = if (activeCategoryFilter == "Interests" || selectedInterestTag != null) Color.Black else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    isSelected = activeCategoryFilter == "Interests" || selectedInterestTag != null,
                    onClick = {
                        showInterestsSheet = true
                    }
                )

                // Language Dropdown Chip
                DiscoverFilterChip(
                    text = if (selectedLanguageFilter != "All") "Language: $selectedLanguageFilter" else "Language",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = if (selectedLanguageFilter != "All") Color.Black else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (selectedLanguageFilter != "All") Color.Black else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    isSelected = selectedLanguageFilter != "All",
                    onClick = {
                        showLanguageSheet = true
                    }
                )

                // Quick Lounge Action Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFFBEB))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(20.dp))
                        .clickable { onCreateRoom() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FunkyMicIcon(color = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start Lounge",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }

            // 4. Discover Cards List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp)
            ) {
                // Empty state
                if (filteredPartners.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                FunkySearchIcon(
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No partners found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try resetting filters or searching with another keyword.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    activeCategoryFilter = "All"
                                    selectedLanguageFilter = "All"
                                    selectedInterestTag = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE380)),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text("Reset All Filters", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    items(filteredPartners, key = { it.id }) { partner ->
                        DiscoverUserCard(
                            partner = partner,
                            onCardClick = { profileSheetUser = partner },
                            onWaveClick = {
                                if (!partner.isWaved) {
                                    waveDialogUser = partner
                                } else {
                                    Toast.makeText(context, "Already waved to ${partner.name}!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal: Quick Icebreaker / Wave Dialog
    waveDialogUser?.let { targetUser ->
        AlertDialog(
            onDismissRequest = { waveDialogUser = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FunkyWaveHandIcon(
                        color = Color(0xFFD97706),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Say Hi to ${targetUser.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Pick an icebreaker or write your own message to start a friendly conversation!",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val quickPrompts = listOf(
                        "Hey ${targetUser.name}, want to practice speaking together?",
                        "Loved your profile! Let's do a language exchange.",
                        "Hi there! What kind of ${targetUser.interests.firstOrNull() ?: "music"} do you like?"
                    )

                    quickPrompts.forEach { prompt ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable { waveCustomMessage = prompt }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 12.sp,
                                color = if (waveCustomMessage == prompt) Color(0xFFD97706) else Color(0xFF334155),
                                fontWeight = if (waveCustomMessage == prompt) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = waveCustomMessage,
                        onValueChange = { waveCustomMessage = it },
                        placeholder = { Text("Or type a custom greeting...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color(0xFFFAFAFA),
                            focusedBorderColor = Color(0xFFFFD43B),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Mark as waved
                        partnersList = partnersList.map {
                            if (it.id == targetUser.id) it.copy(isWaved = true) else it
                        }
                        val greeting = waveCustomMessage.ifBlank { "Waved hello!" }
                        Toast.makeText(context, "Wave sent to ${targetUser.name}!", Toast.LENGTH_SHORT).show()
                        onStartDirectChat(targetUser.id, targetUser.name, targetUser.avatarUrl, targetUser.countryCode)
                        waveDialogUser = null
                        waveCustomMessage = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD43B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FunkyWaveHandIcon(color = Color.Black, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Wave", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { waveDialogUser = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Modal: Full Profile Bottom Sheet
    profileSheetUser?.let { user ->
        ModalBottomSheet(
            onDismissRequest = { profileSheetUser = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with flag
                AvatarWithCircularFlag(
                    avatarUrl = user.avatarUrl,
                    countryCode = user.countryCode,
                    avatarSize = 84.dp,
                    flagSize = 28.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${user.age}",
                        fontSize = 18.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                if (user.isOnline) Color(0xFF22C55E) else Color(0xFFCBD5E1),
                                CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Language pair badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Native: ", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text(user.nativeLang, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("  ⇄  ", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Text("Learning: ", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text(user.learningLang, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "\"${user.bio}\"",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF334155),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interests Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    user.interests.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .background(Color(0xFFFDF2F8), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(text = tag, fontSize = 11.sp, color = Color(0xFFBE185D), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            profileSheetUser = null
                            waveDialogUser = user
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD43B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FunkyWaveHandIcon(color = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Say Hi", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            profileSheetUser = null
                            onStartDirectChat(user.id, user.name, user.avatarUrl, user.countryCode)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Message", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal: Language Filter Sheet
    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Filter by Language Exchange",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(14.dp))

                val availableLangs = listOf(
                    "All" to ("All Languages" to null),
                    "EN" to ("English (EN)" to "us"),
                    "KO" to ("Korean (KO)" to "kr"),
                    "JA" to ("Japanese (JA)" to "jp"),
                    "CN" to ("Chinese (CN)" to "cn"),
                    "ID" to ("Indonesian (ID)" to "id"),
                    "ES" to ("Spanish (ES)" to "es"),
                    "FR" to ("French (FR)" to "fr"),
                    "HI" to ("Hindi (HI)" to "in")
                )

                availableLangs.forEach { (code, data) ->
                    val (label, countryCode) = data
                    val isSelected = selectedLanguageFilter == code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFFFFFBEB) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isSelected) Color(0xFFF59E0B) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .clickable {
                                selectedLanguageFilter = code
                                showLanguageSheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (countryCode != null) {
                                CircleFlag(countryCode = countryCode, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                            } else {
                                Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF92400E) else Color(0xFF1E293B)
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal: Interests Filter Sheet
    if (showInterestsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInterestsSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Filter by Interests & Hobbies",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(14.dp))

                val interestsList = listOf(
                    "Anime", "Music", "K-Drama", "Food", "Travel", "Gaming",
                    "Photography", "Art", "Movies", "Cats", "Books", "Sports", "Tech", "Coding"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedInterestTag == null,
                        onClick = {
                            selectedInterestTag = null
                            activeCategoryFilter = "All"
                            showInterestsSheet = false
                        },
                        label = { Text("All Interests") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grid/Wrap of interest chips
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(240.dp)
                ) {
                    items(interestsList.size) { idx ->
                        val item = interestsList[idx]
                        val isSelected = selectedInterestTag == item
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFFFFE380) else Color(0xFFF1F5F9))
                                .border(1.dp, if (isSelected) Color(0xFFEAB308) else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable {
                                    selectedInterestTag = item
                                    activeCategoryFilter = "Interests"
                                    showInterestsSheet = false
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color(0xFF334155)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal: Preferences / Filter Settings Sheet
    if (showFilterSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSettingsSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discovery Preferences",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    TextButton(onClick = {
                        activeCategoryFilter = "All"
                        selectedLanguageFilter = "All"
                        selectedInterestTag = null
                        showFilterSettingsSheet = false
                    }) {
                        Text("Reset", color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Show Only Active", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .clickable {
                            activeCategoryFilter = if (activeCategoryFilter == "Online") "All" else "Online"
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Only Online Users", fontSize = 13.sp, color = Color(0xFF1E293B))
                    }
                    Switch(
                        checked = activeCategoryFilter == "Online",
                        onCheckedChange = {
                            activeCategoryFilter = if (it) "Online" else "All"
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD97706), checkedTrackColor = Color(0xFFFFE380))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { showFilterSettingsSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD43B)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Apply Preferences", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DiscoverFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0xFFFFE380) else Color.White)
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFFFDE68A) else Color(0xFFE5E7EB),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            leadingIcon?.invoke()
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.Black else Color(0xFF1F2937)
            )
            trailingIcon?.invoke()
        }
    }
}

@Composable
fun DiscoverUserCard(
    partner: DiscoverPartner,
    onCardClick: () -> Unit,
    onWaveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with overlapping Country Flag
            AvatarWithCircularFlag(
                avatarUrl = partner.avatarUrl,
                countryCode = partner.countryCode,
                avatarSize = 62.dp,
                flagSize = 22.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Info Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Name, Age & Online Dot
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = partner.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${partner.age}",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Online Dot
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (partner.isOnline) Color(0xFF22C55E) else Color(0xFFCBD5E1),
                                CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Language Exchange Pair: e.g. EN ⇄ KO
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = partner.nativeLang,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = " ⇄ ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = partner.learningLang,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD97706)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Bio
                Text(
                    text = partner.bio,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Interest Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayTags = partner.interests.take(3)
                    val remaining = partner.interests.size - displayTags.size

                    displayTags.forEachIndexed { i, tag ->
                        val bg = when (i % 3) {
                            0 -> Color(0xFFFDF2F8) // Pink
                            1 -> Color(0xFFF3E8FF) // Lavender
                            else -> Color(0xFFE0F2FE) // Sky
                        }
                        val textCol = when (i % 3) {
                            0 -> Color(0xFFBE185D)
                            1 -> Color(0xFF7E22CE)
                            else -> Color(0xFF0369A1)
                        }

                        Box(
                            modifier = Modifier
                                .background(bg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = textCol
                            )
                        }
                    }

                    if (remaining > 0) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+$remaining",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action: "Hi" Button with vector icon (no 3 dots)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (partner.isWaved) Color(0xFFDCFCE7) else Color(0xFFFFD43B)
                    )
                    .clickable { onWaveClick() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (partner.isWaved) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Sent",
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sent",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF15803D)
                        )
                    } else {
                        FunkyWaveHandIcon(
                            color = Color(0xFF1E293B),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Hi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }
    }
}
