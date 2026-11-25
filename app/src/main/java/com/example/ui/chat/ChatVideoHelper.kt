package com.example.ui.chat

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

data class PreparedVideoMedia(
    val bytes: ByteArray,
    val durationSeconds: Int,
    val thumbnailBytes: ByteArray?,
    val mimeType: String = "video/mp4"
)

data class PreparedAudioFile(
    val bytes: ByteArray,
    val displayName: String,
    val durationSeconds: Int,
    val mimeType: String
)

/**
 * C1c media preparation helper:
 * - Compresses outgoing videos to 720p H.264/AAC via Media3 Transformer before E2EE upload.
 * - Enforces a strict 20 MB cap post-compression and guards ByteArray reads against low-RAM OOMs.
 * - Prepares OpenDocument audio attachments cleanly without broad storage permissions.
 */
object ChatVideoHelper {
    private const val TAG = "ChatVideoHelper"
    const val MAX_MEDIA_BYTES: Long = 20L * 1024L * 1024L
    const val MSG_VIDEO_TOO_LARGE = "Compressed video exceeds 20 MB limit. Please trim or choose a shorter clip."
    const val MSG_AUDIO_TOO_LARGE = "Audio file exceeds 20 MB limit. Please choose a smaller file."
    const val MSG_LOW_MEMORY = "File is too large for available memory. Try a shorter clip."

    /**
     * Checks whether allocating [byteCount] plus encryption overhead is likely to fit within VM heap headroom.
     */
    internal fun hasHeapHeadroomForBytes(
        byteCount: Long,
        freeHeapBytes: Long = computeApproxFreeHeapBytes(),
        deviceAvailMemBytes: Long = Long.MAX_VALUE
    ): Boolean {
        if (byteCount <= 0L) return true
        if (byteCount > MAX_MEDIA_BYTES) return false
        val neededHeap = byteCount * 2L + 10L * 1024L * 1024L
        if (freeHeapBytes < neededHeap) return false
        if (deviceAvailMemBytes < byteCount * 3L) return false
        return true
    }

    private fun computeApproxFreeHeapBytes(): Long {
        val rt = Runtime.getRuntime()
        val used = rt.totalMemory() - rt.freeMemory()
        return (rt.maxMemory() - used).coerceAtLeast(0L)
    }

