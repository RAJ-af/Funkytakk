package com.example.ui.screens.post

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn as AndroidOptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.BuildConfig
import com.example.MomentCommentDto
import com.example.MomentDto
import com.example.MomentMediaDto
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.moderation.ReportUserDialog
import com.example.repository.MomentRpcException
import com.example.repository.MomentsRepository
import com.example.ui.screens.chat.components.ChatImagePreviewDialog
import com.example.ui.screens.formatMomentTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// -----------------------------------------------------------------------------
// POST & COMMENT DATA MODELS
// -----------------------------------------------------------------------------

data class PostComment(
    val id: String,
    val authorName: String,
    val username: String,
    val avatarUrl: String,
    val timeAgo: String,
    val text: String,
    var likesCount: Int = 0,
    var isLiked: Boolean = false,
    val isMe: Boolean = false,
    val kind: String = "comment",
    val originalText: String? = null,
    val correctedText: String? = null,
    val byNative: Boolean = false,
    val isAccepted: Boolean = false,
    val repliesCount: Int = 0
)

data class DetailedPost(
    val id: String,
    val authorId: String = "",
    val authorName: String,
    val authorUsername: String,
    val authorAvatarUrl: String,
    val isAuthorVerified: Boolean = false,
    val location: String = "",
    val timeAgo: String = "Recently",
    val images: List<String>,
    val caption: String,
    var likesCount: Int = 0,
    var isLiked: Boolean = false,
    var isBookmarked: Boolean = false,
    val initialComments: List<PostComment> = emptyList(),
    val title: String? = null,
    val bgKey: String? = null,
    val status: String = "active",
    val isMe: Boolean = false,
    val translation: String? = null,
    val language: String = "",
    val topics: List<String> = emptyList(),
    val media: List<MomentMediaDto> = emptyList(),
    val commentsCount: Int = 0,
    val correctionsCount: Int = 0
)

private fun MomentCommentDto.toPostComment(): PostComment = PostComment(
    id = id,
    authorName = author.name?.takeIf { it.isNotBlank() } ?: author.username?.takeIf { it.isNotBlank() } ?: "Member",
    username = author.username?.takeIf { it.isNotBlank() } ?: author.name?.lowercase()?.replace(" ", "") ?: "member",
    avatarUrl = author.avatar_url.orEmpty(),
    timeAgo = formatMomentTimestamp(created_at),
    text = content.orEmpty(),
    likesCount = likes_count,
    isLiked = liked_by_me,
    isMe = author.is_me,
    kind = kind,
    originalText = original_text,
    correctedText = corrected_text,
    byNative = by_native,
    isAccepted = is_accepted,
    repliesCount = replies_count
)

private fun MomentDto.toDetailedPost(existingBookmark: Boolean = false): DetailedPost {
    val visualUrls = media.mapNotNull { m ->
        val url = if (m.type == "video") (m.thumb_url ?: m.url) else m.url
        url.takeIf { it.isNotBlank() }
    }
    return DetailedPost(
        id = id,
        authorId = author.id,
        authorName = author.name?.takeIf { it.isNotBlank() } ?: author.username?.takeIf { it.isNotBlank() } ?: "Member",
        authorUsername = author.username?.takeIf { it.isNotBlank() } ?: author.name?.lowercase()?.replace(" ", "") ?: "member",
        authorAvatarUrl = author.avatar_url.orEmpty(),
        isAuthorVerified = author.is_verified,
        location = location?.takeIf { it.isNotBlank() } ?: author.city.orEmpty(),
        timeAgo = formatMomentTimestamp(created_at),
        images = visualUrls,
        caption = content.orEmpty(),
        likesCount = likes_count,
        isLiked = liked_by_me,
        isBookmarked = existingBookmark,
        title = title,
        bgKey = bg_key,
        status = status,
        isMe = author.is_me,
        translation = translation,
        language = language,
        topics = topics,
        media = media,
        commentsCount = comments_count,
        correctionsCount = corrections_count
    )
}

