package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.example.MomentAuthorDto
import com.example.MomentCommentDto
import com.example.MomentDto
import com.example.MomentMediaDto
import com.example.R
import com.example.moderation.ReportUserDialog
import com.example.repository.MomentRpcException
import com.example.repository.MomentsRepository
import com.example.ui.screens.FeedPost
import com.example.ui.screens.LanguageData
import com.example.ui.screens.PostType
import com.example.ui.screens.chat.components.ChatImagePreviewDialog
import com.example.ui.screens.formatMomentTimestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class MomentWordSpan(
    val word: String,
    val startIndex: Int,
    val endIndexExclusive: Int
)

private fun tokenizeWordsWithSpans(text: String): List<MomentWordSpan> {
    val regex = Regex("\\S+")
    return regex.findAll(text).map { match ->
        MomentWordSpan(
            word = match.value,
            startIndex = match.range.first,
            endIndexExclusive = match.range.last + 1
        )
    }.toList()
}

fun formatMomentCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

fun FeedPost.toFallbackMomentDto(
    fallbackAuthorId: String = "",
    fallbackAuthorName: String = "Learner",
    fallbackAvatarUrl: String = "",
    fallbackCountryCode: String = "",
    isSelfAuthor: Boolean = false
): MomentDto {
    if (momentDto != null) return momentDto
    val resolvedMedia = when {
        allImages.isNotEmpty() -> allImages.map { url ->
            MomentMediaDto(
                type = if (isVideo) "video" else "image",
                url = url,
                thumb_url = if (isVideo) url else null
            )
        }
        !imageUrl.isNullOrBlank() -> listOf(
            MomentMediaDto(
                type = if (isVideo) "video" else "image",
                url = imageUrl,
                thumb_url = if (isVideo) imageUrl else null
            )
        )
        else -> emptyList()
    }
    return MomentDto(
        id = id,
        title = title,
        content = caption.ifBlank { if (type == PostType.TEXT) textContent.orEmpty() else "" },
        bg_key = bgKey,
        status = status,
        likes_count = likesCount,
        comments_count = commentsCount,
        liked_by_me = isLikedByUser,
        created_at = "",
        author = MomentAuthorDto(
            id = fallbackAuthorId,
            name = fallbackAuthorName,
            avatar_url = fallbackAvatarUrl.ifBlank { null },
            country_code = fallbackCountryCode.ifBlank { null },
            is_me = isMe || isSelfAuthor
        ),
        media = resolvedMedia
    )
}

@Composable
fun MomentActionIconButton(
    @DrawableRes iconRes: Int,
    tint: Color,
    countText: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Black.copy(alpha = 0.38f),
    countColor: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        if (countText.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = countText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = countColor
            )
        }
    }
}

