package com.example.realtime

import android.util.Log

object AgoraVoiceManager {
    private const val TAG = "AgoraVoiceManager"
    const val CLIENT_ROLE_BROADCASTER = 1
    const val CLIENT_ROLE_AUDIENCE = 2

    private var rtcEngine: Any? = null

    fun setRtcEngine(engine: Any?) {
        rtcEngine = engine
        Log.d(TAG, "RtcEngine set: $engine")
    }

    fun getRtcEngine(): Any? = rtcEngine

    fun muteLocalAudioStream(muted: Boolean) {
        Log.d(TAG, "muteLocalAudioStream called: muted=$muted")
        try {
            rtcEngine?.let { engine ->
                val method = engine.javaClass.getMethod("muteLocalAudioStream", Boolean::class.javaPrimitiveType)
                method.invoke(engine, muted)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing muteLocalAudioStream", e)
        }
    }

    fun setClientRole(role: Int) {
        Log.d(TAG, "setClientRole called: role=$role")
        try {
            rtcEngine?.let { engine ->
                val method = engine.javaClass.getMethod("setClientRole", Int::class.javaPrimitiveType)
                method.invoke(engine, role)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing setClientRole", e)
        }
    }

    fun enableLocalAudio(enabled: Boolean) {
        Log.d(TAG, "enableLocalAudio called: enabled=$enabled")
        try {
            rtcEngine?.let { engine ->
                val method = engine.javaClass.getMethod("enableLocalAudio", Boolean::class.javaPrimitiveType)
                method.invoke(engine, enabled)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing enableLocalAudio", e)
        }
    }

    fun renewToken(token: String) {
        Log.d(TAG, "renewToken called (length=${token.length})")
        try {
            rtcEngine?.let { engine ->
                val method = engine.javaClass.getMethod("renewToken", String::class.java)
                method.invoke(engine, token)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing renewToken", e)
        }
    }
}
