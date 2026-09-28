package com.example.ui.screens.home.tabs

import android.util.Log
import android.widget.Toast
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
import com.example.ui.components.CircleFlag
import com.example.ui.components.DynamicHobbyChip
import com.example.ui.components.LanguageItem
import com.example.ui.components.LanguageResolver
import com.example.ui.components.MultiLanguageExchangeCard
import com.example.ui.components.PresetAvatarList
import com.example.ui.components.ProfileBadgesRow
import com.example.ui.components.SoftBlockIcon
import com.example.ui.components.resolveAvatarModel
import com.example.ui.screens.*
import com.example.ui.screens.home.dialogs.BlockedUsersBottomSheet
import com.example.ui.screens.home.dialogs.ModerationReviewDialog
import com.example.ui.screens.home.models.DarkText
import com.example.ui.screens.profile.EditProfileScreen
import com.example.ui.screens.home.models.GraySubtitle
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTabContent(
    currentUserProfile: UserProfile,
    onLogoutClick: () -> Unit,
    viewModel: AuthViewModel? = null,
    onOpenSupportChat: (reportId: String?) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedMainTab by remember { mutableStateOf(0) } // 0 = About Me, 1 = Feed
    var selectedFeedTab by remember { mutableStateOf(0) } // 0 = Grid, 1 = Video, 2 = Tagged
    var activeFullPost by remember { mutableStateOf<FeedPost?>(null) }
    var isFollowing by remember { mutableStateOf(false) }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showModerationDialog by remember { mutableStateOf(false) }
    var showBlockedUsersSheet by remember { mutableStateOf(false) }
    var showOptionsMenuSheet by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }

    var activeFollowerUserId by remember { mutableStateOf<String?>(null) }
    var activeFollowersMode by remember { mutableStateOf(true) } // true = followers, false = following
    var activeClickedProfile by remember { mutableStateOf<com.example.DiscoverableProfile?>(null) }

    val userId = currentUserProfile.uid
    val prefs = remember { context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE) }
    val savedBio = remember { prefs.getString("profile_bio_${userId}", null) }
    val savedCity = remember { prefs.getString("profile_city_${userId}", null) }
    val savedAge = remember { prefs.getInt("profile_age_${userId}", 0).takeIf { it > 0 } }

    var currentProfileName by remember(currentUserProfile.displayName) { mutableStateOf(currentUserProfile.displayName?.ifBlank { "pikaa" } ?: "pikaa") }
    var currentUsername by remember(currentUserProfile.username) { mutableStateOf(currentUserProfile.username?.ifBlank { "pikaa" } ?: "pikaa") }
    var currentAvatarUrl by remember(currentUserProfile.avatar) { mutableStateOf(currentUserProfile.avatar?.ifBlank { "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500" } ?: "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500") }
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

    LaunchedEffect(userId) {
        isAdmin = ModerationManager.checkIsAdmin(context)
        if (userId.isBlank()) return@LaunchedEffect

        // Refresh coin wallet & catalog
        GiftManager.refreshWallet(context)
        GiftManager.fetchCatalog(context)

        // Load profile badges
        try {
            val badges = com.example.BadgeManager.getBadgesForUser(userId)
            userBadges.clear()
            userBadges.addAll(badges)
        } catch (e: Exception) {
            android.util.Log.e("ProfileTabContent", "Error loading badges", e)
        }
        try {
            isGiftsLoading = true
            val gifts = GiftManager.fetchGiftsReceived(context, userId)
            giftsReceived.clear()
            giftsReceived.addAll(gifts)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
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
                    currentCountryCode = resolveCountryCode(p.country, p.country_code)
                    p.age?.let { currentAge = it }
                    p.gender?.let { currentGender = it }
                    isOnline = p.is_online ?: true
                    isVerified = p.is_verified ?: false
                    currentJoinedText = formatJoinedDate(p.created_at)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileTabContent", "Error loading profile data", e)
        }

        try {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                val postCountResp = com.example.SupabaseClient.api.countMomentsByUserId(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    userIdEq = "eq.$userId"
                )
                if (postCountResp.isSuccessful) {
                    val cr = postCountResp.headers()["Content-Range"]
                    postsCount = cr?.substringAfter("/")?.toIntOrNull()
                        ?: postCountResp.body()?.size ?: 0
                }

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
        } catch (e: Exception) {
            android.util.Log.e("ProfileTabContent", "Error loading stats counts", e)
        }

        try {
            isMomentsLoading = true
            val momentsResp = withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.SupabaseClient.api.getMomentsByUserId(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
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
                            timestamp = formatMomentTimestamp(m.created_at)
                        )
                    )
                }
                postsCount = momentsList.size
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileTabContent", "Error loading moments", e)
        } finally {
            isMomentsLoading = false
        }
    }

    val profileDisplayName = currentProfileName
    val profileUsername = currentUsername
    val profileAvatar = currentAvatarUrl
    val profileCover = "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=800"
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

    val posts = remember(momentsList) {
        if (momentsList.isNotEmpty()) momentsList else emptyList()
    }

    val displayedPosts = remember(selectedFeedTab, posts) {
        when (selectedFeedTab) {
            1 -> posts.filter { it.isVideo }
            2 -> posts.take(4)
            else -> posts
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
                                .shadow(elevation = 3.dp, shape = CircleShape)
                                .clickable {
                                    Toast.makeText(context, "Profile link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileShareIcon(modifier = Modifier.size(16.dp), color = Color(0xFF1E293B))
                        }

                        // Three Dots Menu Button (opens bottom sheet for Edit, Admin, Support, Logout)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White, CircleShape)
                                .shadow(elevation = 3.dp, shape = CircleShape)
                                .clickable { showOptionsMenuSheet = true }
                                .testTag("profile_more_options_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileMoreDotsIcon(modifier = Modifier.size(16.dp), color = Color(0xFF1E293B))
                        }
                    }

                    // Circular Avatar with Green Online Dot (Top-Right) & Flag (Bottom-Right)
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
                                model = resolveAvatarModel(profileAvatar),
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Top-Right Green Online Dot with White border
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-6).dp, y = 6.dp)
                                .size(14.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )

                        // Bottom-Right Circle Country Flag with White border
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-2).dp, y = (-2).dp)
                        ) {
                            CircleFlag(
                                countryCode = profileCountryCode,
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

                    // Row: Name "pikaa", Blue Verified Tick, and "Online" Pill Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = profileDisplayName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )

                        // Blue Verified Badge (Custom Canvas Drawn)
                        if (isVerified) {
                            ProfileVerifiedBadge(modifier = Modifier.size(18.dp))
                        }

                        // Profile Badges (e.g. Founder, Early Adopter, etc.)
                        if (userBadges.isNotEmpty()) {
                            ProfileBadgesRow(badges = userBadges, badgeSize = 20.dp)
                        }

                        // Green "🟢 Online" Status Pill
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

                    Spacer(modifier = Modifier.height(2.dp))

                    // Handle: @pikaa_07
                    Text(
                        text = "@${profileUsername}_07",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location & Join Date Row: 📍 Seoul, South Korea · 📅 Joined 3 months ago
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ProfileLocationPinIcon(modifier = Modifier.size(13.dp), color = Color(0xFF64748B))
                            Text(
                                text = profileCity,
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
                                .background(Color(0xFFE2E8F0))
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
                                .background(Color(0xFFE2E8F0))
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
                    // 4. ACTION BUTTONS: "👋 Say Hi" & "👤+ Follow / Edit"
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
                                .background(Color(0xFFFFD43B), RoundedCornerShape(100.dp))
                                .clickable {
                                    Toast.makeText(context, "👋 Sent a friendly wave!", Toast.LENGTH_SHORT).show()
                                }
                                .testTag("profile_say_hi_button"),
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

                        // Outlined Pill Button: Edit Profile / Follow
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .background(Color.White, RoundedCornerShape(100.dp))
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFCBD5E1),
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
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Edit Profile",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
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
                                .clickable { selectedMainTab = 0 }
                                .testTag("tab_about_me"),
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
                                .clickable { selectedMainTab = 1 }
                                .testTag("tab_feed"),
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
                            text = profileBio,
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
                                ProfileCakeIcon(modifier = Modifier.size(20.dp), color = Color(0xFF64748B))
                                Column {
                                    Text("Age", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                                    Text(profileAge, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
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
                                ProfileGenderFemaleIcon(modifier = Modifier.size(20.dp), color = Color(0xFF64748B))
                                Column {
                                    Text("Gender", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                                    Text(profileGender, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                }
                            }

                            // Divider
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFEDE8DC)))

                            // Location
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1.8f).padding(start = 8.dp)
                            ) {
                                ProfileLocationPinIcon(modifier = Modifier.size(20.dp), color = Color(0xFF64748B))
                                Column {
                                    Text("Location", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                                    Text(
                                        profileCity,
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

                // -------------------------------------------------------------
                // CARD 5: COIN WALLET & REWARDS
                // -------------------------------------------------------------
                item {
                    ProfileCardContainer(
                        title = "Coin Wallet 🪙",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFFFFBEB))
                                    .border(BorderStroke(1.dp, Color(0xFFFDE68A)), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
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
                                            color = Color(0xFF92400E)
                                        )
                                        Text(
                                            text = "100 free starter coins included",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = "Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "✨ More ways to get coins coming soon (payment gateway is deferred). Use coins to send animated gifts to your language exchange partners!",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
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

        // Full Screen Post View when a feed post is clicked
        activeFullPost?.let { post ->
            FullscreenPostView(
                post = post,
                profileName = profileDisplayName,
                avatarUrl = profileAvatar,
                onBack = { activeFullPost = null },
                onLikeToggle = {
                    post.isLikedByUser = !post.isLikedByUser
                },
                onBookmarkToggle = {
                    post.isBookmarkedByUser = !post.isBookmarkedByUser
                }
            )
        }
    }

    // Options Bottom Sheet (Triggered by 3 dots on top right)
    if (showOptionsMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsMenuSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Profile Settings & Actions",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 1. Edit Profile
                Surface(
                    onClick = {
                        showOptionsMenuSheet = false
                        showEditProfileDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileAddPersonIcon(modifier = Modifier.size(20.dp), color = Color(0xFF0F172A))
                        Text("Edit Profile & Avatar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                }

                // 2. Official Support Chat
                Surface(
                    onClick = {
                        showOptionsMenuSheet = false
                        onOpenSupportChat(null)
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFFBEB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileWavingHandIcon(modifier = Modifier.size(20.dp), color = Color(0xFFB45309))
                        Column {
                            Text("24/7 Official Support Chat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            Text("Get help or appeal reports", fontSize = 11.sp, color = Color(0xFF92400E))
                        }
                    }
                }

                // 3. Admin Moderation Hub (if Admin)
                if (isAdmin) {
                    Surface(
                        onClick = {
                            showOptionsMenuSheet = false
                            showModerationDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFECFDF5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProfileVerifiedBadge(modifier = Modifier.size(20.dp))
                            Column {
                                Text("Safety & Moderation Hub", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                Text("Admin Review Dashboard", fontSize = 11.sp, color = Color(0xFF065F46))
                            }
                        }
                    }
                }

                // 4. Blocked Users
                Surface(
                    onClick = {
                        showOptionsMenuSheet = false
                        showBlockedUsersSheet = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SoftBlockIcon(size = 18.dp, color = Color(0xFF475569))
                        Text("Blocked Users", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                }

                // 5. Logout
                Surface(
                    onClick = {
                        showOptionsMenuSheet = false
                        onLogoutClick()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF1F2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileSwapIcon(modifier = Modifier.size(18.dp), color = Color(0xFFE11D48))
                        Text("Log Out of Account", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                    }
                }
            }
        }
    }

    // Blocked Users Bottom Sheet
    if (showBlockedUsersSheet) {
        BlockedUsersBottomSheet(
            onDismiss = { showBlockedUsersSheet = false }
        )
    }

    // Edit Profile Dialog Fullscreen Redesign
    if (showEditProfileDialog) {
        EditProfileScreen(
            currentUserProfile = currentUserProfile.copy(
                displayName = currentProfileName,
                username = currentUsername,
                avatar = currentAvatarUrl,
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
                val p = context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE)
                val updatedBio = p.getString("profile_bio_${userId}", null)
                val updatedCity = p.getString("profile_city_${userId}", null)
                val updatedAge = p.getInt("profile_age_${userId}", 0).takeIf { it > 0 }
                val updatedCountry = p.getString("profile_country_${userId}", null)
                val updatedCountryCode = p.getString("profile_countryCode_${userId}", null)
                val updatedDisplayName = p.getString("profile_displayName_${userId}", null)
                val updatedUsername = p.getString("profile_username_${userId}", null)
                val updatedAvatar = p.getString("profile_avatar_${userId}", null)

                if (!updatedBio.isNullOrBlank()) currentBio = updatedBio
                if (!updatedCity.isNullOrBlank()) currentCity = updatedCity
                if (updatedAge != null) currentAge = updatedAge
                if (!updatedCountry.isNullOrBlank()) currentCountry = updatedCountry
                if (!updatedCountryCode.isNullOrBlank()) currentCountryCode = updatedCountryCode
                if (!updatedDisplayName.isNullOrBlank()) currentProfileName = updatedDisplayName
                if (!updatedUsername.isNullOrBlank()) currentUsername = updatedUsername
                if (!updatedAvatar.isNullOrBlank()) currentAvatarUrl = updatedAvatar
            }
        )
    }

    if (showModerationDialog) {
        val supabaseProfiles by (viewModel?.supabaseProfiles ?: MutableStateFlow(emptyList())).collectAsState()
        ModerationReviewDialog(
            onDismiss = { showModerationDialog = false },
            currentUserProfile = currentUserProfile,
            supabaseProfiles = supabaseProfiles
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
