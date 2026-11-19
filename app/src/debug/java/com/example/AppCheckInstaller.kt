package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckInstaller {
    private const val TAG = "AppCheckInstaller"

    fun install(context: Context) {
        try {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "Installed DebugAppCheckProviderFactory")
        } catch (e: Exception) {
            Log.w(TAG, "App Check debug install skipped: ${e.message}")
        }
    }
}
