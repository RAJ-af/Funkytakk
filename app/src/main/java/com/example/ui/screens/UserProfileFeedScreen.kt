package com.example.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.example.BuildConfig
import com.example.SupabaseClient
import com.example.SupabaseFollower
import com.example.SupabaseMoment
import com.example.SupabaseSessionManager
import com.example.repository.EncryptedChatRepository
import com.example.ui.components.CircleFlag
import com.example.ui.components.DynamicHobbyChip
import com.example.ui.components.LanguageItem
import com.example.ui.components.LanguageResolver
import com.example.ui.components.MultiLanguageExchangeCard
import com.example.ui.screens.home.tabs.resolveCountryCode
import com.example.ui.screens.post.DetailedPost
import com.example.ui.screens.post.PostComment
import com.example.ui.screens.post.PostDetailScreen
import com.example.ui.screens.post.createDefaultSamplePost
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
    var isBookmarkedByUser: Boolean = false
)

enum class PostType {
    IMAGE, TEXT
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

    val selfUserId = remember(context) { SupabaseSessionManager.getUserId(context) ?: "" }
    val isSelfProfile = remember(userId, selfUserId) { userId.isNotBlank() && userId == selfUserId }

    var selectedMainTab by remember { mutableStateOf(0) } // 0 = About Me, 1 = Feed
    var selectedFeedTab by remember { mutableStateOf(0) } // 0 = Grid, 1 = Video, 2 = Tagged
    var activeFullPost by remember { mutableStateOf<FeedPost?>(null) }

    // Dynamic Profile State (from discoverable_profiles)
    var currentProfileName by remember(profileName) { mutableStateOf(profileName) }
    var currentUsername by remember(username) { mutableStateOf(username) }
    var currentAvatarUrl by remember(avatarUrl) { mutableStateOf(avatarUrl) }
    var currentBio by remember(bio) { mutableStateOf(bio) }
    var currentCountry by remember { mutableStateOf("") }
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

    // Say Hi & Wave Dialog State
    var showWaveDialog by remember { mutableStateOf(false) }
    var waveCustomMessage by remember { mutableStateOf("") }
    var isSendingHi by remember { mutableStateOf(false) }

    var activeFollowerUserId by remember { mutableStateOf<String?>(null) }
    var activeFollowersMode by remember { mutableStateOf(true) } // true = followers, false = following
    var activeClickedProfile by remember { mutableStateOf<com.example.DiscoverableProfile?>(null) }

