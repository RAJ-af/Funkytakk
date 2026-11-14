package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.util.RoomShareHelper
import com.example.UpdateVoiceRoomRequest
import com.example.SpeakerRequestInsert
import com.example.SupabaseRoomParticipant
import com.example.ModeratorActionRequest
import com.example.UpdateParticipantMicRequest
import com.example.ReportUserRequest
import com.example.ModerationManager
import com.example.MyModerationStatus
import com.example.ReportMessageSnapshot
import com.example.ReportSpeakerLogItem
import com.example.RoomEndSummaryResponse
import com.example.repository.EncryptedChatRepository
import com.example.ui.screens.chat.DirectChatScreen
import com.example.ui.screens.voiceroom.EntryAnimationData
import com.example.ui.screens.voiceroom.EntryAnimationManager
import com.example.ui.screens.voiceroom.EntryAnimationOverlay
import com.example.ui.screens.voiceroom.GiftAnimationData
import com.example.ui.screens.voiceroom.GiftAnimationManager
import com.example.ui.screens.voiceroom.GiftAnimationOverlay
import com.example.ui.screens.voiceroom.GiftReceiverUser
import com.example.ui.screens.voiceroom.RoomGiftBottomSheet
import com.example.ui.screens.voiceroom.parseEntryAnimationConfig
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.net.Uri
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.*
import com.example.ui.components.FunkyChatIcon
import com.example.ui.components.FunkyDoubleCheckIcon
import com.example.ui.components.FunkyLockIcon
import com.example.ui.components.FunkyPlayIcon
import com.example.ui.components.FunkySendIcon
import com.example.ui.components.FunkyVerticalMoreIcon
import com.example.ui.components.SoftBackIcon
import com.example.ui.components.SoftBellIcon
import com.example.ui.components.SoftCheckIcon
import com.example.ui.components.SoftCloseIcon
import com.example.ui.components.SoftExitIcon
import com.example.ui.components.SoftEyeIcon
import com.example.ui.components.SoftFaceIcon
import com.example.ui.components.SoftForwardArrowIcon
import com.example.ui.components.SoftHeartIcon
import com.example.ui.components.SoftHourglassIcon
import com.example.ui.components.SoftImageIcon
import com.example.ui.components.SoftMicIcon
import com.example.ui.components.SoftMoonIcon
import com.example.ui.components.SoftMusicNoteIcon
import com.example.ui.components.SoftPersonIcon
import com.example.ui.components.SoftPlusIcon
import com.example.ui.components.SoftSearchIcon
import com.example.ui.components.SoftSettingsGearIcon
import com.example.ui.components.SoftShareIcon
import com.example.ui.components.SoftSparkleIcon
import com.example.ui.components.SoftSunIcon
import com.example.ui.components.SoftWarningIcon
import com.example.ui.theme.LocalFunkyColors
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
import androidx.compose.ui.text.style.TextAlign
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
    val userId: String = "",
    val isHostAway: Boolean = false,
    val isSpeaking: Boolean = false
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
    val messageType: String? = "chat",
    val iconUrl: String? = null,
    val senderNameBold: String? = null,
    val recipientNameBold: String? = null
)