    private fun readDeviceAvailMemBytes(context: Context): Long {
        return runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (am != null) {
                val mi = ActivityManager.MemoryInfo()
                am.getMemoryInfo(mi)
                if (mi.lowMemory) return@runCatching minOf(mi.availMem, 16L * 1024L * 1024L)
                return@runCatching mi.availMem
            }
            Long.MAX_VALUE
        }.getOrDefault(Long.MAX_VALUE)
    }

    suspend fun compressAndPrepareVideo(
        context: Context,
        uri: Uri
    ): Result<PreparedVideoMedia> {
        val appContext = context.applicationContext
        val tempDir = File(appContext.cacheDir, "video_prep").apply { mkdirs() }
        val outFile = File(tempDir, "vid_${System.currentTimeMillis()}.mp4")
        try {
            // 1. Extract duration and first-frame thumbnail before compression
            val meta = withContext(Dispatchers.IO) { extractVideoMetadata(appContext, uri) }

            // 2. Compress to 720p H.264/AAC using Media3 Transformer (must start on Main Looper)
            val transcodeResult = runTransformer720p(appContext, uri, outFile)
            val finalFile: File?
            var tempRawFallback: File? = null

            if (transcodeResult.isSuccess && outFile.exists() && outFile.length() > 0L) {
                finalFile = outFile
            } else {
                // Fallback only if original URI is already a reasonable <=20 MB clip
                val rawCopy = withContext(Dispatchers.IO) {
                    val f = File(tempDir, "vid_raw_${System.currentTimeMillis()}.mp4")
                    val copied = runCatching {
                        appContext.contentResolver.openInputStream(uri)?.use { input ->
                            f.outputStream().use { output -> input.copyTo(output) }
                            true
                        } ?: false
                    }.getOrDefault(false)
                    if (copied && f.exists() && f.length() in 1L..MAX_MEDIA_BYTES) f else {
                        f.delete()
                        null
                    }
                }
                if (rawCopy != null) {
                    tempRawFallback = rawCopy
                    finalFile = rawCopy
                } else {
                    val cause = transcodeResult.exceptionOrNull()
                    return Result.failure(
                        IllegalStateException(cause?.message ?: "Could not process selected video")
                    )
                }
            }

            return withContext(Dispatchers.IO) {
                try {
                    val sizeBytes = finalFile.length()
                    if (sizeBytes <= 0L) {
                        return@withContext Result.failure(IllegalStateException("Video file is empty"))
                    }
                    if (sizeBytes > MAX_MEDIA_BYTES) {
                        return@withContext Result.failure(IllegalStateException(MSG_VIDEO_TOO_LARGE))
                    }
                    val availMem = readDeviceAvailMemBytes(appContext)
                    if (!hasHeapHeadroomForBytes(sizeBytes, deviceAvailMemBytes = availMem)) {
                        return@withContext Result.failure(IllegalStateException(MSG_LOW_MEMORY))
                    }
                    val bytes = try {
                        finalFile.readBytes()
                    } catch (_: OutOfMemoryError) {
                        return@withContext Result.failure(IllegalStateException(MSG_LOW_MEMORY))
                    }
                    Result.success(
                        PreparedVideoMedia(
                            bytes = bytes,
                            durationSeconds = meta.first,
                            thumbnailBytes = meta.second,
                            mimeType = "video/mp4"
                        )
                    )
                } finally {
                    tempRawFallback?.delete()
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "compressAndPrepareVideo failed", e)
            return Result.failure(e)
        } finally {
            outFile.delete()
        }
    }

    private suspend fun runTransformer720p(
        context: Context,
        inputUri: Uri,
        outputFile: File
    ): Result<Unit> = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { cont ->
            var transformer: Transformer? = null
            try {
                val editedItem = EditedMediaItem.Builder(MediaItem.fromUri(inputUri))
                    .setEffects(
                        Effects(
                            emptyList(),
                            listOf(Presentation.createForHeight(720))
                        )
                    )
                    .build()

                transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (cont.isActive) cont.resume(Result.success(Unit))
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            if (cont.isActive) {
                                cont.resume(
                                    Result.failure(
                                        IllegalStateException(exportException.message ?: "Video compression failed")
                                    )
                                )
                            }
                        }
                    })
                    .build()

                cont.invokeOnCancellation {
                    runCatching { transformer.cancel() }
                    outputFile.delete()
                }

                transformer.start(editedItem, outputFile.absolutePath)
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(Result.failure(e))
            }
        }
    }

    private fun extractVideoMetadata(context: Context, uri: Uri): Pair<Int, ByteArray?> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val durationSec = ((durMs + 500L) / 1000L).toInt().coerceAtLeast(0)
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val thumbBytes = frame?.let { bmp ->
                val maxDim = 480
                val scaled = if (bmp.width > maxDim || bmp.height > maxDim) {
                    val ratio = minOf(maxDim.toFloat() / bmp.width, maxDim.toFloat() / bmp.height)
                    Bitmap.createScaledBitmap(
                        bmp,
                        (bmp.width * ratio).toInt().coerceAtLeast(1),
                        (bmp.height * ratio).toInt().coerceAtLeast(1),
                        true
                    ).also { if (it !== bmp) bmp.recycle() }
                } else {
                    bmp
                }
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 78, out)
                scaled.recycle()
                out.toByteArray()
            }
            durationSec to thumbBytes
        } catch (e: Exception) {
            Log.w(TAG, "extractVideoMetadata partial failure: ${e.message}")
            0 to null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun extractThumbnail(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            frame?.let { bmp ->
                val maxDim = 480
                if (bmp.width > maxDim || bmp.height > maxDim) {
                    val ratio = minOf(maxDim.toFloat() / bmp.width, maxDim.toFloat() / bmp.height)
                    Bitmap.createScaledBitmap(
                        bmp,
                        (bmp.width * ratio).toInt().coerceAtLeast(1),
                        (bmp.height * ratio).toInt().coerceAtLeast(1),
                        true
                    ).also { if (it !== bmp) bmp.recycle() }
                } else {
                    bmp
                }
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun prepareAudioFile(
        context: Context,
        uri: Uri
    ): Result<PreparedAudioFile> = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        try {
            val resolver = appContext.contentResolver
            var displayName = "Audio"
            var reportedSize = -1L
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) c.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                    if (sizeIdx >= 0 && !c.isNull(sizeIdx)) reportedSize = c.getLong(sizeIdx)
                }
            }
            if (reportedSize > MAX_MEDIA_BYTES) {
                return@withContext Result.failure(IllegalStateException(MSG_AUDIO_TOO_LARGE))
            }

            val availMem = readDeviceAvailMemBytes(appContext)
            if (reportedSize > 0L && !hasHeapHeadroomForBytes(reportedSize, deviceAvailMemBytes = availMem)) {
                return@withContext Result.failure(IllegalStateException(MSG_LOW_MEMORY))
            }

            val bytes = try {
                resolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(chunk)
                        if (read <= 0) break
                        total += read
                        if (total > MAX_MEDIA_BYTES) {
                            return@withContext Result.failure(IllegalStateException(MSG_AUDIO_TOO_LARGE))
                        }
                        buffer.write(chunk, 0, read)
                    }
                    buffer.toByteArray()
                } ?: return@withContext Result.failure(IllegalStateException("Could not open selected audio file"))
            } catch (_: OutOfMemoryError) {
                return@withContext Result.failure(IllegalStateException(MSG_LOW_MEMORY))
            }

            if (bytes.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Audio file is empty"))
            }

            val durationSec = runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(appContext, uri)
                    val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    ((ms + 500L) / 1000L).toInt().coerceAtLeast(0)
                } finally {
                    runCatching { retriever.release() }
                }
            }.getOrDefault(0)

            val rawMime = resolver.getType(uri)?.trim()?.takeIf { it.startsWith("audio/") }
                ?: when {
                    displayName.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
                    displayName.endsWith(".aac", ignoreCase = true) -> "audio/aac"
                    displayName.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
                    else -> "audio/mp4"
                }

            Result.success(
                PreparedAudioFile(
                    bytes = bytes,
                    displayName = displayName.replace("|", " ").take(80),
                    durationSeconds = durationSec,
                    mimeType = rawMime
                )
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "prepareAudioFile failed", e)
            Result.failure(e)
        }
    }
}
