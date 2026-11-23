package com.example.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dm_conversations",
    indices = [Index(value = ["otherUserId"], unique = true)]
)
data class LocalDmConversation(
    @PrimaryKey
    val id: String, // conversation_id UUID
    val otherUserId: String,
    val otherUserName: String,
    val otherUserAvatar: String? = null,
    val otherUserFlag: String? = null,
    val lastMessageText: String = "",
    val lastMessageType: String = "text",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

object LocalMessageStatus {
    const val SENDING = "SENDING"
    const val SENT = "SENT"
    const val FAILED = "FAILED"
}

@Entity(
    tableName = "dm_messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"]),
        Index(value = ["clientId"]),
        Index(value = ["status"])
    ]
)
data class LocalDmMessage(
    @PrimaryKey
    val id: String, // message UUID
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val text: String, // Plaintext, decrypted locally!
    val messageType: String = "text", // "text", "image", "voice", "video", "file"
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val clientId: String? = null,
    val status: String = LocalMessageStatus.SENT,
    val failReason: String? = null,
    val retryCount: Int = 0,
    val nextRetryAt: Long = 0L,
    val localMediaPath: String? = null,
    val mediaMimeType: String? = null,
    val mediaDurationSec: Int? = null,
    val mediaCaption: String? = null
)

@Entity(
    tableName = "dm_message_reactions",
    primaryKeys = ["target_message_id", "sender_id"],
    indices = [
        Index(value = ["conversation_id"]),
        Index(value = ["target_message_id"])
    ]
)
data class LocalDmMessageReaction(
    @androidx.room.ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @androidx.room.ColumnInfo(name = "target_message_id")
    val targetMessageId: String,
    @androidx.room.ColumnInfo(name = "sender_id")
    val senderId: String,
    val emoji: String,
    val ts: Long = System.currentTimeMillis()
)