fun parseGiftDetails(
    rawMessage: String,
    payloadObj: org.json.JSONObject? = null,
    payloadMap: Map<*, *>? = null
): Triple<String?, String?, String?> {
    val iconUrl = payloadObj?.optString("icon_url")?.ifBlank { null }
        ?: payloadMap?.get("icon_url")?.toString()?.ifBlank { null }

    var sender = payloadObj?.optString("sender_name")?.ifBlank { null }
        ?: payloadMap?.get("sender_name")?.toString()?.ifBlank { null }

    var recipient = payloadObj?.optString("recipient_name")?.ifBlank { null }
        ?: payloadMap?.get("recipient_name")?.toString()?.ifBlank { null }

    if (sender == null && rawMessage.contains(" sent ", ignoreCase = true)) {
        sender = rawMessage.substringBefore(" sent ").trim().ifBlank { null }
    }
    if (recipient == null && rawMessage.contains(" to ", ignoreCase = true)) {
        recipient = rawMessage.substringAfterLast(" to ").trim().ifBlank { null }
    }

    return Triple(iconUrl, sender, recipient)
}

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
    onBack: () -> Unit,
    onRoomEnded: ((peopleJoinedCount: Int, durationMinutes: Int) -> Unit)? = null
) {
    val context = LocalContext.current

    val supabaseProfiles = if (viewModel != null) {
        viewModel.supabaseProfiles.collectAsState().value
    } else {
        emptyList()
    }

    val voiceProfilesState by com.example.realtime.VoiceRoomProfileManager.profilesFlow.collectAsState()

    val activeRoomId = viewModel?.activeRoomId?.collectAsState()?.value ?: ""
    val activeRoomTitle = viewModel?.activeRoomTitle?.collectAsState()?.value ?: "Voice Circle"

    val roomEnteredAt = remember { System.currentTimeMillis() }
    var isRoomEnded by remember { mutableStateOf(false) }
    var roomEndSummary by remember { mutableStateOf<RoomEndSummaryResponse?>(null) }
    var hasShownRoomEnded by remember { mutableStateOf(false) }
    var cachedRoomId by remember { mutableStateOf(activeRoomId) }
    var cachedRoomTitle by remember { mutableStateOf(activeRoomTitle) }

    LaunchedEffect(activeRoomId) {
        if (activeRoomId.isNotBlank()) {
            cachedRoomId = activeRoomId
            cachedRoomTitle = activeRoomTitle
        }
    }

    LaunchedEffect(activeRoomId, isRoomEnded) {
        if (activeRoomId.isBlank() && !isRoomEnded) {
            onBack()
        }
    }

    // Room Config States (edited by user)
    var roomTitle by remember { mutableStateOf("Let's chat and play games") }
    var roomTags by remember { mutableStateOf("Chill") } // default tag chip from mockup
    var roomDescription by remember { mutableStateOf("A friendly chat room to share good vibes and play music.") }
    var roomLanguage by remember { mutableStateOf("Hindi + English") } // default from mockup
    var roomCountryCode by remember { mutableStateOf("in") }
    var isPrivateRoomState by remember { mutableStateOf(false) }
    var joinCodeState by remember { mutableStateOf("") }

    var myModerationStatus by remember { mutableStateOf<MyModerationStatus?>(null) }
    var showHostLeaveDialog by remember { mutableStateOf(false) }

    val hasOverlayPermission = remember(context) {
        com.example.service.OverlayPermissionHelper.canDrawOverlays(context)
    }
    var showOverlayBanner by remember { mutableStateOf(!hasOverlayPermission) }

    val voiceSession by viewModel?.roomVoiceSession?.collectAsState() ?: remember { mutableStateOf(null) }
    val voiceConnectionState by (voiceSession?.connectionState?.collectAsState() ?: remember { mutableStateOf(com.example.voice.VoiceConnectionState.Idle) })
    val speakingUserIds by (voiceSession?.speakingUserIds?.collectAsState() ?: remember { mutableStateOf(emptySet()) })

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel?.setMicEnabled(true, context)
            if (com.example.service.OverlayPermissionHelper.canDrawOverlays(context)) {
                com.example.service.VoiceRoomOverlayManager.startServiceIfPermitted(context)
            }
        } else {
            Toast.makeText(context, "Microphone permission is required to speak on stage. You can continue listening.", Toast.LENGTH_LONG).show()
        }
    }

    var currentUserId by remember {
        mutableStateOf(SupabaseSessionManager.getUserId(context) ?: "")
    }
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
    val roomHostProfile = remember(supabaseProfiles, roomHostId) {
        supabaseProfiles.find { it.uid == roomHostId }
    }

    LaunchedEffect(participantsData, roomHostId) {
        val uids = (participantsData.map { it.user_id } + listOfNotNull(roomHostId)).filter { it.isNotBlank() }.distinct()
        if (uids.isNotEmpty()) {
            com.example.realtime.VoiceRoomProfileManager.loadParticipants(context, uids, viewModel)
        }
    }

    var hostAwaySecondsLeft by remember { mutableStateOf<Int?>(null) }
    var openDirectChatPartner by remember { mutableStateOf<PrivateChatItem?>(null) }

    if (isRoomEnded) {
        RoomEndedScreen(
            roomTitle = roomEndSummary?.title ?: cachedRoomTitle,
            hostName = roomEndSummary?.host_username ?: "Host",
            hostAvatarUrl = roomEndSummary?.host_avatar_url,
            hostUserId = roomEndSummary?.host_id ?: roomHostId,
            currentUserId = currentUserId,
            endedReason = roomEndSummary?.ended_reason ?: "host_ended",
            durationSeconds = roomEndSummary?.duration_seconds ?: 0,
            totalJoins = roomEndSummary?.total_joins ?: participantsData.size.coerceAtLeast(1),
            peakParticipants = roomEndSummary?.peak_participants ?: participantsData.size.coerceAtLeast(1),
            onCreateAnotherRoom = {
                onBack()
            },
            onClose = {
                onBack()
            },
            onBackToHome = {
                onBack()
            }
        )
        return
    }

    val pendingSpeakerRequests by viewModel?.pendingSpeakerRequests?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val mySpeakerRequestStatus by viewModel?.mySpeakerRequestStatus?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
    var isSpeakerRequestPending by remember { mutableStateOf(false) }
    var isSpeakerRequestSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(mySpeakerRequestStatus) {
        isSpeakerRequestPending = (mySpeakerRequestStatus == "pending")
    }

    val isSpeaker = remember(participantsData, currentUserId, roomHostId) {
        (currentUserId.isNotBlank() && currentUserId.equals(roomHostId, ignoreCase = true)) ||
        participantsData.any { it.user_id == currentUserId && (it.role == "speaker" || it.role == "host") }
    }

    val isCurrentUserHost = remember(currentUserId, roomHostId) {
        currentUserId.isNotBlank() && !roomHostId.isNullOrBlank() && currentUserId.equals(roomHostId, ignoreCase = true)
    }

    BackHandler {
        if (isCurrentUserHost) {
            showHostLeaveDialog = true
        } else {
            if (com.example.service.OverlayPermissionHelper.canDrawOverlays(context)) {
                com.example.service.VoiceRoomOverlayManager.startServiceIfPermitted(context)
            }
            onBack()
        }
    }
    val scope = rememberCoroutineScope()

    val triggerRoomEnded: (String, String) -> Unit = { reason, triggerDesc ->
        if (!isRoomEnded && !hasShownRoomEnded) {
            hasShownRoomEnded = true
            val effectiveRoomId = activeRoomId.ifBlank { cachedRoomId }
            val effectiveTitle = activeRoomTitle.ifBlank { cachedRoomTitle }
            Log.i("VoiceRoomExit", "Room exit: roomId=$effectiveRoomId, reason='$reason', trigger='$triggerDesc'")

            com.example.realtime.AgoraVoiceManager.leaveChannel()
            com.example.realtime.AgoraVoiceManager.muteLocalAudioStream(true)
            viewModel?.setMicEnabled(false)
            viewModel?.roomVoiceSession?.value?.leave()

            if (com.example.service.OverlayPermissionHelper.canDrawOverlays(context)) {
                com.example.service.VoiceRoomOverlayManager.stopService(context)
            }
            com.example.service.VoiceRoomOverlayManager.clear()

            scope.launch(Dispatchers.IO) {
                var summary: RoomEndSummaryResponse? = null
                if (effectiveRoomId.isNotBlank()) {
                    for (retry in 1..3) {
                        try {
                            val summaryRes = SupabaseClient.api.getRoomEndSummary(roomIdEq = "eq.$effectiveRoomId")
                            if (summaryRes.isSuccessful && !summaryRes.body().isNullOrEmpty()) {
                                summary = summaryRes.body()!!.first()
                                break
                            }
                        } catch (_: Exception) {}
                        delay(500)
                    }
                }
                val durationSec = ((System.currentTimeMillis() - roomEnteredAt) / 1000).toInt().coerceAtLeast(0)
                val pCount = participantsData.size.coerceAtLeast(1)
                withContext(Dispatchers.Main) {
                    roomEndSummary = summary ?: RoomEndSummaryResponse(
                        room_id = effectiveRoomId,
                        title = effectiveTitle,
                        ended_reason = reason,
                        duration_seconds = durationSec,
                        total_joins = pCount,
                        peak_participants = pCount
                    )
                    isRoomEnded = true
                }
            }
        }
    }
    val isMicOn by com.example.realtime.RoomMicController.micOn.collectAsState()
    val lastTokenRole by viewModel?.lastRoomTokenRole?.collectAsState(null) ?: remember { mutableStateOf(null) }
    val lastCanPublish by viewModel?.lastRoomTokenCanPublish?.collectAsState(false) ?: remember { mutableStateOf(false) }
    var pendingInviteState by remember { mutableStateOf<com.example.SupabaseSpeakerRequest?>(null) }
    var latestReqKind by remember { mutableStateOf<String?>(null) }
    var latestReqStatus by remember { mutableStateOf<String?>(null) }
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

    val isHostPresent = remember(participantsData, roomHostId) {
        participantsData.any { it.user_id == roomHostId && (it.role == "host" || it.role == "speaker") }
    }

    val myParticipant = participantsData.find { it.user_id == currentUserId }
    val myRole = myParticipant?.role ?: if (isCurrentUserHost) "speaker" else "listener"

    LaunchedEffect(activeRoomId, currentUserId) {
        if (activeRoomId.isNotBlank() && currentUserId.isNotBlank()) {
            val initialRole = if (currentUserId == roomHostId) "speaker" else "listener"
            viewModel?.startVoiceSession(
                context = context.applicationContext,
                roomId = activeRoomId,
                userId = currentUserId,
                userName = currentUserName,
                initialRole = initialRole
            )
            if (currentUserId == roomHostId && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(myRole) {
        voiceSession?.onRoleChanged(myRole)
        if (myRole == "speaker" || myRole == "host") {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val hostSeat = remember(participantsData, supabaseProfiles, voiceProfilesState, roomHostId, isHostPresent, currentUserProfile, currentUserId, speakingUserIds) {
        val hostProfile = supabaseProfiles.find { it.uid == roomHostId }
        val cachedProf = voiceProfilesState[roomHostId ?: ""] ?: com.example.realtime.VoiceRoomProfileManager.getCachedProfile(roomHostId ?: "")
        val snippet = com.example.realtime.VoiceRoomRealtimeManager.getUserSnippet(roomHostId ?: "")
        val name: String = (hostProfile?.displayName?.ifBlank { null }
            ?: hostProfile?.username?.ifBlank { null }
            ?: cachedProf?.name?.ifBlank { null }
            ?: snippet?.name?.ifBlank { null }
            ?: (if (currentUserId == roomHostId) (currentUserProfile?.displayName?.ifBlank { null } ?: currentUserProfile?.username?.ifBlank { null }) else null)
            ?: "Host")
        val avatar: String = (hostProfile?.avatar?.ifBlank { null }
            ?: cachedProf?.avatar?.ifBlank { null }
            ?: snippet?.avatar?.ifBlank { null }
            ?: (if (currentUserId == roomHostId) currentUserProfile?.avatar?.ifBlank { null } else null)
            ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400")
        val flagCode: String = hostProfile?.countryCode ?: cachedProf?.countryCode ?: snippet?.countryCode ?: "in"
        val pHost = participantsData.find { it.user_id == roomHostId }
        val hostUid = hostProfile?.uid ?: (roomHostId ?: "")

        ChillRoomSpeaker(
            name = name,
            imageUrl = avatar,
            flagCode = flagCode,
            isMuted = if (currentUserId == roomHostId) !isMicOn else (pHost?.let { !it.is_mic_enabled } ?: true),
            isCenter = true,
            gradientColors = listOf(Color(0xFFFFB800), Color(0xFFF59E0B), Color(0xFFFFB800)),
            isHost = true,
            userId = hostUid,
            isHostAway = !isHostPresent,
            isSpeaking = speakingUserIds.contains(hostUid)
        )
    }

    val allRoomSeats = remember(hostSeat, participantsData, supabaseProfiles, voiceProfilesState, roomHostId, isMicOn, speakingUserIds) {
        if (participantsData.isEmpty()) {
            listOf(hostSeat)
        } else {
            participantsData.map { p ->
                val pProfile = supabaseProfiles.find { it.uid == p.user_id }
                val cachedProf = voiceProfilesState[p.user_id] ?: com.example.realtime.VoiceRoomProfileManager.getCachedProfile(p.user_id)
                val snippet = com.example.realtime.VoiceRoomRealtimeManager.getUserSnippet(p.user_id)
                val isHost = (p.role == "host") || (p.user_id == roomHostId)
                val roleLabel = if (isHost) "Host" else if (p.role == "speaker") "Speaker" else "Learner"
                val name: String = (pProfile?.displayName?.ifBlank { null }
                    ?: pProfile?.username?.ifBlank { null }
                    ?: cachedProf?.name?.ifBlank { null }
                    ?: snippet?.name?.ifBlank { null }
                    ?: (if (p.user_id == currentUserId) (currentUserProfile?.displayName?.ifBlank { null } ?: currentUserProfile?.username?.ifBlank { null }) else null)
                    ?: roleLabel)
                val avatar: String = (pProfile?.avatar?.ifBlank { null }
                    ?: cachedProf?.avatar?.ifBlank { null }
                    ?: snippet?.avatar?.ifBlank { null }
                    ?: (if (p.user_id == currentUserId) currentUserProfile?.avatar?.ifBlank { null } else null)
                    ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400")
                val flagCode: String = pProfile?.countryCode ?: cachedProf?.countryCode ?: snippet?.countryCode ?: "in"
                ChillRoomSpeaker(
                    name = name,
                    imageUrl = avatar,
                    flagCode = flagCode,
                    isMuted = if (p.user_id == currentUserId) !isMicOn else !p.is_mic_enabled,
                    isCenter = isHost,
                    gradientColors = if (isHost) listOf(Color(0xFFFFB800), Color(0xFFF59E0B), Color(0xFFFFB800)) else listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
                    isHost = isHost,
                    userId = p.user_id,
                    isHostAway = if (isHost) !isHostPresent else false,
                    isSpeaking = speakingUserIds.contains(p.user_id)
                )
            }
        }
    }

    val speakerSeats = remember(participantsData, supabaseProfiles, voiceProfilesState, roomHostId, hostSeat, isMicOn, speakingUserIds) {
        participantsData.filter { p ->
            val isHost = (p.role == "host") || (p.user_id == roomHostId)
            !isHost && p.role == "speaker"
        }.sortedBy { it.joined_at ?: "" }.take(4).map { p ->
            val pProfile = supabaseProfiles.find { it.uid == p.user_id }
            val cachedProf = voiceProfilesState[p.user_id] ?: com.example.realtime.VoiceRoomProfileManager.getCachedProfile(p.user_id)
            val snippet = com.example.realtime.VoiceRoomRealtimeManager.getUserSnippet(p.user_id)
            val name: String = (pProfile?.displayName?.ifBlank { null }
                ?: pProfile?.username?.ifBlank { null }
                ?: cachedProf?.name?.ifBlank { null }
                ?: snippet?.name?.ifBlank { null }
                ?: (if (p.user_id == currentUserId) (currentUserProfile?.displayName?.ifBlank { null } ?: currentUserProfile?.username?.ifBlank { null }) else null)
                ?: "Speaker")
            val avatar: String = (pProfile?.avatar?.ifBlank { null }
                ?: cachedProf?.avatar?.ifBlank { null }
                ?: snippet?.avatar?.ifBlank { null }
                ?: (if (p.user_id == currentUserId) currentUserProfile?.avatar?.ifBlank { null } else null)
                ?: "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400")
            val flagCode: String = (pProfile?.countryCode?.ifBlank { null }
                ?: cachedProf?.countryCode
                ?: snippet?.countryCode
                ?: "in")
            ChillRoomSpeaker(
                name = name,
                imageUrl = avatar,
                flagCode = flagCode,
                isMuted = if (p.user_id == currentUserId) !isMicOn else !p.is_mic_enabled,
                isCenter = false,
                gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
                isHost = false,
                userId = p.user_id,
                isSpeaking = speakingUserIds.contains(p.user_id)
            )
        }
    }

    val listenerSeats = remember(participantsData, supabaseProfiles, voiceProfilesState, roomHostId) {
        participantsData.filter { p ->
            val isHost = (p.role == "host") || (p.user_id == roomHostId)
            !isHost && p.role != "speaker"
        }.map { p ->
            val pProfile = supabaseProfiles.find { it.uid == p.user_id }
            val cachedProf = voiceProfilesState[p.user_id] ?: com.example.realtime.VoiceRoomProfileManager.getCachedProfile(p.user_id)
            val snippet = com.example.realtime.VoiceRoomRealtimeManager.getUserSnippet(p.user_id)
            val name: String = (pProfile?.displayName?.ifBlank { null }
                ?: pProfile?.username?.ifBlank { null }
                ?: cachedProf?.name?.ifBlank { null }
                ?: snippet?.name?.ifBlank { null }
                ?: (if (p.user_id == currentUserId) (currentUserProfile?.displayName?.ifBlank { null } ?: currentUserProfile?.username?.ifBlank { null }) else null)
                ?: "Learner")
            val avatar: String = (pProfile?.avatar?.ifBlank { null }
                ?: cachedProf?.avatar?.ifBlank { null }
                ?: snippet?.avatar?.ifBlank { null }
                ?: (if (p.user_id == currentUserId) currentUserProfile?.avatar?.ifBlank { null } else null)
                ?: "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400")
            val flagCode: String = (pProfile?.countryCode?.ifBlank { null }
                ?: cachedProf?.countryCode
                ?: snippet?.countryCode
                ?: "in")
            Triple(name, avatar, flagCode)
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

    // 10-second polling loop to reconcile deleted participants (only when app is in foreground)
    val currentLifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(activeRoomId) {
        while (isActive) {
            kotlinx.coroutines.delay(10_000)
            val isForeground = currentLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
            if (isForeground && activeRoomId.isNotBlank()) {
                try {
                    val token = SupabaseSessionManager.getBearerToken(context)
                    val partsRes = SupabaseClient.api.getRoomParticipants(auth = token, roomIdEq = "eq.$activeRoomId")
                    if (partsRes.isSuccessful) {
                        val freshList = partsRes.body() ?: emptyList()
                        withContext(Dispatchers.Main) {
                            participantsData = freshList
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(activeRoomId) {
        renderedMessageCount = 6
        withContext(Dispatchers.IO) {
            try {
                val token = SupabaseSessionManager.getBearerToken(context)
                val roomRes = SupabaseClient.api.getVoiceRoomById(auth = token, idEq = "eq.$activeRoomId")
                var roomObj = if (roomRes.isSuccessful) roomRes.body()?.firstOrNull() else null
                if (roomObj == null || roomObj.is_live == false || roomObj.status == "ended") {
                    // Grace period + refreshed auth check: retry 3 times to avoid false auto-close on newly created private rooms
                    for (retry in 1..3) {
                        delay(1500)
                        try {
                            val freshToken = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: SupabaseSessionManager.getBearerToken(context)
                            val retryRes = SupabaseClient.api.getVoiceRoomById(auth = freshToken, idEq = "eq.$activeRoomId")
                            if (retryRes.isSuccessful) {
                                val candidate = retryRes.body()?.firstOrNull()
                                if (candidate != null && candidate.is_live == true && candidate.status != "ended") {
                                    roomObj = candidate
                                    break
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                if (roomObj != null && (roomObj.is_live == false || roomObj.status == "ended")) {
                    withContext(Dispatchers.Main) {
                        triggerRoomEnded("host_ended", "initial_fetch_room_ended")
                    }
                    return@withContext
                }
                if (roomObj == null) {
                    withContext(Dispatchers.Main) {
                        triggerRoomEnded("host_ended", "initial_fetch_room_not_found_after_retries")
                    }
                    return@withContext
                }
                roomHostId = roomObj.host_id ?: ""
                viewModel?.setActiveRoomHostId(roomHostId)
                roomTitle = roomObj.title ?: "Voice Circle"
                roomDescription = roomObj.description ?: ""
                roomLanguage = roomObj.language ?: ""
                roomCountryCode = roomObj.language_code.orEmpty().ifBlank { roomObj.country_code.orEmpty() }.ifBlank { LanguageData.getFlagCodeForLanguage(roomLanguage) }
                isPrivateRoomState = roomObj.is_private ?: false
                joinCodeState = roomObj.join_code ?: ""
                if (currentUserId.isNotBlank()) {
                    try {
                        val token = SupabaseSessionManager.getBearerToken(context)
                        val invRes = SupabaseClient.api.getSpeakerRequests(
                            auth = token,
                            roomIdEq = "eq.$activeRoomId",
                            userIdEq = "eq.$currentUserId",
                            kindEq = "eq.invite",
                            statusEq = "eq.pending"
                        )
                        if (invRes.isSuccessful) {
                            val invList = invRes.body() ?: emptyList()
                            if (invList.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    pendingInviteState = invList.first()
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                val partsRes = SupabaseClient.api.getRoomParticipants(auth = token, roomIdEq = "eq.$activeRoomId")
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
                }

                // Authoritative moderation status check from backend
                val modStatus = ModerationManager.fetchMyModerationStatus(context)
                withContext(Dispatchers.Main) {
                    myModerationStatus = modStatus
                    if (modStatus?.action_type == "suspend" && ModerationManager.isActionActive(modStatus)) {
                        com.example.realtime.RoomMicController.setMic(false, "MODERATION_SUSPEND", context)
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
                triggerUserEntryAnimation(context, currentUserId, myName)
                com.example.realtime.VoiceRoomRealtimeManager.broadcastParticipantUpdate(
                    roomId = activeRoomId,
                    userId = currentUserId,
                    action = "ping"
                )

                // Query existing messages ordered by created_at
                val msgsRes = SupabaseClient.api.getRoomMessages(auth = token, roomIdEq = "eq.$activeRoomId")
                if (msgsRes.isSuccessful) {
                    val list = msgsRes.body() ?: emptyList()
                    Log.d("VoiceRoomScreen", "[InitialFetch] Fetched ${list.size} room_messages for room $activeRoomId")
                    val mapped = list.map { msg ->
                        Log.d("VoiceRoomScreen", "[InitialFetch] Message: id=${msg.id}, type=${msg.message_type}, text='${msg.message}', userId=${msg.user_id}, createdAt=${msg.created_at}")
                        val rawType = msg.message_type
                        val isSystem = rawType?.equals("system", ignoreCase = true) == true
                        val isGift = rawType?.equals("gift", ignoreCase = true) == true
                        val p = supabaseProfiles.find { it.uid == msg.user_id }
                        val isMe = (msg.user_id == currentUserId)
                        val name = if (isMe) (p?.displayName ?: "You") else (p?.displayName ?: p?.username ?: "User")
                        val avatar = p?.avatar ?: (if (isMe) "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200" else "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400")
                        val badge = if (msg.user_id == roomHostId) "Host" else null

                        val (iconUrl, senderBold, recipientBold) = if (isGift) {
                            parseGiftDetails(msg.message ?: "", payloadMap = msg.payload)
                        } else Triple(null, null, null)

                        ChillRoomMessage(
                            senderName = name,
                            senderImageUrl = avatar,
                            badge = badge,
                            badgeColor = if (badge == "Host") ChillGoldIcon else ChillPurpleAccent,
                            text = msg.message ?: "",
                            id = msg.id,
                            userId = msg.user_id,
                            createdAt = msg.created_at,
                            messageType = if (isSystem) "system" else if (isGift) "gift" else (rawType ?: "chat"),
                            iconUrl = iconUrl,
                            senderNameBold = senderBold,
                            recipientNameBold = recipientBold
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
        val currentToken = SupabaseSessionManager.getAccessToken(context)
        VoiceRoomRealtimeManager.setAuthToken(currentToken)
        VoiceRoomRealtimeManager.start()
        VoiceRoomRealtimeManager.subscribeRoomChannel(activeRoomId)
        VoiceRoomRealtimeManager.subscribeRoomSpeakerRequests(activeRoomId)
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

                        val targetName = event.userName ?: event.record?.optString("user_name") ?: "Learner"

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
                        } else if (action in listOf("joined", "sync_presence", "update", "insert", "speaker_promoted")) {
                            if (targetUid.isNotBlank()) {
                                com.example.realtime.VoiceRoomProfileManager.recordProfile(targetUid, event.userName, event.userAvatar)
                                com.example.realtime.VoiceRoomProfileManager.loadParticipantProfile(context, targetUid, viewModel)
                                if (action == "joined") {
                                    triggerUserEntryAnimation(context, targetUid, targetName)
                                }
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
                                        val previousRole = participantsData.find { it.user_id == currentUserId }?.role ?: "listener"
                                        if (previousRole != "speaker" && effectiveRole == "speaker") {
                                            isSpeakerRequestPending = false
                                            com.example.realtime.RoomMicController.onRoleChanged("speaker", targetMic)
                                        }
                                    }
                                }
                            }
                        }

                        // Fetch fresh list from API in background on any participant update event
                        scope.launch(Dispatchers.IO) {
                            try {
                                val token = SupabaseSessionManager.getBearerToken(context)
                                val partsRes = SupabaseClient.api.getRoomParticipants(auth = token, roomIdEq = "eq.$activeRoomId")
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
                                    val token = SupabaseSessionManager.getBearerToken(context)
                                    val partsRes = SupabaseClient.api.getRoomParticipants(auth = token, roomIdEq = "eq.$activeRoomId")
                                    if (partsRes.isSuccessful) {
                                        val list = partsRes.body() ?: emptyList()
                                        withContext(Dispatchers.Main) {
                                            participantsData = list
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
                            val isGift = msgType.equals("gift", ignoreCase = true)

                            // Only process gift messages from postgres_changes to rely on Realtime Broadcast for user and system messages
                            if (isGift) {
                                val rawPayload = record.opt("payload")
                                val payloadObj = if (rawPayload is org.json.JSONObject) rawPayload else if (rawPayload is String) {
                                    try { org.json.JSONObject(rawPayload) } catch (_: Exception) { null }
                                } else null

                                val (iconUrl, senderBold, recipientBold) = parseGiftDetails(text, payloadObj = payloadObj)

                                Log.d("VoiceRoomScreen", "[Realtime] Processing gift message: id=$msgId, text='$text', userId=$userId")

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
                                                messageType = "gift",
                                                iconUrl = iconUrl,
                                                senderNameBold = senderBold,
                                                recipientNameBold = recipientBold
                                            )
                                        )

                                        val tier = payloadObj?.optInt("animation_tier", 0)?.takeIf { it > 0 }
                                            ?: payloadObj?.optInt("tier", 0)?.takeIf { it > 0 }
                                            ?: when {
                                                text.contains("Crown", ignoreCase = true) || text.contains("Diamond", ignoreCase = true) || text.contains("Cake", ignoreCase = true) || text.contains("Firework", ignoreCase = true) -> 3
                                                text.contains("Teddy", ignoreCase = true) || text.contains("Rose", ignoreCase = true) -> 2
                                                else -> 1
                                            }
                                        val qty = payloadObj?.optInt("quantity", 1)?.coerceAtLeast(1) ?: 1
                                        val sName = senderBold ?: name
                                        val rName = recipientBold ?: payloadObj?.optString("recipient_name") ?: "someone"
                                        val rIdParam = payloadObj?.optString("recipient_id") ?: ""

                                        val animData = GiftAnimationData(
                                            id = if (msgId.isNotBlank()) msgId else "gift_${System.currentTimeMillis()}_${(100..999).random()}",
                                            iconUrl = iconUrl ?: payloadObj?.optString("icon_url") ?: "",
                                            animationTier = tier,
                                            animationUrl = payloadObj?.optString("animation_url"),
                                            quantity = qty,
                                            senderName = sName,
                                            recipientName = rName,
                                            recipientId = rIdParam,
                                            giftName = payloadObj?.optString("gift_name")
                                        )
                                        GiftAnimationManager.enqueueAnimation(animData, context)
                                    }
                                }
                            }
                        }
                    }
                }
                is RealtimeTableEvent.ModerationActionEnforced -> {
                    val mySupabaseUid = currentUserId.ifBlank { SupabaseSessionManager.getUserId(context) ?: "" }
                    val myFirebaseUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val targetUid = event.targetUserId.trim()

                    val isTargetMe = targetUid.isNotBlank() && (
                        (mySupabaseUid.isNotBlank() && targetUid.equals(mySupabaseUid, ignoreCase = true)) ||
                        (myFirebaseUid.isNotBlank() && targetUid.equals(myFirebaseUid, ignoreCase = true))
                    )

                    if (isTargetMe) {
                        Log.w("VoiceRoomScreen", "Moderation action received via realtime for ME: ${event.actionType}")
                        val updated = ModerationManager.fetchMyModerationStatus(context)
                        withContext(Dispatchers.Main) {
                            myModerationStatus = updated
                            if (event.actionType == "suspend") {
                                com.example.realtime.RoomMicController.setMic(false, "MODERATION_SUSPEND_REALTIME", context)
                                Toast.makeText(context, "Account suspended: ${event.reason ?: "Violation of guidelines"}", Toast.LENGTH_LONG).show()
                            } else if (event.actionType == "warning") {
                                Toast.makeText(context, "Community Warning: Messaging disabled for 1 hour.", Toast.LENGTH_LONG).show()
                            } else if (event.actionType == "mute") {
                                Toast.makeText(context, "Chat muted by moderation.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Log.d("VoiceRoomScreen", "Ignoring realtime moderation broadcast for target '$targetUid': not for current user ('$mySupabaseUid' / '$myFirebaseUid')")
                    }
                }
                is RealtimeTableEvent.RoomLifecycleEvent -> {
                    if (!event.roomId.isNullOrBlank() && event.roomId == activeRoomId) {
                        if (event.action == "room_ended") {
                            withContext(Dispatchers.Main) {
                                triggerRoomEnded("host_ended", "realtime_room_lifecycle_ended")
                            }
                        }
                    }
                }
                is RealtimeTableEvent.VoiceRoomChanged -> {
                    val record = event.record
                    val rId = record?.optString("id")
                    if (!rId.isNullOrBlank() && rId == activeRoomId) {
                        val isLive = record?.optBoolean("is_live", true) ?: true
                        val status = record?.optString("status") ?: "active"
                        if (!isLive || status == "ended" || event.eventType.equals("DELETE", ignoreCase = true)) {
                            withContext(Dispatchers.Main) {
                                triggerRoomEnded("host_ended", "realtime_voice_room_changed")
                            }
                        }
                    }
                }
                is RealtimeTableEvent.SpeakerRequestChanged -> {
                    if (event.roomId == activeRoomId || event.roomId.isNullOrBlank()) {
                        val targetUid = event.userId ?: ""
                        val reqStatus = event.status ?: event.eventType
                        val isHost = (currentUserId.isNotBlank() && roomHostId != null && currentUserId.equals(roomHostId, ignoreCase = true))

                        withContext(Dispatchers.Main) {
                            latestReqKind = event.kind
                            latestReqStatus = reqStatus
                        }

                        if (isHost) {
                            viewModel?.checkPendingSpeakerRequestsForHost(activeRoomId, context)
                        }

                        if (targetUid.isNotBlank() && targetUid == currentUserId) {
                            val reqKind = event.kind
                            if (reqKind == "invite" && (reqStatus == "pending" || event.eventType == "INSERT")) {
                                withContext(Dispatchers.Main) {
                                    pendingInviteState = com.example.SupabaseSpeakerRequest(
                                        id = event.requestId ?: "inv_${System.currentTimeMillis()}",
                                        room_id = activeRoomId,
                                        user_id = currentUserId,
                                        status = "pending",
                                        kind = "invite",
                                        invited_by = event.invitedBy
                                    )
                                }
                            } else if (reqStatus == "accepted") {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "You are now a speaker!", Toast.LENGTH_SHORT).show()
                                    isSpeakerRequestPending = false
                                    com.example.realtime.RoomMicController.onRoleChanged("speaker", true)
                                    viewModel?.fetchRoomToken(activeRoomId, context)
                                }
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val token = SupabaseSessionManager.getBearerToken(context)
                                        val partsRes = SupabaseClient.api.getRoomParticipants(auth = token, roomIdEq = "eq.$activeRoomId")
                                        if (partsRes.isSuccessful) {
                                            val list = partsRes.body() ?: emptyList()
                                            withContext(Dispatchers.Main) {
                                                participantsData = list
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            } else if (reqStatus == "declined") {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Host declined your request.", Toast.LENGTH_SHORT).show()
                                    isSpeakerRequestPending = false
                                }
                                viewModel?.checkMySpeakerRequest(activeRoomId, context)
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
            val uid = SupabaseSessionManager.ensureActiveSession(context) ?: SupabaseSessionManager.getUserId(context) ?: currentUserId
            if (uid.isNotBlank()) currentUserId = uid
            val isHost = (uid.isNotBlank() && roomHostId != null && uid.equals(roomHostId, ignoreCase = true))
            Log.d("HostSpeakerRequests", "showPeopleSheet opened: isHost=$isHost, uid=$uid, hostId=$roomHostId, roomId=$activeRoomId")
            if (isHost) {
                viewModel?.checkPendingSpeakerRequestsForHost(activeRoomId, context)
            }
            withContext(Dispatchers.IO) {
                try {
                    val token = SupabaseSessionManager.getBearerToken(context)
                    val partsRes = SupabaseClient.api.getRoomParticipants(auth = token, roomIdEq = "eq.$activeRoomId")
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

    LaunchedEffect(activeRoomId, roomHostId, currentUserId) {
        while (isActive && activeRoomId.isNotBlank()) {
            delay(4000)
            try {
                // 1. Fetch room status & enforce server-authoritative 3-minute grace period (authenticated for private rooms)
                val pollToken = SupabaseSessionManager.getBearerToken(context)
                val roomsRes = SupabaseClient.api.getVoiceRoomById(auth = pollToken, idEq = "eq.$activeRoomId")
                if (roomsRes.isSuccessful) {
                    var roomObj = roomsRes.body()?.firstOrNull()
                    if (roomObj == null) {
                        // Retry with fresh authenticated bearer token before treating missing row as ended
                        val maxRetries = if ((System.currentTimeMillis() - roomEnteredAt) < 10_000L) 3 else 1
                        for (retry in 1..maxRetries) {
                            delay(if (maxRetries > 1) 2000 else 800)
                            try {
                                val freshToken = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: pollToken
                                val retryRes = SupabaseClient.api.getVoiceRoomById(auth = freshToken, idEq = "eq.$activeRoomId")
                                if (retryRes.isSuccessful && !retryRes.body().isNullOrEmpty()) {
                                    roomObj = retryRes.body()!!.first()
                                    break
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    if (roomObj == null || roomObj.is_live == false || roomObj.status == "ended") {
                        withContext(Dispatchers.Main) {
                            triggerRoomEnded("host_ended", "polling_room_status_ended")
                        }
                        break
                    } else {
                        val hostLeftStr = roomObj.host_left_at
                        if (!hostLeftStr.isNullOrBlank()) {
                            val leftEpoch = try {
                                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                                }.parse(hostLeftStr)?.time ?: 0L
                            } catch (_: Exception) { 0L }

                            if (leftEpoch > 0L) {
                                val elapsedSec = ((System.currentTimeMillis() - leftEpoch) / 1000).toInt()
                                val remaining = 180 - elapsedSec
                                if (remaining > 0) {
                                    withContext(Dispatchers.Main) {
                                        hostAwaySecondsLeft = remaining
                                    }
                                } else {
                                    // 3-minute grace period expired without host return
                                    val token = SupabaseSessionManager.getBearerToken(context)
                                    try {
                                        SupabaseClient.api.checkAndCleanupEndedRooms(auth = token)
                                    } catch (_: Exception) {}
                                    withContext(Dispatchers.Main) {
                                        triggerRoomEnded("host_timeout", "polling_host_timeout_expired")
                                    }
                                    break
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                hostAwaySecondsLeft = null
                            }
                        }
                    }
                }

                // 2. Fetch room participants
                val partsRes = SupabaseClient.api.getRoomParticipants(auth = pollToken, roomIdEq = "eq.$activeRoomId")
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
                val uid = SupabaseSessionManager.getUserId(context) ?: currentUserId
                val isHost = (uid.isNotBlank() && !roomHostId.isNullOrBlank() && uid.equals(roomHostId, ignoreCase = true))
                if (isHost) {
                    viewModel?.checkPendingSpeakerRequestsForHost(activeRoomId, context)
                } else {
                    viewModel?.checkMySpeakerRequest(activeRoomId, context)
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(activeRoomId, roomHostId, currentUserId) {
        if (activeRoomId.isNotBlank()) {
            val uid = SupabaseSessionManager.getUserId(context) ?: currentUserId
            if (uid.isNotBlank() && currentUserId.isBlank()) {
                currentUserId = uid
            }
            val isHost = (uid.isNotBlank() && roomHostId != null && uid.equals(roomHostId, ignoreCase = true))
            Log.d("HostSpeakerRequests", "LaunchedEffect room sync: isHost=$isHost, uid=$uid, hostId=$roomHostId, roomId=$activeRoomId")
            if (isHost) {
                viewModel?.checkPendingSpeakerRequestsForHost(activeRoomId, context)
            } else {
                viewModel?.checkMySpeakerRequest(activeRoomId, context)
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activeRoomId, roomHostId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && activeRoomId.isNotBlank()) {
                val uid = SupabaseSessionManager.getUserId(context) ?: currentUserId
                val isHost = (uid.isNotBlank() && roomHostId != null && uid.equals(roomHostId, ignoreCase = true))
                Log.d("HostSpeakerRequests", "ON_RESUME event: isHost=$isHost, uid=$uid, hostId=$roomHostId, roomId=$activeRoomId")
                viewModel?.refreshActiveRoomSpeakers(activeRoomId, context)
                if (isHost) {
                    viewModel?.hostRejoinVoiceRoom(activeRoomId, context, trigger = "on_resume_screen_reopen")
                    viewModel?.checkPendingSpeakerRequestsForHost(activeRoomId, context)
                } else {
                    viewModel?.checkMySpeakerRequest(activeRoomId, context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val handleRequestSpeakerSeat: () -> Unit = {
        val myPart = participantsData.find { it.user_id == currentUserId }
        val isAlreadySpeaker = (currentUserId == roomHostId || myPart?.role == "speaker" || speakerSeats.any { it.userId == currentUserId })
        if (isAlreadySpeaker) {
            Toast.makeText(context, "You are already a speaker in this room.", Toast.LENGTH_SHORT).show()
        } else if (isSpeakerRequestPending || mySpeakerRequestStatus == "pending") {
            Toast.makeText(context, "Your request to speak is pending host approval", Toast.LENGTH_SHORT).show()
        } else {
            isSpeakerRequestSubmitting = true
            if (viewModel != null) {
                viewModel.requestSpeakerSeat(activeRoomId, context) { success, message ->
                    isSpeakerRequestSubmitting = false
                    if (success) {
                        isSpeakerRequestPending = true
                    }
                    message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                }
            } else {
                scope.launch(Dispatchers.IO) {
                    try {
                        val token = SupabaseSessionManager.getBearerToken(context)
                        val uid = SupabaseSessionManager.getUserId(context) ?: currentUserId
                        val res = SupabaseClient.api.createSpeakerRequest(
                            auth = token,
                            body = SpeakerRequestInsert(
                                room_id = activeRoomId,
                                user_id = uid,
                                status = "pending"
                            )
                        )
                        withContext(Dispatchers.Main) {
                            isSpeakerRequestSubmitting = false
                            if (res.isSuccessful || res.code() == 409) {
                                isSpeakerRequestPending = true
                                val myName = currentUserProfile?.displayName ?: currentUserProfile?.username ?: "Learner"
                                val myAvatar = currentUserProfile?.avatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"
                                com.example.realtime.VoiceRoomRealtimeManager.broadcastSpeakerRequest(
                                    roomId = activeRoomId,
                                    requestId = "req_${System.currentTimeMillis()}",
                                    userId = uid,
                                    action = "requested",
                                    status = "pending",
                                    userName = myName,
                                    userAvatar = myAvatar
                                )
                                Toast.makeText(context, "Request to speak sent to host!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Could not submit speaker request.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
                            isSpeakerRequestSubmitting = false
                            Toast.makeText(context, "Error sending request to speak", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showEditRoomSheet by remember { mutableStateOf(false) }
    
    // Gift Sheet State Variables
    var showGiftSheet by remember { mutableStateOf(false) }
    var giftTargetUserId by remember { mutableStateOf<String?>(null) }
    var selectedGift by remember { mutableStateOf(giftItemsList[0]) }
    var userCoins by remember { mutableStateOf(1250) }
    var selectedGiftTargetUser by remember { mutableStateOf("Riya") }
    var selectedGiftMultiplier by remember { mutableStateOf(1) }
    var isChangingGift by remember { mutableStateOf(false) }

    // Report Bottom Sheet States
    var selectedReportReasonIndex by remember { mutableStateOf(0) }
    var reportScreenshotUrl by remember { mutableStateOf<String?>(null) }
    var reportAudioClipUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingScreenshot by remember { mutableStateOf(false) }
    var isUploadingClip by remember { mutableStateOf(false) }
    var isSubmittingReport by remember { mutableStateOf(false) }
    var reportAdditionalDetails by remember { mutableStateOf("") }
    var showReportSubmittedSheet by remember { mutableStateOf(false) }

    val reportScreenshotLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingScreenshot = true
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null && bytes.isNotEmpty()) {
                        val filename = "report_shot_${System.currentTimeMillis()}.jpg"
                        val reqBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, bytes.size)
                        
                        // Ensure session is fresh and authenticated
                        SupabaseSessionManager.ensureActiveSession(context)
                        val bearerAuth = SupabaseSessionManager.getBearerToken(context)
                        var usedBucket = "avatars"
                        var isUploadSuccess = false

                        // 1. Try raw bytes upload to avatars bucket (guaranteed public & accessible)
                        val rawRes = SupabaseClient.api.uploadStorageRawBytes(
                            auth = bearerAuth,
                            bucket = "avatars",
                            path = filename,
                            body = reqBody
                        )
                        if (rawRes.isSuccessful) {
                            isUploadSuccess = true
                            usedBucket = "avatars"
                        } else {
                            // 2. Try multipart upload to avatars bucket
                            val multiPart = okhttp3.MultipartBody.Part.createFormData("file", filename, reqBody)
                            val multiRes = SupabaseClient.api.uploadStorageFile(
                                auth = bearerAuth,
                                bucket = "avatars",
                                filename = filename,
                                file = multiPart
                            )
                            if (multiRes.isSuccessful) {
                                isUploadSuccess = true
                                usedBucket = "avatars"
                            } else {
                                // 3. Try reports bucket with raw bytes
                                val repRes = SupabaseClient.api.uploadStorageRawBytes(
                                    auth = bearerAuth,
                                    bucket = "reports",
                                    path = filename,
                                    body = reqBody
                                )
                                if (repRes.isSuccessful) {
                                    isUploadSuccess = true
                                    usedBucket = "reports"
                                }
                            }
                        }

                        if (isUploadSuccess) {
                            val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
                            val url = if (baseUrl.startsWith("http")) baseUrl else "https://$baseUrl"
                            val directUrl = "$url/storage/v1/object/public/$usedBucket/$filename"
                            // If uploaded to reports (private), resolve to a signed URL immediately
                            val finalUrl = if (usedBucket == "reports") {
                                com.example.SupabaseMediaResolver.resolveMediaUrl(directUrl) ?: directUrl
                            } else {
                                directUrl
                            }
                            reportScreenshotUrl = finalUrl
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Screenshot attached successfully", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Failed to upload screenshot. Please check connection.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VoiceRoomScreen", "Screenshot upload failed", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Upload error: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    isUploadingScreenshot = false
                }
            }
        }
    }

    val reportClipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingClip = true
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null && bytes.isNotEmpty()) {
                        val filename = "report_clip_${System.currentTimeMillis()}.mp3"
                        val reqBody = bytes.toRequestBody("audio/mpeg".toMediaTypeOrNull(), 0, bytes.size)
                        
                        // Ensure session is fresh and authenticated
                        SupabaseSessionManager.ensureActiveSession(context)
                        val bearerAuth = SupabaseSessionManager.getBearerToken(context)
                        var usedBucket = "avatars"
                        var isUploadSuccess = false

                        // 1. Try raw bytes upload to avatars bucket (reliable public storage)
                        val rawRes = SupabaseClient.api.uploadStorageRawBytes(
                            auth = bearerAuth,
                            bucket = "avatars",
                            path = filename,
                            body = reqBody
                        )
                        if (rawRes.isSuccessful) {
                            isUploadSuccess = true
                            usedBucket = "avatars"
                        } else {
                            // 2. Try multipart upload to avatars
                            val multiPart = okhttp3.MultipartBody.Part.createFormData("file", filename, reqBody)
                            val multiRes = SupabaseClient.api.uploadStorageFile(
                                auth = bearerAuth,
                                bucket = "avatars",
                                filename = filename,
                                file = multiPart
                            )
                            if (multiRes.isSuccessful) {
                                isUploadSuccess = true
                                usedBucket = "avatars"
                            } else {
                                // 3. Fallback to reports bucket
                                val repRes = SupabaseClient.api.uploadStorageRawBytes(
                                    auth = bearerAuth,
                                    bucket = "reports",
                                    path = filename,
                                    body = reqBody
                                )
                                if (repRes.isSuccessful) {
                                    isUploadSuccess = true
                                    usedBucket = "reports"
                                }
                            }
                        }

                        if (isUploadSuccess) {
                            val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
                            val url = if (baseUrl.startsWith("http")) baseUrl else "https://$baseUrl"
                            val directUrl = "$url/storage/v1/object/public/$usedBucket/$filename"
                            // If uploaded to reports (private), resolve to a signed URL immediately
                            val finalUrl = if (usedBucket == "reports") {
                                com.example.SupabaseMediaResolver.resolveMediaUrl(directUrl) ?: directUrl
                            } else {
                                directUrl
                            }
                            reportAudioClipUrl = finalUrl
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Clip attached successfully", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Failed to upload clip. Please check connection.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VoiceRoomScreen", "Clip upload failed", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Upload error: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    isUploadingClip = false
                }
            }
        }
    }

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val dbConversations by EncryptedChatRepository.conversations.collectAsState()
    val privateChats = remember(dbConversations) {
        dbConversations.map { conv ->
            val lastMsgPreview = when {
                conv.lastMessageText.startsWith("📷") || conv.lastMessageText.contains("Photo", ignoreCase = true) -> "Photo"
                conv.lastMessageText.startsWith("🎤") || conv.lastMessageText.contains("Voice message", ignoreCase = true) -> "Voice message"
                conv.lastMessageText.isBlank() -> "Encrypted conversation"
                else -> conv.lastMessageText
            }
            val formattedTime = if (conv.lastMessageTime > 0) {
                val diff = System.currentTimeMillis() - conv.lastMessageTime
                if (diff < 60_000) "Just now"
                else if (diff < 3600_000) "${diff / 60_000}m"
                else if (diff < 86400_000) "${diff / 3600_000}h"
                else "Yesterday"
            } else "Now"

            PrivateChatItem(
                id = conv.otherUserId,
                name = conv.otherUserName.ifBlank { "User" },
                imageUrl = conv.otherUserAvatar.orEmpty().ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400" },
                flagCode = conv.otherUserFlag.orEmpty().ifBlank { "in" },
                lastMessage = lastMsgPreview,
                time = formattedTime,
                unreadCount = conv.unreadCount,
                isOnline = true,
                isTyping = false
            )
        }
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
            if (ModerationManager.isMessagingBlocked(myModerationStatus)) {
                val action = myModerationStatus?.action_type ?: "restricted"
                Toast.makeText(context, "Cannot send message: Your account is under $action moderation.", Toast.LENGTH_LONG).show()
            } else {
                val msgToSend = text.trim()
                chatInputValue = ""
                val localMsgId = "msg_${System.currentTimeMillis()}_${(100..999).random()}"
                val currentUserProfile = supabaseProfiles.find { it.uid == currentUserId }
                val senderName = currentUserProfile?.displayName ?: currentUserProfile?.username ?: "You"
                val senderAvatar = currentUserProfile?.avatar ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200"
                val badge = if (currentUserId == roomHostId) "Host" else null

                scope.launch(Dispatchers.IO) {
                    try {
                        SupabaseSessionManager.ensureActiveSession(context)
                        val token = SupabaseSessionManager.getBearerToken(context)
                        val resolvedUuid = SupabaseSessionManager.getUserId(context)

                        if (resolvedUuid.isNullOrBlank() || !SupabaseSessionManager.isValidUuid(resolvedUuid)) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Authentication required to send messages", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }

                        val request = SendRoomMessageRequest(
                            room_id = activeRoomId,
                            user_id = resolvedUuid,
                            message = msgToSend
                        )
                        val res = SupabaseClient.api.sendRoomMessage(
                            auth = token,
                            request = request
                        )

                        if (res.isSuccessful) {
                            val inserted = res.body()?.firstOrNull()
                            val msgId = inserted?.id ?: localMsgId
                            val createdAt = inserted?.created_at

                            withContext(Dispatchers.Main) {
                                messages.add(
                                    ChillRoomMessage(
                                        senderName = "You",
                                        senderImageUrl = senderAvatar,
                                        badge = badge,
                                        badgeColor = if (badge == "Host") ChillGoldIcon else ChillPurpleAccent,
                                        text = msgToSend,
                                        id = msgId,
                                        userId = resolvedUuid,
                                        createdAt = createdAt
                                    )
                                )
                            }

                            // Broadcast via Supabase Realtime only AFTER successful backend DB persistence
                            VoiceRoomRealtimeManager.broadcastRoomMessage(
                                roomId = activeRoomId,
                                message = msgToSend,
                                userId = resolvedUuid,
                                senderName = senderName,
                                senderAvatar = senderAvatar,
                                messageId = msgId
                            )
                        } else {
                            val code = res.code()
                            val errBody = res.errorBody()?.string() ?: ""
                            Log.e("VoiceRoomScreen", "sendRoomMessage failed: code=$code, err=$errBody")
                            withContext(Dispatchers.Main) {
                                if (code == 403) {
                                    Toast.makeText(context, "Message blocked by moderation policy (403)", Toast.LENGTH_LONG).show()
                                    scope.launch(Dispatchers.IO) {
                                        val updatedStatus = ModerationManager.fetchMyModerationStatus(context)
                                        withContext(Dispatchers.Main) {
                                            myModerationStatus = updatedStatus
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Failed to send message ($code)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("VoiceRoomScreen", "Exception in sendRoomMessage", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Network error: message not sent", Toast.LENGTH_SHORT).show()
                        }
                    }
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
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                if (isCurrentUserHost) {
                                    showHostLeaveDialog = true
                                } else {
                                    if (com.example.service.OverlayPermissionHelper.canDrawOverlays(context)) {
                                        com.example.service.VoiceRoomOverlayManager.startServiceIfPermitted(context)
                                    }
                                    onBack()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        SoftBackIcon(
                            size = 20.dp,
                            color = currentTheme.primaryTextColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val displayShortCode = remember(activeRoomId, joinCodeState) {
                        RoomShareHelper.getDisplayRoomCode(activeRoomId, joinCodeState)
                    }

                    Column(modifier = Modifier.widthIn(min = 60.dp, max = 150.dp)) {
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
                            text = "ID: $displayShortCode",
                            fontSize = 11.sp,
                            color = currentTheme.secondaryTextColor,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
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
                                showInviteFriendSheet = true
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
                        FunkyVerticalMoreIcon(
                            color = currentTheme.primaryTextColor,
                            modifier = Modifier.size(22.dp)
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
                                            text = if (isCurrentUserHost) "Leave or end circle" else "Exit current circle",
                                            color = Color(0xFFF87171).copy(alpha = 0.85f),
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showThreeDotMenu = false
                                    if (isCurrentUserHost) {
                                        showHostLeaveDialog = true
                                    } else {
                                        viewModel?.leaveRoom(context, trigger = "user_leave_button")
                                        onBack()
                                    }
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
                            FunkyChatIcon(
                                color = Color(0xFFF59E0B),
                                isFilled = true,
                                modifier = Modifier.size(18.dp)
                            )
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
                            SoftCloseIcon(
                                size = 16.dp,
                                color = currentTheme.secondaryTextColor
                            )
                        }
                    }
                }
            }

            if (hostAwaySecondsLeft != null && hostAwaySecondsLeft!! > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SoftHourglassIcon(size = 18.dp, color = Color(0xFFB45309))
                        Spacer(modifier = Modifier.width(10.dp))
                        val mins = hostAwaySecondsLeft!! / 60
                        val secs = hostAwaySecondsLeft!! % 60
                        Text(
                            text = "Host is away - room closes in ${String.format("%d:%02d", mins, secs)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
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
                            FunkyLockIcon(
                                color = if (isRoomDarkMode) Color(0xFFFBBF24) else Color(0xFFB45309),
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
                            SoftShareIcon(
                                size = 16.dp,
                                color = if (isRoomDarkMode) Color.White else Color.Black
                            )
                        }
                    }
                }
            }

            // Voice Connection State Banner
            when (val vState = voiceConnectionState) {
                is com.example.voice.VoiceConnectionState.Connecting -> {
                    VoiceConnectionBanner(
                        text = "Connecting voice...",
                        backgroundColor = Color(0xFF1E293B).copy(alpha = 0.9f),
                        textColor = Color(0xFF94A3B8),
                        isLoading = true
                    )
                }
                is com.example.voice.VoiceConnectionState.Reconnecting -> {
                    VoiceConnectionBanner(
                        text = "Reconnecting voice...",
                        backgroundColor = Color(0xFF78350F).copy(alpha = 0.9f),
                        textColor = Color(0xFFFDE68A),
                        isLoading = true
                    )
                }
                is com.example.voice.VoiceConnectionState.Failed -> {
                    VoiceConnectionBanner(
                        text = vState.reason.ifBlank { "Voice connection failed, tap to retry" },
                        backgroundColor = Color(0xFF7F1D1D).copy(alpha = 0.95f),
                        textColor = Color(0xFFFECACA),
                        showRetry = true,
                        onRetry = { voiceSession?.retry() }
                    )
                }
                else -> {}
            }

            // Host In-Room Pending Requests Banner
            if (currentUserId == roomHostId && pendingSpeakerRequests.isNotEmpty()) {
                val firstReq = pendingSpeakerRequests.first()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRoomDarkMode) Color(0xFF261E10) else Color(0xFFFFFBEB)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = ChillGoldIcon.copy(alpha = 0.8f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AsyncImage(
                                    model = firstReq.requesterAvatar,
                                    contentDescription = firstReq.requesterName,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(ChillGoldIcon, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CustomRaisedHandIcon(
                                        tint = Color.Black,
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (pendingSpeakerRequests.size > 1) {
                                        "${firstReq.requesterName} +${pendingSpeakerRequests.size - 1} more"
                                    } else {
                                        firstReq.requesterName
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRoomDarkMode) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Requested to speak",
                                    fontSize = 11.sp,
                                    color = if (isRoomDarkMode) Color.White.copy(alpha = 0.6f) else Color(0xFF92400E)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decline button
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isRoomDarkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFF1F5F9),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel?.respondToSpeakerRequest(firstReq.id, false, context)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Decline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isRoomDarkMode) Color.White.copy(alpha = 0.75f) else Color(0xFF475569)
                                )
                            }

                            // Accept button
                            Box(
                                modifier = Modifier
                                    .background(
                                        ChillGoldIcon,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel?.respondToSpeakerRequest(firstReq.id, true, context)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Accept",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }

                            if (pendingSpeakerRequests.size > 1) {
                                Text(
                                    text = "View all",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChillGoldIcon,
                                    modifier = Modifier
                                        .clickable { showPeopleSheet = true }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. SPEAKERS VISUAL SYSTEM
            val currentParticipant = participantsData.find { it.user_id == currentUserId }
            val isCurrentUserSpeakerOrHost = (currentUserId == roomHostId || currentParticipant?.role == "speaker" || speakerSeats.any { it.userId == currentUserId })
            val isHostOrModerator = (currentUserId == roomHostId || currentParticipant?.role == "host" || currentParticipant?.role == "moderator")
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
                },
                isSpeakerRequestPending = isSpeakerRequestPending || mySpeakerRequestStatus == "pending",
                isCurrentUserSpeakerOrHost = isCurrentUserSpeakerOrHost,
                isCurrentUserHostOrModerator = isHostOrModerator,
                onHostInviteSeatClick = { showPeopleSheet = true },
                onRequestSpeakerSeat = handleRequestSpeakerSeat
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

            // Moderation Banners above chat
            if (myModerationStatus?.action_type == "warning" && ModerationManager.isActionActive(myModerationStatus)) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SoftWarningIcon(size = 16.dp, color = Color(0xFFD97706))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Community Warning Active (1h): Messaging disabled. ${myModerationStatus?.reason?.let { "($it)" } ?: ""}",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (myModerationStatus?.action_type == "mute" && ModerationManager.isActionActive(myModerationStatus)) {
                Surface(
                    color = Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FunkyLockIcon(color = Color(0xFF4B5563), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Moderation Mute: Outgoing messages are disabled.",
                            fontSize = 11.sp,
                            color = Color(0xFF374151),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 4. ACTION BAR / SAY SOMETHING
            val isChatBlocked = ModerationManager.isMessagingBlocked(myModerationStatus)
            val chatPlaceholder = when {
                myModerationStatus?.action_type == "warning" && isChatBlocked -> "Messaging disabled (Warning active)"
                myModerationStatus?.action_type == "mute" && isChatBlocked -> "Muted by moderation"
                myModerationStatus?.action_type == "suspend" && isChatBlocked -> "Account suspended"
                else -> "Say something..."
            }

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
                        enabled = !isChatBlocked,
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = if (isChatBlocked) Color.Gray else currentTheme.primaryTextColor
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
                                        text = chatPlaceholder,
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
                            FunkySendIcon(
                                color = Color.White,
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
                        .clickable {
                            giftTargetUserId = null
                            showGiftSheet = true
                        },
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
                    .navigationBarsPadding()
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
                val isHost = (currentUserId == roomHostId)
                val peopleBadge = if (isHost && pendingSpeakerRequests.isNotEmpty()) {
                    "+${pendingSpeakerRequests.size}"
                } else {
                    "$realPeopleCount"
                }
                VoiceBottomBarItem(
                    icon = { CustomPeopleGroupIcon(tint = if (isHost && pendingSpeakerRequests.isNotEmpty()) ChillGoldIcon else inactiveIconTint, modifier = Modifier.size(26.dp)) },
                    label = "People",
                    tint = if (isHost && pendingSpeakerRequests.isNotEmpty()) ChillGoldIcon else inactiveIconTint,
                    badgeText = peopleBadge,
                    onClick = { showPeopleSheet = true }
                )

                val myParticipant = participantsData.find { it.user_id == currentUserId }
                val myRole = myParticipant?.role ?: if (isHost) "speaker" else "listener"
                val isListener = !isHost && (myRole == "listener")

                if (isListener) {
                    val isPending = (isSpeakerRequestPending || mySpeakerRequestStatus == "pending")
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
                                    if (isPending) Color(0xFF3F3F46) else ChillGoldIcon
                                )
                                .clickable(enabled = !isSpeakerRequestSubmitting) {
                                    handleRequestSpeakerSeat()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPending) {
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
                            text = if (isPending) "Requested" else "Request to Speak",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPending) Color(0xFFA1A1AA) else ChillGoldIcon
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
                                        com.example.realtime.RoomMicController.toggleMic(context)
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

                VoiceBottomBarItem(
                    icon = { CustomGiftBoxIcon(tint = Color(0xFFEC4899), modifier = Modifier.size(24.dp)) },
                    label = "Gift",
                    tint = Color(0xFFEC4899),
                    onClick = {
                        giftTargetUserId = null
                        showGiftSheet = true
                    }
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
            onDismissRequest = {
                showChatSheet = false
                openDirectChatPartner = null
            },
            sheetState = sheetState,
            containerColor = currentTheme.startColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = if (currentTheme.isDark) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f)
                )
            }
        ) {
            if (openDirectChatPartner != null) {
                OneToOneChatBottomSheetContent(
                    selectedThemeIndex = selectedThemeIndex,
                    chatUser = openDirectChatPartner!!,
                    messagesList = emptyList(),
                    onSendMessage = { msg ->
                        scope.launch {
                            try {
                                com.example.repository.EncryptedChatRepository.sendMessage(
                                    context = context,
                                    recipientUserId = openDirectChatPartner!!.id,
                                    recipientName = openDirectChatPartner!!.name,
                                    recipientAvatar = openDirectChatPartner!!.imageUrl,
                                    text = msg
                                )
                            } catch (e: Exception) {
                                Log.e("VoiceRoomScreen", "Failed to send chat: ${e.message}")
                            }
                        }
                    },
                    onBack = { openDirectChatPartner = null }
                )
            } else {
                ChatBottomSheetContent(
                    privateChats = privateChats,
                    selectedThemeIndex = selectedThemeIndex,
                    onChatClick = { chatUser ->
                        openDirectChatPartner = chatUser
                    }
                )
            }
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
                pendingSpeakerRequests = pendingSpeakerRequests,
                onRespondToSpeakerRequest = { reqId, accept ->
                    viewModel?.respondToSpeakerRequest(reqId, accept, context)
                },
                onInviteToSpeakerSeat = { targetUserId ->
                    viewModel?.inviteToSpeakerSeat(activeRoomId, targetUserId, context) { ok, msg ->
                        Toast.makeText(context, msg ?: (if (ok) "Invitation sent!" else "Failed to send invitation"), Toast.LENGTH_SHORT).show()
                    }
                },
                onDemoteSpeaker = { targetUserId ->
                    viewModel?.demoteSpeaker(activeRoomId, targetUserId, context) { ok, msg ->
                        Toast.makeText(context, msg ?: (if (ok) "Speaker moved to listeners." else "Failed to demote speaker"), Toast.LENGTH_SHORT).show()
                    }
                },
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
        val displayRoomName = roomTitle.ifBlank { activeRoomTitle.ifBlank { "FunkyTalk Room" } }
        val displayShortCode = RoomShareHelper.getDisplayRoomCode(activeRoomId, joinCodeState)
        val displayInviteLink = RoomShareHelper.buildDefaultInviteLink(activeRoomId)
        ModalBottomSheet(
            onDismissRequest = { showInviteFriendSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = currentTheme.cardBgColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            InviteFriendBottomSheetContent(
                isDarkMode = currentTheme.isDark,
                roomName = displayRoomName,
                roomId = activeRoomId,
                inviteLink = displayInviteLink,
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
                                language_flag = com.example.ui.components.getProgrammingIconUrl(newLang).ifBlank { newCountryCode },
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

    val giftReceiversList = remember(participantsData, supabaseProfiles, roomHostId, hostSeat, speakerSeats) {
        val list = mutableListOf<GiftReceiverUser>()
        val hUid = hostSeat.userId.ifBlank { roomHostId ?: "" }
        if (hUid.isNotBlank()) {
            list.add(
                GiftReceiverUser(
                    userId = hUid,
                    name = hostSeat.name,
                    avatarUrl = hostSeat.imageUrl,
                    countryCode = hostSeat.flagCode,
                    role = "host"
                )
            )
        }
        speakerSeats.forEach { sp ->
            if (sp.userId.isNotBlank()) {
                list.add(
                    GiftReceiverUser(
                        userId = sp.userId,
                        name = sp.name,
                        avatarUrl = sp.imageUrl,
                        countryCode = sp.flagCode,
                        role = "speaker"
                    )
                )
            }
        }
        participantsData.filter { p ->
            p.user_id != hUid && speakerSeats.none { it.userId == p.user_id }
        }.forEach { p ->
            val pProfile = supabaseProfiles.find { it.uid == p.user_id }
            val snippet = com.example.realtime.VoiceRoomRealtimeManager.getUserSnippet(p.user_id)
            val name = pProfile?.displayName?.ifBlank { null }
                ?: pProfile?.username?.ifBlank { null }
                ?: snippet?.name?.ifBlank { null }
                ?: "Listener"
            val avatar = pProfile?.avatar?.ifBlank { null } ?: snippet?.avatar
            val flagCode = pProfile?.countryCode?.ifBlank { null } ?: snippet?.countryCode ?: "in"
            list.add(
                GiftReceiverUser(
                    userId = p.user_id,
                    name = name,
                    avatarUrl = avatar,
                    countryCode = flagCode,
                    role = "listener"
                )
            )
        }
        list.distinctBy { it.userId }
    }

    // 1) Gift Bottom Sheet container
    if (showGiftSheet) {
        RoomGiftBottomSheet(
            roomId = activeRoomId,
            currentUserId = currentUserId,
            initialRecipientId = giftTargetUserId,
            participants = giftReceiversList,
            isDarkMode = isRoomDarkMode,
            onDismiss = { showGiftSheet = false },
            onGiftSentSuccess = { giftName, recipientName, qty, coinsSpent ->
                // Ephemeral notification Toast
                Toast.makeText(context, "Sent $qty $giftName to $recipientName!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Suspension Modal Dialog (Active moderation enforcement)
    if (myModerationStatus?.action_type == "suspend" && ModerationManager.isActionActive(myModerationStatus)) {
        val funkyColors = LocalFunkyColors.current
        AlertDialog(
            onDismissRequest = { /* Modal cannot be dismissed without leaving */ },
            icon = {
                SoftWarningIcon(
                    size = 36.dp,
                    color = Color(0xFFDC2626)
                )
            },
            title = {
                Text(
                    text = "Account Suspended",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = funkyColors.ink
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your access to Funkytalk voice rooms has been suspended due to moderation policy violations.",
                        fontSize = 14.sp,
                        color = funkyColors.mute
                    )
                    Surface(
                        color = if (funkyColors.isDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Reason: ${myModerationStatus?.reason ?: "Violation of Community Guidelines"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (funkyColors.isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                            )
                            if (!myModerationStatus?.expires_at.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Suspension expires: ${myModerationStatus?.expires_at}",
                                    fontSize = 12.sp,
                                    color = if (funkyColors.isDark) Color(0xFFFECACA) else Color(0xFFB91C1C)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Leave Room", color = Color.White)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = funkyColors.surface
        )
    }

    // 1b) Host Leave Room vs End Room Confirmation Dialog
    if (showHostLeaveDialog) {
        HostLeaveRoomSheet(
            onDismiss = { showHostLeaveDialog = false },
            onLeaveOnly = {
                showHostLeaveDialog = false
                viewModel?.hostLeaveRoomOnly(context, trigger = "host_leave_only_dialog") {
                    if (com.example.service.OverlayPermissionHelper.canDrawOverlays(context)) {
                        com.example.service.VoiceRoomOverlayManager.startServiceIfPermitted(context)
                    }
                    onBack()
                }
            },
            onEndRoom = {
                showHostLeaveDialog = false
                val pCount = participantsData.size.coerceAtLeast(1)
                scope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        isRoomEnded = true
                    }
                    com.example.realtime.AgoraVoiceManager.leaveChannel()
                    com.example.realtime.AgoraVoiceManager.muteLocalAudioStream(true)
                    viewModel?.setMicEnabled(false)
                    viewModel?.endActiveVoiceRoom(context) {
                        // End room RPC completed
                    }
                    var summary: RoomEndSummaryResponse? = null
                    for (retry in 1..3) {
                        try {
                            val summaryRes = SupabaseClient.api.getRoomEndSummary(roomIdEq = "eq.$activeRoomId")
                            if (summaryRes.isSuccessful && !summaryRes.body().isNullOrEmpty()) {
                                summary = summaryRes.body()!!.first()
                                break
                            }
                        } catch (_: Exception) {}
                        delay(500)
                    }
                    if (summary != null) {
                        withContext(Dispatchers.Main) {
                            roomEndSummary = summary
                        }
                    }
                }
            }
        )
    }

    // Speaker Seat Invitation Dialog
    pendingInviteState?.let { invite ->
        com.example.ui.screens.voiceroom.InviteToTalkDialog(
            hostName = roomHostProfile?.displayName ?: roomHostProfile?.username ?: "Host",
            hostAvatarUrl = roomHostProfile?.avatar,
            hostCountryCode = roomHostProfile?.countryCode ?: "in",
            onDismiss = {
                val invId = invite.id
                pendingInviteState = null
                viewModel?.respondToSpeakerInvite(invId, false, activeRoomId, context) { _, _ -> }
            },
            onAccept = {
                val invId = invite.id
                pendingInviteState = null
                viewModel?.respondToSpeakerInvite(invId, true, activeRoomId, context) { ok, msg ->
                    Toast.makeText(context, msg ?: (if (ok) "You are now a speaker!" else "Failed to accept invite"), Toast.LENGTH_SHORT).show()
                }
            }
        )
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
            val reportCategories = listOf(
                "Harassment / Abuse",
                "Spam / Promotion",
                "Hate Speech",
                "Sexual / Inappropriate",
                "Dangerous Activity",
                "Other Concerns"
            )
            val recentMsgsSnapshot = messages.takeLast(20).map { msg ->
                ReportMessageSnapshot(
                    sender = msg.senderName,
                    text = msg.text,
                    timestamp = msg.createdAt ?: ""
                )
            }
            val speakerLogsSnapshot = participantsData.map { part ->
                val p = supabaseProfiles.find { it.uid == part.user_id }
                val isMe = (part.user_id == currentUserId)
                val name = if (isMe) (p?.displayName ?: "You") else (p?.displayName ?: p?.username ?: "User")
                val targetUuid = if (isMe) {
                    SupabaseSessionManager.getUserId(context) ?: part.user_id
                } else {
                    part.user_id
                }
                ReportSpeakerLogItem(
                    user_id = targetUuid,
                    name = name,
                    role = part.role ?: "listener",
                    is_mic_enabled = part.is_mic_enabled ?: false
                )
            }

            ReportRoomBottomSheetContent(
                selectedThemeIndex = selectedThemeIndex,
                selectedReasonIndex = selectedReportReasonIndex,
                onReasonIndexChange = { selectedReportReasonIndex = it },
                recentMessageCount = recentMsgsSnapshot.size,
                activeParticipantsCount = speakerLogsSnapshot.size,
                screenshotUrl = reportScreenshotUrl,
                isUploadingScreenshot = isUploadingScreenshot,
                onAttachScreenshot = { reportScreenshotLauncher.launch("image/*") },
                onRemoveScreenshot = { reportScreenshotUrl = null },
                clipUrl = reportAudioClipUrl,
                isUploadingClip = isUploadingClip,
                onAttachClip = { reportClipLauncher.launch("audio/*") },
                onRemoveClip = { reportAudioClipUrl = null },
                additionalDetails = reportAdditionalDetails,
                onDetailsChange = { reportAdditionalDetails = it },
                isSubmitting = isSubmittingReport,
                onDismiss = { showReportDialog = false },
                onSubmit = {
                    val category = reportCategories.getOrElse(selectedReportReasonIndex) { "Other Concerns" }
                    isSubmittingReport = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            // Ensure session is fresh and extract Supabase auth UUID
                            val reporterId = SupabaseSessionManager.ensureActiveSession(context)
                            val firebaseUser = FirebaseAuth.getInstance().currentUser
                            val firebaseUid = firebaseUser?.uid
                            val hasSession = SupabaseSessionManager.hasValidAuthenticatedSession(context)
                            val token = SupabaseSessionManager.getAccessToken(context)
                            val jwtSub = SupabaseSessionManager.extractUserIdFromJwt(token)
                            val effectiveReporterId = if (SupabaseSessionManager.isValidUuid(jwtSub)) jwtSub!! else (reporterId ?: "")

                            // Diagnostic Logging required by prompt
                            Log.d("VoiceRoomReport", "=== Report Submission Diagnostic ===")
                            Log.d("VoiceRoomReport", "1. Firebase UID: $firebaseUid")
                            Log.d("VoiceRoomReport", "2. Supabase session exists: $hasSession")
                            Log.d("VoiceRoomReport", "3. Supabase session user.id: $effectiveReporterId")
                            Log.d("VoiceRoomReport", "4. reporter_id being inserted: $effectiveReporterId")
                            Log.d("VoiceRoomReport", "5. room_id: $activeRoomId")

                            val bearerAuth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                            if (bearerAuth == null || effectiveReporterId.isBlank() || !SupabaseSessionManager.isValidUuid(effectiveReporterId)) {
                                Log.e("VoiceRoomReport", "Report blocked: No valid Supabase auth UUID or active JWT available")
                                withContext(Dispatchers.Main) {
                                    isSubmittingReport = false
                                    Toast.makeText(context, "Authentication error: Active Supabase session required to submit report. Please log in again.", Toast.LENGTH_LONG).show()
                                }
                                return@launch
                            }

                            val details = reportAdditionalDetails.trim().ifEmpty { null }

                            // Submit report via Supabase RPC report_user (p_context_type = "room")
                            val targetReportedUserId = roomHostId?.takeIf { it.isNotBlank() && it != effectiveReporterId }
                                ?: participantsData.firstOrNull { it.user_id != effectiveReporterId }?.user_id
                                ?: roomHostId.orEmpty()

                            val reportReq = ReportUserRequest(
                                p_reported_user_id = targetReportedUserId,
                                p_context_type = "room",
                                p_category = category,
                                p_reason = details ?: category,
                                p_context_id = activeRoomId,
                                p_screenshot_url = reportScreenshotUrl
                            )
                            val res = SupabaseClient.api.reportUser(
                                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                                auth = bearerAuth,
                                body = reportReq
                            )

                            if (res.isSuccessful) {
                                Log.i("VoiceRoomReport", "Report room persisted successfully: code=${res.code()}")
                                
                                // Broadcast report event via Realtime websocket after persistence
                                VoiceRoomRealtimeManager.broadcastRoomReport(
                                    roomId = activeRoomId,
                                    reporterId = effectiveReporterId,
                                    category = category,
                                    reason = category,
                                    details = details
                                )

                                withContext(Dispatchers.Main) {
                                    isSubmittingReport = false
                                    showReportDialog = false
                                    showReportSubmittedSheet = true
                                    reportAdditionalDetails = ""
                                    reportScreenshotUrl = null
                                    reportAudioClipUrl = null
                                    Toast.makeText(context, "Report submitted to moderation team", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val errBody = res.errorBody()?.string() ?: res.message()
                                Log.e("VoiceRoomReport", "6. Supabase error code=${res.code()}: $errBody")
                                withContext(Dispatchers.Main) {
                                    isSubmittingReport = false
                                    val errToast = if (res.code() == 403) {
                                        "Report submission failed: 403 Forbidden (RLS policy check failed)"
                                    } else {
                                        "Report submission failed: HTTP ${res.code()}"
                                    }
                                    Toast.makeText(context, errToast, Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("VoiceRoomReport", "Report submission exception", e)
                            withContext(Dispatchers.Main) {
                                isSubmittingReport = false
                                Toast.makeText(context, "Network error: ${e.localizedMessage ?: "Unable to submit report"}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
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

    // Gift Animation Overlay (Non-blocking touch overlay for room animations)
    GiftAnimationOverlay(
        hostUserId = roomHostId,
        speakerUserIds = speakerSeats.map { it.userId }
    )

    // Entry Animation Overlay (Non-blocking touch overlay for room entrance animations & banner)
    EntryAnimationOverlay()
}

private fun triggerUserEntryAnimation(context: android.content.Context, userId: String, userName: String) {
    if (userId.isBlank()) return
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        val cosmeticsMap = com.example.store.CosmeticsManager.getUserCosmetics(context, listOf(userId))
        val cosmetics = cosmeticsMap[userId] ?: com.example.store.CosmeticsManager.getCachedCosmetics(userId)
        val entrySlot = cosmetics?.entry_animation
        if (entrySlot != null && entrySlot.config != null) {
            val animConfig = parseEntryAnimationConfig(entrySlot.config)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                EntryAnimationManager.enqueueAnimation(
                    EntryAnimationData(
                        userId = userId,
                        userName = userName.ifBlank { "Learner" },
                        config = animConfig
                    ),
                    context
                )
            }
        }
    }
}

@Composable
fun ChatBottomSheetContent(privateChats: List<PrivateChatItem>, selectedThemeIndex: Int, onChatClick: (PrivateChatItem) -> Unit) {
    val currentTheme = roomThemes[selectedThemeIndex]
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vaultStatus by com.example.crypto.VaultSecurityManager.vaultStatus.collectAsState()

    if (vaultStatus == com.example.crypto.VaultStatus.LOCKED) {
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .background(Brush.verticalGradient(listOf(currentTheme.startColor, currentTheme.endColor)))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFFEF3C7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                FunkyLockIcon(
                    color = Color(0xFFD97706),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Unlock your chats",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentTheme.isDark) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your Vault password to access direct conversations.",
                fontSize = 13.sp,
                color = if (currentTheme.isDark) Color.White.copy(alpha = 0.7f) else Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                placeholder = { Text("Enter Vault Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        SoftEyeIcon(
                            visible = passwordVisible,
                            size = 20.dp,
                            tint = if (currentTheme.isDark) Color.White.copy(alpha = 0.6f) else Color.Gray
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (currentTheme.isDark) Color.Black.copy(alpha = 0.3f) else Color.White,
                    unfocusedContainerColor = if (currentTheme.isDark) Color.Black.copy(alpha = 0.3f) else Color.White,
                    focusedTextColor = if (currentTheme.isDark) Color.White else Color.Black,
                    unfocusedTextColor = if (currentTheme.isDark) Color.White else Color.Black
                )
            )
            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = err, fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (password.isBlank()) {
                        errorMessage = "Please enter your password"
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        val res = com.example.crypto.VaultSecurityManager.unlockWithPassword(context, password)
                        isLoading = false
                        res.onFailure { ex ->
                            errorMessage = ex.message ?: "Incorrect password."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTheme.isDark) currentTheme.themeColor else Color(0xFFF59E0B)
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Unlock", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
        return
    }

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
            leadingIcon = {
                SoftSearchIcon(
                    size = 18.dp,
                    color = if (currentTheme.isDark) Color.White.copy(alpha = 0.6f) else Color.Gray
                )
            },
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
fun ReportRoomBottomSheetContent(
    selectedThemeIndex: Int,
    selectedReasonIndex: Int,
    onReasonIndexChange: (Int) -> Unit,
    recentMessageCount: Int,
    activeParticipantsCount: Int,
    screenshotUrl: String?,
    isUploadingScreenshot: Boolean,
    onAttachScreenshot: () -> Unit,
    onRemoveScreenshot: () -> Unit,
    clipUrl: String?,
    isUploadingClip: Boolean,
    onAttachClip: () -> Unit,
    onRemoveClip: () -> Unit,
    additionalDetails: String,
    onDetailsChange: (String) -> Unit,
    isSubmitting: Boolean,
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
                SoftCloseIcon(
                    size = 18.dp,
                    color = closeBtnTint
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
                        SoftCheckIcon(
                            size = 14.dp,
                            color = if (isDark) Color(0xFF451A03) else Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Evidence section (Accurate & genuine telemetry)
        Text(
            text = "Evidence (automatically attached)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = titleColor
        )
        Text(
            text = "Live telemetry captured on device to verify reported incident",
            fontSize = 12.sp,
            color = subtitleColor,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Recent Messages Snapshot
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(cardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp, horizontal = 10.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Soft3DMessageStackIcon(modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Recent Messages\nSnapshot",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 14.sp
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            SoftCheckIcon(
                                size = 10.dp,
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF15803D)
                            )
                            Text(
                                text = "$recentMessageCount Attached",
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF15803D),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Card 2: Speaker & People Activity Log
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(cardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp, horizontal = 10.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Soft3DUsersGroupIcon(modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Speaker & People\nActivity Log",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 14.sp
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            SoftCheckIcon(
                                size = 10.dp,
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF15803D)
                            )
                            Text(
                                text = "$activeParticipantsCount Members",
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF15803D),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
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
            // Screenshot Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(
                        color = if (screenshotUrl != null) {
                            if (isDark) Color(0xFF065F46).copy(alpha = 0.5f) else Color(0xFFECFDF5)
                        } else cardBg,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (screenshotUrl != null) Color(0xFF10B981) else cardBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        if (!isUploadingScreenshot) {
                            if (screenshotUrl != null) onRemoveScreenshot() else onAttachScreenshot()
                        }
                    }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isUploadingScreenshot) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = titleColor,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Uploading...",
                            fontSize = 11.sp,
                            color = subtitleColor
                        )
                    }
                } else if (screenshotUrl != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Soft3DScreenshotIcon(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            SoftCheckIcon(
                                size = 11.dp,
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Screenshot",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        SoftCloseIcon(
                            size = 16.dp,
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Soft3DScreenshotIcon(modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Screenshot",
                            fontSize = 12.sp,
                            color = titleColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Clip Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(
                        color = if (clipUrl != null) {
                            if (isDark) Color(0xFF065F46).copy(alpha = 0.5f) else Color(0xFFECFDF5)
                        } else cardBg,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (clipUrl != null) Color(0xFF10B981) else cardBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        if (!isUploadingClip) {
                            if (clipUrl != null) onRemoveClip() else onAttachClip()
                        }
                    }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isUploadingClip) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = titleColor,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Uploading...",
                            fontSize = 11.sp,
                            color = subtitleColor
                        )
                    }
                } else if (clipUrl != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Soft3DVideoCamIcon(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            SoftCheckIcon(
                                size = 11.dp,
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Clip",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        SoftCloseIcon(
                            size = 16.dp,
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Soft3DVideoCamIcon(modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Clip (Audio)",
                            fontSize = 12.sp,
                            color = titleColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF59E0B),
                disabledContainerColor = Color(0xFFF59E0B).copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF451A03),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Submitting Report...",
                    color = Color(0xFF451A03),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            } else {
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
                    text = "Our moderation team reviews the chat snapshots and activity log",
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SoftMoonIcon(size = 11.dp, color = secondaryTextColor)
                            Text(text = "11:10 PM", fontSize = 11.sp, color = secondaryTextColor)
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
                                            FunkyDoubleCheckIcon(
                                                color = Color(0xFF00C4FF),
                                                modifier = Modifier.size(14.dp)
                                            )
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
                                        FunkyPlayIcon(color = Color.Gray, modifier = Modifier.size(8.dp))
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
                SoftSearchIcon(size = 14.dp, color = Color.Gray)
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
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        SoftMicIcon(size = 20.dp, color = secondaryTextColor)
                    }
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
                Box(modifier = Modifier.size(28.dp).clickable {}, contentAlignment = Alignment.Center) {
                    SoftImageIcon(size = 22.dp, color = Color.Gray)
                }
                Box(modifier = Modifier.size(28.dp).clickable {}, contentAlignment = Alignment.Center) {
                    SoftFaceIcon(size = 22.dp, color = Color.Gray)
                }
                Box(modifier = Modifier.size(28.dp).clickable {}, contentAlignment = Alignment.Center) {
                    SoftPlusIcon(size = 22.dp, color = Color.Gray)
                }
                Box(modifier = Modifier.size(28.dp).clickable {}, contentAlignment = Alignment.Center) {
                    CustomGiftBoxIcon(tint = Color(0xFFEC4899), modifier = Modifier.size(22.dp))
                }
                Box(modifier = Modifier.size(28.dp).clickable {}, contentAlignment = Alignment.Center) {
                    SoftHeartIcon(isLiked = true, size = 22.dp, color = Color(0xFFEC4899))
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
            .navigationBarsPadding()
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
        val features: List<Pair<String, @Composable () -> Unit>> = listOf(
            "Video" to { FunkyPlayIcon(color = textColor, modifier = Modifier.size(24.dp)) },
            "Minigame" to { SoftSparkleIcon(size = 26.dp, color = textColor) },
            "Message" to { SoftBellIcon(size = 26.dp, color = textColor) },
            "Mic effect" to { SoftFaceIcon(size = 26.dp, color = textColor) },
            "Music" to { SoftMusicNoteIcon(size = 26.dp, color = textColor) },
            "Setting" to { SoftSettingsGearIcon(size = 26.dp, color = textColor) }
        )

        val context = androidx.compose.ui.platform.LocalContext.current
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            val chunks = features.chunked(3)
            chunks.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { (name, iconContent) ->
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
                                iconContent()
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
                        if (theme.isDark) {
                            SoftMoonIcon(size = 28.dp, color = Color(0xFFFBBF24))
                        } else {
                            SoftSunIcon(size = 28.dp, color = Color(0xFFF59E0B))
                        }
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

@Composable
fun HostLeaveRoomSheet(
    onDismiss: () -> Unit,
    onLeaveOnly: () -> Unit,
    onEndRoom: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    val isDark = funkyColors.isDark
    var selectedOption by remember { mutableStateOf(0) } // 0 = Leave room, 1 = End room
    var showEndConfirmDialog by remember { mutableStateOf(false) }

    if (showEndConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEndConfirmDialog = false },
            containerColor = funkyColors.surface,
            title = {
                Text(
                    text = "End room for everyone?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = funkyColors.ink
                )
            },
            text = {
                Text(
                    text = "This will close the room immediately for all participants and clear all seats.",
                    fontSize = 14.sp,
                    color = funkyColors.mute
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showEndConfirmDialog = false
                        onEndRoom()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("End room", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showEndConfirmDialog = false }
                ) {
                    Text("Cancel", color = funkyColors.mute)
                }
            }
        )
    }

    val sheetBg = if (isDark) funkyColors.surface else Color(0xFFFFFDF8)
    val closeCircleBg = if (isDark) funkyColors.soft else Color(0xFFF1F5F9)
    val unselectedCardBg = if (isDark) funkyColors.soft else Color(0xFFFAF9F6)
    val unselectedBorder = if (isDark) funkyColors.line else Color(0xFFE2E8F0)

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(sheetBg)
                .border(1.dp, funkyColors.line, RoundedCornerShape(32.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle & Close button row
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Drag Handle
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(unselectedBorder)
                            .align(Alignment.Center)
                    )

                    // Close Button X
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(closeCircleBg)
                            .clickable { onDismiss() }
                            .align(Alignment.CenterEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftCloseIcon(size = 15.dp, color = funkyColors.mute)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hero Mascot Graphic
                Image(
                    painter = painterResource(id = R.drawable.img_leave_room_mascot),
                    contentDescription = "Leave Room Mascot",
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(130.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Leave Room?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = funkyColors.ink
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "As the Host, you can leave temporarily or end the session completely for everyone.",
                    fontSize = 13.sp,
                    color = funkyColors.mute,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Option 1: Leave Room
                val isOption0Selected = selectedOption == 0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isOption0Selected) {
                                if (isDark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFFFBEB)
                            } else unselectedCardBg
                        )
                        .border(
                            width = if (isOption0Selected) 1.5.dp else 1.dp,
                            color = if (isOption0Selected) Color(0xFFFFC107) else unselectedBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedOption = 0 }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0xFF78350F).copy(alpha = 0.55f) else Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftPersonIcon(
                                size = 24.dp,
                                color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Leave room",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Keeps room active with floating chat head. You have a 3-minute grace period to return.",
                                fontSize = 11.sp,
                                color = funkyColors.mute,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Radio dot
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isOption0Selected) Color(0xFFFFC107) else Color.Transparent)
                                .border(
                                    width = if (isOption0Selected) 0.dp else 1.5.dp,
                                    color = if (isOption0Selected) Color.Transparent else unselectedBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isOption0Selected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: End Room
                val isOption1Selected = selectedOption == 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isOption1Selected) {
                                if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.38f) else Color(0xFFFEF2F2)
                            } else unselectedCardBg
                        )
                        .border(
                            width = if (isOption1Selected) 1.5.dp else 1.dp,
                            color = if (isOption1Selected) Color(0xFFEF4444) else (if (isDark) Color(0xFFEF4444).copy(alpha = 0.35f) else Color(0xFFFEE2E2)),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedOption = 1 }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.55f) else Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftExitIcon(
                                size = 24.dp,
                                color = Color(0xFFEF4444)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "End room",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Closes the room for all participants immediately and clears all seats.",
                                fontSize = 11.sp,
                                color = funkyColors.mute,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Radio dot
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isOption1Selected) Color(0xFFEF4444) else Color.Transparent)
                                .border(
                                    width = if (isOption1Selected) 0.dp else 1.5.dp,
                                    color = if (isOption1Selected) Color.Transparent else unselectedBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isOption1Selected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Primary CTA: Continue
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(27.dp), spotColor = Color(0xFFFFB800).copy(alpha = 0.4f))
                        .clip(RoundedCornerShape(27.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFD000), Color(0xFFFFB800))
                            )
                        )
                        .clickable {
                            if (selectedOption == 0) {
                                onLeaveOnly()
                            } else {
                                showEndConfirmDialog = true
                            }
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        SoftForwardArrowIcon(
                            size = 18.dp,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary CTA: Cancel
                Text(
                    text = "Cancel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = funkyColors.mute,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun VoiceConnectionBanner(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    isLoading: Boolean = false,
    showRetry: Boolean = false,
    onRetry: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(enabled = showRetry) { onRetry() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (showRetry) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tap to retry",
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }
        }
    }
}

