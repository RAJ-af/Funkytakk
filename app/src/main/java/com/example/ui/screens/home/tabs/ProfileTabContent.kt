package com.example.ui.screens.home.tabs

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
import com.example.ModerationManager
import com.example.UserProfile
import com.example.ui.components.CircleFlag
import com.example.ui.components.DynamicHobbyChip
import com.example.ui.components.LanguageItem
import com.example.ui.components.LanguageResolver
import com.example.ui.components.MultiLanguageExchangeCard
import com.example.ui.components.PresetAvatarList
import com.example.ui.components.resolveAvatarModel
import com.example.ui.screens.*
import com.example.ui.screens.home.dialogs.ModerationReviewDialog
import com.example.ui.screens.home.models.DarkText
import com.example.ui.screens.home.models.GraySubtitle
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.flow.MutableStateFlow

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
    var showOptionsMenuSheet by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserProfile.uid) {
        isAdmin = ModerationManager.checkIsAdmin(context)
    }

    val profileDisplayName = currentUserProfile.displayName?.ifBlank { "pikaa" } ?: "pikaa"
    val profileUsername = currentUserProfile.username?.ifBlank { "pikaa" } ?: "pikaa"
    val profileAvatar = currentUserProfile.avatar?.ifBlank { "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500" }
        ?: "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500"
    val profileCover = "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=800"
    val profileCountryCode = currentUserProfile.countryCode?.ifBlank { "kr" } ?: "kr"
    val profileCountry = currentUserProfile.country?.ifBlank { "South Korea" } ?: "South Korea"
    val profileCity = if (profileCountryCode.lowercase() == "kr") "Seoul, South Korea" else "${profileCountry}"
    val profileAge = currentUserProfile.age?.toString()?.ifBlank { "19" } ?: "19"
    val profileGender = currentUserProfile.gender?.ifBlank { "Female" } ?: "Female"

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

    // Default Bio matching reference
    val profileBio = "I love music, sunset and good conversations! Always open to meet new people and learn about different cultures. Let's be friends! 💛"

    // Curated high-aesthetic feed posts
    val posts = remember {
        mutableStateListOf(
            FeedPost(
                id = "post1",
                type = PostType.IMAGE,
                imageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=600",
                caption = "Sunset selfie from the rooftop cafe! The golden hour view is absolutely stunning today. 🌇✨",
                likesCount = 384,
                commentsCount = 42,
                timestamp = "1 hour ago",
                isCarousel = true
            ),
            FeedPost(
                id = "post2",
                type = PostType.IMAGE,
                imageUrl = "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=600",
                caption = "Spent my afternoon in this cozy coffee shop studying Japanese vocabulary. ☕✍️📚",
                likesCount = 212,
                commentsCount = 19,
                timestamp = "4 hours ago"
            ),
            FeedPost(
                id = "post3",
                type = PostType.IMAGE,
                imageUrl = "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=600",
                caption = "Seoul streets at night have a completely different vibe! Neon lights everywhere. 🌃🇰🇷",
                likesCount = 529,
                commentsCount = 68,
                timestamp = "1 day ago",
                isVideo = true
            ),
            FeedPost(
                id = "post4",
                type = PostType.TEXT,
                textContent = "Excited to study Mandarin tonight! 📚 Anyone want to practice speaking together?",
                textBgColor = Color(0xFFF0FDF4),
                textTextColor = Color(0xFF166534),
                caption = "Mandarin speaking practice! DM me if you're free tonight. 😊💬",
                likesCount = 98,
                commentsCount = 31,
                timestamp = "1 day ago"
            ),
            FeedPost(
                id = "post5",
                type = PostType.IMAGE,
                imageUrl = "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=600",
                caption = "Throwback to my amazing road trip through the national parks last year. Nature is breathtaking! 🌲🚗🎒",
                likesCount = 416,
                commentsCount = 23,
                timestamp = "2 days ago",
                isCarousel = true
            ),
            FeedPost(
                id = "post6",
                type = PostType.IMAGE,
                imageUrl = "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=600",
                caption = "Look at this cute ginger cat I found near my hostel. It wanted some of my lunch! 🐱🍗",
                likesCount = 605,
                commentsCount = 74,
                timestamp = "3 days ago"
            ),
            FeedPost(
                id = "post7",
                type = PostType.TEXT,
                textContent = "Thinking about traveling to Kyoto next spring. Any suggestions for hidden gem spots? 🌸🏯",
                textBgColor = Color(0xFFF5F3FF),
                textTextColor = Color(0xFF5B21B6),
                caption = "Kyoto trip planning! Let me know your favorite places in the comments below. 👇⛩️",
                likesCount = 145,
                commentsCount = 48,
                timestamp = "4 days ago"
            ),
            FeedPost(
                id = "post8",
                type = PostType.IMAGE,
                imageUrl = "https://images.unsplash.com/photo-1484755560695-a4c7300c5629?w=600",
                caption = "Getting lost in my favorite playlist on my evening walk. Music speaks all languages. 🎧🚶‍♀️✨",
                likesCount = 310,
                commentsCount = 15,
                timestamp = "5 days ago",
                isCarousel = true
            ),
            FeedPost(
                id = "post9",
                type = PostType.IMAGE,
                imageUrl = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=600",
                caption = "Satisfying my ramen cravings with this authentic tonkotsu bowl. Absolutely delicious! 🍜😋🔥",
                likesCount = 472,
                commentsCount = 39,
                timestamp = "6 days ago"
            ),
            FeedPost(
                id = "post10",
                type = PostType.IMAGE,
                imageUrl = "https://images.unsplash.com/photo-1452780212940-6f5c0d14d848?w=600",
                caption = "Captured some magical rays of light piercing through the forest canopy this morning. 📷🌲✨",
                likesCount = 258,
                commentsCount = 12,
                timestamp = "1 week ago"
            ),
            FeedPost(
                id = "post11",
                type = PostType.IMAGE,
                imageUrl = "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=600",
                caption = "Laughter and late nights with some of the best people I met through this app. 💛🌍👋",
                likesCount = 511,
                commentsCount = 55,
                timestamp = "1 week ago"
            )
        )
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
                        ProfileVerifiedBadge(modifier = Modifier.size(18.dp))

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
                                text = "Joined 3 months ago",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---------------------------------------------------------
                    // 3. STATS ROW (12 Posts | 1.2K Followers | 128 Following)
                    // ---------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItemExact(count = "12", label = "Posts", modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(Color(0xFFE2E8F0))
                        )
                        StatItemExact(count = "1.2K", label = "Followers", modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(Color(0xFFE2E8F0))
                        )
                        StatItemExact(count = "128", label = "Following", modifier = Modifier.weight(1f))
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

                // 4. Logout
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

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var editUsername by remember { mutableStateOf(currentUserProfile.username ?: "") }
        var editDisplayName by remember { mutableStateOf(currentUserProfile.displayName ?: "") }
        var editAvatar by remember { mutableStateOf(currentUserProfile.avatar ?: PresetAvatarList.first().second) }
        var editCountry by remember { mutableStateOf(currentUserProfile.country ?: "South Korea") }
        var editCountryCode by remember { mutableStateOf(currentUserProfile.countryCode ?: "kr") }
        var showCountryPicker by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isChecking by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Edit Profile",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = DarkText
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Avatar preset picker row
                    Column {
                        Text("Choose Avatar Preset", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PresetAvatarList.take(4).forEach { pair ->
                                val url = pair.second
                                val isSelected = editAvatar == url
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) GoldAccent else Color.LightGray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .clickable { editAvatar = url }
                                ) {
                                    AsyncImage(
                                        model = pair.first,
                                        contentDescription = "Preset Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    // Username Input
                    Column {
                        Text("Username", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editUsername,
                            onValueChange = { input ->
                                errorMessage = null
                                editUsername = input.filter { it.isLetterOrDigit() || it == '_' }.lowercase()
                            },
                            placeholder = { Text("e.g. pikaa_learner", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_username_input"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = DarkText, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DarkText,
                                unfocusedTextColor = DarkText,
                                cursorColor = GoldAccent,
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        Text(
                            text = "Usernames must be unique and automatically lowercased",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Display Name Input
                    Column {
                        Text("Display Name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editDisplayName,
                            onValueChange = { editDisplayName = it },
                            placeholder = { Text("Enter your display name", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = DarkText, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DarkText,
                                unfocusedTextColor = DarkText,
                                cursorColor = GoldAccent,
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }

                    // Country & Flag Selector
                    Column {
                        Text("Country & Flag", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            onClick = { showCountryPicker = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.LightGray),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircleFlag(countryCode = editCountryCode, modifier = Modifier.size(20.dp).clip(CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = editCountry, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText)
                                }
                                Text("Change", fontSize = 12.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanUsername = editUsername.trim().lowercase()
                        if (cleanUsername.isBlank() || editDisplayName.isBlank()) {
                            errorMessage = "Username and display name cannot be blank."
                            return@Button
                        }
                        if (cleanUsername.length < 3) {
                            errorMessage = "Username must be at least 3 characters."
                            return@Button
                        }

                        val performUpdate = {
                            if (viewModel != null) {
                                viewModel.updateProfile(
                                    context = context,
                                    username = cleanUsername,
                                    displayName = editDisplayName,
                                    avatar = editAvatar,
                                    dob = currentUserProfile.dob ?: "",
                                    gender = currentUserProfile.gender ?: "",
                                    nativeLanguage = currentUserProfile.nativeLanguage ?: "",
                                    learningLanguage = currentUserProfile.learningLanguage ?: "",
                                    country = editCountry,
                                    countryCode = editCountryCode,
                                    hobbies = currentUserProfile.hobbies,
                                    onSuccess = {
                                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                        showEditProfileDialog = false
                                    }
                                )
                            } else {
                                showEditProfileDialog = false
                            }
                        }

                        if (cleanUsername == (currentUserProfile.username ?: "").lowercase()) {
                            performUpdate()
                        } else {
                            isChecking = true
                            if (viewModel != null) {
                                viewModel.checkUsernameUnique(cleanUsername, currentUserProfile.uid) { isUnique ->
                                    isChecking = false
                                    if (isUnique) {
                                        performUpdate()
                                    } else {
                                        errorMessage = "This username is already taken. Please pick another."
                                    }
                                }
                            } else {
                                showEditProfileDialog = false
                            }
                        }
                    },
                    enabled = !isChecking,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )

        // Simple Country Picker Dialog
        if (showCountryPicker) {
            var countrySearch by remember { mutableStateOf("") }
            val filteredCountries = remember(countrySearch) {
                if (countrySearch.isBlank()) CountryData.popularCountries + CountryData.allCountries.take(30)
                else CountryData.allCountries.filter { it.name.contains(countrySearch, ignoreCase = true) }
            }

            AlertDialog(
                onDismissRequest = { showCountryPicker = false },
                containerColor = Color.White,
                title = { Text("Select Country", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText) },
                text = {
                    Column(modifier = Modifier.height(300.dp)) {
                        OutlinedTextField(
                            value = countrySearch,
                            onValueChange = { countrySearch = it },
                            placeholder = { Text("Search country...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredCountries.size) { idx ->
                                val countryItem = filteredCountries[idx]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editCountry = countryItem.name
                                            editCountryCode = countryItem.flagCode
                                            showCountryPicker = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircleFlag(countryCode = countryItem.flagCode, modifier = Modifier.size(22.dp).clip(CircleShape))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(countryItem.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkText)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCountryPicker = false }) {
                        Text("Close", color = Color.Gray)
                    }
                }
            )
        }
    }

    if (showModerationDialog) {
        val supabaseProfiles by (viewModel?.supabaseProfiles ?: MutableStateFlow(emptyList())).collectAsState()
        ModerationReviewDialog(
            onDismiss = { showModerationDialog = false },
            currentUserProfile = currentUserProfile,
            supabaseProfiles = supabaseProfiles
        )
    }
}
