package com.example.service

import android.util.Log
import com.example.repository.EncryptedChatRepository
import com.example.repository.GroupChatRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FunkyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FunkyFCMService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        PushNotificationManager.createNotificationChannels(applicationContext)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken received (last6=${token.takeLast(6)})")
        PushNotificationManager.createNotificationChannels(applicationContext)
        serviceScope.launch {
            PushNotificationManager.registerToken(applicationContext, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        Log.d(TAG, "onMessageReceived from=${remoteMessage.from}, kind=${data["kind"]}, type=${data["message_type"]}")

        PushNotificationManager.createNotificationChannels(applicationContext)

        val title = remoteMessage.notification?.title ?: data["title"]
        val body = remoteMessage.notification?.body ?: data["body"]

        PushNotificationManager.showPushNotification(
            context = applicationContext,
            title = title,
            body = body,
            data = data
        )

        // Trigger inbox drain in background
        serviceScope.launch {
            try {
                EncryptedChatRepository.drainPendingDmMessages(applicationContext)
                GroupChatRepository.drainPendingGroupMessages(applicationContext)
            } catch (e: Exception) {
                Log.w(TAG, "Error draining inbox from push: ${e.message}")
            }
        }
    }
}
