package com.example.ui.screens.chat.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Immutable
data class ChatColorScheme(
    val background: Color,
    val incomingBubble: Color,
    val outgoingBubble: Color,
    val text: Color,
    val secondaryText: Color,
    val accent: Color,
    val replyBg: Color,
    val isDark: Boolean
)

val LightChatColors = ChatColorScheme(
    background = Color(0xFFF6F4EE),
    incomingBubble = Color(0xFFFFFFFF),
    outgoingBubble = Color(0xFFFFE8A3),
    text = Color(0xFF1B1B1B),
    secondaryText = Color(0xFF6B6B66),
    accent = Color(0xFF2F6FDE),
    replyBg = Color(0xFF2F6FDE).copy(alpha = 0.10f),
    isDark = false
)

val DarkChatColors = ChatColorScheme(
    background = Color(0xFF121316),
    incomingBubble = Color(0xFF1F2127),
    outgoingBubble = Color(0xFF4A3B10),
    text = Color(0xFFF3EFE2),
    secondaryText = Color(0xFFA7A59C),
    accent = Color(0xFF6FA0FF),
    replyBg = Color(0xFF6FA0FF).copy(alpha = 0.12f),
    isDark = true
)

val LocalChatColors = staticCompositionLocalOf { LightChatColors }

object ChatTheme {
    val colors: ChatColorScheme
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) DarkChatColors else LightChatColors

    // 8-color palette for sender names in group chats
    private val SENDER_PALETTE_LIGHT = listOf(
        Color(0xFF059669), // Emerald
        Color(0xFF2563EB), // Blue
        Color(0xFF7C3AED), // Purple
        Color(0xFFEA580C), // Orange
        Color(0xFF0D9488), // Teal
        Color(0xFFDB2777), // Pink
        Color(0xFFD97706), // Amber
        Color(0xFF4F46E5)  // Indigo
    )

    private val SENDER_PALETTE_DARK = listOf(
        Color(0xFF34D399), // Emerald
        Color(0xFF60A5FA), // Blue
        Color(0xFFC084FC), // Purple
        Color(0xFFFB923C), // Orange
        Color(0xFF2DD4BF), // Teal
        Color(0xFFF472B6), // Pink
        Color(0xFFFBBF24), // Amber
        Color(0xFF818CF8)  // Indigo
    )

    fun getSenderColor(userId: String, isDark: Boolean = false): Color {
        if (userId.isBlank()) return if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
        val hash = abs(userId.hashCode())
        val palette = if (isDark) SENDER_PALETTE_DARK else SENDER_PALETTE_LIGHT
        return palette[hash % palette.size]
    }

    /**
     * Calculates bubble shape with 18dp outer corners and 5dp inner corners for grouped messages.
     */
    fun getBubbleShape(
        isOutgoing: Boolean,
        isFirstInGroup: Boolean,
        isLastInGroup: Boolean,
        isSingle: Boolean
    ): RoundedCornerShape {
        val outer = 18.dp
        val inner = 5.dp

        return if (isSingle) {
            RoundedCornerShape(outer)
        } else if (isOutgoing) {
            // Outgoing bubble (right aligned)
            when {
                isFirstInGroup -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = outer, bottomEnd = inner)
                isLastInGroup -> RoundedCornerShape(topStart = outer, topEnd = inner, bottomStart = outer, bottomEnd = outer)
                else -> RoundedCornerShape(topStart = outer, topEnd = inner, bottomStart = outer, bottomEnd = inner)
            }
        } else {
            // Incoming bubble (left aligned)
            when {
                isFirstInGroup -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = outer)
                isLastInGroup -> RoundedCornerShape(topStart = inner, topEnd = outer, bottomStart = outer, bottomEnd = outer)
                else -> RoundedCornerShape(topStart = inner, topEnd = outer, bottomStart = inner, bottomEnd = outer)
            }
        }
    }
}
