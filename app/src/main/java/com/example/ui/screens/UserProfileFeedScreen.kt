package com.example.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.GiftTransactionItem
import com.example.gifting.AnimatedGiftMedia
import com.example.gifting.GiftManager
import com.example.gifting.SendGiftBottomSheet
import com.example.moderation.ReportUserDialog
import com.example.BuildConfig
import com.example.MomentDto
import com.example.SupabaseClient
import com.example.SupabaseFollower
import com.example.SupabaseSessionManager
import com.example.repository.BlockRepository
import com.example.repository.EncryptedChatRepository
import com.example.repository.MomentsRepository
import com.example.repository.USER_BLOCKED_ERROR_MESSAGE
import com.example.repository.UserBlockedException
import com.example.ui.components.BlockUserConfirmDialog
import com.example.ui.components.CircleFlag
import com.example.ui.components.ProfileBadgesRow
import com.example.ui.components.DynamicHobbyChip
import com.example.ui.components.LanguageItem
import com.example.ui.components.LanguageResolver
import com.example.ui.components.MultiLanguageExchangeCard
import com.example.ui.components.MomentsV2FeedViewerDialog
import com.example.ui.components.toFallbackMomentDto
import com.example.ui.screens.home.tabs.resolveCountryCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// -----------------------------------------------------------------------------
// DATA MODELS
// -----------------------------------------------------------------------------

data class FeedPost(
    val id: String,
    val type: PostType,
    val imageUrl: String? = null,
    val allImages: List<String> = emptyList(),
    val textContent: String? = null,
    val textBgColor: Color = Color(0xFFFFF1F2),
    val textTextColor: Color = Color(0xFF9F1239),
    val caption: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val timestamp: String = "Recently",
    val isVideo: Boolean = false,
    val isCarousel: Boolean = false,
    var isLikedByUser: Boolean = false,
    var isBookmarkedByUser: Boolean = false,
    val status: String = "active",
    val title: String? = null,
    val bgKey: String? = null,
    val isMe: Boolean = false,
    val momentDto: MomentDto? = null
)

enum class PostType {
    IMAGE, TEXT
}

fun MomentDto.toFeedPost(): FeedPost {
    val firstMedia = media.firstOrNull()
    val hasVideo = media.any { it.type == "video" }
    val visualUrls = media.mapNotNull { m ->
        val resolved = if (m.type == "video") (m.thumb_url ?: m.url) else m.url
        resolved.takeIf { it.isNotBlank() }
    }
    val firstVisual = if (firstMedia?.type == "video") {
        (firstMedia.thumb_url ?: firstMedia.url).takeIf { it.isNotBlank() }
    } else {
        firstMedia?.url?.takeIf { it.isNotBlank() }
    }
    val preset = MomentsRepository.resolveBgPreset(bg_key)
    val textSummary = listOfNotNull(
        title?.takeIf { it.isNotBlank() },
        content?.takeIf { it.isNotBlank() }
    ).joinToString("\n")

    return FeedPost(
        id = id,
        type = if (firstVisual != null) PostType.IMAGE else PostType.TEXT,
        imageUrl = firstVisual,
        allImages = visualUrls,
        textContent = if (firstVisual == null) textSummary else null,
        textBgColor = preset.colors.firstOrNull() ?: Color(0xFFFFF1F2),
        textTextColor = if (preset.isDarkText) Color(0xFF0F172A) else Color.White,
        caption = content.orEmpty(),
        likesCount = likes_count,
        commentsCount = comments_count + corrections_count,
        timestamp = formatMomentTimestamp(created_at),
        isVideo = hasVideo,
        isCarousel = media.size > 1,
        isLikedByUser = liked_by_me,
        status = status,
        title = title,
        bgKey = bg_key,
        isMe = author.is_me,
        momentDto = this
    )
}

// -----------------------------------------------------------------------------
// MAIN COMPOSABLE SCREEN
// -----------------------------------------------------------------------------

