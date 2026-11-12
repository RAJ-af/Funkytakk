package com.example.ui.screens.home.tabs

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.moderation.ReportUserDialog
import com.example.ui.screens.home.models.MomentPost
import kotlinx.coroutines.launch

data class MomentV2Item(
    val id: String,
    val title: String,
    val subtitle: String,
    val authorName: String,
    val authorAvatar: String,
    val isVerified: Boolean = true,
    var isFollowing: Boolean = false,
    val location: String,
    val countryFlag: String,
    val languageBadge: String,
    val timeAgo: String,
    val tags: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val bgGradient: List<Color>? = null,
    val audioDuration: String? = null,
    var likesCount: Int = 0,
    var commentsCount: Int = 0,
    var bookmarksCount: Int = 0,
    var isLiked: Boolean = false,
    var isBookmarked: Boolean = false,
    val comments: MutableList<MomentV2Comment> = mutableListOf()
)

data class MomentV2Comment(
    val id: String,
    val authorName: String,
    val authorAvatar: String,
    val timeAgo: String,
    val content: String,
    val isNativeFix: Boolean = false,
    val originalText: String? = null,
    val correctedText: String? = null,
    val explanationTip: String? = null,
    var likesCount: Int = 0,
    var isHelpful: Boolean = false,
    val replyCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsTabContent(
    momentsPosts: List<MomentPost> = emptyList(),
    onToggleLike: ((MomentPost) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Trending, 1: My language
    var showCommentsSheet by remember { mutableStateOf(false) }
    var activeMomentForComments by remember { mutableStateOf<MomentV2Item?>(null) }
    var showCreatePostSheet by remember { mutableStateOf(false) }
    var reportingMoment by remember { mutableStateOf<MomentV2Item?>(null) }

    // Live moments feed initialized with reference spec items
    val momentsList = remember {
        mutableStateListOf(
            // 1. Reference Sunrise Hike in KL (Matches attached screenshot & doc)
            MomentV2Item(
                id = "moment_sunrise_kl",
                title = "Sunrise hike in KL 🌄",
                subtitle = "How do I say this naturally in Korean?\nAny native speakers? 🇰🇷",
                authorName = "Hawa",
                authorAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                isVerified = true,
                isFollowing = false,
                location = "Seoul, South Korea",
                countryFlag = "🇰🇷",
                languageBadge = "🇰🇷 Native",
                timeAgo = "2h ago",
                tags = listOf("#Native", "#Travel", "#Nature", "#Korean"),
                images = listOf("res_sunrise_kl", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800", "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=800"),
                likesCount = 12400,
                commentsCount = 412,
                bookmarksCount = 1800,
                isLiked = false,
                isBookmarked = false,
                comments = mutableListOf(
                    MomentV2Comment(
                        id = "c1",
                        authorName = "Mei",
                        authorAvatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
                        timeAgo = "1h",
                        content = "와, 대단해요! Keep going 🔥",
                        likesCount = 5,
                        replyCount = 2
                    ),
                    MomentV2Comment(
                        id = "c2",
                        authorName = "Hawa",
                        authorAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                        timeAgo = "40m",
                        content = "",
                        isNativeFix = true,
                        originalText = "공부 100일째",
                        correctedText = "공부한 지 100일째",
                        explanationTip = "Sounds more natural when you say you have been studying.",
                        likesCount = 12,
                        replyCount = 1
                    ),
                    MomentV2Comment(
                        id = "c3",
                        authorName = "Jin",
                        authorAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                        timeAgo = "30m",
                        content = "Which app do you use to practice?",
                        likesCount = 2
                    ),
                    MomentV2Comment(
                        id = "c4",
                        authorName = "Yuna",
                        authorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                        timeAgo = "12m",
                        content = "",
                        isNativeFix = true,
                        originalText = "오늘 공부",
                        correctedText = "오늘로 공부",
                        explanationTip = "Tip: add the time marker for “as of today”.",
                        likesCount = 4
                    )
                )
            ),
            // 2. Korean Study 100 Days (From Markdown spec)
            MomentV2Item(
                id = "moment_arun_100days",
                title = "오늘 한국어 공부 100일째! 🎉",
                subtitle = "Day 100 of studying Korean today! Every morning I practice vocabulary and listening.",
                authorName = "Arun",
                authorAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                isVerified = false,
                isFollowing = true,
                location = "Delhi, India",
                countryFlag = "🇮🇳",
                languageBadge = "🇮🇳 Hindi → 🇰🇷 Korean",
                timeAgo = "2h",
                tags = listOf("#Study", "#Korean", "#Milestone", "#Practice"),
                images = listOf("https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800", "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800"),
                likesCount = 8920,
                commentsCount = 156,
                bookmarksCount = 940,
                isLiked = false,
                isBookmarked = false,
                comments = mutableListOf(
                    MomentV2Comment(
                        id = "c2_1",
                        authorName = "Yuna",
                        authorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                        timeAgo = "1h",
                        content = "축하해요! Your dedication is inspiring 👏",
                        likesCount = 14
                    )
                )
            ),
            // 3. Jeju Weekend (From Markdown spec)
            MomentV2Item(
                id = "moment_mei_jeju",
                title = "Jeju weekend 🌊",
                subtitle = "Swipe for 3 photos. Teach me these in Korean!\n1. Emerald ocean waves 2. Black volcanic rocks",
                authorName = "Mei",
                authorAvatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200",
                isVerified = true,
                isFollowing = false,
                location = "Taipei, Taiwan",
                countryFlag = "🇹🇼",
                languageBadge = "🇹🇼 Native · 🇰🇷 Learning",
                timeAgo = "5h",
                tags = listOf("#Travel", "#Food", "#Jeju", "#Island"),
                images = listOf("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800", "https://images.unsplash.com/photo-1519046904884-53103b34b206?w=800", "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=800"),
                likesCount = 15200,
                commentsCount = 620,
                bookmarksCount = 2300,
                isLiked = false,
                isBookmarked = false
            ),
            // 4. Night Walk in Seoul (Audio Moment from Markdown spec)
            MomentV2Item(
                id = "moment_yuna_night",
                title = "Night walk in Seoul 🌙",
                subtitle = "Listen and tell me what I said wrong.\n0:24 🎧 Audio note recorded along Cheonggyecheon stream.",
                authorName = "Yuna",
                authorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                isVerified = true,
                isFollowing = false,
                location = "Seoul, South Korea",
                countryFlag = "🇰🇷",
                languageBadge = "🇰🇷 Native",
                timeAgo = "1h",
                tags = listOf("#City", "#NightWalk", "#Pronunciation"),
                images = listOf("https://images.unsplash.com/photo-1517154421773-0529f29ea451?w=800"),
                audioDuration = "0:24",
                likesCount = 6740,
                commentsCount = 280,
                bookmarksCount = 810,
                isLiked = false,
                isBookmarked = false
            ),
            // 5. Text Card with Color Gradient (From Create_post markdown spec)
            MomentV2Item(
                id = "moment_mei_mandarin",
                title = "What’s your favourite word in Mandarin?",
                subtitle = "Mine is 缘分 (yuánfèn) — fate / serendipity that brings people together ✨\nWhat word touches your soul in your language?",
                authorName = "Mei",
                authorAvatar = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=200",
                isVerified = true,
                isFollowing = false,
                location = "Taipei, Taiwan",
                countryFlag = "🇹🇼",
                languageBadge = "🇹🇼 Native · 📚 Mandarin",
                timeAgo = "5h",
                tags = listOf("#Culture", "#Mandarin", "#Vocab", "#LanguageLove"),
                bgGradient = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)),
                likesCount = 10100,
                commentsCount = 530,
                bookmarksCount = 1420,
                isLiked = false,
                isBookmarked = false
            )
        )
    }

    // Filter moments depending on selected tab
    val displayedMoments = remember(selectedTab, momentsList) {
        if (selectedTab == 0) {
            momentsList
        } else {
            momentsList.filter { it.languageBadge.contains("Korean") || it.countryFlag == "🇰🇷" }
        }
    }

    val verticalPagerState = rememberPagerState(pageCount = { displayedMoments.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (displayedMoments.isNotEmpty()) {
            VerticalPager(
                state = verticalPagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val moment = displayedMoments[pageIndex]
                MomentPageItem(
                    moment = moment,
                    onOpenComments = {
                        activeMomentForComments = moment
                        showCommentsSheet = true
                    },
                    onToggleFollow = {
                        moment.isFollowing = !moment.isFollowing
                        Toast.makeText(
                            context,
                            if (moment.isFollowing) "Following ${moment.authorName}" else "Unfollowed ${moment.authorName}",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onToggleLike = {
                        moment.isLiked = !moment.isLiked
                        if (moment.isLiked) moment.likesCount++ else moment.likesCount--
                    },
                    onToggleBookmark = {
                        moment.isBookmarked = !moment.isBookmarked
                        if (moment.isBookmarked) moment.bookmarksCount++ else moment.bookmarksCount--
                        Toast.makeText(
                            context,
                            if (moment.isBookmarked) "Saved to Bookmarks" else "Removed from Bookmarks",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onShare = {
                        Toast.makeText(context, "Moment link copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    onReport = {
                        reportingMoment = moment
                    }
                )
            }
        }

        // Top Overlay: Title, Tabs, and Create/Menu actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Moments v2",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Create Moment Button (+)
                    IconButton(
                        onClick = { showCreatePostSheet = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Moment",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 3-dots overflow menu
                    var showTopMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Create Moment") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    showCreatePostSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Feed") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    Toast.makeText(context, "Moments link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Trending vs My language tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Trending
                Column(
                    modifier = Modifier.clickable { selectedTab = 0 }
                ) {
                    Text(
                        text = "🔥 Trending",
                        fontSize = 15.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (selectedTab == 0) {
                        Box(
                            modifier = Modifier
                                .width(68.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFFFB800))
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }

                // Tab 2: My language
                Column(
                    modifier = Modifier.clickable { selectedTab = 1 }
                ) {
                    Text(
                        text = "My language",
                        fontSize = 15.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (selectedTab == 1) {
                        Box(
                            modifier = Modifier
                                .width(82.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFFFB800))
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }
    }

    // Comments Modal Bottom Sheet
    if (showCommentsSheet && activeMomentForComments != null) {
        val currentMoment = activeMomentForComments!!
        var newCommentText by remember { mutableStateOf("") }
        var isNativeFixMode by remember { mutableStateOf(false) }
        var fixOriginalInput by remember { mutableStateOf("") }
        var fixCorrectedInput by remember { mutableStateOf("") }
        var fixTipInput by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { showCommentsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E293B),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comments (${currentMoment.comments.size})",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = { showCommentsSheet = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Comments List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(currentMoment.comments) { comment ->
                        CommentItemRow(comment = comment)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Add Comment / Native Fix Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Toggle: Normal Comment vs Native Fix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isNativeFixMode,
                            onClick = { isNativeFixMode = !isNativeFixMode },
                            label = { Text("Native fix ✏️", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White
                            )
                        )
                        if (isNativeFixMode) {
                            Text(
                                text = "Help learner with native correction",
                                fontSize = 11.sp,
                                color = Color(0xFF6EE7B7)
                            )
                        }
                    }

                    if (isNativeFixMode) {
                        OutlinedTextField(
                            value = fixOriginalInput,
                            onValueChange = { fixOriginalInput = it },
                            label = { Text("Original phrase", fontSize = 12.sp) },
                            placeholder = { Text("e.g. 공부 100일째", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = fixCorrectedInput,
                            onValueChange = { fixCorrectedInput = it },
                            label = { Text("Corrected phrase", fontSize = 12.sp) },
                            placeholder = { Text("e.g. 공부한 지 100일째", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = fixTipInput,
                            onValueChange = { fixTipInput = it },
                            label = { Text("Explanation tip", fontSize = 12.sp) },
                            placeholder = { Text("Why this sounds more natural...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = {
                                Text(
                                    if (isNativeFixMode) "Add comment note..." else "Add a comment...",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (isNativeFixMode) {
                                    if (fixOriginalInput.isNotBlank() && fixCorrectedInput.isNotBlank()) {
                                        val newFix = MomentV2Comment(
                                            id = "c_${System.currentTimeMillis()}",
                                            authorName = "You",
                                            authorAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                                            timeAgo = "Just now",
                                            content = newCommentText,
                                            isNativeFix = true,
                                            originalText = fixOriginalInput,
                                            correctedText = fixCorrectedInput,
                                            explanationTip = fixTipInput.ifBlank { "Sounds more natural to native speakers." }
                                        )
                                        currentMoment.comments.add(0, newFix)
                                        currentMoment.commentsCount++
                                        fixOriginalInput = ""
                                        fixCorrectedInput = ""
                                        fixTipInput = ""
                                        newCommentText = ""
                                        isNativeFixMode = false
                                        Toast.makeText(context, "Native fix posted!", Toast.LENGTH_SHORT).show()
                                    }
                                } else if (newCommentText.isNotBlank()) {
                                    val newComment = MomentV2Comment(
                                        id = "c_${System.currentTimeMillis()}",
                                        authorName = "You",
                                        authorAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                                        timeAgo = "Just now",
                                        content = newCommentText
                                    )
                                    currentMoment.comments.add(0, newComment)
                                    currentMoment.commentsCount++
                                    newCommentText = ""
                                    Toast.makeText(context, "Comment posted!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB800))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Moment Sheet (from Create_post markdown spec)
    if (showCreatePostSheet) {
        CreateMomentBottomSheet(
            onDismiss = { showCreatePostSheet = false },
            onPostCreated = { newMoment ->
                momentsList.add(0, newMoment)
                showCreatePostSheet = false
                Toast.makeText(context, "Posted. It’s live in Trending.", Toast.LENGTH_SHORT).show()
                coroutineScope.launch {
                    verticalPagerState.scrollToPage(0)
                }
            }
        )
    }

    // Report Dialog
    reportingMoment?.let { moment ->
        ReportUserDialog(
            reportedUserId = moment.id,
            reportedUserName = moment.authorName,
            contextType = "moment",
            contextId = moment.id,
            onDismiss = { reportingMoment = null }
        )
    }
}

@Composable
private fun MomentPageItem(
    moment: MomentV2Item,
    onOpenComments: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit
) {
    val context = LocalContext.current
    val subPagerState = rememberPagerState(pageCount = {
        if (moment.images.isNotEmpty()) moment.images.size else 1
    })

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Background Content: Multi-photo pager or Color Gradient
        if (moment.bgGradient != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(moment.bgGradient)),
                contentAlignment = Alignment.Center
            ) {
                // Large quote / vocabulary card for text-only moments
                Text(
                    text = moment.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(32.dp),
                    lineHeight = 36.sp
                )
            }
        } else if (moment.images.isNotEmpty()) {
            HorizontalPager(
                state = subPagerState,
                modifier = Modifier.fillMaxSize()
            ) { subIndex ->
                val imgKey = moment.images[subIndex]
                if (imgKey == "res_sunrise_kl") {
                    Image(
                        painter = painterResource(id = R.drawable.moments_sunrise_kl),
                        contentDescription = moment.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imgKey)
                            .crossfade(true)
                            .build(),
                        contentDescription = moment.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Top and Bottom gradient scrims for contrast
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // Photo pagination pill & dots indicator (if multiple photos)
        if (moment.images.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Photo pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Photo ${subPagerState.currentPage + 1} / ${moment.images.size}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Center dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(moment.images.size) { dotIndex ->
                        val isSelected = subPagerState.currentPage == dotIndex
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White else Color.White.copy(alpha = 0.45f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(36.dp))
            }
        }

        // Right Action Bar: Like, Comment, Bookmark, Share
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Heart / Like Button
            ActionIconButton(
                icon = if (moment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                tint = if (moment.isLiked) Color(0xFFFF3366) else Color.White,
                countText = formatCount(moment.likesCount),
                onClick = onToggleLike
            )

            // Comment Button
            ActionIconButton(
                icon = Icons.Default.ChatBubbleOutline,
                tint = Color.White,
                countText = formatCount(moment.commentsCount),
                onClick = onOpenComments
            )

            // Bookmark / Save Button
            ActionIconButton(
                icon = if (moment.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                tint = if (moment.isBookmarked) Color(0xFFFFB800) else Color.White,
                countText = formatCount(moment.bookmarksCount),
                onClick = onToggleBookmark
            )

            // Share Button
            ActionIconButton(
                icon = Icons.Default.Share,
                tint = Color.White,
                countText = "",
                onClick = onShare
            )
        }

        // Bottom Left Content Overlay: Title, Subtitle, User info, Tags
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.82f)
                .padding(start = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            Text(
                text = moment.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 26.sp
            )

            // Subtitle / Caption
            Text(
                text = moment.subtitle,
                fontSize = 13.sp,
                color = Color(0xFFF1F5F9),
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Audio Player pill if audio moment
            if (!moment.audioDuration.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${moment.audioDuration} 🎧 Audio note",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // User Profile Row: Avatar, Name, Verified badge, Location/Country, Follow button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(moment.authorAvatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = moment.authorName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                )

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = moment.authorName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (moment.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Country Flag & Location & Time
                    Text(
                        text = "${moment.countryFlag} ${moment.location} · ${moment.timeAgo}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                // Follow Button
                OutlinedButton(
                    onClick = onToggleFollow,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, Color(0xFFFFB800)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (moment.isFollowing) Color(0xFFFFB800) else Color.Transparent
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (moment.isFollowing) "Following" else "Follow",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (moment.isFollowing) Color.Black else Color(0xFFFFB800)
                    )
                }
            }

            // Hashtags / Topics row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Language badge chip (e.g. 🇰🇷 Native)
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFB800).copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = moment.languageBadge,
                            color = Color(0xFFFFE082),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }

                items(moment.tags) { tag ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = tag,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    countText: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        if (countText.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = countText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CommentItemRow(comment: MomentV2Comment) {
    val context = LocalContext.current
    var isHelpfulLiked by remember { mutableStateOf(comment.isHelpful) }
    var helpfulCount by remember { mutableIntStateOf(comment.likesCount) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(comment.authorAvatar)
                .crossfade(true)
                .build(),
            contentDescription = comment.authorName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = comment.authorName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (comment.isNativeFix) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Text(
                            text = "Native fix",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = comment.timeAgo,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (comment.isNativeFix) {
                // Native Fix card with strikethrough original + green correction + tip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!comment.originalText.isNullOrBlank()) {
                            Text(
                                text = comment.originalText,
                                fontSize = 13.sp,
                                color = Color(0xFFF87171),
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                        if (!comment.correctedText.isNullOrBlank()) {
                            Text(
                                text = comment.correctedText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                        }
                        if (!comment.explanationTip.isNullOrBlank()) {
                            Text(
                                text = comment.explanationTip,
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = comment.content,
                    fontSize = 13.sp,
                    color = Color.White,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action row: Likes / Helpful count + Reply + Translate
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        isHelpfulLiked = !isHelpfulLiked
                        if (isHelpfulLiked) helpfulCount++ else helpfulCount--
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Helpful",
                        tint = if (isHelpfulLiked) Color(0xFFFFB800) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (helpfulCount > 0) "$helpfulCount Helpful" else "Helpful",
                        fontSize = 11.sp,
                        color = if (isHelpfulLiked) Color(0xFFFFB800) else Color.White.copy(alpha = 0.5f)
                    )
                }

                Text(
                    text = "Reply",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Replying to ${comment.authorName}", Toast.LENGTH_SHORT).show()
                    }
                )

                Text(
                    text = "Translate",
                    fontSize = 11.sp,
                    color = Color(0xFF60A5FA),
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Translation: ${comment.content}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMomentBottomSheet(
    onDismiss: () -> Unit,
    onPostCreated: (MomentV2Item) -> Unit
) {
    val context = LocalContext.current
    var captionText by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var isPhotoMode by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedLanguage by remember { mutableStateOf("Korean") }
    var showTranslationLine by remember { mutableStateOf(true) }
    var locationInput by remember { mutableStateOf("Seoul, South Korea") }
    val selectedTopics = remember { mutableStateListOf("#Study", "#Korean") }

    val gradientPresets = listOf(
        listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)),
        listOf(Color(0xFF2C3E50), Color(0xFF3498DB), Color(0xFF2980B9)),
        listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
        listOf(Color(0xFF654EA3), Color(0xFFEAAFC8)),
        listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    )

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            isPhotoMode = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Create Moment", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "${captionText.length}/400",
                    fontSize = 12.sp,
                    color = if (captionText.length > 400) Color(0xFFEF4444) else Color.White.copy(alpha = 0.6f)
                )
            }

            // Mode Selector: Photo vs Gradient
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isPhotoMode) Color(0xFFFFB800) else Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = if (isPhotoMode) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedImageUri != null) "Photo picked" else "Pick Photo",
                        color = if (isPhotoMode) Color.Black else Color.White,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        isPhotoMode = false
                        selectedImageUri = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (!isPhotoMode) Color(0xFFFFB800) else Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = if (!isPhotoMode) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Colour Card",
                        color = if (!isPhotoMode) Color.Black else Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            // Palette picker for text-only moments
            if (!isPhotoMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gradientPresets.forEachIndexed { index, grad ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(grad))
                                .border(
                                    2.dp,
                                    if (selectedColorIndex == index) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }
            }

            // Main Text Input
            OutlinedTextField(
                value = captionText,
                onValueChange = { if (it.length <= 400) captionText = it },
                placeholder = {
                    Text(
                        if (isPhotoMode) "Your text sits on top of your photo..." else "Your words sit on this colour...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Language Selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Which language is this post in?", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val langs = listOf("Korean", "English", "Mandarin", "Japanese", "Hindi", "Malay", "Spanish")
                    items(langs) { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = { selectedLanguage = lang },
                            label = { Text(lang, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFB800),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Translation Line Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show a translation line", fontSize = 13.sp, color = Color.White)
                Switch(
                    checked = showTranslationLine,
                    onCheckedChange = { showTranslationLine = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFFFB800), checkedThumbColor = Color.Black)
                )
            }

            // Topics Picker
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pick up to 3 topics so the right people find it:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                val availableTopics = listOf("#Travel", "#Study", "#Nature", "#Culture", "#Food", "#City", "#Native")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableTopics) { topic ->
                        val isPicked = selectedTopics.contains(topic)
                        FilterChip(
                            selected = isPicked,
                            onClick = {
                                if (isPicked) selectedTopics.remove(topic)
                                else if (selectedTopics.size < 3) selectedTopics.add(topic)
                            },
                            label = { Text(topic, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFB800),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Post Button
            Button(
                onClick = {
                    if (captionText.isBlank()) {
                        Toast.makeText(context, "Please enter some text for your moment", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val newMoment = MomentV2Item(
                        id = "moment_${System.currentTimeMillis()}",
                        title = captionText.lines().firstOrNull() ?: captionText,
                        subtitle = if (captionText.lines().size > 1) captionText.lines().drop(1).joinToString("\n") else "Language practice moment",
                        authorName = "You",
                        authorAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                        isVerified = true,
                        isFollowing = false,
                        location = locationInput,
                        countryFlag = if (selectedLanguage == "Korean") "🇰🇷" else "🌐",
                        languageBadge = "$selectedLanguage · Learner",
                        timeAgo = "Just now",
                        tags = selectedTopics.toList(),
                        images = if (isPhotoMode && selectedImageUri != null) listOf(selectedImageUri.toString()) else emptyList(),
                        bgGradient = if (!isPhotoMode) gradientPresets[selectedColorIndex] else null
                    )
                    onPostCreated(newMoment)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800))
            ) {
                Text("Post Moment", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
