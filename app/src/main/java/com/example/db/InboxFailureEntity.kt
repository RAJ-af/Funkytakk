package com.example.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "inbox_failures")
data class InboxFailureEntity(
    @PrimaryKey
    val messageId: String,
    val type: String, // "DM" or "GROUP"
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface InboxFailureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFailure(failure: InboxFailureEntity)

    @Query("SELECT COUNT(*) FROM inbox_failures")
    fun getFailureCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM inbox_failures")
    suspend fun getFailureCount(): Int

    @Query("DELETE FROM inbox_failures WHERE messageId = :messageId")
    suspend fun deleteFailure(messageId: String)

    @Query("DELETE FROM inbox_failures")
    suspend fun clearAllFailures()

    @Query("SELECT * FROM inbox_failures ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastFailure(): InboxFailureEntity?
}
