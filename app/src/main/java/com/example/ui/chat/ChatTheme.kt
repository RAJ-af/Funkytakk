package com.example.ui.chat

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * FunkyTalk chat design tokens. UI only: no crypto, Supabase or database code in this kit.
 *
 * Wrap the whole chat screen in ProvideChatTheme(...) ONCE. The `darkTheme` flag must come from the
 * same state that decides the screen background, otherwise bubbles and wallpaper will mismatch.
 *
 * Why these colors (all text pairs were checked for >= 4.5:1 contrast, graphics >= 3:1):
 *  - Wallpaper is a warm off-white (not pure white) so white incoming bubbles read as raised surfaces.
 *  - Outgoing bubble is a soft butter yellow (brand) with near-black text; meta text uses a brownish
 *    olive on it (not gray) so it stays harmonious and readable.
 *  - Accent blue is used for links, mentions, reply bars and read ticks. It is a darker blue on the
 *    yellow outgoing bubble (accentOut) because the lighter blue fails contrast there.
 *  - Voice buttons: brand yellow on incoming (high contrast on white), dark amber on outgoing
 *    (a yellow button on a yellow bubble would disappear).
 */

enum class ChatPreset { Butter, Sky, Mint, Graphite }

@Immutable
data class ChatColors(
    val isDark: Boolean,
    val wallpaper: Color,
    val incomingBubble: Color,
    val outgoingBubble: Color,
    val text: Color,
    val textSecondary: Color, // meta on incoming bubbles, muted text on wallpaper
    val textSecondaryOut: Color, // meta on outgoing bubbles
    val accentIn: Color,
    val accentOut: Color,
    val tickRead: Color,
    val hairline: Color,
    val voiceButtonIn: Color,
    val onVoiceButtonIn: Color,
    val voiceButtonOut: Color,
    val onVoiceButtonOut: Color,
    val waveIdleIn: Color,
    val wavePlayedIn: Color,
    val waveIdleOut: Color,
    val wavePlayedOut: Color,
    val surface: Color, // composer, sheets, menus
    val surfaceVariant: Color, // text field background
    val brand: Color,
    val onBrand: Color,
    val highlight: Color, // search match background
    val danger: Color,
    val mediaChip: Color, // dark pill drawn over photos/videos
    val senderPalette: List<Color>,
)

private val LightSender = listOf(
    Color(0xFFC23B4E), Color(0xFFA8650A), Color(0xFF2E7D5B), Color(0xFF00718A),
    Color(0xFF2B5F9A), Color(0xFF7048A8), Color(0xFFB5501F), Color(0xFF8A4F8F),
)
private val DarkSender = listOf(
    Color(0xFFFF7B8C), Color(0xFFE8A93A), Color(0xFF5CC49A), Color(0xFF4CC3DD),
    Color(0xFF7FB2F0), Color(0xFFB493F0), Color(0xFFF29765), Color(0xFFD79ADB),
)

private val ButterLight = ChatColors(
    isDark = false,
    wallpaper = Color(0xFFF6F4EE),
    incomingBubble = Color(0xFFFFFFFF),
    outgoingBubble = Color(0xFFFFE8A3),
    text = Color(0xFF1B1B1B),
    textSecondary = Color(0xFF6B6B66),
    textSecondaryOut = Color(0xFF5C522B),
    accentIn = Color(0xFF2F6FDE),
    accentOut = Color(0xFF1F5BC4),
    tickRead = Color(0xFF2F6FDE),
    hairline = Color(0x18000000),
    voiceButtonIn = Color(0xFFFFC629),
    onVoiceButtonIn = Color(0xFF1B1B1B),
    voiceButtonOut = Color(0xFF8A5A00),
    onVoiceButtonOut = Color(0xFFFFFFFF),
    waveIdleIn = Color(0x596B6B66),
    wavePlayedIn = Color(0xFF2F6FDE),
    waveIdleOut = Color(0x598A5A00),
    wavePlayedOut = Color(0xFF8A5A00),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDE9DF),
    brand = Color(0xFFFFC629),
    onBrand = Color(0xFF1B1B1B),
    highlight = Color(0x66FFC629),
    danger = Color(0xFFD92D20),
    mediaChip = Color(0x73000000),
    senderPalette = LightSender,
)

private val ButterDark = ChatColors(
    isDark = true,
    wallpaper = Color(0xFF0F1114),
    incomingBubble = Color(0xFF1E2128),
    outgoingBubble = Color(0xFF54420F),
    text = Color(0xFFF3EFE2),
    textSecondary = Color(0xFFA7A59C),
    textSecondaryOut = Color(0xFFD8C98F),
    accentIn = Color(0xFF7FB0FF),
    accentOut = Color(0xFF8DB8FF),
    tickRead = Color(0xFF8DB8FF),
    hairline = Color(0x1FFFFFFF),
    voiceButtonIn = Color(0xFFF5B800),
    onVoiceButtonIn = Color(0xFF1B1B1B),
    voiceButtonOut = Color(0xFFFFD25A),
    onVoiceButtonOut = Color(0xFF3A2C00),
    waveIdleIn = Color(0x59A7A59C),
    wavePlayedIn = Color(0xFFF5B800),
    waveIdleOut = Color(0x59D8C98F),
    wavePlayedOut = Color(0xFFFFD25A),
    surface = Color(0xFF181B20),
    surfaceVariant = Color(0xFF262A32),
    brand = Color(0xFFFFC629),
    onBrand = Color(0xFF1B1B1B),
    highlight = Color(0x66FFC629),
    danger = Color(0xFFFF7066),
    mediaChip = Color(0x8C000000),
    senderPalette = DarkSender,
)