@Composable
fun UserProfileFeedScreen(
    userId: String = "",
    username: String = "pikaa",
    profileName: String = "pikaa",
    avatarUrl: String = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500",
    coverUrl: String = "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=800",
    countryCode: String = "kr",
    nativeLanguages: String = "Hindi, English, Marathi",
    learningLanguages: String = "Chinese, Korean, Japanese",
    hobbies: List<String> = listOf("Music", "Photography", "Travel", "K-Drama", "Food", "Movies", "Gaming", "Yoga"),
    bio: String = "I love music, sunset and good conversations! Always open to meet new people and learn about different cultures. Let's be friends! 💛",
    onBack: () -> Unit,
    onStartChat: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val funkyColors = com.example.ui.theme.LocalFunkyColors.current
    val isDark = funkyColors.isDark

    val selfUserId = remember(context) { SupabaseSessionManager.getUserId(context) ?: "" }
    val isSelfProfile = remember(userId, selfUserId) { userId.isNotBlank() && userId == selfUserId }

    var selectedMainTab by remember { mutableStateOf(0) } // 0 = About Me, 1 = Feed
    var selectedFeedTab by remember { mutableStateOf(0) } // 0 = Grid, 1 = Video, 2 = Tagged
    var activeFullPost by remember { mutableStateOf<FeedPost?>(null) }

    // Dynamic Profile State (from discoverable_profiles)
    var currentProfileName by remember(profileName) { mutableStateOf(profileName) }
    var currentUsername by remember(username) { mutableStateOf(username) }
    var currentAvatarUrl by remember(avatarUrl) { mutableStateOf(avatarUrl) }
    var currentCoverUrl by remember(coverUrl) { mutableStateOf(coverUrl) }
    var currentBio by remember(bio) { mutableStateOf(bio) }
    var currentCountry by remember { mutableStateOf("") }
    var currentCity by remember { mutableStateOf("") }
    var currentCountryCode by remember(countryCode) { mutableStateOf(countryCode) }
    var currentAge by remember { mutableStateOf<Int?>(null) }
    var currentGender by remember { mutableStateOf<String?>(null) }
    var currentJoinedText by remember { mutableStateOf("Joined recently") }
    var isOnline by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }
    var currentNativeLanguages by remember { mutableStateOf<List<String>?>(null) }
    var currentLearningLanguages by remember { mutableStateOf<List<String>?>(null) }
    var currentHobbies by remember(hobbies) { mutableStateOf(hobbies) }

    // Dynamic Stats State
    var postsCount by remember { mutableStateOf(0) }
    var followersCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }
    var isFollowLoading by remember { mutableStateOf(false) }

    // Dynamic Feed Posts List (from moments)
    val momentsList = remember { mutableStateListOf<FeedPost>() }
    var isMomentsLoading by remember { mutableStateOf(false) }

    suspend fun refreshUserMoments() {
        if (userId.isBlank()) return
        try {
            isMomentsLoading = true
            val targetUid = if (isSelfProfile) null else userId
            MomentsRepository.getUserMoments(context, userId = targetUid, limit = 20, offset = 0)
                .onSuccess { list ->
                    momentsList.clear()
                    momentsList.addAll(list.map { it.toFeedPost() })
                    postsCount = momentsList.size
                }
                .onFailure { err ->
                    Log.e("UserProfileFeed", "Error fetching user moments: ${err.message}")
                }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("UserProfileFeed", "Error fetching user moments", e)
        } finally {
            isMomentsLoading = false
        }
    }

    LaunchedEffect(selectedMainTab, userId) {
        if (selectedMainTab == 1 && userId.isNotBlank()) {
            refreshUserMoments()
        }
    }

    // Say Hi & Wave Dialog State
    var showWaveDialog by remember { mutableStateOf(false) }
    var waveCustomMessage by remember { mutableStateOf("") }
    var isSendingHi by remember { mutableStateOf(false) }

    var activeFollowerUserId by remember { mutableStateOf<String?>(null) }
    var activeFollowersMode by remember { mutableStateOf(true) } // true = followers, false = following
    var activeClickedProfile by remember { mutableStateOf<com.example.DiscoverableProfile?>(null) }

    var showSendGiftSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showMoreOptionsMenu by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var isBlockingUser by remember { mutableStateOf(false) }
    val blockedUserIds by BlockRepository.blockedUserIds.collectAsState()
    val isBlockedLocally = !isSelfProfile && userId.isNotBlank() && blockedUserIds.contains(userId)
    var isBlockedWithPair by remember(userId) { mutableStateOf(false) }
    val isBlockedEffective = !isSelfProfile && (isBlockedLocally || isBlockedWithPair)
    val giftsReceived = remember { mutableStateListOf<GiftTransactionItem>() }
    val userBadges = remember { mutableStateListOf<com.example.ProfileBadge>() }
    var isGiftsLoading by remember { mutableStateOf(false) }

    LaunchedEffect(userId, blockedUserIds) {
        if (!isSelfProfile && userId.isNotBlank()) {
            isBlockedWithPair = isBlockedLocally || BlockRepository.isBlockedWith(context, userId)
        } else {
            isBlockedWithPair = false
        }
    }

    // -------------------------------------------------------------------------
    // 1. FETCH REAL SUPABASE DATA ON LAUNCH
    // -------------------------------------------------------------------------
    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect
        com.example.store.CosmeticsManager.batchFetchUserCosmetics(context, listOf(userId))

        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            ?: SupabaseSessionManager.getBearerToken(context)
            ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

        // A. Fetch profile details from discoverable_profiles
        try {
            val profileResp = withContext(Dispatchers.IO) {
                SupabaseClient.api.getDiscoverableProfiles(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
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
                    p.cover_url?.let { if (it.isNotBlank()) currentCoverUrl = it }
                    p.bio?.let { if (it.isNotBlank()) currentBio = it }
                    p.country?.let { currentCountry = it }
                    p.city?.let { if (it.isNotBlank()) currentCity = it }
                    currentCountryCode = resolveCountryCode(p.country, p.country_code)
                    currentAge = p.age
                    currentGender = p.gender
                    isOnline = p.is_online ?: false
                    isVerified = p.is_verified ?: false
                    currentJoinedText = formatJoinedDate(p.created_at)
                    if (!p.native_languages.isNullOrEmpty()) {
                        currentNativeLanguages = p.native_languages
                    }
                    if (!p.learning_languages.isNullOrEmpty()) {
                        currentLearningLanguages = p.learning_languages
                    }
                    if (!p.hobbies.isNullOrEmpty()) {
                        currentHobbies = p.hobbies
                    }
                }
            } else {
                Log.w("UserProfileFeed", "Failed to fetch profile: ${profileResp.code()} ${profileResp.message()}")
            }

            // Direct fetch from profiles table to ensure cover_url and latest details are loaded
            try {
                val directResp = withContext(Dispatchers.IO) {
                    SupabaseClient.api.getProfile(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token,
                        authUserIdEq = "eq.$userId"
                    )
                }
                if (directResp.isSuccessful) {
                    val p = directResp.body()?.firstOrNull()
                    if (p != null) {
                        if (!p.cover_url.isNullOrBlank()) currentCoverUrl = p.cover_url
                        if (!p.city.isNullOrBlank()) currentCity = p.city
                        if (!p.avatar_url.isNullOrBlank()) currentAvatarUrl = p.avatar_url
                        if (!p.profile_name.isNullOrBlank()) currentProfileName = p.profile_name
                        if (!p.username.isNullOrBlank()) currentUsername = p.username
                        if (!p.bio.isNullOrBlank()) currentBio = p.bio
                    }
                }
            } catch (e: Exception) {
                Log.e("UserProfileFeed", "Error fetching direct profile for cover and city", e)
            }
        } catch (e: Exception) {
            Log.e("UserProfileFeed", "Error loading profile data", e)
        }

        // Fetch badges for this user
        try {
            val badges = com.example.BadgeManager.getBadgesForUser(userId)
            userBadges.clear()
            userBadges.addAll(badges)
        } catch (e: Exception) {
            Log.e("UserProfileFeed", "Error loading user badges", e)
        }

        val selfUid = SupabaseSessionManager.getUserId(context)
        if (userId == selfUid || isSelfProfile) {
            val prefs = context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE)
            val savedCity = prefs.getString("profile_city_${userId}", null)
            val savedBio = prefs.getString("profile_bio_${userId}", null)
            val savedAge = prefs.getInt("profile_age_${userId}", 0).takeIf { it > 0 }
            val savedCover = prefs.getString("profile_cover_${userId}", null)
                ?: (if (selfUid != null) prefs.getString("profile_cover_${selfUid}", null) else null)
            if (!savedCity.isNullOrBlank()) currentCity = savedCity
            if (!savedBio.isNullOrBlank()) currentBio = savedBio
            if (savedAge != null) currentAge = savedAge
            if (!savedCover.isNullOrBlank()) currentCoverUrl = savedCover
        }

        // B. Fetch follower/following stats counts
        try {
            withContext(Dispatchers.IO) {
                val followerCountResp = SupabaseClient.api.countFollowers(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    followingIdEq = "eq.$userId"
                )
                if (followerCountResp.isSuccessful) {
                    val cr = followerCountResp.headers()["Content-Range"]
                    followersCount = cr?.substringAfter("/")?.toIntOrNull()
                        ?: followerCountResp.body()?.size ?: 0
                }

                val followingCountResp = SupabaseClient.api.countFollowing(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    followerIdEq = "eq.$userId"
                )
                if (followingCountResp.isSuccessful) {
                    val cr = followingCountResp.headers()["Content-Range"]
                    followingCount = cr?.substringAfter("/")?.toIntOrNull()
                        ?: followingCountResp.body()?.size ?: 0
                }
            }
        } catch (e: Exception) {
            Log.e("UserProfileFeed", "Error loading stats counts", e)
        }

        // C. Check initial follow status
        if (selfUserId.isNotBlank() && selfUserId != userId) {
            try {
                val followResp = withContext(Dispatchers.IO) {
                    SupabaseClient.api.getFollowers(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token,
                        followerIdEq = "eq.$selfUserId",
                        followingIdEq = "eq.$userId"
                    )
                }
                if (followResp.isSuccessful) {
                    isFollowing = !followResp.body().isNullOrEmpty()
                }
            } catch (e: Exception) {
                Log.e("UserProfileFeed", "Error checking follow status", e)
            }
        }

        // D. Fetch user's moments via Moments v2 RPC
        refreshUserMoments()

        // D. Fetch gifts received for profile
        try {
            isGiftsLoading = true
            val fetchedGifts = GiftManager.fetchGiftsReceived(context, userId)
            giftsReceived.clear()
            giftsReceived.addAll(fetchedGifts)
        } catch (e: Exception) {
            Log.e("UserProfileFeed", "Error fetching gifts received", e)
        } finally {
            isGiftsLoading = false
        }
    }

    // Follow / Unfollow Handler
    fun handleFollowToggle() {
        if (userId.isBlank() || isSelfProfile || isFollowLoading) return
        val previousState = isFollowing
        val previousCount = followersCount
        val targetState = !previousState

        // Optimistic UI update
        isFollowing = targetState
        followersCount = if (targetState) previousCount + 1 else (previousCount - 1).coerceAtLeast(0)
        isFollowLoading = true

        coroutineScope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)
                    ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

                val isSuccess = withContext(Dispatchers.IO) {
                    if (targetState) {
                        val resp = SupabaseClient.api.followUser(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token,
                            follower = SupabaseFollower(
                                follower_id = selfUserId,
                                following_id = userId
                            )
                        )
                        resp.isSuccessful
                    } else {
                        val resp = SupabaseClient.api.unfollowUser(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token,
                            followerIdEq = "eq.$selfUserId",
                            followingIdEq = "eq.$userId"
                        )
                        resp.isSuccessful
                    }
                }

                if (isSuccess) {
                    Toast.makeText(
                        context,
                        if (targetState) "Now following $currentProfileName" else "Unfollowed $currentProfileName",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Revert
                    isFollowing = previousState
                    followersCount = previousCount
                    Toast.makeText(context, "Failed to update follow status", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isFollowing = previousState
                followersCount = previousCount
                Toast.makeText(context, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isFollowLoading = false
            }
        }
    }

    var showVaultGateDialog by remember { mutableStateOf(false) }
    var pendingWaveGreeting by remember { mutableStateOf<String?>(null) }
    val vaultStatus by com.example.crypto.VaultSecurityManager.vaultStatus.collectAsState()

    // Say Hi / Wave Handler
    fun handleSendWave(greetingMessage: String) {
        if (userId.isBlank() || isSendingHi) {
            onStartChat()
            return
        }
        if (com.example.crypto.VaultSecurityManager.isVaultLocked() ||
            vaultStatus != com.example.crypto.VaultStatus.UNLOCKED
        ) {
            showWaveDialog = false
            pendingWaveGreeting = greetingMessage
            showVaultGateDialog = true
            return
        }
        isSendingHi = true
        coroutineScope.launch {
            try {
                val greeting = greetingMessage.ifBlank { "👋 Hi!" }
                EncryptedChatRepository.init(context)
                val ready = EncryptedChatRepository.checkRecipientKeyStatus(context, userId)
                if (ready) {
                    val result = EncryptedChatRepository.sendMessage(
                        context = context,
                        recipientUserId = userId,
                        recipientName = currentProfileName,
                        recipientAvatar = currentAvatarUrl,
                        text = greeting
                    )
                    if (result.isSuccess) {
                        Toast.makeText(context, "Wave sent to $currentProfileName!", Toast.LENGTH_SHORT).show()
                        onStartChat()
                    } else {
                        val err = result.exceptionOrNull()
                        if (err is UserBlockedException || BlockRepository.isUserBlockedError(err?.message)) {
                            Toast.makeText(context, USER_BLOCKED_ERROR_MESSAGE, Toast.LENGTH_SHORT).show()
                        } else {
                            Log.w("UserProfileFeed", "Wave send: ${err?.message}")
                            onStartChat()
                        }
                    }
                } else {
                    Log.d("UserProfileFeed", "E2EE keys missing, starting chat window.")
                    onStartChat()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("UserProfileFeed", "Error sending wave message", e)
            } finally {
                isSendingHi = false
                showWaveDialog = false
            }
        }
    }

    LaunchedEffect(vaultStatus) {
        if (vaultStatus == com.example.crypto.VaultStatus.UNLOCKED && showVaultGateDialog) {
            showVaultGateDialog = false
            val pending = pendingWaveGreeting
            pendingWaveGreeting = null
            if (pending != null) {
                handleSendWave(pending)
            }
        }
    }

    // Languages parsing
    val parsedNative = remember(currentNativeLanguages, nativeLanguages) {
        if (!currentNativeLanguages.isNullOrEmpty()) {
            LanguageResolver.parseLanguageList(
                currentNativeLanguages,
                listOf(LanguageItem("Hindi", "HI", "in"), LanguageItem("English", "EN", "us"))
            )
        } else {
            LanguageResolver.parseLanguages(
                nativeLanguages,
                listOf(LanguageItem("Hindi", "HI", "in"), LanguageItem("English", "EN", "us"))
            )
        }
    }

    val parsedLearning = remember(currentLearningLanguages, learningLanguages) {
        if (!currentLearningLanguages.isNullOrEmpty()) {
            LanguageResolver.parseLanguageList(
                currentLearningLanguages,
                listOf(LanguageItem("Korean", "KO", "kr"), LanguageItem("Japanese", "JA", "jp"))
            )
        } else {
            LanguageResolver.parseLanguages(
                learningLanguages,
                listOf(LanguageItem("Korean", "KO", "kr"), LanguageItem("Japanese", "JA", "jp"))
            )
        }
    }

    val displayedPosts by remember(isBlockedEffective) {
        derivedStateOf {
            if (isBlockedEffective) {
                emptyList()
            } else when (selectedFeedTab) {
                1 -> momentsList.filter { it.isVideo }
                2 -> momentsList.toList()
                else -> momentsList.toList()
            }
        }
    }

    BackHandler {
        if (activeFullPost != null) {
            activeFullPost = null
        } else {
            onBack()
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
                .statusBarsPadding()
                .background(funkyColors.bg),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // -----------------------------------------------------------------
            // 1. COVER PHOTO & FLOATING ACTION BUTTONS (Back, Share, Menu)
            // -----------------------------------------------------------------
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(255.dp)
                ) {
                    // Sunset / City Cover Image
                    val displayCover = currentCoverUrl.ifBlank { "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=800" }
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(displayCover)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(205.dp)
                    )

                    // Floating Round Buttons Row with statusBarsPadding() (Back, Share, Menu)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Floating Round Back Button (Top Left)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(elevation = 2.dp, shape = CircleShape)
                                .background(funkyColors.surface, CircleShape)
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileBackArrowIcon(modifier = Modifier.size(16.dp), color = funkyColors.ink)
                        }

                        // Floating Round Actions (Share & Menu) (Top Right)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Share Button
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .shadow(elevation = 2.dp, shape = CircleShape)
                                    .background(funkyColors.surface, CircleShape)
                                    .clickable {
                                        Toast.makeText(context, "Shared profile link", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                ProfileShareIcon(modifier = Modifier.size(16.dp), color = funkyColors.ink)
                            }

                            // Three Dots Menu Button
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .shadow(elevation = 2.dp, shape = CircleShape)
                                    .background(funkyColors.surface, CircleShape)
                                    .clickable {
                                        showMoreOptionsMenu = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                ProfileMoreDotsIcon(modifier = Modifier.size(16.dp), color = funkyColors.ink)

                                DropdownMenu(
                                    expanded = showMoreOptionsMenu,
                                    onDismissRequest = { showMoreOptionsMenu = false },
                                    modifier = Modifier
                                        .background(funkyColors.surface, RoundedCornerShape(14.dp))
                                        .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                                ) {
                                    if (!isSelfProfile) {
                                        if (!isBlockedEffective) {
                                            DropdownMenuItem(
                                                text = { Text("Send Gift", fontWeight = FontWeight.Bold, color = funkyColors.mango) },
                                                onClick = {
                                                    showMoreOptionsMenu = false
                                                    showSendGiftSheet = true
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (isBlockedLocally) "Unblock" else "Block user",
                                                    fontWeight = FontWeight.Medium,
                                                    color = funkyColors.danger
                                                )
                                            },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                                if (isBlockedLocally) {
                                                    coroutineScope.launch {
                                                        val res = BlockRepository.unblockUser(context, userId)
                                                        res.onSuccess {
                                                            isBlockedWithPair = false
                                                            Toast.makeText(context, "$currentProfileName unblocked", Toast.LENGTH_SHORT).show()
                                                        }.onFailure { err ->
                                                            Toast.makeText(context, err.message ?: "Could not unblock user.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } else {
                                                    showBlockConfirmDialog = true
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Report Profile", fontWeight = FontWeight.Medium, color = funkyColors.danger) },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                                showReportDialog = true
                                            }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text("Share Profile", fontWeight = FontWeight.Medium, color = funkyColors.ink) },
                                            onClick = {
                                                showMoreOptionsMenu = false
                                                Toast.makeText(context, "Profile link copied", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Circular Avatar with Online Ring & Bottom-Right Overlapping Flag
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp)
                            .size(102.dp)
                    ) {
                        com.example.ui.components.PresenceAvatar(
                            avatarUrl = currentAvatarUrl,
                            size = 80.dp,
                            isSpeaking = false,
                            isOnline = isOnline,
                            countryCode = currentCountryCode,
                            userId = userId,
                            showFlagBadge = true,
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

                    // Row: Name, Verified/Badges & Online Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.store.DecoratedUserName(
                            userId = userId,
                            name = currentProfileName.ifBlank { "User" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            defaultColor = funkyColors.ink
                        )

                        if (isVerified) {
                            ProfileVerifiedBadge(modifier = Modifier.size(18.dp))
                        }

                        // Profile Badges
                        if (userBadges.isNotEmpty()) {
                            ProfileBadgesRow(badges = userBadges, badgeSize = 20.dp)
                        }

                        // Online Status Pill
                        if (isOnline) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isDark) Color(0xFF143523) else Color(0xFFEBFDF2),
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
                                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Handle: @username
                    Text(
                        text = "@$currentUsername",
                        fontSize = 13.sp,
                        color = funkyColors.mute,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location (City & Country), Age & Join Date Row
                    val locationDisplay = remember(currentCity, currentCountry) {
                        when {
                            currentCity.isNotBlank() && currentCountry.isNotBlank() -> {
                                if (currentCity.equals(currentCountry, ignoreCase = true)) currentCountry
                                else "$currentCity, $currentCountry"
                            }
                            currentCity.isNotBlank() -> currentCity
                            currentCountry.isNotBlank() -> currentCountry
                            else -> ""
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (locationDisplay.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ProfileLocationPinIcon(modifier = Modifier.size(13.dp), color = funkyColors.mute)
                                Text(
                                    text = locationDisplay,
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
                        }

                        if (currentAge != null && currentAge!! > 0) {
                            Text(
                                text = "${currentAge} yrs",
                                fontSize = 12.sp,
                                color = funkyColors.mute,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = "·",
                                fontSize = 14.sp,
                                color = funkyColors.mute,
                                fontWeight = FontWeight.Bold
                            )
                        }

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
                    // 3. STATS ROW (Posts | Followers | Following - Real DB Counts)
                    // ---------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItemExact(count = formatStatsCount(postsCount), label = "Posts", modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(funkyColors.line)
                        )
                        StatItemExact(
                            count = formatStatsCount(followersCount),
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
                            count = formatStatsCount(followingCount),
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
                    // 4. ACTION BUTTONS: "Say Hi", "Send Gift", "Follow / Following"
                    // ---------------------------------------------------------
                    if (!isSelfProfile) {
                        if (isBlockedEffective) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = funkyColors.soft,
                                border = BorderStroke(1.dp, funkyColors.line)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isBlockedLocally) "You blocked this user" else USER_BLOCKED_ERROR_MESSAGE,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = funkyColors.ink,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isBlockedLocally) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        OutlinedButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val res = BlockRepository.unblockUser(context, userId)
                                                    res.onSuccess {
                                                        isBlockedWithPair = false
                                                        Toast.makeText(context, "$currentProfileName unblocked", Toast.LENGTH_SHORT).show()
                                                    }.onFailure { err ->
                                                        Toast.makeText(context, err.message ?: "Could not unblock user.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(100.dp),
                                            border = BorderStroke(1.dp, funkyColors.danger.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = "Unblock",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = funkyColors.danger
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Yellow Filled Button: Say Hi
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .background(funkyColors.mango, RoundedCornerShape(100.dp))
                                        .clickable { showWaveDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        ProfileWavingHandIcon(modifier = Modifier.size(16.dp), color = funkyColors.onMango)
                                        Text(
                                            text = "Say Hi",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = funkyColors.onMango
                                        )
                                    }
                                }

                                // Gift Button
                                Box(
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(46.dp)
                                        .background(funkyColors.soft, RoundedCornerShape(100.dp))
                                        .border(1.dp, funkyColors.mango.copy(alpha = 0.55f), RoundedCornerShape(100.dp))
                                        .clickable { showSendGiftSheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = "Send Gift",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) funkyColors.mango else Color(0xFFB45309)
                                        )
                                    }
                                }

                                // Follow / Following Pill Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .background(
                                            if (isFollowing) {
                                                if (isDark) Color(0xFF143523) else Color(0xFFEBFDF2)
                                            } else {
                                                funkyColors.surface
                                            },
                                            RoundedCornerShape(100.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isFollowing) funkyColors.ok else funkyColors.line,
                                            shape = RoundedCornerShape(100.dp)
                                        )
                                        .clickable { handleFollowToggle() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        ProfileAddPersonIcon(
                                            modifier = Modifier.size(16.dp),
                                            color = if (isFollowing) {
                                                if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                                            } else {
                                                funkyColors.ink
                                            }
                                        )
                                        Text(
                                            text = if (isFollowing) "Following" else "Follow",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFollowing) {
                                                if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                                            } else {
                                                funkyColors.ink
                                            }
                                        )
                                    }
                                }
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
                                .clickable { selectedMainTab = 0 },
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
                                .clickable { selectedMainTab = 1 },
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
                            text = currentBio.ifBlank { "Excited to practice languages and make new friends!" },
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
                // CARD 2B: GIFTS RECEIVED CARD
                // -------------------------------------------------------------
                item {
                    ProfileCardContainer(
                        title = "Gifts Received (${giftsReceived.size})",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        if (isGiftsLoading && giftsReceived.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = funkyColors.mango, modifier = Modifier.size(24.dp))
                            }
                        } else if (giftsReceived.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isSelfProfile) "You haven't received any gifts yet." else "Be the first to send a gift to $currentProfileName!",
                                    fontSize = 13.sp,
                                    color = funkyColors.mute,
                                    textAlign = TextAlign.Center
                                )
                                if (!isSelfProfile && !isBlockedEffective) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        onClick = { showSendGiftSheet = true },
                                        color = funkyColors.soft,
                                        border = BorderStroke(1.dp, funkyColors.mango.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(100.dp)
                                    ) {
                                        Text(
                                            text = "Send a Gift",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) funkyColors.mango else Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val distinctGifts = giftsReceived.groupBy { it.gift_name ?: "Gift" }
                                distinctGifts.entries.take(4).forEach { (name, items) ->
                                    val firstItem = items.first()
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = funkyColors.soft,
                                        border = BorderStroke(1.dp, funkyColors.line),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!firstItem.gift_icon_url.isNullOrBlank()) {
                                                    AnimatedGiftMedia(
                                                        url = firstItem.gift_icon_url!!,
                                                        contentDescription = name,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Text("Gift", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = funkyColors.mango)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = funkyColors.ink,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "x${items.size}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = funkyColors.mango
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // CARD 3: INTERESTS CARD (Dynamic Canvas Hobby Chips with Star fallback)
                // -------------------------------------------------------------
                item {
                    ProfileCardContainer(
                        title = "Interests",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        val hobbyChunks = currentHobbies.chunked(3)
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
                // CARD 4: BASIC INFO CARD (Age, Gender, Country - No City)
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
                                    Text(
                                        text = currentAge?.let { "$it" } ?: "—",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = funkyColors.ink
                                    )
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
                                val isMale = currentGender?.equals("Male", ignoreCase = true) == true
                                if (isMale) {
                                    ProfileGenderMaleIcon(modifier = Modifier.size(20.dp), color = funkyColors.mute)
                                } else {
                                    ProfileGenderFemaleIcon(modifier = Modifier.size(20.dp), color = funkyColors.mute)
                                }
                                Column {
                                    Text("Gender", fontSize = 11.sp, color = funkyColors.mute, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = currentGender?.capitalize() ?: "—",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = funkyColors.ink
                                    )
                                }
                            }

                            // Divider
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(funkyColors.line))

                            // Location (Country only)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1.8f).padding(start = 8.dp)
                            ) {
                                ProfileLocationPinIcon(modifier = Modifier.size(20.dp), color = funkyColors.mute)
                                Column {
                                    Text("Country", fontSize = 11.sp, color = funkyColors.mute, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = currentCountry.ifBlank { "—" },
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
                                .border(width = 0.5.dp, color = funkyColors.line),
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
                                    CustomGridIcon(color = if (selectedFeedTab == 0) funkyColors.mango else funkyColors.mute)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    if (selectedFeedTab == 0) {
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(2.5.dp)
                                                .background(funkyColors.mango, CircleShape)
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
                                    CustomReelsIcon(color = if (selectedFeedTab == 1) funkyColors.mango else funkyColors.mute)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    if (selectedFeedTab == 1) {
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(2.5.dp)
                                                .background(funkyColors.mango, CircleShape)
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
                                    CustomTaggedIcon(color = if (selectedFeedTab == 2) funkyColors.mango else funkyColors.mute)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    if (selectedFeedTab == 2) {
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(2.5.dp)
                                                .background(funkyColors.mango, CircleShape)
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

                if (isMomentsLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = funkyColors.mango, modifier = Modifier.size(32.dp))
                        }
                    }
                } else if (displayedPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ProfileCameraIcon(modifier = Modifier.size(28.dp), color = funkyColors.mute)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No posts shared yet",
                                    fontSize = 14.sp,
                                    color = funkyColors.mute,
                                    fontWeight = FontWeight.Medium
                                )
                            }
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

                                        if (isSelfProfile && post.status == "hidden") {
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
                        fallbackAuthorId = userId,
                        fallbackAuthorName = currentProfileName,
                        fallbackAvatarUrl = currentAvatarUrl,
                        fallbackCountryCode = currentCountryCode,
                        isSelfAuthor = isSelfProfile
                    )
                }.ifEmpty {
                    listOf(
                        post.toFallbackMomentDto(
                            fallbackAuthorId = userId,
                            fallbackAuthorName = currentProfileName,
                            fallbackAvatarUrl = currentAvatarUrl,
                            fallbackCountryCode = currentCountryCode,
                            isSelfAuthor = isSelfProfile
                        )
                    )
                }
            }

            MomentsV2FeedViewerDialog(
                moments = feedMoments,
                initialMomentId = post.id,
                currentUserNativeLanguage = nativeLanguages,
                isSelfProfile = isSelfProfile,
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

        // Modal: Quick Icebreaker / Wave Dialog (Identical to Discover screen flow)
        if (showWaveDialog) {
            AlertDialog(
                onDismissRequest = { showWaveDialog = false },
                containerColor = funkyColors.surface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileWavingHandIcon(
                            color = funkyColors.mango,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Say Hi to $currentProfileName",
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
                            "Hey $currentProfileName, want to practice speaking together?",
                            "Loved your profile! Let's do a language exchange.",
                            "Hi there! What kind of ${currentHobbies.firstOrNull() ?: "music"} do you like?"
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
                            val greeting = waveCustomMessage.ifBlank { "Hi!" }
                            handleSendWave(greeting)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Send & Chat", color = funkyColors.onMango, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWaveDialog = false }) {
                        Text("Cancel", color = funkyColors.mute)
                    }
                }
            )
        }

        // Send Gift Bottom Sheet
        if (showSendGiftSheet) {
            SendGiftBottomSheet(
                recipientId = userId,
                recipientName = currentProfileName,
                recipientAvatar = currentAvatarUrl,
                onDismiss = { showSendGiftSheet = false },
                onGiftSent = { remainingBalance ->
                    coroutineScope.launch {
                        val updated = GiftManager.fetchGiftsReceived(context, userId)
                        giftsReceived.clear()
                        giftsReceived.addAll(updated)
                    }
                }
            )
        }

        // Report Profile Dialog
        if (showReportDialog) {
            ReportUserDialog(
                reportedUserId = userId,
                reportedUserName = currentProfileName,
                contextType = "profile",
                contextId = null,
                onDismiss = { showReportDialog = false },
                onReportSubmitted = {
                    showReportDialog = false
                    Toast.makeText(context, "Report submitted. Thank you for keeping FunkyTalk safe.", Toast.LENGTH_SHORT).show()
                }
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

        if (showVaultGateDialog) {
            com.example.ui.screens.chat.VaultManagementDialogs(
                onDismissRequest = {
                    showVaultGateDialog = false
                    pendingWaveGreeting = null
                }
            )
        }

        if (showBlockConfirmDialog) {
            BlockUserConfirmDialog(
                displayName = currentProfileName.ifBlank { "User" },
                isBlocking = isBlockingUser,
                onDismiss = { if (!isBlockingUser) showBlockConfirmDialog = false },
                onConfirm = {
                    isBlockingUser = true
                    coroutineScope.launch {
                        val res = BlockRepository.blockUser(context, userId)
                        isBlockingUser = false
                        showBlockConfirmDialog = false
                        res.onSuccess {
                            isFollowing = false
                            isBlockedWithPair = true
                            Toast.makeText(context, "$currentProfileName blocked", Toast.LENGTH_SHORT).show()
                            onBack()
                        }.onFailure { err ->
                            Toast.makeText(context, err.message ?: "Could not block user.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// HELPER FORMATTERS
// -----------------------------------------------------------------------------

fun formatStatsCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

fun formatJoinedDate(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Joined recently"
    return try {
        val parsedTime = parseIsoDateToMillis(isoString) ?: return "Joined recently"
        val diff = System.currentTimeMillis() - parsedTime
        val days = diff / (1000L * 60 * 60 * 24)
        when {
            days < 1 -> "Joined today"
            days < 7 -> "Joined $days ${if (days == 1L) "day" else "days"} ago"
            days < 30 -> {
                val weeks = (days / 7).coerceAtLeast(1)
                "Joined $weeks ${if (weeks == 1L) "week" else "weeks"} ago"
            }
            days < 365 -> {
                val months = (days / 30).coerceAtLeast(1)
                "Joined $months ${if (months == 1L) "month" else "months"} ago"
            }
            else -> {
                val years = (days / 365).coerceAtLeast(1)
                "Joined $years ${if (years == 1L) "year" else "years"} ago"
            }
        }
    } catch (e: Exception) {
        "Joined recently"
    }
}

fun formatMomentTimestamp(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Recently"
    return try {
        val parsedTime = parseIsoDateToMillis(isoString) ?: return "Recently"
        val diff = System.currentTimeMillis() - parsedTime
        when {
            diff < 60_000L -> "Just now"
            diff < 3600_000L -> "${(diff / 60_000L).coerceAtLeast(1)}m ago"
            diff < 86400_000L -> "${(diff / 3600_000L).coerceAtLeast(1)}h ago"
            diff < 86400_000L * 7 -> "${(diff / 86400_000L).coerceAtLeast(1)}d ago"
            else -> java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(parsedTime))
        }
    } catch (e: Exception) {
        "Recently"
    }
}

fun parseIsoDateToMillis(iso: String): Long? {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    for (fmt in formats) {
        try {
            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = sdf.parse(iso)
            if (date != null) return date.time
        } catch (_: Exception) {}
    }
    return null
}

// -----------------------------------------------------------------------------
// HELPER REUSABLE PROFILE CARD CONTAINER
// -----------------------------------------------------------------------------

@Composable
fun ProfileCardContainer(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val funkyColors = com.example.ui.theme.LocalFunkyColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(funkyColors.surface, RoundedCornerShape(20.dp))
            .border(1.dp, funkyColors.line, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(14.dp)
                        .background(funkyColors.mango, RoundedCornerShape(2.dp))
                )
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = funkyColors.ink
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

// -----------------------------------------------------------------------------
// STATISTIC ITEM (EXACT WITH DIVIDER SUPPORT)
// -----------------------------------------------------------------------------

@Composable
fun StatItemExact(
    count: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val funkyColors = com.example.ui.theme.LocalFunkyColors.current
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = funkyColors.ink
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = funkyColors.mute,
            fontWeight = FontWeight.Medium
        )
    }
}

// -----------------------------------------------------------------------------
// INTEREST PILL WITH CUSTOM VECTOR ICON
// -----------------------------------------------------------------------------

@Composable
fun InterestPillCustom(
    title: String,
    bgColor: Color,
    contentColor: Color,
    icon: @Composable (Modifier) -> Unit
) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            icon(Modifier.size(13.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

// -----------------------------------------------------------------------------
// FULLSCREEN POST VIEW (FOR FEED CLICKS)
// -----------------------------------------------------------------------------

@Composable
fun FullscreenPostView(
    post: FeedPost,
    profileName: String,
    avatarUrl: String,
    currentLoggedInAvatar: String = "",
    onBack: () -> Unit,
    onLikeToggle: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onPostUpdated: (MomentDto) -> Unit = {},
    onPostDeleted: (String) -> Unit = {}
) {
    val fallbackMoment = remember(post.id, post.momentDto) {
        post.toFallbackMomentDto(
            fallbackAuthorName = profileName.ifBlank { "Member" },
            fallbackAvatarUrl = avatarUrl,
            isSelfAuthor = post.isMe
        )
    }

    MomentsV2FeedViewerDialog(
        moments = listOf(fallbackMoment),
        initialMomentId = fallbackMoment.id,
        isSelfProfile = post.isMe,
        onDismiss = onBack,
        onMomentUpdated = { updated ->
            if (updated.liked_by_me != post.isLikedByUser) {
                post.isLikedByUser = updated.liked_by_me
                onLikeToggle()
            }
            onPostUpdated(updated)
        },
        onMomentDeleted = onPostDeleted,
        onBookmarkToggled = { _, isBookmarked ->
            if (isBookmarked != post.isBookmarkedByUser) {
                post.isBookmarkedByUser = isBookmarked
                onBookmarkToggle()
            }
        }
    )
}

// -----------------------------------------------------------------------------
// PART 1: DEDICATED CANVAS DRAWN ICONS (NO MATERIAL ICONS, NO EMOJIS)
// -----------------------------------------------------------------------------

@Composable
fun ProfileBackArrowIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier) {
        val sw = 2.0f * density
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.65f, h * 0.20f)
            lineTo(w * 0.32f, h * 0.50f)
            lineTo(w * 0.65f, h * 0.80f)
        }
        drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ProfileShareIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier) {
        val sw = 1.9f * density
        val w = size.width
        val h = size.height

        // Tray outline
        val trayPath = Path().apply {
            moveTo(w * 0.15f, h * 0.55f)
            lineTo(w * 0.15f, h * 0.88f)
            lineTo(w * 0.85f, h * 0.88f)
            lineTo(w * 0.85f, h * 0.55f)
        }
        drawPath(trayPath, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Arrow line pointing up
        drawLine(color, start = Offset(w * 0.5f, h * 0.68f), end = Offset(w * 0.5f, h * 0.18f), strokeWidth = sw, cap = StrokeCap.Round)

        // Arrow head
        val arrowHead = Path().apply {
            moveTo(w * 0.32f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.18f)
            lineTo(w * 0.68f, h * 0.35f)
        }
        drawPath(arrowHead, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ProfileMoreDotsIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier) {
        val r = 1.8f * density
        val cx = size.width / 2f
        val h = size.height

        drawCircle(color, radius = r, center = Offset(cx, h * 0.25f))
        drawCircle(color, radius = r, center = Offset(cx, h * 0.50f))
        drawCircle(color, radius = r, center = Offset(cx, h * 0.75f))
    }
}

@Composable
fun ProfileCardMoreDotsIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF94A3B8)) {
    Canvas(modifier = modifier) {
        val r = 1.5f * density
        val cx = size.width / 2f
        val h = size.height

        drawCircle(color, radius = r, center = Offset(cx, h * 0.25f))
        drawCircle(color, radius = r, center = Offset(cx, h * 0.50f))
        drawCircle(color, radius = r, center = Offset(cx, h * 0.75f))
    }
}

@Composable
fun ProfileVerifiedBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w / 2f

        // Blue base circle
        drawCircle(Color(0xFF0284C7), radius = r, center = Offset(cx, cy))

        // White checkmark
        val checkPath = Path().apply {
            moveTo(w * 0.30f, h * 0.50f)
            lineTo(w * 0.44f, h * 0.66f)
            lineTo(w * 0.72f, h * 0.36f)
        }
        drawPath(checkPath, Color.White, style = Stroke(width = 1.8f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ProfileLocationPinIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF64748B)) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.90f)
            cubicTo(w * 0.25f, h * 0.60f, w * 0.15f, h * 0.45f, w * 0.15f, h * 0.35f)
            cubicTo(w * 0.15f, h * 0.15f, w * 0.30f, 0f, w * 0.5f, 0f)
            cubicTo(w * 0.70f, 0f, w * 0.85f, h * 0.15f, w * 0.85f, h * 0.35f)
            cubicTo(w * 0.85f, h * 0.45f, w * 0.75f, h * 0.60f, w * 0.5f, h * 0.90f)
            close()
        }
        drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.35f))
    }
}

@Composable
fun ProfileCalendarIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF64748B)) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height

        // Calendar body
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, h * 0.2f),
            size = Size(w, h * 0.8f),
            cornerRadius = CornerRadius(2.5.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Top pins
        drawLine(color, start = Offset(w * 0.3f, 0f), end = Offset(w * 0.3f, h * 0.2f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.7f, 0f), end = Offset(w * 0.7f, h * 0.2f), strokeWidth = sw, cap = StrokeCap.Round)

        // Header line
        drawLine(color, start = Offset(0f, h * 0.45f), end = Offset(w, h * 0.45f), strokeWidth = sw)
    }
}

@Composable
fun ProfileWavingHandIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier) {
        val sw = 1.8f * density
        val w = size.width
        val h = size.height

        // Hand outline (palm + 4 fingers + thumb)
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.90f)
            lineTo(w * 0.20f, h * 0.65f) // thumb
            cubicTo(w * 0.15f, h * 0.55f, w * 0.28f, h * 0.45f, w * 0.35f, h * 0.55f)
            lineTo(w * 0.40f, h * 0.30f) // index finger
            cubicTo(w * 0.40f, h * 0.20f, w * 0.52f, h * 0.20f, w * 0.52f, h * 0.30f)
            lineTo(w * 0.55f, h * 0.20f) // middle finger
            cubicTo(w * 0.55f, h * 0.10f, w * 0.67f, h * 0.10f, w * 0.67f, h * 0.20f)
            lineTo(w * 0.70f, h * 0.25f) // ring finger
            cubicTo(w * 0.70f, h * 0.15f, w * 0.82f, h * 0.15f, w * 0.82f, h * 0.28f)
            lineTo(w * 0.85f, h * 0.40f) // pinky
            cubicTo(w * 0.85f, h * 0.32f, w * 0.95f, h * 0.35f, w * 0.95f, h * 0.45f)
            cubicTo(w * 0.95f, h * 0.75f, w * 0.70f, h * 0.95f, w * 0.50f, h * 0.95f)
            close()
        }
        drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Wave motion lines (top left)
        val wave1 = Path().apply {
            moveTo(w * 0.10f, h * 0.20f)
            quadraticTo(w * 0.05f, h * 0.30f, w * 0.10f, h * 0.40f)
        }
        drawPath(wave1, color, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

@Composable
fun ProfileAddPersonIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier) {
        val sw = 1.8f * density
        val w = size.width
        val h = size.height

        // Head circle
        drawCircle(color, radius = w * 0.20f, center = Offset(w * 0.42f, h * 0.30f), style = Stroke(width = sw))

        // Shoulder curve
        val bodyPath = Path().apply {
            moveTo(w * 0.12f, h * 0.85f)
            cubicTo(w * 0.12f, h * 0.60f, w * 0.72f, h * 0.60f, w * 0.72f, h * 0.85f)
        }
        drawPath(bodyPath, color, style = Stroke(width = sw, cap = StrokeCap.Round))

        // Plus symbol on right
        drawLine(color, start = Offset(w * 0.82f, h * 0.30f), end = Offset(w * 0.82f, h * 0.50f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.72f, h * 0.40f), end = Offset(w * 0.92f, h * 0.40f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun ProfileSwapIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier) {
        val sw = 1.8f * density
        val w = size.width
        val h = size.height

        // Top arrow pointing right →
        drawLine(color, start = Offset(w * 0.15f, h * 0.35f), end = Offset(w * 0.85f, h * 0.35f), strokeWidth = sw, cap = StrokeCap.Round)
        val rightHead = Path().apply {
            moveTo(w * 0.65f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.35f)
            lineTo(w * 0.65f, h * 0.55f)
        }
        drawPath(rightHead, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Bottom arrow pointing left ←
        drawLine(color, start = Offset(w * 0.85f, h * 0.70f), end = Offset(w * 0.15f, h * 0.70f), strokeWidth = sw, cap = StrokeCap.Round)
        val leftHead = Path().apply {
            moveTo(w * 0.35f, h * 0.50f)
            lineTo(w * 0.15f, h * 0.70f)
            lineTo(w * 0.35f, h * 0.90f)
        }
        drawPath(leftHead, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ProfileMusicIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.80f)
            lineTo(w * 0.35f, h * 0.25f)
            lineTo(w * 0.85f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.70f)
        }
        drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = w * 0.18f, center = Offset(w * 0.25f, h * 0.80f))
        drawCircle(color, radius = w * 0.18f, center = Offset(w * 0.75f, h * 0.70f))
    }
}

@Composable
fun ProfileCameraIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, h * 0.25f),
            size = Size(w, h * 0.75f),
            cornerRadius = CornerRadius(2.5.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Camera top flash
        drawLine(color, start = Offset(w * 0.30f, h * 0.25f), end = Offset(w * 0.40f, h * 0.10f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.40f, h * 0.10f), end = Offset(w * 0.60f, h * 0.10f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.60f, h * 0.10f), end = Offset(w * 0.70f, h * 0.25f), strokeWidth = sw, cap = StrokeCap.Round)

        // Lens
        drawCircle(color, radius = w * 0.20f, center = Offset(w * 0.5f, h * 0.62f), style = Stroke(width = sw))
    }
}

@Composable
fun ProfilePlaneIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.90f, h * 0.10f)
            lineTo(w * 0.40f, h * 0.60f)
            lineTo(w * 0.10f, h * 0.55f)
            lineTo(w * 0.45f, h * 0.35f)
            lineTo(w * 0.60f, 0f)
            lineTo(w * 0.70f, 0f)
            lineTo(w * 0.55f, h * 0.30f)
            lineTo(w * 0.90f, h * 0.10f)
            close()
        }
        drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ProfileClapperIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, h * 0.15f),
            size = Size(w, h * 0.85f),
            cornerRadius = CornerRadius(2.5.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(color, start = Offset(0f, h * 0.45f), end = Offset(w, h * 0.45f), strokeWidth = sw)
        drawLine(color, start = Offset(w * 0.35f, h * 0.15f), end = Offset(w * 0.45f, h * 0.45f), strokeWidth = sw)
        drawLine(color, start = Offset(w * 0.70f, h * 0.15f), end = Offset(w * 0.80f, h * 0.45f), strokeWidth = sw)
    }
}

