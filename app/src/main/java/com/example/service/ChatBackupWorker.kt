package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.crypto.DriveBackupEngine
import com.example.util.ChatBackupPreferencesManager

class ChatBackupWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ChatBackupWorker"
        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_PROGRESS_TEXT = "progress_text"
        const val KEY_ERROR_MESSAGE = "error_message"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting ChatBackupWorker task...")

        val autoFreq = ChatBackupPreferencesManager.getAutoBackupFrequency(appContext)
        val isManual = isStopped || runAttemptCount == 0

        if (!isManual && autoFreq == "OFF") {
            Log.d(TAG, "Auto-backup is disabled (OFF). Skipping worker run.")
            return Result.success()
        }

        return try {
            val success = DriveBackupEngine.performBackup(
                context = appContext,
                onProgress = { percent, text ->
                    setProgressAsync(
                        workDataOf(
                            KEY_PROGRESS_PERCENT to percent,
                            KEY_PROGRESS_TEXT to text
                        )
                    )
                },
                isCancelled = { isStopped }
            )

            if (success) {
                Log.d(TAG, "ChatBackupWorker finished successfully.")
                Result.success()
            } else {
                Log.w(TAG, "ChatBackupWorker was cancelled or stopped.")
                Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Backup cancelled."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "ChatBackupWorker failed with exception", e)
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "Backup failed")))
        }
    }
}
