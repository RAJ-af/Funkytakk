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
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.DiscoverableProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.ui.components.AvatarWithCircularFlag
import com.example.ui.components.CircleFlag
import com.example.ui.components.FunkyMicIcon
import com.example.ui.components.FunkySearchIcon
import com.example.ui.components.FunkySlidersIcon
import com.example.ui.components.FunkyWaveHandIcon
import com.example.ui.components.SoftWavingHandIcon
import com.example.ui.components.SoftSwapIcon
import com.example.ui.components.SoftCheckmarkBadge
import com.example.ui.components.SoftStarIcon
import com.example.ui.components.SoftGamepadIcon
import com.example.ui.components.SoftGlobeIcon
import com.example.ui.components.SoftChevronDownIcon
import com.example.ui.components.SoftThreeDotsIcon
import com.example.ui.components.SoftCloseIcon
import com.example.ui.components.DiscoverSquareAvatarWithFlag
import com.example.ui.components.UserAvatarImage
import com.example.ui.screens.LanguageData

data class DiscoverPartner(
    val id: String,
    val name: String,
    val username: String = "",
    val age: Int,
    val isOnline: Boolean,
    val avatarUrl: String,
    val coverUrl: String? = null,
    val countryCode: String,
    val nativeLang: String,
    val learningLang: String,
    val bio: String,
    val interests: List<String>,
    val vibe: String = "Chill",
    val isWaved: Boolean = false,
    val nativeLangFull: String = "",
    val learningLangFull: String = "",
    val city: String = "",
    val country: String = ""
)

fun formatLanguageToCode(langStr: String): String {
    if (langStr.isBlank()) return "EN"
    // e.g. "CHINESE (NATIVE / FLUENT)" -> "CHINESE"
    val nameOnly = langStr.substringBefore("(").trim()
    // Find in LanguageData
    val matched = LanguageData.allLanguages.find {
        it.name.equals(nameOnly, ignoreCase = true) ||
        it.nativeName.equals(nameOnly, ignoreCase = true) ||
        it.code.equals(nameOnly, ignoreCase = true)
    }
    if (matched != null) {
        return matched.code.uppercase()
    }
    // Fallbacks for common variations
    return when (nameOnly.uppercase()) {
        "CHINESE" -> "ZH"
        "JAPANESE" -> "JA"
        "KOREAN" -> "KO"
        "ENGLISH" -> "EN"
        "SPANISH" -> "ES"
        "FRENCH" -> "FR"
        "GERMAN" -> "DE"
        "HINDI" -> "HI"
        "ARABIC" -> "AR"
        "PORTUGUESE" -> "PT"
        "BENGALI" -> "BN"
        "RUSSIAN" -> "RU"
        "TURKISH" -> "TR"
        "VIETNAMESE" -> "VI"
        "THAI" -> "TH"
        "ITALIAN" -> "IT"
        "DUTCH" -> "NL"
        "POLISH" -> "PL"
        "INDONESIAN" -> "ID"
        "FILIPINO" -> "TL"
        "TAGALOG" -> "TL"
        "PUNJABI" -> "PA"
        "TAMIL" -> "TA"
        "TELUGU" -> "TE"
        "URDU" -> "UR"
        "SWEDISH" -> "SV"
        "MALAY" -> "MS"
        "SWAHILI" -> "SW"
        "UKRAINIAN" -> "UK"
        "GREEK" -> "EL"
        "HEBREW" -> "HE"
        "IRISH" -> "GA"
        "LATIN" -> "LA"
        "PERSIAN" -> "FA"
        "FARSI" -> "FA"
        "SANSKRIT" -> "SA"
        else -> nameOnly.take(2).uppercase()
    }
}

fun formatLanguagesToCodes(langsList: List<String>?): String {
    if (langsList.isNullOrEmpty()) return ""
    return langsList.map { formatLanguageToCode(it) }.distinct().joinToString(" ")
}

