package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.util.UUID

/**
 * Provides a persistent unique device identifier and hardware model name.
 * The device ID is a persistent random UUID stored securely in preferences
 * and strictly validated against ^[A-Za-z0-9._-]{8,64}$.
 * It is never deleted during auth/session wipe.
 */
object DeviceInfoProvider {
    private const val TAG = "DeviceInfoProvider"
    private const val PREFS_FILE = "device_identity_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private val DEVICE_ID_REGEX = Regex("^[A-Za-z0-9._-]{8,64}$")

    @Volatile
    private var cachedDeviceId: String? = null

    fun getDeviceId(context: Context): String {
        cachedDeviceId?.let { if (isValidDeviceId(it)) return it }

        synchronized(this) {
            cachedDeviceId?.let { if (isValidDeviceId(it)) return it }

            val prefs = context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            var storedId = prefs.getString(KEY_DEVICE_ID, null)

            val finalId = if (!isValidDeviceId(storedId)) {
                val newId = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
                Log.d(TAG, "Generated new persistent deviceId")
                newId
            } else {
                storedId!!
            }

            cachedDeviceId = finalId
            return finalId
        }
    }

    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val combined = "$manufacturer $model".trim()
        return if (combined.isNotBlank()) combined else "Android Device"
    }

    private fun isValidDeviceId(id: String?): Boolean {
        if (id.isNullOrBlank()) return false
        return DEVICE_ID_REGEX.matches(id.trim())
    }
}
