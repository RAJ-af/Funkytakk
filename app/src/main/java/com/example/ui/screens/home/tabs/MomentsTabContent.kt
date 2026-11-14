package com.example.ui.screens.home.tabs

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MomentDto
import com.example.R
import com.example.moderation.ReportUserDialog
import com.example.repository.MomentRpcException
import com.example.repository.MomentsRepository
import com.example.ui.components.MomentCommentsBottomSheet
import com.example.ui.components.MomentV2Slide
import com.example.ui.components.MomentVideoPlayerDialog
import com.example.ui.screens.post.CreatePostScreen
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalFunkyColors
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsTabContent(
    currentUserNativeLanguage: String = "",
    onCreatePostVisibilityChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val funkyColors = LocalFunkyColors.current
    val isAppDark = LocalAppDarkTheme.current
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
    var commentsInitialTab by remember { mutableIntStateOf(0) }
    var commentsInitialFixMode by remember { mutableStateOf(false) }
    var activeMomentIdForComments by remember { mutableStateOf<String?>(null) }
    var showCreatePostSheet by remember { mutableStateOf(false) }
    var reportingMoment by remember { mutableStateOf<MomentDto?>(null) }
    var activeVideoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showCreatePostSheet) {
        onCreatePostVisibilityChange(showCreatePostSheet)
    }
    DisposableEffect(Unit) {
        onDispose { onCreatePostVisibilityChange(false) }
    }

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

    fun performToggleLike(moment: MomentDto) {
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

    LaunchedEffect(verticalPagerState.currentPage, activeFeed.items.size, selectedTab) {
        val count = activeFeed.items.size
        if (count > 0 && verticalPagerState.currentPage >= count - 3 && activeFeed.hasMore) {
            loadFeedPage(activeFeed, reset = false)
        }
    }

    val currentDisplayedMoment = activeFeed.items.getOrNull(verticalPagerState.currentPage)
    val pullToRefreshState = rememberPullToRefreshState()

    val isCurrentSlideLightCanvas = currentDisplayedMoment != null &&
        currentDisplayedMoment.media.isEmpty() &&
        MomentsRepository.resolveBgPreset(currentDisplayedMoment.bg_key).isDarkText
    val useLightHeaderContrast = !isAppDark && (activeFeed.items.isEmpty() || isCurrentSlideLightCanvas)
    val headerInk = if (useLightHeaderContrast) funkyColors.ink else Color.White
    val headerMutedInk = if (useLightHeaderContrast) funkyColors.mute else Color.White.copy(alpha = 0.72f)
    val headerBtnBg = if (useLightHeaderContrast) funkyColors.surface else Color.White.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(funkyColors.bg)
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
                    containerColor = funkyColors.surface,
                    color = funkyColors.mango
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
                        CircularProgressIndicator(color = funkyColors.mango)
                    }
                }

                activeFeed.items.isEmpty() -> {
                    val isErrorState = !activeFeed.errorMessage.isNullOrBlank()
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
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(funkyColors.soft)
                                    .border(1.dp, funkyColors.line, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_sparkle),
                                    contentDescription = null,
                                    tint = funkyColors.mango,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = when {
                                    isErrorState -> activeFeed.errorMessage!!
                                    selectedTab == 1 -> "No posts in your languages yet"
                                    else -> "No moments yet. Be the first to post!"
                                },
                                color = funkyColors.ink,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isErrorState) {
                                    "Could not load the feed right now."
                                } else {
                                    "Pull down to refresh or share your own language moment."
                                },
                                color = funkyColors.mute,
                                fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch { loadFeedPage(activeFeed, reset = true) }
                                    },
                                    border = BorderStroke(1.dp, funkyColors.line)
                                ) {
                                    Text(if (isErrorState) "Retry" else "Refresh", color = funkyColors.ink)
                                }
                                Button(
                                    onClick = { showCreatePostSheet = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = funkyColors.mango,
                                        contentColor = funkyColors.onMango
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

                        MomentV2Slide(
                            moment = moment,
                            isBookmarked = isBookmarked,
                            onOpenComments = {
                                activeMomentIdForComments = moment.id
                                commentsInitialTab = 0
                                commentsInitialFixMode = false
                                showCommentsSheet = true
                            },
                            onOpenCorrections = {
                                activeMomentIdForComments = moment.id
                                commentsInitialTab = 1
                                commentsInitialFixMode = !moment.author.is_me
                                showCommentsSheet = true
                            },
                            onPlayVideo = { url -> activeVideoUrl = url },
                            onToggleFollow = {
                                if (moment.author.is_me) return@MomentV2Slide
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
                            onToggleLike = { performToggleLike(moment) },
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
                    text = "Moments",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = headerInk
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { showCreatePostSheet = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(headerBtnBg)
                            .then(
                                if (useLightHeaderContrast) Modifier.border(1.dp, funkyColors.line, CircleShape)
                                else Modifier
                            )
                            .testTag("moments_create_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_plus),
                            contentDescription = "Create Moment",
                            tint = headerInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    var showTopMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(headerBtnBg)
                                .then(
                                    if (useLightHeaderContrast) Modifier.border(1.dp, funkyColors.line, CircleShape)
                                    else Modifier
                                )
                                .testTag("moments_more_menu_button")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more_vert),
                                contentDescription = "Menu",
                                tint = headerInk,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false },
                            containerColor = funkyColors.surface,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, funkyColors.line)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Create Moment",
                                        color = funkyColors.ink,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(funkyColors.mango.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_plus),
                                            contentDescription = null,
                                            tint = funkyColors.mango,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    showTopMenu = false
                                    showCreatePostSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Refresh feed",
                                        color = funkyColors.ink,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(funkyColors.soft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_refresh),
                                            contentDescription = null,
                                            tint = funkyColors.ink,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    showTopMenu = false
                                    coroutineScope.launch { loadFeedPage(activeFeed, reset = true) }
                                }
                            )
                            if (currentDisplayedMoment != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = funkyColors.line.copy(alpha = 0.6f)
                                )
                                if (!currentDisplayedMoment.author.is_me) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Not interested",
                                                color = funkyColors.ink,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(funkyColors.soft),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_visibility_off),
                                                    contentDescription = null,
                                                    tint = funkyColors.ink,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
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
                                        text = {
                                            Text(
                                                "Report post",
                                                color = funkyColors.danger,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(funkyColors.danger.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_flag),
                                                    contentDescription = null,
                                                    tint = funkyColors.danger,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            showTopMenu = false
                                            reportingMoment = currentDisplayedMoment
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Delete post",
                                                color = funkyColors.danger,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(funkyColors.danger.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_trash),
                                                    contentDescription = null,
                                                    tint = funkyColors.danger,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_flame),
                            contentDescription = null,
                            tint = if (selectedTab == 0) funkyColors.mango else headerMutedInk,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Trending",
                            fontSize = 15.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) headerInk else headerMutedInk
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (selectedTab == 0) {
                        Box(
                            modifier = Modifier
                                .width(76.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(funkyColors.mango)
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
                        color = if (selectedTab == 1) headerInk else headerMutedInk
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (selectedTab == 1) {
                        Box(
                            modifier = Modifier
                                .width(86.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(funkyColors.mango)
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
            initialTab = commentsInitialTab,
            initialNativeFixMode = commentsInitialFixMode,
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

    // Report Dialog
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