@Composable
fun ProfileFoodIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height

        // Fork
        drawLine(color, start = Offset(w * 0.30f, 0f), end = Offset(w * 0.30f, h), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.15f, 0f), end = Offset(w * 0.15f, h * 0.35f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.45f, 0f), end = Offset(w * 0.45f, h * 0.35f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.15f, h * 0.35f), end = Offset(w * 0.45f, h * 0.35f), strokeWidth = sw, cap = StrokeCap.Round)

        // Knife
        drawLine(color, start = Offset(w * 0.75f, 0f), end = Offset(w * 0.75f, h), strokeWidth = sw, cap = StrokeCap.Round)
        val knifeBlade = Path().apply {
            moveTo(w * 0.75f, 0f)
            cubicTo(w * 0.90f, h * 0.20f, w * 0.90f, h * 0.40f, w * 0.75f, h * 0.50f)
        }
        drawPath(knifeBlade, color, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

@Composable
fun ProfileMovieFilmIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(w, h),
            cornerRadius = CornerRadius(2.5.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.30f, h * 0.35f))
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.70f, h * 0.35f))
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.50f, h * 0.70f))
    }
}

@Composable
fun ProfileCakeIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF64748B)) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height

        // Cake bottom layer
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.10f, h * 0.45f),
            size = Size(w * 0.80f, h * 0.50f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Middle icing line
        drawLine(color, start = Offset(w * 0.10f, h * 0.70f), end = Offset(w * 0.90f, h * 0.70f), strokeWidth = sw)

        // Candle
        drawLine(color, start = Offset(w * 0.50f, h * 0.20f), end = Offset(w * 0.50f, h * 0.45f), strokeWidth = sw, cap = StrokeCap.Round)
        // Flame
        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.10f))
    }
}

