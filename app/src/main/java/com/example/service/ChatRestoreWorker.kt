package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.R
import com.example.crypto.DriveBackupEngine
import com.example.util.ChatBackupPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RestoreProgressState(
    val isRestoring: Boolean = false,
    val percent: Int = 0,
    val statusText: String = "",
    val isComplete: Boolean = false,
    val error: String? = null
)

object RestoreProgressTracker {
    private val _progress = MutableStateFlow(RestoreProgressState())
    val progress: StateFlow<RestoreProgressState> = _progress.asStateFlow()

    fun update(percent: Int, statusText: String) {
        _progress.value = RestoreProgressState(
            isRestoring = true,
            percent = percent,
            statusText = statusText,
            isComplete = false,
            error = null
        )
    }

    fun complete() {
        _progress.value = RestoreProgressState(
            isRestoring = false,
            percent = 100,
            statusText = "Restore complete!",
            isComplete = true,
            error = null
        )
    }

    fun fail(error: String) {
        _progress.value = RestoreProgressState(
            isRestoring = false,
            percent = 0,
            statusText = "Restore failed",
            isComplete = false,
            error = error
        )
    }

    fun reset() {
        _progress.value = RestoreProgressState()
    }
}

class ChatRestoreWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ChatRestoreWorker"
        const val UNIQUE_WORK_NAME = "funkytalk_chat_restore_work"
        const val KEY_VAULT_PASSWORD = "vault_password"
        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_PROGRESS_TEXT = "progress_text"
        const val KEY_ERROR = "error_message"

        private const val NOTIFICATION_CHANNEL_ID = "chat_restore_channel"
        private const val NOTIFICATION_ID = 202611

        fun enqueueRestore(context: Context, vaultPassword: String) {
            val workData = workDataOf(KEY_VAULT_PASSWORD to vaultPassword)
            val request = OneTimeWorkRequestBuilder<ChatRestoreWorker>()
                .setInputData(workData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting ChatRestoreWorker...")

        val vaultPassword = inputData.getString(KEY_VAULT_PASSWORD)
            ?: ChatBackupPreferencesManager.getCachedVaultPassword(appContext)

        if (vaultPassword.isNullOrBlank()) {
            Log.e(TAG, "No vault password provided for restore")
            RestoreProgressTracker.fail("Vault password is required to decrypt backup.")
            return Result.failure(workDataOf(KEY_ERROR to "Missing vault password"))
        }

        return try {
            // Start as Foreground Service type dataSync
            setForeground(createForegroundInfo(0))
            RestoreProgressTracker.update(0, "Connecting to Google Drive...")

            val success = DriveBackupEngine.performRestore(
                context = appContext,
                vaultPassword = vaultPassword,
                onProgress = { percent, text ->
                    RestoreProgressTracker.update(percent, text)
                    setProgressAsync(
                        workDataOf(
                            KEY_PROGRESS_PERCENT to percent,
                            KEY_PROGRESS_TEXT to text
                        )
                    )
                    // Update notification with "Restoring chats $percent%"
                    try {
                        val notification = buildNotification(percent)
                        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not update notification", e)
                    }
                }
            )

            if (success) {
                RestoreProgressTracker.complete()
                Result.success()
            } else {
                RestoreProgressTracker.fail("Restore cancelled or incomplete.")
                Result.failure(workDataOf(KEY_ERROR to "Restore incomplete"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "ChatRestoreWorker failed", e)
            val errorMsg = e.message ?: "Unknown restore error"
            RestoreProgressTracker.fail(errorMsg)
            Result.failure(workDataOf(KEY_ERROR to errorMsg))
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(0)
    }

    private fun createForegroundInfo(percent: Int): ForegroundInfo {
        createNotificationChannel()
        val notification = buildNotification(percent)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(percent: Int): Notification {
        return NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Restoring chats")
            .setContentText("Restoring chats $percent%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Chat Restore",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows chat restore progress notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
