package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.CircleFlag
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.clipPath

// Maps country flag emoji to standard ISO country code used by Hatscripts Circle Flags
fun mapFlagEmojiToCode(emoji: String): String {
    return when (emoji) {
        "🇮🇳" -> "in"
        "🇵🇰" -> "pk"
        "🇯🇵" -> "jp"
        "🇺🇸" -> "us"
        "🇬🇧" -> "gb"
        "🇦🇪" -> "ae"
        "🇨🇳" -> "cn"
        "🇪🇸" -> "es"
        "🇫🇷" -> "fr"
        "🇩🇪" -> "de"
        "🇮🇹" -> "it"
        "🇷🇺" -> "ru"
        "🇰🇷" -> "kr"
        else -> {
            if (emoji.length == 2) emoji.lowercase() else "in"
        }
    }
}

// High-fidelity Color Accents
val ChillPurpleAccent = Color(0xFF8B5CF6)
val ChillGoldIcon = Color(0xFFFFB800)
val LiveGreen = Color(0xFF22C55E)

data class RoomTheme(
    val name: String,
    val description: String,
    val isDark: Boolean,
    val startColor: Color,
    val endColor: Color,
    val themeColor: Color,
    val bgImageResId: Int? = null,
    val accentColor: Color = themeColor,
    val cardBgColor: Color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.85f),
    val cardBorderColor: Color = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFF3F4F6),
    val primaryTextColor: Color = if (isDark) Color.White else Color(0xFF1F2937),
    val secondaryTextColor: Color = if (isDark) Color.White.copy(alpha = 0.65f) else Color.Gray,
    val chatBubbleBg: Color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF3F4F6),
    val chatBubbleText: Color = if (isDark) Color.White else Color(0xFF1F2937)
)

val roomThemes = listOf(
    // PREMIUM WALLPAPER THEMES (New stunning aesthetic themes based on user's uploaded art)
    RoomTheme(
        name = "🌸 Cherry Blossom", 
        description = "Romantic sunset with warm streetlamp glow", 
        isDark = false, 
        startColor = Color(0xFFFDF2F8), 
        endColor = Color(0xFFE0F2FE), 
        themeColor = Color(0xFFEC4899),
        bgImageResId = R.drawable.img_wallpaper_cherry_blossom_1781625392725,
        accentColor = Color(0xFFEC4899),
        cardBgColor = Color.White.copy(alpha = 0.82f),
        cardBorderColor = Color(0xFFFBCFE8),
        primaryTextColor = Color(0xFF4C0519),
        secondaryTextColor = Color(0xFFBE185D),
        chatBubbleBg = Color(0xFFFCE7F3),
        chatBubbleText = Color(0xFF831843)
    ),
    RoomTheme(
        name = "🌌 Lofi Balcony", 
        description = "Cozy apartment balcony over starry neon city", 
        isDark = true, 
        startColor = Color(0xFF1E1B4B), 
        endColor = Color(0xFF0F172A), 
        themeColor = Color(0xFFFBBF24),
        bgImageResId = R.drawable.img_wallpaper_balcony_night_1781625409367, // Using correct resource suffix
        accentColor = Color(0xFFFBBF24),
        cardBgColor = Color(0xFF0F172A).copy(alpha = 0.65f),
        cardBorderColor = Color(0xFF312E81),
        primaryTextColor = Color.White,
        secondaryTextColor = Color(0xFF94A3B8),
        chatBubbleBg = Color(0xFF1E1B4B).copy(alpha = 0.7f),
        chatBubbleText = Color(0xFFF1F5F9)
    ),
    RoomTheme(
        name = "🌕 Midnight Moon", 
        description = "Luminous full moon night over calm valley hills", 
        isDark = true, 
        startColor = Color(0xFF022C22), 
        endColor = Color(0xFF0A192F), 
        themeColor = Color(0xFF34D399),
        bgImageResId = R.drawable.img_wallpaper_moonlight_valley_1781625422948,
        accentColor = Color(0xFF10B981),
        cardBgColor = Color(0xFF022C22).copy(alpha = 0.65f),
        cardBorderColor = Color(0xFF065F46),
        primaryTextColor = Color(0xFFECFDF5),
        secondaryTextColor = Color(0xFFA7F3D0).copy(alpha = 0.7f),
        chatBubbleBg = Color(0xFF064E3B).copy(alpha = 0.65f),
        chatBubbleText = Color(0xFFECFDF5)
    ),
    RoomTheme(
        name = "🏡 Sunny Japan", 
        description = "Sun-dappled residential road with leafy trees", 
        isDark = false, 
        startColor = Color(0xFFECFDF5), 
        endColor = Color(0xFFEFF6FF), 
        themeColor = Color(0xFF059669),
        bgImageResId = R.drawable.img_wallpaper_day_street_1781625437347,
        accentColor = Color(0xFF10B981),
        cardBgColor = Color.White.copy(alpha = 0.88f),
        cardBorderColor = Color(0xFFA7F3D0),
        primaryTextColor = Color(0xFF064E3B),
        secondaryTextColor = Color(0xFF374151),
        chatBubbleBg = Color(0xFFD1FAE5),
        chatBubbleText = Color(0xFF064E3B)
    ),
    RoomTheme(
        name = "🌅 Ocean Lighthouse", 
        description = "Beautiful marine sunset near beach lighthouse", 
        isDark = false, 
        startColor = Color(0xFFFFF7ED), 
        endColor = Color(0xFFFAF5FF), 
        themeColor = Color(0xFFEA580C),
        bgImageResId = R.drawable.img_wallpaper_lighthouse_sunset_1781625450547,
        accentColor = Color(0xFFF97316),
        cardBgColor = Color.White.copy(alpha = 0.82f),
        cardBorderColor = Color(0xFFFDBA74),
        primaryTextColor = Color(0xFF4C1D95),
        secondaryTextColor = Color(0xFF7C3AED),
        chatBubbleBg = Color(0xFFFFEDD5),
        chatBubbleText = Color(0xFF7C2D12)
    ),
    
    // 5 ORIGINAL LIGHT PALETTES (Stylishly customized)
    RoomTheme("Golden Glow", "Warm, bright and energetic yellow", false, Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFFFFB800), null, Color(0xFFD97706), Color(0xFFFFFBEB).copy(alpha = 0.8f), Color(0xFFFFF7ED), Color(0xFF78350F), Color(0xFFB45309)),
    RoomTheme("Sunset Orange", "Vibrant and playful peach", false, Color(0xFFFEF2F2), Color(0xFFFFE4E6), Color(0xFFF97316), null, Color(0xFFEA580C), Color(0xFFFEF2F2).copy(alpha = 0.8f), Color(0xFFFFE4E6), Color(0xFF7F1D1D), Color(0xFF9F1239)),
    RoomTheme("Royal Purple", "Rich, royal and classy violet", false, Color(0xFFF5F3FF), Color(0xFFEDE9FE), Color(0xFF8B5CF6), null, Color(0xFF6D28D9), Color(0xFFF5F3FF).copy(alpha = 0.8f), Color(0xFFEDE9FE), Color(0xFF4C1D95), Color(0xFF5B21B6)),
    RoomTheme("Ocean Blue", "Calm, cool and relaxing waters", false, Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFF0284C7), null, Color(0xFF0369A1), Color(0xFFF0F9FF).copy(alpha = 0.8f), Color(0xFFE0F2FE), Color(0xFF0F172A), Color(0xFF0369A1)),
    RoomTheme("Forest Green", "Fresh, natural and soothing mint", false, Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFF22C55E), null, Color(0xFF15803D), Color(0xFFF0FDF4).copy(alpha = 0.8f), Color(0xFFDCFCE7), Color(0xFF065F46), Color(0xFF166534)),
    
    // 10 ORIGINAL DARK/AMBIENT THEMES (Fully rich, distinct backgrounds)
    RoomTheme("Midnight Dark", "Sleek, deep and modern slate", true, Color(0xFF0F172A), Color(0xFF020617), Color(0xFF38BDF8), null, Color(0xFF0EA5E9), Color(0xFF1E293B).copy(alpha = 0.65f), Color(0xFF334155), Color(0xFFF8FAFC), Color(0xFF94A3B8)),
    RoomTheme("Cosmic Eclipse", "Stars and planets deep indigo", true, Color(0xFF1E1B4B), Color(0xFF030712), Color(0xFF818CF8), null, Color(0xFF6366F1), Color(0xFF1E1B4B).copy(alpha = 0.6f), Color(0xFF312E81), Color(0xFFEEF2F6), Color(0xFF818CF8)),
    RoomTheme("Cyber Violet", "Luminescent vibrant cyberpunk style", true, Color(0xFF2E1065), Color(0xFF020205), Color(0xFFF472B6), null, Color(0xFFEC4899), Color(0xFF2E1065).copy(alpha = 0.65f), Color(0xFF5B21B6), Color(0xFFFDF2F8), Color(0xFFE9D5FF)),
    RoomTheme("Velvet Forest", "Midnight green woods mystique", true, Color(0xFF064E3B), Color(0xFF020617), Color(0xFF34D399), null, Color(0xFF10B981), Color(0xFF064E3B).copy(alpha = 0.6f), Color(0xFF065F46), Color(0xFFECFDF5), Color(0xFFA7F3D0)),
    RoomTheme("Obsid Shadow", "Deep dark charcoal and ash", true, Color(0xFF1E293B), Color(0xFF0B1329), Color(0xFF94A3B8), null, Color(0xFF64748B), Color(0xFF1E293B).copy(alpha = 0.65f), Color(0xFF334155), Color(0xFFF1F5F9), Color(0xFF94A3B8)),
    RoomTheme("Deep Sea Tint", "Underwater twilight explore theme", true, Color(0xFF0369A1), Color(0xFF021526), Color(0xFF38BDF8), null, Color(0xFF0284C7), Color(0xFF0284C7).copy(alpha = 0.6f), Color(0xFF0369A1), Color(0xFFF0F9FF), Color(0xFFD0EFFF)),
    RoomTheme("Crimson Eclipse", "Blood red stellar twilight shadow", true, Color(0xFF7F1D1D), Color(0xFF020205), Color(0xFFF87171), null, Color(0xFFEF4444), Color(0xFF7F1D1D).copy(alpha = 0.65f), Color(0xFF991B1B), Color(0xFFFEF2F2), Color(0xFFFCA5A5)),
    RoomTheme("Emerald Velvet", "Luxurious rich green royalty shade", true, Color(0xFF14532D), Color(0xFF022C22), Color(0xFF4ADE80), null, Color(0xFF22C55E), Color(0xFF14532D).copy(alpha = 0.65f), Color(0xFF15803D), Color(0xFFF0FDF4), Color(0xFF86EFAC)),
    RoomTheme("Rose Dust", "Elegant sweet romantic rose shade", true, Color(0xFF4C0519), Color(0xFF030005), Color(0xFFF472B6), null, Color(0xFFF43F5E), Color(0xFF4C0519).copy(alpha = 0.65f), Color(0xFF881337), Color(0xFFFFF1F2), Color(0xFFFBCFE8)),
    RoomTheme("Satin Charcoal", "Soft satin slate aesthetic space", true, Color(0xFF374151), Color(0xFF111827), Color(0xFF9CA3AF), null, Color(0xFF9CA3AF), Color(0xFF374151).copy(alpha = 0.65f), Color(0xFF4B5563), Color(0xFFF9FAFB), Color(0xFFD1D5DB))
)

val roomGradients = listOf(
    // Name, GradStart, GradEnd
    Triple("Midnight Ocean", Color(0xFF0F172A), Color(0xFF1E1B4B)),
    Triple("Cosmic Eclipse", Color(0xFF1E1B4B), Color(0xFF030712)),
    Triple("Cyber Violet", Color(0xFF2E1065), Color(0xFF020205)),
    Triple("Velvet Forest", Color(0xFF064E3B), Color(0xFF020617)),
    Triple("Obsid Shadow", Color(0xFF1E293B), Color(0xFF0B1329)),
    Triple("Deep Sea", Color(0xFF0369A1), Color(0xFF021526)),
    Triple("Crimson Eclipse", Color(0xFF7F1D1D), Color(0xFF020205)),
    Triple("Emerald Velvet", Color(0xFF14532D), Color(0xFF022C22)),
    Triple("Rose Dust", Color(0xFF4C0519), Color(0xFF030005)),
    Triple("Satin Charcoal", Color(0xFF374151), Color(0xFF111827))
)

data class ChillRoomSpeaker(
    val name: String,
    val imageUrl: String,
    val flagCode: String,
    val isMuted: Boolean = false,
    val isCenter: Boolean = false,
    val gradientColors: List<Color>
)

data class ChillRoomMessage(
    val senderName: String,
    val senderImageUrl: String,
    val badge: String? = null,
    val badgeColor: Color = ChillPurpleAccent,
    val text: String
)

data class PrivateChatItem(
    val id: String,
    val name: String,
    val imageUrl: String,
    val flagCode: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isTyping: Boolean = false
)

data class FloatingEmojiId(val url: String, val id: Long = System.currentTimeMillis())

data class GiftItem(
    val name: String,
    val cost: Int,
    val emoji: String
)

val giftItemsList = listOf(
    GiftItem("Rose", 10, ""),
    GiftItem("Heart", 25, ""),
    GiftItem("Cake", 50, ""),
    GiftItem("Coffee", 75, ""),
    GiftItem("Mic", 100, ""),
    GiftItem("Crown", 250, ""),
    GiftItem("Rocket", 300, ""),
    GiftItem("Diamond", 500, ""),
    GiftItem("Trophy", 1000, "")
)

