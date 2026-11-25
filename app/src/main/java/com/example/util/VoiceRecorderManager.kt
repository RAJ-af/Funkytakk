package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

data class VoiceRecordingResult(
    val file: File,
    val durationSeconds: Int,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VoiceRecordingResult
        if (file != other.file) return false
        if (durationSeconds != other.durationSeconds) return false
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + durationSeconds
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

object VoiceRecorderManager {
    private const val TAG = "VoiceRecorderManager"
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L

    fun isRecording(): Boolean = mediaRecorder != null

    fun startRecording(context: Context): Boolean {
        return try {
            stopRecording() // Clean up any active session

            val file = File(context.cacheDir, "voice_rec_${System.currentTimeMillis()}.m4a")
            outputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            startTimeMs = System.currentTimeMillis()
            Log.d(TAG, "Started recording audio")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start voice recording", e)
            cancelRecording()
            false
        }
    }

    fun stopRecording(): VoiceRecordingResult? {
        val recorder = mediaRecorder ?: return null
        val file = outputFile ?: return null
        val durationSecs = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt().coerceAtLeast(1)

        return try {
            recorder.stop()
            recorder.release()
            mediaRecorder = null
            outputFile = null

            if (file.exists() && file.length() > 0) {
                val bytes = file.readBytes()
                Log.d(TAG, "Stopped voice recording successfully: size=${bytes.size}, duration=${durationSecs}s")
                VoiceRecordingResult(file, durationSecs, bytes)
            } else {
                file.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping voice recorder", e)
            try { recorder.release() } catch (_: Exception) {}
            mediaRecorder = null
            file.delete()
            outputFile = null
            null
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {}
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        outputFile?.delete()
        outputFile = null
        Log.d(TAG, "Voice recording cancelled and discarded")
    }
}
