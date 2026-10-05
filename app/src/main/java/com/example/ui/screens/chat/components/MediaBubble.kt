package com.example.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.example.ui.components.FunkyPlayIcon

@Composable
fun MediaBubble(
    mediaUrls: List<String>,
    caption: String? = null,
    isVideo: Boolean = false,
    videoDurationFormatted: String? = null,
    time: String,
    isOutgoing: Boolean,
    statusCheck: Int = 2, // 0 = pending, 1 = sent, 2 = delivered
    onMediaClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) DarkChatColors else LightChatColors

    val innerCornerRadius = 15.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(innerCornerRadius))
        ) {
            if (mediaUrls.size == 1 || isVideo) {
                // Single Image or Video
                val imgUrl = mediaUrls.firstOrNull().orEmpty()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 250.dp)
                        .clickable { onMediaClick(0) }
                ) {
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = "Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(innerCornerRadius))
                    )

                    // Video Play Badge
                    if (isVideo) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            FunkyPlayIcon(color = Color.White, modifier = Modifier.size(20.dp))
                        }

                        if (!videoDurationFormatted.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = videoDurationFormatted,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Overlaid dark time pill if no caption
                    if (caption.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.60f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = time,
                                    fontSize = 10.5.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isOutgoing) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    FunkyDoubleCheckIcon(
                                        color = colors.accent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (mediaUrls.size in 2..4) {
                // 2 to 4 Grid Layout with 2dp gaps
                val count = mediaUrls.size
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (count == 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            mediaUrls.forEachIndexed { index, url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(140.dp)
                                        .clickable { onMediaClick(index) }
                                )
                            }
                        }
                    } else if (count == 3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            AsyncImage(
                                model = mediaUrls[0],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                                    .clickable { onMediaClick(0) }
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                AsyncImage(
                                    model = mediaUrls[1],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(89.dp)
                                        .clickable { onMediaClick(1) }
                                )
                                AsyncImage(
                                    model = mediaUrls[2],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(89.dp)
                                        .clickable { onMediaClick(2) }
                                )
                            }
                        }
                    } else { // 4 images 2x2 grid
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                AsyncImage(
                                    model = mediaUrls[0],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .clickable { onMediaClick(0) }
                                )
                                AsyncImage(
                                    model = mediaUrls[1],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .clickable { onMediaClick(1) }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                AsyncImage(
                                    model = mediaUrls[2],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .clickable { onMediaClick(2) }
                                )
                                AsyncImage(
                                    model = mediaUrls[3],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .clickable { onMediaClick(3) }
                                )
                            }
                        }
                    }
                }

                if (caption.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.60f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = time,
                                fontSize = 10.5.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            if (isOutgoing) {
                                Spacer(modifier = Modifier.width(3.dp))
                                FunkyDoubleCheckIcon(
                                    color = colors.accent,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Caption text inside bubble below media if provided
        if (!caption.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = caption,
                fontSize = 15.sp,
                color = colors.text,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 4.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = time,
                    fontSize = 11.sp,
                    color = colors.secondaryText
                )
                if (isOutgoing) {
                    Spacer(modifier = Modifier.width(3.dp))
                    FunkyDoubleCheckIcon(
                        color = colors.accent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
