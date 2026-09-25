package com.example.ui.screens.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.FunkyDoubleCheckIcon
import com.example.ui.screens.chat.model.ChatMessage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageMessageBubble(
    message: ChatMessage,
    partnerAvatarUrl: String,
    isHighlighted: Boolean = false,
    onImageClick: (imageUrl: String) -> Unit = {},
    onDownloadClick: (imageUrl: String) -> Unit = {},
    onReactionClick: () -> Unit = {},
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val images = message.getAllImages()
    val primaryImageUrl = images.firstOrNull() ?: message.imageUrl ?: ""

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isMe) {
            if (message.showAvatar) {
                AsyncImage(
                    model = partnerAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFE5E7EB), CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Spacer(modifier = Modifier.width(34.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        if (message.isMe) {
                            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
                        } else {
                            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
                        }
                    )
                    .background(if (message.isMe) Color(0xFFFCD34D) else Color(0xFFF3F4F6))
                    .border(
                        if (isHighlighted) BorderStroke(2.dp, Color(0xFFFFC529)) else BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f)),
                        if (message.isMe) RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
                        else RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
                    )
                    .combinedClickable(
                        onClick = { if (primaryImageUrl.isNotBlank()) onImageClick(primaryImageUrl) },
                        onLongClick = onLongClick
                    )
                    .padding(4.dp)
            ) {
                Column {
                    // Optional caption or text
                    if (message.text.isNotBlank() && !message.text.startsWith("[img]")) {
                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            color = Color(0xFF111827),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    // Render Image(s) Layout
                    when {
                        images.size <= 1 -> {
                            // Single Image Layout
                            SingleImageLayout(
                                imageUrl = primaryImageUrl,
                                onClick = { onImageClick(primaryImageUrl) },
                                onDownload = { onDownloadClick(primaryImageUrl) }
                            )
                        }
                        images.size == 2 -> {
                            // 2 Images Stack / Side-by-Side
                            TwoImagesLayout(
                                images = images,
                                onImageClick = onImageClick
                            )
                        }
                        images.size == 3 -> {
                            // 3 Images Stack
                            ThreeImagesLayout(
                                images = images,
                                onImageClick = onImageClick
                            )
                        }
                        else -> {
                            // 4+ Images Grid Layout with +N counter
                            FourPlusImagesLayout(
                                images = images,
                                onImageClick = onImageClick
                            )
                        }
                    }

                    // Time and delivery status inside/under the bubble
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp, end = 6.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.time,
                            fontSize = 11.sp,
                            color = if (message.isMe) Color(0xFF78350F).copy(alpha = 0.85f) else Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium
                        )
                        if (message.isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            FunkyDoubleCheckIcon(
                                color = Color(0xFF78350F),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Reactions Badge Pill
            if (message.reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                ReactionBadge(
                    reactions = message.reactions,
                    onClick = onReactionClick
                )
            }
        }
    }
}

@Composable
private fun SingleImageLayout(
    imageUrl: String,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE5E7EB))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
        )

        // Download Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDownload() },
            contentAlignment = Alignment.Center
        ) {
            AppSvgIcon(
                icon = ChatIconAsset.Download,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TwoImagesLayout(
    images: List<String>,
    onImageClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        images.take(2).forEach { url ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE5E7EB))
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = "Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onImageClick(url) }
                )
            }
        }
    }
}

@Composable
private fun ThreeImagesLayout(
    images: List<String>,
    onImageClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Large Left Image
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE5E7EB))
        ) {
            AsyncImage(
                model = images[0],
                contentDescription = "Photo 1",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onImageClick(images[0]) }
            )
        }

        // Two stacked right images
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..2) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE5E7EB))
                ) {
                    AsyncImage(
                        model = images.getOrNull(i) ?: "",
                        contentDescription = "Photo ${i + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onImageClick(images[i]) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FourPlusImagesLayout(
    images: List<String>,
    onImageClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top Row (2 images)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0..1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE5E7EB))
                ) {
                    AsyncImage(
                        model = images[i],
                        contentDescription = "Photo ${i + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onImageClick(images[i]) }
                    )
                }
            }
        }

        // Bottom Row (2 images, 2nd has +N overlay if more than 4)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Image 3
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE5E7EB))
            ) {
                AsyncImage(
                    model = images[2],
                    contentDescription = "Photo 3",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onImageClick(images[2]) }
                )
            }

            // Image 4 with possible overlay
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE5E7EB))
            ) {
                AsyncImage(
                    model = images[3],
                    contentDescription = "Photo 4",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onImageClick(images[3]) }
                )

                if (images.size > 4) {
                    val remaining = images.size - 4
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { onImageClick(images[3]) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$remaining",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
