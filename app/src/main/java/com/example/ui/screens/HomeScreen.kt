package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.AuthState
import com.example.AuthViewModel
import com.example.UserProfile
import com.example.ui.components.CircleFlag
import com.example.ui.components.Icon
import com.example.ui.theme.*
import java.util.Calendar


data class FirestoreFriend(
    val userId: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val flagCode: String = "",
    val status: String = "online", // speaking/online/offline
    val isInVoiceRoom: Boolean = false,
    val currentRoomId: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): FirestoreFriend {
            return FirestoreFriend(
                userId = map["userId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                avatarUrl = map["avatarUrl"] as? String ?: "",
                flagCode = map["flagCode"] as? String ?: "",
                status = map["status"] as? String ?: "online",
                isInVoiceRoom = map["isInVoiceRoom"] as? Boolean ?: false,
                currentRoomId = map["currentRoomId"] as? String
            )
        }
    }
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "name" to name,
        "avatarUrl" to avatarUrl,
        "flagCode" to flagCode,
        "status" to status,
        "isInVoiceRoom" to isInVoiceRoom,
        "currentRoomId" to currentRoomId
    )
}

data class FirestoreRoom(
    val roomId: String = "",
    val title: String = "",
    val description: String = "",
    val language: String = "",
    val languageCode: String = "",
    val languageFlag: String = "",
    val bannerImageUrl: String = "",
    val cardBackgroundType: String = "",
    val gradientColors: List<String> = emptyList(),
    val levelTag: String = "",
    val statusTag: String = "",
    val speakingCount: Int = 1,
    val listeningCount: Int = 0,
    val totalParticipants: Int = 1,
    val previewAvatars: List<String> = emptyList(),
    val status: String = "active",
    val isLive: Boolean = true,
    val hostId: String = "",
    val hostIsVip: Boolean = false,
    val hostIsVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivityAt: Long = System.currentTimeMillis(),
    val last1HourJoins: Int = 0,
    val last2HourGrowthRate: Double = 0.0,
    val totalVisits24h: Int = 1
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): FirestoreRoom {
            return FirestoreRoom(
                roomId = map["roomId"] as? String ?: "",
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                language = map["language"] as? String ?: "",
                languageCode = map["languageCode"] as? String ?: "",
                languageFlag = map["languageFlag"] as? String ?: "",
                bannerImageUrl = map["bannerImageUrl"] as? String ?: "",
                cardBackgroundType = map["cardBackgroundType"] as? String ?: "",
                gradientColors = (map["gradientColors"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                levelTag = map["levelTag"] as? String ?: "",
                statusTag = map["statusTag"] as? String ?: "",
                speakingCount = (map["speakingCount"] as? Number)?.toInt() ?: 1,
                listeningCount = (map["listeningCount"] as? Number)?.toInt() ?: 0,
                totalParticipants = (map["totalParticipants"] as? Number)?.toInt() ?: 1,
                previewAvatars = (map["previewAvatars"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                status = map["status"] as? String ?: "active",
                isLive = map["isLive"] as? Boolean ?: true,
                hostId = map["hostId"] as? String ?: "",
                hostIsVip = map["hostIsVip"] as? Boolean ?: false,
                hostIsVerified = map["hostIsVerified"] as? Boolean ?: false,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastActivityAt = (map["lastActivityAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                last1HourJoins = (map["last1HourJoins"] as? Number)?.toInt() ?: 0,
                last2HourGrowthRate = (map["last2HourGrowthRate"] as? Number)?.toDouble() ?: 0.0,
                totalVisits24h = (map["totalVisits24h"] as? Number)?.toInt() ?: 1
            )
        }
    }
    fun toMap(): Map<String, Any?> = mapOf(
        "roomId" to roomId,
        "title" to title,
        "description" to description,
        "language" to language,
        "languageCode" to languageCode,
        "languageFlag" to languageFlag,
        "bannerImageUrl" to bannerImageUrl,
        "cardBackgroundType" to cardBackgroundType,
        "gradientColors" to gradientColors,
        "levelTag" to levelTag,
        "statusTag" to statusTag,
        "speakingCount" to speakingCount,
        "listeningCount" to listeningCount,
        "totalParticipants" to totalParticipants,
        "previewAvatars" to previewAvatars,
        "status" to status,
        "isLive" to isLive,
        "hostId" to hostId,
        "hostIsVip" to hostIsVip,
        "hostIsVerified" to hostIsVerified,
        "createdAt" to createdAt,
        "lastActivityAt" to lastActivityAt,
        "last1HourJoins" to last1HourJoins,
        "last2HourGrowthRate" to last2HourGrowthRate,
        "totalVisits24h" to totalVisits24h
    )
}

fun parseColorHex(hex: String, defaultColor: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}

fun getBadgeColor(tag: String): Color {
    return when (tag) {
        "LIVE" -> Color(0xFFEF4444)
        "HOT" -> Color(0xFFD97706)
        "NEW" -> Color(0xFF10B981)
        "RECOMMENDED" -> Color(0xFF3B82F6)
        "POPULAR" -> Color(0xFF8B5CF6)
        "VIP" -> Color(0xFFE5B120)
        "ELITE" -> Color(0xFF059669)
        "TRENDING" -> Color(0xFFEC4899)
        else -> Color(0xFF6B7280)
    }
}

fun decideRoomTag(
    room: FirestoreRoom,
    allRooms: List<FirestoreRoom>,
    userLearningLanguage: String,
    userNativeLanguage: String
): String {
    if (room.isLive && room.speakingCount > 3) {
        return "LIVE"
    }
    if (room.hostIsVip) {
        return "VIP"
    }
    if (room.hostIsVerified && room.totalVisits24h > 20) {
        return "ELITE"
    }
    if (room.last2HourGrowthRate > 0.4) {
        return "TRENDING"
    }
    if (room.last1HourJoins > 5) {
        return "HOT"
    }
    val isNew = (System.currentTimeMillis() - room.createdAt) < 24 * 60 * 60 * 1000
    if (isNew) {
        return "NEW"
    }
    if (room.language.equals(userLearningLanguage, ignoreCase = true) || 
        room.language.equals(userNativeLanguage, ignoreCase = true)) {
        return "RECOMMENDED"
    }
    val maxVisits = allRooms.maxOfOrNull { it.totalVisits24h } ?: 0
    if (maxVisits > 0 && room.totalVisits24h >= maxVisits) {
        return "POPULAR"
    }
    return room.statusTag.ifBlank { "LIVE" }
}

fun seedDefaultFriends(db: com.google.firebase.firestore.FirebaseFirestore) {
    val defaultFriends = listOf(
        FirestoreFriend(
            userId = "user_001",
            name = "Siddharth",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            flagCode = "in",
            status = "speaking",
            isInVoiceRoom = true,
            currentRoomId = "FT5272"
        ),
        FirestoreFriend(
            userId = "user_002",
            name = "Jungkook",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
            flagCode = "kr",
            status = "speaking",
            isInVoiceRoom = true,
            currentRoomId = "FT1902"
        ),
        FirestoreFriend(
            userId = "user_003",
            name = "Yuki",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
            flagCode = "jp",
            status = "online",
            isInVoiceRoom = false,
            currentRoomId = null
        ),
        FirestoreFriend(
            userId = "user_004",
            name = "Dmitry",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
            flagCode = "ru",
            status = "speaking",
            isInVoiceRoom = true,
            currentRoomId = "FT8823"
        )
    )
    db.collection("friends").get().addOnSuccessListener { snapshot ->
        if (snapshot == null || snapshot.isEmpty) {
            val batch = db.batch()
            for (f in defaultFriends) {
                batch.set(db.collection("friends").document(f.userId), f.toMap())
            }
            batch.commit()
        }
    }
}

fun seedDefaultRoomsAndStats(db: com.google.firebase.firestore.FirebaseFirestore) {
    db.collection("stats").document("onlineCounters").get().addOnSuccessListener { doc ->
        if (doc == null || !doc.exists()) {
            db.collection("stats").document("onlineCounters").set(mapOf("onlineLearnersCount" to 1842))
        }
    }
    val defaultRooms = listOf(
        FirestoreRoom(
            roomId = "FT5272",
            title = "Hindi Voice Circle",
            description = "Connect with native speakers and learn expressions",
            language = "Hindi",
            languageCode = "in",
            bannerImageUrl = "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=800",
            levelTag = "Friendly hosts",
            statusTag = "LIVE",
            speakingCount = 5,
            listeningCount = 31,
            totalParticipants = 36,
            previewAvatars = listOf(
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
            ),
            isLive = true,
            hostId = "host_hindi",
            hostIsVip = false,
            hostIsVerified = false,
            createdAt = System.currentTimeMillis() - 4 * 60 * 60 * 1000
        ),
        FirestoreRoom(
            roomId = "FT1902",
            title = "Korean Talk & Chill",
            description = "Practice Korean with friendly native speakers",
            language = "Korean",
            languageCode = "kr",
            bannerImageUrl = "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800",
            levelTag = "Beginners welcome!",
            statusTag = "TRENDING",
            speakingCount = 4,
            listeningCount = 18,
            totalParticipants = 22,
            previewAvatars = listOf(
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"
            ),
            isLive = true,
            hostId = "host_korean",
            hostIsVip = true,
            hostIsVerified = true,
            createdAt = System.currentTimeMillis() - 2 * 60 * 60 * 1000
        ),
        FirestoreRoom(
            roomId = "FT8823",
            title = "English Lounge Room",
            description = "Let's talk in English!",
            language = "English",
            languageCode = "us",
            bannerImageUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ca1ad?w=800",
            levelTag = "Conversation Practice",
            statusTag = "NEW",
            speakingCount = 6,
            listeningCount = 20,
            totalParticipants = 26,
            previewAvatars = listOf(
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150"
            ),
            isLive = true,
            hostId = "host_english",
            hostIsVip = false,
            hostIsVerified = false,
            createdAt = System.currentTimeMillis() - 50 * 60 * 1000
        )
    )
    db.collection("rooms").get().addOnSuccessListener { snapshot ->
        if (snapshot == null || snapshot.isEmpty) {
            val batch = db.batch()
            for (r in defaultRooms) {
                batch.set(db.collection("rooms").document(r.roomId), r.toMap())
            }
            batch.commit()
        }
    }
}

// Color Palette aligning with FunkyTalk M3 Style
val GoldDark = Color(0xFFE5B120)         // Slightly darker for gradients/pressed states
val DarkText = Color(0xFF111827)         // Off-black
val GraySubtitle = Color(0xFF6B7280)     // Soft cool gray
val CardBgGreen = Color(0xFFEBFDF5)      // Positive badge container
val LiveRed = Color(0xFFEF4444)          // Live highlight

data class CustomRoom(
    val id: String,
    val title: String,
    val subtitle: String,
    val language: String,
    val flagCode: String,
    val speakingCount: Int,
    val listeningCount: Int,
    val tag: String,
    val bgGradient: Brush,
    val imageUrl: String,
    val overlayColor: Color,
    val badgeText: String,
    val badgeColor: Color,
    val avatars: List<String>
)

fun parseHexColor(hex: String, defaultColor: Color): Color {
    return try {
        val cleanHex = hex.removePrefix("#").trim()
        if (cleanHex.length == 6) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else if (cleanHex.length == 8) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else {
            defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}

fun prepopulateDatabaseWithMockRooms(db: com.google.firebase.firestore.FirebaseFirestore) {
    val rooms = listOf(
        mapOf(
            "roomId" to "FT5272",
            "title" to "Hindi Voice Circle",
            "description" to "Connect with native speakers & learn expressions",
            "language" to "Hindi",
            "languageCode" to "in",
            "languageFlag" to "🇮🇳",
            "bannerImageUrl" to "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=800",
            "cardBackgroundType" to "image",
            "levelTag" to "Friendly hosts",
            "statusTag" to "LIVE",
            "speakingCount" to 11,
            "listeningCount" to 50,
            "totalParticipants" to 61,
            "previewAvatars" to listOf(
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150"
            ),
            "status" to "active",
            "isLive" to true,
            "hostId" to "host_hindi",
            "hostIsVip" to false,
            "hostIsVerified" to false,
            "createdAt" to System.currentTimeMillis(),
            "lastActivityAt" to System.currentTimeMillis(),
            "last1HourJoins" to 15,
            "last2HourGrowthRate" to 0.6,
            "totalVisits24h" to 230
        ),
        mapOf(
            "roomId" to "FT1001",
            "title" to "English Lounge",
            "description" to "Casual conversation • Make friends",
            "language" to "English",
            "languageCode" to "us",
            "languageFlag" to "🇺🇸",
            "bannerImageUrl" to "https://images.unsplash.com/photo-1513635269975-59663e0ca1ad?w=800",
            "cardBackgroundType" to "gradient",
            "gradientColors" to listOf("#FFF5D6", "#FFD27C"),
            "levelTag" to "All levels welcome!",
            "statusTag" to "LIVE",
            "speakingCount" to 12,
            "listeningCount" to 48,
            "totalParticipants" to 60,
            "previewAvatars" to listOf(
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150"
            ),
            "status" to "active",
            "isLive" to true,
            "hostId" to "host_english",
            "hostIsVip" to false,
            "hostIsVerified" to false,
            "createdAt" to System.currentTimeMillis(),
            "lastActivityAt" to System.currentTimeMillis(),
            "last1HourJoins" to 8,
            "last2HourGrowthRate" to 0.1,
            "totalVisits24h" to 120
        ),
        mapOf(
            "roomId" to "FT2002",
            "title" to "Japanese Practice",
            "description" to "Let's talk in Japanese!",
            "language" to "Japanese",
            "languageCode" to "jp",
            "languageFlag" to "🇯🇵",
            "bannerImageUrl" to "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800",
            "cardBackgroundType" to "image",
            "levelTag" to "Beginner Friendly",
            "statusTag" to "NEW",
            "speakingCount" to 10,
            "listeningCount" to 40,
            "totalParticipants" to 50,
            "previewAvatars" to listOf(
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150"
            ),
            "status" to "active",
            "isLive" to true,
            "hostId" to "host_japanese",
            "hostIsVip" to false,
            "hostIsVerified" to false,
            "createdAt" to (System.currentTimeMillis() - 2000000L),
            "lastActivityAt" to System.currentTimeMillis(),
            "last1HourJoins" to 5,
            "last2HourGrowthRate" to 0.2,
            "totalVisits24h" to 80
        ),
        mapOf(
            "roomId" to "FT3003",
            "title" to "Italian Dolce Vita",
            "description" to "Beautiful culture, food, & art talks",
            "language" to "Italian",
            "languageCode" to "it",
            "languageFlag" to "🇮🇹",
            "bannerImageUrl" to "",
            "cardBackgroundType" to "gradient",
            "gradientColors" to listOf("#DCFCE7", "#BBF7D0"),
            "levelTag" to "High culture",
            "statusTag" to "ELITE",
            "speakingCount" to 9,
            "listeningCount" to 35,
            "totalParticipants" to 44,
            "previewAvatars" to listOf(
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"
            ),
            "status" to "active",
            "isLive" to true,
            "hostId" to "host_italian",
            "hostIsVip" to false,
            "hostIsVerified" to true,
            "createdAt" to (System.currentTimeMillis() - 86400000L * 3),
            "lastActivityAt" to System.currentTimeMillis(),
            "last1HourJoins" to 4,
            "last2HourGrowthRate" to 0.15,
            "totalVisits24h" to 95
        ),
        mapOf(
            "roomId" to "FT4004",
            "title" to "Korean Talk",
            "description" to "Practice Korean • Native speakers",
            "language" to "Korean",
            "languageCode" to "kr",
            "languageFlag" to "🇰🇷",
            "bannerImageUrl" to "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800",
            "cardBackgroundType" to "image",
            "levelTag" to "Beginner Friendly",
            "statusTag" to "TRENDING",
            "speakingCount" to 8,
            "listeningCount" to 32,
            "totalParticipants" to 40,
            "previewAvatars" to listOf(
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150"
            ),
            "status" to "active",
            "isLive" to true,
            "hostId" to "host_korean",
            "hostIsVip" to false,
            "hostIsVerified" to false,
            "createdAt" to (System.currentTimeMillis() - 86400000L),
            "lastActivityAt" to System.currentTimeMillis(),
            "last1HourJoins" to 22,
            "last2HourGrowthRate" to 0.85,
            "totalVisits24h" to 190
        ),
        mapOf(
            "roomId" to "FT5005",
            "title" to "Spanish Fiesta",
            "description" to "Vibrant Spanish exchange & storytelling",
            "language" to "Spanish",
            "languageCode" to "es",
            "languageFlag" to "🇪🇸",
            "bannerImageUrl" to "",
            "cardBackgroundType" to "gradient",
            "gradientColors" to listOf("#FEF3C7", "#FCD34D"),
            "levelTag" to "Vibrant conversations",
            "statusTag" to "HOT",
            "speakingCount" to 7,
            "listeningCount" to 30,
            "totalParticipants" to 37,
            "previewAvatars" to listOf(
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150"
            ),
            "status" to "active",
            "isLive" to true,
            "hostId" to "host_spanish",
            "hostIsVip" to false,
            "hostIsVerified" to false,
            "createdAt" to (System.currentTimeMillis() - 172800000L),
            "lastActivityAt" to System.currentTimeMillis(),
            "last1HourJoins" to 35,
            "last2HourGrowthRate" to 0.4,
            "totalVisits24h" to 220
        ),
        mapOf(
            "roomId" to "FT6006",
            "title" to "Chinese Chat Room",
            "description" to "Practice Chinese • Improve together",
            "language" to "Chinese",
            "languageCode" to "cn",
            "languageFlag" to "🇨🇳",
            "bannerImageUrl" to "",
            "cardBackgroundType" to "gradient",
            "gradientColors" to listOf("#ECFEFF", "#CFFAFE"),
            "levelTag" to "Intermediate levels",
            "statusTag" to "LIVE",
            "speakingCount" to 6,
            "listeningCount" to 25,
            "totalParticipants" to 31,
            "previewAvatars" to listOf(
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"
            ),
            "status" to "active",
            "isLive" to true,
            "hostId" to "host_chinese",
            "hostIsVip" to false,
            "hostIsVerified" to false,
            "createdAt" to (System.currentTimeMillis() - 86400000L * 2),
            "lastActivityAt" to System.currentTimeMillis(),
            "last1HourJoins" to 3,
            "last2HourGrowthRate" to 0.05,
            "totalVisits24h" to 60
        )
    )

    for (room in rooms) {
        val id = room["roomId"] as String
        db.collection("rooms").document(id).set(room)
    }
}

fun prepopulateDatabaseWithFollowingUsers(db: com.google.firebase.firestore.FirebaseFirestore) {
    val friends = listOf(
        mapOf(
            "userId" to "friend_1",
            "name" to "Siddharth",
            "avatarUrl" to "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            "countryFlag" to "🇮🇳",
            "flagCode" to "in",
            "status" to "speaking",
            "isInVoiceRoom" to true,
            "currentRoomId" to "FT5272"
        ),
        mapOf(
            "userId" to "friend_2",
            "name" to "Jungkook",
            "avatarUrl" to "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
            "countryFlag" to "🇰🇷",
            "flagCode" to "kr",
            "status" to "speaking",
            "isInVoiceRoom" to true,
            "currentRoomId" to "FT4004"
        ),
        mapOf(
            "userId" to "friend_3",
            "name" to "Yuki",
            "avatarUrl" to "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
            "countryFlag" to "🇯🇵",
            "flagCode" to "jp",
            "status" to "online",
            "isInVoiceRoom" to false,
            "currentRoomId" to ""
        ),
        mapOf(
            "userId" to "friend_4",
            "name" to "Dmitry",
            "avatarUrl" to "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
            "countryFlag" to "🇷🇺",
            "flagCode" to "ru",
            "status" to "speaking",
            "isInVoiceRoom" to true,
            "currentRoomId" to "FT5272"
        ),
        mapOf(
            "userId" to "friend_5",
            "name" to "Wei",
            "avatarUrl" to "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
            "countryFlag" to "🇨🇳",
            "flagCode" to "cn",
            "status" to "online",
            "isInVoiceRoom" to false,
            "currentRoomId" to ""
        ),
        mapOf(
            "userId" to "friend_6",
            "name" to "Emily",
            "avatarUrl" to "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
            "countryFlag" to "🇺🇸",
            "flagCode" to "us",
            "status" to "online",
            "isInVoiceRoom" to false,
            "currentRoomId" to ""
        )
    )

    for (friend in friends) {
        val id = friend["userId"] as String
        db.collection("following_users").document(id).set(friend)
    }
}

data class MomentPost(
    val id: String,
    val author: String,
    val username: String,
    val avatar: String,
    val timeAgo: String,
    val content: String,
    val imageUrl: String?,
    val initialLikes: Int,
    val commentsCount: Int,
    var isLiked: Boolean = false
)

data class DirectChat(
    val id: String,
    val name: String,
    val avatar: String,
    val lastMsg: String,
    val time: String,
    val unreadCount: Int,
    val onlineStatus: String, // "online", "idle", "offline"
    val isNativeLanguage: String
)

data class AmusementEvent(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val tag: String,
    val cardColorDark: Color,
    val cardColorLight: Color,
    val imageUrl: String
)

data class FollowedFriend(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val flagCode: String,
    val isSpeaking: Boolean,
    val statusText: String,
    val currentRoomId: String? = null
)

@Composable
fun SpeakingWaveform(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "waveform")
    
    // Animate heights of the three bars with offset phases to make them look organic
    val bar1Height by transition.animateFloat(
        initialValue = 5f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by transition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by transition.animateFloat(
        initialValue = 8f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar1Height.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar2Height.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar3Height.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.White)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit,
    onCreateLanguageRoom: () -> Unit = {},
    onRoomClick: () -> Unit = {}
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    var showCreateRoomBottomSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Dynamic state trackers
    var activeTab by remember { mutableStateOf("home") } // "home", "rooms", "moments", "chats", "profile"
    var searchQuery by remember { mutableStateOf("") }
    var selectedLanguageFilter by remember { mutableStateOf("All") }

    // Advanced search filter states
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterOnlyLive by remember { mutableStateOf(false) }
    var filterByLevel by remember { mutableStateOf("All") }
    var selectedSortOption by remember { mutableStateOf("Popular") }
    
    // Remote events / Amusements
    var showAmusementEventsDialog by remember { mutableStateOf(false) }
    var selectedAmusementEvent by remember { mutableStateOf<AmusementEvent?>(null) }

    // Direct Chat active messages thread dialog
    var activeChatWindowContact by remember { mutableStateOf<DirectChat?>(null) }
    val chatMessagesStorage = remember { mutableStateMapOf<String, List<Pair<String, Boolean>>>() } // Pair: text, isMe

    LaunchedEffect(authState) {
        if (authState !is AuthState.Success) {
            onLogoutSuccess()
        }
    }

    val currentUserProfile = remember(authState) {
        (authState as? AuthState.Success)?.user ?: UserProfile(
            uid = "guest",
            email = "learner@funkytalk.com",
            displayName = "Raj",
            isEmailVerified = true,
            username = "raj_learner",
            avatar = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
            nativeLanguage = "Hindi",
            learningLanguage = "English"
        )
    }

    // Dynamic greeting based on current local hours style
    val greetingText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    // Followed friends state loaded from Firestore in real-time
    var onlineLearnersCount by remember { mutableStateOf(1842) }
    var activeFriends by remember { mutableStateOf<List<FollowedFriend>>(emptyList()) }

    // Real-time Firestore synchronizer for welcome dynamic data & followed friends
    LaunchedEffect(viewModel.firestoreDb) {
        val db = viewModel.firestoreDb ?: return@LaunchedEffect

        // 1. Online count listener
        db.collection("app_metadata").document("welcome_data")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val count = snapshot.getLong("onlineLearnersCount")?.toInt()
                    if (count != null) {
                        onlineLearnersCount = count
                    }
                } else if (snapshot != null && !snapshot.exists()) {
                    db.collection("app_metadata").document("welcome_data")
                        .set(mapOf("onlineLearnersCount" to 1842))
                }
            }

        // 2. Active Following Friends real-time update
        db.collection("following_users").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                if (snapshot.isEmpty) {
                    prepopulateDatabaseWithFollowingUsers(db)
                } else {
                    val friends = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("userId") ?: doc.id
                            val name = doc.getString("name") ?: "Friend"
                            val avatarUrl = doc.getString("avatarUrl") ?: "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150"
                            val countryFlag = doc.getString("countryFlag") ?: "🇮🇳"
                            val flagCode = doc.getString("flagCode") ?: when (countryFlag) {
                                "🇮🇳" -> "in"
                                "🇺🇸" -> "us"
                                "🇰🇷" -> "kr"
                                "🇯🇵" -> "jp"
                                "🇨🇳" -> "cn"
                                "🇪🇸" -> "es"
                                "🇫🇷" -> "fr"
                                "🇩🇪" -> "de"
                                "🇮🇹" -> "it"
                                "🇷🇺" -> "ru"
                                "🇦🇪" -> "ae"
                                else -> "us"
                            }
                            val status = doc.getString("status") ?: "online"
                            
                            val isSpeaking = status == "speaking"
                            val statusText = when (status) {
                                "speaking" -> "Speaking"
                                "online" -> "Online"
                                else -> "Offline"
                            }
                            
                            FollowedFriend(
                                id = id,
                                name = name,
                                avatarUrl = avatarUrl,
                                flagCode = flagCode,
                                isSpeaking = isSpeaking,
                                statusText = statusText
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    activeFriends = friends
                }
            }
        }
    }

    // Local minor real-time active fluctuation simulation
    LaunchedEffect(onlineLearnersCount) {
        kotlinx.coroutines.delay(10000)
        onlineLearnersCount = (onlineLearnersCount + (-3..3).random()).coerceIn(1000, 5000)
    }

    val amusementEvents = remember {
        listOf(
            AmusementEvent(
                id = "evt_1",
                title = "Language Fiesta 2026",
                description = "Join 500+ speakers across 10 languages for an epic mega-room party!",
                date = "June 16, 2026 - 15:00 UTC",
                tag = "Mega Event",
                cardColorDark = Color(0xFFA855F7),
                cardColorLight = Color(0xFFF3E8FF),
                imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800"
            ),
            AmusementEvent(
                id = "evt_2",
                title = "Movie Night: Korean Cinema",
                description = "Watch and discuss Parasite with native Korean speakers.",
                date = "June 18, 2026 - 20:00 KST",
                tag = "Culture",
                cardColorDark = Color(0xFFEF4444),
                cardColorLight = Color(0xFFFEE2E2),
                imageUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800"
            ),
            AmusementEvent(
                id = "evt_3",
                title = "Grammar Escape Room",
                description = "Solve English grammar and vocabulary puzzles to escape the room!",
                date = "June 20, 2026 - 10:00 EST",
                tag = "Game",
                cardColorDark = Color(0xFF3B82F6),
                cardColorLight = Color(0xFFEFF6FF),
                imageUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=800"
            )
        )
    }

    // Dynamic Rooms list state loaded from Firestore in real-time
    var roomsList by remember { mutableStateOf<List<CustomRoom>>(emptyList()) }

    // Real-time Firestore Subscriptions handling memory safety with DisposableEffect
    val firestore = viewModel.firestoreDb
    DisposableEffect(firestore) {
        if (firestore == null) {
            onDispose {}
        } else {
            // Dry seeding to ensure user is greeted with high fidelity database components instantly!
            seedDefaultRoomsAndStats(firestore)
            seedDefaultFriends(firestore)

            // 1. Subscribe to online count document
            val statsSub = firestore.collection("stats").document("onlineCounters")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val count = snapshot.getLong("onlineLearnersCount")?.toInt()
                        if (count != null) {
                            onlineLearnersCount = count
                        }
                    }
                }

            // 2. Subscribe to active friends list
            val friendsSub = firestore.collection("friends")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val fList = snapshot.map { doc ->
                            val f = FirestoreFriend.fromMap(doc.data)
                            FollowedFriend(
                                id = f.userId,
                                name = f.name,
                                avatarUrl = f.avatarUrl,
                                flagCode = f.flagCode,
                                isSpeaking = f.status == "speaking",
                                statusText = f.status.replaceFirstChar { it.uppercase() },
                                currentRoomId = f.currentRoomId
                            )
                        }
                        if (fList.isNotEmpty()) {
                            activeFriends = fList
                        }
                    }
                }

            // 3. Subscribe to active language voice rooms
            val roomsSub = firestore.collection("rooms")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val rawRooms = snapshot.map { doc ->
                            FirestoreRoom.fromMap(doc.data)
                        }
                        // Sort by newest rooms first
                        val sortedRaw = rawRooms.sortedByDescending { it.createdAt }
                        val mapped = sortedRaw.mapIndexed { index, r ->
                            val decidedTag = decideRoomTag(
                                room = r,
                                allRooms = sortedRaw,
                                userLearningLanguage = currentUserProfile.learningLanguage ?: "English",
                                userNativeLanguage = currentUserProfile.nativeLanguage ?: "Hindi"
                            )
                            val badgeColor = getBadgeColor(decidedTag)
                            val overlayVal = when (index % 4) {
                                0 -> Color(0x22FCD34D)
                                1 -> Color(0x22D8B4FE)
                                2 -> Color(0x22FDA4AF)
                                else -> Color(0x223B82F6)
                            }
                            val gradBrush = if (r.gradientColors.isNotEmpty()) {
                                Brush.linearGradient(r.gradientColors.map { parseColorHex(it, Color.White) })
                            } else {
                                Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)))
                            }

                            CustomRoom(
                                id = r.roomId,
                                title = r.title,
                                subtitle = r.description,
                                language = r.language,
                                flagCode = r.languageCode,
                                speakingCount = r.speakingCount,
                                listeningCount = r.listeningCount,
                                tag = r.levelTag,
                                bgGradient = gradBrush,
                                imageUrl = r.bannerImageUrl,
                                overlayColor = overlayVal,
                                badgeText = decidedTag,
                                badgeColor = badgeColor,
                                avatars = r.previewAvatars
                            )
                        }
                        if (mapped.isNotEmpty()) {
                            roomsList = mapped
                        }
                    }
                }

            onDispose {
                statsSub.remove()
                friendsSub.remove()
                roomsSub.remove()
            }
        }
    }

    // High fidelity Room cards fallback backup dataset
    val backupRoomsListStaticOnly = remember {
        listOf(
            CustomRoom(
                id = "room_1",
                title = "English Lounge",
                subtitle = "Casual conversation • Make friends",
                language = "English",
                flagCode = "us",
                speakingCount = 12,
                listeningCount = 48,
                tag = "All levels welcome!",
                bgGradient = Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))),
                imageUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ca1ad?w=800", // London illustration Sunset
                overlayColor = Color(0x22FCD34D),
                badgeText = "LIVE",
                badgeColor = LiveRed,
                avatars = listOf(
                    "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
                )
            ),
            CustomRoom(
                id = "room_2",
                title = "Korean Talk",
                subtitle = "Practice Korean • Native speakers",
                language = "Korean",
                flagCode = "kr",
                speakingCount = 8,
                listeningCount = 32,
                tag = "Beginner Friendly",
                bgGradient = Brush.linearGradient(listOf(Color(0xFFF3E8FF), Color(0xFFE9D5FF))),
                imageUrl = "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800", // Seoul soft sunset
                overlayColor = Color(0x22D8B4FE),
                badgeText = "TRENDING",
                badgeColor = Color(0xFFF97316),
                avatars = listOf(
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    "https://images.unsplash.com/photo-1524250502761-1ac6f2e30d43?w=150"
                )
            ),
            CustomRoom(
                id = "room_3",
                title = "Japanese Practice",
                subtitle = "Let's talk in Japanese!",
                language = "Japanese",
                flagCode = "jp",
                speakingCount = 10,
                listeningCount = 40,
                tag = "Beginner Friendly",
                bgGradient = Brush.linearGradient(listOf(Color(0xFFFFE4E6), Color(0xFFFECDD3))),
                imageUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800", // Pagoda Mt Fuji
                overlayColor = Color(0x22FDA4AF),
                badgeText = "NEW",
                badgeColor = Color(0xFF10B981),
                avatars = listOf(
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                    "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
                )
            ),
            CustomRoom(
                id = "room_4",
                title = "Chinese Chat Room",
                subtitle = "Practice Chinese • Improve together",
                language = "Chinese",
                flagCode = "cn",
                speakingCount = 6,
                listeningCount = 25,
                tag = "Intermediate levels",
                bgGradient = Brush.linearGradient(listOf(Color(0xFFECFEFF), Color(0xFFCFFAFE))),
                imageUrl = "https://images.unsplash.com/photo-1474181487882-5abf3f016c2d?w=800", // Shanghai Skyline
                overlayColor = Color(0x2267E8F9),
                badgeText = "LIVE",
                badgeColor = LiveRed,
                avatars = listOf(
                    "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                    "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150"
                )
            ),
            CustomRoom(
                id = "room_5",
                title = "Spanish Fiesta",
                subtitle = "Vibrant Spanish exchange & storytelling",
                language = "Spanish",
                flagCode = "es",
                speakingCount = 7,
                listeningCount = 30,
                tag = "Vibrant conversations",
                bgGradient = Brush.linearGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFCD34D))),
                imageUrl = "https://images.unsplash.com/photo-1509840144506-2c990f23a7f7?w=800", // Madrid
                overlayColor = Color(0x22FCD34D),
                badgeText = "HOT",
                badgeColor = Color(0xFFF59E0B),
                avatars = listOf(
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                    "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
                )
            ),
            CustomRoom(
                id = "room_6",
                title = "French Café",
                subtitle = "Charming Parisian discussions & idioms",
                language = "French",
                flagCode = "fr",
                speakingCount = 5,
                listeningCount = 18,
                tag = "Elegant speaking",
                bgGradient = Brush.linearGradient(listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD))),
                imageUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800", // Paris
                overlayColor = Color(0x2238BDF8),
                badgeText = "RECOMMENDED",
                badgeColor = Color(0xFF0284C7),
                avatars = listOf(
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    "https://images.unsplash.com/photo-1524250502761-1ac6f2e30d43?w=150"
                )
            ),
            CustomRoom(
                id = "room_7",
                title = "German Speak Club",
                subtitle = "Constructive practice & vocabulary build",
                language = "German",
                flagCode = "de",
                speakingCount = 4,
                listeningCount = 15,
                tag = "Structured debates",
                bgGradient = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))),
                imageUrl = "https://images.unsplash.com/photo-1467269204594-96e1714158fa?w=800", // Germany
                overlayColor = Color(0x2264748B),
                badgeText = "POPULAR",
                badgeColor = Color(0xFF475569),
                avatars = listOf(
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                    "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
                )
            ),
            CustomRoom(
                id = "room_8",
                title = "Italian Dolce Vita",
                subtitle = "Beautiful culture, food, & art talks",
                language = "Italian",
                flagCode = "it",
                speakingCount = 9,
                listeningCount = 35,
                tag = "High culture",
                bgGradient = Brush.linearGradient(listOf(Color(0xFFDCFCE7), Color(0xFFBBF7D0))),
                imageUrl = "https://images.unsplash.com/photo-1529260830199-445524953c2c?w=800", // Italy
                overlayColor = Color(0x224ADE80),
                badgeText = "ELITE",
                badgeColor = Color(0xFF16A34A),
                avatars = listOf(
                    "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                    "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150"
                )
            ),
            CustomRoom(
                id = "room_9",
                title = "Hindi Voice Circle",
                subtitle = "Connect with native speakers & learn expressions",
                language = "Hindi",
                flagCode = "in",
                speakingCount = 11,
                listeningCount = 50,
                tag = "Friendly hosts",
                bgGradient = Brush.linearGradient(listOf(Color(0xFFFFEDD5), Color(0xFFFED7AA))),
                imageUrl = "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=800", // Taj Mahal
                overlayColor = Color(0x22F97316),
                badgeText = "LIVE",
                badgeColor = LiveRed,
                avatars = listOf(
                    "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
                )
            ),
            CustomRoom(
                id = "room_10",
                title = "Arabic Majlis Chat",
                subtitle = "Deep vocabulary exchange & friendly banter",
                language = "Arabic",
                flagCode = "ae",
                speakingCount = 6,
                listeningCount = 20,
                tag = "Warm hospitality",
                bgGradient = Brush.linearGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFCD34D))),
                imageUrl = "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800", // Dubai
                overlayColor = Color(0x22FABF2C),
                badgeText = "VIP EXCLUSIVE",
                badgeColor = GoldDark,
                avatars = listOf(
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    "https://images.unsplash.com/photo-1524250502761-1ac6f2e30d43?w=150"
                )
            ),
            CustomRoom(
                id = "room_11",
                title = "Russian Speech Club",
                subtitle = "Practice Russian idioms and fluid phrasing",
                language = "Russian",
                flagCode = "ru",
                speakingCount = 5,
                listeningCount = 22,
                tag = "All levels welcome!",
                bgGradient = Brush.linearGradient(listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))),
                imageUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ca1ad?w=800",
                overlayColor = Color(0x223B82F6),
                badgeText = "LIVE",
                badgeColor = LiveRed,
                avatars = listOf(
                    "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                    "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150"
                )
            )
        )
    }

    // Social moments feed data
    val momentsPosts = remember {
        mutableStateListOf(
            MomentPost(
                id = "post_1",
                author = "Sarah Jenkins",
                username = "sarah_travels",
                avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                timeAgo = "2 hours ago",
                content = "Just finished a wonderful Korean practice room on FunkyTalk! Loving the friendly nature of speakers here. Any suggestions to level up speaking quickly?",
                imageUrl = "https://images.unsplash.com/photo-1484589065579-248adc015074?w=600",
                initialLikes = 42,
                commentsCount = 11
            ),
            MomentPost(
                id = "post_2",
                author = "Park Min-jun",
                username = "minjun_seoul",
                avatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                timeAgo = "4 hours ago",
                content = "Beautiful sunset here in Seoul today! Hope everyone is having a fabulous language exchange. Ready for another English speech session later?",
                imageUrl = "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=600",
                initialLikes = 89,
                commentsCount = 24
            ),
            MomentPost(
                id = "post_3",
                author = "Kenji Sato",
                username = "kenji_tokyo",
                avatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                timeAgo = "1 day ago",
                content = "Enjoyed reading English materials together with friends in a listening room. Small steps everyday make the difference! Keep going guys!",
                imageUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=600",
                initialLikes = 31,
                commentsCount = 6
            )
        )
    }

    // DM Contacts inbox data
    val conversationsList = remember {
        listOf(
            DirectChat("c_1", "Sarah Jenkins", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", "Hey! Are you free to practice English now?", "9:41 AM", 2, "online", "English"),
            DirectChat("c_2", "Park Min-jun", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150", "Let's join a room together tonight!", "Yesterday", 0, "online", "Korean"),
            DirectChat("c_3", "Yuki Tanaka", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", "Your Japanese is improving so fast!", "Sunday", 0, "offline", "Japanese"),
            DirectChat("c_4", "Chen Wei", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150", "Thanks for helping me with English yesterday!", "May 10", 0, "idle", "Chinese")
        )
    }

    // Ensure initial message queues exist
    LaunchedEffect(Unit) {
        conversationsList.forEach { chat ->
            if (!chatMessagesStorage.containsKey(chat.id)) {
                chatMessagesStorage[chat.id] = listOf(
                    "Hello! Glad we connected here." to false,
                    chat.lastMsg to false
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = "Sign Out", fontWeight = FontWeight.Bold, color = DarkText) },
            text = { Text(text = "Are you sure you want to sign out from FunkyTalk?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Text("Confirm", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showAmusementEventsDialog) {
        Dialog(
            onDismissRequest = { showAmusementEventsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showAmusementEventsDialog = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .padding(24.dp)
                    ) {
                        // Notch
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color.LightGray.copy(alpha = 0.5f))
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Funky Events \uD83C\uDF89",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                            IconButton(onClick = { showAmusementEventsDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Special language events and mega rooms dropping soon! Don't miss out.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(amusementEvents) { event ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedAmusementEvent = event
                                        },
                                    colors = CardDefaults.cardColors(containerColor = event.cardColorLight),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                        ) {
                                            AsyncImage(
                                                model = event.imageUrl,
                                                contentDescription = event.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                                            )
                                            Text(
                                                text = event.tag,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(12.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White.copy(alpha = 0.9f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = event.cardColorDark
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = event.date,
                                                color = event.cardColorDark,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = event.title,
                                                color = Color.Black,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = event.description,
                                                color = Color.DarkGray,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedAmusementEvent != null) {
        Dialog(
            onDismissRequest = { selectedAmusementEvent = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { selectedAmusementEvent = null },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            AsyncImage(
                                model = selectedAmusementEvent!!.imageUrl,
                                contentDescription = selectedAmusementEvent!!.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { selectedAmusementEvent = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = selectedAmusementEvent!!.date,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = selectedAmusementEvent!!.cardColorDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = selectedAmusementEvent!!.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = selectedAmusementEvent!!.description,
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Registered for event!", Toast.LENGTH_SHORT).show()
                                    selectedAmusementEvent = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                            ) {
                                Text(
                                    text = "Join Event",
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        Dialog(
            onDismissRequest = { showFilterDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showFilterDialog = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Outer sheet container capturing clicks
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) { /* Prevent bubble-up dismissal */ },
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        // Decorative bottom sheet gesture notch bar
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color.LightGray.copy(alpha = 0.5f))
                                .align(Alignment.CenterHorizontally)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Header Title & Reset button layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filter Rooms",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                            Text(
                                text = "Reset",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier
                                    .clickable {
                                        selectedSortOption = "Popular"
                                        selectedLanguageFilter = "All"
                                        filterByLevel = "All"
                                        filterOnlyLive = false
                                        Toast.makeText(context, "Filters reset", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(8.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // 1. SORT BY SECTION
                        Text(
                           text = "Sort By",
                           fontSize = 13.sp,
                           fontWeight = FontWeight.Bold,
                           color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val sortOptions = listOf(
                                "Popular" to "🔥 Popular",
                                "Recently Active" to "🕒 Recently Active",
                                "Most Listeners" to "👥 Most Listeners"
                            )
                            sortOptions.forEach { (option, label) ->
                                val isSel = selectedSortOption == option
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) GoldAccent else Color.White)
                                        .border(
                                            width = if (isSel) 1.5.dp else 1.dp,
                                            color = if (isSel) GoldAccent else Color.LightGray.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedSortOption = option }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.Black else Color.DarkGray,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // 2. LANGUAGE SECTION
                        Text(
                            text = "Language",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val langList = listOf(
                            "All" to Pair("🌐", "All Languages"),
                            "English" to Pair("🇺🇸", "English"),
                            "Hindi" to Pair("🇮🇳", "Hindi"),
                            "Russian" to Pair("🇷🇺", "Russian"),
                            "Korean" to Pair("🇰🇷", "Korean"),
                            "Japanese" to Pair("🇯🇵", "Japanese"),
                            "Chinese" to Pair("🇨🇳", "Chinese"),
                            "Spanish" to Pair("🇪🇸", "Spanish"),
                            "French" to Pair("🇫🇷", "French"),
                            "German" to Pair("🇩🇪", "German"),
                            "Arabic" to Pair("🇸🇦", "Arabic")
                        )
                        
                        val pairs = langList.chunked(2)
                        pairs.forEach { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { (langVal, details) ->
                                    val (emoji, label) = details
                                    val isSel = (langVal == "All" && selectedLanguageFilter == "All") || 
                                                (langVal != "All" && selectedLanguageFilter == langVal)
                                    
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White)
                                            .border(
                                                width = if (isSel) 1.5.dp else 1.dp,
                                                color = if (isSel) GoldAccent else Color.LightGray.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                selectedLanguageFilter = langVal
                                            }
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = emoji, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                        
                                        if (isSel) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(GoldAccent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (pair.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // 3. LEVEL SECTION
                        Text(
                            text = "Level",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val levelPairs = listOf(
                            listOf(
                                "All" to Pair("All Levels", null),
                                "Beginner" to Pair("Beginner", Color(0xFF10B981))
                            ),
                            listOf(
                                "Intermediate" to Pair("Intermediate", Color(0xFF3B82F6)),
                                "Advanced" to Pair("Advanced", Color(0xFF8B5CF6))
                            )
                        )
                        
                        levelPairs.forEach { levPair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                levPair.forEach { (levVal, details) ->
                                    val (label, dotColor) = details
                                    val isSel = filterByLevel == levVal
                                    
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White)
                                            .border(
                                                width = if (isSel) 1.5.dp else 1.dp,
                                                color = if (isSel) GoldAccent else Color.LightGray.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable { filterByLevel = levVal },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (dotColor != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                        
                                        if (isSel) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(GoldAccent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(26.dp))
                        
                        // 4. APPLY SOLID BOTTOM CAPSULE BUTTON
                        Button(
                            onClick = { 
                                showFilterDialog = false
                                Toast.makeText(context, "Filters applied!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text(
                                text = "Apply Filters",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // 5. CANCEL TEXT LABEL
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFilterDialog = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF9F9FA),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 12.dp,
                modifier = Modifier.height(72.dp)
            ) {
                // Navigation items
                NavigationBarItem(
                    icon = { Icon(imageVector = if (activeTab == "home") Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) },
                    label = { Text("Home", fontWeight = if (activeTab == "home") FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                    selected = activeTab == "home",
                    onClick = { activeTab = "home" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color.Black,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = GoldAccent.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(imageVector = if (activeTab == "rooms") Icons.Filled.Mic else Icons.Outlined.Mic, contentDescription = "Rooms", modifier = Modifier.size(24.dp)) },
                    label = { Text("Rooms", fontWeight = if (activeTab == "rooms") FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                    selected = activeTab == "rooms",
                    onClick = { activeTab = "rooms" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color.Black,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = GoldAccent.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(imageVector = if (activeTab == "moments") Icons.Filled.Language else Icons.Outlined.Language, contentDescription = "Moments", modifier = Modifier.size(24.dp)) },
                    label = { Text("Moments", fontWeight = if (activeTab == "moments") FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                    selected = activeTab == "moments",
                    onClick = { activeTab = "moments" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color.Black,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = GoldAccent.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    icon = { 
                        BadgedBox(
                            badge = {
                                Badge(containerColor = GoldAccent) {
                                    Text("3", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        ) {
                            Icon(imageVector = if (activeTab == "chats") Icons.Filled.ChatBubble else Icons.Outlined.ChatBubble, contentDescription = "Chats", modifier = Modifier.size(24.dp))
                        }
                    },
                    label = { Text("Chats", fontWeight = if (activeTab == "chats") FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                    selected = activeTab == "chats",
                    onClick = { activeTab = "chats" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color.Black,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = GoldAccent.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(imageVector = if (activeTab == "profile") Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile", modifier = Modifier.size(24.dp)) },
                    label = { Text("Profile", fontWeight = if (activeTab == "profile") FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                    selected = activeTab == "profile",
                    onClick = { activeTab = "profile" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color.Black,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = GoldAccent.copy(alpha = 0.5f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = activeTab,
            label = "HomeSubNavigation",
            modifier = Modifier.padding(innerPadding)
        ) { currentTab ->
            when (currentTab) {
                "home" -> {
                    // Beautiful Main Home tab matching requested mockup
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
                    ) {
                        // 1. BRAND HEADER & CREATION PILL
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(style = SpanStyle(color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)) {
                                                append("Funky")
                                            }
                                            withStyle(style = SpanStyle(color = GoldAccent, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)) {
                                                append("Talk")
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Talk. Learn. Connect. 💛",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GraySubtitle
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Amusements/Events icon with little badge dot
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .clickable {
                                                showAmusementEventsDialog = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = "Amusement Events",
                                            tint = Color.Black,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        // Tiny dynamic badge dot
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-10).dp, y = 10.dp)
                                                .clip(CircleShape)
                                                .background(GoldAccent)
                                        )
                                    }

                                    // Create Room cute yellow pill button
                                    Button(
                                        onClick = { showCreateRoomBottomSheet = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                        shape = RoundedCornerShape(100.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Create Room",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }

                        // 2. GOOD EVENING STATIC WELCOME ACCENT CARD
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFF2C2))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val displayGreetName = currentUserProfile.displayName?.ifBlank { "User" } 
                                            ?: currentUserProfile.username?.ifBlank { "User" } 
                                            ?: "Learner"
                                        Text(
                                            text = "$greetingText, $displayGreetName!",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF5A440D)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${java.text.NumberFormat.getIntegerInstance().format(onlineLearnersCount)} learners online now",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.Black
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2.5 FOLLOWED FRIENDS STORIES WITH LIVE WAVEFORM SPEAKING OVERLAYS
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 18.dp)
                            ) {
                                Text(
                                    text = "Friends you follow (Active)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    itemsIndexed(activeFriends) { index, friend ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable {
                                                    val speakMsg = if (friend.isSpeaking) "Speaking now" else "Active now"
                                                    Toast.makeText(context, "${friend.name} is online! ($speakMsg)", Toast.LENGTH_SHORT).show()
                                                }
                                                .width(76.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(68.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Bouncing dynamic outer scale indicator on actively speaking followed friends
                                                if (friend.isSpeaking) {
                                                    val infiniteTransition = rememberInfiniteTransition(label = "speaking_pulse")
                                                    val speakScale by infiniteTransition.animateFloat(
                                                        initialValue = 1.0f,
                                                        targetValue = 1.2f,
                                                        animationSpec = infiniteRepeatable(
                                                            animation = tween(1000, easing = FastOutSlowInEasing),
                                                            repeatMode = RepeatMode.Reverse
                                                        ),
                                                        label = "pulseScale"
                                                    )
                                                    val speakAlpha by infiniteTransition.animateFloat(
                                                        initialValue = 0.5f,
                                                        targetValue = 0.0f,
                                                        animationSpec = infiniteRepeatable(
                                                            animation = tween(1000, easing = FastOutSlowInEasing),
                                                            repeatMode = RepeatMode.Reverse
                                                        ),
                                                        label = "pulseAlpha"
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(56.dp)
                                                            .scale(speakScale)
                                                            .alpha(speakAlpha)
                                                            .border(
                                                                width = 2.dp,
                                                                color = GoldAccent,
                                                                shape = CircleShape
                                                            )
                                                    )
                                                }

                                                // The visual Avatar card itself with a thin gold border frame
                                                Box(
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .border(
                                                            width = 1.5.dp,
                                                            color = if (friend.isSpeaking) GoldAccent else Color.LightGray.copy(alpha = 0.4f),
                                                            shape = CircleShape
                                                        )
                                                        .padding(2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(modifier = Modifier.fillMaxSize()) {
                                                        AsyncImage(
                                                            model = friend.avatarUrl,
                                                            contentDescription = friend.name,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(CircleShape)
                                                        )

                                                        // Ambient audio wave equalizer overlay if currently speaking
                                                        if (friend.isSpeaking) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .clip(CircleShape)
                                                                    .background(Color.Black.copy(alpha = 0.45f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                SpeakingWaveform()
                                                            }
                                                        }
                                                    }
                                                }

                                                // Clean Physical-Masked Flag Badge completely overlapping the bottom-right corner of outer ring
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .align(Alignment.BottomEnd)
                                                        .border(2.dp, Color.White, CircleShape) // Clean, thick white mask frame
                                                        .clip(CircleShape)
                                                        .background(Color.White)
                                                ) {
                                                    CircleFlag(
                                                        countryCode = friend.flagCode,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = friend.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (friend.isSpeaking) "Speaking 🎙️" else "Online",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (friend.isSpeaking) GoldDark else Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. SEARCH & FILTER ROW
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Search bar capsule
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search language or room...", color = Color.Gray, fontSize = 14.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = Color.Gray
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(100.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = GoldAccent,
                                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { /* Do filtering */ })
                                )

                                // Custom Filter icon button next to it
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White)
                                        .clickable {
                                            showFilterDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Filter Parameters",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }

                        // 4. LANGUAGE CATEGORIES HORIZONTAL PILLS ROW
                        item {
                            val categories = listOf("All", "🇺🇸 English", "🇮🇳 Hindi", "🇰🇷 Korean", "🇯🇵 Japanese", "🇨🇳 Chinese", "🇷🇺 Russian")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { cat ->
                                    val catNormal = when {
                                        cat.contains("English") -> "English"
                                        cat.contains("Hindi") -> "Hindi"
                                        cat.contains("Korean") -> "Korean"
                                        cat.contains("Japanese") -> "Japanese"
                                        cat.contains("Chinese") -> "Chinese"
                                        cat.contains("Russian") -> "Russian"
                                        else -> cat
                                    }
                                    val isActive = selectedLanguageFilter == catNormal

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(if (isActive) GoldAccent else Color.White)
                                            .border(
                                                width = 1.dp,
                                                color = if (isActive) GoldAccent else Color.LightGray.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(100.dp)
                                            )
                                            .clickable {
                                                selectedLanguageFilter = catNormal
                                            }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) Color.Black else Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }

                        // 5. HIGH-FIDELITY ACTIVE ROOM CARDS LIST
                        val filteredRooms = roomsList.filter {
                            val matchesLang = (selectedLanguageFilter == "All" || it.language.equals(selectedLanguageFilter, ignoreCase = true))
                            val matchesQuery = (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.subtitle.contains(searchQuery, ignoreCase = true) || it.language.contains(searchQuery, ignoreCase = true))
                            val matchesLive = (!filterOnlyLive || it.badgeText == "LIVE")
                            val matchesLevel = when (filterByLevel) {
                                "All" -> true
                                "Beginner" -> it.tag.contains("Beginner", ignoreCase = true) || it.subtitle.contains("Beginner", ignoreCase = true) || it.title.contains("Beginner", ignoreCase = true)
                                "Intermediate" -> it.tag.contains("Intermediate", ignoreCase = true) || it.subtitle.contains("Intermediate", ignoreCase = true) || it.title.contains("Intermediate", ignoreCase = true)
                                "Advanced" -> it.tag.contains("Fluent", ignoreCase = true) || it.title.contains("Idioms", ignoreCase = true) || it.tag.contains("hospitality", ignoreCase = true)
                                else -> true
                            }
                            matchesLang && matchesQuery && matchesLive && matchesLevel
                        }.sortedWith(
                            when (selectedSortOption) {
                                "Popular" -> compareByDescending { it.speakingCount + it.listeningCount }
                                "Most Listeners" -> compareByDescending { it.listeningCount }
                                else -> compareBy { it.id } // Order by original ID sequence for Recently Active
                            }
                        )

                        if (filteredRooms.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No matching rooms found", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(filteredRooms) { index, room ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 10.dp)
                                        .height(210.dp)
                                        .clickable {
                                            onRoomClick()
                                        },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AnimatedRoomBanner(
                                            styleIndex = index,
                                            imageUrl = room.imageUrl,
                                            overlayColor = room.overlayColor,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Content hierarchy
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(18.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Top Row (Flag pill & LIVE badge)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Language & Flag Pill
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(100.dp))
                                                        .background(Color.White)
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    CircleFlag(
                                                        countryCode = room.flagCode,
                                                        modifier = Modifier.size(16.dp).clip(CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = room.language,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color.Black
                                                    )
                                                }

                                                // Top Right Badge
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(100.dp))
                                                        .background(room.badgeColor)
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = room.badgeText,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            // Middle Row (Title, Subtitle & Level tag)
                                            Column {
                                                Text(
                                                    text = room.title,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.Black
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = room.subtitle,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.DarkGray
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                // Tag Pill
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.White)
                                                        .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = room.tag,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color.Black
                                                    )
                                                }
                                            }

                                            // Bottom Row (Stacked participants, Stats counters & Join Room Yellow Button)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Participant stack
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    room.avatars.take(3).forEachIndexed { idx, url ->
                                                        AsyncImage(
                                                            model = url,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .offset(x = (-(idx * 10)).dp)
                                                                .size(28.dp)
                                                                .border(1.5.dp, Color.White, CircleShape)
                                                                .clip(CircleShape)
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .offset(x = (-(room.avatars.take(3).size * 10)).dp)
                                                            .size(28.dp)
                                                            .border(1.5.dp, Color.White, CircleShape)
                                                            .clip(CircleShape)
                                                            .background(Color.White),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "+${room.speakingCount + room.listeningCount}",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Black
                                                        )
                                                    }
                                                }

                                                // Right Bottom stats & Action Area inside protective pill container
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(100.dp))
                                                        .background(Color.White)
                                                        .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Speaking
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("${room.speakingCount} Speaking", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                    }
                                                    // Listening
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.Headset, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("${room.listeningCount} Listening", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                    }

                                                    // Join Button (Capsule)
                                                    Button(
                                                        onClick = {
                                                            Toast.makeText(context, "Welcome to ${room.title}! Connecting audio...", Toast.LENGTH_LONG).show()
                                                            onRoomClick()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                                        shape = RoundedCornerShape(100.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(26.dp)
                                                    ) {
                                                        Text(
                                                            text = "Join",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.Black,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "rooms" -> {
                    // Dedicated Voice channel directories
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TabRow(
                            selectedTabIndex = 0,
                            containerColor = Color.White
                        ) {
                            Tab(selected = true, onClick = {}, text = { Text("Active Conversations", fontWeight = FontWeight.Bold) })
                            Tab(selected = false, onClick = {}, text = { Text("Scheduled Audio") })
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(Icons.Default.Podcasts, contentDescription = null, modifier = Modifier.size(64.dp), tint = GoldAccent)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Ready to start practicing speaking?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Browse the rooms list or click the action below to create your own instant voice lounge.", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { showCreateRoomBottomSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                ) {
                                    Text("Start Voice Lounge", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
                "moments" -> {
                    // Interactive Student exchange timeline
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF3F4F6))
                    ) {
                        item {
                            TabRow(
                                selectedTabIndex = 0,
                                containerColor = Color.White
                            ) {
                                Tab(selected = true, onClick = {}, text = { Text("Trending", fontWeight = FontWeight.Bold) })
                                Tab(selected = false, onClick = {}, text = { Text("My Language") })
                            }
                        }

                        items(momentsPosts) { post ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = post.avatar,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(post.author, fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = 15.sp)
                                            Text("@" + post.username + " • " + post.timeAgo, color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(post.content, color = Color.Black, fontSize = 14.sp, lineHeight = 20.sp)

                                    if (post.imageUrl != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        AsyncImage(
                                            model = post.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Like interaction
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    post.isLiked = !post.isLiked
                                                    // Force state update by triggering item reload
                                                    val index = momentsPosts.indexOf(post)
                                                    if (index != -1) {
                                                        momentsPosts[index] = post.copy(isLiked = post.isLiked)
                                                    }
                                                }
                                                .padding(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Likes",
                                                tint = if (post.isLiked) Color.Red else Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${if (post.isLiked) post.initialLikes + 1 else post.initialLikes}",
                                                color = Color.DarkGray,
                                                fontSize = 13.sp
                                            )
                                        }

                                        // Comments counter display
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(6.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "Comments", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("${post.commentsCount}", color = Color.DarkGray, fontSize = 13.sp)
                                        }

                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                "chats" -> {
                    // DM Direct Message list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                Text("Conversations", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        items(conversationsList) { chat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeChatWindowContact = chat
                                    }
                                    .background(Color.White)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box {
                                    AsyncImage(
                                        model = chat.avatar,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                    )
                                    // Live status node
                                    if (chat.onlineStatus != "offline") {
                                        val nodeColor = if (chat.onlineStatus == "online") Color(0xFF10B981) else Color(0xFFFFB300)
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .align(Alignment.BottomEnd)
                                                .clip(CircleShape)
                                                .background(nodeColor)
                                                .border(2.dp, Color.White, CircleShape)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(chat.name, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
                                        Text(chat.time, color = Color.Gray, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = chat.lastMsg,
                                            color = if (chat.unreadCount > 0) Color.Black else Color.Gray,
                                            fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (chat.unreadCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(GoldAccent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "${chat.unreadCount}",
                                                    color = Color.Black,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)
                        }
                    }
                }
                "profile" -> {
                    // Dynamic Profile Details displaying onboarding selections
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF9F9FA)),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            // Profile header card with gradient background
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(GoldAccent.copy(alpha = 0.4f), Color(0xFFF9F9FA))
                                        )
                                    ),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.offset(y = 20.dp)
                                ) {
                                    // Rounded avatar image
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .border(4.dp, Color.White, CircleShape)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    ) {
                                        val avatarVal = currentUserProfile.avatar ?: ""
                                        if (avatarVal.isNotEmpty()) {
                                            AsyncImage(
                                                model = avatarVal,
                                                contentDescription = "My Avatar",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = "Default Avatar",
                                                tint = Color.LightGray,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = currentUserProfile.displayName?.ifBlank { "Unassigned Name" } ?: "FunkyTalk Learner",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "@" + (currentUserProfile.username?.ifBlank { "funky_username" } ?: "learner"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GraySubtitle
                                )

                                // Render flag and country if populated
                                val myCountry = currentUserProfile.country ?: "India"
                                val myCode = currentUserProfile.countryCode ?: "in"
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color.White)
                                        .border(0.5.dp, Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    CircleFlag(
                                        countryCode = myCode,
                                        modifier = Modifier.size(16.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = myCountry,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        // Statistic cells row
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val stats = listOf(
                                    "24" to "Rooms joined",
                                    "18.5h" to "Conversations",
                                    "5 days" to "Study Streak"
                                )
                                stats.forEach { (count, label) ->
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(count, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }

                        // Onboarding Language choices metadata section
                        item {
                            Spacer(modifier = Modifier.height(18.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text("Language Goals", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Native Language
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF3EDFD)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF6B21A8), modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Native Language", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                            Text(currentUserProfile.nativeLanguage ?: "Not set", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Learning Language
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFE6F4EA)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF137333), modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Learning Target", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                            Text(currentUserProfile.learningLanguage ?: "Not set", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }

                        // Onboarding selected Hobbies section
                        item {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text("My Hobbies", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    val hobbiesList = currentUserProfile.hobbies.ifEmpty {
                                        listOf("Exchange", "Travel", "Culture", "Anime", "Pop Vocal")
                                    }

                                    // Wrapping rows of hobby chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        hobbiesList.forEach { hobby ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(100.dp))
                                                    .background(Color(0xFFF3F4F6))
                                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text(hobby, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Metadata Details (Gender, Age / DOB)
                        item {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text("Personal Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Gender
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Gender", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                        Text(currentUserProfile.gender?.replaceFirstChar { it.uppercase() } ?: "Not specified", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Birthday / Date of Birth
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Date of Birth", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                        Text(currentUserProfile.dob ?: "Not specified", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }

                        // Logout Action section - keeping it beautifully positioned in the profile tab!
                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                            Button(
                                onClick = { showLogoutDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .height(56.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = Color.Red)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Logout from FunkyTalk", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet to Create Language / Private Rooms
    if (showCreateRoomBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCreateRoomBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Create Room",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Choose a room type to get started",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = { showCreateRoomBottomSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Private Room Card Choice
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(context, "Private room generation initialized!", Toast.LENGTH_SHORT).show()
                            showCreateRoomBottomSheet = false
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E6)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = GoldDark, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Private Room",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Only speech exchange partners you invite can join",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language Room Card Choice
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            showCreateRoomBottomSheet = false
                            onCreateLanguageRoom()
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDFD)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7C3AED).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Translate, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Language Room",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Add active practice tags and anyone can join to speak!",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Live active Chat conversation dialog (for simulation of messaging inbox DMs)
    if (activeChatWindowContact != null) {
        val activeChat = activeChatWindowContact!!
        val messages = chatMessagesStorage[activeChat.id] ?: emptyList()
        var messageInputText by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = { activeChatWindowContact = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                color = Color.White
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Area with contact status banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9FA))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { activeChatWindowContact = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        AsyncImage(
                            model = activeChat.avatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(activeChat.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (activeChat.onlineStatus == "online") Color(0xFF10B981) else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (activeChat.onlineStatus == "online") "Active now" else "Offline",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Flag of active contact
                        val targetFlag = when (activeChat.isNativeLanguage) {
                            "English" -> "us"
                            "Korean" -> "kr"
                            "Japanese" -> "jp"
                            "Chinese" -> "cn"
                            else -> "us"
                        }
                        CircleFlag(
                            countryCode = targetFlag,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Message bubbles area
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = true
                    ) {
                        // Show in reversed ordering
                        val reversedMessages = messages.reversed()
                        items(reversedMessages) { (text, isMe) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 4.dp,
                                                bottomEnd = if (isMe) 4.dp else 16.dp
                                            )
                                        )
                                        .background(if (isMe) GoldAccent else Color(0xFFF3F4F6))
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .widthIn(max = 260.dp)
                                ) {
                                    Text(
                                        text = text,
                                        fontSize = 14.sp,
                                        color = Color.Black,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    // Bottom chat input row
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = messageInputText,
                            onValueChange = { messageInputText = it },
                            placeholder = { Text("Write in ${activeChat.isNativeLanguage}...", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(100.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (messageInputText.isNotBlank()) {
                                        val workingList = messages.toMutableList()
                                        workingList.add(messageInputText to true)
                                        chatMessagesStorage[activeChat.id] = workingList
                                        messageInputText = ""
                                    }
                                }
                            )
                        )

                        IconButton(
                            onClick = {
                                if (messageInputText.isNotBlank()) {
                                    val workingList = messages.toMutableList()
                                    workingList.add(messageInputText to true)
                                    chatMessagesStorage[activeChat.id] = workingList
                                    messageInputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GoldAccent)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Message",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedRoomBanner(
    styleIndex: Int,
    imageUrl: String,
    overlayColor: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val infiniteTransition = rememberInfiniteTransition(label = "banner_transition")

        when (styleIndex % 10) {
            0 -> {
                // Style 0: CINEMATIC ZOOM & DRIFT
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "zoom"
                )
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().scale(scale)
                )
                val radiantPulse by infiniteTransition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 0.55f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFFD700).copy(alpha = radiantPulse * 0.4f), Color.Transparent),
                                radius = 400f
                            )
                        )
                )
            }
            1 -> {
                // Style 1: AMBIENT GOLDEN GLOW PULSE
                val pulseOpacity by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.55f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow"
                )
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFD97706).copy(alpha = pulseOpacity))
                )
            }
            2 -> {
                // Style 2: SLIDING GLASSMORPHIC PREMIUM SHIMMER
                val shimmerOffsetX by infiniteTransition.animateFloat(
                    initialValue = -1.2f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(5000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "shimmer"
                )
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val width = size.width
                            val height = size.height
                            if (width > 0f) {
                                val currentX = width * shimmerOffsetX
                                val brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0f),
                                        Color.White.copy(alpha = 0.28f),
                                        Color.White.copy(alpha = 0f)
                                    ),
                                    start = androidx.compose.ui.geometry.Offset(currentX - 100f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(currentX + 100f, height)
                                )
                                drawRect(brush)
                            }
                        }
                )
            }
            3 -> {
                // Style 3: COSMIC NEBULA DRIFT
                val nebulaColor by infiniteTransition.animateColor(
                    initialValue = Color(0xFF6D28D9), 
                    targetValue = Color(0xFFBFDBFE), 
                    animationSpec = infiniteRepeatable(
                        animation = tween(6000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "nebula"
                )
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(nebulaColor.copy(alpha = 0.25f))
                )
            }
            4 -> {
                // Style 4: HALO ROTATION
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(15000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotate"
                )
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val cp = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                            this.rotate(rotationAngle) {
                                drawCircle(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            Color(0xFFFFD700).copy(alpha = 0.35f),
                                            Color.Transparent,
                                            Color(0xFFFFD700).copy(alpha = 0.15f),
                                            Color.Transparent
                                        )
                                    ),
                                    radius = size.width * 0.4f,
                                    center = cp
                                )
                            }
                        }
                )
            }
            5 -> {
                // Style 5: BREATHING LUXURY VIGNETTE
                val vignetteBreathe by infiniteTransition.animateFloat(
                    initialValue = 0.45f,
                    targetValue = 0.75f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "vignette"
                )
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = vignetteBreathe * 0.6f)),
                                radius = 600f
                            )
                        )
                )
            }
            6 -> {
                // Style 6: AURORA GRADIENT SWEEP
                val offsetTranslation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(10000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "aurora"
                )
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val w = size.width
                            val h = size.height
                            val startOffset = w * offsetTranslation
                            val brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF34D399).copy(alpha = 0.18f),
                                    Color(0xFF60A5FA).copy(alpha = 0.18f),
                                    Color(0xFF34D399).copy(alpha = 0.18f)
                                ),
                                start = androidx.compose.ui.geometry.Offset(startOffset, 0f),
                                end = androidx.compose.ui.geometry.Offset(startOffset + w, h)
                            )
                            drawRect(brush)
                        }
                )
            }
            7 -> {
                // Style 7: GOLDEN DIAGONAL RIBBONS SLIDE
                val ribbonTranslation by infiniteTransition.animateFloat(
                    initialValue = -250f,
                    targetValue = 250f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(7000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "ribbons"
                )
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                color = Color(0xFFFFD700).copy(alpha = 0.08f),
                                topLeft = androidx.compose.ui.geometry.Offset(ribbonTranslation, 0f),
                                size = androidx.compose.ui.geometry.Size(120f, size.height)
                            )
                            drawRect(
                                color = Color(0xFFFFD700).copy(alpha = 0.05f),
                                topLeft = androidx.compose.ui.geometry.Offset(ribbonTranslation + 180f, 0f),
                                size = androidx.compose.ui.geometry.Size(60f, size.height)
                            )
                        }
                )
            }
            8 -> {
                // Style 8: CLASSIC SPARKLING STARS
                val sparkleBreathe by infiniteTransition.animateFloat(
                    initialValue = 0.1f,
                    targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "sparkle"
                )
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val w = size.width
                            val h = size.height
                            drawCircle(
                                color = Color.White.copy(alpha = sparkleBreathe * 0.7f),
                                radius = 6f,
                                center = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.3f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = (1f - sparkleBreathe) * 0.7f),
                                radius = 4f,
                                center = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.45f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = sparkleBreathe * 0.6f),
                                radius = 5f,
                                center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.75f)
                            )
                        }
                )
            }
            else -> {
                // Style 9: SMOOTH DRIFT VELVET BLOBS
                val blobOffset by infiniteTransition.animateFloat(
                    initialValue = -25f,
                    targetValue = 25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "blob"
                )
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            drawCircle(
                                color = Color(0xFFFCD34D).copy(alpha = 0.15f),
                                radius = size.width * 0.35f,
                                center = androidx.compose.ui.geometry.Offset(cx + blobOffset, cy - blobOffset)
                              )
                            drawCircle(
                                color = Color(0xFFF59E0B).copy(alpha = 0.1f),
                                radius = size.width * 0.25f,
                                center = androidx.compose.ui.geometry.Offset(cx - blobOffset, cy + blobOffset)
                            )
                        }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.95f)),
                        startY = 100f
                    )
                )
        )
    }
}
