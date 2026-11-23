package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DmConversationDao {
    @Query("SELECT * FROM dm_conversations ORDER BY lastMessageTime DESC")
    fun getAllConversations(): Flow<List<LocalDmConversation>>

    @Query("SELECT * FROM dm_conversations ORDER BY lastMessageTime DESC")
    suspend fun getAllConversationsList(): List<LocalDmConversation>

    @Query("SELECT * FROM dm_conversations WHERE id = :id LIMIT 1")
    suspend fun getConversation(id: String): LocalDmConversation?

    @Query("SELECT * FROM dm_conversations WHERE otherUserId = :otherUserId LIMIT 1")
    suspend fun getConversationByOtherUser(otherUserId: String): LocalDmConversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: LocalDmConversation)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConversationIgnore(conversation: LocalDmConversation)

    @Query("UPDATE dm_conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markRead(conversationId: String)

    @Query("UPDATE dm_conversations SET lastMessageText = :text, lastMessageTime = :time, unreadCount = unreadCount + :unreadDelta WHERE id = :conversationId")
    suspend fun updateLastMessage(conversationId: String, text: String, time: Long, unreadDelta: Int)

    @Query("UPDATE dm_conversations SET lastMessageText = :text, lastMessageTime = :time, unreadCount = unreadCount + :unreadDelta, otherUserName = COALESCE(:name, otherUserName), otherUserAvatar = COALESCE(:avatar, otherUserAvatar), otherUserFlag = COALESCE(:flag, otherUserFlag) WHERE id = :conversationId")
    suspend fun updateLastMessageAndProfile(conversationId: String, text: String, time: Long, unreadDelta: Int, name: String?, avatar: String?, flag: String?)

    @Query("UPDATE dm_conversations SET id = :newId WHERE id = :oldId")
    suspend fun updateConversationId(oldId: String, newId: String)

    @Query("DELETE FROM dm_conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM dm_conversations")
    suspend fun deleteAllConversations()
}

