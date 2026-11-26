package com.example.data

import android.content.Context
import android.util.Log
import com.example.ActiveUserManager
import com.example.BuildConfig
import com.example.MomentDto
import com.example.crypto.VaultSecurityManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Device-local E2EE Saved Moments store.
 *
 * All saved moments are encrypted with AES-256-GCM using the user's active Vault key
 * ([VaultSecurityManager.getActiveVaultKey]). Because they are stored exclusively on-device
 * under this Vault key, uninstalling the app or losing/resetting the Vault password permanently
 * locks or wipes access to saved moments.
 */
object SavedMomentsStore {
    private const val TAG = "SavedMomentsStore"
    private const val FILE_PREFIX = "saved_moments_"
    private const val FILE_SUFFIX = ".enc"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    private val gson = Gson()
    private val listType = object : TypeToken<List<MomentDto>>() {}.type
    private val secureRandom = SecureRandom()

    private val _savedMoments = MutableStateFlow<List<MomentDto>>(emptyList())
    val savedMoments: StateFlow<List<MomentDto>> = _savedMoments.asStateFlow()

    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
    val savedIds: StateFlow<Set<String>> = _savedIds.asStateFlow()

    @Volatile
    private var loadedForUid: String? = null

    private fun storeFile(context: Context, uid: String): File {
        val safeUid = uid.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(context.applicationContext.filesDir, "$FILE_PREFIX$safeUid$FILE_SUFFIX")
    }

    internal fun encryptBytes(keyBytes: ByteArray, plainBytes: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val encrypted = cipher.doFinal(plainBytes)
        return ByteArray(GCM_IV_LENGTH + encrypted.size).apply {
            System.arraycopy(iv, 0, this, 0, GCM_IV_LENGTH)
            System.arraycopy(encrypted, 0, this, GCM_IV_LENGTH, encrypted.size)
        }
    }

    internal fun decryptBytes(keyBytes: ByteArray, encryptedBlob: ByteArray): ByteArray {
        require(encryptedBlob.size > GCM_IV_LENGTH) { "Invalid encrypted saved moments payload" }
        val iv = encryptedBlob.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encryptedBlob.copyOfRange(GCM_IV_LENGTH, encryptedBlob.size)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    internal fun serializeMoments(moments: List<MomentDto>): ByteArray {
        return gson.toJson(moments, listType).toByteArray(Charsets.UTF_8)
    }

    internal fun deserializeMoments(jsonBytes: ByteArray): List<MomentDto> {
        val json = String(jsonBytes, Charsets.UTF_8)
        if (json.isBlank()) return emptyList()
        return gson.fromJson<List<MomentDto>>(json, listType) ?: emptyList()
    }

    @Synchronized
    fun loadIfUnlocked(context: Context): List<MomentDto> {
        val key = VaultSecurityManager.getActiveVaultKey(context) ?: return emptyList()
        val uid = ActiveUserManager.getActiveUid(context) ?: return emptyList()
        val file = storeFile(context, uid)
        if (!file.exists()) {
            loadedForUid = uid
            _savedMoments.value = emptyList()
            _savedIds.value = emptySet()
            return emptyList()
        }
        return try {
            val decrypted = decryptBytes(key, file.readBytes())
            val list = deserializeMoments(decrypted)
            loadedForUid = uid
            _savedMoments.value = list
            _savedIds.value = list.map { it.id }.toSet()
            list
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Unable to decrypt saved moments for active vault: ${e.javaClass.simpleName}")
            }
            emptyList()
        }
    }

    /**
     * Toggles a moment in the E2EE saved moments store.
     * Returns `true` if saved, `false` if removed, or `null` if the Vault is currently locked.
     */
    @Synchronized
    fun toggleSave(context: Context, moment: MomentDto): Boolean? {
        val key = VaultSecurityManager.getActiveVaultKey(context) ?: return null
        val uid = ActiveUserManager.getActiveUid(context) ?: return null
        if (loadedForUid != uid) {
            loadIfUnlocked(context)
        }
        val current = _savedMoments.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.id == moment.id }
        val savedNow: Boolean
        if (existingIdx >= 0) {
            current.removeAt(existingIdx)
            savedNow = false
        } else {
            current.add(0, moment)
            savedNow = true
        }
        return if (persistLocked(context, uid, key, current)) savedNow else null
    }

    @Synchronized
    fun removeMoment(context: Context, momentId: String): Boolean {
        val key = VaultSecurityManager.getActiveVaultKey(context) ?: return false
        val uid = ActiveUserManager.getActiveUid(context) ?: return false
        if (loadedForUid != uid) {
            loadIfUnlocked(context)
        }
        val updated = _savedMoments.value.filterNot { it.id == momentId }
        return persistLocked(context, uid, key, updated)
    }

    private fun persistLocked(
        context: Context,
        uid: String,
        key: ByteArray,
        moments: List<MomentDto>
    ): Boolean {
        return try {
            val file = storeFile(context, uid)
            val tmp = File(file.parentFile, "${file.name}.tmp")
            val encrypted = encryptBytes(key, serializeMoments(moments))
            tmp.writeBytes(encrypted)
            if (file.exists()) file.delete()
            if (!tmp.renameTo(file)) {
                file.writeBytes(encrypted)
                tmp.delete()
            }
            loadedForUid = uid
            _savedMoments.value = moments
            _savedIds.value = moments.map { it.id }.toSet()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist encrypted saved moments", e)
            false
        }
    }

    @Synchronized
    fun clearMemoryCache() {
        loadedForUid = null
        _savedMoments.value = emptyList()
        _savedIds.value = emptySet()
    }

    @Synchronized
    fun wipeForUid(context: Context, uid: String) {
        if (uid.isBlank()) return
        if (loadedForUid == uid) {
            clearMemoryCache()
        }
        try {
            storeFile(context, uid).delete()
        } catch (_: Exception) {}
    }
}
