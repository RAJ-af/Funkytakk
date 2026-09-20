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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.screens.chat.model.ChatMessage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageMessageBubble(
    message: ChatMessage,
    partnerAvatarUrl: String,
    isHighlighted: Boolean = false,
    onDownloadClick: () -> Unit = {},
    onReactionClick: () -> Unit = {},
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
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

        Column {
            Box(modifier = Modifier.widthIn(max = 260.dp)) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF3F4F6),
                    border = if (isHighlighted) BorderStroke(2.dp, Color(0xFFFFC529)) else null,
                    shadowElevation = if (isHighlighted) 6.dp else 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = onLongClick
                        )
                ) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Shared photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                }

                // Download Button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 12.dp, y = (-28).dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), CircleShape)
                        .clickable { onDownloadClick() },
                    contentAlignment = Alignment.Center
                ) {
                    AppSvgIcon(
                        icon = ChatIconAsset.Download,
                        tint = Color(0xFF111827),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Reaction Badge
                if (message.reactions.isNotEmpty()) {
                    ReactionBadge(
                        reactions = message.reactions,
                        onClick = onReactionClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 12.dp, y = 14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message.time,
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}