data class DirectMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val time: String,
    val isSentByMe: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRoomScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val speakers = listOf(
        ChillRoomSpeaker(
            name = "Arjun",
            imageUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400",
            flagCode = "in",
            isMuted = false,
            gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
        ),
        ChillRoomSpeaker(
            name = "Riya",
            imageUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
            flagCode = "in",
            isMuted = false,
            isCenter = true,
            gradientColors = listOf(Color(0xFFFFB800), Color(0xFFF59E0B), Color(0xFFFFB800))
        ),
        ChillRoomSpeaker(
            name = "Sara",
            imageUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400",
            flagCode = "pk",
            isMuted = false,
            gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
        ),
        ChillRoomSpeaker(
            name = "Vihaan",
            imageUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400",
            flagCode = "in",
            isMuted = false,
            gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
        ),
        ChillRoomSpeaker(
            name = "Ananya",
            imageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400",
            flagCode = "in",
            isMuted = true,
            gradientColors = listOf(Color(0xFFD1D5DB), Color(0xFF9CA3AF))
        )
    )

    var chatInputValue by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showReactionsBar by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showPeopleSheet by remember { mutableStateOf(false) }
    var showInviteFriendSheet by remember { mutableStateOf(false) }
    var isMicOn by remember { mutableStateOf(true) }
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showEditRoomSheet by remember { mutableStateOf(false) }
    
    // Gift Sheet State Variables
    var showGiftSheet by remember { mutableStateOf(false) }
    var selectedGift by remember { mutableStateOf(giftItemsList[0]) }
    var userCoins by remember { mutableStateOf(1250) }
    var selectedGiftTargetUser by remember { mutableStateOf("Riya") }
    var selectedGiftMultiplier by remember { mutableStateOf(1) }
    var isChangingGift by remember { mutableStateOf(false) }

    // Report Bottom Sheet Extra States
    var selectedReportReasonIndex by remember { mutableStateOf(0) }
    var reportEvidenceAudioAttached by remember { mutableStateOf(true) }
    var reportEvidenceMessagesAttached by remember { mutableStateOf(true) }
    var reportEvidenceActivityAttached by remember { mutableStateOf(true) }
    var reportScreenshotAttached by remember { mutableStateOf(false) }
    var reportVideoAttached by remember { mutableStateOf(false) }
    var reportAdditionalDetails by remember { mutableStateOf("") }
    var showReportSubmittedSheet by remember { mutableStateOf(false) }

    // 1-to-1 Chat State Variables
    var showOneToOneChatSheet by remember { mutableStateOf(false) }
    var oneToOneChatUser by remember { mutableStateOf<PrivateChatItem?>(null) }
    var oneToOneTypedMsg by remember { mutableStateOf("") }

    val directMessagesMap = remember {
        mutableStateMapOf<String, List<DirectMessage>>(
            "Sara" to listOf(
                DirectMessage("11", "Sara", "Hey 👋", "08:40 AM", false),
                DirectMessage("12", "Me", "Hi Sara! 😊", "08:41 AM", true),
                DirectMessage("13", "Sara", "Join my room in a bit?", "08:41 AM", false),
                DirectMessage("14", "Me", "Sure! 5 min me aata hu", "08:41 AM", true),
                DirectMessage("15", "Sara", "Okay 👍", "08:42 AM", false),
                DirectMessage("16", "Sara", "See you there!", "08:42 AM", false),
                DirectMessage("17", "Me", "See you! 🔥", "08:43 AM", true)
            ),
            "Riya" to listOf(
                DirectMessage("21", "Riya", "Aao na room me, bore ho rhi hu!", "08:28 AM", false),
                DirectMessage("22", "Me", "Bss 2 minut me aaya, kuch kaam tha.", "08:30 AM", true),
                DirectMessage("23", "Riya", "Yaar tum online kab aoge?", "08:32 AM", false)
            ),
            "Arjun" to listOf(
                DirectMessage("31", "Arjun", "Bro today's match was crazy!", "Yesterday", false),
                DirectMessage("32", "Me", "Yeah! Unbelievable finish inside the final over.", "Yesterday", true),
                DirectMessage("33", "Arjun", "Let's catch up later", "Yesterday", false)
            ),
            "Vihaan" to listOf(
                DirectMessage("41", "Me", "Bro did you get the notes?", "Yesterday", true),
                DirectMessage("42", "Vihaan", "Thanks! 🙌", "Yesterday", false)
            )
        )
    }
    
    // Room Config States (edited by user)
    var roomTitle by remember { mutableStateOf("Let's chat and play games") }
    var roomTags by remember { mutableStateOf("Chill") } // default tag chip from mockup
    var roomDescription by remember { mutableStateOf("A friendly chat room to share good vibes and play music.") }
    var roomLanguage by remember { mutableStateOf("Hindi + English") } // default from mockup
    var selectedThemeIndex by remember { mutableStateOf(0) } // Default: "Golden Glow" at index 0
    var isRoomDarkMode by remember { mutableStateOf(false) }
    var selectedDarkBgIndex by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val activeReactions = remember { mutableStateListOf<FloatingEmojiId>() }
    val speakerReactions = remember {
        mutableStateMapOf<String, List<FloatingEmojiId>>(
            "Arjun" to emptyList(),
            "Riya" to emptyList(),
            "Sara" to emptyList(),
            "Vihaan" to emptyList(),
            "Ananya" to emptyList()
        )
    }
    
    val privateChats = remember {
        listOf(
            PrivateChatItem("1", "Sara", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400", "in", "Typing...", "08:46 AM", 2, true, true),
            PrivateChatItem("2", "Riya", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400", "in", "Yaar tum online kab aoge?", "08:32 AM", 1, true, false),
            PrivateChatItem("3", "Arjun", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400", "in", "Let's catch up later", "Yesterday", 0, false, false),
            PrivateChatItem("4", "Vihaan", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400", "in", "Thanks! \uD83D\uDE4C", "Yesterday", 0, true, false),
            PrivateChatItem("5", "Ananya", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400", "in", "Photo", "06/12", 0, false, false),
            PrivateChatItem("6", "Daisy", "https://images.unsplash.com/photo-1524250502761-1ac6f2e30d43?w=400", "in", "hey \uD83D\uDC4B", "06/09", 0, true, false),
            PrivateChatItem("7", "Reymon", "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=400", "in", "Invite to Join the Clan", "05/29", 0, false, false)
        )
    }

    val fluentEmojis = listOf(
        "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Smiling%20face%20with%20hearts/3D/smiling_face_with_hearts_3d.png" to "🥰",
        "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Smiling%20face%20with%20sunglasses/3D/smiling_face_with_sunglasses_3d.png" to "😎",
        "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Fire/3D/fire_3d.png" to "🔥",
        "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Partying%20face/3D/partying_face_3d.png" to "🥳",
        "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Red%20heart/3D/red_heart_3d.png" to "❤️",
        "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Face%20with%20tears%20of%20joy/3D/face_with_tears_of_joy_3d.png" to "😂",
        "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Sparkles/3D/sparkles_3d.png" to "✨",
        "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Party%20popper/3D/party_popper_3d.png" to "🎉",
        "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Star-struck/3D/star-struck_3d.png" to "🤩",
        "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Ghost/3D/ghost_3d.png" to "👻"
    )

    val messages = remember { mutableStateListOf(
        ChillRoomMessage("Raj", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200", "Moderator", ChillPurpleAccent, "Hello everyone! 👋"),
        ChillRoomMessage("Sara", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200", "Speaker", ChillPurpleAccent, "Heyyy! Good to see you all ❤️"),
        ChillRoomMessage("Vihaan", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200", null, ChillPurpleAccent, "Let's have a fun conversation 🎉"),
        ChillRoomMessage("Ananya", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200", null, ChillPurpleAccent, "Excited to be here! ✨")
    )}

    val inlineContentMap = remember {
        fluentEmojis.associate { (url, unicode) ->
            unicode to androidx.compose.foundation.text.InlineTextContent(
                androidx.compose.ui.text.Placeholder(
                    width = 24.sp,
                    height = 24.sp,
                    placeholderVerticalAlign = androidx.compose.ui.text.PlaceholderVerticalAlign.Center
                )
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = unicode,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Dynamic Adaptive Background
        val currentTheme = roomThemes[selectedThemeIndex]
        if (currentTheme.bgImageResId != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = currentTheme.bgImageResId),
                    contentDescription = "Theme Wallpaper background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Mild dynamic overlay of selected theme gradient to tint beautifully
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    currentTheme.startColor.copy(alpha = 0.40f),
                                    currentTheme.endColor.copy(alpha = 0.75f)
                                )
                            )
                        )
                )
            }
        } else if (isRoomDarkMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(currentTheme.startColor, currentTheme.endColor)
                        )
                    )
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.chill_vibes_room_bg_1781561393087),
                    contentDescription = "Room background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Mild dynamic overlay of selected light theme gradient to tint beautifully
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    currentTheme.startColor.copy(alpha = 0.55f),
                                    currentTheme.endColor.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
            }
        }

        // Overlay to ensure readable bottom text in both Light and Dark room modes
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (currentTheme.isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.70f),
                                Color.Black.copy(alpha = 0.94f),
                                Color.Black
                            ),
                            startY = 400f,
                            endY = 2200f
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.85f),
                                Color.White.copy(alpha = 0.96f),
                                Color.White
                            ),
                            startY = 400f,
                            endY = 2200f
                        )
                    }
                )
        )

        // Layout Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 44.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // 1. TOP HEADER SECTION (Polished & Interactive)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Light/Dark Theme Pill Title (Interactive Edit Room Trigger)
                Row(
                    modifier = Modifier
                        .shadow(2.dp, RoundedCornerShape(50.dp))
                        .background(
                            currentTheme.cardBgColor,
                            RoundedCornerShape(50.dp)
                        )
                        .border(
                            1.dp,
                            currentTheme.cardBorderColor,
                            RoundedCornerShape(50.dp)
                        )
                        .clickable { showEditRoomSheet = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = currentTheme.primaryTextColor,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBack() }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column(modifier = Modifier.widthIn(max = 120.dp)) {
                        Text(
                            text = roomTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "ID:FT5272",
                            fontSize = 11.sp,
                            color = currentTheme.secondaryTextColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    CustomBriefcaseOutlineIcon(
                        tint = currentTheme.primaryTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Right action buttons (Share, Action Overflow)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Improved Share Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(2.dp, CircleShape)
                            .background(currentTheme.cardBgColor, CircleShape)
                            .border(1.dp, currentTheme.cardBorderColor, CircleShape)
                            .clickable {
                                Toast.makeText(context, "Room link copied! Share with your friends.", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        CustomShareArrowIcon(tint = currentTheme.primaryTextColor, modifier = Modifier.size(18.dp))
                    }
                    
                    // More (DropdownMenu Container)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(2.dp, CircleShape)
                            .background(currentTheme.cardBgColor, CircleShape)
                            .border(1.dp, currentTheme.cardBorderColor, CircleShape)
                            .clickable { showThreeDotMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = currentTheme.primaryTextColor,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        DropdownMenu(
                            expanded = showThreeDotMenu,
                            onDismissRequest = { showThreeDotMenu = false },
                            modifier = Modifier
                                .background(currentTheme.cardBgColor, RoundedCornerShape(16.dp))
                                .border(
                                    width = 1.dp,
                                    color = currentTheme.cardBorderColor,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(vertical = 4.dp)
                                .width(190.dp)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(currentTheme.accentColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CustomFlagIcon(
                                            tint = currentTheme.accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                text = { 
                                    Text(
                                        text = "Report Room", 
                                        color = currentTheme.primaryTextColor, 
                                        fontSize = 14.sp, 
                                        fontWeight = FontWeight.SemiBold
                                    ) 
                                },
                                onClick = { 
                                    showThreeDotMenu = false
                                    showReportDialog = true
                                },
                                colors = androidx.compose.material3.MenuDefaults.itemColors(
                                    textColor = currentTheme.primaryTextColor
                                )
                            )
                            HorizontalDivider(
                                color = currentTheme.cardBorderColor, 
                                thickness = 1.dp
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isRoomDarkMode) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFFFEF2F2)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CustomLeaveIcon(
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                text = { 
                                    Text(
                                        text = "Leave Room", 
                                        color = Color(0xFFEF4444), 
                                        fontSize = 14.sp, 
                                        fontWeight = FontWeight.Bold
                                    ) 
                                },
                                onClick = {
                                    showThreeDotMenu = false
                                    onBack()
                                },
                                colors = androidx.compose.material3.MenuDefaults.itemColors(
                                    textColor = Color(0xFFEF4444)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. SPEAKERS VISUAL SYSTEM
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // First Row - 3 Speakers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    VoiceSpeakerItem(
                        speaker = speakers[0],
                        reactions = speakerReactions[speakers[0].name] ?: emptyList(),
                        theme = currentTheme,
                        isDarkMode = isRoomDarkMode,
                        onReactionComplete = { rid ->
                            speakerReactions[speakers[0].name] = (speakerReactions[speakers[0].name] ?: emptyList()).filter { it.id != rid }
                        },
                        onClick = {
                            oneToOneChatUser = PrivateChatItem("1", speakers[0].name, speakers[0].imageUrl, speakers[0].flagCode, "Typing...", "Now", 0, true, false)
                            showOneToOneChatSheet = true
                        }
                    )
                    VoiceSpeakerItem(
                        speaker = speakers[1],
                        reactions = speakerReactions[speakers[1].name] ?: emptyList(),
                        theme = currentTheme,
                        isDarkMode = isRoomDarkMode,
                        onReactionComplete = { rid ->
                            speakerReactions[speakers[1].name] = (speakerReactions[speakers[1].name] ?: emptyList()).filter { it.id != rid }
                        },
                        onClick = {
                            oneToOneChatUser = PrivateChatItem("2", speakers[1].name, speakers[1].imageUrl, speakers[1].flagCode, "", "Now", 0, true, false)
                            showOneToOneChatSheet = true
                        }
                    )
                    VoiceSpeakerItem(
                        speaker = speakers[2],
                        reactions = speakerReactions[speakers[2].name] ?: emptyList(),
                        theme = currentTheme,
                        isDarkMode = isRoomDarkMode,
                        onReactionComplete = { rid ->
                            speakerReactions[speakers[2].name] = (speakerReactions[speakers[2].name] ?: emptyList()).filter { it.id != rid }
                        },
                        onClick = {
                            oneToOneChatUser = PrivateChatItem("3", speakers[2].name, speakers[2].imageUrl, speakers[2].flagCode, "", "Now", 0, true, false)
                            showOneToOneChatSheet = true
                        }
                    )
                }

                // Second Row - 2 Speakers + Request
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    VoiceSpeakerItem(
                        speaker = speakers[3],
                        reactions = speakerReactions[speakers[3].name] ?: emptyList(),
                        theme = currentTheme,
                        isDarkMode = isRoomDarkMode,
                        onReactionComplete = { rid ->
                            speakerReactions[speakers[3].name] = (speakerReactions[speakers[3].name] ?: emptyList()).filter { it.id != rid }
                        },
                        onClick = {
                            oneToOneChatUser = PrivateChatItem("4", speakers[3].name, speakers[3].imageUrl, speakers[3].flagCode, "", "Now", 0, true, false)
                            showOneToOneChatSheet = true
                        }
                    )
                    VoiceSpeakerItem(
                        speaker = speakers[4],
                        reactions = speakerReactions[speakers[4].name] ?: emptyList(),
                        theme = currentTheme,
                        isDarkMode = isRoomDarkMode,
                        onReactionComplete = { rid ->
                            speakerReactions[speakers[4].name] = (speakerReactions[speakers[4].name] ?: emptyList()).filter { it.id != rid }
                        },
                        onClick = {
                            oneToOneChatUser = PrivateChatItem("5", speakers[4].name, speakers[4].imageUrl, speakers[4].flagCode, "", "Now", 0, true, false)
                            showOneToOneChatSheet = true
                        }
                    )

                    // Request Slot
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(82.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .drawBehind {
                                    drawCircle(
                                        color = currentTheme.accentColor.copy(alpha = 0.45f),
                                        style = Stroke(
                                            width = 1.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                                        )
                                    )
                                }
                                .clip(CircleShape)
                                .clickable {
                                    Toast.makeText(context, "Requested to join!", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Request slot",
                                tint = currentTheme.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Request",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = currentTheme.secondaryTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 3. MINIMAL COMPACT CHAT LOG ROW
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            AsyncImage(
                                model = msg.senderImageUrl,
                                contentDescription = msg.senderName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = msg.senderName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = msg.badgeColor
                                    )
                                    if (msg.badge != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(msg.badgeColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = msg.badge,
                                                fontSize = 9.sp,
                                                color = msg.badgeColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = msg.text.toFluentAnnotatedString(fluentEmojis),
                                    inlineContent = inlineContentMap,
                                    fontSize = 14.sp,
                                    color = currentTheme.primaryTextColor,
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }
                }
                
                // Top fading gradient to make it look like older chats disappear
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    if (currentTheme.isDark) Color.Black else Color.White,
                                    (if (currentTheme.isDark) Color.Black else Color.White).copy(alpha = 0.85f),
                                    (if (currentTheme.isDark) Color.Black else Color.White).copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            )
                        )
                        .align(Alignment.TopCenter)
                )
            }

            androidx.compose.animation.AnimatedVisibility(visible = showEmojiPicker) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .background(Color.White.copy(alpha=0.95f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(fluentEmojis.size) { index ->
                        val (url, unicode) = fluentEmojis[index]
                        AsyncImage(
                            model = url,
                            contentDescription = "Emoji",
                            modifier = Modifier
                                .size(42.dp)
                                .clickable {
                                    chatInputValue += unicode
                                }
                        )
                    }
                }
            }

            // 4. ACTION BAR / SAY SOMETHING
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(
                            currentTheme.cardBgColor,
                            RoundedCornerShape(100.dp)
                        )
                        .border(
                            1.dp,
                            currentTheme.cardBorderColor,
                            RoundedCornerShape(100.dp)
                        )
                        .padding(start = 16.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = chatInputValue,
                        onValueChange = { chatInputValue = it },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = currentTheme.primaryTextColor
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(currentTheme.primaryTextColor),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSend = {
                                if (chatInputValue.isNotBlank()) {
                                    messages.add(ChillRoomMessage("You", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200", null, ChillGoldIcon, chatInputValue))
                                    chatInputValue = ""
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (chatInputValue.isEmpty()) {
                                    Text(
                                        text = "Say something...",
                                        color = currentTheme.secondaryTextColor,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    CustomMoodSmileOutline(
                        tint = if (showEmojiPicker) currentTheme.accentColor else currentTheme.secondaryTextColor, 
                        modifier = Modifier.size(24.dp).clickable { showEmojiPicker = !showEmojiPicker }.padding(2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(1.dp, CircleShape)
                        .clip(CircleShape)
                        .background(currentTheme.cardBgColor)
                        .border(
                            1.dp,
                            currentTheme.cardBorderColor,
                            CircleShape
                        )
                        .clickable { showGiftSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    CustomGiftBoxIcon(tint = Color(0xFFEC4899), modifier = Modifier.size(24.dp))
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = showReactionsBar) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .background(Color.White.copy(alpha=0.95f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(fluentEmojis.size) { index ->
                        val (url, _) = fluentEmojis[index]
                        AsyncImage(
                            model = url,
                            contentDescription = "Reaction",
                            modifier = Modifier
                                .size(42.dp)
                                .clickable {
                                    val randomSpeaker = speakers.random().name
                                    val current = speakerReactions[randomSpeaker] ?: emptyList()
                                    speakerReactions[randomSpeaker] = current + FloatingEmojiId(url, System.currentTimeMillis() + index)
                                    
                                    // Make the room feel alive by randomly triggering a second speaker occasionally
                                    if (Math.random() > 0.4) {
                                        val anotherSpeaker = speakers.random().name
                                        if (anotherSpeaker != randomSpeaker) {
                                            val anotherCurrent = speakerReactions[anotherSpeaker] ?: emptyList()
                                            speakerReactions[anotherSpeaker] = anotherCurrent + FloatingEmojiId(url, System.currentTimeMillis() + index + 99)
                                        }
                                    }
                                    showReactionsBar = false
                                }
                        )
                    }
                }
            }

            // 5. BOTTOM NAVIGATION BAR SYSTEM
            val inactiveIconTint = if (isRoomDarkMode) Color(0xFFD1D5DB) else Color.Gray
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VoiceBottomBarItem(
                    icon = { CustomChatBubbleIcon(tint = if (isRoomDarkMode) Color(0xFFC084FC) else ChillPurpleAccent, modifier = Modifier.size(24.dp)) },
                    label = "Chat",
                    tint = if (isRoomDarkMode) Color(0xFFC084FC) else ChillPurpleAccent,
                    hasDot = true,
                    onClick = { showChatSheet = true }
                )
                VoiceBottomBarItem(
                    icon = { CustomPeopleGroupIcon(tint = inactiveIconTint, modifier = Modifier.size(26.dp)) },
                    label = "People",
                    tint = inactiveIconTint,
                    badgeText = "24",
                    onClick = { showPeopleSheet = true }
                )

                // Main Mic Action Bubble
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = (-6).dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(if (isMicOn) ChillGoldIcon else Color(0xFFE53935))
                            .clickable { isMicOn = !isMicOn },
                        contentAlignment = Alignment.Center
                    ) {
                        CustomMicIcon(tint = if (isMicOn) Color.Black else Color.White, modifier = Modifier.size(28.dp), isMuted = !isMicOn)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isMicOn) "Mic On" else "Mic Off",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMicOn) ChillGoldIcon else Color(0xFFE53935)
                    )
                }

                VoiceBottomBarItem(
                    icon = { CustomReactionSmileIcon(tint = inactiveIconTint, modifier = Modifier.size(24.dp)) },
                    label = "Reactions",
                    tint = inactiveIconTint,
                    onClick = { showReactionsBar = !showReactionsBar }
                )
                var showMoreFeaturesSheet by remember { mutableStateOf(false) }
                VoiceBottomBarItem(
                    icon = { CustomGridMoreIcon(tint = inactiveIconTint, modifier = Modifier.size(22.dp)) },
                    label = "More",
                    tint = inactiveIconTint,
                    onClick = { showMoreFeaturesSheet = true }
                )
                
                if (showMoreFeaturesSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showMoreFeaturesSheet = false },
                        sheetState = sheetState,
                        containerColor = currentTheme.cardBgColor,
                        dragHandle = { BottomSheetDefaults.DragHandle() },
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        MoreFeaturesBottomSheetContent(
                            currentThemeIndex = selectedThemeIndex,
                            isDarkMode = currentTheme.isDark,
                            onThemeSelect = { selectedThemeIndex = it },
                            onDismiss = { showMoreFeaturesSheet = false }
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        activeReactions.toList().forEach { reaction ->
            androidx.compose.runtime.key(reaction.id) {
                FloatingEmojiAnimation(url = reaction.url, onComplete = { activeReactions.remove(reaction) })
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    if (showChatSheet) {
        val currentTheme = roomThemes[selectedThemeIndex]
        ModalBottomSheet(
            onDismissRequest = { showChatSheet = false },
            sheetState = sheetState,
            containerColor = currentTheme.startColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = if (currentTheme.isDark) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f)
                )
            }
        ) {
            ChatBottomSheetContent(
                privateChats = privateChats,
                selectedThemeIndex = selectedThemeIndex,
                onChatClick = { chatUser ->
                    oneToOneChatUser = chatUser
                    showOneToOneChatSheet = true
                    showChatSheet = false
                }
            )
        }
    }

    if (showPeopleSheet) {
        val currentTheme = roomThemes[selectedThemeIndex]
        ModalBottomSheet(
            onDismissRequest = { showPeopleSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = currentTheme.startColor,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                if (currentTheme.isDark) Color.White.copy(alpha = 0.3f) else Color(0xFFE5E5EA),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        ) {
            PeopleBottomSheetContent(
                selectedThemeIndex = selectedThemeIndex,
                onUserClick = { chatUser ->
                    oneToOneChatUser = chatUser
                    showOneToOneChatSheet = true
                    showPeopleSheet = false
                },
                onInviteClick = {
                    showInviteFriendSheet = true
                    showPeopleSheet = false
                },
                onDismiss = { showPeopleSheet = false }
            )
        }
    }

    if (showInviteFriendSheet) {
        val currentTheme = roomThemes[selectedThemeIndex]
        ModalBottomSheet(
            onDismissRequest = { showInviteFriendSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = currentTheme.cardBgColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            InviteFriendBottomSheetContent(
                isDarkMode = currentTheme.isDark,
                onDismiss = { showInviteFriendSheet = false }
            )
        }
    }

    if (showEditRoomSheet) {
        val currentTheme = roomThemes[selectedThemeIndex]
        ModalBottomSheet(
            onDismissRequest = { showEditRoomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = currentTheme.startColor,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                if (currentTheme.isDark) Color.White.copy(alpha = 0.3f) else Color(0xFFE5E5EA),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        ) {
            EditRoomBottomSheetContent(
                roomTitle = roomTitle,
                onTitleChange = { roomTitle = it },
                roomTags = roomTags,
                onTagsChange = { roomTags = it },
                roomDescription = roomDescription,
                onDescriptionChange = { roomDescription = it },
                roomLanguage = roomLanguage,
                onLanguageChange = { roomLanguage = it },
                selectedThemeIndex = selectedThemeIndex,
                onThemeChange = { index ->
                    selectedThemeIndex = index
                    isRoomDarkMode = roomThemes[index].isDark
                },
                onDismiss = { showEditRoomSheet = false }
            )
        }
    }

    // 1) Gift Bottom Sheet container
    @OptIn(ExperimentalMaterial3Api::class)
    if (showGiftSheet) {
        val currentTheme = roomThemes[selectedThemeIndex]
        ModalBottomSheet(
            onDismissRequest = { showGiftSheet = false },
            containerColor = currentTheme.startColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = if (currentTheme.isDark) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f)
                )
            }
        ) {
            GiftBottomSheetContent(
                selectedThemeIndex = selectedThemeIndex,
                coins = userCoins,
                selectedGift = selectedGift,
                onSelectedGiftChange = { selectedGift = it },
                onCoinsChange = { userCoins = it },
                selectedTarget = selectedGiftTargetUser,
                onTargetChange = { selectedGiftTargetUser = it },
                multiplier = selectedGiftMultiplier,
                onMultiplierChange = { selectedGiftMultiplier = it },
                isChangingGift = isChangingGift,
                onChangingGiftChange = { isChangingGift = it },
                onDismiss = { showGiftSheet = false },
                onGiftSent = { gift, target, mult ->
                    val finalCost = gift.cost * mult
                    if (userCoins >= finalCost) {
                        userCoins -= finalCost
                        Toast.makeText(context, "Sent $mult ${gift.name} to $target successfully! 🎁", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Insufficient Coins! Tap + icon to add coins.", Toast.LENGTH_LONG).show()
                    }
                    showGiftSheet = false
                }
            )
        }
    }

    // 2) Report Room Bottom Sheet container
    @OptIn(ExperimentalMaterial3Api::class)
    if (showReportDialog) {
        val currentTheme = roomThemes[selectedThemeIndex]
        ModalBottomSheet(
            onDismissRequest = { showReportDialog = false },
            containerColor = currentTheme.startColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null
        ) {
            ReportRoomBottomSheetContent(
                selectedThemeIndex = selectedThemeIndex,
                selectedReasonIndex = selectedReportReasonIndex,
                onReasonIndexChange = { selectedReportReasonIndex = it },
                audioAttached = reportEvidenceAudioAttached,
                onAudioAttachChange = { reportEvidenceAudioAttached = it },
                messagesAttached = reportEvidenceMessagesAttached,
                onMessagesAttachChange = { reportEvidenceMessagesAttached = it },
                activityAttached = reportEvidenceActivityAttached,
                onActivityAttachChange = { reportEvidenceActivityAttached = it },
                screenshotAttached = reportScreenshotAttached,
                onScreenshotAttachChange = { reportScreenshotAttached = it },
                videoAttached = reportVideoAttached,
                onVideoAttachChange = { reportVideoAttached = it },
                additionalDetails = reportAdditionalDetails,
                onDetailsChange = { reportAdditionalDetails = it },
                onDismiss = { showReportDialog = false },
                onSubmit = {
                    showReportDialog = false
                    showReportSubmittedSheet = true
                }
            )
        }
    }

    // 3) Report Submitted Sheet container
    @OptIn(ExperimentalMaterial3Api::class)
    if (showReportSubmittedSheet) {
        val currentTheme = roomThemes[selectedThemeIndex]
        ModalBottomSheet(
            onDismissRequest = { showReportSubmittedSheet = false },
            containerColor = currentTheme.startColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null
        ) {
            ReportSubmittedBottomSheetContent(
                selectedThemeIndex = selectedThemeIndex,
                onDismiss = { showReportSubmittedSheet = false }
            )
        }
    }

    // 4) 1-to-1 Chat Bottom Sheet container
    @OptIn(ExperimentalMaterial3Api::class)
    if (showOneToOneChatSheet) {
        val currentTheme = roomThemes[selectedThemeIndex]
        val activeChatUser = oneToOneChatUser ?: PrivateChatItem("1", "Riya", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400", "in", "Started chat", "Now", 0, true, false)
        val msgs = directMessagesMap[activeChatUser.name] ?: emptyList()

        ModalBottomSheet(
            onDismissRequest = { showOneToOneChatSheet = false },
            containerColor = currentTheme.startColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null
        ) {
            OneToOneChatBottomSheetContent(
                selectedThemeIndex = selectedThemeIndex,
                chatUser = activeChatUser,
                messagesList = msgs,
                onSendMessage = { txt ->
                    val newList = msgs + DirectMessage(
                        id = (msgs.size + 1).toString(),
                        senderName = "Me",
                        text = txt,
                        time = "08:42 AM",
                        isSentByMe = true
                    )
                    directMessagesMap[activeChatUser.name] = newList
                },
                onBack = { showOneToOneChatSheet = false }
            )
        }
    }
}

@Composable
fun ChatBottomSheetContent(privateChats: List<PrivateChatItem>, selectedThemeIndex: Int, onChatClick: (PrivateChatItem) -> Unit) {
    val currentTheme = roomThemes[selectedThemeIndex]
    var activeFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredChats = privateChats.filter { 
        when(activeFilter) {
            "Unread" -> it.unreadCount > 0
            "Online" -> it.isOnline
            else -> true
        }
    }.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .background(
                Brush.verticalGradient(
                    colors = listOf(currentTheme.startColor, currentTheme.endColor)
                )
            )
    ) {
        // Title
        Text(
            text = "Chats",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (currentTheme.isDark) Color.White else Color.Black,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        
        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            placeholder = { Text("Search chats or users", color = if (currentTheme.isDark) Color.White.copy(alpha = 0.5f) else Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = if (currentTheme.isDark) Color.White.copy(alpha = 0.6f) else Color.Gray) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = if (currentTheme.isDark) Color.Black.copy(alpha = 0.25f) else Color(0xFFF9FAFB),
                unfocusedContainerColor = if (currentTheme.isDark) Color.Black.copy(alpha = 0.25f) else Color(0xFFF9FAFB),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = if (currentTheme.isDark) Color.White else Color.Black,
                focusedTextColor = if (currentTheme.isDark) Color.White else Color.Black,
                unfocusedTextColor = if (currentTheme.isDark) Color.White else Color.Black
            ),
            shape = RoundedCornerShape(100.dp)
        )
        
        // Filter Chips
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // All
            Box(
                modifier = Modifier
                    .background(
                        if (activeFilter == "All") (if (currentTheme.isDark) currentTheme.themeColor else ChillGoldIcon) else Color.Transparent,
                        RoundedCornerShape(100.dp)
                    )
                    .border(
                        if (activeFilter == "All") 0.dp else 1.dp,
                        if (currentTheme.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFE5E5EA),
                        RoundedCornerShape(100.dp)
                    )
                    .clickable { activeFilter = "All" }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "All",
                    fontWeight = if (activeFilter == "All") FontWeight.Bold else FontWeight.Medium,
                    color = if (activeFilter == "All") Color.White else (if (currentTheme.isDark) Color.White.copy(alpha = 0.7f) else Color.Black),
                    fontSize = 14.sp
                )
            }
            // Unread
            Row(
                modifier = Modifier
                    .background(
                        if (activeFilter == "Unread") (if (currentTheme.isDark) currentTheme.themeColor else ChillGoldIcon) else Color.Transparent,
                        RoundedCornerShape(100.dp)
                    )
                    .border(
                        if (activeFilter == "Unread") 0.dp else 1.dp,
                        if (currentTheme.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFE5E5EA),
                        RoundedCornerShape(100.dp)
                    )
                    .clickable { activeFilter = "Unread" }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Unread",
                    fontWeight = if (activeFilter == "Unread") FontWeight.Bold else FontWeight.Medium,
                    color = if (activeFilter == "Unread") Color.White else (if (currentTheme.isDark) Color.White.copy(alpha = 0.7f) else Color.Black),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (activeFilter == "Unread") Color.White else (if (currentTheme.isDark) currentTheme.themeColor else ChillGoldIcon),
                            CircleShape
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    val unreadTotal = privateChats.count { it.unreadCount > 0 }
                    Text(
                        text = unreadTotal.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeFilter == "Unread") (if (currentTheme.isDark) currentTheme.themeColor else Color.Black) else Color.White
                    )
                }
            }
            // Online
            Row(
                modifier = Modifier
                    .background(
                        if (activeFilter == "Online") (if (currentTheme.isDark) currentTheme.themeColor else ChillGoldIcon) else Color.Transparent,
                        RoundedCornerShape(100.dp)
                    )
                    .border(
                        if (activeFilter == "Online") 0.dp else 1.dp,
                        if (currentTheme.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFE5E5EA),
                        RoundedCornerShape(100.dp)
                    )
                    .clickable { activeFilter = "Online" }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Online",
                    fontWeight = if (activeFilter == "Online") FontWeight.Bold else FontWeight.Medium,
                    color = if (activeFilter == "Online") Color.White else (if (currentTheme.isDark) Color.White.copy(alpha = 0.7f) else Color.Black),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(8.dp).background(LiveGreen, CircleShape))
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // LazyColumn for private chats
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredChats) { chat ->
                PrivateChatItemRow(chat, selectedThemeIndex) {
                    onChatClick(chat)
                }
            }
        }
    }
}

@Composable
fun PrivateChatItemRow(chat: PrivateChatItem, selectedThemeIndex: Int, onClick: () -> Unit) {
    val currentTheme = roomThemes[selectedThemeIndex]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image
        Box {
            AsyncImage(
                model = chat.imageUrl,
                contentDescription = chat.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (currentTheme.isDark) Color.White.copy(alpha = 0.1f) else Color.LightGray)
            )
            // country flag
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(20.dp)
                    .shadow(1.dp, CircleShape)
                    .clip(CircleShape)
                    .background(if (currentTheme.isDark) Color(0xFF1E293B) else Color.White)
                    .border(1.5.dp, if (currentTheme.isDark) Color(0xFF1E293B) else Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                 AsyncImage(
                      model = "https://flagcdn.com/w40/${chat.flagCode}.png",
                      contentDescription = "Flag",
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize()
                 ) 
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (currentTheme.isDark) Color.White else Color.Black
                )
                if (chat.isOnline) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(8.dp).background(LiveGreen, CircleShape))
                } else {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(8.dp).background(if (currentTheme.isDark) Color.White.copy(alpha = 0.3f) else Color.LightGray, CircleShape))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (chat.isTyping) {
                Text(chat.lastMessage, color = if (currentTheme.isDark) currentTheme.themeColor else ChillGoldIcon, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            } else {
                Text(
                    text = chat.lastMessage,
                    color = if (currentTheme.isDark) Color.White.copy(alpha = 0.6f) else Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(chat.time, fontSize = 12.sp, color = if (currentTheme.isDark) Color.White.copy(alpha = 0.5f) else Color.Gray)
            Spacer(modifier = Modifier.height(6.dp))
            if (chat.unreadCount > 0) {
                Box(
                    contentAlignment = Alignment.Center, 
                    modifier = Modifier
                        .size(20.dp)
                        .background(if (currentTheme.isDark) currentTheme.themeColor else ChillGoldIcon, CircleShape)
                ) {
                    Text(
                        text = chat.unreadCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceSpeakerItem(
    speaker: ChillRoomSpeaker,
    reactions: List<FloatingEmojiId> = emptyList(),
    theme: RoomTheme? = null,
    isDarkMode: Boolean = false,
    onReactionComplete: (Long) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val size = if (speaker.isCenter) 94.dp else 80.dp
    val primaryTextColor = theme?.primaryTextColor ?: (if (isDarkMode) Color.White else Color.Black)
    val micIconTint = theme?.accentColor ?: ChillPurpleAccent
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(if (speaker.isCenter) 100.dp else 86.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors = speaker.gradientColors))
                    .padding(if (speaker.isCenter) 3.5.dp else 2.5.dp)
            ) {
                AsyncImage(
                    model = speaker.imageUrl,
                    contentDescription = speaker.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )
            }

            // Circular Image Flag
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(26.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://flagcdn.com/w40/${speaker.flagCode}.png",
                    contentDescription = "Flag",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Speaker floating reactions overlay starting from avatar center and rising upward beautifully
            reactions.forEach { reaction ->
                key(reaction.id) {
                    FloatingSpeakerEmoji(url = reaction.url, onComplete = { onReactionComplete(reaction.id) })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = speaker.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            CustomMicIcon(
                tint = if (speaker.isMuted) Color.LightGray else micIconTint,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun FloatingSpeakerEmoji(url: String, onComplete: () -> Unit) {
    var isStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isStarted = true
        kotlinx.coroutines.delay(1800)
        onComplete()
    }
    
    val offsetY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isStarted) -180f else 0f, 
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1800, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        label = "offsetY"
    )
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isStarted) 0f else 1f, 
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1800, easing = androidx.compose.animation.core.LinearEasing),
        label = "alpha"
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isStarted) 1.5f else 0.5f, 
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1800, easing = androidx.compose.animation.core.EaseOutBack),
        label = "scale"
    )
    
    val wobbleX = remember { (-25..25).random().dp }
    
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .offset(x = wobbleX, y = offsetY.dp)
            .size(40.dp)
            .scale(scale)
            .alpha(alpha)
    )
}

@Composable
fun VoiceBottomBarItem(
    icon: @Composable () -> Unit,
    label: String,
    tint: Color,
    hasDot: Boolean = false,
    badgeText: String? = null,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        Box {
            Box(
                modifier = Modifier.padding(top = 2.dp, end = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            if (hasDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(8.dp)
                        .background(Color.Red, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 12.dp, y = (-4).dp)
                        .background(ChillPurpleAccent, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = tint,
            fontWeight = if (tint != Color.Gray) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// ----------------------------------------
// CUSTOM HAND-DRAWN ICONS SYSTEM
// ----------------------------------------

@Composable
fun CustomChatBubbleIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val corner = w * 0.25f
        
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, 0f),
            size = Size(w, h * 0.75f),
            cornerRadius = CornerRadius(corner, corner)
        )
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.7f)
            lineTo(w * 0.15f, h * 0.95f)
            lineTo(w * 0.4f, h * 0.75f)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
fun CustomPeopleGroupIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        drawCircle(color = tint, radius = w * 0.18f, center = Offset(w * 0.3f, h * 0.25f))
        val pathLeft = Path().apply {
            moveTo(0f, h * 0.9f)
            quadraticBezierTo(w * 0.05f, h * 0.55f, w * 0.3f, h * 0.55f)
            quadraticBezierTo(w * 0.55f, h * 0.55f, w * 0.6f, h * 0.9f)
            close()
        }
        drawPath(pathLeft, color = tint)

        drawCircle(color = Color.White, radius = w * 0.2f, center = Offset(w * 0.7f, h * 0.3f))
        drawCircle(color = tint, radius = w * 0.15f, center = Offset(w * 0.7f, h * 0.3f))
        
        val backPerson = Path().apply {
            moveTo(w * 0.4f, h * 0.95f)
            quadraticBezierTo(w * 0.45f, h * 0.65f, w * 0.7f, h * 0.65f)
            quadraticBezierTo(w * 0.95f, h * 0.65f, w * 1f, h * 0.95f)
            close()
        }
        drawPath(backPerson, color = Color.White, style = Stroke(width = w * 0.06f))
        drawPath(backPerson, color = tint)
    }
}

@Composable
fun CustomMicIcon(tint: Color, modifier: Modifier = Modifier, isMuted: Boolean = false) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.35f, h * 0.1f),
            size = Size(w * 0.3f, h * 0.5f),
            cornerRadius = CornerRadius(w * 0.15f)
        )
        
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.45f)
            quadraticBezierTo(w * 0.2f, h * 0.75f, w * 0.5f, h * 0.75f)
            quadraticBezierTo(w * 0.8f, h * 0.75f, w * 0.8f, h * 0.45f)
        }
        drawPath(path, color = tint, style = stroke)
        drawLine(color = tint, start = Offset(w * 0.5f, h * 0.75f), end = Offset(w * 0.5f, h * 0.95f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        
        if (isMuted) {
            drawLine(
                color = Color.White,
                start = Offset(w * 0.15f, h * 0.15f),
                end = Offset(w * 0.85f, h * 0.85f),
                strokeWidth = w * 0.1f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFE53935),
                start = Offset(w * 0.18f, h * 0.18f),
                end = Offset(w * 0.82f, h * 0.82f),
                strokeWidth = w * 0.05f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun CustomReactionSmileIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        drawCircle(color = tint, radius = w/2, center = Offset(w/2, h/2))
        drawCircle(color = Color.White, radius = w * 0.1f, center = Offset(w * 0.35f, h * 0.4f))
        drawCircle(color = Color.White, radius = w * 0.1f, center = Offset(w * 0.65f, h * 0.4f))
        
        val path = Path().apply {
            moveTo(w * 0.3f, h * 0.6f)
            quadraticBezierTo(w * 0.5f, h * 0.75f, w * 0.7f, h * 0.6f)
        }
        drawPath(path, color = Color.White, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
    }
}

@Composable
fun CustomGridMoreIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val box = w * 0.35f
        val radius = CornerRadius(w * 0.1f)
        
        drawRoundRect(color = tint, topLeft = Offset(w*0.1f, w*0.1f), size = Size(box, box), cornerRadius = radius)
        drawRoundRect(color = tint, topLeft = Offset(w*0.55f, w*0.1f), size = Size(box, box), cornerRadius = radius)
        drawRoundRect(color = tint, topLeft = Offset(w*0.1f, w*0.55f), size = Size(box, box), cornerRadius = radius)
        drawRoundRect(color = tint, topLeft = Offset(w*0.55f, w*0.55f), size = Size(box, box), cornerRadius = radius)
    }
}

@Composable
fun CustomMoodSmileOutline(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = tint, radius = w/2 - w*0.05f, center = Offset(w/2, h/2), style = Stroke(width = w*0.08f))
        
        drawCircle(color = tint, radius = w * 0.08f, center = Offset(w * 0.35f, h * 0.4f))
        drawCircle(color = tint, radius = w * 0.08f, center = Offset(w * 0.65f, h * 0.4f))
        
        val path = Path().apply {
            moveTo(w * 0.3f, h * 0.6f)
            quadraticBezierTo(w * 0.5f, h * 0.75f, w * 0.7f, h * 0.6f)
        }
        drawPath(path, color = tint, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
    }
}

@Composable
fun CustomGiftBoxIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val boxW = w * 0.7f
        val boxH = h * 0.5f
        val topX = (w - boxW) / 2
        val topY = h * 0.4f
        
        drawRoundRect(
            color = tint,
            topLeft = Offset(topX, topY),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(w * 0.1f)
        )
        drawRect(
            color = Color.White.copy(alpha=0.9f),
            topLeft = Offset(w / 2 - w * 0.08f, topY),
            size = Size(w * 0.16f, boxH)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(topX - w*0.05f, topY),
            size = Size(boxW + w*0.1f, h * 0.2f),
            cornerRadius = CornerRadius(w * 0.05f)
        )
        val bowPath = Path().apply {
            moveTo(w * 0.5f, topY)
            cubicTo(w * 0.15f, topY - h * 0.4f, w * 0.35f, topY - h * 0.4f, w * 0.5f, topY)
            cubicTo(w * 0.85f, topY - h * 0.4f, w * 0.65f, topY - h * 0.4f, w * 0.5f, topY)
        }
        drawPath(bowPath, color = tint, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
    }
}

@Composable
fun CustomRaisedHandIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val palmW = w * 0.6f
        val palmH = h * 0.45f
        val palmX = w * 0.2f
        val palmY = h * 0.4f
        
        drawRoundRect(
            color = tint,
            topLeft = Offset(palmX, palmY),
            size = Size(palmW, palmH),
            cornerRadius = CornerRadius(w * 0.15f)
        )
        
        val fW = w * 0.14f
        drawRoundRect(color = tint, topLeft = Offset(w * 0.22f, h * 0.20f), size = Size(fW, h * 0.35f), cornerRadius = CornerRadius(fW))
        drawRoundRect(color = tint, topLeft = Offset(w * 0.38f, h * 0.10f), size = Size(fW, h * 0.45f), cornerRadius = CornerRadius(fW))
        drawRoundRect(color = tint, topLeft = Offset(w * 0.54f, h * 0.05f), size = Size(fW, h * 0.50f), cornerRadius = CornerRadius(fW))
        drawRoundRect(color = tint, topLeft = Offset(w * 0.70f, h * 0.15f), size = Size(fW, h * 0.40f), cornerRadius = CornerRadius(fW))
        
        val thumbPath = Path().apply {
            moveTo(w * 0.25f, h * 0.55f)
            quadraticBezierTo(w * 0.0f, h * 0.65f, w * 0.1f, h * 0.75f)
            lineTo(w * 0.3f, h * 0.75f)
            close()
        }
        drawPath(thumbPath, color = tint)
    }
}

