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
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dm_messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"])
    ]
)
data class LocalDmMessage(
    @PrimaryKey
    val id: String, // message UUID
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val text: String, // Plaintext, decrypted locally!
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean,
    val replyToText: String? = null,
    val replyToSender: String? = null
)
