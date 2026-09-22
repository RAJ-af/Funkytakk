package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.UpdateVoiceRoomRequest
import com.example.SpeakerRequestInsert
import com.example.SupabaseRoomParticipant
import com.example.ModeratorActionRequest
import com.example.UpdateParticipantMicRequest
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
import com.example.ui.components.Soft3DFloatingBubble
import com.example.ui.components.Soft3DReportShield
import com.example.ui.components.Soft3DLeaveDoor
import com.example.ui.components.Soft3DHarassmentIcon
import com.example.ui.components.Soft3DSpamIcon
import com.example.ui.components.Soft3DHateIcon
import com.example.ui.components.Soft3DSexualContentIcon
import com.example.ui.components.Soft3DDangerousIcon
import com.example.ui.components.Soft3DOtherIcon
import com.example.ui.components.Soft3DMicrophoneIcon
import com.example.ui.components.Soft3DMessageStackIcon
import com.example.ui.components.Soft3DUsersGroupIcon
import com.example.ui.components.Soft3DScreenshotIcon
import com.example.ui.components.Soft3DVideoCamIcon
import com.example.ui.components.Soft3DSuccessBadge
import com.example.ui.components.Soft3DSearchAuditIcon
import com.example.ui.components.Soft3DRulesActionIcon
import com.example.ui.components.Soft3DPrivacyLockIcon
import com.example.ui.components.Soft3DBellNotificationIcon
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.AuthViewModel
import com.example.UserProfile
import com.example.SupabaseClient
import com.example.SendRoomMessageRequest
import com.example.SupabaseRoomMessage
import com.example.realtime.VoiceRoomRealtimeManager
import com.example.realtime.RealtimeTableEvent
import android.util.Log
import com.example.SupabaseSessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
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

