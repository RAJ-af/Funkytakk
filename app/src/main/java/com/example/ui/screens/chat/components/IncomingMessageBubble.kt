package com.example.ui.screens.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
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
fun IncomingMessageBubble(
    message: ChatMessage,
    partnerAvatarUrl: String,
    isHighlighted: Boolean = false,
    onLongClick: () -> Unit,
    onReactionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
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

        Row(verticalAlignment = Alignment.Bottom) {
            Column {
                Surface(
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                    color = Color(0xFFF3F4F6),
                    border = if (isHighlighted) BorderStroke(2.dp, Color(0xFFFFC529)) else null,
                    shadowElevation = if (isHighlighted) 6.dp else 0.dp,
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = onLongClick
                        )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = if (message.isTranslated && message.translation != null) message.translation else message.text,
                            fontSize = 14.5.sp,
                            color = Color(0xFF111827),
                            lineHeight = 20.sp
                        )
                    }
                }

                if (message.reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    ReactionBadge(
                        reactions = message.reactions,
                        onClick = onReactionClick
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = message.time,
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
