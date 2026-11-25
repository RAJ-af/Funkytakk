package com.example.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.AsyncImage
import com.example.R
import com.example.db.LocalGroupMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

data class ChatPhotoNode(
    val id: String,
    val source: String,
    val caption: String?,
    val isDownloading: Boolean,
    val isAutoDownloadPaused: Boolean = false,
    val downloadState: String? = null,
    val failReason: String? = null,
    val timeText: String,
    val isOutgoing: Boolean
)

fun isDmImageText(rawText: String): Boolean =
    rawText.startsWith("[img]") || rawText.startsWith("[imgs]")

fun parseDmPhotoNode(
    id: String,
    rawText: String,
    timestamp: Long,
    isOutgoing: Boolean,
    downloadState: String? = null,
    failReason: String? = null
): ChatPhotoNode {
    val stripped = rawText.removePrefix("[img]").removePrefix("[imgs]")
    val source = stripped.substringBefore("|").trim()
    val caption = stripped.substringAfter("|", "").trim().takeIf { it.isNotBlank() }
    val hasLocalSource = source.isNotBlank() && source != "downloading..." && !source.startsWith("⚠️")
    val resolvedState = when {
        hasLocalSource -> com.example.db.DmMediaDownloadState.READY
        !downloadState.isNullOrBlank() -> downloadState
        else -> com.example.db.DmMediaDownloadState.DOWNLOADING
    }
    return ChatPhotoNode(
        id = id,
        source = if (hasLocalSource) source else "",
        caption = caption,
        isDownloading = !hasLocalSource && (resolvedState == com.example.db.DmMediaDownloadState.DOWNLOADING || resolvedState == com.example.db.DmMediaDownloadState.PENDING),
        isAutoDownloadPaused = !hasLocalSource && resolvedState == com.example.db.DmMediaDownloadState.PENDING,
        downloadState = resolvedState,
        failReason = failReason,
        timeText = formatChatTime(timestamp),
        isOutgoing = isOutgoing
    )
}

fun isGroupImageMessage(msg: LocalGroupMessage): Boolean =
    msg.messageType == "image" || msg.text.startsWith("[img]") || msg.text.startsWith("[imgs]")

fun parseGroupPhotoNode(
    msg: LocalGroupMessage,
    isOutgoing: Boolean,
    groupState: Pair<String, String?>? = null
): ChatPhotoNode {
    val rawText = msg.text
    val fallbackSource = rawText.removePrefix("[img]").removePrefix("[imgs]").substringBefore("|")
        .takeIf { it.startsWith("file://") || it.startsWith("http") || it.startsWith("/") }
        .orEmpty()
    val resolvedSource = msg.localMediaPath?.takeIf { it.isNotBlank() } ?: fallbackSource
    val isMissingLocal = resolvedSource.isBlank() || resolvedSource == "downloading..."
    val hasRemoteKey = !msg.storagePath.isNullOrBlank() && !msg.mediaKey.isNullOrBlank()
    val resolvedState = when {
        !isMissingLocal -> com.example.db.DmMediaDownloadState.READY
        groupState != null -> groupState.first
        hasRemoteKey -> com.example.db.DmMediaDownloadState.PENDING
        else -> com.example.db.DmMediaDownloadState.DOWNLOADING
    }
    val caption = when {
        rawText.startsWith("[img]") || rawText.startsWith("[imgs]") ->
            rawText.substringAfter("|", "").trim().takeIf { it.isNotBlank() }
        rawText.isNotBlank() && !rawText.equals("Photo", ignoreCase = true) -> rawText.trim()
        else -> null
    }
    return ChatPhotoNode(
        id = msg.id,
        source = if (isMissingLocal) "" else resolvedSource,
        caption = caption,
        isDownloading = isMissingLocal && resolvedState == com.example.db.DmMediaDownloadState.DOWNLOADING,
        isAutoDownloadPaused = isMissingLocal && resolvedState == com.example.db.DmMediaDownloadState.PENDING,
        downloadState = resolvedState,
        failReason = groupState?.second,
        timeText = formatChatTime(msg.timestamp),
        isOutgoing = isOutgoing
    )
}

data class ChatVideoSpec(
    val localPath: String?,
    val durationSec: Int,
    val caption: String?,
    val downloadState: String? = null,
    val failReason: String? = null
)

data class ChatAudioSpec(
    val localPath: String?,
    val durationSec: Int,
    val title: String,
    val downloadState: String? = null,
    val failReason: String? = null
)

