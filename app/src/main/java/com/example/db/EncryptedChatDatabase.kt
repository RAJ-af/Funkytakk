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
        LocalDmMessageReaction::class,
        LocalGroupConversation::class,
        LocalGroupMessage::class,
        LocalMessageReaction::class,
        LocalMessageTranslation::class,
        InboxFailureEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class EncryptedChatDatabase : RoomDatabase() {
    abstract fun conversationDao(): DmConversationDao
    abstract fun messageDao(): DmMessageDao
    abstract fun dmMessageReactionDao(): DmMessageReactionDao
    abstract fun groupConversationDao(): GroupConversationDao
    abstract fun groupMessageDao(): GroupMessageDao
    abstract fun messageReactionDao(): MessageReactionDao
    abstract fun messageTranslationDao(): MessageTranslationDao
    abstract fun inboxFailureDao(): InboxFailureDao

    companion object {
        @Volatile
        private var INSTANCE: EncryptedChatDatabase? = null

        @Volatile
        private var currentUid: String? = null

        fun getInstance(context: Context, vaultKey: ByteArray, uid: String? = null): EncryptedChatDatabase {
            val effectiveUid = uid?.trim()?.takeIf { it.isNotBlank() }
                ?: com.example.ActiveUserManager.getActiveUid(context)
                ?: throw IllegalStateException("Active user UID is required to open chat database")

            val existing = INSTANCE
            if (existing != null && currentUid == effectiveUid) {
                return existing
            }

            return synchronized(this) {
                val doubleCheck = INSTANCE
                if (doubleCheck != null && currentUid == effectiveUid) {
                    doubleCheck
                } else {
                    if (doubleCheck != null) {
                        try {
                            doubleCheck.close()
                        } catch (_: Exception) {}
                        INSTANCE = null
                        currentUid = null
                    }
                    buildDatabase(context, vaultKey, effectiveUid).also {
                        INSTANCE = it
                        currentUid = effectiveUid
                    }
                }
            }
        }

        fun closeInstance() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (_: Exception) {}
                INSTANCE = null
                currentUid = null
            }
        }

        fun buildDatabase(context: Context, vaultKey: ByteArray, uid: String? = null): EncryptedChatDatabase {
            val effectiveUid = uid?.trim()?.takeIf { it.isNotBlank() }
                ?: com.example.ActiveUserManager.requireActiveUid(context)
            SQLiteDatabase.loadLibs(context)
            val openHelperFactory = SupportFactory(vaultKey)
            return Room.databaseBuilder(
                context.applicationContext,
                EncryptedChatDatabase::class.java,
                com.example.ActiveUserManager.dbName(effectiveUid)
            )
                .openHelperFactory(openHelperFactory)
                .fallbackToDestructiveMigration()
                .build()
        }

        fun deleteDatabase(context: Context, uid: String? = null): Boolean {
            val effectiveUid = uid?.trim()?.takeIf { it.isNotBlank() }
                ?: com.example.ActiveUserManager.getActiveUid(context)
            if (effectiveUid == null) {
                closeInstance()
                return false
            }
            return deleteDatabaseForUid(context, effectiveUid)
        }

        fun deleteDatabaseForUid(context: Context, uid: String): Boolean {
            if (uid.isBlank()) return false
            synchronized(this) {
                if (currentUid == uid) {
                    try {
                        INSTANCE?.close()
                    } catch (_: Exception) {}
                    INSTANCE = null
                    currentUid = null
                }
            }
            val dbName = com.example.ActiveUserManager.dbName(uid)
            val dbFile = context.getDatabasePath(dbName)
            val deleted = context.deleteDatabase(dbName)
            listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
                val sidecar = java.io.File("${dbFile.absolutePath}$suffix")
                if (sidecar.exists()) sidecar.delete()
            }
            return deleted
        }
    }
}
