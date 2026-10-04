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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
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
                res.onSuccess { msg ->
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
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

                    // Top-Right: Download Icon & Three-Dot Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Direct Download Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { saveCurrent() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Save to device",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
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
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More actions",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
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
                                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Share", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                        }
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        shareCurrent()
                                    }
                                )

                                // Action: Save to device
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Save to device", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                        }
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        saveCurrent()
                                    }
                                )

                                if (isMyImage) {
                                    // Action: Set as profile photo
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFF6C945), modifier = Modifier.size(17.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("Set as profile photo", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                            }
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            onSetAsProfilePhoto?.invoke()
                                            Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show()
                                        }
                                    )

                                    // Action: Delete
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
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
                                } else {
                                    // Action: Report
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Report, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("Report", color = Color(0xFFEF4444), fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                            }
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            Toast.makeText(context, "Report submitted for review", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Minimal Thumbnail Strip (only if multiple images) & Optional Caption
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Optional Caption if present
                    if (captionText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = captionText,
                                fontSize = 13.sp,
                                color = Color.White,
                                maxLines = 2
                            )
                        }
                    }

                    // Compact Thumbnail Strip (4-5 thumbnails max visible)
                    if (resolvedList.size > 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            val visibleSlice = resolvedList.take(5)
                            visibleSlice.forEachIndexed { idx, url ->
                                val isSelected = idx == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 44.dp else 36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) Color(0xFFF6C945) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            scope.launch { pagerState.animateScrollToPage(idx) }
                                        }
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
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
