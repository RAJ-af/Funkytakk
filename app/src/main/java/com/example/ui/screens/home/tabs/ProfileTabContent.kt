package com.example.ui.screens.home.tabs

import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.AuthViewModel
import com.example.BuildConfig
import com.example.GiftTransactionItem
import com.example.ModerationManager
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.UserProfile
import com.example.gifting.AnimatedGiftMedia
import com.example.gifting.CuteCoinIcon
import com.example.gifting.GiftManager
import com.example.repository.MomentsRepository
import com.example.ui.components.CircleFlag
import com.example.ui.components.DynamicHobbyChip
import com.example.ui.components.LanguageItem
import com.example.ui.components.LanguageResolver
import com.example.ui.components.MultiLanguageExchangeCard
import com.example.ui.components.PresetAvatarList
import com.example.ui.components.ProfileBadgesRow
import com.example.ui.components.MomentsV2FeedViewerDialog
import com.example.ui.components.toFallbackMomentDto
import com.example.ui.components.SoftBlockIcon
import com.example.ui.components.Soft3DCloudIcon
import com.example.ui.components.resolveAvatarModel
import com.example.ui.screens.*
import com.example.ui.screens.profile.ChatBackupScreen
import com.example.ui.screens.home.dialogs.BlockedUsersBottomSheet
import com.example.ui.screens.home.dialogs.ModerationReviewDialog
import com.example.ui.screens.home.models.DarkText
import com.example.ui.screens.profile.EditProfileScreen
import com.example.ui.screens.home.models.GraySubtitle
import com.example.ui.theme.GoldAccent
import com.example.ui.screens.profile.SettingsScreen
import com.example.ui.screens.store.CoinStoreScreen
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileTabContent(
    currentUserProfile: UserProfile,
    onLogoutClick: () -> Unit,
    viewModel: AuthViewModel? = null,
    onOpenSupportChat: (reportId: String?) -> Unit = {},
    onDeleteAccountClick: () -> Unit = {},
    onOverlayVisibilityChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    var selectedMainTab by remember { mutableStateOf(0) } // 0 = About Me, 1 = Feed
    var selectedFeedTab by remember { mutableStateOf(0) } // 0 = Grid, 1 = Video, 2 = Tagged
    var activeFullPost by remember { mutableStateOf<FeedPost?>(null) }
    var isFollowing by remember { mutableStateOf(false) }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showModerationDialog by remember { mutableStateOf(false) }
    var showBlockedUsersSheet by remember { mutableStateOf(false) }
    var showBlockedGroupsSheet by remember { mutableStateOf(false) }
    var showChatBackupScreen by remember { mutableStateOf(false) }
    var showChatAppearanceSheet by remember { mutableStateOf(false) }
    var showThemeStoreSheet by remember { mutableStateOf(false) }
    var showCoinStoreScreen by remember { mutableStateOf(false) }
    var showChatKitGalleryDebug by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }

    var activeFollowerUserId by remember { mutableStateOf<String?>(null) }
    var activeFollowersMode by remember { mutableStateOf(true) } // true = followers, false = following
    var activeClickedProfile by remember { mutableStateOf<com.example.DiscoverableProfile?>(null) }

    val anyOverlayOpen = showEditProfileDialog ||
        showSettingsScreen ||
        showModerationDialog ||
        showBlockedUsersSheet ||
        showBlockedGroupsSheet ||
        showChatBackupScreen ||
        showChatAppearanceSheet ||
        showThemeStoreSheet ||
        showCoinStoreScreen ||
        showChatKitGalleryDebug ||
        activeFullPost != null ||
        activeFollowerUserId != null ||
        activeClickedProfile != null

    LaunchedEffect(anyOverlayOpen) {
        onOverlayVisibilityChange(anyOverlayOpen)
    }

    DisposableEffect(Unit) {
        onDispose { onOverlayVisibilityChange(false) }
    }

    val userId = currentUserProfile.uid
    val prefs = remember { context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE) }
    val supaUid = remember { com.example.SupabaseSessionManager.getUserId(context) ?: userId }
    val savedBio = remember { prefs.getString("profile_bio_${userId}", null) }
    val savedCity = remember { prefs.getString("profile_city_${userId}", null) }
    val savedAge = remember { prefs.getInt("profile_age_${userId}", 0).takeIf { it > 0 } }
    val savedCover = remember {
        prefs.getString("profile_cover_${userId}", null)
            ?: (if (supaUid != userId) prefs.getString("profile_cover_${supaUid}", null) else null)
    }

    var currentProfileName by remember(currentUserProfile.displayName) { mutableStateOf(currentUserProfile.displayName?.ifBlank { "pikaa" } ?: "pikaa") }
    var currentUsername by remember(currentUserProfile.username) { mutableStateOf(currentUserProfile.username?.ifBlank { "pikaa" } ?: "pikaa") }
    var currentAvatarUrl by remember(currentUserProfile.avatar) { mutableStateOf(currentUserProfile.avatar.orEmpty()) }
    var currentCoverUrl by remember(currentUserProfile.coverUrl) {
        mutableStateOf(currentUserProfile.coverUrl?.ifBlank { null } ?: savedCover.orEmpty())
    }
    var currentBio by remember(currentUserProfile.bio) { mutableStateOf(currentUserProfile.bio ?: savedBio ?: "I love music, sunset and good conversations! Always open to meet new people and learn about different cultures. Let's be friends! 💛") }
    var currentCountry by remember(currentUserProfile.country) { mutableStateOf(currentUserProfile.country?.ifBlank { "South Korea" } ?: "South Korea") }
    var currentCountryCode by remember(currentUserProfile.countryCode) { mutableStateOf(currentUserProfile.countryCode?.ifBlank { "kr" } ?: "kr") }
    var currentCity by remember(currentUserProfile.city) { mutableStateOf(currentUserProfile.city ?: savedCity ?: "") }
    var currentAge by remember(currentUserProfile.age) { mutableStateOf(currentUserProfile.age ?: savedAge ?: 19) }
    var currentGender by remember(currentUserProfile.gender) { mutableStateOf(currentUserProfile.gender?.ifBlank { "Female" } ?: "Female") }
    var isVerified by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(true) }
    var postsCount by remember { mutableStateOf(0) }
    var followersCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }
    var currentJoinedText by remember { mutableStateOf("Joined recently") }

    val momentsList = remember { mutableStateListOf<FeedPost>() }
    var isMomentsLoading by remember { mutableStateOf(false) }

    val walletBalance by GiftManager.walletBalance.collectAsState()
    val giftsReceived = remember { mutableStateListOf<GiftTransactionItem>() }
    val userBadges = remember { mutableStateListOf<com.example.ProfileBadge>() }
    var isGiftsLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Keep locally-held state synced when currentUserProfile prop updates from parent
    LaunchedEffect(currentUserProfile.avatar) {
        if (!currentUserProfile.avatar.isNullOrBlank()) {
            currentAvatarUrl = currentUserProfile.avatar
        }
    }
    LaunchedEffect(currentUserProfile.coverUrl) {
        if (!currentUserProfile.coverUrl.isNullOrBlank()) {
            currentCoverUrl = currentUserProfile.coverUrl.orEmpty()
        }
    }
    LaunchedEffect(currentUserProfile.displayName) {
        if (!currentUserProfile.displayName.isNullOrBlank()) {
            currentProfileName = currentUserProfile.displayName
        }
    }
    LaunchedEffect(currentUserProfile.username) {
        if (!currentUserProfile.username.isNullOrBlank()) {
            currentUsername = currentUserProfile.username
        }
    }

    val refreshUserMoments: suspend () -> Unit = {
        try {
            isMomentsLoading = true
            MomentsRepository.getUserMoments(context, userId = null, limit = 20, offset = 0)
                .onSuccess { moments ->
                    momentsList.clear()
                    momentsList.addAll(moments.map { it.toFeedPost() })
                    postsCount = momentsList.size
                }
                .onFailure { err ->
                    android.util.Log.e("ProfileTabContent", "Error loading moments: ${err.message}")
                }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("ProfileTabContent", "Error loading moments", e)
        } finally {
            isMomentsLoading = false
        }
    }

    LaunchedEffect(selectedMainTab) {
        if (selectedMainTab == 1) {
            refreshUserMoments()
        }
    }

    val refreshProfileData: suspend () -> Unit = {
        val supaUid = com.example.SupabaseSessionManager.getUserId(context) ?: userId
        val effectiveUid = supaUid.takeIf { com.example.SupabaseSessionManager.isValidUuid(it) }
            ?: (if (com.example.SupabaseSessionManager.isValidUuid(userId)) userId else supaUid)
        val token = com.example.SupabaseSessionManager.getAuthenticatedBearerToken(context)
            ?: com.example.SupabaseSessionManager.getBearerToken(context)
            ?: "Bearer ${com.example.BuildConfig.SUPABASE_ANON_KEY}"

        var remoteProfileLoaded = false
        try {
            val res = withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.SupabaseClient.api.getProfile(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    authUserIdEq = "eq.$effectiveUid"
                )
            }
            if (res.isSuccessful) {
                val p = res.body()?.firstOrNull()
                if (p != null) {
                    remoteProfileLoaded = true
                    if (!p.profile_name.isNullOrBlank()) currentProfileName = p.profile_name
                    if (!p.username.isNullOrBlank()) currentUsername = p.username
                    currentAvatarUrl = p.avatar_url.orEmpty()
                    currentCoverUrl = p.cover_url.orEmpty()
                    if (!p.bio.isNullOrBlank()) currentBio = p.bio
                    if (p.age != null && p.age > 0) currentAge = p.age
                    if (!p.gender.isNullOrBlank()) currentGender = p.gender
                    if (!p.country.isNullOrBlank()) currentCountry = p.country
                    if (!p.city.isNullOrBlank()) currentCity = p.city
                    currentCountryCode = resolveCountryCode(p.country, p.country_code)

                    // Sync remote profile into SharedPreferences to avoid stale local cache
                    val prefsEditor = context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE).edit()
                    p.avatar_url?.let {
                        prefsEditor.putString("profile_avatar_${userId}", it)
                        if (supaUid != userId) prefsEditor.putString("profile_avatar_${supaUid}", it)
                    } ?: run {
                        prefsEditor.remove("profile_avatar_${userId}")
                        if (supaUid != userId) prefsEditor.remove("profile_avatar_${supaUid}")
                    }
                    p.cover_url?.let {
                        prefsEditor.putString("profile_cover_${userId}", it)
                        if (supaUid != userId) prefsEditor.putString("profile_cover_${supaUid}", it)
                    } ?: run {
                        prefsEditor.remove("profile_cover_${userId}")
                        if (supaUid != userId) prefsEditor.remove("profile_cover_${supaUid}")
                    }
                    prefsEditor.apply()
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ProfileTabContent", "Error re-fetching profile from Supabase", e)
        }

        // Secondary fallback to SharedPreferences ONLY if remote fetch did not succeed
        if (!remoteProfileLoaded) {
            val p = context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE)
            val updatedAvatar = p.getString("profile_avatar_${userId}", null)
                ?: p.getString("profile_avatar_${supaUid}", null)
            if (!updatedAvatar.isNullOrBlank() && currentAvatarUrl.isBlank()) currentAvatarUrl = updatedAvatar
            val updatedCover = p.getString("profile_cover_${userId}", null)
                ?: p.getString("profile_cover_${supaUid}", null)
            if (!updatedCover.isNullOrBlank() && currentCoverUrl.isBlank()) currentCoverUrl = updatedCover
        }
    }

    LaunchedEffect(userId) {
        isAdmin = ModerationManager.checkIsAdmin(context)
        if (userId.isBlank()) return@LaunchedEffect

        // Refresh coin wallet & catalog
        GiftManager.refreshWallet(context)
        GiftManager.fetchCatalog(context)

        // Load live profile data immediately
        refreshProfileData()

        // Load profile badges
        try {
            val badges = com.example.BadgeManager.getBadgesForUser(userId)
            userBadges.clear()
            userBadges.addAll(badges)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("ProfileTabContent", "Error loading badges", e)
        }
        try {
            isGiftsLoading = true
            val gifts = GiftManager.fetchGiftsReceived(context, userId)
            giftsReceived.clear()
            giftsReceived.addAll(gifts)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("ProfileTabContent", "Error loading gifts", e)
        } finally {
            isGiftsLoading = false
        }

        val token = com.example.SupabaseSessionManager.getAuthenticatedBearerToken(context)
            ?: com.example.SupabaseSessionManager.getBearerToken(context)
            ?: "Bearer ${com.example.BuildConfig.SUPABASE_ANON_KEY}"

        try {
            val profileResp = withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.SupabaseClient.api.getDiscoverableProfiles(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    options = mapOf("auth_user_id" to "eq.$userId")
                )
            }
            if (profileResp.isSuccessful) {
                val p = profileResp.body()?.firstOrNull()
                if (p != null) {
                    currentProfileName = p.profile_name ?: p.username ?: currentProfileName
                    currentUsername = p.username ?: p.profile_name?.lowercase()?.replace(" ", "") ?: currentUsername
                    p.avatar_url?.let { if (it.isNotBlank()) currentAvatarUrl = it }
                    p.bio?.let { if (it.isNotBlank()) currentBio = it }
                    p.country?.let { currentCountry = it }
                    p.city?.let { if (it.isNotBlank()) currentCity = it }
                    currentCountryCode = resolveCountryCode(p.country, p.country_code)
                    p.age?.let { currentAge = it }
                    p.gender?.let { currentGender = it }
                    isOnline = p.is_online ?: true
                    isVerified = p.is_verified ?: false
                    currentJoinedText = formatJoinedDate(p.created_at)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("ProfileTabContent", "Error loading profile data", e)
        }

        try {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                val followerCountResp = com.example.SupabaseClient.api.countFollowers(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    followingIdEq = "eq.$userId"
                )
                if (followerCountResp.isSuccessful) {
                    val cr = followerCountResp.headers()["Content-Range"]
                    followersCount = cr?.substringAfter("/")?.toIntOrNull()
                        ?: followerCountResp.body()?.size ?: 0
                }

                val followingCountResp = com.example.SupabaseClient.api.countFollowing(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    followerIdEq = "eq.$userId"
                )
                if (followingCountResp.isSuccessful) {
                    val cr = followingCountResp.headers()["Content-Range"]
                    followingCount = cr?.substringAfter("/")?.toIntOrNull()
                        ?: followingCountResp.body()?.size ?: 0
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("ProfileTabContent", "Error loading stats counts", e)
        }

        refreshUserMoments()
    }

    val profileDisplayName = currentProfileName
    val profileUsername = currentUsername
    val profileAvatar = currentAvatarUrl
    val profileCover = currentCoverUrl.ifBlank { "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=800" }
    val profileCountryCode = currentCountryCode
    val profileCountry = currentCountry
    val profileCity = remember(currentCity, profileCountry) {
        when {
            currentCity.isNotBlank() && profileCountry.isNotBlank() -> {
                if (profileCountry.contains(currentCity, ignoreCase = true)) profileCountry
                else "$currentCity, $profileCountry"
            }
            currentCity.isNotBlank() -> currentCity
            profileCountry.isNotBlank() -> profileCountry
            else -> "Seoul, South Korea"
        }
    }
    val profileAge = currentAge.toString()
    val profileGender = currentGender

    // Multi-language parsing (supports up to 3 stacked flags)
    val parsedNative = remember(currentUserProfile.nativeLanguage, profileCountryCode) {
        LanguageResolver.parseLanguages(
            currentUserProfile.nativeLanguage,
            listOf(LanguageItem("Hindi", "HI", "in"), LanguageItem("English", "EN", "us"), LanguageItem("Marathi", "MR", "in"))
        )
    }

    val parsedLearning = remember(currentUserProfile.learningLanguage) {
        LanguageResolver.parseLanguages(
            currentUserProfile.learningLanguage,
            listOf(LanguageItem("Chinese", "ZH", "cn"), LanguageItem("Korean", "KO", "kr"), LanguageItem("Japanese", "JA", "jp"))
        )
    }

    val userHobbies = remember(currentUserProfile.hobbies) {
        if (currentUserProfile.hobbies.isNotEmpty()) currentUserProfile.hobbies
        else listOf("Music", "Photography", "Travel", "K-Drama", "Food", "Movies", "Gaming", "Yoga")
    }

    val profileBio = currentBio

    val displayedPosts by remember {
        derivedStateOf {
            when (selectedFeedTab) {
                1 -> momentsList.filter { it.isVideo }
                2 -> momentsList.toList()
                else -> momentsList.toList()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(funkyColors.bg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(funkyColors.bg),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // -----------------------------------------------------------------
            // 1. COVER PHOTO & FLOATING ACTION BUTTONS (Share, Menu)
            // -----------------------------------------------------------------
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(235.dp)
                ) {
                    // Sunset / City Cover Image
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(profileCover)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp)
                    )

                    // Floating Round Actions (Share & Settings Menu) (Top Right)
                    Row(
                        modifier = Modifier
                            .padding(end = 16.dp, top = 16.dp)
                            .align(Alignment.TopEnd),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Share Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(elevation = 3.dp, shape = CircleShape)
                                .background(funkyColors.surface, CircleShape)
                                .clickable {
                                    Toast.makeText(context, "Profile link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileShareIcon(modifier = Modifier.size(16.dp), color = funkyColors.ink)
                        }

                        // Three Dots Menu Button (opens full-screen SettingsScreen)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(elevation = 3.dp, shape = CircleShape)
                                .background(funkyColors.surface, CircleShape)
                                .clickable { showSettingsScreen = true }
                                .testTag("profile_more_options_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileMoreDotsIcon(modifier = Modifier.size(16.dp), color = funkyColors.ink)
                        }
                    }

                    // Circular Avatar with Green Online Dot (Top-Right) & Flag (Bottom-Right)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp)
                    ) {
                        val myUid = SupabaseSessionManager.getUserId(context)
                        com.example.ui.components.PresenceAvatar(
                            avatarUrl = resolveAvatarModel(profileAvatar)?.toString(),
                            size = 80.dp,
                            isSpeaking = false,
                            isOnline = true,
                            countryCode = profileCountryCode,
                            userId = myUid,
                            enableFrameAnimation = true
                        )
                    }
                }
            }

            // -----------------------------------------------------------------
            // 2. USER HEADER & STATS CONTAINER
            // -----------------------------------------------------------------
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Row: Name, Verified Tick, "Online" Pill Badge, and Right-aligned Country Flag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val myUid = SupabaseSessionManager.getUserId(context)
                            com.example.store.DecoratedUserName(
                                userId = myUid,
                                name = profileDisplayName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                defaultColor = funkyColors.ink
                            )

                            // Blue Verified Badge (Custom Canvas Drawn)
                            if (isVerified) {
                                ProfileVerifiedBadge(modifier = Modifier.size(18.dp))
                            }

                            // Profile Badges (e.g. Founder, Early Adopter, etc.)
                            if (userBadges.isNotEmpty()) {
                                ProfileBadgesRow(badges = userBadges, badgeSize = 20.dp)
                            }

                            // Green "Online" Status Pill
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (funkyColors.isDark) Color(0xFF143523) else Color(0xFFEBFDF2),
                                        RoundedCornerShape(100.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF16A34A), CircleShape)
                                    )
                                    Text(
                                        text = "Online",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF22C55E)
                                    )
                                }
                            }
                        }

                        if (profileCountryCode.isNotBlank() && profileCountryCode.lowercase() != "un") {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircleFlag(
                                countryCode = profileCountryCode,
                                allowProgramming = false,
                                contentDescription = profileCountry,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, funkyColors.line, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Handle
                    Text(
                        text = "@${profileUsername}_07",
                        fontSize = 13.sp,
                        color = funkyColors.mute,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location & Join Date Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ProfileLocationPinIcon(modifier = Modifier.size(13.dp), color = funkyColors.mute)
                            Text(
                                text = profileCity,
                                fontSize = 12.sp,
                                color = funkyColors.mute,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Text(
                            text = "·",
                            fontSize = 14.sp,
                            color = funkyColors.mute,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ProfileCalendarIcon(modifier = Modifier.size(13.dp), color = funkyColors.mute)
                            Text(
                                text = currentJoinedText,
                                fontSize = 12.sp,
                                color = funkyColors.mute,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---------------------------------------------------------
                    // 3. STATS ROW (Posts | Followers | Following)
                    // ---------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItemExact(count = postsCount.toString(), label = "Posts", modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(funkyColors.line)
                        )
                        StatItemExact(
                            count = followersCount.toString(),
                            label = "Followers",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeFollowerUserId = userId
                                activeFollowersMode = true
                            }
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(funkyColors.line)
                        )
                        StatItemExact(
                            count = followingCount.toString(),
                            label = "Following",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeFollowerUserId = userId
                                activeFollowersMode = false
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---------------------------------------------------------
                    // 4. ACTION BUTTONS: "Say Hi" & "Edit Profile"
                    // ---------------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Yellow Filled Button: Say Hi
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .background(funkyColors.mango, RoundedCornerShape(100.dp))
                                .clickable {
                                    Toast.makeText(context, "Sent a friendly wave!", Toast.LENGTH_SHORT).show()
                                }
                                .testTag("profile_say_hi_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileWavingHandIcon(modifier = Modifier.size(18.dp), color = funkyColors.onMango)
                                Text(
                                    text = "Say Hi",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = funkyColors.onMango
                                )
                            }
                        }

                        // Outlined Pill Button: Edit Profile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .background(funkyColors.surface, RoundedCornerShape(100.dp))
                                .border(
                                    width = 1.dp,
                                    color = funkyColors.line,
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .clickable { showEditProfileDialog = true }
                                .testTag("profile_edit_follow_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileAddPersonIcon(
                                    modifier = Modifier.size(18.dp),
                                    color = funkyColors.ink
                                )
                                Text(
                                    text = "Edit Profile",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.ink
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chat Theme Store & Bubbles Banner Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { showThemeStoreSheet = true },
                        shape = RoundedCornerShape(18.dp),
                        color = funkyColors.soft,
                        border = BorderStroke(1.dp, funkyColors.mango.copy(alpha = 0.55f)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(funkyColors.mango.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = com.example.R.drawable.ic_palette),
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Funky Theme & Cosmetics Store",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = funkyColors.ink
                                    )
                                    Text(
                                        text = "Customize bubbles, frames, effects & app theme",
                                        fontSize = 11.5.sp,
                                        color = funkyColors.mute
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = funkyColors.mango
                            ) {
                                Text(
                                    text = "Open Store",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.onMango,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---------------------------------------------------------
                    // 5. SEGMENTED TAB SELECTOR: "About Me" | "Feed"
                    // ---------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .background(funkyColors.soft, RoundedCornerShape(100.dp))
                            .padding(3.5.dp)
                    ) {
                        // About Me Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (selectedMainTab == 0) funkyColors.surface else Color.Transparent)
                                .border(
                                    width = if (selectedMainTab == 0) 1.dp else 0.dp,
                                    color = if (selectedMainTab == 0) funkyColors.mango.copy(alpha = 0.6f) else Color.Transparent,
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .clickable { selectedMainTab = 0 }
                                .testTag("tab_about_me"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "About Me",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMainTab == 0) funkyColors.ink else funkyColors.mute
                            )
                        }

                        // Feed Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (selectedMainTab == 1) funkyColors.surface else Color.Transparent)
                                .border(
                                    width = if (selectedMainTab == 1) 1.dp else 0.dp,
                                    color = if (selectedMainTab == 1) funkyColors.mango.copy(alpha = 0.6f) else Color.Transparent,
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .clickable { selectedMainTab = 1 }
                                .testTag("tab_feed"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Feed",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMainTab == 1) funkyColors.ink else funkyColors.mute
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // -----------------------------------------------------------------
            // 6. CONDITIONAL VIEW: "ABOUT ME" CARDS VS "FEED" 3-COLUMN GRID
            // -----------------------------------------------------------------
            if (selectedMainTab == 0) {
                // -------------------------------------------------------------
                // CARD 1: ABOUT CARD
                // -------------------------------------------------------------
                item {
                    ProfileCardContainer(
                        title = "About",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = profileBio,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            color = funkyColors.ink.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // -------------------------------------------------------------
                // CARD 2: LANGUAGES CARD (Multi-Language Stacked Flags & Exchange)
                // -------------------------------------------------------------
                item {
                    ProfileCardContainer(
                        title = "Languages",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        MultiLanguageExchangeCard(
                            nativeLanguages = parsedNative,
                            learningLanguages = parsedLearning
                        )
                    }
                }

                // -------------------------------------------------------------
                // CARD 3: INTERESTS CARD (Dynamic 50+ Canvas Hobby Chips)
                // -------------------------------------------------------------
                item {
                    ProfileCardContainer(
                        title = "Interests",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        val hobbyChunks = userHobbies.chunked(3)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            hobbyChunks.forEach { rowList ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowList.forEach { hobby ->
                                        DynamicHobbyChip(hobby = hobby)
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // CARD 4: BASIC INFO CARD (Age, Gender, Location)
                // -------------------------------------------------------------
                item {
                    ProfileCardContainer(
                        title = "Basic Info",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Age
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                ProfileCakeIcon(modifier = Modifier.size(20.dp), color = funkyColors.mute)
                                Column {
                                    Text("Age", fontSize = 11.sp, color = funkyColors.mute, fontWeight = FontWeight.Medium)
                                    Text(profileAge, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = funkyColors.ink)
                                }
                            }

                            // Divider
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(funkyColors.line))

                            // Gender
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1.2f).padding(horizontal = 8.dp)
                            ) {
                                ProfileGenderFemaleIcon(modifier = Modifier.size(20.dp), color = funkyColors.mute)
                                Column {
                                    Text("Gender", fontSize = 11.sp, color = funkyColors.mute, fontWeight = FontWeight.Medium)
                                    Text(profileGender, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = funkyColors.ink)
                                }
                            }

                            // Divider
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(funkyColors.line))

                            // Location
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1.8f).padding(start = 8.dp)
                            ) {
                                ProfileLocationPinIcon(modifier = Modifier.size(20.dp), color = funkyColors.mute)
                                Column {
                                    Text("Location", fontSize = 11.sp, color = funkyColors.mute, fontWeight = FontWeight.Medium)
                                    Text(
                                        profileCity,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = funkyColors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // CARD 5: COIN WALLET & REWARDS (Tap to open CoinStoreScreen)
                // -------------------------------------------------------------
                item {
                    ProfileCardContainer(
                        title = "Coin Wallet",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(funkyColors.soft)
                                    .border(BorderStroke(1.dp, funkyColors.mango.copy(alpha = 0.5f)), RoundedCornerShape(14.dp))
                                    .clickable { showCoinStoreScreen = true }
                                    .padding(14.dp)
                                    .testTag("profile_coin_wallet_card"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CuteCoinIcon(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "$walletBalance coins",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = funkyColors.ink
                                        )
                                        Text(
                                            text = "Tap to open Coin Store",
                                            fontSize = 11.5.sp,
                                            color = funkyColors.mute
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = funkyColors.mango
                                ) {
                                    Text(
                                        text = "+ Top Up",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = funkyColors.onMango,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Use coins to send animated gifts, unlock avatar frames, entry rockets & chat themes!",
                                fontSize = 12.sp,
                                color = funkyColors.mute,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // CARD 6: GIFTS RECEIVED & HISTORY
                // -------------------------------------------------------------
                item {
                    ProfileCardContainer(
                        title = "Gifts Received (${giftsReceived.size}) 🎁",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        if (isGiftsLoading && giftsReceived.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = GoldAccent)
                            }
                        } else if (giftsReceived.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🎁",
                                    fontSize = 28.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No gifts received yet.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Chat and practice languages to receive animated gifts from friends!",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        } else {
                            val distinctGifts = giftsReceived.groupBy { it.gift_name ?: "Gift" }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                distinctGifts.entries.chunked(2).forEach { rowPairs ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowPairs.forEach { (name, items) ->
                                            val first = items.first()
                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFFFFFBEB),
                                                border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (!first.gift_icon_url.isNullOrBlank()) {
                                                            AnimatedGiftMedia(
                                                                url = first.gift_icon_url!!,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        } else {
                                                            Text("🎁", fontSize = 18.sp)
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = name,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF0F172A),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "x${items.size}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFFB45309)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (rowPairs.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // -------------------------------------------------------------
                // FEED TAB CONTENT: SUB-TABS (Grid, Video, Tagged) & 3-COLUMN GRID
                // -------------------------------------------------------------
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .border(width = 0.5.dp, color = Color(0xFFE2E8F0)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Grid icon for Posts
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedFeedTab = 0 },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CustomGridIcon(color = if (selectedFeedTab == 0) Color(0xFFFFD43B) else Color(0xFF475569))
                                    Spacer(modifier = Modifier.height(3.dp))
                                    if (selectedFeedTab == 0) {
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(2.5.dp)
                                                .background(Color(0xFFFFD43B), CircleShape)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(2.5.dp))
                                    }
                                }
                            }

                            // 2. Video/Reels-style icon
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedFeedTab = 1 },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CustomReelsIcon(color = if (selectedFeedTab == 1) Color(0xFFFFD43B) else Color(0xFF475569))
                                    Spacer(modifier = Modifier.height(3.dp))
                                    if (selectedFeedTab == 1) {
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(2.5.dp)
                                                .background(Color(0xFFFFD43B), CircleShape)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(2.5.dp))
                                    }
                                }
                            }

                            // 3. Tagged/posts icon
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedFeedTab = 2 },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CustomTaggedIcon(color = if (selectedFeedTab == 2) Color(0xFFFFD43B) else Color(0xFF475569))
                                    Spacer(modifier = Modifier.height(3.dp))
                                    if (selectedFeedTab == 2) {
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(2.5.dp)
                                                .background(Color(0xFFFFD43B), CircleShape)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(2.5.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                if (isMomentsLoading && displayedPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFFD43B),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else if (displayedPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No posts shared yet",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                // Photo Grid in chunks of 3 for LazyColumn
                val chunkedPosts = displayedPosts.chunked(3)
                items(chunkedPosts.size) { chunkIndex ->
                    val rowPosts = chunkedPosts[chunkIndex]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 1.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (i in 0 until 3) {
                            if (i < rowPosts.size) {
                                val post = rowPosts[i]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { activeFullPost = post }
                                ) {
                                    if (post.type == PostType.IMAGE && post.imageUrl != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(post.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Post",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        if (post.isVideo) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                                    .size(18.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(modifier = Modifier.size(7.dp)) {
                                                    val path = Path().apply {
                                                        moveTo(0f, 0f)
                                                        lineTo(size.width, size.height / 2f)
                                                        lineTo(0f, size.height)
                                                        close()
                                                    }
                                                    drawPath(path, Color.White)
                                                }
                                            }
                                        } else if (post.isCarousel) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                                    .size(18.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(modifier = Modifier.size(8.dp)) {
                                                    drawRoundRect(
                                                        color = Color.White,
                                                        topLeft = Offset(0f, 0f),
                                                        size = Size(size.width * 0.8f, size.height * 0.8f),
                                                        cornerRadius = CornerRadius(1.dp.toPx())
                                                    )
                                                    drawRoundRect(
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        topLeft = Offset(size.width * 0.2f, size.height * 0.2f),
                                                        size = Size(size.width * 0.8f, size.height * 0.8f),
                                                        cornerRadius = CornerRadius(1.dp.toPx()),
                                                        style = Stroke(width = 1.dp.toPx())
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        val preset = MomentsRepository.resolveBgPreset(post.bgKey)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    if (!post.bgKey.isNullOrBlank()) Brush.linearGradient(preset.colors)
                                                    else Brush.linearGradient(listOf(post.textBgColor, post.textBgColor))
                                                )
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = post.textContent.orEmpty(),
                                                color = if (!post.bgKey.isNullOrBlank()) {
                                                    if (preset.isDarkText) Color(0xFF0F172A) else Color.White
                                                } else post.textTextColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 5,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }

                                    if (post.status == "hidden") {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(4.dp)
                                                .background(Color(0xCC0F172A), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Hidden by moderators",
                                                color = Color(0xFFFDE047),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                }
            }
        }

        // Full Screen Moments v2 Feed Viewer when a feed post is clicked
        activeFullPost?.let { post ->
            val feedMoments = remember(momentsList.toList(), selectedFeedTab) {
                val subset = if (selectedFeedTab == 1) momentsList.filter { it.isVideo } else momentsList
                subset.map { fp ->
                    fp.toFallbackMomentDto(
                        fallbackAuthorId = supaUid,
                        fallbackAuthorName = profileDisplayName,
                        fallbackAvatarUrl = profileAvatar,
                        fallbackCountryCode = currentUserProfile.countryCode.orEmpty(),
                        isSelfAuthor = true
                    )
                }.ifEmpty {
                    listOf(
                        post.toFallbackMomentDto(
                            fallbackAuthorId = supaUid,
                            fallbackAuthorName = profileDisplayName,
                            fallbackAvatarUrl = profileAvatar,
                            fallbackCountryCode = currentUserProfile.countryCode.orEmpty(),
                            isSelfAuthor = true
                        )
                    )
                }
            }

            MomentsV2FeedViewerDialog(
                moments = feedMoments,
                initialMomentId = post.id,
                currentUserNativeLanguage = currentUserProfile.nativeLanguage.orEmpty(),
                isSelfProfile = true,
                onDismiss = { activeFullPost = null },
                onMomentUpdated = { updatedDto ->
                    val idx = momentsList.indexOfFirst { it.id == updatedDto.id }
                    if (idx >= 0) {
                        val existingBookmark = momentsList[idx].isBookmarkedByUser
                        momentsList[idx] = updatedDto.toFeedPost().copy(isBookmarkedByUser = existingBookmark)
                    }
                },
                onMomentDeleted = { deletedId ->
                    momentsList.removeAll { it.id == deletedId }
                    postsCount = momentsList.size
                    if (activeFullPost?.id == deletedId) {
                        activeFullPost = null
                    }
                },
                onBookmarkToggled = { momentId, isBookmarked ->
                    val idx = momentsList.indexOfFirst { it.id == momentId }
                    if (idx >= 0) {
                        momentsList[idx] = momentsList[idx].copy(isBookmarkedByUser = isBookmarked)
                    }
                }
            )
        }
    }

    // Full-screen Settings Screen (Triggered by 3 dots on top right)
    if (showSettingsScreen) {
        SettingsScreen(
            walletBalance = walletBalance,
            isAdmin = isAdmin,
            onBack = { showSettingsScreen = false },
            onEditProfile = {
                showSettingsScreen = false
                showEditProfileDialog = true
            },
            onOpenCoinStore = {
                showSettingsScreen = false
                showCoinStoreScreen = true
            },
            onOpenDecorationStore = {
                showSettingsScreen = false
                showThemeStoreSheet = true
            },
            onOpenSupportChat = {
                showSettingsScreen = false
                onOpenSupportChat(null)
            },
            onOpenModerationHub = {
                showSettingsScreen = false
                showModerationDialog = true
            },
            onOpenBlockedUsers = {
                showSettingsScreen = false
                showBlockedUsersSheet = true
            },
            onOpenBlockedGroups = {
                showSettingsScreen = false
                showBlockedGroupsSheet = true
            },
            onOpenChatBackup = {
                showSettingsScreen = false
                showChatBackupScreen = true
            },
            onOpenChatAppearance = {
                showSettingsScreen = false
                showChatAppearanceSheet = true
            },
            onOpenDebugGallery = {
                showSettingsScreen = false
                showChatKitGalleryDebug = true
            },
            onLogout = {
                showSettingsScreen = false
                onLogoutClick()
            },
            onDeleteAccount = {
                showSettingsScreen = false
                onDeleteAccountClick()
            }
        )
    }

    // Full-screen Coin Store Screen
    if (showCoinStoreScreen) {
        CoinStoreScreen(
            currentBalance = walletBalance,
            onDismiss = { showCoinStoreScreen = false },
            onPurchaseCoins = { addedCoins ->
                com.example.store.CosmeticsManager.updateWalletBalance(walletBalance + addedCoins)
            }
        )
    }

    if (showChatAppearanceSheet) {
        com.example.ui.chat.ChatAppearanceBottomSheet(
            onDismiss = { showChatAppearanceSheet = false },
            onOpenDebugGallery = {
                showChatAppearanceSheet = false
                showChatKitGalleryDebug = true
            }
        )
    }

    if (showChatKitGalleryDebug) {
        com.example.ui.chat.ChatKitGalleryDebugDialog(
            onDismiss = { showChatKitGalleryDebug = false }
        )
    }

    // Blocked Users Bottom Sheet
    if (showBlockedUsersSheet) {
        BlockedUsersBottomSheet(
            onDismiss = { showBlockedUsersSheet = false }
        )
    }

    if (showBlockedGroupsSheet) {
        com.example.ui.screens.home.dialogs.BlockedGroupsBottomSheet(
            onDismiss = { showBlockedGroupsSheet = false }
        )
    }

    // Edit Profile Dialog Fullscreen Redesign
    if (showEditProfileDialog) {
        EditProfileScreen(
            currentUserProfile = currentUserProfile.copy(
                displayName = currentProfileName,
                username = currentUsername,
                avatar = currentAvatarUrl,
                coverUrl = currentCoverUrl.ifBlank { null },
                bio = currentBio,
                city = currentCity,
                country = currentCountry,
                countryCode = currentCountryCode,
                age = currentAge,
                gender = currentGender
            ),
            viewModel = viewModel,
            onDismiss = {
                showEditProfileDialog = false
                try {
                    val loader = coil.Coil.imageLoader(context)
                    loader.memoryCache?.clear()
                    loader.diskCache?.clear()
                } catch (_: Exception) {}

                coroutineScope.launch {
                    refreshProfileData()
                }

                val supaUid = com.example.SupabaseSessionManager.getUserId(context) ?: userId
                val p = context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE)
                val updatedBio = p.getString("profile_bio_${userId}", null)
                    ?: p.getString("profile_bio_${supaUid}", null)
                val updatedCity = p.getString("profile_city_${userId}", null)
                    ?: p.getString("profile_city_${supaUid}", null)
                val updatedAge = (p.getInt("profile_age_${userId}", 0).takeIf { it > 0 })
                    ?: (p.getInt("profile_age_${supaUid}", 0).takeIf { it > 0 })
                val updatedCountry = p.getString("profile_country_${userId}", null)
                    ?: p.getString("profile_country_${supaUid}", null)
                val updatedCountryCode = p.getString("profile_countryCode_${userId}", null)
                    ?: p.getString("profile_countryCode_${supaUid}", null)
                val updatedDisplayName = p.getString("profile_displayName_${userId}", null)
                    ?: p.getString("profile_displayName_${supaUid}", null)
                val updatedUsername = p.getString("profile_username_${userId}", null)
                    ?: p.getString("profile_username_${supaUid}", null)
                val updatedCover = p.getString("profile_cover_${userId}", null)
                    ?: p.getString("profile_cover_${supaUid}", null)

                if (!updatedCover.isNullOrBlank()) currentCoverUrl = updatedCover
                if (!updatedBio.isNullOrBlank()) currentBio = updatedBio
                if (!updatedCity.isNullOrBlank()) currentCity = updatedCity
                if (updatedAge != null) currentAge = updatedAge
                if (!updatedCountry.isNullOrBlank()) currentCountry = updatedCountry
                if (!updatedCountryCode.isNullOrBlank()) currentCountryCode = updatedCountryCode
                if (!updatedDisplayName.isNullOrBlank()) currentProfileName = updatedDisplayName
                if (!updatedUsername.isNullOrBlank()) currentUsername = updatedUsername
            }
        )
    }

    if (showModerationDialog) {
        val supabaseProfiles by (viewModel?.supabaseProfiles ?: MutableStateFlow(emptyList())).collectAsState()
        ModerationReviewDialog(
            onDismiss = {
                showModerationDialog = false
                com.example.ModerationManager.showModerationHubDialog = false
                com.example.ModerationManager.pendingReportIdToOpen = null
            },
            currentUserProfile = currentUserProfile,
            supabaseProfiles = supabaseProfiles,
            initialReportId = com.example.ModerationManager.pendingReportIdToOpen
        )
    }

    if (activeFollowerUserId != null) {
        com.example.ui.screens.profile.FollowersListBottomSheet(
            userId = activeFollowerUserId!!,
            showFollowers = activeFollowersMode,
            onDismiss = { activeFollowerUserId = null },
            onUserClick = { user ->
                activeClickedProfile = user
            }
        )
    }

    if (activeClickedProfile != null) {
        val user = activeClickedProfile!!
        com.example.ui.screens.UserProfileFeedScreen(
            userId = user.auth_user_id,
            username = user.username ?: "anonymous",
            profileName = user.profile_name ?: user.username ?: "Anonymous",
            avatarUrl = user.avatar_url ?: "",
            coverUrl = user.cover_url ?: "",
            countryCode = user.country_code ?: "us",
            bio = user.bio ?: "",
            onBack = { activeClickedProfile = null },
            onStartChat = {
                activeClickedProfile = null
            }
        )
    }

    if (showChatBackupScreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            ChatBackupScreen(
                onBackClick = { showChatBackupScreen = false }
            )
        }
    }

    if (showThemeStoreSheet) {
        com.example.ui.screens.store.DecorationStoreScreen(
            onDismiss = { showThemeStoreSheet = false },
            currentUserAvatar = currentAvatarUrl,
            currentUserName = currentProfileName
        )
    }
}