fun parseDmVideoSpec(msg: com.example.db.LocalDmMessage): ChatVideoSpec {
    val parts = msg.text.removePrefix("[vid]").split("|")
    val rawSrc = parts.getOrNull(0)?.trim().orEmpty()
    val pathCandidate = msg.localMediaPath?.takeIf { it.isNotBlank() }
        ?: rawSrc.takeIf { it.isNotBlank() && it != "downloading..." && !it.startsWith("⚠️") }
    val cleanPath = pathCandidate?.removePrefix("file://")?.takeIf {
        val f = File(it)
        f.exists() && f.length() > 0L
    }
    val duration = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(0) ?: (msg.mediaDurationSec ?: 0)
    val caption = parts.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
        ?: msg.mediaCaption?.takeIf { it.isNotBlank() }
    val state = if (cleanPath != null) {
        com.example.db.DmMediaDownloadState.READY
    } else {
        msg.mediaDownloadState ?: com.example.db.DmMediaDownloadState.PENDING
    }
    return ChatVideoSpec(
        localPath = cleanPath,
        durationSec = duration,
        caption = caption,
        downloadState = state,
        failReason = msg.mediaFailReason
    )
}

fun parseDmAudioSpec(msg: com.example.db.LocalDmMessage): ChatAudioSpec {
    val parts = msg.text.removePrefix("[audio]").split("|")
    val rawSrc = parts.getOrNull(0)?.trim().orEmpty()
    val pathCandidate = msg.localMediaPath?.takeIf { it.isNotBlank() }
        ?: rawSrc.takeIf { it.isNotBlank() && it != "downloading..." && !it.startsWith("⚠️") }
    val cleanPath = pathCandidate?.removePrefix("file://")?.takeIf {
        val f = File(it)
        f.exists() && f.length() > 0L
    }
    val duration = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(0) ?: (msg.mediaDurationSec ?: 0)
    val title = parts.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
        ?: msg.mediaCaption?.takeIf { it.isNotBlank() }
        ?: "Audio file"
    val state = if (cleanPath != null) {
        com.example.db.DmMediaDownloadState.READY
    } else {
        msg.mediaDownloadState ?: com.example.db.DmMediaDownloadState.PENDING
    }
    return ChatAudioSpec(
        localPath = cleanPath,
        durationSec = duration,
        title = title,
        downloadState = state,
        failReason = msg.mediaFailReason
    )
}

fun parseGroupVideoSpec(
    msg: LocalGroupMessage,
    groupState: Pair<String, String?>? = null
): ChatVideoSpec {
    val cleanPath = msg.localMediaPath?.takeIf {
        it.isNotBlank() && File(it.removePrefix("file://")).let { f -> f.exists() && f.length() > 0L }
    }?.removePrefix("file://")
    val parts = msg.text.removePrefix("[vid]").split("|")
    val duration = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val caption = parts.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
    val state = when {
        cleanPath != null -> com.example.db.DmMediaDownloadState.READY
        groupState != null -> groupState.first
        !msg.storagePath.isNullOrBlank() && !msg.mediaKey.isNullOrBlank() -> com.example.db.DmMediaDownloadState.PENDING
        else -> com.example.db.DmMediaDownloadState.DOWNLOADING
    }
    return ChatVideoSpec(
        localPath = cleanPath,
        durationSec = duration,
        caption = caption,
        downloadState = state,
        failReason = groupState?.second
    )
}