fun chatColors(preset: ChatPreset, dark: Boolean): ChatColors = when (preset) {
    ChatPreset.Butter -> if (dark) ButterDark else ButterLight
    ChatPreset.Sky -> if (dark) {
        ButterDark.copy(
            wallpaper = Color(0xFF0E1218), outgoingBubble = Color(0xFF1F3A66),
            textSecondaryOut = Color(0xFFA9C2E8), accentOut = Color(0xFF9CC2FF), tickRead = Color(0xFF9CC2FF),
            voiceButtonOut = Color(0xFF9CC2FF), onVoiceButtonOut = Color(0xFF0E2240),
            waveIdleOut = Color(0x599CC2FF), wavePlayedOut = Color(0xFF9CC2FF),
            brand = Color(0xFF7FB0FF), onBrand = Color(0xFF0E2240),
        )
    } else {
        ButterLight.copy(
            wallpaper = Color(0xFFEEF3F8), outgoingBubble = Color(0xFFD6E8FF),
            textSecondaryOut = Color(0xFF3F5F8F), accentOut = Color(0xFF0B57D0), tickRead = Color(0xFF0B57D0),
            voiceButtonOut = Color(0xFF0B57D0), onVoiceButtonOut = Color(0xFFFFFFFF),
            waveIdleOut = Color(0x590B57D0), wavePlayedOut = Color(0xFF0B57D0),
            brand = Color(0xFF2F6FDE), onBrand = Color(0xFFFFFFFF),
        )
    }
    ChatPreset.Mint -> if (dark) {
        ButterDark.copy(
            wallpaper = Color(0xFF0E1513), outgoingBubble = Color(0xFF1B4A3A),
            textSecondaryOut = Color(0xFFA6D4C0), accentOut = Color(0xFF5FD4B0), tickRead = Color(0xFF5FD4B0),
            voiceButtonOut = Color(0xFF5FD4B0), onVoiceButtonOut = Color(0xFF0B2A20),
            waveIdleOut = Color(0x595FD4B0), wavePlayedOut = Color(0xFF5FD4B0),
            brand = Color(0xFF5FD4B0), onBrand = Color(0xFF0B2A20),
        )
    } else {
        ButterLight.copy(
            wallpaper = Color(0xFFEEF4EF), outgoingBubble = Color(0xFFD5F2E3),
            textSecondaryOut = Color(0xFF3E6B58), accentOut = Color(0xFF0F766E), tickRead = Color(0xFF0F766E),
            voiceButtonOut = Color(0xFF0F766E), onVoiceButtonOut = Color(0xFFFFFFFF),
            waveIdleOut = Color(0x590F766E), wavePlayedOut = Color(0xFF0F766E),
            brand = Color(0xFF1D9A6C), onBrand = Color(0xFFFFFFFF),
        )
    }
    ChatPreset.Graphite -> if (dark) {
        ButterDark.copy(
            outgoingBubble = Color(0xFF2D3138),
            textSecondaryOut = Color(0xFFB4B9C2), accentOut = Color(0xFF8FA8FF), tickRead = Color(0xFF8FA8FF),
            voiceButtonOut = Color(0xFF8FA8FF), onVoiceButtonOut = Color(0xFF101935),
            waveIdleOut = Color(0x598FA8FF), wavePlayedOut = Color(0xFF8FA8FF),
            brand = Color(0xFF8FA8FF), onBrand = Color(0xFF101935),
        )
    } else {
        ButterLight.copy(
            wallpaper = Color(0xFFF2F3F5), outgoingBubble = Color(0xFFE3E5E8),
            textSecondaryOut = Color(0xFF5A5F68), accentOut = Color(0xFF3B5BDB), tickRead = Color(0xFF3B5BDB),
            voiceButtonOut = Color(0xFF3B5BDB), onVoiceButtonOut = Color(0xFFFFFFFF),
            waveIdleOut = Color(0x593B5BDB), wavePlayedOut = Color(0xFF3B5BDB),
            brand = Color(0xFF3B5BDB), onBrand = Color(0xFFFFFFFF),
        )
    }
}

fun ChatColors.senderColor(userId: String): Color =
    senderPalette[(userId.hashCode() and 0x7fffffff) % senderPalette.size]

/** Sizes and spacing. Touch targets are never below 48dp. */
object ChatDimens {
    val BubbleBig = 18.dp
    val BubbleSmall = 5.dp
    val BubbleMaxWidth = 340.dp // also capped at 80% of the screen width
    val BubblePadH = 10.dp
    val BubblePadV = 6.dp
    val InGroupGap = 2.dp
    val BetweenGroupsGap = 8.dp
    val ScreenEdge = 8.dp
    val AvatarSize = 32.dp
    val AvatarGap = 6.dp
    val MediaPad = 3.dp
    val MediaInnerRadius = 15.dp // BubbleBig - MediaPad
    val MinTouch = 48.dp
    val VoiceWidth = 240.dp
    val PlayButton = 40.dp
    val FabSize = 40.dp
    val ReactionHeight = 24.dp
    val SwipeReplyThreshold = 64.dp
    val SwipeReplyMax = 96.dp
}

/** User-adjustable message text size (Telegram-style). 1f = 16sp body text. Range 0.85f..1.4f. */
val LocalChatTextScale = compositionLocalOf { 1f }

@Composable
fun Float.chatSp(): TextUnit = (this * LocalChatTextScale.current).sp

val LocalChatColors = staticCompositionLocalOf { ButterLight }
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

@Composable
fun ProvideChatTheme(
    darkTheme: Boolean,
    preset: ChatPreset = ChatPreset.Butter,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalChatColors provides chatColors(preset, darkTheme),
        LocalChatTextScale provides textScale.coerceIn(0.85f, 1.4f),
        LocalReduceMotion provides rememberReduceMotion(),
        content = content,
    )
}
