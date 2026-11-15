package com.example

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Single source of truth for the currently authenticated Supabase Auth UUID (`profiles.auth_user_id`).
 * All local databases, E2EE Tink keysets, chat media, vaults, backup prefs, and user caches are strictly
 * scoped by this UID so Account A's local state is never visible or reused by Account B.
 */
object ActiveUserManager {
    private val _activeUid = MutableStateFlow<String?>(null)
    val activeUid: StateFlow<String?> = _activeUid.asStateFlow()

    fun setActiveUid(uid: String?) {
        val clean = uid?.trim()?.takeIf { it.isNotBlank() }
        _activeUid.value = clean
    }

    fun clear() {
        _activeUid.value = null
    }

    fun getActiveUid(context: Context? = null): String? {
        val current = _activeUid.value?.takeIf { it.isNotBlank() }
        if (current != null) return current
        if (context != null) {
            val fromSession = SupabaseSessionManager.getUserId(context)?.takeIf { it.isNotBlank() }
            if (fromSession != null) {
                _activeUid.value = fromSession
                return fromSession
            }
        }
        return null
    }

    fun requireActiveUid(context: Context? = null): String {
        return getActiveUid(context)
            ?: throw IllegalStateException("No active authenticated user UID available")
    }

    fun sanitizeUid(uid: String): String {
        return uid.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    fun dbName(uid: String): String = "chat_${sanitizeUid(uid)}.db"

    fun e2eePrefsName(uid: String): String = "e2ee_tink_prefs_${sanitizeUid(uid)}"
    fun e2eeKeysetName(uid: String): String = "e2ee_hpke_private_keyset_${sanitizeUid(uid)}"
    fun e2eeMasterKeyAlias(uid: String): String = "e2ee_hpke_master_key_${sanitizeUid(uid)}"
    fun e2eeMasterKeyUri(uid: String): String = "android-keystore://${e2eeMasterKeyAlias(uid)}"

    fun vaultPrefsName(uid: String): String = "vault_envelope_prefs_${sanitizeUid(uid)}"
    fun vaultBiometricAlias(uid: String): String = "vault_biometric_master_key_${sanitizeUid(uid)}"

    fun backupKeystorePrefsName(uid: String): String = "funkytalk_bk_keystore_prefs_${sanitizeUid(uid)}"
    fun backupKeystoreAlias(uid: String): String = "funkytalk_bk_keystore_alias_${sanitizeUid(uid)}"

    fun backupPrefsName(uid: String): String = "chat_backup_prefs_${sanitizeUid(uid)}"
    fun backupWrapperKeyAlias(uid: String): String = "vault_password_wrapper_key_${sanitizeUid(uid)}"

    fun chatPrefsName(uid: String): String = "funkytalk_chat_prefs_${sanitizeUid(uid)}"
    fun supportPrefsName(uid: String): String = "funkytalk_support_prefs_${sanitizeUid(uid)}"
    fun cloudAiPrefsName(uid: String): String = "funkytalk_cloud_ai_prefs_${sanitizeUid(uid)}"

    fun mediaDir(context: Context, uid: String): File {
        return File(context.filesDir, "encrypted_vault_media/${sanitizeUid(uid)}")
    }

    fun restoredMediaDir(context: Context, uid: String): File {
        return File(context.filesDir, "media/${sanitizeUid(uid)}")
    }
}