fun mapCountryCodeToFlagEmoji(countryCode: String): String {
    return when (countryCode.lowercase()) {
        "in" -> "🇮🇳"
        "pk" -> "🇵🇰"
        "jp" -> "🇯🇵"
        "us" -> "🇺🇸"
        "gb" -> "🇬🇧"
        "ae" -> "🇦🇪"
        "cn" -> "🇨🇳"
        "es" -> "🇪🇸"
        "fr" -> "🇫🇷"
        "de" -> "🇩🇪"
        "it" -> "🇮🇹"
        "ru" -> "🇷🇺"
        "kr" -> "🇰🇷"
        else -> "🇮🇳"
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
    val gradientColors: List<Color>,
    val isHost: Boolean = false,
    val userId: String = ""
)

data class ChillRoomMessage(
    val senderName: String,
    val senderImageUrl: String,
    val badge: String? = null,
    val badgeColor: Color = ChillPurpleAccent,
    val text: String,
    val id: String? = null,
    val userId: String? = null,
    val createdAt: String? = null,
    val messageType: String? = "chat"
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
    viewModel: AuthViewModel? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val supabaseProfiles = if (viewModel != null) {
        viewModel.supabaseProfiles.collectAsState().value
    } else {
        emptyList()
    }

    val activeRoomId = viewModel?.activeRoomId?.collectAsState()?.value ?: "FT5272"
    val activeRoomTitle = viewModel?.activeRoomTitle?.collectAsState()?.value ?: "Hindi Voice Circle"

    // Room Config States (edited by user)
    var roomTitle by remember { mutableStateOf("Let's chat and play games") }
    var roomTags by remember { mutableStateOf("Chill") } // default tag chip from mockup
    var roomDescription by remember { mutableStateOf("A friendly chat room to share good vibes and play music.") }
    var roomLanguage by remember { mutableStateOf("Hindi + English") } // default from mockup
    var roomCountryCode by remember { mutableStateOf("in") }
    var isPrivateRoomState by remember { mutableStateOf(false) }
    var joinCodeState by remember { mutableStateOf("") }

    val hasOverlayPermission = remember(context) {
        com.example.service.OverlayPermissionHelper.canDrawOverlays(context)
    }
    var showOverlayBanner by remember { mutableStateOf(!hasOverlayPermission) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel?.setMicEnabled(true, context)
            if (com.example.service.OverlayPermissionHelper.canDrawOverlays(context)) {
                com.example.service.VoiceRoomOverlayManager.startServiceIfPermitted(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val currentUserId = remember(context) { SupabaseSessionManager.getUserId(context) ?: "" }
    val currentUserProfile = remember(supabaseProfiles, currentUserId) {
        supabaseProfiles.find { it.uid == currentUserId }
    }
    val currentUserName = remember(supabaseProfiles, currentUserId) {
        currentUserProfile?.displayName 
            ?: currentUserProfile?.username 
            ?: "User"
    }
    var participantsData by remember { mutableStateOf<List<SupabaseRoomParticipant>>(emptyList()) }
    var roomHostId by remember { mutableStateOf<String?>("") }
    val isSpeaker = remember(participantsData, currentUserId, roomHostId) {
        (currentUserId.isNotBlank() && currentUserId == roomHostId) ||
        participantsData.any { it.user_id == currentUserId && (it.role == "speaker" || it.role == "host") }
    }
    val scope = rememberCoroutineScope()
    var isMicOn by remember { mutableStateOf(true) }
    val messages = remember { mutableStateListOf<ChillRoomMessage>() }
    var renderedMessageCount by remember { mutableStateOf(30) }
    val renderedMessages = remember(messages, renderedMessageCount) {
        derivedStateOf {
            messages.takeLast(renderedMessageCount).reversed()
        }
    }.value
    val chatListState = rememberLazyListState()

    // Auto-scroll to the newest message whenever new messages arrive in the room
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            try {
                chatListState.animateScrollToItem(0)
            } catch (_: Exception) {}
        }
    }
    val activeReactions = remember { mutableStateListOf<FloatingEmojiId>() }
    val speakerReactions = remember { mutableStateMapOf<String, List<FloatingEmojiId>>() }

    val allRoomSeats = remember(participantsData, supabaseProfiles, roomHostId) {
        if (participantsData.isEmpty()) {
            val hostProfile = supabaseProfiles.find { it.uid == roomHostId }
            listOf(
                ChillRoomSpeaker(
                    name = hostProfile?.displayName ?: hostProfile?.username ?: "Host",
                    imageUrl = hostProfile?.avatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                    flagCode = hostProfile?.countryCode ?: "in",
                    isMuted = false,
                    isCenter = true,
                    gradientColors = listOf(Color(0xFFFFB800), Color(0xFFF59E0B), Color(0xFFFFB800)),
                    isHost = true,
                    userId = hostProfile?.uid ?: (roomHostId ?: "")
                )
            )
        } else {
            participantsData.map { p ->
                val pProfile = supabaseProfiles.find { it.uid == p.user_id }
                val name = pProfile?.displayName ?: pProfile?.username ?: "User"
                val avatar = pProfile?.avatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"
                val flagCode = pProfile?.countryCode ?: "in"
                val isHost = (p.role == "host") || (p.user_id == roomHostId)
                ChillRoomSpeaker(
                    name = name,
                    imageUrl = avatar,
                    flagCode = flagCode,
                    isMuted = !p.is_mic_enabled,
                    isCenter = isHost,
                    gradientColors = if (isHost) listOf(Color(0xFFFFB800), Color(0xFFF59E0B), Color(0xFFFFB800)) else listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
                    isHost = isHost,
                    userId = p.user_id
                )
            }
        }
    }

    val hostSeat = remember(allRoomSeats, roomHostId) {
        allRoomSeats.find { it.isHost } ?: allRoomSeats.first()
    }

    val speakerSeats = remember(participantsData, supabaseProfiles, roomHostId, hostSeat) {
        participantsData.filter { p ->
            val isHost = (p.role == "host") || (p.user_id == roomHostId)
            !isHost && p.role == "speaker"
        }.sortedBy { it.joined_at ?: "" }.take(4).map { p ->
            val pProfile = supabaseProfiles.find { it.uid == p.user_id }
            val snippet = com.example.realtime.VoiceRoomRealtimeManager.getUserSnippet(p.user_id)
            val name = pProfile?.displayName?.ifBlank { null }
                ?: pProfile?.username?.ifBlank { null }
                ?: snippet?.name?.ifBlank { null }
                ?: if (p.user_id == currentUserId) (currentUserProfile?.displayName ?: "Speaker") else "Speaker"
            val avatar = pProfile?.avatar?.ifBlank { null }
                ?: snippet?.avatar?.ifBlank { null }
                ?: if (p.user_id == currentUserId) currentUserProfile?.avatar else "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400"
            val flagCode = pProfile?.countryCode?.ifBlank { null }
                ?: snippet?.countryCode
                ?: "in"
            ChillRoomSpeaker(
                name = name,
                imageUrl = avatar ?: "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400",
                flagCode = flagCode,
                isMuted = !p.is_mic_enabled,
                isCenter = false,
                gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
                isHost = false,
                userId = p.user_id
            )
        }
    }

    val listenerSeats = remember(participantsData, supabaseProfiles, roomHostId) {
        participantsData.filter { p ->
            val isHost = (p.role == "host") || (p.user_id == roomHostId)
            !isHost && p.role != "speaker"
        }.map { p ->
            val pProfile = supabaseProfiles.find { it.uid == p.user_id }
            val snippet = com.example.realtime.VoiceRoomRealtimeManager.getUserSnippet(p.user_id)
            val name = pProfile?.displayName?.ifBlank { null }
                ?: pProfile?.username?.ifBlank { null }
                ?: snippet?.name?.ifBlank { null }
                ?: if (p.user_id == currentUserId) (currentUserProfile?.displayName ?: "Listener") else "Listener"
            val avatar = pProfile?.avatar?.ifBlank { null }
                ?: snippet?.avatar?.ifBlank { null }
                ?: if (p.user_id == currentUserId) currentUserProfile?.avatar else "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400"
            val flagCode = pProfile?.countryCode?.ifBlank { null }
                ?: snippet?.countryCode
                ?: "in"
            Triple(name, avatar ?: "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400", flagCode)
        }
    }

    val listenerUserIds = remember(participantsData, roomHostId) {
        participantsData.filter { p ->
            val isHost = (p.role == "host") || (p.user_id == roomHostId)
            !isHost && p.role != "speaker"
        }.map { it.user_id }
    }

    val speakers = remember(hostSeat, speakerSeats) {
        listOf(hostSeat) + speakerSeats
    }

    LaunchedEffect(participantsData.size, roomHostId, activeRoomId) {
        if (currentUserId.isNotBlank() && currentUserId == roomHostId && activeRoomId.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val token = SupabaseSessionManager.getBearerToken(context)
                    val sCount = (1 + speakerSeats.size).coerceAtLeast(1)
                    val lCount = listenerSeats.size
                    val totalP = 1 + speakerSeats.size + listenerSeats.size
                    SupabaseClient.api.updateVoiceRoom(
                        auth = token,
                        idEq = "eq.$activeRoomId",
                        updates = UpdateVoiceRoomRequest(
                            speaking_count = sCount,
                            listening_count = lCount,
                            participants = totalP
                        )
                    )
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(activeRoomId) {
        renderedMessageCount = 6
        withContext(Dispatchers.IO) {
            try {
                val roomRes = SupabaseClient.api.getVoiceRoomById(idEq = "eq.$activeRoomId")
                if (roomRes.isSuccessful) {
                    val roomObj = roomRes.body()?.firstOrNull()
                    if (roomObj != null) {
                        roomHostId = roomObj.host_id ?: ""
                        viewModel?.setActiveRoomHostId(roomHostId)
                        roomTitle = roomObj.title ?: "Voice Circle"
                        roomDescription = roomObj.description ?: ""
                        roomLanguage = roomObj.language ?: ""
                        roomCountryCode = roomObj.language_code.orEmpty().ifBlank { roomObj.country_code.orEmpty() }.ifBlank { LanguageData.getFlagCodeForLanguage(roomLanguage) }
                        isPrivateRoomState = roomObj.is_private ?: false
                        joinCodeState = roomObj.join_code ?: ""
                    }
                }
                val partsRes = SupabaseClient.api.getRoomParticipants(roomIdEq = "eq.$activeRoomId")
                val dbList = if (partsRes.isSuccessful) partsRes.body() ?: emptyList() else emptyList()
                val liveList = com.example.realtime.VoiceRoomRealtimeManager.getLiveParticipants(activeRoomId)
                val combined = (dbList + liveList).distinctBy { it.user_id }.toMutableList()
                if (currentUserId.isNotBlank() && combined.none { it.user_id == currentUserId }) {
                    combined.add(
                        SupabaseRoomParticipant(
                            room_id = activeRoomId,
                            user_id = currentUserId,
                            role = if (currentUserId == roomHostId) "host" else (if (isSpeaker) "speaker" else "listener"),
                            is_mic_enabled = isMicOn
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    participantsData = combined
                    val me = combined.find { it.user_id == currentUserId }
                    if (me != null) {
                        val newMicState = me.is_mic_enabled
                        if (isMicOn != newMicState) {
                            isMicOn = newMicState
                            com.example.realtime.AgoraVoiceManager.muteLocalAudioStream(!newMicState)
                        }
                    }
                }

                // Broadcast presence and ping so everyone in room knows we are here, and replies
                val myName = currentUserProfile?.displayName ?: currentUserProfile?.username ?: "Learner"
                val myAvatar = currentUserProfile?.avatar ?: ""
                val myCountry = currentUserProfile?.countryCode ?: "in"
                val myRole = if (currentUserId == roomHostId) "host" else (if (isSpeaker) "speaker" else "listener")

                com.example.realtime.VoiceRoomRealtimeManager.broadcastParticipantUpdate(
                    roomId = activeRoomId,
                    userId = currentUserId,
                    action = "joined",
                    role = myRole,
                    isMicEnabled = isMicOn,
                    userName = myName,
                    userAvatar = myAvatar,
                    countryCode = myCountry
                )
                com.example.realtime.VoiceRoomRealtimeManager.broadcastParticipantUpdate(
                    roomId = activeRoomId,
                    userId = currentUserId,
                    action = "ping"
                )

                // Query existing messages ordered by created_at
                val msgsRes = SupabaseClient.api.getRoomMessages(roomIdEq = "eq.$activeRoomId")
                if (msgsRes.isSuccessful) {
                    val list = msgsRes.body() ?: emptyList()
                    Log.d("VoiceRoomScreen", "[InitialFetch] Fetched ${list.size} room_messages for room $activeRoomId")
                    val mapped = list.map { msg ->
                        Log.d("VoiceRoomScreen", "[InitialFetch] Message: id=${msg.id}, type=${msg.message_type}, text='${msg.message}', userId=${msg.user_id}, createdAt=${msg.created_at}")
                        val rawType = msg.message_type
                        val isSystem = rawType?.equals("system", ignoreCase = true) == true
                        val p = supabaseProfiles.find { it.uid == msg.user_id }
                        val isMe = (msg.user_id == currentUserId)
                        val name = if (isMe) (p?.displayName ?: "You") else (p?.displayName ?: p?.username ?: "User")
                        val avatar = p?.avatar ?: (if (isMe) "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200" else "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400")
                        val badge = if (msg.user_id == roomHostId) "Host" else null
                        ChillRoomMessage(
                            senderName = name,
                            senderImageUrl = avatar,
                            badge = badge,
                            badgeColor = if (badge == "Host") ChillGoldIcon else ChillPurpleAccent,
                            text = msg.message ?: "",
                            id = msg.id,
                            userId = msg.user_id,
                            createdAt = msg.created_at,
                            messageType = if (isSystem) "system" else (rawType ?: "chat")
                        )
                    }
                    withContext(Dispatchers.Main) {
                        if (messages.isEmpty()) {
                            messages.addAll(mapped)
                        } else {
                            for (m in mapped) {
                                if (messages.none { it.id == m.id }) {
                                    messages.add(m)
                                }
                            }
                        }
                    }
                } else {
                    Log.e("VoiceRoomScreen", "[InitialFetch] Failed to fetch room_messages: code=${msgsRes.code()}, error=${msgsRes.errorBody()?.string()}")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("VoiceRoomScreen", "Error fetching voice room data from Supabase", e)
            }
        }
    }

    LaunchedEffect(activeRoomId) {
        VoiceRoomRealtimeManager.start()
        VoiceRoomRealtimeManager.subscribeRoomChannel(activeRoomId)
        VoiceRoomRealtimeManager.subscribeRoomMessages(activeRoomId)
        VoiceRoomRealtimeManager.events.collect { event ->
            when (event) {
                is RealtimeTableEvent.RoomEmojiBroadcast -> {
                    if (event.roomId == activeRoomId) {
                        val emojiUrl = if (event.emoji.startsWith("http")) {
                            event.emoji
                        } else {
                            com.example.ui.components.FluentEmojiAssets.getUrlForEmoji(event.emoji)
                        }
                        val reactionId = FloatingEmojiId(emojiUrl, System.currentTimeMillis() + (0..999).random())
                        withContext(Dispatchers.Main) {
                            activeReactions.add(reactionId)
                            val randomSpeaker = speakers.randomOrNull()?.name
                            if (randomSpeaker != null) {
                                val current = speakerReactions[randomSpeaker] ?: emptyList()
                                speakerReactions[randomSpeaker] = current + reactionId
                            }
                        }
                    }
                }
                is RealtimeTableEvent.ParticipantChanged -> {
                    if (event.roomId.isNullOrBlank() || event.roomId == activeRoomId) {
                        val targetUid = event.userId ?: event.record?.optString("user_id").orEmpty()
                        val action = event.eventType.lowercase()
                        val targetRole = event.role ?: event.record?.optString("role") ?: "listener"
                        val targetMic = event.isMicEnabled ?: event.record?.optBoolean("is_mic_enabled", false) ?: false

                        if (action == "ping") {
                            // Another participant is pinging for presence; reply with our info
                            if (currentUserId.isNotBlank()) {
                                val myName = currentUserProfile?.displayName ?: currentUserProfile?.username ?: "Learner"
                                val myAvatar = currentUserProfile?.avatar ?: ""
                                val myCountry = currentUserProfile?.countryCode ?: "in"
                                val myRole = if (currentUserId == roomHostId) "host" else (if (isSpeaker) "speaker" else "listener")
                                com.example.realtime.VoiceRoomRealtimeManager.broadcastParticipantUpdate(
                                    roomId = activeRoomId,
                                    userId = currentUserId,
                                    action = "sync_presence",
                                    role = myRole,
                                    isMicEnabled = isMicOn,
                                    userName = myName,
                                    userAvatar = myAvatar,
                                    countryCode = myCountry
                                )
                            }
                        } else if (action == "left" || action == "delete" || action == "kicked") {
                            if (targetUid.isNotBlank()) {
                                withContext(Dispatchers.Main) {
                                    participantsData = participantsData.filter { it.user_id != targetUid }
                                }
                            }
                        } else if (action in listOf("joined", "sync_presence", "update", "insert")) {
                            if (targetUid.isNotBlank()) {
                                val effectiveRole = if (targetUid == roomHostId) "host" else targetRole
                                withContext(Dispatchers.Main) {
                                    val existingIndex = participantsData.indexOfFirst { it.user_id == targetUid }
                                    if (existingIndex >= 0) {
                                        val updated = participantsData.toMutableList()
                                        updated[existingIndex] = updated[existingIndex].copy(
                                            role = effectiveRole,
                                            is_mic_enabled = targetMic
                                        )
                                        participantsData = updated
                                    } else {
                                        participantsData = participantsData + SupabaseRoomParticipant(
                                            room_id = activeRoomId,
                                            user_id = targetUid,
                                            role = effectiveRole,
                                            is_mic_enabled = targetMic
                                        )
                                    }
                                    if (targetUid == currentUserId) {
                                        if (isMicOn != targetMic) {
                                            isMicOn = targetMic
                                            com.example.realtime.AgoraVoiceManager.muteLocalAudioStream(!targetMic)
                                        }
                                    }
                                }
                            }
                        }

                        // Also fetch from API in background as fallback
                        scope.launch(Dispatchers.IO) {
                            try {
                                val partsRes = SupabaseClient.api.getRoomParticipants(roomIdEq = "eq.$activeRoomId")
                                if (partsRes.isSuccessful) {
                                    val list = partsRes.body() ?: emptyList()
                                    val liveList = com.example.realtime.VoiceRoomRealtimeManager.getLiveParticipants(activeRoomId)
                                    val combined = (list + liveList).distinctBy { it.user_id }
                                    withContext(Dispatchers.Main) {
                                        if (combined.isNotEmpty()) {
                                            participantsData = combined
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
                is RealtimeTableEvent.RoomChatBroadcast -> {
                    if (event.roomId == activeRoomId) {
                        val senderUid = event.userId
                        if (senderUid.isNotBlank() && senderUid != "anon_user" && senderUid != "System") {
                            withContext(Dispatchers.Main) {
                                if (participantsData.none { it.user_id == senderUid }) {
                                    participantsData = participantsData + SupabaseRoomParticipant(
                                        room_id = activeRoomId,
                                        user_id = senderUid,
                                        role = if (senderUid == roomHostId) "host" else "listener",
                                        is_mic_enabled = false
                                    )
                                }
                            }
                        }
                        if (event.message.contains("joined the room", ignoreCase = true) ||
                            event.message.contains("left the room", ignoreCase = true) ||
                            event.senderName.equals("System", ignoreCase = true)
                        ) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val partsRes = SupabaseClient.api.getRoomParticipants(roomIdEq = "eq.$activeRoomId")
                                    if (partsRes.isSuccessful) {
                                        val list = partsRes.body() ?: emptyList()
                                        val liveList = com.example.realtime.VoiceRoomRealtimeManager.getLiveParticipants(activeRoomId)
                                        val combined = (list + liveList).distinctBy { it.user_id }
                                        withContext(Dispatchers.Main) {
                                            if (combined.isNotEmpty()) {
                                                participantsData = combined
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                        withContext(Dispatchers.Main) {
                            if (messages.none { it.id == event.messageId }) {
                                val isMe = (event.userId == currentUserId)
                                val name = if (isMe) "You" else event.senderName.ifBlank { "User" }
                                val avatar = event.senderAvatar.ifBlank {
                                    if (isMe) "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200"
                                    else "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"
                                }
                                val badge = if (event.userId == roomHostId) "Host" else null
                                messages.add(
                                    ChillRoomMessage(
                                        senderName = name,
                                        senderImageUrl = avatar,
                                        badge = badge,
                                        badgeColor = if (badge == "Host") ChillGoldIcon else ChillPurpleAccent,
                                        text = event.message,
                                        id = event.messageId,
                                        userId = event.userId,
                                        createdAt = null
                                    )
                                )
                            }
                        }
                    }
                }
                is RealtimeTableEvent.RoomMessageInserted -> {
                    val record = event.record
                    Log.d("VoiceRoomScreen", "[Realtime] RoomMessageInserted received: record=$record, eventRoomId=${event.roomId}, activeRoomId=$activeRoomId")
                    if (record != null) {
                        val rId = record.optString("room_id")
                        if (rId == activeRoomId || event.roomId == activeRoomId || rId.isBlank()) {
                            val msgId = record.optString("id")
                            val userId = record.optString("user_id")
                            val text = record.optString("message")
                            val createdAt = record.optString("created_at")
                            val msgType = record.optString("message_type")
                            val isSystem = msgType.equals("system", ignoreCase = true)
                            val messageType = if (isSystem) "system" else if (msgType.isNullOrBlank()) "chat" else msgType

                            if (isSystem || text.contains("joined the room", ignoreCase = true) || text.contains("left the room", ignoreCase = true)) {
                                if (text.contains("joined the room", ignoreCase = true) && userId.isNotBlank() && userId != "anon_user" && userId != "System") {
                                    withContext(Dispatchers.Main) {
                                        if (participantsData.none { it.user_id == userId }) {
                                            participantsData = participantsData + SupabaseRoomParticipant(
                                                room_id = activeRoomId,
                                                user_id = userId,
                                                role = if (userId == roomHostId) "host" else "listener",
                                                is_mic_enabled = false
                                            )
                                        }
                                    }
                                }
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val partsRes = SupabaseClient.api.getRoomParticipants(roomIdEq = "eq.$activeRoomId")
                                        if (partsRes.isSuccessful) {
                                            val list = partsRes.body() ?: emptyList()
                                            val liveList = com.example.realtime.VoiceRoomRealtimeManager.getLiveParticipants(activeRoomId)
                                            val combined = (list + liveList).distinctBy { it.user_id }
                                            withContext(Dispatchers.Main) {
                                                if (combined.isNotEmpty()) {
                                                    participantsData = combined
                                                }
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            }

                            Log.d("VoiceRoomScreen", "[Realtime] Processing message: id=$msgId, type=$messageType (raw=$msgType), text='$text', userId=$userId")

                            withContext(Dispatchers.Main) {
                                if (msgId.isBlank() || messages.none { it.id == msgId }) {
                                    val p = supabaseProfiles.find { it.uid == userId }
                                    val isMe = (userId == currentUserId)
                                    val name = if (isMe) (p?.displayName ?: "You") else (p?.displayName ?: p?.username ?: "User")
                                    val avatar = p?.avatar ?: (if (isMe) "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200" else "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400")
                                    val badge = if (userId == roomHostId) "Host" else null
                                    messages.add(
                                        ChillRoomMessage(
                                            senderName = name,
                                            senderImageUrl = avatar,
                                            badge = badge,
                                            badgeColor = if (badge == "Host") ChillGoldIcon else ChillPurpleAccent,
                                            text = text,
                                            id = msgId,
                                            userId = userId,
                                            createdAt = createdAt,
                                            messageType = messageType
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    DisposableEffect(activeRoomId) {
        onDispose {
            VoiceRoomRealtimeManager.leaveRoomChannel(activeRoomId)
        }
    }

    var chatInputValue by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showReactionsBar by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showPeopleSheet by remember { mutableStateOf(false) }
    var showInviteFriendSheet by remember { mutableStateOf(false) }

    LaunchedEffect(showPeopleSheet) {
        if (showPeopleSheet && activeRoomId.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val partsRes = SupabaseClient.api.getRoomParticipants(roomIdEq = "eq.$activeRoomId")
                    if (partsRes.isSuccessful) {
                        val list = partsRes.body() ?: emptyList()
                        withContext(Dispatchers.Main) {
                            participantsData = list
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(activeRoomId) {
        while (isActive && activeRoomId.isNotBlank()) {
            delay(5000)
            try {
                val partsRes = SupabaseClient.api.getRoomParticipants(roomIdEq = "eq.$activeRoomId")
                if (partsRes.isSuccessful) {
                    val list = partsRes.body() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        participantsData = list
                    }
                }
            } catch (_: Exception) {}
        }
    }
    val vmMicEnabled by viewModel?.isMicEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    LaunchedEffect(vmMicEnabled) {
        isMicOn = vmMicEnabled
    }
    var isSpeakerRequestPending by remember { mutableStateOf(false) }
    var isSpeakerRequestSubmitting by remember { mutableStateOf(false) }
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
    
    var selectedThemeIndex by remember { mutableStateOf(0) } // Default: "Golden Glow" at index 0
    var isRoomDarkMode by remember { mutableStateOf(false) }
    var selectedDarkBgIndex by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    
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

    val onSendRoomMessage: (String) -> Unit = { text ->
        if (text.isNotBlank()) {
            val msgToSend = text.trim()
            chatInputValue = ""
            val localMsgId = "msg_${System.currentTimeMillis()}_${(100..999).random()}"
            val currentUserProfile = supabaseProfiles.find { it.uid == currentUserId }
            val senderName = currentUserProfile?.displayName ?: currentUserProfile?.username ?: "You"
            val senderAvatar = currentUserProfile?.avatar ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200"
            val badge = if (currentUserId == roomHostId) "Host" else null

            // 1. Optimistic UI update: message appears instantly for the local user
            if (messages.none { it.id == localMsgId }) {
                messages.add(
                    ChillRoomMessage(
                        senderName = "You",
                        senderImageUrl = senderAvatar,
                        badge = badge,
                        badgeColor = if (badge == "Host") ChillGoldIcon else ChillPurpleAccent,
                        text = msgToSend,
                        id = localMsgId,
                        userId = currentUserId,
                        createdAt = null
                    )
                )
            }

            val uid = currentUserId.ifBlank { SupabaseSessionManager.getUserId(context) ?: "anon_user" }

            // 2. Broadcast via Supabase Realtime WebSocket channel to all participants in this room
            VoiceRoomRealtimeManager.broadcastRoomMessage(
                roomId = activeRoomId,
                message = msgToSend,
                userId = uid,
                senderName = senderName,
                senderAvatar = senderAvatar,
                messageId = localMsgId
            )

            // 3. Persist to Postgres room_messages table via REST API
            scope.launch(Dispatchers.IO) {
                try {
                    val token = SupabaseSessionManager.getBearerToken(context)
                    val requestUid = when {
                        uid.isNotBlank() && uid != "anon_user" -> uid
                        currentUserProfile?.uid?.isNotBlank() == true -> currentUserProfile.uid
                        else -> "00000000-0000-0000-0000-000000000000"
                    }
                    val request = SendRoomMessageRequest(
                        room_id = activeRoomId,
                        user_id = requestUid,
                        message = msgToSend
                    )
                    val res = SupabaseClient.api.sendRoomMessage(
                        auth = token,
                        request = request
                    )
                    if (res.isSuccessful) {
                        val inserted = res.body()?.firstOrNull()
                        if (inserted != null) {
                            withContext(Dispatchers.Main) {
                                val idx = messages.indexOfFirst { it.id == localMsgId }
                                if (idx >= 0) {
                                    messages[idx] = messages[idx].copy(
                                        id = inserted.id ?: localMsgId,
                                        createdAt = inserted.created_at
                                    )
                                }
                            }
                        }
                    } else {
                        Log.i("VoiceRoomScreen", "Room message broadcasted via Realtime channel (DB insert status ${res.code()})")
                    }
                } catch (e: Exception) {
                    Log.i("VoiceRoomScreen", "Room message sent locally & broadcasted (DB sync note: ${e.message})")
                }
            }
        }
    }

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
                        .clickable {
                            if (roomHostId == currentUserId) {
                                showEditRoomSheet = true
                            } else {
                                Toast.makeText(context, "Only the Host can edit room settings!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = currentTheme.primaryTextColor,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                if (com.example.service.OverlayPermissionHelper.canDrawOverlays(context)) {
                                    com.example.service.VoiceRoomOverlayManager.startServiceIfPermitted(context)
                                }
                                onBack()
                            }
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
                        
                        val menuBg = if (currentTheme.isDark) Color(0xFF1E293B).copy(alpha = 0.98f) else Color.White
                        val menuBorder = if (currentTheme.isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0)
                        val menuTitleColor = if (currentTheme.isDark) Color.White else Color(0xFF0F172A)
                        val menuSubtitleColor = if (currentTheme.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        val menuDividerColor = if (currentTheme.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)

                        DropdownMenu(
                            expanded = showThreeDotMenu,
                            onDismissRequest = { showThreeDotMenu = false },
                            shape = RoundedCornerShape(20.dp),
                            containerColor = menuBg,
                            tonalElevation = 8.dp,
                            shadowElevation = 14.dp,
                            border = BorderStroke(1.dp, menuBorder),
                            modifier = Modifier
                                .width(230.dp)
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Soft3DFloatingBubble(modifier = Modifier.size(34.dp))
                                },
                                text = {
                                    Column {
                                        Text(
                                            text = "Floating Bubble",
                                            color = menuTitleColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Multitask in background",
                                            color = menuSubtitleColor,
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showThreeDotMenu = false
                                    if (com.example.service.OverlayPermissionHelper.canDrawOverlays(context)) {
                                        com.example.service.VoiceRoomOverlayManager.startServiceIfPermitted(context)
                                        Toast.makeText(context, "Floating bubble active! You can now use other apps.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        com.example.service.OverlayPermissionHelper.requestOverlayPermission(context)
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            HorizontalDivider(
                                color = menuDividerColor, 
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Soft3DReportShield(modifier = Modifier.size(34.dp))
                                },
                                text = { 
                                    Column {
                                        Text(
                                            text = "Report Room", 
                                            color = menuTitleColor, 
                                            fontSize = 14.sp, 
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Flag safety concerns",
                                            color = menuSubtitleColor,
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                onClick = { 
                                    showThreeDotMenu = false
                                    showReportDialog = true
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            HorizontalDivider(
                                color = menuDividerColor, 
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Soft3DLeaveDoor(modifier = Modifier.size(34.dp))
                                },
                                text = { 
                                    Column {
                                        Text(
                                            text = "Leave Room", 
                                            color = Color(0xFFEF4444), 
                                            fontSize = 14.sp, 
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Exit current circle",
                                            color = Color(0xFFF87171).copy(alpha = 0.85f),
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showThreeDotMenu = false
                                    viewModel?.leaveRoom(context)
                                    onBack()
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (showOverlayBanner && !com.example.service.OverlayPermissionHelper.canDrawOverlays(context)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRoomDarkMode) Color(0xFF1E1A11) else Color(0xFFFFFBEB)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFF59E0B).copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💬", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Floating Chat Head",
                                color = currentTheme.primaryTextColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Keep talking while using other apps",
                                color = currentTheme.secondaryTextColor,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                com.example.service.OverlayPermissionHelper.requestOverlayPermission(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showOverlayBanner = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = currentTheme.secondaryTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (isPrivateRoomState) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRoomDarkMode) Color(0xFF1E293B) else Color(0xFFFFFBEB)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isRoomDarkMode) Color(0xFF334155) else Color(0xFFFEF3C7)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Private Room Icon",
                                tint = if (isRoomDarkMode) Color(0xFFFBBF24) else Color(0xFFB45309),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Private Room Code",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isRoomDarkMode) Color.LightGray else Color.Gray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = joinCodeState.ifEmpty { "------" },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isRoomDarkMode) Color.White else Color.Black,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        // Copy / Share button
                        androidx.compose.material3.IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Join Code", joinCodeState)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Join code copied! Share with your friends.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isRoomDarkMode) Color.White.copy(alpha = 0.1f) else Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Copy Code",
                                tint = if (isRoomDarkMode) Color.White else Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. SPEAKERS VISUAL SYSTEM
            SpeakerSeatsGrid(
                hostSeat = hostSeat,
                speakerSeats = speakerSeats,
                speakerReactions = speakerReactions,
                currentTheme = currentTheme,
                isRoomDarkMode = isRoomDarkMode,
                activeRoomId = activeRoomId,
                currentUserId = currentUserId,
                scope = scope,
                onHostClick = {
                    oneToOneChatUser = PrivateChatItem("host", hostSeat.name, hostSeat.imageUrl, hostSeat.flagCode, "Host", "Now", 0, true, false)
                    showOneToOneChatSheet = true
                },
                onSpeakerClick = { index, sp ->
                    oneToOneChatUser = PrivateChatItem("sp${index + 1}", sp.name, sp.imageUrl, sp.flagCode, "", "Now", 0, true, false)
                    showOneToOneChatSheet = true
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 3. MINIMAL COMPACT CHAT LOG ROW
            RoomChatPanel(
                chatListState = chatListState,
                renderedMessages = renderedMessages,
                messages = messages,
                renderedMessageCount = renderedMessageCount,
                onRenderedCountChange = { renderedMessageCount = it },
                currentTheme = currentTheme,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                isSpeakerRequestSubmitting = isSpeakerRequestSubmitting,
                onSpeakerRequestSubmittingChange = { isSpeakerRequestSubmitting = it },
                isSpeakerRequestPending = isSpeakerRequestPending,
                onSpeakerRequestPendingChange = { isSpeakerRequestPending = it },
                activeRoomId = activeRoomId,
                scope = scope
            )

            androidx.compose.animation.AnimatedVisibility(visible = showEmojiPicker) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .background(
                            if (currentTheme.isDark) Color(0xFF1E293B).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f),
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            if (currentTheme.isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6),
                            RoundedCornerShape(20.dp)
                        )
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
                                onSendRoomMessage(chatInputValue)
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
                    if (chatInputValue.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(currentTheme.accentColor)
                                .clickable { onSendRoomMessage(chatInputValue) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
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
                        .background(
                            if (currentTheme.isDark) Color(0xFF1E293B).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f),
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            if (currentTheme.isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFF3F4F6),
                            RoundedCornerShape(20.dp)
                        )
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
                                    val uid = currentUserId.ifBlank { SupabaseSessionManager.getUserId(context) ?: "anon_user" }
                                    // Ephemeral Supabase Realtime Broadcast to per-room channel realtime:room:{id}
                                    VoiceRoomRealtimeManager.broadcastEmoji(activeRoomId, url, uid)

                                    val reactionId = FloatingEmojiId(url, System.currentTimeMillis() + index)
                                    activeReactions.add(reactionId)

                                    val randomSpeaker = speakers.random().name
                                    val current = speakerReactions[randomSpeaker] ?: emptyList()
                                    speakerReactions[randomSpeaker] = current + reactionId
                                    
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
                val realPeopleCount = if (participantsData.isNotEmpty()) participantsData.size else allRoomSeats.size.coerceAtLeast(1)
                VoiceBottomBarItem(
                    icon = { CustomPeopleGroupIcon(tint = inactiveIconTint, modifier = Modifier.size(26.dp)) },
                    label = "People",
                    tint = inactiveIconTint,
                    badgeText = "$realPeopleCount",
                    onClick = { showPeopleSheet = true }
                )

                val myParticipant = participantsData.find { it.user_id == currentUserId }
                val isHost = (currentUserId == roomHostId)
                val myRole = myParticipant?.role ?: if (isHost) "speaker" else "listener"
                val isListener = !isHost && (myRole == "listener")

                if (isListener) {
                    // Listener-side Request to Speak Action Bubble
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.offset(y = (-6).dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    if (isSpeakerRequestPending) Color(0xFF3F3F46) else ChillGoldIcon
                                )
                                .clickable(enabled = !isSpeakerRequestSubmitting) {
                                    if (isSpeakerRequestPending) {
                                        Toast.makeText(
                                            context,
                                            "Your request to speak is pending host approval",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        isSpeakerRequestSubmitting = true
                                        val token = SupabaseSessionManager.getBearerToken(context)
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val res = SupabaseClient.api.createSpeakerRequest(
                                                    auth = token,
                                                    body = SpeakerRequestInsert(
                                                        room_id = activeRoomId,
                                                        user_id = currentUserId
                                                    )
                                                )
                                                withContext(Dispatchers.Main) {
                                                    isSpeakerRequestSubmitting = false
                                                    if (res.isSuccessful) {
                                                        isSpeakerRequestPending = true
                                                        Toast.makeText(
                                                            context,
                                                            "Request to speak sent to host!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else {
                                                        isSpeakerRequestPending = true
                                                        Toast.makeText(
                                                            context,
                                                            "Request to speak submitted.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    isSpeakerRequestSubmitting = false
                                                    isSpeakerRequestPending = true
                                                    Toast.makeText(
                                                        context,
                                                        "Request submitted to host.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSpeakerRequestPending) {
                                CustomPendingHourglassIcon(
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            } else {
                                CustomRaisedHandIcon(
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isSpeakerRequestPending) "Requested" else "Request to Speak",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSpeakerRequestPending) Color(0xFFA1A1AA) else ChillGoldIcon
                        )
                    }
                } else {
                    // Main Mic Action Bubble for Speakers and Host
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
                                .clickable {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        if (viewModel != null) {
                                            viewModel.toggleMic(context)
                                        } else {
                                            isMicOn = !isMicOn
                                        }
                                    }
                                },
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
                hostSeat = hostSeat,
                speakerSeats = speakerSeats,
                listenerSeats = listenerSeats,
                listenerUserIds = listenerUserIds,
                isCurrentUserHost = (currentUserId == roomHostId && currentUserId.isNotBlank()),
                currentUserId = currentUserId,
                onUserClick = { chatUser ->
                    oneToOneChatUser = chatUser
                    showOneToOneChatSheet = true
                    showPeopleSheet = false
                },
                onInviteClick = {
                    showInviteFriendSheet = true
                    showPeopleSheet = false
                },
                onModeratorAction = { targetUserId, targetUserName, actionType ->
                    if (targetUserId.isNotBlank()) {
                        scope.launch(Dispatchers.IO) {
                            val bearerToken = SupabaseSessionManager.getBearerToken(context)
                            try {
                                val req = ModeratorActionRequest(
                                    target_user_id = targetUserId,
                                    action_type = actionType,
                                    room_id = activeRoomId,
                                    reason = "Host moderation action"
                                )
                                val res = SupabaseClient.api.executeModeratorAction(
                                    auth = bearerToken,
                                    request = req
                                )
                                withContext(Dispatchers.Main) {
                                    if (res.isSuccessful) {
                                        val actionDesc = when (actionType) {
                                            "mute_mic" -> "muted"
                                            "kick_from_room" -> "removed from the room"
                                            "ban_user" -> "banned"
                                            else -> "moderated"
                                        }
                                        Toast.makeText(context, "$targetUserName has been $actionDesc.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        when (actionType) {
                                            "mute_mic" -> {
                                                SupabaseClient.api.updateRoomParticipantMic(
                                                    auth = bearerToken,
                                                    roomIdEq = "eq.$activeRoomId",
                                                    userIdEq = "eq.$targetUserId",
                                                    body = UpdateParticipantMicRequest(is_mic_enabled = false)
                                                )
                                                Toast.makeText(context, "$targetUserName has been muted.", Toast.LENGTH_SHORT).show()
                                            }
                                            "kick_from_room" -> {
                                                SupabaseClient.api.leaveRoomParticipant(
                                                    auth = bearerToken,
                                                    roomIdEq = "eq.$activeRoomId",
                                                    userIdEq = "eq.$targetUserId"
                                                )
                                                Toast.makeText(context, "$targetUserName has been removed from room.", Toast.LENGTH_SHORT).show()
                                            }
                                            else -> {
                                                Toast.makeText(context, "Action failed: ${res.code()}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    try {
                                        when (actionType) {
                                            "mute_mic" -> {
                                                SupabaseClient.api.updateRoomParticipantMic(
                                                    auth = bearerToken,
                                                    roomIdEq = "eq.$activeRoomId",
                                                    userIdEq = "eq.$targetUserId",
                                                    body = UpdateParticipantMicRequest(is_mic_enabled = false)
                                                )
                                                Toast.makeText(context, "$targetUserName has been muted.", Toast.LENGTH_SHORT).show()
                                            }
                                            "kick_from_room" -> {
                                                SupabaseClient.api.leaveRoomParticipant(
                                                    auth = bearerToken,
                                                    roomIdEq = "eq.$activeRoomId",
                                                    userIdEq = "eq.$targetUserId"
                                                )
                                                Toast.makeText(context, "$targetUserName has been removed from room.", Toast.LENGTH_SHORT).show()
                                            }
                                            else -> {
                                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e2: Exception) {
                                        Toast.makeText(context, "Failed: ${e2.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
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
                onLanguageChange = { 
                    roomLanguage = it
                    roomCountryCode = LanguageData.getFlagCodeForLanguage(it)
                },
                roomCountryCode = roomCountryCode,
                onCountryCodeChange = { roomCountryCode = it },
                selectedThemeIndex = selectedThemeIndex,
                onThemeChange = { index ->
                    selectedThemeIndex = index
                    isRoomDarkMode = roomThemes[index].isDark
                },
                isPrivate = isPrivateRoomState,
                onPrivateChange = { isPrivateRoomState = it },
                joinCode = joinCodeState,
                onJoinCodeChange = { joinCodeState = it },
                isHost = (roomHostId == currentUserId),
                onApplyChanges = {
                    val newTitle = roomTitle
                    val newDesc = roomDescription
                    val newLang = roomLanguage
                    val newCountryCode = LanguageData.resolveFlagCode(newLang, roomCountryCode)
                    roomCountryCode = newCountryCode
                    val newTag = roomTags
                    val newPrivate = isPrivateRoomState
                    val newCode = joinCodeState

                    // Update active title in ViewModel immediately
                    viewModel?.updateActiveRoomTitle(newTitle)

                    scope.launch(Dispatchers.IO) {
                        try {
                            val token = SupabaseSessionManager.getBearerToken(context)
                            val request = UpdateVoiceRoomRequest(
                                title = newTitle,
                                description = newDesc,
                                language = newLang,
                                country_code = newCountryCode,
                                language_code = newCountryCode,
                                language_flag = "",
                                tag = newTag,
                                is_private = newPrivate,
                                join_code = if (newPrivate) newCode else null
                            )
                            val res = SupabaseClient.api.updateVoiceRoom(
                                auth = token,
                                idEq = "eq.$activeRoomId",
                                updates = request
                            )
                            if (res.isSuccessful) {
                                Log.d("VoiceRoomScreen", "Supabase room updated successfully: $activeRoomId")
                            } else {
                                Log.w("VoiceRoomScreen", "Supabase update status: ${res.code()}")
                            }
                        } catch (e: Exception) {
                            Log.e("VoiceRoomScreen", "Supabase update error", e)
                        }

                        // Broadcast room setting change via WebSocket realtime
                        try {
                            VoiceRoomRealtimeManager.broadcastRoomMessage(
                                roomId = activeRoomId,
                                message = "Room updated: $newTitle",
                                userId = currentUserId,
                                senderName = "System",
                                senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
                                messageId = "sys_${System.currentTimeMillis()}"
                            )
                        } catch (_: Exception) {}

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Room settings updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showEditRoomSheet = false
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
        val reportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val currentThemeForReport = roomThemes.getOrElse(selectedThemeIndex) { roomThemes[0] }
        val sheetContainerColor = if (currentThemeForReport.isDark) Color(0xFF0F172A) else Color.White
        val dragHandleColor = if (currentThemeForReport.isDark) Color.White.copy(alpha = 0.25f) else Color(0xFFE2E8F0)

        ModalBottomSheet(
            onDismissRequest = { showReportDialog = false },
            sheetState = reportSheetState,
            containerColor = sheetContainerColor,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 6.dp)
                        .width(44.dp)
                        .height(5.dp)
                        .background(dragHandleColor, CircleShape)
                )
            }
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
        val submittedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val currentThemeForSubmitted = roomThemes.getOrElse(selectedThemeIndex) { roomThemes[0] }
        val sheetContainerColor = if (currentThemeForSubmitted.isDark) Color(0xFF0F172A) else Color.White
        val dragHandleColor = if (currentThemeForSubmitted.isDark) Color.White.copy(alpha = 0.25f) else Color(0xFFE2E8F0)

        ModalBottomSheet(
            onDismissRequest = { showReportSubmittedSheet = false },
            sheetState = submittedSheetState,
            containerColor = sheetContainerColor,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 6.dp)
                        .width(44.dp)
                        .height(5.dp)
                        .background(dragHandleColor, CircleShape)
                )
            }
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
        val r = w * 0.07f // soft, thick rounded radius for fingers

        // Palm base (very rounded)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.26f, h * 0.42f),
            size = Size(w * 0.48f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.18f)
        )

        // Wrist (soft bottom base)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.36f, h * 0.72f),
            size = Size(w * 0.28f, h * 0.14f),
            cornerRadius = CornerRadius(w * 0.06f)
        )

        // 4 Rounded fingers smoothly rising up with rounded caps
        // Index
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.28f, h * 0.18f),
            size = Size(w * 0.10f, h * 0.34f),
            cornerRadius = CornerRadius(r)
        )
        // Middle (longest)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.40f, h * 0.12f),
            size = Size(w * 0.10f, h * 0.40f),
            cornerRadius = CornerRadius(r)
        )
        // Ring
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.52f, h * 0.16f),
            size = Size(w * 0.10f, h * 0.36f),
            cornerRadius = CornerRadius(r)
        )
        // Pinky (shortest)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.64f, h * 0.26f),
            size = Size(w * 0.10f, h * 0.26f),
            cornerRadius = CornerRadius(r)
        )

        // Smooth thumb curved outwards
        val thumbPath = Path().apply {
            moveTo(w * 0.28f, h * 0.50f)
            quadraticTo(w * 0.10f, h * 0.52f, w * 0.12f, h * 0.64f)
            quadraticTo(w * 0.14f, h * 0.72f, w * 0.32f, h * 0.68f)
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
fun CustomPendingHourglassIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.08f
        val capCorner = w * 0.06f

        // Top soft cap
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.22f, h * 0.15f),
            size = Size(w * 0.56f, h * 0.08f),
            cornerRadius = CornerRadius(capCorner)
        )

        // Bottom soft cap
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.22f, h * 0.77f),
            size = Size(w * 0.56f, h * 0.08f),
            cornerRadius = CornerRadius(capCorner)
        )

        // Curved soft glass body
        val glassPath = Path().apply {
            moveTo(w * 0.28f, h * 0.23f)
            // Left curve in to neck, then out to bottom
            cubicTo(w * 0.28f, h * 0.38f, w * 0.44f, h * 0.46f, w * 0.44f, h * 0.5f)
            cubicTo(w * 0.44f, h * 0.54f, w * 0.28f, h * 0.62f, w * 0.28f, h * 0.77f)
            // Bottom line
            lineTo(w * 0.72f, h * 0.77f)
            // Right curve back to top
            cubicTo(w * 0.72f, h * 0.62f, w * 0.56f, h * 0.54f, w * 0.56f, h * 0.5f)
            cubicTo(w * 0.56f, h * 0.46f, w * 0.72f, h * 0.38f, w * 0.72f, h * 0.23f)
            close()
        }
        drawPath(
            path = glassPath,
            color = tint,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Falling sand stream in center
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.42f),
            end = Offset(w * 0.5f, h * 0.58f),
            strokeWidth = w * 0.04f,
            cap = StrokeCap.Round
        )

        // Sand accumulation at bottom (soft triangle/half-circle shape)
        val bottomSand = Path().apply {
            moveTo(w * 0.38f, h * 0.74f)
            quadraticTo(w * 0.5f, h * 0.58f, w * 0.62f, h * 0.74f)
            close()
        }
        drawPath(bottomSand, color = tint)

        // Sand accumulation at top (soft inverted triangle/half-circle shape)
        val topSand = Path().apply {
            moveTo(w * 0.34f, h * 0.26f)
            quadraticTo(w * 0.5f, h * 0.40f, w * 0.66f, h * 0.26f)
            close()
        }
        drawPath(topSand, color = tint)
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
    val currentTheme = roomThemes.getOrElse(selectedThemeIndex) { roomThemes[0] }
    val isDark = currentTheme.isDark
    val sheetBg = if (isDark) Color(0xFF0F172A) else Color.White
    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)
    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val closeBtnBg = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFF1F5F9)
    val closeBtnTint = if (isDark) Color.White else Color(0xFF475569)

    data class ReportReasonItem(
        val title: String,
        val desc: String,
        val icon: @Composable () -> Unit
    )

    val reasons = listOf(
        ReportReasonItem("Harassment / Abuse", "Bullying, personal attacks, rude or hostile conduct") {
            Soft3DHarassmentIcon(modifier = Modifier.size(36.dp))
        },
        ReportReasonItem("Spam / Promotion", "Commercial ads, unwanted links or repeated spam") {
            Soft3DSpamIcon(modifier = Modifier.size(36.dp))
        },
        ReportReasonItem("Hate Speech", "Discrimination, intolerance or malicious abuse") {
            Soft3DHateIcon(modifier = Modifier.size(36.dp))
        },
        ReportReasonItem("Sexual / Inappropriate", "Unwanted explicit themes or suggestive behavior") {
            Soft3DSexualContentIcon(modifier = Modifier.size(36.dp))
        },
        ReportReasonItem("Dangerous Activity", "Threats of violence, illegal acts or severe harm") {
            Soft3DDangerousIcon(modifier = Modifier.size(36.dp))
        },
        ReportReasonItem("Other Concerns", "Rule violations or safety issues not listed above") {
            Soft3DOtherIcon(modifier = Modifier.size(36.dp))
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(sheetBg)
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Soft3DReportShield(modifier = Modifier.size(42.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Report Room",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = titleColor
                    )
                    Text(
                        text = "Help maintain a respectful community",
                        fontSize = 12.sp,
                        color = subtitleColor
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(closeBtnBg)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = closeBtnTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Why are you reporting?
        Text(
            text = "Why are you reporting?",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = titleColor
        )
        Text(
            text = "Select the option that best describes the issue",
            fontSize = 12.sp,
            color = subtitleColor,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        reasons.forEachIndexed { index, item ->
            val isSelected = selectedReasonIndex == index
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) (if (isDark) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFFFBEB)) 
                        else cardBg
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) Color(0xFFF59E0B) else cardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onReasonIndexChange(index) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    item.icon()
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) (if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E)) else titleColor
                        )
                        Text(
                            text = item.desc,
                            fontSize = 11.sp,
                            color = if (isSelected) (if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)) else subtitleColor,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            if (isSelected) Color(0xFFF59E0B) else Color.Transparent,
                            CircleShape
                        )
                        .border(
                            width = if (isSelected) 0.dp else 1.5.dp,
                            color = if (isSelected) Color.Transparent else (if (isDark) Color.White.copy(alpha = 0.25f) else Color(0xFFCBD5E1)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF451A03) else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Evidence section
        Text(
            text = "Evidence (automatically attached)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = titleColor
        )
        Text(
            text = "Encrypted telemetry to verify reported incidents",
            fontSize = 12.sp,
            color = subtitleColor,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(cardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Soft3DMicrophoneIcon(modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Last 1 min\nAudio Clip",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (isDark) Color(0xFF065F46).copy(alpha = 0.5f) else Color(0xFFDCFCE7),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "✓ Attached",
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF15803D),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Card 2
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(cardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Soft3DMessageStackIcon(modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Recent 20\nMessages",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (isDark) Color(0xFF065F46).copy(alpha = 0.5f) else Color(0xFFDCFCE7),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "✓ Attached",
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF15803D),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Card 3
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(cardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Soft3DUsersGroupIcon(modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Speaker\nActivity Log",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (isDark) Color(0xFF065F46).copy(alpha = 0.5f) else Color(0xFFDCFCE7),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "✓ Attached",
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF15803D),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Optional add-ons
        Text(
            text = "Add more proof (optional)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = titleColor
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onScreenshotAttachChange(!screenshotAttached) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (screenshotAttached) {
                        if (isDark) Color(0xFF065F46).copy(alpha = 0.5f) else Color(0xFFECFDF5)
                    } else {
                        cardBg
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (screenshotAttached) Color(0xFF10B981) else cardBorder
                ),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Soft3DScreenshotIcon(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (screenshotAttached) "Attached" else "Screenshot",
                        fontSize = 12.sp,
                        color = if (screenshotAttached) (if (isDark) Color(0xFF34D399) else Color(0xFF047857)) else titleColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = { onVideoAttachChange(!videoAttached) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (videoAttached) {
                        if (isDark) Color(0xFF065F46).copy(alpha = 0.5f) else Color(0xFFECFDF5)
                    } else {
                        cardBg
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (videoAttached) Color(0xFF10B981) else cardBorder
                ),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Soft3DVideoCamIcon(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (videoAttached) "Attached" else "Clip (30s)",
                        fontSize = 12.sp,
                        color = if (videoAttached) (if (isDark) Color(0xFF34D399) else Color(0xFF047857)) else titleColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Additional details
        Text(
            text = "Additional details (optional)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = titleColor
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = additionalDetails,
            onValueChange = { if (it.length <= 300) onDetailsChange(it) },
            placeholder = {
                Text(
                    text = "Provide any specific context that helps our reviewers...",
                    fontSize = 13.sp,
                    color = subtitleColor
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            minLines = 3,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = titleColor,
                unfocusedTextColor = titleColor,
                focusedBorderColor = Color(0xFFF59E0B),
                unfocusedBorderColor = cardBorder,
                focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFAFAFA)
            )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${additionalDetails.length}/300",
                fontSize = 11.sp,
                color = subtitleColor
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Submit Button
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF59E0B)
            ),
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Soft3DReportShield(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Submit Report",
                    color = Color(0xFF451A03),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ReportSubmittedBottomSheetContent(
    selectedThemeIndex: Int,
    onDismiss: () -> Unit
) {
    val currentTheme = roomThemes.getOrElse(selectedThemeIndex) { roomThemes[0] }
    val isDark = currentTheme.isDark
    val sheetBg = if (isDark) Color(0xFF0F172A) else Color.White
    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)
    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(sheetBg)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Soft3DSuccessBadge(modifier = Modifier.size(80.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Report Submitted",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Thank you for helping keep our community safe!\nYour report has been sent to our moderation review queue.",
                fontSize = 13.sp,
                color = subtitleColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, RoundedCornerShape(18.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "What happens next?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(12.dp))

                NextStepReportRow(
                    text = "Our moderation team reviews the audio and activity log",
                    textColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                    icon = { Soft3DSearchAuditIcon(modifier = Modifier.size(22.dp)) }
                )
                NextStepReportRow(
                    text = "If violations are detected, prompt action will be taken",
                    textColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                    icon = { Soft3DRulesActionIcon(modifier = Modifier.size(22.dp)) }
                )
                NextStepReportRow(
                    text = "Your reporter identity remains 100% confidential",
                    textColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                    icon = { Soft3DPrivacyLockIcon(modifier = Modifier.size(22.dp)) }
                )
                NextStepReportRow(
                    text = "You will receive an in-app notice regarding the decision",
                    textColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                    icon = { Soft3DBellNotificationIcon(modifier = Modifier.size(22.dp)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF59E0B)
            ),
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = "Got it",
                color = Color(0xFF451A03),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun NextStepReportRow(
    text: String,
    textColor: Color = Color(0xFF334155),
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = textColor,
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp
        )
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