fun resolveCountryCode(countryName: String?, dbCountryCode: String?): String {
    if (!dbCountryCode.isNullOrBlank()) return dbCountryCode.trim().lowercase()
    if (countryName.isNullOrBlank()) return "us"
    return when (countryName.trim().lowercase()) {
        "india" -> "in"
        "korea", "south korea", "republic of korea" -> "kr"
        "japan" -> "jp"
        "malaysia" -> "my"
        "albania" -> "al"
        "united arab emirates", "uae" -> "ae"
        "united states", "usa", "us" -> "us"
        "united kingdom", "uk", "great britain" -> "gb"
        "canada" -> "ca"
        "australia" -> "au"
        "germany" -> "de"
        "france" -> "fr"
        "spain" -> "es"
        "italy" -> "it"
        "china" -> "cn"
        "brazil" -> "br"
        "russia" -> "ru"
        "saudi arabia" -> "sa"
        "indonesia" -> "id"
        "turkey" -> "tr"
        "vietnam" -> "vn"
        "thailand" -> "th"
        "philippines" -> "ph"
        "bangladesh" -> "bd"
        "pakistan" -> "pk"
        "netherlands" -> "nl"
        "poland" -> "pl"
        "ukraine" -> "ua"
        "sweden" -> "se"
        "switzerland" -> "ch"
        "egypt" -> "eg"
        "mexico" -> "mx"
        "colombia" -> "co"
        "argentina" -> "ar"
        else -> "us"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverTabContent(
    supabaseProfiles: List<UserProfile> = emptyList(),
    onCreateRoom: () -> Unit = {},
    onStartDirectChat: (userId: String, name: String, avatarUrl: String, flagCode: String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current

    // Interactive States
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeCategoryFilter by remember { mutableStateOf("All") } // "All", "Online", "Same Vibe", "Interests", "Language"
    var selectedLanguageFilter by remember { mutableStateOf("All") }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showInterestsSheet by remember { mutableStateOf(false) }
    var showFilterSettingsSheet by remember { mutableStateOf(false) }
    var selectedInterestTag by remember { mutableStateOf<String?>(null) }

    // Dynamic state of partners with wave tracking
    var partnersList by remember { mutableStateOf<List<DiscoverPartner>>(emptyList()) }
    var isDatabaseLoading by remember { mutableStateOf(false) }
    var databaseError by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // Fetch discoverable profiles dynamically from Supabase
    LaunchedEffect(activeCategoryFilter, selectedLanguageFilter, selectedInterestTag, refreshTrigger) {
        isDatabaseLoading = true
        databaseError = null
        try {
            val currentUserId = SupabaseSessionManager.getUserId(context).orEmpty()
            val token = SupabaseSessionManager.getBearerToken(context)
            val options = mutableMapOf<String, String>()
            
            // Sort by: is_online desc, last_seen desc
            options["order"] = "is_online.desc,last_seen.desc"
            options["limit"] = "50" // Paginate / don't fetch whole table
            
            // Exclude current user
            if (currentUserId.isNotBlank()) {
                options["auth_user_id"] = "neq.$currentUserId"
            }
            
            if (activeCategoryFilter == "Online") {
                options["is_online"] = "eq.true"
            }

            val response = withContext(Dispatchers.IO) {
                SupabaseClient.api.getDiscoverableProfiles(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    options = options
                )
            }

            if (response.isSuccessful) {
                val profiles = response.body().orEmpty()
                partnersList = profiles.map { p ->
                    val rawNativeList = p.native_languages.orEmpty()
                    val rawLearningList = p.learning_languages.orEmpty()
                    DiscoverPartner(
                        id = p.auth_user_id,
                        name = p.profile_name ?: p.username ?: "Anonymous",
                        username = p.username ?: "",
                        age = p.age ?: 21,
                        isOnline = p.is_online ?: false,
                        avatarUrl = p.avatar_url ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500",
                        coverUrl = p.cover_url,
                        countryCode = resolveCountryCode(p.country, p.country_code),
                        nativeLang = formatLanguagesToCodes(rawNativeList),
                        learningLang = formatLanguagesToCodes(rawLearningList),
                        bio = p.bio ?: "Excited to practice languages and make new friends!",
                        interests = p.hobbies ?: emptyList(),
                        vibe = if (p.is_online == true) "Same Vibe" else "Chill",
                        isWaved = false,
                        nativeLangFull = rawNativeList.joinToString(", "),
                        learningLangFull = rawLearningList.joinToString(", "),
                        city = p.city ?: "",
                        country = p.country ?: ""
                    )
                }
            } else {
                val errBody = response.errorBody()?.string()
                Log.e("DiscoverTab", "Failed to fetch discoverable profiles: $errBody")
                databaseError = "Failed to load profiles: ${response.message()}"
            }
        } catch (e: Exception) {
            Log.e("DiscoverTab", "Error loading discoverable profiles", e)
            databaseError = e.localizedMessage ?: "Unknown error occurred"
        } finally {
            isDatabaseLoading = false
        }
    }

    // User Interaction Modals
    var waveDialogUser by remember { mutableStateOf<DiscoverPartner?>(null) }
    var waveCustomMessage by remember { mutableStateOf("") }
    var profileSheetUser by remember { mutableStateOf<DiscoverPartner?>(null) }
    var showFullProfileScreenWithFeed by remember { mutableStateOf<DiscoverPartner?>(null) }

    val languageSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val interestsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filterSettingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val profileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Filter logic
    val filteredPartners = remember(partnersList, searchQuery, activeCategoryFilter, selectedLanguageFilter, selectedInterestTag) {
        partnersList.filter { partner ->
            // Search query filter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                partner.name.contains(searchQuery, ignoreCase = true) ||
                partner.username.contains(searchQuery, ignoreCase = true) ||
                partner.bio.contains(searchQuery, ignoreCase = true) ||
                partner.nativeLang.contains(searchQuery, ignoreCase = true) ||
                partner.learningLang.contains(searchQuery, ignoreCase = true) ||
                partner.nativeLangFull.contains(searchQuery, ignoreCase = true) ||
                partner.learningLangFull.contains(searchQuery, ignoreCase = true)
            }

            // Category filter
            val matchesCategory = when (activeCategoryFilter) {
                "Online" -> partner.isOnline
                "Same Vibe" -> partner.vibe == "Same Vibe"
                else -> true
            }

            // Language filter
            val matchesLanguage = if (selectedLanguageFilter == "All") true else {
                partner.nativeLang.split(" ").any { it.equals(selectedLanguageFilter, ignoreCase = true) } ||
                partner.learningLang.split(" ").any { it.equals(selectedLanguageFilter, ignoreCase = true) } ||
                partner.nativeLangFull.contains(selectedLanguageFilter, ignoreCase = true) ||
                partner.learningLangFull.contains(selectedLanguageFilter, ignoreCase = true)
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
                            text = "Talk.",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF59E0B)
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
                        SoftStarIcon(
                            color = if (activeCategoryFilter == "Same Vibe") Color.Black else Color(0xFF64748B),
                            size = 15.dp
                        )
                    },
                    isSelected = activeCategoryFilter == "Same Vibe",
                    onClick = {
                        activeCategoryFilter = if (activeCategoryFilter == "Same Vibe") "All" else "Same Vibe"
                    }
                )

                // Language Dropdown Chip
                DiscoverFilterChip(
                    text = if (selectedLanguageFilter != "All") "Language: $selectedLanguageFilter" else "Language",
                    leadingIcon = {
                        SoftGlobeIcon(
                            color = if (selectedLanguageFilter != "All") Color.Black else Color(0xFF64748B),
                            size = 15.dp
                        )
                    },
                    trailingIcon = {
                        SoftChevronDownIcon(
                            color = if (selectedLanguageFilter != "All") Color.Black else Color(0xFF64748B),
                            size = 14.dp
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
                if (isDatabaseLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFFD43B)
                            )
                        }
                    }
                } else if (databaseError != null) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Failed to load partners: $databaseError",
                                color = Color.Red,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { refreshTrigger++ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD43B)),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text("Retry Connection", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (filteredPartners.isEmpty()) {
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
                    items(filteredPartners.distinctBy { it.id }, key = { it.id }) { partner ->
                        DiscoverUserCard(
                            partner = partner,
                            onCardClick = { showFullProfileScreenWithFeed = partner },
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
                    SoftWavingHandIcon(
                        color = Color(0xFFD97706),
                        size = 22.dp
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
                        SoftWavingHandIcon(color = Color.Black, size = 15.dp)
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
            sheetState = profileSheetState,
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
                // Avatar with presence & flag
                com.example.ui.components.PresenceAvatar(
                    avatarUrl = user.avatarUrl,
                    size = 84.dp,
                    isOnline = user.isOnline,
                    countryCode = user.countryCode
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

                val locStr = when {
                    user.city.isNotBlank() && user.country.isNotBlank() -> {
                        if (user.city.equals(user.country, ignoreCase = true)) user.country
                        else "${user.city}, ${user.country}"
                    }
                    user.city.isNotBlank() -> user.city
                    user.country.isNotBlank() -> user.country
                    else -> ""
                }
                if (locStr.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.example.ui.screens.ProfileLocationPinIcon(modifier = Modifier.size(12.dp), color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = locStr, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    }
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
                    Text(user.nativeLangFull.ifBlank { user.nativeLang }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("  ⇄  ", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Text("Learning: ", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text(user.learningLangFull.ifBlank { user.learningLang }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
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
                            SoftWavingHandIcon(color = Color.Black, size = 16.dp)
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

    // Full Screen Profile Feed View
    showFullProfileScreenWithFeed?.let { partner ->
        com.example.ui.screens.UserProfileFeedScreen(
            userId = partner.id,
            username = partner.name.lowercase().replace(" ", ""),
            profileName = partner.name,
            avatarUrl = partner.avatarUrl,
            coverUrl = partner.coverUrl ?: "",
            countryCode = partner.countryCode,
            bio = partner.bio,
            onBack = { showFullProfileScreenWithFeed = null },
            onStartChat = {
                showFullProfileScreenWithFeed = null
                onStartDirectChat(partner.id, partner.name, partner.avatarUrl, partner.countryCode)
            }
        )
    }

    // Modal: Language Filter Sheet
    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            sheetState = languageSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCBD5E1))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Filter by ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Language Exchange",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF59E0B)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { showLanguageSheet = false },
                        contentAlignment = Alignment.Center
                    ) {
                        SoftCloseIcon(size = 14.dp, color = Color(0xFF64748B))
                    }
                }

                Text(
                    text = "Find people to practice the languages you want.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // "They speak" vs "They're learning" Segmented Control
                var languageFilterType by remember { mutableStateOf("They speak") }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (languageFilterType == "They speak") Color.White else Color.Transparent)
                            .clickable { languageFilterType = "They speak" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "They speak",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (languageFilterType == "They're learning") Color.White else Color.Transparent)
                            .clickable { languageFilterType = "They're learning" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "They're learning",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Input
                var langSearchQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = langSearchQuery,
                    onValueChange = { langSearchQuery = it },
                    placeholder = { Text("Search languages...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                    leadingIcon = { FunkySearchIcon(color = Color(0xFF94A3B8), modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedBorderColor = Color(0xFFFFD43B),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                val availableLangs = remember {
                    listOf("All" to ("All Languages" to null)) +
                    LanguageData.allLanguages.map { lang ->
                        lang.code.uppercase() to ("${lang.name} (${lang.code.uppercase()})" to lang.flagCode)
                    }
                }

                val filteredLangs = availableLangs.filter { (code, data) ->
                    langSearchQuery.isBlank() || data.first.contains(langSearchQuery, ignoreCase = true) || code.contains(langSearchQuery, ignoreCase = true)
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 340.dp)
                ) {
                    items(filteredLangs) { (code, data) ->
                        val (label, countryCode) = data
                        val isSelected = selectedLanguageFilter == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isSelected) Color(0xFFFFFBEB) else Color(0xFFF8FAFC)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    selectedLanguageFilter = code
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (countryCode != null) {
                                    CircleFlag(countryCode = countryCode, modifier = Modifier.size(22.dp).clip(CircleShape))
                                    Spacer(modifier = Modifier.width(12.dp))
                                } else {
                                    SoftGlobeIcon(size = 20.dp, color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF64748B))
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            if (isSelected) {
                                SoftCheckmarkBadge(size = 20.dp, badgeColor = Color(0xFFF59E0B), checkColor = Color.White)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, Color(0xFFCBD5E1), CircleShape)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Yellow Bottom Apply Button with Dark Count Badge
                Button(
                    onClick = { showLanguageSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD43B)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(28.dp))
                        Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Modal: Interests Filter Sheet
    if (showInterestsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInterestsSheet = false },
            sheetState = interestsSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCBD5E1))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Filter by ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Interests & Hobbies",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF59E0B)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { showInterestsSheet = false },
                        contentAlignment = Alignment.Center
                    ) {
                        SoftCloseIcon(size = 14.dp, color = Color(0xFF64748B))
                    }
                }

                Text(
                    text = "Find people who share your interests.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Search Input
                var interestSearchQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = interestSearchQuery,
                    onValueChange = { interestSearchQuery = it },
                    placeholder = { Text("Search interests...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                    leadingIcon = { FunkySearchIcon(color = Color(0xFF94A3B8), modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedBorderColor = Color(0xFFFFD43B),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                val interestsList = listOf(
                    "Anime", "Music", "K-Drama", "Food", "Travel", "Gaming",
                    "Photography", "Art", "Movies", "Cats", "Books", "Sports", "Tech", "Coding"
                ).filter { interestSearchQuery.isBlank() || it.contains(interestSearchQuery, ignoreCase = true) }

                // Grid of 3 columns of interest chips
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(260.dp)
                ) {
                    // Item 0: All Interests
                    item {
                        val isAllSelected = selectedInterestTag == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isAllSelected) Color(0xFFFFF0B3) else Color(0xFFF8FAFC))
                                .border(
                                    width = if (isAllSelected) 1.5.dp else 1.dp,
                                    color = if (isAllSelected) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    selectedInterestTag = null
                                    activeCategoryFilter = "All"
                                }
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "All Interests",
                                    fontSize = 12.sp,
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isAllSelected) Color(0xFF0F172A) else Color(0xFF475569)
                                )
                                if (isAllSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    SoftCheckmarkBadge(size = 14.dp, badgeColor = Color(0xFFF59E0B), checkColor = Color.White)
                                }
                            }
                        }
                    }

                    items(interestsList.size) { idx ->
                        val item = interestsList[idx]
                        val isSelected = selectedInterestTag == item
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) Color(0xFFFFF0B3) else Color(0xFFF8FAFC))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    selectedInterestTag = item
                                    activeCategoryFilter = "Interests"
                                }
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Yellow Bottom Apply Button with Dark Count Badge
                Button(
                    onClick = { showInterestsSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD43B)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(28.dp))
                        Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Modal: Preferences / Filter Settings Sheet
    if (showFilterSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSettingsSheet = false },
            sheetState = filterSettingsSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCBD5E1))
                )
            }
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                    TextButton(onClick = {
                        activeCategoryFilter = "All"
                        selectedLanguageFilter = "All"
                        selectedInterestTag = null
                        showFilterSettingsSheet = false
                    }) {
                        Text("Reset", color = Color(0xFFD97706), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Show Only Active", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                        .clickable {
                            activeCategoryFilter = if (activeCategoryFilter == "Online") "All" else "Online"
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Only Online Users", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                    }
                    Switch(
                        checked = activeCategoryFilter == "Online",
                        onCheckedChange = {
                            activeCategoryFilter = if (it) "Online" else "All"
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFFFFC529)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showFilterSettingsSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC529)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("Apply Preferences", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Rounded-Square Avatar with Flag & Online Dot
                DiscoverSquareAvatarWithFlag(
                    avatarUrl = partner.avatarUrl,
                    countryCode = partner.countryCode,
                    isOnline = partner.isOnline,
                    avatarSize = 82.dp,
                    flagSize = 24.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Info Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Name, Age & Online/Offline Pill Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = partner.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${partner.age}",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        // Online / Offline Status Badge with dot inside and increased roundness
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (partner.isOnline) Color(0xFFE6F4EA) else Color(0xFFF1F5F9))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (partner.isOnline) Color(0xFF22C55E) else Color(0xFF94A3B8))
                                )
                                Text(
                                    text = if (partner.isOnline) "Online" else "Offline",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (partner.isOnline) Color(0xFF137333) else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Language Exchange: e.g., hi en mr → zh ko jp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = partner.nativeLang.lowercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF475569) // slate gray
                        )
                        Text(
                            text = "→",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8) // light gray
                        )
                        Text(
                            text = partner.learningLang.lowercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD97706) // FunkyTalk warm yellow/amber
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
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Action: "Hi" Button on the RIGHT
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (partner.isWaved) Color(0xFFDCFCE7) else Color(0xFFFFD43B)
                        )
                        .clickable { onWaveClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (partner.isWaved) {
                            SoftCheckmarkBadge(
                                size = 15.dp,
                                badgeColor = Color(0xFF15803D),
                                checkColor = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sent",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D)
                            )
                        } else {
                            SoftWavingHandIcon(
                                color = Color(0xFF1E293B),
                                size = 15.dp
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
}
