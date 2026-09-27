package com.example.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.BuildConfig
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object PresenceManager : DefaultLifecycleObserver {
    private const val TAG = "PresenceManager"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        // Observe process-level lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.d(TAG, "PresenceManager initialized and registered to ProcessLifecycleOwner")
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "App foregrounded - starting presence heartbeats")
        startHeartbeat()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "App backgrounded - stopping heartbeats and going offline")
        stopHeartbeat()
        goOffline()
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                sendHeartbeat()
                delay(60_000L) // repeating every 60 seconds
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun sendHeartbeat() {
        val context = appContext ?: return
        val userId = SupabaseSessionManager.getUserId(context) ?: return
        
        // Skip calling if user hasn't completed onboarding yet
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        val isOnboardingComplete = prefs.getBoolean("profile_onboarding_complete_$userId", false)
        if (!isOnboardingComplete) {
            Log.d(TAG, "Onboarding not complete for user: $userId, skipping heartbeat")
            return
        }

        scope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)
                    ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

                val response = SupabaseClient.api.heartbeat(auth = token)
                if (response.isSuccessful) {
                    Log.d(TAG, "Heartbeat sent successfully")
                } else {
                    Log.e(TAG, "Failed to send heartbeat: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending heartbeat: ${e.message}")
            }
        }
    }

    private fun goOffline() {
        val context = appContext ?: return
        val userId = SupabaseSessionManager.getUserId(context) ?: return

        // Skip calling if user hasn't completed onboarding yet
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        val isOnboardingComplete = prefs.getBoolean("profile_onboarding_complete_$userId", false)
        if (!isOnboardingComplete) {
            Log.d(TAG, "Onboarding not complete for user: $userId, skipping go_offline")
            return
        }

        scope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)
                    ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

                val response = SupabaseClient.api.goOffline(auth = token)
                if (response.isSuccessful) {
                    Log.d(TAG, "Set offline successfully")
                } else {
                    Log.e(TAG, "Failed to set offline: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting offline: ${e.message}")
            }
        }
    }
}