fun parseGroupAudioSpec(
    msg: LocalGroupMessage,
    groupState: Pair<String, String?>? = null
): ChatAudioSpec {
    val cleanPath = msg.localMediaPath?.takeIf {
        it.isNotBlank() && File(it.removePrefix("file://")).let { f -> f.exists() && f.length() > 0L }
    }?.removePrefix("file://")
    val parts = msg.text.removePrefix("[audio]").split("|")
    val duration = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val title = parts.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() } ?: "Audio file"
    val state = when {
        cleanPath != null -> com.example.db.DmMediaDownloadState.READY
        groupState != null -> groupState.first
        !msg.storagePath.isNullOrBlank() && !msg.mediaKey.isNullOrBlank() -> com.example.db.DmMediaDownloadState.PENDING
        else -> com.example.db.DmMediaDownloadState.DOWNLOADING
    }
    return ChatAudioSpec(
        localPath = cleanPath,
        durationSec = duration,
        title = title,
        downloadState = state,
        failReason = groupState?.second
    )
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ChatAttachmentChooserSheet(
    onPickPhotos: () -> Unit,
    onPickVideo: () -> Unit,
    onPickAudio: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalChatColors.current
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        contentColor = c.text,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Send media",
                color = c.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
            Surface(
                onClick = {
                    onDismiss()
                    onPickPhotos()
                },
                shape = RoundedCornerShape(14.dp),
                color = c.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .testTag("attach_option_photos")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ChatIconPhoto(tint = c.accentIn, modifier = Modifier.size(22.dp))
                    Column {
                        Text("Photos", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Pick up to 10 photos", color = c.textSecondary, fontSize = 12.sp)
                    }
                }
            }
            Surface(
                onClick = {
                    onDismiss()
                    onPickVideo()
                },
                shape = RoundedCornerShape(14.dp),
                color = c.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .testTag("attach_option_video")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ChatIconVideo(tint = c.accentIn, modifier = Modifier.size(22.dp))
                    Column {
                        Text("Video", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Compressed to 720p (max 20 MB)", color = c.textSecondary, fontSize = 12.sp)
                    }
                }
            }
            Surface(
                onClick = {
                    onDismiss()
                    onPickAudio()
                },
                shape = RoundedCornerShape(14.dp),
                color = c.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .testTag("attach_option_audio")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ChatIconMusic(tint = c.accentIn, modifier = Modifier.size(22.dp))
                    Column {
                        Text("Audio File", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("MP3, M4A, AAC, or OGG (max 20 MB)", color = c.textSecondary, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

/**
 * Safely prepares an image Uri for E2EE upload: keeps photos compact (< ~2048px)
 * so sending 10-photo albums stays fast and memory-safe while preserving crisp quality.
 */
suspend fun readChatImageBytes(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
        if (rawBytes.isEmpty()) return@withContext null
        if (rawBytes.size <= 1_600_000) return@withContext rawBytes

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        if (maxDim <= 0) return@withContext rawBytes

        var sample = 1
        while (maxDim / sample > 2048) sample *= 2
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
            ?: return@withContext rawBytes

        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 86, out)
        bmp.recycle()
        val compressed = out.toByteArray()
        if (compressed.isNotEmpty() && compressed.size < rawBytes.size) compressed else rawBytes
    } catch (_: Exception) {
        null
    }
}

/**
 * Telegram-style full-screen preview & caption sheet shown after picking 1-10 photos.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelegramPendingMediaDialog(
    uris: List<Uri>,
    recipientLabel: String,
    initialCaption: String = "",
    maxSelection: Int = 10,
    onDismiss: () -> Unit,
    onRemoveAt: (Int) -> Unit,
    onAddMore: () -> Unit,
    onSend: (uris: List<Uri>, caption: String) -> Unit = { _, _ -> },
    onSendWithOptions: ((uris: List<Uri>, caption: String, rotations: Map<Int, Int>, isHd: Boolean) -> Unit)? = null
) {
    if (uris.isEmpty()) {
        onDismiss()
        return
    }

    val c = LocalChatColors.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { uris.size })
    val thumbListState = rememberLazyListState()
    var caption by remember(initialCaption) { mutableStateOf(initialCaption) }
    var isHd by remember { mutableStateOf(false) }
    val rotations = remember { mutableStateMapOf<Int, Int>() }

    LaunchedEffect(uris.size) {
        if (pagerState.currentPage >= uris.size && uris.isNotEmpty()) {
            pagerState.scrollToPage(uris.size - 1)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage in uris.indices) {
            thumbListState.animateScrollToItem(pagerState.currentPage)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            @Suppress("DEPRECATION")
            dialogWindow?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0D14))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            ) {
                // Top Chrome Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pending_media_close_button")
                    ) {
                        ChatIconBack(tint = Color.White, modifier = Modifier.size(22.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recipientLabel.ifBlank { "Send Photos" },
                            color = Color.White,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${(pagerState.currentPage + 1).coerceAtMost(uris.size)} of ${uris.size} · ${if (isHd) "HD" else "Standard"}",
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // HD / Standard Quality Toggle
                    Surface(
                        onClick = { isHd = !isHd },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isHd) c.brand else Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(
                            1.dp,
                            if (isHd) c.brand else Color.White.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("pending_media_hd_toggle")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isHd) "HD" else "SD",
                                color = if (isHd) c.onBrand else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Rotate 90° button
                    IconButton(
                        onClick = {
                            val page = pagerState.currentPage
                            val currentRot = rotations[page] ?: 0
                            rotations[page] = (currentRot + 90) % 360
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("pending_media_rotate_button")
                    ) {
                        ChatIconRotate(tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    if (uris.size < maxSelection) {
                        Surface(
                            onClick = onAddMore,
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("pending_media_add_more_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_plus),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Add",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            val idx = pagerState.currentPage
                            rotations.remove(idx)
                            onRemoveAt(idx)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("pending_media_remove_current_button")
                    ) {
                        ChatIconTrash(tint = Color(0xFFF87171), modifier = Modifier.size(19.dp))
                    }
                }

                // Swipeable Photo Preview Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { page ->
                    val uri = uris.getOrNull(page)
                    val rotDeg = (rotations[page] ?: 0).toFloat()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF101520)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uri != null) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected photo ${page + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationZ = rotDeg },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // Bottom Telegram Thumbnail Strip + Caption + Send Bar
                Surface(
                    color = Color(0xFF121722),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 12.dp)
                    ) {
                        if (uris.size > 1) {
                            LazyRow(
                                state = thumbListState,
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                itemsIndexed(uris) { index, uri ->
                                    val isActive = pagerState.currentPage == index
                                    val rotDeg = (rotations[index] ?: 0).toFloat()
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .scale(if (isActive) 1.04f else 0.96f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(
                                                    width = if (isActive) 2.2.dp else 1.dp,
                                                    color = if (isActive) c.brand else Color.White.copy(alpha = 0.18f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    scope.launch { pagerState.animateScrollToPage(index) }
                                                }
                                        ) {
                                            AsyncImage(
                                                model = uri,
                                                contentDescription = "Thumbnail ${index + 1}",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer { rotationZ = rotDeg },
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        // Remove badge on thumbnail
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(3.dp)
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.72f))
                                                .clickable {
                                                    rotations.remove(index)
                                                    onRemoveAt(index)
                                                }
                                                .semantics { contentDescription = "Remove photo ${index + 1}" },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ChatIconClose(tint = Color.White, modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFF1E2533),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 46.dp)
                            ) {
                                BasicTextField(
                                    value = caption,
                                    onValueChange = { caption = it },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp
                                    ),
                                    cursorBrush = SolidColor(c.brand),
                                    maxLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .testTag("pending_media_caption_input"),
                                    decorationBox = { inner ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (caption.isEmpty()) {
                                                Text(
                                                    text = "Add a caption...",
                                                    color = Color.White.copy(alpha = 0.50f),
                                                    fontSize = 15.sp
                                                )
                                            }
                                            inner()
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier.size(52.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val cleanCaption = caption.trim()
                                        if (onSendWithOptions != null) {
                                            onSendWithOptions(uris, cleanCaption, rotations.toMap(), isHd)
                                        } else {
                                            onSend(uris, cleanCaption)
                                        }
                                    },
                                    shape = CircleShape,
                                    color = c.brand,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("pending_media_send_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        ChatIconSend(tint = c.onBrand, modifier = Modifier.size(21.dp))
                                    }
                                }

                                if (uris.size > 1) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0F172A))
                                            .border(1.5.dp, c.brand, CircleShape)
                                            .padding(horizontal = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${uris.size}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Telegram-style single-photo & grouped multi-photo album collage renderer.
 * Designed to live cleanly inside [MessageBubble] with `PaddingValues(2.dp)`.
 */
@Composable
fun TelegramMediaAlbumContent(
    photos: List<ChatPhotoNode>,
    outgoing: Boolean,
    position: GroupPosition,
    status: MessageStatus?,
    replyQuote: (@Composable () -> Unit)? = null,
    onPhotoClick: (index: Int, photo: ChatPhotoNode) -> Unit,
    onPhotoLongPress: (photo: ChatPhotoNode) -> Unit,
    onDownloadClick: ((photo: ChatPhotoNode) -> Unit)? = null
) {
    if (photos.isEmpty()) return
    val c = LocalChatColors.current
    val caption = remember(photos) {
        photos.firstNotNullOfOrNull { it.caption?.takeIf { c -> c.isNotBlank() } }
    }
    val hasCaption = !caption.isNullOrBlank()
    val hasReply = replyQuote != null
    val lastPhoto = photos.last()

    val outerRadius = ChatDimens.MediaInnerRadius
    val mediaShape = when {
        hasReply && hasCaption -> RoundedCornerShape(10.dp)
        hasReply -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = outerRadius, bottomEnd = outerRadius)
        hasCaption -> RoundedCornerShape(topStart = outerRadius, topEnd = outerRadius, bottomStart = 8.dp, bottomEnd = 8.dp)
        else -> RoundedCornerShape(outerRadius)
    }

    Column(
        modifier = Modifier.widthIn(min = 216.dp, max = ChatDimens.BubbleMaxWidth)
    ) {
        if (replyQuote != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 4.dp)
            ) {
                replyQuote()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(mediaShape)
        ) {
            val gap = 2.dp
            when (photos.size) {
                1 -> {
                    val photo = photos[0]
                    val aspect = rememberPhotoAspectRatio(photo.source)
                    TelegramChatPhotoTile(
                        photo = photo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspect.coerceIn(0.62f, 1.85f)),
                        onClick = { onPhotoClick(0, photo) },
                        onLongPress = { onPhotoLongPress(photo) },
                        onDownloadClick = onDownloadClick?.let { fn -> { fn(photo) } },
                        overlay = {
                            if (!hasCaption) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(7.dp)
                                ) {
                                    MediaTimeChip(
                                        time = photo.timeText,
                                        outgoing = outgoing,
                                        status = status
                                    )
                                }
                            }
                        }
                    )
                }

                2 -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        photos.forEachIndexed { idx, photo ->
                            TelegramChatPhotoTile(
                                photo = photo,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = { onPhotoClick(idx, photo) },
                                onLongPress = { onPhotoLongPress(photo) },
                                onDownloadClick = onDownloadClick?.let { fn -> { fn(photo) } },
                                overlay = {
                                    if (!hasCaption && idx == 1) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                        ) {
                                            MediaTimeChip(time = photo.timeText, outgoing = outgoing, status = status)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                3 -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        TelegramChatPhotoTile(
                            photo = photos[0],
                            modifier = Modifier
                                .weight(1.45f)
                                .fillMaxHeight(),
                            onClick = { onPhotoClick(0, photos[0]) },
                            onLongPress = { onPhotoLongPress(photos[0]) },
                            onDownloadClick = onDownloadClick?.let { fn -> { fn(photos[0]) } }
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            TelegramChatPhotoTile(
                                photo = photos[1],
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                onClick = { onPhotoClick(1, photos[1]) },
                                onLongPress = { onPhotoLongPress(photos[1]) },
                                onDownloadClick = onDownloadClick?.let { fn -> { fn(photos[1]) } }
                            )
                            TelegramChatPhotoTile(
                                photo = photos[2],
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                onClick = { onPhotoClick(2, photos[2]) },
                                onLongPress = { onPhotoLongPress(photos[2]) },
                                onDownloadClick = onDownloadClick?.let { fn -> { fn(photos[2]) } },
                                overlay = {
                                    if (!hasCaption) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                        ) {
                                            MediaTimeChip(time = photos[2].timeText, outgoing = outgoing, status = status)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                4 -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            for (i in 0..1) {
                                TelegramChatPhotoTile(
                                    photo = photos[i],
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClick = { onPhotoClick(i, photos[i]) },
                                    onLongPress = { onPhotoLongPress(photos[i]) },
                                    onDownloadClick = onDownloadClick?.let { fn -> { fn(photos[i]) } }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            for (i in 2..3) {
                                TelegramChatPhotoTile(
                                    photo = photos[i],
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClick = { onPhotoClick(i, photos[i]) },
                                    onLongPress = { onPhotoLongPress(photos[i]) },
                                    onDownloadClick = onDownloadClick?.let { fn -> { fn(photos[i]) } },
                                    overlay = {
                                        if (!hasCaption && i == 3) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(6.dp)
                                            ) {
                                                MediaTimeChip(time = photos[3].timeText, outgoing = outgoing, status = status)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                else -> {
                    // 5 to 10 photos: hero row of 2 on top, mosaic grid below (max 10 visible tiles)
                    val topCount = 2
                    val remaining = photos.drop(topCount)
                    val visibleRemaining = remaining.take(8)
                    val overflowCount = (photos.size - 10).coerceAtLeast(0)
                    val rowsBelow = visibleRemaining.chunked(if (remaining.size == 3) 3 else 3)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(136.dp),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            for (i in 0 until topCount) {
                                TelegramChatPhotoTile(
                                    photo = photos[i],
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClick = { onPhotoClick(i, photos[i]) },
                                    onLongPress = { onPhotoLongPress(photos[i]) },
                                    onDownloadClick = onDownloadClick?.let { fn -> { fn(photos[i]) } }
                                )
                            }
                        }

                        val rowHeight = if (rowsBelow.size >= 2) 96.dp else 112.dp
                        var cursor = topCount
                        rowsBelow.forEachIndexed { rowIndex, rowPhotos ->
                            val isLastRow = rowIndex == rowsBelow.lastIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight),
                                horizontalArrangement = Arrangement.spacedBy(gap)
                            ) {
                                rowPhotos.forEachIndexed { colIndex, photo ->
                                    val globalIdx = cursor + colIndex
                                    val isVeryLastTile = isLastRow && colIndex == rowPhotos.lastIndex
                                    TelegramChatPhotoTile(
                                        photo = photo,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        onClick = { onPhotoClick(globalIdx, photo) },
                                        onLongPress = { onPhotoLongPress(photo) },
                                        onDownloadClick = onDownloadClick?.let { fn -> { fn(photo) } },
                                        overlay = {
                                            if (isVeryLastTile && overflowCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.52f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "+$overflowCount",
                                                        color = Color.White,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                            if (!hasCaption && isVeryLastTile) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(5.dp)
                                                ) {
                                                    MediaTimeChip(
                                                        time = photo.timeText,
                                                        outgoing = outgoing,
                                                        status = status
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            cursor += rowPhotos.size
                        }
                    }
                }
            }
        }

        if (hasCaption) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                MessageText(
                    text = AnnotatedString(caption!!),
                    time = lastPhoto.timeText,
                    outgoing = outgoing,
                    status = status
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TelegramChatPhotoTile(
    photo: ChatPhotoNode,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDownloadClick: (() -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val c = LocalChatColors.current
    val pulseTransition = rememberInfiniteTransition(label = "photoShimmer")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    val isMissing = photo.source.isBlank()
    val effectiveState = photo.downloadState
    val isExpired = isMissing && effectiveState == com.example.db.DmMediaDownloadState.EXPIRED
    val isFailed = isMissing && effectiveState == com.example.db.DmMediaDownloadState.FAILED
    val isPendingTap = isMissing && (effectiveState == com.example.db.DmMediaDownloadState.PENDING || photo.isAutoDownloadPaused)
    val isInProgress = isMissing && !isExpired && !isFailed && !isPendingTap

    Box(
        modifier = modifier
            .background(c.hairline.copy(alpha = 0.45f))
            .combinedClickable(
                onClick = {
                    when {
                        isExpired -> Unit
                        (isFailed || isPendingTap || isInProgress) && onDownloadClick != null -> onDownloadClick()
                        !isMissing -> onClick()
                    }
                },
                onLongClick = onLongPress
            )
    ) {
        when {
            isExpired -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E293B).copy(alpha = 0.92f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = photo.failReason?.takeIf { it.isNotBlank() } ?: "Media expired",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            isFailed -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E293B).copy(alpha = 0.90f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444).copy(alpha = 0.85f)
                        ) {
                            Box(Modifier.padding(10.dp)) {
                                ChatIconRetry(tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(
                            text = "Failed. Tap to retry",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            isInProgress -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E293B).copy(alpha = pulseAlpha + 0.5f),
                                    Color(0xFF334155).copy(alpha = pulseAlpha + 0.5f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.4.dp
                        )
                        Text(
                            text = "Downloading...",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            isPendingTap -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E293B).copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ChatIconDownload(tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Tap to download",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            else -> {
                AsyncImage(
                    model = photo.source,
                    contentDescription = photo.caption ?: "Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        overlay()
    }
}

@Composable
private fun rememberPhotoAspectRatio(source: String): Float {
    val context = LocalContext.current
    val ratio by produceState(initialValue = 1.25f, key1 = source) {
        if (source.isBlank()) {
            value = 1.25f
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                when {
                    source.startsWith("file://") -> {
                        val path = Uri.parse(source).path
                        if (!path.isNullOrBlank()) BitmapFactory.decodeFile(path, opts)
                    }
                    source.startsWith("/") -> {
                        if (File(source).exists()) BitmapFactory.decodeFile(source, opts)
                    }
                    source.startsWith("content://") -> {
                        context.contentResolver.openInputStream(Uri.parse(source))?.use {
                            BitmapFactory.decodeStream(it, null, opts)
                        }
                    }
                }
                if (opts.outWidth > 0 && opts.outHeight > 0) {
                    (opts.outWidth.toFloat() / opts.outHeight.toFloat()).coerceIn(0.62f, 1.85f)
                } else {
                    1.25f
                }
            } catch (_: Exception) {
                1.25f
            }
        }
    }
    return ratio
}