@Dao
interface DmMessageDao {
    @Query("SELECT * FROM dm_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessages(conversationId: String): Flow<List<LocalDmMessage>>

    @Query("SELECT * FROM dm_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesList(conversationId: String): List<LocalDmMessage>

    @Query("SELECT * FROM dm_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessage(conversationId: String): LocalDmMessage?

    @Query("SELECT * FROM dm_messages WHERE isOutgoing = 1 AND status IN ('SENDING', 'FAILED') ORDER BY timestamp ASC")
    suspend fun getPendingOutgoingMessages(): List<LocalDmMessage>

    @Query("SELECT * FROM dm_messages WHERE clientId = :clientId LIMIT 1")
    suspend fun getMessageByClientId(clientId: String): LocalDmMessage?

    @Query("UPDATE dm_messages SET status = :status, failReason = :failReason, retryCount = :retryCount, nextRetryAt = :nextRetryAt WHERE id = :id")
    suspend fun updateMessageStatus(
        id: String,
        status: String,
        failReason: String? = null,
        retryCount: Int = 0,
        nextRetryAt: Long = 0L
    )

    @Query("UPDATE dm_messages SET conversationId = :newConversationId WHERE conversationId = :oldConversationId")
    suspend fun updateMessagesConversationId(oldConversationId: String, newConversationId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: LocalDmMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<LocalDmMessage>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessageIgnore(message: LocalDmMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessagesIgnore(messages: List<LocalDmMessage>)

    @Query("DELETE FROM dm_messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("SELECT * FROM dm_messages WHERE id = :id LIMIT 1")
    suspend fun getMessage(id: String): LocalDmMessage?

    @Query("SELECT EXISTS(SELECT 1 FROM dm_messages WHERE id = :id)")
    suspend fun messageExists(id: String): Boolean

    @Query("DELETE FROM dm_messages WHERE conversationId = :conversationId")
    suspend fun clearConversationMessages(conversationId: String)

    @Query("DELETE FROM dm_messages")
    suspend fun deleteAllMessages()

    @Transaction
    suspend fun deleteMessageAndRefreshConversation(
        messageId: String,
        conversationId: String,
        conversationDao: DmConversationDao
    ) {
        deleteMessage(messageId)
        val latest = getLatestMessage(conversationId)
        val conv = conversationDao.getConversation(conversationId) ?: return
        if (latest != null) {
            val preview = when (latest.messageType) {
                "image" -> "[Encrypted Photo]"
                "voice" -> "[Encrypted Voice Note]"
                else -> latest.text
            }
            conversationDao.insertOrUpdate(
                conv.copy(
                    lastMessageText = preview,
                    lastMessageType = latest.messageType,
                    lastMessageTime = latest.timestamp
                )
            )
        } else {
            conversationDao.insertOrUpdate(
                conv.copy(
                    lastMessageText = "",
                    lastMessageType = "text",
                    lastMessageTime = conv.createdAt
                )
            )
        }
    }

    @Transaction
    suspend fun migrateConversationId(
        oldConvId: String,
        newConvId: String,
        conversationDao: DmConversationDao,
        reactionDao: DmMessageReactionDao
    ) {
        if (oldConvId == newConvId || oldConvId.isBlank() || newConvId.isBlank()) return
        val existingNew = conversationDao.getConversation(newConvId)
        val oldConv = conversationDao.getConversation(oldConvId)
        updateMessagesConversationId(oldConvId, newConvId)
        reactionDao.updateConversationId(oldConvId, newConvId)
        if (existingNew != null && oldConv != null) {
            val mergedTime = maxOf(existingNew.lastMessageTime, oldConv.lastMessageTime)
            val mergedText = if (oldConv.lastMessageTime >= existingNew.lastMessageTime) oldConv.lastMessageText else existingNew.lastMessageText
            conversationDao.deleteConversation(oldConvId)
            conversationDao.insertOrUpdate(
                existingNew.copy(
                    lastMessageText = mergedText,
                    lastMessageTime = mergedTime,
                    unreadCount = existingNew.unreadCount + oldConv.unreadCount
                )
            )
        } else if (oldConv != null) {
            conversationDao.updateConversationId(oldConvId, newConvId)
        }
    }
}

@Dao
interface DmMessageReactionDao {
    @Query("SELECT * FROM dm_message_reactions WHERE conversation_id = :conversationId ORDER BY ts ASC")
    fun getReactionsForConversation(conversationId: String): Flow<List<LocalDmMessageReaction>>

    @Query("SELECT * FROM dm_message_reactions WHERE conversation_id = :conversationId ORDER BY ts ASC")
    suspend fun getReactionsForConversationList(conversationId: String): List<LocalDmMessageReaction>

    @Query("SELECT * FROM dm_message_reactions WHERE target_message_id = :messageId ORDER BY ts ASC")
    fun getReactionsForMessage(messageId: String): Flow<List<LocalDmMessageReaction>>

    @Query("SELECT * FROM dm_message_reactions WHERE target_message_id = :messageId AND sender_id = :senderId LIMIT 1")
    suspend fun getReaction(messageId: String, senderId: String): LocalDmMessageReaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: LocalDmMessageReaction)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReactionsIgnore(reactions: List<LocalDmMessageReaction>)

    @Query("DELETE FROM dm_message_reactions WHERE target_message_id = :messageId AND sender_id = :senderId")
    suspend fun deleteReaction(messageId: String, senderId: String)

    @Query("DELETE FROM dm_message_reactions WHERE target_message_id = :messageId")
    suspend fun deleteReactionsForMessage(messageId: String)

    @Query("DELETE FROM dm_message_reactions WHERE conversation_id = :conversationId")
    suspend fun deleteReactionsForConversation(conversationId: String)

    @Query("UPDATE dm_message_reactions SET conversation_id = :newConversationId WHERE conversation_id = :oldConversationId")
    suspend fun updateConversationId(oldConversationId: String, newConversationId: String)

    @Query("DELETE FROM dm_message_reactions WHERE target_message_id NOT IN (SELECT id FROM dm_messages)")
    suspend fun deleteOrphanReactions()
}