@Composable
fun CustomBriefcaseOutlineIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.08f
        
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.35f, h * 0.15f),
            size = Size(w * 0.3f, h * 0.2f),
            cornerRadius = CornerRadius(w * 0.05f),
            style = Stroke(width = strokeW)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.1f, h * 0.35f),
            size = Size(w * 0.8f, h * 0.55f),
            cornerRadius = CornerRadius(w * 0.15f),
            style = Stroke(width = strokeW)
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.3f, h * 0.6f),
            end = Offset(w * 0.7f, h * 0.6f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun CustomShareArrowIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val arrowPath = Path().apply {
            moveTo(w * 0.2f, h * 0.7f)
            quadraticBezierTo(w * 0.2f, h * 0.3f, w * 0.75f, h * 0.35f)
        }
        drawPath(arrowPath, color = tint, style = Stroke(width = w * 0.1f, cap = StrokeCap.Round))
        
        val arrowhead = Path().apply {
            moveTo(w * 0.55f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.35f)
            lineTo(w * 0.55f, h * 0.55f)
        }
        drawPath(arrowhead, color = tint, style = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CustomFlagIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.08f
        
        // Flagpole
        drawLine(
            color = tint,
            start = Offset(w * 0.25f, h * 0.15f),
            end = Offset(w * 0.25f, h * 0.85f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        // Flag Banner
        val bannerPath = Path().apply {
            moveTo(w * 0.25f, h * 0.2f)
            quadraticBezierTo(w * 0.55f, h * 0.1f, w * 0.85f, h * 0.25f)
            lineTo(w * 0.85f, h * 0.5f)
            quadraticBezierTo(w * 0.55f, h * 0.61f, w * 0.25f, h * 0.45f)
            close()
        }
        drawPath(bannerPath, color = tint)
    }
}

@Composable
fun CustomLeaveIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.08f
        
        // Door bracket shape on the left
        val doorPath = Path().apply {
            moveTo(w * 0.55f, h * 0.15f)
            lineTo(w * 0.2f, h * 0.15f)
            lineTo(w * 0.2f, h * 0.85f)
            lineTo(w * 0.55f, h * 0.85f)
        }
        drawPath(doorPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Arrow pointing right/outwards
        drawLine(
            color = tint,
            start = Offset(w * 0.35f, h * 0.5f),
            end = Offset(w * 0.8f, h * 0.5f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        // Arrowhead
        val headPath = Path().apply {
            moveTo(w * 0.65f, h * 0.35f)
            lineTo(w * 0.8f, h * 0.5f)
            lineTo(w * 0.65f, h * 0.65f)
        }
        drawPath(headPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CustomPencilIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.08f
        
        // Diagonal pencil body
        val bodyPath = Path().apply {
            moveTo(w * 0.2f, h * 0.8f)
            lineTo(w * 0.35f, h * 0.8f)
            lineTo(w * 0.85f, h * 0.3f)
            lineTo(w * 0.7f, h * 0.15f)
            lineTo(w * 0.2f, h * 0.65f)
            close()
        }
        drawPath(bodyPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Lead tip triangle
        val tipPath = Path().apply {
            moveTo(w * 0.2f, h * 0.65f)
            lineTo(w * 0.2f, h * 0.8f)
            lineTo(w * 0.35f, h * 0.8f)
            close()
        }
        drawPath(tipPath, color = tint)
    }
}

@Composable
fun CustomLanguageGlobeIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.08f
        
        // Outer circle
        drawCircle(color = tint, radius = w/2 - strokeW, center = Offset(w/2, h/2), style = Stroke(width = strokeW))
        // Equator horizontal line
        drawLine(color = tint, start = Offset(strokeW, h/2), end = Offset(w - strokeW, h/2), strokeWidth = strokeW, cap = StrokeCap.Round)
        // Vertical prime meridian
        drawLine(color = tint, start = Offset(w/2, strokeW), end = Offset(w/2, h - strokeW), strokeWidth = strokeW, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomPaletteIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.08f
        
        // Artist Palette kidney shape
        drawCircle(color = tint, radius = w/2 - strokeW, center = Offset(w/2, h/2), style = Stroke(width = strokeW))
        // Thumb hole
        drawCircle(color = tint, radius = w * 0.08f, center = Offset(w * 0.7f, h * 0.5f))
        // Colored spots
        drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.45f, h * 0.28f))
        drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.28f, h * 0.45f))
        drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.45f, h * 0.68f))
    }
}

