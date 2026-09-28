package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupConversationDao {
    @Query("SELECT * FROM group_conversations WHERE myStatus = 'joined' ORDER BY lastMessageTime DESC")
    fun getAllJoinedGroups(): Flow<List<LocalGroupConversation>>

    @Query("SELECT * FROM group_conversations WHERE myStatus = 'joined' ORDER BY lastMessageTime DESC")
    suspend fun getAllJoinedGroupsList(): List<LocalGroupConversation>

    @Query("SELECT * FROM group_conversations WHERE myStatus = 'invited' ORDER BY createdAt DESC")
    fun getPendingInvites(): Flow<List<LocalGroupConversation>>

    @Query("SELECT * FROM group_conversations WHERE myStatus = 'invited' ORDER BY createdAt DESC")
    suspend fun getPendingInvitesList(): List<LocalGroupConversation>

    @Query("SELECT * FROM group_conversations WHERE id = :id LIMIT 1")
    suspend fun getGroup(id: String): LocalGroupConversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(group: LocalGroupConversation)

    @Query("UPDATE group_conversations SET myStatus = :status WHERE id = :groupId")
    suspend fun updateStatus(groupId: String, status: String)

    @Query("UPDATE group_conversations SET unreadCount = 0 WHERE id = :groupId")
    suspend fun markRead(groupId: String)

    @Query("UPDATE group_conversations SET lastMessageText = :text, lastMessageSenderName = :senderName, lastMessageTime = :time, unreadCount = unreadCount + :unreadDelta WHERE id = :groupId")
    suspend fun updateLastMessage(groupId: String, text: String, senderName: String, time: Long, unreadDelta: Int)

    @Query("UPDATE group_conversations SET memberCount = :count WHERE id = :groupId")
    suspend fun updateMemberCount(groupId: String, count: Int)

    @Query("DELETE FROM group_conversations WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("DELETE FROM group_conversations")
    suspend fun deleteAllGroups()
}

@Dao
interface GroupMessageDao {
    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getGroupMessages(groupId: String): Flow<List<LocalGroupMessage>>

    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    suspend fun getGroupMessagesList(groupId: String): List<LocalGroupMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: LocalGroupMessage)

    @Query("SELECT EXISTS(SELECT 1 FROM group_messages WHERE id = :id)")
    suspend fun messageExists(id: String): Boolean

    @Query("DELETE FROM group_messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM group_messages WHERE groupId = :groupId")
    suspend fun clearGroupMessages(groupId: String)

    @Query("DELETE FROM group_messages")
    suspend fun deleteAllMessages()
}
