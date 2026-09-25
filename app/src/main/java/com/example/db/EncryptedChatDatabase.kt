package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [LocalDmConversation::class, LocalDmMessage::class],
    version = 1,
    exportSchema = false
)
abstract class EncryptedChatDatabase : RoomDatabase() {
    abstract fun conversationDao(): DmConversationDao
    abstract fun messageDao(): DmMessageDao

    companion object {
        const val DB_NAME = "encrypted_dm_chat_vault.db"

        fun buildDatabase(context: Context, vaultKey: ByteArray): EncryptedChatDatabase {
            SQLiteDatabase.loadLibs(context)
            val openHelperFactory = SupportFactory(vaultKey)
            return Room.databaseBuilder(
                context.applicationContext,
                EncryptedChatDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(openHelperFactory)
                .fallbackToDestructiveMigration()
                .build()
        }

        fun deleteDatabase(context: Context): Boolean {
            return context.deleteDatabase(DB_NAME)
        }
    }
}