@Composable
fun CustomTagsIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.08f
        
        // Diagonal Price Tag shape
        val tagPath = Path().apply {
            moveTo(w * 0.15f, h * 0.45f)
            lineTo(w * 0.45f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.55f)
            lineTo(w * 0.55f, h * 0.85f)
            lineTo(w * 0.15f, h * 0.45f)
            close()
        }
        drawPath(tagPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Thread eyelet hole
        drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.65f, h * 0.35f))
    }
}

fun String.toFluentAnnotatedString(fluentEmojis: List<Pair<String, String>>): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        var currentIndex = 0
        while (currentIndex < this@toFluentAnnotatedString.length) {
            var matched = false
            for ((_, unicode) in fluentEmojis) {
                if (this@toFluentAnnotatedString.startsWith(unicode, currentIndex)) {
                    appendInlineContent(unicode, unicode)
                    currentIndex += unicode.length
                    matched = true
                    break
                }
            }
            if (!matched) {
                val char = this@toFluentAnnotatedString[currentIndex]
                if (char.isHighSurrogate() && currentIndex + 1 < this@toFluentAnnotatedString.length) {
                    append(this@toFluentAnnotatedString.substring(currentIndex, currentIndex + 2))
                    currentIndex += 2
                } else {
                    append(char)
                    currentIndex++
                }
            }
        }
    }
}


