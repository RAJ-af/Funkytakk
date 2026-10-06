package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.PushTokenRequest
import com.example.R
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.crypto.VaultSecurityManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

object PushNotificationManager {
    private const val TAG = "PushNotificationManager"
    const val CHANNEL_ID_MESSAGES = "messages"
    private const val CHANNEL_NAME_MESSAGES = "Messages & Chats"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                CHANNEL_NAME_MESSAGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications for direct and group chat messages"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    suspend fun registerCurrentToken(context: Context) = withContext(Dispatchers.IO) {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val token = task.result
                    Log.d(TAG, "Fetched FCM token: $token")
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        registerToken(context, token)
                    }
                } else {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating FCM token fetch", e)
        }
    }

    suspend fun registerToken(context: Context, token: String): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext false
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val authToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: (SupabaseSessionManager.getAccessToken(context)?.let { "Bearer $it" })
                ?: return@withContext false

            val resp = SupabaseClient.api.registerPushToken(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = authToken,
                body = PushTokenRequest(p_token = token)
            )
            if (resp.isSuccessful) {
                Log.d(TAG, "Successfully registered push token on server")
                return@withContext true
            } else {
                Log.w(TAG, "Failed registering push token: code=${resp.code()} message=${resp.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering push token", e)
        }
        return@withContext false
    }

    suspend fun unregisterCurrentToken(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val authToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: (SupabaseSessionManager.getAccessToken(context)?.let { "Bearer $it" })

            var token: String? = null
            try {
                token = FirebaseMessaging.getInstance().token.result
            } catch (_: Exception) {}

            if (token != null && authToken != null) {
                SupabaseClient.api.unregisterPushToken(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = authToken,
                    body = PushTokenRequest(p_token = token)
                )
            }
            FirebaseMessaging.getInstance().deleteToken()
            Log.d(TAG, "Deleted FCM token and unregistered from server")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering push token", e)
        }
        return@withContext false
    }

    fun showPushNotification(
        context: Context,
        title: String?,
        body: String?,
        data: Map<String, String>
    ) {
        createNotificationChannels(context)

        val isVaultLocked = VaultSecurityManager.isVaultLocked()
        val displayTitle = if (isVaultLocked) "FunkyTalk" else (title?.takeIf { it.isNotBlank() } ?: "New message")
        val displayBody = if (isVaultLocked) "New encrypted message received • Tap to unlock" else (body?.takeIf { it.isNotBlank() } ?: "You received a new message")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data.forEach { (k, v) -> putExtra(k, v) }
            putExtra("from_push", true)
        }

        val requestCode = (System.currentTimeMillis() % 100000).toInt()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(displayTitle)
            .setContentText(displayBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = (data["conversation_id"] ?: data["group_id"] ?: UUID.randomUUID().toString()).hashCode()
        notificationManager?.notify(notificationId, notification)
    }
}
