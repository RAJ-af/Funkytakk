package com.example.ui.screens.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.chat.model.ChatMessage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutgoingMessageBubble(
    message: ChatMessage,
    isHighlighted: Boolean = false,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                color = Color(0xFFFCD34D), // Soft yellow brand color
                border = if (isHighlighted) BorderStroke(2.dp, Color.White) else null,
                shadowElevation = if (isHighlighted) 6.dp else 0.dp,
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text,
                        fontSize = 14.5.sp,
                        color = Color(0xFF111827),
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.time,
                            fontSize = 11.sp,
                            color = Color(0xFF78350F).copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        AppSvgIcon(
                            icon = ChatIconAsset.DoubleCheck,
                            tint = Color(0xFF78350F),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            if (message.reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                ReactionBadge(
                    reactions = message.reactions,
                    onClick = onLongClick
                )
            }
        }
    }
}
