package com.example.service

import android.util.Log
import com.example.repository.EncryptedChatRepository
import com.example.repository.GroupChatRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FunkyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FunkyFCMService"
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken received: $token")
        serviceScope.launch {
            PushNotificationManager.registerToken(applicationContext, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "onMessageReceived from: ${remoteMessage.from}, data: ${remoteMessage.data}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"]
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"]
        val data = remoteMessage.data

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
