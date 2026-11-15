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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalFunkyColors
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
    onStartDirectChat: (userId: String, name: String, avatarUrl: String, flagCode: String) -> Unit = { _, _, _, _ -> },
    onOverlayVisibilityChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val isDark = LocalAppDarkTheme.current

    // Interactive States
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeCategoryFilter by remember { mutableStateOf("All") } // "All", "Online", "Same Vibe", "Interests", "Language"
    var selectedLanguageFilter by remember { mutableStateOf("All") }
    var languageFilterType by remember { mutableStateOf("They speak") }
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
                val partnerIds = partnersList.map { it.id }.filter { it.isNotBlank() }.distinct()
                if (partnerIds.isNotEmpty()) {
                    com.example.store.CosmeticsManager.batchFetchUserCosmetics(context, partnerIds)
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
    val filteredPartners = remember(partnersList, searchQuery, activeCategoryFilter, selectedLanguageFilter, languageFilterType, selectedInterestTag) {
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
                "Interests" -> selectedInterestTag == null || partner.interests.any { it.equals(selectedInterestTag, ignoreCase = true) }
                else -> true
            }

            // Language filter ("They speak" checks native language, "They're learning" checks learning language)
            val matchesLanguage = if (selectedLanguageFilter == "All") true else {
                if (languageFilterType == "They're learning") {
                    partner.learningLang.split(" ").any { it.equals(selectedLanguageFilter, ignoreCase = true) } ||
                    partner.learningLangFull.contains(selectedLanguageFilter, ignoreCase = true)
                } else {
                    partner.nativeLang.split(" ").any { it.equals(selectedLanguageFilter, ignoreCase = true) } ||
                    partner.nativeLangFull.contains(selectedLanguageFilter, ignoreCase = true)
                }
            }

            matchesSearch && matchesCategory && matchesLanguage
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(funkyColors.bg)
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
                            color = funkyColors.ink
                        )
                        Text(
                            text = "Talk.",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            color = funkyColors.mango
                        )
                    }
                    Text(
                        text = "Real People. Real Conversations.",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = funkyColors.mute,
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
                            .background(funkyColors.surface)
                            .border(1.dp, funkyColors.line, CircleShape)
                            .clickable { isSearchActive = !isSearchActive },
                        contentAlignment = Alignment.Center
                    ) {
                        FunkySearchIcon(
                            color = if (isSearchActive) funkyColors.mango else funkyColors.ink,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Filter Sliders Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(funkyColors.surface)
                            .border(1.dp, funkyColors.line, CircleShape)
                            .clickable { showFilterSettingsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        FunkySlidersIcon(
                            color = funkyColors.ink,
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
                            Text("Search by name, country, interest, language...", fontSize = 13.sp, color = funkyColors.mute)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    SoftCloseIcon(color = funkyColors.mute, size = 16.dp)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = funkyColors.surface,
                            unfocusedContainerColor = funkyColors.surface,
                            focusedTextColor = funkyColors.ink,
                            unfocusedTextColor = funkyColors.ink,
                            focusedBorderColor = funkyColors.mango,
                            unfocusedBorderColor = funkyColors.line
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
                    isSelected = activeCategoryFilter == "All" && selectedLanguageFilter == "All",
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
                                .background(funkyColors.ok, CircleShape)
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
                            color = if (activeCategoryFilter == "Same Vibe") funkyColors.onMango else funkyColors.mute,
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
                            color = if (selectedLanguageFilter != "All") funkyColors.onMango else funkyColors.mute,
                            size = 15.dp
                        )
                    },
                    trailingIcon = {
                        SoftChevronDownIcon(
                            color = if (selectedLanguageFilter != "All") funkyColors.onMango else funkyColors.mute,
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
                        .background(funkyColors.soft)
                        .border(1.dp, funkyColors.mango.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .clickable { onCreateRoom() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FunkyMicIcon(color = funkyColors.mango, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start Lounge",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) funkyColors.mango else Color(0xFFB45309)
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
                                color = funkyColors.mango
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
                                color = funkyColors.danger,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { refreshTrigger++ },
                                colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text("Retry Connection", color = funkyColors.onMango, fontWeight = FontWeight.Bold)
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
                                    .background(funkyColors.soft),
                                contentAlignment = Alignment.Center
                            ) {
                                FunkySearchIcon(
                                    color = funkyColors.mute,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No partners found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try resetting filters or searching with another keyword.",
                                fontSize = 13.sp,
                                color = funkyColors.mute
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    activeCategoryFilter = "All"
                                    selectedLanguageFilter = "All"
                                    selectedInterestTag = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text("Reset All Filters", color = funkyColors.onMango, fontWeight = FontWeight.Bold)
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
            containerColor = funkyColors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftWavingHandIcon(
                        color = funkyColors.mango,
                        size = 22.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Say Hi to ${targetUser.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = funkyColors.ink
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Pick an icebreaker or write your own message to start a friendly conversation!",
                        fontSize = 13.sp,
                        color = funkyColors.mute
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val quickPrompts = listOf(
                        "Hey ${targetUser.name}, want to practice speaking together?",
                        "Loved your profile! Let's do a language exchange.",
                        "Hi there! What kind of ${targetUser.interests.firstOrNull() ?: "music"} do you like?"
                    )

                    quickPrompts.forEach { prompt ->
                        val isPromptSelected = waveCustomMessage == prompt
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isPromptSelected) funkyColors.soft else funkyColors.bg)
                                .border(
                                    1.dp,
                                    if (isPromptSelected) funkyColors.mango else funkyColors.line,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { waveCustomMessage = prompt }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 12.sp,
                                color = if (isPromptSelected) funkyColors.mango else funkyColors.ink,
                                fontWeight = if (isPromptSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = waveCustomMessage,
                        onValueChange = { waveCustomMessage = it },
                        placeholder = { Text("Or type a custom greeting...", fontSize = 12.sp, color = funkyColors.mute) },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = funkyColors.bg,
                            unfocusedContainerColor = funkyColors.bg,
                            focusedTextColor = funkyColors.ink,
                            unfocusedTextColor = funkyColors.ink,
                            focusedBorderColor = funkyColors.mango,
                            unfocusedBorderColor = funkyColors.line
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
                        Toast.makeText(context, "Wave sent to ${targetUser.name}!", Toast.LENGTH_SHORT).show()
                        onStartDirectChat(targetUser.id, targetUser.name, targetUser.avatarUrl, targetUser.countryCode)
                        waveDialogUser = null
                        waveCustomMessage = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftWavingHandIcon(color = funkyColors.onMango, size = 15.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Wave", color = funkyColors.onMango, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { waveDialogUser = null }) {
                    Text("Cancel", color = funkyColors.mute)
                }
            }
        )
    }

    // Modal: Full Profile Bottom Sheet
    profileSheetUser?.let { user ->
        ModalBottomSheet(
            onDismissRequest = { profileSheetUser = null },
            sheetState = profileSheetState,
            containerColor = funkyColors.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with presence & overlapping bottom-right flag
                com.example.ui.components.PresenceAvatar(
                    avatarUrl = user.avatarUrl,
                    size = 84.dp,
                    isOnline = user.isOnline,
                    countryCode = user.countryCode,
                    userId = user.id,
                    enableFrameAnimation = true,
                    showFlagBadge = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.example.store.DecoratedUserName(
                        userId = user.id,
                        name = user.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        defaultColor = funkyColors.ink
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${user.age}",
                        fontSize = 18.sp,
                        color = funkyColors.mute,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                if (user.isOnline) funkyColors.ok else funkyColors.mute.copy(alpha = 0.5f),
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
                        com.example.ui.screens.ProfileLocationPinIcon(modifier = Modifier.size(12.dp), color = funkyColors.mute)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = locStr, fontSize = 12.sp, color = funkyColors.mute, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Language pair badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(funkyColors.soft, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Native: ", fontSize = 12.sp, color = funkyColors.mute)
                    Text(user.nativeLangFull.ifBlank { user.nativeLang }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
                    Text("  ⇄  ", fontSize = 12.sp, color = funkyColors.mute, fontWeight = FontWeight.Bold)
                    Text("Learning: ", fontSize = 12.sp, color = funkyColors.mute)
                    Text(user.learningLangFull.ifBlank { user.learningLang }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = funkyColors.mango)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "\"${user.bio}\"",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = funkyColors.ink.copy(alpha = 0.85f),
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
                                .background(
                                    if (isDark) Color(0xFF3E1D2B) else Color(0xFFFDF2F8),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFF472B6) else Color(0xFFBE185D),
                                fontWeight = FontWeight.Bold
                            )
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
                        colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoftWavingHandIcon(color = funkyColors.onMango, size = 16.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Say Hi", color = funkyColors.onMango, fontWeight = FontWeight.Bold)
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
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) funkyColors.soft else funkyColors.ink),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chats),
                                contentDescription = null,
                                tint = if (isDark) funkyColors.ink else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Message",
                                color = if (isDark) funkyColors.ink else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    LaunchedEffect(showFullProfileScreenWithFeed) {
        onOverlayVisibilityChange(showFullProfileScreenWithFeed != null)
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
            containerColor = funkyColors.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(funkyColors.line)
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
                            color = funkyColors.ink
                        )
                        Text(
                            text = "Language Exchange",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = funkyColors.mango
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(funkyColors.soft)
                            .clickable { showLanguageSheet = false },
                        contentAlignment = Alignment.Center
                    ) {
                        SoftCloseIcon(size = 14.dp, color = funkyColors.mute)
                    }
                }

                Text(
                    text = "Find people to practice the languages you want.",
                    fontSize = 12.sp,
                    color = funkyColors.mute,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // "They speak" vs "They're learning" Segmented Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(funkyColors.soft)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (languageFilterType == "They speak") funkyColors.surface else Color.Transparent)
                            .clickable { languageFilterType = "They speak" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "They speak",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (languageFilterType == "They speak") funkyColors.ink else funkyColors.mute
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (languageFilterType == "They're learning") funkyColors.surface else Color.Transparent)
                            .clickable { languageFilterType = "They're learning" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "They're learning",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (languageFilterType == "They're learning") funkyColors.ink else funkyColors.mute
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Input
                var langSearchQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = langSearchQuery,
                    onValueChange = { langSearchQuery = it },
                    placeholder = { Text("Search languages...", fontSize = 13.sp, color = funkyColors.mute) },
                    leadingIcon = { FunkySearchIcon(color = funkyColors.mute, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = funkyColors.bg,
                        unfocusedContainerColor = funkyColors.bg,
                        focusedTextColor = funkyColors.ink,
                        unfocusedTextColor = funkyColors.ink,
                        focusedBorderColor = funkyColors.mango,
                        unfocusedBorderColor = funkyColors.line
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
                                    if (isSelected) funkyColors.soft else funkyColors.bg
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) funkyColors.mango else funkyColors.line,
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
                                    CircleFlag(
                                        countryCode = countryCode,
                                        allowProgramming = false,
                                        modifier = Modifier.size(22.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                } else {
                                    SoftGlobeIcon(size = 20.dp, color = if (isSelected) funkyColors.mango else funkyColors.mute)
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = funkyColors.ink
                                )
                            }

                            if (isSelected) {
                                SoftCheckmarkBadge(size = 20.dp, badgeColor = funkyColors.mango, checkColor = funkyColors.onMango)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, funkyColors.line, CircleShape)
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
                    colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(28.dp))
                        Text("Apply", color = funkyColors.onMango, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(funkyColors.onMango),
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
            containerColor = funkyColors.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(funkyColors.line)
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
                            color = funkyColors.ink
                        )
                        Text(
                            text = "Interests & Hobbies",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = funkyColors.mango
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(funkyColors.soft)
                            .clickable { showInterestsSheet = false },
                        contentAlignment = Alignment.Center
                    ) {
                        SoftCloseIcon(size = 14.dp, color = funkyColors.mute)
                    }
                }

                Text(
                    text = "Find people who share your interests.",
                    fontSize = 12.sp,
                    color = funkyColors.mute,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Search Input
                var interestSearchQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = interestSearchQuery,
                    onValueChange = { interestSearchQuery = it },
                    placeholder = { Text("Search interests...", fontSize = 13.sp, color = funkyColors.mute) },
                    leadingIcon = { FunkySearchIcon(color = funkyColors.mute, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = funkyColors.bg,
                        unfocusedContainerColor = funkyColors.bg,
                        focusedTextColor = funkyColors.ink,
                        unfocusedTextColor = funkyColors.ink,
                        focusedBorderColor = funkyColors.mango,
                        unfocusedBorderColor = funkyColors.line
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
                                .background(if (isAllSelected) funkyColors.soft else funkyColors.bg)
                                .border(
                                    width = if (isAllSelected) 1.5.dp else 1.dp,
                                    color = if (isAllSelected) funkyColors.mango else funkyColors.line,
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
                                    color = if (isAllSelected) funkyColors.ink else funkyColors.mute
                                )
                                if (isAllSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    SoftCheckmarkBadge(size = 14.dp, badgeColor = funkyColors.mango, checkColor = funkyColors.onMango)
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
                                .background(if (isSelected) funkyColors.soft else funkyColors.bg)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) funkyColors.mango else funkyColors.line,
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
                                color = if (isSelected) funkyColors.ink else funkyColors.mute
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
                    colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(28.dp))
                        Text("Apply", color = funkyColors.onMango, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(funkyColors.onMango),
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
            containerColor = funkyColors.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(funkyColors.line)
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
                        color = funkyColors.ink
                    )
                    TextButton(onClick = {
                        activeCategoryFilter = "All"
                        selectedLanguageFilter = "All"
                        selectedInterestTag = null
                        showFilterSettingsSheet = false
                    }) {
                        Text("Reset", color = funkyColors.mango, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Show Only Active", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = funkyColors.mute)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(funkyColors.bg)
                        .border(1.dp, funkyColors.line, RoundedCornerShape(18.dp))
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
                                .background(funkyColors.ok, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Only Online Users", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = funkyColors.ink)
                    }
                    Switch(
                        checked = activeCategoryFilter == "Online",
                        onCheckedChange = {
                            activeCategoryFilter = if (it) "Online" else "All"
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = funkyColors.onMango,
                            checkedTrackColor = funkyColors.mango
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showFilterSettingsSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("Apply Preferences", color = funkyColors.onMango, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
    val funkyColors = LocalFunkyColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) funkyColors.mango else funkyColors.surface)
            .border(
                width = 1.dp,
                color = if (isSelected) funkyColors.mango else funkyColors.line,
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
                color = if (isSelected) funkyColors.onMango else funkyColors.ink
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
    val funkyColors = LocalFunkyColors.current
    val isDark = LocalAppDarkTheme.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
        border = BorderStroke(1.dp, funkyColors.line)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rounded-Square Avatar with bottom-right overlapping country flag
                DiscoverSquareAvatarWithFlag(
                    avatarUrl = partner.avatarUrl,
                    countryCode = partner.countryCode,
                    isOnline = partner.isOnline,
                    avatarSize = 80.dp,
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
                        com.example.store.DecoratedUserName(
                            userId = partner.id,
                            name = partner.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            defaultColor = funkyColors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "${partner.age}",
                            fontSize = 13.sp,
                            color = funkyColors.mute,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (partner.isOnline) {
                                        if (isDark) Color(0xFF143524) else Color(0xFFE6F4EA)
                                    } else {
                                        funkyColors.soft
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (partner.isOnline) Color(0xFF22C55E) else funkyColors.mute)
                                )
                                Text(
                                    text = if (partner.isOnline) "Online" else "Offline",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (partner.isOnline) {
                                        if (isDark) Color(0xFF4ADE80) else Color(0xFF137333)
                                    } else {
                                        funkyColors.mute
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Language Exchange: e.g., hi en mr -> zh ko jp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = partner.nativeLang.lowercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = funkyColors.mute
                        )
                        Text(
                            text = "→",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.mute
                        )
                        Text(
                            text = partner.learningLang.lowercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = funkyColors.mango
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Bio
                    Text(
                        text = partner.bio,
                        fontSize = 12.sp,
                        color = funkyColors.mute,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Hi / Sent Action Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (partner.isWaved) {
                                if (isDark) Color(0xFF143524) else Color(0xFFDCFCE7)
                            } else {
                                funkyColors.mango
                            }
                        )
                        .clickable { onWaveClick() }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
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
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                            )
                        } else {
                            SoftWavingHandIcon(
                                color = funkyColors.onMango,
                                size = 15.dp
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Hi",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = funkyColors.onMango
                            )
                        }
                    }
                }
            }
        }
    }
}
