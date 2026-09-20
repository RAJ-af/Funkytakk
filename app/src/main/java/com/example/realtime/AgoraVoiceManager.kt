package com.example.realtime

import android.util.Log

object AgoraVoiceManager {
    private var rtcEngine: Any? = null

    fun setRtcEngine(engine: Any?) {
        rtcEngine = engine
        Log.d("AgoraVoiceManager", "RtcEngine set: $engine")
    }

    fun muteLocalAudioStream(muted: Boolean) {
        Log.d("AgoraVoiceManager", "muteLocalAudioStream called: muted=$muted")
        try {
            rtcEngine?.let { engine ->
                val method = engine.javaClass.getMethod("muteLocalAudioStream", Boolean::class.javaPrimitiveType)
                method.invoke(engine, muted)
            }
        } catch (e: Exception) {
            Log.e("AgoraVoiceManager", "Error executing muteLocalAudioStream", e)
        }
    }
}
