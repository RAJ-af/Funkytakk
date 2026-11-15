package com.example

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.WorkManager
import coil.Coil
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.security.KeyStore

/**
 * One-time migration from legacy unscoped local storage (DB, Tink keysets, vaults, media, prefs, Keystore aliases)
 * to UID-scoped isolation (`schema_scope_version = 2`).
 */
object LegacySecurityMigration {
    private const val TAG = "LegacySecurityMigration"
    private const val GLOBAL_PREFS_NAME = "funky_global_migration_prefs"
    private const val KEY_SCHEMA_SCOPE_VERSION = "schema_scope_version"
    private const val KEY_PENDING_RESET_DIALOG = "pending_security_reset_dialog"
    private const val TARGET_SCHEMA_SCOPE_VERSION = 2

    private const val LEGACY_DB_NAME = "encrypted_dm_chat_vault.db"

    private val LEGACY_PREFS = listOf(
        "e2ee_tink_prefs",
        "vault_envelope_prefs",
        "funkytalk_bk_keystore_prefs",
        "chat_backup_prefs",
        "funkytalk_chat_prefs",
        "funkytalk_support_prefs",
        "funkytalk_cloud_ai_prefs",
        "supabase_session_prefs",
        "account_deletion_prefs"
    )

    private val LEGACY_KEYSTORE_ALIASES = listOf(
        "e2ee_hpke_master_key",
        "vault_biometric_master_key",
        "funkytalk_bk_keystore_alias",
        "vault_password_wrapper_key"
    )

    fun runIfNeeded(context: Context) {
        val appCtx = context.applicationContext
        val globalPrefs = appCtx.getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE)
        val currentVersion = globalPrefs.getInt(KEY_SCHEMA_SCOPE_VERSION, 0)
        if (currentVersion >= TARGET_SCHEMA_SCOPE_VERSION) return

        val hadLegacyData = detectLegacyData(appCtx)
        Log.i(TAG, "Running legacy security scope migration v$TARGET_SCHEMA_SCOPE_VERSION (hadLegacyData=$hadLegacyData)")

        // 1. Delete legacy unscoped SQLCipher database & sidecar files
        try {
            val dbFile = appCtx.getDatabasePath(LEGACY_DB_NAME)
            appCtx.deleteDatabase(LEGACY_DB_NAME)
            listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
                val f = File("${dbFile.absolutePath}$suffix")
                if (f.exists()) f.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting legacy DB", e)
        }

        // 2. Delete legacy unscoped encrypted media & restored media directories
        try {
            val legacyMediaDir = File(appCtx.filesDir, "encrypted_vault_media")
            if (legacyMediaDir.exists()) legacyMediaDir.deleteRecursively()
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting legacy encrypted_vault_media", e)
        }
        try {
            val legacyRestoredMediaDir = File(appCtx.filesDir, "media")
            if (legacyRestoredMediaDir.exists()) legacyRestoredMediaDir.deleteRecursively()
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting legacy media dir", e)
        }

        // 3. Delete legacy unscoped SharedPreferences files
        LEGACY_PREFS.forEach { prefsName ->
            deleteSharedPrefsCompat(appCtx, prefsName)
        }

        // 4. Delete legacy unscoped AndroidKeyStore aliases
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            LEGACY_KEYSTORE_ALIASES.forEach { alias ->
                try {
                    if (ks.containsAlias(alias)) ks.deleteEntry(alias)
                } catch (e: Exception) {
                    Log.w(TAG, "Error deleting KeyStore alias $alias", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error accessing AndroidKeyStore during migration", e)
        }

        // 5. Cancel any pending legacy WorkManager backup/restore tasks
        try {
            WorkManager.getInstance(appCtx).cancelAllWork()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling WorkManager jobs during migration", e)
        }

        // 6. Sign out any legacy Firebase session & clear Coil caches
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        ActiveUserManager.clear()
        try {
            Coil.imageLoader(appCtx).memoryCache?.clear()
            Coil.imageLoader(appCtx).diskCache?.clear()
        } catch (_: Exception) {}

        // 7. Record schema_scope_version = 2 and one-time notice if user had prior legacy data
        globalPrefs.edit()
            .putInt(KEY_SCHEMA_SCOPE_VERSION, TARGET_SCHEMA_SCOPE_VERSION)
            .putBoolean(KEY_PENDING_RESET_DIALOG, hadLegacyData)
            .commit()
    }

    fun shouldShowSecurityResetDialog(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PENDING_RESET_DIALOG, false)
    }

    fun dismissSecurityResetDialog(context: Context) {
        context.applicationContext.getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PENDING_RESET_DIALOG, false)
            .apply()
    }

    private fun detectLegacyData(context: Context): Boolean {
        if (context.getDatabasePath(LEGACY_DB_NAME).exists()) return true
        if (File(context.filesDir, "encrypted_vault_media").exists()) return true
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        if (sharedPrefsDir.exists()) {
            val triggerNames = listOf(
                "e2ee_tink_prefs.xml",
                "vault_envelope_prefs.xml",
                "supabase_session_prefs.xml",
                "funkytalk_chat_prefs.xml",
                "chat_backup_prefs.xml"
            )
            if (triggerNames.any { File(sharedPrefsDir, it).exists() }) return true
        }
        return false
    }

    fun deleteSharedPrefsCompat(context: Context, prefsName: String) {
        try {
            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(prefsName)
            } else {
                val file = File(File(context.applicationInfo.dataDir, "shared_prefs"), "$prefsName.xml")
                if (file.exists()) file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing SharedPreferences $prefsName", e)
        }
    }
}