    // -------------------------------------------------------------------------
    // 1. FETCH REAL SUPABASE DATA ON LAUNCH
    // -------------------------------------------------------------------------
    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect

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
                    p.bio?.let { if (it.isNotBlank()) currentBio = it }
                    p.country?.let { currentCountry = it }
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
        } catch (e: Exception) {
            Log.e("UserProfileFeed", "Error loading profile data", e)
        }

        // B. Fetch stats counts (Posts / Followers / Following)
        try {
            withContext(Dispatchers.IO) {
                // 1. Posts count
                val postCountResp = SupabaseClient.api.countMomentsByUserId(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    userIdEq = "eq.$userId"
                )
                if (postCountResp.isSuccessful) {
                    val cr = postCountResp.headers()["Content-Range"]
                    postsCount = cr?.substringAfter("/")?.toIntOrNull()
                        ?: postCountResp.body()?.size ?: 0
                }

                // 2. Followers count
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

                // 3. Following count
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

        // D. Fetch user's moments (Feed tab)
        try {
            isMomentsLoading = true
            val momentsResp = withContext(Dispatchers.IO) {
                SupabaseClient.api.getMomentsByUserId(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    userIdEq = "eq.$userId"
                )
            }
            if (momentsResp.isSuccessful) {
                val moments = momentsResp.body().orEmpty()
                momentsList.clear()
                moments.forEach { m ->
                    val firstImg = m.image_urls?.firstOrNull()
                    momentsList.add(
                        FeedPost(
                            id = m.id,
                            type = if (!firstImg.isNullOrBlank()) PostType.IMAGE else PostType.TEXT,
                            imageUrl = firstImg,
                            allImages = m.image_urls ?: emptyList(),
                            textContent = if (firstImg.isNullOrBlank()) m.content else null,
                            caption = m.content.orEmpty(),
                            likesCount = m.likes_count ?: 0,
                            commentsCount = m.comments_count ?: 0,
                            timestamp = formatMomentTimestamp(m.created_at),
                            isCarousel = (m.image_urls?.size ?: 0) > 1
                        )
                    )
                }
                if (postsCount == 0 && momentsList.isNotEmpty()) {
                    postsCount = momentsList.size
                }
            }
        } catch (e: Exception) {
            Log.e("UserProfileFeed", "Error fetching user moments", e)
        } finally {
            isMomentsLoading = false
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

    // Say Hi / Wave Handler
    fun handleSendWave(greetingMessage: String) {
        if (userId.isBlank() || isSendingHi) {
            onStartChat()
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
                    } else {
                        Log.w("UserProfileFeed", "Wave send: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    Log.d("UserProfileFeed", "E2EE keys missing for $currentProfileName, starting chat window.")
                }
            } catch (e: Exception) {
                Log.e("UserProfileFeed", "Error sending wave message", e)
            } finally {
                isSendingHi = false
                showWaveDialog = false
                onStartChat()
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

    val displayedPosts = remember(selectedFeedTab, momentsList) {
        when (selectedFeedTab) {
            1 -> momentsList.filter { it.isVideo }
            2 -> momentsList.take(4)
            else -> momentsList
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
            .background(Color(0xFFFAFAEE))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAEE)),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // -----------------------------------------------------------------
            // 1. COVER PHOTO & FLOATING ACTION BUTTONS (Back, Share, Menu)
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
                            .data(coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp)
                    )

                    // Floating Round Back Button (Top Left)
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp, top = 16.dp)
                            .align(Alignment.TopStart)
                            .size(38.dp)
                            .background(Color.White, CircleShape)
                            .shadow(elevation = 2.dp, shape = CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileBackArrowIcon(modifier = Modifier.size(16.dp), color = Color(0xFF1E293B))
                    }

                    // Floating Round Actions (Share & Menu) (Top Right)
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
                                .background(Color.White, CircleShape)
                                .shadow(elevation = 2.dp, shape = CircleShape)
                                .clickable {
                                    Toast.makeText(context, "Shared profile link", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileShareIcon(modifier = Modifier.size(16.dp), color = Color(0xFF1E293B))
                        }

                        // Three Dots Menu Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White, CircleShape)
                                .shadow(elevation = 2.dp, shape = CircleShape)
                                .clickable {
                                    Toast.makeText(context, "More options", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileMoreDotsIcon(modifier = Modifier.size(16.dp), color = Color(0xFF1E293B))
                        }
                    }

                    // Circular Avatar with Online Dot & Flag
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp)
                            .size(102.dp)
                    ) {
                        // Avatar Photo with White Border
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(width = 4.dp, color = Color.White, shape = CircleShape)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(currentAvatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Top-Right Green Online Dot (only if online)
                        if (isOnline) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-6).dp, y = 6.dp)
                                    .size(14.dp)
                                    .background(Color(0xFF22C55E), CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }

                        // Bottom-Right Circle Country Flag with White border
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-2).dp, y = (-2).dp)
                        ) {
                            CircleFlag(
                                countryCode = currentCountryCode,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }
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

                    // Row: Name and "Online" / "Offline" Pill Badge (Verified badge hidden per requirement)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = currentProfileName.ifBlank { "User" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )

                        if (isVerified) {
                            ProfileVerifiedBadge(modifier = Modifier.size(18.dp))
                        }

                        // Online Status Pill
                        if (isOnline) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFEBFDF2), RoundedCornerShape(100.dp))
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
                                        color = Color(0xFF16A34A)
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
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location (Country only, NO city) & Join Date Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentCountry.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ProfileLocationPinIcon(modifier = Modifier.size(13.dp), color = Color(0xFF64748B))
                                Text(
                                    text = currentCountry,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            Text(
                                text = "·",
                                fontSize = 14.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ProfileCalendarIcon(modifier = Modifier.size(13.dp), color = Color(0xFF64748B))
                            Text(
                                text = currentJoinedText,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
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
                                .background(Color(0xFFE2E8F0))
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
                                .background(Color(0xFFE2E8F0))
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
                    // 4. ACTION BUTTONS: "👋 Say Hi" & "👤+ Follow / Following"
                    // ---------------------------------------------------------
                    if (!isSelfProfile) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Yellow Filled Button: Say Hi
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .background(Color(0xFFFFD43B), RoundedCornerShape(100.dp))
                                    .clickable { showWaveDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ProfileWavingHandIcon(modifier = Modifier.size(18.dp), color = Color(0xFF0F172A))
                                    Text(
                                        text = "Say Hi",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            // Follow / Following Pill Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .background(if (isFollowing) Color(0xFFEBFDF2) else Color.White, RoundedCornerShape(100.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (isFollowing) Color(0xFF22C55E) else Color(0xFFCBD5E1),
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                    .clickable { handleFollowToggle() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ProfileAddPersonIcon(
                                        modifier = Modifier.size(18.dp),
                                        color = if (isFollowing) Color(0xFF16A34A) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (isFollowing) "Following" else "Follow",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFollowing) Color(0xFF16A34A) else Color(0xFF0F172A)
                                    )
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
                            .background(Color(0xFFEBE6D8), RoundedCornerShape(100.dp))
                            .padding(3.5.dp)
                    ) {
                        // About Me Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (selectedMainTab == 0) Color.White else Color.Transparent)
                                .border(
                                    width = if (selectedMainTab == 0) 1.dp else 0.dp,
                                    color = if (selectedMainTab == 0) Color(0xFFFDE68A) else Color.Transparent,
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .clickable { selectedMainTab = 0 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "About Me",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMainTab == 0) Color(0xFF0F172A) else Color(0xFF64748B)
                            )
                        }

                        // Feed Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (selectedMainTab == 1) Color.White else Color.Transparent)
                                .border(
                                    width = if (selectedMainTab == 1) 1.dp else 0.dp,
                                    color = if (selectedMainTab == 1) Color(0xFFFDE68A) else Color.Transparent,
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .clickable { selectedMainTab = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Feed",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMainTab == 1) Color(0xFF0F172A) else Color(0xFF64748B)
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
                            color = Color(0xFF334155),
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
                                ProfileCakeIcon(modifier = Modifier.size(20.dp), color = Color(0xFF64748B))
                                Column {
                                    Text("Age", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                                    Text(
                                        text = currentAge?.let { "$it" } ?: "—",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            // Divider
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFEDE8DC)))

                            // Gender
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1.2f).padding(horizontal = 8.dp)
                            ) {
                                val isMale = currentGender?.equals("Male", ignoreCase = true) == true
                                if (isMale) {
                                    ProfileGenderMaleIcon(modifier = Modifier.size(20.dp), color = Color(0xFF64748B))
                                } else {
                                    ProfileGenderFemaleIcon(modifier = Modifier.size(20.dp), color = Color(0xFF64748B))
                                }
                                Column {
                                    Text("Gender", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                                    Text(
                                        text = currentGender?.capitalize() ?: "—",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            // Divider
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFEDE8DC)))

                            // Location (Country only)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1.8f).padding(start = 8.dp)
                            ) {
                                ProfileLocationPinIcon(modifier = Modifier.size(20.dp), color = Color(0xFF64748B))
                                Column {
                                    Text("Country", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                                    Text(
                                        text = currentCountry.ifBlank { "—" },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A),
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

                if (isMomentsLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFFD43B), modifier = Modifier.size(32.dp))
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
                                Text("📸", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No posts shared yet",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B),
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
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(post.textBgColor)
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = post.textContent.orEmpty(),
                                                    color = post.textTextColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 5,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 15.sp
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

        // Full Screen Post Detail View when a feed post is clicked
        activeFullPost?.let { post ->
            val detailedImages = if (post.allImages.isNotEmpty()) post.allImages else if (post.imageUrl != null) listOf(post.imageUrl) else emptyList()
            val detailedPost = DetailedPost(
                id = post.id,
                authorName = currentProfileName,
                authorUsername = currentUsername,
                authorAvatarUrl = currentAvatarUrl,
                isAuthorVerified = false,
                location = currentCountry.ifBlank { "Global" },
                timeAgo = post.timestamp,
                images = if (detailedImages.isNotEmpty()) detailedImages else listOf(
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=1000"
                ),
                caption = post.caption,
                likesCount = post.likesCount,
                isLiked = post.isLikedByUser,
                isBookmarked = post.isBookmarkedByUser,
                initialComments = listOf(
                    PostComment("c1", "Minji", "minji", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300", "3 days ago", "So pretty!! 😍", 12, true),
                    PostComment("c2", "Ken", "ken_02", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=300", "3 days ago", "Amazing photo! ✨", 8, false)
                )
            )

            PostDetailScreen(
                post = detailedPost,
                currentLoggedInAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                onBack = { activeFullPost = null },
                onLikePostToggle = { isLiked ->
                    post.isLikedByUser = isLiked
                },
                onBookmarkPostToggle = { isBookmarked ->
                    post.isBookmarkedByUser = isBookmarked
                }
            )
        }

        // Modal: Quick Icebreaker / Wave Dialog (Identical to Discover screen flow)
        if (showWaveDialog) {
            AlertDialog(
                onDismissRequest = { showWaveDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileWavingHandIcon(
                            color = Color(0xFFD97706),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Say Hi to $currentProfileName",
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
                            "Hey $currentProfileName, want to practice speaking together?",
                            "Loved your profile! Let's do a language exchange.",
                            "Hi there! What kind of ${currentHobbies.firstOrNull() ?: "music"} do you like?"
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
                            val greeting = waveCustomMessage.ifBlank { "👋 Hi!" }
                            handleSendWave(greeting)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD43B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Send & Chat", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWaveDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
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
                countryCode = user.country_code ?: "us",
                bio = user.bio ?: "",
                onBack = { activeClickedProfile = null },
                onStartChat = {
                    activeClickedProfile = null
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFEDE8DC), RoundedCornerShape(20.dp))
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
                        .background(Color(0xFFFFD43B), RoundedCornerShape(2.dp))
                )
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
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
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF0F172A)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = Color(0xFF64748B),
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
    onBack: () -> Unit,
    onLikeToggle: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    val detailedPost = remember(post.id) {
        val sampleImages = if (post.imageUrl != null) {
            listOf(
                post.imageUrl,
                "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=1000",
                "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1000",
                "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1000",
                "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1000"
            )
        } else {
            listOf(
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=1000",
                "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=1000",
                "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1000"
            )
        }

        DetailedPost(
            id = post.id,
            authorName = profileName.ifBlank { "pikaa" },
            authorUsername = "${profileName.lowercase().replace(" ", "")}_07",
            authorAvatarUrl = avatarUrl,
            isAuthorVerified = true,
            location = "Seoul, South Korea",
            timeAgo = post.timestamp,
            images = sampleImages,
            caption = post.caption.ifBlank { "Beautiful sunset today! Nature always gives me so much peace. 🌅 💛" },
            likesCount = post.likesCount,
            isLiked = post.isLikedByUser,
            isBookmarked = post.isBookmarkedByUser,
            initialComments = listOf(
                PostComment(
                    id = "c1",
                    authorName = "Minji",
                    username = "minji",
                    avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300",
                    timeAgo = "3 days ago",
                    text = "So pretty!! 😍 Where is this place?",
                    likesCount = 12,
                    isLiked = true
                ),
                PostComment(
                    id = "c2",
                    authorName = "Ken",
                    username = "ken_02",
                    avatarUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=300",
                    timeAgo = "3 days ago",
                    text = "The sky looks amazing! ✨",
                    likesCount = 8,
                    isLiked = false
                ),
                PostComment(
                    id = "c3",
                    authorName = "Sora",
                    username = "sora_jp",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300",
                    timeAgo = "2 days ago",
                    text = "I want to visit Korea someday! 🇰🇷 💛",
                    likesCount = 5,
                    isLiked = false
                ),
                PostComment(
                    id = "c4",
                    authorName = "Alex",
                    username = "alex_99",
                    avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300",
                    timeAgo = "1 day ago",
                    text = "Incredible photography angle! What camera did you use?",
                    likesCount = 4,
                    isLiked = false
                ),
                PostComment(
                    id = "c5",
                    authorName = "Yuki",
                    username = "yuki_nihon",
                    avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=300",
                    timeAgo = "18 hours ago",
                    text = "Golden hour in Seoul is truly magical 🌇",
                    likesCount = 3,
                    isLiked = false
                )
            )
        )
    }

    PostDetailScreen(
        post = detailedPost,
        currentLoggedInAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
        onBack = onBack,
        onLikePostToggle = { isLiked ->
            post.isLikedByUser = isLiked
            onLikeToggle()
        },
        onBookmarkPostToggle = { isBookmarked ->
            post.isBookmarkedByUser = isBookmarked
            onBookmarkToggle()
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
