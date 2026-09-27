package com.example.ui.screens.chat.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.components.*
import com.example.util.ImageSaveUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatImagePreviewDialog(
    imageUrl: String? = null,
    imageUrls: List<String> = emptyList(),
    initialIndex: Int = 0,
    senderName: String = "Partner",
    timestampText: String = "",
    onDismiss: () -> Unit
) {
    // Resolve all image URLs
    val resolvedList = remember(imageUrl, imageUrls) {
        if (imageUrls.isNotEmpty()) imageUrls.filter { it.isNotBlank() }
        else if (!imageUrl.isNullOrBlank()) listOf(imageUrl)
        else emptyList()
    }

    if (resolvedList.isEmpty()) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val safeInitialIndex = initialIndex.coerceIn(0, resolvedList.size - 1)
    val pagerState = rememberPagerState(initialPage = safeInitialIndex) { resolvedList.size }

    var areControlsVisible by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

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
                .background(Color.Black.copy(alpha = 0.98f))
        ) {
            // Main Pager View with Pinch-to-Zoom & Double Tap
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
                                onTap = {
                                    areControlsVisible = !areControlsVisible
                                },
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
                                    val maxOffset = 500f * (scale - 1f)
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
                        contentDescription = "Fullscreen photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 24.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )

                    // Zoom Level Pill & Quick Reset Button
                    if (scale > 1.05f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = 120.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .clickable {
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${(scale * 100).toInt()}% • Reset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Top Header Overlay (Title, Index counter, Close button)
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Close Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            SoftCloseIcon(
                                size = 18.dp,
                                color = Color.White
                            )
                        }

                        // Title & Sender Info
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = senderName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (timestampText.isNotBlank()) {
                                Text(
                                    text = timestampText,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFD1D5DB)
                                )
                            }
                        }

                        // Counter Badge (e.g. "1 / 4")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${resolvedList.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Action Bar (Save to Gallery, Share, Copy Link)
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Multi-photo Thumbnail Strip if more than 1 image
                    if (resolvedList.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            resolvedList.forEachIndexed { idx, url ->
                                val isSelected = idx == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (isSelected) 46.dp else 36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFFFFC529) else Color.White.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            scope.launch { pagerState.animateScrollToPage(idx) }
                                        }
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Thumbnail ${idx + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Floating Actions Dock
                    Surface(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        color = Color(0xFF18181B).copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeUrl = resolvedList.getOrNull(pagerState.currentPage) ?: ""

                            // 1. SAVE TO GALLERY BUTTON (REAL MEDIASTORE SAVE)
                            Button(
                                onClick = {
                                    if (activeUrl.isNotBlank() && !isSaving) {
                                        isSaving = true
                                        scope.launch {
                                            val result = ImageSaveUtils.saveImageToGallery(context, activeUrl)
                                            isSaving = false
                                            result.onSuccess { msg ->
                                                Toast.makeText(context, "✅ $msg", Toast.LENGTH_LONG).show()
                                            }.onFailure { ex ->
                                                Toast.makeText(context, "Failed to save: ${ex.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC529),
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Saving...", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    SoftDownloadIcon(size = 18.dp, color = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save to Gallery", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            // 2. SHARE LINK BUTTON
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable {
                                        if (activeUrl.isNotBlank()) {
                                            ImageSaveUtils.shareImage(context, activeUrl)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                SoftShareIcon(size = 20.dp, color = Color.White)
                            }

                            // 3. COPY LINK BUTTON
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable {
                                        if (activeUrl.isNotBlank()) {
                                            clipboardManager.setText(AnnotatedString(activeUrl))
                                            Toast.makeText(context, "Image link copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                SoftBookmarkIcon(size = 20.dp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
