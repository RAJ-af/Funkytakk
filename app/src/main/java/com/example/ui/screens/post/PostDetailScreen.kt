package com.example.ui.screens.post

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.screens.chat.components.ChatImagePreviewDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var isLiked: Boolean = false
)

data class DetailedPost(
    val id: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatarUrl: String,
    val isAuthorVerified: Boolean = true,
    val location: String = "Seoul, South Korea",
    val timeAgo: String = "3 days ago",
    val images: List<String>,
    val caption: String,
    var likesCount: Int = 1248,
    var isLiked: Boolean = false,
    var isBookmarked: Boolean = false,
    val initialComments: List<PostComment> = emptyList()
)

// -----------------------------------------------------------------------------
// DEFAULT MOCK POST GENERATOR
// -----------------------------------------------------------------------------

fun createDefaultSamplePost(
    authorName: String = "pikaa",
    authorUsername: String = "pikaa_07",
    authorAvatar: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500",
    initialImageUrl: String? = null,
    captionText: String? = null
): DetailedPost {
    val sampleImages = if (initialImageUrl != null) {
        listOf(
            initialImageUrl,
            "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=1000",
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1000",
            "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1000",
            "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1000"
        )
    } else {
        listOf(
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=1000",
            "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=1000",
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1000",
            "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1000",
            "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1000"
        )
    }

    return DetailedPost(
        id = "post_${System.currentTimeMillis()}",
        authorName = authorName,
        authorUsername = authorUsername,
        authorAvatarUrl = authorAvatar,
        isAuthorVerified = true,
        location = "Seoul, South Korea",
        timeAgo = "3 days ago",
        images = sampleImages,
        caption = captionText ?: "Beautiful sunset today! Nature always gives me so much peace. 🌅 💛",
        likesCount = 1248,
        isLiked = true,
        isBookmarked = false,
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

// -----------------------------------------------------------------------------
// POST DETAIL SCREEN COMPOSABLE
// -----------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostDetailScreen(
    post: DetailedPost,
    currentLoggedInAvatar: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
    onBack: () -> Unit,
    onLikePostToggle: (Boolean) -> Unit = {},
    onBookmarkPostToggle: (Boolean) -> Unit = {},
    onSharePost: () -> Unit = {},
    onOptionsClick: () -> Unit = {}
) {
    BackHandler { onBack() }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // State
    var isLiked by remember { mutableStateOf(post.isLiked) }
    var likesCount by remember { mutableStateOf(post.likesCount) }
    var isBookmarked by remember { mutableStateOf(post.isBookmarked) }
    val comments = remember { mutableStateListOf<PostComment>().apply { addAll(post.initialComments) } }
    var newCommentText by remember { mutableStateOf("") }
    
    // Double tap heart animation overlay & fullscreen image preview
    var showHeartPopup by remember { mutableStateOf(false) }
    var fullscreenPreviewUrl by remember { mutableStateOf<String?>(null) }

    // Image Pager State
    val pagerState = rememberPagerState(pageCount = { post.images.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // =================================================================
            // 1. TOP HERO IMAGE SECTION WITH HORIZONTAL PAGER & OVERLAYS
            // =================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.05f)
                    .background(Color.Black)
            ) {
                // Swipable Carousel Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    fullscreenPreviewUrl = post.images.getOrElse(pagerState.currentPage) { post.images.firstOrNull() ?: "" }
                                },
                                onDoubleTap = {
                                    if (!isLiked) {
                                        isLiked = true
                                        likesCount++
                                        onLikePostToggle(true)
                                    }
                                    showHeartPopup = true
                                    coroutineScope.launch {
                                        delay(800)
                                        showHeartPopup = false
                                    }
                                }
                            )
                        }
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(post.images.getOrElse(page) { post.images.firstOrNull() ?: "" })
                                .crossfade(true)
                                .build(),
                            contentDescription = "Post image ${page + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

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
                    // Back Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(4.dp, CircleShape)
                            .background(Color.White.copy(alpha = 0.92f), CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        PostChevronLeftIcon(modifier = Modifier.size(16.dp), color = Color(0xFF0F172A))
                    }

                    // More Options Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(4.dp, CircleShape)
                            .background(Color.White.copy(alpha = 0.92f), CircleShape)
                            .clickable { onOptionsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        PostMoreHorizontalIcon(modifier = Modifier.size(18.dp), color = Color(0xFF0F172A))
                    }
                }

                // Pager Indicators (Dots at bottom center of photo)
                if (post.images.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(post.images.size) { index ->
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
                    .weight(1.15f)
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
                    // Scrollable Area (Author info, Caption, Engagement Bar, and Comments)
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
                                // Author Avatar
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(post.authorAvatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = post.authorName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color(0xFFF1EADF), CircleShape)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = post.authorName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )
                                        if (post.isAuthorVerified) {
                                            PostVerifiedCheckBadge(modifier = Modifier.size(15.dp))
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "@${post.authorUsername}",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = post.timeAgo,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                        if (post.location.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                PostLocationPinIcon(modifier = Modifier.size(11.dp), color = Color(0xFF64748B))
                                                Text(
                                                    text = post.location,
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

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 2.2 Post Caption
                        item {
                            val gradientThemes = remember {
                                listOf(
                                    listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899)),
                                    listOf(Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFF59E0B)),
                                    listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF3B82F6)),
                                    listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF4C1D95)),
                                    listOf(Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFFFDA4AF)),
                                    listOf(Color(0xFF18181B), Color(0xFF27272A), Color(0xFF3F3F46))
                                )
                            }
                            val isGradient = post.caption.startsWith("[gradient:")
                            val gradientIdx = if (isGradient) {
                                post.caption.substringAfter("[gradient:").substringBefore("]").toIntOrNull() ?: 0
                            } else 0
                            val cleanCaption = post.caption
                                .replace(Regex("^\\[gradient:\\d+\\]\\s*"), "")
                                .replace(Regex("^\\[video\\]\\s*"), "")

                            if (isGradient) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 160.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Brush.linearGradient(gradientThemes[gradientIdx.coerceIn(0, 5)]))
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cleanCaption,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 24.sp
                                    )
                                }
                            } else if (cleanCaption.isNotBlank()) {
                                Text(
                                    text = cleanCaption,
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp,
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.Normal
                                )
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
                                // Left side: Like, Comment, Share
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Like Action
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) {
                                            isLiked = !isLiked
                                            if (isLiked) likesCount++ else likesCount--
                                            onLikePostToggle(isLiked)
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

                                    // Comments Count
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        PostCommentBubbleIcon(modifier = Modifier.size(19.dp), color = Color(0xFF334155))
                                        Text(
                                            text = "${comments.size}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    // Share Action
                                    Box(
                                        modifier = Modifier.clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { onSharePost() }
                                    ) {
                                        PostSharePaperAirplaneIcon(modifier = Modifier.size(19.dp), color = Color(0xFF334155))
                                    }
                                }

                                // Right side: Bookmark Action
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

                        // 2.4 Comments List Items (Live Scrollable Stream)
                        itemsIndexed(comments, key = { _, c -> c.id }) { index, comment ->
                            CommentRowItem(
                                comment = comment,
                                onToggleLike = {
                                    val currentLiked = comment.isLiked
                                    comment.isLiked = !currentLiked
                                    comment.likesCount = if (!currentLiked) comment.likesCount + 1 else comment.likesCount - 1
                                    // Trigger recomposition
                                    comments[index] = comment.copy(isLiked = comment.isLiked, likesCount = comment.likesCount)
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Extra bottom spacing so last comments aren't clipped by input bar
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
                            // Current User Avatar
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(currentLoggedInAvatar)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "My avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            )

                            // Input Box Pill
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
                                    onValueChange = { newCommentText = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (newCommentText.isNotBlank()) {
                                                val newComment = PostComment(
                                                    id = "c_${System.currentTimeMillis()}",
                                                    authorName = "Me",
                                                    username = "you",
                                                    avatarUrl = currentLoggedInAvatar,
                                                    timeAgo = "Just now",
                                                    text = newCommentText.trim(),
                                                    likesCount = 0,
                                                    isLiked = false
                                                )
                                                comments.add(newComment)
                                                newCommentText = ""
                                                focusManager.clearFocus()
                                                coroutineScope.launch {
                                                    delay(100)
                                                    listState.animateScrollToItem(comments.size + 2)
                                                }
                                            }
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Yellow/Amber Round Send Button
                            val canSend = newCommentText.isNotBlank()
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFD43B))
                                    .clickable {
                                        if (newCommentText.isNotBlank()) {
                                            val newComment = PostComment(
                                                id = "c_${System.currentTimeMillis()}",
                                                authorName = "Me",
                                                username = "you",
                                                avatarUrl = currentLoggedInAvatar,
                                                timeAgo = "Just now",
                                                text = newCommentText.trim(),
                                                likesCount = 0,
                                                isLiked = false
                                            )
                                            comments.add(newComment)
                                            newCommentText = ""
                                            focusManager.clearFocus()
                                            coroutineScope.launch {
                                                delay(100)
                                                listState.animateScrollToItem(comments.size + 2)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
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

        // Fullscreen Image Zoom & Save Preview
        if (fullscreenPreviewUrl != null) {
            ChatImagePreviewDialog(
                imageUrl = fullscreenPreviewUrl,
                imageUrls = post.images,
                initialIndex = pagerState.currentPage,
                senderName = post.authorName,
                onDismiss = { fullscreenPreviewUrl = null }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// COMMENT ROW ITEM
// -----------------------------------------------------------------------------

@Composable
fun CommentRowItem(
    comment: PostComment,
    onToggleLike: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Commenter Avatar
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(comment.avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = comment.username,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(1.dp, Color(0xFFF1EADF), CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Comment Body
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
                Text(
                    text = comment.timeAgo,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = comment.text,
                fontSize = 12.5.sp,
                color = Color(0xFF334155),
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Comment Like Heart & Counter
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

        // Blue circle background
        drawCircle(
            color = Color(0xFF0284C7),
            radius = radius,
            center = Offset(cx, cy)
        )

        // White checkmark
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

        // Inner dart crease line
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
        val sw = 2f * density

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
