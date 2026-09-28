package com.example.ui.screens.home.models

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Color Palette aligning with FunkyTalk M3 Style
val GoldDark = Color(0xFFE5B120)         // Slightly darker for gradients/pressed states
val DarkText = Color(0xFF111827)         // Off-black
val GraySubtitle = Color(0xFF6B7280)     // Soft cool gray
val CardBgGreen = Color(0xFFEBFDF5)      // Positive badge container
val LiveRed = Color(0xFFEF4444)          // Live highlight

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
    val avatars: List<String>,
    val hostId: String = ""
)

data class MomentPost(
    val id: String,
    val userId: String = "",
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
    val currentRoomId: String? = null,
    val isOnline: Boolean = true
)

fun parseColorHex(hex: String, defaultColor: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}

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

fun decideSupabaseRoomTag(
    room: com.example.SupabaseVoiceRoom,
    allRooms: List<com.example.SupabaseVoiceRoom>,
    realSpeakingCount: Int = 1,
    realListeningCount: Int = 0,
    userLearningLanguage: String = "",
    userNativeLanguage: String = ""
): String {
    val totalInRoom = realSpeakingCount + realListeningCount
    if (totalInRoom >= 5 || (room.last_1h_joins ?: 0) > 4) {
        return "HOT"
    }
    if (room.host_is_vip == true) {
        return "VIP"
    }
    if (room.host_is_verified == true && (room.total_visits_24h ?: 0) > 15) {
        return "ELITE"
    }
    if ((room.last_2h_growth_rate ?: 0.0) > 0.3) {
        return "TRENDING"
    }
    if (realSpeakingCount > 2) {
        return "LIVE"
    }
    if (userLearningLanguage.isNotBlank() && room.language.equals(userLearningLanguage, ignoreCase = true)) {
        return "RECOMMENDED"
    }
    // Check if room was created in the last 24 hours
    val createdTimeMs = try {
        room.created_at?.let {
            java.time.Instant.parse(it).toEpochMilli()
        } ?: 0L
    } catch (e: Exception) {
        0L
    }
    val isRecentlyCreated = createdTimeMs > 0L && (System.currentTimeMillis() - createdTimeMs) < 24 * 60 * 60 * 1000
    if (room.status_tag.equals("NEW", ignoreCase = true) && isRecentlyCreated) {
        return "NEW"
    }
    val maxVisits = allRooms.maxOfOrNull { it.total_visits_24h ?: 0 } ?: 0
    if (maxVisits > 10 && (room.total_visits_24h ?: 0) >= maxVisits) {
        return "POPULAR"
    }
    return room.status_tag?.ifBlank { "LIVE" } ?: "LIVE"
}

