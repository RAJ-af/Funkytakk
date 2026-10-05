package com.example.ui.screens.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChatReactionItem(
    val emoji: String,
    val count: Int,
    val isMine: Boolean,
    val usersList: List<String> = emptyList()
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReactionRow(
    reactions: List<ChatReactionItem>,
    onToggleReaction: (emoji: String) -> Unit,
    onShowReactionDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (reactions.isEmpty()) return

    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) DarkChatColors else LightChatColors

    LazyRow(
        modifier = modifier.padding(top = 3.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(reactions, key = { it.emoji }) { item ->
            val bg = if (item.isMine) colors.accent.copy(alpha = 0.12f) else colors.incomingBubble
            val border = if (item.isMine) BorderStroke(1.dp, colors.accent) else BorderStroke(0.5.dp, colors.secondaryText.copy(alpha = 0.25f))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bg,
                border = border,
                modifier = Modifier
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = { onToggleReaction(item.emoji) },
                        onLongClick = onShowReactionDetails
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.emoji,
                        fontSize = 12.5.sp
                    )
                    if (item.count > 1) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = item.count.toString(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isMine) colors.accent else colors.text
                        )
                    }
                }
            }
        }
    }
}
