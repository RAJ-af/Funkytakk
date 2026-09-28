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
    val avatarUrl: String = "",
    val createdBy: String = "",
    val myRole: String = "member", // "admin" or "member"
    val myStatus: String = "joined", // "invited", "joined", "left"
    val memberCount: Int = 1,
    val lastMessageText: String = "",
    val lastMessageSenderName: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "group_messages",
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["timestamp"])
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
    val messageType: String = "text", // "text", "image", "audio"
    val storagePath: String? = null,
    val localMediaPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean
)
