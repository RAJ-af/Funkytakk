package com.example.ui.screens.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FunkyCopyIcon
import com.example.ui.components.FunkyPlusIcon
import com.example.ui.components.FunkyReplyIcon
import com.example.ui.components.SoftShieldIcon

val QUICK_EMOJIS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

@Composable
fun ChatLongPressMenu(
    messageText: String,
    senderName: String?,
    isOutgoing: Boolean,
    myReaction: String? = null,
    onDismiss: () -> Unit,
    onSelectEmoji: (emoji: String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onTranslate: () -> Unit,
    onListen: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) DarkChatColors else LightChatColors

    var animated by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        targetValue = if (animated) 1f else 0.90f,
        animationSpec = tween(durationMillis = 120),
        label = "menuScale"
    )

    LaunchedEffect(Unit) {
        animated = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.50f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .scale(scaleAnim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume click */ }
        ) {
            // Quick-Emoji Bar
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = if (isDark) Color(0xFF1F2127) else Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QUICK_EMOJIS.forEach { emoji ->
                        val isSelected = myReaction == emoji
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.accent.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    onSelectEmoji(emoji)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            }

            // Context Actions Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) Color(0xFF1F2127) else Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
                    MenuItemRow("Reply", { FunkyReplyIcon(color = colors.text, modifier = Modifier.size(18.dp)) }) {
                        onReply()
                        onDismiss()
                    }
                    if (messageText.isNotBlank() && !messageText.startsWith("[")) {
                        MenuItemRow("Copy", { FunkyCopyIcon(color = colors.text, modifier = Modifier.size(18.dp)) }) {
                            onCopy()
                            onDismiss()
                        }
                        MenuItemRow("Translate", { AppSvgIcon(icon = ChatIconAsset.Translate, tint = colors.text, modifier = Modifier.size(18.dp)) }) {
                            onTranslate()
                            onDismiss()
                        }
                        MenuItemRow("Listen (Text to Speech)", { AppSvgIcon(icon = ChatIconAsset.VoiceWave, tint = colors.text, modifier = Modifier.size(18.dp)) }) {
                            onListen()
                            onDismiss()
                        }
                    }
                    MenuItemRow("Forward", { AppSvgIcon(icon = ChatIconAsset.Forward, tint = colors.text, modifier = Modifier.size(18.dp)) }) {
                        onForward()
                        onDismiss()
                    }
                    HorizontalDivider(color = colors.secondaryText.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                    MenuItemRow("Delete", { AppSvgIcon(icon = ChatIconAsset.Trash, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp)) }, isDanger = true) {
                        onDelete()
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemRow(
    label: String,
    icon: @Composable () -> Unit,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) DarkChatColors else LightChatColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDanger) Color(0xFFEF4444) else colors.text
        )
    }
}
