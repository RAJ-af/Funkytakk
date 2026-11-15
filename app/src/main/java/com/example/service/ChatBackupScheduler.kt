package com.example.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ActiveUserManager
import com.example.util.ChatBackupPreferencesManager
import java.util.concurrent.TimeUnit

object ChatBackupScheduler {
    private const val PERIODIC_WORK_NAME = "funkytalk_periodic_chat_backup"
    private const val ONE_TIME_WORK_NAME = "funkytalk_one_time_chat_backup"

    fun updateBackupSchedule(context: Context) {
        if (ActiveUserManager.getActiveUid(context) == null) return
        val freq = ChatBackupPreferencesManager.getAutoBackupFrequency(context)
        val wifiOnly = ChatBackupPreferencesManager.getWifiOnly(context)

        val workManager = WorkManager.getInstance(context)

        if (freq == "OFF") {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val repeatIntervalDays = if (freq == "WEEKLY") 7L else 1L
        val flexIntervalHours = 4L

        val periodicRequest = PeriodicWorkRequestBuilder<ChatBackupWorker>(
            repeatIntervalDays, TimeUnit.DAYS,
            flexIntervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun triggerOneTimeBackup(context: Context) {
        if (ActiveUserManager.getActiveUid(context) == null) return
        val wifiOnly = ChatBackupPreferencesManager.getWifiOnly(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<ChatBackupWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    fun cancelBackup(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(ONE_TIME_WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    fun cancelAllWork(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }
}
