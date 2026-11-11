package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupConversationDao {
    @Query("SELECT * FROM group_conversations WHERE myStatus = 'joined' AND isArchived = 0 ORDER BY lastMessageTime DESC")
    fun getAllJoinedGroups(): Flow<List<LocalGroupConversation>>

    @Query("SELECT * FROM group_conversations WHERE myStatus = 'joined' AND isArchived = 0 ORDER BY lastMessageTime DESC")
    suspend fun getAllJoinedGroupsList(): List<LocalGroupConversation>

    @Query("SELECT * FROM group_conversations WHERE myStatus IN ('invited', 'requested') AND isArchived = 0 ORDER BY createdAt DESC")
    fun getPendingInvites(): Flow<List<LocalGroupConversation>>

    @Query("SELECT * FROM group_conversations WHERE myStatus IN ('invited', 'requested') AND isArchived = 0 ORDER BY createdAt DESC")
    suspend fun getPendingInvitesList(): List<LocalGroupConversation>

    @Query("SELECT * FROM group_conversations WHERE myStatus IN ('joined', 'invited', 'requested') AND isArchived = 0 ORDER BY CASE WHEN myStatus = 'invited' THEN 0 WHEN myStatus = 'requested' THEN 1 ELSE 2 END, lastMessageTime DESC")
    fun getAllMyGroups(): Flow<List<LocalGroupConversation>>

    @Query("SELECT * FROM group_conversations WHERE isArchived = 1 ORDER BY lastMessageTime DESC")
    fun getArchivedGroups(): Flow<List<LocalGroupConversation>>

    @Query("SELECT * FROM group_conversations WHERE id = :id LIMIT 1")
    fun getGroupFlow(id: String): Flow<LocalGroupConversation?>

    @Query("SELECT * FROM group_conversations WHERE id = :id LIMIT 1")
    suspend fun getGroup(id: String): LocalGroupConversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(group: LocalGroupConversation)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroupIgnore(group: LocalGroupConversation)

    @Query("UPDATE group_conversations SET isArchived = 1, myStatus = :reason, tombstoneReason = :reason, tombstoneDeletedByName = :deletedByName WHERE id = :groupId")
    suspend fun archiveGroup(groupId: String, reason: String, deletedByName: String?)

    @Query("UPDATE group_conversations SET myStatus = :status WHERE id = :groupId")
    suspend fun updateStatus(groupId: String, status: String)

    @Query("UPDATE group_conversations SET unreadCount = 0, hasUnreadMention = 0 WHERE id = :groupId")
    suspend fun markRead(groupId: String)

    @Query("UPDATE group_conversations SET hasUnreadMention = :hasMention WHERE id = :groupId")
    suspend fun setHasUnreadMention(groupId: String, hasMention: Boolean)

    @Query("UPDATE group_conversations SET lastSyncedEventAt = :isoTimestamp WHERE id = :groupId")
    suspend fun updateLastSyncedEventAt(groupId: String, isoTimestamp: String)

    @Query("UPDATE group_conversations SET lastMessageText = :text, lastMessageSenderName = :senderName, lastMessageTime = :time, unreadCount = unreadCount + :unreadDelta WHERE id = :groupId")
    suspend fun updateLastMessage(groupId: String, text: String, senderName: String, time: Long, unreadDelta: Int)

    @Query("UPDATE group_conversations SET memberCount = :count WHERE id = :groupId")
    suspend fun updateMemberCount(groupId: String, count: Int)

    @Query("DELETE FROM group_conversations WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("DELETE FROM group_conversations WHERE id NOT IN (:ids)")
    suspend fun deleteGroupsNotIn(ids: List<String>)

    @Query("DELETE FROM group_conversations")
    suspend fun deleteAllGroups()
}

@Dao
interface GroupMessageDao {
    @Query("SELECT * FROM group_messages WHERE groupId = :groupId AND (expiresAt IS NULL OR expiresAt = 0 OR expiresAt > :now) ORDER BY timestamp DESC")
    fun getGroupMessages(groupId: String, now: Long = System.currentTimeMillis()): Flow<List<LocalGroupMessage>>

    @Query("SELECT * FROM group_messages WHERE groupId = :groupId AND (expiresAt IS NULL OR expiresAt = 0 OR expiresAt > :now) ORDER BY timestamp DESC")
    suspend fun getGroupMessagesList(groupId: String, now: Long = System.currentTimeMillis()): List<LocalGroupMessage>

    @Query("SELECT * FROM group_messages WHERE groupId = :groupId AND messageType IN ('image', 'voice', 'audio') AND (expiresAt IS NULL OR expiresAt = 0 OR expiresAt > :now) ORDER BY timestamp DESC")
    fun getSharedMediaMessages(groupId: String, now: Long = System.currentTimeMillis()): Flow<List<LocalGroupMessage>>

    @Query("SELECT * FROM group_messages WHERE groupId = :groupId AND messageType != 'system' AND (expiresAt IS NULL OR expiresAt = 0 OR expiresAt > :now) ORDER BY timestamp DESC")
    fun getAllGroupMessagesForLinksAndMedia(groupId: String, now: Long = System.currentTimeMillis()): Flow<List<LocalGroupMessage>>

    @Query("SELECT * FROM group_messages WHERE groupId = :groupId AND messageType = 'text' AND text LIKE '%' || :query || '%' AND (expiresAt IS NULL OR expiresAt = 0 OR expiresAt > :now) ORDER BY timestamp DESC")
    suspend fun searchGroupMessages(groupId: String, query: String, now: Long = System.currentTimeMillis()): List<LocalGroupMessage>

    @Query("DELETE FROM group_messages WHERE expiresAt IS NOT NULL AND expiresAt > 0 AND expiresAt <= :now")
    suspend fun deleteExpiredMessagesOnly(now: Long = System.currentTimeMillis())

    @Transaction
    suspend fun deleteExpiredMessages(now: Long = System.currentTimeMillis()) {
        deleteExpiredMessagesOnly(now)
    }

    @Query("UPDATE group_messages SET localMediaPath = :localPath WHERE id = :id")
    suspend fun updateLocalMediaPath(id: String, localPath: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: LocalGroupMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessageIgnore(message: LocalGroupMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessagesIgnore(messages: List<LocalGroupMessage>)

    @Query("SELECT EXISTS(SELECT 1 FROM group_messages WHERE id = :id)")
    suspend fun messageExists(id: String): Boolean

    @Query("SELECT * FROM group_messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): LocalGroupMessage?

    @Query("DELETE FROM group_messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM group_messages WHERE groupId = :groupId")
    suspend fun clearGroupMessages(groupId: String)

    @Query("DELETE FROM group_messages")
    suspend fun deleteAllMessages()
}

@Dao
interface MessageReactionDao {
    @Query("SELECT * FROM message_reactions WHERE group_id = :groupId")
    fun getReactionsForGroup(groupId: String): Flow<List<LocalMessageReaction>>

    @Query("SELECT * FROM message_reactions WHERE group_id = :groupId")
    suspend fun getReactionsForGroupList(groupId: String): List<LocalMessageReaction>

    @Query("SELECT * FROM message_reactions WHERE target_message_id = :targetMessageId")
    fun getReactionsForMessage(targetMessageId: String): Flow<List<LocalMessageReaction>>

    @Query("SELECT * FROM message_reactions WHERE target_message_id = :targetMessageId")
    suspend fun getReactionsForMessageList(targetMessageId: String): List<LocalMessageReaction>

    @Query("SELECT * FROM message_reactions WHERE target_message_id = :targetMessageId AND sender_id = :senderId LIMIT 1")
    suspend fun getReaction(targetMessageId: String, senderId: String): LocalMessageReaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateReaction(reaction: LocalMessageReaction)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReactionIgnore(reaction: LocalMessageReaction)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReactionsIgnore(reactions: List<LocalMessageReaction>)

    @Query("DELETE FROM message_reactions WHERE target_message_id = :targetMessageId AND sender_id = :senderId")
    suspend fun deleteReaction(targetMessageId: String, senderId: String)

    @Query("DELETE FROM message_reactions WHERE target_message_id = :targetMessageId")
    suspend fun deleteReactionsForMessage(targetMessageId: String)

    @Query("DELETE FROM message_reactions WHERE group_id = :groupId")
    suspend fun deleteReactionsForGroup(groupId: String)

    @Query("DELETE FROM message_reactions WHERE target_message_id NOT IN (SELECT id FROM group_messages)")
    suspend fun deleteOrphanReactions()

    @Query("DELETE FROM message_reactions")
    suspend fun deleteAllReactions()
}

@Dao
interface MessageTranslationDao {
    @Query("SELECT * FROM message_translations WHERE message_id = :messageId LIMIT 1")
    fun getTranslationFlow(messageId: String): Flow<LocalMessageTranslation?>

    @Query("SELECT * FROM message_translations WHERE message_id = :messageId LIMIT 1")
    suspend fun getTranslation(messageId: String): LocalMessageTranslation?

    @Query("SELECT * FROM message_translations")
    fun getAllTranslationsFlow(): Flow<List<LocalMessageTranslation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTranslation(translation: LocalMessageTranslation)

    @Query("DELETE FROM message_translations WHERE message_id = :messageId")
    suspend fun deleteTranslation(messageId: String)

    @Query("DELETE FROM message_translations")
    suspend fun deleteAllTranslations()
}