// -----------------------------------------------------------------------------
// POST DETAIL SCREEN COMPOSABLE
// -----------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    post: DetailedPost,
    currentLoggedInAvatar: String = "",
    onBack: () -> Unit,
    onLikePostToggle: (Boolean) -> Unit = {},
    onBookmarkPostToggle: (Boolean) -> Unit = {},
    onSharePost: () -> Unit = {},
    onOptionsClick: (() -> Unit)? = null,
    onPostUpdated: (MomentDto) -> Unit = {},
    onPostDeleted: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Live Post State
    var currentPost by remember(post.id) { mutableStateOf(post) }
    var isLiked by remember(post.id) { mutableStateOf(post.isLiked) }
    var likesCount by remember(post.id) { mutableStateOf(post.likesCount) }
    var isBookmarked by remember(post.id) { mutableStateOf(post.isBookmarked) }
    var totalCommentsCount by remember(post.id) {
        mutableStateOf(maxOf(post.initialComments.size, post.commentsCount + post.correctionsCount))
    }

    val comments = remember(post.id) {
        mutableStateListOf<PostComment>().apply { addAll(post.initialComments) }
    }
    var isLoadingComments by remember(post.id) { mutableStateOf(true) }
    var isLoadingMoreComments by remember(post.id) { mutableStateOf(false) }
    var hasMoreComments by remember(post.id) { mutableStateOf(false) }
    var newCommentText by remember(post.id) { mutableStateOf("") }
    var isSendingComment by remember(post.id) { mutableStateOf(false) }
    var isLikeInFlight by remember(post.id) { mutableStateOf(false) }
    var myAvatarUrl by remember(currentLoggedInAvatar) { mutableStateOf(currentLoggedInAvatar) }

    // Double tap heart, fullscreen media & options state
    var showHeartPopup by remember { mutableStateOf(false) }
    var fullscreenPreviewUrl by remember { mutableStateOf<String?>(null) }
    var activeVideoUrl by remember { mutableStateOf<String?>(null) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var isDeletingPost by remember { mutableStateOf(false) }

    LaunchedEffect(post.id) {
        // Resolve current logged-in avatar if not passed
        if (myAvatarUrl.isBlank()) {
            withContext(Dispatchers.IO) {
                val uid = SupabaseSessionManager.getUserId(context)
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (!uid.isNullOrBlank() && !token.isNullOrBlank()) {
                    try {
                        val resp = SupabaseClient.api.getProfile(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token,
                            authUserIdEq = "eq.$uid"
                        )
                        if (resp.isSuccessful) {
                            val url = resp.body()?.firstOrNull()?.avatar_url
                            if (!url.isNullOrBlank()) {
                                myAvatarUrl = url
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Fetch fresh moment details from RPC
        MomentsRepository.getMoment(context, post.id)
            .onSuccess { dto ->
                val updated = dto.toDetailedPost(existingBookmark = isBookmarked)
                currentPost = updated
                isLiked = updated.isLiked
                likesCount = updated.likesCount
                totalCommentsCount = updated.commentsCount + updated.correctionsCount
                onPostUpdated(dto)
            }
            .onFailure { err ->
                if (err is MomentRpcException && err.isNotFound) {
                    Toast.makeText(context, err.message, Toast.LENGTH_SHORT).show()
                    onPostDeleted(post.id)
                    onBack()
                    return@LaunchedEffect
                }
            }

        // Fetch real comments from RPC
        isLoadingComments = true
        MomentsRepository.getMomentComments(context, post.id, kind = "all", limit = 30, offset = 0)
            .onSuccess { dtos ->
                comments.clear()
                comments.addAll(dtos.map { it.toPostComment() })
                hasMoreComments = dtos.size >= 30
                totalCommentsCount = maxOf(totalCommentsCount, comments.size)
            }
            .onFailure { err ->
                Toast.makeText(context, err.message ?: "Something went wrong. Try again", Toast.LENGTH_SHORT).show()
            }
        isLoadingComments = false
    }

    fun triggerTogglePostLike(forceLikeOnly: Boolean = false) {
        if (forceLikeOnly && isLiked) return
        if (isLikeInFlight) return
        val prevLiked = isLiked
        val prevCount = likesCount
        val targetLiked = if (forceLikeOnly) true else !prevLiked

        isLiked = targetLiked
        likesCount = if (targetLiked) prevCount + 1 else (prevCount - 1).coerceAtLeast(0)
        onLikePostToggle(targetLiked)
        isLikeInFlight = true

        coroutineScope.launch {
            MomentsRepository.toggleMomentLike(context, currentPost.id)
                .onSuccess { res ->
                    isLiked = res.liked
                    likesCount = res.likes_count
                    onLikePostToggle(res.liked)
                }
                .onFailure { err ->
                    isLiked = prevLiked
                    likesCount = prevCount
                    onLikePostToggle(prevLiked)
                    Toast.makeText(context, err.message ?: "Something went wrong. Try again", Toast.LENGTH_SHORT).show()
                }
            isLikeInFlight = false
        }
    }

    fun submitComment() {
        val trimmed = newCommentText.trim()
        if (trimmed.isEmpty() || isSendingComment) return
        isSendingComment = true
        focusManager.clearFocus()
        coroutineScope.launch {
            MomentsRepository.addMomentComment(context, currentPost.id, trimmed, parentId = null)
                .onSuccess { dto ->
                    val created = dto.toPostComment()
                    comments.add(0, created)
                    totalCommentsCount += 1
                    newCommentText = ""
                    delay(80)
                    listState.animateScrollToItem(3)
                }
                .onFailure { err ->
                    Toast.makeText(context, err.message ?: "Something went wrong. Try again", Toast.LENGTH_SHORT).show()
                }
            isSendingComment = false
        }
    }

    val mediaList = currentPost.media
    val hasVisualMedia = currentPost.images.isNotEmpty()
    val pageCount = if (mediaList.isNotEmpty()) mediaList.size else currentPost.images.size
    val pagerState = rememberPagerState(pageCount = { pageCount.coerceAtLeast(1) })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // =================================================================
            // 1. TOP HERO SECTION (MEDIA CAROUSEL OR GRADIENT TEXT CARD)
            // =================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (hasVisualMedia) 1.05f else 0.72f)
                    .background(Color.Black)
            ) {
                if (hasVisualMedia) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(currentPost.id, pagerState.currentPage) {
                                detectTapGestures(
                                    onTap = {
                                        val mediaItem = mediaList.getOrNull(pagerState.currentPage)
                                        if (mediaItem?.type == "video" && mediaItem.url.isNotBlank()) {
                                            activeVideoUrl = mediaItem.url
                                        } else {
                                            val previewUrl = currentPost.images.getOrElse(pagerState.currentPage) {
                                                currentPost.images.firstOrNull() ?: ""
                                            }
                                            if (previewUrl.isNotBlank()) {
                                                fullscreenPreviewUrl = previewUrl
                                            }
                                        }
                                    },
                                    onDoubleTap = {
                                        triggerTogglePostLike(forceLikeOnly = true)
                                        showHeartPopup = true
                                        coroutineScope.launch {
                                            delay(800)
                                            showHeartPopup = false
                                        }
                                    }
                                )
                            }
                    ) { page ->
                        val mediaItem = mediaList.getOrNull(page)
                        val isVideoPage = mediaItem?.type == "video"
                        val displayUrl = currentPost.images.getOrElse(page) {
                            mediaItem?.thumb_url ?: mediaItem?.url ?: ""
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(displayUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Post media ${page + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isVideoPage) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .align(Alignment.Center)
                                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                        .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(22.dp)) {
                                        val path = Path().apply {
                                            moveTo(size.width * 0.25f, size.height * 0.12f)
                                            lineTo(size.width * 0.88f, size.height * 0.5f)
                                            lineTo(size.width * 0.25f, size.height * 0.88f)
                                            close()
                                        }
                                        drawPath(path, Color.White, style = Fill)
                                    }
                                }
                            }

                            // Subtle bottom gradient to blend into card and contrast dots
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                        )
                                    )
                            )
                        }
                    }
                } else {
                    val preset = remember(currentPost.bgKey) {
                        MomentsRepository.resolveBgPreset(currentPost.bgKey)
                    }
                    val textColor = if (preset.isDarkText) Color(0xFF0F172A) else Color.White
                    val subTextColor = if (preset.isDarkText) Color(0xFF334155) else Color.White.copy(alpha = 0.88f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(preset.colors))
                            .pointerInput(currentPost.id) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        triggerTogglePostLike(forceLikeOnly = true)
                                        showHeartPopup = true
                                        coroutineScope.launch {
                                            delay(800)
                                            showHeartPopup = false
                                        }
                                    }
                                )
                            }
                            .padding(horizontal = 28.dp, vertical = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!currentPost.title.isNullOrBlank()) {
                                Text(
                                    text = currentPost.title.orEmpty(),
                                    color = textColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 26.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (currentPost.caption.isNotBlank()) {
                                Text(
                                    text = currentPost.caption,
                                    color = subTextColor,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp,
                                    maxLines = 6,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Floating Double-Tap Heart Animation
                androidx.compose.animation.AnimatedVisibility(
                    visible = showHeartPopup,
                    enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut(targetScale = 1.3f) + fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .shadow(16.dp, CircleShape)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PostHeartIcon(modifier = Modifier.size(64.dp), color = Color(0xFFEF4444), isFilled = true)
                    }
                }

                // Top Floating Actions (Back Button & More Button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(4.dp, CircleShape)
                                .background(Color.White.copy(alpha = 0.92f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            PostChevronLeftIcon(modifier = Modifier.size(16.dp), color = Color(0xFF0F172A))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (onOptionsClick != null) onOptionsClick() else showOptionsSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(4.dp, CircleShape)
                                .background(Color.White.copy(alpha = 0.92f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            PostMoreHorizontalIcon(modifier = Modifier.size(18.dp), color = Color(0xFF0F172A))
                        }
                    }
                }

                // Pager Indicators (Dots at bottom center of photo)
                if (hasVisualMedia && pageCount > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pageCount) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 6.5.dp else 4.5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color(0xFFFFD43B) else Color.White.copy(alpha = 0.65f)
                                    )
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 2. BOTTOM CARD SECTION: AUTHOR, CAPTION, ACTIONS & SCROLLABLE COMMENTS
            // =================================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (hasVisualMedia) 1.15f else 1.48f)
                    .offset(y = (-14).dp),
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 14.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        // 2.1 Author Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(currentPost.authorAvatarUrl.ifBlank { null })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = currentPost.authorName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F5F9), CircleShape)
                                        .border(1.dp, Color(0xFFF1EADF), CircleShape)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = currentPost.authorName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )
                                        if (currentPost.isAuthorVerified) {
                                            PostVerifiedCheckBadge(modifier = Modifier.size(15.dp))
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "@${currentPost.authorUsername}",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = currentPost.timeAgo,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                        if (currentPost.location.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                PostLocationPinIcon(modifier = Modifier.size(11.dp), color = Color(0xFF64748B))
                                                Text(
                                                    text = currentPost.location,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (currentPost.status == "hidden") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Hidden by moderators — only visible to you",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 2.2 Post Title, Caption, Translation & Topics
                        item {
                            if (hasVisualMedia) {
                                if (!currentPost.title.isNullOrBlank()) {
                                    Text(
                                        text = currentPost.title.orEmpty(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A),
                                        lineHeight = 22.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                if (currentPost.caption.isNotBlank()) {
                                    Text(
                                        text = currentPost.caption,
                                        fontSize = 13.5.sp,
                                        lineHeight = 19.sp,
                                        color = Color(0xFF1E293B),
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                            if (!currentPost.translation.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = currentPost.translation.orEmpty(),
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF475569),
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                            if (currentPost.topics.isNotEmpty() || currentPost.language.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (currentPost.language.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFFEF9C3), RoundedCornerShape(100.dp))
                                                .padding(horizontal = 9.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = currentPost.language,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF854D0E)
                                            )
                                        }
                                    }
                                    currentPost.topics.take(3).forEach { slug ->
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFF1F5F9), RoundedCornerShape(100.dp))
                                                .padding(horizontal = 9.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = MomentsRepository.resolveTopicChipText(slug),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 2.3 Post Action Engagement Bar (Heart, Comment, Share, Bookmark)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) {
                                            triggerTogglePostLike(forceLikeOnly = false)
                                        }
                                    ) {
                                        PostHeartIcon(
                                            modifier = Modifier.size(20.dp),
                                            color = if (isLiked) Color(0xFFE11D48) else Color(0xFF334155),
                                            isFilled = isLiked
                                        )
                                        Text(
                                            text = formatLikeCount(likesCount),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        PostCommentBubbleIcon(modifier = Modifier.size(19.dp), color = Color(0xFF334155))
                                        Text(
                                            text = "${maxOf(totalCommentsCount, comments.size)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier.clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { onSharePost() }
                                    ) {
                                        PostSharePaperAirplaneIcon(modifier = Modifier.size(19.dp), color = Color(0xFF334155))
                                    }
                                }

                                Box(
                                    modifier = Modifier.clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        isBookmarked = !isBookmarked
                                        onBookmarkPostToggle(isBookmarked)
                                    }
                                ) {
                                    PostBookmarkIcon(
                                        modifier = Modifier.size(20.dp),
                                        color = if (isBookmarked) Color(0xFFFFD43B) else Color(0xFF334155),
                                        isFilled = isBookmarked
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 2.4 Comments Loading / Empty / Live Stream
                        if (isLoadingComments) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFFEAB308)
                                    )
                                }
                            }
                        } else if (comments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No comments yet. Be the first to reply!",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(comments, key = { _, c -> c.id }) { index, comment ->
                                CommentRowItem(
                                    comment = comment,
                                    canDelete = comment.isMe || currentPost.isMe,
                                    onToggleLike = {
                                        val prevLiked = comment.isLiked
                                        val prevLikes = comment.likesCount
                                        val nextLiked = !prevLiked
                                        comments[index] = comment.copy(
                                            isLiked = nextLiked,
                                            likesCount = if (nextLiked) prevLikes + 1 else (prevLikes - 1).coerceAtLeast(0)
                                        )
                                        coroutineScope.launch {
                                            MomentsRepository.toggleCommentLike(context, comment.id)
                                                .onSuccess { res ->
                                                    val idx = comments.indexOfFirst { it.id == comment.id }
                                                    if (idx >= 0) {
                                                        comments[idx] = comments[idx].copy(
                                                            isLiked = res.liked,
                                                            likesCount = res.likes_count
                                                        )
                                                    }
                                                }
                                                .onFailure { err ->
                                                    val idx = comments.indexOfFirst { it.id == comment.id }
                                                    if (idx >= 0) {
                                                        comments[idx] = comments[idx].copy(
                                                            isLiked = prevLiked,
                                                            likesCount = prevLikes
                                                        )
                                                    }
                                                    Toast.makeText(
                                                        context,
                                                        err.message ?: "Something went wrong. Try again",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                    },
                                    onDelete = {
                                        coroutineScope.launch {
                                            MomentsRepository.deleteMomentComment(context, comment.id)
                                                .onSuccess {
                                                    comments.removeAll { it.id == comment.id }
                                                    totalCommentsCount = (totalCommentsCount - 1).coerceAtLeast(comments.size)
                                                }
                                                .onFailure { err ->
                                                    Toast.makeText(
                                                        context,
                                                        err.message ?: "Something went wrong. Try again",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (hasMoreComments) {
                                item {
                                    TextButton(
                                        onClick = {
                                            if (isLoadingMoreComments) return@TextButton
                                            isLoadingMoreComments = true
                                            coroutineScope.launch {
                                                MomentsRepository.getMomentComments(
                                                    context = context,
                                                    momentId = currentPost.id,
                                                    kind = "all",
                                                    limit = 30,
                                                    offset = comments.size
                                                ).onSuccess { more ->
                                                    val existing = comments.map { it.id }.toSet()
                                                    comments.addAll(
                                                        more.filterNot { it.id in existing }.map { it.toPostComment() }
                                                    )
                                                    hasMoreComments = more.size >= 30
                                                }.onFailure { err ->
                                                    Toast.makeText(
                                                        context,
                                                        err.message ?: "Something went wrong. Try again",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                isLoadingMoreComments = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isLoadingMoreComments) "Loading..." else "Load more comments",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // =========================================================
                    // 3. STICKY BOTTOM COMMENT INPUT BAR
                    // =========================================================
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .imePadding()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(myAvatarUrl.ifBlank { null })
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "My avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9), CircleShape)
                                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (newCommentText.isEmpty()) {
                                    Text(
                                        text = "Add a comment...",
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                BasicTextField(
                                    value = newCommentText,
                                    onValueChange = { if (it.length <= 500) newCommentText = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { submitComment() }),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            val canSend = newCommentText.isNotBlank() && !isSendingComment
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (canSend) Color(0xFFFFD43B) else Color(0xFFFDE68A))
                                    .clickable(enabled = canSend) { submitComment() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSendingComment) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF0F172A)
                                    )
                                } else {
                                    PostSendArrowIcon(
                                        modifier = Modifier.size(16.dp),
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (fullscreenPreviewUrl != null) {
            ChatImagePreviewDialog(
                imageUrl = fullscreenPreviewUrl,
                imageUrls = currentPost.images,
                initialIndex = pagerState.currentPage,
                senderName = currentPost.authorName,
                onDismiss = { fullscreenPreviewUrl = null }
            )
        }

        if (activeVideoUrl != null) {
            DetailVideoPlayerDialog(
                videoUrl = activeVideoUrl!!,
                onDismiss = { activeVideoUrl = null }
            )
        }

        if (showOptionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showOptionsSheet = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentPost.isMe) {
                        Surface(
                            onClick = {
                                if (isDeletingPost) return@Surface
                                isDeletingPost = true
                                coroutineScope.launch {
                                    MomentsRepository.deleteMoment(context, currentPost.id)
                                        .onSuccess {
                                            showOptionsSheet = false
                                            Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                                            onPostDeleted(currentPost.id)
                                            onBack()
                                        }
                                        .onFailure { err ->
                                            Toast.makeText(
                                                context,
                                                err.message ?: "Something went wrong. Try again",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    isDeletingPost = false
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFEF2F2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isDeletingPost) "Deleting..." else "Delete post",
                                color = Color(0xFFDC2626),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Surface(
                            onClick = {
                                showOptionsSheet = false
                                coroutineScope.launch {
                                    MomentsRepository.markMomentNotInterested(context, currentPost.id)
                                        .onSuccess {
                                            Toast.makeText(context, "Got it, showing less like this", Toast.LENGTH_SHORT).show()
                                            onPostDeleted(currentPost.id)
                                            onBack()
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
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Not interested",
                                color = Color(0xFF0F172A),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        if (currentPost.authorId.isNotBlank()) {
                            Surface(
                                onClick = {
                                    showOptionsSheet = false
                                    showReportDialog = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFEF2F2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Report post",
                                    color = Color(0xFFDC2626),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (showReportDialog && currentPost.authorId.isNotBlank()) {
            ReportUserDialog(
                reportedUserId = currentPost.authorId,
                reportedUserName = currentPost.authorName,
                contextType = "moment",
                contextId = currentPost.id,
                onDismiss = { showReportDialog = false },
                onReportSubmitted = {
                    showReportDialog = false
                    Toast.makeText(context, "Report submitted. Thank you.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@AndroidOptIn(UnstableApi::class)
@Composable
private fun DetailVideoPlayerDialog(
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
        onDispose { exoPlayer.release() }
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
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMMENT ROW ITEM
// -----------------------------------------------------------------------------

@Composable
fun CommentRowItem(
    comment: PostComment,
    canDelete: Boolean = false,
    onToggleLike: () -> Unit,
    onDelete: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(comment.avatarUrl.ifBlank { null })
                .crossfade(true)
                .build(),
            contentDescription = comment.username,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9), CircleShape)
                .border(1.dp, Color(0xFFF1EADF), CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = comment.username,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                if (comment.byNative) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Native",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                    }
                }
                Text(
                    text = comment.timeAgo,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            if (comment.kind == "correction" && (!comment.originalText.isNullOrBlank() || !comment.correctedText.isNullOrBlank())) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (!comment.originalText.isNullOrBlank()) {
                        Text(
                            text = comment.originalText,
                            fontSize = 12.sp,
                            color = Color(0xFFDC2626),
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    if (!comment.correctedText.isNullOrBlank()) {
                        Text(
                            text = comment.correctedText,
                            fontSize = 12.5.sp,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (comment.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = comment.text,
                    fontSize = 12.5.sp,
                    color = Color(0xFF334155),
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canDelete) {
                Text(
                    text = "Delete",
                    fontSize = 10.5.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onDelete() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onToggleLike() }
                    .padding(4.dp)
            ) {
                PostHeartIcon(
                    modifier = Modifier.size(14.dp),
                    color = if (comment.isLiked) Color(0xFFE11D48) else Color(0xFF94A3B8),
                    isFilled = comment.isLiked
                )
                if (comment.likesCount > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${comment.likesCount}",
                        fontSize = 10.5.sp,
                        color = if (comment.isLiked) Color(0xFFE11D48) else Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// HELPER FUNCTIONS
// -----------------------------------------------------------------------------

fun formatLikeCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

// -----------------------------------------------------------------------------
// CUSTOM HIGH-PRECISION VECTOR ICONS
// -----------------------------------------------------------------------------

@Composable
fun PostVerifiedCheckBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(w, h) * 0.46f

        drawCircle(
            color = Color(0xFF0284C7),
            radius = radius,
            center = Offset(cx, cy)
        )

        val strokeW = 1.8f * density
        val path = Path().apply {
            moveTo(cx - radius * 0.45f, cy)
            lineTo(cx - radius * 0.1f, cy + radius * 0.35f)
            lineTo(cx + radius * 0.48f, cy - radius * 0.35f)
        }
        drawPath(path, Color.White, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun PostLocationPinIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF64748B)) {
    Canvas(modifier = modifier) {
        val sw = 1.3f * density
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h * 0.38f
        val r = w * 0.34f

        val path = Path().apply {
            moveTo(cx, h * 0.95f)
            cubicTo(cx - r, h * 0.65f, cx - r, cy, cx - r, cy)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            cubicTo(cx + r, cy, cx + r, h * 0.65f, cx, h * 0.95f)
            close()
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = r * 0.38f, center = Offset(cx, cy), style = Fill)
    }
}

@Composable
fun PostHeartIcon(modifier: Modifier = Modifier, color: Color, isFilled: Boolean) {
    Canvas(modifier = modifier) {
        val sw = 1.8f * density
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w / 2f, h * 0.85f)
            cubicTo(w * 0.15f, h * 0.60f, 0f, h * 0.35f, 0f, h * 0.22f)
            cubicTo(0f, h * 0.08f, w * 0.12f, 0f, w * 0.28f, 0f)
            cubicTo(w * 0.40f, 0f, w / 2f, h * 0.15f, w / 2f, h * 0.15f)
            cubicTo(w / 2f, h * 0.15f, w * 0.60f, 0f, w * 0.72f, 0f)
            cubicTo(w * 0.88f, 0f, w, h * 0.08f, w, h * 0.22f)
            cubicTo(w, h * 0.35f, w * 0.85f, h * 0.60f, w / 2f, h * 0.85f)
            close()
        }
        if (isFilled) {
            drawPath(path, color, style = Fill)
        } else {
            drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun PostCommentBubbleIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF334155)) {
    Canvas(modifier = modifier) {
        val sw = 1.8f * density
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.15f)
            lineTo(w * 0.88f, h * 0.15f)
            cubicTo(w * 0.96f, h * 0.15f, w * 0.96f, h * 0.68f, w * 0.88f, h * 0.68f)
            lineTo(w * 0.45f, h * 0.68f)
            lineTo(w * 0.22f, h * 0.92f)
            lineTo(w * 0.22f, h * 0.68f)
            lineTo(w * 0.12f, h * 0.68f)
            cubicTo(w * 0.04f, h * 0.68f, w * 0.04f, h * 0.15f, w * 0.12f, h * 0.15f)
            close()
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun PostSharePaperAirplaneIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF334155)) {
    Canvas(modifier = modifier) {
        val sw = 1.8f * density
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.92f, h * 0.08f)
            lineTo(w * 0.08f, h * 0.45f)
            lineTo(w * 0.40f, h * 0.58f)
            lineTo(w * 0.52f, h * 0.92f)
            close()
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawLine(
            color = color,
            start = Offset(w * 0.40f, h * 0.58f),
            end = Offset(w * 0.92f, h * 0.08f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun PostBookmarkIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF334155), isFilled: Boolean) {
    Canvas(modifier = modifier) {
        val sw = 1.8f * density
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.05f)
            lineTo(w * 0.82f, h * 0.05f)
            lineTo(w * 0.82f, h * 0.95f)
            lineTo(w * 0.50f, h * 0.70f)
            lineTo(w * 0.18f, h * 0.95f)
            close()
        }
        if (isFilled) {
            drawPath(path, color, style = Fill)
        } else {
            drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun PostChevronLeftIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier) {
        val sw = 2.2f * density
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.65f, h * 0.20f)
            lineTo(w * 0.35f, h * 0.50f)
            lineTo(w * 0.65f, h * 0.80f)
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun PostMoreHorizontalIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier) {
        val r = 2.2f * density
        val cy = size.height / 2f
        val cx = size.width / 2f
        val gap = 6f * density

        drawCircle(color, radius = r, center = Offset(cx - gap, cy))
        drawCircle(color, radius = r, center = Offset(cx, cy))
        drawCircle(color, radius = r, center = Offset(cx + gap, cy))
    }
}

@Composable
fun PostSendArrowIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.50f)
            lineTo(w * 0.2f, h * 0.85f)
            lineTo(w * 0.35f, h * 0.50f)
            close()
        }
        drawPath(path, color, style = Fill)
    }
}
