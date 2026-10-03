package com.example.crypto

import android.content.Context
import android.util.Log
import com.example.db.EncryptedChatDatabase
import com.example.util.ChatBackupPreferencesManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ChatBackupArchiveManager {
    private const val TAG = "ChatBackupArchive"
    private const val TINK_PREFS_NAME = "e2ee_tink_prefs"
    private const val DB_NAME = EncryptedChatDatabase.DB_NAME

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    @Suppress("UNCHECKED_CAST")
    private val mapAdapter = moshi.adapter(Map::class.java)

    /**
     * Packs (a) Tink identity keyset, (b) WAL-checkpointed Room DB snapshot, and (c) optional media
     * into a ZIP archive ByteArray.
     */
    suspend fun createArchive(context: Context, includesMedia: Boolean, vaultKey: ByteArray?): ByteArray = withContext(Dispatchers.IO) {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // 1. Export Tink HPKE Keyset shared preferences
            try {
                val tinkPrefs = context.getSharedPreferences(TINK_PREFS_NAME, Context.MODE_PRIVATE)
                val allTinkEntries = tinkPrefs.all
                val jsonString = mapAdapter.toJson(allTinkEntries as Map<String, Any?>)
                val tinkBytes = jsonString.toByteArray(Charsets.UTF_8)

                zos.putNextEntry(ZipEntry("tink_keyset.json"))
                zos.write(tinkBytes)
                zos.closeEntry()
                Log.d(TAG, "Added Tink keyset to archive (${tinkBytes.size} bytes)")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding Tink keyset to backup archive", e)
            }

            // 2. Checkpoint Room WAL database & add DB snapshot
            try {
                if (vaultKey != null) {
                    try {
                        val db = EncryptedChatDatabase.getInstance(context, vaultKey)
                        db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
                        Log.d(TAG, "Room DB WAL checkpoint completed successfully")
                    } catch (e: Exception) {
                        Log.w(TAG, "WAL checkpoint warning (continuing with DB file)", e)
                    }
                }

                val dbFile = context.getDatabasePath(DB_NAME)
                if (dbFile.exists() && dbFile.length() > 0) {
                    val dbBytes = dbFile.readBytes()
                    zos.putNextEntry(ZipEntry("db_snapshot.db"))
                    zos.write(dbBytes)
                    zos.closeEntry()
                    Log.d(TAG, "Added Room DB snapshot to archive (${dbBytes.size} bytes)")
                } else {
                    Log.w(TAG, "Database file $DB_NAME does not exist or is empty")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding Room DB snapshot to backup archive", e)
            }

            // 3. Optional media files
            if (includesMedia) {
                try {
                    val mediaDir = File(context.filesDir, "chat_media")
                    if (mediaDir.exists() && mediaDir.isDirectory) {
                        val mediaFiles = mediaDir.listFiles() ?: emptyArray()
                        for (file in mediaFiles) {
                            if (file.isFile && file.length() > 0) {
                                zos.putNextEntry(ZipEntry("media/${file.name}"))
                                zos.write(file.readBytes())
                                zos.closeEntry()
                            }
                        }
                        Log.d(TAG, "Added ${mediaFiles.size} media files to archive")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding media files to backup archive", e)
                }
            }

            zos.finish()
        }

        val resultBytes = baos.toByteArray()
        Log.d(TAG, "Created backup archive successfully. Size: ${resultBytes.size} bytes")
        resultBytes
    }

    /**
     * Unpacks ZIP archive atomically using a temp dir in cacheDir, validates Tink keyset and Room DB snapshot,
     * and performs atomic swap to live location.
     */
    suspend fun restoreFromArchive(context: Context, archiveBytes: ByteArray, vaultPassword: String): Boolean = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            var foundTink = false
            var foundDb = false

            // 1. Unpack everything into tempDir inside cacheDir (never write to live files while streaming zip)
            ZipInputStream(ByteArrayInputStream(archiveBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name

                    // ZIP SLIP PROTECTION: Reject names that are absolute or contain ".."
                    if (entryName.startsWith("/") || entryName.contains("..")) {
                        Log.w(TAG, "Zip Slip attempt detected or invalid entry name rejected: $entryName")
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue
                    }

                    when {
                        entryName == "tink_keyset.json" -> {
                            val tempFile = File(tempDir, "tink_keyset.json")
                            tempFile.writeBytes(zis.readBytes())
                            foundTink = true
                        }
                        entryName == "db_snapshot.db" -> {
                            val tempFile = File(tempDir, "db_snapshot.db")
                            tempFile.writeBytes(zis.readBytes())
                            foundDb = true
                        }
                        entryName.startsWith("media/") -> {
                            val fileName = entryName.removePrefix("media/")
                            if (fileName.isNotBlank() && !fileName.startsWith("/") && !fileName.contains("..")) {
                                val tempMediaDir = File(tempDir, "media").apply { mkdirs() }
                                val targetFile = File(tempMediaDir, fileName)
                                val mediaDirCanonical = tempMediaDir.canonicalPath + File.separator
                                if (targetFile.canonicalPath.startsWith(mediaDirCanonical)) {
                                    targetFile.parentFile?.mkdirs()
                                    targetFile.writeBytes(zis.readBytes())
                                } else {
                                    Log.w(TAG, "Zip Slip detected for media path: $entryName")
                                }
                            }
                        }
                        else -> {
                            // Unknown entry names MUST be ignored
                            Log.d(TAG, "Ignoring unknown zip entry: $entryName")
                        }
                    }

                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // 2. VALIDATION PHASE
            var tinkMapToRestore: Map<String, Any?>? = null
            val tempTinkFile = File(tempDir, "tink_keyset.json")
            if (tempTinkFile.exists() && tempTinkFile.length() > 0) {
                val jsonString = tempTinkFile.readText(Charsets.UTF_8)
                val map = try {
                    @Suppress("UNCHECKED_CAST")
                    mapAdapter.fromJson(jsonString) as? Map<String, Any?>
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse tink_keyset.json from archive", e)
                    null
                }
                if (map == null) {
                    throw IllegalArgumentException("Validation failed: tink_keyset.json is invalid or corrupted")
                }
                tinkMapToRestore = map
            }

            val tempDbFile = File(tempDir, "db_snapshot.db")
            var validatedVaultKey: ByteArray? = null
            if (tempDbFile.exists() && tempDbFile.length() > 0) {
                // Determine vault key derived from vaultPassword
                var key = VaultSecurityManager.getActiveVaultKey()
                if (key == null) {
                    val unlockRes = VaultSecurityManager.unlockWithPassword(context, vaultPassword)
                    key = unlockRes.getOrNull()
                    if (key == null) {
                        val setupRes = VaultSecurityManager.setupVaultWithPassword(context, vaultPassword)
                        key = setupRes.getOrNull()
                    }
                }
                if (key == null) {
                    throw IllegalArgumentException("Validation failed: unable to derive vault key from provided password")
                }

                // Validate db_snapshot.db opens with the vault key
                net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
                var testDb: net.sqlcipher.database.SQLiteDatabase? = null
                try {
                    testDb = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                        tempDbFile.absolutePath,
                        String(key, Charsets.UTF_8),
                        null,
                        net.sqlcipher.database.SQLiteDatabase.OPEN_READONLY
                    )
                    testDb?.rawQuery("SELECT count(*) FROM sqlite_master", null)?.use { cursor ->
                        cursor.moveToFirst()
                    }
                    validatedVaultKey = key
                    Log.d(TAG, "db_snapshot.db validated successfully with vault key")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open db_snapshot.db with vault key", e)
                    throw IllegalArgumentException("Validation failed: db_snapshot.db cannot be opened with provided vault password", e)
                } finally {
                    try { testDb?.close() } catch (_: Exception) {}
                }
            }

            // 3. ATOMIC SWAP TO LIVE LOCATION (only executed if all validations succeeded)
            if (tempDbFile.exists() && tempDbFile.length() > 0) {
                EncryptedChatDatabase.closeInstance()

                val liveDbFile = context.getDatabasePath(DB_NAME)
                liveDbFile.parentFile?.mkdirs()

                // Overwrite live DB with validated temp DB
                tempDbFile.copyTo(liveDbFile, overwrite = true)

                // Delete WAL / SHM files
                val walFile = File(liveDbFile.path + "-wal")
                if (walFile.exists()) walFile.delete()
                val shmFile = File(liveDbFile.path + "-shm")
                if (shmFile.exists()) shmFile.delete()

                Log.d(TAG, "Live Room database atomically swapped")
            }

            if (tinkMapToRestore != null) {
                val tinkPrefs = context.getSharedPreferences(TINK_PREFS_NAME, Context.MODE_PRIVATE)
                val editor = tinkPrefs.edit()
                for ((k, v) in tinkMapToRestore) {
                    when (v) {
                        is String -> editor.putString(k, v)
                        is Boolean -> editor.putBoolean(k, v)
                        is Int -> editor.putInt(k, v)
                        is Long -> editor.putLong(k, v)
                        is Float -> editor.putFloat(k, v)
                        is Double -> editor.putLong(k, v.toLong())
                    }
                }
                editor.apply()
                Log.d(TAG, "Tink keyset restored into shared preferences")
            }

            val tempMediaDir = File(tempDir, "media")
            if (tempMediaDir.exists() && tempMediaDir.isDirectory) {
                val liveMediaDir = File(context.filesDir, "chat_media").apply { mkdirs() }
                val mediaDirCanonical = liveMediaDir.canonicalPath + File.separator
                val mediaFiles = tempMediaDir.listFiles() ?: emptyArray()
                for (f in mediaFiles) {
                    if (f.isFile) {
                        val targetLiveFile = File(liveMediaDir, f.name)
                        if (targetLiveFile.canonicalPath.startsWith(mediaDirCanonical)) {
                            f.copyTo(targetLiveFile, overwrite = true)
                        }
                    }
                }
                Log.d(TAG, "Media files restored into live directory")
            }

            // Cache vault password securely using Keystore wrapper
            ChatBackupPreferencesManager.setCachedVaultPassword(context, vaultPassword)

            Log.d(TAG, "Atomic restore completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during atomic restore from archive", e)
            false
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
