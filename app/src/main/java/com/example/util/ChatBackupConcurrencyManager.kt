package com.example.util

import kotlinx.coroutines.sync.Mutex

object ChatBackupConcurrencyManager {
    /**
     * Singleton Mutex guarding the entire backup execution (UI manual backup and WorkManager worker).
     * Prevents simultaneous backup triggers from creating race conditions.
     */
    val backupMutex = Mutex()
}
