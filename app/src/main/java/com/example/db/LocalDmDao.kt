package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DmConversationDao {
    @Query("SELECT * FROM dm_conversations ORDER BY lastMessageTime DESC")
    fun getAllConversations(): Flow<List<LocalDmConversation>>

    @Query("SELECT * FROM dm_conversations WHERE id = :id LIMIT 1")
    suspend fun getConversation(id: String): LocalDmConversation?

    @Query("SELECT * FROM dm_conversations WHERE otherUserId = :otherUserId LIMIT 1")
    suspend fun getConversationByOtherUser(otherUserId: String): LocalDmConversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: LocalDmConversation)

    @Query("UPDATE dm_conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markRead(conversationId: String)

    @Query("UPDATE dm_conversations SET lastMessageText = :text, lastMessageTime = :time, unreadCount = unreadCount + :unreadDelta WHERE id = :conversationId")
    suspend fun updateLastMessage(conversationId: String, text: String, time: Long, unreadDelta: Int)

    @Query("DELETE FROM dm_conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM dm_conversations")
    suspend fun deleteAllConversations()
}

@Dao
interface DmMessageDao {
    @Query("SELECT * FROM dm_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessages(conversationId: String): Flow<List<LocalDmMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: LocalDmMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<LocalDmMessage>)

    @Query("DELETE FROM dm_messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM dm_messages WHERE conversationId = :conversationId")
    suspend fun clearConversationMessages(conversationId: String)

    @Query("DELETE FROM dm_messages")
    suspend fun deleteAllMessages()
}