@Composable
fun FloatingEmojiAnimation(url: String, onComplete: () -> Unit) {
    var isStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isStarted = true
        kotlinx.coroutines.delay(2000)
        onComplete()
    }
    
    val offsetY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isStarted) -600f else 0f, 
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 2000, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        label = "offsetY"
    )
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isStarted) 0f else 1f, 
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 2000, easing = androidx.compose.animation.core.LinearEasing),
        label = "alpha"
    )
    
    val startX = remember { (-80..80).random().dp }
    
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .offset(x = startX, y = offsetY.dp)
            .size(48.dp)
            .alpha(alpha)
    )
}


@Composable
fun PeopleBottomSheetContent(
    selectedThemeIndex: Int,
    onUserClick: (PrivateChatItem) -> Unit = {},
    onInviteClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val currentTheme = roomThemes[selectedThemeIndex]
    val isDark = currentTheme.isDark
    var showAllListeners by remember { mutableStateOf(false) }

    val allExpandedListeners = listOf(
        Triple("Ananya", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400", "🇮🇳"),
        Triple("Vihaan", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400", "🇯🇵"),
        Triple("Meera", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400", "🇺🇸"),
        Triple("Rahul", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400", "🇬🇧"),
        Triple("Kavya", "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=400", "🇮🇳"),
        Triple("Pranav", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400", "🇮🇳"),
        Triple("Neha", "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=400", "🇮🇳"),
        Triple("Nikhil", "https://images.unsplash.com/photo-1489980508314-941910ded1f4?w=400", "🇮🇳"),
        Triple("Liam", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=410", "🇺🇸"),
        Triple("Sophie", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=410", "🇫🇷"),
        Triple("Hiro", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=410", "🇯🇵"),
        Triple("Anya", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=410", "🇷🇺"),
        Triple("Jose", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=410", "🇪🇸"),
        Triple("Fatima", "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=410", "🇦🇪"),
        Triple("Jack", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400", "🇬🇧"),
        Triple("Emily", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=420", "🇺🇸"),
        Triple("Chloe", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=420", "🇫🇷"),
        Triple("David", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=420", "🇩🇪"),
        Triple("Maria", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=430", "🇮🇹"),
        Triple("Wei", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=430", "🇨🇳"),
        Triple("Jin", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=430", "🇰🇷")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .background(
                Brush.verticalGradient(
                    colors = listOf(currentTheme.startColor, currentTheme.endColor)
                )
            )
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header Row: 24 People Title / Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (isDark) currentTheme.themeColor else ChillGoldIcon, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CustomPeopleGroupIcon(tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "24 People",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "3 Speaking",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(4.dp)
                                .background(if (isDark) currentTheme.themeColor else ChillGoldIcon, CircleShape)
                        )
                        Text(
                            text = "21 Listening",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6), CircleShape)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                CustomCloseIcon(tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Speaking Now Section Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            CustomMicIcon(tint = if (isDark) currentTheme.themeColor else ChillGoldIcon, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Speaking Now",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Gray
            )
        }

        // Speakers Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Speaker 1: Riya (Host)
            PeopleSheetSpeakerItem(
                name = "Riya",
                imageUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                badgeText = "Host",
                badgeColor = Color(0xFFFEF3C7),
                isHost = true,
                flagEmoji = "🇮🇳",
                isDarkMode = isDark,
                onClick = {
                    onUserClick(PrivateChatItem("2", "Riya", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400", "in", "", "Now", 0, true, false))
                }
            )
            // Speaker 2: Sara (Speaker)
            PeopleSheetSpeakerItem(
                name = "Sara",
                imageUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400",
                badgeText = "Speaker",
                badgeColor = Color(0xFFF3F4F6),
                isHost = false,
                flagEmoji = "🇵🇰",
                isDarkMode = isDark,
                onClick = {
                    onUserClick(PrivateChatItem("1", "Sara", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400", "in", "", "Now", 0, true, false))
                }
            )
            // Speaker 3: Arjun (Speaker)
            PeopleSheetSpeakerItem(
                name = "Arjun",
                imageUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400",
                badgeText = "Speaker",
                badgeColor = Color(0xFFF3F4F6),
                isHost = false,
                flagEmoji = "🇮🇳",
                isDarkMode = isDark,
                onClick = {
                    onUserClick(PrivateChatItem("3", "Arjun", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400", "in", "", "Now", 0, true, false))
                }
            )
        }

        // Listening Section Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            CustomHeadphoneIcon(tint = if (isDark) currentTheme.themeColor else ChillGoldIcon, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (showAllListeners) "All 21 Listeners" else "Listening",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Gray
            )
        }

        if (showAllListeners) {
            val chunkedExpanded = allExpandedListeners.chunked(4)
            chunkedExpanded.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (name, imageUrl, flagCode) ->
                        PeopleSheetListenerItem(
                            name = name,
                            imageUrl = imageUrl,
                            modifier = Modifier.weight(1f),
                            flagEmoji = flagCode,
                            isDarkMode = isDark,
                            onClick = {
                                onUserClick(PrivateChatItem(name.hashCode().toString(), name, imageUrl, mapFlagEmojiToCode(flagCode), "", "Now", 0, true, false))
                            }
                        )
                    }
                    if (rowItems.size < 4) {
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            // Listeners Grid (Rows of 4 items)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PeopleSheetListenerItem("Ananya", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400", Modifier.weight(1f), flagEmoji = "🇮🇳", isDarkMode = isDark, onClick = {
                        onUserClick(PrivateChatItem("5", "Ananya", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400", "in", "", "Now", 0, true, false))
                    })
                    PeopleSheetListenerItem("Vihaan", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400", Modifier.weight(1f), flagEmoji = "🇯🇵", isDarkMode = isDark, onClick = {
                        onUserClick(PrivateChatItem("4", "Vihaan", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400", "in", "", "Now", 0, true, false))
                    })
                    PeopleSheetListenerItem("Meera", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400", Modifier.weight(1f), flagEmoji = "🇺🇸", isDarkMode = isDark, onClick = {
                        onUserClick(PrivateChatItem("10", "Meera", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400", "in", "", "Now", 0, true, false))
                    })
                    PeopleSheetListenerItem("Rahul", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400", Modifier.weight(1f), flagEmoji = "🇬🇧", isDarkMode = isDark, onClick = {
                        onUserClick(PrivateChatItem("11", "Rahul", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400", "in", "", "Now", 0, true, false))
                    })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PeopleSheetListenerItem("Kavya", "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=400", Modifier.weight(1f), flagEmoji = "🇮🇳", isDarkMode = isDark, onClick = {
                        onUserClick(PrivateChatItem("12", "Kavya", "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=400", "in", "", "Now", 0, true, false))
                    })
                    PeopleSheetListenerItem("Pranav", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400", Modifier.weight(1f), flagEmoji = "🇮🇳", isDarkMode = isDark, onClick = {
                        onUserClick(PrivateChatItem("13", "Pranav", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400", "in", "", "Now", 0, true, false))
                    })
                    PeopleSheetListenerItem("Neha", "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=400", Modifier.weight(1f), flagEmoji = "🇮🇳", isDarkMode = isDark, onClick = {
                        onUserClick(PrivateChatItem("14", "Neha", "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=400", "in", "", "Now", 0, true, false))
                    })
                    PeopleSheetListenerItem("Nikhil", "https://images.unsplash.com/photo-1489980508314-941910ded1f4?w=400", Modifier.weight(1f), flagEmoji = "🇮🇳", isDarkMode = isDark, onClick = {
                        onUserClick(PrivateChatItem("15", "Nikhil", "https://images.unsplash.com/photo-1489980508314-941910ded1f4?w=400", "in", "", "Now", 0, true, false))
                    })
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Overlapping Listener Avatar Stack
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showAllListeners = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.height(32.dp).width(120.dp)) {
                        val faces = listOf(
                            "https://images.unsplash.com/photo-1524250502761-1ac6f2e30d43?w=200",
                            "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=200",
                            "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200",
                            "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=200"
                        )
                        faces.forEachIndexed { i, imgUrl ->
                            Box(
                                modifier = Modifier
                                    .offset(x = (18 * i).dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, if (isDark) Color(0xFF1E293B) else Color.White, CircleShape)
                            ) {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .offset(x = 72.dp)
                                .size(32.dp)
                                .background(if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6), CircleShape)
                                .border(1.5.dp, if (isDark) Color(0xFF1E293B) else Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+13",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "and 13 more listeners",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.35f, h * 0.25f)
                            lineTo(w * 0.6f, h * 0.5f)
                            lineTo(w * 0.35f, h * 0.75f)
                        }
                        drawPath(
                            path = path,
                            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray,
                            style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Large prominent "Invite Friend" CTA Button matching the image perfectly
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    if (isDark) currentTheme.themeColor else ChillGoldIcon,
                    RoundedCornerShape(28.dp)
                )
                .clickable { onInviteClick() }
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CustomPersonAddIcon(tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Invite Friend",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (isDark) Color.Black.copy(alpha = 0.2f) else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else ChillGoldIcon,
                        modifier = Modifier.offset(y = (-1).dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
fun CustomCloseIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLine(color = tint, start = Offset(w * 0.25f, h * 0.25f), end = Offset(w * 0.75f, h * 0.75f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.75f, h * 0.25f), end = Offset(w * 0.25f, h * 0.75f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomSpeakerWaveBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(ChillGoldIcon, CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            val w = size.width
            val h = size.height
            val strokeW = w * 0.15f
            val color = Color(0xFFC2410C)
            
            drawLine(color = color, start = Offset(w * 0.25f, h * 0.3f), end = Offset(w * 0.25f, h * 0.7f), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(color = color, start = Offset(w * 0.5f, h * 0.1f), end = Offset(w * 0.5f, h * 0.9f), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(color = color, start = Offset(w * 0.75f, h * 0.4f), end = Offset(w * 0.75f, h * 0.6f), strokeWidth = strokeW, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun CustomHeadphoneIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.7f)
            quadraticTo(w * 0.2f, h * 0.2f, w * 0.5f, h * 0.2f)
            quadraticTo(w * 0.8f, h * 0.2f, w * 0.8f, h * 0.7f)
        }
        drawPath(path, color = tint, style = stroke)
        drawRoundRect(color = tint, topLeft = Offset(w * 0.1f, h * 0.6f), size = Size(w * 0.18f, h * 0.25f), cornerRadius = CornerRadius(w * 0.05f))
        drawRoundRect(color = tint, topLeft = Offset(w * 0.72f, h * 0.6f), size = Size(w * 0.18f, h * 0.25f), cornerRadius = CornerRadius(w * 0.05f))
    }
}

@Composable
fun CustomPersonAddIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = tint, radius = w * 0.2f, center = Offset(w * 0.4f, h * 0.35f))
        val body = Path().apply {
            moveTo(w * 0.1f, h * 0.85f)
            quadraticTo(w * 0.15f, h * 0.55f, w * 0.4f, h * 0.55f)
            quadraticTo(w * 0.65f, h * 0.55f, w * 0.7f, h * 0.85f)
        }
        drawPath(body, color = tint)
        drawLine(color = tint, start = Offset(w * 0.75f, h * 0.45f), end = Offset(w * 0.95f, h * 0.45f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.85f, h * 0.35f), end = Offset(w * 0.85f, h * 0.55f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun PeopleSheetSpeakerItem(
    name: String,
    imageUrl: String,
    badgeText: String,
    badgeColor: Color,
    isHost: Boolean,
    flagEmoji: String = "🇮🇳",
    isDarkMode: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.size(86.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(if (isHost) ChillGoldIcon else Color.Transparent)
                    .padding(if (isHost) 3.dp else 0.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-2).dp)
                    .size(20.dp)
                    .background(if (isDarkMode) Color(0xFF1E293B) else Color.White, CircleShape)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                CircleFlag(
                    countryCode = mapFlagEmojiToCode(flagEmoji),
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else Color.Black
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .background(if (isDarkMode) Color.White.copy(alpha = 0.12f) else badgeColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHost) {
                    if (isDarkMode) ChillGoldIcon else Color(0xFFC2410C)
                } else {
                    if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray
                }
            )
        }
    }
}

@Composable
fun PeopleSheetListenerItem(
    name: String,
    imageUrl: String,
    modifier: Modifier = Modifier,
    flagEmoji: String = "🇮🇳",
    isDarkMode: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-2).dp)
                    .size(16.dp)
                    .background(if (isDarkMode) Color(0xFF1E293B) else Color.White, CircleShape)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                CircleFlag(
                    countryCode = mapFlagEmojiToCode(flagEmoji),
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) Color.White else Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EditRoomBottomSheetContent(
    roomTitle: String,
    onTitleChange: (String) -> Unit,
    roomTags: String,
    onTagsChange: (String) -> Unit,
    roomDescription: String,
    onDescriptionChange: (String) -> Unit,
    roomLanguage: String,
    onLanguageChange: (String) -> Unit,
    selectedThemeIndex: Int,
    onThemeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Modal state controllers
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }

    val currentTheme = roomThemes[selectedThemeIndex]
    val isDark = currentTheme.isDark

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Sheet Header (Beautiful, matching Picture 4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    CustomPencilIcon(
                        tint = if (isDark) Color.White else Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Edit Room",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Text(
                        text = "Update your room details",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray
                    )
                }
            }
            androidx.compose.material3.IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = if (isDark) Color.White else Color.DarkGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Room Name Field (Picture 4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Room Name",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black
            )
            Text(
                text = "${roomTitle.length}/60",
                fontSize = 12.sp,
                color = if (roomTitle.length > 50) Color.Red else (if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        androidx.compose.material3.OutlinedTextField(
            value = roomTitle,
            onValueChange = { if (it.length <= 60) onTitleChange(it) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (isDark) Color.White else Color.Black,
                unfocusedTextColor = if (isDark) Color.White else Color.Black,
                focusedBorderColor = if (isDark) currentTheme.themeColor else Color(0xFFF59E0B),
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFE5E7EB),
                focusedContainerColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White,
                unfocusedContainerColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White,
                cursorColor = if (isDark) Color.White else Color.Black
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Description Field (Picture 4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Description",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black
            )
            Text(
                text = "${roomDescription.length}/120",
                fontSize = 12.sp,
                color = if (roomDescription.length > 100) Color.Red else (if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        androidx.compose.material3.OutlinedTextField(
            value = roomDescription,
            onValueChange = { if (it.length <= 120) onDescriptionChange(it) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            minLines = 2,
            maxLines = 3,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (isDark) Color.White else Color.Black,
                unfocusedTextColor = if (isDark) Color.White else Color.Black,
                focusedBorderColor = if (isDark) currentTheme.themeColor else Color(0xFFF59E0B),
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFE5E7EB),
                focusedContainerColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White,
                unfocusedContainerColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White,
                cursorColor = if (isDark) Color.White else Color.Black
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Card Row 1: Language Selection (Picture 4 style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White)
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
                .clickable { showLanguagePicker = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    CustomLanguageGlobeIcon(
                        tint = if (isDark) Color.White else Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Language",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Text(
                        text = roomLanguage,
                        fontSize = 12.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Expand Language",
                tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Gray,
                modifier = Modifier
                    .size(24.dp)
                    .scale(-1f, 1f) // flip arrow horizontally to make it point right
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Card Row 2: Theme Selection (Picture 4 / 6 style)
        val selectedTheme = roomThemes[selectedThemeIndex]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White)
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
                .clickable { showThemePicker = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    CustomPaletteIcon(
                        tint = if (isDark) Color.White else Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Theme",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Text(
                        text = "${selectedTheme.name} (${if (selectedTheme.isDark) "Immersive Dark" else "Vibrant Light"})",
                        fontSize = 12.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Color preview circle representing active theme style
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .shadow(1.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(selectedTheme.startColor, selectedTheme.endColor)
                            )
                        )
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Expand Theme",
                    tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Gray,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(-1f, 1f) // point right
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Card Row 3: Tags (Replacing Advanced Settings - Picture 8 style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White)
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    CustomTagsIcon(
                        tint = if (isDark) Color.White else Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Tags",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Text(
                        text = "Select one popular tag for your room",
                        fontSize = 11.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tag List containing selected active tag chip & "+ Add Tag" dashed border button (Picture 8 style)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (roomTags.isNotEmpty()) {
                    // Selected active tag chip with close button (Picture 8 style)
                    Box(
                        modifier = Modifier
                            .background(
                                if (isDark) currentTheme.themeColor.copy(alpha = 0.2f) else Color(0xFFFEF3C7),
                                RoundedCornerShape(50.dp)
                            )
                            .border(1.dp, if (isDark) currentTheme.themeColor else Color(0xFFFBBF24), RoundedCornerShape(50.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = roomTags,
                                fontSize = 12.sp,
                                color = if (isDark) Color.White else Color(0xFFD97706),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Tag",
                                tint = if (isDark) Color.White else Color(0xFFD97706),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onTagsChange("") }
                            )
                        }
                    }
                }

                // Beautiful "+ Add Tag" button with dashed border style
                Box(
                    modifier = Modifier
                        .drawBehind {
                            val stroke = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                            drawRoundRect(
                                color = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFFF59E0B),
                                style = stroke,
                                cornerRadius = CornerRadius(50.dp.toPx(), 50.dp.toPx())
                            )
                        }
                        .clip(RoundedCornerShape(50.dp))
                        .clickable { showTagPicker = true }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Icon",
                            tint = if (isDark) Color.White else Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Tag",
                            fontSize = 12.sp,
                            color = if (isDark) Color.White else Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Save Bottom Button
        androidx.compose.material3.Button(
            onClick = {
                Toast.makeText(context, "Room settings updated successfully!", Toast.LENGTH_SHORT).show()
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(100.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = if (isDark) currentTheme.themeColor else Color(0xFFF59E0B)
            )
        ) {
            Text(
                text = "Apply Changes",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal Popup 1: Language Picker Selector (Modern Dialog UI)
    if (showLanguagePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Select Language", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    androidx.compose.material3.IconButton(onClick = { showLanguagePicker = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
            },
            text = {
                val languages = listOf("Hindi + English", "Hindi Only", "English Only", "Bhojpuri", "Punjabi", "Spanish", "Arabic")
                Column(modifier = Modifier.fillMaxWidth()) {
                    languages.forEach { lang ->
                        val isSelected = roomLanguage == lang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFFFFAEB) else Color.Transparent)
                                .clickable {
                                    onLanguageChange(lang)
                                    showLanguagePicker = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lang,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFFD97706) else Color.DarkGray
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Modal Popup 2: Choose Theme Dialog (Beautifully matches Picture 6!)
    if (showThemePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Choose Theme", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        Text(text = "Pick a theme for your room", fontSize = 11.sp, color = Color.Gray)
                    }
                    androidx.compose.material3.IconButton(
                        onClick = { showThemePicker = false },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                    }
                }
            },
            text = {
                // High contrast list scrollbox (Picture 6 layout)
                Box(modifier = Modifier.height(350.dp)) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(roomThemes.size) { index ->
                            val current = roomThemes[index]
                            val isSelected = selectedThemeIndex == index
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFFFFFAEB) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFFFBBF24) else Color(0xFFF3F4F6),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        onThemeChange(index)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic Color Preview Chip Circle
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .shadow(1.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(Brush.verticalGradient(colors = listOf(current.startColor, current.endColor)))
                                        .border(2.dp, if (isSelected) Color(0xFFD97706) else Color.Transparent, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = current.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFD97706) else Color.Black
                                    )
                                    Text(
                                        text = current.description,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFFD97706) else Color(0xFFE5E7EB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showThemePicker = false }
                ) {
                    Text("Apply Theme", fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Modal Popup 3: Add Tags Dialog (Matches Picture 7 Popular Tags exactly!)
    if (showTagPicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTagPicker = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Add Tags", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        Text(text = "Popular Tags", fontSize = 11.sp, color = Color.Gray)
                    }
                    androidx.compose.material3.IconButton(
                        onClick = { showTagPicker = false },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                    }
                }
            },
            text = {
                val popularTagsList = listOf(
                    "Chill", "Music", "Gaming", "Friendship",
                    "Fun", "Dating", "Study", "Debate",
                    "Travel", "Poetry", "Anime", "Sports"
                )
                // Flow Layout/Grid for tags (Picture 7 style)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Simple structured layout representing a gorgeous visual flow of tags
                    val rows = popularTagsList.chunked(3)
                    rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            row.forEach { tag ->
                                val isSelected = roomTags == tag
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) Color(0xFFFFFAEB) else Color(0xFFF3F4F6))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFFFBBF24) else Color.Transparent,
                                            shape = RoundedCornerShape(50.dp)
                                        )
                                        .clickable {
                                            onTagsChange(tag)
                                            showTagPicker = false
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFD97706) else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun CuteStarIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFFFBBF24)) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            val cx = size.width / 2
            val cy = size.height / 2
            val spikes = 5
            val outerRadius = size.width / 2
            val innerRadius = size.width * 0.22f
            var rot = Math.PI.toFloat() / 2f * 3f
            val step = Math.PI.toFloat() / spikes.toFloat()
            moveTo(cx, cy - outerRadius)
            for (i in 0 until spikes) {
                var x = cx + Math.cos(rot.toDouble()).toFloat() * outerRadius
                var y = cy + Math.sin(rot.toDouble()).toFloat() * outerRadius
                lineTo(x, y)
                rot += step
                x = cx + Math.cos(rot.toDouble()).toFloat() * innerRadius
                y = cy + Math.sin(rot.toDouble()).toFloat() * innerRadius
                lineTo(x, y)
                rot += step
            }
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
fun CuteFireIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFFEF4444)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val outerFlame = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            quadraticTo(w * 0.15f, h * 0.45f, w * 0.2f, h * 0.72f)
            quadraticTo(w * 0.25f, h * 0.95f, w * 0.5f, h * 0.95f)
            quadraticTo(w * 0.75f, h * 0.95f, w * 0.8f, h * 0.72f)
            quadraticTo(w * 0.85f, h * 0.45f, w * 0.5f, h * 0.05f)
        }
        drawPath(outerFlame, color = tint)
        
        val innerFlame = Path().apply {
            moveTo(w * 0.5f, h * 0.4f)
            quadraticTo(w * 0.32f, h * 0.62f, w * 0.36f, h * 0.82f)
            quadraticTo(w * 0.5f, h * 0.92f, w * 0.5f, h * 0.92f)
            quadraticTo(w * 0.64f, h * 0.82f, w * 0.5f, h * 0.4f)
        }
        drawPath(innerFlame, color = Color(0xFFFBBF24))
    }
}

@Composable
fun CuteFestivalIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFFEC4899)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cone = Path().apply {
            moveTo(w * 0.15f, h * 0.85f)
            lineTo(w * 0.72f, h * 0.28f)
            lineTo(w * 0.28f, h * 0.15f)
            close()
        }
        drawPath(cone, color = tint)
        drawCircle(Color(0xFF3B82F6), radius = 2.5.dp.toPx(), center = Offset(w * 0.82f, h * 0.22f))
        drawCircle(Color(0xFF10B981), radius = 2.dp.toPx(), center = Offset(w * 0.62f, h * 0.12f))
        drawCircle(Color(0xFFFBBF24), radius = 3.dp.toPx(), center = Offset(w * 0.86f, h * 0.48f))
    }
}

@Composable
fun CuteJewelIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFF8B5CF6)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.86f)
            lineTo(w * 0.16f, h * 0.44f)
            lineTo(w * 0.34f, h * 0.16f)
            lineTo(w * 0.66f, h * 0.16f)
            lineTo(w * 0.84f, h * 0.44f)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
fun CutePlusIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFFEC4899)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val thickness = w * 0.2f
        drawRoundRect(
            color = tint,
            topLeft = Offset((w - thickness) / 2f, h * 0.12f),
            size = Size(thickness, h * 0.76f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.12f, (h - thickness) / 2f),
            size = Size(w * 0.76f, thickness),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )
    }
}

@Composable
fun CuteBackArrowIcon(modifier: Modifier = Modifier, tint: Color = Color.Black) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.62f, h * 0.22f)
            lineTo(w * 0.34f, h * 0.5f)
            lineTo(w * 0.62f, h * 0.78f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun CuteWaveformIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFFFBBF24)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val spacing = w * 0.11f
        val barWidth = w * 0.11f
        val heights = listOf(0.45f, 0.85f, 0.55f, 0.75f, 0.35f)
        heights.forEachIndexed { i, factor ->
            val rx = i * (barWidth + spacing) + spacing * 1.5f
            val bh = h * factor
            val ry = (h - bh) / 2f
            drawRoundRect(
                color = tint,
                topLeft = Offset(rx, ry),
                size = Size(barWidth, bh),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun CuteSendIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.25f)
            lineTo(w * 0.86f, h * 0.5f)
            lineTo(w * 0.22f, h * 0.75f)
            lineTo(w * 0.38f, h * 0.5f)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
fun CuteSmileIcon(modifier: Modifier = Modifier, tint: Color = Color.Gray) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = tint,
            radius = (w / 2f) - 1.5.dp.toPx(),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(color = tint, radius = 1.5.dp.toPx(), center = Offset(w * 0.35f, h * 0.42f))
        drawCircle(color = tint, radius = 1.5.dp.toPx(), center = Offset(w * 0.65f, h * 0.42f))
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.3f, h * 0.42f),
            size = Size(w * 0.4f, h * 0.32f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun CuteMegaphoneIcon(modifier: Modifier = Modifier, tint: Color = Color.Gray) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val body = Path().apply {
            moveTo(w * 0.28f, h * 0.42f)
            lineTo(w * 0.68f, h * 0.26f)
            lineTo(w * 0.74f, h * 0.74f)
            lineTo(w * 0.28f, h * 0.58f)
            close()
        }
        drawPath(body, color = tint)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.3f, h * 0.54f),
            size = Size(w * 0.12f, h * 0.26f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )
    }
}

@Composable
fun CuteShieldCheckIcon(modifier: Modifier = Modifier, tint: Color = Color.Gray) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.25f)
            quadraticTo(w * 0.85f, h * 0.55f, w * 0.5f, h * 0.85f)
            quadraticTo(w * 0.15f, h * 0.55f, w * 0.15f, h * 0.25f)
            close()
        }
        drawPath(shield, color = tint.copy(alpha = 0.15f))
        drawPath(shield, color = tint, style = Stroke(width = 1.5.dp.toPx()))
        
        val check = Path().apply {
            moveTo(w * 0.38f, h * 0.48f)
            lineTo(w * 0.47f, h * 0.58f)
            lineTo(w * 0.64f, h * 0.38f)
        }
        drawPath(check, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CutePresentIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.2f, h * 0.35f),
            size = Size(w * 0.6f, h * 0.48f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRect(
            color = Color(0xFFFBBF24),
            topLeft = Offset(w * 0.45f, h * 0.35f),
            size = Size(w * 0.1f, h * 0.48f)
        )
        drawRect(
            color = Color(0xFFFBBF24),
            topLeft = Offset(w * 0.2f, h * 0.54f),
            size = Size(w * 0.6f, h * 0.08f)
        )
        drawArc(
            color = Color(0xFFFBBF24),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(w * 0.28f, h * 0.18f),
            size = Size(w * 0.22f, h * 0.22f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFFFBBF24),
            startAngle = -45f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(w * 0.5f, h * 0.18f),
            size = Size(w * 0.22f, h * 0.22f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun CuteCoinIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFFFF0B3), Color(0xFFF59E0B))),
            radius = w / 2f
        )
        drawCircle(
            color = Color(0xFFD97706),
            radius = w / 2f,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = w / 3.2f,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
fun CuteMicSmallIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFFF59E0B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.36f, h * 0.2f),
            size = Size(w * 0.28f, h * 0.44f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.22f, h * 0.34f),
            size = Size(w * 0.56f, h * 0.42f),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.76f),
            end = Offset(w * 0.5f, h * 0.94f),
            strokeWidth = 1.8.dp.toPx()
        )
    }
}

@Composable
fun GiftIconRepresentation(name: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (name) {
            "Rose" -> {
                val leafPath = Path().apply {
                    moveTo(w * 0.35f, h * 0.7f)
                    quadraticTo(w * 0.15f, h * 0.65f, w * 0.3f, h * 0.55f)
                    quadraticTo(w * 0.4f, h * 0.62f, w * 0.35f, h * 0.7f)
                }
                drawPath(leafPath, color = Color(0xFF10B981))
                val stemPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.44f)
                    quadraticTo(w * 0.45f, h * 0.65f, w * 0.35f, h * 0.85f)
                }
                drawPath(
                    stemPath,
                    color = Color(0xFF059669),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(Color(0xFFE11D48), radius = w * 0.22f, center = Offset(w * 0.5f, h * 0.35f))
                drawCircle(Color(0xFFF43F5E), radius = w * 0.16f, center = Offset(w * 0.44f, h * 0.3f))
                drawCircle(Color(0xFFFB7185), radius = w * 0.11f, center = Offset(w * 0.54f, h * 0.34f))
                drawCircle(Color(0xFFFFF1F2), radius = w * 0.05f, center = Offset(w * 0.48f, h * 0.28f))
            }
            "Heart" -> {
                val heartPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.82f)
                    cubicTo(w * 0.1f, h * 0.48f, w * 0.12f, h * 0.15f, w * 0.5f, h * 0.34f)
                    cubicTo(w * 0.88f, h * 0.15f, w * 0.9f, h * 0.48f, w * 0.5f, h * 0.82f)
                }
                drawPath(
                    path = heartPath,
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF5A79), Color(0xFFE11D48)),
                        center = Offset(w * 0.4f, h * 0.4f),
                        radius = w * 0.6f
                    )
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.5f),
                    startAngle = 185f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(w * 0.22f, h * 0.28f),
                    size = Size(w * 0.4f, h * 0.35f),
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            "Cake" -> {
                drawRoundRect(
                    color = Color(0xFFFDA4AF),
                    topLeft = Offset(w * 0.15f, h * 0.52f),
                    size = Size(w * 0.7f, h * 0.28f),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.12f, h * 0.48f),
                    size = Size(w * 0.76f, h * 0.08f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFFFEE2E2),
                    topLeft = Offset(w * 0.25f, h * 0.3f),
                    size = Size(w * 0.5f, h * 0.22f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.23f, h * 0.26f),
                    size = Size(w * 0.54f, h * 0.07f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                drawCircle(Color(0xFFEF4444), radius = w * 0.07f, center = Offset(w * 0.5f, h * 0.18f))
                drawCircle(Color(0xFFF43F5E), radius = w * 0.04f, center = Offset(w * 0.25f, h * 0.48f))
                drawCircle(Color(0xFFF43F5E), radius = w * 0.04f, center = Offset(w * 0.75f, h * 0.48f))
            }
            "Coffee" -> {
                drawOval(
                    color = Color(0xFFD1D5DB),
                    topLeft = Offset(w * 0.15f, h * 0.7f),
                    size = Size(w * 0.7f, h * 0.13f)
                )
                val cupPath = Path().apply {
                    moveTo(w * 0.23f, h * 0.38f)
                    lineTo(w * 0.77f, h * 0.38f)
                    quadraticTo(w * 0.72f, h * 0.72f, w * 0.5f, h * 0.72f)
                    quadraticTo(w * 0.28f, h * 0.72f, w * 0.23f, h * 0.38f)
                }
                drawPath(cupPath, color = Color.White)
                drawPath(cupPath, color = Color(0xFFE5E7EB), style = Stroke(width = 1.dp.toPx()))
                drawArc(
                    color = Color(0xFFE5E7EB),
                    startAngle = -80f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(w * 0.72f, h * 0.43f),
                    size = Size(w * 0.15f, h * 0.22f),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawOval(
                    color = Color(0xFF78350F),
                    topLeft = Offset(w * 0.25f, h * 0.36f),
                    size = Size(w * 0.5f, h * 0.08f)
                )
                val steamPath = Path().apply {
                    moveTo(w * 0.45f, h * 0.28f)
                    quadraticTo(w * 0.4f, h * 0.22f, w * 0.45f, h * 0.14f)
                    moveTo(w * 0.55f, h * 0.3f)
                    quadraticTo(w * 0.6f, h * 0.24f, w * 0.55f, h * 0.16f)
                }
                drawPath(
                    steamPath,
                    color = Color(0xFF9CA3AF),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            "Mic" -> {
                drawRoundRect(
                    color = Color(0xFF374151),
                    topLeft = Offset(w * 0.44f, h * 0.5f),
                    size = Size(w * 0.12f, h * 0.35f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                drawRect(
                    color = Color(0xFF8B5CF6),
                    topLeft = Offset(w * 0.42f, h * 0.46f),
                    size = Size(w * 0.16f, h * 0.05f)
                )
                val conePath = Path().apply {
                    moveTo(w * 0.4f, h * 0.45f)
                    lineTo(w * 0.6f, h * 0.45f)
                    lineTo(w * 0.56f, h * 0.36f)
                    lineTo(w * 0.44f, h * 0.36f)
                    close()
                }
                drawPath(conePath, color = Color(0xFF4B5563))
                drawCircle(Color(0xFF9CA3AF), radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.26f))
                clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(w * 0.5f, h * 0.26f), w * 0.18f)) }) {
                    for (i in -4..4) {
                        val offset = i * w * 0.08f
                        drawLine(
                            Color(0xFF4B5563),
                            start = Offset(w * 0.2f + offset, h * 0.1f),
                            end = Offset(w * 0.8f + offset, h * 0.42f),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            Color(0xFF4B5563),
                            start = Offset(w * 0.8f - offset, h * 0.1f),
                            end = Offset(w * 0.2f - offset, h * 0.42f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }
            "Crown" -> {
                val cPath = Path().apply {
                    moveTo(w * 0.18f, h * 0.72f)
                    lineTo(w * 0.14f, h * 0.32f)
                    lineTo(w * 0.36f, h * 0.53f)
                    lineTo(w * 0.5f, h * 0.22f)
                    lineTo(w * 0.64f, h * 0.53f)
                    lineTo(w * 0.86f, h * 0.32f)
                    lineTo(w * 0.82f, h * 0.72f)
                    close()
                }
                drawPath(
                    path = cPath,
                    brush = Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706)))
                )
                drawRoundRect(
                    color = Color(0xFFB45309),
                    topLeft = Offset(w * 0.16f, h * 0.7f),
                    size = Size(w * 0.68f, h * 0.08f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawCircle(Color(0xFF2563EB), radius = w * 0.045f, center = Offset(w * 0.14f, h * 0.32f))
                drawCircle(Color(0xFF2563EB), radius = w * 0.045f, center = Offset(w * 0.5f, h * 0.22f))
                drawCircle(Color(0xFF2563EB), radius = w * 0.045f, center = Offset(w * 0.86f, h * 0.32f))
                val gemPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.65f)
                    lineTo(w * 0.44f, h * 0.72f)
                    lineTo(w * 0.5f, h * 0.79f)
                    lineTo(w * 0.56f, h * 0.72f)
                    close()
                }
                drawPath(gemPath, color = Color(0xFF60A5FA))
            }
            "Rocket" -> {
                val rAngle = -30f
                rotate(rAngle, pivot = Offset(w * 0.5f, h * 0.5f)) {
                    val fPath = Path().apply {
                        moveTo(w * 0.42f, h * 0.72f)
                        lineTo(w * 0.5f, h * 0.95f)
                        lineTo(w * 0.58f, h * 0.72f)
                        close()
                    }
                    drawPath(
                        path = fPath,
                        brush = Brush.verticalGradient(listOf(Color(0xFFF97316), Color(0xFFFBBF24)))
                    )
                    val lWing = Path().apply {
                        moveTo(w * 0.32f, h * 0.58f)
                        lineTo(w * 0.22f, h * 0.72f)
                        lineTo(w * 0.42f, h * 0.72f)
                        close()
                    }
                    drawPath(lWing, color = Color(0xFFEF4444))
                    val rWing = Path().apply {
                        moveTo(w * 0.68f, h * 0.58f)
                        lineTo(w * 0.78f, h * 0.72f)
                        lineTo(w * 0.58f, h * 0.72f)
                        close()
                    }
                    drawPath(rWing, color = Color(0xFFEF4444))
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.35f, h * 0.3f),
                        size = Size(w * 0.3f, h * 0.42f),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFF9CA3AF),
                        topLeft = Offset(w * 0.35f, h * 0.3f),
                        size = Size(w * 0.3f, h * 0.42f),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    val nosePath = Path().apply {
                        moveTo(w * 0.35f, h * 0.31f)
                        quadraticTo(w * 0.5f, h * 0.1f, w * 0.65f, h * 0.31f)
                        close()
                    }
                    drawPath(nosePath, color = Color(0xFFEF4444))
                    drawCircle(Color(0xFF3B82F6), radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.48f))
                    drawCircle(
                        color = Color(0xFF9CA3AF),
                        radius = w * 0.08f,
                        center = Offset(w * 0.5f, h * 0.48f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
            "Diamond" -> {
                val dPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.86f)
                    lineTo(w * 0.14f, h * 0.45f)
                    lineTo(w * 0.32f, h * 0.18f)
                    lineTo(w * 0.68f, h * 0.18f)
                    lineTo(w * 0.86f, h * 0.45f)
                    close()
                }
                drawPath(
                    path = dPath,
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFC084FC), Color(0xFF7C3AED)),
                        center = Offset(w * 0.5f, h * 0.3f),
                        radius = w * 0.6f
                    )
                )
                drawLine(Color(0xFFF3E8FF), start = Offset(w * 0.32f, h * 0.18f), end = Offset(w * 0.68f, h * 0.18f), strokeWidth = 1.dp.toPx())
                drawLine(Color(0xFFF3E8FF), start = Offset(w * 0.14f, h * 0.45f), end = Offset(w * 0.86f, h * 0.45f), strokeWidth = 1.dp.toPx())
                drawLine(Color(0xFFF3E8FF).copy(alpha = 0.5f), start = Offset(w * 0.32f, h * 0.18f), end = Offset(w * 0.35f, h * 0.45f), strokeWidth = 1.dp.toPx())
                drawLine(Color(0xFFF3E8FF).copy(alpha = 0.5f), start = Offset(w * 0.68f, h * 0.18f), end = Offset(w * 0.65f, h * 0.45f), strokeWidth = 1.dp.toPx())
                drawLine(Color(0xFFF3E8FF).copy(alpha = 0.6f), start = Offset(w * 0.35f, h * 0.45f), end = Offset(w * 0.5f, h * 0.86f), strokeWidth = 1.2.dp.toPx())
                drawLine(Color(0xFFF3E8FF).copy(alpha = 0.6f), start = Offset(w * 0.65f, h * 0.45f), end = Offset(w * 0.5f, h * 0.86f), strokeWidth = 1.2.dp.toPx())
                drawLine(Color(0xFFF3E8FF).copy(alpha = 0.6f), start = Offset(w * 0.5f, h * 0.18f), end = Offset(w * 0.5f, h * 0.45f), strokeWidth = 1.2.dp.toPx())
            }
            "Trophy" -> {
                drawRoundRect(
                    color = Color(0xFF374151),
                    topLeft = Offset(w * 0.32f, h * 0.72f),
                    size = Size(w * 0.36f, h * 0.13f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                drawRect(
                    color = Color(0xFFD97706),
                    topLeft = Offset(w * 0.44f, h * 0.62f),
                    size = Size(w * 0.12f, h * 0.11f)
                )
                val bowlPath = Path().apply {
                    moveTo(w * 0.25f, h * 0.22f)
                    lineTo(w * 0.75f, h * 0.22f)
                    cubicTo(w * 0.72f, h * 0.48f, w * 0.65f, h * 0.63f, w * 0.5f, h * 0.63f)
                    cubicTo(w * 0.35f, h * 0.63f, w * 0.28f, h * 0.48f, w * 0.25f, h * 0.22f)
                }
                drawPath(
                    path = bowlPath,
                    brush = Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706)))
                )
                drawArc(
                    color = Color(0xFFF59E0B),
                    startAngle = 100f,
                    sweepAngle = 170f,
                    useCenter = false,
                    topLeft = Offset(w * 0.16f, h * 0.26f),
                    size = Size(w * 0.15f, h * 0.24f),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawArc(
                    color = Color(0xFFF59E0B),
                    startAngle = -90f,
                    sweepAngle = 170f,
                    useCenter = false,
                    topLeft = Offset(w * 0.69f, h * 0.26f),
                    size = Size(w * 0.15f, h * 0.24f),
                    style = Stroke(width = 3.dp.toPx())
                )
                val starPath = Path().apply {
                    val cx = w * 0.5f
                    val cy = h * 0.38f
                    val spikes = 5
                    val outerRadius = w * 0.08f
                    val innerRadius = w * 0.035f
                    var rot = Math.PI.toFloat() / 2f * 3f
                    val step = Math.PI.toFloat() / spikes.toFloat()
                    moveTo(cx, cy - outerRadius)
                    for (i in 0 until spikes) {
                        var x = cx + Math.cos(rot.toDouble()).toFloat() * outerRadius
                        var y = cy + Math.sin(rot.toDouble()).toFloat() * outerRadius
                        lineTo(x, y)
                        rot += step
                        x = cx + Math.cos(rot.toDouble()).toFloat() * innerRadius
                        y = cy + Math.sin(rot.toDouble()).toFloat() * innerRadius
                        lineTo(x, y)
                        rot += step
                    }
                    close()
                }
                drawPath(starPath, color = Color.White)
            }
        }
    }
}

@Composable
fun GiftBottomSheetContent(
    selectedThemeIndex: Int,
    coins: Int,
    selectedGift: GiftItem,
    onSelectedGiftChange: (GiftItem) -> Unit,
    onCoinsChange: (Int) -> Unit,
    selectedTarget: String,
    onTargetChange: (String) -> Unit,
    multiplier: Int,
    onMultiplierChange: (Int) -> Unit,
    isChangingGift: Boolean,
    onChangingGiftChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onGiftSent: (GiftItem, String, Int) -> Unit
) {
    val currentTheme = roomThemes[selectedThemeIndex]
    val textColor = if (currentTheme.isDark) Color.White else Color.Black
    val secondaryTextColor = if (currentTheme.isDark) Color.White.copy(alpha = 0.5f) else Color.Gray

    var subTabSelected by remember { mutableStateOf(0) } // 0 = Gifts, 1 = Backpack
    var showMultiplierOptions by remember { mutableStateOf(false) }

    val backpackItems = listOf(
        GiftItem("Free Rose", 0, ""),
        GiftItem("Lucky Clover", 0, ""),
        GiftItem("Star Shard", 0, ""),
        GiftItem("Magic Sticker", 0, ""),
        GiftItem("Event Heart", 0, "")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
            .background(if (currentTheme.isDark) Color(0xFF111827) else Color.White)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        // "To" Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "To",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(end = 12.dp)
            )

            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val targets = listOf(
                    "Riya" to Triple("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400", "🇮🇳", "1"),
                    "Sara" to Triple("https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400", "🇵🇰", "2"),
                    "Ananya" to Triple("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400", "🇮🇳", "3"),
                    "Arjun" to Triple("https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400", "🇮🇳", "4"),
                    "Meera" to Triple("https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400", "🇺🇸", "5"),
                    "Vihaan" to Triple("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400", "🇯🇵", "6")
                )

                targets.forEach { (name, info) ->
                    val isSel = selectedTarget == name
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onTargetChange(name) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    if (isSel) Color(0xFFFBBF24) else Color.Transparent,
                                    CircleShape
                                )
                                .padding(if (isSel) 3.dp else 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = info.first,
                                contentDescription = name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 1.dp, y = 1.dp)
                                    .size(12.dp)
                                    .background(if (currentTheme.isDark) Color(0xFF1E293B) else Color.White, CircleShape)
                                    .padding(0.5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircleFlag(
                                    countryCode = mapFlagEmojiToCode(info.second),
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .background(if (isSel) Color(0xFFFBBF24) else Color(0xFFF3F4F6), RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = info.third,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color(0xFF78350F) else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Select ALL Speakers Button
            val isAllSel = selectedTarget == "All Speakers"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(if (isAllSel) Color(0xFFEC4899) else Color.Transparent, RoundedCornerShape(100.dp))
                    .border(1.dp, if (isAllSel) Color.Transparent else Color(0xFFE5E5EA), RoundedCornerShape(100.dp))
                    .clickable { onTargetChange("All Speakers") }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ALL", 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = if (isAllSel) Color.White else textColor
                )
            }
        }

        // Subtabs: Gifts vs Backpack
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { subTabSelected = 0 }
                    .padding(end = 24.dp)
            ) {
                Text(
                    text = "Gifts", 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = if (subTabSelected == 0) Color(0xFFF59E0B) else Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(2.5.dp)
                        .background(if (subTabSelected == 0) Color(0xFFF59E0B) else Color.Transparent, RoundedCornerShape(100.dp))
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { subTabSelected = 1 }
            ) {
                Text(
                    text = "Backpack", 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = if (subTabSelected == 1) Color(0xFFF59E0B) else Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(2.5.dp)
                        .background(if (subTabSelected == 1) Color(0xFFF59E0B) else Color.Transparent, RoundedCornerShape(100.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(if (currentTheme.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF3F4F6)))
        Spacer(modifier = Modifier.height(10.dp))

        val activeGiftList = if (subTabSelected == 0) giftItemsList else backpackItems

        // Horizontal swipable/scrollable list of gifts utilizing custom HorizontalPager state
        val itemsPerPage = 4
        val pagesCount = (activeGiftList.size + itemsPerPage - 1) / itemsPerPage
        val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pagesCount })

        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) { pageIndex ->
            val startIndex = pageIndex * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, activeGiftList.size)
            val pageItems = activeGiftList.subList(startIndex, endIndex)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pageItems.forEach { gift ->
                    val isSel = selectedGift.name == gift.name
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (currentTheme.isDark) Color(0xFF1F2937) else Color(0xFFF9FAFB))
                            .border(
                                1.5.dp,
                                if (isSel) Color(0xFFFBBF24) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectedGiftChange(gift) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GiftIconRepresentation(name = gift.name, modifier = Modifier.size(46.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = gift.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (gift.cost > 0) {
                                CuteCoinIcon(modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = gift.cost.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = secondaryTextColor)
                            } else {
                                Text(
                                    text = "FREE", 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
                
                // Pad with empty cells on the last page if needed
                repeat(itemsPerPage - pageItems.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Dynamic indication dots
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagesCount) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(6.5.dp)
                        .background(if (active) Color(0xFFFBBF24) else Color(0xFFE5E5EA), CircleShape)
                )
            }
        }

        // Multiplier sliding tray
        if (showMultiplierOptions) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(if (currentTheme.isDark) Color(0xFF1F2937) else Color(0xFFFFFBEB), RoundedCornerShape(100.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val options = listOf(1, 10, 30, 40, 60, 70)
                options.forEach { opt ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(if (multiplier == opt) Color(0xFFFBBF24) else Color.Transparent, CircleShape)
                            .clickable {
                                onMultiplierChange(opt)
                                showMultiplierOptions = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (multiplier == opt) Color(0xFF78350F) else textColor
                        )
                    }
                }
            }
        }

        // Footer block
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coin count display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(if (currentTheme.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFF9FAFB), RoundedCornerShape(100.dp))
                    .border(1.dp, if (currentTheme.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFE5E5EA), RoundedCornerShape(100.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                CuteCoinIcon(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = String.format("%,d", coins),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "+", 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Medium, 
                    color = secondaryTextColor, 
                    modifier = Modifier.clickable { onCoinsChange(coins + 500) }
                )
            }

            // Send with Multiplier trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(if (currentTheme.isDark) Color.White.copy(alpha = 0.05f) else Color.White, RoundedCornerShape(100.dp))
                    .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(100.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showMultiplierOptions = !showMultiplierOptions }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = multiplier.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Canvas(modifier = Modifier.size(8.dp)) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.1f, h * 0.8f)
                            lineTo(w * 0.5f, h * 0.4f)
                            lineTo(w * 0.9f, h * 0.8f)
                        }
                        drawPath(path, color = textColor, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF59E0B), RoundedCornerShape(100.dp))
                        .clickable { onGiftSent(selectedGift, selectedTarget, multiplier) }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(text = "Send", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                }
            }
        }
    }
}