@Composable
fun ProfileGenderMaleIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF64748B)) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height

        // Circle
        drawCircle(color, radius = w * 0.25f, center = Offset(w * 0.42f, h * 0.58f), style = Stroke(width = sw))

        // Arrow pointing top right
        drawLine(color, start = Offset(w * 0.58f, h * 0.42f), end = Offset(w * 0.85f, h * 0.15f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.62f, h * 0.15f), end = Offset(w * 0.85f, h * 0.15f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.85f, h * 0.15f), end = Offset(w * 0.85f, h * 0.38f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun ProfileGenderFemaleIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF64748B)) {
    Canvas(modifier = modifier) {
        val sw = 1.6f * density
        val w = size.width
        val h = size.height

        // Circle
        drawCircle(color, radius = w * 0.25f, center = Offset(w * 0.50f, h * 0.32f), style = Stroke(width = sw))

        // Cross below
        drawLine(color, start = Offset(w * 0.50f, h * 0.57f), end = Offset(w * 0.50f, h * 0.95f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.30f, h * 0.75f), end = Offset(w * 0.70f, h * 0.75f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomGridIcon(color: Color, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val sw = 1.9f * density
        val w = size.width
        val h = size.height

        val gap = 3.5f * density
        val rectW = (w - gap) / 2f
        val rectH = (h - gap) / 2f

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(rectW, rectH),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(rectW + gap, 0f),
            size = Size(rectW, rectH),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, rectH + gap),
            size = Size(rectW, rectH),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(rectW + gap, rectH + gap),
            size = Size(rectW, rectH),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun CustomReelsIcon(color: Color, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val sw = 1.9f * density
        val w = size.width
        val h = size.height

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(w, h),
            cornerRadius = CornerRadius(3.5.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(color = color, start = Offset(0f, h * 0.32f), end = Offset(w, h * 0.32f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.28f, 0f), end = Offset(w * 0.38f, h * 0.32f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.62f, 0f), end = Offset(w * 0.72f, h * 0.32f), strokeWidth = sw, cap = StrokeCap.Round)

        val triPath = Path().apply {
            moveTo(w * 0.42f, h * 0.48f)
            lineTo(w * 0.62f, h * 0.63f)
            lineTo(w * 0.42f, h * 0.78f)
            close()
        }
        drawPath(triPath, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CustomTaggedIcon(color: Color, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val sw = 1.9f * density
        val w = size.width
        val h = size.height

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(w, h),
            cornerRadius = CornerRadius(3.5.dp.toPx()),
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(color, radius = w * 0.16f, center = Offset(w / 2f, h * 0.42f), style = Stroke(width = sw))
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.80f)
            quadraticTo(w * 0.5f, h * 0.62f, w * 0.78f, h * 0.80f)
        }
        drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}
