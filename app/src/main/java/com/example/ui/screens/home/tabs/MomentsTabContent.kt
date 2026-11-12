package com.example.ui.screens.home.tabs

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.CloudAiManager
import com.example.MomentCommentDto
import com.example.MomentDto
import com.example.MomentMediaDto
import com.example.moderation.ReportUserDialog
import com.example.repository.MomentRpcException
import com.example.repository.MomentsRepository
import com.example.ui.components.CircleFlag
import com.example.ui.components.resolveAvatarModel
import com.example.ui.screens.LanguageData
import com.example.ui.screens.formatMomentTimestamp
import com.example.ui.screens.post.CreatePostScreen
import kotlinx.coroutines.launch

private class MomentTabFeedState(val tabKey: String) {
    val items = mutableStateListOf<MomentDto>()
    var isInitialLoading by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
    var isLoadingMore by mutableStateOf(false)
    var hasMore by mutableStateOf(true)
    var nextOffset by mutableIntStateOf(0)
    var errorMessage by mutableStateOf<String?>(null)
    var hasLoadedOnce by mutableStateOf(false)
}

private data class WordSpan(
    val word: String,
    val startIndex: Int,
    val endIndexExclusive: Int
)

private fun tokenizeWordsWithSpans(text: String): List<WordSpan> {
    val regex = Regex("\\S+")
    return regex.findAll(text).map { match ->
        WordSpan(
            word = match.value,
            startIndex = match.range.first,
            endIndexExclusive = match.range.last + 1
        )
    }.toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsTabContent(
    currentUserNativeLanguage: String = ""
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember(context) {
        context.getSharedPreferences("funky_talk_prefs", android.content.Context.MODE_PRIVATE)
    }
    val bookmarkedIds = remember {
        mutableStateListOf<String>().apply {
            addAll(prefs.getStringSet("bookmarked_moment_ids", emptySet()).orEmpty())
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: trending, 1: my_language
    val trendingFeed = remember { MomentTabFeedState("trending") }
    val myLangFeed = remember { MomentTabFeedState("my_language") }
    val activeFeed = if (selectedTab == 0) trendingFeed else myLangFeed

    var showCommentsSheet by remember { mutableStateOf(false) }
    var activeMomentIdForComments by remember { mutableStateOf<String?>(null) }
    var showCreatePostSheet by remember { mutableStateOf(false) }
    var reportingMoment by remember { mutableStateOf<MomentDto?>(null) }
    var activeVideoUrl by remember { mutableStateOf<String?>(null) }

    fun removeMomentEverywhere(momentId: String) {
        trendingFeed.items.removeAll { it.id == momentId }
        myLangFeed.items.removeAll { it.id == momentId }
        if (activeMomentIdForComments == momentId) {
            showCommentsSheet = false
            activeMomentIdForComments = null
        }
    }

    fun updateMomentEverywhere(momentId: String, transform: (MomentDto) -> MomentDto) {
        val tIdx = trendingFeed.items.indexOfFirst { it.id == momentId }
        if (tIdx != -1) trendingFeed.items[tIdx] = transform(trendingFeed.items[tIdx])
        val mIdx = myLangFeed.items.indexOfFirst { it.id == momentId }
        if (mIdx != -1) myLangFeed.items[mIdx] = transform(myLangFeed.items[mIdx])
    }

    fun updateAuthorFollowEverywhere(authorId: String, following: Boolean) {
        for (i in trendingFeed.items.indices) {
            val m = trendingFeed.items[i]
            if (m.author.id == authorId) {
                trendingFeed.items[i] = m.copy(author = m.author.copy(following = following))
            }
        }
        for (i in myLangFeed.items.indices) {
            val m = myLangFeed.items[i]
            if (m.author.id == authorId) {
                myLangFeed.items[i] = m.copy(author = m.author.copy(following = following))
            }
        }
    }

    suspend fun loadFeedPage(feed: MomentTabFeedState, reset: Boolean) {
        if (reset) {
            if (feed.items.isEmpty()) feed.isInitialLoading = true else feed.isRefreshing = true
            feed.errorMessage = null
        } else {
            if (!feed.hasMore || feed.isLoadingMore || feed.isInitialLoading) return
            feed.isLoadingMore = true
        }

        val offset = if (reset) 0 else feed.nextOffset
        val result = MomentsRepository.getMomentsFeed(
            context = context,
            tab = feed.tabKey,
            limit = 20,
            offset = offset
        )

        result.onSuccess { page ->
            if (reset) {
                feed.items.clear()
                feed.items.addAll(page)
                feed.nextOffset = page.size
            } else {
                val existingIds = feed.items.map { it.id }.toHashSet()
                val unique = page.filter { it.id !in existingIds }
                feed.items.addAll(unique)
                feed.nextOffset = offset + page.size
            }
            feed.hasMore = page.size >= 20
            feed.hasLoadedOnce = true
            feed.errorMessage = null
        }.onFailure { err ->
            val msg = err.message ?: "Something went wrong. Try again"
            feed.errorMessage = msg
            if (feed.items.isNotEmpty()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

        feed.isInitialLoading = false
        feed.isRefreshing = false
        feed.isLoadingMore = false
    }

    LaunchedEffect(selectedTab) {
        val target = if (selectedTab == 0) trendingFeed else myLangFeed
        if (!target.hasLoadedOnce && !target.isInitialLoading) {
            loadFeedPage(target, reset = true)
        }
    }

    val verticalPagerState = rememberPagerState(pageCount = { activeFeed.items.size })

    // Trigger next page load cleanly near end of pager
    LaunchedEffect(verticalPagerState.currentPage, activeFeed.items.size, selectedTab) {
        val count = activeFeed.items.size
        if (count > 0 && verticalPagerState.currentPage >= count - 3 && activeFeed.hasMore) {
            loadFeedPage(activeFeed, reset = false)
        }
    }

    val currentDisplayedMoment = activeFeed.items.getOrNull(verticalPagerState.currentPage)
    val pullToRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        PullToRefreshBox(
            isRefreshing = activeFeed.isRefreshing,
            onRefresh = {
                coroutineScope.launch { loadFeedPage(activeFeed, reset = true) }
            },
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = activeFeed.isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp),
                    containerColor = Color(0xFF1E293B),
                    color = Color(0xFFFFB800)
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                activeFeed.isInitialLoading && activeFeed.items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFFB800))
                    }
                }

                activeFeed.items.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = activeFeed.errorMessage
                                    ?: if (selectedTab == 1) "No moments in your language yet" else "No moments yet",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pull down to refresh or share your own language moment.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch { loadFeedPage(activeFeed, reset = true) }
                                    },
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                                ) {
                                    Text("Refresh", color = Color.White)
                                }
                                Button(
                                    onClick = { showCreatePostSheet = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFB800),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("Create Moment", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                else -> {
                    VerticalPager(
                        state = verticalPagerState,
                        key = { idx -> activeFeed.items.getOrNull(idx)?.id ?: idx },
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val moment = activeFeed.items.getOrNull(pageIndex) ?: return@VerticalPager
                        val isBookmarked = bookmarkedIds.contains(moment.id)

                        MomentPageItem(
                            moment = moment,
                            isBookmarked = isBookmarked,
                            onOpenComments = {
                                activeMomentIdForComments = moment.id
                                showCommentsSheet = true
                            },
                            onPlayVideo = { url -> activeVideoUrl = url },
                            onToggleFollow = {
                                if (moment.author.is_me) return@MomentPageItem
                                val prevFollowing = moment.author.following
                                val optimisticNext = !prevFollowing
                                updateAuthorFollowEverywhere(moment.author.id, optimisticNext)
                                coroutineScope.launch {
                                    MomentsRepository.toggleFollowUser(
                                        context = context,
                                        targetUserId = moment.author.id,
                                        currentlyFollowing = prevFollowing
                                    ).onSuccess { nowFollowing ->
                                        updateAuthorFollowEverywhere(moment.author.id, nowFollowing)
                                        val displayName = moment.author.name ?: moment.author.username ?: "User"
                                        Toast.makeText(
                                            context,
                                            if (nowFollowing) "Following $displayName" else "Unfollowed $displayName",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.onFailure { err ->
                                        updateAuthorFollowEverywhere(moment.author.id, prevFollowing)
                                        Toast.makeText(
                                            context,
                                            err.message ?: "Something went wrong. Try again",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onToggleLike = {
                                val prevLiked = moment.liked_by_me
                                val prevCount = moment.likes_count
                                val nextLiked = !prevLiked
                                val nextCount = (prevCount + if (nextLiked) 1 else -1).coerceAtLeast(0)
                                updateMomentEverywhere(moment.id) {
                                    it.copy(liked_by_me = nextLiked, likes_count = nextCount)
                                }
                                coroutineScope.launch {
                                    MomentsRepository.toggleMomentLike(context, moment.id)
                                        .onSuccess { resp ->
                                            updateMomentEverywhere(moment.id) {
                                                it.copy(liked_by_me = resp.liked, likes_count = resp.likes_count)
                                            }
                                        }
                                        .onFailure { err ->
                                            if ((err as? MomentRpcException)?.isNotFound == true) {
                                                Toast.makeText(context, err.message, Toast.LENGTH_SHORT).show()
                                                removeMomentEverywhere(moment.id)
                                            } else {
                                                updateMomentEverywhere(moment.id) {
                                                    it.copy(liked_by_me = prevLiked, likes_count = prevCount)
                                                }
                                                Toast.makeText(
                                                    context,
                                                    err.message ?: "Something went wrong. Try again",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                }
                            },
                            onToggleBookmark = {
                                if (isBookmarked) bookmarkedIds.remove(moment.id)
                                else bookmarkedIds.add(moment.id)
                                prefs.edit()
                                    .putStringSet("bookmarked_moment_ids", bookmarkedIds.toSet())
                                    .apply()
                                Toast.makeText(
                                    context,
                                    if (!isBookmarked) "Saved to Bookmarks" else "Removed from Bookmarks",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onShare = {
                                val shareText = buildString {
                                    if (!moment.title.isNullOrBlank()) append(moment.title).append("\n")
                                    if (!moment.content.isNullOrBlank()) append(moment.content)
                                }.ifBlank { "Check out this moment on FunkyTalk!" }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share moment"))
                            }
                        )
                    }
                }
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
                                text = { Text("Refresh") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    coroutineScope.launch { loadFeedPage(activeFeed, reset = true) }
                                }
                            )
                            if (currentDisplayedMoment != null) {
                                if (!currentDisplayedMoment.author.is_me) {
                                    DropdownMenuItem(
                                        text = { Text("Not interested") },
                                        leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                                        onClick = {
                                            showTopMenu = false
                                            val targetId = currentDisplayedMoment.id
                                            removeMomentEverywhere(targetId)
                                            coroutineScope.launch {
                                                MomentsRepository.markMomentNotInterested(context, targetId)
                                                    .onFailure { err ->
                                                        if ((err as? MomentRpcException)?.isNotFound != true) {
                                                            Toast.makeText(
                                                                context,
                                                                err.message ?: "Something went wrong. Try again",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Report post", color = Color(0xFFEF4444)) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Flag,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444)
                                            )
                                        },
                                        onClick = {
                                            showTopMenu = false
                                            reportingMoment = currentDisplayedMoment
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Delete post", color = Color(0xFFEF4444)) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444)
                                            )
                                        },
                                        onClick = {
                                            showTopMenu = false
                                            val targetId = currentDisplayedMoment.id
                                            coroutineScope.launch {
                                                MomentsRepository.deleteMoment(context, targetId)
                                                    .onSuccess {
                                                        removeMomentEverywhere(targetId)
                                                        Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .onFailure { err ->
                                                        if ((err as? MomentRpcException)?.isNotFound == true) {
                                                            removeMomentEverywhere(targetId)
                                                        }
                                                        Toast.makeText(
                                                            context,
                                                            err.message ?: "Something went wrong. Try again",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            }
                                        }
                                    )
                                }
                            }
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

    // Comments & Corrections Modal Bottom Sheet
    val currentMomentForSheet = remember(activeMomentIdForComments, activeFeed.items.toList()) {
        activeMomentIdForComments?.let { id ->
            activeFeed.items.firstOrNull { it.id == id }
                ?: trendingFeed.items.firstOrNull { it.id == id }
                ?: myLangFeed.items.firstOrNull { it.id == id }
        }
    }

    if (showCommentsSheet && currentMomentForSheet != null) {
        MomentCommentsBottomSheet(
            moment = currentMomentForSheet,
            currentUserNativeLanguage = currentUserNativeLanguage,
            onDismiss = {
                showCommentsSheet = false
                activeMomentIdForComments = null
            },
            onMomentRemoved = {
                removeMomentEverywhere(currentMomentForSheet.id)
            },
            onCountsChanged = { newCommentsCount, newCorrectionsCount ->
                updateMomentEverywhere(currentMomentForSheet.id) {
                    it.copy(
                        comments_count = newCommentsCount.coerceAtLeast(0),
                        corrections_count = newCorrectionsCount.coerceAtLeast(0)
                    )
                }
            }
        )
    }

    // Create Moment Sheet
    if (showCreatePostSheet) {
        CreatePostScreen(
            defaultLanguage = currentUserNativeLanguage,
            onDismiss = { showCreatePostSheet = false },
            onPostCreated = { newMoment ->
                trendingFeed.items.removeAll { it.id == newMoment.id }
                trendingFeed.items.add(0, newMoment)
                if (selectedTab == 1) {
                    myLangFeed.items.removeAll { it.id == newMoment.id }
                    myLangFeed.items.add(0, newMoment)
                }
                showCreatePostSheet = false
                coroutineScope.launch {
                    verticalPagerState.scrollToPage(0)
                }
            }
        )
    }

    // Video Player Dialog
    activeVideoUrl?.let { videoUrl ->
        MomentVideoPlayerDialog(
            videoUrl = videoUrl,
            onDismiss = { activeVideoUrl = null }
        )
    }

    // Report Dialog (fixed: reportedUserId = moment.author.id, contextId = moment.id)
    reportingMoment?.let { moment ->
        ReportUserDialog(
            reportedUserId = moment.author.id,
            reportedUserName = moment.author.name ?: moment.author.username ?: "User",
            contextType = "moment",
            contextId = moment.id,
            onDismiss = { reportingMoment = null }
        )
    }
}

@Composable
private fun MomentPageItem(
    moment: MomentDto,
    isBookmarked: Boolean,
    onOpenComments: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onToggleFollow: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val mediaList = moment.media
    val subPagerState = rememberPagerState(pageCount = { mediaList.size.coerceAtLeast(1) })
    var showTranslation by remember(moment.id) { mutableStateOf(false) }

    val preset = remember(moment.bg_key) { MomentsRepository.resolveBgPreset(moment.bg_key) }
    val isLightCanvas = mediaList.isEmpty() && preset.isDarkText

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Background Content: Multi-photo/video pager or Color Gradient
        if (mediaList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(preset.colors)),
                contentAlignment = Alignment.Center
            ) {
                val centerText = moment.title?.takeIf { it.isNotBlank() }
                    ?: moment.content?.takeIf { it.isNotBlank() }
                    ?: ""
                Text(
                    text = centerText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isLightCanvas) Color(0xFF1B1608) else Color.White,
                    modifier = Modifier.padding(32.dp),
                    lineHeight = 36.sp
                )
            }
        } else {
            HorizontalPager(
                state = subPagerState,
                modifier = Modifier.fillMaxSize()
            ) { subIndex ->
                val media: MomentMediaDto = mediaList[subIndex]
                val previewModel = if (media.type == "video") {
                    media.thumb_url?.takeIf { it.isNotBlank() } ?: media.url
                } else {
                    media.url
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(previewModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = moment.title ?: "Moment media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (media.type == "video") {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                .clickable { onPlayVideo(media.url) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        media.duration_sec?.let { dur ->
                            val mins = dur / 60
                            val secs = dur % 60
                            val formattedDur = String.format(java.util.Locale.US, "%d:%02d", mins, secs)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.Black.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .statusBarsPadding()
                                    .padding(top = 80.dp, start = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = formattedDur,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
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
        if (mediaList.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            text = "Photo ${subPagerState.currentPage + 1} / ${mediaList.size}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(mediaList.size) { dotIndex ->
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
            ActionIconButton(
                icon = if (moment.liked_by_me) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                tint = if (moment.liked_by_me) Color(0xFFFF3366) else Color.White,
                countText = formatCount(moment.likes_count),
                onClick = onToggleLike
            )

            ActionIconButton(
                icon = Icons.Default.ChatBubbleOutline,
                tint = Color.White,
                countText = formatCount(moment.comments_count),
                onClick = onOpenComments
            )

            ActionIconButton(
                icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                tint = if (isBookmarked) Color(0xFFFFB800) else Color.White,
                countText = "",
                onClick = onToggleBookmark
            )

            ActionIconButton(
                icon = Icons.Default.Share,
                tint = Color.White,
                countText = "",
                onClick = onShare
            )
        }

        // Bottom Left Content Overlay: Title, Content, Translation, User info, Badges, Tags
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.82f)
                .padding(start = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!moment.title.isNullOrBlank()) {
                Text(
                    text = moment.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 26.sp
                )
            }

            if (!moment.content.isNullOrBlank()) {
                Text(
                    text = moment.content,
                    fontSize = 13.sp,
                    color = Color(0xFFF1F5F9),
                    lineHeight = 18.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!moment.translation.isNullOrBlank()) {
                Surface(
                    onClick = { showTranslation = !showTranslation },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (showTranslation) "Translation: ${moment.translation}" else "🌐 View author translation",
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            val authorName = moment.author.name?.takeIf { it.isNotBlank() }
                ?: moment.author.username?.takeIf { it.isNotBlank() }
                ?: "Learner"
            val displayLocation = moment.location?.takeIf { it.isNotBlank() }
                ?: moment.author.city?.takeIf { it.isNotBlank() }
                ?: ""
            val timeAgo = formatMomentTimestamp(moment.created_at)
            val subLocationLine = listOfNotNull(
                displayLocation.takeIf { it.isNotBlank() },
                timeAgo.takeIf { it.isNotBlank() }
            ).joinToString(" · ")

            // User Profile Row: Avatar, Name, Verified badge, Flag & Location/Time, Follow button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val avatarModel = resolveAvatarModel(moment.author.avatar_url)
                if (avatarModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = authorName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB800))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = authorName.firstOrNull()?.uppercase() ?: "U",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = authorName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (moment.author.is_verified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val flagCode = moment.author.country_code?.takeIf { it.isNotBlank() }
                            ?: LanguageData.getFlagCodeForLanguage(moment.language)
                        CircleFlag(countryCode = flagCode, modifier = Modifier.size(12.dp))
                        Text(
                            text = subLocationLine,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!moment.author.is_me) {
                    OutlinedButton(
                        onClick = onToggleFollow,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, Color(0xFFFFB800)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (moment.author.following) Color(0xFFFFB800) else Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = if (moment.author.following) "Following" else "Follow",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (moment.author.following) Color.Black else Color(0xFFFFB800)
                        )
                    }
                }
            }

            // Language Badge & Topics row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isNative = moment.author.is_native
                    val nativeLang = moment.author.native_languages.firstOrNull()
                    val langBadgeText = when {
                        isNative -> "Native"
                        !nativeLang.isNullOrBlank() -> "$nativeLang → ${moment.language}"
                        else -> moment.language
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFB800).copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.7f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            CircleFlag(
                                countryCode = LanguageData.getFlagCodeForLanguage(moment.language),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = langBadgeText,
                                color = Color(0xFFFFE082),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                items(moment.topics) { slug ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = MomentsRepository.resolveTopicChipText(slug),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MomentCommentsBottomSheet(
    moment: MomentDto,
    currentUserNativeLanguage: String,
    onDismiss: () -> Unit,
    onMomentRemoved: () -> Unit,
    onCountsChanged: (commentsCount: Int, correctionsCount: Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val limits = remember { MomentsRepository.getCachedOptions().limits }

    // Sub-tab inside sheet: 0 = "Comments" (all), 1 = "Corrections" (corrections)
    var selectedSheetTab by remember { mutableIntStateOf(0) }
    val kindParam = if (selectedSheetTab == 0) "all" else "corrections"

    val commentsList = remember { mutableStateListOf<MomentCommentDto>() }
    var isLoadingComments by remember { mutableStateOf(false) }
    var isLoadingMoreComments by remember { mutableStateOf(false) }
    var hasMoreComments by remember { mutableStateOf(true) }
    var commentsOffset by remember { mutableIntStateOf(0) }
    var commentsError by mutableStateOf<String?>(null)

    val expandedRepliesMap = remember { mutableStateMapOf<String, List<MomentCommentDto>>() }
    val loadingRepliesIds = remember { mutableStateMapOf<String, Boolean>() }
    val translatedMap = remember { mutableStateMapOf<String, String>() }

    var replyingTo by remember { mutableStateOf<MomentCommentDto?>(null) }
    var newCommentText by remember { mutableStateOf("") }
    var isNativeFixMode by remember { mutableStateOf(false) }
    var fixOriginalInput by remember { mutableStateOf("") }
    var fixCorrectedInput by remember { mutableStateOf("") }
    var fixTipInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Available verbatim post segments for selecting original text in corrections
    val postSegments = remember(moment.id, moment.title, moment.content, moment.translation) {
        listOfNotNull(
            moment.title?.takeIf { it.isNotBlank() }?.let { "Title" to it },
            moment.content?.takeIf { it.isNotBlank() }?.let { "Post" to it },
            moment.translation?.takeIf { it.isNotBlank() }?.let { "Translation" to it }
        )
    }
    var selectedSegmentIndex by remember { mutableIntStateOf(0) }
    var startWordIndex by remember { mutableStateOf<Int?>(null) }

    suspend fun loadComments(reset: Boolean) {
        if (reset) {
            isLoadingComments = true
            commentsOffset = 0
            commentsError = null
        } else {
            if (!hasMoreComments || isLoadingMoreComments) return
            isLoadingMoreComments = true
        }
        val offset = if (reset) 0 else commentsOffset
        MomentsRepository.getMomentComments(
            context = context,
            momentId = moment.id,
            kind = kindParam,
            limit = 30,
            offset = offset
        ).onSuccess { list ->
            if (reset) {
                commentsList.clear()
                commentsList.addAll(list)
                commentsOffset = list.size
            } else {
                val existing = commentsList.map { it.id }.toHashSet()
                commentsList.addAll(list.filter { it.id !in existing })
                commentsOffset = offset + list.size
            }
            hasMoreComments = list.size >= 30
        }.onFailure { err ->
            if ((err as? MomentRpcException)?.isNotFound == true) {
                Toast.makeText(context, err.message, Toast.LENGTH_SHORT).show()
                onMomentRemoved()
            } else {
                commentsError = err.message ?: "Something went wrong. Try again"
            }
        }
        isLoadingComments = false
        isLoadingMoreComments = false
    }

    LaunchedEffect(moment.id, selectedSheetTab) {
        loadComments(reset = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF1E293B),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Comments (${moment.comments_count}) · Corrections (${moment.corrections_count})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Tabs: Comments vs Corrections
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedSheetTab == 0,
                    onClick = { selectedSheetTab = 0 },
                    label = { Text("Comments (${moment.comments_count})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFB800),
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = selectedSheetTab == 1,
                    onClick = { selectedSheetTab = 1 },
                    label = { Text("Corrections (${moment.corrections_count})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981),
                        selectedLabelColor = Color.White
                    )
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Comments List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoadingComments && commentsList.isEmpty() -> {
                        CircularProgressIndicator(
                            color = Color(0xFFFFB800),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    commentsList.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = commentsError ?: if (selectedSheetTab == 1) {
                                    "No corrections yet. Help the author sound natural!"
                                } else {
                                    "No comments yet. Start the conversation!"
                                },
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(commentsList, key = { it.id }) { comment ->
                                val replies = expandedRepliesMap[comment.id].orEmpty()
                                val isLoadingReplies = loadingRepliesIds[comment.id] == true

                                CommentItemRow(
                                    comment = comment,
                                    isPostAuthor = moment.author.is_me,
                                    translatedText = translatedMap[comment.id],
                                    replies = replies,
                                    isLoadingReplies = isLoadingReplies,
                                    onToggleLike = { targetComment ->
                                        val isReply = targetComment.parent_id != null
                                        val prevLiked = targetComment.liked_by_me
                                        val prevCount = targetComment.likes_count
                                        val nextLiked = !prevLiked
                                        val nextCount = (prevCount + if (nextLiked) 1 else -1).coerceAtLeast(0)

                                        fun applyCommentLike(liked: Boolean, count: Int) {
                                            if (isReply) {
                                                val pId = targetComment.parent_id!!
                                                val list = expandedRepliesMap[pId].orEmpty().map {
                                                    if (it.id == targetComment.id) it.copy(liked_by_me = liked, likes_count = count)
                                                    else it
                                                }
                                                expandedRepliesMap[pId] = list
                                            } else {
                                                val idx = commentsList.indexOfFirst { it.id == targetComment.id }
                                                if (idx != -1) {
                                                    commentsList[idx] = commentsList[idx].copy(liked_by_me = liked, likes_count = count)
                                                }
                                            }
                                        }

                                        applyCommentLike(nextLiked, nextCount)
                                        coroutineScope.launch {
                                            MomentsRepository.toggleCommentLike(context, targetComment.id)
                                                .onSuccess { res -> applyCommentLike(res.liked, res.likes_count) }
                                                .onFailure { err ->
                                                    if ((err as? MomentRpcException)?.isNotFound == true) {
                                                        Toast.makeText(context, err.message, Toast.LENGTH_SHORT).show()
                                                        onMomentRemoved()
                                                    } else {
                                                        applyCommentLike(prevLiked, prevCount)
                                                        Toast.makeText(
                                                            context,
                                                            err.message ?: "Something went wrong. Try again",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                        }
                                    },
                                    onAcceptCorrection = { targetComment ->
                                        coroutineScope.launch {
                                            MomentsRepository.acceptCorrection(context, targetComment.id)
                                                .onSuccess { res ->
                                                    val idx = commentsList.indexOfFirst { it.id == targetComment.id }
                                                    if (idx != -1) {
                                                        commentsList[idx] = commentsList[idx].copy(is_accepted = res.is_accepted)
                                                    }
                                                }
                                                .onFailure { err ->
                                                    Toast.makeText(
                                                        context,
                                                        err.message ?: "Something went wrong. Try again",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                    },
                                    onLoadReplies = { rootComment ->
                                        if (expandedRepliesMap.containsKey(rootComment.id)) {
                                            expandedRepliesMap.remove(rootComment.id)
                                        } else {
                                            loadingRepliesIds[rootComment.id] = true
                                            coroutineScope.launch {
                                                MomentsRepository.getCommentReplies(
                                                    context = context,
                                                    commentId = rootComment.id,
                                                    limit = 30,
                                                    offset = 0
                                                ).onSuccess { loaded ->
                                                    expandedRepliesMap[rootComment.id] = loaded
                                                }.onFailure { err ->
                                                    Toast.makeText(
                                                        context,
                                                        err.message ?: "Something went wrong. Try again",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                loadingRepliesIds[rootComment.id] = false
                                            }
                                        }
                                    },
                                    onReply = { target ->
                                        replyingTo = target
                                        isNativeFixMode = false
                                    },
                                    onDelete = { target ->
                                        coroutineScope.launch {
                                            MomentsRepository.deleteMomentComment(context, target.id)
                                                .onSuccess {
                                                    if (target.parent_id == null) {
                                                        val removedRepliesCount = target.replies_count
                                                        commentsList.removeAll { it.id == target.id }
                                                        expandedRepliesMap.remove(target.id)
                                                        val newCommentsCount = (moment.comments_count - 1 - removedRepliesCount).coerceAtLeast(0)
                                                        val newCorrectionsCount = if (target.kind == "correction") {
                                                            (moment.corrections_count - 1).coerceAtLeast(0)
                                                        } else {
                                                            moment.corrections_count
                                                        }
                                                        onCountsChanged(newCommentsCount, newCorrectionsCount)
                                                    } else {
                                                        val rootId = target.parent_id
                                                        expandedRepliesMap[rootId] = expandedRepliesMap[rootId].orEmpty()
                                                            .filterNot { it.id == target.id }
                                                        val rootIdx = commentsList.indexOfFirst { it.id == rootId }
                                                        if (rootIdx != -1) {
                                                            val cur = commentsList[rootIdx]
                                                            commentsList[rootIdx] = cur.copy(
                                                                replies_count = (cur.replies_count - 1).coerceAtLeast(0)
                                                            )
                                                        }
                                                        onCountsChanged(
                                                            (moment.comments_count - 1).coerceAtLeast(0),
                                                            moment.corrections_count
                                                        )
                                                    }
                                                }
                                                .onFailure { err ->
                                                    Toast.makeText(
                                                        context,
                                                        err.message ?: "Something went wrong. Try again",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                    },
                                    onTranslate = { target ->
                                        val sourceText = listOfNotNull(
                                            target.corrected_text?.takeIf { it.isNotBlank() },
                                            target.content?.takeIf { it.isNotBlank() }
                                        ).joinToString(" — ")
                                        if (sourceText.isBlank()) return@CommentItemRow
                                        val targetLang = currentUserNativeLanguage.split(",")
                                            .firstOrNull()?.trim()?.ifBlank { "English" } ?: "English"
                                        coroutineScope.launch {
                                            if (CloudAiManager.isCloudAiEnabled.value) {
                                                val res = CloudAiManager.translateMessage(
                                                    context = context,
                                                    messageId = "moment_c_${target.id}",
                                                    text = sourceText,
                                                    targetLanguage = targetLang
                                                )
                                                res.onSuccess { ai ->
                                                    translatedMap[target.id] = ai.text
                                                }.onFailure {
                                                    translatedMap[target.id] = sourceText
                                                }
                                            } else {
                                                translatedMap[target.id] = "Original ($targetLang preview unavailable without Cloud AI): $sourceText"
                                            }
                                        }
                                    }
                                )
                            }

                            if (hasMoreComments && commentsList.isNotEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TextButton(
                                            onClick = { coroutineScope.launch { loadComments(reset = false) } },
                                            enabled = !isLoadingMoreComments
                                        ) {
                                            Text(
                                                text = if (isLoadingMoreComments) "Loading..." else "Load more comments",
                                                color = Color(0xFFFFB800)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Add Comment / Native Fix Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hide Native Fix chip on user's own posts
                    if (!moment.author.is_me && postSegments.isNotEmpty()) {
                        FilterChip(
                            selected = isNativeFixMode,
                            onClick = {
                                isNativeFixMode = !isNativeFixMode
                                if (isNativeFixMode) {
                                    replyingTo = null
                                    if (fixOriginalInput.isBlank()) {
                                        val seg = postSegments.getOrNull(selectedSegmentIndex)?.second ?: ""
                                        fixOriginalInput = seg.take(limits.correction_text)
                                        fixCorrectedInput = fixOriginalInput
                                    }
                                }
                            },
                            label = { Text("Native fix ✏️", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White
                            )
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (isNativeFixMode) {
                        Text(
                            text = "Tap words/phrase from post to fix",
                            fontSize = 11.sp,
                            color = Color(0xFF6EE7B7)
                        )
                    } else if (replyingTo != null) {
                        val replyName = replyingTo!!.author.name ?: replyingTo!!.author.username ?: "user"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Replying to $replyName",
                                fontSize = 11.sp,
                                color = Color(0xFFFFB800)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel reply",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { replyingTo = null }
                            )
                        }
                    }
                }

                if (isNativeFixMode) {
                    // Segment switcher if multiple parts exist (Title / Post / Translation)
                    if (postSegments.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(postSegments.size) { idx ->
                                val (segLabel, segText) = postSegments[idx]
                                FilterChip(
                                    selected = selectedSegmentIndex == idx,
                                    onClick = {
                                        selectedSegmentIndex = idx
                                        startWordIndex = null
                                        fixOriginalInput = segText.take(limits.correction_text)
                                        if (fixCorrectedInput.isBlank() || fixCorrectedInput == postSegments.getOrNull(selectedSegmentIndex)?.second) {
                                            fixCorrectedInput = fixOriginalInput
                                        }
                                    },
                                    label = { Text(segLabel, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    val activeSegmentText = postSegments.getOrNull(selectedSegmentIndex)?.second ?: ""
                    val words = remember(activeSegmentText) { tokenizeWordsWithSpans(activeSegmentText) }

                    // Tappable word-range selector guaranteed to match exact substring in the post
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Original text (tap 1 or 2 words for range):",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "Whole line",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB800),
                                    modifier = Modifier.clickable {
                                        startWordIndex = null
                                        fixOriginalInput = activeSegmentText.take(limits.correction_text)
                                        fixCorrectedInput = fixOriginalInput
                                    }
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                words.forEachIndexed { idx, span ->
                                    val isInSelection = fixOriginalInput.isNotEmpty() &&
                                        activeSegmentText.indexOf(fixOriginalInput).let { selStart ->
                                            selStart >= 0 &&
                                                span.startIndex >= selStart &&
                                                span.endIndexExclusive <= selStart + fixOriginalInput.length
                                        }
                                    Surface(
                                        onClick = {
                                            val anchor = startWordIndex
                                            if (anchor == null) {
                                                startWordIndex = idx
                                                val picked = activeSegmentText.substring(span.startIndex, span.endIndexExclusive)
                                                    .take(limits.correction_text)
                                                fixOriginalInput = picked
                                                fixCorrectedInput = picked
                                            } else {
                                                val fromIdx = minOf(anchor, idx)
                                                val toIdx = maxOf(anchor, idx)
                                                val fromChar = words[fromIdx].startIndex
                                                val toChar = words[toIdx].endIndexExclusive
                                                val picked = activeSegmentText.substring(fromChar, toChar)
                                                    .take(limits.correction_text)
                                                fixOriginalInput = picked
                                                fixCorrectedInput = picked
                                                startWordIndex = null
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isInSelection) Color(0xFFEF4444).copy(alpha = 0.35f) else Color(0xFF1E293B),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isInSelection) Color(0xFFF87171) else Color(0xFF475569)
                                        )
                                    ) {
                                        Text(
                                            text = span.word,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (fixOriginalInput.isNotBlank()) {
                                Text(
                                    text = "Selected: \"$fixOriginalInput\"",
                                    fontSize = 12.sp,
                                    color = Color(0xFFF87171),
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = fixCorrectedInput,
                        onValueChange = { if (it.length <= limits.correction_text) fixCorrectedInput = it },
                        label = { Text("Corrected phrase (${fixCorrectedInput.length}/${limits.correction_text})", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = fixTipInput,
                            onValueChange = { if (it.length <= limits.comment) fixTipInput = it },
                            label = { Text("Explanation (optional)", fontSize = 12.sp) },
                            placeholder = { Text("Why this sounds more natural...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            enabled = !isSubmitting && fixOriginalInput.isNotBlank() && fixCorrectedInput.isNotBlank(),
                            onClick = {
                                if (isSubmitting) return@IconButton
                                isSubmitting = true
                                coroutineScope.launch {
                                    MomentsRepository.addMomentCorrection(
                                        context = context,
                                        momentId = moment.id,
                                        original = fixOriginalInput,
                                        corrected = fixCorrectedInput,
                                        explanation = fixTipInput.takeIf { it.isNotBlank() }
                                    ).onSuccess { newFix ->
                                        commentsList.add(0, newFix)
                                        onCountsChanged(
                                            moment.comments_count + 1,
                                            moment.corrections_count + 1
                                        )
                                        fixOriginalInput = ""
                                        fixCorrectedInput = ""
                                        fixTipInput = ""
                                        isNativeFixMode = false
                                        Toast.makeText(context, "Native fix posted!", Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        if ((err as? MomentRpcException)?.isNotFound == true) {
                                            Toast.makeText(context, err.message, Toast.LENGTH_SHORT).show()
                                            onMomentRemoved()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                err.message ?: "Something went wrong. Try again",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    isSubmitting = false
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send correction",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { if (it.length <= limits.comment) newCommentText = it },
                            placeholder = {
                                Text(
                                    if (replyingTo != null) "Write a reply..." else "Add a comment...",
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
                            enabled = !isSubmitting && newCommentText.isNotBlank(),
                            onClick = {
                                if (isSubmitting || newCommentText.isBlank()) return@IconButton
                                isSubmitting = true
                                val replyTarget = replyingTo
                                coroutineScope.launch {
                                    MomentsRepository.addMomentComment(
                                        context = context,
                                        momentId = moment.id,
                                        content = newCommentText,
                                        parentId = replyTarget?.id
                                    ).onSuccess { createdComment ->
                                        val rootParentId = createdComment.parent_id ?: replyTarget?.parent_id ?: replyTarget?.id
                                        if (rootParentId != null) {
                                            val existingReplies = expandedRepliesMap[rootParentId].orEmpty()
                                            expandedRepliesMap[rootParentId] = existingReplies + createdComment
                                            val rootIdx = commentsList.indexOfFirst { it.id == rootParentId }
                                            if (rootIdx != -1) {
                                                val root = commentsList[rootIdx]
                                                commentsList[rootIdx] = root.copy(replies_count = root.replies_count + 1)
                                            }
                                        } else {
                                            if (selectedSheetTab == 0) {
                                                commentsList.add(0, createdComment)
                                            }
                                        }
                                        onCountsChanged(
                                            moment.comments_count + 1,
                                            moment.corrections_count
                                        )
                                        newCommentText = ""
                                        replyingTo = null
                                    }.onFailure { err ->
                                        if ((err as? MomentRpcException)?.isNotFound == true) {
                                            Toast.makeText(context, err.message, Toast.LENGTH_SHORT).show()
                                            onMomentRemoved()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                err.message ?: "Something went wrong. Try again",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    isSubmitting = false
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB800))
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
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
    }
}

@Composable
private fun CommentItemRow(
    comment: MomentCommentDto,
    isPostAuthor: Boolean,
    translatedText: String?,
    replies: List<MomentCommentDto>,
    isLoadingReplies: Boolean,
    onToggleLike: (MomentCommentDto) -> Unit,
    onAcceptCorrection: (MomentCommentDto) -> Unit,
    onLoadReplies: (MomentCommentDto) -> Unit,
    onReply: (MomentCommentDto) -> Unit,
    onDelete: (MomentCommentDto) -> Unit,
    onTranslate: (MomentCommentDto) -> Unit,
    isNestedReply: Boolean = false
) {
    val context = LocalContext.current
    val authorName = comment.author.name?.takeIf { it.isNotBlank() }
        ?: comment.author.username?.takeIf { it.isNotBlank() }
        ?: "User"
    val timeText = formatMomentTimestamp(comment.created_at)
    val isCorrection = comment.kind == "correction"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isNestedReply) 36.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val avatarModel = resolveAvatarModel(comment.author.avatar_url)
        if (avatarModel != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(avatarModel)
                    .crossfade(true)
                    .build(),
                contentDescription = authorName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(if (isNestedReply) 28.dp else 34.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(if (isNestedReply) 28.dp else 34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = authorName.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = authorName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (isCorrection && comment.by_native) {
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
                if (isCorrection && comment.is_accepted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF059669).copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = "✓ Accepted by author",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = timeText,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isCorrection) {
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
                        if (!comment.original_text.isNullOrBlank()) {
                            Text(
                                text = comment.original_text,
                                fontSize = 13.sp,
                                color = Color(0xFFF87171),
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                        if (!comment.corrected_text.isNullOrBlank()) {
                            Text(
                                text = comment.corrected_text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                        }
                        if (!comment.content.isNullOrBlank()) {
                            Text(
                                text = comment.content,
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = comment.content.orEmpty(),
                    fontSize = 13.sp,
                    color = Color.White,
                    lineHeight = 17.sp
                )
            }

            if (!translatedText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = translatedText,
                    fontSize = 12.sp,
                    color = Color(0xFF93C5FD),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action row: Helpful/Like + Reply + Accept (for author) + Translate + Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onToggleLike(comment) }
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Helpful",
                        tint = if (comment.liked_by_me) Color(0xFFFFB800) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (comment.likes_count > 0) "${comment.likes_count}" else "Like",
                        fontSize = 11.sp,
                        color = if (comment.liked_by_me) Color(0xFFFFB800) else Color.White.copy(alpha = 0.5f)
                    )
                }

                Text(
                    text = "Reply",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { onReply(comment) }
                )

                if (isCorrection && isPostAuthor) {
                    Text(
                        text = if (comment.is_accepted) "Accepted ✓" else "Accept fix",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (comment.is_accepted) Color(0xFF34D399) else Color(0xFFFFB800),
                        modifier = Modifier.clickable { onAcceptCorrection(comment) }
                    )
                }

                Text(
                    text = "Translate",
                    fontSize = 11.sp,
                    color = Color(0xFF60A5FA),
                    modifier = Modifier.clickable { onTranslate(comment) }
                )

                if (comment.author.is_me || isPostAuthor) {
                    Text(
                        text = "Delete",
                        fontSize = 11.sp,
                        color = Color(0xFFF87171),
                        modifier = Modifier.clickable { onDelete(comment) }
                    )
                }
            }

            if (!isNestedReply && comment.replies_count > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when {
                        isLoadingReplies -> "Loading replies..."
                        replies.isNotEmpty() -> "Hide replies"
                        else -> "View ${comment.replies_count} ${if (comment.replies_count == 1) "reply" else "replies"}"
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFB800),
                    modifier = Modifier.clickable { onLoadReplies(comment) }
                )
            }

            if (replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    replies.forEach { reply ->
                        CommentItemRow(
                            comment = reply,
                            isPostAuthor = isPostAuthor,
                            translatedText = null,
                            replies = emptyList(),
                            isLoadingReplies = false,
                            onToggleLike = onToggleLike,
                            onAcceptCorrection = onAcceptCorrection,
                            onLoadReplies = onLoadReplies,
                            onReply = onReply,
                            onDelete = onDelete,
                            onTranslate = onTranslate,
                            isNestedReply = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentVideoPlayerDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close video",
                    tint = Color.White
                )
            }
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
