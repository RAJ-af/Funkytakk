package com.example.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_conversations"
)
data class LocalGroupConversation(
    @PrimaryKey
    val id: String, // group_id UUID
    val name: String,
    val description: String = "",
    val avatarUrl: String = "",
    val createdBy: String = "",
    val isCreator: Boolean = false,
    val myRole: String = "member", // "admin" or "member"
    val myStatus: String = "joined", // "joined", "invited", "requested"
    val memberCount: Int = 1, // joined_count from server
    val pendingRequestsCount: Int = 0,
    val mutedUntil: Long? = null,
    val notifyLevel: String = "all", // "all", "mentions", "none"
    val mediaAutoDownload: Boolean = true,
    val whoCanSend: String = "everyone", // "everyone", "admins"
    val whoCanEditInfo: String = "admins", // "admins", "members"
    val whoCanAddMembers: String = "admins", // "admins", "members"
    val approveNewMembers: Boolean = false,
    val showJoinNotifications: Boolean = true,
    val disappearingSeconds: Int = 0, // 0, 86400, 604800, 7776000
    val memberListVisibility: String = "members", // "members", "admins"
    val lastMessageText: String = "",
    val lastMessageSenderName: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val hasUnreadMention: Boolean = false,
    val lastSyncedEventAt: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "group_messages",
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["timestamp"]),
        Index(value = ["expiresAt"])
    ]
)
data class LocalGroupMessage(
    @PrimaryKey
    val id: String, // message UUID
    val groupId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String? = null,
    val text: String, // Plaintext, decrypted locally!
    val messageType: String = "text", // "text", "image", "voice", "system"
    val eventType: String? = null, // for system events: "created", "joined", "left", "removed", "promoted", "demoted", "info_updated", "settings_updated", "invite_reset"
    val storagePath: String? = null,
    val localMediaPath: String? = null,
    val mediaKey: String? = null,
    val mediaNonce: String? = null,
    val mimeType: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null, // receive/send time + disappearing_seconds * 1000L
    val isOutgoing: Boolean,
    val mentionsJson: String = "[]",
    val isMentioned: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToSenderId: String? = null,
    val replyToSenderName: String? = null,
    val replyToPreview: String? = null
)

@Entity(
    tableName = "message_reactions",
    primaryKeys = ["target_message_id", "sender_id"],
    indices = [
        Index(value = ["target_message_id"]),
        Index(value = ["group_id"])
    ]
)
data class LocalMessageReaction(
    @androidx.room.ColumnInfo(name = "group_id")
    val groupId: String,
    @androidx.room.ColumnInfo(name = "target_message_id")
    val targetMessageId: String,
    @androidx.room.ColumnInfo(name = "sender_id")
    val senderId: String,
    @androidx.room.ColumnInfo(name = "emoji")
    val emoji: String,
    @androidx.room.ColumnInfo(name = "ts")
    val ts: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "message_translations",
    indices = [
        Index(value = ["message_id"])
    ]
)
data class LocalMessageTranslation(
    @PrimaryKey
    @androidx.room.ColumnInfo(name = "message_id")
    val messageId: String,
    @androidx.room.ColumnInfo(name = "translated_text")
    val translatedText: String,
    @androidx.room.ColumnInfo(name = "target_language")
    val targetLanguage: String = "English",
    @androidx.room.ColumnInfo(name = "provider")
    val provider: String = "sarvam", // "sarvam", "cerebras", "gemini", "on_device"
    @androidx.room.ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

