package com.example.ui.screens.chat.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.chat.*
import com.example.util.ImageSaveUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatImagePreviewDialog(
    imageUrl: String? = null,
    imageUrls: List<String> = emptyList(),
    initialIndex: Int = 0,
    isMyImage: Boolean = false,
    captionText: String = "",
    senderName: String = "",
    timestampText: String = "",
    onDeleteImage: (() -> Unit)? = null,
    onSetAsProfilePhoto: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val resolvedList = remember(imageUrl, imageUrls) {
        if (imageUrls.isNotEmpty()) imageUrls.filter { it.isNotBlank() }
        else if (!imageUrl.isNullOrBlank()) listOf(imageUrl)
        else emptyList()
    }

    if (resolvedList.isEmpty()) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val safeInitialIndex = initialIndex.coerceIn(0, resolvedList.size - 1)
    val pagerState = rememberPagerState(initialPage = safeInitialIndex) { resolvedList.size }

    var areControlsVisible by remember { mutableStateOf(true) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    fun saveCurrent() {
        val activeUrl = resolvedList.getOrNull(pagerState.currentPage) ?: return
        if (!isSaving) {
            isSaving = true
            scope.launch {
                val res = ImageSaveUtils.saveImageToGallery(context, activeUrl)
                isSaving = false
                res.onSuccess {
                    Toast.makeText(context, "Saved to device", Toast.LENGTH_SHORT).show()
                }.onFailure { ex ->
                    Toast.makeText(context, "Could not save: ${ex.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun shareCurrent() {
        val activeUrl = resolvedList.getOrNull(pagerState.currentPage) ?: return
        try {
            ImageSaveUtils.shareImage(context, activeUrl)
        } catch (_: Exception) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, activeUrl)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Image"))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
        ) {
            // Main Pager View with Double-Tap & Pinch Zoom
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) { page ->
                val currentUrl = resolvedList.getOrNull(page) ?: return@HorizontalPager

                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { areControlsVisible = !areControlsVisible },
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    val maxOffset = 600f * (scale - 1f)
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                                        y = (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = currentUrl,
                        contentDescription = "Full-screen photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 32.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Minimal Cinematic Top Bar
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top-Left: Back Arrow Button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            ChatIconBack(tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Center-Left Counter: "3 / 12"
                        if (resolvedList.size > 1) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${pagerState.currentPage + 1} / ${resolvedList.size}",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // Top-Right: Share & Three-Dot Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { shareCurrent() },
                            contentAlignment = Alignment.Center
                        ) {
                            ChatIconForward(tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Three-Dot Menu Button & Menu
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { showOptionsMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                ChatIconMoreVert(tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = Color(0xFF18181B),
                                shadowElevation = 12.dp,
                                modifier = Modifier
                                    .width(200.dp)
                                    .background(Color(0xFF18181B), RoundedCornerShape(16.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            ) {
                                // Action: Share
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            ChatIconForward(tint = Color.White, modifier = Modifier.size(17.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Share", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                        }
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        shareCurrent()
                                    }
                                )

                                if (isMyImage) {
                                    // Action: Delete
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                ChatIconTrash(tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("Delete", color = Color(0xFFEF4444), fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                            }
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            onDeleteImage?.invoke()
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Caption Bar
            AnimatedVisibility(
                visible = areControlsVisible && (captionText.isNotBlank() || senderName.isNotBlank()),
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (senderName.isNotBlank() || timestampText.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = senderName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFC629)
                                )
                                Text(
                                    text = timestampText,
                                    fontSize = 11.5.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                        if (captionText.isNotBlank()) {
                            Text(
                                text = captionText,
                                fontSize = 14.sp,
                                color = Color.White,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