@Composable
fun ReportRoomBottomSheetContent(
    selectedThemeIndex: Int,
    selectedReasonIndex: Int,
    onReasonIndexChange: (Int) -> Unit,
    audioAttached: Boolean,
    onAudioAttachChange: (Boolean) -> Unit,
    messagesAttached: Boolean,
    onMessagesAttachChange: (Boolean) -> Unit,
    activityAttached: Boolean,
    onActivityAttachChange: (Boolean) -> Unit,
    screenshotAttached: Boolean,
    onScreenshotAttachChange: (Boolean) -> Unit,
    videoAttached: Boolean,
    onVideoAttachChange: (Boolean) -> Unit,
    additionalDetails: String,
    onDetailsChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val currentTheme = roomThemes[selectedThemeIndex]
    val textColor = if (currentTheme.isDark) Color.White else Color.Black
    val secondaryTextColor = if (currentTheme.isDark) Color.White.copy(alpha = 0.7f) else Color.Gray

    val reasons = listOf(
        Triple("Harassment / Abuse", "Bullying, threats, rude or abusive behavior", "😡"),
        Triple("Spam / Promotion", "Advertising, referral links or repeated spam", "📢"),
        Triple("Hate Content", "Hate speech, racism, discrimination", "✋"),
        Triple("Sexual Content", "Nudity, sexual acts or inappropriate content", "💖"),
        Triple("Dangerous Activity", "Violence, self-harm or illegal activities", "⚠️"),
        Triple("Other", "Something else not listed above", "💬")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .background(Brush.verticalGradient(listOf(currentTheme.startColor, currentTheme.endColor)))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFEE2E2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🚩", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Report Room", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text(text = "Help keep our community safe", fontSize = 11.sp, color = secondaryTextColor)
                }
            }

            androidx.compose.material3.IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (currentTheme.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFF3F4F6))
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = textColor, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Why are you reporting?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
        Spacer(modifier = Modifier.height(10.dp))

        reasons.forEachIndexed { index, (title, desc, emoji) ->
            val isSelected = selectedReasonIndex == index
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReasonIndexChange(index) }
                    .background(
                        if (isSelected) (if (currentTheme.isDark) currentTheme.themeColor.copy(alpha = 0.15f) else Color(0xFFFFFAEB))
                        else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isSelected) (if (currentTheme.isDark) currentTheme.themeColor else Color(0xFFFDE68A)) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text(text = desc, fontSize = 11.sp, color = secondaryTextColor)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .border(1.5.dp, if (isSelected) (if (currentTheme.isDark) currentTheme.themeColor else Color(0xFFFBBF24)) else Color.LightGray, CircleShape)
                        .padding(3.dp)
                        .background(if (isSelected) (if (currentTheme.isDark) currentTheme.themeColor else Color(0xFFFBBF24)) else Color.Transparent, CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Evidence (automatically attached)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (currentTheme.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "🎤", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Last 1 min\nAudio", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "✓", color = Color(0xFF16A34A), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (currentTheme.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "💬", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Last 20\nMessages", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "✓", color = Color(0xFF16A34A), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (currentTheme.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "👥", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Speaker\nActivity", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "✓", color = Color(0xFF16A34A), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Add more (optional)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onScreenshotAttachChange(!screenshotAttached) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (screenshotAttached) Color(0xFFE8F5E9) else (if (currentTheme.isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF3F4F6))
                ),
                border = BorderStroke(1.dp, if (screenshotAttached) Color(0xFF81C784) else Color.Transparent)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🖼️", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (screenshotAttached) "Attached" else "Screenshot", fontSize = 12.sp, color = if (screenshotAttached) Color(0xFF2E7D32) else textColor, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { onVideoAttachChange(!videoAttached) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (videoAttached) Color(0xFFE8F5E9) else (if (currentTheme.isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF3F4F6))
                ),
                border = BorderStroke(1.dp, if (videoAttached) Color(0xFF81C784) else Color.Transparent)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📹", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (videoAttached) "Attached" else "Video (30s)", fontSize = 12.sp, color = if (videoAttached) Color(0xFF2E7D32) else textColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Additional details (optional)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = additionalDetails,
            onValueChange = { if (it.length <= 300) onDetailsChange(it) },
            placeholder = { Text(text = "Tell us more about what happened...", fontSize = 13.sp, color = secondaryTextColor) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = if (currentTheme.isDark) currentTheme.themeColor else ChillPurpleAccent,
                unfocusedBorderColor = if (currentTheme.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFE5E5EA),
                focusedLabelColor = if (currentTheme.isDark) currentTheme.themeColor else ChillPurpleAccent
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(text = "${additionalDetails.length}/300", fontSize = 11.sp, color = secondaryTextColor)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = currentTheme.accentColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Send, 
                    contentDescription = null, 
                    tint = if (currentTheme.isDark) Color(0xFF1E1B4B) else Color.White, 
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submit Report", 
                    color = if (currentTheme.isDark) Color(0xFF1E1B4B) else Color.White, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun ReportSubmittedBottomSheetContent(
    selectedThemeIndex: Int,
    onDismiss: () -> Unit
) {
    val currentTheme = roomThemes[selectedThemeIndex]
    val textColor = if (currentTheme.isDark) Color.White else Color.Black
    val secondaryTextColor = if (currentTheme.isDark) Color.White.copy(alpha = 0.7f) else Color.Gray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f)
            .background(Brush.verticalGradient(listOf(currentTheme.startColor, currentTheme.endColor)))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color(0xFFE8F5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✓", color = Color(0xFF2E7D32), fontSize = 36.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Report Submitted", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Thank you! Your report has been submitted successfully.",
                fontSize = 13.sp,
                color = secondaryTextColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (currentTheme.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFFFFBEB), RoundedCornerShape(16.dp))
                    .border(1.dp, if (currentTheme.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFFEF3C7), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(text = "What happens next?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                Spacer(modifier = Modifier.height(12.dp))

                val bulletPoints = listOf(
                    Triple(Icons.Default.Search, "Our team will review the report", Color(0xFFD97706)),
                    Triple(Icons.Default.Info, "If it violates our rules, action will be taken", Color(0xFFD97706)),
                    Triple(Icons.Default.Lock, "Your identity will remain private", Color(0xFFD97706)),
                    Triple(Icons.Default.Notifications, "You'll get a notification if we take action", Color(0xFFD97706))
                )

                bulletPoints.forEach { (icon, text, col) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = col, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = text, fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFBBF24)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Got it", color = Color(0xFF78350F), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun OneToOneChatBottomSheetContent(
    selectedThemeIndex: Int,
    chatUser: PrivateChatItem,
    messagesList: List<DirectMessage>,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val currentTheme = roomThemes[selectedThemeIndex]
    val textColor = if (currentTheme.isDark) Color.White else Color.Black
    val secondaryTextColor = if (currentTheme.isDark) Color.White.copy(alpha = 0.7f) else Color.Gray
    var typedMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .background(
                if (currentTheme.isDark) Color(0xFF111827) else Color(0xFFF9FAFB)
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (currentTheme.isDark) Color(0xFF1F2937) else Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        CuteBackArrowIcon(modifier = Modifier.size(20.dp), tint = textColor)
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    Box {
                        AsyncImage(
                            model = chatUser.imageUrl,
                            contentDescription = chatUser.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .size(18.dp)
                                .shadow(1.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircleFlag(
                                countryCode = chatUser.flagCode,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = chatUser.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(8.dp).background(LiveGreen, CircleShape))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🌙 11:10 PM", fontSize = 11.sp, color = secondaryTextColor)
                        }
                    }
                }

                Text(
                    text = "invite",
                    color = Color(0xFF00C4FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clickable { }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Status room bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (currentTheme.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFFEFBF0))
                    .border(width = 1.dp, color = if (currentTheme.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFFEF3C7))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CuteWaveformIcon(modifier = Modifier.size(18.dp), tint = Color(0xFFFBBF24))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${chatUser.name} is in this room",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                }

                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentTheme.isDark) currentTheme.themeColor else Color(0xFFFEF3C7)
                    ),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(text = "Join Room", color = if (currentTheme.isDark) Color.White else Color(0xFF78350F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Today", fontSize = 11.sp, color = secondaryTextColor.copy(alpha = 0.8f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messagesList) { msg ->
                        val alignEnd = msg.isSentByMe
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
                        ) {
                            if (!alignEnd) {
                                Box(modifier = Modifier.size(36.dp)) {
                                    AsyncImage(
                                        model = chatUser.imageUrl,
                                        contentDescription = chatUser.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Color.LightGray)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .offset(x = 1.dp, y = 1.dp)
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .padding(0.5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircleFlag(
                                            countryCode = chatUser.flagCode,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
                            ) {
                                Column(
                                    horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (alignEnd) 16.dp else 4.dp,
                                                    bottomEnd = if (alignEnd) 4.dp else 16.dp
                                                )
                                            )
                                            .background(
                                                if (alignEnd) {
                                                    Color(0xFF00C4FF)
                                                } else {
                                                    Color.White
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                if (alignEnd) Color.Transparent else Color(0xFFE5E7EB),
                                                RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (alignEnd) 16.dp else 4.dp,
                                                    bottomEnd = if (alignEnd) 4.dp else 16.dp
                                                )
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            fontSize = 14.sp,
                                            color = if (alignEnd) Color.White else Color.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = msg.time, fontSize = 10.sp, color = secondaryTextColor)
                                        if (alignEnd) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "✓✓", fontSize = 11.sp, color = Color(0xFF00C4FF), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (!alignEnd) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // AI rewrite/support pill next to received bubble
                                    Row(
                                        modifier = Modifier
                                            .background(Color.White, RoundedCornerShape(100.dp))
                                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(100.dp))
                                            .clickable { }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("AI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C4FF))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(8.dp), tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom input rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (currentTheme.isDark) Color(0xFF1F2937) else Color.White)
                .padding(bottom = 12.dp)
        ) {
            // [ EN v ] Translate... capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(if (currentTheme.isDark) Color.Black.copy(alpha = 0.2f) else Color(0xFFF3F4F6), RoundedCornerShape(100.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .background(if (currentTheme.isDark) Color.White.copy(alpha=0.1f) else Color.White, RoundedCornerShape(100.dp))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("EN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("^", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Translate...", fontSize = 12.sp, color = secondaryTextColor)
                }
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
            }

            // Input Field Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (currentTheme.isDark) Color.Black.copy(alpha = 0.3f) else Color(0xFFF3F4F6),
                            RoundedCornerShape(100.dp)
                        )
                        .border(
                            1.dp,
                            if (currentTheme.isDark) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = secondaryTextColor,
                        modifier = Modifier.size(20.dp).clickable { }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = typedMsg,
                        onValueChange = { typedMsg = it },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = textColor
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (typedMsg.isEmpty()) {
                                    Text(text = "Message...", fontSize = 14.sp, color = secondaryTextColor)
                                }
                                innerTextField()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (currentTheme.isDark) Color.White.copy(alpha=0.1f) else Color.White, CircleShape)
                            .border(1.dp, Color.LightGray.copy(alpha=0.5f), CircleShape)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("AI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C4FF))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (typedMsg.isNotBlank()) Color(0xFF00C4FF) else Color.LightGray)
                        .clickable {
                            if (typedMsg.isNotBlank()) {
                                onSendMessage(typedMsg)
                                typedMsg = ""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CuteSendIcon(modifier = Modifier.size(18.dp), tint = Color.White)
                }
            }

            // 5 Colorful Quick Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Gallery", tint = Color.Gray, modifier = Modifier.size(22.dp).clickable {})
                Icon(imageVector = Icons.Default.Face, contentDescription = "Emojis", tint = Color.Gray, modifier = Modifier.size(22.dp).clickable {})
                Icon(imageVector = Icons.Default.Add, contentDescription = "Call", tint = Color.Gray, modifier = Modifier.size(22.dp).clickable {})
                CustomGiftBoxIcon(tint = Color(0xFFEC4899), modifier = Modifier.size(22.dp).clickable {})
                Icon(imageVector = Icons.Default.Star, contentDescription = "Heart", tint = Color(0xFFEC4899), modifier = Modifier.size(22.dp).clickable {})
            }
        }
    }
}

@Composable
fun InviteFriendBottomSheetContent(
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    val textColor = if (isDarkMode) Color.White else Color.Black
    val bgColors = if (isDarkMode) Color(0xFF1F2937) else Color(0xFFF3F4F6)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Invite Friend",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(bgColors, CircleShape)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Share Platforms
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val platforms = listOf(
                "Copy Link" to Icons.Default.Share,
                "WhatsApp" to Icons.Default.Add,
                "Facebook" to Icons.Default.Face,
                "More" to Icons.Default.MoreVert
            )
            platforms.forEach { (name, icon) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onDismiss() }) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(bgColors, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = name, tint = textColor, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = name, fontSize = 12.sp, color = if (isDarkMode) Color.White.copy(alpha=0.7f) else Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Online Friends",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        val friends = listOf(
            "Ananya" to "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400",
            "Vihaan" to "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400",
            "Meera" to "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400",
            "Rahul" to "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400"
        )
        
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            friends.forEach { (name, img) ->
                var isInvited by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp)) {
                            AsyncImage(
                                model = img,
                                contentDescription = name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .size(16.dp)
                                    .background(if (isDarkMode) Color(0xFF1E293B) else Color.White, CircleShape)
                                    .padding(1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val flagCodeOptional = when (name) {
                                    "Ananya" -> "in"
                                    "Vihaan" -> "jp"
                                    "Meera" -> "us"
                                    "Rahul" -> "gb"
                                    else -> "in"
                                }
                                CircleFlag(
                                    countryCode = flagCodeOptional,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Online", fontSize = 12.sp, color = Color(0xFF10B981))
                        }
                    }
                    
                    Button(
                        onClick = { isInvited = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isInvited) bgColors else Color(0xFFFBBF24)
                        ),
                        shape = RoundedCornerShape(100.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isInvited) "Invited" else "Invite",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInvited) (if (isDarkMode) Color.White else Color.Gray) else Color(0xFF78350F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoreFeaturesBottomSheetContent(
    currentThemeIndex: Int,
    isDarkMode: Boolean,
    onThemeSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val textColor = if (isDarkMode) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Upcoming Features",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            CuteStarIcon(modifier = Modifier.size(16.dp), tint = Color(0xFFFBBF24))
            CuteStarIcon(modifier = Modifier.size(12.dp), tint = Color(0xFFFDE68A))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Grid of Features
        val features = listOf(
            "Video" to Icons.Default.PlayArrow,
            "Minigame" to Icons.Default.Star,
            "Message" to Icons.Default.Notifications,
            "Mic effect" to Icons.Default.Face,
            "Music" to Icons.Default.MoreVert,
            "Setting" to Icons.Default.Search
        )

        val context = androidx.compose.ui.platform.LocalContext.current
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            val chunks = features.chunked(3)
            chunks.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { (name, icon) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).clickable { Toast.makeText(context, "Coming Soon!", Toast.LENGTH_SHORT).show() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color(0xFFF3F4F6), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = name,
                                    tint = textColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Theme Room",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            roomThemes.forEachIndexed { i, theme ->
                val isSel = i == currentThemeIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onThemeSelect(i) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Brush.verticalGradient(listOf(theme.startColor, theme.endColor)), RoundedCornerShape(16.dp))
                            .border(
                                2.dp,
                                if (isSel) Color(0xFFFBBF24) else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (theme.isDark) "🌙" else "☀️",
                            fontSize = 28.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = theme.name.split(" ").first(),
                        fontSize = 13.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) textColor else (if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Gray)
                    )
                }
            }
        }
    }
}