@Composable
fun MomentV2Slide(
    moment: MomentDto,
    isBookmarked: Boolean,
    onOpenComments: () -> Unit,
    onOpenCorrections: () -> Unit = onOpenComments,
    onPlayVideo: (String) -> Unit,
    onToggleFollow: () -> Unit,
    onToggleLike: () -> Unit,
    onEnsureLikedOnDoubleTap: () -> Unit = onToggleLike,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onMediaImageClick: ((List<String>, Int) -> Unit)? = null,
    contentBottomPadding: Dp = 96.dp
) {
    val context = LocalContext.current
    val mediaList = moment.media
    val subPagerState = rememberPagerState(pageCount = { mediaList.size.coerceAtLeast(1) })
    var showTranslation by remember(moment.id) { mutableStateOf(false) }
    var showDoubleTapHeart by remember(moment.id) { mutableStateOf(false) }

    val isAppDark = com.example.ui.theme.LocalAppDarkTheme.current
    val funkyColors = com.example.ui.theme.LocalFunkyColors.current
    val preset = remember(moment.bg_key) { MomentsRepository.resolveBgPreset(moment.bg_key) }
    val isLightCanvas = mediaList.isEmpty() && preset.isDarkText
    val useLightUiContrast = isLightCanvas && !isAppDark
    val overlayInk = if (useLightUiContrast) funkyColors.ink else Color.White
    val overlaySubInk = if (useLightUiContrast) funkyColors.mute else Color.White.copy(alpha = 0.80f)
    val actionChipBg = if (useLightUiContrast) funkyColors.surface.copy(alpha = 0.88f) else Color.Black.copy(alpha = 0.38f)

    val heartScale by animateFloatAsState(
        targetValue = if (showDoubleTapHeart) 1.25f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "double_tap_heart"
    )

    LaunchedEffect(showDoubleTapHeart) {
        if (showDoubleTapHeart) {
            delay(650)
            showDoubleTapHeart = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(moment.id, moment.liked_by_me) {
                detectTapGestures(
                    onDoubleTap = {
                        showDoubleTapHeart = true
                        if (!moment.liked_by_me) {
                            onEnsureLikedOnDoubleTap()
                        }
                    }
                )
            }
    ) {
        // 1. Background Content: Multi-photo/video pager or Color Gradient
        if (mediaList.isEmpty()) {
            val textCardInk = if (useLightUiContrast) Color(0xFF1B1608) else Color.White
            val textCardSubInk = if (useLightUiContrast) Color(0xFF3F3323) else Color.White.copy(alpha = 0.90f)
            val hasBodyText = !moment.content.isNullOrBlank()
            val hasTitleText = !moment.title.isNullOrBlank()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(preset.colors))
                    .statusBarsPadding()
                    .padding(
                        top = 104.dp,
                        bottom = contentBottomPadding + 108.dp,
                        start = 20.dp,
                        end = 84.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = if (hasBodyText) Alignment.Start else Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (hasTitleText) {
                        Text(
                            text = moment.title!!,
                            fontSize = if (hasBodyText) 24.sp else 28.sp,
                            fontWeight = FontWeight.Black,
                            color = textCardInk,
                            textAlign = if (hasBodyText) TextAlign.Start else TextAlign.Center,
                            lineHeight = if (hasBodyText) 30.sp else 36.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (hasBodyText) {
                        Text(
                            text = moment.content!!,
                            fontSize = if (hasTitleText) 16.sp else 21.sp,
                            fontWeight = if (hasTitleText) FontWeight.Medium else FontWeight.Bold,
                            color = textCardSubInk,
                            textAlign = TextAlign.Start,
                            lineHeight = if (hasTitleText) 24.sp else 29.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (!moment.translation.isNullOrBlank()) {
                        Surface(
                            onClick = { showTranslation = !showTranslation },
                            shape = RoundedCornerShape(14.dp),
                            color = if (useLightUiContrast) funkyColors.surface.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.16f),
                            border = if (useLightUiContrast) BorderStroke(1.dp, funkyColors.line) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_globe),
                                    contentDescription = null,
                                    tint = funkyColors.mango,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (showTranslation) "Translation: ${moment.translation}" else "View author translation",
                                    fontSize = 12.5.sp,
                                    color = textCardInk,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (media.type != "video" && onMediaImageClick != null) {
                                Modifier.clickable {
                                    val imageUrls = mediaList
                                        .filter { it.type != "video" }
                                        .map { it.url }
                                    val clickedIdx = imageUrls.indexOf(media.url).coerceAtLeast(0)
                                    if (imageUrls.isNotEmpty()) {
                                        onMediaImageClick(imageUrls, clickedIdx)
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )
                ) {
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
                                painter = painterResource(id = R.drawable.ic_play_filled),
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
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
                                    .padding(top = 104.dp, start = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_video),
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

        // Top and Bottom gradient scrims for contrast (skipped when rendering a light-canvas text card in Light Mode)
        if (!useLightUiContrast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
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
        }

        // Photo pagination pill & dots indicator (if multiple photos)
        if (mediaList.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 104.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.48f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_photo),
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

        // Double tap animated heart burst
        AnimatedVisibility(
            visible = showDoubleTapHeart,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_heart_filled),
                contentDescription = null,
                tint = Color(0xFFFF3366),
                modifier = Modifier
                    .size(92.dp)
                    .scale(heartScale)
            )
        }

        // Right Action Bar: Like, Comment, Fix, Bookmark, Share
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = contentBottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MomentActionIconButton(
                iconRes = if (moment.liked_by_me) R.drawable.ic_heart_filled else R.drawable.ic_heart,
                tint = if (moment.liked_by_me) Color(0xFFFF3366) else overlayInk,
                countText = formatMomentCount(moment.likes_count),
                contentDescription = if (moment.liked_by_me) "Unlike moment" else "Like moment",
                containerColor = actionChipBg,
                countColor = overlayInk,
                onClick = onToggleLike
            )

            MomentActionIconButton(
                iconRes = R.drawable.ic_comment,
                tint = overlayInk,
                countText = formatMomentCount(moment.comments_count),
                contentDescription = "Comments",
                containerColor = actionChipBg,
                countColor = overlayInk,
                onClick = onOpenComments
            )

            MomentActionIconButton(
                iconRes = R.drawable.ic_fix,
                tint = Color(0xFFFFB627),
                countText = if (moment.corrections_count > 0) formatMomentCount(moment.corrections_count) else "Fix",
                contentDescription = "Native corrections",
                onClick = onOpenCorrections,
                containerColor = if (useLightUiContrast) Color(0xFF2B2118) else Color(0xFF2B2118).copy(alpha = 0.72f),
                countColor = overlayInk
            )

            MomentActionIconButton(
                iconRes = if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark,
                tint = if (isBookmarked) Color(0xFFFFB627) else overlayInk,
                countText = "",
                contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark moment",
                containerColor = actionChipBg,
                countColor = overlayInk,
                onClick = onToggleBookmark
            )

            MomentActionIconButton(
                iconRes = R.drawable.ic_share,
                tint = overlayInk,
                countText = "",
                contentDescription = "Share moment",
                containerColor = actionChipBg,
                countColor = overlayInk,
                onClick = onShare
            )
        }

        // Bottom Left Content Overlay: User info, Badges, Tags (plus Title/Caption/Translation on media posts only)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 84.dp, bottom = contentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (mediaList.isNotEmpty()) {
                if (!moment.title.isNullOrBlank()) {
                    Text(
                        text = moment.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = overlayInk,
                        lineHeight = 24.sp
                    )
                }

                if (!moment.content.isNullOrBlank()) {
                    Text(
                        text = moment.content,
                        fontSize = 13.5.sp,
                        color = if (useLightUiContrast) funkyColors.ink.copy(alpha = 0.85f) else Color(0xFFF1F5F9),
                        lineHeight = 19.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!moment.translation.isNullOrBlank()) {
                    Surface(
                        onClick = { showTranslation = !showTranslation },
                        shape = RoundedCornerShape(14.dp),
                        color = if (useLightUiContrast) funkyColors.soft else Color.White.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_globe),
                                contentDescription = null,
                                tint = funkyColors.mango,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (showTranslation) "Translation: ${moment.translation}" else "View author translation",
                                fontSize = 12.sp,
                                color = if (useLightUiContrast) funkyColors.ink else Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
            val flagCode = moment.author.country_code?.takeIf { it.isNotBlank() }
                ?: LanguageData.getFlagCodeForLanguage(moment.language)

            // User Profile Row: Avatar with overlapping bottom-right Flag, Name, Verified badge, Location/Time, Follow button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                com.example.ui.components.PresenceAvatar(
                    avatarUrl = moment.author.avatar_url,
                    size = 40.dp,
                    userId = moment.author.id,
                    countryCode = flagCode.takeIf { it.isNotBlank() },
                    showFlagBadge = flagCode.isNotBlank(),
                    enableFrameAnimation = true
                )

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.example.store.DecoratedUserName(
                            userId = moment.author.id,
                            name = authorName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            defaultColor = overlayInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (moment.author.is_verified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_verified),
                                contentDescription = "Verified",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    if (subLocationLine.isNotBlank()) {
                        Text(
                            text = subLocationLine,
                            fontSize = 11.sp,
                            color = overlaySubInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!moment.author.is_me) {
                    OutlinedButton(
                        onClick = onToggleFollow,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, Color(0xFFFFB627)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (moment.author.following) Color(0xFFFFB627) else Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = if (moment.author.following) "Following" else "Follow",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (moment.author.following) Color(0xFF2B2118) else Color(0xFFFFB627)
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
                    val cleanMomentLang = MomentsRepository.stripLanguageLevel(moment.language).ifBlank { "English" }
                    val nativeLang = MomentsRepository.stripLanguageLevel(moment.author.native_languages.firstOrNull())
                    val langBadgeText = when {
                        isNative -> "Native"
                        nativeLang.isNotBlank() -> "$nativeLang → $cleanMomentLang"
                        else -> cleanMomentLang
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (useLightUiContrast) Color(0xFFFFECB3) else Color(0xFFFFB627).copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, if (useLightUiContrast) Color(0xFFD97706) else Color(0xFFFFB627).copy(alpha = 0.7f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            CircleFlag(
                                countryCode = LanguageData.getFlagCodeForLanguage(cleanMomentLang),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = langBadgeText,
                                color = if (useLightUiContrast) Color(0xFF78350F) else Color(0xFFFFE082),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                items(moment.topics) { slug ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (useLightUiContrast) funkyColors.surface.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.18f),
                        border = if (useLightUiContrast) BorderStroke(1.dp, funkyColors.line) else null
                    ) {
                        Text(
                            text = MomentsRepository.resolveTopicChipText(slug),
                            color = if (useLightUiContrast) funkyColors.ink else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MomentCommentsBottomSheet(
    moment: MomentDto,
    currentUserNativeLanguage: String,
    initialTab: Int = 0,
    initialNativeFixMode: Boolean = false,
    onDismiss: () -> Unit,
    onMomentRemoved: () -> Unit,
    onCountsChanged: (commentsCount: Int, correctionsCount: Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val commentInputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val limits = remember { MomentsRepository.getCachedOptions().limits }

    // Warm Cream Palette (with Dark mode support) honoring app theme preference
    val funkyColors = com.example.ui.theme.LocalFunkyColors.current
    val isDark = funkyColors.isDark || com.example.ui.theme.LocalAppDarkTheme.current
    val sheetBg = if (isDark) funkyColors.surface else Color(0xFFFFF8E7)
    val softSurface = if (isDark) funkyColors.soft else Color(0xFFFFEFC2)
    val activeTabBg = if (isDark) funkyColors.bg else Color.White
    val inkColor = if (isDark) funkyColors.ink else Color(0xFF2B2118)
    val muteColor = if (isDark) funkyColors.mute else Color(0xFF8A7B66)
    val lineColor = if (isDark) funkyColors.line else Color(0xFFF0E3C4)
    val mangoColor = funkyColors.mango

    // Sub-tab inside sheet: 0 = "Comments" (all), 1 = "Corrections" (corrections)
    var selectedSheetTab by remember(moment.id, initialTab) { mutableIntStateOf(initialTab) }
    val kindParam = if (selectedSheetTab == 0) "all" else "corrections"

    val blockedUserIds by com.example.repository.BlockRepository.blockedUserIds.collectAsState()
    val commentsList = remember(moment.id) { mutableStateListOf<MomentCommentDto>() }
    val visibleComments = remember(commentsList.toList(), blockedUserIds) {
        commentsList.filterNot { blockedUserIds.contains(it.author.id) }
    }
    var isLoadingComments by remember { mutableStateOf(false) }
    var isLoadingMoreComments by remember { mutableStateOf(false) }
    var hasMoreComments by remember { mutableStateOf(true) }
    var commentsOffset by remember { mutableIntStateOf(0) }
    var commentsError by remember { mutableStateOf<String?>(null) }

    val expandedRepliesMap = remember(moment.id) { mutableStateMapOf<String, List<MomentCommentDto>>() }
    val loadingRepliesIds = remember(moment.id) { mutableStateMapOf<String, Boolean>() }
    val translatedMap = remember(moment.id) { mutableStateMapOf<String, String>() }

    // Available verbatim post segments for selecting original text in corrections
    val postSegments = remember(moment.id, moment.title, moment.content, moment.translation) {
        listOfNotNull(
            moment.title?.takeIf { it.isNotBlank() }?.let { "Title" to it },
            moment.content?.takeIf { it.isNotBlank() }?.let { "Post" to it },
            moment.translation?.takeIf { it.isNotBlank() }?.let { "Translation" to it }
        )
    }

    var replyingTo by remember { mutableStateOf<MomentCommentDto?>(null) }
    var newCommentText by remember { mutableStateOf("") }
    var isNativeFixMode by remember(moment.id, initialNativeFixMode) {
        mutableStateOf(initialNativeFixMode && !moment.author.is_me && postSegments.isNotEmpty())
    }
    var fixOriginalInput by remember(moment.id) {
        mutableStateOf(
            if (initialNativeFixMode && !moment.author.is_me && postSegments.isNotEmpty()) {
                postSegments.first().second.take(limits.correction_text)
            } else ""
        )
    }
    var fixCorrectedInput by remember(moment.id) {
        mutableStateOf(fixOriginalInput)
    }
    var fixTipInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

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
            val commentAuthorIds = list.map { it.author.id }.filter { it.isNotBlank() }.distinct()
            if (commentAuthorIds.isNotEmpty()) {
                coroutineScope.launch {
                    com.example.store.CosmeticsManager.batchFetchUserCosmetics(context, commentAuthorIds)
                }
            }
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
        containerColor = sheetBg,
        contentColor = inkColor,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(lineColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .padding(horizontal = 16.dp)
        ) {
            // Top Pill Tab Switcher + Circular Close Button (Exact screenshot header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = softSurface
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab 0: Comments
                        Surface(
                            onClick = { selectedSheetTab = 0 },
                            shape = RoundedCornerShape(18.dp),
                            color = if (selectedSheetTab == 0) activeTabBg else Color.Transparent,
                            modifier = Modifier.testTag("comments_tab_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Comments",
                                    fontSize = 13.5.sp,
                                    fontWeight = if (selectedSheetTab == 0) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (selectedSheetTab == 0) inkColor else muteColor
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(mangoColor)
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${moment.comments_count}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2B2118)
                                    )
                                }
                            }
                        }

                        // Tab 1: Corrections
                        Surface(
                            onClick = { selectedSheetTab = 1 },
                            shape = RoundedCornerShape(18.dp),
                            color = if (selectedSheetTab == 1) activeTabBg else Color.Transparent,
                            modifier = Modifier.testTag("corrections_tab_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Corrections",
                                    fontSize = 13.5.sp,
                                    fontWeight = if (selectedSheetTab == 1) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (selectedSheetTab == 1) inkColor else muteColor
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(mangoColor)
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${moment.corrections_count}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2B2118)
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(softSurface)
                        .testTag("comments_sheet_close_button")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = inkColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Comments / Corrections List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoadingComments && visibleComments.isEmpty() -> {
                        Box(
                            modifier = Modifier.align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = mangoColor,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    visibleComments.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(softSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (selectedSheetTab == 1) R.drawable.ic_fix else R.drawable.ic_comment
                                    ),
                                    contentDescription = null,
                                    tint = mangoColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = commentsError ?: if (selectedSheetTab == 1) {
                                    "No corrections yet. Help the author sound natural!"
                                } else {
                                    "No comments yet. Start the conversation!"
                                },
                                color = muteColor,
                                fontSize = 13.5.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    else -> {
                        val commentsListState = androidx.compose.foundation.lazy.rememberLazyListState()
                        LazyColumn(
                            state = commentsListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(visibleComments, key = { it.id }) { comment ->
                                val replies = expandedRepliesMap[comment.id].orEmpty()
                                    .filterNot { blockedUserIds.contains(it.author.id) }
                                val isLoadingReplies = loadingRepliesIds[comment.id] == true

                                CommentItemRow(
                                    comment = comment,
                                    isPostAuthor = moment.author.is_me,
                                    translatedText = translatedMap[comment.id],
                                    replies = replies,
                                    isLoadingReplies = isLoadingReplies,
                                    animateNameEffects = !commentsListState.isScrollInProgress,
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
                                                    val replyAuthorIds = loaded.map { it.author.id }.filter { it.isNotBlank() }.distinct()
                                                    if (replyAuthorIds.isNotEmpty()) {
                                                        coroutineScope.launch {
                                                            com.example.store.CosmeticsManager.batchFetchUserCosmetics(context, replyAuthorIds)
                                                        }
                                                    }
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
                                        commentInputFocusRequester.requestFocus()
                                        keyboardController?.show()
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
                                                color = Color(0xFFD97706),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = lineColor, thickness = 1.dp)

            // Bottom Composer Section: Mode Toggles + Input Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(top = 10.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Comment Mode Toggle Pill
                        val isCommentSelected = !isNativeFixMode
                        Surface(
                            onClick = { isNativeFixMode = false },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isCommentSelected) inkColor else softSurface,
                            modifier = Modifier.testTag("composer_comment_mode_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_comment),
                                    contentDescription = null,
                                    tint = if (isCommentSelected) sheetBg else inkColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Comment",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCommentSelected) sheetBg else inkColor
                                )
                            }
                        }

                        // Native fix Mode Toggle Pill (hidden on user's own posts)
                        if (!moment.author.is_me && postSegments.isNotEmpty()) {
                            Surface(
                                onClick = {
                                    isNativeFixMode = true
                                    replyingTo = null
                                    if (fixOriginalInput.isBlank()) {
                                        val seg = postSegments.getOrNull(selectedSegmentIndex)?.second ?: ""
                                        fixOriginalInput = seg.take(limits.correction_text)
                                        fixCorrectedInput = fixOriginalInput
                                    }
                                },
                                shape = RoundedCornerShape(18.dp),
                                color = if (isNativeFixMode) mangoColor else softSurface,
                                modifier = Modifier.testTag("composer_native_fix_mode_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_fix),
                                        contentDescription = null,
                                        tint = if (isNativeFixMode) Color(0xFF2B2118) else muteColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Native fix",
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isNativeFixMode) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isNativeFixMode) Color(0xFF2B2118) else muteColor
                                    )
                                }
                            }
                        }
                    }

                    if (!isNativeFixMode && replyingTo != null) {
                        val replyName = replyingTo!!.author.name ?: replyingTo!!.author.username ?: "user"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Replying to $replyName",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD97706),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Cancel reply",
                                tint = muteColor,
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
                                val isSegSelected = selectedSegmentIndex == idx
                                Surface(
                                    onClick = {
                                        selectedSegmentIndex = idx
                                        startWordIndex = null
                                        fixOriginalInput = segText.take(limits.correction_text)
                                        fixCorrectedInput = fixOriginalInput
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSegSelected) mangoColor else softSurface
                                ) {
                                    Text(
                                        text = segLabel,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSegSelected) Color(0xFF2B2118) else muteColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    val activeSegmentText = postSegments.getOrNull(selectedSegmentIndex)?.second ?: ""
                    val words = remember(activeSegmentText) { tokenizeWordsWithSpans(activeSegmentText) }

                    // Tappable word-range selector card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = softSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tap 1 or 2 words to fix:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = muteColor
                                )
                                Text(
                                    text = "Whole line",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.clickable {
                                        startWordIndex = null
                                        fixOriginalInput = activeSegmentText.take(limits.correction_text)
                                        fixCorrectedInput = fixOriginalInput
                                    }
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
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
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isInSelection) mangoColor else activeTabBg
                                    ) {
                                        Text(
                                            text = span.word,
                                            fontSize = 12.sp,
                                            fontWeight = if (isInSelection) FontWeight.Bold else FontWeight.Medium,
                                            color = inkColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (fixOriginalInput.isNotBlank()) {
                                Text(
                                    text = fixOriginalInput,
                                    fontSize = 12.5.sp,
                                    color = muteColor,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                        }
                    }

                    // Corrected phrase pill input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(softSurface)
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        BasicTextField(
                            value = fixCorrectedInput,
                            onValueChange = { if (it.length <= limits.correction_text) fixCorrectedInput = it },
                            textStyle = TextStyle(
                                color = inkColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(mangoColor),
                            decorationBox = { inner ->
                                if (fixCorrectedInput.isEmpty()) {
                                    Text(
                                        text = "Corrected phrase (${fixCorrectedInput.length}/${limits.correction_text})",
                                        color = muteColor,
                                        fontSize = 13.5.sp
                                    )
                                }
                                inner()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Explanation input + Send circular button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(softSurface)
                                .padding(horizontal = 18.dp, vertical = 13.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = fixTipInput,
                                onValueChange = { if (it.length <= limits.comment) fixTipInput = it },
                                textStyle = TextStyle(
                                    color = inkColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(mangoColor),
                                decorationBox = { inner ->
                                    if (fixTipInput.isEmpty()) {
                                        Text(
                                            text = "Explanation (optional)",
                                            color = muteColor,
                                            fontSize = 13.5.sp
                                        )
                                    }
                                    inner()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        val canSendFix = !isSubmitting && fixOriginalInput.isNotBlank() && fixCorrectedInput.isNotBlank()
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (canSendFix) mangoColor else mangoColor.copy(alpha = 0.45f))
                                .clickable(enabled = canSendFix) {
                                    if (isSubmitting) return@clickable
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
                                            selectedSheetTab = 1
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
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color(0xFF2B2118),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_share),
                                    contentDescription = "Send correction",
                                    tint = Color(0xFF2B2118),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(softSurface)
                                .clickable {
                                    commentInputFocusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = newCommentText,
                                onValueChange = { if (it.length <= limits.comment) newCommentText = it },
                                textStyle = TextStyle(
                                    color = inkColor,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(mangoColor),
                                decorationBox = { inner ->
                                    if (newCommentText.isEmpty()) {
                                        Text(
                                            text = if (replyingTo != null) "Write a reply..." else "Add a comment...",
                                            color = muteColor,
                                            fontSize = 14.5.sp
                                        )
                                    }
                                    inner()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(commentInputFocusRequester)
                                    .testTag("comment_input_field")
                            )
                        }

                        val canSendComment = !isSubmitting && newCommentText.isNotBlank()
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (canSendComment) mangoColor else mangoColor.copy(alpha = 0.85f))
                                .clickable(enabled = canSendComment) {
                                    if (isSubmitting || newCommentText.isBlank()) return@clickable
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
                                }
                                .testTag("comment_send_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color(0xFF2B2118),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_share),
                                    contentDescription = "Send",
                                    tint = Color(0xFF2B2118),
                                    modifier = Modifier.size(20.dp)
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
fun CommentItemRow(
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
    isNestedReply: Boolean = false,
    animateNameEffects: Boolean = true
) {
    val context = LocalContext.current
    val funkyColors = com.example.ui.theme.LocalFunkyColors.current
    val isDark = funkyColors.isDark || com.example.ui.theme.LocalAppDarkTheme.current
    val inkColor = if (isDark) funkyColors.ink else Color(0xFF2B2118)
    val muteColor = if (isDark) funkyColors.mute else Color(0xFF8A7B66)
    val cardBg = if (isDark) funkyColors.soft else Color(0xFFFFEFC2)
    val mangoColor = funkyColors.mango

    val authorName = comment.author.name?.takeIf { it.isNotBlank() }
        ?: comment.author.username?.takeIf { it.isNotBlank() }
        ?: "User"
    val timeText = formatMomentTimestamp(comment.created_at)
    val isCorrection = comment.kind == "correction"

    val avatarSize = if (isNestedReply) 30.dp else 38.dp
    val avatarCorner = if (isNestedReply) 11.dp else 14.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isNestedReply) 44.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    .size(avatarSize)
                    .clip(RoundedCornerShape(avatarCorner))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(RoundedCornerShape(avatarCorner))
                    .background(mangoColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = authorName.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2B2118)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                com.example.store.DecoratedUserName(
                    userId = comment.author.id,
                    name = authorName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    defaultColor = inkColor,
                    animate = animateNameEffects
                )
                Text(
                    text = timeText,
                    fontSize = 12.sp,
                    color = muteColor
                )
                if (isCorrection) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = mangoColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_fix),
                                contentDescription = null,
                                tint = Color(0xFF2B2118),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Native fix",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2B2118)
                            )
                        }
                    }
                }
                if (isCorrection && comment.is_accepted) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF16A34A).copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            SoftCheckIcon(size = 10.dp, color = Color(0xFF15803D))
                            Text(
                                text = "Accepted",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            if (isCorrection) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (!comment.original_text.isNullOrBlank()) {
                            Text(
                                text = comment.original_text,
                                fontSize = 14.sp,
                                color = muteColor,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                        if (!comment.corrected_text.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = mangoColor
                            ) {
                                Text(
                                    text = comment.corrected_text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2B2118),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (!comment.content.isNullOrBlank()) {
                            Text(
                                text = comment.content,
                                fontSize = 13.sp,
                                color = muteColor,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = comment.content.orEmpty(),
                    fontSize = 14.sp,
                    color = inkColor,
                    lineHeight = 19.sp
                )
            }

            if (!translatedText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg
                ) {
                    Text(
                        text = translatedText,
                        fontSize = 12.5.sp,
                        color = inkColor,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Action row matching screenshots verbatim: Heart+Like, Reply, Translate, Accept/Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onToggleLike(comment) }
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (comment.liked_by_me) R.drawable.ic_heart_filled else R.drawable.ic_heart
                        ),
                        contentDescription = "Like comment",
                        tint = if (comment.liked_by_me) Color(0xFFEF4444) else muteColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (comment.likes_count > 0) "${comment.likes_count}" else "Like",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (comment.liked_by_me) Color(0xFFEF4444) else muteColor
                    )
                }

                Text(
                    text = "Reply",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = muteColor,
                    modifier = Modifier.clickable { onReply(comment) }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onTranslate(comment) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_translate),
                        contentDescription = "Translate comment",
                        tint = muteColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Translate",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = muteColor
                    )
                }

                if (isCorrection && isPostAuthor) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.clickable { onAcceptCorrection(comment) }
                    ) {
                        Text(
                            text = if (comment.is_accepted) "Accepted" else "Accept fix",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (comment.is_accepted) Color(0xFF16A34A) else Color(0xFFD97706)
                        )
                        if (comment.is_accepted) {
                            SoftCheckIcon(size = 11.dp, color = Color(0xFF16A34A))
                        }
                    }
                }

                if (comment.author.is_me || isPostAuthor) {
                    Text(
                        text = "Delete",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFEF4444),
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706),
                    modifier = Modifier.clickable { onLoadReplies(comment) }
                )
            }

            if (replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            animateNameEffects = animateNameEffects,
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
fun MomentVideoPlayerDialog(
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
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Close video",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Full-screen Moments v2 feed viewer opened from Profile / UserProfileFeedScreen grid items.
 * Supports vertical swiping across profile posts, live comments/corrections sheet, like, bookmark,
 * share, photo preview, video playback, delete post (if own), not-interested, and report.
 */
@Composable
fun MomentsV2FeedViewerDialog(
    moments: List<MomentDto>,
    initialMomentId: String,
    currentUserNativeLanguage: String = "",
    isSelfProfile: Boolean = false,
    onDismiss: () -> Unit,
    onMomentUpdated: (MomentDto) -> Unit = {},
    onMomentDeleted: (String) -> Unit = {},
    onBookmarkToggled: (String, Boolean) -> Unit = { _, _ -> }
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember(context) {
        context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
    }
    val bookmarkedIds = remember {
        mutableStateListOf<String>().apply {
            addAll(prefs.getStringSet("bookmarked_moment_ids", emptySet()).orEmpty())
        }
    }

    val blockedUserIds by com.example.repository.BlockRepository.blockedUserIds.collectAsState()
    val localMoments = remember {
        mutableStateListOf<MomentDto>().apply { addAll(moments) }
    }
    LaunchedEffect(moments) {
        val currentIds = localMoments.map { it.id }.toSet()
        val incomingIds = moments.map { it.id }.toSet()
        if (currentIds != incomingIds) {
            localMoments.clear()
            localMoments.addAll(moments)
        }
    }
    val visibleMoments = remember(localMoments.toList(), blockedUserIds) {
        localMoments.filterNot { blockedUserIds.contains(it.author.id) }
    }

    if (visibleMoments.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val startIdx = remember(initialMomentId, visibleMoments) {
        visibleMoments.indexOfFirst { it.id == initialMomentId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = startIdx,
        pageCount = { visibleMoments.size }
    )

    var showCommentsSheet by remember { mutableStateOf(false) }
    var commentsInitialTab by remember { mutableIntStateOf(0) }
    var commentsInitialFixMode by remember { mutableStateOf(false) }
    var activeMomentIdForComments by remember { mutableStateOf<String?>(null) }
    var activeVideoUrl by remember { mutableStateOf<String?>(null) }
    var previewImagesState by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var reportingMoment by remember { mutableStateOf<MomentDto?>(null) }
    var showTopMenu by remember { mutableStateOf(false) }

    val currentMoment = visibleMoments.getOrNull(pagerState.currentPage)

    // Refresh current visible moment from Supabase RPC on swipe/open
    LaunchedEffect(currentMoment?.id) {
        val targetId = currentMoment?.id ?: return@LaunchedEffect
        MomentsRepository.getMoment(context, targetId)
            .onSuccess { updated ->
                val idx = localMoments.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    localMoments[idx] = updated
                }
                onMomentUpdated(updated)
            }
            .onFailure { err ->
                if ((err as? MomentRpcException)?.isNotFound == true) {
                    Toast.makeText(context, err.message, Toast.LENGTH_SHORT).show()
                    localMoments.removeAll { it.id == targetId }
                    onMomentDeleted(targetId)
                    if (localMoments.isEmpty()) onDismiss()
                }
            }
    }

    fun updateLocalMoment(momentId: String, transform: (MomentDto) -> MomentDto) {
        val idx = localMoments.indexOfFirst { it.id == momentId }
        if (idx != -1) {
            val updated = transform(localMoments[idx])
            localMoments[idx] = updated
            onMomentUpdated(updated)
        }
    }

    fun removeLocalMoment(momentId: String) {
        localMoments.removeAll { it.id == momentId }
        onMomentDeleted(momentId)
        if (activeMomentIdForComments == momentId) {
            showCommentsSheet = false
            activeMomentIdForComments = null
        }
        if (localMoments.isEmpty()) {
            onDismiss()
        }
    }

    val funkyColors = com.example.ui.theme.LocalFunkyColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(funkyColors.bg)
                .navigationBarsPadding()
                .testTag("profile_moments_v2_viewer")
        ) {
            VerticalPager(
                state = pagerState,
                key = { idx -> visibleMoments.getOrNull(idx)?.id ?: idx },
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val moment = visibleMoments.getOrNull(pageIndex) ?: return@VerticalPager
                val isBookmarked = bookmarkedIds.contains(moment.id)

                MomentV2Slide(
                    moment = moment,
                    isBookmarked = isBookmarked,
                    contentBottomPadding = 24.dp,
                    onOpenComments = {
                        activeMomentIdForComments = moment.id
                        commentsInitialTab = 0
                        commentsInitialFixMode = false
                        showCommentsSheet = true
                    },
                    onOpenCorrections = {
                        activeMomentIdForComments = moment.id
                        commentsInitialTab = 1
                        commentsInitialFixMode = !(moment.author.is_me || isSelfProfile)
                        showCommentsSheet = true
                    },
                    onPlayVideo = { url -> activeVideoUrl = url },
                    onToggleFollow = {
                        if (moment.author.is_me || isSelfProfile) return@MomentV2Slide
                        val prevFollowing = moment.author.following
                        val optimisticNext = !prevFollowing
                        for (i in localMoments.indices) {
                            val m = localMoments[i]
                            if (m.author.id == moment.author.id) {
                                localMoments[i] = m.copy(author = m.author.copy(following = optimisticNext))
                            }
                        }
                        coroutineScope.launch {
                            MomentsRepository.toggleFollowUser(
                                context = context,
                                targetUserId = moment.author.id,
                                currentlyFollowing = prevFollowing
                            ).onSuccess { nowFollowing ->
                                for (i in localMoments.indices) {
                                    val m = localMoments[i]
                                    if (m.author.id == moment.author.id) {
                                        localMoments[i] = m.copy(author = m.author.copy(following = nowFollowing))
                                    }
                                }
                            }.onFailure { err ->
                                for (i in localMoments.indices) {
                                    val m = localMoments[i]
                                    if (m.author.id == moment.author.id) {
                                        localMoments[i] = m.copy(author = m.author.copy(following = prevFollowing))
                                    }
                                }
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
                        updateLocalMoment(moment.id) {
                            it.copy(liked_by_me = nextLiked, likes_count = nextCount)
                        }
                        coroutineScope.launch {
                            MomentsRepository.toggleMomentLike(context, moment.id)
                                .onSuccess { resp ->
                                    updateLocalMoment(moment.id) {
                                        it.copy(liked_by_me = resp.liked, likes_count = resp.likes_count)
                                    }
                                }
                                .onFailure { err ->
                                    if ((err as? MomentRpcException)?.isNotFound == true) {
                                        Toast.makeText(context, err.message, Toast.LENGTH_SHORT).show()
                                        removeLocalMoment(moment.id)
                                    } else {
                                        updateLocalMoment(moment.id) {
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
                        val nextState = !isBookmarked
                        if (isBookmarked) bookmarkedIds.remove(moment.id) else bookmarkedIds.add(moment.id)
                        prefs.edit()
                            .putStringSet("bookmarked_moment_ids", bookmarkedIds.toSet())
                            .apply()
                        onBookmarkToggled(moment.id, nextState)
                        Toast.makeText(
                            context,
                            if (nextState) "Saved to Bookmarks" else "Removed from Bookmarks",
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
                    },
                    onMediaImageClick = { urls, index ->
                        previewImagesState = urls to index
                    }
                )
            }

            // Top Floating Navigation Row (Back + Post indicator + More menu)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .testTag("profile_viewer_back_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Moments",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Box {
                    IconButton(
                        onClick = { showTopMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more_vert),
                            contentDescription = "Post options",
                            tint = Color.White,
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
                        if (currentMoment != null) {
                            val canDelete = currentMoment.author.is_me || isSelfProfile
                            if (!canDelete) {
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
                                        val targetId = currentMoment.id
                                        removeLocalMoment(targetId)
                                        coroutineScope.launch {
                                            MomentsRepository.markMomentNotInterested(context, targetId)
                                        }
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = funkyColors.line.copy(alpha = 0.6f)
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
                                        reportingMoment = currentMoment
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
                                        val targetId = currentMoment.id
                                        coroutineScope.launch {
                                            MomentsRepository.deleteMoment(context, targetId)
                                                .onSuccess {
                                                    removeLocalMoment(targetId)
                                                    Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                                                }
                                                .onFailure { err ->
                                                    if ((err as? MomentRpcException)?.isNotFound == true) {
                                                        removeLocalMoment(targetId)
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
    }

    val commentMoment = remember(activeMomentIdForComments, localMoments.toList()) {
        activeMomentIdForComments?.let { id -> localMoments.firstOrNull { it.id == id } }
    }
    if (showCommentsSheet && commentMoment != null) {
        MomentCommentsBottomSheet(
            moment = commentMoment,
            currentUserNativeLanguage = currentUserNativeLanguage,
            initialTab = commentsInitialTab,
            initialNativeFixMode = commentsInitialFixMode,
            onDismiss = {
                showCommentsSheet = false
                activeMomentIdForComments = null
            },
            onMomentRemoved = { removeLocalMoment(commentMoment.id) },
            onCountsChanged = { newCommentsCount, newCorrectionsCount ->
                updateLocalMoment(commentMoment.id) {
                    it.copy(
                        comments_count = newCommentsCount.coerceAtLeast(0),
                        corrections_count = newCorrectionsCount.coerceAtLeast(0)
                    )
                }
            }
        )
    }

    activeVideoUrl?.let { videoUrl ->
        MomentVideoPlayerDialog(
            videoUrl = videoUrl,
            onDismiss = { activeVideoUrl = null }
        )
    }

    previewImagesState?.let { (urls, idx) ->
        ChatImagePreviewDialog(
            imageUrls = urls,
            initialIndex = idx,
            isMyImage = isSelfProfile,
            captionText = currentMoment?.content.orEmpty(),
            senderName = currentMoment?.author?.name ?: currentMoment?.author?.username ?: "",
            onDismiss = { previewImagesState = null }
        )
    }

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
