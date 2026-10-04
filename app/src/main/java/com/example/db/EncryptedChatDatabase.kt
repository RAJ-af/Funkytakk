package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        LocalDmConversation::class,
        LocalDmMessage::class,
        LocalGroupConversation::class,
        LocalGroupMessage::class
    ],
    version = 3,
    exportSchema = false
)
abstract class EncryptedChatDatabase : RoomDatabase() {
    abstract fun conversationDao(): DmConversationDao
    abstract fun messageDao(): DmMessageDao
    abstract fun groupConversationDao(): GroupConversationDao
    abstract fun groupMessageDao(): GroupMessageDao

    companion object {
        const val DB_NAME = "encrypted_dm_chat_vault.db"

        @Volatile
        private var INSTANCE: EncryptedChatDatabase? = null

        fun getInstance(context: Context, vaultKey: ByteArray): EncryptedChatDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, vaultKey).also { INSTANCE = it }
            }
        }

        fun closeInstance() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (_: Exception) {}
                INSTANCE = null
            }
        }

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
            closeInstance()
            return context.deleteDatabase(DB_NAME)
        }
    }
}
