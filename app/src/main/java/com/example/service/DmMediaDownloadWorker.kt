package com.example.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.repository.EncryptedChatRepository
import java.util.concurrent.TimeUnit

/**
 * Durable WorkManager worker that downloads and decrypts an incoming DM media payload
 * using the persisted Room descriptor (storagePath, mediaKey, mediaNonce) even if the user
 * closes the chat or restarts the app while the download was in progress or offline.
 */
class DmMediaDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val messageId = inputData.getString(KEY_MESSAGE_ID)?.trim().orEmpty()
        if (messageId.isBlank()) return Result.success()

        return when (val outcome = EncryptedChatRepository.performDmMediaDownload(applicationContext, messageId)) {
            is EncryptedChatRepository.DmMediaDownloadOutcome.Ready -> Result.success()
            is EncryptedChatRepository.DmMediaDownloadOutcome.Expired -> Result.success()
            is EncryptedChatRepository.DmMediaDownloadOutcome.NotFound -> Result.success()
            is EncryptedChatRepository.DmMediaDownloadOutcome.VaultLocked -> Result.retry()
            is EncryptedChatRepository.DmMediaDownloadOutcome.Retry -> {
                if (runAttemptCount >= MAX_WORK_ATTEMPTS) Result.failure() else Result.retry()
            }
        }
    }

    companion object {
        const val KEY_MESSAGE_ID = "message_id"
        private const val WORK_NAME_PREFIX = "dm_media_dl_"
        private const val MAX_WORK_ATTEMPTS = 4

        fun enqueue(context: Context, messageId: String, replaceExisting: Boolean = false) {
            if (messageId.isBlank()) return
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DmMediaDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_MESSAGE_ID to messageId))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10L, TimeUnit.SECONDS)
                .build()

            val policy = if (replaceExisting) {
                ExistingWorkPolicy.REPLACE
            } else {
                ExistingWorkPolicy.KEEP
            }

            runCatching {
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork("$WORK_NAME_PREFIX$messageId", policy, request)
            }
        }
    }
}
