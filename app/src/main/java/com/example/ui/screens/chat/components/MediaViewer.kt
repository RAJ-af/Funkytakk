package com.example.ui.screens.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

data class ChatMediaItem(
    val id: String,
    val url: String,
    val senderName: String = "User",
    val dateText: String = "Today",
    val caption: String? = null,
    val isVideo: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewer(
    mediaList: List<ChatMediaItem>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onSave: (ChatMediaItem) -> Unit,
    onShare: (ChatMediaItem) -> Unit,
    onForward: (ChatMediaItem) -> Unit,
    onDelete: (ChatMediaItem) -> Unit
) {
    if (mediaList.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, mediaList.size - 1)) { mediaList.size }
    var showChrome by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }

    // Swipe down to dismiss gesture state
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    showChrome = !showChrome
                }
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = mediaList[page]
            var scale by remember { mutableFloatStateOf(1f) }
            var panX by remember { mutableFloatStateOf(0f) }
            var panY by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = panX
                        translationY = panY + offsetY
                        alpha = (1f - (offsetY / 600f).coerceIn(0f, 0.8f))
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.8f, 4.0f)
                            if (scale > 1f) {
                                panX += pan.x
                                panY += pan.y
                            } else {
                                panX = 0f
                                panY = 0f
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                if (scale <= 1.05f) {
                                    offsetY += dragAmount
                                }
                            },
                            onDragEnd = {
                                if (offsetY > 150f) {
                                    onDismiss()
                                } else {
                                    offsetY = 0f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = item.url,
                    contentDescription = "Fullscreen media",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top Chrome Bar
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val currentItem = mediaList.getOrNull(pagerState.currentPage) ?: mediaList[0]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    AppSvgIcon(
                        icon = ChatIconAsset.ArrowBack,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentItem.senderName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentItem.dateText} · ${pagerState.currentPage + 1} of ${mediaList.size}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = { onShare(currentItem) }) {
                    AppSvgIcon(
                        icon = ChatIconAsset.Share,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        AppSvgIcon(
                            icon = ChatIconAsset.MoreVertical,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = Color(0xFF1F2127)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Save to device", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onSave(currentItem)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Forward", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onForward(currentItem)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFEF4444)) },
                            onClick = {
                                showMenu = false
                                onDelete(currentItem)
                            }
                        )
                    }
                }
            }
        }

        // Bottom Caption Chrome Bar
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val currentItem = mediaList.getOrNull(pagerState.currentPage)
            if (!currentItem?.caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Text(
                        text = currentItem!!.caption!!,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
