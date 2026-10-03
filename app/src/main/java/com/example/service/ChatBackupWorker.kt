package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.BuildConfig
import com.example.RecordChatBackupRequest
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.crypto.ChatBackupArchiveManager
import com.example.crypto.ChatBackupCryptoManager
import com.example.crypto.VaultSecurityManager
import com.example.util.ChatBackupPreferencesManager
import com.example.util.GoogleDriveBackupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ChatBackupWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ChatBackupWorker"
        const val WORK_NAME = "funkytalk_chat_auto_backup"

        fun scheduleAutoBackup(context: Context) {
            val freq = ChatBackupPreferencesManager.getAutoBackupFrequency(context)
            if (freq == "OFF") {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                Log.d(TAG, "Cancelled auto backup work schedule (Frequency is OFF)")
                return
            }

            val wifiOnly = ChatBackupPreferencesManager.getWifiOnly(context)
            val networkConstraint = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkConstraint)
                .setRequiresBatteryNotLow(true)
                .build()

            val intervalDays = if (freq == "WEEKLY") 7L else 1L
            val workRequest = PeriodicWorkRequestBuilder<ChatBackupWorker>(intervalDays, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.d(TAG, "Scheduled periodic auto-backup every $intervalDays days (WiFi only: $wifiOnly)")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting auto-backup background execution...")

            val googleAccount = GoogleDriveBackupHelper.getSignedInAccount(appContext)
                ?: return@withContext Result.failure().also { Log.w(TAG, "No Google Drive account connected") }

            val token = GoogleDriveBackupHelper.fetchOAuthToken(appContext, googleAccount)
                ?: return@withContext Result.retry().also { Log.w(TAG, "Failed to fetch OAuth token for Drive") }

            val vaultPassword = ChatBackupPreferencesManager.getCachedVaultPassword(appContext)
                ?: return@withContext Result.failure().also { Log.w(TAG, "No cached vault password available for auto-backup") }

            val vaultKey = VaultSecurityManager.getActiveVaultKey()
            val includesMedia = ChatBackupPreferencesManager.getIncludeMedia(appContext)

            // 1. Pack archive
            val archiveBytes = ChatBackupArchiveManager.createArchive(appContext, includesMedia, vaultKey)

            // 2. Encrypt with AES-256-GCM + PBKDF2
            val encryptedBytes = ChatBackupCryptoManager.encryptBackup(vaultPassword, archiveBytes)

            // 3. Upload to Google Drive appDataFolder
            val uploadResult = GoogleDriveBackupHelper.uploadBackupFile(token, encryptedBytes)
            if (uploadResult.isFailure) {
                Log.e(TAG, "Failed to upload backup during background auto-backup", uploadResult.exceptionOrNull())
                return@withContext Result.retry()
            }

            val fileId = uploadResult.getOrNull()
            Log.d(TAG, "Auto-backup uploaded successfully. FileId: $fileId")

            // 4. Update local preferences
            val now = System.currentTimeMillis()
            ChatBackupPreferencesManager.setLastBackupTimestamp(appContext, now)
            ChatBackupPreferencesManager.setLastBackupSizeBytes(appContext, encryptedBytes.size.toLong())

            // 5. Notify Supabase RPC record_chat_backup
            val supaAuth = SupabaseSessionManager.getBearerToken(appContext)
            if (!supaAuth.isNullOrBlank()) {
                try {
                    val resp = SupabaseClient.api.recordChatBackup(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = supaAuth,
                        body = RecordChatBackupRequest(
                            p_size_bytes = encryptedBytes.size.toLong(),
                            p_format_version = 1,
                            p_includes_media = includesMedia,
                            p_key_version = 1
                        )
                    )
                    Log.d(TAG, "Recorded chat backup metadata on Supabase. Code: ${resp.code()}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error recording chat backup metadata on Supabase", e)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled exception in ChatBackupWorker", e)
            Result.failure()
        }
    }
}
